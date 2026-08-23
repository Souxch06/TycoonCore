package fr.valoriatycoon.professions;

/** Current permanent level and experience of one profession. */
public record ProfessionProfile(
        ProfessionType type,
        int level,
        long experience
) {
    public ProfessionProfile {
        if (level < 1 || experience < 0) {
            throw new IllegalArgumentException("Invalid profession progression");
        }
    }
}
