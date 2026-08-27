/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.enchantment.EnchantItemEvent
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.gensplus.events;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.gensplus.utils.config.Config;

public class EnchantItem
implements Listener {
    @EventHandler
    public void onItemEnchant(EnchantItemEvent enchantItemEvent) {
        ItemStack itemStack;
        if (Config.CAN_DROPS_BE_USED_IN_ENCHANTING.getBoolean()) {
            return;
        }
        ItemStack itemStack2 = enchantItemEvent.getItem();
        if (NBTEditor.contains(itemStack2, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "spawnitem", "tier"}) || NBTEditor.contains(itemStack2, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"})) {
            enchantItemEvent.setCancelled(true);
        }
        if ((itemStack = enchantItemEvent.getView().getItem(1)) != null && (NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "spawnitem", "tier"}) || NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"}))) {
            enchantItemEvent.setCancelled(true);
        }
    }
}

