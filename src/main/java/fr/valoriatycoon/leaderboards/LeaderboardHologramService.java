package fr.valoriatycoon.leaderboards;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.spawn.SpawnWorldService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Maintains five non-persistent spawn TextDisplays from the immutable leaderboard cache only. */
public final class LeaderboardHologramService {
    private final JavaPlugin plugin;
    private final LeaderboardHologramSettings settings;
    private final LeaderboardService leaderboards;
    private final LeaderboardValueFormatter values;
    private final SpawnWorldService spawnWorld;
    private final MessageService messages;
    private final Logger logger;
    private final NamespacedKey markerKey;
    private final NamespacedKey typeKey;
    private final Map<LeaderboardType, TextDisplay> displays = new EnumMap<>(LeaderboardType.class);
    private BukkitTask task;

    public LeaderboardHologramService(
            JavaPlugin plugin,
            LeaderboardHologramSettings settings,
            LeaderboardService leaderboards,
            LeaderboardValueFormatter values,
            SpawnWorldService spawnWorld,
            MessageService messages,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.leaderboards = Objects.requireNonNull(leaderboards, "leaderboards");
        this.values = Objects.requireNonNull(values, "values");
        this.spawnWorld = Objects.requireNonNull(spawnWorld, "spawnWorld");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.markerKey = new NamespacedKey(plugin, "leaderboard_hologram");
        this.typeKey = new NamespacedKey(plugin, "leaderboard_hologram_type");
    }

    /** Spawns no chunks and starts one lightweight cache-render task. */
    public void start() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Leaderboard holograms must start on the primary thread");
        }
        if (!settings.enabled() || task != null) {
            return;
        }
        World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            logger.warning("Leaderboard hologram world is unavailable: " + settings.worldName());
            return;
        }
        removeStaleDisplays(world);
        render();
        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::render,
                settings.updateIntervalTicks(),
                settings.updateIntervalTicks()
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        displays.values().forEach(Entity::remove);
        displays.clear();
    }

    private void render() {
        World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            return;
        }
        LeaderboardSnapshot snapshot = leaderboards.snapshot();
        for (LeaderboardType type : LeaderboardType.values()) {
            TextDisplay display = display(type, world);
            if (display != null) {
                display.text(text(type, snapshot));
            }
        }
    }

    private TextDisplay display(LeaderboardType type, World world) {
        Location location = location(type, world);
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return null;
        }
        TextDisplay current = displays.get(type);
        if (current != null && current.isValid() && current.getWorld().getUID().equals(world.getUID())) {
            return current;
        }
        displays.remove(type);
        TextDisplay created = world.spawn(location, TextDisplay.class, display -> {
            display.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
            display.getPersistentDataContainer().set(
                    typeKey,
                    PersistentDataType.STRING,
                    type.name()
            );
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGravity(false);
            display.setSilent(true);
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(settings.shadowed());
            display.setSeeThrough(false);
            display.setDefaultBackground(settings.defaultBackground());
            display.setLineWidth(settings.lineWidth());
            display.setViewRange(settings.viewRange());
            display.setInterpolationDuration(0);
        });
        displays.put(type, created);
        return created;
    }

    private Component text(LeaderboardType type, LeaderboardSnapshot snapshot) {
        Component result = messages.render(
                "<gold><bold>TOP — <category></bold></gold>",
                Placeholder.unparsed("category", type.displayName().toUpperCase(java.util.Locale.ROOT))
        ).append(Component.newline()).append(messages.render("<dark_gray>━━━━━━━━━━━━</dark_gray>"));
        if (!snapshot.initialized()) {
            return result.append(Component.newline())
                    .append(messages.render("<yellow>Calcul asynchrone en cours...</yellow>"));
        }
        List<LeaderboardEntry> entries = snapshot.entries(type);
        int maximum = Math.min(settings.topEntries(), entries.size());
        if (maximum == 0) {
            return result.append(Component.newline())
                    .append(messages.render("<gray>Aucune donnée classée</gray>"));
        }
        for (int index = 0; index < maximum; index++) {
            LeaderboardEntry entry = entries.get(index);
            result = result.append(Component.newline()).append(messages.render(
                    color(entry.position()) + "#<position></color> <white><player></white> <dark_gray>—</dark_gray> <yellow><value></yellow>",
                    Placeholder.unparsed("position", Integer.toString(entry.position())),
                    Placeholder.unparsed("player", entry.playerName()),
                    Placeholder.unparsed("value", values.format(type, entry.value()))
            ));
        }
        return result;
    }

    private String color(int position) {
        return switch (position) {
            case 1 -> "<color:#FFD45A>";
            case 2 -> "<color:#D5D9E2>";
            case 3 -> "<color:#D58A55>";
            default -> "<color:#A882E8>";
        };
    }

    private Location location(LeaderboardType type, World world) {
        Location configuredAnchor = spawnWorld.spawn();
        Location anchor = configuredAnchor.getWorld().getUID().equals(world.getUID())
                ? configuredAnchor
                : world.getSpawnLocation();
        LeaderboardHologramSettings.Position position = settings.position(type);
        return anchor.add(position.offsetX(), position.offsetY(), position.offsetZ());
    }

    private void removeStaleDisplays(World world) {
        for (LeaderboardType type : LeaderboardType.values()) {
            Location location = location(type, world);
            if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                continue;
            }
            world.getNearbyEntities(location, 2.0, 4.0, 2.0).stream()
                    .filter(entity -> entity.getPersistentDataContainer().has(
                            markerKey,
                            PersistentDataType.BYTE
                    ))
                    .forEach(Entity::remove);
        }
    }
}
