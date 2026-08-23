package fr.valoriatycoon.farm;

/** Rank-gated bridge and server-side portal between two resource zones. */
public record FarmBridgeDefinition(
        int fromZone,
        int toZone,
        int minimumX,
        int maximumX,
        int centerZ,
        int width,
        int requiredRank
) {
    public FarmBridgeDefinition {
        if (fromZone < 1 || toZone <= fromZone || minimumX > maximumX || width < 3 || requiredRank < 0) {
            throw new IllegalArgumentException("Invalid farm bridge definition");
        }
    }

    public boolean contains(int x, int z) {
        return x >= minimumX && x <= maximumX && Math.abs(z - centerZ) <= width / 2;
    }
}
