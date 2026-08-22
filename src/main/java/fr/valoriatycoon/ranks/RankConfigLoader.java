package fr.valoriatycoon.ranks;

import fr.valoriatycoon.compaction.CompactedResource;
import fr.valoriatycoon.compaction.CompactionSettings;
import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.professions.ProfessionType;
import fr.valoriatycoon.quests.QuestRarity;
import fr.valoriatycoon.tools.ToolType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Loads and validates the complete medieval-rank progression. */
public final class RankConfigLoader {
    private static final long SECONDS_PER_MINUTE = 60L;

    private RankConfigLoader() {
    }

    /** Loads rank requirements from {@code ranks.yml}. */
    public static RankSettings load(FileConfiguration config) {
        return load(config, null);
    }

    /** Loads ranks and validates every compacted requirement against compaction.yml. */
    public static RankSettings load(
            FileConfiguration config,
            CompactionSettings compactionSettings
    ) {
        int maximum = config.getInt("ranks.max-level", 10);
        if (maximum != 10) {
            throw new IllegalArgumentException("This version requires exactly 10 medieval ranks");
        }

        List<RankRequirement> levels = new ArrayList<>(maximum);
        ConfigurationSection section = required(config, "ranks.levels");
        long previousPlaytime = 0L;
        int previousVanillaExperience = 0;
        double previousRevenueBonus = 0.0;
        double previousToolExperienceBonus = 0.0;
        double previousProfessionExperienceBonus = 0.0;
        double previousToolCoinBonus = 0.0;
        double previousGeneratorProductionBonus = 0.0;
        int previousGeneratorSlotBonus = 0;
        Map<ProfessionType, Integer> previousProfessionLevels = new EnumMap<>(ProfessionType.class);
        for (int level = 1; level <= maximum; level++) {
            ConfigurationSection value = required(section, Integer.toString(level));
            long requiredPlaytime = playtimeSeconds(value, level);
            if (requiredPlaytime < previousPlaytime) {
                throw new IllegalArgumentException(
                        "playtime-minutes must not decrease between medieval ranks"
                );
            }
            int requiredVanillaExperience = nonNegativeInteger(
                    value,
                    "vanilla-xp-levels",
                    defaultVanillaExperience(level)
            );
            if (requiredVanillaExperience < previousVanillaExperience) {
                throw new IllegalArgumentException(
                        "vanilla-xp-levels must not decrease between medieval ranks"
                );
            }
            ConfigurationSection professionSection = value.getConfigurationSection(
                    "profession-levels"
            );
            Map<ProfessionType, Integer> requiredProfessions = professionSection == null
                    ? defaultProfessionLevels(level)
                    : professionMap(professionSection);
            for (ProfessionType type : ProfessionType.values()) {
                int current = requiredProfessions.getOrDefault(type, 0);
                if (current < previousProfessionLevels.getOrDefault(type, 0)) {
                    throw new IllegalArgumentException(
                            "profession-levels must not decrease for " + type
                    );
                }
            }
            double revenueBonus = bonus(
                    value,
                    "permanent-revenue-bonus",
                    defaultRevenueBonus(level)
            );
            double toolExperienceBonus = bonus(
                    value,
                    "permanent-tool-xp-bonus",
                    defaultToolExperienceBonus(level)
            );
            double professionExperienceBonus = bonus(
                    value,
                    "permanent-profession-xp-bonus",
                    defaultProfessionExperienceBonus(level)
            );
            double toolCoinBonus = bonus(
                    value,
                    "permanent-tool-coin-bonus",
                    defaultToolCoinBonus(level)
            );
            double generatorProductionBonus = bonus(
                    value,
                    "permanent-generator-production-bonus",
                    defaultGeneratorProductionBonus(level)
            );
            int generatorSlotBonus = nonNegativeInteger(
                    value,
                    "generator-slot-bonus",
                    defaultGeneratorSlotBonus(level)
            );
            if (revenueBonus < previousRevenueBonus
                    || toolExperienceBonus < previousToolExperienceBonus
                    || professionExperienceBonus < previousProfessionExperienceBonus
                    || toolCoinBonus < previousToolCoinBonus
                    || generatorProductionBonus < previousGeneratorProductionBonus
                    || generatorSlotBonus < previousGeneratorSlotBonus) {
                throw new IllegalArgumentException("Permanent rank bonuses must not decrease");
            }
            levels.add(new RankRequirement(
                    level,
                    text(value, "name"),
                    money(value, "money"),
                    requiredPlaytime,
                    requiredVanillaExperience,
                    rarityMap(value.getConfigurationSection("quests")),
                    toolMap(value.getConfigurationSection("tool-levels")),
                    requiredProfessions,
                    itemMap(value.getConfigurationSection("items")),
                    compactedItemMap(
                            value.getConfigurationSection("compacted-items"),
                            compactionSettings
                    ),
                    revenueBonus,
                    toolExperienceBonus,
                    professionExperienceBonus,
                    toolCoinBonus,
                    generatorProductionBonus,
                    generatorSlotBonus
            ));
            previousPlaytime = requiredPlaytime;
            previousVanillaExperience = requiredVanillaExperience;
            previousRevenueBonus = revenueBonus;
            previousToolExperienceBonus = toolExperienceBonus;
            previousProfessionExperienceBonus = professionExperienceBonus;
            previousToolCoinBonus = toolCoinBonus;
            previousGeneratorProductionBonus = generatorProductionBonus;
            previousGeneratorSlotBonus = generatorSlotBonus;
            previousProfessionLevels.clear();
            previousProfessionLevels.putAll(requiredProfessions);
        }
        return new RankSettings(levels);
    }

    private static Map<QuestRarity, Integer> rarityMap(ConfigurationSection section) {
        Map<QuestRarity, Integer> values = new EnumMap<>(QuestRarity.class);
        if (section == null) {
            return values;
        }
        section.getKeys(false).forEach(key -> values.put(
                QuestRarity.valueOf(key.toUpperCase(Locale.ROOT)),
                section.getInt(key)
        ));
        return values;
    }

    private static Map<ToolType, Integer> toolMap(ConfigurationSection section) {
        Map<ToolType, Integer> values = new EnumMap<>(ToolType.class);
        if (section == null) {
            return values;
        }
        section.getKeys(false).forEach(key -> values.put(
                ToolType.valueOf(key.toUpperCase(Locale.ROOT)),
                section.getInt(key)
        ));
        return values;
    }

    private static Map<ProfessionType, Integer> professionMap(ConfigurationSection section) {
        Map<ProfessionType, Integer> values = new EnumMap<>(ProfessionType.class);
        if (section == null) {
            return values;
        }
        section.getKeys(false).forEach(key -> values.put(
                ProfessionType.valueOf(key.toUpperCase(Locale.ROOT)),
                positiveInteger(section, key)
        ));
        return values;
    }

    private static Map<ProfessionType, Integer> defaultProfessionLevels(int level) {
        int[][] requirements = {
                {1, 1, 1, 1},
                {4, 3, 4, 2},
                {7, 6, 7, 4},
                {15, 12, 15, 9},
                {22, 18, 22, 13},
                {30, 24, 30, 18},
                {40, 32, 40, 24},
                {52, 42, 52, 32},
                {66, 54, 66, 42},
                {100, 100, 100, 100}
        };
        int[] values = requirements[level - 1];
        Map<ProfessionType, Integer> result = new EnumMap<>(ProfessionType.class);
        result.put(ProfessionType.MINER, values[0]);
        result.put(ProfessionType.LUMBERJACK, values[1]);
        result.put(ProfessionType.FARMER, values[2]);
        result.put(ProfessionType.FISHER, values[3]);
        return result;
    }

    private static int defaultVanillaExperience(int level) {
        return switch (level) {
            case 1 -> 20;
            case 2 -> 100;
            case 3 -> 200;
            case 4 -> 400;
            case 5 -> 500;
            case 6 -> 600;
            case 7 -> 700;
            case 8 -> 800;
            case 9 -> 900;
            case 10 -> 1_000;
            default -> throw new IllegalArgumentException("Unsupported medieval rank " + level);
        };
    }

    private static double defaultRevenueBonus(int level) {
        return level * 0.02;
    }

    private static double defaultToolExperienceBonus(int level) {
        return new double[]{0.02, 0.05, 0.08, 0.12, 0.16, 0.20, 0.25, 0.30, 0.40, 0.50}[level - 1];
    }

    private static double defaultProfessionExperienceBonus(int level) {
        return new double[]{0.02, 0.05, 0.08, 0.12, 0.16, 0.20, 0.25, 0.30, 0.40, 0.50}[level - 1];
    }

    private static double defaultToolCoinBonus(int level) {
        return new double[]{0.01, 0.03, 0.05, 0.08, 0.12, 0.16, 0.20, 0.25, 0.30, 0.40}[level - 1];
    }

    private static double defaultGeneratorProductionBonus(int level) {
        return new double[]{0.00, 0.02, 0.04, 0.06, 0.08, 0.10, 0.12, 0.15, 0.18, 0.25}[level - 1];
    }

    private static int defaultGeneratorSlotBonus(int level) {
        return new int[]{0, 1, 2, 3, 4, 6, 8, 10, 12, 15}[level - 1];
    }

    private static double bonus(
            ConfigurationSection section,
            String path,
            double fallback
    ) {
        double value = section.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < 0.0 || value > 10.0) {
            throw new IllegalArgumentException(path + " must be between 0 and 10");
        }
        return value;
    }

    private static Map<CompactedResource, Integer> compactedItemMap(
            ConfigurationSection section,
            CompactionSettings compactionSettings
    ) {
        Map<CompactedResource, Integer> values = new LinkedHashMap<>();
        if (section == null) {
            return values;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                throw new IllegalArgumentException("Unknown compacted item " + key);
            }
            ConfigurationSection configured = required(section, key);
            int level = positiveInteger(configured, "level");
            int amount = positiveInteger(configured, "amount");
            if (compactionSettings != null
                    && (!compactionSettings.resources().containsKey(material)
                    || level > compactionSettings.maximumLevel())) {
                throw new IllegalArgumentException(
                        key + " level " + level + " is not configured for compaction"
                );
            }
            values.put(new CompactedResource(material, level), amount);
        }
        return values;
    }

    private static Map<Material, Integer> itemMap(ConfigurationSection section) {
        Map<Material, Integer> values = new LinkedHashMap<>();
        if (section == null) {
            return values;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                throw new IllegalArgumentException("Unknown item " + key);
            }
            values.put(material, section.getInt(key));
        }
        return values;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        return value.trim();
    }

    private static long money(ConfigurationSection section, String path) {
        long cents = MoneyCodec.toCents(new BigDecimal(text(section, path)));
        if (cents < 0) {
            throw new IllegalArgumentException(path + " must not be negative");
        }
        return cents;
    }

    private static int positiveInteger(ConfigurationSection section, String path) {
        int value = section.getInt(path, -1);
        if (value < 1) {
            throw new IllegalArgumentException(path + " must be positive");
        }
        return value;
    }

    private static int nonNegativeInteger(
            ConfigurationSection section,
            String path,
            int fallback
    ) {
        int value = section.getInt(path, fallback);
        if (value < 0) {
            throw new IllegalArgumentException(path + " must not be negative");
        }
        return value;
    }

    private static long playtimeSeconds(ConfigurationSection section, int level) {
        long defaultMinutes = switch (level) {
            case 1 -> 30L;
            case 2 -> 180L;
            case 3 -> 360L;
            case 4 -> 600L;
            case 5 -> 1_200L;
            case 6 -> 2_400L;
            case 7 -> 4_800L;
            case 8 -> 8_400L;
            case 9 -> 13_200L;
            case 10 -> 20_160L;
            default -> throw new IllegalArgumentException("Unsupported medieval rank " + level);
        };
        long minutes = section.getLong("playtime-minutes", defaultMinutes);
        if (minutes < 0) {
            throw new IllegalArgumentException("playtime-minutes must not be negative");
        }
        try {
            return Math.multiplyExact(minutes, SECONDS_PER_MINUTE);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("playtime-minutes is too large", exception);
        }
    }

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return value;
    }
}
