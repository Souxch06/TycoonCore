package fr.valoriatycoon.tools;

/** Atomic capability purchase result with both resulting balances. */
public record ToolUpgradeResult(
        ToolUpgradeStatus status,
        ToolType toolType,
        ToolCapability capability,
        ToolUpgradeCurrency currency,
        int resultingLevel,
        long chargedAmount,
        long balanceCents,
        long toolCoins
) {
    public boolean successful() {
        return status == ToolUpgradeStatus.SUCCESS;
    }
}
