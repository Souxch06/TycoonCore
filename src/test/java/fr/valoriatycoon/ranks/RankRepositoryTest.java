package fr.valoriatycoon.ranks;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.professions.ProfessionType;
import fr.valoriatycoon.quests.QuestDefinition;
import fr.valoriatycoon.quests.QuestProfile;
import fr.valoriatycoon.quests.QuestRarity;
import fr.valoriatycoon.quests.QuestRepository;
import fr.valoriatycoon.quests.QuestSettings;
import fr.valoriatycoon.tools.ToolType;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonPlotGroup;
import fr.valoriatycoon.tycoon.TycoonRepository;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RankRepositoryTest {
    @TempDir
    Path directory;

    private SqliteDatabase database;

    @AfterEach
    void close() {
        if (database != null) {
            database.close(Duration.ofSeconds(5));
        }
    }

    @Test
    void requiresPlaytimeThenDeductsOnlyRankPriceAndResetsProgressAtomically() {
        database = new SqliteDatabase(
                directory.resolve("ranks.db"),
                1_000,
                Logger.getAnonymousLogger()
        );
        database.start().join();

        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 500_00L)
                .loadAccount(playerId, "Ranker")
                .join();

        TycoonRepository islands = new TycoonRepository(database);
        Tycoon island = islands.allocate(playerId, group()).join().tycoon();
        islands.updateStatus(island.id(), TycoonStatus.ACTIVE).join();
        insertToolProfiles(playerId, 10);
        insertProfessionProfiles(playerId, 10);

        QuestDefinition quest = new QuestDefinition(
                "common",
                QuestRarity.COMMON,
                ToolType.PICKAXE,
                1,
                0L
        );
        QuestSettings questSettings = new QuestSettings(20, Map.of(quest.id(), quest));
        QuestRepository quests = new QuestRepository(database, questSettings);
        quests.loadOrCreate(playerId).join();
        quests.addProgress(playerId, quest, 2).join();

        long requiredPlaytime = 120L * 60L;
        RankRequirement requirement = new RankRequirement(
                1,
                "Citoyen",
                100_00L,
                requiredPlaytime,
                10,
                Map.of(QuestRarity.COMMON, 2),
                Map.of(ToolType.PICKAXE, 5),
                Map.of(ProfessionType.MINER, 11),
                Map.of(),
                0.02
        );
        RankRepository ranks = new RankRepository(database, questSettings);

        RankPromotionResult tooSoon = ranks.promote(playerId, 0, requirement).join();
        assertAll(
                () -> assertEquals(RankPromotionStatus.INSUFFICIENT_PLAYTIME, tooSoon.status()),
                () -> assertEquals(0, tooSoon.resultingRank()),
                () -> assertEquals(500_00L, tooSoon.resultingBalanceCents()),
                () -> assertEquals(2L, quests.loadOrCreate(playerId).join()
                        .available(QuestRarity.COMMON))
        );

        islands.addPlaytime(playerId, requiredPlaytime).join();
        RankPromotionResult professionTooLow = ranks.promote(playerId, 0, requirement).join();
        assertEquals(
                RankPromotionStatus.INSUFFICIENT_PROFESSION_LEVELS,
                professionTooLow.status()
        );
        setProfessionLevel(playerId, ProfessionType.MINER, 11);

        RankPromotionResult result = ranks.promote(playerId, 0, requirement).join();

        StoredPromotion stored = loadStoredPromotion(playerId);
        QuestProfile questsAfter = quests.loadOrCreate(playerId).join();
        assertAll(
                () -> assertEquals(RankPromotionStatus.SUCCESS, result.status()),
                () -> assertEquals(1, result.resultingRank()),
                () -> assertEquals(400_00L, result.resultingBalanceCents()),
                () -> assertEquals(1, stored.rank()),
                () -> assertEquals(requiredPlaytime, stored.playtimeSeconds()),
                () -> assertEquals(400_00L, stored.balanceCents()),
                () -> assertEquals(100_00L, stored.auditedChargeCents()),
                () -> assertEquals(1, stored.pickaxeLevel()),
                () -> assertEquals(11, stored.minerLevel()),
                () -> assertEquals(0L, questsAfter.available(QuestRarity.COMMON))
        );
    }

    private void insertToolProfiles(UUID playerId, int level) {
        database.submit(connection -> {
            for (ToolType type : ToolType.values()) {
                try (var statement = connection.prepareStatement("""
                        INSERT INTO player_tools(
                            player_uuid, tool_type, tool_level, tool_experience,
                            updated_at, special_coins
                        ) VALUES (?, ?, ?, 0, ?, 0)
                        """)) {
                    statement.setString(1, playerId.toString());
                    statement.setString(2, type.storageKey());
                    statement.setInt(3, level);
                    statement.setLong(4, System.currentTimeMillis());
                    statement.executeUpdate();
                }
            }
            return null;
        }).join();
    }

    private void insertProfessionProfiles(UUID playerId, int level) {
        database.submit(connection -> {
            for (ProfessionType type : ProfessionType.values()) {
                try (var statement = connection.prepareStatement("""
                        INSERT INTO player_professions(
                            player_uuid, profession_type, profession_level,
                            profession_experience, updated_at
                        ) VALUES (?, ?, ?, 0, ?)
                        """)) {
                    statement.setString(1, playerId.toString());
                    statement.setString(2, type.storageKey());
                    statement.setInt(3, level);
                    statement.setLong(4, System.currentTimeMillis());
                    statement.executeUpdate();
                }
            }
            return null;
        }).join();
    }

    private void setProfessionLevel(
            UUID playerId,
            ProfessionType type,
            int level
    ) {
        database.submit(connection -> {
            try (var statement = connection.prepareStatement("""
                    UPDATE player_professions
                    SET profession_level = ?, updated_at = ?
                    WHERE player_uuid = ? AND profession_type = ?
                    """)) {
                statement.setInt(1, level);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, playerId.toString());
                statement.setString(4, type.storageKey());
                statement.executeUpdate();
            }
            return null;
        }).join();
    }

    private StoredPromotion loadStoredPromotion(UUID playerId) {
        return database.submit(connection -> {
            int rank;
            long playtime;
            try (var statement = connection.prepareStatement("""
                    SELECT prestige_level, playtime_seconds
                    FROM tycoons
                    WHERE owner_uuid = ? AND status = 'ACTIVE'
                    """)) {
                statement.setString(1, playerId.toString());
                try (var result = statement.executeQuery()) {
                    result.next();
                    rank = result.getInt(1);
                    playtime = result.getLong(2);
                }
            }

            long balance;
            try (var statement = connection.prepareStatement("""
                    SELECT balance_cents
                    FROM tycoon_players
                    WHERE player_uuid = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (var result = statement.executeQuery()) {
                    result.next();
                    balance = result.getLong(1);
                }
            }

            long auditedCharge;
            try (var statement = connection.prepareStatement("""
                    SELECT amount_cents
                    FROM economy_transactions
                    WHERE source_uuid = ? AND transaction_type = 'RANK_PROMOTION'
                    """)) {
                statement.setString(1, playerId.toString());
                try (var result = statement.executeQuery()) {
                    result.next();
                    auditedCharge = result.getLong(1);
                }
            }

            int pickaxeLevel;
            try (var statement = connection.prepareStatement("""
                    SELECT tool_level
                    FROM player_tools
                    WHERE player_uuid = ? AND tool_type = 'pickaxe'
                    """)) {
                statement.setString(1, playerId.toString());
                try (var result = statement.executeQuery()) {
                    result.next();
                    pickaxeLevel = result.getInt(1);
                }
            }
            int minerLevel;
            try (var statement = connection.prepareStatement("""
                    SELECT profession_level
                    FROM player_professions
                    WHERE player_uuid = ? AND profession_type = 'miner'
                    """)) {
                statement.setString(1, playerId.toString());
                try (var result = statement.executeQuery()) {
                    result.next();
                    minerLevel = result.getInt(1);
                }
            }
            return new StoredPromotion(
                    rank,
                    playtime,
                    balance,
                    auditedCharge,
                    pickaxeLevel,
                    minerLevel
            );
        }).join();
    }

    private TycoonPlotGroup group() {
        return new TycoonPlotGroup(
                "default",
                "world",
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

    private record StoredPromotion(
            int rank,
            long playtimeSeconds,
            long balanceCents,
            long auditedChargeCents,
            int pickaxeLevel,
            int minerLevel
    ) {
    }
}
