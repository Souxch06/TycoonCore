package fr.valoriatycoon.crates;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Immutable crates.yml snapshot for physical key presentation, acquisition and enabled openings. */
public record CrateSettings(
        boolean openingEnabled,
        Material keyMaterial,
        Map<CrateType, KeyPresentation> keys,
        Map<CrateType, Integer> toolRarityWeights
) {
    public CrateSettings {
        keyMaterial = Objects.requireNonNull(keyMaterial, "keyMaterial");
        keys = immutableEnumMap(keys);
        toolRarityWeights = immutableEnumMap(toolRarityWeights);
        if (!keyMaterial.isItem() || keys.size() != CrateType.values().length) {
            throw new IllegalArgumentException("Every generic crate key needs a valid presentation");
        }
        for (CrateType type : CrateType.values()) {
            if (keys.get(type).paid() != type.paid()) {
                throw new IllegalArgumentException("Paid flag mismatch for crate " + type);
            }
        }
        for (CrateType type : List.of(CrateType.COMMON, CrateType.RARE, CrateType.EPIC, CrateType.LEGENDARY)) {
            Integer weight = toolRarityWeights.get(type);
            if (weight == null || weight < 1) {
                throw new IllegalArgumentException("Missing positive tool key weight for " + type);
            }
        }
        if (toolRarityWeights.size() != 4) {
            throw new IllegalArgumentException("Tool key weights may only contain four rarity crates");
        }
    }

    public KeyPresentation key(CrateType type) {
        KeyPresentation presentation = keys.get(type);
        if (presentation == null) {
            throw new IllegalArgumentException("Missing crate key presentation: " + type);
        }
        return presentation;
    }

    public record KeyPresentation(String name, List<String> lore, boolean paid) {
        public KeyPresentation {
            name = Objects.requireNonNull(name, "name");
            lore = List.copyOf(lore);
            if (name.isBlank()) {
                throw new IllegalArgumentException("Crate key name must not be blank");
            }
        }
    }

    private static <V> Map<CrateType, V> immutableEnumMap(Map<CrateType, V> source) {
        Objects.requireNonNull(source, "source");
        EnumMap<CrateType, V> copy = new EnumMap<>(CrateType.class);
        copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }
}
