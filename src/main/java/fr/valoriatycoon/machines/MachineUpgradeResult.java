package fr.valoriatycoon.machines;

/** Atomic money-only generator upgrade result. */
public record MachineUpgradeResult(
        MachineUpgradeStatus status,
        PlacedMachine machine,
        long chargedCents,
        long resultingBalanceCents
) {
    public boolean successful() { return status == MachineUpgradeStatus.SUCCESS; }
}
