package fr.valoriatycoon.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class IslandMenuConfigLoaderTest {
    @Test
    void loadsImplementedAndFutureMenuActions() {
        var stream = getClass().getClassLoader().getResourceAsStream("menus.yml");
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        IslandMenuSettings settings = IslandMenuConfigLoader.load(yaml);
        assertEquals(54, settings.size());
        assertTrue(settings.entries().values().stream().anyMatch(e -> e.action() == IslandMenuAction.MACHINES));
        assertTrue(settings.entries().values().stream().anyMatch(e -> e.action() == IslandMenuAction.RANKS));
        assertTrue(settings.entries().values().stream().anyMatch(e -> e.action() == IslandMenuAction.PETS));
        assertTrue(settings.entries().values().stream()
                .anyMatch(e -> e.action() == IslandMenuAction.LEADERBOARDS));
    }
}
