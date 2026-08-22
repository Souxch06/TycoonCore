package fr.valoriatycoon.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ToolBlockResolverTest {
    @Test
    void resolvesOnlyTheThreeBlockBreakingForms() {
        assertEquals(ToolType.PICKAXE, ToolBlockResolver.resolve(Material.STONE).orElseThrow());
        assertEquals(ToolType.AXE, ToolBlockResolver.resolve(Material.OAK_LOG).orElseThrow());
        assertEquals(ToolType.HOE, ToolBlockResolver.resolve(Material.WHEAT).orElseThrow());
        assertTrue(ToolBlockResolver.resolve(Material.DIRT).isEmpty(), "No shovel form must be introduced");
    }
}
