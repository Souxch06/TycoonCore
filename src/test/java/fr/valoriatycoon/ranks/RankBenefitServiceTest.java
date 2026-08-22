package fr.valoriatycoon.ranks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RankBenefitServiceTest {

    @Test
    void resolvesCumulativeMultipliersAndGeneratorSlotsFromCachedRank() {
        RankRequirement first = rank(1, 0.02, 0.05, 0.06, 0.03, 0.04, 1);
        RankRequirement second = rank(2, 0.08, 0.12, 0.14, 0.10, 0.15, 4);
        AtomicInteger currentRank = new AtomicInteger();
        RankBenefitService benefits = new RankBenefitService(
                new RankSettings(List.of(first, second)),
                ignored -> currentRank.get()
        );
        UUID playerId = UUID.randomUUID();

        assertEquals(BigDecimal.ONE, benefits.revenueMultiplier(playerId));
        assertEquals(0, benefits.generatorSlotBonus(playerId));

        currentRank.set(2);
        assertEquals(new BigDecimal("1.08"), benefits.revenueMultiplier(playerId));
        assertEquals(new BigDecimal("1.12"), benefits.toolExperienceMultiplier(playerId));
        assertEquals(new BigDecimal("1.14"), benefits.professionExperienceMultiplier(playerId));
        assertEquals(new BigDecimal("1.1"), benefits.toolCoinMultiplier(playerId));
        assertEquals(new BigDecimal("1.15"), benefits.generatorProductionMultiplier(playerId));
        assertEquals(4, benefits.generatorSlotBonus(playerId));
    }

    private RankRequirement rank(
            int level,
            double revenue,
            double toolExperience,
            double professionExperience,
            double toolCoins,
            double generatorProduction,
            int generatorSlots
    ) {
        return new RankRequirement(
                level,
                "Rank " + level,
                0L,
                0L,
                0,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                revenue,
                toolExperience,
                professionExperience,
                toolCoins,
                generatorProduction,
                generatorSlots
        );
    }
}
