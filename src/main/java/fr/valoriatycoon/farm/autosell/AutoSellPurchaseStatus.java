package fr.valoriatycoon.farm.autosell;

/** Authoritative result of an atomic auto-sell level purchase. */
public enum AutoSellPurchaseStatus {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    MAXIMUM_LEVEL,
    PROFILE_STALE
}
