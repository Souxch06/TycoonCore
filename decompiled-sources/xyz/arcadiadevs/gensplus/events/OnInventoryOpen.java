/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.inventory.Inventory
 */
package xyz.arcadiadevs.gensplus.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import xyz.arcadiadevs.gensplus.utils.ItemUtil;

public class OnInventoryOpen
implements Listener {
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent inventoryOpenEvent) {
        Player player = (Player)inventoryOpenEvent.getPlayer();
        ItemUtil.upgradeGens((Inventory)player.getInventory());
        ItemUtil.upgradeGens(inventoryOpenEvent.getInventory());
    }
}

