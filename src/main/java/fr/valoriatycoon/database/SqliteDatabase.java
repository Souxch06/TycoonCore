package fr.valoriatycoon.database;

import fr.valoriatycoon.utils.NamedThreadFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single-writer asynchronous SQLite gateway.
 *
 * <p>One plugin-owned worker owns the JDBC connection. This avoids concurrent SQLite writes,
 * keeps all SQL away from the server thread, and provides deterministic transaction ordering.</p>
 */
public final class SqliteDatabase {
    private static final int CURRENT_SCHEMA_VERSION = 26;

    private final Path databaseFile;
    private final int busyTimeoutMillis;
    private final Logger logger;
    private final ExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private volatile Connection connection;
    private volatile boolean initialized;

    public SqliteDatabase(Path databaseFile, int busyTimeoutMillis, Logger logger) {
        this.databaseFile = Objects.requireNonNull(databaseFile, "databaseFile");
        this.busyTimeoutMillis = busyTimeoutMillis;
        this.logger = Objects.requireNonNull(logger, "logger");
        this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory(
                "ValoriaTycoon-SQLite",
                (thread, error) -> logger.log(Level.SEVERE, "Uncaught database thread failure", error)
        ));
    }

    public CompletableFuture<Void> start() {
        if (!started.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Database has already been started"));
        }
        return CompletableFuture.runAsync(() -> {
            try {
                Path parent = databaseFile.toAbsolutePath().normalize().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Class.forName("org.sqlite.JDBC");
                Connection opened = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath().normalize());
                connection = opened;
                configure(opened);
                migrate(opened);
                initialized = true;
            } catch (Exception exception) {
                closeConnectionQuietly(connection);
                connection = null;
                throw new DatabaseException("Cannot initialize SQLite database", exception);
            }
        }, executor);
    }

    public <T> CompletableFuture<T> submit(SqlOperation<T> operation) {
        Objects.requireNonNull(operation, "operation");
        if (!accepting.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Database is shutting down"));
        }
        try {
            return CompletableFuture.supplyAsync(() -> {
                Connection activeConnection = connection;
                if (!initialized || activeConnection == null) {
                    throw new DatabaseException("Database is not initialized", null);
                }
                try {
                    return operation.execute(activeConnection);
                } catch (DatabaseException exception) {
                    throw exception;
                } catch (Exception exception) {
                    throw new DatabaseException("SQLite operation failed", exception);
                }
            }, executor);
        } catch (RejectedExecutionException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Drains queued work, closes the owned connection, and waits at most the supplied duration. */
    public void close(Duration timeout) {
        if (!accepting.compareAndSet(true, false)) {
            return;
        }
        try {
            executor.submit(() -> {
                initialized = false;
                closeConnectionQuietly(connection);
                connection = null;
            }).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while closing SQLite", exception);
        } catch (ExecutionException | TimeoutException | RejectedExecutionException exception) {
            logger.log(Level.WARNING, "Could not close SQLite cleanly", exception);
        } finally {
            executor.shutdownNow();
        }
    }

    private void configure(Connection opened) throws SQLException {
        try (Statement statement = opened.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("PRAGMA busy_timeout = " + busyTimeoutMillis);
        }
    }

    private void migrate(Connection opened) throws SQLException {
        int version;
        try (Statement statement = opened.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            version = result.next() ? result.getInt(1) : 0;
        }
        if (version > CURRENT_SCHEMA_VERSION) {
            throw new SQLException("Database schema " + version + " is newer than supported " + CURRENT_SCHEMA_VERSION);
        }
        if (version < 1) {
            applyVersionOne(opened);
            version = 1;
        }
        if (version < 2) {
            applyVersionTwo(opened);
            version = 2;
        }
        if (version < 3) {
            applyVersionThree(opened);
            version = 3;
        }
        if (version < 4) {
            applyVersionFour(opened);
            version = 4;
        }
        if (version < 5) {
            applyVersionFive(opened);
            version = 5;
        }
        if (version < 6) {
            applyVersionSix(opened);
            version = 6;
        }
        if (version < 7) {
            applyVersionSeven(opened);
            version = 7;
        }
        if (version < 8) {
            applyVersionEight(opened);
            version = 8;
        }
        if (version < 9) {
            applyVersionNine(opened);
            version = 9;
        }
        if (version < 10) {
            applyVersionTen(opened);
            version = 10;
        }
        if (version < 11) {
            applyVersionEleven(opened);
            version = 11;
        }
        if (version < 12) {
            applyVersionTwelve(opened);
            version = 12;
        }
        if (version < 13) {
            applyVersionThirteen(opened);
            version = 13;
        }
        if (version < 14) {
            applyVersionFourteen(opened);
            version = 14;
        }
        if (version < 15) {
            applyVersionFifteen(opened);
            version = 15;
        }
        if (version < 16) {
            applyVersionSixteen(opened);
            version = 16;
        }
        if (version < 17) {
            applyVersionSeventeen(opened);
            version = 17;
        }
        if (version < 18) {
            applyVersionEighteen(opened);
            version = 18;
        }
        if (version < 19) {
            applyVersionNineteen(opened);
            version = 19;
        }
        if (version < 20) {
            applyVersionTwenty(opened);
            version = 20;
        }
        if (version < 21) {
            applyVersionTwentyOne(opened);
            version = 21;
        }
        if (version < 22) {
            applyVersionTwentyTwo(opened);
            version = 22;
        }
        if (version < 23) {
            applyVersionTwentyThree(opened);
            version = 23;
        }
        if (version < 24) {
            applyVersionTwentyFour(opened);
            version = 24;
        }
        if (version < 25) {
            applyVersionTwentyFive(opened);
            version = 25;
        }
        if (version < 26) {
            applyVersionTwentySix(opened);
        }
    }

    private void applyVersionOne(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tycoon_players (
                        player_uuid TEXT PRIMARY KEY NOT NULL,
                        last_known_name TEXT,
                        balance_cents INTEGER NOT NULL DEFAULT 0 CHECK (balance_cents >= 0),
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS economy_transactions (
                        transaction_id TEXT PRIMARY KEY NOT NULL,
                        transaction_type TEXT NOT NULL,
                        source_uuid TEXT,
                        target_uuid TEXT,
                        amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0),
                        reason TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_economy_source ON economy_transactions(source_uuid, created_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_economy_target ON economy_transactions(target_uuid, created_at)");
            statement.execute("PRAGMA user_version = 1");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwo(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    ALTER TABLE tycoon_players
                    ADD COLUMN autosell_enabled INTEGER NOT NULL DEFAULT 0
                    CHECK (autosell_enabled IN (0, 1))
                    """);
            statement.executeUpdate("""
                    CREATE TABLE pending_block_regenerations (
                        world_name TEXT NOT NULL,
                        block_x INTEGER NOT NULL,
                        block_y INTEGER NOT NULL,
                        block_z INTEGER NOT NULL,
                        block_data TEXT NOT NULL,
                        due_at INTEGER NOT NULL,
                        PRIMARY KEY (world_name, block_x, block_y, block_z)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_pending_regeneration_due
                    ON pending_block_regenerations(due_at)
                    """);
            statement.execute("PRAGMA user_version = 2");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionThree(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    ALTER TABLE tycoon_players
                    ADD COLUMN autosell_level INTEGER NOT NULL DEFAULT 0
                    CHECK (autosell_level BETWEEN 0 AND 5)
                    """);
            // Auto-sell was unlocked globally in schema v2, so existing accounts keep level-one access.
            statement.executeUpdate("""
                    UPDATE tycoon_players
                    SET autosell_level = 1
                    """);
            statement.execute("PRAGMA user_version = 3");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionFour(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE player_tools (
                        player_uuid TEXT NOT NULL,
                        tool_type TEXT NOT NULL,
                        tool_level INTEGER NOT NULL DEFAULT 1 CHECK (tool_level >= 1),
                        tool_experience INTEGER NOT NULL DEFAULT 0 CHECK (tool_experience >= 0),
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, tool_type)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE tool_capabilities (
                        player_uuid TEXT NOT NULL,
                        tool_type TEXT NOT NULL,
                        capability_id TEXT NOT NULL,
                        capability_level INTEGER NOT NULL DEFAULT 0 CHECK (capability_level >= 0),
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, tool_type, capability_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_tool_capabilities_player
                    ON tool_capabilities(player_uuid, tool_type)
                    """);
            statement.execute("PRAGMA user_version = 4");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionFive(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE tycoons (
                        tycoon_id TEXT PRIMARY KEY NOT NULL,
                        owner_uuid TEXT UNIQUE NOT NULL,
                        group_id TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        plot_index INTEGER NOT NULL,
                        minimum_x INTEGER NOT NULL,
                        maximum_x INTEGER NOT NULL,
                        minimum_z INTEGER NOT NULL,
                        maximum_z INTEGER NOT NULL,
                        floor_y INTEGER NOT NULL,
                        build_minimum_y INTEGER NOT NULL,
                        build_maximum_y INTEGER NOT NULL,
                        tycoon_level INTEGER NOT NULL DEFAULT 1 CHECK (tycoon_level >= 1),
                        prestige_level INTEGER NOT NULL DEFAULT 0 CHECK (prestige_level >= 0),
                        progress_points INTEGER NOT NULL DEFAULT 0 CHECK (progress_points >= 0),
                        total_production INTEGER NOT NULL DEFAULT 0 CHECK (total_production >= 0),
                        playtime_seconds INTEGER NOT NULL DEFAULT 0 CHECK (playtime_seconds >= 0),
                        status TEXT NOT NULL CHECK (status IN ('PREPARING', 'ACTIVE', 'DELETING')),
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        UNIQUE (group_id, plot_index)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE tycoon_members (
                        tycoon_id TEXT NOT NULL,
                        player_uuid TEXT NOT NULL,
                        joined_at INTEGER NOT NULL,
                        PRIMARY KEY (tycoon_id, player_uuid),
                        FOREIGN KEY (tycoon_id) REFERENCES tycoons(tycoon_id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("CREATE INDEX idx_tycoons_world ON tycoons(world_name, status)");
            statement.executeUpdate("CREATE INDEX idx_tycoon_members_player ON tycoon_members(player_uuid)");
            statement.execute("PRAGMA user_version = 5");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionSix(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    ALTER TABLE tycoons
                    ADD COLUMN plot_size_level INTEGER NOT NULL DEFAULT 1 CHECK (plot_size_level >= 1)
                    """);
            statement.executeUpdate("""
                    ALTER TABLE tycoons
                    ADD COLUMN hopper_limit_level INTEGER NOT NULL DEFAULT 1 CHECK (hopper_limit_level >= 1)
                    """);
            statement.executeUpdate("""
                    ALTER TABLE tycoons
                    ADD COLUMN member_limit_level INTEGER NOT NULL DEFAULT 1 CHECK (member_limit_level >= 1)
                    """);
            // Existing installations keep their former full-size/member access as closely as possible.
            statement.executeUpdate("""
                    UPDATE tycoons
                    SET plot_size_level = 5, hopper_limit_level = 6, member_limit_level = 2
                    """);
            statement.executeUpdate("""
                    CREATE TABLE tycoon_hoppers (
                        tycoon_id TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        block_x INTEGER NOT NULL,
                        block_y INTEGER NOT NULL,
                        block_z INTEGER NOT NULL,
                        placed_at INTEGER NOT NULL,
                        PRIMARY KEY (world_name, block_x, block_y, block_z),
                        FOREIGN KEY (tycoon_id) REFERENCES tycoons(tycoon_id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("CREATE INDEX idx_tycoon_hoppers_owner ON tycoon_hoppers(tycoon_id)");
            statement.execute("PRAGMA user_version = 6");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionSeven(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    ALTER TABLE player_tools
                    ADD COLUMN special_coins INTEGER NOT NULL DEFAULT 0 CHECK (special_coins >= 0)
                    """);
            statement.executeUpdate("""
                    CREATE TABLE tool_coin_transactions (
                        transaction_id TEXT PRIMARY KEY NOT NULL,
                        player_uuid TEXT NOT NULL,
                        tool_type TEXT NOT NULL,
                        amount_delta INTEGER NOT NULL,
                        balance_after INTEGER NOT NULL CHECK (balance_after >= 0),
                        transaction_type TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_tool_coin_transactions_player
                    ON tool_coin_transactions(player_uuid, tool_type, created_at)
                    """);
            statement.execute("PRAGMA user_version = 7");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionEight(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    UPDATE tool_capabilities
                    SET capability_level = 1, updated_at = strftime('%s', 'now') * 1000
                    WHERE capability_id = 'timber' AND capability_level = 0
                    """);
            statement.execute("PRAGMA user_version = 8");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionNine(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE machines (
                        machine_id TEXT PRIMARY KEY NOT NULL,
                        tycoon_id TEXT NOT NULL,
                        owner_uuid TEXT NOT NULL,
                        machine_type TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        block_x INTEGER NOT NULL,
                        block_y INTEGER NOT NULL,
                        block_z INTEGER NOT NULL,
                        stored_amount INTEGER NOT NULL DEFAULT 0 CHECK (stored_amount >= 0),
                        auto_sell INTEGER NOT NULL DEFAULT 0 CHECK (auto_sell IN (0, 1)),
                        next_run_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        UNIQUE (world_name, block_x, block_y, block_z),
                        FOREIGN KEY (tycoon_id) REFERENCES tycoons(tycoon_id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE machine_energy (
                        tycoon_id TEXT PRIMARY KEY NOT NULL,
                        energy INTEGER NOT NULL DEFAULT 0 CHECK (energy >= 0),
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (tycoon_id) REFERENCES tycoons(tycoon_id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("CREATE INDEX idx_machines_tycoon ON machines(tycoon_id)");
            statement.executeUpdate("CREATE INDEX idx_machines_due ON machines(next_run_at)");
            statement.execute("PRAGMA user_version = 9");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    ALTER TABLE machines
                    ADD COLUMN speed_level INTEGER NOT NULL DEFAULT 1 CHECK (speed_level >= 1)
                    """);
            statement.executeUpdate("""
                    ALTER TABLE machines
                    ADD COLUMN sell_price_level INTEGER NOT NULL DEFAULT 1 CHECK (sell_price_level >= 1)
                    """);
            statement.executeUpdate("DROP TABLE IF EXISTS machine_energy");
            statement.execute("PRAGMA user_version = 10");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionEleven(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE quest_progress (
                        player_uuid TEXT NOT NULL,
                        quest_id TEXT NOT NULL,
                        progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0),
                        completions INTEGER NOT NULL DEFAULT 0 CHECK (completions >= 0),
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, quest_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE quest_consumed (
                        player_uuid TEXT NOT NULL,
                        rarity TEXT NOT NULL,
                        consumed INTEGER NOT NULL DEFAULT 0 CHECK (consumed >= 0),
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, rarity)
                    )
                    """);
            statement.executeUpdate("CREATE INDEX idx_quest_progress_player ON quest_progress(player_uuid)");
            statement.execute("PRAGMA user_version = 11");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwelve(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE player_professions (
                        player_uuid TEXT NOT NULL,
                        profession_type TEXT NOT NULL,
                        profession_level INTEGER NOT NULL DEFAULT 1
                            CHECK (profession_level >= 1),
                        profession_experience INTEGER NOT NULL DEFAULT 0
                            CHECK (profession_experience >= 0),
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, profession_type),
                        FOREIGN KEY (player_uuid)
                            REFERENCES tycoon_players(player_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_player_professions_player
                    ON player_professions(player_uuid)
                    """);
            statement.execute("PRAGMA user_version = 12");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionThirteen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE tutorial_progress (
                        player_uuid TEXT PRIMARY KEY NOT NULL,
                        step_index INTEGER NOT NULL DEFAULT 0
                            CHECK (step_index BETWEEN 0 AND 6),
                        progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0),
                        completed INTEGER NOT NULL DEFAULT 0 CHECK (completed IN (0, 1)),
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (player_uuid)
                            REFERENCES tycoon_players(player_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.execute("PRAGMA user_version = 13");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionFourteen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            // The v14 farm layout replaces uniform worlds with bounded rank zones.
            statement.executeUpdate("DELETE FROM pending_block_regenerations");
            statement.execute("PRAGMA user_version = 14");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionFifteen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            // Circular islands, bridges and terraced mines replace the first square-zone layout.
            statement.executeUpdate("DELETE FROM pending_block_regenerations");
            statement.execute("PRAGMA user_version = 15");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionSixteen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            // Massive waterless fields and prison-style mines replace the v15 resource terrain.
            statement.executeUpdate("DELETE FROM pending_block_regenerations");
            statement.execute("PRAGMA user_version = 16");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionSeventeen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            // Enclosed caverns and longer ranked bridges replace the open prison quarries.
            statement.executeUpdate("DELETE FROM pending_block_regenerations");
            statement.execute("PRAGMA user_version = 17");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionEighteen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            // Organic 2048-block caverns replace the smaller geometric v17 rooms.
            statement.executeUpdate("DELETE FROM pending_block_regenerations");
            statement.execute("PRAGMA user_version = 18");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionNineteen(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            // Organic 2048-block forest islands replace the uniform v18 tree grids.
            statement.executeUpdate("DELETE FROM pending_block_regenerations");
            statement.execute("PRAGMA user_version = 19");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwenty(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            migrateWorldName(statement, "tycoon_spawn", "valoria_spawn");
            migrateWorldName(statement, "tycoon_farm_mine", "valoria_farm_mine");
            migrateWorldName(statement, "tycoon_farm_fields", "valoria_farm_fields");
            migrateWorldName(statement, "tycoon_farm_fishing", "valoria_farm_fishing");
            migrateWorldName(statement, "tycoon_farm_forest", "valoria_farm_forest");
            migrateWorldName(statement, "tycoon_plots", "valoria_plots");
            statement.execute("PRAGMA user_version = 20");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void migrateWorldName(Statement statement, String legacy, String current)
            throws SQLException {
        for (String table : List.of(
                "pending_block_regenerations",
                "tycoons",
                "tycoon_hoppers",
                "machines"
        )) {
            statement.executeUpdate(
                    "UPDATE " + table + " SET world_name = '" + current
                            + "' WHERE world_name = '" + legacy + "'"
            );
        }
    }

    private void applyVersionTwentyOne(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE player_pets (
                        player_uuid TEXT NOT NULL,
                        pet_id TEXT NOT NULL,
                        pet_level INTEGER NOT NULL DEFAULT 1 CHECK (pet_level >= 1),
                        pet_experience INTEGER NOT NULL DEFAULT 0 CHECK (pet_experience >= 0),
                        active INTEGER NOT NULL DEFAULT 0 CHECK (active IN (0, 1)),
                        obtained_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, pet_id),
                        FOREIGN KEY (player_uuid)
                            REFERENCES tycoon_players(player_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE UNIQUE INDEX idx_player_pets_one_active
                    ON player_pets(player_uuid)
                    WHERE active = 1
                    """);
            statement.executeUpdate("""
                    CREATE TABLE pet_transactions (
                        transaction_id TEXT PRIMARY KEY NOT NULL,
                        player_uuid TEXT NOT NULL,
                        pet_id TEXT NOT NULL,
                        action TEXT NOT NULL,
                        amount_cents INTEGER NOT NULL DEFAULT 0 CHECK (amount_cents >= 0),
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY (player_uuid)
                            REFERENCES tycoon_players(player_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_pet_transactions_player
                    ON pet_transactions(player_uuid, created_at)
                    """);
            statement.execute("PRAGMA user_version = 21");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwentyTwo(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE consumed_pet_keys (
                        key_id TEXT PRIMARY KEY NOT NULL,
                        player_uuid TEXT NOT NULL,
                        pet_id TEXT NOT NULL,
                        consumed_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_consumed_pet_keys_player
                    ON consumed_pet_keys(player_uuid, consumed_at)
                    """);
            statement.execute("PRAGMA user_version = 22");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwentyThree(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    ALTER TABLE player_pets
                    ADD COLUMN chromatic INTEGER NOT NULL DEFAULT 0
                        CHECK (chromatic IN (0, 1))
                    """);
            statement.executeUpdate("""
                    CREATE TABLE issued_pet_eggs (
                        egg_id TEXT PRIMARY KEY NOT NULL,
                        pet_id TEXT NOT NULL,
                        chromatic INTEGER NOT NULL CHECK (chromatic IN (0, 1)),
                        pet_level INTEGER NOT NULL CHECK (pet_level >= 1),
                        pet_experience INTEGER NOT NULL CHECK (pet_experience >= 0),
                        issued_to_uuid TEXT NOT NULL,
                        source TEXT NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0 CHECK (delivered IN (0, 1)),
                        consumed_by_uuid TEXT,
                        issued_at INTEGER NOT NULL,
                        delivered_at INTEGER,
                        consumed_at INTEGER
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_issued_pet_eggs_pending
                    ON issued_pet_eggs(issued_to_uuid, delivered)
                    """);
            statement.execute("PRAGMA user_version = 23");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwentyFour(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE issued_crate_keys (
                        key_id TEXT PRIMARY KEY NOT NULL,
                        crate_type TEXT NOT NULL
                            CHECK (crate_type IN ('VOTE', 'QUEST', 'FARM', 'COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
                        issued_to_uuid TEXT NOT NULL,
                        source TEXT NOT NULL,
                        source_reference TEXT NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0 CHECK (delivered IN (0, 1)),
                        issued_at INTEGER NOT NULL,
                        delivered_at INTEGER,
                        consumed_by_uuid TEXT,
                        consumed_at INTEGER,
                        UNIQUE (source, source_reference)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_issued_crate_keys_pending
                    ON issued_crate_keys(issued_to_uuid, delivered, consumed_by_uuid)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_issued_crate_keys_type
                    ON issued_crate_keys(crate_type, issued_at)
                    """);
            statement.execute("PRAGMA user_version = 24");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwentyFive(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("ALTER TABLE issued_crate_keys RENAME TO issued_crate_keys_v24");
            statement.executeUpdate("DROP INDEX IF EXISTS idx_issued_crate_keys_pending");
            statement.executeUpdate("DROP INDEX IF EXISTS idx_issued_crate_keys_type");
            statement.executeUpdate("""
                    CREATE TABLE issued_crate_keys (
                        key_id TEXT PRIMARY KEY NOT NULL,
                        crate_type TEXT NOT NULL
                            CHECK (crate_type IN ('VOTE', 'QUEST', 'FARM', 'COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'VALORIA')),
                        issued_to_uuid TEXT NOT NULL,
                        source TEXT NOT NULL,
                        source_reference TEXT NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0 CHECK (delivered IN (0, 1)),
                        issued_at INTEGER NOT NULL,
                        delivered_at INTEGER,
                        consumed_by_uuid TEXT,
                        consumed_at INTEGER,
                        UNIQUE (source, source_reference)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO issued_crate_keys
                        (key_id, crate_type, issued_to_uuid, source, source_reference,
                         delivered, issued_at, delivered_at, consumed_by_uuid, consumed_at)
                    SELECT key_id, crate_type, issued_to_uuid, source, source_reference,
                           delivered, issued_at, delivered_at, consumed_by_uuid, consumed_at
                    FROM issued_crate_keys_v24
                    """);
            statement.executeUpdate("DROP TABLE issued_crate_keys_v24");
            statement.executeUpdate("""
                    CREATE INDEX idx_issued_crate_keys_pending
                    ON issued_crate_keys(issued_to_uuid, delivered, consumed_by_uuid)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_issued_crate_keys_type
                    ON issued_crate_keys(crate_type, issued_at)
                    """);
            statement.execute("PRAGMA user_version = 25");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void applyVersionTwentySix(Connection opened) throws SQLException {
        boolean previousAutoCommit = opened.getAutoCommit();
        opened.setAutoCommit(false);
        try (Statement statement = opened.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE issued_crate_rewards (
                        reward_id TEXT PRIMARY KEY NOT NULL,
                        key_id TEXT UNIQUE NOT NULL,
                        crate_type TEXT NOT NULL
                            CHECK (crate_type IN ('VOTE', 'QUEST', 'FARM', 'COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'VALORIA')),
                        reward_definition TEXT NOT NULL,
                        reward_kind TEXT NOT NULL
                            CHECK (reward_kind IN ('MONEY_BAG', 'COIN_BAG', 'UNIVERSAL_COIN_BAG',
                                                   'XP_VIAL', 'RESOURCE_BUNDLE', 'VANILLA_ITEM',
                                                   'CRATE_KEYS', 'PET_KEYS', 'GENERATORS')),
                        payload TEXT NOT NULL,
                        issued_to_uuid TEXT NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0 CHECK (delivered IN (0, 1)),
                        issued_at INTEGER NOT NULL,
                        delivered_at INTEGER,
                        consumed_by_uuid TEXT,
                        consumed_at INTEGER,
                        claim_delivered INTEGER NOT NULL DEFAULT 0 CHECK (claim_delivered IN (0, 1)),
                        claim_delivered_at INTEGER,
                        FOREIGN KEY (key_id) REFERENCES issued_crate_keys(key_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_issued_crate_rewards_pending
                    ON issued_crate_rewards(issued_to_uuid, delivered, consumed_by_uuid)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX idx_issued_crate_rewards_consumed
                    ON issued_crate_rewards(consumed_by_uuid, claim_delivered, consumed_at)
                    """);
            statement.execute("PRAGMA user_version = 26");
            opened.commit();
        } catch (SQLException exception) {
            rollbackQuietly(opened);
            throw exception;
        } finally {
            opened.setAutoCommit(previousAutoCommit);
        }
    }

    private void rollbackQuietly(Connection activeConnection) {
        try {
            activeConnection.rollback();
        } catch (SQLException rollbackFailure) {
            logger.log(Level.SEVERE, "SQLite rollback failed", rollbackFailure);
        }
    }

    private void closeConnectionQuietly(Connection activeConnection) {
        if (activeConnection == null) {
            return;
        }
        try {
            activeConnection.close();
        } catch (SQLException exception) {
            logger.log(Level.WARNING, "SQLite connection close failed", exception);
        }
    }
}
