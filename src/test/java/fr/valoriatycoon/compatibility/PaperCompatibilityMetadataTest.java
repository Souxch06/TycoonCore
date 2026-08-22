package fr.valoriatycoon.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PaperCompatibilityMetadataTest {

    @Test
    void targetsPaperTwentySixTwoAndJavaTwentyFive() throws Exception {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("plugin.yml")
        );
        var plugin = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        YamlConfiguration config = load("config.yml");
        YamlConfiguration farms = load("farms.yml");
        YamlConfiguration pets = load("pets.yml");
        YamlConfiguration spawn = load("spawn.yml");
        YamlConfiguration tycoons = load("tycoons.yml");

        assertEquals("ValoriaTycoon", plugin.getString("name"));
        assertEquals("fr.valoriatycoon.ValoriaTycoonPlugin", plugin.getString("main"));
        assertEquals("26.2", plugin.getString("api-version"));
        assertTrue(pom.contains("<groupId>fr.valoriatycoon</groupId>"));
        assertTrue(pom.contains("<artifactId>valoriatycoon</artifactId>"));
        assertTrue(pom.contains("<finalName>ValoriaTycoon-${project.version}</finalName>"));
        assertTrue(pom.contains("<maven.compiler.release>25</maven.compiler.release>"));
        assertTrue(pom.contains("<paper.version>26.2.build.112-stable</paper.version>"));
        assertTrue(pom.contains("<version>2.12.3</version>"));
        assertEquals("valoriatycoon.db", config.getString("database.sqlite.file"));
        assertEquals("valoria_farm_mine", farms.getString("farms.mine.world"));
        assertEquals("valoria_farm_fields", farms.getString("farms.fields.world"));
        assertEquals("valoria_farm_fishing", farms.getString("farms.fishing.world"));
        assertEquals("valoria_farm_forest", farms.getString("farms.forest.world"));
        assertEquals(8, Objects.requireNonNull(pets.getConfigurationSection("pets")).getKeys(false).size());
        assertEquals("valoria_spawn", spawn.getString("world"));
        assertEquals("valoria_plots", tycoons.getString("tycoons.default.world"));
    }

    private YamlConfiguration load(String resourceName) {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(resourceName)
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }
}
