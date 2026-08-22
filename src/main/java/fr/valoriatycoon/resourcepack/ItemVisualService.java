package fr.valoriatycoon.resourcepack;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Applies deterministic resource-pack item models while preserving vanilla material fallbacks. */
public final class ItemVisualService {
    private static final Pattern MODEL_PATH = Pattern.compile("[a-z0-9._/-]{1,160}");

    private final ResourcePackSettings settings;
    private final Map<String, NamespacedKey> keys = new ConcurrentHashMap<>();

    public ItemVisualService(ResourcePackSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /** Returns whether custom item models are enabled for newly rendered items. */
    public boolean enabled() {
        return settings.customItemModels();
    }

    /** Applies a configured model to mutable metadata; disabled packs leave the item untouched. */
    public void apply(ItemMeta meta, String modelPath) {
        Objects.requireNonNull(meta, "meta");
        if (!settings.customItemModels()) {
            meta.setItemModel(null);
            return;
        }
        meta.setItemModel(key(modelPath));
    }

    /** Removes a model owned by the configured namespace without touching another plugin's model. */
    public boolean clearOwned(ItemMeta meta) {
        Objects.requireNonNull(meta, "meta");
        NamespacedKey current = meta.getItemModel();
        if (current == null || !current.getNamespace().equals(settings.namespace())) {
            return false;
        }
        meta.setItemModel(null);
        return true;
    }

    /** Applies a configured model directly to an item stack. */
    public void apply(ItemStack item, String modelPath) {
        Objects.requireNonNull(item, "item");
        ItemMeta meta = item.getItemMeta();
        apply(meta, modelPath);
        item.setItemMeta(meta);
    }

    /** Fills every menu slot with the common ornamental Valoria background tile. */
    public void fillMenu(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        if (!enabled()) {
            return;
        }
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        apply(meta, "ui/filler");
        filler.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    /** Returns the namespaced client-model key for one validated logical path. */
    public NamespacedKey key(String modelPath) {
        String normalized = normalizePath(modelPath);
        return keys.computeIfAbsent(normalized, path -> Objects.requireNonNull(
                NamespacedKey.fromString(settings.namespace() + ':' + path),
                "Invalid generated item-model key"
        ));
    }

    /** Converts enum/config identifiers into safe lowercase resource-pack path segments. */
    public static String segment(Object identifier) {
        String value = Objects.requireNonNull(identifier, "identifier")
                .toString()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
        if (!value.matches("[a-z0-9._]+")) {
            throw new IllegalArgumentException("Invalid model path segment: " + value);
        }
        return value;
    }

    private String normalizePath(String modelPath) {
        String value = Objects.requireNonNull(modelPath, "modelPath")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!MODEL_PATH.matcher(value).matches()
                || value.startsWith("/")
                || value.endsWith("/")
                || value.contains("//")
                || value.contains("..")) {
            throw new IllegalArgumentException("Invalid item-model path: " + modelPath);
        }
        return value;
    }
}
