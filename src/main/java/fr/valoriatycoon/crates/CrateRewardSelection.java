package fr.valoriatycoon.crates;

import java.util.Objects;

/** One fully resolved server-side draw ready for immutable SQLite persistence. */
public record CrateRewardSelection(
        String definitionId,
        CrateRewardKind kind,
        CrateRewardPayload payload
) {
    public CrateRewardSelection {
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        kind = Objects.requireNonNull(kind, "kind");
        payload = Objects.requireNonNull(payload, "payload");
        if (!definitionId.matches("[a-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid crate reward definition id");
        }
    }
}
