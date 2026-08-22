package fr.valoriatycoon.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.valoriatycoon.database.SqliteDatabase;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerAccountRepositoryTest {
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
    void transferIsAtomicAndInsufficientFundsDoNotMutateBalances() {
        database = new SqliteDatabase(temporaryDirectory.resolve("test.db"), 1000, Logger.getAnonymousLogger());
        database.start().join();
        PlayerAccountRepository repository = new PlayerAccountRepository(database, 100_00L);
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        assertEquals(100_00L, repository.loadAccount(source, "Source").join());
        assertEquals(100_00L, repository.loadAccount(target, "Target").join());

        var success = repository.transfer(UUID.randomUUID(), source, target, 25_00L, "test").join();
        assertEquals(PlayerAccountRepository.RepositoryStatus.SUCCESS, success.status());
        assertEquals(75_00L, success.sourceBalance());
        assertEquals(125_00L, success.targetBalance());

        var rejected = repository.transfer(UUID.randomUUID(), source, target, 80_00L, "test").join();
        assertEquals(PlayerAccountRepository.RepositoryStatus.INSUFFICIENT_FUNDS, rejected.status());
        assertEquals(75_00L, repository.loadAccount(source, null).join());
        assertEquals(125_00L, repository.loadAccount(target, null).join());
    }
}
