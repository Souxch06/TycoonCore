package fr.valoriatycoon.leaderboards;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict leaderboards.yml parser. */
public final class LeaderboardConfigLoader {
    private LeaderboardConfigLoader() {
    }

    public static LeaderboardSettings load(FileConfiguration config) {
        int size = integer(config, "menu.size", 54, 27, 54);
        if (size % 9 != 0) {
            throw new IllegalArgumentException("leaderboards menu size must be a multiple of 9");
        }
        Map<LeaderboardType, Integer> slots = new EnumMap<>(LeaderboardType.class);
        ConfigurationSection categorySection = required(config, "menu.category-slots");
        Set<Integer> unique = new HashSet<>();
        for (LeaderboardType type : LeaderboardType.values()) {
            int slot = integer(categorySection, type.configKey(), -1, 0, size - 1);
            if (!unique.add(slot)) {
                throw new IllegalArgumentException("Duplicate leaderboard category slot: " + slot);
            }
            slots.put(type, slot);
        }
        int backSlot = integer(config, "menu.back-slot", 49, 0, size - 1);
        Set<Integer> detailReserved = Set.of(10, 11, 12, 13, 14, 15, 16, 21, 22, 23, 45, 53);
        if (unique.contains(backSlot) || unique.contains(53) || detailReserved.contains(backSlot)) {
            throw new IllegalArgumentException("Leaderboard slots conflict with reserved navigation/details");
        }
        LeaderboardSettings.Menu menu = new LeaderboardSettings.Menu(
                size,
                text(config, "menu.title"),
                text(config, "menu.detail-title"),
                material(config, "menu.filler"),
                backSlot,
                slots
        );
        return new LeaderboardSettings(
                config.getBoolean("enabled", true),
                integer(config, "refresh-interval-ticks", 1_200, 20, 72_000),
                integer(config, "query-limit", 100, 1, 1_000),
                integer(config, "display-limit", 10, 1, 10),
                menu
        );
    }

    private static ConfigurationSection required(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing leaderboard section: " + path);
        }
        return section;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing leaderboard text: " + path);
        }
        return value.trim();
    }

    private static Material material(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path).toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("Invalid leaderboard material: " + path);
        }
        return material;
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
