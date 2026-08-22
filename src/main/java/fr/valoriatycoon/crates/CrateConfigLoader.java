package fr.valoriatycoon.crates;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for generic physical crate keys and their multi-tool acquisition weights. */
public final class CrateConfigLoader {
    private CrateConfigLoader() {
    }

    public static CrateSettings load(FileConfiguration config) {
        Material keyMaterial = material(config, "key-material");
        Map<CrateType, CrateSettings.KeyPresentation> keys = new EnumMap<>(CrateType.class);
        ConfigurationSection keySection = required(config, "keys");
        for (CrateType type : CrateType.values()) {
            ConfigurationSection value = required(keySection, type.configKey());
            keys.put(type, new CrateSettings.KeyPresentation(
                    text(value, "name"),
                    value.getStringList("lore"),
                    value.getBoolean("paid", false)
            ));
        }
        if (keySection.getKeys(false).size() != CrateType.values().length) {
            throw new IllegalArgumentException("keys must contain exactly every generic crate type");
        }
        Map<CrateType, Integer> toolWeights = new EnumMap<>(CrateType.class);
        ConfigurationSection toolSection = required(config, "tool-rarity-weights");
        for (CrateType type : new CrateType[]{
                CrateType.COMMON, CrateType.RARE, CrateType.EPIC, CrateType.LEGENDARY
        }) {
            toolWeights.put(type, positiveInteger(toolSection, type.name()));
        }
        if (toolSection.getKeys(false).size() != 4) {
            throw new IllegalArgumentException("tool-rarity-weights must contain exactly four rarity crates");
        }
        // v0.36 shipped an explicit false before reward tables existed. Missing version means
        // that legacy file is upgraded in memory; versioned files keep the administrator choice.
        if (config.contains("reward-system-version")
                && config.getInt("reward-system-version", -1) != 1) {
            throw new IllegalArgumentException("Unsupported generic crate reward-system-version");
        }
        boolean migratedOpening = !config.contains("reward-system-version")
                || config.getBoolean("opening-enabled", true);
        return new CrateSettings(migratedOpening, keyMaterial, keys, toolWeights);
    }

    private static ConfigurationSection required(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing crate section: " + path);
        }
        return section;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing crate text: " + path);
        }
        return value.trim();
    }

    private static Material material(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path).toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("Invalid crate material: " + path);
        }
        return material;
    }

    private static int positiveInteger(ConfigurationSection section, String path) {
        int value = section.getInt(path, -1);
        if (value < 1 || value > 1_000_000) {
            throw new IllegalArgumentException("Crate weight must be positive at " + path);
        }
        return value;
    }
}
