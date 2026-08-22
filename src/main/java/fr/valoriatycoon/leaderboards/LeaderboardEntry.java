package fr.valoriatycoon.leaderboards;

import java.util.Objects;
import java.util.UUID;

/** One immutable ranked player value produced away from the server thread. */
public record LeaderboardEntry(int position, UUID playerId, String playerName, long value) {
    public LeaderboardEntry {
        playerId = Objects.requireNonNull(playerId, "playerId");
        playerName = Objects.requireNonNull(playerName, "playerName");
        if (position < 1 || playerName.isBlank() || value < 0L) {
            throw new IllegalArgumentException("Invalid leaderboard entry");
        }
    }
}
