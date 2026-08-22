package fr.valoriatycoon.tycoon;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.upgrades.PlotUpgradeDefinition;
import fr.valoriatycoon.upgrades.PlotUpgradeResult;
import fr.valoriatycoon.upgrades.PlotUpgradeStatus;
import fr.valoriatycoon.upgrades.PlotUpgradeType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Authoritative asynchronous Tycoon, plot-slot and member repository. */
public final class TycoonRepository {
    private final SqliteDatabase database;

    public TycoonRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public CompletableFuture<TycoonDataSnapshot> loadAll() {
        return database.submit(connection -> {
            List<Tycoon> tycoons = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM tycoons");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    tycoons.add(map(rows));
                }
            }
            Map<UUID, Set<UUID>> members = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT tycoon_id, player_uuid FROM tycoon_members");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    UUID tycoonId = UUID.fromString(rows.getString(1));
                    members.computeIfAbsent(tycoonId, ignored -> new HashSet<>())
                            .add(UUID.fromString(rows.getString(2)));
                }
            }
            Map<UUID, Set<HopperPosition>> hoppers = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT tycoon_id, world_name, block_x, block_y, block_z FROM tycoon_hoppers
                    """); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    UUID tycoonId = UUID.fromString(rows.getString(1));
                    hoppers.computeIfAbsent(tycoonId, ignored -> new HashSet<>()).add(
                            new HopperPosition(rows.getString(2), rows.getInt(3), rows.getInt(4), rows.getInt(5))
                    );
                }
            }
            return new TycoonDataSnapshot(tycoons, members, hoppers);
        });
    }

    public CompletableFuture<TycoonAllocationResult> allocate(UUID ownerId, TycoonPlotGroup group) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Optional<Tycoon> existing = selectByOwner(connection, ownerId);
            if (existing.isPresent()) {
                return new TycoonAllocationResult(TycoonAllocationStatus.ALREADY_OWNS, existing.get());
            }
            Set<Integer> used = new HashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT plot_index FROM tycoons WHERE group_id = ?")) {
                statement.setString(1, group.id());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        used.add(rows.getInt(1));
                    }
                }
            }
            int available = -1;
            for (int index = 0; index < group.maximumPlots(); index++) {
                if (!used.contains(index)) {
                    available = index;
                    break;
                }
            }
            if (available < 0) {
                return new TycoonAllocationResult(TycoonAllocationStatus.GROUP_FULL, null);
            }

            long now = System.currentTimeMillis();
            TycoonPlotGroup.Bounds bounds = group.bounds(available);
            Tycoon tycoon = new Tycoon(
                    UUID.randomUUID(), ownerId, group.id(), group.worldName(), available, bounds,
                    group.floorY(), group.buildMinimumY(), group.buildMaximumY(),
                    1, 0, 1, 1, 1,
                    0L, 0L, 0L, TycoonStatus.PREPARING, Instant.ofEpochMilli(now)
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tycoons (
                        tycoon_id, owner_uuid, group_id, world_name, plot_index,
                        minimum_x, maximum_x, minimum_z, maximum_z,
                        floor_y, build_minimum_y, build_maximum_y,
                        tycoon_level, prestige_level, progress_points,
                        total_production, playtime_seconds, status, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, 0, 0, 0, 'PREPARING', ?, ?)
                    """)) {
                statement.setString(1, tycoon.id().toString());
                statement.setString(2, ownerId.toString());
                statement.setString(3, group.id());
                statement.setString(4, group.worldName());
                statement.setInt(5, available);
                statement.setInt(6, bounds.minimumX());
                statement.setInt(7, bounds.maximumX());
                statement.setInt(8, bounds.minimumZ());
                statement.setInt(9, bounds.maximumZ());
                statement.setInt(10, group.floorY());
                statement.setInt(11, group.buildMinimumY());
                statement.setInt(12, group.buildMaximumY());
                statement.setLong(13, now);
                statement.setLong(14, now);
                statement.executeUpdate();
            }
            return new TycoonAllocationResult(TycoonAllocationStatus.SUCCESS, tycoon);
        }));
    }

    public CompletableFuture<Tycoon> updateStatus(UUID tycoonId, TycoonStatus status) {
        return database.submit(connection -> inTransaction(connection, () -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE tycoons SET status = ?, updated_at = ? WHERE tycoon_id = ?
                    """)) {
                statement.setString(1, status.name());
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, tycoonId.toString());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Missing Tycoon for status update: " + tycoonId);
                }
            }
            return selectById(connection, tycoonId)
                    .orElseThrow(() -> new SQLException("Tycoon disappeared after status update"));
        }));
    }

    public CompletableFuture<Optional<Tycoon>> beginPreparation(UUID ownerId) {
        return updateActiveStatus(ownerId, TycoonStatus.PREPARING);
    }

    public CompletableFuture<Optional<Tycoon>> beginDeletion(UUID ownerId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Optional<Tycoon> existing = selectByOwner(connection, ownerId);
            if (existing.isEmpty() || existing.get().status() != TycoonStatus.ACTIVE) {
                return Optional.empty();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE tycoons SET status = 'DELETING', updated_at = ?
                    WHERE tycoon_id = ? AND status = 'ACTIVE'
                    """)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, existing.get().id().toString());
                if (statement.executeUpdate() != 1) {
                    return Optional.empty();
                }
            }
            return Optional.of(existing.get().withStatus(TycoonStatus.DELETING));
        }));
    }

    public CompletableFuture<Void> delete(UUID tycoonId) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tycoons WHERE tycoon_id = ? AND status = 'DELETING'")) {
                statement.setString(1, tycoonId.toString());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Could not finalize Tycoon deletion: " + tycoonId);
                }
            }
            return null;
        });
    }

    public CompletableFuture<PlotUpgradeResult> purchaseUpgrade(
            UUID ownerId,
            PlotUpgradeType type,
            int expectedLevel,
            PlotUpgradeDefinition definition
    ) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Optional<Tycoon> selected = selectByOwner(connection, ownerId);
            if (selected.isEmpty() || selected.get().status() != TycoonStatus.ACTIVE) {
                return new PlotUpgradeResult(
                        PlotUpgradeStatus.NO_ACTIVE_TYCOON, type, expectedLevel, 0L, 0L
                );
            }
            Tycoon tycoon = selected.get();
            int currentLevel = switch (type) {
                case PLOT_SIZE -> tycoon.plotSizeLevel();
                case HOPPER_LIMIT -> tycoon.hopperLimitLevel();
                case MEMBER_LIMIT -> tycoon.memberLimitLevel();
            };
            long balance = selectPlayerBalance(connection, ownerId);
            if (currentLevel != expectedLevel) {
                return new PlotUpgradeResult(
                        PlotUpgradeStatus.PROFILE_STALE, type, currentLevel, 0L, balance
                );
            }
            if (currentLevel >= definition.maximumLevel()) {
                return new PlotUpgradeResult(
                        PlotUpgradeStatus.MAXIMUM_LEVEL, type, currentLevel, 0L, balance
                );
            }
            PlotUpgradeDefinition.Level next = definition.level(currentLevel + 1).orElseThrow();
            if (balance < next.costCents()) {
                return new PlotUpgradeResult(
                        PlotUpgradeStatus.INSUFFICIENT_FUNDS,
                        type,
                        currentLevel,
                        next.costCents(),
                        balance
                );
            }
            long resultingBalance = balance - next.costCents();
            String column = switch (type) {
                case PLOT_SIZE -> "plot_size_level";
                case HOPPER_LIMIT -> "hopper_limit_level";
                case MEMBER_LIMIT -> "member_limit_level";
            };
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tycoons SET " + column + " = ?, updated_at = ? WHERE tycoon_id = ?")) {
                statement.setInt(1, next.level());
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, tycoon.id().toString());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Could not update plot upgrade for " + tycoon.id());
                }
            }
            updatePlayerBalance(connection, ownerId, resultingBalance);
            insertUpgradeAudit(connection, ownerId, type, next.level(), next.costCents());
            return new PlotUpgradeResult(
                    PlotUpgradeStatus.SUCCESS,
                    type,
                    next.level(),
                    next.costCents(),
                    resultingBalance
            );
        }));
    }

    public CompletableFuture<Tycoon> addPlaytime(UUID ownerId, long seconds) {
        if (seconds <= 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Playtime delta must be positive"));
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE tycoons
                    SET playtime_seconds = playtime_seconds + ?, updated_at = ?
                    WHERE owner_uuid = ? AND status = 'ACTIVE'
                    """)) {
                statement.setLong(1, seconds);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, ownerId.toString());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("No active Tycoon for playtime update: " + ownerId);
                }
            }
            return selectByOwner(connection, ownerId).orElseThrow();
        }));
    }

    public CompletableFuture<Void> addHopper(UUID tycoonId, HopperPosition position) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tycoon_hoppers
                        (tycoon_id, world_name, block_x, block_y, block_z, placed_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, tycoonId.toString());
                statement.setString(2, position.worldName());
                statement.setInt(3, position.x());
                statement.setInt(4, position.y());
                statement.setInt(5, position.z());
                statement.setLong(6, System.currentTimeMillis());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Void> clearHoppers(UUID tycoonId) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tycoon_hoppers WHERE tycoon_id = ?")) {
                statement.setString(1, tycoonId.toString());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Void> removeHopper(HopperPosition position) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM tycoon_hoppers
                    WHERE world_name = ? AND block_x = ? AND block_y = ? AND block_z = ?
                    """)) {
                statement.setString(1, position.worldName());
                statement.setInt(2, position.x());
                statement.setInt(3, position.y());
                statement.setInt(4, position.z());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<MemberOperationStatus> addMember(
            UUID ownerId,
            UUID memberId,
            int maximumMembers
    ) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Optional<Tycoon> selected = selectByOwner(connection, ownerId);
            if (selected.isEmpty() || selected.get().status() != TycoonStatus.ACTIVE) {
                return MemberOperationStatus.NOT_FOUND;
            }
            Tycoon tycoon = selected.get();
            if (ownerId.equals(memberId)) {
                return MemberOperationStatus.CANNOT_ADD_OWNER;
            }
            if (isMember(connection, tycoon.id(), memberId)) {
                return MemberOperationStatus.ALREADY_MEMBER;
            }
            if (countMembers(connection, tycoon.id()) >= maximumMembers) {
                return MemberOperationStatus.MEMBER_LIMIT;
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tycoon_members (tycoon_id, player_uuid, joined_at) VALUES (?, ?, ?)
                    """)) {
                statement.setString(1, tycoon.id().toString());
                statement.setString(2, memberId.toString());
                statement.setLong(3, System.currentTimeMillis());
                statement.executeUpdate();
            }
            return MemberOperationStatus.SUCCESS;
        }));
    }

    public CompletableFuture<MemberOperationStatus> removeMember(UUID ownerId, UUID memberId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Optional<Tycoon> selected = selectByOwner(connection, ownerId);
            if (selected.isEmpty()) {
                return MemberOperationStatus.NOT_FOUND;
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM tycoon_members WHERE tycoon_id = ? AND player_uuid = ?
                    """)) {
                statement.setString(1, selected.get().id().toString());
                statement.setString(2, memberId.toString());
                return statement.executeUpdate() == 1
                        ? MemberOperationStatus.SUCCESS
                        : MemberOperationStatus.NOT_MEMBER;
            }
        }));
    }

    private CompletableFuture<Optional<Tycoon>> updateActiveStatus(UUID ownerId, TycoonStatus status) {
        return database.submit(connection -> inTransaction(connection, () -> {
            Optional<Tycoon> existing = selectByOwner(connection, ownerId);
            if (existing.isEmpty() || existing.get().status() != TycoonStatus.ACTIVE) {
                return Optional.empty();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE tycoons SET status = ?, updated_at = ?
                    WHERE tycoon_id = ? AND status = 'ACTIVE'
                    """)) {
                statement.setString(1, status.name());
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, existing.get().id().toString());
                if (statement.executeUpdate() != 1) {
                    return Optional.empty();
                }
            }
            return Optional.of(existing.get().withStatus(status));
        }));
    }

    private long selectPlayerBalance(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing economy account for " + playerId);
                }
                return result.getLong(1);
            }
        }
    }

    private void updatePlayerBalance(Connection connection, UUID playerId, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoon_players SET balance_cents = ?, updated_at = ? WHERE player_uuid = ?
                """)) {
            statement.setLong(1, balance);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not debit plot upgrade from " + playerId);
            }
        }
    }

    private void insertUpgradeAudit(
            Connection connection,
            UUID playerId,
            PlotUpgradeType type,
            int level,
            long cost
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid, amount_cents, reason, created_at)
                VALUES (?, 'PLOT_UPGRADE', ?, NULL, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, cost);
            statement.setString(4, "plot:" + type.configKey() + ':' + level);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private Optional<Tycoon> selectByOwner(Connection connection, UUID ownerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tycoons WHERE owner_uuid = ?")) {
            statement.setString(1, ownerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        }
    }

    private Optional<Tycoon> selectById(Connection connection, UUID tycoonId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tycoons WHERE tycoon_id = ?")) {
            statement.setString(1, tycoonId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        }
    }

    private boolean isMember(Connection connection, UUID tycoonId, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM tycoon_members WHERE tycoon_id = ? AND player_uuid = ?
                """)) {
            statement.setString(1, tycoonId.toString());
            statement.setString(2, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private int countMembers(Connection connection, UUID tycoonId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM tycoon_members WHERE tycoon_id = ?")) {
            statement.setString(1, tycoonId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private Tycoon map(ResultSet row) throws SQLException {
        return new Tycoon(
                UUID.fromString(row.getString("tycoon_id")),
                UUID.fromString(row.getString("owner_uuid")),
                row.getString("group_id"),
                row.getString("world_name"),
                row.getInt("plot_index"),
                new TycoonPlotGroup.Bounds(
                        row.getInt("minimum_x"), row.getInt("maximum_x"),
                        row.getInt("minimum_z"), row.getInt("maximum_z")
                ),
                row.getInt("floor_y"),
                row.getInt("build_minimum_y"),
                row.getInt("build_maximum_y"),
                row.getInt("tycoon_level"),
                row.getInt("prestige_level"),
                row.getInt("plot_size_level"),
                row.getInt("hopper_limit_level"),
                row.getInt("member_limit_level"),
                row.getLong("progress_points"),
                row.getLong("total_production"),
                row.getLong("playtime_seconds"),
                TycoonStatus.valueOf(row.getString("status")),
                Instant.ofEpochMilli(row.getLong("created_at"))
        );
    }

    private <T> T inTransaction(Connection connection, TransactionWork<T> work) throws Exception {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.execute();
            connection.commit();
            return result;
        } catch (Exception exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T execute() throws Exception;
    }
}
