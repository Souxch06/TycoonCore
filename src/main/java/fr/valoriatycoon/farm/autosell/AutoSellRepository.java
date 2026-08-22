package fr.valoriatycoon.farm.autosell;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.farm.FarmSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persists preferences and purchases levels atomically with their economy debit. */
public final class AutoSellRepository {
    private final SqliteDatabase database;

    public AutoSellRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public CompletableFuture<AutoSellProfile> load(UUID playerId) {
        return database.submit(connection -> selectAccount(connection, playerId).profile());
    }

    public CompletableFuture<AutoSellProfile> toggle(UUID playerId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Account account = selectAccount(connection, playerId);
            if (!account.profile().unlocked()) {
                return account.profile();
            }
            AutoSellProfile updated = new AutoSellProfile(!account.profile().enabled(), account.profile().level());
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE tycoon_players
                    SET autosell_enabled = ?, updated_at = ?
                    WHERE player_uuid = ?
                    """)) {
                statement.setInt(1, updated.enabled() ? 1 : 0);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, playerId.toString());
                requireSingleUpdate(statement, playerId);
            }
            return updated;
        }));
    }

    /** Debits money and raises the level in the same SQLite transaction. */
    public CompletableFuture<AutoSellPurchaseResult> purchaseNext(
            UUID playerId,
            int expectedCurrentLevel,
            List<FarmSettings.AutoSellLevel> levels
    ) {
        List<FarmSettings.AutoSellLevel> immutableLevels = List.copyOf(levels);
        return database.submit(connection -> inTransaction(connection, () -> {
            Account account = selectAccount(connection, playerId);
            int currentLevel = account.profile().level();
            if (currentLevel != expectedCurrentLevel) {
                return new AutoSellPurchaseResult(
                        AutoSellPurchaseStatus.PROFILE_STALE,
                        account.profile(),
                        0L,
                        account.balanceCents()
                );
            }
            if (currentLevel >= immutableLevels.size()) {
                return new AutoSellPurchaseResult(
                        AutoSellPurchaseStatus.MAXIMUM_LEVEL,
                        account.profile(),
                        0L,
                        account.balanceCents()
                );
            }

            FarmSettings.AutoSellLevel next = immutableLevels.get(currentLevel);
            if (next.level() != currentLevel + 1) {
                throw new IllegalStateException("Auto-sell level configuration is not sequential");
            }
            long cost = next.costCents();
            if (account.balanceCents() < cost) {
                return new AutoSellPurchaseResult(
                        AutoSellPurchaseStatus.INSUFFICIENT_FUNDS,
                        account.profile(),
                        cost,
                        account.balanceCents()
                );
            }

            long resultingBalance = account.balanceCents() - cost;
            AutoSellProfile resultingProfile = new AutoSellProfile(
                    currentLevel == 0 || account.profile().enabled(),
                    next.level()
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE tycoon_players
                    SET balance_cents = ?, autosell_enabled = ?, autosell_level = ?, updated_at = ?
                    WHERE player_uuid = ?
                    """)) {
                statement.setLong(1, resultingBalance);
                statement.setInt(2, resultingProfile.enabled() ? 1 : 0);
                statement.setInt(3, resultingProfile.level());
                statement.setLong(4, System.currentTimeMillis());
                statement.setString(5, playerId.toString());
                requireSingleUpdate(statement, playerId);
            }
            insertAudit(connection, playerId, cost, next.level());
            return new AutoSellPurchaseResult(
                    AutoSellPurchaseStatus.SUCCESS,
                    resultingProfile,
                    cost,
                    resultingBalance
            );
        }));
    }

    private Account selectAccount(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT autosell_enabled, autosell_level, balance_cents
                FROM tycoon_players
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing account for auto-sell operation: " + playerId);
                }
                return new Account(
                        new AutoSellProfile(result.getInt(1) == 1, result.getInt(2)),
                        result.getLong(3)
                );
            }
        }
    }

    private void insertAudit(Connection connection, UUID playerId, long cost, int level) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid, amount_cents, reason, created_at)
                VALUES (?, 'AUTOSELL_UPGRADE', ?, NULL, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, cost);
            statement.setString(4, "autosell:upgrade:" + level);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void requireSingleUpdate(PreparedStatement statement, UUID playerId) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Expected exactly one updated account for " + playerId);
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

    private record Account(AutoSellProfile profile, long balanceCents) {
    }
}
