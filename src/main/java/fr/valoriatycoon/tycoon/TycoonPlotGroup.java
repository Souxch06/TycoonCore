package fr.valoriatycoon.tycoon;

import org.bukkit.Material;

/** Validated grid and generated-world settings for one Tycoon allocation group. */
public record TycoonPlotGroup(
        String id,
        String worldName,
        long seed,
        int plotSize,
        int spacing,
        int maximumPlots,
        int originX,
        int originZ,
        int floorY,
        int buildMinimumY,
        int buildMaximumY,
        Material floorMaterial,
        Material baseMaterial,
        int islandRadius,
        int baseDepth,
        double worldBorderSize,
        int maximumMembers
) {
    public int columns() {
        return (int) Math.ceil(Math.sqrt(maximumPlots));
    }

    public Bounds bounds(int plotIndex) {
        if (plotIndex < 0 || plotIndex >= maximumPlots) {
            throw new IllegalArgumentException("Plot index is outside group capacity: " + plotIndex);
        }
        int column = plotIndex % columns();
        int row = plotIndex / columns();
        int minimumX = Math.addExact(originX, Math.multiplyExact(column, plotSize + spacing));
        int minimumZ = Math.addExact(originZ, Math.multiplyExact(row, plotSize + spacing));
        return new Bounds(
                minimumX,
                Math.addExact(minimumX, plotSize - 1),
                minimumZ,
                Math.addExact(minimumZ, plotSize - 1)
        );
    }

    public record Bounds(int minimumX, int maximumX, int minimumZ, int maximumZ) {
        public int centerX() {
            return minimumX + (maximumX - minimumX) / 2;
        }

        public int centerZ() {
            return minimumZ + (maximumZ - minimumZ) / 2;
        }

        public boolean contains(int x, int z) {
            return x >= minimumX && x <= maximumX && z >= minimumZ && z <= maximumZ;
        }

        public boolean intersects(Bounds other) {
            return minimumX <= other.maximumX && maximumX >= other.minimumX
                    && minimumZ <= other.maximumZ && maximumZ >= other.minimumZ;
        }
    }
}
