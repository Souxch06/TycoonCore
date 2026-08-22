package fr.valoriatycoon.listeners;

import fr.valoriatycoon.farm.FarmWorldService;
import fr.valoriatycoon.farm.autosell.AutoSellBatchService;
import fr.valoriatycoon.farm.autosell.AutoSellService;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Owns farm preference, pending-sale and teleport state for online players. */
public final class FarmPlayerListener implements Listener {
    private final AutoSellService autoSell;
    private final AutoSellBatchService batches;
    private final FarmWorldService worlds;
    private final FarmProtectionListener protection;
    private final Logger logger;

    public FarmPlayerListener(
            AutoSellService autoSell,
            AutoSellBatchService batches,
            FarmWorldService worlds,
            FarmProtectionListener protection,
            Logger logger
    ) {
        this.autoSell = Objects.requireNonNull(autoSell, "autoSell");
        this.batches = Objects.requireNonNull(batches, "batches");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.protection = Objects.requireNonNull(protection, "protection");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        autoSell.activate(playerId).exceptionally(error -> {
            logger.log(Level.SEVERE, "Could not load auto-sell preference for " + playerId, error);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        batches.flushPlayer(playerId);
        autoSell.deactivate(playerId);
        worlds.releasePlayer(playerId);
        protection.releasePlayer(playerId);
    }
}
