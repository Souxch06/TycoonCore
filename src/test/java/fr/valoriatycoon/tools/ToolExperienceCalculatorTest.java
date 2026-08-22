package fr.valoriatycoon.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolExperienceCalculatorTest {
    private final ToolSettings.Progression progression = new ToolSettings.Progression(
            10,
            100L,
            new BigDecimal("2.00"),
            20
    );

    @Test
    void appliesIncreasingRequirementsAndCarriesExperience() {
        assertEquals(100L, ToolExperienceCalculator.requiredForNextLevel(1, progression));
        assertEquals(200L, ToolExperienceCalculator.requiredForNextLevel(2, progression));
        assertEquals(new ToolExperienceCalculator.Progress(2, 150L),
                ToolExperienceCalculator.add(1, 0L, 250L, progression));
        assertEquals(new ToolExperienceCalculator.Progress(3, 0L),
                ToolExperienceCalculator.add(1, 0L, 300L, progression));
    }

    @Test
    void appliesAdditionalDifficultyAtConfiguredMilestones() {
        ToolSettings.Progression tiered = new ToolSettings.Progression(
                100,
                100L,
                BigDecimal.ONE,
                List.of(
                        new ToolSettings.Progression.ExperienceTier(1, BigDecimal.ONE),
                        new ToolSettings.Progression.ExperienceTier(21, new BigDecimal("1.50")),
                        new ToolSettings.Progression.ExperienceTier(41, new BigDecimal("2.25")),
                        new ToolSettings.Progression.ExperienceTier(61, new BigDecimal("3.50")),
                        new ToolSettings.Progression.ExperienceTier(81, new BigDecimal("5.00"))
                ),
                20
        );

        assertEquals(100L, ToolExperienceCalculator.requiredForNextLevel(20, tiered));
        assertEquals(150L, ToolExperienceCalculator.requiredForNextLevel(21, tiered));
        assertEquals(225L, ToolExperienceCalculator.requiredForNextLevel(41, tiered));
        assertEquals(350L, ToolExperienceCalculator.requiredForNextLevel(61, tiered));
        assertEquals(500L, ToolExperienceCalculator.requiredForNextLevel(81, tiered));
        assertEquals(new ToolExperienceCalculator.Progress(21, 149L),
                ToolExperienceCalculator.add(20, 0L, 249L, tiered));
    }
}
