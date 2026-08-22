package fr.valoriatycoon.tycoon;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Shows and enforces each parcel's current build size with a per-player virtual world border. */
public final class TycoonBoundaryService implements Listener {
    private final JavaPlugin plugin;
    private final TycoonService tycoons;
    private final TycoonWorldService worlds;
    private final Map<UUID, BoundaryKey> displayed = new HashMap<>();

    public TycoonBoundaryService(JavaPlugin plugin, TycoonService tycoons, TycoonWorldService worlds) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    public void refresh(Player player) {
        Location location = player.getLocation();
        if (!worlds.isTycoonWorld(location.getWorld())) {
            clear(player);
            return;
        }
        Tycoon tycoon = tycoons.at(
                location.getWorld().getName(), location.getBlockX(), location.getBlockZ()
        ).filter(candidate -> candidate.status() == TycoonStatus.ACTIVE).orElse(null);
        if (tycoon == null) {
            clear(player);
            return;
        }
        TycoonPlotGroup.Bounds bounds = tycoons.buildBounds(tycoon);
        int size = bounds.maximumX() - bounds.minimumX() + 1;
        BoundaryKey key = new BoundaryKey(tycoon.id(), size);
        if (key.equals(displayed.get(player.getUniqueId()))) {
            return;
        }
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(
                bounds.minimumX() + size / 2.0,
                bounds.minimumZ() + size / 2.0
        );
        border.setSize(size);
        border.setWarningDistance(0);
        border.setWarningTime(0);
        border.setDamageAmount(0.0);
        player.setWorldBorder(border);
        displayed.put(player.getUniqueId(), key);
    }

    public void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        displayed.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() != null && changedHorizontalBlock(event.getFrom(), event.getTo())) {
            refresh(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        schedule(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        schedule(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        schedule(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        displayed.remove(event.getPlayer().getUniqueId());
    }

    private void clear(Player player) {
        if (displayed.remove(player.getUniqueId()) != null) {
            player.setWorldBorder(null);
        }
    }

    private void schedule(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }

    private boolean changedHorizontalBlock(Location from, Location to) {
        return from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockZ() != to.getBlockZ();
    }

    private record BoundaryKey(UUID tycoonId, int size) {
    }
}
