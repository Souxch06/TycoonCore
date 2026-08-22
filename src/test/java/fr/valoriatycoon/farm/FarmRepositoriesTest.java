package fr.valoriatycoon.farm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.farm.autosell.AutoSellProfile;
import fr.valoriatycoon.farm.autosell.AutoSellPurchaseStatus;
import fr.valoriatycoon.farm.autosell.AutoSellRepository;
import fr.valoriatycoon.farm.regeneration.BlockPosition;
import fr.valoriatycoon.farm.regeneration.BlockRegenerationRepository;
import fr.valoriatycoon.farm.regeneration.PendingBlockRegeneration;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FarmRepositoriesTest {
    @TempDir
    Path temporaryDirectory;

    private SqliteDatabase database;

    @AfterEach
    void closeDatabase() {
        if (database != null) {
            database.close(Duration.ofSeconds(5));
        }
    }

    @Test
    void purchasesLevelsAtomicallyAndPersistsRegenerations() {
        database = new SqliteDatabase(temporaryDirectory.resolve("farms.db"), 1000, Logger.getAnonymousLogger());
        database.start().join();

        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 1_000_00L).loadAccount(playerId, "Farmer").join();
        AutoSellRepository autoSell = new AutoSellRepository(database);
        assertEquals(new AutoSellProfile(false, 0), autoSell.load(playerId).join());
        assertEquals(new AutoSellProfile(false, 0), autoSell.toggle(playerId).join());

        List<FarmSettings.AutoSellLevel> levels = List.of(
                new FarmSettings.AutoSellLevel(1, 100_00L, BigDecimal.ONE),
                new FarmSettings.AutoSellLevel(2, 500_00L, BigDecimal.valueOf(2)),
                new FarmSettings.AutoSellLevel(3, 2_000_00L, BigDecimal.valueOf(3))
        );
        var firstPurchase = autoSell.purchaseNext(playerId, 0, levels).join();
        assertEquals(AutoSellPurchaseStatus.SUCCESS, firstPurchase.status());
        assertEquals(new AutoSellProfile(true, 1), firstPurchase.profile());
        assertEquals(900_00L, firstPurchase.balanceCents());

        assertEquals(new AutoSellProfile(false, 1), autoSell.toggle(playerId).join());
        var secondPurchase = autoSell.purchaseNext(playerId, 1, levels).join();
        assertEquals(AutoSellPurchaseStatus.SUCCESS, secondPurchase.status());
        assertEquals(new AutoSellProfile(false, 2), secondPurchase.profile());
        assertEquals(400_00L, secondPurchase.balanceCents());

        var rejected = autoSell.purchaseNext(playerId, 2, levels).join();
        assertEquals(AutoSellPurchaseStatus.INSUFFICIENT_FUNDS, rejected.status());
        assertEquals(400_00L, rejected.balanceCents());
        assertEquals(new AutoSellProfile(false, 2), autoSell.load(playerId).join());

        var stale = autoSell.purchaseNext(playerId, 0, levels).join();
        assertEquals(AutoSellPurchaseStatus.PROFILE_STALE, stale.status());
        assertEquals(400_00L, stale.balanceCents());

        BlockRegenerationRepository regenerations = new BlockRegenerationRepository(database);
        BlockPosition position = new BlockPosition("farm_mine", 12, 42, -7);
        PendingBlockRegeneration pending = new PendingBlockRegeneration(
                position,
                "minecraft:diamond_ore",
                System.currentTimeMillis() + 30_000L
        );
        regenerations.save(pending).join();
        assertEquals(List.of(pending), regenerations.loadAll().join());
        regenerations.delete(position).join();
        assertTrue(regenerations.loadAll().join().isEmpty());
    }

    @Test
    void versionTwentyMigratesLegacyWorldNames() throws Exception {
        Path file = temporaryDirectory.resolve("version-nineteen.db");
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) {
            for (String table : List.of(
                    "pending_block_regenerations",
                    "tycoons",
                    "tycoon_hoppers",
                    "machines"
            )) {
                statement.executeUpdate("CREATE TABLE " + table + " (world_name TEXT NOT NULL)");
            }
            statement.executeUpdate(
                    "INSERT INTO pending_block_regenerations VALUES ('tycoon_farm_forest')"
            );
            statement.executeUpdate("INSERT INTO tycoons VALUES ('tycoon_plots')");
            statement.executeUpdate("INSERT INTO tycoon_hoppers VALUES ('tycoon_plots')");
            statement.executeUpdate("INSERT INTO machines VALUES ('tycoon_plots')");
            statement.execute("PRAGMA user_version = 19");
        }

        database = new SqliteDatabase(file, 1000, Logger.getAnonymousLogger());
        database.start().join();

        List<String> worlds = database.submit(connection -> {
            var migrated = new java.util.ArrayList<String>();
            try (var statement = connection.createStatement()) {
                for (String table : List.of(
                        "pending_block_regenerations",
                        "tycoons",
                        "tycoon_hoppers",
                        "machines"
                )) {
                    try (var result = statement.executeQuery("SELECT world_name FROM " + table)) {
                        if (result.next()) {
                            migrated.add(result.getString(1));
                        }
                    }
                }
            }
            return List.copyOf(migrated);
        }).join();
        assertEquals(
                List.of("valoria_farm_forest", "valoria_plots", "valoria_plots", "valoria_plots"),
                worlds
        );
    }

    @Test
    void versionThreeMigrationPreservesPreviouslyEnabledPlayersAtLevelOne() throws Exception {
        Path file = temporaryDirectory.resolve("version-two.db");
        Class.forName("org.sqlite.JDBC");
        UUID playerId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE tycoon_players (
                        player_uuid TEXT PRIMARY KEY NOT NULL,
                        last_known_name TEXT,
                        balance_cents INTEGER NOT NULL DEFAULT 0 CHECK (balance_cents >= 0),
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        autosell_enabled INTEGER NOT NULL DEFAULT 0 CHECK (autosell_enabled IN (0, 1))
                    )
                    """);
            try (var insert = connection.prepareStatement("""
                    INSERT INTO tycoon_players
                        (player_uuid, last_known_name, balance_cents, created_at, updated_at, autosell_enabled)
                    VALUES (?, 'Legacy', 0, 1, 1, 1)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.executeUpdate();
            }
            statement.execute("PRAGMA user_version = 2");
        }

        database = new SqliteDatabase(file, 1000, Logger.getAnonymousLogger());
        database.start().join();
        AutoSellProfile profile = new AutoSellRepository(database).load(playerId).join();
        assertTrue(profile.enabled());
        assertEquals(1, profile.level());
    }
}
