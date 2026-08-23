package fr.valoriatycoon.warps;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Immutable warps.yml snapshot including a normalized command alias index. */
public record WarpSettings(
        int cooldownSeconds,
        Menu menu,
        Map<String, WarpDefinition> warps,
        Map<String, String> aliases
) {
    public WarpSettings {
        menu = Objects.requireNonNull(menu, "menu");
        warps = Collections.unmodifiableMap(new LinkedHashMap<>(warps));
        aliases = Collections.unmodifiableMap(new LinkedHashMap<>(aliases));
        if (cooldownSeconds < 0 || cooldownSeconds > 300 || warps.isEmpty()) {
            throw new IllegalArgumentException("Invalid warp settings");
        }
    }

    public WarpDefinition resolve(String input) {
        if (input == null) {
            return null;
        }
        String id = aliases.get(input.toLowerCase(java.util.Locale.ROOT));
        return id == null ? null : warps.get(id);
    }

    public record Menu(int size, String title, Material filler) {
        public Menu {
            title = Objects.requireNonNull(title, "title");
            filler = Objects.requireNonNull(filler, "filler");
            if (size < 9 || size > 54 || size % 9 != 0 || title.isBlank() || !filler.isItem()) {
                throw new IllegalArgumentException("Invalid warp menu settings");
            }
        }
    }
}
