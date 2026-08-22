package fr.valoriatycoon.spawn;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Creates, protects and exposes the generated medieval server hub. */
public final class SpawnWorldService {
    private final SpawnSettings settings;
    private final Logger logger;
    private World world;
    private Location spawn;

    public SpawnWorldService(SpawnSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Creates or loads the spawn world without pre-generating surrounding chunks. */
    public void initialize() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Spawn world must initialize on the primary thread");
        }
        world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            world = new WorldCreator(settings.worldName())
                    .environment(World.Environment.NORMAL)
                    .type(WorldType.NORMAL)
                    .seed(settings.seed())
                    .generateStructures(false)
                    .generator(new MedievalSpawnGenerator(settings))
                    .createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("Paper could not create spawn world " + settings.worldName());
        }
        configureWorld(world);
        spawn = new Location(
                world,
                settings.spawnX() + 0.5,
                settings.groundY() + 2.0,
                settings.spawnZ() + 0.5,
                settings.spawnYaw(),
                0.0F
        );
        world.setSpawnLocation(
                settings.spawnX(),
                settings.groundY() + 1,
                settings.spawnZ()
        );
        logger.info("Loaded generated medieval spawn world " + settings.worldName() + '.');
    }

    public CompletableFuture<Boolean> teleport(Player player) {
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        return player.teleportAsync(spawn(), PlayerTeleportEvent.TeleportCause.COMMAND);
    }

    public Location spawn() {
        if (spawn == null) {
            throw new IllegalStateException("Spawn world has not been initialized");
        }
        return spawn.clone();
    }

    public boolean isSpawnWorld(World candidate) {
        return world != null && world.getUID().equals(candidate.getUID());
    }

    public boolean isProtected(Location location) {
        return location != null
                && isSpawnWorld(location.getWorld())
                && Math.hypot(location.getX(), location.getZ()) <= settings.protectionRadius();
    }

    public Optional<SpawnSettings.PortalDefinition> portalAt(Location location) {
        if (location == null || !isSpawnWorld(location.getWorld())) {
            return Optional.empty();
        }
        return settings.portals().stream()
                .filter(portal -> portal.contains(location.getBlockX(), location.getBlockZ()))
                .findFirst();
    }

    public SpawnSettings settings() {
        return settings;
    }

    private void configureWorld(World configured) {
        configured.setDifficulty(Difficulty.PEACEFUL);
        configured.setTime(6000L);
        configured.setStorm(false);
        configured.setThundering(false);
        setRule(configured, GameRules.SPAWN_MOBS, false);
        setRule(configured, GameRules.ADVANCE_TIME, false);
        setRule(configured, GameRules.ADVANCE_WEATHER, false);
        setRule(configured, GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        setRule(configured, GameRules.MOB_GRIEFING, false);
        setRule(configured, GameRules.KEEP_INVENTORY, true);
        setRule(configured, GameRules.RANDOM_TICK_SPEED, 0);
        setRule(configured, GameRules.PVP, false);
        configured.getWorldBorder().setCenter(0.0, 0.0);
        configured.getWorldBorder().setSize(settings.borderSize());
    }

    private <T> void setRule(World target, GameRule<T> rule, T value) {
        if (!target.setGameRule(rule, value)) {
            logger.warning("Could not set " + rule.getKey() + " in " + target.getName());
        }
    }
}
