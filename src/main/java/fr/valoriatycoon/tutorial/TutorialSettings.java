package fr.valoriatycoon.tutorial;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable tutorial.yml snapshot. */
public record TutorialSettings(
        boolean enabled,
        int tickInterval,
        String noIslandActionBar,
        String stepActionBar,
        String readyActionBar,
        Map<TutorialStep, StepDefinition> steps
) {
    public TutorialSettings {
        if (tickInterval < 1) {
            throw new IllegalArgumentException("Tutorial tick interval must be positive");
        }
        noIslandActionBar = requireText(noIslandActionBar, "noIslandActionBar");
        stepActionBar = requireText(stepActionBar, "stepActionBar");
        readyActionBar = requireText(readyActionBar, "readyActionBar");
        EnumMap<TutorialStep, StepDefinition> copy = new EnumMap<>(TutorialStep.class);
        copy.putAll(steps);
        for (TutorialStep step : TutorialStep.values()) {
            if (step.actionable() && !copy.containsKey(step)) {
                throw new IllegalArgumentException("Missing tutorial step " + step);
            }
        }
        steps = Collections.unmodifiableMap(copy);
    }

    public StepDefinition step(TutorialStep step) {
        StepDefinition definition = steps.get(step);
        if (definition == null) {
            throw new IllegalArgumentException("Tutorial step has no objective: " + step);
        }
        return definition;
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /** Target, exact money reward and user-facing objective of one stage. */
    public record StepDefinition(
            TutorialStep step,
            long target,
            long rewardMoneyCents,
            String objective
    ) {
        public StepDefinition {
            step = Objects.requireNonNull(step, "step");
            if (!step.actionable() || target < 1 || rewardMoneyCents < 1) {
                throw new IllegalArgumentException("Invalid tutorial step definition for " + step);
            }
            objective = requireText(objective, "objective");
        }
    }
}
