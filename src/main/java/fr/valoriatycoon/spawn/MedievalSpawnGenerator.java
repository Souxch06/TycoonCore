package fr.valoriatycoon.spawn;

import java.util.List;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

/** Deterministic floating medieval hub with plaza, castle, market, landscaping and farm arches. */
public final class MedievalSpawnGenerator extends ChunkGenerator {
    private final SpawnSettings settings;

    public MedievalSpawnGenerator(SpawnSettings settings) {
        this.settings = settings;
    }

    @Override
    public void generateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData data
    ) {
        Writer writer = new Writer(chunkX, chunkZ, data);
        generateIsland(writer);
        generateFountain(writer);
        generateCastle(writer);
        generateGuildHalls(writer);
        generateMarkets(writer);
        generatePortals(writer);
        generateLamps(writer);
        generateTrees(writer);
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

    private void generateIsland(Writer writer) {
        int groundY = settings.groundY();
        int radius = settings.islandRadius();
        for (int localX = 0; localX < 16; localX++) {
            int x = writer.worldX(localX);
            for (int localZ = 0; localZ < 16; localZ++) {
                int z = writer.worldZ(localZ);
                double distance = Math.hypot(x, z);
                if (distance > radius) {
                    continue;
                }
                int depth = Math.max(5, 14 - (int) (distance / radius * 8.0));
                for (int y = groundY - depth; y <= groundY - 4; y++) {
                    writer.set(x, y, z, y == groundY - depth ? Material.DEEPSLATE : Material.STONE);
                }
                writer.set(x, groundY - 3, z, Material.DIRT);
                writer.set(x, groundY - 2, z, Material.DIRT);
                writer.set(x, groundY - 1, z, Material.DIRT);
                writer.set(x, groundY, z, topMaterial(x, z, distance));
            }
        }
    }

    private Material topMaterial(int x, int z, double distance) {
        if (distance <= 50.0) {
            return Math.floorMod(x + z, 7) == 0
                    ? Material.CHISELED_STONE_BRICKS
                    : Math.floorMod(x - z, 3) == 0
                    ? Material.POLISHED_ANDESITE
                    : Material.STONE_BRICKS;
        }
        if (Math.abs(x) <= 4 && z >= -112 && z <= 54) {
            return Material.STONE_BRICKS;
        }
        for (SpawnSettings.PortalDefinition portal : settings.portals()) {
            if (distanceToSegment(x, z, 0, 0, portal.centerX(), portal.centerZ()) <= 3.5) {
                return Material.STONE_BRICKS;
            }
        }
        return Math.floorMod(x * 31 + z * 17, 23) == 0
                ? Material.MOSS_BLOCK
                : Material.GRASS_BLOCK;
    }

    private void generateFountain(Writer writer) {
        int y = settings.groundY();
        for (int x = -9; x <= 9; x++) {
            for (int z = -9; z <= 9; z++) {
                double distance = Math.hypot(x, z);
                if (distance >= 7.0 && distance <= 9.0) {
                    writer.set(x, y + 1, z, Material.QUARTZ_BLOCK);
                } else if (distance < 7.0) {
                    writer.set(x, y, z, Material.QUARTZ_BLOCK);
                    writer.set(x, y + 1, z, Material.WATER);
                }
            }
        }
        writer.fill(-1, y + 1, -1, 1, y + 7, 1, Material.CUT_COPPER);
        writer.fill(-2, y + 5, -2, 2, y + 5, 2, Material.QUARTZ_BLOCK);
        writer.set(0, y + 8, 0, Material.SEA_LANTERN);
        writer.set(-3, y + 6, 0, Material.WATER);
        writer.set(3, y + 6, 0, Material.WATER);
        writer.set(0, y + 6, -3, Material.WATER);
        writer.set(0, y + 6, 3, Material.WATER);
    }

    private void generateCastle(Writer writer) {
        int y = settings.groundY();
        writer.fill(-31, y + 1, -108, 31, y + 1, -67, Material.STONE_BRICKS);
        writer.fill(-31, y + 2, -108, -27, y + 13, -67, Material.STONE_BRICKS);
        writer.fill(27, y + 2, -108, 31, y + 13, -67, Material.STONE_BRICKS);
        writer.fill(-31, y + 2, -108, 31, y + 13, -104, Material.STONE_BRICKS);
        writer.fill(-31, y + 2, -71, -5, y + 13, -67, Material.STONE_BRICKS);
        writer.fill(5, y + 2, -71, 31, y + 13, -67, Material.STONE_BRICKS);
        writer.fill(-4, y + 10, -71, 4, y + 13, -67, Material.STONE_BRICKS);
        writer.fill(-4, y + 2, -108, 4, y + 9, -104, Material.DARK_OAK_PLANKS);

        for (int towerX : List.of(-27, 27)) {
            for (int towerZ : List.of(-103, -72)) {
                buildTower(writer, towerX, towerZ, y);
            }
        }
        for (int x = -31; x <= 31; x += 4) {
            writer.fill(x, y + 14, -108, x + 1, y + 16, -105, Material.STONE_BRICKS);
            writer.fill(x, y + 14, -70, x + 1, y + 16, -67, Material.STONE_BRICKS);
        }
        writer.fill(-3, y + 2, -70, 3, y + 8, -70, Material.AIR);
        writer.fill(-3, y + 9, -70, 3, y + 9, -70, Material.GOLD_BLOCK);
        writer.set(-10, y + 9, -66, Material.RED_WOOL);
        writer.set(10, y + 9, -66, Material.BLUE_WOOL);
    }

    private void buildTower(Writer writer, int centerX, int centerZ, int y) {
        writer.fill(centerX - 5, y + 2, centerZ - 5, centerX + 5, y + 18, centerZ - 5, Material.DEEPSLATE_BRICKS);
        writer.fill(centerX - 5, y + 2, centerZ + 5, centerX + 5, y + 18, centerZ + 5, Material.DEEPSLATE_BRICKS);
        writer.fill(centerX - 5, y + 2, centerZ - 4, centerX - 5, y + 18, centerZ + 4, Material.DEEPSLATE_BRICKS);
        writer.fill(centerX + 5, y + 2, centerZ - 4, centerX + 5, y + 18, centerZ + 4, Material.DEEPSLATE_BRICKS);
        writer.fill(centerX - 5, y + 18, centerZ - 5, centerX + 5, y + 18, centerZ + 5, Material.STONE_BRICKS);
        for (int offset = -5; offset <= 5; offset += 4) {
            writer.fill(centerX + offset, y + 19, centerZ - 5, centerX + offset + 1, y + 21, centerZ - 4, Material.STONE_BRICKS);
            writer.fill(centerX + offset, y + 19, centerZ + 4, centerX + offset + 1, y + 21, centerZ + 5, Material.STONE_BRICKS);
        }
        writer.set(centerX, y + 11, centerZ - 5, Material.SEA_LANTERN);
    }

    private void generateGuildHalls(Writer writer) {
        int y = settings.groundY();
        buildGuildHall(writer, -72, -38, y, Material.RED_WOOL);
        buildGuildHall(writer, 72, -38, y, Material.BLUE_WOOL);
    }

    private void buildGuildHall(
            Writer writer,
            int centerX,
            int centerZ,
            int y,
            Material banner
    ) {
        writer.fill(centerX - 9, y + 1, centerZ - 7, centerX + 9, y + 1, centerZ + 7, Material.COBBLESTONE);
        writer.fill(centerX - 8, y + 2, centerZ - 6, centerX + 8, y + 8, centerZ - 6, Material.WHITE_TERRACOTTA);
        writer.fill(centerX - 8, y + 2, centerZ + 6, centerX + 8, y + 8, centerZ + 6, Material.WHITE_TERRACOTTA);
        writer.fill(centerX - 8, y + 2, centerZ - 5, centerX - 8, y + 8, centerZ + 5, Material.WHITE_TERRACOTTA);
        writer.fill(centerX + 8, y + 2, centerZ - 5, centerX + 8, y + 8, centerZ + 5, Material.WHITE_TERRACOTTA);
        for (int x : List.of(centerX - 8, centerX, centerX + 8)) {
            writer.fill(x, y + 2, centerZ - 6, x, y + 9, centerZ - 6, Material.DARK_OAK_LOG);
            writer.fill(x, y + 2, centerZ + 6, x, y + 9, centerZ + 6, Material.DARK_OAK_LOG);
        }
        writer.fill(centerX - 8, y + 5, centerZ - 6, centerX + 8, y + 5, centerZ - 6, Material.DARK_OAK_LOG);
        writer.fill(centerX - 8, y + 5, centerZ + 6, centerX + 8, y + 5, centerZ + 6, Material.DARK_OAK_LOG);
        writer.fill(centerX - 2, y + 2, centerZ - 6, centerX + 2, y + 5, centerZ - 6, Material.AIR);
        for (int layer = 0; layer <= 6; layer++) {
            writer.fill(
                    centerX - 10 + layer,
                    y + 9 + layer,
                    centerZ - 8,
                    centerX + 10 - layer,
                    y + 9 + layer,
                    centerZ + 8,
                    Material.DEEPSLATE_TILES
            );
        }
        writer.fill(centerX + 5, y + 10, centerZ + 2, centerX + 7, y + 17, centerZ + 4, Material.BRICKS);
        writer.fill(centerX - 1, y + 7, centerZ - 7, centerX + 1, y + 9, centerZ - 7, banner);
        writer.set(centerX - 5, y + 4, centerZ - 7, Material.SEA_LANTERN);
        writer.set(centerX + 5, y + 4, centerZ - 7, Material.SEA_LANTERN);
    }

    private void generateMarkets(Writer writer) {
        int y = settings.groundY();
        buildStall(writer, -39, 30, y, Material.RED_WOOL);
        buildStall(writer, 39, 30, y, Material.BLUE_WOOL);
        buildStall(writer, -39, -28, y, Material.YELLOW_WOOL);
        buildStall(writer, 39, -28, y, Material.GREEN_WOOL);
        buildStall(writer, -62, 47, y, Material.ORANGE_WOOL);
        buildStall(writer, 62, 47, y, Material.PURPLE_WOOL);
    }

    private void buildStall(Writer writer, int centerX, int centerZ, int y, Material roof) {
        writer.fill(centerX - 5, y + 1, centerZ - 4, centerX + 5, y + 1, centerZ + 4, Material.SPRUCE_PLANKS);
        for (int x : List.of(centerX - 5, centerX + 5)) {
            for (int z : List.of(centerZ - 4, centerZ + 4)) {
                writer.fill(x, y + 2, z, x, y + 6, z, Material.STRIPPED_DARK_OAK_LOG);
            }
        }
        writer.fill(centerX - 6, y + 7, centerZ - 5, centerX + 6, y + 7, centerZ + 5, roof);
        writer.fill(centerX - 4, y + 2, centerZ - 3, centerX + 4, y + 3, centerZ - 3, Material.BARREL);
        writer.set(centerX, y + 6, centerZ, Material.LANTERN);
    }

    private void generatePortals(Writer writer) {
        int y = settings.groundY();
        for (SpawnSettings.PortalDefinition portal : settings.portals()) {
            if (portal.axis() == SpawnSettings.Axis.X) {
                writer.fill(portal.centerX() - 5, y + 1, portal.centerZ(), portal.centerX() - 3, y + 10, portal.centerZ(), portal.frameMaterial());
                writer.fill(portal.centerX() + 3, y + 1, portal.centerZ(), portal.centerX() + 5, y + 10, portal.centerZ(), portal.frameMaterial());
                writer.fill(portal.centerX() - 5, y + 9, portal.centerZ(), portal.centerX() + 5, y + 11, portal.centerZ(), portal.frameMaterial());
                writer.fill(portal.centerX() - 2, y + 2, portal.centerZ(), portal.centerX() + 2, y + 2, portal.centerZ(), portal.accentMaterial());
            } else {
                writer.fill(portal.centerX(), y + 1, portal.centerZ() - 5, portal.centerX(), y + 10, portal.centerZ() - 3, portal.frameMaterial());
                writer.fill(portal.centerX(), y + 1, portal.centerZ() + 3, portal.centerX(), y + 10, portal.centerZ() + 5, portal.frameMaterial());
                writer.fill(portal.centerX(), y + 9, portal.centerZ() - 5, portal.centerX(), y + 11, portal.centerZ() + 5, portal.frameMaterial());
                writer.fill(portal.centerX(), y + 2, portal.centerZ() - 2, portal.centerX(), y + 2, portal.centerZ() + 2, portal.accentMaterial());
            }
        }
    }

    private void generateLamps(Writer writer) {
        int y = settings.groundY();
        int[][] positions = {
                {-14, -14}, {14, -14}, {-14, 14}, {14, 14},
                {-48, 0}, {48, 0}, {0, 48}, {0, -48},
                {-82, 48}, {82, 48}, {-54, 84}, {54, 84}
        };
        for (int[] position : positions) {
            writer.fill(position[0], y + 1, position[1], position[0], y + 5, position[1], Material.DARK_OAK_FENCE);
            writer.set(position[0], y + 6, position[1], Material.LANTERN);
        }
    }

    private void generateTrees(Writer writer) {
        int y = settings.groundY();
        int[][] positions = {
                {-92, -25}, {92, -25}, {-92, 72}, {92, 72},
                {-72, -72}, {72, -72}, {-18, 98}, {18, 98},
                {-105, 20}, {105, 20}
        };
        for (int[] position : positions) {
            writer.fill(position[0], y + 1, position[1], position[0], y + 6, position[1], Material.OAK_LOG);
            for (int x = position[0] - 3; x <= position[0] + 3; x++) {
                for (int z = position[1] - 3; z <= position[1] + 3; z++) {
                    if (Math.abs(x - position[0]) + Math.abs(z - position[1]) <= 4) {
                        writer.set(x, y + 6, z, Material.OAK_LEAVES);
                        writer.set(x, y + 7, z, Material.OAK_LEAVES);
                    }
                }
            }
            writer.set(position[0], y + 9, position[1], Material.OAK_LEAVES);
        }
    }

    private double distanceToSegment(
            double px,
            double pz,
            double ax,
            double az,
            double bx,
            double bz
    ) {
        double dx = bx - ax;
        double dz = bz - az;
        double denominator = dx * dx + dz * dz;
        if (denominator == 0.0) {
            return Math.hypot(px - ax, pz - az);
        }
        double projection = Math.max(0.0, Math.min(1.0,
                ((px - ax) * dx + (pz - az) * dz) / denominator));
        return Math.hypot(px - (ax + projection * dx), pz - (az + projection * dz));
    }

    private static final class Writer {
        private final int minimumX;
        private final int minimumZ;
        private final ChunkData data;

        private Writer(int chunkX, int chunkZ, ChunkData data) {
            this.minimumX = chunkX * 16;
            this.minimumZ = chunkZ * 16;
            this.data = data;
        }

        private int worldX(int localX) {
            return minimumX + localX;
        }

        private int worldZ(int localZ) {
            return minimumZ + localZ;
        }

        private void set(int x, int y, int z, Material material) {
            if (x < minimumX || x >= minimumX + 16 || z < minimumZ || z >= minimumZ + 16) {
                return;
            }
            if (y >= data.getMinHeight() && y < data.getMaxHeight()) {
                data.setBlock(x - minimumX, y, z - minimumZ, material);
            }
        }

        private void fill(
                int fromX,
                int fromY,
                int fromZ,
                int toX,
                int toY,
                int toZ,
                Material material
        ) {
            int startX = Math.max(fromX, minimumX);
            int endX = Math.min(toX, minimumX + 15);
            int startZ = Math.max(fromZ, minimumZ);
            int endZ = Math.min(toZ, minimumZ + 15);
            int startY = Math.max(fromY, data.getMinHeight());
            int endY = Math.min(toY, data.getMaxHeight() - 1);
            if (startX > endX || startZ > endZ || startY > endY) {
                return;
            }
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    for (int z = startZ; z <= endZ; z++) {
                        data.setBlock(x - minimumX, y, z - minimumZ, material);
                    }
                }
            }
        }
    }
}
