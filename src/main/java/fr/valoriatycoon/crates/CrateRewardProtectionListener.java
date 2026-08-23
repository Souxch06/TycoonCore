package fr.valoriatycoon.crates;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/** Blocks vanilla mechanics that could alter or consume an authenticated crate reward token. */
public final class CrateRewardProtectionListener implements Listener {
    private final CrateRewardItemService items;

    public CrateRewardProtectionListener(CrateRewardItemService items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (items.token(event.getItem()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (items.token(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (items.token(event.getItem()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBundleInteraction(InventoryClickEvent event) {
        if ((event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)
                && (items.token(event.getCurrentItem()).isPresent()
                || items.token(event.getCursor()).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (contains(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (contains(event.getInventory().getMatrix())) {
            event.setCancelled(true);
        }
    }

    private boolean contains(ItemStack[] itemsToCheck) {
        for (ItemStack item : itemsToCheck) {
            if (items.token(item).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
