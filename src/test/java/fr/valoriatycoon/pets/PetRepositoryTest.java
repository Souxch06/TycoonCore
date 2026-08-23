package fr.valoriatycoon.pets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.tycoon.TycoonPlotGroup;
import fr.valoriatycoon.tycoon.TycoonRepository;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PetRepositoryTest {
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
    void consumesKeysOnceAndPreservesVariantWhenReclaimingEggs() {
        database = new SqliteDatabase(
                temporaryDirectory.resolve("pets.db"),
                1_000,
                Logger.getAnonymousLogger()
        );
        database.start().join();
        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 500_000_000_00L)
                .loadAccount(playerId, "PetOwner")
                .join();
        TycoonRepository tycoons = new TycoonRepository(database);
        var island = tycoons.allocate(playerId, group()).join().tycoon();
        tycoons.updateStatus(island.id(), TycoonStatus.ACTIVE).join();
        setRank(playerId, 10);

        PetSettings settings = PetConfigLoader.load(loadConfiguration());
        PetRepository pets = new PetRepository(database, settings);
        UUID firstKey = UUID.randomUUID();
        PetOperationResult crate = pets.openCrate(playerId, firstKey).join();
        assertEquals(PetOperationStatus.SUCCESS, crate.status());
        assertTrue(crate.egg() != null);
        assertEquals(1, pets.loadAll().join().pendingEggs().size());
        assertEquals(
                PetOperationStatus.KEY_ALREADY_USED,
                pets.openCrate(playerId, firstKey).join().status()
        );

        PetEgg firstEgg = crate.egg();
        assertEquals(
                PetOperationStatus.INVALID_EGG,
                pets.redeemEgg(
                        playerId,
                        firstEgg.eggId(),
                        firstEgg.petId(),
                        !firstEgg.chromatic()
                ).join().status()
        );
        PetOperationResult redeemed = pets.redeemEgg(
                playerId,
                firstEgg.eggId(),
                firstEgg.petId(),
                firstEgg.chromatic()
        ).join();
        assertEquals(PetOperationStatus.SUCCESS, redeemed.status());
        assertTrue(redeemed.pet().active());
        PetProfile progressed = pets.addExperience(playerId, 1_000L).join();
        assertTrue(progressed.level() > 1);

        PetOperationResult reclaimed = pets.reclaim(
                playerId,
                progressed.petId(),
                settings.reclaim().moneyCostCents()
        ).join();
        assertEquals(PetOperationStatus.SUCCESS, reclaimed.status());
        assertEquals(495_000_000_00L, reclaimed.balanceCents());
        assertEquals(progressed.chromatic(), reclaimed.egg().chromatic());
        assertEquals(progressed.level(), reclaimed.egg().level());
        assertEquals(progressed.experience(), reclaimed.egg().experience());

        PetOperationResult restored = pets.redeemEgg(
                playerId,
                reclaimed.egg().eggId(),
                reclaimed.egg().petId(),
                reclaimed.egg().chromatic()
        ).join();
        assertEquals(PetOperationStatus.SUCCESS, restored.status());
        assertEquals(progressed.chromatic(), restored.pet().chromatic());
        assertEquals(progressed.level(), restored.pet().level());
        assertEquals(
                PetOperationStatus.EGG_ALREADY_USED,
                pets.redeemEgg(
                        playerId,
                        reclaimed.egg().eggId(),
                        reclaimed.egg().petId(),
                        reclaimed.egg().chromatic()
                ).join().status()
        );

        assertEquals(PetOperationStatus.SUCCESS,
                pets.openCrate(playerId, UUID.randomUUID()).join().status());
        long consumed = database.submit(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM consumed_pet_keys")) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }).join();
        assertEquals(2L, consumed);
    }

    @Test
    void versionTwentyThreeKeepsExistingPetsNormalAndCreatesEggLedger() throws Exception {
        Path file = temporaryDirectory.resolve("pets-v22.db");
        Class.forName("org.sqlite.JDBC");
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE player_pets (
                        player_uuid TEXT NOT NULL,
                        pet_id TEXT NOT NULL,
                        pet_level INTEGER NOT NULL,
                        pet_experience INTEGER NOT NULL,
                        active INTEGER NOT NULL,
                        obtained_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, pet_id)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO player_pets VALUES ('player', 'rabbit_farmer', 7, 12, 1, 1, 1)
                    """);
            statement.execute("PRAGMA user_version = 22");
        }
        database = new SqliteDatabase(file, 1_000, Logger.getAnonymousLogger());
        database.start().join();
        int chromatic = database.submit(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT chromatic FROM player_pets WHERE player_uuid = 'player'")) {
                return result.next() ? result.getInt(1) : -1;
            }
        }).join();
        assertEquals(0, chromatic);
        long eggTables = database.submit(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT COUNT(*) FROM sqlite_master
                         WHERE type = 'table' AND name = 'issued_pet_eggs'
                         """)) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }).join();
        assertEquals(1L, eggTables);
    }

    private void setRank(UUID playerId, int rank) {
        database.submit(connection -> {
            try (var statement = connection.prepareStatement(
                    "UPDATE tycoons SET prestige_level = ? WHERE owner_uuid = ?")) {
                statement.setInt(1, rank);
                statement.setString(2, playerId.toString());
                statement.executeUpdate();
            }
            return null;
        }).join();
    }

    private YamlConfiguration loadConfiguration() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("pets.yml")
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }

    private TycoonPlotGroup group() {
        return new TycoonPlotGroup(
                "default",
                "valoria_plots",
                1L,
                32,
                16,
                10,
                0,
                0,
                80,
                72,
                180,
                Material.GRASS_BLOCK,
                Material.DIRT,
                13,
                8,
                10_000,
                10
        );
    }
}
