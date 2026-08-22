package fr.valoriatycoon.tycoon;

import org.bukkit.Material;

/** Deterministic floating-island shape used by incremental plot preparation. */
public final class SkyblockIslandShape {
    private SkyblockIslandShape() {
    }

    public static Material materialAt(
            int centerX,
            int centerZ,
            int floorY,
            int islandRadius,
            int depth,
            Material floorMaterial,
            Material baseMaterial,
            int x,
            int y,
            int z
    ) {
        int layerDepth = floorY - y;
        if (layerDepth < 0 || layerDepth > depth) {
            return Material.AIR;
        }
        int radiusAtDepth = Math.max(1, islandRadius - layerDepth);
        long dx = x - centerX;
        long dz = z - centerZ;
        if (dx * dx + dz * dz > (long) radiusAtDepth * radiusAtDepth) {
            return Material.AIR;
        }
        return layerDepth == 0 ? floorMaterial : baseMaterial;
    }
}
