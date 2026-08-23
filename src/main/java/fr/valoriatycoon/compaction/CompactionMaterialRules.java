package fr.valoriatycoon.compaction;

import java.util.Map;
import org.bukkit.Material;

/** Canonical vanilla storage blocks used before custom mineral compaction starts. */
final class CompactionMaterialRules {
    private static final Map<Material, Material> MINERAL_BLOCKS = Map.of(
            Material.COAL, Material.COAL_BLOCK,
            Material.COPPER_INGOT, Material.COPPER_BLOCK,
            Material.IRON_INGOT, Material.IRON_BLOCK,
            Material.GOLD_INGOT, Material.GOLD_BLOCK,
            Material.REDSTONE, Material.REDSTONE_BLOCK,
            Material.LAPIS_LAZULI, Material.LAPIS_BLOCK,
            Material.DIAMOND, Material.DIAMOND_BLOCK,
            Material.EMERALD, Material.EMERALD_BLOCK
    );

    private CompactionMaterialRules() {
    }

    static Material craftingMaterial(Material baseMaterial) {
        return MINERAL_BLOCKS.getOrDefault(baseMaterial, baseMaterial);
    }

    static int baseUnitsPerCraftingItem(Material baseMaterial) {
        return MINERAL_BLOCKS.containsKey(baseMaterial) ? 9 : 1;
    }

    static boolean isMineral(Material baseMaterial) {
        return MINERAL_BLOCKS.containsKey(baseMaterial);
    }
}
