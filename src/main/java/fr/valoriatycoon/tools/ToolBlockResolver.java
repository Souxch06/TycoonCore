package fr.valoriatycoon.tools;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Tag;

/** Resolves a block to one of the three block-breaking multi-tool forms. */
public final class ToolBlockResolver {
    private static final Set<Material> FARM_CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.SUGAR_CANE
    );

    private ToolBlockResolver() {
    }

    public static Optional<ToolType> resolve(Material blockMaterial) {
        if (FARM_CROPS.contains(blockMaterial)) {
            return Optional.of(ToolType.HOE);
        }
        if (Tag.MINEABLE_PICKAXE.isTagged(blockMaterial)) {
            return Optional.of(ToolType.PICKAXE);
        }
        if (Tag.MINEABLE_AXE.isTagged(blockMaterial)) {
            return Optional.of(ToolType.AXE);
        }
        if (Tag.MINEABLE_HOE.isTagged(blockMaterial)) {
            return Optional.of(ToolType.HOE);
        }
        return Optional.empty();
    }
}
