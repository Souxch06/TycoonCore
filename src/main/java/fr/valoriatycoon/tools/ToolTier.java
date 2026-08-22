package fr.valoriatycoon.tools;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;

/** Material tier retained while the multi-tool changes form. */
public enum ToolTier {
    WOODEN,
    STONE,
    GOLDEN,
    IRON,
    DIAMOND,
    NETHERITE;

    public static Optional<ToolTier> fromMaterial(Material material) {
        String name = material.name();
        for (ToolTier tier : values()) {
            if (name.startsWith(tier.name() + '_')) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }

    public Material material(ToolType type) {
        if (type == ToolType.FISHING_ROD) {
            return Material.FISHING_ROD;
        }
        String suffix = switch (type) {
            case PICKAXE -> "PICKAXE";
            case AXE -> "AXE";
            case HOE -> "HOE";
            case FISHING_ROD -> throw new IllegalStateException("Handled above");
        };
        return Material.valueOf(name().toUpperCase(Locale.ROOT) + '_' + suffix);
    }
}
