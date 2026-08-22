package fr.valoriatycoon.farm.autosell;

/**
 * Result committed by SQLite when purchasing the next auto-sell level.
 *
 * @param status purchase outcome
 * @param profile resulting profile
 * @param chargedCents price charged, or next required price when funds were insufficient
 * @param balanceCents authoritative resulting account balance
 */
public record AutoSellPurchaseResult(
        AutoSellPurchaseStatus status,
        AutoSellProfile profile,
        long chargedCents,
        long balanceCents
) {
    public boolean successful() {
        return status == AutoSellPurchaseStatus.SUCCESS;
    }
}
