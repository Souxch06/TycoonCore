package fr.valoriatycoon.quests;

import fr.valoriatycoon.database.SqliteDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Repeatable quest progress and rarity-completion repository. */
public final class QuestRepository {
    private final SqliteDatabase database;
    private final QuestSettings settings;

    public QuestRepository(SqliteDatabase database, QuestSettings settings) {
        this.database = database;
        this.settings = settings;
    }

    public CompletableFuture<QuestProfile> loadOrCreate(UUID playerId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRows(connection, playerId);
            return load(connection, playerId);
        }));
    }

    public CompletableFuture<QuestProgressUpdate> addProgress(
            UUID playerId,
            QuestDefinition quest,
            long amount
    ) {
        return database.submit(connection -> inTransaction(connection, () -> {
            ensureRow(connection, playerId, quest.id());
            QuestProgress current = selectProgress(connection, playerId, quest.id());
            long total = saturatingAdd(current.progress(), amount);
            long completed = total / quest.targetActions();
            long remainder = total % quest.targetActions();
            long completions = saturatingAdd(current.completions(), completed);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE quest_progress
                    SET progress = ?, completions = ?, updated_at = ?
                    WHERE player_uuid = ? AND quest_id = ?
                    """)) {
                statement.setLong(1, remainder);
                statement.setLong(2, completions);
                statement.setLong(3, System.currentTimeMillis());
                statement.setString(4, playerId.toString());
                statement.setString(5, quest.id());
                statement.executeUpdate();
            }
            long money = -1L;
            if (completed > 0) {
                long reward = Math.multiplyExact(quest.rewardMoneyCents(), completed);
                money = creditMoney(connection, playerId, reward, quest.id());
            }
            return new QuestProgressUpdate(
                    new QuestProgress(quest.id(), remainder, completions), completed, money
            );
        }));
    }

    private QuestProfile load(Connection connection, UUID playerId) throws SQLException {
        Map<String, QuestProgress> progress = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT quest_id, progress, completions FROM quest_progress WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    progress.put(rows.getString(1), new QuestProgress(
                            rows.getString(1), rows.getLong(2), rows.getLong(3)
                    ));
                }
            }
        }
        Map<QuestRarity, Long> completed = new EnumMap<>(QuestRarity.class);
        for (QuestDefinition quest : settings.quests().values()) {
            completed.merge(quest.rarity(), progress.get(quest.id()).completions(), Long::sum);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT rarity, consumed FROM quest_consumed WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    QuestRarity rarity = QuestRarity.valueOf(rows.getString(1));
                    completed.merge(rarity, -rows.getLong(2), Long::sum);
                }
            }
        }
        completed.replaceAll((rarity, value) -> Math.max(0L, value));
        return new QuestProfile(progress, completed);
    }

    private void ensureRows(Connection connection, UUID playerId) throws SQLException {
        for (String questId : settings.quests().keySet()) ensureRow(connection, playerId, questId);
    }

    private void ensureRow(Connection connection, UUID playerId, String questId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO quest_progress
                    (player_uuid, quest_id, progress, completions, updated_at)
                VALUES (?, ?, 0, 0, ?)
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, questId);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private QuestProgress selectProgress(Connection connection, UUID playerId, String questId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT progress, completions FROM quest_progress WHERE player_uuid = ? AND quest_id = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, questId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("Missing quest progress");
                return new QuestProgress(questId, row.getLong(1), row.getLong(2));
            }
        }
    }

    private long creditMoney(Connection connection, UUID playerId, long amount, String questId) throws SQLException {
        long before;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("Missing quest player account");
                before = row.getLong(1);
            }
        }
        if (amount > Long.MAX_VALUE - before) throw new SQLException("Quest reward overflow");
        long after = before + amount;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tycoon_players SET balance_cents = ?, updated_at = ? WHERE player_uuid = ?")) {
            statement.setLong(1, after);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, playerId.toString());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid, amount_cents, reason, created_at)
                VALUES (?, 'QUEST_REWARD', NULL, ?, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, amount);
            statement.setString(4, "quest:" + questId);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
        return after;
    }

    private <T> T inTransaction(Connection connection, Work<T> work) throws Exception {
        boolean auto = connection.getAutoCommit(); connection.setAutoCommit(false);
        try { T result=work.run(); connection.commit(); return result; }
        catch(Exception e){ try{connection.rollback();}catch(SQLException r){e.addSuppressed(r);} throw e; }
        finally{connection.setAutoCommit(auto);}
    }
    private static long saturatingAdd(long a,long b){return b>0&&a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
    @FunctionalInterface private interface Work<T>{T run() throws Exception;}
}
