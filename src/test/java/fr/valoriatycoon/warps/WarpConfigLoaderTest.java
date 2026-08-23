package fr.valoriatycoon.warps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WarpConfigLoaderTest {

    @Test
    void loadsTutorialAndCrateWarpsWithFrenchAliases() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("warps.yml")
        );
        WarpSettings settings = WarpConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );

        assertEquals(3, settings.cooldownSeconds());
        assertEquals(2, settings.warps().size());
        WarpDefinition tutorial = settings.resolve("tuto");
        assertNotNull(tutorial);
        assertEquals(tutorial, settings.resolve("tutoriel"));
        assertEquals(tutorial, settings.resolve("guide"));
        assertEquals(tutorial, settings.resolve("aide"));
        assertEquals(Material.WRITABLE_BOOK, tutorial.icon());
        assertEquals("ui/warp/tutorial", tutorial.itemModel());
        assertEquals(-111.0, tutorial.z());
        WarpDefinition crates = settings.resolve("caisse");
        assertNotNull(crates);
        assertEquals(crates, settings.resolve("caisses"));
        assertEquals(crates, settings.resolve("crates"));
        assertEquals("valoria_spawn", crates.worldName());
        assertTrue(crates.relativeToSpawn());
        assertEquals(Material.ENDER_CHEST, crates.icon());
        assertEquals("ui/warp/crates", crates.itemModel());
        assertEquals(39.0, crates.x());
        assertEquals(4.0, crates.z());
    }

    @Test
    void addsTutorialToPreviousWarpFilesWithoutOverwritingCrates() {
        String legacy = """
                teleport-cooldown-seconds: 3
                menu:
                  size: 27
                  title: '<dark_gray>Warps</dark_gray>'
                  filler: BLACK_STAINED_GLASS_PANE
                warps:
                  crates:
                    aliases: [caisse, caisses, crate]
                    world: valoria_spawn
                    relative-to-spawn: true
                    x: 39.0
                    y: 0.0
                    z: 4.0
                    yaw: 180.0
                    pitch: 0.0
                    slot: 13
                    icon: ENDER_CHEST
                    item-model: ui/warp/crates
                    name: '<light_purple>Caisses</light_purple>'
                    lore: ['<gray>Marché</gray>']
                """;

        WarpSettings settings = WarpConfigLoader.load(
                YamlConfiguration.loadConfiguration(new StringReader(legacy))
        );

        assertEquals(2, settings.warps().size());
        assertNotNull(settings.resolve("caisse"));
        assertNotNull(settings.resolve("tuto"));
        assertEquals(11, settings.resolve("tuto").menuSlot());
    }
}
