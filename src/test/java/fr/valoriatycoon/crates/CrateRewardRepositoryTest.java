package fr.valoriatycoon.crates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.tools.ToolType;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrateRewardRepositoryTest {
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
    void consumesEachKeyAndRewardOnceWithoutRerollingAfterRecovery() {
        start();
        UUID opener = UUID.randomUUID();
        UUID claimant = UUID.randomUUID();
        PlayerAccountRepository accounts = new PlayerAccountRepository(database, 0L);
        accounts.loadAccount(opener, "Opener").join();
        accounts.loadAccount(claimant, "Claimant").join();
        CrateKeyRepository keys = new CrateKeyRepository(database);
        CrateRewardRepository rewards = new CrateRewardRepository(database);

        CrateKey moneyKey = keys.issue(opener, CrateType.LEGENDARY, "ADMIN", "money-key").join();
        CrateRewardSelection money = new CrateRewardSelection(
                "money_iv",
                CrateRewardKind.MONEY_BAG,
                new CrateRewardPayload(Map.of(
                        "amount_cents", "12345678",
                        "count", "1",
                        "tier", "4"
                ))
        );
        assertEquals(CrateOpenStatus.KEY_TYPE_MISMATCH, rewards.open(
                opener,
                new CrateKeyItemService.KeyToken(moneyKey.keyId(), CrateType.EPIC),
                money
        ).join().status());
        CrateOpenResult opened = rewards.open(
                opener,
                new CrateKeyItemService.KeyToken(moneyKey.keyId(), CrateType.LEGENDARY),
                money
        ).join();
        assertTrue(opened.successful());
        assertEquals("12345678", opened.reward().payload().require("amount_cents"));
        assertEquals(CrateOpenStatus.KEY_ALREADY_USED, rewards.open(
                opener,
                new CrateKeyItemService.KeyToken(moneyKey.keyId(), CrateType.LEGENDARY),
                new CrateRewardSelection(
                        "forbidden_reroll",
                        CrateRewardKind.XP_VIAL,
                        new CrateRewardPayload(Map.of("count", "1", "levels", "15", "tier", "4"))
                )
        ).join().status());

        assertEquals(1, rewards.pending(opener).join().size());
        assertEquals(opened.reward().rewardId(), rewards.pending(opener).join().getFirst().rewardId());
        rewards.markDelivered(opened.reward().rewardId()).join();
        assertTrue(rewards.pending(opener).join().isEmpty());

        assertEquals(CrateClaimStatus.REWARD_KIND_MISMATCH, rewards.claim(
                claimant,
                opened.reward().rewardId(),
                CrateRewardKind.XP_VIAL
        ).join().status());
        CrateClaimResult claimed = rewards.claim(
                claimant,
                opened.reward().rewardId(),
                CrateRewardKind.MONEY_BAG
        ).join();
        assertTrue(claimed.successful());
        assertEquals(12_345_678L, claimed.resultingMoneyCents());
        assertEquals(CrateClaimStatus.REWARD_ALREADY_USED, rewards.claim(
                claimant,
                opened.reward().rewardId(),
                CrateRewardKind.MONEY_BAG
        ).join().status());
    }

    @Test
    void creditsCoinsAndIssuesFollowUpKeysInsideClaimTransactions() {
        start();
        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 0L).loadAccount(playerId, "Rewards").join();
        CrateKeyRepository keys = new CrateKeyRepository(database);
        CrateRewardRepository rewards = new CrateRewardRepository(database);

        CrateReward coins = open(
                keys,
                rewards,
                playerId,
                "coins",
                new CrateRewardSelection(
                        "coins_v",
                        CrateRewardKind.COIN_BAG,
                        new CrateRewardPayload(Map.of(
                                "amount", "250000",
                                "count", "1",
                                "tier", "5",
                                "tool", "PICKAXE"
                        ))
                )
        );
        CrateClaimResult coinClaim = rewards.claim(
                playerId,
                coins.rewardId(),
                CrateRewardKind.COIN_BAG
        ).join();
        assertEquals(250_000L, coinClaim.resultingToolCoins().get(ToolType.PICKAXE));

        CrateReward keyVoucher = open(
                keys,
                rewards,
                playerId,
                "keys",
                new CrateRewardSelection(
                        "legendary_keys_2",
                        CrateRewardKind.CRATE_KEYS,
                        new CrateRewardPayload(Map.of(
                                "amount", "2",
                                "count", "2",
                                "crate_type", "LEGENDARY"
                        ))
                )
        );
        assertTrue(rewards.claim(
                playerId,
                keyVoucher.rewardId(),
                CrateRewardKind.CRATE_KEYS
        ).join().successful());
        assertEquals(2L, keys.pending(playerId).join().stream()
                .filter(key -> key.type() == CrateType.LEGENDARY)
                .count());

        CrateReward xp = open(
                keys,
                rewards,
                playerId,
                "xp",
                new CrateRewardSelection(
                        "xp_iv",
                        CrateRewardKind.XP_VIAL,
                        new CrateRewardPayload(Map.of("count", "1", "levels", "12", "tier", "4"))
                )
        );
        assertTrue(rewards.claim(playerId, xp.rewardId(), CrateRewardKind.XP_VIAL).join().successful());
        assertEquals(1, rewards.pendingClaims(playerId).join().size());
        rewards.markClaimDelivered(xp.rewardId()).join();
        assertTrue(rewards.pendingClaims(playerId).join().isEmpty());
    }

    private CrateReward open(
            CrateKeyRepository keys,
            CrateRewardRepository rewards,
            UUID playerId,
            String reference,
            CrateRewardSelection selection
    ) {
        CrateKey key = keys.issue(playerId, CrateType.VALORIA, "STORE", reference).join();
        return rewards.open(
                playerId,
                new CrateKeyItemService.KeyToken(key.keyId(), key.type()),
                selection
        ).join().reward();
    }

    private void start() {
        database = new SqliteDatabase(
                temporaryDirectory.resolve(UUID.randomUUID() + ".db"),
                1_000,
                Logger.getAnonymousLogger()
        );
        database.start().join();
    }
}
