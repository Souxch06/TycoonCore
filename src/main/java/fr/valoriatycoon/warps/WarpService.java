package fr.valoriatycoon.warps;

import fr.valoriatycoon.spawn.SpawnWorldService;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Resolves extensible warps and performs cooldown-protected Paper async teleports. */
public final class WarpService implements Listener {
    private final WarpSettings settings;
    private final SpawnWorldService spawnWorld;
    private final Map<UUID, Long> deadlines = new ConcurrentHashMap<>();

    public WarpService(WarpSettings settings, SpawnWorldService spawnWorld) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.spawnWorld = Objects.requireNonNull(spawnWorld, "spawnWorld");
    }

    public WarpDefinition resolve(String input) {
        return settings.resolve(input);
    }

    public Collection<WarpDefinition> warps() {
        return settings.warps().values();
    }

    public WarpSettings settings() {
        return settings;
    }

    /** Starts no sync chunk load; Paper resolves the destination asynchronously. */
    public WarpTeleportAttempt teleport(Player player, WarpDefinition warp) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(warp, "warp");
        long now = System.nanoTime();
        long deadline = deadlines.getOrDefault(player.getUniqueId(), 0L);
        if (deadline > now) {
            return WarpTeleportAttempt.rejected(
                    Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(deadline - now) + 1L)
            );
        }
        World world = Bukkit.getWorld(warp.worldName());
        if (world == null) {
            return WarpTeleportAttempt.rejected(0L);
        }
        if (settings.cooldownSeconds() > 0) {
            deadlines.put(
                    player.getUniqueId(),
                    now + TimeUnit.SECONDS.toNanos(settings.cooldownSeconds())
            );
        }
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        return new WarpTeleportAttempt(
                true,
                0L,
                player.teleportAsync(destination(world, warp), PlayerTeleportEvent.TeleportCause.COMMAND)
        );
    }

    public void release(UUID playerId) {
        deadlines.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        release(event.getPlayer().getUniqueId());
    }

    private Location destination(World world, WarpDefinition warp) {
        Location base;
        if (warp.relativeToSpawn()) {
            Location configuredSpawn = spawnWorld.spawn();
            base = configuredSpawn.getWorld().getUID().equals(world.getUID())
                    ? configuredSpawn
                    : world.getSpawnLocation();
        } else {
            base = new Location(world, 0.0, 0.0, 0.0);
        }
        Location destination = base.add(warp.x(), warp.y(), warp.z());
        destination.setYaw(warp.yaw());
        destination.setPitch(warp.pitch());
        return destination;
    }
}
