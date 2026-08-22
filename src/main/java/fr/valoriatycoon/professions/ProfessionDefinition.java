package fr.valoriatycoon.professions;

/** Configurable presentation and reward rate of one permanent profession. */
public record ProfessionDefinition(
        ProfessionType type,
        String displayName,
        long experiencePerAction
) {
    public ProfessionDefinition {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Profession display name must not be blank");
        }
        if (experiencePerAction < 1) {
            throw new IllegalArgumentException("Profession experience per action must be positive");
        }
    }
}
