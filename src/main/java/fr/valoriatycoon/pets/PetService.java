package fr.valoriatycoon.pets;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.InternalEconomyService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/** Cached pet ownership, batched XP, passive effects and non-persistent follower visuals. */
public final class PetService implements Listener {
    private final JavaPlugin plugin;
    private final PetSettings settings;
    private final PetRepository repository;
    private final PetEggService eggItems;
    private final InternalEconomyService economy;
    private final MessageService messages;
    private final Executor mainThread;
    private final Logger logger;
    private final NamespacedKey visualKey;
    private final Map<UUID, Map<String, PetProfile>> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pendingExperience = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, PetEgg>> pendingEggs = new ConcurrentHashMap<>();
    private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Mob> visuals = new HashMap<>();
    private BukkitTask flushTask;
    private BukkitTask followTask;
    private boolean started;

    public PetService(
            JavaPlugin plugin,
            PetSettings settings,
            PetRepository repository,
            PetEggService eggItems,
            InternalEconomyService economy,
            MessageService messages,
            Executor mainThread,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.eggItems = Objects.requireNonNull(eggItems, "eggItems");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.visualKey = new NamespacedKey(plugin, "pet_visual");
    }

    /** Rebuilds the complete in-memory ownership cache from asynchronous startup data. */
    public void initialize(PetSnapshot snapshot) {
        profiles.clear();
        pendingEggs.clear();
        for (PetProfile profile : snapshot.pets()) {
            if (settings.pets().containsKey(profile.petId())) {
                put(profile);
            }
        }
        for (PetEgg egg : snapshot.pendingEggs()) {
            if (settings.pets().containsKey(egg.petId())) {
                pendingEggs.compute(egg.recipientId(), (ignored, current) -> {
                    Map<UUID, PetEgg> updated = new LinkedHashMap<>();
                    if (current != null) {
                        updated.putAll(current);
                    }
                    updated.put(egg.eggId(), egg);
                    return Map.copyOf(updated);
                });
            }
        }
    }

    /** Starts one XP flush task and one lightweight follower task. */
    public void start() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Pet lifecycle must start on the primary thread");
        }
        started = true;
        onlinePlayers.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList());
        flushTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::flushAll,
                settings.progression().flushIntervalTicks(),
                settings.progression().flushIntervalTicks()
        );
        followTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::followVisuals,
                1L,
                settings.progression().followIntervalTicks()
        );
        Bukkit.getOnlinePlayers().forEach(player -> {
            refreshVisual(player);
            deliverPendingEggs(player);
        });
    }

    /** Stops visuals and flushes pending XP without running SQL on the server thread. */
    public void stop(Duration timeout) {
        started = false;
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        visuals.values().forEach(Entity::remove);
        visuals.clear();
        Map<UUID, Long> snapshot = new HashMap<>(pendingExperience);
        snapshot.forEach((playerId, experience) -> pendingExperience.remove(playerId, experience));
        List<CompletableFuture<PetProfile>> futures = flush(snapshot);
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while flushing pet XP", exception);
        } catch (ExecutionException | TimeoutException exception) {
            logger.log(Level.SEVERE, "Pet XP shutdown flush did not complete", exception);
        }
    }

    public PetSettings settings() {
        return settings;
    }

    /** Returns every owned pet with pending XP projected into its active profile. */
    public List<PetProfile> pets(UUID playerId) {
        Map<String, PetProfile> owned = profiles.getOrDefault(playerId, Map.of());
        List<PetProfile> result = new ArrayList<>(owned.size());
        for (PetDefinition definition : settings.pets().values()) {
            PetProfile profile = owned.get(definition.id());
            if (profile != null) {
                result.add(profile.active() ? projected(profile) : profile);
            }
        }
        return List.copyOf(result);
    }

    public PetProfile profile(UUID playerId, String petId) {
        PetProfile profile = profiles.getOrDefault(playerId, Map.of()).get(petId);
        return profile != null && profile.active() ? projected(profile) : profile;
    }

    public PetProfile activePet(UUID playerId) {
        for (PetProfile profile : profiles.getOrDefault(playerId, Map.of()).values()) {
            if (profile.active()) {
                return projected(profile);
            }
        }
        return null;
    }

    public int count(UUID playerId) {
        return profiles.getOrDefault(playerId, Map.of()).size();
    }

    /** Opens the pet crate with one authenticated physical key identifier. */
    public CompletableFuture<PetOperationResult> openCrate(UUID playerId, UUID keyId) {
        return flushPlayer(playerId)
                .thenCompose(ignored -> repository.openCrate(playerId, keyId))
                .thenApplyAsync(result -> {
                    if (result.successful() && result.egg() != null) {
                        addPendingEgg(result.egg());
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            deliverEgg(player, result.egg(), true, "pets.egg-obtained");
                        }
                    }
                    return result;
                }, mainThread);
    }

    /** Redeems one authenticated egg and activates the pet without rerolling its variant. */
    public CompletableFuture<PetOperationResult> redeemEgg(
            UUID playerId,
            PetEggService.EggToken token
    ) {
        return flushPlayer(playerId)
                .thenCompose(ignored -> repository.redeemEgg(
                        playerId,
                        token.eggId(),
                        token.petId(),
                        token.chromatic()
                ))
                .thenApplyAsync(result -> {
                    if (result.successful() && result.pet() != null) {
                        profiles.compute(playerId, (ignored, current) -> {
                            Map<String, PetProfile> updated = new LinkedHashMap<>();
                            if (current != null) {
                                current.forEach((id, profile) -> updated.put(id, profile.withActive(false)));
                            }
                            updated.put(result.pet().petId(), result.pet());
                            return Map.copyOf(updated);
                        });
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            refreshVisual(player);
                        }
                    }
                    return result;
                }, mainThread);
    }

    /** Pays to turn an owned pet back into an egg while preserving its variant. */
    public CompletableFuture<PetOperationResult> reclaim(UUID playerId, String petId) {
        return flushPlayer(playerId)
                .thenCompose(ignored -> repository.reclaim(
                        playerId,
                        petId,
                        settings.reclaim().moneyCostCents()
                ))
                .thenApplyAsync(result -> {
                    if (result.successful() && result.egg() != null) {
                        profiles.computeIfPresent(playerId, (ignored, current) -> {
                            Map<String, PetProfile> updated = new LinkedHashMap<>(current);
                            updated.remove(petId);
                            return Map.copyOf(updated);
                        });
                        economy.synchronizeCommittedBalance(playerId, result.balanceCents());
                        addPendingEgg(result.egg());
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            refreshVisual(player);
                            deliverEgg(player, result.egg(), false, "pets.egg-reclaimed");
                        }
                    }
                    return result;
                }, mainThread);
    }

    public CompletableFuture<PetOperationResult> activate(UUID playerId, String petId) {
        return flushPlayer(playerId)
                .thenCompose(ignored -> repository.activate(playerId, petId))
                .thenApplyAsync(result -> {
                    if (result.successful() && result.pet() != null) {
                        profiles.compute(playerId, (ignored, current) -> {
                            Map<String, PetProfile> updated = new LinkedHashMap<>();
                            if (current != null) {
                                current.forEach((id, profile) -> updated.put(id, profile.withActive(false)));
                            }
                            updated.put(petId, result.pet());
                            return Map.copyOf(updated);
                        });
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            refreshVisual(player);
                        }
                    }
                    return result;
                }, mainThread);
    }

    /** Queues pet XP from one valid manual tool action. */
    public void queueToolAction(UUID playerId) {
        if (onlinePlayers.contains(playerId) && activePet(playerId) != null) {
            pendingExperience.merge(
                    playerId,
                    settings.progression().experiencePerToolAction(),
                    PetService::saturatingAdd
            );
        }
    }

    /** Queues pet XP from a generator cycle, including when another member loads the island. */
    public void queueGeneratorCycle(UUID playerId) {
        if (activePet(playerId) != null) {
            pendingExperience.merge(
                    playerId,
                    settings.progression().experiencePerGeneratorCycle(),
                    PetService::saturatingAdd
            );
        }
    }

    public BigDecimal multiplier(UUID playerId, PetEffect effect) {
        return BigDecimal.ONE.add(effectValue(playerId, effect));
    }

    public boolean roll(UUID playerId, PetEffect effect) {
        if (!effect.chance()) {
            throw new IllegalArgumentException(effect + " is not a pet chance effect");
        }
        BigDecimal chance = effectValue(playerId, effect).min(BigDecimal.ONE);
        return chance.signum() > 0
                && ThreadLocalRandom.current().nextDouble() < chance.doubleValue();
    }

    public BigDecimal effectValue(UUID playerId, PetEffect effect) {
        PetProfile profile = activePet(playerId);
        if (profile == null) {
            return BigDecimal.ZERO;
        }
        PetDefinition definition = settings.pet(profile.petId());
        return definition.effect(effect).atLevel(profile.level());
    }

    public long requiredExperience(PetProfile profile) {
        PetDefinition definition = settings.pet(profile.petId());
        return PetExperienceCalculator.requiredForNextLevel(
                profile.level(),
                settings.rarity(definition.rarity())
        );
    }

    public CompletableFuture<Void> flushPlayer(UUID playerId) {
        Long experience = pendingExperience.remove(playerId);
        if (experience == null || experience <= 0L) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.addExperience(playerId, experience)
                .thenAccept(profile -> {
                    if (profile != null) {
                        put(profile);
                    }
                })
                .exceptionally(error -> {
                    pendingExperience.merge(playerId, experience, PetService::saturatingAdd);
                    throw new java.util.concurrent.CompletionException(error);
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        onlinePlayers.add(event.getPlayer().getUniqueId());
        if (started) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                refreshVisual(event.getPlayer());
                deliverPendingEggs(event.getPlayer());
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        onlinePlayers.remove(playerId);
        flushPlayer(playerId);
        removeVisual(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetInteract(PlayerInteractEntityEvent event) {
        if (isVisual(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetDamage(EntityDamageEvent event) {
        if (isVisual(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetLeash(PlayerLeashEntityEvent event) {
        if (isVisual(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private PetProfile projected(PetProfile profile) {
        long pending = pendingExperience.getOrDefault(profile.playerId(), 0L);
        if (!profile.active() || pending <= 0L) {
            return profile;
        }
        PetDefinition definition = settings.pet(profile.petId());
        PetExperienceCalculator.Progress progress = PetExperienceCalculator.add(
                profile.level(),
                profile.experience(),
                pending,
                settings.rarity(definition.rarity())
        );
        return profile.withProgress(progress.level(), progress.experience());
    }

    private void flushAll() {
        if (pendingExperience.isEmpty()) {
            return;
        }
        Map<UUID, Long> snapshot = new HashMap<>(pendingExperience);
        snapshot.forEach((playerId, experience) -> pendingExperience.remove(playerId, experience));
        flush(snapshot);
    }

    private List<CompletableFuture<PetProfile>> flush(Map<UUID, Long> snapshot) {
        List<CompletableFuture<PetProfile>> futures = new ArrayList<>(snapshot.size());
        snapshot.forEach((playerId, experience) -> {
            CompletableFuture<PetProfile> future = repository.addExperience(playerId, experience)
                    .whenComplete((profile, error) -> {
                        if (error != null) {
                            logger.log(Level.SEVERE, "Could not persist pet XP for " + playerId, error);
                            pendingExperience.merge(playerId, experience, PetService::saturatingAdd);
                        } else if (profile != null) {
                            put(profile);
                        }
                    });
            futures.add(future);
        });
        return futures;
    }

    private void addPendingEgg(PetEgg egg) {
        pendingEggs.compute(egg.recipientId(), (ignored, current) -> {
            Map<UUID, PetEgg> updated = new LinkedHashMap<>();
            if (current != null) {
                updated.putAll(current);
            }
            updated.put(egg.eggId(), egg);
            return Map.copyOf(updated);
        });
    }

    private void deliverPendingEggs(Player player) {
        for (PetEgg egg : pendingEggs.getOrDefault(player.getUniqueId(), Map.of()).values()) {
            deliverEgg(player, egg, false, "pets.egg-recovered");
        }
    }

    private void deliverEgg(
            Player player,
            PetEgg egg,
            boolean announceChromatic,
            String messageKey
    ) {
        eggItems.give(player, egg);
        pendingEggs.computeIfPresent(egg.recipientId(), (ignored, current) -> {
            Map<UUID, PetEgg> updated = new LinkedHashMap<>(current);
            updated.remove(egg.eggId());
            return updated.isEmpty() ? null : Map.copyOf(updated);
        });
        repository.markEggDelivered(egg.eggId()).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not mark pet egg delivered " + egg.eggId(), error);
            return null;
        });
        PetDefinition definition = settings.pet(egg.petId());
        messages.send(
                player,
                messageKey,
                Placeholder.component("pet", messages.render(definition.displayName())),
                Placeholder.unparsed("variant", egg.chromatic() ? "chromatique" : "normal")
        );
        if (announceChromatic && egg.chromatic()) {
            Bukkit.broadcast(messages.component(
                    "pets.chromatic-broadcast",
                    false,
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.component("pet", messages.render(definition.displayName()))
            ));
        }
    }

    private void put(PetProfile profile) {
        profiles.compute(profile.playerId(), (ignored, current) -> {
            Map<String, PetProfile> updated = new LinkedHashMap<>();
            if (current != null) {
                updated.putAll(current);
            }
            updated.put(profile.petId(), profile);
            return Map.copyOf(updated);
        });
    }

    private void followVisuals() {
        for (UUID playerId : List.copyOf(onlinePlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || activePet(playerId) == null) {
                removeVisual(playerId);
                continue;
            }
            Mob visual = visuals.get(playerId);
            if (visual == null || !visual.isValid() || visual.getWorld() != player.getWorld()) {
                refreshVisual(player);
                visual = visuals.get(playerId);
            }
            if (visual == null) {
                continue;
            }
            Location target = followerLocation(player);
            visual.teleport(target);
        }
    }

    private void refreshVisual(Player player) {
        removeVisual(player.getUniqueId());
        PetProfile profile = activePet(player.getUniqueId());
        if (profile == null || !player.isOnline()) {
            return;
        }
        PetDefinition definition = settings.pet(profile.petId());
        Entity spawned = player.getWorld().spawnEntity(followerLocation(player), definition.entityType());
        if (!(spawned instanceof Mob mob)) {
            spawned.remove();
            logger.warning("Configured pet entity is not a Mob: " + definition.entityType());
            return;
        }
        mob.getPersistentDataContainer().set(visualKey, PersistentDataType.BYTE, (byte) 1);
        mob.setAI(false);
        mob.setGravity(false);
        mob.setInvulnerable(true);
        mob.setSilent(true);
        mob.setCollidable(false);
        mob.setPersistent(false);
        mob.setGlowing(profile.chromatic());
        mob.customName(messages.render(
                profile.chromatic()
                        ? "<aqua><bold>Chromatique</bold></aqua> " + definition.displayName()
                        : definition.displayName()
        ));
        mob.setCustomNameVisible(true);
        visuals.put(player.getUniqueId(), mob);
    }

    private Location followerLocation(Player player) {
        Location target = player.getLocation().clone();
        Vector direction = target.getDirection().setY(0.0);
        if (direction.lengthSquared() > 0.0) {
            direction.normalize().multiply(-1.5);
            target.add(direction);
        }
        return target.add(0.0, 0.35, 0.0);
    }

    private boolean isVisual(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(visualKey, PersistentDataType.BYTE);
    }

    private void removeVisual(UUID playerId) {
        Mob visual = visuals.remove(playerId);
        if (visual != null && visual.isValid()) {
            visual.remove();
        }
    }

    private static long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
