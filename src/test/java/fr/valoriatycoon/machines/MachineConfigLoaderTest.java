package fr.valoriatycoon.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MachineConfigLoaderTest {
    @Test
    void loadsFourUpgradeableResourceGeneratorsWithoutEnergy() {
        var stream = getClass().getClassLoader().getResourceAsStream("machines.yml");
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        MachineSettings settings = MachineConfigLoader.load(yaml);
        assertEquals(4, settings.machines().size());
        assertEquals(Material.RAW_IRON, settings.machine("miner").outputMaterial());
        assertEquals(Material.WHEAT, settings.machine("farmer").outputMaterial());
        assertEquals(10, settings.upgrades().speed().maximumLevel());
        assertEquals(new BigDecimal("0.5"), settings.upgrades().speed().minimumIntervalMultiplier());
        assertEquals(100, settings.maximumCyclesPerTick());
    }
}
