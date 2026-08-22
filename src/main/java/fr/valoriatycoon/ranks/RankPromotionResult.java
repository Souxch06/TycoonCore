package fr.valoriatycoon.ranks;

public record RankPromotionResult(
        RankPromotionStatus status,
        int resultingRank,
        long resultingBalanceCents
) {
    public boolean successful() { return status == RankPromotionStatus.SUCCESS; }
}
