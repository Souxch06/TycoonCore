package fr.valoriatycoon.tycoon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser and overlap validator for generated Tycoon plot grids. */
public final class TycoonConfigLoader {
    private static final Pattern GROUP_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private TycoonConfigLoader() {
    }

    public static TycoonSettings load(FileConfiguration config) {
        Map<String, TycoonPlotGroup> groups = new LinkedHashMap<>();
        ConfigurationSection section = requiredSection(config, "tycoons");
        for (String id : section.getKeys(false)) {
            if (!GROUP_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid lowercase Tycoon group id: " + id);
            }
            groups.put(id, parseGroup(id, requiredSection(section, id)));
        }
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("At least one Tycoon plot group is required");
        }
        validateNoOverlap(groups.values().stream().toList());
        validateIdenticalIslandTemplates(groups.values().stream().toList());
        return new TycoonSettings(
                integer(config, "reset.confirmation-seconds", 30, 5, 300),
                integer(config, "reset.blocks-per-tick", 2000, 100, 50_000),
                integer(config, "members.invite-expiration-seconds", 60, 10, 600),
                new TycoonSettings.Flight(
                        config.getBoolean("flight.enabled", true),
                        config.getBoolean("flight.allow-members", true),
                        integer(config, "flight.void-rescue-below-floor", 32, 8, 256)
                ),
                groups
        );
    }

    private static TycoonPlotGroup parseGroup(String id, ConfigurationSection section) {
        int plotSize = integer(section, "size", 32, 8, 256);
        int islandRadius = integer(section, "island-radius", 10, 2, Math.max(2, (plotSize - 2) / 2));
        int spacing = integer(section, "spacing", 5, 1, 128);
        int maximumPlots = integer(section, "max-players", 100, 1, 10_000);
        int floorY = integer(section, "floor-y", 80, -32, 300);
        int baseDepth = integer(section, "base-depth", 6, 1, islandRadius - 1);
        int buildMinimumY = integer(
                section,
                "build-min-y",
                floorY - baseDepth,
                floorY - baseDepth,
                floorY
        );
        int buildMaximumY = integer(section, "build-max-y", 180, floorY + 4, 319);
        if (floorY - baseDepth < -64) {
            throw new IllegalArgumentException("Base depth leaves the world height range for group " + id);
        }
        return new TycoonPlotGroup(
                id,
                text(section, "world"),
                section.getLong("seed", 1L),
                plotSize,
                spacing,
                maximumPlots,
                section.getInt("origin-x", 0),
                section.getInt("origin-z", 0),
                floorY,
                buildMinimumY,
                buildMaximumY,
                blockMaterial(section, "floor-material"),
                blockMaterial(section, "base-material"),
                islandRadius,
                baseDepth,
                decimal(section, "world-border-size", 10_000.0, 128.0, 59_999_968.0),
                integer(section, "max-members", 4, 0, 100)
        );
    }

    private static void validateIdenticalIslandTemplates(List<TycoonPlotGroup> groups) {
        TycoonPlotGroup template = groups.getFirst();
        for (TycoonPlotGroup group : groups) {
            if (group.plotSize() != template.plotSize()
                    || group.floorY() != template.floorY()
                    || group.buildMinimumY() != template.buildMinimumY()
                    || group.buildMaximumY() != template.buildMaximumY()
                    || group.floorMaterial() != template.floorMaterial()
                    || group.baseMaterial() != template.baseMaterial()
                    || group.islandRadius() != template.islandRadius()
                    || group.baseDepth() != template.baseDepth()) {
                throw new IllegalArgumentException(
                        "All player Skyblock groups must use the same island template; group "
                                + group.id() + " differs from " + template.id()
                );
            }
        }
    }

    private static void validateNoOverlap(List<TycoonPlotGroup> groups) {
        List<TycoonPlotGroup> checked = new ArrayList<>();
        for (TycoonPlotGroup current : groups) {
            TycoonPlotGroup.Bounds currentExtent = extent(current);
            for (TycoonPlotGroup previous : checked) {
                if (current.worldName().equals(previous.worldName())
                        && currentExtent.intersects(extent(previous))) {
                    throw new IllegalArgumentException(
                            "Tycoon groups overlap in world " + current.worldName()
                                    + ": " + previous.id() + " and " + current.id()
                    );
                }
            }
            checked.add(current);
        }
    }

    private static TycoonPlotGroup.Bounds extent(TycoonPlotGroup group) {
        int rows = (int) Math.ceil((double) group.maximumPlots() / group.columns());
        int maximumX = Math.addExact(
                group.originX(),
                Math.addExact(
                        Math.multiplyExact(group.columns() - 1, group.plotSize() + group.spacing()),
                        group.plotSize() - 1
                )
        );
        int maximumZ = Math.addExact(
                group.originZ(),
                Math.addExact(
                        Math.multiplyExact(rows - 1, group.plotSize() + group.spacing()),
                        group.plotSize() - 1
                )
        );
        return new TycoonPlotGroup.Bounds(group.originX(), maximumX, group.originZ(), maximumZ);
    }

    private static int integer(ConfigurationSection section, String path, int fallback, int minimum, int maximum) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static double decimal(
            ConfigurationSection section,
            String path,
            double fallback,
            double minimum,
            double maximum
    ) {
        double value = section.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
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

    private static Material blockMaterial(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path).toUpperCase(Locale.ROOT));
        if (material == null || !material.isBlock()) {
            throw new IllegalArgumentException(path + " must be a block material");
        }
        return material;
    }

    private static ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return section;
    }
}
