package fr.valoriatycoon.tools;

import fr.valoriatycoon.crates.ToolCrateRewardSink;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.pets.PetEffect;
import fr.valoriatycoon.pets.PetService;
import fr.valoriatycoon.professions.ProfessionService;
import fr.valoriatycoon.ranks.RankBenefitService;
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

/** Online tool cache, batched XP/coins, multipliers and atomic purchase coordinator. */
public final class ToolProgressionService {
    private final JavaPlugin plugin;
    private final ToolSettings settings;
    private final ToolRepository repository;
    private final InternalEconomyService economy;
    private final ProfessionService professions;
    private final RankBenefitService rankBenefits;
    private final PetService pets;
    private final ToolCrateRewardSink crateRewards;
    private final Logger logger;
    private final ConcurrentHashMap<ToolProfileKey, ToolProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ToolProfileKey, PendingRewards> pendingRewards = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ToolProfileKey, Long> speedCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask flushTask;

    public ToolProgressionService(
            JavaPlugin plugin,
            ToolSettings settings,
            ToolRepository repository,
            InternalEconomyService economy,
            ProfessionService professions,
            RankBenefitService rankBenefits,
            PetService pets,
            ToolCrateRewardSink crateRewards,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.professions = Objects.requireNonNull(professions, "professions");
        this.rankBenefits = Objects.requireNonNull(rankBenefits, "rankBenefits");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.crateRewards = Objects.requireNonNull(crateRewards, "crateRewards");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void start() {
        requirePrimaryThread();
        flushTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::flushAllAsync,
                settings.progression().flushIntervalTicks(),
                settings.progression().flushIntervalTicks()
        );
    }

    public CompletableFuture<List<ToolProfile>> activate(UUID playerId) {
        activePlayers.add(playerId);
        return repository.loadOrCreate(playerId).thenApply(loaded -> {
            if (activePlayers.contains(playerId)) {
                for (ToolProfile profile : loaded) {
                    profiles.put(new ToolProfileKey(playerId, profile.toolType()), profile);
                }
            }
            return loaded;
        });
    }

    public void deactivate(UUID playerId) {
        flushPlayerRewards(playerId);
        activePlayers.remove(playerId);
        for (ToolType type : ToolType.values()) {
            ToolProfileKey key = new ToolProfileKey(playerId, type);
            profiles.remove(key);
            speedCooldowns.remove(key);
        }
    }

    public ToolProfile profile(UUID playerId, ToolType type) {
        ToolProfileKey key = new ToolProfileKey(playerId, type);
        ToolProfile persisted = profiles.getOrDefault(key, defaultProfile(type));
        PendingRewards pending = pendingRewards.get(key);
        if (pending == null) {
            return persisted;
        }
        ToolExperienceCalculator.Progress progress = ToolExperienceCalculator.add(
                persisted.toolLevel(),
                persisted.toolExperience(),
                pending.experience(),
                settings.progression()
        );
        return persisted.withProgress(
                progress.level(),
                progress.experience(),
                saturatingAdd(persisted.specialCoins(), pending.coins())
        );
    }

    public CompletableFuture<ToolUpgradeResult> purchase(
            UUID playerId,
            ToolType type,
            ToolCapability capability,
            ToolUpgradeCurrency currency
    ) {
        if (!settings.capability(capability).appliesTo(type)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(capability + " does not apply to " + type)
            );
        }
        ToolProfileKey key = new ToolProfileKey(playerId, type);
        PendingRewards pending = pendingRewards.remove(key);
        CompletableFuture<ToolProfile> ready = pending == null
                ? CompletableFuture.completedFuture(profiles.getOrDefault(key, defaultProfile(type)))
                : repository.addRewards(
                        playerId, type, pending.experience(), pending.coins()
                ).thenApply(profile -> {
                    profiles.put(key, profile);
                    return profile;
                });
        return ready.thenCompose(current -> repository.purchaseCapability(
                playerId,
                type,
                capability,
                current.capabilityLevel(capability),
                currency
        )).thenApply(result -> {
            profiles.compute(key, (ignored, current) -> (current == null ? defaultProfile(type) : current)
                    .withCapability(capability, result.resultingLevel(), result.toolCoins()));
            if (result.successful() && currency == ToolUpgradeCurrency.BASE_MONEY) {
                economy.synchronizeCommittedBalance(playerId, result.balanceCents());
            }
            return result;
        });
    }

    public CompletableFuture<ToolCoinSpendResult> spendCoins(
            UUID playerId,
            ToolType type,
            long amount,
            String reason
    ) {
        ToolProfileKey key = new ToolProfileKey(playerId, type);
        PendingRewards pending = pendingRewards.remove(key);
        CompletableFuture<ToolProfile> ready = pending == null
                ? CompletableFuture.completedFuture(profiles.getOrDefault(key, defaultProfile(type)))
                : repository.addRewards(playerId, type, pending.experience(), pending.coins())
                        .thenApply(profile -> {
                            profiles.put(key, profile);
                            return profile;
                        });
        return ready.thenCompose(ignored -> repository.spendCoins(playerId, type, amount, reason))
                .thenApply(result -> {
                    profiles.computeIfPresent(key, (ignored, current) -> new ToolProfile(
                            current.toolType(),
                            current.toolLevel(),
                            current.toolExperience(),
                            result.resultingCoins(),
                            current.capabilityLevels()
                    ));
                    return result;
                });
    }

    /** Queues an action outside a public farm; Farm keys are intentionally ineligible. */
    public void queueActionRewards(UUID playerId, ToolType type) {
        queueActionRewards(playerId, type, false);
    }

    /** Queues XP/coins and optionally enables the public-farm physical-key roll. */
    public void queueActionRewards(UUID playerId, ToolType type, boolean publicFarmAction) {
        if (!activePlayers.contains(playerId)) {
            return;
        }
        boolean doubleReward = pets.roll(playerId, PetEffect.DOUBLE_TOOL_REWARD_CHANCE);
        int actionCount = doubleReward ? 2 : 1;
        professions.queueAction(playerId, type, actionCount);
        pets.queueToolAction(playerId);
        ToolDefinition tool = settings.tool(type);
        long experience = multiply(
                tool.experiencePerAction(),
                capabilityValue(playerId, type, ToolCapability.LEVEL_BOOST)
                        .multiply(rankBenefits.toolExperienceMultiplier(playerId))
                        .multiply(pets.multiplier(playerId, PetEffect.TOOL_EXPERIENCE))
                        .multiply(BigDecimal.valueOf(actionCount))
        );
        long coins = multiply(
                tool.coinsPerAction(),
                capabilityValue(playerId, type, ToolCapability.COIN_BOOST)
                        .multiply(rankBenefits.toolCoinMultiplier(playerId))
                        .multiply(pets.multiplier(playerId, PetEffect.TOOL_COINS))
                        .multiply(BigDecimal.valueOf(actionCount))
        );
        ToolProfileKey key = new ToolProfileKey(playerId, type);
        pendingRewards.merge(
                key,
                new PendingRewards(Math.max(1L, experience), Math.max(1L, coins)),
                PendingRewards::merge
        );
        crateRewards.roll(
                playerId,
                publicFarmAction
                        ? capabilityValue(playerId, type, ToolCapability.FARM_KEY_FINDER)
                        : BigDecimal.ZERO,
                capabilityValue(playerId, type, ToolCapability.CRATE_KEY_FINDER)
        );
        trySpeedBurst(key);
    }

    private void trySpeedBurst(ToolProfileKey key) {
        ToolCapabilityDefinition speed = settings.capability(ToolCapability.SPEED_BURST);
        if (!speed.appliesTo(key.toolType())) {
            return;
        }
        BigDecimal chance = capabilityValue(key.playerId(), key.toolType(), ToolCapability.SPEED_BURST);
        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= chance.doubleValue()) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMillis = settings.abilities().speedCooldownSeconds() * 1000L;
        Long deadline = speedCooldowns.get(key);
        if (deadline != null && deadline > now) {
            return;
        }
        org.bukkit.entity.Player player = Bukkit.getPlayer(key.playerId());
        if (player == null) {
            return;
        }
        speedCooldowns.put(key, now + cooldownMillis);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                settings.abilities().speedDurationTicks(),
                settings.abilities().speedAmplifier(),
                false,
                false,
                true
        ));
    }

    public void queueBonusCoins(UUID playerId, ToolType type, long coins) {
        if (coins <= 0 || !activePlayers.contains(playerId)) {
            return;
        }
        long boosted = multiply(
                coins,
                capabilityValue(playerId, type, ToolCapability.COIN_BOOST)
                        .multiply(rankBenefits.toolCoinMultiplier(playerId))
                        .multiply(pets.multiplier(playerId, PetEffect.TOOL_COINS))
        );
        pendingRewards.merge(
                new ToolProfileKey(playerId, type),
                new PendingRewards(0L, boosted),
                PendingRewards::merge
        );
    }

    /** Synchronizes the online cache after another SQLite transaction credits tool coins. */
    public void synchronizeCommittedCoins(UUID playerId, ToolType type, long resultingCoins) {
        if (resultingCoins < 0L) {
            throw new IllegalArgumentException("Committed tool coin balance cannot be negative");
        }
        if (!activePlayers.contains(playerId)) {
            return;
        }
        ToolProfileKey key = new ToolProfileKey(playerId, type);
        profiles.compute(key, (ignored, current) -> {
            ToolProfile profile = current == null ? defaultProfile(type) : current;
            return new ToolProfile(
                    profile.toolType(),
                    profile.toolLevel(),
                    profile.toolExperience(),
                    resultingCoins,
                    profile.capabilityLevels()
            );
        });
    }

    public BigDecimal capabilityValue(UUID playerId, ToolType type, ToolCapability capability) {
        ToolCapabilityDefinition definition = settings.capability(capability);
        if (!definition.appliesTo(type)) {
            return BigDecimal.ZERO;
        }
        int level = profile(playerId, type).capabilityLevel(capability);
        if (level <= 0) {
            return switch (capability) {
                case LEVEL_BOOST, MONEY_BOOST, COIN_BOOST -> BigDecimal.ONE;
                default -> BigDecimal.ZERO;
            };
        }
        return definition.level(level)
                .map(ToolCapabilityDefinition.Level::value)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal moneyMultiplier(UUID playerId, ToolType type) {
        return capabilityValue(playerId, type, ToolCapability.MONEY_BOOST);
    }

    public BigDecimal efficiencyBonus(UUID playerId, ToolType type) {
        return capabilityValue(playerId, type, ToolCapability.EFFICIENCY);
    }

    public long requiredExperience(int toolLevel) {
        return ToolExperienceCalculator.requiredForNextLevel(toolLevel, settings.progression());
    }

    public ToolSettings settings() {
        return settings;
    }

    public void stopAndFlush(Duration timeout) {
        requirePrimaryThread();
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        List<CompletableFuture<ToolProfile>> futures = flushSnapshot(new HashMap<>(pendingRewards));
        pendingRewards.clear();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while flushing tool rewards", exception);
        } catch (ExecutionException | TimeoutException exception) {
            logger.log(Level.SEVERE, "Tool reward shutdown flush did not complete", exception);
        }
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

    private void flushAllAsync() {
        if (pendingRewards.isEmpty()) {
            return;
        }
        Map<ToolProfileKey, PendingRewards> snapshot = new HashMap<>(pendingRewards);
        snapshot.forEach((key, rewards) -> pendingRewards.remove(key, rewards));
        flushSnapshot(snapshot);
    }

    public CompletableFuture<Void> flushPlayerRewards(UUID playerId) {
        Map<ToolProfileKey, PendingRewards> snapshot = new HashMap<>();
        for (Map.Entry<ToolProfileKey, PendingRewards> entry : pendingRewards.entrySet()) {
            if (entry.getKey().playerId().equals(playerId)
                    && pendingRewards.remove(entry.getKey(), entry.getValue())) {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }
        List<CompletableFuture<ToolProfile>> futures = flushSnapshot(snapshot);
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private List<CompletableFuture<ToolProfile>> flushSnapshot(
            Map<ToolProfileKey, PendingRewards> snapshot
    ) {
        List<CompletableFuture<ToolProfile>> futures = new ArrayList<>(snapshot.size());
        snapshot.forEach((key, rewards) -> {
            CompletableFuture<ToolProfile> future = repository
                    .addRewards(key.playerId(), key.toolType(), rewards.experience(), rewards.coins())
                    .whenComplete((profile, error) -> {
                        if (error != null) {
                            logger.log(Level.SEVERE, "Could not persist tool rewards for " + key, error);
                        } else if (activePlayers.contains(key.playerId())) {
                            profiles.put(key, profile);
                        }
                    });
            futures.add(future);
        });
        return futures;
    }

    private ToolProfile defaultProfile(ToolType type) {
        Map<ToolCapability, Integer> levels = new java.util.EnumMap<>(ToolCapability.class);
        for (ToolCapability capability : ToolCapability.values()) {
            ToolCapabilityDefinition definition = settings.capability(capability);
            if (definition.appliesTo(type)) {
                levels.put(capability, definition.initialLevel());
            }
        }
        return new ToolProfile(type, 1, 0L, 0L, levels);
    }

    private void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Tool progression lifecycle must run on the primary thread");
        }
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private record PendingRewards(long experience, long coins) {
        private PendingRewards merge(PendingRewards other) {
            return new PendingRewards(
                    saturatingAdd(experience, other.experience),
                    saturatingAdd(coins, other.coins)
            );
        }
    }
}
