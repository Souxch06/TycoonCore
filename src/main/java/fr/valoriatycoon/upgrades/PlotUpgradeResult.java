package fr.valoriatycoon.upgrades;

/** Atomic plot upgrade and shared-balance debit result. */
public record PlotUpgradeResult(
        PlotUpgradeStatus status,
        PlotUpgradeType type,
        int resultingLevel,
        long chargedCents,
        long balanceCents
) {
    public boolean successful() {
        return status == PlotUpgradeStatus.SUCCESS;
    }
}
