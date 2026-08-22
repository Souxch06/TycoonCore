package fr.valoriatycoon.warps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict extensible parser for menu and direct /warp destinations. */
public final class WarpConfigLoader {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private WarpConfigLoader() {
    }

    public static WarpSettings load(FileConfiguration config) {
        int menuSize = integer(config, "menu.size", 27, 9, 54);
        if (menuSize % 9 != 0) {
            throw new IllegalArgumentException("warp menu size must be a multiple of 9");
        }
        WarpSettings.Menu menu = new WarpSettings.Menu(
                menuSize,
                text(config, "menu.title"),
                material(config, "menu.filler")
        );
        Map<String, WarpDefinition> warps = new LinkedHashMap<>();
        Map<String, String> aliases = new LinkedHashMap<>();
        Set<Integer> slots = new HashSet<>();
        ConfigurationSection definitions = required(config, "warps");
        for (String id : definitions.getKeys(false)) {
            if (!ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid warp id: " + id);
            }
            ConfigurationSection section = required(definitions, id);
            int slot = integer(section, "slot", -1, 0, menuSize - 1);
            if (!slots.add(slot)) {
                throw new IllegalArgumentException("Duplicate warp menu slot: " + slot);
            }
            List<String> configuredAliases = new ArrayList<>();
            for (String alias : section.getStringList("aliases")) {
                String normalized = alias.toLowerCase(Locale.ROOT);
                if (!ID.matcher(normalized).matches()) {
                    throw new IllegalArgumentException("Invalid alias for warp " + id + ": " + alias);
                }
                configuredAliases.add(normalized);
            }
            WarpDefinition warp = new WarpDefinition(
                    id,
                    configuredAliases,
                    text(section, "world"),
                    section.getBoolean("relative-to-spawn", false),
                    finite(section, "x"),
                    finite(section, "y"),
                    finite(section, "z"),
                    (float) finite(section, "yaw"),
                    (float) finite(section, "pitch"),
                    slot,
                    material(section, "icon"),
                    text(section, "item-model"),
                    text(section, "name"),
                    section.getStringList("lore")
            );
            warps.put(id, warp);
            registerAlias(aliases, id, id);
            for (String alias : configuredAliases) {
                registerAlias(aliases, alias, id);
            }
        }
        addMissingTutorialWarp(menuSize, warps, aliases, slots);
        return new WarpSettings(
                integer(config, "teleport-cooldown-seconds", 3, 0, 300),
                menu,
                warps,
                aliases
        );
    }

    private static void addMissingTutorialWarp(
            int menuSize,
            Map<String, WarpDefinition> warps,
            Map<String, String> aliases,
            Set<Integer> slots
    ) {
        List<String> tutorialAliases = List.of("tuto", "tutoriel", "guide", "aide");
        if (warps.containsKey("tutorial")
                || aliases.containsKey("tutorial")
                || tutorialAliases.stream().anyMatch(aliases::containsKey)) {
            return;
        }
        int slot = 11 < menuSize && !slots.contains(11) ? 11 : -1;
        for (int candidate = 0; slot < 0 && candidate < menuSize; candidate++) {
            if (!slots.contains(candidate)) {
                slot = candidate;
            }
        }
        if (slot < 0) {
            return;
        }
        String worldName = warps.values().stream()
                .filter(WarpDefinition::relativeToSpawn)
                .map(WarpDefinition::worldName)
                .findFirst()
                .orElse("valoria_spawn");
        WarpDefinition tutorial = new WarpDefinition(
                "tutorial",
                tutorialAliases,
                worldName,
                true,
                0.0,
                0.0,
                -111.0,
                180.0F,
                0.0F,
                slot,
                Material.WRITABLE_BOOK,
                "ui/warp/tutorial",
                "<gold><bold>Académie — Tutoriel</bold></gold>",
                List.of(
                        "<gray>Guide complet des systèmes importants de Valoria.</gray>",
                        "<yellow>Cliquez ou utilisez /warp tuto.</yellow>"
                )
        );
        warps.put(tutorial.id(), tutorial);
        slots.add(slot);
        registerAlias(aliases, tutorial.id(), tutorial.id());
        tutorialAliases.forEach(alias -> registerAlias(aliases, alias, tutorial.id()));
    }

    private static void registerAlias(Map<String, String> aliases, String alias, String id) {
        if (aliases.putIfAbsent(alias, id) != null) {
            throw new IllegalArgumentException("Duplicate warp id/alias: " + alias);
        }
    }

    private static ConfigurationSection required(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing warp section: " + path);
        }
        return section;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing warp text: " + path);
        }
        return value.trim();
    }

    private static Material material(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path).toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("Invalid warp material: " + path);
        }
        return material;
    }

    private static double finite(ConfigurationSection section, String path) {
        double value = section.getDouble(path, Double.NaN);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Invalid warp number: " + path);
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
