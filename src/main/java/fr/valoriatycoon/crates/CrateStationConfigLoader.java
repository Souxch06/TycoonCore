package fr.valoriatycoon.crates;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for crate-stations.yml. */
public final class CrateStationConfigLoader {
    private CrateStationConfigLoader() {
    }

    public static CrateStationSettings load(FileConfiguration config) {
        Map<CrateStationType, CrateStationSettings.Station> stations = new EnumMap<>(
                CrateStationType.class
        );
        ConfigurationSection configured = required(config, "stations");
        for (CrateStationType type : CrateStationType.values()) {
            ConfigurationSection section = required(configured, type.configKey());
            ConfigurationSection effect = section.getConfigurationSection("effect");
            stations.put(type, new CrateStationSettings.Station(
                    finite(section, "offset-x"),
                    finite(section, "offset-y"),
                    finite(section, "offset-z"),
                    (float) finite(section, "yaw"),
                    text(section, "item-model"),
                    effect == null ? defaultEffect(type) : new CrateStationSettings.Effect(
                            rgb(effect, "primary-color"),
                            rgb(effect, "secondary-color"),
                            integer(effect, "particle-count", 3, 1, 12),
                            (float) finite(effect, "particle-size"),
                            finite(effect, "orbit-radius"),
                            finite(effect, "orbit-speed"),
                            finite(effect, "bob-height"),
                            finite(effect, "yaw-sway-degrees")
                    )
            ));
        }
        if (configured.getKeys(false).size() != CrateStationType.values().length) {
            throw new IllegalArgumentException("stations must contain exactly all physical crate types");
        }
        ConfigurationSection effects = config.getConfigurationSection("effects");
        return new CrateStationSettings(
                config.getBoolean("enabled", true),
                text(config, "world"),
                integer(config, "refresh-interval-ticks", 100, 20, 72_000),
                (float) finite(config, "interaction-width"),
                (float) finite(config, "interaction-height"),
                finite(config, "label-offset-y"),
                effects == null || effects.getBoolean("enabled", true),
                effects == null ? 4 : integer(effects, "interval-ticks", 4, 1, 40),
                effects == null ? 40.0 : finite(effects, "view-distance"),
                stations
        );
    }

    private static CrateStationSettings.Effect defaultEffect(CrateStationType type) {
        return switch (type) {
            case VOTE -> new CrateStationSettings.Effect(
                    0x45E7EF, 0xF5FFFF, 4, 0.75F, 0.78, 0.035, 0.07, 2.5
            );
            case QUEST -> new CrateStationSettings.Effect(
                    0x55DDF0, 0xF8DA84, 4, 0.75F, 0.74, 0.030, 0.06, 2.0
            );
            case FARM -> new CrateStationSettings.Effect(
                    0x48BE52, 0xF2C94C, 3, 0.80F, 0.72, 0.025, 0.05, 1.5
            );
            case COMMON -> new CrateStationSettings.Effect(
                    0x48DD69, 0xCAE0D0, 2, 0.70F, 0.62, 0.022, 0.04, 1.0
            );
            case RARE -> new CrateStationSettings.Effect(
                    0x3D8BFF, 0x6FE3FF, 4, 0.85F, 0.78, 0.035, 0.07, 2.5
            );
            case EPIC -> new CrateStationSettings.Effect(
                    0xFF7818, 0xFFC439, 5, 0.90F, 0.84, 0.042, 0.08, 3.0
            );
            case LEGENDARY -> new CrateStationSettings.Effect(
                    0xFFD32D, 0xFFF8B2, 6, 1.00F, 0.92, 0.048, 0.10, 3.5
            );
            case VALORIA -> new CrateStationSettings.Effect(
                    0xFF2E4B, 0xFFBA27, 8, 1.10F, 1.05, 0.055, 0.12, 4.5
            );
            case PETS -> new CrateStationSettings.Effect(
                    0xF669B3, 0x53E7EE, 5, 0.90F, 0.82, 0.040, 0.08, 3.0
            );
        };
    }

    private static ConfigurationSection required(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing physical crate station section: " + path);
        }
        return section;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing physical crate station text: " + path);
        }
        return value.trim();
    }

    private static int rgb(ConfigurationSection section, String path) {
        String value = text(section, path);
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (!normalized.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(path + " must be a six-digit hexadecimal color");
        }
        return Integer.parseInt(normalized, 16);
    }

    private static double finite(ConfigurationSection section, String path) {
        double value = section.getDouble(path, Double.NaN);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Invalid physical crate station number: " + path);
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
