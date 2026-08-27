package xyz.arcadiadevs.valoriatycoon.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import xyz.arcadiadevs.valoriatycoon.utils.ItemUtil;

public class OnInventoryOpen
implements Listener {
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent inventoryOpenEvent) {
        Player player = (Player)inventoryOpenEvent.getPlayer();
        ItemUtil.upgradeGens((Inventory)player.getInventory());
        ItemUtil.upgradeGens(inventoryOpenEvent.getInventory());
    }
}

