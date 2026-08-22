package fr.valoriatycoon.tycoon;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Batches owner playtime once per minute instead of issuing per-tick storage updates. */
public final class TycoonPlaytimeService {
    private static final long FLUSH_INTERVAL_TICKS = 1200L;

    private final JavaPlugin plugin;
    private final TycoonService tycoons;
    private final Logger logger;
    private final Map<UUID, Long> creditedAtMillis = new HashMap<>();
    private BukkitTask task;

    public TycoonPlaytimeService(JavaPlugin plugin, TycoonService tycoons, Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void start() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isActiveOwner(player.getUniqueId())) {
                creditedAtMillis.put(player.getUniqueId(), now);
            }
        }
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::flushOnline,
                FLUSH_INTERVAL_TICKS,
                FLUSH_INTERVAL_TICKS
        );
    }

    /** Starts measuring an online island owner's session immediately. */
    public void playerJoin(UUID playerId) {
        if (isActiveOwner(playerId)) {
            creditedAtMillis.putIfAbsent(playerId, System.currentTimeMillis());
        }
    }

    /** Persists the final whole seconds of an island owner's session. */
    public void playerQuit(UUID playerId) {
        flushPlayer(playerId, System.currentTimeMillis());
        creditedAtMillis.remove(playerId);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        flushOnline();
        creditedAtMillis.clear();
    }

    private void flushOnline() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            if (isActiveOwner(playerId)) {
                Long previous = creditedAtMillis.putIfAbsent(playerId, now);
                if (previous != null) {
                    flushDelta(playerId, previous, now);
                    creditedAtMillis.put(playerId, now);
                }
            } else {
                creditedAtMillis.remove(playerId);
            }
        }
    }

    private void flushPlayer(UUID playerId, long now) {
        Long previous = creditedAtMillis.get(playerId);
        if (previous != null && isActiveOwner(playerId)) {
            flushDelta(playerId, previous, now);
        }
    }

    private void flushDelta(UUID playerId, long previous, long now) {
        long seconds = Math.max(0L, (now - previous) / 1000L);
        if (seconds == 0) {
            return;
        }
        tycoons.addPlaytime(playerId, seconds).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not persist Tycoon playtime for " + playerId, error);
            return null;
        });
    }

    private boolean isActiveOwner(UUID playerId) {
        return tycoons.ownedBy(playerId)
                .filter(tycoon -> tycoon.status() == TycoonStatus.ACTIVE)
                .isPresent();
    }
}
