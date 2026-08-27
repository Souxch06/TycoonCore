package xyz.arcadiadevs.valoriatycoon.events;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public class CraftItem
implements Listener {
    @EventHandler
    public void onItemCraft(CraftItemEvent craftItemEvent) {
        ItemStack[] itemStackArray;
        if (Config.CAN_DROPS_BE_USED_IN_CRAFTING.getBoolean()) {
            return;
        }
        ItemStack[] itemStackArray2 = itemStackArray = craftItemEvent.getInventory().getMatrix();
        int n = itemStackArray.length;
        int n2 = 0;
        while (n2 < n) {
            ItemStack itemStack = itemStackArray2[n2];
            if (itemStack != null && (NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"}) || NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "blocktype", "tier"}))) {
                craftItemEvent.setResult(Event.Result.DENY);
                craftItemEvent.setCancelled(true);
                return;
            }
            ++n2;
        }
    }
}

