package fr.valoriatycoon.ranks;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.function.ToIntFunction;

/** Resolves cumulative permanent benefits from the current cached medieval rank. */
public final class RankBenefitService {
    private final RankSettings settings;
    private final ToIntFunction<UUID> rankLookup;

    /** Creates a resolver backed by rank configuration and a non-blocking cached rank lookup. */
    public RankBenefitService(RankSettings settings, ToIntFunction<UUID> rankLookup) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.rankLookup = Objects.requireNonNull(rankLookup, "rankLookup");
    }

    /** Returns the cumulative multiplier applied to money earned from production. */
    public BigDecimal revenueMultiplier(UUID playerId) {
        return multiplier(playerId, RankRequirement::permanentRevenueBonus);
    }

    /** Returns the cumulative multiplier applied to multi-tool experience. */
    public BigDecimal toolExperienceMultiplier(UUID playerId) {
        return multiplier(playerId, RankRequirement::toolExperienceBonus);
    }

    /** Returns the cumulative multiplier applied to permanent profession experience. */
    public BigDecimal professionExperienceMultiplier(UUID playerId) {
        return multiplier(playerId, RankRequirement::professionExperienceBonus);
    }

    /** Returns the cumulative multiplier applied to each tool's dedicated coins. */
    public BigDecimal toolCoinMultiplier(UUID playerId) {
        return multiplier(playerId, RankRequirement::toolCoinBonus);
    }

    /** Returns the cumulative multiplier applied to generated resource quantities. */
    public BigDecimal generatorProductionMultiplier(UUID playerId) {
        return multiplier(playerId, RankRequirement::generatorProductionBonus);
    }

    /** Returns additional generator slots granted by the current rank. */
    public int generatorSlotBonus(UUID playerId) {
        RankRequirement rank = current(playerId);
        return rank == null ? 0 : rank.generatorSlotBonus();
    }

    private BigDecimal multiplier(UUID playerId, Bonus bonus) {
        RankRequirement rank = current(playerId);
        return rank == null
                ? BigDecimal.ONE
                : BigDecimal.ONE.add(BigDecimal.valueOf(bonus.value(rank)));
    }

    private RankRequirement current(UUID playerId) {
        int level = rankLookup.applyAsInt(playerId);
        return level <= 0 ? null : settings.level(level).orElse(null);
    }

    @FunctionalInterface
    private interface Bonus {
        double value(RankRequirement requirement);
    }
}
