package fr.valoriatycoon.tutorial;

import fr.valoriatycoon.database.SqliteDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Atomic SQLite progression and reward storage for the first-rank tutorial. */
public final class TutorialRepository {
    private final SqliteDatabase database;

    public TutorialRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public CompletableFuture<TutorialProfile> loadOrCreate(UUID playerId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRow(connection, playerId);
            return select(connection, playerId);
        }));
    }

    public CompletableFuture<TutorialProgressUpdate> advance(
            UUID playerId,
            TutorialStep expectedStep,
            long amount,
            TutorialSettings.StepDefinition definition
    ) {
        if (amount < 1L || definition.step() != expectedStep) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Invalid tutorial progression batch")
            );
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRow(connection, playerId);
            TutorialProfile current = select(connection, playerId);
            if (current.completed()
                    || current.step() != expectedStep
                    || !current.step().actionable()) {
                return new TutorialProgressUpdate(current, null, 0L, -1L);
            }

            long progress = saturatingAdd(current.progress(), amount);
            if (progress < definition.target()) {
                TutorialProfile updated = new TutorialProfile(current.step(), progress, false);
                updateProfile(connection, playerId, updated);
                return new TutorialProgressUpdate(updated, null, 0L, -1L);
            }

            TutorialProfile updated = new TutorialProfile(current.step().next(), 0L, false);
            long balance = creditReward(
                    connection,
                    playerId,
                    definition.rewardMoneyCents(),
                    current.step()
            );
            updateProfile(connection, playerId, updated);
            return new TutorialProgressUpdate(
                    updated,
                    current.step(),
                    definition.rewardMoneyCents(),
                    balance
            );
        }));
    }

    /** Permanently disables guidance without granting an additional reward. */
    public CompletableFuture<TutorialProfile> finish(UUID playerId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRow(connection, playerId);
            TutorialProfile completed = new TutorialProfile(
                    TutorialStep.READY_FOR_RANK,
                    0L,
                    true
            );
            updateProfile(connection, playerId, completed);
            return completed;
        }));
    }

    private void ensureRow(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO tutorial_progress
                    (player_uuid, step_index, progress, completed, updated_at)
                VALUES (?, 0, 0, 0, ?)
                """)) {
            statement.setString(1, playerId.toString());
            statement.setLong(2, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private TutorialProfile select(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT step_index, progress, completed
                FROM tutorial_progress
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing tutorial row for " + playerId);
                }
                int index = result.getInt(1);
                TutorialStep[] steps = TutorialStep.values();
                if (index < 0 || index >= steps.length) {
                    throw new SQLException("Invalid tutorial step " + index + " for " + playerId);
                }
                return new TutorialProfile(
                        steps[index],
                        result.getLong(2),
                        result.getInt(3) != 0
                );
            }
        }
    }

    private void updateProfile(
            Connection connection,
            UUID playerId,
            TutorialProfile profile
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tutorial_progress
                SET step_index = ?, progress = ?, completed = ?, updated_at = ?
                WHERE player_uuid = ?
                """)) {
            statement.setInt(1, profile.step().ordinal());
            statement.setLong(2, profile.progress());
            statement.setInt(3, profile.completed() ? 1 : 0);
            statement.setLong(4, System.currentTimeMillis());
            statement.setString(5, playerId.toString());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not update tutorial for " + playerId);
            }
        }
    }

    private long creditReward(
            Connection connection,
            UUID playerId,
            long amount,
            TutorialStep step
    ) throws SQLException {
        long before;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing economy account for tutorial reward");
                }
                before = result.getLong(1);
            }
        }
        if (amount > Long.MAX_VALUE - before) {
            throw new SQLException("Tutorial reward balance overflow");
        }
        long after = before + amount;
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoon_players
                SET balance_cents = ?, updated_at = ?
                WHERE player_uuid = ?
                """)) {
            statement.setLong(1, after);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not credit tutorial reward");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid,
                     amount_cents, reason, created_at)
                VALUES (?, 'TUTORIAL_REWARD', NULL, ?, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, amount);
            statement.setString(4, "tutorial:" + step.configKey());
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
        return after;
    }

    private long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right
                ? Long.MAX_VALUE
                : left + right;
    }

    private <T> T inTransaction(Connection connection, Work<T> work) throws Exception {
        boolean autoCommit = connection.getAutoCommit();
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
            connection.setAutoCommit(autoCommit);
        }
    }

    @FunctionalInterface
    private interface Work<T> {
        T execute() throws Exception;
    }
}
