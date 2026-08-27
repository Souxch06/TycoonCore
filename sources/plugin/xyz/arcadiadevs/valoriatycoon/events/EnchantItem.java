package xyz.arcadiadevs.valoriatycoon.events;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public class EnchantItem
implements Listener {
    @EventHandler
    public void onItemEnchant(EnchantItemEvent enchantItemEvent) {
        ItemStack itemStack;
        if (Config.CAN_DROPS_BE_USED_IN_ENCHANTING.getBoolean()) {
            return;
        }
        ItemStack itemStack2 = enchantItemEvent.getItem();
        if (NBTEditor.contains(itemStack2, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"}) || NBTEditor.contains(itemStack2, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "blocktype", "tier"})) {
            enchantItemEvent.setCancelled(true);
        }
        if ((itemStack = enchantItemEvent.getView().getItem(1)) != null && (NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"}) || NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "blocktype", "tier"}))) {
            enchantItemEvent.setCancelled(true);
        }
    }
}

