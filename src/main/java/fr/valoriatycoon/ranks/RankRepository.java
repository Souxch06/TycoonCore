package fr.valoriatycoon.ranks;

import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.professions.ProfessionType;
import fr.valoriatycoon.quests.QuestDefinition;
import fr.valoriatycoon.quests.QuestRarity;
import fr.valoriatycoon.quests.QuestSettings;
import fr.valoriatycoon.tools.ToolType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persists medieval-rank promotions atomically.
 *
 * <p>A promotion verifies authoritative money, playtime, quest, tool and
 * profession data. On success it deducts only the configured rank price, consumes the required
 * quest validations and resets tool levels and experience. The player's
 * remaining balance and cumulative playtime are preserved.</p>
 */
public final class RankRepository {
    private final SqliteDatabase database;
    private final QuestSettings quests;

    public RankRepository(SqliteDatabase database, QuestSettings quests) {
        this.database = Objects.requireNonNull(database, "database");
        this.quests = Objects.requireNonNull(quests, "quests");
    }

    /**
     * Attempts one promotion using a single database transaction.
     *
     * @param playerId promoted island owner
     * @param expectedRank rank observed before scheduling the asynchronous work
     * @param requirement requirements and price of the next rank
     * @return future containing the promotion status and authoritative remaining balance
     */
    public CompletableFuture<RankPromotionResult> promote(
            UUID playerId,
            int expectedRank,
            RankRequirement requirement
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(requirement, "requirement");
        return database.submit(connection -> inTransaction(
                connection,
                () -> promoteInTransaction(connection, playerId, expectedRank, requirement)
        ));
    }

    private RankPromotionResult promoteInTransaction(
            Connection connection,
            UUID playerId,
            int expectedRank,
            RankRequirement requirement
    ) throws SQLException {
        RankState rankState = selectRankState(connection, playerId);
        if (rankState == null) {
            return result(RankPromotionStatus.NO_ACTIVE_ISLAND, 0, -1);
        }
        int currentRank = rankState.level();
        if (currentRank != expectedRank) {
            return result(RankPromotionStatus.STALE_RANK, currentRank, -1);
        }
        if (currentRank >= requirement.level()) {
            return result(RankPromotionStatus.MAXIMUM_RANK, currentRank, -1);
        }

        long balance = selectBalance(connection, playerId);
        long price = requirement.requiredMoneyCents();
        if (balance < price) {
            return result(RankPromotionStatus.INSUFFICIENT_MONEY, currentRank, balance);
        }
        if (rankState.playtimeSeconds() < requirement.requiredPlaytimeSeconds()) {
            return result(RankPromotionStatus.INSUFFICIENT_PLAYTIME, currentRank, balance);
        }
        if (!toolsSatisfied(connection, playerId, requirement.toolLevels())) {
            return result(RankPromotionStatus.INSUFFICIENT_TOOL_LEVELS, currentRank, balance);
        }
        if (!professionsSatisfied(connection, playerId, requirement.professionLevels())) {
            return result(RankPromotionStatus.INSUFFICIENT_PROFESSION_LEVELS, currentRank, balance);
        }

        Map<QuestRarity, Long> available = availableQuests(connection, playerId);
        for (Map.Entry<QuestRarity, Integer> entry : requirement.quests().entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                return result(RankPromotionStatus.INSUFFICIENT_QUESTS, currentRank, balance);
            }
        }

        long now = System.currentTimeMillis();
        long remainingBalance = balance - price;
        for (Map.Entry<QuestRarity, Integer> entry : requirement.quests().entrySet()) {
            consumeQuests(connection, playerId, entry.getKey(), entry.getValue(), now);
        }
        updateRank(connection, playerId, requirement.level(), now);
        updateBalance(connection, playerId, remainingBalance, now);
        insertPromotionAudit(connection, playerId, requirement, price, now);
        resetToolProgression(connection, playerId, now);

        return result(RankPromotionStatus.SUCCESS, requirement.level(), remainingBalance);
    }

    private RankState selectRankState(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT prestige_level, playtime_seconds
                FROM tycoons
                WHERE owner_uuid = ? AND status = 'ACTIVE'
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new RankState(result.getInt(1), result.getLong(2));
            }
        }
    }

    private long selectBalance(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT balance_cents
                FROM tycoon_players
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing account for rank promotion: " + playerId);
                }
                return result.getLong(1);
            }
        }
    }

    private boolean toolsSatisfied(
            Connection connection,
            UUID playerId,
            Map<ToolType, Integer> required
    ) throws SQLException {
        for (Map.Entry<ToolType, Integer> entry : required.entrySet()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT tool_level
                    FROM player_tools
                    WHERE player_uuid = ? AND tool_type = ?
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, entry.getKey().storageKey());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || result.getInt(1) < entry.getValue()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean professionsSatisfied(
            Connection connection,
            UUID playerId,
            Map<ProfessionType, Integer> required
    ) throws SQLException {
        for (Map.Entry<ProfessionType, Integer> entry : required.entrySet()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT profession_level
                    FROM player_professions
                    WHERE player_uuid = ? AND profession_type = ?
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, entry.getKey().storageKey());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || result.getInt(1) < entry.getValue()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Map<QuestRarity, Long> availableQuests(
            Connection connection,
            UUID playerId
    ) throws SQLException {
        Map<QuestRarity, Long> available = new EnumMap<>(QuestRarity.class);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT quest_id, completions
                FROM quest_progress
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    QuestDefinition quest = quests.quests().get(result.getString(1));
                    if (quest != null) {
                        available.merge(quest.rarity(), result.getLong(2), Long::sum);
                    }
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT rarity, consumed
                FROM quest_consumed
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    QuestRarity rarity = QuestRarity.valueOf(result.getString(1));
                    available.merge(rarity, -result.getLong(2), Long::sum);
                }
            }
        }
        return available;
    }

    private void consumeQuests(
            Connection connection,
            UUID playerId,
            QuestRarity rarity,
            long amount,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO quest_consumed(player_uuid, rarity, consumed, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_uuid, rarity) DO UPDATE SET
                    consumed = consumed + excluded.consumed,
                    updated_at = excluded.updated_at
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, rarity.name());
            statement.setLong(3, amount);
            statement.setLong(4, now);
            statement.executeUpdate();
        }
    }

    private void updateRank(
            Connection connection,
            UUID playerId,
            int rank,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoons
                SET prestige_level = ?, updated_at = ?
                WHERE owner_uuid = ? AND status = 'ACTIVE'
                """)) {
            statement.setInt(1, rank);
            statement.setLong(2, now);
            statement.setString(3, playerId.toString());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Missing active island during rank promotion: " + playerId);
            }
        }
    }

    private void updateBalance(
            Connection connection,
            UUID playerId,
            long balance,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoon_players
                SET balance_cents = ?, updated_at = ?
                WHERE player_uuid = ?
                """)) {
            statement.setLong(1, balance);
            statement.setLong(2, now);
            statement.setString(3, playerId.toString());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Missing account during rank promotion: " + playerId);
            }
        }
    }

    private void insertPromotionAudit(
            Connection connection,
            UUID playerId,
            RankRequirement requirement,
            long chargedCents,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid,
                     amount_cents, reason, created_at)
                VALUES (?, 'RANK_PROMOTION', ?, NULL, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, chargedCents);
            statement.setString(4, "rank:" + requirement.name());
            statement.setLong(5, now);
            statement.executeUpdate();
        }
    }

    private void resetToolProgression(
            Connection connection,
            UUID playerId,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE player_tools
                SET tool_level = 1, tool_experience = 0, updated_at = ?
                WHERE player_uuid = ?
                """)) {
            statement.setLong(1, now);
            statement.setString(2, playerId.toString());
            statement.executeUpdate();
        }
    }

    private RankPromotionResult result(
            RankPromotionStatus status,
            int rank,
            long balance
    ) {
        return new RankPromotionResult(status, rank, balance);
    }

    private <T> T inTransaction(Connection connection, TransactionWork<T> work) throws Exception {
        boolean previousAutoCommit = connection.getAutoCommit();
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
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private record RankState(int level, long playtimeSeconds) {
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T execute() throws Exception;
    }
}
