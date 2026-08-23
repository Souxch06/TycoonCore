package fr.valoriatycoon.tutorial;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.quests.QuestRarity;
import fr.valoriatycoon.quests.QuestService;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Sequential, persisted and reward-safe guidance active only before rank Citoyen. */
public final class TutorialService {
    private final JavaPlugin plugin;
    private final TutorialSettings settings;
    private final TutorialRepository repository;
    private final InternalEconomyService economy;
    private final QuestService quests;
    private final TycoonService tycoons;
    private final CurrencyFormatter currency;
    private final MessageService messages;
    private final Logger logger;
    private final Map<UUID, TutorialProfile> profiles = new ConcurrentHashMap<>();
    private final Map<PendingKey, Long> pending = new ConcurrentHashMap<>();
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    private volatile boolean running;
    private BukkitTask task;

    public TutorialService(
            JavaPlugin plugin,
            TutorialSettings settings,
            TutorialRepository repository,
            InternalEconomyService economy,
            QuestService quests,
            TycoonService tycoons,
            CurrencyFormatter currency,
            MessageService messages,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Activates online profiles and starts the batched progression/actionbar task. */
    public void start() {
        if (!settings.enabled()) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Tutorial must start on the server thread");
        }
        running = true;
        Bukkit.getOnlinePlayers().forEach(player -> activate(player.getUniqueId()));
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tick,
                settings.tickInterval(),
                settings.tickInterval()
        );
    }

    public CompletableFuture<TutorialProfile> activate(UUID playerId) {
        if (!settings.enabled()) {
            return CompletableFuture.completedFuture(new TutorialProfile(
                    TutorialStep.READY_FOR_RANK,
                    0L,
                    true
            ));
        }
        activePlayers.add(playerId);
        return repository.loadOrCreate(playerId).whenComplete((profile, error) -> {
            if (error != null) {
                activePlayers.remove(playerId);
                logger.log(Level.WARNING, "Could not load tutorial for " + playerId, error);
            } else if (activePlayers.contains(playerId)) {
                profiles.put(playerId, profile);
            }
        });
    }

    public void deactivate(UUID playerId) {
        flushPlayer(playerId);
        activePlayers.remove(playerId);
        profiles.remove(playerId);
    }

    /** Queues progress only when the supplied action is the player's current stage. */
    public void record(UUID playerId, TutorialStep action, long amount) {
        if (amount < 1L || !activePlayers.contains(playerId)) {
            return;
        }
        Tycoon island = tycoons.ownedBy(playerId).orElse(null);
        if (island != null && island.prestige() >= 1) {
            finish(playerId);
            return;
        }
        TutorialProfile profile = profiles.get(playerId);
        if (profile == null || profile.completed() || profile.step() != action) {
            return;
        }
        pending.merge(new PendingKey(playerId, action), amount, TutorialService::saturatingAdd);
    }

    public TutorialProfile profile(UUID playerId) {
        return profiles.getOrDefault(playerId, TutorialProfile.initial());
    }

    /** Flushes this player's pending objective before another authoritative operation. */
    public CompletableFuture<Void> flushPlayer(UUID playerId) {
        Map<PendingKey, Long> snapshot = new HashMap<>();
        for (Map.Entry<PendingKey, Long> entry : pending.entrySet()) {
            if (entry.getKey().playerId().equals(playerId)
                    && pending.remove(entry.getKey(), entry.getValue())) {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }
        return flush(snapshot);
    }

    /**
     * Flushes pending actions and resolves XP/common-quest stages immediately before /rank.
     * Two passes allow XP completion to reveal an already completed common quest.
     */
    public CompletableFuture<Void> prepareForPromotion(
            Player player,
            int observedVanillaLevel
    ) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Runnable preparation = () -> prepareForPromotion(
                player,
                observedVanillaLevel,
                2,
                result
        );
        if (Bukkit.isPrimaryThread()) {
            preparation.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, preparation);
        }
        return result;
    }

    /** Permanently stops guidance after the first successful medieval-rank promotion. */
    public CompletableFuture<TutorialProfile> finish(UUID playerId) {
        pending.keySet().removeIf(key -> key.playerId().equals(playerId));
        TutorialProfile completed = new TutorialProfile(TutorialStep.READY_FOR_RANK, 0L, true);
        profiles.put(playerId, completed);
        return repository.finish(playerId).whenComplete((profile, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not finish tutorial for " + playerId, error);
            }
        });
    }

    public void stop(Duration timeout) {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        Map<PendingKey, Long> snapshot = new HashMap<>(pending);
        pending.clear();
        try {
            flush(snapshot).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Tutorial shutdown flush failed", exception);
        }
        activePlayers.clear();
        profiles.clear();
    }

    private void prepareForPromotion(
            Player player,
            int observedVanillaLevel,
            int remainingPasses,
            CompletableFuture<Void> result
    ) {
        boolean automaticQueued = queueAutomaticObjective(player, observedVanillaLevel);
        flushPlayer(player.getUniqueId()).whenComplete((ignored, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
            } else if (automaticQueued && remainingPasses > 1 && running) {
                plugin.getServer().getScheduler().runTask(
                        plugin,
                        () -> prepareForPromotion(
                                player,
                                observedVanillaLevel,
                                remainingPasses - 1,
                                result
                        )
                );
            } else {
                result.complete(null);
            }
        });
    }

    private boolean queueAutomaticObjective(Player player) {
        return queueAutomaticObjective(player, player.getLevel());
    }

    private boolean queueAutomaticObjective(Player player, int observedVanillaLevel) {
        UUID playerId = player.getUniqueId();
        TutorialProfile profile = profiles.get(playerId);
        if (profile == null || profile.completed() || !profile.step().actionable()) {
            return false;
        }
        TutorialSettings.StepDefinition definition = settings.step(profile.step());
        if (profile.step() == TutorialStep.REACH_VANILLA_LEVEL
                && observedVanillaLevel >= definition.target()) {
            record(playerId, profile.step(), definition.target());
            return true;
        }
        if (profile.step() == TutorialStep.COMPLETE_COMMON_QUEST
                && quests.profile(playerId).available(QuestRarity.COMMON) >= definition.target()) {
            record(playerId, profile.step(), definition.target());
            return true;
        }
        return false;
    }

    private void tick() {
        flushAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            guide(player);
        }
    }

    private void guide(Player player) {
        UUID playerId = player.getUniqueId();
        TutorialProfile profile = profiles.get(playerId);
        if (profile == null || profile.completed()) {
            return;
        }
        Tycoon island = tycoons.ownedBy(playerId).orElse(null);
        if (island != null && island.prestige() >= 1) {
            finish(playerId);
            return;
        }
        if (island == null || island.status() != TycoonStatus.ACTIVE) {
            player.sendActionBar(messages.render(settings.noIslandActionBar()));
            return;
        }
        if (profile.step() == TutorialStep.READY_FOR_RANK) {
            player.sendActionBar(messages.render(settings.readyActionBar()));
            return;
        }

        TutorialSettings.StepDefinition definition = settings.step(profile.step());
        queueAutomaticObjective(player);
        long queued = pending.getOrDefault(new PendingKey(playerId, profile.step()), 0L);
        long visibleProgress = Math.min(definition.target(), saturatingAdd(profile.progress(), queued));
        player.sendActionBar(messages.render(
                settings.stepActionBar(),
                Placeholder.unparsed("objective", definition.objective()),
                Placeholder.unparsed("progress", Long.toString(visibleProgress)),
                Placeholder.unparsed("target", Long.toString(definition.target()))
        ));
    }

    private void flushAll() {
        if (pending.isEmpty()) {
            return;
        }
        Map<PendingKey, Long> snapshot = new HashMap<>(pending);
        snapshot.forEach((key, amount) -> pending.remove(key, amount));
        flush(snapshot);
    }

    private CompletableFuture<Void> flush(Map<PendingKey, Long> snapshot) {
        CompletableFuture<?>[] writes = snapshot.entrySet().stream()
                .map(entry -> persist(entry.getKey(), entry.getValue()))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(writes);
    }

    private CompletableFuture<TutorialProgressUpdate> persist(PendingKey key, long amount) {
        TutorialSettings.StepDefinition definition = settings.step(key.step());
        return repository.advance(key.playerId(), key.step(), amount, definition)
                .whenComplete((update, error) -> {
                    if (error != null) {
                        logger.log(Level.WARNING, "Could not progress tutorial for " + key, error);
                        pending.merge(key, amount, TutorialService::saturatingAdd);
                        return;
                    }
                    if (activePlayers.contains(key.playerId())) {
                        profiles.compute(key.playerId(), (playerId, current) ->
                                shouldReplace(current, update.profile()) ? update.profile() : current
                        );
                    }
                    if (update.rewarded()) {
                        economy.synchronizeCommittedBalance(
                                key.playerId(),
                                update.resultingBalanceCents()
                        );
                        notifyReward(key.playerId(), update);
                    }
                });
    }

    private void notifyReward(UUID playerId, TutorialProgressUpdate update) {
        if (!running) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return;
            }
            TutorialSettings.StepDefinition completed = settings.step(update.completedStep());
            messages.send(
                    player,
                    "tutorial.step-completed",
                    Placeholder.unparsed("objective", completed.objective()),
                    Placeholder.unparsed("reward", currency.format(update.rewardedCents()))
            );
            if (update.profile().step() == TutorialStep.READY_FOR_RANK) {
                messages.send(player, "tutorial.ready-for-rank");
            }
        });
    }

    private boolean shouldReplace(TutorialProfile current, TutorialProfile update) {
        return current == null
                || update.completed()
                || update.step().ordinal() > current.step().ordinal()
                || (update.step() == current.step() && update.progress() >= current.progress());
    }

    private static long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private record PendingKey(UUID playerId, TutorialStep step) {
    }
}
