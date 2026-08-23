package fr.valoriatycoon.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.ToIntFunction;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Applies account-bound efficiency without writing transferable enchantments onto item stacks. */
public final class ToolEffectService implements Listener {
    private final JavaPlugin plugin;
    private final ToolProgressionService progression;
    private final MultiToolItemService items;
    private final ToIntFunction<UUID> currentRank;
    private final BooleanSupplier available;
    private final NamespacedKey efficiencyKey;
    private final NamespacedKey legacyEfficiencyKey;
    private final Set<UUID> pendingRefreshes = ConcurrentHashMap.newKeySet();

    public ToolEffectService(
            JavaPlugin plugin,
            ToolProgressionService progression,
            MultiToolItemService items,
            ToIntFunction<UUID> currentRank,
            BooleanSupplier available
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.items = Objects.requireNonNull(items, "items");
        this.currentRank = Objects.requireNonNull(currentRank, "currentRank");
        this.available = Objects.requireNonNull(available, "available");
        this.efficiencyKey = new NamespacedKey(plugin, "tool_efficiency");
        this.legacyEfficiencyKey = new NamespacedKey("tycooncore", "tool_efficiency");
    }

    public void stop() {
        pendingRefreshes.clear();
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            items.release(player.getUniqueId());
            removeModifier(player);
        });
    }

    public void releasePlayer(UUID playerId) {
        pendingRefreshes.remove(playerId);
        items.release(playerId);
    }

    public void refresh(Player player) {
        if (!available.getAsBoolean() || !progression.settings().multiTool().enabled()) {
            return;
        }
        synchronizeHeldMultiTool(player);
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.getModifiers().stream()
                .filter(this::isOwnedEfficiencyModifier)
                .toList()
                .forEach(attribute::removeModifier);

        org.bukkit.inventory.ItemStack held = player.getInventory().getItemInMainHand();
        ToolType type = ToolType.fromMaterial(held.getType()).orElse(null);
        if (type == null
                || type == ToolType.FISHING_ROD
                || !items.isOwnedBy(held, player.getUniqueId())) {
            return;
        }
        double bonus = progression.efficiencyBonus(player.getUniqueId(), type)
                .min(progression.settings().abilities().efficiencyHardCap())
                .doubleValue();
        if (bonus <= 0) {
            return;
        }
        attribute.addTransientModifier(new AttributeModifier(
                efficiencyKey,
                bonus,
                AttributeModifier.Operation.ADD_SCALAR
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFishingStarted(PlayerFishEvent event) {
        if (!available.getAsBoolean() || event.getState() != PlayerFishEvent.State.FISHING) {
            return;
        }
        org.bukkit.inventory.ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (ToolType.fromMaterial(held.getType()).orElse(null) != ToolType.FISHING_ROD
                || !items.isOwnedBy(held, event.getPlayer().getUniqueId())) {
            return;
        }
        BigDecimal speedBonus = progression.efficiencyBonus(
                event.getPlayer().getUniqueId(),
                ToolType.FISHING_ROD
        ).min(progression.settings().abilities().efficiencyHardCap());
        BigDecimal divisor = BigDecimal.ONE.add(speedBonus);
        int minimum = BigDecimal.valueOf(100).divide(divisor, 0, RoundingMode.CEILING).intValueExact();
        int maximum = BigDecimal.valueOf(600).divide(divisor, 0, RoundingMode.CEILING).intValueExact();
        event.getHook().setWaitTime(Math.max(1, minimum), Math.max(minimum, maximum));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        pendingRefreshes.remove(event.getPlayer().getUniqueId());
        items.release(event.getPlayer().getUniqueId());
        removeModifier(event.getPlayer());
    }

    private void synchronizeHeldMultiTool(Player player) {
        items.ensureSingle(player, currentRank.applyAsInt(player.getUniqueId()));
    }

    private void scheduleRefresh(Player player) {
        UUID playerId = player.getUniqueId();
        if (!pendingRefreshes.add(playerId)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pendingRefreshes.remove(playerId);
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }

    private boolean isOwnedEfficiencyModifier(AttributeModifier modifier) {
        return modifier.getKey().equals(efficiencyKey)
                || modifier.getKey().equals(legacyEfficiencyKey);
    }

    private void removeModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.getModifiers().stream()
                .filter(this::isOwnedEfficiencyModifier)
                .toList()
                .forEach(attribute::removeModifier);
    }
}
