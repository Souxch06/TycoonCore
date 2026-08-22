package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.farm.FarmSettings;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Dedicated /autosell panel containing only status and level progression controls. */
public final class AutoSellPanel implements Listener {
    private final FarmSettings.AutoSellMenu settings;
    private final AutoSellMenuController controller;
    private final MessageService messages;

    public AutoSellPanel(
            FarmSettings.AutoSellMenu settings,
            AutoSellMenuController controller,
            MessageService messages
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void open(Player player) {
        AutoSellPanelHolder holder = new AutoSellPanelHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                settings.size(),
                messages.render(settings.title())
        );
        holder.bind(inventory);
        controller.populateToggle(inventory, player);
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AutoSellPanelHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= settings.size()
                || !controller.handlesToggle(event.getRawSlot())) {
            return;
        }
        controller.handleToggleClick(player, () -> reopenIfViewing(player, holder));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AutoSellPanelHolder)) {
            return;
        }
        if (event.getRawSlots().stream().anyMatch(slot -> slot < settings.size())) {
            event.setCancelled(true);
        }
    }

    private void reopenIfViewing(Player player, AutoSellPanelHolder holder) {
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == holder) {
            open(player);
        }
    }
}
