package fr.valoriatycoon.professions;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.progression.LevelExperienceCalculator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous SQLite storage for permanent profession levels. */
public final class ProfessionRepository {
    private final SqliteDatabase database;
    private final ProfessionSettings settings;

    public ProfessionRepository(SqliteDatabase database, ProfessionSettings settings) {
        this.database = Objects.requireNonNull(database, "database");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /** Creates missing rows and loads all four profession profiles. */
    public CompletableFuture<List<ProfessionProfile>> loadOrCreate(UUID playerId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRows(connection, playerId);
            return selectProfiles(connection, playerId);
        }));
    }

    /** Applies one persisted batch of profession experience. */
    public CompletableFuture<ProfessionProfile> addExperience(
            UUID playerId,
            ProfessionType type,
            long experience
    ) {
        if (experience < 0L) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Profession experience cannot be negative")
            );
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRow(connection, playerId, type, System.currentTimeMillis());
            ProfessionProfile current = selectProfile(connection, playerId, type);
            LevelExperienceCalculator.Progress progress = LevelExperienceCalculator.add(
                    current.level(),
                    current.experience(),
                    experience,
                    settings.progression()
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE player_professions
                    SET profession_level = ?, profession_experience = ?, updated_at = ?
                    WHERE player_uuid = ? AND profession_type = ?
                    """)) {
                statement.setInt(1, progress.level());
                statement.setLong(2, progress.experience());
                statement.setLong(3, System.currentTimeMillis());
                statement.setString(4, playerId.toString());
                statement.setString(5, type.storageKey());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Missing profession row for " + playerId + '/' + type);
                }
            }
            return new ProfessionProfile(type, progress.level(), progress.experience());
        }));
    }

    private void ensureRows(Connection connection, UUID playerId) throws SQLException {
        long now = System.currentTimeMillis();
        for (ProfessionType type : ProfessionType.values()) {
            ensureRow(connection, playerId, type, now);
        }
    }

    private void ensureRow(
            Connection connection,
            UUID playerId,
            ProfessionType type,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO player_professions
                    (player_uuid, profession_type, profession_level, profession_experience, updated_at)
                VALUES (?, ?, 1, 0, ?)
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.storageKey());
            statement.setLong(3, now);
            statement.executeUpdate();
        }
    }

    private List<ProfessionProfile> selectProfiles(
            Connection connection,
            UUID playerId
    ) throws SQLException {
        List<ProfessionProfile> profiles = new ArrayList<>(ProfessionType.values().length);
        for (ProfessionType type : ProfessionType.values()) {
            profiles.add(selectProfile(connection, playerId, type));
        }
        return profiles;
    }

    private ProfessionProfile selectProfile(
            Connection connection,
            UUID playerId,
            ProfessionType type
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT profession_level, profession_experience
                FROM player_professions
                WHERE player_uuid = ? AND profession_type = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.storageKey());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing profession row for " + playerId + '/' + type);
                }
                return new ProfessionProfile(type, result.getInt(1), result.getLong(2));
            }
        }
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
