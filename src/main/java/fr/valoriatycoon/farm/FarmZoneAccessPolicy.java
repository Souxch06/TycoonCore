package fr.valoriatycoon.farm;

import java.util.UUID;

/** Non-blocking rank lookup used to secure public farm zones. */
@FunctionalInterface
public interface FarmZoneAccessPolicy {
    int currentRank(UUID playerId);

    default boolean canAccess(UUID playerId, FarmZoneDefinition zone) {
        return currentRank(playerId) >= zone.requiredRank();
    }
}
