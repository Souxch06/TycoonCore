package fr.valoriatycoon.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class TutorialConfigLoaderTest {

    @Test
    void loadsSixRewardedStepsThatFundCitizen() {
        TutorialSettings settings = TutorialConfigLoader.load(loadConfiguration());

        assertEquals(6, settings.steps().size());
        assertEquals(48L, settings.step(TutorialStep.MINE_COAL).target());
        assertEquals(55L, settings.step(TutorialStep.HARVEST_WHEAT).target());
        assertEquals(20L, settings.step(TutorialStep.CHOP_OAK).target());
        assertEquals(4L, settings.step(TutorialStep.CATCH_COD).target());
        assertEquals(
                "Pêche 4 Morues crues dans /farm",
                settings.step(TutorialStep.CATCH_COD).objective()
        );
        assertEquals(20L, settings.step(TutorialStep.REACH_VANILLA_LEVEL).target());
        assertEquals(1L, settings.step(TutorialStep.COMPLETE_COMMON_QUEST).target());
        assertEquals(
                2_400_000L,
                settings.steps().values().stream()
                        .mapToLong(TutorialSettings.StepDefinition::rewardMoneyCents)
                        .sum()
        );
    }

    private YamlConfiguration loadConfiguration() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("tutorial.yml")
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }
}
