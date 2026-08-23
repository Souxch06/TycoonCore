package fr.valoriatycoon.compaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CompactedResourceTest {

    @Test
    void calculatesRecursiveNineToOneEquivalence() {
        assertEquals(9L, new CompactedResource(Material.OAK_LOG, 1).baseUnits());
        assertEquals(81L, new CompactedResource(Material.OAK_LOG, 2).baseUnits());
        assertEquals(729L, new CompactedResource(Material.OAK_LOG, 3).baseUnits());
    }

    @Test
    void countsTheVanillaStorageBlockLayerForMinerals() {
        assertEquals(81L, new CompactedResource(Material.IRON_INGOT, 1).baseUnits());
        assertEquals(729L, new CompactedResource(Material.IRON_INGOT, 2).baseUnits());
        assertEquals(6_561L, new CompactedResource(Material.IRON_INGOT, 3).baseUnits());
        assertEquals(81L, new CompactedResource(Material.DIAMOND, 1).baseUnits());
    }

    @Test
    void rejectsUnsupportedLevels() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactedResource(Material.OAK_LOG, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactedResource(Material.OAK_LOG, 4)
        );
    }
}
