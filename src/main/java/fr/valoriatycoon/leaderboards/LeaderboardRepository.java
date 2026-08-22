package fr.valoriatycoon.leaderboards;

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

/** Performs bounded leaderboard reads exclusively through the asynchronous database worker. */
public final class LeaderboardRepository {
    private final SqliteDatabase database;

    public LeaderboardRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** Loads every leaderboard in one ordered worker operation for a coherent cache snapshot. */
    public CompletableFuture<Map<LeaderboardType, List<LeaderboardEntry>>> loadAll(int limit) {
        if (limit < 1 || limit > 1_000) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Leaderboard query limit must be between 1 and 1000")
            );
        }
        return database.submit(connection -> {
            EnumMap<LeaderboardType, List<LeaderboardEntry>> result = new EnumMap<>(LeaderboardType.class);
            for (LeaderboardType type : LeaderboardType.values()) {
                result.put(type, select(connection, type, limit));
            }
            return Map.copyOf(result);
        });
    }

    private List<LeaderboardEntry> select(
            Connection connection,
            LeaderboardType type,
            int limit
    ) throws SQLException {
        String sql = switch (type) {
            case MONEY -> """
                    SELECT p.player_uuid, p.last_known_name, p.balance_cents
                    FROM tycoon_players p
                    ORDER BY p.balance_cents DESC,
                             COALESCE(p.last_known_name, '') COLLATE NOCASE ASC,
                             p.player_uuid ASC
                    LIMIT ?
                    """;
            case ISLAND_LEVEL -> tycoonQuery("t.tycoon_level");
            case RANK -> tycoonQuery("t.prestige_level");
            case PRODUCTION -> tycoonQuery("t.total_production");
            case PLAYTIME -> tycoonQuery("t.playtime_seconds");
        };
        List<LeaderboardEntry> entries = new ArrayList<>(limit);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rows = statement.executeQuery()) {
                int position = 1;
                while (rows.next()) {
                    UUID playerId = UUID.fromString(rows.getString(1));
                    String name = rows.getString(2);
                    if (name == null || name.isBlank()) {
                        name = playerId.toString().substring(0, 8);
                    }
                    entries.add(new LeaderboardEntry(
                            position++,
                            playerId,
                            name,
                            Math.max(0L, rows.getLong(3))
                    ));
                }
            }
        }
        return List.copyOf(entries);
    }

    private String tycoonQuery(String valueColumn) {
        return """
                SELECT t.owner_uuid, p.last_known_name, %s
                FROM tycoons t
                LEFT JOIN tycoon_players p ON p.player_uuid = t.owner_uuid
                WHERE t.status = 'ACTIVE'
                ORDER BY %s DESC,
                         COALESCE(p.last_known_name, '') COLLATE NOCASE ASC,
                         t.owner_uuid ASC
                LIMIT ?
                """.formatted(valueColumn, valueColumn);
    }
}
