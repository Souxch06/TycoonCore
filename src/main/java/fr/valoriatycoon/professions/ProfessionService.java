package fr.valoriatycoon.professions;

import fr.valoriatycoon.pets.PetEffect;
import fr.valoriatycoon.pets.PetService;
import fr.valoriatycoon.progression.LevelExperienceCalculator;
import fr.valoriatycoon.ranks.RankBenefitService;
import fr.valoriatycoon.tools.ToolType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Online cache and batched action XP for the four permanent professions. */
public final class ProfessionService {
    private final JavaPlugin plugin;
    private final ProfessionSettings settings;
    private final ProfessionRepository repository;
    private final RankBenefitService rankBenefits;
    private final PetService pets;
    private final Logger logger;
    private final Map<ProfileKey, ProfessionProfile> profiles = new ConcurrentHashMap<>();
    private final Map<ProfileKey, Long> pendingExperience = new ConcurrentHashMap<>();
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask flushTask;

    public ProfessionService(
            JavaPlugin plugin,
            ProfessionSettings settings,
            ProfessionRepository repository,
            RankBenefitService rankBenefits,
            PetService pets,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.rankBenefits = Objects.requireNonNull(rankBenefits, "rankBenefits");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Starts the single periodic profession-XP flush task. */
    public void start() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Profession lifecycle must run on the primary thread");
        }
        flushTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::flushAllAsync,
                settings.progression().flushIntervalTicks(),
                settings.progression().flushIntervalTicks()
        );
    }

    /** Loads and activates all permanent professions for one online player. */
    public CompletableFuture<List<ProfessionProfile>> activate(UUID playerId) {
        activePlayers.add(playerId);
        return repository.loadOrCreate(playerId).whenComplete((loaded, error) -> {
            if (error != null) {
                activePlayers.remove(playerId);
                return;
            }
            if (activePlayers.contains(playerId)) {
                for (ProfessionProfile profile : loaded) {
                    profiles.put(new ProfileKey(playerId, profile.type()), profile);
                }
            }
        });
    }

    /** Flushes and evicts one disconnecting player's online profiles. */
    public void deactivate(UUID playerId) {
        flushPlayer(playerId);
        activePlayers.remove(playerId);
        for (ProfessionType type : ProfessionType.values()) {
            profiles.remove(new ProfileKey(playerId, type));
        }
    }

    /** Queues profession XP for one validated action made with the associated tool. */
    public void queueAction(UUID playerId, ToolType toolType) {
        queueAction(playerId, toolType, 1);
    }

    /** Queues one or more equivalent actions after a special pet reward proc. */
    public void queueAction(UUID playerId, ToolType toolType, int actionCount) {
        if (!activePlayers.contains(playerId) || actionCount < 1) {
            return;
        }
        ProfessionType type = ProfessionType.fromTool(toolType);
        long experience = multiply(
                settings.definition(type).experiencePerAction(),
                rankBenefits.professionExperienceMultiplier(playerId)
                        .multiply(pets.multiplier(playerId, PetEffect.PROFESSION_EXPERIENCE))
                        .multiply(BigDecimal.valueOf(actionCount))
        );
        pendingExperience.merge(
                new ProfileKey(playerId, type),
                experience,
                ProfessionService::saturatingAdd
        );
    }

    /** Returns the online profile including XP not yet flushed to SQLite. */
    public ProfessionProfile profile(UUID playerId, ProfessionType type) {
        ProfileKey key = new ProfileKey(playerId, type);
        ProfessionProfile persisted = profiles.getOrDefault(key, new ProfessionProfile(type, 1, 0L));
        long pending = pendingExperience.getOrDefault(key, 0L);
        if (pending == 0L) {
            return persisted;
        }
        LevelExperienceCalculator.Progress progress = LevelExperienceCalculator.add(
                persisted.level(),
                persisted.experience(),
                pending,
                settings.progression()
        );
        return new ProfessionProfile(type, progress.level(), progress.experience());
    }

    /** Returns profession XP needed to leave one current level. */
    public long requiredExperience(int level) {
        return LevelExperienceCalculator.requiredForNextLevel(level, settings.progression());
    }

    /** Returns the validated immutable profession settings. */
    public ProfessionSettings settings() {
        return settings;
    }

    /** Flushes all pending XP for one player before an authoritative rank check. */
    public CompletableFuture<Void> flushPlayer(UUID playerId) {
        Map<ProfileKey, Long> snapshot = new HashMap<>();
        for (Map.Entry<ProfileKey, Long> entry : pendingExperience.entrySet()) {
            if (entry.getKey().playerId().equals(playerId)
                    && pendingExperience.remove(entry.getKey(), entry.getValue())) {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }
        return CompletableFuture.allOf(flushSnapshot(snapshot).toArray(CompletableFuture[]::new));
    }

    /** Stops batching and waits up to the supplied timeout for final writes. */
    public void stopAndFlush(Duration timeout) {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        Map<ProfileKey, Long> snapshot = new HashMap<>(pendingExperience);
        snapshot.forEach((key, experience) -> pendingExperience.remove(key, experience));
        List<CompletableFuture<ProfessionProfile>> futures = flushSnapshot(snapshot);
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while flushing profession XP", exception);
        } catch (ExecutionException | TimeoutException exception) {
            logger.log(Level.SEVERE, "Profession shutdown flush did not complete", exception);
        }
        activePlayers.clear();
        profiles.clear();
    }

    private void flushAllAsync() {
        if (pendingExperience.isEmpty()) {
            return;
        }
        Map<ProfileKey, Long> snapshot = new HashMap<>(pendingExperience);
        snapshot.forEach((key, experience) -> pendingExperience.remove(key, experience));
        flushSnapshot(snapshot);
    }

    private List<CompletableFuture<ProfessionProfile>> flushSnapshot(Map<ProfileKey, Long> snapshot) {
        List<CompletableFuture<ProfessionProfile>> futures = new ArrayList<>(snapshot.size());
        snapshot.forEach((key, experience) -> {
            CompletableFuture<ProfessionProfile> future = repository
                    .addExperience(key.playerId(), key.type(), experience)
                    .whenComplete((profile, error) -> {
                        if (error != null) {
                            logger.log(Level.SEVERE, "Could not persist profession XP for " + key, error);
                            pendingExperience.merge(
                                    key,
                                    experience,
                                    ProfessionService::saturatingAdd
                            );
                        } else if (activePlayers.contains(key.playerId())) {
                            profiles.put(key, profile);
                        }
                    });
            futures.add(future);
        });
        return futures;
    }

    private long multiply(long base, BigDecimal multiplier) {
        try {
            BigDecimal exact = BigDecimal.valueOf(base).multiply(multiplier);
            BigDecimal floored = exact.setScale(0, RoundingMode.DOWN);
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

    private static long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private record ProfileKey(UUID playerId, ProfessionType type) {
    }
}
