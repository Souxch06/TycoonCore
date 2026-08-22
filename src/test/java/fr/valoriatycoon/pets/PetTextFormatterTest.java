package fr.valoriatycoon.pets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PetTextFormatterTest {

    @Test
    void formatsExactPercentagesWithoutFloatingPointRounding() {
        assertEquals("1", PetTextFormatter.percentage(new BigDecimal("0.01")));
        assertEquals("12.5", PetTextFormatter.percentage(new BigDecimal("0.125")));
    }

    @Test
    void exposesFrenchEffectLabelsUsedByEveryPetTooltip() {
        assertEquals("revenus", PetTextFormatter.effectName(PetEffect.MONEY));
        assertEquals(
                "chance de double production",
                PetTextFormatter.effectName(PetEffect.DOUBLE_GENERATOR_OUTPUT_CHANCE)
        );
    }
}
