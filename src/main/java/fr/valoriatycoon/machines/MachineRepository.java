package fr.valoriatycoon.machines;

import fr.valoriatycoon.database.SqliteDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** SQLite repository for generators, storage, auto-sell and money-only upgrades. */
public final class MachineRepository {
    private final SqliteDatabase database;

    public MachineRepository(SqliteDatabase database) {
        this.database = database;
    }

    public CompletableFuture<MachineSnapshot> loadAll() {
        return database.submit(connection -> {
            List<PlacedMachine> machines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM machines");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) machines.add(map(rows));
            }
            return new MachineSnapshot(machines);
        });
    }

    public CompletableFuture<PlacedMachine> create(
            UUID tycoonId,
            UUID ownerId,
            MachineDefinition definition,
            MachinePosition position
    ) {
        return database.submit(connection -> {
            long now = System.currentTimeMillis();
            PlacedMachine machine = new PlacedMachine(
                    UUID.randomUUID(), tycoonId, ownerId, definition.id(),
                    position.worldName(), position.x(), position.y(), position.z(),
                    0L, false, 1, 1,
                    now + definition.productionInterval().toMillis(), Instant.ofEpochMilli(now)
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO machines (
                        machine_id, tycoon_id, owner_uuid, machine_type,
                        world_name, block_x, block_y, block_z,
                        stored_amount, auto_sell, next_run_at, created_at, updated_at,
                        speed_level, sell_price_level
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, 1, 1)
                    """)) {
                statement.setString(1, machine.id().toString());
                statement.setString(2, tycoonId.toString());
                statement.setString(3, ownerId.toString());
                statement.setString(4, definition.id());
                statement.setString(5, position.worldName());
                statement.setInt(6, position.x());
                statement.setInt(7, position.y());
                statement.setInt(8, position.z());
                statement.setLong(9, machine.nextRunAtMillis());
                statement.setLong(10, now);
                statement.setLong(11, now);
                statement.executeUpdate();
            }
            return machine;
        });
    }

    public CompletableFuture<Optional<PlacedMachine>> remove(UUID machineId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Optional<PlacedMachine> machine = select(connection, machineId);
            if (machine.isEmpty()) return Optional.empty();
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM machines WHERE machine_id = ?")) {
                statement.setString(1, machineId.toString());
                statement.executeUpdate();
            }
            return machine;
        }));
    }

    public CompletableFuture<PlacedMachine> toggleAutoSell(UUID machineId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE machines
                    SET auto_sell = CASE auto_sell WHEN 0 THEN 1 ELSE 0 END, updated_at = ?
                    WHERE machine_id = ?
                    """)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, machineId.toString());
                requireOne(statement);
            }
            return select(connection, machineId).orElseThrow();
        }));
    }

    public CompletableFuture<PlacedMachine> collect(UUID machineId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            PlacedMachine before = select(connection, machineId).orElseThrow();
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE machines SET stored_amount = 0, updated_at = ? WHERE machine_id = ?")) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, machineId.toString());
                requireOne(statement);
            }
            return before;
        }));
    }

    public CompletableFuture<MachineUpgradeResult> purchaseUpgrade(
            UUID machineId,
            UUID ownerId,
            MachineUpgradeType type,
            int maximumLevel,
            long cost
    ) {
        return database.submit(connection -> inTransaction(connection, () -> {
            PlacedMachine machine = select(connection, machineId).orElse(null);
            if (machine == null) {
                return new MachineUpgradeResult(MachineUpgradeStatus.MACHINE_MISSING, null, 0L, 0L);
            }
            if (!machine.ownerId().equals(ownerId)) {
                return new MachineUpgradeResult(MachineUpgradeStatus.NOT_OWNER, machine, 0L, 0L);
            }
            int level = type == MachineUpgradeType.SPEED ? machine.speedLevel() : machine.sellPriceLevel();
            long balance = selectBalance(connection, ownerId);
            if (level >= maximumLevel) {
                return new MachineUpgradeResult(MachineUpgradeStatus.MAXIMUM_LEVEL, machine, 0L, balance);
            }
            if (balance < cost) {
                return new MachineUpgradeResult(MachineUpgradeStatus.INSUFFICIENT_FUNDS, machine, cost, balance);
            }
            int nextLevel = level + 1;
            String column = type == MachineUpgradeType.SPEED ? "speed_level" : "sell_price_level";
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE machines SET " + column + " = ?, updated_at = ? WHERE machine_id = ?")) {
                statement.setInt(1, nextLevel);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, machineId.toString());
                requireOne(statement);
            }
            long resulting = balance - cost;
            updateBalance(connection, ownerId, resulting);
            insertUpgradeAudit(connection, ownerId, machine, type, nextLevel, cost);
            return new MachineUpgradeResult(
                    MachineUpgradeStatus.SUCCESS,
                    machine.withUpgrade(type, nextLevel),
                    cost,
                    resulting
            );
        }));
    }

    public CompletableFuture<MachineCycleResult> runCycle(
            UUID machineId,
            MachineDefinition definition,
            long outputAmount,
            long intervalMillis,
            long sellPriceCents
    ) {
        if (outputAmount < 1L) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Machine output amount must be positive")
            );
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            PlacedMachine machine = select(connection, machineId).orElse(null);
            if (machine == null) {
                return new MachineCycleResult(MachineCycleStatus.MACHINE_MISSING, null, 0L, -1L);
            }
            long nextRun = System.currentTimeMillis() + intervalMillis;
            if (!machine.autoSell() && machine.storedAmount() >= definition.storageCapacity()) {
                updateCycle(connection, machine.id(), machine.storedAmount(), nextRun);
                return new MachineCycleResult(
                        MachineCycleStatus.STORAGE_FULL,
                        machine.afterCycle(machine.storedAmount(), nextRun),
                        0L,
                        -1L
                );
            }
            long stored = machine.storedAmount();
            long credited = 0L;
            long ownerBalance = -1L;
            if (machine.autoSell()) {
                credited = Math.multiplyExact(sellPriceCents, outputAmount);
                ownerBalance = creditMoney(connection, machine.ownerId(), credited, machine);
            } else {
                stored = Math.min(
                        definition.storageCapacity(),
                        saturatingAdd(stored, outputAmount)
                );
            }
            updateCycle(connection, machine.id(), stored, nextRun);
            return new MachineCycleResult(
                    MachineCycleStatus.PRODUCED,
                    machine.afterCycle(stored, nextRun),
                    credited,
                    ownerBalance
            );
        }));
    }

    private long selectBalance(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Missing machine owner account");
                return result.getLong(1);
            }
        }
    }

    private void updateBalance(Connection connection, UUID playerId, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tycoon_players SET balance_cents = ?, updated_at = ? WHERE player_uuid = ?")) {
            statement.setLong(1, balance);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            requireOne(statement);
        }
    }

    private long creditMoney(Connection connection, UUID playerId, long amount, PlacedMachine machine)
            throws SQLException {
        long before = selectBalance(connection, playerId);
        if (amount > Long.MAX_VALUE - before) throw new SQLException("Generator payout overflow");
        long after = before + amount;
        updateBalance(connection, playerId, after);
        insertEconomyAudit(connection, playerId, machine, "MACHINE_SELL", amount, "generator:auto-sell");
        return after;
    }

    private void insertUpgradeAudit(
            Connection connection,
            UUID ownerId,
            PlacedMachine machine,
            MachineUpgradeType type,
            int level,
            long cost
    ) throws SQLException {
        insertEconomyAudit(
                connection,
                ownerId,
                machine,
                "MACHINE_UPGRADE",
                cost,
                "generator:upgrade:" + type.name().toLowerCase(java.util.Locale.ROOT) + ':' + level
        );
    }

    private void insertEconomyAudit(
            Connection connection,
            UUID playerId,
            PlacedMachine machine,
            String transactionType,
            long amount,
            String reason
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid,
                     amount_cents, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, transactionType);
            statement.setString(3, transactionType.equals("MACHINE_UPGRADE") ? playerId.toString() : null);
            statement.setString(4, transactionType.equals("MACHINE_SELL") ? playerId.toString() : null);
            statement.setLong(5, amount);
            statement.setString(6, reason + ':' + machine.machineType());
            statement.setLong(7, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void updateCycle(Connection connection, UUID id, long stored, long nextRun) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE machines SET stored_amount = ?, next_run_at = ?, updated_at = ? WHERE machine_id = ?")) {
            statement.setLong(1, stored);
            statement.setLong(2, nextRun);
            statement.setLong(3, System.currentTimeMillis());
            statement.setString(4, id.toString());
            requireOne(statement);
        }
    }

    private Optional<PlacedMachine> select(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM machines WHERE machine_id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private PlacedMachine map(ResultSet row) throws SQLException {
        return new PlacedMachine(
                UUID.fromString(row.getString("machine_id")),
                UUID.fromString(row.getString("tycoon_id")),
                UUID.fromString(row.getString("owner_uuid")),
                row.getString("machine_type"),
                row.getString("world_name"),
                row.getInt("block_x"), row.getInt("block_y"), row.getInt("block_z"),
                row.getLong("stored_amount"), row.getInt("auto_sell") == 1,
                row.getInt("speed_level"), row.getInt("sell_price_level"),
                row.getLong("next_run_at"), Instant.ofEpochMilli(row.getLong("created_at"))
        );
    }

    private <T> T inTransaction(Connection connection, Work<T> work) throws Exception {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.execute();
            connection.commit();
            return result;
        } catch (Exception exception) {
            try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private void requireOne(PreparedStatement statement) throws SQLException {
        if (statement.executeUpdate() != 1) throw new SQLException("Expected one updated row");
    }

    private static long saturatingAdd(long left, long right) {
        return right > 0 && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    @FunctionalInterface private interface Work<T> { T execute() throws Exception; }
}
