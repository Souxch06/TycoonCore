package fr.valoriatycoon.pets;

import java.math.BigDecimal;
import java.util.Objects;

/** Base pet effect and additional value gained for each level after level one. */
public record PetBoost(BigDecimal base, BigDecimal perLevel) {
    public PetBoost {
        base = Objects.requireNonNull(base, "base");
        perLevel = Objects.requireNonNull(perLevel, "perLevel");
        if (base.signum() < 0 || perLevel.signum() < 0) {
            throw new IllegalArgumentException("Pet boosts must not be negative");
        }
    }

    /** Returns the effect value at the supplied owned-pet level. */
    public BigDecimal atLevel(int level) {
        if (level < 1) {
            return BigDecimal.ZERO;
        }
        return base.add(perLevel.multiply(BigDecimal.valueOf(level - 1L)));
    }
}
