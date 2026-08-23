package fr.valoriatycoon.machines;

/** Database-committed generator cycle result. */
public record MachineCycleResult(
        MachineCycleStatus status,
        PlacedMachine machine,
        long creditedMoneyCents,
        long ownerBalanceCents
) {
}
