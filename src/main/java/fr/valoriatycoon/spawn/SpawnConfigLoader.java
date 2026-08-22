package fr.valoriatycoon.spawn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for the generated medieval server spawn. */
public final class SpawnConfigLoader {
    private SpawnConfigLoader() {
    }

    public static SpawnSettings load(FileConfiguration config) {
        ConfigurationSection portalsSection = required(config, "portals");
        List<SpawnSettings.PortalDefinition> portals = new ArrayList<>();
        Set<String> farmIds = new HashSet<>();
        for (String key : portalsSection.getKeys(false)) {
            ConfigurationSection section = required(portalsSection, key);
            String farmId = text(section, "farm-id").toLowerCase(Locale.ROOT);
            if (!farmIds.add(farmId)) {
                throw new IllegalArgumentException("Duplicate spawn farm portal: " + farmId);
            }
            portals.add(new SpawnSettings.PortalDefinition(
                    farmId,
                    text(section, "display-name"),
                    integer(section, "center-x", -512, 512),
                    integer(section, "center-z", -512, 512),
                    enumValue(SpawnSettings.Axis.class, text(section, "axis")),
                    blockMaterial(section, "frame-material"),
                    blockMaterial(section, "accent-material")
            ));
        }
        return new SpawnSettings(
                text(config, "world"),
                config.getLong("seed", 1L),
                integer(config, "ground-y", 32, 280),
                integer(config, "island-radius", 64, 512),
                finiteDouble(config, "border-size", 128.0, 4096.0),
                integer(config, "protection-radius", 64, 512),
                config.getBoolean("teleport-on-first-join", true),
                integer(config, "spawn.x", -512, 512),
                integer(config, "spawn.z", -512, 512),
                (float) finiteDouble(config, "spawn.yaw", -360.0, 360.0),
                portals
        );
    }

    private static int integer(
            ConfigurationSection section,
            String path,
            int minimum,
            int maximum
    ) {
        int value = section.getInt(path, Integer.MIN_VALUE);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static double finiteDouble(
            ConfigurationSection section,
            String path,
            double minimum,
            double maximum
    ) {
        double value = section.getDouble(path, Double.NaN);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        return value.trim();
    }

    private static Material blockMaterial(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path));
        if (material == null || !material.isBlock()) {
            throw new IllegalArgumentException(path + " must be a block material");
        }
        return material;
    }

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return value;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + ": " + raw, exception);
        }
    }
}
