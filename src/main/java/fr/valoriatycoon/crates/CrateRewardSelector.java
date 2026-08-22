package fr.valoriatycoon.crates;

import fr.valoriatycoon.tools.ToolType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/** Resolves every random field once before the key/reward transaction is committed. */
public final class CrateRewardSelector {
    private final CrateRewardSettings settings;
    private final ToIntFunction<UUID> rankProvider;
    private final List<String> machineTypes;

    public CrateRewardSelector(
            CrateRewardSettings settings,
            ToIntFunction<UUID> rankProvider,
            List<String> machineTypes
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.rankProvider = Objects.requireNonNull(rankProvider, "rankProvider");
        this.machineTypes = machineTypes.stream().sorted().toList();
        if (this.machineTypes.isEmpty()
                || this.machineTypes.stream().anyMatch(type -> !type.matches("[a-z0-9_-]{1,32}"))) {
            throw new IllegalArgumentException("Crate generator rewards require valid machine types");
        }
    }

    public CrateRewardSelection select(UUID playerId, CrateType crateType) {
        return select(playerId, crateType, ThreadLocalRandom.current());
    }

    /** Exposed for deterministic tests; production callers use the server random generator. */
    public CrateRewardSelection select(
            UUID playerId,
            CrateType crateType,
            RandomGenerator random
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(crateType, "crateType");
        Objects.requireNonNull(random, "random");
        CrateRewardSettings.RewardDefinition definition = drawDefinition(crateType, random);
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("count", Integer.toString(definition.count()));
        switch (definition.kind()) {
            case MONEY_BAG -> {
                long amount = total(settings.moneyTier(definition.tier()), definition.count(), random);
                payload.put("amount_cents", Long.toString(amount));
                payload.put("tier", Integer.toString(definition.tier()));
            }
            case COIN_BAG -> {
                ToolType tool = definition.toolType() == null ? randomTool(random) : definition.toolType();
                long amount = total(settings.coinTier(definition.tier()), definition.count(), random);
                payload.put("amount", Long.toString(amount));
                payload.put("tier", Integer.toString(definition.tier()));
                payload.put("tool", tool.name());
            }
            case UNIVERSAL_COIN_BAG -> {
                long amount = total(settings.universalCoinBag(), definition.count(), random);
                payload.put("amount_each", Long.toString(amount));
            }
            case XP_VIAL -> {
                long levels = total(settings.experienceTier(definition.tier()), definition.count(), random);
                payload.put("levels", Long.toString(levels));
                payload.put("tier", Integer.toString(definition.tier()));
            }
            case RESOURCE_BUNDLE -> {
                List<CrateRewardSettings.Resource> eligible = settings.resources().stream()
                        .filter(resource -> resource.requiredRank() <= rankProvider.applyAsInt(playerId))
                        .toList();
                if (eligible.isEmpty()) {
                    throw new IllegalStateException("No crate bundle resource is available for rank zero");
                }
                CrateRewardSettings.Resource resource = eligible.get(random.nextInt(eligible.size()));
                long amount = total(settings.resourceTier(definition.tier()), definition.count(), random);
                payload.put("amount", Long.toString(amount));
                payload.put("material", resource.material().name());
                payload.put("tier", Integer.toString(definition.tier()));
            }
            case VANILLA_ITEM -> {
                payload.put("amount", Integer.toString(definition.count()));
                payload.put("material", definition.material().name());
            }
            case CRATE_KEYS -> {
                payload.put("amount", Integer.toString(definition.count()));
                payload.put("crate_type", definition.keyType().name());
            }
            case PET_KEYS -> payload.put("amount", Integer.toString(definition.count()));
            case GENERATORS -> {
                List<String> selected = new ArrayList<>();
                if (definition.allGenerators()) {
                    for (int repetition = 0; repetition < definition.count(); repetition++) {
                        selected.addAll(machineTypes);
                    }
                } else {
                    for (int index = 0; index < definition.count(); index++) {
                        selected.add(machineTypes.get(random.nextInt(machineTypes.size())));
                    }
                }
                payload.put("amount", Integer.toString(selected.size()));
                payload.put("types", String.join(",", selected));
            }
        }
        return new CrateRewardSelection(
                definition.id(),
                definition.kind(),
                new CrateRewardPayload(payload)
        );
    }

    public boolean broadcasts(CrateType crateType, String definitionId) {
        return settings.pool(crateType).rewards().stream()
                .filter(reward -> reward.id().equals(definitionId))
                .findFirst()
                .map(CrateRewardSettings.RewardDefinition::broadcast)
                .orElse(false);
    }

    private CrateRewardSettings.RewardDefinition drawDefinition(
            CrateType type,
            RandomGenerator random
    ) {
        int selected = random.nextInt(CrateRewardSettings.POOL_WEIGHT);
        for (CrateRewardSettings.RewardDefinition definition : settings.pool(type).rewards()) {
            if (selected < definition.weight()) {
                return definition;
            }
            selected -= definition.weight();
        }
        throw new IllegalStateException("Crate reward weights did not select a definition");
    }

    private ToolType randomTool(RandomGenerator random) {
        ToolType[] values = EnumSet.allOf(ToolType.class).toArray(ToolType[]::new);
        return values[random.nextInt(values.length)];
    }

    private long total(
            CrateRewardSettings.AmountRange range,
            int count,
            RandomGenerator random
    ) {
        long total = 0L;
        for (int index = 0; index < count; index++) {
            long amount = range.minimum() == range.maximum()
                    ? range.minimum()
                    : random.nextLong(range.minimum(), range.maximum() + 1L);
            total = Math.addExact(total, amount);
        }
        return total;
    }
}
