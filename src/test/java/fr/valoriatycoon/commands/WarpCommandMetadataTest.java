package fr.valoriatycoon.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WarpCommandMetadataTest {

    @Test
    void exposesWarpAsPrefixCommand() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("plugin.yml")
        );
        var yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );

        var warp = yaml.getConfigurationSection("commands.warp");
        assertNotNull(warp);
        assertEquals("/warp [destination]", warp.getString("usage"));
        assertEquals("tycoon.warp", warp.getString("permission"));
        assertTrue(warp.getStringList("aliases").contains("warps"));
        assertTrue(yaml.getBoolean("permissions.tycoon.warp.default"));
    }
}
