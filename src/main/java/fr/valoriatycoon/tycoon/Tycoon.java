package fr.valoriatycoon.tycoon;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;

/** Immutable persisted Tycoon aggregate root for the current MVP slice. */
public record Tycoon(
        UUID id,
        UUID ownerId,
        String groupId,
        String worldName,
        int plotIndex,
        TycoonPlotGroup.Bounds bounds,
        int floorY,
        int buildMinimumY,
        int buildMaximumY,
        int level,
        int prestige,
        int plotSizeLevel,
        int hopperLimitLevel,
        int memberLimitLevel,
        long progressPoints,
        long totalProduction,
        long playtimeSeconds,
        TycoonStatus status,
        Instant createdAt
) {
    public boolean contains(int x, int y, int z) {
        return bounds.contains(x, z) && y >= buildMinimumY && y <= buildMaximumY;
    }

    public boolean containsHorizontal(int x, int z) {
        return bounds.contains(x, z);
    }

    public Location home(World world) {
        if (!world.getName().equals(worldName)) {
            throw new IllegalArgumentException("Home world mismatch for Tycoon " + id);
        }
        return new Location(world, bounds.centerX() + 0.5, floorY + 1.0, bounds.centerZ() + 0.5, 0F, 0F);
    }

    public Tycoon withRank(int rankLevel) {
        return new Tycoon(
                id, ownerId, groupId, worldName, plotIndex, bounds,
                floorY, buildMinimumY, buildMaximumY,
                level, rankLevel, plotSizeLevel, hopperLimitLevel, memberLimitLevel,
                progressPoints, totalProduction, playtimeSeconds,
                status, createdAt
        );
    }

    public Tycoon withUpgradeLevels(int plotSize, int hoppers, int members) {
        return new Tycoon(
                id, ownerId, groupId, worldName, plotIndex, bounds,
                floorY, buildMinimumY, buildMaximumY,
                level, prestige, plotSize, hoppers, members,
                progressPoints, totalProduction, playtimeSeconds,
                status, createdAt
        );
    }

    public Tycoon withStatus(TycoonStatus newStatus) {
        return new Tycoon(
                id, ownerId, groupId, worldName, plotIndex, bounds,
                floorY, buildMinimumY, buildMaximumY,
                level, prestige, plotSizeLevel, hopperLimitLevel, memberLimitLevel,
                progressPoints, totalProduction, playtimeSeconds,
                newStatus, createdAt
        );
    }
}
