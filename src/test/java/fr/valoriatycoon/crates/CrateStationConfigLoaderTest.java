package fr.valoriatycoon.crates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CrateStationConfigLoaderTest {

    @Test
    void loadsNineModeledMarketCratesIncludingValoriaAndPets() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("crate-stations.yml")
        );
        CrateStationSettings settings = CrateStationConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );

        assertTrue(settings.enabled());
        assertTrue(settings.effectsEnabled());
        assertEquals(4, settings.effectIntervalTicks());
        assertEquals(40.0, settings.effectViewDistance());
        assertEquals("valoria_spawn", settings.worldName());
        assertEquals(9, settings.stations().size());
        assertEquals(
                "item/crate/valoria",
                settings.station(CrateStationType.VALORIA).itemModel()
        );
        assertTrue(CrateStationType.PETS.pets());
        assertEquals(39.0, settings.station(CrateStationType.VALORIA).offsetX());
        assertEquals(8.0, settings.station(CrateStationType.VALORIA).offsetZ());
        assertEquals(0x48DD69, settings.station(CrateStationType.COMMON).effect().primaryRgb());
        assertEquals(0x3D8BFF, settings.station(CrateStationType.RARE).effect().primaryRgb());
        assertEquals(0xFF7818, settings.station(CrateStationType.EPIC).effect().primaryRgb());
        assertEquals(0xFFD32D, settings.station(CrateStationType.LEGENDARY).effect().primaryRgb());
        assertEquals(0xFF2E4B, settings.station(CrateStationType.VALORIA).effect().primaryRgb());
        assertEquals(8, settings.station(CrateStationType.VALORIA).effect().particleCount());
    }

    @Test
    void keepsPreviousStationFilesCompatibleWithDefaultEffects() {
        String legacy = """
                enabled: true
                world: valoria_spawn
                refresh-interval-ticks: 100
                interaction-width: 1.6
                interaction-height: 1.8
                label-offset-y: 1.65
                stations:
                  vote: { offset-x: 33.0, offset-y: 0.0, offset-z: 0.0, yaw: 180.0, item-model: 'item/crate/vote' }
                  quest: { offset-x: 39.0, offset-y: 0.0, offset-z: 0.0, yaw: 180.0, item-model: 'item/crate/quest' }
                  farm: { offset-x: 45.0, offset-y: 0.0, offset-z: 0.0, yaw: 180.0, item-model: 'item/crate/farm' }
                  common: { offset-x: 33.0, offset-y: 0.0, offset-z: 4.0, yaw: 180.0, item-model: 'item/crate/common' }
                  rare: { offset-x: 39.0, offset-y: 0.0, offset-z: 4.0, yaw: 180.0, item-model: 'item/crate/rare' }
                  epic: { offset-x: 45.0, offset-y: 0.0, offset-z: 4.0, yaw: 180.0, item-model: 'item/crate/epic' }
                  legendary: { offset-x: 33.0, offset-y: 0.0, offset-z: 8.0, yaw: 180.0, item-model: 'item/crate/legendary' }
                  valoria: { offset-x: 39.0, offset-y: 0.0, offset-z: 8.0, yaw: 180.0, item-model: 'item/crate/valoria' }
                  pets: { offset-x: 45.0, offset-y: 0.0, offset-z: 8.0, yaw: 180.0, item-model: 'item/crate/pets' }
                """;

        CrateStationSettings settings = CrateStationConfigLoader.load(
                YamlConfiguration.loadConfiguration(new StringReader(legacy))
        );

        assertTrue(settings.effectsEnabled());
        assertEquals(4, settings.effectIntervalTicks());
        assertEquals(0xFF2E4B, settings.station(CrateStationType.VALORIA).effect().primaryRgb());
        assertEquals(2, settings.station(CrateStationType.COMMON).effect().particleCount());
    }
}
