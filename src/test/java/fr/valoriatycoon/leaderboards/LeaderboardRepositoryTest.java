package fr.valoriatycoon.leaderboards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.tycoon.TycoonPlotGroup;
import fr.valoriatycoon.tycoon.TycoonRepository;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeaderboardRepositoryTest {
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
    void asynchronouslyOrdersEveryCategoryAndExcludesInactiveIslands() {
        database = new SqliteDatabase(
                temporaryDirectory.resolve("leaderboards.db"),
                1_000,
                Logger.getAnonymousLogger()
        );
        database.start().join();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        PlayerAccountRepository accounts = new PlayerAccountRepository(database, 0L);
        accounts.loadAccount(alice, "Alice").join();
        accounts.loadAccount(bob, "Bob").join();
        accounts.loadAccount(inactive, "Inactive").join();
        TycoonRepository tycoons = new TycoonRepository(database);
        var aliceIsland = tycoons.allocate(alice, group()).join().tycoon();
        var bobIsland = tycoons.allocate(bob, group()).join().tycoon();
        tycoons.allocate(inactive, group()).join();
        tycoons.updateStatus(aliceIsland.id(), TycoonStatus.ACTIVE).join();
        tycoons.updateStatus(bobIsland.id(), TycoonStatus.ACTIVE).join();
        seedValues(alice, 100_00L, 8, 2, 500L, 60L);
        seedValues(bob, 200_00L, 4, 5, 300L, 3_600L);
        seedValues(inactive, 1_000_00L, 99, 10, 99_999L, 99_999L);

        var snapshot = new LeaderboardRepository(database).loadAll(10).join();

        assertEquals(inactive, snapshot.get(LeaderboardType.MONEY).getFirst().playerId());
        assertEquals(alice, snapshot.get(LeaderboardType.ISLAND_LEVEL).getFirst().playerId());
        assertEquals(bob, snapshot.get(LeaderboardType.RANK).getFirst().playerId());
        assertEquals(alice, snapshot.get(LeaderboardType.PRODUCTION).getFirst().playerId());
        assertEquals(bob, snapshot.get(LeaderboardType.PLAYTIME).getFirst().playerId());
        assertEquals(2, snapshot.get(LeaderboardType.RANK).size());
        assertEquals(1, new LeaderboardRepository(database)
                .loadAll(1).join().get(LeaderboardType.MONEY).size());
    }

    private void seedValues(
            UUID playerId,
            long balance,
            int level,
            int rank,
            long production,
            long playtime
    ) {
        database.submit(connection -> {
            try (var account = connection.prepareStatement("""
                    UPDATE tycoon_players SET balance_cents = ? WHERE player_uuid = ?
                    """); var island = connection.prepareStatement("""
                    UPDATE tycoons
                    SET tycoon_level = ?, prestige_level = ?, total_production = ?, playtime_seconds = ?
                    WHERE owner_uuid = ?
                    """)) {
                account.setLong(1, balance);
                account.setString(2, playerId.toString());
                account.executeUpdate();
                island.setInt(1, level);
                island.setInt(2, rank);
                island.setLong(3, production);
                island.setLong(4, playtime);
                island.setString(5, playerId.toString());
                island.executeUpdate();
            }
            return null;
        }).join();
    }

    private TycoonPlotGroup group() {
        return new TycoonPlotGroup(
                "default", "valoria_plots", 1L,
                32, 8, 4, 0, 0,
                64, 64, 160,
                Material.GRASS_BLOCK, Material.DIRT,
                10, 6, 1_000.0, 8
        );
    }
}
