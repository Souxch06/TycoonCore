package fr.valoriatycoon.crates;

import fr.valoriatycoon.database.SqliteDatabase;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Authoritative async ledger for idempotent, crash-recoverable generic crate keys. */
public final class CrateKeyRepository {
    private final SqliteDatabase database;

    public CrateKeyRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** Issues one key exactly once for a source reference. */
    public CompletableFuture<CrateKey> issue(
            UUID playerId,
            CrateType type,
            String source,
            String sourceReference
    ) {
        validate(playerId, type, source, sourceReference);
        return database.submit(connection -> issue(connection, playerId, type, source, sourceReference));
    }

    /** Reconciles one key per completed quest index and returns every still-undelivered key. */
    public CompletableFuture<List<CrateKey>> ensureQuestKeys(
            UUID playerId,
            String questId,
            long totalCompletions
    ) {
        if (questId == null || questId.isBlank() || totalCompletions < 0L || totalCompletions > 100_000L) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid quest key reconciliation"));
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            long issued = issuedQuestCount(connection, playerId, questId);
            for (long completion = issued + 1L; completion <= totalCompletions; completion++) {
                issue(
                        connection,
                        playerId,
                        CrateType.QUEST,
                        "QUEST",
                        playerId + ":" + questId + ":" + completion
                );
            }
            return pending(connection, playerId);
        }));
    }

    public CompletableFuture<List<CrateKey>> pending(UUID playerId) {
        return database.submit(connection -> pending(connection, playerId));
    }

    public CompletableFuture<Void> markDelivered(UUID keyId) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE issued_crate_keys
                    SET delivered = 1, delivered_at = COALESCE(delivered_at, ?)
                    WHERE key_id = ? AND delivered = 0 AND consumed_by_uuid IS NULL
                    """)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, keyId.toString());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Optional<UUID>> findPlayerId(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT player_uuid
                    FROM tycoon_players
                    WHERE last_known_name = ? COLLATE NOCASE
                    ORDER BY updated_at DESC
                    LIMIT 1
                    """)) {
                statement.setString(1, playerName);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next()
                            ? Optional.of(UUID.fromString(result.getString(1)))
                            : Optional.empty();
                }
            }
        });
    }

    private CrateKey issue(
            Connection connection,
            UUID playerId,
            CrateType type,
            String source,
            String sourceReference
    ) throws SQLException {
        validate(playerId, type, source, sourceReference);
        UUID keyId = deterministicId(source, sourceReference);
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO issued_crate_keys
                    (key_id, crate_type, issued_to_uuid, source, source_reference, delivered, issued_at)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                """)) {
            statement.setString(1, keyId.toString());
            statement.setString(2, type.name());
            statement.setString(3, playerId.toString());
            statement.setString(4, source);
            statement.setString(5, sourceReference);
            statement.setLong(6, now);
            statement.executeUpdate();
        }
        CrateKey key = select(connection, keyId);
        if (key == null
                || !key.issuedTo().equals(playerId)
                || key.type() != type
                || !key.source().equals(source)
                || !key.sourceReference().equals(sourceReference)) {
            throw new SQLException("Crate key source reference conflicts with another issuance");
        }
        return key;
    }

    private CrateKey select(Connection connection, UUID keyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM issued_crate_keys WHERE key_id = ?
                """)) {
            statement.setString(1, keyId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    private long issuedQuestCount(Connection connection, UUID playerId, String questId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM issued_crate_keys
                WHERE issued_to_uuid = ? AND source = 'QUEST' AND source_reference GLOB ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, playerId + ":" + questId + ":*");
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }
    }

    private List<CrateKey> pending(Connection connection, UUID playerId) throws SQLException {
        List<CrateKey> keys = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM issued_crate_keys
                WHERE issued_to_uuid = ? AND delivered = 0 AND consumed_by_uuid IS NULL
                ORDER BY issued_at ASC, key_id ASC
                LIMIT 512
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    keys.add(map(result));
                }
            }
        }
        return List.copyOf(keys);
    }

    private CrateKey map(ResultSet result) throws SQLException {
        return new CrateKey(
                UUID.fromString(result.getString("key_id")),
                CrateType.valueOf(result.getString("crate_type")),
                UUID.fromString(result.getString("issued_to_uuid")),
                result.getString("source"),
                result.getString("source_reference"),
                result.getInt("delivered") == 1,
                Instant.ofEpochMilli(result.getLong("issued_at"))
        );
    }

    private UUID deterministicId(String source, String sourceReference) {
        return UUID.nameUUIDFromBytes(
                ("valoriatycoon:crate-key:" + source + ':' + sourceReference)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private void validate(UUID playerId, CrateType type, String source, String reference) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(type, "type");
        if (source == null || source.isBlank() || reference == null || reference.isBlank()
                || source.length() > 32 || reference.length() > 256) {
            throw new IllegalArgumentException("Invalid crate key source identity");
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
            } catch (SQLException rollback) {
                exception.addSuppressed(rollback);
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
