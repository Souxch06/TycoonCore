package fr.valoriatycoon.crates;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Creates and reads tradable generic crate keys authenticated by PDC and the SQLite ledger. */
public final class CrateKeyItemService {
    private final CrateSettings settings;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey typeKey;

    public CrateKeyItemService(
            JavaPlugin plugin,
            CrateSettings settings,
            ItemVisualService visuals,
            MessageService messages
    ) {
        Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.markerKey = new NamespacedKey(plugin, "crate_key");
        this.idKey = new NamespacedKey(plugin, "crate_key_id");
        this.typeKey = new NamespacedKey(plugin, "crate_key_type");
    }

    public ItemStack create(CrateKey key) {
        CrateSettings.KeyPresentation presentation = settings.key(key.type());
        ItemStack item = new ItemStack(settings.keyMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(presentation.name()));
        meta.lore(presentation.lore().stream().map(messages::render).toList());
        visuals.apply(meta, "item/key/crate_" + key.type().configKey());
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, key.keyId().toString());
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, key.type().name());
        item.setItemMeta(meta);
        return item;
    }

    /** Creates a non-redeemable menu preview using the same model as a physical key. */
    public ItemStack preview(CrateType type) {
        CrateSettings.KeyPresentation presentation = settings.key(type);
        ItemStack item = new ItemStack(settings.keyMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(presentation.name()));
        meta.lore(presentation.lore().stream().map(messages::render).toList());
        visuals.apply(meta, "item/key/crate_" + type.configKey());
        item.setItemMeta(meta);
        return item;
    }

    /** Gives or naturally drops one issued key without changing its immutable UUID. */
    public void give(Player player, CrateKey key) {
        player.getInventory().addItem(create(key)).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }

    public Optional<KeyToken> token(ItemStack item) {
        if (item == null || item.getType() != settings.keyMaterial()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        String rawId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        String rawType = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (marker == null || marker.byteValue() != 1 || rawId == null || rawType == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new KeyToken(
                    UUID.fromString(rawId),
                    CrateType.valueOf(rawType)
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** Finds the first distinct physical key matching the station type. */
    public Optional<KeyToken> firstToken(Player player, CrateType type) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            KeyToken token = token(item).orElse(null);
            if (token != null && token.type() == type) {
                return Optional.of(token);
            }
        }
        KeyToken offHand = token(player.getInventory().getItemInOffHand()).orElse(null);
        return offHand != null && offHand.type() == type ? Optional.of(offHand) : Optional.empty();
    }

    /** Removes all local copies carrying a key UUID after authoritative consumption/rejection. */
    public void removeCopies(Player player, UUID keyId) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        boolean changed = false;
        for (int slot = 0; slot < storage.length; slot++) {
            KeyToken token = token(storage[slot]).orElse(null);
            if (token != null && token.keyId().equals(keyId)) {
                storage[slot] = null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setStorageContents(storage);
        }
        KeyToken offHand = token(player.getInventory().getItemInOffHand()).orElse(null);
        if (offHand != null && offHand.keyId().equals(keyId)) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    public Map<CrateType, Integer> count(Player player) {
        Map<CrateType, java.util.Set<UUID>> unique = new EnumMap<>(CrateType.class);
        for (ItemStack item : player.getInventory().getStorageContents()) {
            token(item).ifPresent(token -> unique
                    .computeIfAbsent(token.type(), ignored -> new java.util.HashSet<>())
                    .add(token.keyId()));
        }
        token(player.getInventory().getItemInOffHand()).ifPresent(token -> unique
                .computeIfAbsent(token.type(), ignored -> new java.util.HashSet<>())
                .add(token.keyId()));
        Map<CrateType, Integer> result = new EnumMap<>(CrateType.class);
        unique.forEach((type, ids) -> result.put(type, ids.size()));
        return Map.copyOf(result);
    }

    public record KeyToken(UUID keyId, CrateType type) {
        public KeyToken {
            keyId = Objects.requireNonNull(keyId, "keyId");
            type = Objects.requireNonNull(type, "type");
        }
    }
}
