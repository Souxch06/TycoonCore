package fr.valoriatycoon.api.economy;

/** Describes the authoritative result of an economy mutation. */
public enum EconomyTransactionStatus {
    SUCCESS,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    SAME_ACCOUNT,
    BALANCE_OVERFLOW,
    SERVICE_UNAVAILABLE,
    STORAGE_ERROR
}
