package fr.valoriatycoon.leaderboards;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete immutable cache atomically published after one asynchronous database read. */
public record LeaderboardSnapshot(
        Instant generatedAt,
        Map<LeaderboardType, List<LeaderboardEntry>> entries
) {
    public LeaderboardSnapshot {
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        entries = Objects.requireNonNull(entries, "entries");
        EnumMap<LeaderboardType, List<LeaderboardEntry>> copy = new EnumMap<>(LeaderboardType.class);
        for (LeaderboardType type : LeaderboardType.values()) {
            copy.put(type, List.copyOf(entries.getOrDefault(type, List.of())));
        }
        entries = Collections.unmodifiableMap(copy);
    }

    public static LeaderboardSnapshot empty() {
        return new LeaderboardSnapshot(Instant.EPOCH, Map.of());
    }

    public List<LeaderboardEntry> entries(LeaderboardType type) {
        return entries.getOrDefault(type, List.of());
    }

    public boolean initialized() {
        return !generatedAt.equals(Instant.EPOCH);
    }
}
