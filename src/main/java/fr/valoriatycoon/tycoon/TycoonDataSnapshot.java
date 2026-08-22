package fr.valoriatycoon.tycoon;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Startup snapshot loaded in one ordered database operation. */
public record TycoonDataSnapshot(
        List<Tycoon> tycoons,
        Map<UUID, Set<UUID>> membersByTycoon,
        Map<UUID, Set<HopperPosition>> hoppersByTycoon
) {
}
