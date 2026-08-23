package fr.valoriatycoon.listeners;

import fr.valoriatycoon.professions.ProfessionService;
import fr.valoriatycoon.tools.ToolEffectService;
import fr.valoriatycoon.tools.ToolProgressionService;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Owns online tool/profession activation and clean XP flushing. */
public final class ToolPlayerListener implements Listener {
    private final ToolProgressionService tools;
    private final ProfessionService professions;
    private final ToolEffectService effects;
    private final Executor mainThread;
    private final Logger logger;

    public ToolPlayerListener(
            ToolProgressionService tools,
            ProfessionService professions,
            ToolEffectService effects,
            Executor mainThread,
            Logger logger
    ) {
        this.tools = Objects.requireNonNull(tools, "tools");
        this.professions = Objects.requireNonNull(professions, "professions");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var toolsReady = tools.activate(player.getUniqueId());
        var professionsReady = professions.activate(player.getUniqueId());
        java.util.concurrent.CompletableFuture.allOf(toolsReady, professionsReady)
                .whenCompleteAsync((ignored, error) -> {
                    if (error != null) {
                        logger.log(
                                Level.SEVERE,
                                "Could not load tool/profession profiles for " + player.getUniqueId(),
                                error
                        );
                    } else {
                        effects.refresh(player);
                    }
                }, mainThread);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        effects.releasePlayer(playerId);
        tools.deactivate(playerId);
        professions.deactivate(playerId);
    }
}
