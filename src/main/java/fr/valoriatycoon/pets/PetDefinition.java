package fr.valoriatycoon.pets;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

/**
 * Immutable configured pet species, acquisition rule and level-scaled effects.
 * The species spawn-egg material is retained only to redeem physical eggs issued before v0.26.
 */
public record PetDefinition(
        String id,
        PetRarity rarity,
        EntityType entityType,
        Material icon,
        Material legacyEggMaterial,
        int menuSlot,
        int requiredRank,
        String displayName,
        List<String> lore,
        Map<PetEffect, PetBoost> effects
) {
    private static final PetBoost ZERO_BOOST = new PetBoost(
            java.math.BigDecimal.ZERO,
            java.math.BigDecimal.ZERO
    );

    public PetDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Pet id must not be blank");
        }
        rarity = Objects.requireNonNull(rarity, "rarity");
        entityType = Objects.requireNonNull(entityType, "entityType");
        icon = Objects.requireNonNull(icon, "icon");
        legacyEggMaterial = Objects.requireNonNull(legacyEggMaterial, "legacyEggMaterial");
        if (entityType.getEntityClass() == null
                || !Mob.class.isAssignableFrom(entityType.getEntityClass())
                || !icon.isItem()
                || !legacyEggMaterial.isItem()
                || !legacyEggMaterial.name().endsWith("_SPAWN_EGG")
                || menuSlot < 0
                || requiredRank < 0
                || displayName == null
                || displayName.isBlank()) {
            throw new IllegalArgumentException("Invalid pet definition: " + id);
        }
        lore = List.copyOf(lore);
        effects = Map.copyOf(effects);
    }

    /** Returns a configured effect or a zero-valued fallback. */
    public PetBoost effect(PetEffect effect) {
        return effects.getOrDefault(effect, ZERO_BOOST);
    }
}
