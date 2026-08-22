package fr.valoriatycoon.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TutorialRepositoryTest {
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
    void rewardsEveryStageExactlyOnceAndPersistsCompletion() {
        database = new SqliteDatabase(
                directory.resolve("tutorial.db"),
                1_000,
                Logger.getAnonymousLogger()
        );
        database.start().join();
        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 1_000_00L)
                .loadAccount(playerId, "NewPlayer")
                .join();

        TutorialSettings settings = settings();
        TutorialRepository repository = new TutorialRepository(database);
        assertEquals(TutorialProfile.initial(), repository.loadOrCreate(playerId).join());

        TutorialProgressUpdate partial = repository.advance(
                playerId,
                TutorialStep.MINE_COAL,
                47L,
                settings.step(TutorialStep.MINE_COAL)
        ).join();
        assertFalse(partial.rewarded());
        assertEquals(47L, partial.profile().progress());

        TutorialProgressUpdate firstReward = repository.advance(
                playerId,
                TutorialStep.MINE_COAL,
                1L,
                settings.step(TutorialStep.MINE_COAL)
        ).join();
        assertTrue(firstReward.rewarded());
        assertEquals(TutorialStep.HARVEST_WHEAT, firstReward.profile().step());
        assertEquals(5_000_00L, firstReward.resultingBalanceCents());

        TutorialProgressUpdate duplicate = repository.advance(
                playerId,
                TutorialStep.MINE_COAL,
                48L,
                settings.step(TutorialStep.MINE_COAL)
        ).join();
        assertFalse(duplicate.rewarded());
        assertEquals(TutorialStep.HARVEST_WHEAT, duplicate.profile().step());

        for (TutorialStep step : TutorialStep.values()) {
            if (step == TutorialStep.MINE_COAL || !step.actionable()) {
                continue;
            }
            TutorialSettings.StepDefinition definition = settings.step(step);
            TutorialProgressUpdate update = repository.advance(
                    playerId,
                    step,
                    definition.target(),
                    definition
            ).join();
            assertTrue(update.rewarded());
        }

        TutorialProfile ready = repository.loadOrCreate(playerId).join();
        assertEquals(TutorialStep.READY_FOR_RANK, ready.step());
        assertFalse(ready.completed());
        assertEquals(25_000_00L, storedBalance(playerId));
        assertEquals(6, rewardAuditCount(playerId));

        TutorialProfile completed = repository.finish(playerId).join();
        assertTrue(completed.completed());
        assertEquals(completed, repository.loadOrCreate(playerId).join());

        TutorialProgressUpdate replayAttempt = repository.advance(
                playerId,
                TutorialStep.MINE_COAL,
                settings.step(TutorialStep.MINE_COAL).target(),
                settings.step(TutorialStep.MINE_COAL)
        ).join();
        assertFalse(replayAttempt.rewarded());
        assertTrue(replayAttempt.profile().completed());
        assertEquals(25_000_00L, storedBalance(playerId));
        assertEquals(6, rewardAuditCount(playerId));
    }

    private long storedBalance(UUID playerId) {
        return database.submit(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (var result = statement.executeQuery()) {
                    result.next();
                    return result.getLong(1);
                }
            }
        }).join();
    }

    private int rewardAuditCount(UUID playerId) {
        return database.submit(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM economy_transactions
                    WHERE target_uuid = ? AND transaction_type = 'TUTORIAL_REWARD'
                    """)) {
                statement.setString(1, playerId.toString());
                try (var result = statement.executeQuery()) {
                    result.next();
                    return result.getInt(1);
                }
            }
        }).join();
    }

    private TutorialSettings settings() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("tutorial.yml")
        );
        var yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        return TutorialConfigLoader.load(yaml);
    }
}
