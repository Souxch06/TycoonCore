package fr.valoriatycoon.farm;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

/** Deterministic, stateless and parallel-safe generator for shared resource worlds. */
public final class GeneratedFarmChunkGenerator extends ChunkGenerator {
    private final FarmGenerationSettings settings;
    private final MineCavernGenerator mineCaverns;
    private final ForestIslandGenerator forestIslands;
    private final Map<Material, BlockData> matureCrops;
    private final BlockData hydratedFarmland;

    public GeneratedFarmChunkGenerator(FarmGenerationSettings settings) {
        this.settings = settings;
        this.mineCaverns = settings instanceof FarmGenerationSettings.Mine mine
                ? new MineCavernGenerator(mine)
                : null;
        this.forestIslands = settings instanceof FarmGenerationSettings.Forest forest
                ? new ForestIslandGenerator(forest)
                : null;
        this.matureCrops = createMatureCropData(settings);
        this.hydratedFarmland = createHydratedFarmland();
    }

    @Override
    public void generateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData
    ) {
        validateHeightRange(chunkData);
        switch (settings) {
            case FarmGenerationSettings.Mine ignored -> mineCaverns.generate(
                    chunkX,
                    chunkZ,
                    random,
                    chunkData
            );
            case FarmGenerationSettings.Fields fields -> generateFields(chunkX, chunkZ, chunkData, fields);
            case FarmGenerationSettings.Fishing fishing -> generateFishing(chunkX, chunkZ, chunkData, fishing);
            case FarmGenerationSettings.Forest ignored -> forestIslands.generate(
                    chunkX,
                    chunkZ,
                    chunkData
            );
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    private void generateFields(
            int chunkX,
            int chunkZ,
            ChunkData data,
            FarmGenerationSettings.Fields fields
    ) {
        for (int x = 0; x < 16; x++) {
            int worldX = chunkX * 16 + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = chunkZ * 16 + z;
                FarmZoneDefinition zone = zoneAt(fields, worldX, worldZ);
                FarmBridgeDefinition bridge = zone == null ? bridgeAt(fields, worldX, worldZ) : null;
                if (zone == null && bridge == null) {
                    continue;
                }
                if (zone == null) {
                    generateSurfaceBridgeColumn(
                            data, x, z, worldX, worldZ, fields.soilY(), bridge,
                            Material.OAK_PLANKS, Material.OAK_FENCE
                    );
                    continue;
                }
                Material crop = ((FarmZoneDefinition.FieldsResource) zone.resource()).crop();
                int localX = worldX - zone.minimumX();
                int islandDepth = Math.max(5, 13 - (int) (zone.normalizedDistance(worldX, worldZ) * 7));
                for (int y = fields.soilY() - islandDepth; y < fields.soilY() - 3; y++) {
                    data.setBlock(x, y, z, Material.STONE);
                }
                data.setRegion(
                        x,
                        fields.soilY() - 3,
                        z,
                        x + 1,
                        fields.soilY(),
                        z + 1,
                        Material.DIRT
                );
                boolean mainAvenue = Math.abs(worldZ - zone.centerZ()) <= 4;
                boolean arrivalPlaza = Math.hypot(
                        worldX - zone.centerX(),
                        worldZ - zone.centerZ()
                ) <= 12.0;
                boolean perimeterRoad = zone.normalizedDistance(worldX, worldZ) >= 0.94;
                boolean path = mainAvenue || arrivalPlaza || perimeterRoad
                        || Math.floorMod(localX, fields.pathSpacing()) == 0;
                if (path) {
                    data.setBlock(x, fields.soilY(), z, fields.pathMaterial());
                } else {
                    data.setBlock(x, fields.soilY(), z, hydratedFarmland);
                    data.setBlock(x, fields.soilY() + 1, z, matureCrops.get(crop));
                }
                generateFieldDecoration(data, x, z, worldX, worldZ, fields.soilY(), zone);
            }
        }
    }

    private void generateFishing(
            int chunkX,
            int chunkZ,
            ChunkData data,
            FarmGenerationSettings.Fishing fishing
    ) {
        for (int x = 0; x < 16; x++) {
            int worldX = chunkX * 16 + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = chunkZ * 16 + z;
                data.setBlock(x, fishing.bottomY(), z, Material.BEDROCK);
                data.setRegion(
                        x,
                        fishing.bottomY() + 1,
                        z,
                        x + 1,
                        fishing.seabedY() + 1,
                        z + 1,
                        fishing.seabedMaterial()
                );
                data.setRegion(
                        x,
                        fishing.seabedY() + 1,
                        z,
                        x + 1,
                        fishing.waterY() + 1,
                        z + 1,
                        Material.WATER
                );
                if (insideSquare(worldX, worldZ, 0, 0, fishing.platformRadius())) {
                    data.setBlock(x, fishing.waterY() + 1, z, fishing.platformMaterial());
                }
            }
        }
    }

    private void generateSurfaceBridgeColumn(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            int surfaceY,
            FarmBridgeDefinition bridge,
            Material deck,
            Material railing
    ) {
        int distanceZ = Math.abs(worldZ - bridge.centerZ());
        if (distanceZ <= bridge.width() / 2) {
            data.setBlock(localX, surfaceY, localZ, deck);
        }
        if (distanceZ == bridge.width() / 2 + 1) {
            data.setBlock(localX, surfaceY + 1, localZ, railing);
        }
        if (distanceZ <= 1 && Math.floorMod(worldX - bridge.minimumX(), 32) == 0) {
            for (int y = surfaceY - 4; y < surfaceY; y++) {
                data.setBlock(localX, y, localZ, Material.STONE_BRICKS);
            }
            data.setBlock(localX, surfaceY + 1, localZ, Material.LANTERN);
        }
        generateBridgeGate(data, localX, localZ, worldX, worldZ, surfaceY, bridge);
    }

    private void generateBridgeGate(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            int surfaceY,
            FarmBridgeDefinition bridge
    ) {
        int distanceZ = Math.abs(worldZ - bridge.centerZ());
        boolean gateColumn = worldX == bridge.minimumX() + 1
                || worldX == bridge.maximumX() - 1;
        if (!gateColumn) {
            return;
        }
        if (distanceZ >= bridge.width() / 2 && distanceZ <= bridge.width() / 2 + 1) {
            for (int y = surfaceY + 1; y <= surfaceY + 6; y++) {
                data.setBlock(localX, y, localZ, Material.STONE_BRICKS);
            }
        }
        if (distanceZ <= bridge.width() / 2 + 1) {
            data.setBlock(localX, surfaceY + 6, localZ, Material.STONE_BRICKS);
        }
        if (distanceZ == bridge.width() / 2) {
            data.setBlock(localX, surfaceY + 5, localZ, Material.REDSTONE_LAMP);
        }
    }

    private void generateFieldDecoration(
            ChunkData data,
            int localX,
            int localZ,
            int worldX,
            int worldZ,
            int surfaceY,
            FarmZoneDefinition zone
    ) {
        int relativeX = worldX - zone.centerX();
        int relativeZ = worldZ - zone.centerZ();
        if (Math.abs(relativeZ) == 7 && Math.floorMod(relativeX, 64) == 0) {
            data.setBlock(localX, surfaceY + 1, localZ, Material.OAK_FENCE);
            data.setBlock(localX, surfaceY + 2, localZ, Material.OAK_FENCE);
            data.setBlock(localX, surfaceY + 3, localZ, Material.LANTERN);
        }
        if ((Math.abs(relativeX) == 18 || Math.abs(relativeX) == 22)
                && (Math.abs(relativeZ) == 18 || Math.abs(relativeZ) == 22)) {
            data.setBlock(localX, surfaceY + 1, localZ, Material.HAY_BLOCK);
            if (Math.abs(relativeX) == 18 && Math.abs(relativeZ) == 18) {
                data.setBlock(localX, surfaceY + 2, localZ, Material.HAY_BLOCK);
            }
        }
        generateFieldBarn(data, localX, localZ, relativeX, relativeZ, surfaceY);
        generateFieldWindmill(data, localX, localZ, relativeX, relativeZ, surfaceY);
    }

    private void generateFieldBarn(
            ChunkData data,
            int localX,
            int localZ,
            int relativeX,
            int relativeZ,
            int surfaceY
    ) {
        int dx = relativeX + 180;
        int dz = relativeZ + 180;
        if (Math.abs(dx) > 12 || Math.abs(dz) > 9) {
            return;
        }
        data.setBlock(localX, surfaceY, localZ, Material.SPRUCE_PLANKS);
        data.setBlock(localX, surfaceY + 1, localZ, Material.AIR);
        boolean corner = Math.abs(dx) == 12 && Math.abs(dz) == 9;
        boolean wall = Math.abs(dx) == 12 || Math.abs(dz) == 9;
        if (wall) {
            Material material = corner ? Material.STRIPPED_OAK_LOG : Material.RED_TERRACOTTA;
            for (int y = surfaceY + 1; y <= surfaceY + 6; y++) {
                data.setBlock(localX, y, localZ, material);
            }
        }
        int roofY = surfaceY + 7 + Math.max(0, 3 - Math.abs(dx) / 4);
        data.setBlock(localX, roofY, localZ, Material.DARK_OAK_PLANKS);
        if (dz == -9 && Math.abs(dx) <= 2) {
            for (int y = surfaceY + 1; y <= surfaceY + 4; y++) {
                data.setBlock(localX, y, localZ, Material.AIR);
            }
        }
    }

    private void generateFieldWindmill(
            ChunkData data,
            int localX,
            int localZ,
            int relativeX,
            int relativeZ,
            int surfaceY
    ) {
        int dx = relativeX - 180;
        int dz = relativeZ - 180;
        double distance = Math.hypot(dx, dz);
        if (distance <= 8.0) {
            data.setBlock(localX, surfaceY, localZ, Material.COBBLESTONE);
            data.setBlock(localX, surfaceY + 1, localZ, Material.AIR);
            if (distance >= 6.0) {
                for (int y = surfaceY + 1; y <= surfaceY + 12; y++) {
                    data.setBlock(localX, y, localZ, Material.COBBLESTONE);
                }
            }
            if (distance <= 7.0) {
                data.setBlock(localX, surfaceY + 13, localZ, Material.SPRUCE_PLANKS);
            }
        }
        if (dz == -9 && Math.abs(dx) <= 15) {
            data.setBlock(localX, surfaceY + 12, localZ, Material.SPRUCE_PLANKS);
        }
        if (dx == 0 && dz == -9) {
            for (int y = surfaceY + 2; y <= surfaceY + 24; y++) {
                data.setBlock(localX, y, localZ, Material.SPRUCE_PLANKS);
            }
            data.setBlock(localX, surfaceY + 13, localZ, Material.GOLD_BLOCK);
        }
    }

    private FarmBridgeDefinition bridgeAt(FarmGenerationSettings generation, int x, int z) {
        for (int index = 0; index < generation.zones().size() - 1; index++) {
            FarmZoneDefinition from = generation.zones().get(index);
            FarmZoneDefinition to = generation.zones().get(index + 1);
            int minimumX = from.maximumX() + 1;
            int maximumX = to.minimumX() - 1;
            int centerZ = (from.centerZ() + to.centerZ()) / 2;
            int halfWidth = generation.bridgeWidth() / 2;
            if (x >= minimumX
                    && x <= maximumX
                    && Math.abs(z - centerZ) <= halfWidth + 1) {
                return new FarmBridgeDefinition(
                        from.index(),
                        to.index(),
                        minimumX,
                        maximumX,
                        centerZ,
                        generation.bridgeWidth(),
                        to.requiredRank()
                );
            }
        }
        return null;
    }

    private FarmZoneDefinition zoneAt(FarmGenerationSettings generation, int x, int z) {
        for (FarmZoneDefinition zone : generation.zones()) {
            if (zone.contains(x, z)) {
                return zone;
            }
        }
        return null;
    }

    private boolean insideSquare(
            int worldX,
            int worldZ,
            int centerX,
            int centerZ,
            int radius
    ) {
        return Math.abs(worldX - centerX) <= radius && Math.abs(worldZ - centerZ) <= radius;
    }

    private void validateHeightRange(ChunkData data) {
        if (settings.spawnY() < data.getMinHeight() || settings.spawnY() >= data.getMaxHeight()) {
            throw new IllegalStateException(
                    "Configured farm height " + settings.spawnY()
                            + " is outside world range " + data.getMinHeight() + ".." + data.getMaxHeight()
            );
        }
        if (settings instanceof FarmGenerationSettings.Mine mine
                && (mine.bottomY() < data.getMinHeight()
                || mine.ceilingY() + mine.ceilingVariation() + 6 >= data.getMaxHeight())) {
            throw new IllegalStateException(
                    "Configured mine cavern exceeds world height range "
                            + data.getMinHeight() + ".." + data.getMaxHeight()
            );
        }
        if (settings instanceof FarmGenerationSettings.Forest forest
                && (forest.bottomY() < data.getMinHeight()
                || forest.groundY()
                + forest.terrainVariation()
                + forest.maximumTrunkHeight()
                + 8 >= data.getMaxHeight())) {
            throw new IllegalStateException(
                    "Configured forest canopy exceeds world height range "
                            + data.getMinHeight() + ".." + data.getMaxHeight()
            );
        }
    }

    private BlockData createHydratedFarmland() {
        BlockData data = Bukkit.createBlockData(Material.FARMLAND);
        if (!(data instanceof Farmland farmland)) {
            throw new IllegalStateException("Paper did not expose Farmland block data");
        }
        farmland.setMoisture(farmland.getMaximumMoisture());
        return farmland;
    }

    private Map<Material, BlockData> createMatureCropData(FarmGenerationSettings generation) {
        Map<Material, BlockData> result = new EnumMap<>(Material.class);
        for (FarmZoneDefinition zone : generation.zones()) {
            if (!(zone.resource() instanceof FarmZoneDefinition.FieldsResource fields)) {
                continue;
            }
            Material material = fields.crop();
            BlockData data = Bukkit.createBlockData(material);
            if (!(data instanceof Ageable ageable)) {
                throw new IllegalArgumentException("Farm crop is not ageable: " + material);
            }
            ageable.setAge(ageable.getMaximumAge());
            result.put(material, ageable);
        }
        return Map.copyOf(result);
    }
}
