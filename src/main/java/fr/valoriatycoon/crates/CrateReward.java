package fr.valoriatycoon.crates;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A committed no-reroll crate reward represented by one uniquely identified physical token. */
public record CrateReward(
        UUID rewardId,
        UUID keyId,
        CrateType crateType,
        String definitionId,
        CrateRewardKind kind,
        CrateRewardPayload payload,
        UUID issuedTo,
        boolean delivered,
        UUID consumedBy,
        boolean claimDelivered,
        Instant issuedAt
) {
    public CrateReward {
        rewardId = Objects.requireNonNull(rewardId, "rewardId");
        keyId = Objects.requireNonNull(keyId, "keyId");
        crateType = Objects.requireNonNull(crateType, "crateType");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        kind = Objects.requireNonNull(kind, "kind");
        payload = Objects.requireNonNull(payload, "payload");
        issuedTo = Objects.requireNonNull(issuedTo, "issuedTo");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        if (definitionId.isBlank() || claimDelivered && consumedBy == null) {
            throw new IllegalArgumentException("Invalid crate reward delivery state");
        }
    }

    public boolean consumed() {
        return consumedBy != null;
    }
}
