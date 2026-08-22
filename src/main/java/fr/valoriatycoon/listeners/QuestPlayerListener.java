package fr.valoriatycoon.listeners;

import fr.valoriatycoon.quests.QuestService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class QuestPlayerListener implements Listener {
    private final QuestService quests;private final Logger logger;
    public QuestPlayerListener(QuestService quests,Logger logger){this.quests=quests;this.logger=logger;}
    @EventHandler(priority=EventPriority.MONITOR)public void join(PlayerJoinEvent e){quests.activate(e.getPlayer().getUniqueId()).exceptionally(error->{logger.log(Level.WARNING,"Quest load failed",error);return null;});}
    @EventHandler(priority=EventPriority.MONITOR)public void quit(PlayerQuitEvent e){quests.deactivate(e.getPlayer().getUniqueId());}
}
