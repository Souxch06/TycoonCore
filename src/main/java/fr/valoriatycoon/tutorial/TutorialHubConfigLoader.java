package fr.valoriatycoon.tutorial;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for the informational tutorial-hub.yml gallery. */
public final class TutorialHubConfigLoader {
    private static final Pattern PANEL_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private TutorialHubConfigLoader() {
    }

    public static TutorialHubSettings load(FileConfiguration config) {
        Map<String, TutorialHubSettings.Panel> panels = new LinkedHashMap<>();
        ConfigurationSection configured = required(config, "panels");
        for (String id : configured.getKeys(false)) {
            if (!PANEL_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid tutorial hub panel id: " + id);
            }
            ConfigurationSection panel = required(configured, id);
            List<String> lines = panel.getStringList("lines");
            panels.put(id, new TutorialHubSettings.Panel(
                    finite(panel, "offset-x"),
                    finite(panel, "offset-y"),
                    finite(panel, "offset-z"),
                    lines
            ));
        }
        return new TutorialHubSettings(
                config.getBoolean("enabled", true),
                text(config, "world"),
                integer(config, "refresh-interval-ticks", 100, 20, 72_000),
                (float) finite(config, "view-range"),
                integer(config, "line-width", 280, 80, 1_024),
                config.getBoolean("shadowed", true),
                config.getBoolean("default-background", true),
                panels
        );
    }

    private static ConfigurationSection required(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing tutorial hub section: " + path);
        }
        return section;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing tutorial hub text: " + path);
        }
        return value.trim();
    }

    private static double finite(ConfigurationSection section, String path) {
        double value = section.getDouble(path, Double.NaN);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Invalid tutorial hub number: " + path);
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
