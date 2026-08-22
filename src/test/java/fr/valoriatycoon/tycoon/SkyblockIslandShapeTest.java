package fr.valoriatycoon.tycoon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SkyblockIslandShapeTest {
    @Test
    void createsCircularTopAndTaperedFloatingBase() {
        assertEquals(Material.GRASS_BLOCK, material(0, 80, 0));
        assertEquals(Material.GRASS_BLOCK, material(10, 80, 0));
        assertEquals(Material.AIR, material(11, 80, 0));
        assertEquals(Material.DIRT, material(4, 74, 0));
        assertEquals(Material.AIR, material(5, 74, 0));
        assertEquals(Material.AIR, material(0, 73, 0));
        assertEquals(Material.AIR, material(0, 81, 0));
    }

    @Test
    void translatedPlayerIslandsUseTheExactSameTemplate() {
        for (int x = -12; x <= 12; x++) {
            for (int y = 73; y <= 81; y++) {
                for (int z = -12; z <= 12; z++) {
                    Material first = SkyblockIslandShape.materialAt(
                            0, 0, 80, 10, 6,
                            Material.GRASS_BLOCK, Material.DIRT,
                            x, y, z
                    );
                    Material translated = SkyblockIslandShape.materialAt(
                            200, -150, 80, 10, 6,
                            Material.GRASS_BLOCK, Material.DIRT,
                            x + 200, y, z - 150
                    );
                    assertEquals(first, translated);
                }
            }
        }
    }

    private Material material(int x, int y, int z) {
        return SkyblockIslandShape.materialAt(
                0, 0, 80, 10, 6,
                Material.GRASS_BLOCK, Material.DIRT,
                x, y, z
        );
    }
}
