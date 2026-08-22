package fr.valoriatycoon.pets;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Creates and validates physical pet eggs whose type and variant are fixed in persistent storage. */
public final class PetEggService {
    private final PetSettings settings;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey petKey;
    private final NamespacedKey chromaticKey;

    public PetEggService(
            JavaPlugin plugin,
            PetSettings settings,
            ItemVisualService visuals,
            MessageService messages
    ) {
        Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.markerKey = new NamespacedKey(plugin, "pet_egg");
        this.idKey = new NamespacedKey(plugin, "pet_egg_id");
        this.petKey = new NamespacedKey(plugin, "pet_egg_type");
        this.chromaticKey = new NamespacedKey(plugin, "pet_egg_chromatic");
    }

    /** Materializes one persisted egg without rerolling its chromatic state. */
    public ItemStack create(PetEgg egg) {
        PetDefinition definition = settings.pet(egg.petId());
        PetRarityDefinition rarity = settings.rarity(definition.rarity());
        ItemStack item = createVisual(egg.chromatic());
        ItemMeta meta = item.getItemMeta();
        String name = egg.chromatic()
                ? "<aqua><bold>Œuf chromatique de pet</bold></aqua>"
                : "<light_purple><bold>Œuf de pet</bold></light_purple>";
        meta.displayName(messages.render(name));
        meta.lore(lore(egg, definition, rarity));
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, egg.eggId().toString());
        meta.getPersistentDataContainer().set(petKey, PersistentDataType.STRING, egg.petId());
        meta.getPersistentDataContainer().set(
                chromaticKey,
                PersistentDataType.BYTE,
                egg.chromatic() ? (byte) 1 : (byte) 0
        );
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the common resource-pack-backed egg appearance without authentication data.
     * This is intended for trusted menu previews only and can never be redeemed as an egg.
     */
    public ItemStack createVisual(boolean chromatic) {
        ItemStack item = new ItemStack(settings.egg().material());
        ItemMeta meta = item.getItemMeta();
        if (visuals.enabled()) {
            meta.setItemModel(settings.egg().itemModel(chromatic));
        }
        meta.setEnchantmentGlintOverride(chromatic);
        item.setItemMeta(meta);
        return item;
    }

    /** Gives the egg or drops it at the player if the inventory is full. */
    public void give(Player player, PetEgg egg) {
        player.getInventory().addItem(create(egg)).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }

    /** Extracts authenticated item-side data before the asynchronous database validation. */
    public Optional<EggToken> token(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        String rawId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        String petId = meta.getPersistentDataContainer().get(petKey, PersistentDataType.STRING);
        Byte chromatic = meta.getPersistentDataContainer().get(
                chromaticKey,
                PersistentDataType.BYTE
        );
        if (marker == null
                || marker.byteValue() != 1
                || rawId == null
                || petId == null
                || chromatic == null
                || (chromatic.byteValue() != 0 && chromatic.byteValue() != 1)) {
            return Optional.empty();
        }
        PetDefinition definition = settings.pets().get(petId);
        if (definition == null) {
            return Optional.empty();
        }
        boolean chromaticVariant = chromatic.byteValue() == 1;
        boolean currentAppearance = item.getType() == settings.egg().material()
                && (!visuals.enabled()
                || settings.egg().itemModel(chromaticVariant).equals(meta.getItemModel()));
        boolean legacyAppearance = item.getType() == definition.legacyEggMaterial();
        if (!currentAppearance && !legacyAppearance) {
            return Optional.empty();
        }
        try {
            return Optional.of(new EggToken(
                    UUID.fromString(rawId),
                    petId,
                    chromaticVariant
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** Removes every local copy of a consumed egg UUID. */
    public void removeConsumed(Player player, UUID eggId) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        boolean changed = false;
        for (int slot = 0; slot < storage.length; slot++) {
            EggToken token = token(storage[slot]).orElse(null);
            if (token != null && token.eggId().equals(eggId)) {
                storage[slot] = null;
                changed = true;
            }
        }
        if (changed) {
            inventory.setStorageContents(storage);
        }
        EggToken offHand = token(inventory.getItemInOffHand()).orElse(null);
        if (offHand != null && offHand.eggId().equals(eggId)) {
            inventory.setItemInOffHand(null);
        }
    }

    private List<net.kyori.adventure.text.Component> lore(
            PetEgg egg,
            PetDefinition definition,
            PetRarityDefinition rarity
    ) {
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(messages.render("<dark_gray>Survolez pour consulter le contenu authentifié.</dark_gray>"));
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(messages.render("<gray>Pet : " + definition.displayName() + "</gray>"));
        lore.add(messages.render("<gray>Rareté : " + rarity.displayName() + "</gray>"));
        definition.lore().forEach(line -> lore.add(messages.render(line)));
        lore.add(messages.render(egg.chromatic()
                ? "<gray>Variante fixée : <aqua><bold>Chromatique</bold></aqua> <dark_gray>(cosmétique)</dark_gray></gray>"
                : "<gray>Variante fixée : <white>Normale</white></gray>"));
        lore.add(messages.render("<gray>Probabilité chromatique initiale : <aqua>"
                + PetTextFormatter.percentage(settings.crate().chromaticChance())
                + "%</aqua></gray>"));
        lore.add(messages.render("<red>La variante est définitive et ne peut pas être relancée.</red>"));
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(messages.render("<gray>Niveau conservé : <yellow>" + egg.level()
                + "/" + rarity.maximumLevel() + "</yellow></gray>"));
        long required = PetExperienceCalculator.requiredForNextLevel(egg.level(), rarity);
        lore.add(messages.render(required <= 0L
                ? "<gold>XP : niveau maximum atteint.</gold>"
                : "<gray>XP conservée : <aqua>" + egg.experience() + "/" + required + "</aqua></gray>"));
        lore.add(messages.render("<dark_gray>Bonus au niveau " + egg.level() + " :</dark_gray>"));
        definition.effects().forEach((effect, boost) -> {
            String current = PetTextFormatter.percentage(boost.atLevel(egg.level()));
            String maximum = egg.level() >= rarity.maximumLevel()
                    ? ""
                    : " <dark_gray>(max. +"
                    + PetTextFormatter.percentage(boost.atLevel(rarity.maximumLevel()))
                    + "%)</dark_gray>";
            lore.add(messages.render("<green>+" + current + "% "
                    + PetTextFormatter.effectName(effect) + "</green>" + maximum));
        });
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(messages.render("<yellow>Clic droit pour ajouter et équiper ce pet.</yellow>"));
        lore.add(messages.render("<dark_gray>Œuf échangeable — ID sécurisé "
                + egg.eggId().toString().substring(0, 8) + "…</dark_gray>"));
        return List.copyOf(lore);
    }

    /** Immutable data authenticated by both item PDC and the issued-egg database row. */
    public record EggToken(UUID eggId, String petId, boolean chromatic) {
        public EggToken {
            eggId = Objects.requireNonNull(eggId, "eggId");
            if (petId == null || petId.isBlank()) {
                throw new IllegalArgumentException("Pet egg token type must not be blank");
            }
        }
    }
}
