package fr.valoriatycoon.compaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CompactionMaterialRulesTest {

    @Test
    void mapsOnlyMineralsToTheirVanillaStorageBlocks() {
        Map<Material, Material> minerals = Map.of(
                Material.COAL, Material.COAL_BLOCK,
                Material.COPPER_INGOT, Material.COPPER_BLOCK,
                Material.IRON_INGOT, Material.IRON_BLOCK,
                Material.GOLD_INGOT, Material.GOLD_BLOCK,
                Material.REDSTONE, Material.REDSTONE_BLOCK,
                Material.LAPIS_LAZULI, Material.LAPIS_BLOCK,
                Material.DIAMOND, Material.DIAMOND_BLOCK,
                Material.EMERALD, Material.EMERALD_BLOCK
        );
        minerals.forEach((resource, block) -> {
            assertTrue(CompactionMaterialRules.isMineral(resource));
            assertEquals(block, CompactionMaterialRules.craftingMaterial(resource));
            assertEquals(9, CompactionMaterialRules.baseUnitsPerCraftingItem(resource));
        });

        for (Material unchanged : new Material[]{
                Material.WHEAT,
                Material.CARROT,
                Material.POTATO,
                Material.BEETROOT,
                Material.OAK_LOG,
                Material.BIRCH_LOG,
                Material.SPRUCE_LOG,
                Material.DARK_OAK_LOG
        }) {
            assertFalse(CompactionMaterialRules.isMineral(unchanged));
            assertEquals(unchanged, CompactionMaterialRules.craftingMaterial(unchanged));
            assertEquals(1, CompactionMaterialRules.baseUnitsPerCraftingItem(unchanged));
        }
    }
}
