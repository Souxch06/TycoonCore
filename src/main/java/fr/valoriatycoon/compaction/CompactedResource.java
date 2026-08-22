package fr.valoriatycoon.compaction;

import java.util.Objects;
import org.bukkit.Material;

/** Identifies one authenticated compacted resource level. */
public record CompactedResource(Material material, int level) {
    public CompactedResource {
        material = Objects.requireNonNull(material, "material");
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Compaction level must be between one and three");
        }
    }

    /** Number of normal resources represented by one compact item. */
    public long baseUnits() {
        long units = CompactionMaterialRules.baseUnitsPerCraftingItem(material);
        for (int index = 0; index < level; index++) {
            units = Math.multiplyExact(units, 9L);
        }
        return units;
    }
}
