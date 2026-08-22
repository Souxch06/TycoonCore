package fr.valoriatycoon.pets;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Persisted uniquely identified pet egg with an immutable normal/chromatic variant. */
public record PetEgg(
        UUID eggId,
        String petId,
        boolean chromatic,
        int level,
        long experience,
        UUID recipientId,
        Instant issuedAt
) {
    public PetEgg {
        eggId = Objects.requireNonNull(eggId, "eggId");
        if (petId == null || petId.isBlank() || level < 1 || experience < 0L) {
            throw new IllegalArgumentException("Invalid persisted pet egg");
        }
        recipientId = Objects.requireNonNull(recipientId, "recipientId");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
