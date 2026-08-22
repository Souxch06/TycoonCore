package fr.valoriatycoon.machines;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Creates and validates server-authenticated machine placement items. */
public final class MachineItemService {
    private final NamespacedKey typeKey;
    private final NamespacedKey markerKey;
    private final NamespacedKey legacyTypeKey;
    private final NamespacedKey legacyMarkerKey;
    private final ItemVisualService visuals;
    private final MessageService messages;

    public MachineItemService(
            JavaPlugin plugin,
            ItemVisualService visuals,
            MessageService messages
    ) {
        this.typeKey = new NamespacedKey(plugin, "machine_type");
        this.markerKey = new NamespacedKey(plugin, "machine_item");
        this.legacyTypeKey = new NamespacedKey("tycooncore", "machine_type");
        this.legacyMarkerKey = new NamespacedKey("tycooncore", "machine_item");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public ItemStack create(MachineDefinition definition) {
        ItemStack item = new ItemStack(definition.blockMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(definition.displayName()));
        List<Component> lore = new ArrayList<>();
        for (String line : definition.lore()) lore.add(messages.render(line));
        meta.lore(lore);
        visuals.apply(meta, "item/generator/" + ItemVisualService.segment(definition.id()));
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, definition.id());
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> machineType(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        String type = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (marker == null) {
            marker = meta.getPersistentDataContainer().get(legacyMarkerKey, PersistentDataType.BYTE);
        }
        if (type == null) {
            type = meta.getPersistentDataContainer().get(legacyTypeKey, PersistentDataType.STRING);
        }
        return marker != null && marker == 1 && type != null ? Optional.of(type) : Optional.empty();
    }
}
