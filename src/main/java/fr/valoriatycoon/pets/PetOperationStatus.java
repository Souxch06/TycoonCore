package fr.valoriatycoon.pets;

/** Authoritative result of a pet crate, egg, activation or reclaim request. */
public enum PetOperationStatus {
    SUCCESS,
    NO_ACTIVE_ISLAND,
    RANK_LOCKED,
    NOT_OWNED,
    ALREADY_OWNED,
    INSUFFICIENT_FUNDS,
    KEY_ALREADY_USED,
    INVALID_EGG,
    EGG_ALREADY_USED
}
