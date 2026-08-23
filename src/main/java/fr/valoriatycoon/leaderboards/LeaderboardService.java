package fr.valoriatycoon.leaderboards;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Periodically refreshes and atomically publishes immutable asynchronous leaderboard caches. */
public final class LeaderboardService {
    private final JavaPlugin plugin;
    private final LeaderboardSettings settings;
    private final LeaderboardRepository repository;
    private final Logger logger;
    private final AtomicBoolean refreshInFlight = new AtomicBoolean();
    private volatile LeaderboardSnapshot snapshot = LeaderboardSnapshot.empty();
    private BukkitTask task;

    public LeaderboardService(
            JavaPlugin plugin,
            LeaderboardSettings settings,
            LeaderboardRepository repository,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Starts a single bounded refresh schedule; SQL remains on the database worker. */
    public void start() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Leaderboard lifecycle must start on the primary thread");
        }
        if (!settings.enabled() || task != null) {
            return;
        }
        refresh();
        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::refresh,
                settings.refreshIntervalTicks(),
                settings.refreshIntervalTicks()
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Requests a refresh and coalesces overlapping scheduled/manual attempts. */
    public CompletableFuture<LeaderboardSnapshot> refresh() {
        if (!settings.enabled()) {
            return CompletableFuture.completedFuture(snapshot);
        }
        if (!refreshInFlight.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(snapshot);
        }
        return repository.loadAll(settings.queryLimit())
                .thenApply(entries -> {
                    LeaderboardSnapshot updated = new LeaderboardSnapshot(Instant.now(), entries);
                    snapshot = updated;
                    return updated;
                })
                .whenComplete((ignored, error) -> {
                    refreshInFlight.set(false);
                    if (error != null) {
                        logger.log(Level.WARNING, "Could not refresh asynchronous leaderboards", error);
                    }
                });
    }

    /** Returns the last complete immutable cache without blocking or SQL. */
    public LeaderboardSnapshot snapshot() {
        return snapshot;
    }

    public LeaderboardSettings settings() {
        return settings;
    }
}
