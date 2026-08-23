package fr.valoriatycoon.farm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.valoriatycoon.farm.autosell.AutoSellValueCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AutoSellValueCalculatorTest {
    @Test
    void appliesConfiguredLevelMultipliersExactly() {
        assertEquals(125L, AutoSellValueCalculator.apply(125L, new BigDecimal("1.00")));
        assertEquals(250L, AutoSellValueCalculator.apply(125L, new BigDecimal("2.00")));
        assertEquals(625L, AutoSellValueCalculator.apply(125L, new BigDecimal("5.00")));
    }

    @Test
    void roundsFractionalCentsDownAndRejectsOverflow() {
        assertEquals(187L, AutoSellValueCalculator.apply(125L, new BigDecimal("1.50")));
        assertThrows(
                IllegalArgumentException.class,
                () -> AutoSellValueCalculator.apply(Long.MAX_VALUE, new BigDecimal("5"))
        );
    }
}
