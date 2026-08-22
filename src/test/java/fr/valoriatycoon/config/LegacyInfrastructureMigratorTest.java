package fr.valoriatycoon.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyInfrastructureMigratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesLegacyDataDirectoryDatabaseAndConfiguredWorldNames() throws Exception {
        Path legacy = Files.createDirectory(temporaryDirectory.resolve("TycoonCore"));
        Path current = temporaryDirectory.resolve("ValoriaTycoon");
        Files.writeString(legacy.resolve("tycooncore.db"), "database");
        Files.writeString(legacy.resolve("config.yml"), "database:\n  sqlite:\n    file: tycooncore.db\n");
        Files.writeString(legacy.resolve("spawn.yml"), "world: tycoon_spawn\n");
        Files.writeString(legacy.resolve("messages.yml"), "prefix: TycoonCore\n");
        Files.writeString(
                legacy.resolve("farms.yml"),
                "worlds: [tycoon_farm_mine, tycoon_farm_forest]\n"
        );

        LegacyInfrastructureMigrator.migrateDataDirectory(legacy, current);
        LegacyInfrastructureMigrator.migrateDataFiles(current);

        assertFalse(Files.exists(legacy));
        assertTrue(Files.isRegularFile(current.resolve("valoriatycoon.db")));
        assertFalse(Files.exists(current.resolve("tycooncore.db")));
        assertTrue(Files.readString(current.resolve("config.yml")).contains("valoriatycoon.db"));
        assertTrue(Files.readString(current.resolve("spawn.yml")).contains("valoria_spawn"));
        assertTrue(Files.readString(current.resolve("messages.yml")).contains("ValoriaTycoon"));
        String farms = Files.readString(current.resolve("farms.yml"));
        assertTrue(farms.contains("valoria_farm_mine"));
        assertTrue(farms.contains("valoria_farm_forest"));
    }

    @Test
    void refusesToMergeTwoNonEmptyDataDirectories() throws Exception {
        Path legacy = Files.createDirectory(temporaryDirectory.resolve("TycoonCore"));
        Path current = Files.createDirectory(temporaryDirectory.resolve("ValoriaTycoon"));
        Files.writeString(legacy.resolve("config.yml"), "legacy");
        Files.writeString(current.resolve("config.yml"), "current");

        assertThrows(
                IOException.class,
                () -> LegacyInfrastructureMigrator.migrateDataDirectory(legacy, current)
        );
    }
}
