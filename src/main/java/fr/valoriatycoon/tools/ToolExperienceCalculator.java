package fr.valoriatycoon.tools;

import fr.valoriatycoon.progression.LevelExperienceCalculator;

/** Exact deterministic tool-level curve calculations. */
public final class ToolExperienceCalculator {
    private ToolExperienceCalculator() {
    }

    public static long requiredForNextLevel(int currentLevel, ToolSettings.Progression progression) {
        return LevelExperienceCalculator.requiredForNextLevel(currentLevel, progression);
    }

    public static Progress add(
            int currentLevel,
            long currentExperience,
            long addedExperience,
            ToolSettings.Progression progression
    ) {
        LevelExperienceCalculator.Progress result = LevelExperienceCalculator.add(
                currentLevel,
                currentExperience,
                addedExperience,
                progression
        );
        return new Progress(result.level(), result.experience());
    }

    public record Progress(int level, long experience) {
    }
}
