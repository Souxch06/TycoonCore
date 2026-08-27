/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.valoriatycoon.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.WandData;
import xyz.arcadiadevs.valoriatycoon.models.events.ActiveEvent;
import xyz.arcadiadevs.valoriatycoon.models.events.SellEvent;
import xyz.arcadiadevs.valoriatycoon.tasks.EventLoop;
import xyz.arcadiadevs.valoriatycoon.utils.PlayerUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public class SellUtil {
    public static void sellAll(Player player, Inventory inventory, boolean ... blArray) {
        int n = 0;
        ActiveEvent activeEvent = EventLoop.getActiveEvent();
        double d = activeEvent.event() instanceof SellEvent ? (double)activeEvent.event().getMultiplier() * PlayerUtil.getMultiplier(player) : 1.0 * PlayerUtil.getMultiplier(player);
        int n2 = 0;
        while (n2 < inventory.getSize()) {
            ItemStack itemStack = inventory.getItem(n2);
            if (itemStack != null) {
                if (NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"})) {
                    int n3 = NBTEditor.getInt(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"});
                    GeneratorsData generatorsData = ValoriaTycoon.getInstance().getGeneratorsData();
                    GeneratorsData.Generator generator = generatorsData.getGenerator(n3);
                    double d2 = itemStack.getAmount();
                    double d3 = generator.sellPrice();
                    double d4 = d3 * d2 * d;
                    n = (int)((double)n + d4);
                    inventory.setItem(n2, null);
                } else if (blArray.length > 0 && blArray[0]) {
                    player.getInventory().addItem(new ItemStack[]{itemStack});
                }
            }
            ++n2;
        }
        if (n <= 0) {
            Messages.NOTHING_TO_SELL.format(new Object[0]).send((CommandSender)player);
            return;
        }
        Economy economy = ValoriaTycoon.getInstance().getEcon();
        economy.depositPlayer((OfflinePlayer)player, (double)n);
        Messages.SUCCESSFULLY_SOLD.format("price", economy.format((double)n)).send((CommandSender)player);
    }

    public static void sellWand(Player player, Inventory inventory, double d, WandData.Wand wand) {
        int n = 0;
        ActiveEvent activeEvent = EventLoop.getActiveEvent();
        double d2 = (activeEvent.event() instanceof SellEvent ? (double)activeEvent.event().getMultiplier() * PlayerUtil.getMultiplier(player) : 1.0 * PlayerUtil.getMultiplier(player)) * d;
        long l = 0L;
        int n2 = 0;
        while (n2 < inventory.getSize()) {
            ItemStack itemStack = inventory.getItem(n2);
            if (itemStack != null && NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"})) {
                int n3 = NBTEditor.getInt(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"});
                GeneratorsData generatorsData = ValoriaTycoon.getInstance().getGeneratorsData();
                GeneratorsData.Generator generator = generatorsData.getGenerator(n3);
                long l2 = itemStack.getAmount();
                l += l2;
                double d3 = generator.sellPrice();
                double d4 = d3 * (double)l2 * d2;
                n += (int)d4;
                inventory.removeItem(new ItemStack[]{itemStack});
            }
            ++n2;
        }
        if (n <= 0) {
            Messages.NOTHING_TO_SELL.format(new Object[0]).send((CommandSender)player);
            return;
        }
        Economy economy = ValoriaTycoon.getInstance().getEcon();
        wand.setUses(wand.getUses() <= -1 ? -1 : wand.getUses() - 1);
        wand.setTotalEarned(wand.getTotalEarned() + (long)n);
        wand.setTotalItemsSold(wand.getTotalItemsSold() + l);
        economy.depositPlayer((OfflinePlayer)player, (double)n);
        Messages.SUCCESSFULLY_SOLD.format("price", economy.format((double)n), "amount", String.valueOf(l)).send((CommandSender)player);
        Messages.SUCCESSFULLY_SOLD.format("price", economy.format((double)n), "amount", String.valueOf(l)).sendInActionBar(player);
    }

    public static void sellHand(Player player) {
        boolean bl;
        ActiveEvent activeEvent = EventLoop.getActiveEvent();
        double d = activeEvent.event() instanceof SellEvent ? (double)activeEvent.event().getMultiplier() * PlayerUtil.getMultiplier(player) : 1.0 * PlayerUtil.getMultiplier(player);
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        boolean bl2 = bl = itemStack.getType() == Material.AIR;
        if (!NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"}) || bl) {
            Messages.NOTHING_TO_SELL.format(new Object[0]).send((CommandSender)player);
            return;
        }
        int n = NBTEditor.getInt(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"});
        GeneratorsData generatorsData = ValoriaTycoon.getInstance().getGeneratorsData();
        GeneratorsData.Generator generator = generatorsData.getGenerator(n);
        double d2 = itemStack.getAmount();
        double d3 = generator.sellPrice();
        double d4 = d3 * d2 * d;
        player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        Economy economy = ValoriaTycoon.getInstance().getEcon();
        economy.depositPlayer((OfflinePlayer)player, d4);
        Messages.SUCCESSFULLY_SOLD.format("price", economy.format(d4)).send((CommandSender)player);
    }
}

