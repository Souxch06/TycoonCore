/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.lone.itemsadder.api.CustomStack
 *  io.th0rgal.oraxen.api.OraxenItems
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package xyz.arcadiadevs.gensplus.utils;

import com.awaitquality.api.spigot.chat.formatter.Formatter;
import com.cryptomorin.xseries.XMaterial;
import dev.lone.itemsadder.api.CustomStack;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.th0rgal.oraxen.api.OraxenItems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.WandData;
import xyz.arcadiadevs.gensplus.utils.ServerVersion;
import xyz.arcadiadevs.guilib.ItemBuilder;

public class ItemUtil {
    public static ItemStack getUniversalItem(String string, boolean bl, boolean bl2) {
        if (string.toLowerCase().startsWith("oraxen:")) {
            string = string.substring(7);
            return OraxenItems.getItemById((String)string).build();
        }
        if (bl && string.toLowerCase().startsWith("itemsadder:")) {
            CustomStack customStack = CustomStack.getInstance((String)(string = string.substring(11)));
            if (customStack == null) {
                return null;
            }
            return customStack.getItemStack();
        }
        if (string.toLowerCase().startsWith("customid:")) {
            string = string.substring(9);
            String[] stringArray = string.split(";");
            String string2 = stringArray[0];
            String string3 = stringArray[1];
            ItemStack itemStack = XMaterial.matchXMaterial(string3).orElseThrow().parseItem();
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_16)) {
                itemMeta.setCustomModelData(Integer.valueOf(Integer.parseInt(string2)));
            }
            itemStack.setItemMeta(itemMeta);
            return itemStack;
        }
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        if (bl2 && string.startsWith("head:")) {
            SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer((String)string.substring(5)));
            itemStack.setItemMeta((ItemMeta)skullMeta);
            return itemStack;
        }
        XMaterial xMaterial = XMaterial.matchXMaterial(string).orElse(null);
        if (xMaterial == null) {
            return null;
        }
        return xMaterial.parseItem();
    }

    public static void upgradeGens(Inventory inventory) {
        int n = 0;
        while (n < inventory.getSize()) {
            List list;
            ItemMeta itemMeta;
            ItemStack itemStack = inventory.getItem(n);
            if (itemStack != null && (itemMeta = itemStack.getItemMeta()) != null && itemMeta.hasLore() && (list = itemMeta.getLore()) != null) {
                String string = (String)list.get(0);
                if (string.contains("Generator drop tier")) {
                    var6_6 = Integer.parseInt(string.split(" ")[3]);
                    list.remove(0);
                    itemMeta.setLore(list);
                    itemStack.setItemMeta(itemMeta);
                    itemStack = NBTEditor.set(itemStack, var6_6, "gensplus", "spawnitem", "tier");
                    inventory.setItem(n, itemStack);
                } else if (string.contains("Generator tier")) {
                    var6_6 = Integer.parseInt(string.split(" ")[2]);
                    list.remove(0);
                    itemMeta.setLore(list);
                    itemStack.setItemMeta(itemMeta);
                    itemStack = NBTEditor.set(itemStack, var6_6, "gensplus", "blocktype", "tier");
                    inventory.setItem(n, itemStack);
                }
            }
            ++n;
        }
    }

    public static ItemStack getWand(WandData.Wand.WandType wandType, int n, double d) {
        WandData wandData = GensPlus.getInstance().getWandData();
        WandData.Wand wand = wandData.create(wandType, n, d);
        String string = "wands.sell-wand";
        FileConfiguration fileConfiguration = GensPlus.getInstance().getConfig();
        List list = fileConfiguration.getStringList(string + ".lore");
        List<String> list2 = Formatter.format(wand, list);
        Material material = XMaterial.matchXMaterial(fileConfiguration.getString(string + ".material")).orElseThrow().parseMaterial();
        String string2 = Formatter.format(wand, fileConfiguration.getString(string + ".name"));
        ItemBuilder itemBuilder = new ItemBuilder(material).name(string2).lore(list2);
        ItemStack itemStack = itemBuilder.build();
        String string3 = "sell-wand-uuid";
        itemStack = NBTEditor.set(itemStack, wand.getUuid().toString(), new Object[]{NBTEditor.CUSTOM_DATA, string3});
        return itemStack;
    }

    public static ItemStack getSellWand(int n, double d) {
        return ItemUtil.getWand(WandData.Wand.WandType.SELL_WAND, n, d);
    }
}

