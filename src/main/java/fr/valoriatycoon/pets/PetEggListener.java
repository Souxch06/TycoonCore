package fr.valoriatycoon.pets;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.ranks.RankSettings;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Redeems pet eggs once and prevents every non-pet use of authenticated eggs. */
public final class PetEggListener implements Listener {
    private final PetService pets;
    private final PetEggService eggs;
    private final RankSettings ranks;
    private final MessageService messages;
    private final Executor mainThread;
    private final Set<UUID> operations = ConcurrentHashMap.newKeySet();

    public PetEggListener(
            PetService pets,
            PetEggService eggs,
            RankSettings ranks,
            MessageService messages,
            Executor mainThread
    ) {
        this.pets = Objects.requireNonNull(pets, "pets");
        this.eggs = Objects.requireNonNull(eggs, "eggs");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        PetEggService.EggToken token = eggs.token(event.getItem()).orElse(null);
        if (token == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !operations.add(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        pets.redeemEgg(player.getUniqueId(), token)
                .whenCompleteAsync(
                        (result, error) -> complete(player, token, result, error),
                        mainThread
                );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (eggs.token(event.getItem()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (eggs.token(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (containsEgg(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (containsEgg(event.getInventory().getMatrix())) {
            event.setCancelled(true);
        }
    }

    private void complete(
            Player player,
            PetEggService.EggToken token,
            PetOperationResult result,
            Throwable error
    ) {
        operations.remove(player.getUniqueId());
        if (error != null || result == null) {
            messages.send(player, "errors.storage");
            return;
        }
        if (result.status() == PetOperationStatus.SUCCESS && result.pet() != null) {
            eggs.removeConsumed(player, token.eggId());
            PetDefinition definition = pets.settings().pet(result.pet().petId());
            messages.send(
                    player,
                    "pets.egg-redeemed",
                    Placeholder.component("pet", messages.render(definition.displayName())),
                    Placeholder.unparsed(
                            "variant",
                            result.pet().chromatic() ? "chromatique" : "normal"
                    )
            );
        } else if (result.status() == PetOperationStatus.EGG_ALREADY_USED
                || result.status() == PetOperationStatus.INVALID_EGG) {
            eggs.removeConsumed(player, token.eggId());
            messages.send(player, "pets.egg-invalid");
        } else if (result.status() == PetOperationStatus.ALREADY_OWNED) {
            messages.send(player, "pets.egg-already-owned");
        } else if (result.status() == PetOperationStatus.RANK_LOCKED) {
            messages.send(
                    player,
                    "pets.rank-locked",
                    Placeholder.unparsed("rank", ranks.name(result.requiredRank()))
            );
        } else if (result.status() == PetOperationStatus.NO_ACTIVE_ISLAND) {
            messages.send(player, "tycoon.not-ready");
        } else {
            messages.send(player, "pets.unavailable");
        }
    }

    private boolean containsEgg(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (eggs.token(item).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
