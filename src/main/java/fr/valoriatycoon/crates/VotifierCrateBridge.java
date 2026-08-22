package fr.valoriatycoon.crates;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Optional reflection bridge supporting NuVotifier/Votifier without a hard runtime dependency. */
public final class VotifierCrateBridge {
    private VotifierCrateBridge() {
    }

    public static boolean register(
            JavaPlugin plugin,
            CrateKeyService keys,
            Logger logger
    ) {
        Plugin votifier = List.of("NuVotifier", "Votifier").stream()
                .map(name -> plugin.getServer().getPluginManager().getPlugin(name))
                .filter(candidate -> candidate != null && candidate.isEnabled())
                .findFirst()
                .orElse(null);
        if (votifier == null) {
            logger.info("No Votifier provider detected; Vote key API remains available for later integration.");
            return false;
        }
        try {
            Class<?> rawEvent = Class.forName("com.vexsoftware.votifier.model.VotifierEvent");
            if (!Event.class.isAssignableFrom(rawEvent)) {
                throw new IllegalStateException("VotifierEvent is not a Bukkit Event");
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) rawEvent;
            Listener listener = new Listener() {
            };
            plugin.getServer().getPluginManager().registerEvent(
                    eventType,
                    listener,
                    EventPriority.MONITOR,
                    (ignored, event) -> handle(keys, event, logger),
                    plugin,
                    true
            );
            logger.info("Vote crate keys connected to " + votifier.getName() + '.');
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING, "Could not connect Vote crate keys to Votifier", exception);
            return false;
        }
    }

    private static void handle(CrateKeyService keys, Event event, Logger logger) {
        try {
            Object vote = event.getClass().getMethod("getVote").invoke(event);
            String playerName = invoke(vote, "getUsername", "unknown");
            String service = invoke(vote, "getServiceName", "vote");
            String timestamp = invoke(vote, "getTimeStamp", "unknown-time");
            String address = invoke(vote, "getAddress", "unknown-address");
            String rawReference = service + ':' + playerName + ':' + timestamp + ':' + address;
            String reference = UUID.nameUUIDFromBytes(
                    rawReference.getBytes(StandardCharsets.UTF_8)
            ).toString();
            keys.recordVote(playerName, service, reference).whenComplete((rewarded, error) -> {
                if (error != null) {
                    logger.log(Level.WARNING, "Could not persist Vote crate key for " + playerName, error);
                } else if (!rewarded) {
                    logger.warning("Vote received for unknown Valoria player " + playerName);
                }
            });
        } catch (ReflectiveOperationException exception) {
            logger.log(Level.WARNING, "Could not read Votifier vote payload", exception);
        }
    }

    private static String invoke(Object target, String methodName, String fallback)
            throws ReflectiveOperationException {
        Method method;
        try {
            method = target.getClass().getMethod(methodName);
        } catch (NoSuchMethodException exception) {
            return fallback;
        }
        Object value = method.invoke(target);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
