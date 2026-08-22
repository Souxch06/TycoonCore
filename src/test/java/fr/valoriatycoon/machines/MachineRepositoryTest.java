package fr.valoriatycoon.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.tools.ToolType;
import fr.valoriatycoon.tycoon.TycoonPlotGroup;
import fr.valoriatycoon.tycoon.TycoonRepository;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MachineRepositoryTest {
    @TempDir Path temporaryDirectory;
    private SqliteDatabase database;

    @AfterEach
    void close() {
        if (database != null) database.close(Duration.ofSeconds(5));
    }

    @Test
    void storesSellsAndUpgradesGeneratorWithoutEnergy() {
        database = new SqliteDatabase(temporaryDirectory.resolve("machines.db"), 1000, Logger.getAnonymousLogger());
        database.start().join();
        UUID owner = UUID.randomUUID();
        new PlayerAccountRepository(database, 1_000_00L).loadAccount(owner, "Owner").join();
        TycoonRepository tycoons = new TycoonRepository(database);
        var island = tycoons.allocate(owner, group()).join().tycoon();
        tycoons.updateStatus(island.id(), TycoonStatus.ACTIVE).join();

        MachineRepository machines = new MachineRepository(database);
        MachineDefinition miner = miner();
        PlacedMachine placed = machines.create(
                island.id(), owner, miner, new MachinePosition("world", 2, 80, 1)
        ).join();

        MachineCycleResult stored = machines.runCycle(
                placed.id(), miner, 3L, 10_000L, 5_00L
        ).join();
        assertEquals(MachineCycleStatus.PRODUCED, stored.status());
        assertEquals(3L, stored.machine().storedAmount());

        assertEquals(3L, machines.collect(placed.id()).join().storedAmount());
        PlacedMachine autoSell = machines.toggleAutoSell(placed.id()).join();
        assertTrue(autoSell.autoSell());
        MachineCycleResult sold = machines.runCycle(
                placed.id(), miner, 2L, 10_000L, 5_00L
        ).join();
        assertEquals(10_00L, sold.creditedMoneyCents());
        assertEquals(1_010_00L, sold.ownerBalanceCents());

        MachineUpgradeResult upgrade = machines.purchaseUpgrade(
                placed.id(), owner, MachineUpgradeType.SPEED, 10, 100_00L
        ).join();
        assertEquals(MachineUpgradeStatus.SUCCESS, upgrade.status());
        assertEquals(2, upgrade.machine().speedLevel());
        assertEquals(910_00L, upgrade.resultingBalanceCents());
    }

    private MachineDefinition miner() {
        return new MachineDefinition(
                "miner", Material.IRON_BLOCK, Material.IRON_PICKAXE,
                11, "Miner", List.of(),
                250_00L, ToolType.PICKAXE, 250L, Duration.ofSeconds(10),
                Material.RAW_IRON, 2, 5_00L, 1000L
        );
    }

    private TycoonPlotGroup group() {
        return new TycoonPlotGroup(
                "default", "world", 1L, 32, 16, 10,
                0, 0, 80, 72, 180,
                Material.GRASS_BLOCK, Material.DIRT, 13, 8,
                10000, 10
        );
    }
}
