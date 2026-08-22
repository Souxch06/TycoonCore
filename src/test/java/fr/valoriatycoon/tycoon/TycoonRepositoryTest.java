package fr.valoriatycoon.tycoon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.upgrades.PlotUpgradeDefinition;
import fr.valoriatycoon.upgrades.PlotUpgradeStatus;
import fr.valoriatycoon.upgrades.PlotUpgradeType;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TycoonRepositoryTest {
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
    void allocatesUniqueSlotsPersistsMembersAndReleasesDeletedPlots() {
        database = new SqliteDatabase(temporaryDirectory.resolve("tycoons.db"), 1000, Logger.getAnonymousLogger());
        database.start().join();
        TycoonRepository repository = new TycoonRepository(database);
        TycoonPlotGroup group = group();
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        new PlayerAccountRepository(database, 1_000_00L)
                .loadAccount(firstOwner, "Owner").join();

        Tycoon first = repository.allocate(firstOwner, group).join().tycoon();
        Tycoon second = repository.allocate(secondOwner, group).join().tycoon();
        assertEquals(0, first.plotIndex());
        assertEquals(1, second.plotIndex());
        assertEquals(TycoonAllocationStatus.ALREADY_OWNS,
                repository.allocate(firstOwner, group).join().status());
        assertEquals(TycoonAllocationStatus.GROUP_FULL,
                repository.allocate(UUID.randomUUID(), group).join().status());

        repository.updateStatus(first.id(), TycoonStatus.ACTIVE).join();
        assertEquals(60L, repository.addPlaytime(firstOwner, 60L).join().playtimeSeconds());
        PlotUpgradeDefinition sizeUpgrade = new PlotUpgradeDefinition(
                PlotUpgradeType.PLOT_SIZE,
                11,
                Material.GRASS_BLOCK,
                "Size",
                java.util.List.of(),
                java.util.List.of(
                        new PlotUpgradeDefinition.Level(1, 24, 0L),
                        new PlotUpgradeDefinition.Level(2, 28, 100_00L)
                )
        );
        var upgrade = repository.purchaseUpgrade(firstOwner, PlotUpgradeType.PLOT_SIZE, 1, sizeUpgrade).join();
        assertEquals(PlotUpgradeStatus.SUCCESS, upgrade.status());
        assertEquals(900_00L, upgrade.balanceCents());

        assertEquals(MemberOperationStatus.SUCCESS,
                repository.addMember(firstOwner, member, group.maximumMembers()).join());
        HopperPosition hopper = new HopperPosition(group.worldName(), 10, 80, 10);
        repository.addHopper(first.id(), hopper).join();
        TycoonDataSnapshot snapshot = repository.loadAll().join();
        assertTrue(snapshot.membersByTycoon().get(first.id()).contains(member));
        assertTrue(snapshot.hoppersByTycoon().get(first.id()).contains(hopper));
        assertEquals(2, snapshot.tycoons().stream()
                .filter(tycoon -> tycoon.id().equals(first.id()))
                .findFirst().orElseThrow().plotSizeLevel());
        assertEquals(1, snapshot.tycoons().stream()
                .filter(tycoon -> tycoon.id().equals(second.id()))
                .findFirst().orElseThrow().plotSizeLevel());

        Tycoon deleting = repository.beginDeletion(firstOwner).join().orElseThrow();
        assertEquals(TycoonStatus.DELETING, deleting.status());
        repository.delete(first.id()).join();
        assertTrue(repository.loadAll().join().hoppersByTycoon().get(first.id()) == null);

        Tycoon replacement = repository.allocate(UUID.randomUUID(), group).join().tycoon();
        assertEquals(0, replacement.plotIndex());
    }

    private TycoonPlotGroup group() {
        return new TycoonPlotGroup(
                "default", "valoria_plots", 1L,
                32, 5, 2, 0, 0,
                64, 64, 160,
                Material.GRASS_BLOCK, Material.DIRT, 10, 6,
                1000.0, 4
        );
    }
}
