package fr.valoriatycoon.tycoon;

import fr.valoriatycoon.config.MessageService;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Incrementally prepares and clears bounded plots with durable lifecycle states. */
public final class PlotResetService {
    private final JavaPlugin plugin;
    private final TycoonSettings settings;
    private final TycoonService tycoons;
    private final TycoonWorldService worlds;
    private final MessageService messages;
    private final Executor mainThread;
    private final Logger logger;
    private final Queue<ResetJob> jobs = new ArrayDeque<>();
    private final Set<UUID> scheduled = new HashSet<>();
    private BukkitTask task;

    public PlotResetService(
            JavaPlugin plugin,
            TycoonSettings settings,
            TycoonService tycoons,
            TycoonWorldService worlds,
            MessageService messages,
            Executor mainThread,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void start() {
        if (task != null) {
            throw new IllegalStateException("Plot reset service is already running");
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::process, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ResetJob job : jobs) {
            releaseTickets(job);
        }
        jobs.clear();
        scheduled.clear();
    }

    public void resume(java.util.List<Tycoon> tycoons) {
        for (Tycoon tycoon : tycoons) {
            if (tycoon.status() == TycoonStatus.PREPARING) {
                schedulePreparation(tycoon);
            } else if (tycoon.status() == TycoonStatus.DELETING) {
                schedule(tycoon, true);
            }
        }
    }

    public void schedulePreparation(Tycoon tycoon) {
        tycoons.clearHoppers(tycoon.id()).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not clear tracked hoppers for " + tycoon.id(), error);
            return null;
        });
        schedule(tycoon, false);
    }

    public void scheduleDeletion(Tycoon tycoon) {
        schedule(tycoon, true);
    }

    public int queuedJobs() {
        return scheduled.size();
    }

    private void schedule(Tycoon tycoon, boolean deleteAfter) {
        if (!scheduled.add(tycoon.id())) {
            return;
        }
        World world = worlds.world(tycoon.worldName());
        int minimumChunkX = tycoon.bounds().minimumX() >> 4;
        int maximumChunkX = tycoon.bounds().maximumX() >> 4;
        int minimumChunkZ = tycoon.bounds().minimumZ() >> 4;
        int maximumChunkZ = tycoon.bounds().maximumZ() >> 4;
        java.util.List<CompletableFuture<Chunk>> loads = new java.util.ArrayList<>();
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                loads.add(world.getChunkAtAsync(chunkX, chunkZ, true));
            }
        }
        CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).whenCompleteAsync((ignored, error) -> {
            if (error != null) {
                scheduled.remove(tycoon.id());
                logger.log(Level.SEVERE, "Could not load chunks for Tycoon plot " + tycoon.id(), error);
                return;
            }
            for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
                for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                    world.addPluginChunkTicket(chunkX, chunkZ, plugin);
                }
            }
            if (deleteAfter) {
                evacuate(tycoon, world);
            }
            TycoonPlotGroup group = settings.group(tycoon.groupId());
            jobs.add(new ResetJob(
                    tycoon,
                    deleteAfter,
                    group.floorMaterial(),
                    group.baseMaterial(),
                    group.islandRadius(),
                    group.baseDepth(),
                    tycoon.bounds().minimumX(),
                    tycoon.floorY() - group.baseDepth(),
                    tycoon.bounds().minimumZ(),
                    minimumChunkX,
                    maximumChunkX,
                    minimumChunkZ,
                    maximumChunkZ
            ));
        }, mainThread);
    }

    private void process() {
        int remaining = settings.resetBlocksPerTick();
        while (remaining > 0 && !jobs.isEmpty()) {
            ResetJob job = jobs.peek();
            int processed = job.process(worlds.world(job.tycoon.worldName()), remaining);
            remaining -= processed;
            if (job.complete()) {
                jobs.poll();
                complete(job);
            } else if (processed == 0) {
                return;
            }
        }
    }

    private void complete(ResetJob job) {
        releaseTickets(job);
        CompletableFuture<?> completion = job.deleteAfter
                ? tycoons.finalizeDeletion(job.tycoon.id())
                : tycoons.markActive(job.tycoon.id());
        completion.whenCompleteAsync((result, error) -> {
            scheduled.remove(job.tycoon.id());
            if (error != null) {
                logger.log(Level.SEVERE, "Could not finalize plot reset for " + job.tycoon.id(), error);
                return;
            }
            Player owner = Bukkit.getPlayer(job.tycoon.ownerId());
            if (owner == null) {
                return;
            }
            if (job.deleteAfter) {
                messages.send(owner, "tycoon.reset-complete");
            } else if (result instanceof Tycoon active) {
                owner.teleportAsync(active.home(worlds.world(active.worldName())));
                messages.send(owner, "tycoon.created");
            }
        }, mainThread);
    }

    private void releaseTickets(ResetJob job) {
        World world = worlds.world(job.tycoon.worldName());
        for (int chunkX = job.minimumChunkX; chunkX <= job.maximumChunkX; chunkX++) {
            for (int chunkZ = job.minimumChunkZ; chunkZ <= job.maximumChunkZ; chunkZ++) {
                world.removePluginChunkTicket(chunkX, chunkZ, plugin);
            }
        }
    }

    private void evacuate(Tycoon tycoon, World world) {
        for (Player player : world.getPlayers()) {
            if (tycoon.containsHorizontal(player.getLocation().getBlockX(), player.getLocation().getBlockZ())) {
                player.teleportAsync(worlds.safeSpawn());
            }
        }
    }

    private static final class ResetJob {
        private final Tycoon tycoon;
        private final boolean deleteAfter;
        private final Material floorMaterial;
        private final Material baseMaterial;
        private final int islandRadius;
        private final int baseDepth;
        private int x;
        private int y;
        private int z;
        private final int minimumChunkX;
        private final int maximumChunkX;
        private final int minimumChunkZ;
        private final int maximumChunkZ;
        private boolean complete;

        private ResetJob(
                Tycoon tycoon,
                boolean deleteAfter,
                Material floorMaterial,
                Material baseMaterial,
                int islandRadius,
                int baseDepth,
                int x,
                int y,
                int z,
                int minimumChunkX,
                int maximumChunkX,
                int minimumChunkZ,
                int maximumChunkZ
        ) {
            this.tycoon = tycoon;
            this.deleteAfter = deleteAfter;
            this.floorMaterial = floorMaterial;
            this.baseMaterial = baseMaterial;
            this.islandRadius = islandRadius;
            this.baseDepth = baseDepth;
            this.x = x;
            this.y = y;
            this.z = z;
            this.minimumChunkX = minimumChunkX;
            this.maximumChunkX = maximumChunkX;
            this.minimumChunkZ = minimumChunkZ;
            this.maximumChunkZ = maximumChunkZ;
        }

        private int process(World world, int budget) {
            int processed = 0;
            while (processed < budget && !complete) {
                world.getBlockAt(x, y, z).setType(desiredMaterial(), false);
                processed++;
                advance();
            }
            return processed;
        }

        private Material desiredMaterial() {
            return SkyblockIslandShape.materialAt(
                    tycoon.bounds().centerX(),
                    tycoon.bounds().centerZ(),
                    tycoon.floorY(),
                    islandRadius,
                    baseDepth,
                    floorMaterial,
                    baseMaterial,
                    x,
                    y,
                    z
            );
        }

        private void advance() {
            y++;
            if (y <= tycoon.buildMaximumY()) {
                return;
            }
            y = tycoon.floorY() - baseDepth;
            z++;
            if (z <= tycoon.bounds().maximumZ()) {
                return;
            }
            z = tycoon.bounds().minimumZ();
            x++;
            if (x > tycoon.bounds().maximumX()) {
                complete = true;
            }
        }

        private boolean complete() {
            return complete;
        }
    }
}
