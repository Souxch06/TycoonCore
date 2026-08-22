package fr.valoriatycoon.pets;

import fr.valoriatycoon.economy.MoneyCodec;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

/** Strict parser for pet rarities, acquisition costs, visuals and passive boosts. */
public final class PetConfigLoader {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private PetConfigLoader() {
    }

    public static PetSettings load(FileConfiguration config) {
        int menuSize = integer(config, "menu.size", 54, 9, 54);
        PetSettings.Menu menu = new PetSettings.Menu(
                menuSize,
                text(config, "menu.title"),
                material(config, "menu.filler")
        );
        ConfigurationSection crateSection = required(config, "crate");
        Map<PetRarity, Integer> rarityWeights = new EnumMap<>(PetRarity.class);
        ConfigurationSection weightSection = required(crateSection, "rarity-weights");
        for (PetRarity rarity : PetRarity.values()) {
            rarityWeights.put(rarity, integer(weightSection, rarity.name(), 1, 1, 1_000_000));
        }
        if (weightSection.getKeys(false).size() != PetRarity.values().length) {
            throw new IllegalArgumentException("crate.rarity-weights must contain every rarity exactly once");
        }
        PetSettings.Crate crate = new PetSettings.Crate(
                integer(crateSection, "slot", 49, 0, menuSize - 1),
                material(crateSection, "icon"),
                text(crateSection, "name"),
                crateSection.getStringList("lore"),
                nonNegativeDecimal(crateSection, "chromatic-chance"),
                material(crateSection, "key-material"),
                text(crateSection, "key-name"),
                crateSection.getStringList("key-lore"),
                rarityWeights
        );
        ConfigurationSection reclaimSection = required(config, "reclaim");
        PetSettings.Egg egg = loadEgg(config);
        PetSettings.Reclaim reclaim = new PetSettings.Reclaim(
                reclaimSection.getBoolean("enabled", true),
                reclaimSection.getString("world", ""),
                finiteDouble(reclaimSection, "offset-x", -2.5),
                finiteDouble(reclaimSection, "offset-y", 0.0),
                finiteDouble(reclaimSection, "offset-z", 0.5),
                (float) finiteDouble(reclaimSection, "yaw", 180.0),
                text(reclaimSection, "name"),
                text(reclaimSection, "menu-title"),
                money(reclaimSection, "money-cost"),
                integer(reclaimSection, "vanilla-xp-levels", 30, 0, 10_000)
        );
        PetSettings.Progression progression = new PetSettings.Progression(
                integer(config, "progression.flush-interval-ticks", 40, 1, 1_200),
                positiveLong(config, "progression.experience-per-tool-action"),
                positiveLong(config, "progression.experience-per-generator-cycle"),
                integer(config, "visual.follow-interval-ticks", 10, 1, 100)
        );

        Map<PetRarity, PetRarityDefinition> rarities = new EnumMap<>(PetRarity.class);
        ConfigurationSection raritySection = required(config, "rarities");
        for (PetRarity rarity : PetRarity.values()) {
            ConfigurationSection value = required(raritySection, rarity.name());
            rarities.put(rarity, new PetRarityDefinition(
                    rarity,
                    text(value, "name"),
                    integer(value, "max-level", 25, 1, 1_000),
                    positiveLong(value, "base-experience"),
                    decimal(value, "experience-growth")
            ));
        }
        if (raritySection.getKeys(false).size() != PetRarity.values().length) {
            throw new IllegalArgumentException("rarities must contain exactly every PetRarity");
        }

        Map<String, PetDefinition> pets = new LinkedHashMap<>();
        Set<Integer> slots = new HashSet<>();
        slots.add(crate.menuSlot());
        ConfigurationSection petSection = required(config, "pets");
        for (String id : petSection.getKeys(false)) {
            if (!ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid pet id: " + id);
            }
            ConfigurationSection value = required(petSection, id);
            PetRarity rarity = enumValue(PetRarity.class, text(value, "rarity"), id + ".rarity");
            int slot = integer(value, "slot", 0, 0, menuSize - 1);
            if (!slots.add(slot)) {
                throw new IllegalArgumentException("Duplicate pet menu slot: " + slot);
            }
            Map<PetEffect, PetBoost> effects = effects(required(value, "effects"));
            int maximumLevel = rarities.get(rarity).maximumLevel();
            effects.forEach((effect, boost) -> {
                BigDecimal maximum = boost.atLevel(maximumLevel);
                BigDecimal limit = effect.chance() ? BigDecimal.ONE : BigDecimal.valueOf(5L);
                if (maximum.compareTo(limit) > 0) {
                    throw new IllegalArgumentException(
                            id + " effect " + effect + " exceeds " + limit + " at maximum level"
                    );
                }
            });
            pets.put(id, new PetDefinition(
                    id,
                    rarity,
                    enumValue(EntityType.class, text(value, "entity"), id + ".entity"),
                    material(value, "icon"),
                    legacyEggMaterial(value),
                    slot,
                    integer(value, "required-rank", 0, 0, 10),
                    text(value, "name"),
                    value.getStringList("lore"),
                    effects
            ));
        }
        return new PetSettings(menu, crate, egg, reclaim, progression, rarities, pets);
    }

    private static PetSettings.Egg loadEgg(ConfigurationSection config) {
        ConfigurationSection section = config.getConfigurationSection("egg");
        if (section == null) {
            return new PetSettings.Egg(
                    Material.EGG,
                    requireKey("valoriatycoon:pet_egg", "egg.normal-item-model"),
                    requireKey("valoriatycoon:pet_egg_chromatic", "egg.chromatic-item-model")
            );
        }
        return new PetSettings.Egg(
                material(section, "material"),
                requireKey(text(section, "normal-item-model"), "egg.normal-item-model"),
                requireKey(text(section, "chromatic-item-model"), "egg.chromatic-item-model")
        );
    }

    private static Material legacyEggMaterial(ConfigurationSection section) {
        String path = section.contains("legacy-egg-material")
                ? "legacy-egg-material"
                : "egg-material";
        return material(section, path);
    }

    private static NamespacedKey requireKey(String value, String path) {
        NamespacedKey key = NamespacedKey.fromString(value);
        if (key == null) {
            throw new IllegalArgumentException("Invalid namespaced key at " + path + ": " + value);
        }
        return key;
    }

    private static Map<PetEffect, PetBoost> effects(ConfigurationSection section) {
        Map<PetEffect, PetBoost> values = new EnumMap<>(PetEffect.class);
        for (String key : section.getKeys(false)) {
            PetEffect effect = enumValue(PetEffect.class, key, "effects." + key);
            ConfigurationSection value = required(section, key);
            values.put(effect, new PetBoost(
                    nonNegativeDecimal(value, "base"),
                    nonNegativeDecimal(value, "per-level")
            ));
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Each pet needs at least one effect");
        }
        return values;
    }

    private static long money(ConfigurationSection section, String path) {
        try {
            long value = MoneyCodec.toCents(new BigDecimal(text(section, path)));
            if (value < 0L) {
                throw new IllegalArgumentException(path + " must not be negative");
            }
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid money at " + path, exception);
        }
    }

    private static double finiteDouble(
            ConfigurationSection section,
            String path,
            double fallback
    ) {
        double value = section.getDouble(path, fallback);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(path + " must be finite");
        }
        return value;
    }

    private static BigDecimal decimal(ConfigurationSection section, String path) {
        try {
            BigDecimal value = new BigDecimal(text(section, path)).stripTrailingZeros();
            if (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(10L)) > 0) {
                throw new IllegalArgumentException(path + " must be between 1 and 10");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal at " + path, exception);
        }
    }

    private static BigDecimal nonNegativeDecimal(ConfigurationSection section, String path) {
        try {
            BigDecimal value = new BigDecimal(text(section, path)).stripTrailingZeros();
            if (value.signum() < 0) {
                throw new IllegalArgumentException(path + " must not be negative");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal at " + path, exception);
        }
    }

    private static Material material(ConfigurationSection section, String path) {
        Material material = Material.matchMaterial(text(section, path));
        if (material == null) {
            throw new IllegalArgumentException("Unknown material at " + path);
        }
        return material;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        return value.trim();
    }

    private static long positiveLong(ConfigurationSection section, String path) {
        long value = section.getLong(path, -1L);
        if (value < 1L) {
            throw new IllegalArgumentException(path + " must be positive");
        }
        return value;
    }

    private static int integer(
            ConfigurationSection section,
            String path,
            int fallback,
            int minimum,
            int maximum
    ) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return value;
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            String path
    ) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown value at " + path + ": " + value, exception);
        }
    }
}
