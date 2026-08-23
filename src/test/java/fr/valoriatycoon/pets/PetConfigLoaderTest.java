package fr.valoriatycoon.pets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.entity.EntityType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PetConfigLoaderTest {

    @Test
    void loadsFiveRaritiesAndSpecialBoosts() {
        PetSettings settings = PetConfigLoader.load(loadConfiguration());

        assertEquals(5, settings.rarities().size());
        assertEquals(8, settings.pets().size());
        assertEquals(org.bukkit.Material.TRIPWIRE_HOOK, settings.crate().keyMaterial());
        assertEquals(org.bukkit.Material.EGG, settings.egg().material());
        assertEquals("valoriatycoon:pet_egg", settings.egg().normalItemModel().toString());
        assertEquals(
                "valoriatycoon:pet_egg_chromatic",
                settings.egg().chromaticItemModel().toString()
        );
        assertEquals(new BigDecimal("0.01"), settings.crate().chromaticChance());
        assertEquals(5_000_000_00L, settings.reclaim().moneyCostCents());
        assertEquals(30, settings.reclaim().vanillaExperienceLevels());
        assertEquals(60, settings.crate().rarityWeights().get(PetRarity.COMMON));
        assertEquals(1, settings.crate().rarityWeights().get(PetRarity.MYTHIC));
        assertEquals(25, settings.rarity(PetRarity.COMMON).maximumLevel());
        assertEquals(100, settings.rarity(PetRarity.MYTHIC).maximumLevel());
        PetDefinition allay = settings.pet("allay_collector");
        assertEquals(PetRarity.EPIC, allay.rarity());
        assertEquals(EntityType.ALLAY, allay.entityType());
        assertEquals(org.bukkit.Material.ALLAY_SPAWN_EGG, allay.legacyEggMaterial());
        assertTrue(allay.effects().containsKey(PetEffect.DOUBLE_TOOL_REWARD_CHANCE));
        PetDefinition phoenix = settings.pet("phoenix");
        assertEquals(7, phoenix.effects().size());
        assertTrue(phoenix.effect(PetEffect.DOUBLE_GENERATOR_OUTPUT_CHANCE)
                .atLevel(100)
                .compareTo(BigDecimal.ONE) < 0);
    }

    @Test
    void keepsExistingPetConfigurationsCompatibleWithTheSharedEggVisual() {
        YamlConfiguration configuration = loadConfiguration();
        configuration.set("egg", null);
        for (String petId : configuration.getConfigurationSection("pets").getKeys(false)) {
            String base = "pets." + petId + ".";
            configuration.set(
                    base + "egg-material",
                    configuration.getString(base + "legacy-egg-material")
            );
            configuration.set(base + "legacy-egg-material", null);
        }

        PetSettings settings = PetConfigLoader.load(configuration);

        assertEquals(org.bukkit.Material.EGG, settings.egg().material());
        assertEquals("valoriatycoon:pet_egg", settings.egg().normalItemModel().toString());
        assertEquals(
                org.bukkit.Material.RABBIT_SPAWN_EGG,
                settings.pet("rabbit_farmer").legacyEggMaterial()
        );
    }

    private YamlConfiguration loadConfiguration() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("pets.yml")
        );
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }
}
