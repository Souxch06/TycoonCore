package fr.valoriatycoon.tycoon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

/** Creates empty private Skyblock worlds without deleting any existing world data. */
public final class TycoonWorldService {
    private final TycoonSettings settings;
    private final Supplier<Location> safeSpawnSupplier;
    private final Logger logger;
    private final Map<String, World> worlds = new HashMap<>();

    public TycoonWorldService(
            TycoonSettings settings,
            Supplier<Location> safeSpawnSupplier,
            Logger logger
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.safeSpawnSupplier = Objects.requireNonNull(safeSpawnSupplier, "safeSpawnSupplier");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void initialize() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Tycoon worlds must initialize on the primary thread");
        }
        Map<String, List<TycoonPlotGroup>> byWorld = settings.groups().values().stream()
                .collect(java.util.stream.Collectors.groupingBy(TycoonPlotGroup::worldName));
        byWorld.forEach((worldName, groups) -> {
            TycoonPlotGroup generatorSettings = groups.getFirst();
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                world = new WorldCreator(worldName)
                        .environment(World.Environment.NORMAL)
                        .type(WorldType.NORMAL)
                        .seed(generatorSettings.seed())
                        .generateStructures(false)
                        .generator(new TycoonVoidGenerator())
                        .createWorld();
            }
            if (world == null) {
                throw new IllegalStateException("Paper could not create Tycoon world " + worldName);
            }
            configure(world, groups);
            worlds.put(worldName, world);
            logger.info("Loaded generated Tycoon world " + worldName + " with " + groups.size() + " plot grid(s).");
        });
    }

    public World world(String name) {
        World world = worlds.get(name);
        if (world == null) {
            throw new IllegalArgumentException("Tycoon world is not loaded: " + name);
        }
        return world;
    }

    public boolean isTycoonWorld(World world) {
        return worlds.containsKey(world.getName());
    }

    public int referenceFloor(String worldName) {
        return settings.groups().values().stream()
                .filter(group -> group.worldName().equals(worldName))
                .mapToInt(TycoonPlotGroup::floorY)
                .min()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Tycoon world " + worldName));
    }

    public Location safeSpawn() {
        return safeSpawnSupplier.get().clone();
    }

    private void configure(World world, List<TycoonPlotGroup> groups) {
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

        int minimumX = groups.stream().mapToInt(TycoonPlotGroup::originX).min().orElse(0);
        int minimumZ = groups.stream().mapToInt(TycoonPlotGroup::originZ).min().orElse(0);
        int maximumX = groups.stream().mapToInt(this::maximumX).max().orElse(0);
        int maximumZ = groups.stream().mapToInt(this::maximumZ).max().orElse(0);
        double centerX = minimumX + (maximumX - minimumX) / 2.0;
        double centerZ = minimumZ + (maximumZ - minimumZ) / 2.0;
        double requiredSize = Math.max(maximumX - minimumX + 64.0, maximumZ - minimumZ + 64.0);
        double configuredSize = groups.stream().mapToDouble(TycoonPlotGroup::worldBorderSize).max().orElse(requiredSize);
        world.getWorldBorder().setCenter(centerX, centerZ);
        world.getWorldBorder().setSize(Math.max(requiredSize, configuredSize));

        TycoonPlotGroup first = groups.getFirst();
        TycoonPlotGroup.Bounds firstPlot = first.bounds(0);
        world.setSpawnLocation(firstPlot.centerX(), first.floorY() + 1, firstPlot.centerZ());
    }

    private int maximumX(TycoonPlotGroup group) {
        return group.bounds(group.columns() - 1).maximumX();
    }

    private int maximumZ(TycoonPlotGroup group) {
        int rows = (int) Math.ceil((double) group.maximumPlots() / group.columns());
        int lastRowIndex = Math.min(group.maximumPlots() - 1, (rows - 1) * group.columns());
        return group.bounds(lastRowIndex).maximumZ();
    }

    private <T> void setRule(World world, GameRule<T> rule, T value) {
        if (!world.setGameRule(rule, value)) {
            logger.warning("Could not set " + rule.getKey() + " in " + world.getName());
        }
    }
}
