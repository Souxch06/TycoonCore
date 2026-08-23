package fr.valoriatycoon.crates;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One server-issued physical key with a globally unique authenticated identity. */
public record CrateKey(
        UUID keyId,
        CrateType type,
        UUID issuedTo,
        String source,
        String sourceReference,
        boolean delivered,
        Instant issuedAt
) {
    public CrateKey {
        keyId = Objects.requireNonNull(keyId, "keyId");
        type = Objects.requireNonNull(type, "type");
        issuedTo = Objects.requireNonNull(issuedTo, "issuedTo");
        source = Objects.requireNonNull(source, "source");
        sourceReference = Objects.requireNonNull(sourceReference, "sourceReference");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        if (source.isBlank() || sourceReference.isBlank()) {
            throw new IllegalArgumentException("Crate key source must not be blank");
        }
    }
}
