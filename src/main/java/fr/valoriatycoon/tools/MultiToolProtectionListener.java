package fr.valoriatycoon.tools;

import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/** Keeps the account-bound multi-tool unique, soulbound and outside external containers/recipes. */
public final class MultiToolProtectionListener implements Listener {
    private final MultiToolItemService items;

    public MultiToolProtectionListener(MultiToolItemService items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (items.isOwnedBy(event.getItemDrop().getItemStack(), event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID owner = items.owner(event.getItem().getItemStack()).orElse(null);
        if (owner != null && !owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!externalInventory(event.getView().getTopInventory().getType())) {
            return;
        }
        boolean involved = items.isMultiTool(event.getCurrentItem())
                || items.isMultiTool(event.getCursor());
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getHotbarButton() >= 0) {
                involved |= items.isMultiTool(player.getInventory().getItem(event.getHotbarButton()));
            }
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                involved |= items.isMultiTool(player.getInventory().getItemInOffHand());
            }
        }
        if (involved) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (externalInventory(event.getView().getTopInventory().getType())
                && items.isMultiTool(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (containsMultiTool(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (containsMultiTool(event.getInventory().getMatrix())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return;
        }
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack drop = drops.next();
            if (items.isOwnedBy(drop, event.getPlayer().getUniqueId())) {
                drops.remove();
                event.getItemsToKeep().add(drop);
            }
        }
    }

    private boolean containsMultiTool(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (items.isMultiTool(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean externalInventory(InventoryType type) {
        return type != InventoryType.CRAFTING && type != InventoryType.CREATIVE;
    }
}
