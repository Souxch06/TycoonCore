package fr.valoriatycoon.crates;

import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.tools.ToolType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for exact generic crate reward pools and amount ranges. */
public final class CrateRewardConfigLoader {
    private static final Set<String> REWARD_FIELDS = Set.of(
            "kind", "weight", "tier", "count", "tool", "key-type", "material",
            "all-generators", "broadcast"
    );

    private CrateRewardConfigLoader() {
    }

    public static CrateRewardSettings load(FileConfiguration config) {
        Map<Integer, CrateRewardSettings.AmountRange> money = tiers(
                required(config, "money-bags"), 5, true
        );
        Map<Integer, CrateRewardSettings.AmountRange> coins = tiers(
                required(config, "coin-bags"), 5, false
        );
        CrateRewardSettings.AmountRange universal = range(
                required(config, "universal-coin-bag"), false
        );
        Map<Integer, CrateRewardSettings.AmountRange> experience = tiers(
                required(config, "experience-vials"), 4, false
        );
        Map<Integer, CrateRewardSettings.AmountRange> bundles = tiers(
                required(config, "resource-bundles"), 3, false
        );
        List<CrateRewardSettings.Resource> resources = resources(required(config, "resources"));
        Map<CrateType, CrateRewardSettings.Pool> pools = pools(required(config, "pools"));
        CrateRewardSettings settings = new CrateRewardSettings(
                money, coins, universal, experience, bundles, resources, pools
        );
        validateDefinitions(settings);
        validateKeyChain(settings);
        return settings;
    }

    private static Map<Integer, CrateRewardSettings.AmountRange> tiers(
            ConfigurationSection section,
            int maximum,
            boolean money
    ) {
        Map<Integer, CrateRewardSettings.AmountRange> result = new LinkedHashMap<>();
        for (int tier = 1; tier <= maximum; tier++) {
            result.put(tier, range(required(section, Integer.toString(tier)), money));
        }
        if (section.getKeys(false).size() != maximum) {
            throw new IllegalArgumentException("Unexpected crate reward tier");
        }
        return result;
    }

    private static CrateRewardSettings.AmountRange range(
            ConfigurationSection section,
            boolean money
    ) {
        long minimum = money ? cents(section, "minimum") : positiveLong(section, "minimum");
        long maximum = money ? cents(section, "maximum") : positiveLong(section, "maximum");
        if (!section.getKeys(false).equals(Set.of("minimum", "maximum"))) {
            throw new IllegalArgumentException("Reward ranges require exactly minimum and maximum");
        }
        return new CrateRewardSettings.AmountRange(minimum, maximum);
    }

    private static List<CrateRewardSettings.Resource> resources(ConfigurationSection section) {
        List<CrateRewardSettings.Resource> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
            if (material == null) {
                throw new IllegalArgumentException("Unknown crate bundle resource: " + key);
            }
            result.add(new CrateRewardSettings.Resource(
                    material,
                    integer(section, key, -1, 0, 10)
            ));
        }
        return List.copyOf(result);
    }

    private static Map<CrateType, CrateRewardSettings.Pool> pools(ConfigurationSection section) {
        Map<CrateType, CrateRewardSettings.Pool> result = new EnumMap<>(CrateType.class);
        for (CrateType type : CrateType.values()) {
            ConfigurationSection pool = required(section, type.configKey());
            List<CrateRewardSettings.RewardDefinition> definitions = new ArrayList<>();
            for (String id : pool.getKeys(false)) {
                if (!id.matches("[a-z0-9_-]{1,64}")) {
                    throw new IllegalArgumentException("Invalid reward id in " + type + ": " + id);
                }
                ConfigurationSection reward = required(pool, id);
                if (!REWARD_FIELDS.containsAll(reward.getKeys(false))) {
                    throw new IllegalArgumentException("Unsupported field in crate reward " + id);
                }
                CrateRewardKind kind = enumValue(
                        CrateRewardKind.class,
                        text(reward, "kind"),
                        "reward kind"
                );
                ToolType tool = optionalEnum(ToolType.class, reward.getString("tool"), "reward tool");
                CrateType keyType = optionalEnum(
                        CrateType.class,
                        reward.getString("key-type"),
                        "reward key type"
                );
                Material material = optionalMaterial(reward.getString("material"));
                definitions.add(new CrateRewardSettings.RewardDefinition(
                        id,
                        kind,
                        integer(reward, "weight", -1, 1, CrateRewardSettings.POOL_WEIGHT),
                        integer(reward, "tier", 0, 0, 5),
                        integer(reward, "count", 1, 1, 64),
                        tool,
                        keyType,
                        material,
                        reward.getBoolean("all-generators", false),
                        reward.getBoolean("broadcast", false)
                ));
            }
            result.put(type, new CrateRewardSettings.Pool(definitions));
        }
        if (section.getKeys(false).size() != CrateType.values().length) {
            throw new IllegalArgumentException("pools must contain exactly every generic crate type");
        }
        return result;
    }

    private static void validateDefinitions(CrateRewardSettings settings) {
        for (CrateRewardSettings.Pool pool : settings.pools().values()) {
            for (CrateRewardSettings.RewardDefinition definition : pool.rewards()) {
                switch (definition.kind()) {
                    case MONEY_BAG -> settings.moneyTier(definition.tier());
                    case COIN_BAG -> settings.coinTier(definition.tier());
                    case XP_VIAL -> settings.experienceTier(definition.tier());
                    case RESOURCE_BUNDLE -> settings.resourceTier(definition.tier());
                    case VANILLA_ITEM -> {
                        if (!definition.material().isItem()) {
                            throw new IllegalArgumentException("Vanilla crate reward must be an item");
                        }
                    }
                    case UNIVERSAL_COIN_BAG, CRATE_KEYS, PET_KEYS, GENERATORS -> {
                        // Validated structurally by RewardDefinition.
                    }
                }
            }
        }
    }

    private static void validateKeyChain(CrateRewardSettings settings) {
        for (Map.Entry<CrateType, CrateRewardSettings.Pool> entry : settings.pools().entrySet()) {
            for (CrateRewardSettings.RewardDefinition reward : entry.getValue().rewards()) {
                if (reward.kind() != CrateRewardKind.CRATE_KEYS) {
                    continue;
                }
                CrateType source = entry.getKey();
                CrateType target = reward.keyType();
                if (target == CrateType.VALORIA && source != CrateType.LEGENDARY
                        || source == CrateType.COMMON && target == CrateType.LEGENDARY
                        || source == CrateType.COMMON && target == CrateType.VALORIA
                        || source == CrateType.RARE && target == CrateType.VALORIA
                        || source == CrateType.EPIC && target == CrateType.VALORIA) {
                    throw new IllegalArgumentException("Invalid generic crate key reward chain");
                }
            }
        }
        long valoriaWeight = settings.pool(CrateType.LEGENDARY).rewards().stream()
                .filter(reward -> reward.kind() == CrateRewardKind.CRATE_KEYS)
                .filter(reward -> reward.keyType() == CrateType.VALORIA)
                .mapToLong(CrateRewardSettings.RewardDefinition::weight)
                .sum();
        if (valoriaWeight != 50L) {
            throw new IllegalArgumentException("Legendary must contain exactly 0.5% Valoria key weight");
        }
    }

    private static long cents(ConfigurationSection section, String path) {
        String value = section.getString(path);
        try {
            long cents = MoneyCodec.toCents(new BigDecimal(value));
            if (cents < 1L) {
                throw new IllegalArgumentException("Money reward must be positive");
            }
            return cents;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid exact crate money at " + path, exception);
        }
    }

    private static long positiveLong(ConfigurationSection section, String path) {
        long value = section.getLong(path, -1L);
        if (value < 1L || value > 10_000_000_000L) {
            throw new IllegalArgumentException("Invalid positive crate reward number: " + path);
        }
        return value;
    }

    private static int integer(
            ConfigurationSection section,
            String path,
            int fallback,
            int minimum,
            int maximum
    ) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static Material optionalMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        if (material == null) {
            throw new IllegalArgumentException("Unknown crate reward material: " + raw);
        }
        return material;
    }

    private static <E extends Enum<E>> E optionalEnum(
            Class<E> type,
            String raw,
            String name
    ) {
        return raw == null || raw.isBlank() ? null : enumValue(type, raw, name);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String name) {
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown " + name + ": " + raw, exception);
        }
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing crate reward text: " + path);
        }
        return value.trim();
    }

    private static ConfigurationSection required(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing crate reward section: " + path);
        }
        return section;
    }
}
