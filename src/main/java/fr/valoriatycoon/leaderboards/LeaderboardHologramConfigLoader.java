package fr.valoriatycoon.leaderboards;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict leaderboard-holograms.yml parser. */
public final class LeaderboardHologramConfigLoader {
    private LeaderboardHologramConfigLoader() {
    }

    public static LeaderboardHologramSettings load(FileConfiguration config) {
        Map<LeaderboardType, LeaderboardHologramSettings.Position> positions = new EnumMap<>(
                LeaderboardType.class
        );
        ConfigurationSection categories = required(config, "categories");
        for (LeaderboardType type : LeaderboardType.values()) {
            ConfigurationSection position = required(categories, type.configKey());
            positions.put(type, new LeaderboardHologramSettings.Position(
                    finite(position, "offset-x"),
                    finite(position, "offset-y"),
                    finite(position, "offset-z")
            ));
        }
        return new LeaderboardHologramSettings(
                config.getBoolean("enabled", true),
                text(config, "world"),
                integer(config, "update-interval-ticks", 100, 20, 72_000),
                integer(config, "top-entries", 5, 1, 10),
                (float) finite(config, "view-range"),
                integer(config, "line-width", 260, 40, 1_024),
                config.getBoolean("shadowed", true),
                config.getBoolean("default-background", true),
                positions
        );
    }

    private static ConfigurationSection required(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing leaderboard hologram section: " + path);
        }
        return section;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing leaderboard hologram text: " + path);
        }
        return value.trim();
    }

    private static double finite(ConfigurationSection section, String path) {
        double value = section.getDouble(path, Double.NaN);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Invalid leaderboard hologram decimal: " + path);
        }
        return value;
    }

    private static int integer(
            ConfigurationSection section,
            String path,
            int fallback,
            int minimum,
            int maximum
    ) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }
}
