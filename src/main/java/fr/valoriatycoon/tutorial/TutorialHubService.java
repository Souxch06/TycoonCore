package fr.valoriatycoon.tutorial;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.spawn.SpawnWorldService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Maintains the non-persistent informational gallery reached through /warp tuto. */
public final class TutorialHubService implements Listener {
    private final JavaPlugin plugin;
    private final TutorialHubSettings settings;
    private final SpawnWorldService spawnWorld;
    private final MessageService messages;
    private final Logger logger;
    private final NamespacedKey markerKey;
    private final NamespacedKey panelIdKey;
    private final Map<String, TextDisplay> displays = new LinkedHashMap<>();
    private BukkitTask task;
    private boolean running;

    public TutorialHubService(
            JavaPlugin plugin,
            TutorialHubSettings settings,
            SpawnWorldService spawnWorld,
            MessageService messages,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.spawnWorld = Objects.requireNonNull(spawnWorld, "spawnWorld");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.markerKey = new NamespacedKey(plugin, "tutorial_hub_panel");
        this.panelIdKey = new NamespacedKey(plugin, "tutorial_hub_panel_id");
    }

    /** Starts the gallery without loading any destination chunk. */
    public void start() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Tutorial hub panels must start on the primary thread");
        }
        if (!settings.enabled() || running) {
            return;
        }
        World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            logger.warning("Tutorial hub world is unavailable: " + settings.worldName());
            return;
        }
        running = true;
        removeStale(world);
        ensurePanels();
        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::ensurePanels,
                settings.refreshIntervalTicks(),
                settings.refreshIntervalTicks()
        );
    }

    public void stop() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        displays.values().forEach(Entity::remove);
        displays.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (running && event.getWorld().getName().equals(settings.worldName())) {
            ensurePanels();
        }
    }

    private void ensurePanels() {
        if (!running) {
            return;
        }
        World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            return;
        }
        settings.panels().forEach((id, panel) -> {
            Location location = location(panel, world);
            if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                return;
            }
            TextDisplay current = displays.get(id);
            if (current == null || !current.isValid() || !current.getWorld().getUID().equals(world.getUID())) {
                if (current != null) {
                    current.remove();
                }
                displays.put(id, spawn(id, panel, location));
            }
        });
    }

    private TextDisplay spawn(String id, TutorialHubSettings.Panel panel, Location location) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
            display.getPersistentDataContainer().set(panelIdKey, PersistentDataType.STRING, id);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGravity(false);
            display.setSilent(true);
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(settings.shadowed());
            display.setSeeThrough(false);
            display.setDefaultBackground(settings.defaultBackground());
            display.setLineWidth(settings.lineWidth());
            display.setViewRange(settings.viewRange());
            display.setInterpolationDuration(0);
            display.text(text(panel));
        });
    }

    private Component text(TutorialHubSettings.Panel panel) {
        return messages.render(String.join("\n", panel.lines()));
    }

    private Location location(TutorialHubSettings.Panel panel, World world) {
        Location configuredAnchor = spawnWorld.spawn();
        Location anchor = configuredAnchor.getWorld().getUID().equals(world.getUID())
                ? configuredAnchor
                : world.getSpawnLocation();
        return anchor.add(panel.offsetX(), panel.offsetY(), panel.offsetZ());
    }

    private void removeStale(World world) {
        for (TutorialHubSettings.Panel panel : settings.panels().values()) {
            Location location = location(panel, world);
            if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                continue;
            }
            world.getNearbyEntities(location, 2.5, 4.0, 2.5).stream()
                    .filter(entity -> entity.getPersistentDataContainer().has(
                            markerKey,
                            PersistentDataType.BYTE
                    ))
                    .forEach(Entity::remove);
        }
    }
}
