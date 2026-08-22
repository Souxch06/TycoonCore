package fr.valoriatycoon.pets;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Persisted owned-pet level, XP and active state. */
public record PetProfile(
        UUID playerId,
        String petId,
        int level,
        long experience,
        boolean chromatic,
        boolean active,
        Instant obtainedAt
) {
    public PetProfile {
        playerId = Objects.requireNonNull(playerId, "playerId");
        if (petId == null || petId.isBlank() || level < 1 || experience < 0L) {
            throw new IllegalArgumentException("Invalid pet profile");
        }
        obtainedAt = Objects.requireNonNull(obtainedAt, "obtainedAt");
    }

    /** Returns this profile with a changed active state. */
    public PetProfile withActive(boolean active) {
        return new PetProfile(playerId, petId, level, experience, chromatic, active, obtainedAt);
    }

    /** Returns this profile with updated level progress. */
    public PetProfile withProgress(int level, long experience) {
        return new PetProfile(playerId, petId, level, experience, chromatic, active, obtainedAt);
    }
}
