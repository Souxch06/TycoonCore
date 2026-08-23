package fr.valoriatycoon.crates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrateKeyRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private SqliteDatabase database;

    @AfterEach
    void close() {
        if (database != null) {
            database.close(Duration.ofSeconds(5));
        }
    }

    @Test
    void issuesSourceReferencesOnceAndRecoversQuestDeliveries() {
        database = new SqliteDatabase(
                temporaryDirectory.resolve("crate-keys.db"),
                1_000,
                Logger.getAnonymousLogger()
        );
        database.start().join();
        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 0L).loadAccount(playerId, "KeyOwner").join();
        CrateKeyRepository keys = new CrateKeyRepository(database);

        CrateKey firstVote = keys.issue(
                playerId,
                CrateType.VOTE,
                "VOTE",
                "service:vote-1"
        ).join();
        CrateKey duplicateVote = keys.issue(
                playerId,
                CrateType.VOTE,
                "VOTE",
                "service:vote-1"
        ).join();
        assertEquals(firstVote.keyId(), duplicateVote.keyId());
        assertEquals(1, keys.pending(playerId).join().size());
        keys.markDelivered(firstVote.keyId()).join();
        assertTrue(keys.pending(playerId).join().isEmpty());

        assertEquals(3, keys.ensureQuestKeys(playerId, "common_miner", 3L).join().size());
        assertEquals(3, keys.ensureQuestKeys(playerId, "common_miner", 3L).join().size());
        keys.pending(playerId).join().forEach(key -> keys.markDelivered(key.keyId()).join());
        assertTrue(keys.pending(playerId).join().isEmpty());
        assertEquals(1, keys.ensureQuestKeys(playerId, "common_miner", 4L).join().size());
        assertEquals(CrateType.QUEST, keys.pending(playerId).join().getFirst().type());
        assertEquals(playerId, keys.findPlayerId("keyowner").join().orElseThrow());
        assertFalse(keys.findPlayerId("unknown").join().isPresent());
        CrateKey valoria = keys.issue(
                playerId,
                CrateType.VALORIA,
                "STORE",
                "transaction-001:0"
        ).join();
        CrateKey retriedValoria = keys.issue(
                playerId,
                CrateType.VALORIA,
                "STORE",
                "transaction-001:0"
        ).join();
        assertEquals(CrateType.VALORIA, valoria.type());
        assertEquals(valoria.keyId(), retriedValoria.keyId());

        int schema = database.submit(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("PRAGMA user_version")) {
                return result.next() ? result.getInt(1) : -1;
            }
        }).join();
        assertEquals(26, schema);
    }

    @Test
    void migrationsPreserveFreeKeysAndAddValoriaAndRewardLedger() throws Exception {
        Path file = temporaryDirectory.resolve("crate-v24.db");
        Class.forName("org.sqlite.JDBC");
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE issued_crate_keys (
                        key_id TEXT PRIMARY KEY NOT NULL,
                        crate_type TEXT NOT NULL CHECK (crate_type IN
                            ('VOTE','QUEST','FARM','COMMON','RARE','EPIC','LEGENDARY')),
                        issued_to_uuid TEXT NOT NULL,
                        source TEXT NOT NULL,
                        source_reference TEXT NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0,
                        issued_at INTEGER NOT NULL,
                        delivered_at INTEGER,
                        consumed_by_uuid TEXT,
                        consumed_at INTEGER,
                        UNIQUE(source, source_reference)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO issued_crate_keys
                    VALUES ('11111111-1111-1111-1111-111111111111', 'LEGENDARY',
                            '22222222-2222-2222-2222-222222222222', 'ADMIN', 'legacy',
                            1, 1, 1, NULL, NULL)
                    """);
            statement.execute("PRAGMA user_version = 24");
        }
        database = new SqliteDatabase(file, 1_000, Logger.getAnonymousLogger());
        database.start().join();
        long preserved = database.submit(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM issued_crate_keys WHERE crate_type = 'LEGENDARY'")) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }).join();
        assertEquals(1L, preserved);
        UUID playerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        CrateKey valoria = new CrateKeyRepository(database)
                .issue(playerId, CrateType.VALORIA, "STORE", "order:0")
                .join();
        assertEquals(CrateType.VALORIA, valoria.type());
        int rewardTable = database.submit(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM sqlite_master
                    WHERE type = 'table' AND name = 'issued_crate_rewards'
                    """); var result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }).join();
        assertEquals(1, rewardTable);
    }
}
