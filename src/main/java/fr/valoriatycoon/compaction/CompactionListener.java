package fr.valoriatycoon.compaction;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Validates recursive compact crafts and owns interaction with the spawn decompactor. */
public final class CompactionListener implements Listener {
    private final CompactionService compaction;

    public CompactionListener(CompactionService compaction) {
        this.compaction = Objects.requireNonNull(compaction, "compaction");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Material material = compaction.recipeMaterial(event.getRecipe()).orElse(null);
        if (material == null) {
            if (containsCompacted(event.getInventory().getMatrix())) {
                event.getInventory().setResult(null);
            }
            return;
        }
        event.getInventory().setResult(compaction.craftResult(
                event.getInventory().getMatrix(),
                material
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Material material = compaction.recipeMaterial(event.getRecipe()).orElse(null);
        if (material == null) {
            if (containsCompacted(event.getInventory().getMatrix())) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    compaction.notifyCannotUse(player);
                }
            }
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                compaction.notifyCraftOneAtTime(player);
            }
            return;
        }
        ItemStack result = compaction.craftResult(event.getInventory().getMatrix(), material);
        if (result == null) {
            event.setCancelled(true);
            event.setCurrentItem(null);
            return;
        }
        event.setCurrentItem(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCookCompactedItem(BlockCookEvent event) {
        if (compaction.isCompacted(event.getSource())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUseCompactedItem(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && compaction.isCompacted(event.getItem())) {
            event.setCancelled(true);
            compaction.notifyCannotUse(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceCompactedItem(BlockPlaceEvent event) {
        if (compaction.isCompacted(event.getItemInHand())) {
            event.setCancelled(true);
            compaction.notifyCannotUse(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsumeCompactedItem(PlayerItemConsumeEvent event) {
        if (compaction.isCompacted(event.getItem())) {
            event.setCancelled(true);
            compaction.notifyCannotUse(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        compaction.discoverRecipes(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (compaction.isNpc(event.getRightClicked())) {
            event.setCancelled(true);
            compaction.decompactHeldItem(player);
        } else if (compaction.isCompacted(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            compaction.notifyCannotUse(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcDamage(EntityDamageEvent event) {
        if (compaction.isNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcLeash(PlayerLeashEntityEvent event) {
        if (compaction.isNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean containsCompacted(ItemStack[] items) {
        for (ItemStack item : items) {
            if (compaction.isCompacted(item)) {
                return true;
            }
        }
        return false;
    }
}
