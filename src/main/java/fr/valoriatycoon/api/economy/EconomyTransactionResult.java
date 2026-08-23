package fr.valoriatycoon.api.economy;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable result returned by economy mutations.
 *
 * @param status authoritative outcome
 * @param amount validated transaction amount
 * @param sourceBalance source/account balance after the operation, when known
 * @param targetBalance target balance after a transfer, when applicable
 */
public record EconomyTransactionResult(
        EconomyTransactionStatus status,
        BigDecimal amount,
        BigDecimal sourceBalance,
        BigDecimal targetBalance
) {
    public EconomyTransactionResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(amount, "amount");
    }

    /** Returns {@code true} only when storage committed the mutation. */
    public boolean successful() {
        return status == EconomyTransactionStatus.SUCCESS;
    }
}
