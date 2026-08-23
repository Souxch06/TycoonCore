package fr.valoriatycoon.listeners;

import fr.valoriatycoon.commands.TycoonCommand;
import fr.valoriatycoon.tycoon.TycoonPlaytimeService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Tracks owner sessions and clears ephemeral player state on logout. */
public final class TycoonSessionListener implements Listener {
    private final TycoonCommand command;
    private final TycoonProtectionListener protection;
    private final TycoonPlaytimeService playtime;

    public TycoonSessionListener(
            TycoonCommand command,
            TycoonProtectionListener protection,
            TycoonPlaytimeService playtime
    ) {
        this.command = Objects.requireNonNull(command, "command");
        this.protection = Objects.requireNonNull(protection, "protection");
        this.playtime = Objects.requireNonNull(playtime, "playtime");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        playtime.playerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        command.releasePlayer(event.getPlayer().getUniqueId());
        protection.releasePlayer(event.getPlayer().getUniqueId());
        playtime.playerQuit(event.getPlayer().getUniqueId());
    }
}
