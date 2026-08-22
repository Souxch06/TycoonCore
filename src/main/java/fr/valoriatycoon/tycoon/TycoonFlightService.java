package fr.valoriatycoon.tycoon;

import fr.valoriatycoon.config.MessageService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Grants scoped Skyblock flight, revokes only grants it owns, and rescues players from the void. */
public final class TycoonFlightService implements Listener {
    private final JavaPlugin plugin;
    private final TycoonSettings.Flight settings;
    private final TycoonService tycoons;
    private final TycoonWorldService worlds;
    private final TycoonFlightAccessPolicy accessPolicy;
    private final MessageService messages;
    private final Set<UUID> grantedFlight = new HashSet<>();
    private final Set<UUID> pendingRefresh = new HashSet<>();
    private final Set<UUID> rescuing = new HashSet<>();

    public TycoonFlightService(
            JavaPlugin plugin,
            TycoonSettings.Flight settings,
            TycoonService tycoons,
            TycoonWorldService worlds,
            TycoonFlightAccessPolicy accessPolicy,
            MessageService messages
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void refresh(Player player) {
        Location location = player.getLocation();
        if (!worlds.isTycoonWorld(location.getWorld())) {
            revoke(player);
            return;
        }
        Tycoon tycoon = tycoons.at(
                location.getWorld().getName(), location.getBlockX(), location.getBlockZ()
        ).orElse(null);
        if (rescueIfNeeded(player, tycoon)) {
            return;
        }
        if (tycoon == null
                || tycoon.status() != TycoonStatus.ACTIVE
                || !tycoons.isInsideBuildArea(
                        tycoon,
                        location.getBlockX(),
                        location.getBlockZ()
                )) {
            revoke(player);
            return;
        }
        boolean owner = tycoon.ownerId().equals(player.getUniqueId());
        boolean member = tycoons.members(tycoon.id()).contains(player.getUniqueId());
        if (accessPolicy.canFly(player.getUniqueId(), tycoon, owner, member)) {
            grant(player);
        } else {
            revoke(player);
        }
    }

    public void stop() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            revoke(player);
        }
        grantedFlight.clear();
        pendingRefresh.clear();
        rescuing.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || sameBlock(event.getFrom(), to)) {
            return;
        }
        refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        pendingRefresh.remove(event.getPlayer().getUniqueId());
        rescuing.remove(event.getPlayer().getUniqueId());
        revoke(event.getPlayer());
    }

    private void grant(Player player) {
        if (creativeFlight(player)) {
            grantedFlight.remove(player.getUniqueId());
            return;
        }
        if (player.getAllowFlight() && !grantedFlight.contains(player.getUniqueId())) {
            return; // Another plugin or permission owns this flight state.
        }
        if (grantedFlight.add(player.getUniqueId())) {
            player.setAllowFlight(true);
            player.sendActionBar(messages.component("tycoon.flight-enabled", false));
        }
    }

    private void revoke(Player player) {
        if (!grantedFlight.remove(player.getUniqueId())) {
            return;
        }
        if (!creativeFlight(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendActionBar(messages.component("tycoon.flight-disabled", false));
        }
    }

    private boolean rescueIfNeeded(Player player, Tycoon horizontalPlot) {
        int threshold = (horizontalPlot == null
                ? worlds.referenceFloor(player.getWorld().getName())
                : horizontalPlot.floorY()) - settings.voidRescueBelowFloor();
        if (player.getLocation().getY() >= threshold) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (!rescuing.add(playerId)) {
            return true;
        }
        revoke(player);
        java.util.concurrent.CompletableFuture<Boolean> teleport;
        if (horizontalPlot != null
                && horizontalPlot.status() == TycoonStatus.ACTIVE
                && tycoons.canBuild(playerId, horizontalPlot)) {
            teleport = player.teleportAsync(horizontalPlot.home(worlds.world(horizontalPlot.worldName())));
        } else {
            teleport = player.teleportAsync(worlds.safeSpawn());
        }
        teleport.whenComplete((success, error) -> {
            if (plugin.isEnabled()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> rescuing.remove(playerId));
            }
        });
        return true;
    }

    private void scheduleRefresh(Player player) {
        UUID playerId = player.getUniqueId();
        if (!pendingRefresh.add(playerId)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pendingRefresh.remove(playerId);
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }

    private boolean creativeFlight(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
