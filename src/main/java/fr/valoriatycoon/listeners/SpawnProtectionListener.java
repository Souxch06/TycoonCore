package fr.valoriatycoon.listeners;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.gui.FarmMenu;
import fr.valoriatycoon.spawn.SpawnWorldService;
import java.util.Objects;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Protects the generated hub, handles first join and connects its arches to public farms. */
public final class SpawnProtectionListener implements Listener {
    private final JavaPlugin plugin;
    private final SpawnWorldService spawn;
    private final FarmMenu farms;
    private final MessageService messages;

    public SpawnProtectionListener(
            JavaPlugin plugin,
            SpawnWorldService spawn,
            FarmMenu farms,
            MessageService messages
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.spawn = Objects.requireNonNull(spawn, "spawn");
        this.farms = Objects.requireNonNull(farms, "farms");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (spawn.settings().teleportOnFirstJoin() && !event.getPlayer().hasPlayedBefore()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> spawn.teleport(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null
                || !spawn.isProtected(event.getClickedBlock().getLocation())) {
            return;
        }
        var portal = spawn.portalAt(event.getClickedBlock().getLocation()).orElse(null);
        if (portal != null) {
            event.setCancelled(true);
            farms.openFarm(event.getPlayer(), portal.farmId());
        } else if (!event.getPlayer().hasPermission("tycoon.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !spawn.isSpawnWorld(event.getTo().getWorld())) {
            return;
        }
        if (event.getTo().getY() < spawn.settings().groundY() - 24) {
            spawn.teleport(event.getPlayer());
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        spawn.portalAt(event.getTo()).ifPresent(portal -> event.getPlayer().sendActionBar(
                messages.component(
                        "spawn.portal-hint",
                        false,
                        Placeholder.unparsed("farm", portal.displayName())
                )
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (protectedPlayer(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (protectedPlayer(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (protectedPlayer(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (protectedPlayer(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (spawn.isProtected(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (spawn.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (spawn.isProtected(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (spawn.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (spawn.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (spawn.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (spawn.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean protectedPlayer(Player player, org.bukkit.Location location) {
        return spawn.isProtected(location) && !player.hasPermission("tycoon.bypass");
    }
}
