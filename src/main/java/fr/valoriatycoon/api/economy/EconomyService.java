package fr.valoriatycoon.api.economy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Public asynchronous economy contract.
 *
 * <p>Mutations are persisted before their returned stage completes. Implementations must never
 * perform storage I/O on the calling thread. Amounts are represented as {@link BigDecimal} to
 * avoid floating-point rounding and are limited to two decimal places by ValoriaTycoon.</p>
 */
public interface EconomyService {

    /** Returns the persisted or safely cached balance for an account. */
    CompletionStage<BigDecimal> getBalance(UUID playerId);

    /** Credits an account after server-side validation. */
    CompletionStage<EconomyTransactionResult> addMoney(UUID playerId, BigDecimal amount, String reason);

    /** Debits an account if sufficient funds are available. */
    CompletionStage<EconomyTransactionResult> removeMoney(UUID playerId, BigDecimal amount, String reason);

    /** Replaces an account balance after server-side validation. */
    CompletionStage<EconomyTransactionResult> setBalance(UUID playerId, BigDecimal amount, String reason);

    /** Atomically transfers money between two different accounts. */
    CompletionStage<EconomyTransactionResult> transfer(
            UUID sourcePlayerId,
            UUID targetPlayerId,
            BigDecimal amount,
            String reason
    );

    /** Convenience overload intended for integrations receiving a finite {@code double}. */
    default CompletionStage<EconomyTransactionResult> addMoney(UUID playerId, double amount) {
        return addMoney(playerId, decimal(amount), "api:add");
    }

    /** Convenience overload intended for integrations receiving a finite {@code double}. */
    default CompletionStage<EconomyTransactionResult> removeMoney(UUID playerId, double amount) {
        return removeMoney(playerId, decimal(amount), "api:remove");
    }

    /** Convenience overload intended for integrations receiving a finite {@code double}. */
    default CompletionStage<EconomyTransactionResult> setBalance(UUID playerId, double amount) {
        return setBalance(playerId, decimal(amount), "api:set");
    }

    /** Indicates whether the backing storage is ready to accept operations. */
    boolean isAvailable();

    private static BigDecimal decimal(double amount) {
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("The amount must be finite");
        }
        return BigDecimal.valueOf(amount);
    }
}
