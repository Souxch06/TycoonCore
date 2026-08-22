package fr.valoriatycoon.crates;

/** Atomic key-consumption and immutable reward-issuance result. */
public record CrateOpenResult(CrateOpenStatus status, CrateReward reward) {
    public boolean successful() {
        return status == CrateOpenStatus.SUCCESS && reward != null;
    }
}
