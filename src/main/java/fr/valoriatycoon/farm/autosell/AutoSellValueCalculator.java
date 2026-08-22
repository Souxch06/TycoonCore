package fr.valoriatycoon.farm.autosell;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact cent-based multiplier calculation shared by all farm sale sources. */
public final class AutoSellValueCalculator {
    private AutoSellValueCalculator() {
    }

    public static long apply(long baseValueCents, BigDecimal multiplier) {
        if (baseValueCents < 0) {
            throw new IllegalArgumentException("Base sale value cannot be negative");
        }
        if (multiplier == null || multiplier.signum() <= 0) {
            throw new IllegalArgumentException("Sale multiplier must be positive");
        }
        try {
            return BigDecimal.valueOf(baseValueCents)
                    .multiply(multiplier)
                    .setScale(0, RoundingMode.DOWN)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Multiplied auto-sell value exceeds storage capacity", exception);
        }
    }
}
