package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.warps.WarpDefinition;
import fr.valoriatycoon.warps.WarpService;
import fr.valoriatycoon.warps.WarpTeleportAttempt;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Configurable warp selector that delegates every destination to WarpService. */
public final class WarpPanel implements Listener {
    private final WarpService warps;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;

    public WarpPanel(
            WarpService warps,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.warps = Objects.requireNonNull(warps, "warps");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void open(Player player) {
        WarpPanelHolder holder = new WarpPanelHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                warps.settings().menu().size(),
                messages.render(warps.settings().menu().title())
        );
        holder.bind(inventory);
        ItemStack filler = item(
                warps.settings().menu().filler(),
                " ",
                List.of(),
                "ui/filler"
        );
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        for (WarpDefinition warp : warps.warps()) {
            inventory.setItem(warp.menuSlot(), item(
                    warp.icon(),
                    warp.displayName(),
                    warp.lore(),
                    warp.itemModel()
            ));
        }
        player.openInventory(inventory);
    }

    public void teleport(Player player, WarpDefinition warp) {
        WarpTeleportAttempt attempt = warps.teleport(player, warp);
        if (!attempt.accepted()) {
            if (attempt.waitSeconds() > 0L) {
                messages.send(
                        player,
                        "warps.cooldown",
                        Placeholder.unparsed("seconds", Long.toString(attempt.waitSeconds()))
                );
            } else {
                messages.send(player, "warps.unavailable");
            }
            return;
        }
        player.closeInventory();
        attempt.completion().whenCompleteAsync((success, error) -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                messages.send(player, "warps.failed");
            } else {
                messages.send(
                        player,
                        "warps.teleported",
                        Placeholder.component("warp", messages.render(warp.displayName()))
                );
            }
        }, mainThread);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WarpPanelHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        warps.warps().stream()
                .filter(warp -> warp.menuSlot() == event.getRawSlot())
                .findFirst()
                .ifPresent(warp -> teleport(player, warp));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WarpPanelHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack item(
            org.bukkit.Material material,
            String name,
            List<String> lore,
            String modelPath
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name));
        List<Component> rendered = lore.stream().map(messages::render).toList();
        meta.lore(rendered);
        visuals.apply(meta, modelPath);
        item.setItemMeta(meta);
        return item;
    }
}
