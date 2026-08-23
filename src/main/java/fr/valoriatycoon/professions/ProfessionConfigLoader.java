package fr.valoriatycoon.professions;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for permanent profession progression. */
public final class ProfessionConfigLoader {
    private ProfessionConfigLoader() {
    }

    /** Loads and validates all profession definitions and their XP curve. */
    public static ProfessionSettings load(FileConfiguration config) {
        ProfessionSettings.Progression progression = new ProfessionSettings.Progression(
                integer(config, "progression.max-level", 100, 1, 10_000),
                positiveLong(config, "progression.base-experience"),
                positiveDecimal(config, "progression.experience-multiplier"),
                integer(config, "progression.flush-interval-ticks", 20, 1, 1_200)
        );
        ConfigurationSection section = required(config, "professions");
        Map<ProfessionType, ProfessionDefinition> definitions = new EnumMap<>(ProfessionType.class);
        for (ProfessionType type : ProfessionType.values()) {
            ConfigurationSection value = required(section, type.storageKey());
            definitions.put(type, new ProfessionDefinition(
                    type,
                    text(value, "display-name"),
                    positiveLong(value, "experience-per-action")
            ));
        }
        if (section.getKeys(false).size() != ProfessionType.values().length) {
            throw new IllegalArgumentException("professions contains an unsupported profession");
        }
        return new ProfessionSettings(progression, definitions);
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

    private static long positiveLong(ConfigurationSection section, String path) {
        long value = section.getLong(path, -1L);
        if (value < 1L) {
            throw new IllegalArgumentException(path + " must be positive");
        }
        return value;
    }

    private static BigDecimal positiveDecimal(ConfigurationSection section, String path) {
        String raw = section.getString(path);
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.compareTo(BigDecimal.ONE) <= 0 || value.scale() > 4) {
                throw new IllegalArgumentException("Invalid growth multiplier");
            }
            return value.stripTrailingZeros();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(path + " must be greater than 1", exception);
        }
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        return value.trim();
    }

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return value;
    }
}
