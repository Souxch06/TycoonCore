package fr.valoriatycoon.machines;

import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.pets.PetEffect;
import fr.valoriatycoon.pets.PetService;
import fr.valoriatycoon.ranks.RankService;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Loaded-chunk production engine and write-through machine cache. */
public final class MachineService {
    private final JavaPlugin plugin;
    private final MachineSettings settings;
    private final MachineRepository repository;
    private final InternalEconomyService economy;
    private final RankService ranks;
    private final PetService pets;
    private final Executor mainThread;
    private final Logger logger;
    private final Map<UUID, PlacedMachine> byId = new HashMap<>();
    private final Map<MachinePosition, PlacedMachine> byPosition = new HashMap<>();
    private final Set<UUID> cyclesInFlight = new HashSet<>();
    private final Set<MachinePosition> placementsInFlight = new HashSet<>();
    private final Map<UUID, Integer> pendingPlacementsByTycoon = new HashMap<>();
    private final PriorityQueue<DueEntry> due = new PriorityQueue<>(Comparator.comparingLong(DueEntry::dueAt));
    private BukkitTask task;

    public MachineService(
            JavaPlugin plugin,
            MachineSettings settings,
            MachineRepository repository,
            InternalEconomyService economy,
            RankService ranks,
            PetService pets,
            Executor mainThread,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void initialize(MachineSnapshot snapshot) {
        byId.clear();
        byPosition.clear();
        placementsInFlight.clear();
        pendingPlacementsByTycoon.clear();
        due.clear();
        snapshot.machines().forEach(this::cache);
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public CompletableFuture<PlacedMachine> place(
            UUID tycoonId,
            UUID ownerId,
            String machineType,
            MachinePosition position
    ) {
        int pending = pendingPlacementsByTycoon.getOrDefault(tycoonId, 0);
        if (count(tycoonId) + pending >= maximumMachines(ownerId)
                || byPosition.containsKey(position)
                || !placementsInFlight.add(position)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Machine placement limit or position conflict"));
        }
        pendingPlacementsByTycoon.put(tycoonId, pending + 1);
        MachineDefinition definition = settings.machine(machineType);
        return repository.create(tycoonId, ownerId, definition, position)
                .thenApplyAsync(machine -> {
                    cache(machine);
                    return machine;
                }, mainThread)
                .whenCompleteAsync((machine, error) -> finishPlacement(tycoonId, position), mainThread);
    }

    public CompletableFuture<Optional<PlacedMachine>> remove(UUID machineId) {
        return repository.remove(machineId).thenApplyAsync(optional -> {
            optional.ifPresent(this::uncache);
            return optional;
        }, mainThread);
    }

    public CompletableFuture<PlacedMachine> toggleAutoSell(UUID machineId) {
        return repository.toggleAutoSell(machineId).thenApplyAsync(machine -> {
            replace(machine);
            return machine;
        }, mainThread);
    }

    /** Returns the pre-collection machine so callers know how many items to give. */
    public CompletableFuture<PlacedMachine> collect(UUID machineId) {
        return repository.collect(machineId).thenApplyAsync(before -> {
            replace(before.withStoredAmount(0L));
            return before;
        }, mainThread);
    }

    public Optional<PlacedMachine> at(MachinePosition position) {
        return Optional.ofNullable(byPosition.get(position));
    }

    public Optional<PlacedMachine> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public int count(UUID tycoonId) {
        return (int) byId.values().stream().filter(machine -> machine.tycoonId().equals(tycoonId)).count();
    }

    /** Returns the base island limit plus permanent slots granted by rank. */
    public int maximumMachines(UUID ownerId) {
        return Math.addExact(
                settings.maximumMachinesPerIsland(),
                ranks.generatorSlotBonus(ownerId)
        );
    }

    /** Returns the rank-boosted output quantity for one production cycle. */
    public long productionAmount(PlacedMachine machine) {
        MachineDefinition definition = settings.machine(machine.machineType());
        try {
            java.math.BigDecimal exact = java.math.BigDecimal.valueOf(definition.outputAmount())
                    .multiply(ranks.generatorProductionMultiplier(machine.ownerId()))
                    .multiply(pets.multiplier(machine.ownerId(), PetEffect.GENERATOR_PRODUCTION));
            if (pets.roll(machine.ownerId(), PetEffect.DOUBLE_GENERATOR_OUTPUT_CHANCE)) {
                exact = exact.multiply(java.math.BigDecimal.valueOf(2L));
            }
            java.math.BigDecimal floored = exact.setScale(0, java.math.RoundingMode.DOWN);
            long result = floored.longValueExact();
            double fractionalChance = exact.subtract(floored).doubleValue();
            if (result < Long.MAX_VALUE
                    && fractionalChance > 0.0
                    && java.util.concurrent.ThreadLocalRandom.current().nextDouble()
                    < fractionalChance) {
                return result + 1L;
            }
            return result;
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    public CompletableFuture<MachineUpgradeResult> purchaseUpgrade(
            UUID machineId,
            UUID ownerId,
            MachineUpgradeType type
    ) {
        PlacedMachine machine = byId.get(machineId);
        if (machine == null) {
            return CompletableFuture.completedFuture(new MachineUpgradeResult(
                    MachineUpgradeStatus.MACHINE_MISSING, null, 0L, 0L
            ));
        }
        int level = type == MachineUpgradeType.SPEED ? machine.speedLevel() : machine.sellPriceLevel();
        int maximum = type == MachineUpgradeType.SPEED
                ? settings.upgrades().speed().maximumLevel()
                : settings.upgrades().sellPrice().maximumLevel();
        long cost = upgradeCost(type, level + 1);
        return repository.purchaseUpgrade(machineId, ownerId, type, maximum, cost)
                .thenApplyAsync(result -> {
                    if (result.successful() && result.machine() != null) {
                        replaceWithoutQueue(result.machine());
                        economy.synchronizeCommittedBalance(ownerId, result.resultingBalanceCents());
                    }
                    return result;
                }, mainThread);
    }

    public long upgradeCost(MachineUpgradeType type, int targetLevel) {
        long index = Math.max(0L, targetLevel - 2L);
        long base = type == MachineUpgradeType.SPEED
                ? settings.upgrades().speed().baseCostCents()
                : settings.upgrades().sellPrice().baseCostCents();
        long step = type == MachineUpgradeType.SPEED
                ? settings.upgrades().speed().costPerLevelCents()
                : settings.upgrades().sellPrice().costPerLevelCents();
        try { return Math.addExact(base, Math.multiplyExact(step, index)); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }

    public long intervalMillis(PlacedMachine machine) {
        MachineDefinition definition = settings.machine(machine.machineType());
        java.math.BigDecimal reduction = settings.upgrades().speed().reductionPerLevel()
                .multiply(java.math.BigDecimal.valueOf(machine.speedLevel() - 1L));
        java.math.BigDecimal multiplier = java.math.BigDecimal.ONE.subtract(reduction)
                .max(settings.upgrades().speed().minimumIntervalMultiplier());
        return java.math.BigDecimal.valueOf(definition.productionInterval().toMillis())
                .multiply(multiplier)
                .setScale(0, java.math.RoundingMode.CEILING)
                .longValueExact();
    }

    public long sellPriceCents(PlacedMachine machine) {
        MachineDefinition definition = settings.machine(machine.machineType());
        java.math.BigDecimal multiplier = java.math.BigDecimal.ONE.add(
                settings.upgrades().sellPrice().bonusPerLevel()
                        .multiply(java.math.BigDecimal.valueOf(machine.sellPriceLevel() - 1L))
        );
        return java.math.BigDecimal.valueOf(definition.outputSellPriceCents())
                .multiply(multiplier)
                .multiply(ranks.revenueMultiplier(machine.ownerId()))
                .multiply(pets.multiplier(machine.ownerId(), PetEffect.MONEY))
                .setScale(0, java.math.RoundingMode.DOWN)
                .longValueExact();
    }

    public MachineSettings settings() {
        return settings;
    }

    private void finishPlacement(UUID tycoonId, MachinePosition position) {
        placementsInFlight.remove(position);
        pendingPlacementsByTycoon.computeIfPresent(tycoonId, (ignored, count) -> count <= 1 ? null : count - 1);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        int processed = 0;
        while (processed < settings.maximumCyclesPerTick() && !due.isEmpty() && due.peek().dueAt() <= now) {
            DueEntry entry = due.poll();
            PlacedMachine machine = byId.get(entry.machineId());
            if (machine == null || machine.nextRunAtMillis() != entry.dueAt() || !cyclesInFlight.add(machine.id())) {
                continue;
            }
            MachineDefinition definition = settings.machine(machine.machineType());
            World world = Bukkit.getWorld(machine.worldName());
            if (world == null || !world.isChunkLoaded(machine.position().chunkX(), machine.position().chunkZ())) {
                cyclesInFlight.remove(machine.id());
                long retry = now + Duration.ofSeconds(5).toMillis();
                PlacedMachine delayed = machine.afterCycle(machine.storedAmount(), retry);
                replaceWithoutQueue(delayed);
                due.add(new DueEntry(machine.id(), retry));
                continue;
            }
            if (world.getBlockAt(machine.x(), machine.y(), machine.z()).getType() != definition.blockMaterial()) {
                cyclesInFlight.remove(machine.id());
                remove(machine.id()).exceptionally(error -> {
                    logger.log(Level.WARNING, "Could not remove missing machine " + machine.id(), error);
                    return Optional.empty();
                });
                continue;
            }
            processed++;
            repository.runCycle(
                    machine.id(),
                    definition,
                    productionAmount(machine),
                    intervalMillis(machine),
                    sellPriceCents(machine)
            ).whenCompleteAsync((result, error) -> completeCycle(machine, result, error), mainThread);
        }
    }

    private void completeCycle(PlacedMachine previous, MachineCycleResult result, Throwable error) {
        cyclesInFlight.remove(previous.id());
        if (error != null || result == null) {
            logger.log(Level.SEVERE, "Machine cycle failed for " + previous.id(), error);
            long retry = System.currentTimeMillis() + Duration.ofSeconds(5).toMillis();
            PlacedMachine delayed = previous.afterCycle(previous.storedAmount(), retry);
            replace(delayed);
            return;
        }
        if (result.status() == MachineCycleStatus.MACHINE_MISSING || result.machine() == null) {
            uncache(previous);
            return;
        }
        if (result.status() == MachineCycleStatus.PRODUCED) {
            pets.queueGeneratorCycle(previous.ownerId());
        }
        replace(result.machine());
        if (result.ownerBalanceCents() >= 0) {
            economy.synchronizeCommittedBalance(previous.ownerId(), result.ownerBalanceCents());
        }
    }

    private void cache(PlacedMachine machine) {
        byId.put(machine.id(), machine);
        byPosition.put(machine.position(), machine);
        due.add(new DueEntry(machine.id(), machine.nextRunAtMillis()));
    }

    private void replace(PlacedMachine machine) {
        replaceWithoutQueue(machine);
        due.add(new DueEntry(machine.id(), machine.nextRunAtMillis()));
    }

    private void replaceWithoutQueue(PlacedMachine machine) {
        PlacedMachine previous = byId.put(machine.id(), machine);
        if (previous != null) byPosition.remove(previous.position());
        byPosition.put(machine.position(), machine);
    }

    private void uncache(PlacedMachine machine) {
        byId.remove(machine.id());
        byPosition.remove(machine.position());
        cyclesInFlight.remove(machine.id());
    }

    private record DueEntry(UUID machineId, long dueAt) {
    }
}
