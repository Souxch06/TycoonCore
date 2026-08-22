package fr.valoriatycoon.listeners;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.tycoon.HopperPosition;
import fr.valoriatycoon.tycoon.HopperReservation;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonWorldService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/** Fail-closed owner/member protection for generated Tycoon worlds and spacing. */
public final class TycoonProtectionListener implements Listener {
    private static final long MESSAGE_INTERVAL = TimeUnit.SECONDS.toNanos(1);

    private final TycoonService tycoons;
    private final TycoonWorldService worlds;
    private final MessageService messages;
    private final Map<UUID, Long> lastMessage = new HashMap<>();

    public TycoonProtectionListener(
            TycoonService tycoons,
            TycoonWorldService worlds,
            MessageService messages
    ) {
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        protect(event.getPlayer(), event.getBlock().getLocation(), event);
        if (!event.isCancelled() && event.getBlock().getType() == Material.HOPPER) {
            tycoons.releaseHopper(position(event.getBlock().getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Tycoon tycoon = protect(event.getPlayer(), event.getBlock().getLocation(), event);
        if (event.isCancelled() || tycoon == null || event.getBlock().getType() != Material.HOPPER) {
            return;
        }
        HopperReservation reservation = tycoons.reserveHopper(tycoon, position(event.getBlock().getLocation()));
        if (!reservation.accepted()) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(messages.component("tycoon.hopper-limit", false));
            return;
        }
        reservation.persistence().whenComplete((ignored, error) -> {
            if (error == null || event.getBlock().getType() != Material.HOPPER) {
                return;
            }
            event.getBlock().setType(Material.AIR, false);
            org.bukkit.inventory.ItemStack hopper = new org.bukkit.inventory.ItemStack(Material.HOPPER);
            if (event.getPlayer().isOnline()) {
                Map<Integer, org.bukkit.inventory.ItemStack> leftovers = event.getPlayer().getInventory().addItem(hopper);
                leftovers.values().forEach(item -> event.getBlock().getWorld()
                        .dropItemNaturally(event.getBlock().getLocation(), item));
                messages.send(event.getPlayer(), "errors.storage");
            } else {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), hopper);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlaceFinal(BlockPlaceEvent event) {
        if (event.isCancelled() && event.getBlock().getType() == Material.HOPPER) {
            tycoons.releaseHopper(position(event.getBlock().getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            protect(event.getPlayer(), event.getClickedBlock().getLocation(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        protect(event.getPlayer(), event.getRightClicked().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player player = null;
        if (event.getDamager() instanceof Player direct) {
            player = direct;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            player = shooter;
        }
        if (player != null) {
            protect(player, event.getEntity().getLocation(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            protect(player, event.getItem().getLocation(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        protect(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        protect(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Object holder = event.getInventory().getHolder();
        if (holder instanceof BlockState blockState) {
            protect(player, blockState.getLocation(), event);
        } else if (holder instanceof DoubleChest doubleChest) {
            protect(player, doubleChest.getLocation(), event);
        } else if (holder instanceof Entity entity) {
            protect(player, entity.getLocation(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null) {
            protect(event.getPlayer(), event.getEntity().getLocation(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            protect(player, event.getEntity().getLocation(), event);
        } else if (worlds.isTycoonWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (!worlds.isTycoonWorld(event.getBlock().getWorld())) {
            return;
        }
        Tycoon source = tycoons.at(
                event.getBlock().getWorld().getName(), event.getBlock().getX(), event.getBlock().getZ()
        ).orElse(null);
        Tycoon destination = tycoons.at(
                event.getToBlock().getWorld().getName(), event.getToBlock().getX(), event.getToBlock().getZ()
        ).orElse(null);
        if (source == null
                || destination == null
                || !source.id().equals(destination.id())
                || !tycoons.isInsideBuildArea(source, event.getBlock().getX(), event.getBlock().getZ())
                || !tycoons.isInsideBuildArea(
                        destination,
                        event.getToBlock().getX(),
                        event.getToBlock().getZ()
                )) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (worlds.isTycoonWorld(event.getBlock().getWorld())
                && !pistonMovementAllowed(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (worlds.isTycoonWorld(event.getBlock().getWorld())
                && !pistonMovementAllowed(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (!worlds.isTycoonWorld(event.getBlock().getWorld())) {
            return;
        }
        Tycoon source = tycoons.at(
                event.getBlock().getWorld().getName(), event.getBlock().getX(), event.getBlock().getZ()
        ).orElse(null);
        if (source == null) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> !insideSameBuildArea(source, block));
        releaseExplodedHoppers(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (!worlds.isTycoonWorld(event.getLocation().getWorld())) {
            return;
        }
        Tycoon source = tycoons.at(
                event.getLocation().getWorld().getName(),
                event.getLocation().getBlockX(),
                event.getLocation().getBlockZ()
        ).orElse(null);
        if (source == null) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> !insideSameBuildArea(source, block));
        releaseExplodedHoppers(event.blockList());
    }

    public void releasePlayer(UUID playerId) {
        lastMessage.remove(playerId);
    }

    private Tycoon protect(Player player, Location location, Cancellable event) {
        if (!worlds.isTycoonWorld(location.getWorld()) || player.hasPermission("tycoon.bypass")) {
            return null;
        }
        Tycoon tycoon = tycoons.at(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockZ()
        ).orElse(null);
        if (tycoon == null
                || location.getBlockY() < tycoon.buildMinimumY()
                || location.getBlockY() > tycoon.buildMaximumY()
                || !tycoons.isInsideBuildArea(tycoon, location.getBlockX(), location.getBlockZ())
                || !tycoons.canBuild(player.getUniqueId(), tycoon)) {
            event.setCancelled(true);
            deny(player);
            return null;
        }
        return tycoon;
    }

    private void releaseExplodedHoppers(java.util.List<Block> blocks) {
        blocks.stream()
                .filter(block -> block.getType() == Material.HOPPER)
                .map(block -> position(block.getLocation()))
                .forEach(tycoons::releaseHopper);
    }

    private boolean insideSameBuildArea(Tycoon source, Block block) {
        Tycoon target = tycoons.at(block.getWorld().getName(), block.getX(), block.getZ()).orElse(null);
        return target != null
                && target.id().equals(source.id())
                && tycoons.isInsideBuildArea(source, block.getX(), block.getZ())
                && block.getY() >= source.buildMinimumY()
                && block.getY() <= source.buildMaximumY();
    }

    private boolean pistonMovementAllowed(
            Block piston,
            java.util.List<Block> movedBlocks,
            org.bukkit.block.BlockFace direction
    ) {
        Tycoon source = tycoons.at(piston.getWorld().getName(), piston.getX(), piston.getZ()).orElse(null);
        if (source == null
                || source.status() != fr.valoriatycoon.tycoon.TycoonStatus.ACTIVE
                || !tycoons.isInsideBuildArea(source, piston.getX(), piston.getZ())) {
            return false;
        }
        for (Block block : movedBlocks) {
            Block destination = block.getRelative(direction);
            Tycoon destinationPlot = tycoons.at(
                    destination.getWorld().getName(), destination.getX(), destination.getZ()
            ).orElse(null);
            if (destinationPlot == null
                    || !destinationPlot.id().equals(source.id())
                    || !tycoons.isInsideBuildArea(source, destination.getX(), destination.getZ())
                    || destination.getY() < source.buildMinimumY()
                    || destination.getY() > source.buildMaximumY()) {
                return false;
            }
        }
        return true;
    }

    private HopperPosition position(Location location) {
        return new HopperPosition(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    private void deny(Player player) {
        long now = System.nanoTime();
        Long previous = lastMessage.get(player.getUniqueId());
        if (previous != null && now - previous < MESSAGE_INTERVAL) {
            return;
        }
        lastMessage.put(player.getUniqueId(), now);
        player.sendActionBar(messages.component("tycoon.protected", false));
    }
}
