package fr.valoriatycoon.pets;

import fr.valoriatycoon.config.MessageService;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/** Prevents authenticated physical keys from being placed, crafted or accidentally consumed. */
public final class PetKeyListener implements Listener {
    private final PetKeyService keys;
    private final MessageService messages;

    public PetKeyListener(PetKeyService keys, MessageService messages) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (keys.keyId(event.getItem()).isPresent()) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "pets.key-use-menu");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (keys.keyId(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "pets.key-use-menu");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (containsKey(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!containsKey(event.getInventory().getMatrix())) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            messages.send(player, "pets.key-use-menu");
        }
    }

    private boolean containsKey(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (keys.keyId(item).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
