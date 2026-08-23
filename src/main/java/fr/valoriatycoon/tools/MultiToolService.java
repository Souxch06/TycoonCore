package fr.valoriatycoon.tools;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.farm.FarmWorldService;
import fr.valoriatycoon.quests.QuestService;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.ToIntFunction;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

/** Morphs one held, tiered multi-tool between pickaxe, axe, hoe and fishing-rod forms. */
public final class MultiToolService implements Listener {
    private final ToolSettings.MultiTool settings;
    private final ToolProgressionService progression;
    private final FarmWorldService farmWorlds;
    private final QuestService quests;
    private final ToolEffectService effects;
    private final MultiToolItemService items;
    private final ToIntFunction<UUID> currentRank;
    private final BooleanSupplier available;
    private final MessageService messages;

    public MultiToolService(
            ToolSettings.MultiTool settings,
            ToolProgressionService progression,
            FarmWorldService farmWorlds,
            QuestService quests,
            ToolEffectService effects,
            MultiToolItemService items,
            ToIntFunction<UUID> currentRank,
            BooleanSupplier available,
            MessageService messages
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.farmWorlds = Objects.requireNonNull(farmWorlds, "farmWorlds");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.items = Objects.requireNonNull(items, "items");
        this.currentRank = Objects.requireNonNull(currentRank, "currentRank");
        this.available = Objects.requireNonNull(available, "available");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!settings.enabled() || !available.getAsBoolean() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if ((event.getAction() == Action.LEFT_CLICK_BLOCK) && event.getClickedBlock() != null) {
            ToolBlockResolver.resolve(event.getClickedBlock().getType())
                    .ifPresent(type -> transform(event.getPlayer(), type));
            return;
        }
        if (!event.getPlayer().isSneaking()
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && targetsWater(event.getPlayer(), event.getClickedBlock())) {
            if (transform(event.getPlayer(), ToolType.FISHING_ROD)) {
                // The first click changes form; the next click casts with normal Minecraft behavior.
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!available.getAsBoolean()) {
            return;
        }
        ToolType required = ToolBlockResolver.resolve(event.getBlock().getType()).orElse(null);
        ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
        ToolType held = ToolType.fromMaterial(heldItem.getType()).orElse(null);
        if (required != null
                && held == required
                && items.isOwnedBy(heldItem, event.getPlayer().getUniqueId())) {
            progression.queueActionRewards(
                    event.getPlayer().getUniqueId(),
                    held,
                    farmWorlds.farm(event.getBlock().getWorld()).isPresent()
            );
            quests.recordToolAction(event.getPlayer().getUniqueId(), held);
        }
    }

    public boolean transform(Player player, ToolType targetType) {
        if (!available.getAsBoolean()) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        int rank = currentRank.applyAsInt(playerId);
        items.ensureSingle(player, rank);
        ItemStack current = player.getInventory().getItemInMainHand();
        ToolType currentType = ToolType.fromMaterial(current.getType()).orElse(null);
        if (currentType == null || currentType == targetType) {
            return false;
        }
        ItemStack transformed = items.transform(current, targetType, rank, playerId).orElse(null);
        if (transformed == null) {
            return false;
        }
        player.getInventory().setItemInMainHand(transformed);
        effects.refresh(player);
        if (settings.notifySwitch()) {
            messages.send(
                    player,
                    "tools.multitool-switched",
                    Placeholder.unparsed("tool", displayName(targetType))
            );
        }
        return true;
    }

    private boolean targetsWater(Player player, Block clickedBlock) {
        if (clickedBlock != null && clickedBlock.getType() == Material.WATER) {
            return true;
        }
        RayTraceResult trace = player.rayTraceBlocks(
                settings.fishingRayDistance(),
                FluidCollisionMode.ALWAYS
        );
        return trace != null && trace.getHitBlock() != null && trace.getHitBlock().getType() == Material.WATER;
    }

    private String displayName(ToolType type) {
        return switch (type) {
            case PICKAXE -> "pioche";
            case AXE -> "hache";
            case HOE -> "houe";
            case FISHING_ROD -> "canne à pêche";
        };
    }
}
