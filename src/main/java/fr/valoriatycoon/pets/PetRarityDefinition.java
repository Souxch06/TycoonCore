package fr.valoriatycoon.pets;

import java.math.BigDecimal;
import java.util.Objects;

/** Level cap, XP curve and visual label shared by one pet rarity. */
public record PetRarityDefinition(
        PetRarity rarity,
        String displayName,
        int maximumLevel,
        long baseExperience,
        BigDecimal experienceGrowth
) {
    public PetRarityDefinition {
        rarity = Objects.requireNonNull(rarity, "rarity");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Pet rarity display name must not be blank");
        }
        if (maximumLevel < 1 || maximumLevel > 1_000
                || baseExperience < 1L
                || experienceGrowth == null
                || experienceGrowth.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Invalid pet rarity progression");
        }
    }
}
