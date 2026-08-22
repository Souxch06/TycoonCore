package fr.valoriatycoon.tools;

/** Result of an atomic non-capability tool-coin purchase. */
public record ToolCoinSpendResult(
        ToolCoinSpendStatus status,
        ToolType toolType,
        long chargedCoins,
        long resultingCoins
) {
    public boolean successful() {
        return status == ToolCoinSpendStatus.SUCCESS;
    }
}
