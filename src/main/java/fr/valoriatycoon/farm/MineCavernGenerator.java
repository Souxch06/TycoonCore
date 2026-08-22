package fr.valoriatycoon.farm;

import java.util.Random;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

/** Generates four immense organic mining caverns separated by void bridges and rank portals. */
final class MineCavernGenerator {
    private static final int ROOF_THICKNESS = 5;
    private static final int PORTAL_HEIGHT = 15;
    private static final int PILLAR_CELL_SIZE = 224;

    private final FarmGenerationSettings.Mine settings;

    MineCavernGenerator(FarmGenerationSettings.Mine settings) {
        this.settings = settings;
    }

    void generate(int chunkX, int chunkZ, Random random, ChunkData data) {
        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkX * 16 + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkZ * 16 + localZ;
                FarmZoneDefinition zone = zoneAt(worldX, worldZ);
                if (zone != null) {
                    generateCavernColumn(data, localX, localZ, worldX, worldZ, random, zone);
                    continue;
                }
                FarmBridgeDefinition bridge = bridgeAt(worldX, worldZ);
                if (bridge != null) {
                    generateBridgeColumn(data, localX, localZ, worldX, worldZ, bridge);
                }
            }
        }
    }

    private void generateCavernColumn(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            Random random,
            FarmZoneDefinition zone
    ) {
        FarmZoneDefinition.MineResource resource =
                (FarmZoneDefinition.MineResource) zone.resource();
        int relativeX = worldX - zone.centerX();
        int relativeZ = worldZ - zone.centerZ();
        int floorY = cavernFloorY(worldX, worldZ);
        int roofY = cavernRoofY(worldX, worldZ);
        boolean wall = isCavernWall(zone, worldX, worldZ);
        boolean pillar = !wall && isNaturalPillar(worldX, worldZ, relativeX, relativeZ);

        data.setBlock(localX, settings.bottomY(), localZ, Material.BEDROCK);
        for (int y = settings.bottomY() + 1; y <= floorY; y++) {
            data.setBlock(
                    localX,
                    y,
                    localZ,
                    selectMineMaterial(random, resource, worldX, y, worldZ)
            );
        }

        if (wall || pillar) {
            for (int y = floorY + 1; y < roofY; y++) {
                data.setBlock(localX, y, localZ, rockMaterial(resource.filler(), worldX, y, worldZ));
            }
        }
        for (int y = roofY; y <= roofY + ROOF_THICKNESS; y++) {
            data.setBlock(localX, y, localZ, rockMaterial(resource.filler(), worldX, y, worldZ));
        }
        data.setBlock(localX, roofY + ROOF_THICKNESS + 1, localZ, Material.BEDROCK);

        boolean portal = isPortal(zone, worldX, worldZ);
        if (portal) {
            for (int y = settings.bridgeY() + 1;
                 y <= settings.bridgeY() + PORTAL_HEIGHT;
                 y++) {
                data.setBlock(localX, y, localZ, Material.AIR);
            }
        }

        generateCentralWalkway(
                data,
                localX,
                localZ,
                relativeX,
                relativeZ,
                floorY
        );
        if (!wall && !pillar && !insideProtectedPassage(relativeX, relativeZ)) {
            generateNaturalFormations(
                    data,
                    localX,
                    localZ,
                    worldX,
                    worldZ,
                    floorY,
                    roofY
            );
        }
        if (!portal) {
            generateCavernDecorations(
                    data,
                    localX,
                    localZ,
                    relativeX,
                    relativeZ,
                    roofY
            );
        }
    }

    private void generateCentralWalkway(
            ChunkData data,
            int localX,
            int localZ,
            int relativeX,
            int relativeZ,
            int floorY
    ) {
        int absoluteX = Math.abs(relativeX);
        int absoluteZ = Math.abs(relativeZ);
        int halfWidth = settings.bridgeWidth() / 2;
        int platformRadius = 28;
        boolean avenue = absoluteZ <= halfWidth;
        boolean arrivalPlatform = absoluteX <= platformRadius && absoluteZ <= platformRadius;
        if (avenue || arrivalPlatform) {
            data.setBlock(localX, settings.bridgeY(), localZ, Material.POLISHED_DEEPSLATE);
        }
        if (absoluteZ == halfWidth + 1 && !arrivalPlatform) {
            data.setBlock(localX, settings.bridgeY() + 1, localZ, Material.IRON_BARS);
        }
        if (avenue && absoluteZ <= 1 && Math.floorMod(relativeX, 64) == 0) {
            for (int y = floorY + 1; y < settings.bridgeY(); y++) {
                data.setBlock(localX, y, localZ, Material.DEEPSLATE_BRICKS);
            }
        }

        int rampStart = platformRadius + 1;
        int rampEnd = rampStart + (settings.bridgeY() - settings.quarryFloorY()) * 4;
        boolean rampDeck = absoluteX <= halfWidth
                && absoluteZ >= rampStart
                && absoluteZ <= rampEnd;
        boolean rampRail = absoluteX == halfWidth + 1
                && absoluteZ >= rampStart
                && absoluteZ <= rampEnd;
        if (rampDeck || rampRail) {
            int descent = Math.max(0, (absoluteZ - rampStart) / 4);
            int rampY = Math.max(floorY + 1, settings.bridgeY() - descent);
            if (rampDeck) {
                for (int y = floorY + 1; y <= rampY; y++) {
                    data.setBlock(localX, y, localZ, Material.DEEPSLATE_BRICKS);
                }
                data.setBlock(localX, rampY, localZ, Material.POLISHED_DEEPSLATE);
            } else {
                data.setBlock(localX, rampY + 1, localZ, Material.DEEPSLATE_BRICK_WALL);
            }
        }
    }

    private void generateNaturalFormations(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            int floorY,
            int roofY
    ) {
        double dripstone = valueNoise(worldX, worldZ, 42, 7_031);
        int availableHeight = roofY - floorY - 12;
        if (availableHeight <= 0) {
            return;
        }
        if (dripstone > 0.76) {
            int length = Math.min(
                    availableHeight / 3,
                    2 + (int) Math.floor((dripstone - 0.76) * 70.0)
            );
            for (int offset = 1; offset <= length; offset++) {
                data.setBlock(localX, roofY - offset, localZ, Material.DRIPSTONE_BLOCK);
            }
        }
        if (dripstone < -0.80) {
            int height = Math.min(
                    availableHeight / 4,
                    2 + (int) Math.floor((-dripstone - 0.80) * 60.0)
            );
            for (int offset = 1; offset <= height; offset++) {
                data.setBlock(localX, floorY + offset, localZ, Material.DRIPSTONE_BLOCK);
            }
        }
    }

    private void generateCavernDecorations(
            ChunkData data,
            int localX,
            int localZ,
            int relativeX,
            int relativeZ,
            int roofY
    ) {
        int absoluteX = Math.abs(relativeX);
        int absoluteZ = Math.abs(relativeZ);
        if ((absoluteX == 20 || absoluteX == 25)
                && (absoluteZ == 20 || absoluteZ == 25)) {
            data.setBlock(localX, settings.bridgeY() + 1, localZ, Material.BARREL);
        }
        if (relativeZ == -18 && absoluteX <= 15) {
            data.setBlock(localX, settings.bridgeY() + 1, localZ, Material.RAIL);
        }
        if (relativeZ == 0 && Math.floorMod(relativeX, 96) == 0) {
            int lanternY = settings.bridgeY() + 9;
            for (int y = roofY - 1; y > lanternY; y--) {
                data.setBlock(localX, y, localZ, Material.CHAIN);
            }
            data.setBlock(localX, lanternY, localZ, Material.LANTERN);
        }
        if (absoluteX == 27 && absoluteZ == 27) {
            for (int y = settings.bridgeY() + 1; y <= settings.bridgeY() + 6; y++) {
                data.setBlock(localX, y, localZ, Material.DEEPSLATE_BRICK_WALL);
            }
            data.setBlock(localX, settings.bridgeY() + 7, localZ, Material.LANTERN);
        }
    }

    private void generateBridgeColumn(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            FarmBridgeDefinition bridge
    ) {
        int distanceZ = Math.abs(worldZ - bridge.centerZ());
        int halfWidth = bridge.width() / 2;
        if (distanceZ <= halfWidth) {
            data.setBlock(localX, settings.bridgeY(), localZ, Material.POLISHED_DEEPSLATE);
        }
        if (distanceZ == halfWidth + 1) {
            data.setBlock(localX, settings.bridgeY() + 1, localZ, Material.IRON_BARS);
        }
        if (distanceZ <= 1 && Math.floorMod(worldX - bridge.minimumX(), 64) == 0) {
            for (int y = settings.bridgeY() - 12; y < settings.bridgeY(); y++) {
                data.setBlock(localX, y, localZ, Material.DEEPSLATE_BRICKS);
            }
        }
        if (distanceZ == halfWidth + 1
                && Math.floorMod(worldX - bridge.minimumX(), 48) == 0) {
            data.setBlock(localX, settings.bridgeY() + 2, localZ, Material.DEEPSLATE_BRICK_WALL);
            data.setBlock(localX, settings.bridgeY() + 3, localZ, Material.LANTERN);
        }
        generateRankGate(data, localX, localZ, worldX, worldZ, bridge);
    }

    private void generateRankGate(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            FarmBridgeDefinition bridge
    ) {
        int distanceZ = Math.abs(worldZ - bridge.centerZ());
        int halfWidth = bridge.width() / 2;
        boolean gateColumn = worldX == bridge.minimumX()
                || worldX == bridge.maximumX();
        if (!gateColumn) {
            return;
        }
        if (distanceZ <= halfWidth) {
            data.setBlock(localX, settings.bridgeY(), localZ, Material.RED_NETHER_BRICKS);
        }
        if (distanceZ >= halfWidth && distanceZ <= halfWidth + 1) {
            for (int y = settings.bridgeY() + 1; y <= settings.bridgeY() + 13; y++) {
                data.setBlock(localX, y, localZ, Material.POLISHED_BLACKSTONE_BRICKS);
            }
            data.setBlock(localX, settings.bridgeY() + 7, localZ, Material.CRYING_OBSIDIAN);
        }
        if (distanceZ <= halfWidth + 1) {
            data.setBlock(
                    localX,
                    settings.bridgeY() + 13,
                    localZ,
                    Material.POLISHED_BLACKSTONE_BRICKS
            );
        }
        if (distanceZ == halfWidth) {
            data.setBlock(localX, settings.bridgeY() + 11, localZ, Material.REDSTONE_LAMP);
        }
    }

    private int cavernFloorY(int worldX, int worldZ) {
        double noise = fractalNoise(worldX, worldZ, 1_013);
        int variation = (int) Math.round(noise * settings.floorVariation());
        return Math.min(
                settings.bridgeY() - 4,
                settings.quarryFloorY() + variation
        );
    }

    private int cavernRoofY(int worldX, int worldZ) {
        double noise = fractalNoise(worldX, worldZ, 5_021);
        int variation = (int) Math.round(noise * settings.ceilingVariation());
        return Math.max(
                settings.bridgeY() + 24,
                settings.ceilingY() + variation
        );
    }

    private boolean isCavernWall(FarmZoneDefinition zone, int worldX, int worldZ) {
        int edgeDistance = distanceFromEdge(zone, worldX, worldZ);
        double noise = valueNoise(worldX, worldZ, 96, 9_019);
        int irregularThickness = settings.wallThickness()
                + (int) Math.round(noise * settings.wallThickness() * 0.45);
        return edgeDistance < Math.max(8, irregularThickness);
    }

    private boolean isNaturalPillar(
            int worldX,
            int worldZ,
            int relativeX,
            int relativeZ
    ) {
        if (insideProtectedPassage(relativeX, relativeZ)) {
            return false;
        }
        int cellX = Math.floorDiv(worldX, PILLAR_CELL_SIZE);
        int cellZ = Math.floorDiv(worldZ, PILLAR_CELL_SIZE);
        long hash = coordinateHash(cellX, 11_033, cellZ);
        int centerX = cellX * PILLAR_CELL_SIZE
                + 56
                + (int) Math.floorMod(hash, PILLAR_CELL_SIZE - 112L);
        int centerZ = cellZ * PILLAR_CELL_SIZE
                + 56
                + (int) Math.floorMod(hash >>> 16, PILLAR_CELL_SIZE - 112L);
        int radius = 9 + (int) Math.floorMod(hash >>> 32, 12L);
        long dx = (long) worldX - centerX;
        long dz = (long) worldZ - centerZ;
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    private boolean insideProtectedPassage(int relativeX, int relativeZ) {
        int halfWidth = settings.bridgeWidth() / 2;
        return Math.abs(relativeZ) <= halfWidth + 18
                || Math.hypot(relativeX, relativeZ) <= 72.0;
    }

    private int distanceFromEdge(FarmZoneDefinition zone, int worldX, int worldZ) {
        if (zone.shape() == FarmZoneDefinition.Shape.CIRCLE) {
            double radius = zone.size() / 2.0 - 0.5;
            double distance = Math.hypot(worldX - zone.centerX(), worldZ - zone.centerZ());
            return (int) Math.floor(radius - distance);
        }
        return Math.min(
                Math.min(worldX - zone.minimumX(), zone.maximumX() - worldX),
                Math.min(worldZ - zone.minimumZ(), zone.maximumZ() - worldZ)
        );
    }

    private boolean isPortal(FarmZoneDefinition zone, int worldX, int worldZ) {
        int portalHalfWidth = settings.bridgeWidth() / 2 + 1;
        if (Math.abs(worldZ - zone.centerZ()) > portalHalfWidth) {
            return false;
        }
        int maximumWallDepth = settings.wallThickness() * 2;
        boolean westPortal = zone.index() > 1
                && worldX - zone.minimumX() < maximumWallDepth;
        boolean eastPortal = zone.index() < settings.zones().size()
                && zone.maximumX() - worldX < maximumWallDepth;
        return westPortal || eastPortal;
    }

    private Material selectMineMaterial(
            Random random,
            FarmZoneDefinition.MineResource resource,
            int x,
            int y,
            int z
    ) {
        double roll = random.nextDouble();
        double cumulative = 0.0;
        for (FarmGenerationSettings.OreRule ore : resource.ores()) {
            if (y < ore.minimumY() || y > ore.maximumY()) {
                continue;
            }
            cumulative += ore.chance();
            if (roll < cumulative) {
                return ore.material();
            }
        }
        return rockMaterial(resource.filler(), x, y, z);
    }

    private Material rockMaterial(Material filler, int x, int y, int z) {
        if (filler != Material.STONE) {
            return filler;
        }
        double primary = valueNoise(x + y * 2, z - y, 52, 12_043);
        double secondary = valueNoise(x - y, z + y * 2, 84, 16_057);
        if (y <= 0) {
            if (primary > 0.72) {
                return Material.TUFF;
            }
            if (secondary < -0.82) {
                return Material.ANDESITE;
            }
            return Material.DEEPSLATE;
        }
        if (secondary > 0.88) {
            return Material.CALCITE;
        }
        if (primary > 0.76) {
            return Material.GRANITE;
        }
        if (primary < -0.78) {
            return Material.DIORITE;
        }
        if (secondary < -0.80) {
            return Material.ANDESITE;
        }
        if (secondary > 0.72) {
            return Material.TUFF;
        }
        return Material.STONE;
    }

    private double fractalNoise(int x, int z, int salt) {
        return valueNoise(x, z, 320, salt) * 0.52
                + valueNoise(x, z, 128, salt + 1) * 0.30
                + valueNoise(x, z, 48, salt + 2) * 0.18;
    }

    private double valueNoise(int x, int z, int scale, int salt) {
        int gridX = Math.floorDiv(x, scale);
        int gridZ = Math.floorDiv(z, scale);
        double fractionX = Math.floorMod(x, scale) / (double) scale;
        double fractionZ = Math.floorMod(z, scale) / (double) scale;
        double smoothX = smooth(fractionX);
        double smoothZ = smooth(fractionZ);
        double northWest = unitNoise(gridX, gridZ, salt);
        double northEast = unitNoise(gridX + 1, gridZ, salt);
        double southWest = unitNoise(gridX, gridZ + 1, salt);
        double southEast = unitNoise(gridX + 1, gridZ + 1, salt);
        double north = lerp(northWest, northEast, smoothX);
        double south = lerp(southWest, southEast, smoothX);
        return lerp(north, south, smoothZ);
    }

    private double unitNoise(int x, int z, int salt) {
        long hash = coordinateHash(x, salt, z);
        return ((hash >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private double smooth(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private long coordinateHash(int x, int y, int z) {
        long value = 0x6A09E667F3BCC909L;
        value ^= (long) settings.bottomY() * 31L;
        value ^= (long) settings.quarryFloorY() * 131L;
        value ^= (long) settings.floorVariation() * 263L;
        value ^= (long) settings.bridgeY() * 521L;
        value ^= (long) settings.ceilingY() * 2_053L;
        value ^= (long) settings.ceilingVariation() * 4_099L;
        value ^= (long) x * 341_873_128_712L;
        value ^= (long) y * 132_897_987_541L;
        value ^= (long) z * 42_317_861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }

    private FarmZoneDefinition zoneAt(int x, int z) {
        for (FarmZoneDefinition zone : settings.zones()) {
            if (zone.contains(x, z)) {
                return zone;
            }
        }
        return null;
    }

    private FarmBridgeDefinition bridgeAt(int x, int z) {
        for (int index = 0; index < settings.zones().size() - 1; index++) {
            FarmZoneDefinition from = settings.zones().get(index);
            FarmZoneDefinition to = settings.zones().get(index + 1);
            FarmBridgeDefinition bridge = new FarmBridgeDefinition(
                    from.index(),
                    to.index(),
                    from.maximumX() + 1,
                    to.minimumX() - 1,
                    (from.centerZ() + to.centerZ()) / 2,
                    settings.bridgeWidth(),
                    to.requiredRank()
            );
            if (x >= bridge.minimumX()
                    && x <= bridge.maximumX()
                    && Math.abs(z - bridge.centerZ()) <= bridge.width() / 2 + 1) {
                return bridge;
            }
        }
        return null;
    }
}
