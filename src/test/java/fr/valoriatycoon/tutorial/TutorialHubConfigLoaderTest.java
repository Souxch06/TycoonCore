package fr.valoriatycoon.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class TutorialHubConfigLoaderTest {

    @Test
    void loadsTheTwelvePanelCastleAcademy() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("tutorial-hub.yml")
        );
        TutorialHubSettings settings = TutorialHubConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );

        assertTrue(settings.enabled());
        assertEquals("valoria_spawn", settings.worldName());
        assertEquals(100, settings.refreshIntervalTicks());
        assertEquals(12, settings.panels().size());
        assertEquals(-101.0, settings.panels().get("welcome").offsetZ());
        assertEquals(-24.0, settings.panels().get("multitool").offsetX());
        assertEquals(24.0, settings.panels().get("ranks").offsetX());
        assertTrue(settings.panels().get("commands").lines().stream()
                .anyMatch(line -> line.contains("/warp caisse")));
        assertTrue(settings.panels().get("first-steps").lines().stream()
                .anyMatch(line -> line.contains("Citoyen")));
    }
}
