package fr.valoriatycoon.farm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class FarmConfigLoaderTest {

    @Test
    void loadsFourRankedZonesAndCommonFishing() {
        FarmSettings settings = FarmConfigLoader.load(loadConfiguration());
        List<Integer> expectedRanks = List.of(0, 2, 5, 8);

        for (String farmId : List.of("mine", "fields", "forest")) {
            FarmDefinition farm = settings.farm(farmId).orElseThrow();
            assertEquals(4, farm.zones().size());
            int expectedZoneSize = switch (farmId) {
                case "fields" -> 5_120;
                default -> 2_048;
            };
            FarmZoneDefinition.Shape expectedShape = farmId.equals("fields")
                    ? FarmZoneDefinition.Shape.CIRCLE
                    : FarmZoneDefinition.Shape.SQUARE;
            assertTrue(farm.zones().stream().allMatch(zone ->
                    zone.size() == expectedZoneSize && zone.shape() == expectedShape
            ));
            assertEquals(3, farm.bridges().size());
            int expectedBridgeLength = 1_024;
            int expectedBridgeWidth = switch (farmId) {
                case "mine" -> 15;
                case "forest" -> 13;
                default -> 9;
            };
            assertTrue(farm.bridges().stream().allMatch(bridge ->
                    bridge.width() == expectedBridgeWidth
                            && bridge.maximumX() - bridge.minimumX() + 1 == expectedBridgeLength
            ));
            FarmZoneDefinition firstZone = farm.zone(1).orElseThrow();
            assertTrue(firstZone.contains(firstZone.centerX(), firstZone.centerZ()));
            assertFalse(firstZone.contains(firstZone.maximumX() + 1, firstZone.centerZ()));
            assertEquals(expectedRanks, farm.zones().stream()
                    .map(FarmZoneDefinition::requiredRank)
                    .toList());
            for (int left = 0; left < farm.zones().size(); left++) {
                for (int right = left + 1; right < farm.zones().size(); right++) {
                    FarmZoneDefinition a = farm.zones().get(left);
                    FarmZoneDefinition b = farm.zones().get(right);
                    assertFalse(a.contains(b.centerX(), b.centerZ()));
                    assertFalse(b.contains(a.centerX(), a.centerZ()));
                }
            }
        }
        assertTrue(settings.farm("fishing").orElseThrow().zones().isEmpty());
        assertEquals(1.45, settings.rankBarrier().horizontalKnockback(), 0.000_001);
        assertEquals(0.42, settings.rankBarrier().verticalKnockback(), 0.000_001);
    }

    @Test
    void appliesRankAccessWithoutStorageIo() {
        FarmDefinition mine = FarmConfigLoader.load(loadConfiguration())
                .farm("mine").orElseThrow();
        FarmZoneAccessPolicy chevalier = ignored -> 5;

        assertTrue(chevalier.canAccess(
                java.util.UUID.randomUUID(),
                mine.zone(3).orElseThrow()
        ));
        assertFalse(chevalier.canAccess(
                java.util.UUID.randomUUID(),
                mine.zone(4).orElseThrow()
        ));
        assertEquals(List.of(2, 5, 8), mine.bridges().stream()
                .map(FarmBridgeDefinition::requiredRank)
                .toList());
        for (FarmBridgeDefinition bridge : mine.bridges()) {
            assertEquals(bridge.requiredRank(), mine.requiredRankAt(
                    bridge.minimumX(),
                    bridge.centerZ()
            ).orElseThrow());
            assertTrue(mine.requiredRankAt(
                    bridge.minimumX(),
                    bridge.centerZ() + bridge.width() / 2 + 1
            ).isEmpty());
        }
    }

    @Test
    void mapsEachZoneToItsExpectedResource() {
        FarmSettings settings = FarmConfigLoader.load(loadConfiguration());
        FarmDefinition mine = settings.farm("mine").orElseThrow();
        FarmDefinition fields = settings.farm("fields").orElseThrow();
        FarmDefinition forest = settings.farm("forest").orElseThrow();

        var coal = assertInstanceOf(
                FarmZoneDefinition.MineResource.class,
                mine.zone(1).orElseThrow().resource()
        );
        assertEquals(List.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE), coal.ores().stream()
                .map(FarmGenerationSettings.OreRule::material)
                .toList());
        var mineGeneration = assertInstanceOf(
                FarmGenerationSettings.Mine.class,
                mine.generation()
        );
        assertEquals(-48, mineGeneration.bottomY());
        assertEquals(12, mineGeneration.quarryFloorY());
        assertEquals(18, mineGeneration.floorVariation());
        assertEquals(48, mineGeneration.bridgeY());
        assertEquals(160, mineGeneration.ceilingY());
        assertEquals(24, mineGeneration.ceilingVariation());
        assertEquals(48, mineGeneration.wallThickness());
        assertEquals(15, mineGeneration.bridgeWidth());
        assertTrue(mine.breakableBlocks().containsAll(Set.of(
                Material.STONE,
                Material.DEEPSLATE,
                Material.ANDESITE,
                Material.GRANITE,
                Material.DIORITE,
                Material.TUFF,
                Material.CALCITE,
                Material.DRIPSTONE_BLOCK
        )));
        assertTrue(mine.zones().stream().allMatch(zone ->
                ((FarmZoneDefinition.MineResource) zone.resource()).ores().stream()
                        .mapToDouble(FarmGenerationSettings.OreRule::chance)
                        .sum() <= 0.080_001
        ));
        assertEquals(Material.CARROTS, assertInstanceOf(
                FarmZoneDefinition.FieldsResource.class,
                fields.zone(2).orElseThrow().resource()
        ).crop());
        var fieldGeneration = assertInstanceOf(
                FarmGenerationSettings.Fields.class,
                fields.generation()
        );
        assertEquals(256, fieldGeneration.pathSpacing());
        assertEquals(Material.DARK_OAK_LOG, assertInstanceOf(
                FarmZoneDefinition.ForestResource.class,
                forest.zone(4).orElseThrow().resource()
        ).log());
        var forestGeneration = assertInstanceOf(
                FarmGenerationSettings.Forest.class,
                forest.generation()
        );
        assertEquals(72, forestGeneration.groundY());
        assertEquals(12, forestGeneration.terrainVariation());
        assertEquals(28, forestGeneration.islandDepth());
        assertEquals(30, forestGeneration.treeSpacing());
        assertEquals(10, forestGeneration.minimumTrunkHeight());
        assertEquals(28, forestGeneration.maximumTrunkHeight());
        assertEquals(256, forestGeneration.pathSpacing());
        assertEquals(24, forestGeneration.platformRadius());
        assertEquals(13, forestGeneration.bridgeWidth());
    }

    private YamlConfiguration loadConfiguration() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("farms.yml")
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }
}
