package fr.valoriatycoon.gui;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for the configurable main Skyblock menu. */
public final class IslandMenuConfigLoader {
    private IslandMenuConfigLoader() {}

    public static IslandMenuSettings load(FileConfiguration config) {
        int size = config.getInt("menu.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("menu.size must be a multiple of 9 between 9 and 54");
        }
        String title = text(config, "menu.title");
        Material filler = material(config, "menu.filler");
        ConfigurationSection items = required(config, "menu.items");
        Map<Integer, IslandMenuSettings.Entry> entries = new LinkedHashMap<>();
        for (String id : items.getKeys(false)) {
            ConfigurationSection item = required(items, id);
            int slot = item.getInt("slot", -1);
            if (slot < 0 || slot >= size || entries.containsKey(slot)) {
                throw new IllegalArgumentException("Invalid or duplicate menu slot for " + id);
            }
            IslandMenuAction action;
            try {
                action = IslandMenuAction.valueOf(text(item, "action").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown menu action for " + id, exception);
            }
            entries.put(slot, new IslandMenuSettings.Entry(
                    slot,
                    material(item, "material"),
                    text(item, "name"),
                    item.getStringList("lore"),
                    action
            ));
        }
        return new IslandMenuSettings(size, title, filler, entries);
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(path + " cannot be blank");
        return value.trim();
    }
    private static Material material(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path).toUpperCase(Locale.ROOT));
        if (material == null) throw new IllegalArgumentException("Unknown material at " + path);
        return material;
    }
    private static ConfigurationSection required(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) throw new IllegalArgumentException("Missing section " + path);
        return value;
    }
}
