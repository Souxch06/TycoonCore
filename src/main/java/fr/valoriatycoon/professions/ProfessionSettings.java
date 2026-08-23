package fr.valoriatycoon.professions;

import fr.valoriatycoon.progression.ExperienceCurve;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable professions.yml snapshot. */
public record ProfessionSettings(
        Progression progression,
        Map<ProfessionType, ProfessionDefinition> professions
) {
    public ProfessionSettings {
        EnumMap<ProfessionType, ProfessionDefinition> copy = new EnumMap<>(ProfessionType.class);
        copy.putAll(professions);
        if (copy.size() != ProfessionType.values().length) {
            throw new IllegalArgumentException("Every profession must be configured");
        }
        professions = Collections.unmodifiableMap(copy);
    }

    /** Returns the required configured definition for a profession. */
    public ProfessionDefinition definition(ProfessionType type) {
        ProfessionDefinition definition = professions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Missing profession definition for " + type);
        }
        return definition;
    }

    /** Exponential profession-level curve. */
    public record Progression(
            int maximumLevel,
            long baseExperience,
            BigDecimal experienceMultiplier,
            int flushIntervalTicks
    ) implements ExperienceCurve {
        public Progression {
            if (maximumLevel < 1 || baseExperience < 1 || flushIntervalTicks < 1) {
                throw new IllegalArgumentException("Profession progression values must be positive");
            }
            if (experienceMultiplier == null
                    || experienceMultiplier.compareTo(BigDecimal.ONE) <= 0) {
                throw new IllegalArgumentException(
                        "Profession experience multiplier must be greater than 1"
                );
            }
        }

        @Override
        public BigDecimal difficultyMultiplier(int level) {
            if (level < 1) {
                throw new IllegalArgumentException("Profession level must be positive");
            }
            return BigDecimal.ONE;
        }
    }
}
