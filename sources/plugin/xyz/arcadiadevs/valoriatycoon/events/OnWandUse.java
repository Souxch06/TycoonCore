/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.Chest
 *  org.bukkit.block.Hopper
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package xyz.arcadiadevs.valoriatycoon.events;

import com.awaitquality.api.spigot.chat.formatter.Formatter;
import com.cryptomorin.xseries.XMaterial;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import java.util.List;
import java.util.UUID;
import lombok.Generated;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.arcadiadevs.valoriatycoon.models.WandData;
import xyz.arcadiadevs.valoriatycoon.utils.PlayerUtil;
import xyz.arcadiadevs.valoriatycoon.utils.SellUtil;
import xyz.arcadiadevs.valoriatycoon.utils.ServerVersion;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public class OnWandUse
implements Listener {
    private final WandData wandData;
    private final FileConfiguration config;

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onWandUse(PlayerInteractEvent playerInteractEvent) {
        Player player = playerInteractEvent.getPlayer();
        if (ServerVersion.isServerVersionAbove(ServerVersion.V1_8) && playerInteractEvent.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = playerInteractEvent.getClickedBlock();
        ItemStack itemStack = PlayerUtil.getHeldItem(player);
        if (block == null) {
            return;
        }
        if (itemStack == null || XMaterial.AIR.isSimilar(itemStack) || itemStack.getType() == Material.AIR) {
            return;
        }
        if (NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "sell-wand-uuid"})) {
            boolean bl = Config.SELL_WAND_ACTION_SNEAK.getBoolean();
            String string = Config.SELL_WAND_ACTION.getString();
            if (bl && !player.isSneaking() || playerInteractEvent.getAction() != Action.valueOf((String)string)) {
                return;
            }
            this.onSellWandUse(player, itemStack, block);
        }
    }

    public void onSellWandUse(Player player, ItemStack itemStack, Block block) {
        WandData.Wand wand = this.wandData.getWand(UUID.fromString(NBTEditor.getString(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "sell-wand-uuid"})));
        if (block.getType() == Material.CHEST || block.getType() == Material.HOPPER) {
            Object object;
            Inventory inventory = null;
            if (block.getType() == Material.CHEST) {
                object = (Chest)block.getState();
                inventory = object.getInventory();
            } else if (block.getType() == Material.HOPPER) {
                object = (Hopper)block.getState();
                inventory = object.getInventory();
            }
            if (inventory == null) {
                return;
            }
            if (inventory.getContents().length == 0) {
                Messages.NOTHING_TO_SELL.format(new Object[0]).send((CommandSender)player);
                return;
            }
            SellUtil.sellWand(player, inventory, wand.getMultiplier(), wand);
            if (wand.getUses() == 0) {
                player.getInventory().remove(itemStack);
                this.wandData.remove(wand.getUuid());
                Messages.WAND_BROKE.format(new Object[0]).send((CommandSender)player);
                return;
            }
            object = Formatter.format(wand, this.config.getStringList("wands.sell-wand.lore"));
            String string = Formatter.format(wand, this.config.getString("wands.sell-wand.name"));
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setLore((List)object);
            itemMeta.setDisplayName(string);
            itemStack.setItemMeta(itemMeta);
        }
    }

    @Generated
    public OnWandUse(WandData wandData, FileConfiguration fileConfiguration) {
        this.wandData = wandData;
        this.config = fileConfiguration;
    }
}

