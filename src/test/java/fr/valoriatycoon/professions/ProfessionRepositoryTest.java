package fr.valoriatycoon.professions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfessionRepositoryTest {
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
    void persistsIndependentPermanentProfessionProgress() {
        database = new SqliteDatabase(
                directory.resolve("professions.db"),
                1_000,
                Logger.getAnonymousLogger()
        );
        database.start().join();
        UUID playerId = UUID.randomUUID();
        new PlayerAccountRepository(database, 0L).loadAccount(playerId, "Worker").join();

        ProfessionRepository repository = new ProfessionRepository(database, settings());
        assertEquals(4, repository.loadOrCreate(playerId).join().size());

        ProfessionProfile levelTwo = repository
                .addExperience(playerId, ProfessionType.MINER, 100L)
                .join();
        ProfessionProfile almostThree = repository
                .addExperience(playerId, ProfessionType.MINER, 114L)
                .join();
        ProfessionProfile levelThree = repository
                .addExperience(playerId, ProfessionType.MINER, 1L)
                .join();

        assertEquals(new ProfessionProfile(ProfessionType.MINER, 2, 0L), levelTwo);
        assertEquals(new ProfessionProfile(ProfessionType.MINER, 2, 114L), almostThree);
        assertEquals(new ProfessionProfile(ProfessionType.MINER, 3, 0L), levelThree);
        assertEquals(1, repository.loadOrCreate(playerId).join().stream()
                .filter(profile -> profile.type() == ProfessionType.FARMER)
                .findFirst().orElseThrow().level());
    }

    private ProfessionSettings settings() {
        Map<ProfessionType, ProfessionDefinition> definitions = new EnumMap<>(ProfessionType.class);
        for (ProfessionType type : ProfessionType.values()) {
            definitions.put(type, new ProfessionDefinition(type, type.displayName(), 5L));
        }
        return new ProfessionSettings(
                new ProfessionSettings.Progression(100, 100L, new BigDecimal("1.15"), 20),
                definitions
        );
    }
}
