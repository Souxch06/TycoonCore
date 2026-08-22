package fr.valoriatycoon.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ToolConfigLoaderTest {
    @Test
    void loadsLongBalancedCurvesAndToolSpecificAbilities() {
        var stream = ToolConfigLoaderTest.class.getClassLoader().getResourceAsStream("tools.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        ToolSettings settings = ToolConfigLoader.load(yaml);

        assertEquals(ToolTier.WOODEN, settings.multiTool().tierForRank(0));
        assertEquals(ToolTier.STONE, settings.multiTool().tierForRank(2));
        assertEquals(ToolTier.IRON, settings.multiTool().tierForRank(5));
        assertEquals(ToolTier.GOLDEN, settings.multiTool().tierForRank(7));
        assertEquals(ToolTier.DIAMOND, settings.multiTool().tierForRank(9));
        assertEquals(ToolTier.NETHERITE, settings.multiTool().tierForRank(10));

        ToolSettings.Progression progression = settings.progression();
        assertEquals(new BigDecimal("1.1"), progression.experienceMultiplier());
        assertEquals(5, progression.difficultyTiers().size());
        assertEquals(new BigDecimal("1.5"), progression.difficultyMultiplier(21));
        assertEquals(new BigDecimal("2.25"), progression.difficultyMultiplier(41));
        assertEquals(new BigDecimal("3.5"), progression.difficultyMultiplier(61));
        assertEquals(new BigDecimal("5"), progression.difficultyMultiplier(81));
        assertTrue(ToolExperienceCalculator.requiredForNextLevel(21, progression)
                > ToolExperienceCalculator.requiredForNextLevel(20, progression));

        ToolCapabilityDefinition money = settings.capability(ToolCapability.MONEY_BOOST);
        assertEquals(1000, money.maximumLevel());
        assertEquals(new BigDecimal("1.4995"), money.level(1000).orElseThrow().value());
        assertEquals(1000, settings.capability(ToolCapability.COIN_BOOST).maximumLevel());
        assertEquals(25, settings.capability(ToolCapability.EFFICIENCY).maximumLevel());
        assertEquals(new BigDecimal("0.116"), settings.capability(ToolCapability.EFFICIENCY)
                .level(25).orElseThrow().value());

        assertTrue(settings.capability(ToolCapability.FARM_KEY_FINDER)
                .appliesTo(ToolType.FISHING_ROD));
        assertTrue(settings.capability(ToolCapability.CRATE_KEY_FINDER)
                .appliesTo(ToolType.PICKAXE));
        assertEquals(20, settings.capability(ToolCapability.FARM_KEY_FINDER).slot());
        assertEquals(21, settings.capability(ToolCapability.CRATE_KEY_FINDER).slot());

        ToolCapabilityDefinition speed = settings.capability(ToolCapability.SPEED_BURST);
        assertTrue(speed.appliesTo(ToolType.PICKAXE));
        assertTrue(speed.appliesTo(ToolType.HOE));
        assertTrue(speed.appliesTo(ToolType.AXE));
        assertFalse(speed.appliesTo(ToolType.FISHING_ROD));

        assertEquals(3, settings.capability(ToolCapability.AREA_MINING)
                .level(2).orElseThrow().value().intValue());
        assertEquals(5, settings.capability(ToolCapability.AREA_HARVEST)
                .level(3).orElseThrow().value().intValue());
        assertEquals(1, settings.capability(ToolCapability.TIMBER).initialLevel());
        assertEquals(4, settings.capability(ToolCapability.TIMBER).maximumLevel());
        assertEquals(4, settings.capability(ToolCapability.TIMBER)
                .level(4).orElseThrow().value().intValue());
        assertEquals(4, settings.abilities().maximumTimberBlocks());
        assertTrue(settings.capability(ToolCapability.UFO_HARVEST).appliesTo(ToolType.HOE));
        assertFalse(settings.capability(ToolCapability.UFO_HARVEST).appliesTo(ToolType.AXE));
    }

    @Test
    void suppliesRankMaterialDefaultsForExistingToolConfigurations() {
        var stream = ToolConfigLoaderTest.class.getClassLoader().getResourceAsStream("tools.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        yaml.set("multitool.rank-tiers", null);

        ToolSettings.MultiTool multiTool = ToolConfigLoader.load(yaml).multiTool();

        assertEquals(ToolTier.WOODEN, multiTool.tierForRank(1));
        assertEquals(ToolTier.IRON, multiTool.tierForRank(4));
        assertEquals(ToolTier.GOLDEN, multiTool.tierForRank(6));
        assertEquals(ToolTier.DIAMOND, multiTool.tierForRank(8));
        assertEquals(ToolTier.NETHERITE, multiTool.tierForRank(10));
    }
}
