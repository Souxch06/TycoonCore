package fr.valoriatycoon.leaderboards;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable spawn TextDisplay settings for cached leaderboard holograms. */
public record LeaderboardHologramSettings(
        boolean enabled,
        String worldName,
        int updateIntervalTicks,
        int topEntries,
        float viewRange,
        int lineWidth,
        boolean shadowed,
        boolean defaultBackground,
        Map<LeaderboardType, Position> positions
) {
    public LeaderboardHologramSettings {
        worldName = Objects.requireNonNull(worldName, "worldName").trim();
        positions = Objects.requireNonNull(positions, "positions");
        positions = Collections.unmodifiableMap(new EnumMap<>(positions));
        if (worldName.isBlank() || updateIntervalTicks < 20
                || topEntries < 1 || topEntries > 10
                || !Float.isFinite(viewRange) || viewRange <= 0.0F || viewRange > 4.0F
                || lineWidth < 40 || lineWidth > 1_024
                || positions.size() != LeaderboardType.values().length) {
            throw new IllegalArgumentException("Invalid leaderboard hologram settings");
        }
    }

    public Position position(LeaderboardType type) {
        Position position = positions.get(type);
        if (position == null) {
            throw new IllegalArgumentException("Missing hologram position for " + type);
        }
        return position;
    }

    /** Offset relative to Valoria's authoritative generated spawn location. */
    public record Position(double offsetX, double offsetY, double offsetZ) {
        public Position {
            if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY) || !Double.isFinite(offsetZ)
                    || Math.abs(offsetX) > 64.0 || Math.abs(offsetY) > 32.0 || Math.abs(offsetZ) > 64.0) {
                throw new IllegalArgumentException("Leaderboard hologram offsets must be finite and spawn-local");
            }
        }
    }
}
