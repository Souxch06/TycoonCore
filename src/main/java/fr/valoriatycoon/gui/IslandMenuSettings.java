package fr.valoriatycoon.gui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/** Immutable menus.yml snapshot for the current main menu. */
public record IslandMenuSettings(
        int size,
        String title,
        Material filler,
        Map<Integer, Entry> entries
) {
    public IslandMenuSettings {
        entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public record Entry(
            int slot,
            Material material,
            String name,
            List<String> lore,
            IslandMenuAction action
    ) {
        public Entry { lore = List.copyOf(lore); }
    }
}
