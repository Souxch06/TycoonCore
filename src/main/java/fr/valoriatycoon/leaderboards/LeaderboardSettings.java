package fr.valoriatycoon.leaderboards;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Immutable leaderboards.yml configuration snapshot. */
public record LeaderboardSettings(
        boolean enabled,
        int refreshIntervalTicks,
        int queryLimit,
        int displayLimit,
        Menu menu
) {
    public LeaderboardSettings {
        menu = Objects.requireNonNull(menu, "menu");
        if (refreshIntervalTicks < 20 || queryLimit < 1 || displayLimit < 1
                || displayLimit > 10 || queryLimit < displayLimit) {
            throw new IllegalArgumentException("Invalid leaderboard cache limits or refresh interval");
        }
    }

    public record Menu(
            int size,
            String title,
            String detailTitle,
            Material filler,
            int backSlot,
            Map<LeaderboardType, Integer> categorySlots
    ) {
        public Menu {
            filler = Objects.requireNonNull(filler, "filler");
            title = Objects.requireNonNull(title, "title");
            detailTitle = Objects.requireNonNull(detailTitle, "detailTitle");
            categorySlots = Objects.requireNonNull(categorySlots, "categorySlots");
            categorySlots = Collections.unmodifiableMap(new EnumMap<>(categorySlots));
            if (size != 54
                    || title.isBlank() || detailTitle.isBlank()
                    || !filler.isItem() || backSlot < 0 || backSlot >= size
                    || categorySlots.size() != LeaderboardType.values().length) {
                throw new IllegalArgumentException("Invalid leaderboard menu");
            }
        }

        public int slot(LeaderboardType type) {
            Integer slot = categorySlots.get(type);
            if (slot == null) {
                throw new IllegalArgumentException("Missing leaderboard menu slot for " + type);
            }
            return slot;
        }
    }
}
