package fr.valoriatycoon.tools;

import fr.valoriatycoon.economy.MoneyCodec;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict tools.yml parser. Invalid level curves fail startup rather than corrupting purchases. */
public final class ToolConfigLoader {
    private ToolConfigLoader() {
    }

    public static ToolSettings load(FileConfiguration config) {
        int menuSize = integer(config, "menu.size", 36, 9, 54);
        if (menuSize % 9 != 0) {
            throw new IllegalArgumentException("menu.size must be a multiple of 9");
        }
        ToolSettings.Menu menu = new ToolSettings.Menu(
                menuSize,
                text(config, "menu.title"),
                slot(config, "menu.info.slot", menuSize),
                text(config, "menu.info.name"),
                config.getStringList("menu.info.lore"),
                slot(config, "menu.autosell.slot", menuSize),
                material(config, "menu.autosell.icon"),
                material(config, "menu.autosell.maximum-icon"),
                text(config, "menu.autosell.name"),
                text(config, "menu.autosell.maximum-name"),
                config.getStringList("menu.autosell.lore"),
                config.getStringList("menu.autosell.maximum-lore")
        );
        int purchaseSize = integer(config, "purchase-menu.size", 9, 9, 54);
        if (purchaseSize % 9 != 0) {
            throw new IllegalArgumentException("purchase-menu.size must be a multiple of 9");
        }
        ToolSettings.PurchaseMenu purchaseMenu = new ToolSettings.PurchaseMenu(
                purchaseSize,
                text(config, "purchase-menu.title"),
                slot(config, "purchase-menu.money.slot", purchaseSize),
                material(config, "purchase-menu.money.icon"),
                text(config, "purchase-menu.money.name"),
                config.getStringList("purchase-menu.money.lore"),
                slot(config, "purchase-menu.coins.slot", purchaseSize),
                text(config, "purchase-menu.coins.name"),
                config.getStringList("purchase-menu.coins.lore")
        );
        if (purchaseMenu.moneySlot() == purchaseMenu.coinSlot()) {
            throw new IllegalArgumentException("Purchase currency slots must differ");
        }
        ToolSettings.MultiTool multiTool = new ToolSettings.MultiTool(
                config.getBoolean("multitool.enabled", true),
                decimal(config, "multitool.fishing-ray-distance", 6.0, 1.0, 12.0),
                config.getBoolean("multitool.notify-switch", false),
                rankTiers(config)
        );
        ToolSettings.AbilitySettings abilities = new ToolSettings.AbilitySettings(
                integer(config, "ability-settings.speed-duration-ticks", 60, 10, 600),
                integer(config, "ability-settings.speed-amplifier", 1, 0, 4),
                integer(config, "ability-settings.speed-cooldown-seconds", 15, 1, 300),
                integer(config, "ability-settings.maximum-timber-blocks", 4, 1, 16),
                positiveDecimal(config, "ability-settings.efficiency-hard-cap"),
                integer(config, "ability-settings.gem-coin-bonus", 25, 1, 1_000_000),
                positiveDecimal(config, "ability-settings.golden-apple-relative-chance"),
                positiveDecimal(config, "ability-settings.notch-apple-relative-chance"),
                integer(config, "ability-settings.ufo-display-ticks", 40, 10, 200)
        );
        if (abilities.goldenAppleRelativeChance().add(abilities.notchAppleRelativeChance())
                .compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Apple rarity chances must total less than 1");
        }
        int maximumToolLevel = integer(
                config,
                "progression.max-tool-level",
                100,
                1,
                10_000
        );
        ToolSettings.Progression progression = new ToolSettings.Progression(
                maximumToolLevel,
                positiveLong(config, "progression.base-experience"),
                positiveDecimal(config, "progression.experience-multiplier"),
                parseExperienceTiers(
                        config.getConfigurationSection("progression.difficulty-tiers"),
                        maximumToolLevel
                ),
                integer(config, "progression.flush-interval-ticks", 20, 1, 1200)
        );

        Map<ToolType, ToolDefinition> tools = parseTools(requiredSection(config, "tools"), menuSize);
        Map<ToolCapability, ToolCapabilityDefinition> capabilities = parseCapabilities(
                requiredSection(config, "capabilities"),
                menuSize
        );
        ToolCapabilityDefinition efficiency = capabilities.get(ToolCapability.EFFICIENCY);
        BigDecimal maximumEfficiency = efficiency.level(efficiency.maximumLevel()).orElseThrow().value();
        if (maximumEfficiency.compareTo(abilities.efficiencyHardCap()) > 0) {
            throw new IllegalArgumentException(
                    "Efficiency curve exceeds ability-settings.efficiency-hard-cap"
            );
        }
        for (ToolType type : ToolType.values()) {
            Set<Integer> slots = new HashSet<>();
            requireUniqueSlot(slots, menu.infoSlot(), "menu.info.slot");
            requireUniqueSlot(slots, menu.autoSellSlot(), "menu.autosell.slot");
            for (ToolDefinition tool : tools.values()) {
                requireUniqueSlot(slots, tool.menuSlot(), "tools." + tool.type().storageKey() + ".slot");
            }
            for (ToolCapabilityDefinition capability : capabilities.values()) {
                if (capability.appliesTo(type)) {
                    requireUniqueSlot(
                            slots,
                            capability.slot(),
                            "capabilities." + capability.capability().storageKey() + ".slot for " + type
                    );
                }
            }
        }
        return new ToolSettings(
                menu, purchaseMenu, multiTool, abilities, progression, tools, capabilities
        );
    }

    private static Map<Integer, ToolTier> rankTiers(ConfigurationSection config) {
        ConfigurationSection section = config.getConfigurationSection("multitool.rank-tiers");
        if (section == null) {
            return new ToolSettings.MultiTool(true, 6.0, false).rankTiers();
        }
        Map<Integer, ToolTier> tiers = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            int rank;
            try {
                rank = Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid multi-tool rank: " + key, exception);
            }
            if (rank < 0 || rank > 10 || tiers.containsKey(rank)) {
                throw new IllegalArgumentException("Invalid or duplicate multi-tool rank: " + rank);
            }
            String rawTier = text(section, key).toUpperCase(Locale.ROOT);
            try {
                tiers.put(rank, ToolTier.valueOf(rawTier));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unknown multi-tool tier at rank " + rank + ": " + rawTier,
                        exception
                );
            }
        }
        return tiers;
    }

    private static List<ToolSettings.Progression.ExperienceTier> parseExperienceTiers(
            ConfigurationSection section,
            int maximumToolLevel
    ) {
        if (section == null) {
            return List.of(new ToolSettings.Progression.ExperienceTier(1, BigDecimal.ONE));
        }
        List<ToolSettings.Progression.ExperienceTier> tiers = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            int minimumLevel;
            try {
                minimumLevel = Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "progression.difficulty-tiers keys must be tool levels",
                        exception
                );
            }
            if (minimumLevel < 1 || minimumLevel > maximumToolLevel) {
                throw new IllegalArgumentException(
                        "Tool difficulty tier " + minimumLevel + " is outside the level cap"
                );
            }
            tiers.add(new ToolSettings.Progression.ExperienceTier(
                    minimumLevel,
                    positiveDecimal(section, key)
            ));
        }
        tiers.sort((left, right) -> Integer.compare(left.minimumLevel(), right.minimumLevel()));
        return tiers;
    }

    private static Map<ToolType, ToolDefinition> parseTools(ConfigurationSection section, int menuSize) {
        Map<ToolType, ToolDefinition> tools = new EnumMap<>(ToolType.class);
        for (ToolType type : ToolType.values()) {
            ConfigurationSection tool = requiredSection(section, type.storageKey());
            tools.put(type, new ToolDefinition(
                    type,
                    material(tool, "icon"),
                    text(tool, "name"),
                    slot(tool, "slot", menuSize),
                    tool.getStringList("lore"),
                    positiveLong(tool, "experience-per-action"),
                    text(tool, "currency.name"),
                    material(tool, "currency.icon"),
                    positiveLong(tool, "currency.coins-per-action")
            ));
        }
        if (section.getKeys(false).size() != ToolType.values().length) {
            throw new IllegalArgumentException("tools must contain exactly the supported tool types");
        }
        return tools;
    }

    private static Map<ToolCapability, ToolCapabilityDefinition> parseCapabilities(
            ConfigurationSection section,
            int menuSize
    ) {
        Map<ToolCapability, ToolCapabilityDefinition> capabilities = new EnumMap<>(ToolCapability.class);
        for (ToolCapability capability : ToolCapability.values()) {
            ConfigurationSection configured = requiredSection(section, capability.storageKey());
            int initialLevel = integer(configured, "initial-level", 1, 0, 2_000);
            int maximumLevel = integer(configured, "max-level", 5, 1, 2_000);
            if (initialLevel > maximumLevel) {
                throw new IllegalArgumentException(capability + " initial-level exceeds max-level");
            }
            EnumSet<ToolType> applicableTools = EnumSet.noneOf(ToolType.class);
            for (String configuredTool : configured.getStringList("applicable-tools")) {
                try {
                    applicableTools.add(ToolType.valueOf(configuredTool.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                            "Unknown tool " + configuredTool + " for " + capability,
                            exception
                    );
                }
            }
            if (applicableTools.isEmpty()) {
                throw new IllegalArgumentException(capability + " must apply to at least one tool");
            }
            List<ToolCapabilityDefinition.Level> levels = parseLevels(configured, maximumLevel, capability);
            capabilities.put(capability, new ToolCapabilityDefinition(
                    capability,
                    applicableTools,
                    initialLevel,
                    slot(configured, "slot", menuSize),
                    material(configured, "icon"),
                    text(configured, "name"),
                    configured.getStringList("lore"),
                    levels
            ));
        }
        if (section.getKeys(false).size() != ToolCapability.values().length) {
            throw new IllegalArgumentException("capabilities contains an unsupported or missing capability");
        }
        return capabilities;
    }

    private static List<ToolCapabilityDefinition.Level> parseLevels(
            ConfigurationSection configured,
            int maximumLevel,
            ToolCapability capability
    ) {
        ConfigurationSection explicit = configured.getConfigurationSection("levels");
        if (explicit != null) {
            List<ToolCapabilityDefinition.Level> levels = new ArrayList<>(maximumLevel);
            for (int level = 1; level <= maximumLevel; level++) {
                ConfigurationSection levelSection = requiredSection(explicit, Integer.toString(level));
                levels.add(new ToolCapabilityDefinition.Level(
                        level,
                        nonNegativeMoney(levelSection, "cost"),
                        nonNegativeLong(levelSection, "coin-cost"),
                        positiveDecimal(levelSection, "value")
                ));
            }
            if (explicit.getKeys(false).size() != maximumLevel) {
                throw new IllegalArgumentException(capability + " levels must be sequential");
            }
            validateMonotonic(levels, capability);
            return levels;
        }

        ConfigurationSection curve = requiredSection(configured, "curve");
        long moneyUnlock = optionalMoney(curve, "unlock-money-cost", 0L);
        long coinUnlock = curve.getLong("unlock-coin-cost", 0L);
        if (coinUnlock < 0) {
            throw new IllegalArgumentException("unlock-coin-cost must be non-negative");
        }
        long moneyBase = nonNegativeMoney(curve, "money-base-cost");
        long moneyStep = nonNegativeMoney(curve, "money-cost-per-level");
        long coinBase = nonNegativeLong(curve, "coin-base-cost");
        long coinStep = nonNegativeLong(curve, "coin-cost-per-level");
        BigDecimal baseValue = positiveDecimal(curve, "base-value");
        BigDecimal valueStep = nonNegativeDecimal(curve, "value-per-level");
        List<ToolCapabilityDefinition.Level> levels = new ArrayList<>(maximumLevel);
        for (int level = 1; level <= maximumLevel; level++) {
            long costIndex = Math.max(0L, level - 2L);
            long moneyCost = level == 1 ? moneyUnlock : saturatingLinear(moneyBase, moneyStep, costIndex);
            long coinCost = level == 1 ? coinUnlock : saturatingLinear(coinBase, coinStep, costIndex);
            BigDecimal value = baseValue.add(valueStep.multiply(BigDecimal.valueOf(level - 1L)));
            levels.add(new ToolCapabilityDefinition.Level(level, moneyCost, coinCost, value));
        }
        return levels;
    }

    private static void validateMonotonic(
            List<ToolCapabilityDefinition.Level> levels,
            ToolCapability capability
    ) {
        long money = -1L;
        long coins = -1L;
        BigDecimal value = BigDecimal.ZERO;
        for (ToolCapabilityDefinition.Level level : levels) {
            if (level.moneyCostCents() < money
                    || level.toolCoinCost() < coins
                    || level.value().compareTo(value) < 0) {
                throw new IllegalArgumentException(capability + " costs and values must not decrease");
            }
            money = level.moneyCostCents();
            coins = level.toolCoinCost();
            value = level.value();
        }
    }

    private static long saturatingLinear(long base, long step, long index) {
        try {
            return Math.addExact(base, Math.multiplyExact(step, index));
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static void requireUniqueSlot(Set<Integer> slots, int slot, String path) {
        if (!slots.add(slot)) {
            throw new IllegalArgumentException("Duplicate tool menu slot at " + path + ": " + slot);
        }
    }

    private static long optionalMoney(ConfigurationSection section, String path, long fallbackCents) {
        if (!section.contains(path)) {
            return fallbackCents;
        }
        return nonNegativeMoney(section, path);
    }

    private static long nonNegativeMoney(ConfigurationSection section, String path) {
        String raw = section.getString(path);
        try {
            long cents = MoneyCodec.toCents(new BigDecimal(raw));
            if (cents < 0) {
                throw new IllegalArgumentException("Negative cost");
            }
            return cents;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(path + " must be an exact non-negative amount", exception);
        }
    }

    private static long nonNegativeLong(ConfigurationSection section, String path) {
        long value = section.getLong(path, -1L);
        if (value < 0) {
            throw new IllegalArgumentException(path + " must be non-negative");
        }
        return value;
    }

    private static long positiveLong(ConfigurationSection section, String path) {
        long value = section.getLong(path, -1);
        if (value <= 0) {
            throw new IllegalArgumentException(path + " must be positive");
        }
        return value;
    }

    private static BigDecimal nonNegativeDecimal(ConfigurationSection section, String path) {
        String raw = section.getString(path);
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.signum() < 0 || value.scale() > 6) {
                throw new IllegalArgumentException("Invalid decimal");
            }
            return value.stripTrailingZeros();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(path + " must be non-negative with at most six decimals", exception);
        }
    }

    private static BigDecimal positiveDecimal(ConfigurationSection section, String path) {
        String raw = section.getString(path);
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.signum() <= 0 || value.scale() > 4) {
                throw new IllegalArgumentException("Invalid decimal");
            }
            return value.stripTrailingZeros();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(path + " must be positive with at most four decimals", exception);
        }
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

    private static int integer(ConfigurationSection section, String path, int fallback, int minimum, int maximum) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static int slot(ConfigurationSection section, String path, int menuSize) {
        return integer(section, path, 0, 0, menuSize - 1);
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " cannot be blank");
        }
        return value.trim();
    }

    private static Material material(ConfigurationSection section, String path) {
        String value = text(section, path).toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(value);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material at " + path + ": " + value);
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
