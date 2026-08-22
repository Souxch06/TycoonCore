package fr.valoriatycoon.crates;

import fr.valoriatycoon.tools.ToolType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Immutable reward ranges and exact 10,000-weight pools for every generic crate. */
public record CrateRewardSettings(
        Map<Integer, AmountRange> moneyBagTiers,
        Map<Integer, AmountRange> coinBagTiers,
        AmountRange universalCoinBag,
        Map<Integer, AmountRange> experienceVialTiers,
        Map<Integer, AmountRange> resourceBundleTiers,
        List<Resource> resources,
        Map<CrateType, Pool> pools
) {
    public static final int POOL_WEIGHT = 10_000;

    public CrateRewardSettings {
        moneyBagTiers = immutableIntegerMap(moneyBagTiers);
        coinBagTiers = immutableIntegerMap(coinBagTiers);
        experienceVialTiers = immutableIntegerMap(experienceVialTiers);
        resourceBundleTiers = immutableIntegerMap(resourceBundleTiers);
        universalCoinBag = Objects.requireNonNull(universalCoinBag, "universalCoinBag");
        resources = List.copyOf(resources);
        EnumMap<CrateType, Pool> poolCopy = new EnumMap<>(CrateType.class);
        poolCopy.putAll(Objects.requireNonNull(pools, "pools"));
        pools = Collections.unmodifiableMap(poolCopy);
        if (moneyBagTiers.size() != 5 || coinBagTiers.size() != 5
                || experienceVialTiers.size() != 4 || resourceBundleTiers.size() != 3
                || resources.isEmpty() || pools.size() != CrateType.values().length) {
            throw new IllegalArgumentException("Incomplete generic crate reward settings");
        }
        requireConsecutive(moneyBagTiers, 5, "money bags");
        requireConsecutive(coinBagTiers, 5, "coin bags");
        requireConsecutive(experienceVialTiers, 4, "experience vials");
        requireConsecutive(resourceBundleTiers, 3, "resource bundles");
        for (CrateType type : CrateType.values()) {
            Pool pool = pools.get(type);
            if (pool == null || pool.totalWeight() != POOL_WEIGHT) {
                throw new IllegalArgumentException(type + " reward pool must total " + POOL_WEIGHT);
            }
        }
    }

    public Pool pool(CrateType type) {
        Pool pool = pools.get(type);
        if (pool == null) {
            throw new IllegalArgumentException("Missing crate reward pool: " + type);
        }
        return pool;
    }

    public AmountRange moneyTier(int tier) {
        return requiredTier(moneyBagTiers, tier, "money bag");
    }

    public AmountRange coinTier(int tier) {
        return requiredTier(coinBagTiers, tier, "coin bag");
    }

    public AmountRange experienceTier(int tier) {
        return requiredTier(experienceVialTiers, tier, "experience vial");
    }

    public AmountRange resourceTier(int tier) {
        return requiredTier(resourceBundleTiers, tier, "resource bundle");
    }

    private AmountRange requiredTier(Map<Integer, AmountRange> tiers, int tier, String name) {
        AmountRange range = tiers.get(tier);
        if (range == null) {
            throw new IllegalArgumentException("Unknown " + name + " tier " + tier);
        }
        return range;
    }

    private static Map<Integer, AmountRange> immutableIntegerMap(Map<Integer, AmountRange> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(source, "source")));
    }

    private static void requireConsecutive(Map<Integer, AmountRange> tiers, int maximum, String name) {
        for (int tier = 1; tier <= maximum; tier++) {
            if (!tiers.containsKey(tier)) {
                throw new IllegalArgumentException("Missing " + name + " tier " + tier);
            }
        }
    }

    /** Inclusive positive range resolved once during the server-side draw. */
    public record AmountRange(long minimum, long maximum) {
        public AmountRange {
            if (minimum < 1L || maximum < minimum || maximum > 10_000_000_000L) {
                throw new IllegalArgumentException("Invalid crate reward amount range");
            }
        }
    }

    /** One normal resource unlocked at a medieval rank. */
    public record Resource(Material material, int requiredRank) {
        public Resource {
            material = Objects.requireNonNull(material, "material");
            if (!material.isItem() || requiredRank < 0 || requiredRank > 10
                    || material == Material.COD || material == Material.SALMON
                    || material == Material.PUFFERFISH || material == Material.TROPICAL_FISH) {
                throw new IllegalArgumentException("Invalid crate bundle resource: " + material);
            }
        }
    }

    /** One weighted immutable definition. Kind-specific random values are not stored here. */
    public record RewardDefinition(
            String id,
            CrateRewardKind kind,
            int weight,
            int tier,
            int count,
            ToolType toolType,
            CrateType keyType,
            Material material,
            boolean allGenerators,
            boolean broadcast
    ) {
        public RewardDefinition {
            id = Objects.requireNonNull(id, "id");
            kind = Objects.requireNonNull(kind, "kind");
            if (!id.matches("[a-z0-9_-]{1,64}") || weight < 1 || weight > POOL_WEIGHT
                    || tier < 0 || tier > 5 || count < 1 || count > 64) {
                throw new IllegalArgumentException("Invalid crate reward definition: " + id);
            }
            switch (kind) {
                case MONEY_BAG, COIN_BAG, XP_VIAL, RESOURCE_BUNDLE -> {
                    if (tier < 1) {
                        throw new IllegalArgumentException(kind + " requires a tier");
                    }
                }
                case UNIVERSAL_COIN_BAG -> {
                    if (tier != 0) {
                        throw new IllegalArgumentException("Universal coin bags do not use tiers");
                    }
                }
                case VANILLA_ITEM -> Objects.requireNonNull(material, "material");
                case CRATE_KEYS -> Objects.requireNonNull(keyType, "keyType");
                case PET_KEYS, GENERATORS -> {
                    // Count is sufficient; generator identities are resolved at draw time.
                }
            }
            if (kind != CrateRewardKind.COIN_BAG && toolType != null
                    || kind != CrateRewardKind.CRATE_KEYS && keyType != null
                    || kind != CrateRewardKind.VANILLA_ITEM && material != null
                    || kind != CrateRewardKind.GENERATORS && allGenerators) {
                throw new IllegalArgumentException("Unexpected crate reward definition field: " + id);
            }
        }
    }

    /** Ordered weighted pool with stable configuration identifiers. */
    public record Pool(List<RewardDefinition> rewards) {
        public Pool {
            rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
            if (rewards.isEmpty()) {
                throw new IllegalArgumentException("Crate reward pool must not be empty");
            }
            long unique = rewards.stream().map(RewardDefinition::id).distinct().count();
            if (unique != rewards.size()) {
                throw new IllegalArgumentException("Duplicate crate reward definition id");
            }
        }

        public int totalWeight() {
            return rewards.stream().mapToInt(RewardDefinition::weight).sum();
        }
    }
}
