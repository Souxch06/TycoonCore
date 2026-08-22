package fr.valoriatycoon.ranks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.valoriatycoon.compaction.CompactedResource;
import fr.valoriatycoon.compaction.CompactionConfigLoader;
import fr.valoriatycoon.compaction.CompactionSettings;
import fr.valoriatycoon.professions.ProfessionType;
import fr.valoriatycoon.quests.QuestRarity;
import fr.valoriatycoon.tools.ToolType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class RankConfigLoaderTest {

    @Test
    void loadsTenCoherentMedievalRanks() {
        RankSettings settings = RankConfigLoader.load(loadConfiguration());

        assertEquals(10, settings.maximumLevel());
        assertEquals("Citoyen", settings.name(1));
        assertEquals("Chevalier", settings.name(5));
        assertEquals("Duc", settings.name(10));
        assertTrue(settings.level(10).orElseThrow().quests().containsKey(QuestRarity.LEGENDARY));
    }

    @Test
    void loadsCumulativePlaytimeRequirementsUpToTwoWeeks() {
        RankSettings settings = RankConfigLoader.load(loadConfiguration());
        long[] expectedMinutes = {
                30L, 180L, 360L, 600L, 1_200L,
                2_400L, 4_800L, 8_400L, 13_200L, 20_160L
        };

        for (int level = 1; level <= expectedMinutes.length; level++) {
            assertEquals(
                    expectedMinutes[level - 1] * 60L,
                    settings.level(level).orElseThrow().requiredPlaytimeSeconds()
            );
        }
    }

    @Test
    void keepsFirstThreeRanksAccessibleButChallenging() {
        RankSettings settings = RankConfigLoader.load(loadConfiguration());
        RankRequirement citizen = settings.level(1).orElseThrow();
        RankRequirement artisan = settings.level(2).orElseThrow();
        RankRequirement merchant = settings.level(3).orElseThrow();

        assertEquals(2_500_000L, citizen.requiredMoneyCents());
        assertEquals(50_000_000L, artisan.requiredMoneyCents());
        assertEquals(150_000_000L, merchant.requiredMoneyCents());
        assertEquals(20, citizen.requiredVanillaExperienceLevels());
        assertEquals(100, artisan.requiredVanillaExperienceLevels());
        assertEquals(200, merchant.requiredVanillaExperienceLevels());
        assertEquals(1, citizen.quests().get(QuestRarity.COMMON));
        assertEquals(5, artisan.quests().get(QuestRarity.COMMON));
        assertEquals(8, merchant.quests().get(QuestRarity.COMMON));
        assertEquals(3, citizen.toolLevels().get(ToolType.PICKAXE));
        assertEquals(8, artisan.toolLevels().get(ToolType.PICKAXE));
        assertEquals(12, merchant.toolLevels().get(ToolType.PICKAXE));
        assertEquals(1, citizen.professionLevels().get(ProfessionType.MINER));
        assertEquals(4, artisan.professionLevels().get(ProfessionType.MINER));
        assertEquals(7, merchant.professionLevels().get(ProfessionType.MINER));
        assertEquals(16, citizen.items().get(Material.COAL));
        assertEquals(4, citizen.items().get(Material.COD));
    }

    @Test
    void requiresOnlyResourcesAvailableBeforeEachFutureZoneUnlock() {
        RankSettings settings = RankConfigLoader.load(loadConfiguration());
        EnumSet<Material> accessible = EnumSet.of(
                Material.COAL,
                Material.WHEAT,
                Material.OAK_LOG,
                Material.COD,
                Material.SALMON,
                Material.PUFFERFISH,
                Material.TROPICAL_FISH
        );

        for (int level = 1; level <= settings.maximumLevel(); level++) {
            if (level == 3) {
                accessible.addAll(EnumSet.of(
                        Material.RAW_IRON,
                        Material.RAW_COPPER,
                        Material.IRON_INGOT,
                        Material.COPPER_INGOT,
                        Material.CARROT,
                        Material.BIRCH_LOG
                ));
            } else if (level == 6) {
                accessible.addAll(EnumSet.of(
                        Material.RAW_GOLD,
                        Material.GOLD_INGOT,
                        Material.REDSTONE,
                        Material.LAPIS_LAZULI,
                        Material.POTATO,
                        Material.SPRUCE_LOG
                ));
            } else if (level == 9) {
                accessible.addAll(EnumSet.of(
                        Material.DIAMOND,
                        Material.EMERALD,
                        Material.BEETROOT,
                        Material.DARK_OAK_LOG
                ));
            }
            RankRequirement requirement = settings.level(level).orElseThrow();
            assertTrue(
                    accessible.containsAll(requirement.items().keySet()),
                    "Rank " + level + " requires a normal item from a future zone"
            );
            assertTrue(
                    requirement.compactedItems().keySet().stream()
                            .map(CompactedResource::material)
                            .allMatch(accessible::contains),
                    "Rank " + level + " requires a compacted item from a future zone"
            );
        }
    }

    @Test
    void loadsMuchHarderFinalRankRequirements() {
        RankSettings settings = RankConfigLoader.load(loadConfiguration());
        RankRequirement comte = settings.level(8).orElseThrow();
        RankRequirement marquis = settings.level(9).orElseThrow();
        RankRequirement duke = settings.level(10).orElseThrow();

        assertEquals(
                2,
                comte.compactedItems().get(new CompactedResource(Material.GOLD_INGOT, 2))
        );
        assertEquals(
                4,
                comte.compactedItems().get(new CompactedResource(Material.REDSTONE, 2))
        );
        assertEquals(
                2,
                marquis.compactedItems().get(new CompactedResource(Material.DIAMOND, 2))
        );
        assertEquals(
                4,
                marquis.compactedItems().get(new CompactedResource(Material.BEETROOT, 2))
        );
        assertEquals(1_000, duke.requiredVanillaExperienceLevels());
        for (ProfessionType profession : ProfessionType.values()) {
            assertEquals(100, duke.professionLevels().get(profession));
        }
        for (ToolType tool : ToolType.values()) {
            assertEquals(100, duke.toolLevels().get(tool));
        }
        assertEquals(180, duke.quests().get(QuestRarity.COMMON));
        assertEquals(20, duke.quests().get(QuestRarity.LEGENDARY));
        assertEquals(
                2,
                duke.compactedItems().get(new CompactedResource(Material.DIAMOND, 3))
        );
        assertEquals(
                2,
                duke.compactedItems().get(new CompactedResource(Material.EMERALD, 3))
        );
        assertEquals(
                4,
                duke.compactedItems().get(new CompactedResource(Material.BEETROOT, 3))
        );
        assertEquals(
                3,
                duke.compactedItems().get(new CompactedResource(Material.DARK_OAK_LOG, 3))
        );
        assertEquals(96, duke.items().get(Material.TROPICAL_FISH));
        assertFalse(duke.items().containsKey(Material.NETHERITE_INGOT));
        assertFalse(duke.items().containsKey(Material.NETHER_STAR));
        assertEquals(200_000_000_000L, duke.requiredMoneyCents());
    }

    @Test
    void suppliesNewRequirementsToOlderRankConfigurations() {
        YamlConfiguration yaml = loadConfiguration();
        yaml.set("ranks.levels.5.playtime-minutes", null);
        yaml.set("ranks.levels.5.playtime-hours", 25L);
        yaml.set("ranks.levels.5.vanilla-xp-levels", null);
        yaml.set("ranks.levels.5.profession-levels", null);

        RankRequirement chevalier = RankConfigLoader.load(yaml).level(5).orElseThrow();

        assertEquals(1_200L * 60L, chevalier.requiredPlaytimeSeconds());
        assertEquals(500, chevalier.requiredVanillaExperienceLevels());
        assertEquals(22, chevalier.professionLevels().get(ProfessionType.MINER));
    }

    @Test
    void rejectsCompactedFishAndUnsupportedLevels() {
        CompactionSettings compaction = loadCompactionSettings();
        YamlConfiguration compactedFish = loadConfiguration();
        compactedFish.set("ranks.levels.4.compacted-items.SALMON.level", 1);
        compactedFish.set("ranks.levels.4.compacted-items.SALMON.amount", 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> RankConfigLoader.load(compactedFish, compaction)
        );

        YamlConfiguration excessiveLevel = loadConfiguration();
        excessiveLevel.set("ranks.levels.4.compacted-items.IRON_INGOT.level", 4);
        assertThrows(
                IllegalArgumentException.class,
                () -> RankConfigLoader.load(excessiveLevel, compaction)
        );
    }

    @Test
    void rejectsDecreasingPlaytimeRequirement() {
        YamlConfiguration yaml = loadConfiguration();
        yaml.set("ranks.levels.9.playtime-minutes", 100L);

        assertThrows(IllegalArgumentException.class, () -> RankConfigLoader.load(yaml));
    }

    @Test
    void rejectsDecreasingProfessionRequirement() {
        YamlConfiguration yaml = loadConfiguration();
        yaml.set("ranks.levels.6.profession-levels.MINER", 5);

        assertThrows(IllegalArgumentException.class, () -> RankConfigLoader.load(yaml));
    }

    @Test
    void rejectsDecreasingPermanentBenefit() {
        YamlConfiguration yaml = loadConfiguration();
        yaml.set("ranks.levels.6.permanent-tool-xp-bonus", 0.01);

        assertThrows(IllegalArgumentException.class, () -> RankConfigLoader.load(yaml));
    }

    @Test
    void rejectsNegativeRankPrice() {
        YamlConfiguration yaml = loadConfiguration();
        yaml.set("ranks.levels.1.money", "-1.00");

        assertThrows(IllegalArgumentException.class, () -> RankConfigLoader.load(yaml));
    }

    @Test
    void loadsProgressivePermanentRankBenefits() {
        RankSettings settings = RankConfigLoader.load(loadConfiguration());
        double previousToolExperience = -1.0;
        double previousProfessionExperience = -1.0;
        double previousCoins = -1.0;
        double previousGeneratorProduction = -1.0;
        int previousGeneratorSlots = -1;

        for (int level = 1; level <= settings.maximumLevel(); level++) {
            RankRequirement rank = settings.level(level).orElseThrow();
            assertEquals(level * 0.02, rank.permanentRevenueBonus(), 0.000_001);
            assertTrue(rank.toolExperienceBonus() >= previousToolExperience);
            assertTrue(rank.professionExperienceBonus() >= previousProfessionExperience);
            assertTrue(rank.toolCoinBonus() >= previousCoins);
            assertTrue(rank.generatorProductionBonus() >= previousGeneratorProduction);
            assertTrue(rank.generatorSlotBonus() >= previousGeneratorSlots);
            previousToolExperience = rank.toolExperienceBonus();
            previousProfessionExperience = rank.professionExperienceBonus();
            previousCoins = rank.toolCoinBonus();
            previousGeneratorProduction = rank.generatorProductionBonus();
            previousGeneratorSlots = rank.generatorSlotBonus();
        }
        RankRequirement duke = settings.level(10).orElseThrow();
        assertEquals(0.20, duke.permanentRevenueBonus(), 0.000_001);
        assertEquals(0.50, duke.toolExperienceBonus(), 0.000_001);
        assertEquals(0.50, duke.professionExperienceBonus(), 0.000_001);
        assertEquals(0.40, duke.toolCoinBonus(), 0.000_001);
        assertEquals(0.25, duke.generatorProductionBonus(), 0.000_001);
        assertEquals(15, duke.generatorSlotBonus());
    }

    private CompactionSettings loadCompactionSettings() {
        InputStream input = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("compaction.yml")
        );
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        );
        return CompactionConfigLoader.load(yaml);
    }

    private YamlConfiguration loadConfiguration() {
        InputStream input = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("ranks.yml")
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        );
    }
}
