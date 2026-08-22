package fr.valoriatycoon.progression;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact, deterministic and overflow-safe calculations for level-based experience. */
public final class LevelExperienceCalculator {
    private static final BigDecimal LONG_MAXIMUM = BigDecimal.valueOf(Long.MAX_VALUE);

    private LevelExperienceCalculator() {
    }

    /** Returns the experience needed to leave {@code currentLevel}, or zero at the cap. */
    public static long requiredForNextLevel(int currentLevel, ExperienceCurve curve) {
        if (currentLevel >= curve.maximumLevel()) {
            return 0L;
        }
        if (currentLevel < 1) {
            throw new IllegalArgumentException("Current level must be positive");
        }
        BigDecimal required = BigDecimal.valueOf(curve.baseExperience());
        for (int level = 1; level < currentLevel; level++) {
            required = required.multiply(curve.experienceMultiplier());
            if (required.compareTo(LONG_MAXIMUM) >= 0) {
                return Long.MAX_VALUE;
            }
        }
        required = required.multiply(curve.difficultyMultiplier(currentLevel));
        if (required.compareTo(LONG_MAXIMUM) >= 0) {
            return Long.MAX_VALUE;
        }
        return required.setScale(0, RoundingMode.CEILING).longValueExact();
    }

    /** Adds experience, applies every crossed level and preserves the remainder. */
    public static Progress add(
            int currentLevel,
            long currentExperience,
            long addedExperience,
            ExperienceCurve curve
    ) {
        if (addedExperience < 0 || currentExperience < 0) {
            throw new IllegalArgumentException("Experience cannot be negative");
        }
        long experience;
        try {
            experience = Math.addExact(currentExperience, addedExperience);
        } catch (ArithmeticException exception) {
            experience = Long.MAX_VALUE;
        }
        int level = currentLevel;
        while (level < curve.maximumLevel()) {
            long required = requiredForNextLevel(level, curve);
            if (required <= 0 || experience < required) {
                break;
            }
            experience -= required;
            level++;
        }
        if (level >= curve.maximumLevel()) {
            experience = 0L;
        }
        return new Progress(level, experience);
    }

    /** Result of one batched experience application. */
    public record Progress(int level, long experience) {
    }
}
