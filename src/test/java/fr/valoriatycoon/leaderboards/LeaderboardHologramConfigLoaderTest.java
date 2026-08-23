package fr.valoriatycoon.leaderboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LeaderboardHologramConfigLoaderTest {

    @Test
    void loadsFiveSpawnRelativeTextDisplaysWithoutChunkLoadingSettings() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("leaderboard-holograms.yml")
        );
        LeaderboardHologramSettings settings = LeaderboardHologramConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );

        assertTrue(settings.enabled());
        assertEquals("valoria_spawn", settings.worldName());
        assertEquals(100, settings.updateIntervalTicks());
        assertEquals(5, settings.topEntries());
        assertEquals(5, settings.positions().size());
        assertEquals(-18.0, settings.position(LeaderboardType.MONEY).offsetX());
        assertEquals(0.0, settings.position(LeaderboardType.RANK).offsetX());
        assertEquals(18.0, settings.position(LeaderboardType.PLAYTIME).offsetX());
    }
}
