package fr.valoriatycoon.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class TopCommandMetadataTest {

    @Test
    void exposesCachedLeaderboardsAsDedicatedRootCommand() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("plugin.yml")
        );
        var yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );

        var top = yaml.getConfigurationSection("commands.top");
        assertNotNull(top);
        assertEquals("/top", top.getString("usage"));
        assertEquals("tycoon.leaderboards", top.getString("permission"));
        assertTrue(top.getStringList("aliases").contains("classement"));
        assertTrue(yaml.getBoolean("permissions.tycoon.leaderboards.default"));
    }
}
