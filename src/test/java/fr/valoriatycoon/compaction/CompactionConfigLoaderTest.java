package fr.valoriatycoon.compaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CompactionConfigLoaderTest {

    @Test
    void loadsThreeLevelsForMineFieldAndForestResourcesOnly() {
        CompactionSettings settings = CompactionConfigLoader.load(loadConfiguration());

        assertEquals(3, settings.maximumLevel());
        assertEquals(16, settings.resources().size());
        assertTrue(settings.resources().containsKey(Material.COAL));
        assertTrue(settings.resources().containsKey(Material.IRON_INGOT));
        assertFalse(settings.resources().containsKey(Material.RAW_IRON));
        assertTrue(settings.resources().containsKey(Material.WHEAT));
        assertTrue(settings.resources().containsKey(Material.OAK_LOG));
        assertEquals(Material.IRON_BLOCK, settings.resource(Material.IRON_INGOT).craftingMaterial());
        assertEquals(9, settings.resource(Material.IRON_INGOT).baseUnitsPerCraftingItem());
        assertTrue(settings.resource(Material.IRON_INGOT).mineralBlockBased());
        assertEquals(Material.WHEAT, settings.resource(Material.WHEAT).craftingMaterial());
        assertEquals(1, settings.resource(Material.WHEAT).baseUnitsPerCraftingItem());
        assertFalse(settings.resource(Material.WHEAT).mineralBlockBased());
        assertFalse(settings.resources().containsKey(Material.COD));
        assertFalse(settings.resources().containsKey(Material.SALMON));
    }

    @Test
    void rejectsFishingResources() {
        YamlConfiguration yaml = loadConfiguration();
        yaml.set("resources.COD", "Morue crue");

        assertThrows(IllegalArgumentException.class, () -> CompactionConfigLoader.load(yaml));
    }

    private YamlConfiguration loadConfiguration() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("compaction.yml")
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }
}
