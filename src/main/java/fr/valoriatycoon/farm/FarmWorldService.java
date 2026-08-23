package fr.valoriatycoon.farm;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

/** Creates generated farm worlds and provides indexed, cooldown-protected teleport access. */
public final class FarmWorldService {
    private final FarmSettings settings;
    private final FarmZoneAccessPolicy accessPolicy;
    private final Logger logger;
    private final Map<String, FarmWorld> farmsById = new LinkedHashMap<>();
    private final Map<UUID, FarmWorld> farmsByWorldId = new LinkedHashMap<>();
    private final ConcurrentHashMap<UUID, Long> teleportDeadlines = new ConcurrentHashMap<>();

    public FarmWorldService(
            FarmSettings settings,
            FarmZoneAccessPolicy accessPolicy,
            Logger logger
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Must run on the Bukkit primary thread because world creation is server-owned. */
    public void initialize() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Farm worlds must be initialized on the primary thread");
        }
        for (FarmDefinition definition : settings.farms().values()) {
            if (!definition.enabled()) {
                continue;
            }
            World world = createOrLoad(definition);
            configureWorld(world, definition);
            Map<Integer, Location> zoneDestinations = new LinkedHashMap<>();
            for (FarmZoneDefinition zone : definition.zones()) {
                zoneDestinations.put(zone.index(), new Location(
                        world,
                        zone.centerX() + 0.5,
                        definition.generation().spawnY(),
                        zone.centerZ() + 0.5,
                        definition.spawnYaw(),
                        0.0F
                ));
            }
            Location destination = definition.zones().isEmpty()
                    ? new Location(
                            world,
                            0.5,
                            definition.generation().spawnY(),
                            0.5,
                            definition.spawnYaw(),
                            0.0F
                    )
                    : zoneDestinations.get(1).clone();
            FarmWorld farmWorld = new FarmWorld(
                    definition,
                    world,
                    destination,
                    zoneDestinations
            );
            farmsById.put(definition.id(), farmWorld);
            farmsByWorldId.put(world.getUID(), farmWorld);
        }
        logger.info("Loaded " + farmsById.size() + " generated public farm worlds.");
    }

    public Optional<FarmWorld> farm(String farmId) {
        return Optional.ofNullable(farmsById.get(farmId));
    }

    public Optional<FarmWorld> farm(World world) {
        return Optional.ofNullable(farmsByWorldId.get(world.getUID()));
    }

    public Map<String, FarmWorld> farms() {
        return Map.copyOf(farmsById);
    }

    public FarmTeleportAttempt teleport(Player player, String farmId) {
        FarmWorld farm = farmsById.get(farmId);
        if (farm == null) {
            return FarmTeleportAttempt.cooldown(0L);
        }
        if (farm.definition().zoned()) {
            return teleport(player, farmId, 1);
        }
        return teleportTo(player, farm.destination());
    }

    public FarmTeleportAttempt teleport(Player player, String farmId, int zoneIndex) {
        FarmWorld farm = farmsById.get(farmId);
        FarmZoneDefinition zone = farm == null
                ? null
                : farm.definition().zone(zoneIndex).orElse(null);
        if (farm == null
                || zone == null
                || !player.hasPermission("tycoon.bypass")
                && !accessPolicy.canAccess(player.getUniqueId(), zone)) {
            return FarmTeleportAttempt.cooldown(0L);
        }
        Location destination = farm.zoneDestination(zoneIndex).orElse(null);
        return destination == null
                ? FarmTeleportAttempt.cooldown(0L)
                : teleportTo(player, destination);
    }

    public int currentRank(UUID playerId) {
        return accessPolicy.currentRank(playerId);
    }

    public boolean canAccess(UUID playerId, FarmZoneDefinition zone) {
        return accessPolicy.canAccess(playerId, zone);
    }

    private FarmTeleportAttempt teleportTo(Player player, Location destination) {
        Duration cooldown = Duration.ofSeconds(settings.teleport().cooldownSeconds());
        long waitSeconds = acquireTeleport(player.getUniqueId(), cooldown);
        if (waitSeconds > 0) {
            return FarmTeleportAttempt.cooldown(waitSeconds);
        }
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        return FarmTeleportAttempt.accepted(player.teleportAsync(
                destination,
                PlayerTeleportEvent.TeleportCause.COMMAND
        ));
    }

    public void releasePlayer(UUID playerId) {
        teleportDeadlines.remove(playerId);
    }

    private World createOrLoad(FarmDefinition definition) {
        World loaded = Bukkit.getWorld(definition.worldName());
        if (loaded != null) {
            logger.info("Using existing farm world " + definition.worldName() + ".");
            return loaded;
        }
        WorldCreator creator = new WorldCreator(definition.worldName())
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .seed(definition.seed())
                .generateStructures(false)
                .generator(new GeneratedFarmChunkGenerator(definition.generation()));
        World created = creator.createWorld();
        if (created == null) {
            throw new IllegalStateException("Paper could not create farm world " + definition.worldName());
        }
        logger.info("Generated farm world " + definition.worldName() + " (" + definition.type() + ").");
        return created;
    }

    private void configureWorld(World world, FarmDefinition definition) {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        setRule(world, GameRules.SPAWN_MOBS, false);
        setRule(world, GameRules.ADVANCE_TIME, false);
        setRule(world, GameRules.ADVANCE_WEATHER, false);
        setRule(world, GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        setRule(world, GameRules.MOB_GRIEFING, false);
        setRule(world, GameRules.KEEP_INVENTORY, true);
        setRule(world, GameRules.RANDOM_TICK_SPEED, 0);
        world.getWorldBorder().setCenter(0.0, 0.0);
        world.getWorldBorder().setSize(definition.borderSize());
        FarmZoneDefinition firstZone = definition.zone(1).orElse(null);
        world.setSpawnLocation(
                firstZone == null ? 0 : firstZone.centerX(),
                definition.generation().spawnY(),
                firstZone == null ? 0 : firstZone.centerZ()
        );
    }

    private <T> void setRule(World world, GameRule<T> rule, T value) {
        if (!world.setGameRule(rule, value)) {
            logger.warning("Could not set game rule " + rule.getKey() + " in " + world.getName());
        }
    }

    private long acquireTeleport(UUID playerId, Duration cooldown) {
        long cooldownNanos = cooldown.toNanos();
        if (cooldownNanos <= 0) {
            return 0L;
        }
        long now = System.nanoTime();
        long deadline = teleportDeadlines.getOrDefault(playerId, 0L);
        if (deadline > now) {
            return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(deadline - now - 1L) + 1L);
        }
        teleportDeadlines.put(playerId, now + cooldownNanos);
        return 0L;
    }
}
