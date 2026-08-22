package fr.valoriatycoon.crates;

/** Authoritative result of consuming one generic reward token. */
public enum CrateClaimStatus {
    SUCCESS,
    REWARD_INVALID,
    REWARD_KIND_MISMATCH,
    REWARD_ALREADY_USED,
    BALANCE_OVERFLOW
}
