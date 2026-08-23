package fr.valoriatycoon.crates;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.tools.ToolType;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Atomic SQLite key consumption, no-reroll reward ledger and account-claim mutations. */
public final class CrateRewardRepository {
    private final SqliteDatabase database;

    public CrateRewardRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** Consumes one database-authenticated key and persists its already-resolved reward together. */
    public CompletableFuture<CrateOpenResult> open(
            UUID playerId,
            CrateKeyItemService.KeyToken key,
            CrateRewardSelection selection
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(selection, "selection");
        return database.submit(connection -> inTransaction(connection, () -> {
            StoredKey stored = selectKey(connection, key.keyId());
            if (stored == null) {
                return new CrateOpenResult(CrateOpenStatus.KEY_INVALID, null);
            }
            if (stored.type() != key.type()) {
                return new CrateOpenResult(CrateOpenStatus.KEY_TYPE_MISMATCH, null);
            }
            if (stored.consumed()) {
                return new CrateOpenResult(CrateOpenStatus.KEY_ALREADY_USED, null);
            }
            long now = System.currentTimeMillis();
            UUID rewardId = deterministicRewardId(key.keyId());
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE issued_crate_keys
                    SET consumed_by_uuid = ?, consumed_at = ?
                    WHERE key_id = ? AND consumed_by_uuid IS NULL
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setLong(2, now);
                statement.setString(3, key.keyId().toString());
                requireOne(statement);
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO issued_crate_rewards
                        (reward_id, key_id, crate_type, reward_definition, reward_kind,
                         payload, issued_to_uuid, delivered, issued_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)
                    """)) {
                statement.setString(1, rewardId.toString());
                statement.setString(2, key.keyId().toString());
                statement.setString(3, key.type().name());
                statement.setString(4, selection.definitionId());
                statement.setString(5, selection.kind().name());
                statement.setString(6, selection.payload().encode());
                statement.setString(7, playerId.toString());
                statement.setLong(8, now);
                requireOne(statement);
            }
            return new CrateOpenResult(
                    CrateOpenStatus.SUCCESS,
                    new CrateReward(
                            rewardId,
                            key.keyId(),
                            key.type(),
                            selection.definitionId(),
                            selection.kind(),
                            selection.payload(),
                            playerId,
                            false,
                            null,
                            false,
                            Instant.ofEpochMilli(now)
                    )
            );
        }));
    }

    public CompletableFuture<List<CrateReward>> pending(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return database.submit(connection -> pending(connection, playerId));
    }

    /** Returns consumed physical outcomes that still require main-thread delivery after a crash/quit. */
    public CompletableFuture<List<CrateReward>> pendingClaims(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return database.submit(connection -> {
            List<CrateReward> rewards = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM issued_crate_rewards
                    WHERE consumed_by_uuid = ? AND claim_delivered = 0
                    ORDER BY consumed_at ASC, reward_id ASC
                    LIMIT 512
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        rewards.add(map(result));
                    }
                }
            }
            return List.copyOf(rewards);
        });
    }

    public CompletableFuture<Void> markDelivered(UUID rewardId) {
        Objects.requireNonNull(rewardId, "rewardId");
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE issued_crate_rewards
                    SET delivered = 1, delivered_at = COALESCE(delivered_at, ?)
                    WHERE reward_id = ? AND delivered = 0 AND consumed_by_uuid IS NULL
                    """)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, rewardId.toString());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Void> markClaimDelivered(UUID rewardId) {
        Objects.requireNonNull(rewardId, "rewardId");
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE issued_crate_rewards
                    SET claim_delivered = 1, claim_delivered_at = COALESCE(claim_delivered_at, ?)
                    WHERE reward_id = ? AND consumed_by_uuid IS NOT NULL AND claim_delivered = 0
                    """)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, rewardId.toString());
                statement.executeUpdate();
            }
            return null;
        });
    }

    /** Atomically consumes a token; money, tool coins and generated keys commit in this transaction. */
    public CompletableFuture<CrateClaimResult> claim(
            UUID playerId,
            UUID rewardId,
            CrateRewardKind tokenKind
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(rewardId, "rewardId");
        Objects.requireNonNull(tokenKind, "tokenKind");
        return database.submit(connection -> inTransaction(connection, () -> {
            CrateReward reward = selectReward(connection, rewardId);
            if (reward == null) {
                return result(CrateClaimStatus.REWARD_INVALID, null, -1L, Map.of());
            }
            if (reward.kind() != tokenKind) {
                return result(CrateClaimStatus.REWARD_KIND_MISMATCH, reward, -1L, Map.of());
            }
            if (reward.consumed()) {
                return result(CrateClaimStatus.REWARD_ALREADY_USED, reward, -1L, Map.of());
            }

            long resultingMoney = -1L;
            Map<ToolType, Long> resultingCoins = Map.of();
            boolean claimDelivered = switch (reward.kind()) {
                case MONEY_BAG, COIN_BAG, UNIVERSAL_COIN_BAG, CRATE_KEYS -> true;
                case XP_VIAL, RESOURCE_BUNDLE, VANILLA_ITEM, PET_KEYS, GENERATORS -> false;
            };
            switch (reward.kind()) {
                case MONEY_BAG -> {
                    long amount = positive(reward.payload().requireLong("amount_cents"), "money amount");
                    long before = balance(connection, playerId);
                    if (amount > Long.MAX_VALUE - before) {
                        return result(CrateClaimStatus.BALANCE_OVERFLOW, reward, before, Map.of());
                    }
                    resultingMoney = before + amount;
                    updateBalance(connection, playerId, resultingMoney);
                    insertEconomyAudit(connection, rewardId, playerId, amount);
                }
                case COIN_BAG -> {
                    ToolType type = ToolType.valueOf(reward.payload().require("tool"));
                    long amount = positive(reward.payload().requireLong("amount"), "coin amount");
                    resultingCoins = creditCoins(connection, rewardId, playerId, Map.of(type, amount));
                    if (resultingCoins.isEmpty()) {
                        return result(CrateClaimStatus.BALANCE_OVERFLOW, reward, -1L, Map.of());
                    }
                }
                case UNIVERSAL_COIN_BAG -> {
                    long amount = positive(
                            reward.payload().requireLong("amount_each"),
                            "universal coin amount"
                    );
                    EnumMap<ToolType, Long> credits = new EnumMap<>(ToolType.class);
                    for (ToolType type : ToolType.values()) {
                        credits.put(type, amount);
                    }
                    resultingCoins = creditCoins(connection, rewardId, playerId, credits);
                    if (resultingCoins.isEmpty()) {
                        return result(CrateClaimStatus.BALANCE_OVERFLOW, reward, -1L, Map.of());
                    }
                }
                case CRATE_KEYS -> issueKeys(connection, reward, playerId);
                case XP_VIAL, RESOURCE_BUNDLE, VANILLA_ITEM, PET_KEYS, GENERATORS -> {
                    // Physical outcome is granted on the main thread after this at-most-once commit.
                }
            }
            consumeReward(connection, rewardId, playerId, claimDelivered);
            CrateReward consumed = new CrateReward(
                    reward.rewardId(), reward.keyId(), reward.crateType(), reward.definitionId(),
                    reward.kind(), reward.payload(), reward.issuedTo(), true, playerId,
                    claimDelivered, reward.issuedAt()
            );
            return result(CrateClaimStatus.SUCCESS, consumed, resultingMoney, resultingCoins);
        }));
    }

    private Map<ToolType, Long> creditCoins(
            Connection connection,
            UUID rewardId,
            UUID playerId,
            Map<ToolType, Long> credits
    ) throws SQLException {
        EnumMap<ToolType, Long> before = new EnumMap<>(ToolType.class);
        long now = System.currentTimeMillis();
        for (Map.Entry<ToolType, Long> entry : credits.entrySet()) {
            ensureToolRow(connection, playerId, entry.getKey(), now);
            long balance = toolCoins(connection, playerId, entry.getKey());
            if (entry.getValue() > Long.MAX_VALUE - balance) {
                return Map.of();
            }
            before.put(entry.getKey(), balance);
        }
        EnumMap<ToolType, Long> after = new EnumMap<>(ToolType.class);
        for (Map.Entry<ToolType, Long> entry : credits.entrySet()) {
            long resulting = before.get(entry.getKey()) + entry.getValue();
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE player_tools
                    SET special_coins = ?, updated_at = ?
                    WHERE player_uuid = ? AND tool_type = ?
                    """)) {
                statement.setLong(1, resulting);
                statement.setLong(2, now);
                statement.setString(3, playerId.toString());
                statement.setString(4, entry.getKey().storageKey());
                requireOne(statement);
            }
            insertCoinAudit(
                    connection,
                    rewardId,
                    playerId,
                    entry.getKey(),
                    entry.getValue(),
                    resulting
            );
            after.put(entry.getKey(), resulting);
        }
        return Map.copyOf(after);
    }

    private void issueKeys(Connection connection, CrateReward reward, UUID playerId)
            throws SQLException {
        CrateType type = CrateType.valueOf(reward.payload().require("crate_type"));
        int amount = reward.payload().requireInt("amount");
        if (amount < 1 || amount > 64) {
            throw new SQLException("Invalid committed crate key reward amount");
        }
        String source = type == CrateType.VALORIA ? "LEGENDARY_CRATE" : "CRATE_REWARD";
        long now = System.currentTimeMillis();
        for (int index = 0; index < amount; index++) {
            String reference = reward.rewardId() + ":" + index;
            UUID keyId = UUID.nameUUIDFromBytes(
                    ("valoriatycoon:crate-key:" + source + ':' + reference)
                            .getBytes(StandardCharsets.UTF_8)
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT OR IGNORE INTO issued_crate_keys
                        (key_id, crate_type, issued_to_uuid, source, source_reference,
                         delivered, issued_at)
                    VALUES (?, ?, ?, ?, ?, 0, ?)
                    """)) {
                statement.setString(1, keyId.toString());
                statement.setString(2, type.name());
                statement.setString(3, playerId.toString());
                statement.setString(4, source);
                statement.setString(5, reference);
                statement.setLong(6, now);
                statement.executeUpdate();
            }
        }
    }

    private void ensureToolRow(Connection connection, UUID playerId, ToolType type, long now)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO player_tools
                    (player_uuid, tool_type, tool_level, tool_experience, special_coins, updated_at)
                VALUES (?, ?, 1, 0, 0, ?)
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.storageKey());
            statement.setLong(3, now);
            statement.executeUpdate();
        }
    }

    private long toolCoins(Connection connection, UUID playerId, ToolType type) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT special_coins FROM player_tools
                WHERE player_uuid = ? AND tool_type = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.storageKey());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing tool coin row after creation");
                }
                return result.getLong(1);
            }
        }
    }

    private long balance(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing crate reward account");
                }
                return result.getLong(1);
            }
        }
    }

    private void updateBalance(Connection connection, UUID playerId, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoon_players SET balance_cents = ?, updated_at = ? WHERE player_uuid = ?
                """)) {
            statement.setLong(1, balance);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            requireOne(statement);
        }
    }

    private void insertEconomyAudit(
            Connection connection,
            UUID rewardId,
            UUID playerId,
            long amount
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid,
                     amount_cents, reason, created_at)
                VALUES (?, 'CRATE_REWARD', NULL, ?, ?, ?, ?)
                """)) {
            statement.setString(1, deterministicAuditId(rewardId, "money").toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, amount);
            statement.setString(4, "crate:money-bag:" + rewardId);
            statement.setLong(5, System.currentTimeMillis());
            requireOne(statement);
        }
    }

    private void insertCoinAudit(
            Connection connection,
            UUID rewardId,
            UUID playerId,
            ToolType type,
            long amount,
            long balanceAfter
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tool_coin_transactions
                    (transaction_id, player_uuid, tool_type, amount_delta,
                     balance_after, transaction_type, reason, created_at)
                VALUES (?, ?, ?, ?, ?, 'CRATE_REWARD', ?, ?)
                """)) {
            statement.setString(1, deterministicAuditId(rewardId, type.storageKey()).toString());
            statement.setString(2, playerId.toString());
            statement.setString(3, type.storageKey());
            statement.setLong(4, amount);
            statement.setLong(5, balanceAfter);
            statement.setString(6, "crate:coin-bag:" + rewardId);
            statement.setLong(7, System.currentTimeMillis());
            requireOne(statement);
        }
    }

    private void consumeReward(
            Connection connection,
            UUID rewardId,
            UUID playerId,
            boolean claimDelivered
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE issued_crate_rewards
                SET delivered = 1,
                    delivered_at = COALESCE(delivered_at, ?),
                    consumed_by_uuid = ?, consumed_at = ?,
                    claim_delivered = ?,
                    claim_delivered_at = CASE WHEN ? = 1 THEN ? ELSE claim_delivered_at END
                WHERE reward_id = ? AND consumed_by_uuid IS NULL
                """)) {
            long now = System.currentTimeMillis();
            statement.setLong(1, now);
            statement.setString(2, playerId.toString());
            statement.setLong(3, now);
            statement.setInt(4, claimDelivered ? 1 : 0);
            statement.setInt(5, claimDelivered ? 1 : 0);
            statement.setLong(6, now);
            statement.setString(7, rewardId.toString());
            requireOne(statement);
        }
    }

    private List<CrateReward> pending(Connection connection, UUID playerId) throws SQLException {
        List<CrateReward> rewards = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM issued_crate_rewards
                WHERE issued_to_uuid = ? AND delivered = 0 AND consumed_by_uuid IS NULL
                ORDER BY issued_at ASC, reward_id ASC
                LIMIT 512
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rewards.add(map(result));
                }
            }
        }
        return List.copyOf(rewards);
    }

    private StoredKey selectKey(Connection connection, UUID keyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT crate_type, consumed_by_uuid FROM issued_crate_keys WHERE key_id = ?
                """)) {
            statement.setString(1, keyId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? new StoredKey(
                                CrateType.valueOf(result.getString(1)),
                                result.getString(2) != null
                        )
                        : null;
            }
        }
    }

    private CrateReward selectReward(Connection connection, UUID rewardId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM issued_crate_rewards WHERE reward_id = ?")) {
            statement.setString(1, rewardId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    private CrateReward map(ResultSet result) throws SQLException {
        String consumed = result.getString("consumed_by_uuid");
        return new CrateReward(
                UUID.fromString(result.getString("reward_id")),
                UUID.fromString(result.getString("key_id")),
                CrateType.valueOf(result.getString("crate_type")),
                result.getString("reward_definition"),
                CrateRewardKind.valueOf(result.getString("reward_kind")),
                CrateRewardPayload.decode(result.getString("payload")),
                UUID.fromString(result.getString("issued_to_uuid")),
                result.getInt("delivered") == 1,
                consumed == null ? null : UUID.fromString(consumed),
                result.getInt("claim_delivered") == 1,
                Instant.ofEpochMilli(result.getLong("issued_at"))
        );
    }

    private CrateClaimResult result(
            CrateClaimStatus status,
            CrateReward reward,
            long money,
            Map<ToolType, Long> coins
    ) {
        return new CrateClaimResult(status, reward, money, coins);
    }

    private UUID deterministicRewardId(UUID keyId) {
        return UUID.nameUUIDFromBytes(
                ("valoriatycoon:crate-reward:" + keyId).getBytes(StandardCharsets.UTF_8)
        );
    }

    private UUID deterministicAuditId(UUID rewardId, String suffix) {
        return UUID.nameUUIDFromBytes(
                ("valoriatycoon:crate-audit:" + rewardId + ':' + suffix)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private long positive(long value, String name) {
        if (value < 1L) {
            throw new IllegalArgumentException("Invalid committed " + name);
        }
        return value;
    }

    private void requireOne(PreparedStatement statement) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Expected exactly one crate reward row mutation");
        }
    }

    private <T> T inTransaction(Connection connection, Work<T> work) throws Exception {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T value = work.execute();
            connection.commit();
            return value;
        } catch (Exception exception) {
            try {
                connection.rollback();
            } catch (SQLException rollback) {
                exception.addSuppressed(rollback);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private record StoredKey(CrateType type, boolean consumed) {
    }

    @FunctionalInterface
    private interface Work<T> {
        T execute() throws Exception;
    }
}
