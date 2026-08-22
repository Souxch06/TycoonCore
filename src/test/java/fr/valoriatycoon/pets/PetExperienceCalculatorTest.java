package fr.valoriatycoon.pets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PetExperienceCalculatorTest {

    @Test
    void carriesExperienceAcrossLevelsAndStopsAtRarityCap() {
        PetRarityDefinition rarity = new PetRarityDefinition(
                PetRarity.COMMON,
                "Commun",
                3,
                100L,
                new BigDecimal("2.0")
        );

        assertEquals(100L, PetExperienceCalculator.requiredForNextLevel(1, rarity));
        assertEquals(200L, PetExperienceCalculator.requiredForNextLevel(2, rarity));
        assertEquals(new PetExperienceCalculator.Progress(2, 50L),
                PetExperienceCalculator.add(1, 0L, 150L, rarity));
        assertEquals(new PetExperienceCalculator.Progress(3, 0L),
                PetExperienceCalculator.add(1, 0L, 500L, rarity));
        assertEquals(0L, PetExperienceCalculator.requiredForNextLevel(3, rarity));
    }
}
