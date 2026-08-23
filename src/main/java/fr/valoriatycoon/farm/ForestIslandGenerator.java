package fr.valoriatycoon.farm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.ChunkGenerator.ChunkData;

/** Generates immense organic forest islands with deterministic species-specific trees. */
final class ForestIslandGenerator {
    private final FarmGenerationSettings.Forest settings;
    private final Map<Material, BlockData> persistentLeaves;

    ForestIslandGenerator(FarmGenerationSettings.Forest settings) {
        this.settings = settings;
        this.persistentLeaves = createPersistentLeaves(settings);
    }

    void generate(int chunkX, int chunkZ, ChunkData data) {
        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkX * 16 + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkZ * 16 + localZ;
                FarmZoneDefinition zone = zoneAt(worldX, worldZ);
                if (zone != null) {
                    if (insideOrganicIsland(zone, worldX, worldZ)) {
                        generateIslandColumn(data, localX, localZ, worldX, worldZ, zone);
                    }
                    continue;
                }
                FarmBridgeDefinition bridge = bridgeAt(worldX, worldZ);
                if (bridge != null) {
                    generateBridgeColumn(data, localX, localZ, worldX, worldZ, bridge);
                }
            }
        }
    }

    private void generateIslandColumn(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            FarmZoneDefinition zone
    ) {
        FarmZoneDefinition.ForestResource resource =
                (FarmZoneDefinition.ForestResource) zone.resource();
        int relativeX = worldX - zone.centerX();
        int relativeZ = worldZ - zone.centerZ();
        boolean arrivalPlatform = Math.hypot(relativeX, relativeZ) <= settings.platformRadius();
        boolean avenue = isAvenue(zone, relativeX, relativeZ, 0);
        boolean trail = isTrail(worldX, worldZ, zone);
        int surfaceY = terrainSurfaceY(worldX, worldZ, zone, arrivalPlatform || avenue);
        int depth = islandDepth(zone, worldX, worldZ);
        int stoneTopY = surfaceY - 4;

        for (int y = surfaceY - depth; y <= stoneTopY; y++) {
            data.setBlock(localX, y, localZ, naturalStone(worldX, y, worldZ));
        }
        for (int y = stoneTopY + 1; y < surfaceY; y++) {
            data.setBlock(localX, y, localZ, Material.DIRT);
        }
        data.setBlock(
                localX,
                surfaceY,
                localZ,
                surfaceMaterial(zone, worldX, worldZ, arrivalPlatform, avenue, trail)
        );

        boolean trunk = false;
        if (!arrivalPlatform && !avenue && !trail) {
            trunk = generateTreeColumn(
                    data,
                    localX,
                    localZ,
                    worldX,
                    worldZ,
                    surfaceY,
                    zone,
                    resource
            );
        }
        if (!trunk) {
            generateGroundDecoration(
                    data,
                    localX,
                    localZ,
                    worldX,
                    worldZ,
                    surfaceY,
                    zone,
                    arrivalPlatform,
                    avenue,
                    trail
            );
        }
    }

    private int terrainSurfaceY(
            int worldX,
            int worldZ,
            FarmZoneDefinition zone,
            boolean flattened
    ) {
        if (flattened) {
            return settings.groundY();
        }
        int relativeX = worldX - zone.centerX();
        int relativeZ = worldZ - zone.centerZ();
        double broad = valueNoise(relativeX, relativeZ, 288, zone.index() * 1_003);
        double detail = valueNoise(relativeX, relativeZ, 96, zone.index() * 1_003 + 1);
        int variation = (int) Math.round(
                (broad * 0.72 + detail * 0.28) * settings.terrainVariation()
        );
        return settings.groundY() + variation;
    }

    private int islandDepth(FarmZoneDefinition zone, int worldX, int worldZ) {
        int edgeDistance = distanceFromEdge(zone, worldX, worldZ);
        int edgeDepth = Math.min(settings.islandDepth(), 6 + Math.max(0, edgeDistance) / 10);
        int undersideNoise = (int) Math.round(valueNoise(
                worldX,
                worldZ,
                128,
                20_011 + zone.index()
        ) * 4.0);
        return Math.max(6, Math.min(settings.islandDepth() + 4, edgeDepth + undersideNoise));
    }

    private Material surfaceMaterial(
            FarmZoneDefinition zone,
            int worldX,
            int worldZ,
            boolean arrivalPlatform,
            boolean avenue,
            boolean trail
    ) {
        if (arrivalPlatform) {
            return settings.platformMaterial();
        }
        if (avenue || trail) {
            return Material.DIRT_PATH;
        }
        double patch = valueNoise(worldX, worldZ, 72, 30_013 + zone.index());
        return switch (zone.index()) {
            case 1 -> patch > 0.78 ? Material.MOSS_BLOCK : Material.GRASS_BLOCK;
            case 2 -> patch < -0.82 ? Material.COARSE_DIRT : Material.GRASS_BLOCK;
            case 3 -> patch > -0.45 ? Material.PODZOL : Material.COARSE_DIRT;
            case 4 -> patch > 0.35 ? Material.MOSS_BLOCK : Material.PODZOL;
            default -> Material.GRASS_BLOCK;
        };
    }

    private boolean generateTreeColumn(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            int surfaceY,
            FarmZoneDefinition zone,
            FarmZoneDefinition.ForestResource resource
    ) {
        List<TreeSpec> trees = nearbyTrees(zone, resource, worldX, worldZ);
        BlockData leaves = persistentLeaves.get(resource.leaves());
        for (TreeSpec tree : trees) {
            generateCanopyColumn(data, localX, localZ, worldX, worldZ, tree, leaves);
        }
        boolean trunk = false;
        for (TreeSpec tree : trees) {
            if (!insideTrunk(worldX - tree.centerX(), worldZ - tree.centerZ(), tree.trunkWidth())) {
                continue;
            }
            trunk = true;
            for (int y = surfaceY + 1; y <= tree.topY(); y++) {
                data.setBlock(localX, y, localZ, resource.log());
            }
        }
        return trunk;
    }

    private void generateCanopyColumn(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            TreeSpec tree,
            BlockData leaves
    ) {
        int dx = worldX - tree.centerX();
        int dz = worldZ - tree.centerZ();
        int horizontalDistance = Math.max(Math.abs(dx), Math.abs(dz));
        switch (tree.kind()) {
            case OAK -> generateRoundedCanopy(
                    data,
                    localX,
                    localZ,
                    horizontalDistance,
                    tree.topY() - 4,
                    tree.topY() + 2,
                    tree.crownRadius(),
                    leaves
            );
            case BIRCH -> generateRoundedCanopy(
                    data,
                    localX,
                    localZ,
                    horizontalDistance,
                    tree.topY() - 3,
                    tree.topY() + 2,
                    tree.crownRadius(),
                    leaves
            );
            case DARK_OAK -> generateRoundedCanopy(
                    data,
                    localX,
                    localZ,
                    horizontalDistance,
                    tree.topY() - 5,
                    tree.topY() + 3,
                    tree.crownRadius(),
                    leaves
            );
            case SPRUCE -> generateSpruceCanopy(
                    data,
                    localX,
                    localZ,
                    horizontalDistance,
                    tree,
                    leaves
            );
        }
    }

    private void generateRoundedCanopy(
            ChunkData data,
            int localX,
            int localZ,
            int horizontalDistance,
            int minimumY,
            int maximumY,
            int radius,
            BlockData leaves
    ) {
        if (horizontalDistance > radius) {
            return;
        }
        int centerY = maximumY - 3;
        for (int y = minimumY; y <= maximumY; y++) {
            int verticalDistance = Math.abs(y - centerY);
            int allowedRadius = Math.max(1, radius - Math.max(0, verticalDistance - 1));
            if (horizontalDistance <= allowedRadius
                    && !(horizontalDistance == allowedRadius && verticalDistance >= 3)) {
                data.setBlock(localX, y, localZ, leaves);
            }
        }
    }

    private void generateSpruceCanopy(
            ChunkData data,
            int localX,
            int localZ,
            int horizontalDistance,
            TreeSpec tree,
            BlockData leaves
    ) {
        if (horizontalDistance > tree.crownRadius()) {
            return;
        }
        int minimumY = tree.baseY() + tree.height() / 3;
        for (int y = minimumY; y <= tree.topY() + 2; y++) {
            int distanceFromTop = tree.topY() + 2 - y;
            int allowedRadius = Math.min(
                    tree.crownRadius(),
                    1 + distanceFromTop * tree.crownRadius()
                            / Math.max(1, tree.topY() + 2 - minimumY)
            );
            if (distanceFromTop % 3 == 0) {
                allowedRadius = Math.max(1, allowedRadius - 1);
            }
            if (horizontalDistance <= allowedRadius) {
                data.setBlock(localX, y, localZ, leaves);
            }
        }
    }

    private List<TreeSpec> nearbyTrees(
            FarmZoneDefinition zone,
            FarmZoneDefinition.ForestResource resource,
            int worldX,
            int worldZ
    ) {
        int spacing = settings.treeSpacing();
        int cellX = Math.floorDiv(worldX - zone.minimumX(), spacing);
        int cellZ = Math.floorDiv(worldZ - zone.minimumZ(), spacing);
        List<TreeSpec> trees = new ArrayList<>(9);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                TreeSpec tree = treeInCell(zone, resource, cellX + offsetX, cellZ + offsetZ);
                if (tree != null) {
                    trees.add(tree);
                }
            }
        }
        return trees;
    }

    private TreeSpec treeInCell(
            FarmZoneDefinition zone,
            FarmZoneDefinition.ForestResource resource,
            int cellX,
            int cellZ
    ) {
        int spacing = settings.treeSpacing();
        long hash = coordinateHash(cellX, zone.index() * 40_009, cellZ);
        int jitterRange = Math.max(2, spacing / 3);
        int centerX = zone.minimumX()
                + cellX * spacing
                + spacing / 2
                + (int) Math.floorMod(hash, jitterRange * 2L + 1L)
                - jitterRange;
        int centerZ = zone.minimumZ()
                + cellZ * spacing
                + spacing / 2
                + (int) Math.floorMod(hash >>> 16, jitterRange * 2L + 1L)
                - jitterRange;
        TreeKind kind = TreeKind.from(resource.log());
        int crownRadius = kind.crownRadius(hash);
        int margin = crownRadius + 4;
        if (centerX < zone.minimumX() + margin
                || centerX > zone.maximumX() - margin
                || centerZ < zone.minimumZ() + margin
                || centerZ > zone.maximumZ() - margin
                || !insideOrganicIsland(zone, centerX, centerZ)
                || organicEdgeDistance(zone, centerX, centerZ) < margin
                || treeCenterReserved(centerX, centerZ, zone, crownRadius + 5)) {
            return null;
        }
        int configuredRange = settings.maximumTrunkHeight() - settings.minimumTrunkHeight() + 1;
        int rawHeight = settings.minimumTrunkHeight()
                + (int) Math.floorMod(hash >>> 32, configuredRange);
        int height = kind.adjustHeight(
                rawHeight,
                settings.minimumTrunkHeight(),
                settings.maximumTrunkHeight()
        );
        int trunkWidth = kind.trunkWidth(hash);
        int baseY = terrainSurfaceY(centerX, centerZ, zone, false);
        return new TreeSpec(
                kind,
                centerX,
                centerZ,
                baseY,
                height,
                trunkWidth,
                crownRadius
        );
    }

    private boolean treeCenterReserved(
            int centerX,
            int centerZ,
            FarmZoneDefinition zone,
            int margin
    ) {
        int relativeX = centerX - zone.centerX();
        int relativeZ = centerZ - zone.centerZ();
        if (isAvenue(zone, relativeX, relativeZ, margin)
                || Math.hypot(relativeX, relativeZ) <= settings.platformRadius() + margin) {
            return true;
        }
        return distanceFromTrail(centerX, centerZ, zone) <= margin;
    }

    private boolean insideTrunk(int dx, int dz, int width) {
        int minimum = -width / 2;
        int maximumExclusive = minimum + width;
        return dx >= minimum && dx < maximumExclusive
                && dz >= minimum && dz < maximumExclusive;
    }

    private void generateGroundDecoration(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            int surfaceY,
            FarmZoneDefinition zone,
            boolean arrivalPlatform,
            boolean avenue,
            boolean trail
    ) {
        if (arrivalPlatform) {
            if (worldX == zone.centerX() && worldZ == zone.centerZ() + 12) {
                data.setBlock(localX, surfaceY + 1, localZ, Material.CAMPFIRE);
            }
            return;
        }
        if (avenue) {
            int edge = settings.bridgeWidth() / 2 + 2;
            if (Math.abs(worldZ - zone.centerZ()) == edge
                    && Math.floorMod(worldX - zone.centerX(), 96) == 0) {
                data.setBlock(localX, surfaceY + 1, localZ, Material.SPRUCE_FENCE);
                data.setBlock(localX, surfaceY + 2, localZ, Material.SPRUCE_FENCE);
                data.setBlock(localX, surfaceY + 3, localZ, Material.LANTERN);
            }
            return;
        }
        if (trail) {
            return;
        }
        int roll = (int) Math.floorMod(coordinateHash(worldX, zone.index(), worldZ), 1_000L);
        if (roll < 5) {
            data.setBlock(localX, surfaceY + 1, localZ, Material.MOSSY_COBBLESTONE);
            return;
        }
        Material decoration = switch (zone.index()) {
            case 1 -> roll < 25 ? Material.FERN : roll < 30 ? Material.DANDELION : null;
            case 2 -> roll < 22 ? Material.FERN : roll < 28 ? Material.AZURE_BLUET : null;
            case 3 -> roll < 28 ? Material.FERN : roll < 34 ? Material.BROWN_MUSHROOM : null;
            case 4 -> roll < 22 ? Material.BROWN_MUSHROOM
                    : roll < 32 ? Material.RED_MUSHROOM
                    : roll < 42 ? Material.FERN : null;
            default -> null;
        };
        if (decoration != null) {
            data.setBlock(localX, surfaceY + 1, localZ, decoration);
        }
    }

    private Material naturalStone(int x, int y, int z) {
        double patch = valueNoise(x + y, z - y, 64, 50_023);
        if (patch > 0.82) {
            return Material.ANDESITE;
        }
        if (patch < -0.86) {
            return Material.TUFF;
        }
        return Material.STONE;
    }

    private boolean isAvenue(
            FarmZoneDefinition zone,
            int relativeX,
            int relativeZ,
            int margin
    ) {
        if (Math.abs(relativeZ) > settings.bridgeWidth() / 2 + margin) {
            return false;
        }
        boolean connectsWest = zone.index() > 1
                || relativeX >= -settings.platformRadius() - margin;
        boolean connectsEast = zone.index() < settings.zones().size()
                || relativeX <= settings.platformRadius() + margin;
        return connectsWest && connectsEast;
    }

    private boolean isTrail(int worldX, int worldZ, FarmZoneDefinition zone) {
        return distanceFromTrail(worldX, worldZ, zone) <= 2;
    }

    private int distanceFromTrail(int worldX, int worldZ, FarmZoneDefinition zone) {
        int bend = (int) Math.round(valueNoise(
                worldZ,
                zone.index() * 97,
                160,
                60_029 + zone.index()
        ) * 24.0);
        int coordinate = worldX - zone.minimumX() + bend;
        int remainder = Math.floorMod(coordinate, settings.pathSpacing());
        return Math.abs(remainder - settings.pathSpacing() / 2);
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
            data.setBlock(localX, settings.groundY(), localZ, Material.SPRUCE_PLANKS);
        }
        if (distanceZ == halfWidth + 1) {
            data.setBlock(localX, settings.groundY() + 1, localZ, Material.SPRUCE_FENCE);
        }
        if (distanceZ <= 1 && Math.floorMod(worldX - bridge.minimumX(), 64) == 0) {
            for (int y = settings.groundY() - 10; y < settings.groundY(); y++) {
                data.setBlock(localX, y, localZ, Material.STRIPPED_SPRUCE_LOG);
            }
        }
        if (distanceZ == halfWidth + 1
                && Math.floorMod(worldX - bridge.minimumX(), 48) == 0) {
            data.setBlock(localX, settings.groundY() + 2, localZ, Material.SPRUCE_FENCE);
            data.setBlock(localX, settings.groundY() + 3, localZ, Material.LANTERN);
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
            data.setBlock(localX, settings.groundY(), localZ, Material.RED_NETHER_BRICKS);
        }
        if (distanceZ >= halfWidth && distanceZ <= halfWidth + 1) {
            for (int y = settings.groundY() + 1; y <= settings.groundY() + 11; y++) {
                data.setBlock(localX, y, localZ, Material.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        if (distanceZ <= halfWidth + 1) {
            data.setBlock(
                    localX,
                    settings.groundY() + 11,
                    localZ,
                    Material.POLISHED_BLACKSTONE_BRICKS
            );
        }
        if (distanceZ == halfWidth) {
            data.setBlock(localX, settings.groundY() + 9, localZ, Material.REDSTONE_LAMP);
        }
    }

    private int distanceFromEdge(FarmZoneDefinition zone, int worldX, int worldZ) {
        return organicEdgeDistance(zone, worldX, worldZ);
    }

    private boolean insideOrganicIsland(
            FarmZoneDefinition zone,
            int worldX,
            int worldZ
    ) {
        return organicEdgeDistance(zone, worldX, worldZ) >= 0;
    }

    private int organicEdgeDistance(
            FarmZoneDefinition zone,
            int worldX,
            int worldZ
    ) {
        int relativeX = worldX - zone.centerX();
        int relativeZ = worldZ - zone.centerZ();
        if (isAvenue(zone, relativeX, relativeZ, 4)) {
            return Math.min(
                    worldX - zone.minimumX(),
                    zone.maximumX() - worldX
            );
        }
        double halfSize = zone.size() / 2.0;
        double normalizedX = Math.abs(worldX - zone.centerX()) / halfSize;
        double normalizedZ = Math.abs(worldZ - zone.centerZ()) / halfSize;
        double roundedDistance = Math.pow(
                Math.pow(normalizedX, 4.0) + Math.pow(normalizedZ, 4.0),
                0.25
        );
        double edgeNoise = valueNoise(
                worldX,
                worldZ,
                144,
                70_031 + zone.index()
        ) * 0.025;
        return (int) Math.floor((1.0 + edgeNoise - roundedDistance) * halfSize);
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
        long value = 0xBB67AE8584CAA73BL;
        value ^= (long) settings.groundY() * 131L;
        value ^= (long) settings.terrainVariation() * 521L;
        value ^= (long) settings.treeSpacing() * 2_053L;
        value ^= (long) x * 341_873_128_712L;
        value ^= (long) y * 132_897_987_541L;
        value ^= (long) z * 42_317_861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }

    private Map<Material, BlockData> createPersistentLeaves(
            FarmGenerationSettings.Forest generation
    ) {
        Map<Material, BlockData> result = new EnumMap<>(Material.class);
        for (FarmZoneDefinition zone : generation.zones()) {
            FarmZoneDefinition.ForestResource forest =
                    (FarmZoneDefinition.ForestResource) zone.resource();
            Material material = forest.leaves();
            BlockData data = Bukkit.createBlockData(material);
            if (!(data instanceof Leaves leaves)) {
                throw new IllegalArgumentException(
                        "Configured forest leaves material is not leaves: " + material
                );
            }
            leaves.setPersistent(true);
            result.put(material, leaves);
        }
        return Map.copyOf(result);
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

    private enum TreeKind {
        OAK,
        BIRCH,
        SPRUCE,
        DARK_OAK;

        private static TreeKind from(Material log) {
            return switch (log) {
                case OAK_LOG -> OAK;
                case BIRCH_LOG -> BIRCH;
                case SPRUCE_LOG -> SPRUCE;
                case DARK_OAK_LOG -> DARK_OAK;
                default -> throw new IllegalArgumentException("Unsupported forest log: " + log);
            };
        }

        private int adjustHeight(int height, int minimum, int maximum) {
            return switch (this) {
                case OAK -> clamp(height, minimum, Math.min(maximum, minimum + 10));
                case BIRCH -> clamp(height, minimum, Math.min(maximum, minimum + 7));
                case SPRUCE -> clamp(height + 6, minimum, maximum);
                case DARK_OAK -> clamp(height + 3, minimum, maximum);
            };
        }

        private static int clamp(int value, int minimum, int maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }

        private int trunkWidth(long hash) {
            return switch (this) {
                case OAK -> Math.floorMod(hash >>> 45, 5L) == 0L ? 2 : 1;
                case BIRCH -> 1;
                case SPRUCE -> Math.floorMod(hash >>> 45, 4L) == 0L ? 2 : 1;
                case DARK_OAK -> 2;
            };
        }

        private int crownRadius(long hash) {
            int variation = (int) Math.floorMod(hash >>> 52, 3L);
            return switch (this) {
                case OAK -> 5 + variation;
                case BIRCH -> 3 + variation / 2;
                case SPRUCE -> 4 + variation;
                case DARK_OAK -> 7 + variation;
            };
        }
    }

    private record TreeSpec(
            TreeKind kind,
            int centerX,
            int centerZ,
            int baseY,
            int height,
            int trunkWidth,
            int crownRadius
    ) {
        private int topY() {
            return baseY + height;
        }
    }
}
