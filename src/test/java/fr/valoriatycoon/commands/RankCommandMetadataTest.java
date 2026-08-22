package fr.valoriatycoon.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class RankCommandMetadataTest {

    @Test
    void exposesRankAsDedicatedRootCommand() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("plugin.yml")
        );
        var yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );

        ConfigurationSection rank = yaml.getConfigurationSection("commands.rank");
        assertNotNull(rank);
        assertEquals("/rank", rank.getString("usage"));
        assertEquals("tycoon.ranks", rank.getString("permission"));
        assertFalse(Objects.requireNonNull(
                yaml.getString("commands.is.usage")
        ).contains("rank"));

        ConfigurationSection spawn = yaml.getConfigurationSection("commands.spawn");
        assertNotNull(spawn);
        assertEquals("/spawn", spawn.getString("usage"));
        assertEquals("tycoon.spawn", spawn.getString("permission"));
    }
}
