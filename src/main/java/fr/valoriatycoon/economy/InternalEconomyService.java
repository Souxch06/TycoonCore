package fr.valoriatycoon.economy;

import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.api.economy.EconomyTransactionResult;
import fr.valoriatycoon.api.economy.EconomyTransactionStatus;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** SQLite-backed economy with an online-player read cache and write-through mutations. */
public final class InternalEconomyService implements EconomyService {
    private static final int MAX_REASON_LENGTH = 128;

    private final PlayerAccountRepository repository;
    private final BooleanSupplier available;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, Long> activeBalances = new ConcurrentHashMap<>();
    private final Set<UUID> activeAccounts = ConcurrentHashMap.newKeySet();

    public InternalEconomyService(
            PlayerAccountRepository repository,
            BooleanSupplier available,
            Logger logger
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.available = Objects.requireNonNull(available, "available");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Creates or validates an account during asynchronous pre-login without activating its cache. */
    public CompletableFuture<Long> prepareAccount(UUID playerId, String playerName) {
        return repository.loadAccount(playerId, playerName);
    }

    /** Marks an online account as cacheable and refreshes its authoritative balance. */
    public CompletableFuture<Long> activateAccount(UUID playerId, String playerName) {
        activeAccounts.add(playerId);
        return repository.loadAccount(playerId, playerName).thenApply(balance -> {
            if (activeAccounts.contains(playerId)) {
                activeBalances.put(playerId, balance);
            }
            return balance;
        });
    }

    public void deactivateAccount(UUID playerId) {
        activeAccounts.remove(playerId);
        activeBalances.remove(playerId);
    }

    public int cachedAccountCount() {
        return activeBalances.size();
    }

    /** Returns an online cached balance without performing storage I/O. */
    public java.util.OptionalLong cachedBalanceCents(UUID playerId) {
        Long balance = activeBalances.get(playerId);
        return balance == null ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(balance);
    }

    /** Synchronizes the online cache after another service committed an atomic account transaction. */
    public void synchronizeCommittedBalance(UUID playerId, long balanceCents) {
        if (balanceCents < 0) {
            throw new IllegalArgumentException("Committed balance cannot be negative");
        }
        updateActiveBalance(playerId, balanceCents);
    }

    @Override
    public CompletionStage<BigDecimal> getBalance(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (!isAvailable()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Economy storage is unavailable"));
        }
        Long cached = activeBalances.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(MoneyCodec.fromCents(cached));
        }
        return repository.loadAccount(playerId, null).thenApply(MoneyCodec::fromCents);
    }

    @Override
    public CompletionStage<EconomyTransactionResult> addMoney(
            UUID playerId,
            BigDecimal amount,
            String reason
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Long cents = positiveCents(amount);
        if (cents == null) {
            return completedFailure(EconomyTransactionStatus.INVALID_AMOUNT, amount);
        }
        if (!isAvailable()) {
            return completedFailure(EconomyTransactionStatus.SERVICE_UNAVAILABLE, amount);
        }
        UUID transactionId = UUID.randomUUID();
        return repository.credit(transactionId, playerId, cents, sanitizeReason(reason))
                .thenApply(mutation -> {
                    updateActiveBalance(playerId, mutation.balanceAfter());
                    return mutationResult(mutation, cents);
                })
                .exceptionally(error -> storageFailure(transactionId, cents, error));
    }

    @Override
    public CompletionStage<EconomyTransactionResult> removeMoney(
            UUID playerId,
            BigDecimal amount,
            String reason
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Long cents = positiveCents(amount);
        if (cents == null) {
            return completedFailure(EconomyTransactionStatus.INVALID_AMOUNT, amount);
        }
        if (!isAvailable()) {
            return completedFailure(EconomyTransactionStatus.SERVICE_UNAVAILABLE, amount);
        }
        UUID transactionId = UUID.randomUUID();
        return repository.debit(transactionId, playerId, cents, sanitizeReason(reason))
                .thenApply(mutation -> {
                    updateActiveBalance(playerId, mutation.balanceAfter());
                    return mutationResult(mutation, cents);
                })
                .exceptionally(error -> storageFailure(transactionId, cents, error));
    }

    @Override
    public CompletionStage<EconomyTransactionResult> setBalance(
            UUID playerId,
            BigDecimal amount,
            String reason
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Long cents = nonNegativeCents(amount);
        if (cents == null) {
            return completedFailure(EconomyTransactionStatus.INVALID_AMOUNT, amount);
        }
        if (!isAvailable()) {
            return completedFailure(EconomyTransactionStatus.SERVICE_UNAVAILABLE, amount);
        }
        UUID transactionId = UUID.randomUUID();
        return repository.setBalance(transactionId, playerId, cents, sanitizeReason(reason))
                .thenApply(mutation -> {
                    updateActiveBalance(playerId, mutation.balanceAfter());
                    return mutationResult(mutation, cents);
                })
                .exceptionally(error -> storageFailure(transactionId, cents, error));
    }

    @Override
    public CompletionStage<EconomyTransactionResult> transfer(
            UUID sourcePlayerId,
            UUID targetPlayerId,
            BigDecimal amount,
            String reason
    ) {
        Objects.requireNonNull(sourcePlayerId, "sourcePlayerId");
        Objects.requireNonNull(targetPlayerId, "targetPlayerId");
        Long cents = positiveCents(amount);
        if (cents == null) {
            return completedFailure(EconomyTransactionStatus.INVALID_AMOUNT, amount);
        }
        if (sourcePlayerId.equals(targetPlayerId)) {
            return completedFailure(EconomyTransactionStatus.SAME_ACCOUNT, amount);
        }
        if (!isAvailable()) {
            return completedFailure(EconomyTransactionStatus.SERVICE_UNAVAILABLE, amount);
        }
        UUID transactionId = UUID.randomUUID();
        return repository.transfer(transactionId, sourcePlayerId, targetPlayerId, cents, sanitizeReason(reason))
                .thenApply(transfer -> {
                    updateActiveBalance(sourcePlayerId, transfer.sourceBalance());
                    updateActiveBalance(targetPlayerId, transfer.targetBalance());
                    return new EconomyTransactionResult(
                            mapStatus(transfer.status()),
                            MoneyCodec.fromCents(cents),
                            MoneyCodec.fromCents(transfer.sourceBalance()),
                            MoneyCodec.fromCents(transfer.targetBalance())
                    );
                })
                .exceptionally(error -> storageFailure(transactionId, cents, error));
    }

    @Override
    public boolean isAvailable() {
        return available.getAsBoolean();
    }

    private EconomyTransactionResult mutationResult(PlayerAccountRepository.Mutation mutation, long cents) {
        return new EconomyTransactionResult(
                mapStatus(mutation.status()),
                MoneyCodec.fromCents(cents),
                MoneyCodec.fromCents(mutation.balanceAfter()),
                null
        );
    }

    private EconomyTransactionStatus mapStatus(PlayerAccountRepository.RepositoryStatus status) {
        return switch (status) {
            case SUCCESS -> EconomyTransactionStatus.SUCCESS;
            case INSUFFICIENT_FUNDS -> EconomyTransactionStatus.INSUFFICIENT_FUNDS;
            case BALANCE_OVERFLOW -> EconomyTransactionStatus.BALANCE_OVERFLOW;
        };
    }

    private void updateActiveBalance(UUID playerId, long balance) {
        if (activeAccounts.contains(playerId)) {
            activeBalances.put(playerId, balance);
        }
    }

    private CompletionStage<EconomyTransactionResult> completedFailure(
            EconomyTransactionStatus status,
            BigDecimal amount
    ) {
        return CompletableFuture.completedFuture(new EconomyTransactionResult(
                status,
                amount == null ? BigDecimal.ZERO : amount,
                null,
                null
        ));
    }

    private EconomyTransactionResult storageFailure(UUID transactionId, long cents, Throwable error) {
        logger.log(Level.SEVERE, "Economy transaction " + transactionId + " failed", unwrap(error));
        return new EconomyTransactionResult(
                EconomyTransactionStatus.STORAGE_ERROR,
                MoneyCodec.fromCents(cents),
                null,
                null
        );
    }

    private Long positiveCents(BigDecimal amount) {
        Long cents = nonNegativeCents(amount);
        return cents == null || cents == 0 ? null : cents;
    }

    private Long nonNegativeCents(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            return null;
        }
        try {
            return MoneyCodec.toCents(amount);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String sanitizeReason(String reason) {
        String safe = reason == null ? "unspecified" : reason.replaceAll("\\p{Cntrl}", "").trim();
        if (safe.isEmpty()) {
            safe = "unspecified";
        }
        return safe.length() <= MAX_REASON_LENGTH ? safe : safe.substring(0, MAX_REASON_LENGTH);
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current;
    }
}
