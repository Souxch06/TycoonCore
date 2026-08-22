package fr.valoriatycoon.tycoon;

import java.util.UUID;

/** Extension point for future rank, permission, upgrade or shop-based flight acquisition. */
@FunctionalInterface
public interface TycoonFlightAccessPolicy {
    boolean canFly(UUID playerId, Tycoon tycoon, boolean owner, boolean trustedMember);
}
