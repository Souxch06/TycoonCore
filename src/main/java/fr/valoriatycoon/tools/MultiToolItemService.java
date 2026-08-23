package fr.valoriatycoon.tools;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Owns the single account-bound item that morphs between all four multi-tool forms. */
public final class MultiToolItemService {
    private final ToolSettings.MultiTool settings;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final NamespacedKey markerKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey rankKey;
    private final NamespacedKey legacyTierKey;
    private final Set<UUID> provisionedPlayers = ConcurrentHashMap.newKeySet();

    public MultiToolItemService(
            JavaPlugin plugin,
            ToolSettings.MultiTool settings,
            ItemVisualService visuals,
            MessageService messages
    ) {
        Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.markerKey = new NamespacedKey(plugin, "multitool");
        this.ownerKey = new NamespacedKey(plugin, "multitool_owner");
        this.idKey = new NamespacedKey(plugin, "multitool_id");
        this.tierKey = new NamespacedKey(plugin, "multitool_tier");
        this.rankKey = new NamespacedKey(plugin, "multitool_rank");
        this.legacyTierKey = new NamespacedKey("tycooncore", "multitool_tier");
    }

    /**
     * Ensures that the player owns exactly one local multi-tool, migrates the legacy object,
     * demotes local copies to normal tools and synchronizes the retained item to the current rank.
     */
    public void ensureSingle(Player player, int rank) {
        Objects.requireNonNull(player, "player");
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        ItemStack offHand = inventory.getItemInOffHand();
        UUID playerId = player.getUniqueId();

        ItemStack retained = preferredCandidate(storage, offHand, inventory.getHeldItemSlot(), playerId);
        if (retained == null) {
            if (provisionedPlayers.add(playerId)) {
                give(player, create(playerId, rank));
            }
            return;
        }

        provisionedPlayers.add(playerId);
        bindAndApply(retained, form(retained), rank, playerId);
        boolean storageChanged = false;
        for (ItemStack item : storage) {
            if (item != null
                    && item != retained
                    && (isOwnedBy(item, playerId) || legacyCandidate(item))) {
                stripMultiToolData(item);
                storageChanged = true;
            }
        }
        if (storageChanged) {
            inventory.setStorageContents(storage);
        }
        if (offHand != retained
                && (isOwnedBy(offHand, playerId) || legacyCandidate(offHand))) {
            stripMultiToolData(offHand);
            inventory.setItemInOffHand(offHand);
        }
    }

    /** Transforms the same authenticated logical item into another form. */
    public Optional<ItemStack> transform(
            ItemStack current,
            ToolType targetType,
            int rank,
            UUID playerId
    ) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(playerId, "playerId");
        if (!isOwnedBy(current, playerId)) {
            return Optional.empty();
        }
        ItemStack transformed = current.clone();
        bindAndApply(transformed, targetType, rank, playerId);
        return Optional.of(transformed);
    }

    /** Updates one authenticated item in place using its owner's authoritative rank. */
    public boolean synchronize(ItemStack item, int rank, UUID playerId) {
        if (!isOwnedBy(item, playerId)) {
            return false;
        }
        ToolType type = form(item);
        ToolTier targetTier = settings.tierForRank(rank);
        ItemMeta before = item.getItemMeta();
        Integer storedRank = before.getPersistentDataContainer().get(rankKey, PersistentDataType.INTEGER);
        boolean changed = item.getType() != targetTier.material(type)
                || !targetTier.name().equals(before.getPersistentDataContainer().get(
                        tierKey,
                        PersistentDataType.STRING
                ))
                || storedRank == null
                || storedRank != rank
                || !modelMatches(before, rank, type);
        bindAndApply(item, type, rank, playerId);
        return changed;
    }

    /** Refreshes the sole local object immediately after a successful rank promotion. */
    public void synchronizeInventory(Player player, int rank) {
        ensureSingle(player, rank);
    }

    /** Returns whether the item is the authenticated multi-tool owned by this player. */
    public boolean isOwnedBy(ItemStack item, UUID playerId) {
        if (item == null || item.getType().isAir() || playerId == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        String owner = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        String id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        return marker != null
                && marker.byteValue() == 1
                && playerId.toString().equals(owner)
                && identityFor(playerId).toString().equals(id)
                && ToolType.fromMaterial(item.getType()).isPresent();
    }

    /** Returns whether the item carries any current or legacy Valoria multi-tool identity. */
    public boolean isMultiTool(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)
                || legacyCandidate(item);
    }

    /** Returns the persisted owner of a bound multi-tool. */
    public Optional<UUID> owner(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        String owner = item.getItemMeta().getPersistentDataContainer().get(
                ownerKey,
                PersistentDataType.STRING
        );
        try {
            return owner == null ? Optional.empty() : Optional.of(UUID.fromString(owner));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** Releases only the online delivery guard; the deterministic item identity remains unchanged. */
    public void release(UUID playerId) {
        provisionedPlayers.remove(playerId);
    }

    private ItemStack preferredCandidate(
            ItemStack[] storage,
            ItemStack offHand,
            int heldSlot,
            UUID playerId
    ) {
        ItemStack held = heldSlot >= 0 && heldSlot < storage.length ? storage[heldSlot] : null;
        if (isOwnedBy(held, playerId)) {
            return held;
        }
        for (ItemStack item : storage) {
            if (isOwnedBy(item, playerId)) {
                return item;
            }
        }
        if (isOwnedBy(offHand, playerId)) {
            return offHand;
        }
        if (legacyCandidate(held)) {
            bindIdentity(held, playerId);
            return held;
        }
        for (ItemStack item : storage) {
            if (legacyCandidate(item)) {
                bindIdentity(item, playerId);
                return item;
            }
        }
        if (legacyCandidate(offHand)) {
            bindIdentity(offHand, playerId);
            return offHand;
        }
        return null;
    }

    private ItemStack create(UUID playerId, int rank) {
        ToolTier tier = settings.tierForRank(rank);
        ItemStack item = new ItemStack(tier.material(ToolType.PICKAXE));
        bindAndApply(item, ToolType.PICKAXE, rank, playerId);
        return item;
    }

    private void bindAndApply(ItemStack item, ToolType type, int rank, UUID playerId) {
        ToolTier tier = settings.tierForRank(rank);
        item.setType(tier.material(type));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render("<light_purple><bold>Multi-tool de Valoria</bold></light_purple>"));
        meta.lore(List.of(
                messages.render("<gray>Votre unique outil personnel.</gray>"),
                messages.render("<gray>Il change entre pioche, houe, hache et canne à pêche.</gray>"),
                messages.render("<gray>Sa matière et son apparence évoluent avec votre rang.</gray>"),
                messages.render("<dark_gray>Objet lié : impossible à jeter, stocker ou fabriquer.</dark_gray>")
        ));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, playerId.toString());
        meta.getPersistentDataContainer().set(
                idKey,
                PersistentDataType.STRING,
                identityFor(playerId).toString()
        );
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.name());
        meta.getPersistentDataContainer().set(rankKey, PersistentDataType.INTEGER, rank);
        preserveGoldenPickaxeDrops(meta, tier, type);
        visuals.apply(meta, modelPath(rank, type));
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
        item.setItemMeta(meta);
    }

    private void bindIdentity(ItemStack item, UUID playerId) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, playerId.toString());
        meta.getPersistentDataContainer().set(
                idKey,
                PersistentDataType.STRING,
                identityFor(playerId).toString()
        );
        item.setItemMeta(meta);
    }

    private void stripMultiToolData(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(markerKey);
        meta.getPersistentDataContainer().remove(ownerKey);
        meta.getPersistentDataContainer().remove(idKey);
        meta.getPersistentDataContainer().remove(tierKey);
        meta.getPersistentDataContainer().remove(rankKey);
        meta.getPersistentDataContainer().remove(legacyTierKey);
        meta.displayName(null);
        meta.lore(null);
        meta.setUnbreakable(false);
        meta.setEnchantmentGlintOverride(null);
        if (meta.hasTool()) {
            meta.setTool(null);
        }
        visuals.clearOwned(meta);
        item.setItemMeta(meta);
    }

    private boolean legacyCandidate(ItemStack item) {
        if (item == null || item.getType().isAir()
                || ToolType.fromMaterial(item.getType()).isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) {
            return false;
        }
        return meta.getPersistentDataContainer().has(tierKey, PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(legacyTierKey, PersistentDataType.STRING);
    }

    private ToolType form(ItemStack item) {
        return ToolType.fromMaterial(item.getType()).orElseThrow(() ->
                new IllegalArgumentException("Multi-tool has an unsupported material: " + item.getType())
        );
    }

    /** Returns the stable account-bound identity shared by all four forms of one multi-tool. */
    public static UUID identityFor(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return UUID.nameUUIDFromBytes(
                ("valoriatycoon:multitool:" + playerId).getBytes(StandardCharsets.UTF_8)
        );
    }

    private void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }

    private void preserveGoldenPickaxeDrops(ItemMeta meta, ToolTier tier, ToolType type) {
        if (tier == ToolTier.GOLDEN && type == ToolType.PICKAXE) {
            var tool = meta.getTool();
            tool.setRules(List.of());
            tool.addRule(Tag.MINEABLE_PICKAXE, 12.0f, true);
            meta.setTool(tool);
        } else if (meta.hasTool()) {
            meta.setTool(null);
        }
    }

    private String modelPath(int rank, ToolType type) {
        return "item/multitool/rank/" + rank + '/'
                + ItemVisualService.segment(type.name());
    }

    private boolean modelMatches(ItemMeta meta, int rank, ToolType type) {
        NamespacedKey model = meta.getItemModel();
        if (!visuals.enabled()) {
            return model == null;
        }
        return visuals.key(modelPath(rank, type)).equals(model);
    }
}
