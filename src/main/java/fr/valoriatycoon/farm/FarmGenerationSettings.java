package fr.valoriatycoon.farm;

import java.util.List;
import org.bukkit.Material;

/** Type-safe settings consumed by the deterministic public-farm chunk generator. */
public sealed interface FarmGenerationSettings permits
        FarmGenerationSettings.Mine,
        FarmGenerationSettings.Fields,
        FarmGenerationSettings.Fishing,
        FarmGenerationSettings.Forest {

    int spawnY();

    default List<FarmZoneDefinition> zones() {
        return List.of();
    }

    /** Width of the traversable deck connecting consecutive generated zones. */
    default int bridgeWidth() {
        return 9;
    }

    /** Vertical and organic-noise layout of one enclosed mining cavern and its bridge. */
    record Mine(
            int bottomY,
            int quarryFloorY,
            int floorVariation,
            int bridgeY,
            int ceilingY,
            int ceilingVariation,
            int wallThickness,
            int bridgeWidth,
            List<FarmZoneDefinition> zones
    ) implements FarmGenerationSettings {
        public Mine {
            if (quarryFloorY <= bottomY
                    || floorVariation < 1
                    || bridgeY < quarryFloorY + floorVariation + 4
                    || ceilingVariation < 1
                    || ceilingY - ceilingVariation < bridgeY + 24
                    || wallThickness < 8
                    || bridgeWidth < 5
                    || bridgeWidth % 2 == 0) {
                throw new IllegalArgumentException("Invalid mine cavern layout");
            }
            zones = List.copyOf(zones);
        }

        @Override
        public int spawnY() {
            return bridgeY + 1;
        }
    }

    record OreRule(Material material, double chance, int minimumY, int maximumY) {
    }

    record Fields(
            int bottomY,
            int soilY,
            int pathSpacing,
            Material pathMaterial,
            List<FarmZoneDefinition> zones
    ) implements FarmGenerationSettings {
        public Fields {
            zones = List.copyOf(zones);
        }

        @Override
        public int spawnY() {
            return soilY + 1;
        }
    }

    record Fishing(
            int bottomY,
            int seabedY,
            int waterY,
            int platformRadius,
            Material seabedMaterial,
            Material platformMaterial
    ) implements FarmGenerationSettings {
        @Override
        public int spawnY() {
            return waterY + 2;
        }
    }

    /** Organic floating-forest island layout with varied tree silhouettes. */
    record Forest(
            int bottomY,
            int groundY,
            int terrainVariation,
            int islandDepth,
            int treeSpacing,
            int minimumTrunkHeight,
            int maximumTrunkHeight,
            int pathSpacing,
            int platformRadius,
            Material platformMaterial,
            int bridgeWidth,
            List<FarmZoneDefinition> zones
    ) implements FarmGenerationSettings {
        public Forest {
            if (terrainVariation < 1
                    || islandDepth < 6
                    || groundY - terrainVariation - islandDepth - 4 < bottomY
                    || treeSpacing < 12
                    || minimumTrunkHeight < 4
                    || maximumTrunkHeight < minimumTrunkHeight
                    || pathSpacing < treeSpacing * 2
                    || platformRadius < 4
                    || bridgeWidth < 5
                    || bridgeWidth % 2 == 0) {
                throw new IllegalArgumentException("Invalid forest island layout");
            }
            zones = List.copyOf(zones);
        }

        @Override
        public int spawnY() {
            return groundY + 1;
        }
    }
}
