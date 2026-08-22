package fr.valoriatycoon.professions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.progression.LevelExperienceCalculator;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ProfessionConfigLoaderTest {

    @Test
    void loadsFourPermanentProfessionsWithIncreasingCurve() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("professions.yml")
        );
        var yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );

        ProfessionSettings settings = ProfessionConfigLoader.load(yaml);

        assertEquals(4, settings.professions().size());
        assertEquals("Mineur", settings.definition(ProfessionType.MINER).displayName());
        assertEquals("Bûcheron", settings.definition(ProfessionType.LUMBERJACK).displayName());
        assertEquals(new BigDecimal("1.15"), settings.progression().experienceMultiplier());
        assertTrue(LevelExperienceCalculator.requiredForNextLevel(20, settings.progression())
                > LevelExperienceCalculator.requiredForNextLevel(10, settings.progression()));
    }
}
