package xyz.arcadiadevs.valoriatycoon.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import xyz.arcadiadevs.valoriatycoon.utils.SellUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public class OnInventoryClose
implements Listener {
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {
        Inventory inventory = inventoryCloseEvent.getInventory();
        Player player = (Player)inventoryCloseEvent.getPlayer();
        if (inventoryCloseEvent.getView().getTitle().equals(Config.GUIS_SELL_GUI_TITLE.getString())) {
            SellUtil.sellAll(player, inventory, true);
        }
    }
}

