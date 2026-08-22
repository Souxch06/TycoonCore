package fr.valoriatycoon.upgrades;

import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.tycoon.TycoonPlotGroup;
import fr.valoriatycoon.tycoon.TycoonSettings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict upgrades.yml parser for the first plot-upgrade slice. */
public final class PlotUpgradeConfigLoader {
    private PlotUpgradeConfigLoader() {
    }

    public static PlotUpgradeSettings load(FileConfiguration config, TycoonSettings tycoons) {
        int menuSize = integer(config, "menu.size", 27, 9, 54);
        if (menuSize % 9 != 0) {
            throw new IllegalArgumentException("upgrades menu size must be a multiple of 9");
        }
        Map<PlotUpgradeType, PlotUpgradeDefinition> definitions = new EnumMap<>(PlotUpgradeType.class);
        Set<Integer> slots = new HashSet<>();
        ConfigurationSection upgrades = requiredSection(config, "plot-upgrades");
        for (PlotUpgradeType type : PlotUpgradeType.values()) {
            ConfigurationSection section = requiredSection(upgrades, type.configKey());
            int slot = integer(section, "slot", 0, 0, menuSize - 1);
            if (!slots.add(slot)) {
                throw new IllegalArgumentException("Duplicate plot upgrade menu slot: " + slot);
            }
            int maximumLevel = integer(section, "max-level", 1, 1, 100);
            List<PlotUpgradeDefinition.Level> levels = new ArrayList<>(maximumLevel);
            int previousValue = -1;
            long previousCost = -1;
            ConfigurationSection configuredLevels = requiredSection(section, "levels");
            for (int level = 1; level <= maximumLevel; level++) {
                ConfigurationSection configured = requiredSection(configuredLevels, Integer.toString(level));
                int value = integer(configured, "value", 1, 1, Integer.MAX_VALUE);
                long cost = money(configured, "cost");
                if (value < previousValue || cost < previousCost) {
                    throw new IllegalArgumentException(type + " values and costs must not decrease");
                }
                levels.add(new PlotUpgradeDefinition.Level(level, value, cost));
                previousValue = value;
                previousCost = cost;
            }
            if (configuredLevels.getKeys(false).size() != maximumLevel) {
                throw new IllegalArgumentException(type + " levels must be sequential");
            }
            definitions.put(type, new PlotUpgradeDefinition(
                    type,
                    slot,
                    material(section, "icon"),
                    text(section, "name"),
                    section.getStringList("lore"),
                    levels
            ));
        }
        validateValues(definitions, tycoons);
        return new PlotUpgradeSettings(menuSize, text(config, "menu.title"), definitions);
    }

    private static void validateValues(
            Map<PlotUpgradeType, PlotUpgradeDefinition> definitions,
            TycoonSettings settings
    ) {
        int smallestPlot = settings.groups().values().stream().mapToInt(TycoonPlotGroup::plotSize).min().orElseThrow();
        int largestIslandDiameter = settings.groups().values().stream()
                .mapToInt(group -> group.islandRadius() * 2 + 1).max().orElseThrow();
        PlotUpgradeDefinition size = definitions.get(PlotUpgradeType.PLOT_SIZE);
        if (size.level(1).orElseThrow().value() < largestIslandDiameter
                || size.level(size.maximumLevel()).orElseThrow().value() > smallestPlot) {
            throw new IllegalArgumentException("Plot size upgrades must contain the island and stay in allocation bounds");
        }
        int hardMemberLimit = settings.groups().values().stream()
                .mapToInt(TycoonPlotGroup::maximumMembers).min().orElseThrow();
        PlotUpgradeDefinition members = definitions.get(PlotUpgradeType.MEMBER_LIMIT);
        if (members.level(members.maximumLevel()).orElseThrow().value() > hardMemberLimit) {
            throw new IllegalArgumentException("Member upgrade exceeds tycoons.yml max-members");
        }
    }

    private static long money(ConfigurationSection section, String path) {
        try {
            long value = MoneyCodec.toCents(new BigDecimal(section.getString(path)));
            if (value < 0) {
                throw new IllegalArgumentException("Negative cost");
            }
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid money at " + path, exception);
        }
    }

    private static int integer(ConfigurationSection section, String path, int fallback, int minimum, int maximum) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " cannot be blank");
        }
        return value.trim();
    }

    private static Material material(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path).toUpperCase(Locale.ROOT));
        if (material == null) {
            throw new IllegalArgumentException("Unknown material at " + path);
        }
        return material;
    }

    private static ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing section " + path);
        }
        return section;
    }
}
