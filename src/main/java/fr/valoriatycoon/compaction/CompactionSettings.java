package fr.valoriatycoon.compaction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Immutable compaction.yml snapshot. */
public record CompactionSettings(
        int maximumLevel,
        Map<Integer, String> levelNames,
        List<String> itemLore,
        Map<Material, ResourceDefinition> resources,
        NpcSettings npc
) {
    public CompactionSettings {
        if (maximumLevel < 1 || maximumLevel > 3) {
            throw new IllegalArgumentException("Compaction supports between one and three levels");
        }
        levelNames = Collections.unmodifiableMap(new LinkedHashMap<>(levelNames));
        itemLore = List.copyOf(itemLore);
        resources = Collections.unmodifiableMap(new LinkedHashMap<>(resources));
        npc = Objects.requireNonNull(npc, "npc");
        for (int level = 1; level <= maximumLevel; level++) {
            if (!levelNames.containsKey(level)) {
                throw new IllegalArgumentException("Missing compact item name for level " + level);
            }
        }
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("At least one compactable resource is required");
        }
    }

    public ResourceDefinition resource(Material material) {
        ResourceDefinition definition = resources.get(material);
        if (definition == null) {
            throw new IllegalArgumentException("Material is not compactable: " + material);
        }
        return definition;
    }

    public String levelName(int level) {
        String name = levelNames.get(level);
        if (name == null) {
            throw new IllegalArgumentException("Unsupported compaction level " + level);
        }
        return name;
    }

    /** Plain user-facing name and first-level ingredient of one logical base resource. */
    public record ResourceDefinition(
            Material material,
            Material craftingMaterial,
            int baseUnitsPerCraftingItem,
            String displayName
    ) {
        public ResourceDefinition {
            material = Objects.requireNonNull(material, "material");
            craftingMaterial = Objects.requireNonNull(craftingMaterial, "craftingMaterial");
            if (!craftingMaterial.isItem()
                    || craftingMaterial.getMaxStackSize() < 9
                    || baseUnitsPerCraftingItem < 1) {
                throw new IllegalArgumentException("Invalid compaction crafting material");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Compaction resource name must not be blank");
            }
        }

        /** Returns whether level I starts from nine vanilla mineral storage blocks. */
        public boolean mineralBlockBased() {
            return baseUnitsPerCraftingItem > 1;
        }
    }

    /** Location and presentation of the built-in decompaction villager. */
    public record NpcSettings(
            boolean enabled,
            String worldName,
            double offsetX,
            double offsetY,
            double offsetZ,
            float yaw,
            String name
    ) {
        public NpcSettings {
            worldName = worldName == null ? "" : worldName.trim();
            if (!Double.isFinite(offsetX)
                    || !Double.isFinite(offsetY)
                    || !Double.isFinite(offsetZ)
                    || !Float.isFinite(yaw)) {
                throw new IllegalArgumentException("Invalid decompactor NPC location");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Decompactor NPC name must not be blank");
            }
        }
    }
}
