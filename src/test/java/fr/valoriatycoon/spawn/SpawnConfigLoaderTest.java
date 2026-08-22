package fr.valoriatycoon.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SpawnConfigLoaderTest {

    @Test
    void loadsProtectedMedievalHubAndFourFarmArches() {
        SpawnSettings settings = SpawnConfigLoader.load(loadConfiguration());

        assertEquals("valoria_spawn", settings.worldName());
        assertEquals(120, settings.islandRadius());
        assertEquals(140, settings.protectionRadius());
        assertEquals(4, settings.portals().size());
        assertTrue(settings.teleportOnFirstJoin());
        assertEquals(
                Set.of("mine", "fields", "forest", "fishing"),
                settings.portals().stream()
                        .map(SpawnSettings.PortalDefinition::farmId)
                        .collect(Collectors.toSet())
        );
        assertTrue(settings.portals().stream().allMatch(portal ->
                Math.hypot(portal.centerX(), portal.centerZ()) < settings.islandRadius()
        ));
    }

    private YamlConfiguration loadConfiguration() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("spawn.yml")
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }
}
