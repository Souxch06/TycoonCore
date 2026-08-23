package fr.valoriatycoon.farm.autosell;

import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.MoneyCodec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Batches rapid harvest income into one authoritative economy transaction per player and interval. */
public final class AutoSellBatchService {
    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final CurrencyFormatter formatter;
    private final MessageService messages;
    private final Executor mainThread;
    private final Logger logger;
    private final int flushIntervalTicks;
    private final Map<UUID, Batch> batches = new HashMap<>();
    private BukkitTask task;

    public AutoSellBatchService(
            JavaPlugin plugin,
            EconomyService economy,
            CurrencyFormatter formatter,
            MessageService messages,
            Executor mainThread,
            Logger logger,
            int flushIntervalTicks
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.flushIntervalTicks = flushIntervalTicks;
    }

    public void start() {
        requirePrimaryThread();
        if (task != null) {
            throw new IllegalStateException("Auto-sell batch service is already running");
        }
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::flushAllAsync,
                flushIntervalTicks,
                flushIntervalTicks
        );
    }

    /** Queues already-priced drops. Items are retained as a fallback until money commits. */
    public void queue(Player player, long valueCents, List<ItemStack> fallbackItems) {
        requirePrimaryThread();
        if (valueCents <= 0) {
            throw new IllegalArgumentException("Queued auto-sell value must be positive");
        }
        Batch batch = batches.computeIfAbsent(player.getUniqueId(), ignored -> new Batch());
        try {
            batch.valueCents = Math.addExact(batch.valueCents, valueCents);
        } catch (ArithmeticException overflow) {
            batches.remove(player.getUniqueId());
            flush(player.getUniqueId(), batch);
            batch = new Batch();
            batch.valueCents = valueCents;
            batches.put(player.getUniqueId(), batch);
        }
        for (ItemStack item : fallbackItems) {
            batch.fallbackItems.add(item.clone());
        }
        batch.lastLocation = player.getLocation().clone();
    }

    public void flushPlayer(UUID playerId) {
        requirePrimaryThread();
        Batch batch = batches.remove(playerId);
        if (batch != null) {
            flush(playerId, batch);
        }
    }

    /** Flushes committed sales during plugin shutdown; known failures return their items without duplication. */
    public void stopAndFlush(Duration timeout) {
        requirePrimaryThread();
        if (task != null) {
            task.cancel();
            task = null;
        }
        List<CompletableFuture<Resolution>> resolutions = new ArrayList<>();
        for (Map.Entry<UUID, Batch> entry : batches.entrySet()) {
            resolutions.add(submit(entry.getKey(), entry.getValue()));
        }
        batches.clear();
        if (resolutions.isEmpty()) {
            return;
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(resolutions.toArray(CompletableFuture[]::new));
        try {
            all.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            for (CompletableFuture<Resolution> future : resolutions) {
                Resolution resolution = future.join();
                if (!resolution.successful()) {
                    deliverFallback(resolution.playerId(), resolution.batch());
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while flushing auto-sell batches", exception);
        } catch (ExecutionException | TimeoutException exception) {
            // Do not return items when commit outcome is unknown: that could duplicate value.
            logger.log(Level.SEVERE, "Auto-sell shutdown flush did not reach a known outcome", exception);
        }
    }

    private void flushAllAsync() {
        if (batches.isEmpty()) {
            return;
        }
        Map<UUID, Batch> snapshot = new HashMap<>(batches);
        batches.clear();
        snapshot.forEach(this::flush);
    }

    private void flush(UUID playerId, Batch batch) {
        submit(playerId, batch).whenCompleteAsync((resolution, error) -> {
            if (error != null || resolution == null || !resolution.successful()) {
                logger.log(Level.WARNING, "Auto-sell batch failed for " + playerId, error);
                deliverFallback(playerId, batch);
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendActionBar(messages.component(
                        "farm.autosell-sold",
                        false,
                        Placeholder.unparsed("amount", formatter.format(batch.valueCents))
                ));
            }
        }, mainThread);
    }

    private CompletableFuture<Resolution> submit(UUID playerId, Batch batch) {
        return economy.addMoney(playerId, MoneyCodec.fromCents(batch.valueCents), "farm:autosell")
                .toCompletableFuture()
                .handle((result, error) -> new Resolution(
                        playerId,
                        batch,
                        error == null && result != null && result.successful()
                ));
    }

    private void deliverFallback(UUID playerId, Batch batch) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(
                    batch.fallbackItems.toArray(ItemStack[]::new)
            );
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            messages.send(player, "farm.autosell-failed");
            return;
        }
        Location location = batch.lastLocation;
        if (location != null && location.getWorld() != null) {
            batch.fallbackItems.forEach(item -> location.getWorld().dropItemNaturally(location, item));
        }
    }

    private void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Auto-sell batches must be modified on the primary thread");
        }
    }

    private static final class Batch {
        private long valueCents;
        private final List<ItemStack> fallbackItems = new ArrayList<>();
        private Location lastLocation;
    }

    private record Resolution(UUID playerId, Batch batch, boolean successful) {
    }
}
