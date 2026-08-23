package fr.valoriatycoon.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ToolTierTest {
    @Test
    void preservesTierAcrossAllMultiToolForms() {
        ToolTier tier = ToolTier.fromMaterial(Material.DIAMOND_PICKAXE).orElseThrow();
        assertEquals(Material.DIAMOND_AXE, tier.material(ToolType.AXE));
        assertEquals(Material.DIAMOND_HOE, tier.material(ToolType.HOE));
        assertEquals(Material.FISHING_ROD, tier.material(ToolType.FISHING_ROD));
        assertTrue(ToolTier.fromMaterial(Material.FISHING_ROD).isEmpty());
    }
}
