package fr.valoriatycoon.leaderboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LeaderboardConfigLoaderTest {

    @Test
    void loadsBoundedAsynchronousCacheAndAllCategories() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("leaderboards.yml")
        );
        LeaderboardSettings settings = LeaderboardConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );

        assertTrue(settings.enabled());
        assertEquals(1_200, settings.refreshIntervalTicks());
        assertEquals(100, settings.queryLimit());
        assertEquals(10, settings.displayLimit());
        assertEquals(54, settings.menu().size());
        assertEquals(5, settings.menu().categorySlots().size());
        assertEquals(15, settings.menu().slot(LeaderboardType.RANK));
    }
}
