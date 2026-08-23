package fr.valoriatycoon.crates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CrateRewardConfigLoaderTest {

    @Test
    void loadsExactPoolsAndTheValoriaSignatureTable() {
        CrateRewardSettings settings = load();

        assertEquals(8, settings.pools().size());
        for (CrateType type : CrateType.values()) {
            assertEquals(10_000, settings.pool(type).totalWeight(), type.name());
        }
        assertEquals(5, settings.moneyBagTiers().size());
        assertEquals(35_000_000L, settings.moneyTier(5).minimum());
        assertEquals(125_000, settings.coinTier(4).maximum());
        assertEquals(16, settings.resources().size());
        assertTrue(settings.resources().stream().noneMatch(resource ->
                resource.material() == Material.COD || resource.material() == Material.SALMON
        ));

        var commonKeys = settings.pool(CrateType.COMMON).rewards().stream()
                .filter(reward -> reward.kind() == CrateRewardKind.CRATE_KEYS)
                .map(CrateRewardSettings.RewardDefinition::keyType)
                .toList();
        assertTrue(commonKeys.contains(CrateType.RARE));
        assertTrue(commonKeys.contains(CrateType.EPIC));
        assertFalse(commonKeys.contains(CrateType.LEGENDARY));

        long valoriaJackpot = settings.pool(CrateType.LEGENDARY).rewards().stream()
                .filter(reward -> reward.kind() == CrateRewardKind.CRATE_KEYS)
                .filter(reward -> reward.keyType() == CrateType.VALORIA)
                .mapToLong(CrateRewardSettings.RewardDefinition::weight)
                .sum();
        assertEquals(50L, valoriaJackpot);
        assertTrue(settings.pool(CrateType.VALORIA).rewards().stream()
                .anyMatch(reward -> reward.id().equals("money_v_4") && reward.weight() == 50));
    }

    @Test
    void selectorCommitsOnlyRankAccessibleResourcesAndResolvedGeneratorTypes() {
        CrateRewardSettings settings = load();
        CrateRewardSelector selector = new CrateRewardSelector(
                settings,
                ignored -> 0,
                List.of("miner", "farmer", "lumber", "fisher")
        );
        Random random = new Random(2602L);
        UUID playerId = UUID.randomUUID();
        for (int index = 0; index < 5_000; index++) {
            CrateRewardSelection selection = selector.select(playerId, CrateType.VALORIA, random);
            assertFalse(selection.payload().encode().isBlank());
            if (selection.kind() == CrateRewardKind.RESOURCE_BUNDLE) {
                Material material = Material.valueOf(selection.payload().require("material"));
                assertTrue(material == Material.COAL
                        || material == Material.WHEAT
                        || material == Material.OAK_LOG);
            }
            if (selection.kind() == CrateRewardKind.GENERATORS) {
                assertEquals(
                        selection.payload().requireInt("amount"),
                        selection.payload().require("types").split(",").length
                );
            }
        }
    }

    private CrateRewardSettings load() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("crate-rewards.yml")
        );
        return CrateRewardConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );
    }
}
