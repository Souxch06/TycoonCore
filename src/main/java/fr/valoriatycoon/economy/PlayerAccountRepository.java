package fr.valoriatycoon.economy;

import fr.valoriatycoon.database.SqliteDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Performs authoritative account mutations on the database-owned worker. */
public final class PlayerAccountRepository {
    private final SqliteDatabase database;
    private final long startingBalanceCents;

    public PlayerAccountRepository(SqliteDatabase database, long startingBalanceCents) {
        this.database = Objects.requireNonNull(database, "database");
        this.startingBalanceCents = startingBalanceCents;
    }

    public CompletableFuture<Long> loadAccount(UUID playerId, String lastKnownName) {
        return database.submit(connection -> {
            ensureAccount(connection, playerId, lastKnownName);
            return selectBalance(connection, playerId);
        });
    }

    public CompletableFuture<Mutation> credit(UUID transactionId, UUID playerId, long amount, String reason) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureAccount(connection, playerId, null);
            long before = selectBalance(connection, playerId);
            if (amount > Long.MAX_VALUE - before) {
                return new Mutation(RepositoryStatus.BALANCE_OVERFLOW, before, before);
            }
            long after = before + amount;
            updateBalance(connection, playerId, after);
            insertAudit(connection, transactionId, "CREDIT", null, playerId, amount, reason);
            return new Mutation(RepositoryStatus.SUCCESS, before, after);
        }));
    }

    public CompletableFuture<Mutation> debit(UUID transactionId, UUID playerId, long amount, String reason) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureAccount(connection, playerId, null);
            long before = selectBalance(connection, playerId);
            if (before < amount) {
                return new Mutation(RepositoryStatus.INSUFFICIENT_FUNDS, before, before);
            }
            long after = before - amount;
            updateBalance(connection, playerId, after);
            insertAudit(connection, transactionId, "DEBIT", playerId, null, amount, reason);
            return new Mutation(RepositoryStatus.SUCCESS, before, after);
        }));
    }

    public CompletableFuture<Mutation> setBalance(UUID transactionId, UUID playerId, long amount, String reason) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureAccount(connection, playerId, null);
            long before = selectBalance(connection, playerId);
            updateBalance(connection, playerId, amount);
            insertAudit(connection, transactionId, "SET", playerId, playerId, amount, reason);
            return new Mutation(RepositoryStatus.SUCCESS, before, amount);
        }));
    }

    public CompletableFuture<Transfer> transfer(
            UUID transactionId,
            UUID sourcePlayerId,
            UUID targetPlayerId,
            long amount,
            String reason
    ) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureAccount(connection, sourcePlayerId, null);
            ensureAccount(connection, targetPlayerId, null);
            long sourceBefore = selectBalance(connection, sourcePlayerId);
            long targetBefore = selectBalance(connection, targetPlayerId);
            if (sourceBefore < amount) {
                return new Transfer(RepositoryStatus.INSUFFICIENT_FUNDS, sourceBefore, targetBefore);
            }
            if (amount > Long.MAX_VALUE - targetBefore) {
                return new Transfer(RepositoryStatus.BALANCE_OVERFLOW, sourceBefore, targetBefore);
            }
            long sourceAfter = sourceBefore - amount;
            long targetAfter = targetBefore + amount;
            updateBalance(connection, sourcePlayerId, sourceAfter);
            updateBalance(connection, targetPlayerId, targetAfter);
            insertAudit(connection, transactionId, "TRANSFER", sourcePlayerId, targetPlayerId, amount, reason);
            return new Transfer(RepositoryStatus.SUCCESS, sourceAfter, targetAfter);
        }));
    }

    private void ensureAccount(Connection connection, UUID playerId, String lastKnownName) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT OR IGNORE INTO tycoon_players
                    (player_uuid, last_known_name, balance_cents, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            insert.setString(1, playerId.toString());
            insert.setString(2, lastKnownName);
            insert.setLong(3, startingBalanceCents);
            insert.setLong(4, now);
            insert.setLong(5, now);
            insert.executeUpdate();
        }
        if (lastKnownName != null && !lastKnownName.isBlank()) {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE tycoon_players
                    SET last_known_name = ?, updated_at = ?
                    WHERE player_uuid = ?
                    """)) {
                update.setString(1, lastKnownName);
                update.setLong(2, now);
                update.setString(3, playerId.toString());
                update.executeUpdate();
            }
        }
    }

    private long selectBalance(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Account disappeared after creation: " + playerId);
                }
                return result.getLong(1);
            }
        }
    }

    private void updateBalance(Connection connection, UUID playerId, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoon_players
                SET balance_cents = ?, updated_at = ?
                WHERE player_uuid = ?
                """)) {
            statement.setLong(1, balance);
            statement.setLong(2, Instant.now().toEpochMilli());
            statement.setString(3, playerId.toString());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expected exactly one updated account for " + playerId);
            }
        }
    }

    private void insertAudit(
            Connection connection,
            UUID transactionId,
            String type,
            UUID sourcePlayerId,
            UUID targetPlayerId,
            long amount,
            String reason
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid, amount_cents, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, transactionId.toString());
            statement.setString(2, type);
            statement.setString(3, sourcePlayerId == null ? null : sourcePlayerId.toString());
            statement.setString(4, targetPlayerId == null ? null : targetPlayerId.toString());
            statement.setLong(5, amount);
            statement.setString(6, reason);
            statement.setLong(7, Instant.now().toEpochMilli());
            statement.executeUpdate();
        }
    }

    private <T> T inTransaction(Connection connection, TransactionWork<T> work) throws Exception {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.execute();
            connection.commit();
            return result;
        } catch (Exception exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T execute() throws Exception;
    }

    public enum RepositoryStatus {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        BALANCE_OVERFLOW
    }

    public record Mutation(RepositoryStatus status, long balanceBefore, long balanceAfter) {
    }

    public record Transfer(RepositoryStatus status, long sourceBalance, long targetBalance) {
    }
}
