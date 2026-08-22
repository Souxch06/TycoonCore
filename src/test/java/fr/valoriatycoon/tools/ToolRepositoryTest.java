package fr.valoriatycoon.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolRepositoryTest {
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
    void keepsCoinsAndCapabilitiesUniquePerToolWithDualPayment() {
        database = new SqliteDatabase(temporaryDirectory.resolve("tools.db"), 1000, Logger.getAnonymousLogger());
        database.start().join();
        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 1_000_00L).loadAccount(playerId, "ToolUser").join();

        ToolRepository repository = new ToolRepository(database, settings());
        repository.loadOrCreate(playerId).join();
        ToolProfile rewarded = repository.addRewards(playerId, ToolType.PICKAXE, 250L, 1_000L).join();
        assertEquals(1_000L, rewarded.specialCoins());
        assertEquals(0L, repository.loadOrCreate(playerId).join().stream()
                .filter(profile -> profile.toolType() == ToolType.HOE)
                .findFirst().orElseThrow().specialCoins());

        ToolUpgradeResult moneyPurchase = repository.purchaseCapability(
                playerId,
                ToolType.PICKAXE,
                ToolCapability.EFFICIENCY,
                1,
                ToolUpgradeCurrency.BASE_MONEY
        ).join();
        assertEquals(ToolUpgradeStatus.SUCCESS, moneyPurchase.status());
        assertEquals(2, moneyPurchase.resultingLevel());
        assertEquals(900_00L, moneyPurchase.balanceCents());
        assertEquals(1_000L, moneyPurchase.toolCoins());

        ToolUpgradeResult coinPurchase = repository.purchaseCapability(
                playerId,
                ToolType.PICKAXE,
                ToolCapability.MONEY_BOOST,
                1,
                ToolUpgradeCurrency.TOOL_COINS
        ).join();
        assertEquals(ToolUpgradeStatus.SUCCESS, coinPurchase.status());
        assertEquals(750L, coinPurchase.toolCoins());
        assertEquals(900_00L, coinPurchase.balanceCents());

        ToolUpgradeResult isolatedCurrency = repository.purchaseCapability(
                playerId,
                ToolType.HOE,
                ToolCapability.EFFICIENCY,
                1,
                ToolUpgradeCurrency.TOOL_COINS
        ).join();
        assertEquals(ToolUpgradeStatus.INSUFFICIENT_TOOL_COINS, isolatedCurrency.status());
        assertEquals(0L, isolatedCurrency.toolCoins());

        List<ToolProfile> reloaded = repository.loadOrCreate(playerId).join();
        assertEquals(2, reloaded.stream().filter(p -> p.toolType() == ToolType.PICKAXE)
                .findFirst().orElseThrow().capabilityLevel(ToolCapability.EFFICIENCY));
        assertEquals(1, reloaded.stream().filter(p -> p.toolType() == ToolType.HOE)
                .findFirst().orElseThrow().capabilityLevel(ToolCapability.EFFICIENCY));

        ToolUpgradeResult stale = repository.purchaseCapability(
                playerId,
                ToolType.PICKAXE,
                ToolCapability.EFFICIENCY,
                1,
                ToolUpgradeCurrency.BASE_MONEY
        ).join();
        assertEquals(ToolUpgradeStatus.PROFILE_STALE, stale.status());

        ToolProfile progressed = repository.addRewards(playerId, ToolType.PICKAXE, 50L, 25L).join();
        assertEquals(3, progressed.toolLevel());
        assertEquals(0L, progressed.toolExperience());
        assertEquals(775L, progressed.specialCoins());
    }

    private ToolSettings settings() {
        Map<ToolType, ToolDefinition> tools = new EnumMap<>(ToolType.class);
        for (ToolType type : ToolType.values()) {
            tools.put(type, new ToolDefinition(
                    type,
                    Material.DIAMOND_PICKAXE,
                    type.name(),
                    type.ordinal(),
                    List.of(),
                    5L,
                    type.name() + "Coins",
                    Material.EMERALD,
                    1L
            ));
        }
        Map<ToolCapability, ToolCapabilityDefinition> capabilities = new EnumMap<>(ToolCapability.class);
        for (ToolCapability capability : ToolCapability.values()) {
            capabilities.put(capability, new ToolCapabilityDefinition(
                    capability,
                    java.util.EnumSet.allOf(ToolType.class),
                    1,
                    10 + capability.ordinal(),
                    Material.NETHER_STAR,
                    capability.name(),
                    List.of(),
                    List.of(
                            new ToolCapabilityDefinition.Level(1, 0L, 0L, BigDecimal.ONE),
                            new ToolCapabilityDefinition.Level(2, 100_00L, 250L, BigDecimal.valueOf(2))
                    )
            ));
        }
        ToolSettings.Menu menu = new ToolSettings.Menu(
                27, "Tools", 4, "Info", List.of(), 22,
                Material.HOPPER, Material.BEACON, "Auto", "Max", List.of(), List.of()
        );
        ToolSettings.PurchaseMenu purchase = new ToolSettings.PurchaseMenu(
                9, "Purchase", 3, Material.SUNFLOWER, "Money", List.of(),
                5, "Coins", List.of()
        );
        return new ToolSettings(
                menu,
                purchase,
                new ToolSettings.MultiTool(true, 6.0, false),
                new ToolSettings.AbilitySettings(
                        60, 1, 15, 4, new BigDecimal("0.12"), 25,
                        new BigDecimal("0.10"), new BigDecimal("0.01"), 40
                ),
                new ToolSettings.Progression(10, 100L, BigDecimal.valueOf(2), 20),
                tools,
                capabilities
        );
    }
}
