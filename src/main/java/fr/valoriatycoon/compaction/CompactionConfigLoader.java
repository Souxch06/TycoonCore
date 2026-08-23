package fr.valoriatycoon.compaction;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for compact resources, item presentation and the decompaction NPC. */
public final class CompactionConfigLoader {
    private static final EnumSet<Material> FISHING_RESOURCES = EnumSet.of(
            Material.COD,
            Material.SALMON,
            Material.PUFFERFISH,
            Material.TROPICAL_FISH
    );

    private CompactionConfigLoader() {
    }

    public static CompactionSettings load(FileConfiguration config) {
        int maximumLevel = integer(config, "maximum-level", 3, 1, 3);
        ConfigurationSection levels = required(config, "levels");
        Map<Integer, String> levelNames = new LinkedHashMap<>();
        for (int level = 1; level <= maximumLevel; level++) {
            levelNames.put(level, text(levels, level + ".name"));
        }

        ConfigurationSection configuredResources = required(config, "resources");
        Map<Material, CompactionSettings.ResourceDefinition> resources = new LinkedHashMap<>();
        for (String key : configuredResources.getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
            if (material == null
                    || material.isAir()
                    || !material.isItem()
                    || material.getMaxStackSize() < 9
                    || FISHING_RESOURCES.contains(material)) {
                throw new IllegalArgumentException("Invalid compactable material: " + key);
            }
            Material craftingMaterial = CompactionMaterialRules.craftingMaterial(material);
            resources.put(material, new CompactionSettings.ResourceDefinition(
                    material,
                    craftingMaterial,
                    CompactionMaterialRules.baseUnitsPerCraftingItem(material),
                    text(configuredResources, key)
            ));
        }

        ConfigurationSection npc = required(config, "decompactor-npc");
        CompactionSettings.NpcSettings npcSettings = new CompactionSettings.NpcSettings(
                npc.getBoolean("enabled", true),
                npc.getString("world", ""),
                finiteDouble(npc, "offset-x", 2.5),
                finiteDouble(npc, "offset-y", 0.0),
                finiteDouble(npc, "offset-z", 0.5),
                (float) finiteDouble(npc, "yaw", 180.0),
                text(npc, "name")
        );
        List<String> lore = config.getStringList("item-lore");
        if (lore.isEmpty()) {
            throw new IllegalArgumentException("item-lore must contain at least one line");
        }
        return new CompactionSettings(maximumLevel, levelNames, lore, resources, npcSettings);
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

    private static double finiteDouble(
            ConfigurationSection section,
            String path,
            double fallback
    ) {
        double value = section.getDouble(path, fallback);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(path + " must be finite");
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

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return value;
    }
}
