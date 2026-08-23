package fr.valoriatycoon.listeners;

import fr.valoriatycoon.LifecycleState;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.economy.PaymentRateLimiter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Loads account data off-thread before login and owns the online balance cache lifecycle. */
public final class PlayerAccountListener implements Listener {
    private static final long LOGIN_STORAGE_TIMEOUT_SECONDS = 10;

    private final InternalEconomyService economy;
    private final PaymentRateLimiter paymentRateLimiter;
    private final Supplier<LifecycleState> lifecycle;
    private final MessageService messages;
    private final Logger logger;

    public PlayerAccountListener(
            InternalEconomyService economy,
            PaymentRateLimiter paymentRateLimiter,
            Supplier<LifecycleState> lifecycle,
            MessageService messages,
            Logger logger
    ) {
        this.economy = Objects.requireNonNull(economy, "economy");
        this.paymentRateLimiter = Objects.requireNonNull(paymentRateLimiter, "paymentRateLimiter");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        LifecycleState state = lifecycle.get();
        if (state == LifecycleState.FAILED || state == LifecycleState.STOPPING) {
            denyLogin(event);
            return;
        }
        try {
            economy.prepareAccount(event.getUniqueId(), event.getName())
                    .get(LOGIN_STORAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Account preparation interrupted for " + event.getUniqueId(), exception);
            denyLogin(event);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Could not prepare account for " + event.getUniqueId(), exception);
            denyLogin(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        economy.activateAccount(playerId, playerName)
                .exceptionally(error -> {
                    logger.log(Level.SEVERE, "Could not activate account cache for " + playerId, error);
                    return null;
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        economy.deactivateAccount(event.getPlayer().getUniqueId());
        paymentRateLimiter.remove(event.getPlayer().getUniqueId());
    }

    private void denyLogin(AsyncPlayerPreLoginEvent event) {
        event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                messages.component("login.storage-unavailable", false)
        );
    }
}
