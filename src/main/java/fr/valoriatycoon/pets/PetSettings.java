package fr.valoriatycoon.pets;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/** Immutable pets.yml configuration snapshot. */
public record PetSettings(
        Menu menu,
        Crate crate,
        Egg egg,
        Reclaim reclaim,
        Progression progression,
        Map<PetRarity, PetRarityDefinition> rarities,
        Map<String, PetDefinition> pets
) {
    public PetSettings {
        menu = Objects.requireNonNull(menu, "menu");
        crate = Objects.requireNonNull(crate, "crate");
        egg = Objects.requireNonNull(egg, "egg");
        reclaim = Objects.requireNonNull(reclaim, "reclaim");
        progression = Objects.requireNonNull(progression, "progression");
        rarities = Collections.unmodifiableMap(new EnumMap<>(rarities));
        pets = Collections.unmodifiableMap(new LinkedHashMap<>(pets));
        if (rarities.size() != PetRarity.values().length || pets.isEmpty()) {
            throw new IllegalArgumentException("Every pet rarity and at least one pet are required");
        }
    }

    public PetDefinition pet(String id) {
        PetDefinition definition = pets.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown pet: " + id);
        }
        return definition;
    }

    public PetRarityDefinition rarity(PetRarity rarity) {
        PetRarityDefinition definition = rarities.get(rarity);
        if (definition == null) {
            throw new IllegalArgumentException("Missing pet rarity: " + rarity);
        }
        return definition;
    }

    public record Menu(int size, String title, Material filler) {
        public Menu {
            if (size < 9 || size > 54 || size % 9 != 0 || title == null || title.isBlank()) {
                throw new IllegalArgumentException("Invalid pet menu");
            }
            filler = Objects.requireNonNull(filler, "filler");
        }
    }

    public record Crate(
            int menuSlot,
            Material icon,
            String displayName,
            List<String> lore,
            BigDecimal chromaticChance,
            Material keyMaterial,
            String keyName,
            List<String> keyLore,
            Map<PetRarity, Integer> rarityWeights
    ) {
        public Crate {
            icon = Objects.requireNonNull(icon, "icon");
            chromaticChance = Objects.requireNonNull(chromaticChance, "chromaticChance");
            keyMaterial = Objects.requireNonNull(keyMaterial, "keyMaterial");
            if (menuSlot < 0
                    || chromaticChance.signum() < 0
                    || chromaticChance.compareTo(BigDecimal.ONE) > 0
                    || !icon.isItem()
                    || !keyMaterial.isItem()
                    || displayName == null
                    || displayName.isBlank()
                    || keyName == null
                    || keyName.isBlank()) {
                throw new IllegalArgumentException("Invalid pet crate presentation");
            }
            lore = List.copyOf(lore);
            keyLore = List.copyOf(keyLore);
            rarityWeights = Collections.unmodifiableMap(new EnumMap<>(rarityWeights));
            if (rarityWeights.size() != PetRarity.values().length
                    || rarityWeights.values().stream().anyMatch(weight -> weight == null || weight < 1)) {
                throw new IllegalArgumentException("Every pet rarity needs a positive crate weight");
            }
        }
    }

    /** Shared physical appearance used by every pet species. */
    public record Egg(
            Material material,
            NamespacedKey normalItemModel,
            NamespacedKey chromaticItemModel
    ) {
        public Egg {
            material = Objects.requireNonNull(material, "material");
            normalItemModel = Objects.requireNonNull(normalItemModel, "normalItemModel");
            chromaticItemModel = Objects.requireNonNull(chromaticItemModel, "chromaticItemModel");
            if (!material.isItem()) {
                throw new IllegalArgumentException("Pet egg material must be an item");
            }
        }

        /** Returns the resource-pack model matching the immutable egg variant. */
        public NamespacedKey itemModel(boolean chromatic) {
            return chromatic ? chromaticItemModel : normalItemModel;
        }
    }

    public record Reclaim(
            boolean enabled,
            String worldName,
            double offsetX,
            double offsetY,
            double offsetZ,
            float yaw,
            String npcName,
            String menuTitle,
            long moneyCostCents,
            int vanillaExperienceLevels
    ) {
        public Reclaim {
            worldName = worldName == null ? "" : worldName.trim();
            if (!Double.isFinite(offsetX)
                    || !Double.isFinite(offsetY)
                    || !Double.isFinite(offsetZ)
                    || !Float.isFinite(yaw)
                    || npcName == null
                    || npcName.isBlank()
                    || menuTitle == null
                    || menuTitle.isBlank()
                    || moneyCostCents < 0L
                    || vanillaExperienceLevels < 0) {
                throw new IllegalArgumentException("Invalid pet reclaim NPC settings");
            }
        }
    }

    public record Progression(
            int flushIntervalTicks,
            long experiencePerToolAction,
            long experiencePerGeneratorCycle,
            int followIntervalTicks
    ) {
        public Progression {
            if (flushIntervalTicks < 1
                    || experiencePerToolAction < 1L
                    || experiencePerGeneratorCycle < 1L
                    || followIntervalTicks < 1) {
                throw new IllegalArgumentException("Invalid pet progression timings");
            }
        }
    }
}
