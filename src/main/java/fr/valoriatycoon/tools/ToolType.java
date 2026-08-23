package fr.valoriatycoon.tools;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;

/** Farm tool categories with per-player progression. */
public enum ToolType {
    PICKAXE,
    HOE,
    AXE,
    FISHING_ROD;

    public static Optional<ToolType> fromMaterial(Material material) {
        String name = material.name();
        if (name.endsWith("_PICKAXE")) {
            return Optional.of(PICKAXE);
        }
        if (name.endsWith("_HOE")) {
            return Optional.of(HOE);
        }
        if (name.endsWith("_AXE") && !name.endsWith("_PICKAXE")) {
            return Optional.of(AXE);
        }
        if (material == Material.FISHING_ROD) {
            return Optional.of(FISHING_ROD);
        }
        return Optional.empty();
    }

    public String storageKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
