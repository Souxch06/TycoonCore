package fr.valoriatycoon.farm.regeneration;

import fr.valoriatycoon.farm.FarmSettings;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

/**
 * Restores farm blocks through one bounded priority-queue task rather than one Bukkit task per block.
 */
public final class BlockRegenerationService {
    private final JavaPlugin plugin;
    private final FarmSettings.Regeneration settings;
    private final BlockRegenerationRepository repository;
    private final Logger logger;
    private final PriorityQueue<PendingBlockRegeneration> dueQueue = new PriorityQueue<>();
    private final Map<BlockPosition, PendingBlockRegeneration> active = new HashMap<>();
    private BukkitTask task;

    public BlockRegenerationService(
            JavaPlugin plugin,
            FarmSettings.Regeneration settings,
            BlockRegenerationRepository repository,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Imports persisted work and starts one bounded main-thread timer. */
    public void start(List<PendingBlockRegeneration> persisted) {
        requirePrimaryThread();
        if (task != null) {
            throw new IllegalStateException("Block regeneration service is already running");
        }
        for (PendingBlockRegeneration pending : persisted) {
            put(pending);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::processDueBlocks,
                settings.checkIntervalTicks(),
                settings.checkIntervalTicks()
        );
    }

    public void schedule(Block block, Duration delay) {
        requirePrimaryThread();
        BlockPosition position = new BlockPosition(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
        );
        long delayMillis = Math.max(1L, delay.toMillis());
        long now = System.currentTimeMillis();
        long dueAt = now > Long.MAX_VALUE - delayMillis ? Long.MAX_VALUE : now + delayMillis;
        PendingBlockRegeneration pending = new PendingBlockRegeneration(
                position,
                block.getBlockData().getAsString(),
                dueAt
        );
        put(pending);
        repository.save(pending).exceptionally(error -> {
            logger.log(Level.SEVERE, "Could not persist block regeneration at " + position, error);
            return null;
        });
    }

    public void stop() {
        requirePrimaryThread();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public int pendingCount() {
        return active.size();
    }

    private void processDueBlocks() {
        long now = System.currentTimeMillis();
        int processed = 0;
        while (processed < settings.maximumBlocksPerRun()) {
            PendingBlockRegeneration pending = dueQueue.peek();
            if (pending == null || pending.dueAtEpochMillis() > now) {
                return;
            }
            dueQueue.poll();
            if (active.get(pending.position()) != pending) {
                continue;
            }
            processed++;
            attemptRestoration(pending, now);
        }
    }

    private void attemptRestoration(PendingBlockRegeneration pending, long now) {
        BlockPosition position = pending.position();
        World world = Bukkit.getWorld(position.worldName());
        if (world == null || !world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) {
            long retryAt = now + Duration.ofSeconds(settings.unloadedRetrySeconds()).toMillis();
            PendingBlockRegeneration retry = new PendingBlockRegeneration(position, pending.blockData(), retryAt);
            put(retry);
            return;
        }

        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        if (occupiedByPlayer(world, position)) {
            PendingBlockRegeneration retry = new PendingBlockRegeneration(
                    position,
                    pending.blockData(),
                    now + 1_000L
            );
            put(retry);
            return;
        }
        if (block.getType().isAir() || block.getType() == Material.WATER) {
            try {
                BlockData data = Bukkit.createBlockData(pending.blockData());
                block.setBlockData(data, false);
            } catch (RuntimeException exception) {
                logger.log(Level.SEVERE, "Invalid persisted block data at " + position + ": " + pending.blockData(), exception);
            }
        }
        active.remove(position, pending);
        repository.delete(position).exceptionally(error -> {
            logger.log(Level.SEVERE, "Could not delete completed block regeneration at " + position, error);
            return null;
        });
    }

    private boolean occupiedByPlayer(World world, BlockPosition position) {
        BoundingBox blockBox = new BoundingBox(
                position.x(),
                position.y(),
                position.z(),
                position.x() + 1.0,
                position.y() + 1.0,
                position.z() + 1.0
        );
        for (Player player : world.getPlayers()) {
            if (player.getBoundingBox().overlaps(blockBox)) {
                return true;
            }
        }
        return false;
    }

    private void put(PendingBlockRegeneration pending) {
        active.put(pending.position(), pending);
        dueQueue.add(pending);
    }

    private void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Block regeneration state must be modified on the primary thread");
        }
    }
}
