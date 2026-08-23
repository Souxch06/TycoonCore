package fr.valoriatycoon.config;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Migrates pre-ValoriaTycoon data directories, file names and generated world folders. */
public final class LegacyInfrastructureMigrator {
    private static final String LEGACY_DATA_DIRECTORY = "TycoonCore";
    private static final String LEGACY_DATABASE = "tycooncore.db";
    private static final String CURRENT_DATABASE = "valoriatycoon.db";
    private static final Map<String, String> WORLD_NAMES = worldNames();
    private static final List<String> CONFIGURATION_FILES = List.of(
            "compaction.yml",
            "config.yml",
            "farms.yml",
            "machines.yml",
            "menus.yml",
            "messages.yml",
            "pets.yml",
            "professions.yml",
            "quests.yml",
            "ranks.yml",
            "spawn.yml",
            "tools.yml",
            "tutorial.yml",
            "tycoons.yml",
            "upgrades.yml"
    );

    private LegacyInfrastructureMigrator() {
    }

    /**
     * Performs the one-time infrastructure rename before configuration and worlds are loaded.
     * Existing destinations are never overwritten.
     */
    public static void migrate(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (Bukkit.getPluginManager().getPlugin(LEGACY_DATA_DIRECTORY) != null) {
            throw new IllegalStateException(
                    "Remove the legacy TycoonCore JAR before starting ValoriaTycoon"
            );
        }
        Logger logger = plugin.getLogger();
        try {
            Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
            Path pluginsDirectory = dataDirectory.getParent();
            if (pluginsDirectory == null) {
                throw new IOException("Plugin data directory has no parent");
            }
            Path legacyDirectory = pluginsDirectory.resolve(LEGACY_DATA_DIRECTORY).normalize();
            migrateDataDirectory(legacyDirectory, dataDirectory);
            migrateDataFiles(dataDirectory);
            migrateWorldDirectories(Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize());
            if (Files.isDirectory(dataDirectory)) {
                logger.info("ValoriaTycoon infrastructure paths are ready.");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot migrate legacy TycoonCore infrastructure", exception);
        }
    }

    static void migrateDataDirectory(Path legacyDirectory, Path currentDirectory) throws IOException {
        if (!Files.isDirectory(legacyDirectory)) {
            return;
        }
        if (Files.exists(currentDirectory)) {
            if (!Files.isDirectory(currentDirectory) || !isDirectoryEmpty(currentDirectory)) {
                throw new IOException(
                        "Both legacy and current plugin data directories exist: "
                                + legacyDirectory + " / " + currentDirectory
                );
            }
            Files.delete(currentDirectory);
        }
        move(legacyDirectory, currentDirectory);
    }

    static void migrateDataFiles(Path dataDirectory) throws IOException {
        migrateDatabase(dataDirectory);
        migrateConfigurationValues(dataDirectory);
    }

    private static void migrateDatabase(Path dataDirectory) throws IOException {
        if (!Files.isDirectory(dataDirectory)) {
            return;
        }
        Path legacy = dataDirectory.resolve(LEGACY_DATABASE);
        Path current = dataDirectory.resolve(CURRENT_DATABASE);
        if (!Files.exists(legacy)) {
            return;
        }
        if (Files.exists(current)) {
            throw new IOException("Both legacy and current SQLite files exist in " + dataDirectory);
        }
        move(legacy, current);
    }

    private static void migrateConfigurationValues(Path dataDirectory) throws IOException {
        Map<String, String> replacements = new LinkedHashMap<>(WORLD_NAMES);
        replacements.put(LEGACY_DATABASE, CURRENT_DATABASE);
        replacements.put("TycoonCore", "ValoriaTycoon");
        for (String fileName : CONFIGURATION_FILES) {
            Path file = dataDirectory.resolve(fileName);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String original = Files.readString(file);
            String migrated = original;
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                migrated = migrated.replace(replacement.getKey(), replacement.getValue());
            }
            if (!migrated.equals(original)) {
                Files.writeString(file, migrated);
            }
        }
    }

    private static void migrateWorldDirectories(Path worldContainer) throws IOException {
        for (Map.Entry<String, String> world : WORLD_NAMES.entrySet()) {
            Path legacy = worldContainer.resolve(world.getKey()).normalize();
            Path current = worldContainer.resolve(world.getValue()).normalize();
            if (!Files.exists(legacy)) {
                continue;
            }
            if (Bukkit.getWorld(world.getKey()) != null) {
                throw new IOException("Legacy world is already loaded and cannot be renamed: " + world.getKey());
            }
            if (Files.exists(current)) {
                throw new IOException("Both legacy and current world folders exist: " + legacy + " / " + current);
            }
            move(legacy, current);
        }
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }

    private static Map<String, String> worldNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("tycoon_spawn", "valoria_spawn");
        names.put("tycoon_farm_mine", "valoria_farm_mine");
        names.put("tycoon_farm_fields", "valoria_farm_fields");
        names.put("tycoon_farm_fishing", "valoria_farm_fishing");
        names.put("tycoon_farm_forest", "valoria_farm_forest");
        names.put("tycoon_plots", "valoria_plots");
        return Map.copyOf(names);
    }
}
