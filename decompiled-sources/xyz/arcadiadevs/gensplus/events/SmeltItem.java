/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.BlockState
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryMoveItemEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.gensplus.events;

import com.cryptomorin.xseries.XMaterial;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import java.util.Arrays;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.gensplus.utils.PlayerUtil;
import xyz.arcadiadevs.gensplus.utils.ServerVersion;
import xyz.arcadiadevs.gensplus.utils.config.Config;

public class SmeltItem
implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
        if (Config.CAN_DROPS_BE_USED_IN_SMELTING.getBoolean()) {
            return;
        }
        if (inventoryClickEvent.getClickedInventory() == null) {
            return;
        }
        if (!this.isGensItem(inventoryClickEvent.getCurrentItem())) {
            return;
        }
        InventoryType[] inventoryTypeArray = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_14) ? new InventoryType[]{InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER} : new InventoryType[]{InventoryType.FURNACE};
        if (!Arrays.asList(inventoryTypeArray).contains(inventoryClickEvent.getView().getTopInventory().getType())) {
            return;
        }
        inventoryClickEvent.setCancelled(true);
    }

    @EventHandler
    public void onHopperTransfer(InventoryMoveItemEvent inventoryMoveItemEvent) {
        InventoryType[] inventoryTypeArray;
        if (Config.CAN_DROPS_BE_USED_IN_SMELTING.getBoolean()) {
            return;
        }
        if (inventoryMoveItemEvent.getSource().getType() == InventoryType.PLAYER) {
            inventoryTypeArray = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_14) ? new InventoryType[]{InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER} : new InventoryType[]{InventoryType.FURNACE};
            if (!Arrays.asList(inventoryTypeArray).contains(inventoryMoveItemEvent.getDestination().getType())) {
                return;
            }
            InventoryHolder inventoryHolder = inventoryMoveItemEvent.getDestination().getHolder();
            if (!(inventoryHolder instanceof BlockState)) {
                return;
            }
            if (!this.isGensItem(inventoryMoveItemEvent.getItem())) {
                return;
            }
            inventoryMoveItemEvent.setCancelled(true);
        }
        if (inventoryMoveItemEvent.getSource().getType() == InventoryType.HOPPER) {
            inventoryTypeArray = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_14) ? new InventoryType[]{InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER} : new InventoryType[]{InventoryType.FURNACE};
            if (!Arrays.asList(inventoryTypeArray).contains(inventoryMoveItemEvent.getDestination().getType())) {
                return;
            }
            inventoryMoveItemEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent inventoryDragEvent) {
        if (Config.CAN_DROPS_BE_USED_IN_SMELTING.getBoolean()) {
            return;
        }
        InventoryType[] inventoryTypeArray = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_14) ? new InventoryType[]{InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER} : new InventoryType[]{InventoryType.FURNACE};
        if (!Arrays.asList(inventoryTypeArray).contains(inventoryDragEvent.getView().getTopInventory().getType())) {
            return;
        }
        inventoryDragEvent.getRawSlots().forEach(n -> {
            if (n < inventoryDragEvent.getView().getTopInventory().getSize()) {
                if (!this.isGensItem(inventoryDragEvent.getOldCursor())) {
                    return;
                }
                inventoryDragEvent.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onCampfireClick(PlayerInteractEvent playerInteractEvent) {
        if (Config.CAN_DROPS_BE_USED_IN_SMELTING.getBoolean()) {
            return;
        }
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_14) || playerInteractEvent.getClickedBlock() == null || playerInteractEvent.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (playerInteractEvent.getClickedBlock().getType() == XMaterial.SHORT_GRASS.parseMaterial()) {
            return;
        }
        if (XMaterial.CAMPFIRE.parseMaterial() != XMaterial.matchXMaterial(playerInteractEvent.getClickedBlock().getType()).parseMaterial()) {
            return;
        }
        Player player = playerInteractEvent.getPlayer();
        if (this.isGensItem(PlayerUtil.getHeldItem(player)) && PlayerUtil.getHeldItem(player) != null && PlayerUtil.getHeldItem(player) != XMaterial.AIR.parseItem()) {
            playerInteractEvent.setCancelled(true);
        } else if (PlayerUtil.getOffHeldItem(player) != null && PlayerUtil.getOffHeldItem(player) != XMaterial.AIR.parseItem()) {
            playerInteractEvent.setCancelled(true);
        }
    }

    private boolean isGensItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == XMaterial.AIR.parseMaterial()) {
            return false;
        }
        return NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "spawnitem", "tier"}) || NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"});
    }
}

