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

/** Creates uniquely identified physical crate keys and removes consumed copies safely. */
public final class PetKeyService {
    private final PetSettings.Crate crate;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;

    public PetKeyService(
            JavaPlugin plugin,
            PetSettings settings,
            ItemVisualService visuals,
            MessageService messages
    ) {
        Objects.requireNonNull(plugin, "plugin");
        this.crate = Objects.requireNonNull(settings, "settings").crate();
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.markerKey = new NamespacedKey(plugin, "pet_crate_key");
        this.idKey = new NamespacedKey(plugin, "pet_crate_key_id");
    }

    /** Creates one unstackable-by-identity, tradable physical key. */
    public ItemStack create() {
        return create(UUID.randomUUID());
    }

    /** Creates a key with a deterministic id for crash-safe crate-voucher recovery. */
    public ItemStack create(UUID keyId) {
        Objects.requireNonNull(keyId, "keyId");
        ItemStack key = new ItemStack(crate.keyMaterial());
        ItemMeta meta = key.getItemMeta();
        meta.displayName(messages.render(crate.keyName()));
        meta.lore(crate.keyLore().stream().map(messages::render).toList());
        visuals.apply(meta, "item/key/pet_crate");
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(
                idKey,
                PersistentDataType.STRING,
                keyId.toString()
        );
        key.setItemMeta(meta);
        return key;
    }

    /** Creates a non-authenticated Pet key preview for protected menus. */
    public ItemStack preview() {
        ItemStack key = new ItemStack(crate.keyMaterial());
        ItemMeta meta = key.getItemMeta();
        meta.displayName(messages.render(crate.keyName()));
        meta.lore(crate.keyLore().stream().map(messages::render).toList());
        visuals.apply(meta, "item/key/pet_crate");
        key.setItemMeta(meta);
        return key;
    }

    /** Creates distinct keys; every returned item carries a different immutable identifier. */
    public List<ItemStack> create(int amount) {
        if (amount < 1 || amount > 512) {
            throw new IllegalArgumentException("Physical pet key amount must be between 1 and 512");
        }
        List<ItemStack> keys = new ArrayList<>(amount);
        for (int index = 0; index < amount; index++) {
            keys.add(create());
        }
        return List.copyOf(keys);
    }

    /** Finds the first authenticated key in storage or the off-hand. */
    public Optional<UUID> firstKey(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getStorageContents()) {
            Optional<UUID> id = keyId(item);
            if (id.isPresent()) {
                return id;
            }
        }
        return keyId(inventory.getItemInOffHand());
    }

    /** Counts distinct authenticated identifiers; copied stacks count as one usable key. */
    public int count(Player player) {
        java.util.Set<UUID> identifiers = new java.util.HashSet<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            keyId(item).ifPresent(identifiers::add);
        }
        keyId(player.getInventory().getItemInOffHand()).ifPresent(identifiers::add);
        return identifiers.size();
    }

    /** Removes every local copy carrying a consumed identifier, including illicit stacked copies. */
    public void removeConsumed(Player player, UUID keyId) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        boolean changed = false;
        for (int slot = 0; slot < storage.length; slot++) {
            UUID itemId = keyId(storage[slot]).orElse(null);
            if (keyId.equals(itemId)) {
                storage[slot] = null;
                changed = true;
            }
        }
        if (changed) {
            inventory.setStorageContents(storage);
        }
        UUID offHandId = keyId(inventory.getItemInOffHand()).orElse(null);
        if (keyId.equals(offHandId)) {
            inventory.setItemInOffHand(null);
        }
    }

    /** Delivers distinct physical keys and drops only inventory overflow at the player. */
    public void give(Player player, int amount) {
        for (ItemStack key : create(amount)) {
            give(player, key);
        }
    }

    /** Delivers deterministic key ids; retrying produces harmless copies of the same identities. */
    public void give(Player player, List<UUID> keyIds) {
        for (UUID keyId : keyIds) {
            give(player, create(keyId));
        }
    }

    private void give(Player player, ItemStack key) {
        player.getInventory().addItem(key).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }

    public Optional<UUID> keyId(ItemStack item) {
        if (item == null || item.getType() != crate.keyMaterial()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        String rawId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (marker == null || marker != 1 || rawId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(rawId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
