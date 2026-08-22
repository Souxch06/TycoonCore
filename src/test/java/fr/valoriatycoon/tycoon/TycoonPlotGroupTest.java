package fr.valoriatycoon.tycoon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class TycoonPlotGroupTest {
    @Test
    void laysOutPlotsInDeterministicSpacedGrid() {
        TycoonPlotGroup group = new TycoonPlotGroup(
                "default", "world", 1L, 32, 5, 100,
                100, -100, 64, 64, 160,
                Material.GRASS_BLOCK, Material.DIRT, 10, 6, 10000, 4
        );
        assertEquals(new TycoonPlotGroup.Bounds(100, 131, -100, -69), group.bounds(0));
        assertEquals(new TycoonPlotGroup.Bounds(137, 168, -100, -69), group.bounds(1));
        assertEquals(new TycoonPlotGroup.Bounds(100, 131, -63, -32), group.bounds(10));
        assertTrue(group.bounds(0).contains(120, -80));
        assertFalse(group.bounds(0).contains(133, -80));
    }
}
