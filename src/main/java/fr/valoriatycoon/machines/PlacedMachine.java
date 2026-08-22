package fr.valoriatycoon.machines;

import java.time.Instant;
import java.util.UUID;

/** Immutable persisted resource generator instance. */
public record PlacedMachine(
        UUID id,
        UUID tycoonId,
        UUID ownerId,
        String machineType,
        String worldName,
        int x,
        int y,
        int z,
        long storedAmount,
        boolean autoSell,
        int speedLevel,
        int sellPriceLevel,
        long nextRunAtMillis,
        Instant createdAt
) {
    public MachinePosition position() { return new MachinePosition(worldName, x, y, z); }

    public PlacedMachine afterCycle(long stored, long nextRun) {
        return new PlacedMachine(
                id, tycoonId, ownerId, machineType, worldName, x, y, z,
                stored, autoSell, speedLevel, sellPriceLevel, nextRun, createdAt
        );
    }

    public PlacedMachine withAutoSell(boolean enabled) {
        return new PlacedMachine(
                id, tycoonId, ownerId, machineType, worldName, x, y, z,
                storedAmount, enabled, speedLevel, sellPriceLevel, nextRunAtMillis, createdAt
        );
    }

    public PlacedMachine withStoredAmount(long amount) {
        return new PlacedMachine(
                id, tycoonId, ownerId, machineType, worldName, x, y, z,
                amount, autoSell, speedLevel, sellPriceLevel, nextRunAtMillis, createdAt
        );
    }

    public PlacedMachine withUpgrade(MachineUpgradeType type, int level) {
        return new PlacedMachine(
                id, tycoonId, ownerId, machineType, worldName, x, y, z,
                storedAmount, autoSell,
                type == MachineUpgradeType.SPEED ? level : speedLevel,
                type == MachineUpgradeType.SELL_PRICE ? level : sellPriceLevel,
                nextRunAtMillis, createdAt
        );
    }
}
