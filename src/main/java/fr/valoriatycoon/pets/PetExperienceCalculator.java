package fr.valoriatycoon.pets;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Overflow-safe exponential XP progression for rarity-dependent pet levels. */
public final class PetExperienceCalculator {
    private PetExperienceCalculator() {
    }

    /** Returns XP needed to leave the supplied level, or zero at the cap. */
    public static long requiredForNextLevel(int level, PetRarityDefinition rarity) {
        if (level < 1 || level >= rarity.maximumLevel()) {
            return 0L;
        }
        try {
            return BigDecimal.valueOf(rarity.baseExperience())
                    .multiply(rarity.experienceGrowth().pow(level - 1))
                    .setScale(0, RoundingMode.CEILING)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    /** Applies XP while carrying overflow across as many levels as necessary. */
    public static Progress add(
            int currentLevel,
            long currentExperience,
            long gainedExperience,
            PetRarityDefinition rarity
    ) {
        if (currentLevel < 1 || currentExperience < 0L || gainedExperience < 0L) {
            throw new IllegalArgumentException("Invalid pet experience progress");
        }
        int level = Math.min(currentLevel, rarity.maximumLevel());
        long experience = saturatingAdd(currentExperience, gainedExperience);
        while (level < rarity.maximumLevel()) {
            long required = requiredForNextLevel(level, rarity);
            if (required <= 0L || experience < required) {
                break;
            }
            experience -= required;
            level++;
        }
        if (level >= rarity.maximumLevel()) {
            experience = 0L;
        }
        return new Progress(level, experience);
    }

    private static long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public record Progress(int level, long experience) {
    }
}
