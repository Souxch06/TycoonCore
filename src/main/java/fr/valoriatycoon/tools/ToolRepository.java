package fr.valoriatycoon.tools;

import fr.valoriatycoon.database.SqliteDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** SQLite repository for per-tool progression and atomic capability purchases. */
public final class ToolRepository {
    private final SqliteDatabase database;
    private final ToolSettings settings;

    public ToolRepository(SqliteDatabase database, ToolSettings settings) {
        this.database = Objects.requireNonNull(database, "database");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public CompletableFuture<List<ToolProfile>> loadOrCreate(UUID playerId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRows(connection, playerId);
            return selectProfiles(connection, playerId);
        }));
    }

    public CompletableFuture<ToolCoinSpendResult> spendCoins(
            UUID playerId,
            ToolType toolType,
            long amount,
            String reason
    ) {
        if (amount < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Coin charge cannot be negative"));
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            ToolProfile current = selectProfile(connection, playerId, toolType);
            if (current.specialCoins() < amount) {
                return new ToolCoinSpendResult(
                        ToolCoinSpendStatus.INSUFFICIENT_COINS,
                        toolType,
                        amount,
                        current.specialCoins()
                );
            }
            long resulting = current.specialCoins() - amount;
            updateToolCoins(connection, playerId, toolType, resulting);
            insertCoinAudit(connection, playerId, toolType, -amount, resulting, "PURCHASE", reason);
            return new ToolCoinSpendResult(ToolCoinSpendStatus.SUCCESS, toolType, amount, resulting);
        }));
    }

    public CompletableFuture<ToolUpgradeResult> purchaseCapability(
            UUID playerId,
            ToolType toolType,
            ToolCapability capability,
            int expectedCurrentLevel,
            ToolUpgradeCurrency currency
    ) {
        ToolCapabilityDefinition definition = settings.capability(capability);
        return database.submit(connection -> inTransaction(connection, () -> {
            ToolProfile current = selectProfile(connection, playerId, toolType);
            int currentLevel = current.capabilityLevel(capability);
            long balance = selectBalance(connection, playerId);
            if (currentLevel != expectedCurrentLevel) {
                return result(
                        ToolUpgradeStatus.PROFILE_STALE, toolType, capability, currency,
                        currentLevel, 0L, balance, current.specialCoins()
                );
            }
            if (currentLevel >= definition.maximumLevel()) {
                return result(
                        ToolUpgradeStatus.MAXIMUM_LEVEL, toolType, capability, currency,
                        currentLevel, 0L, balance, current.specialCoins()
                );
            }
            int nextLevel = currentLevel + 1;
            ToolCapabilityDefinition.Level next = definition.level(nextLevel).orElseThrow();
            long charge = currency == ToolUpgradeCurrency.BASE_MONEY
                    ? next.moneyCostCents()
                    : next.toolCoinCost();
            if (currency == ToolUpgradeCurrency.BASE_MONEY && balance < charge) {
                return result(
                        ToolUpgradeStatus.INSUFFICIENT_FUNDS, toolType, capability, currency,
                        currentLevel, charge, balance, current.specialCoins()
                );
            }
            if (currency == ToolUpgradeCurrency.TOOL_COINS && current.specialCoins() < charge) {
                return result(
                        ToolUpgradeStatus.INSUFFICIENT_TOOL_COINS, toolType, capability, currency,
                        currentLevel, charge, balance, current.specialCoins()
                );
            }
            long resultingBalance = currency == ToolUpgradeCurrency.BASE_MONEY ? balance - charge : balance;
            long resultingCoins = currency == ToolUpgradeCurrency.TOOL_COINS
                    ? current.specialCoins() - charge
                    : current.specialCoins();
            updateCapability(connection, playerId, toolType, capability, nextLevel);
            if (currency == ToolUpgradeCurrency.BASE_MONEY) {
                updateBalance(connection, playerId, resultingBalance);
                insertAudit(connection, playerId, toolType, capability, nextLevel, charge);
            } else {
                updateToolCoins(connection, playerId, toolType, resultingCoins);
                insertCoinAudit(
                        connection, playerId, toolType, -charge, resultingCoins,
                        "UPGRADE", "capability:" + capability.storageKey() + ':' + nextLevel
                );
            }
            return result(
                    ToolUpgradeStatus.SUCCESS, toolType, capability, currency,
                    nextLevel, charge, resultingBalance, resultingCoins
            );
        }));
    }

    public CompletableFuture<ToolProfile> addRewards(
            UUID playerId,
            ToolType toolType,
            long experience,
            long coins
    ) {
        if (experience < 0 || coins < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Tool rewards cannot be negative"));
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            ToolProfile current = selectProfile(connection, playerId, toolType);
            ToolExperienceCalculator.Progress progress = ToolExperienceCalculator.add(
                    current.toolLevel(),
                    current.toolExperience(),
                    experience,
                    settings.progression()
            );
            long resultingCoins = saturatingAdd(current.specialCoins(), coins);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE player_tools
                    SET tool_level = ?, tool_experience = ?, special_coins = ?, updated_at = ?
                    WHERE player_uuid = ? AND tool_type = ?
                    """)) {
                statement.setInt(1, progress.level());
                statement.setLong(2, progress.experience());
                statement.setLong(3, resultingCoins);
                statement.setLong(4, System.currentTimeMillis());
                statement.setString(5, playerId.toString());
                statement.setString(6, toolType.storageKey());
                requireSingleUpdate(statement, playerId);
            }
            long creditedCoins = resultingCoins - current.specialCoins();
            if (creditedCoins > 0) {
                insertCoinAudit(
                        connection, playerId, toolType, creditedCoins, resultingCoins,
                        "EARN", "tool:action-batch"
                );
            }
            return current.withProgress(progress.level(), progress.experience(), resultingCoins);
        }));
    }

    private void ensureRows(Connection connection, UUID playerId) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement tool = connection.prepareStatement("""
                INSERT OR IGNORE INTO player_tools
                    (player_uuid, tool_type, tool_level, tool_experience, updated_at)
                VALUES (?, ?, 1, 0, ?)
                """); PreparedStatement capability = connection.prepareStatement("""
                INSERT OR IGNORE INTO tool_capabilities
                    (player_uuid, tool_type, capability_id, capability_level, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (ToolType type : ToolType.values()) {
                tool.setString(1, playerId.toString());
                tool.setString(2, type.storageKey());
                tool.setLong(3, now);
                tool.addBatch();
                for (ToolCapability value : ToolCapability.values()) {
                    if (!settings.capability(value).appliesTo(type)) {
                        continue;
                    }
                    capability.setString(1, playerId.toString());
                    capability.setString(2, type.storageKey());
                    capability.setString(3, value.storageKey());
                    capability.setInt(4, settings.capability(value).initialLevel());
                    capability.setLong(5, now);
                    capability.addBatch();
                }
            }
            tool.executeBatch();
            capability.executeBatch();
        }
    }

    private List<ToolProfile> selectProfiles(Connection connection, UUID playerId) throws SQLException {
        List<ToolProfile> profiles = new ArrayList<>(ToolType.values().length);
        for (ToolType type : ToolType.values()) {
            profiles.add(selectProfile(connection, playerId, type));
        }
        return profiles;
    }

    private ToolProfile selectProfile(Connection connection, UUID playerId, ToolType type) throws SQLException {
        int level;
        long experience;
        long specialCoins;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT tool_level, tool_experience, special_coins
                FROM player_tools
                WHERE player_uuid = ? AND tool_type = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.storageKey());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing tool row for " + playerId + '/' + type);
                }
                level = result.getInt(1);
                experience = result.getLong(2);
                specialCoins = result.getLong(3);
            }
        }
        Map<ToolCapability, Integer> capabilities = new EnumMap<>(ToolCapability.class);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT capability_id, capability_level
                FROM tool_capabilities
                WHERE player_uuid = ? AND tool_type = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.storageKey());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    try {
                        ToolCapability capability = ToolCapability.valueOf(
                                result.getString(1).toUpperCase(java.util.Locale.ROOT)
                        );
                        capabilities.put(capability, result.getInt(2));
                    } catch (IllegalArgumentException ignored) {
                        // A removed capability may remain in an older database; it is safely ignored.
                    }
                }
            }
        }
        return new ToolProfile(type, level, experience, specialCoins, capabilities);
    }

    private long selectBalance(Connection connection, UUID playerId) throws SQLException {
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

    private void updateCapability(
            Connection connection,
            UUID playerId,
            ToolType type,
            ToolCapability capability,
            int level
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tool_capabilities
                SET capability_level = ?, updated_at = ?
                WHERE player_uuid = ? AND tool_type = ? AND capability_id = ?
                """)) {
            statement.setInt(1, level);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            statement.setString(4, type.storageKey());
            statement.setString(5, capability.storageKey());
            requireSingleUpdate(statement, playerId);
        }
    }

    private void updateToolCoins(
            Connection connection,
            UUID playerId,
            ToolType type,
            long coins
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE player_tools SET special_coins = ?, updated_at = ?
                WHERE player_uuid = ? AND tool_type = ?
                """)) {
            statement.setLong(1, coins);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            statement.setString(4, type.storageKey());
            requireSingleUpdate(statement, playerId);
        }
    }

    private void updateBalance(Connection connection, UUID playerId, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoon_players SET balance_cents = ?, updated_at = ? WHERE player_uuid = ?
                """)) {
            statement.setLong(1, balance);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            requireSingleUpdate(statement, playerId);
        }
    }

    private void insertAudit(
            Connection connection,
            UUID playerId,
            ToolType type,
            ToolCapability capability,
            int level,
            long cost
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid, amount_cents, reason, created_at)
                VALUES (?, 'TOOL_CAPABILITY_UPGRADE', ?, NULL, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, cost);
            statement.setString(4, "tool:" + type.storageKey() + ':' + capability.storageKey() + ':' + level);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void insertCoinAudit(
            Connection connection,
            UUID playerId,
            ToolType type,
            long delta,
            long balanceAfter,
            String transactionType,
            String reason
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tool_coin_transactions
                    (transaction_id, player_uuid, tool_type, amount_delta,
                     balance_after, transaction_type, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setString(3, type.storageKey());
            statement.setLong(4, delta);
            statement.setLong(5, balanceAfter);
            statement.setString(6, transactionType);
            statement.setString(7, reason);
            statement.setLong(8, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private ToolUpgradeResult result(
            ToolUpgradeStatus status,
            ToolType type,
            ToolCapability capability,
            ToolUpgradeCurrency currency,
            int level,
            long chargedAmount,
            long balance,
            long toolCoins
    ) {
        return new ToolUpgradeResult(
                status, type, capability, currency,
                level, chargedAmount, balance, toolCoins
        );
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private void requireSingleUpdate(PreparedStatement statement, UUID playerId) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Expected one updated row for " + playerId);
        }
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
