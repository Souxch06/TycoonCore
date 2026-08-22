package fr.valoriatycoon.crates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CrateConfigLoaderTest {

    @Test
    void loadsEightKeysAndEnablesTheFinalRewardSystem() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("crates.yml")
        );
        CrateSettings settings = CrateConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );

        assertTrue(settings.openingEnabled());
        assertEquals(Material.TRIPWIRE_HOOK, settings.keyMaterial());
        assertEquals(8, settings.keys().size());
        assertTrue(settings.key(CrateType.VALORIA).paid());
        assertEquals(80, settings.toolRarityWeights().get(CrateType.COMMON));
        assertEquals(4, settings.toolRarityWeights().size());
    }

    @Test
    void upgradesThePreRewardFalseFlagInMemory() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("crates.yml")
        );
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        config.set("reward-system-version", null);
        config.set("opening-enabled", false);

        assertTrue(CrateConfigLoader.load(config).openingEnabled());
    }
}
