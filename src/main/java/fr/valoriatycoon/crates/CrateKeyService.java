package fr.valoriatycoon.crates;

import fr.valoriatycoon.config.MessageService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Issues, rolls and crash-recovers generic physical keys consumed by the reward service. */
public final class CrateKeyService implements Listener, QuestKeyRewardSink, ToolCrateRewardSink {
    private final CrateSettings settings;
    private final CrateKeyRepository repository;
    private final CrateKeyItemService items;
    private final MessageService messages;
    private final Executor mainThread;
    private final BooleanSupplier available;
    private final Logger logger;
    private final java.util.Set<UUID> deliveries = ConcurrentHashMap.newKeySet();

    public CrateKeyService(
            CrateSettings settings,
            CrateKeyRepository repository,
            CrateKeyItemService items,
            MessageService messages,
            Executor mainThread,
            BooleanSupplier available,
            Logger logger
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.items = Objects.requireNonNull(items, "items");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.available = Objects.requireNonNull(available, "available");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Recovers every committed but physically undelivered key for online players. */
    public void start() {
        Bukkit.getOnlinePlayers().forEach(player -> deliverPending(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (available.getAsBoolean()) {
            deliverPending(event.getPlayer().getUniqueId());
        }
    }

    /** Issues a bounded admin/system batch with independent UUID/source references. */
    public CompletableFuture<Void> issue(
            UUID playerId,
            CrateType type,
            int amount,
            String source
    ) {
        if (amount < 1 || amount > 512) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Crate key amount must be between 1 and 512")
            );
        }
        if (type == CrateType.VALORIA
                && !"ADMIN".equals(source)
                && !"LEGENDARY_CRATE".equals(source)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Valoria keys require a store transaction, admin action or Legendary jackpot"
                    )
            );
        }
        CompletableFuture<?>[] futures = new CompletableFuture<?>[amount];
        for (int index = 0; index < amount; index++) {
            futures[index] = repository.issue(
                    playerId,
                    type,
                    source,
                    UUID.randomUUID().toString()
            );
        }
        return CompletableFuture.allOf(futures)
                .thenRun(() -> deliverPending(playerId));
    }

    /**
     * Issues paid Valoria keys exactly once for a web-store transaction.
     * Retrying the same transaction id returns the same key UUIDs and never duplicates delivery.
     */
    public CompletableFuture<Void> issueStorePurchase(
            UUID playerId,
            String transactionId,
            int amount
    ) {
        if (transactionId == null || transactionId.isBlank() || transactionId.length() > 200
                || amount < 1 || amount > 512) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Invalid Valoria store transaction")
            );
        }
        CompletableFuture<?>[] futures = new CompletableFuture<?>[amount];
        for (int index = 0; index < amount; index++) {
            futures[index] = repository.issue(
                    playerId,
                    CrateType.VALORIA,
                    "STORE",
                    transactionId + ':' + index
            );
        }
        return CompletableFuture.allOf(futures).thenRun(() -> deliverPending(playerId));
    }

    /** One idempotent Vote key per external vote identity. */
    public CompletableFuture<Boolean> recordVote(
            String playerName,
            String serviceName,
            String voteReference
    ) {
        String reference = serviceName + ':' + voteReference;
        return repository.findPlayerId(playerName).thenCompose(optional -> {
            if (optional.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            UUID playerId = optional.get();
            return repository.issue(playerId, CrateType.VOTE, "VOTE", reference)
                    .thenApply(key -> {
                        deliverPending(playerId);
                        return true;
                    });
        });
    }

    @Override
    public CompletableFuture<Void> synchronize(
            UUID playerId,
            String questId,
            long totalCompletions
    ) {
        return repository.ensureQuestKeys(playerId, questId, totalCompletions)
                .thenAccept(ignored -> deliverPending(playerId));
    }

    @Override
    public void roll(UUID playerId, BigDecimal farmChance, BigDecimal rarityChance) {
        validateChance(farmChance, "farmChance");
        validateChance(rarityChance, "rarityChance");
        if (roll(farmChance)) {
            issueRolled(playerId, CrateType.FARM, "TOOL_FARM");
        }
        if (roll(rarityChance)) {
            issueRolled(playerId, drawRarity(), "TOOL_RARITY");
        }
    }

    public Map<CrateType, Integer> count(Player player) {
        return items.count(player);
    }

    public CrateSettings settings() {
        return settings;
    }

    /** Re-runs crash-safe physical delivery after another atomic service issues keys. */
    public void recoverPending(UUID playerId) {
        if (available.getAsBoolean()) {
            deliverPending(playerId);
        }
    }

    private void issueRolled(UUID playerId, CrateType type, String source) {
        repository.issue(playerId, type, source, UUID.randomUUID().toString())
                .whenComplete((key, error) -> {
                    if (error != null) {
                        logger.log(Level.WARNING, "Could not issue rolled " + type + " key", error);
                    } else {
                        deliverPending(playerId);
                    }
                });
    }

    private void deliverPending(UUID playerId) {
        if (!deliveries.add(playerId)) {
            return;
        }
        repository.pending(playerId).whenCompleteAsync((keys, error) -> {
            if (error != null) {
                deliveries.remove(playerId);
                logger.log(Level.WARNING, "Could not load pending crate keys for " + playerId, error);
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || keys.isEmpty()) {
                deliveries.remove(playerId);
                return;
            }
            CompletableFuture<?>[] marks = new CompletableFuture<?>[keys.size()];
            for (int index = 0; index < keys.size(); index++) {
                CrateKey key = keys.get(index);
                items.give(player, key);
                marks[index] = repository.markDelivered(key.keyId());
                player.sendMessage(messages.render(
                        "<green>Vous obtenez une <key>.</green>",
                        Placeholder.unparsed("key", "Clé " + key.type().displayName())
                ));
            }
            CompletableFuture.allOf(marks).whenComplete((ignored, markError) -> {
                deliveries.remove(playerId);
                if (markError != null) {
                    logger.log(Level.WARNING, "Could not mark delivered crate keys for " + playerId, markError);
                } else {
                    deliverPending(playerId);
                }
            });
        }, mainThread);
    }

    private CrateType drawRarity() {
        long total = settings.toolRarityWeights().values().stream()
                .mapToLong(Integer::longValue)
                .sum();
        long selected = ThreadLocalRandom.current().nextLong(total);
        for (CrateType type : List.of(
                CrateType.COMMON, CrateType.RARE, CrateType.EPIC, CrateType.LEGENDARY
        )) {
            int weight = settings.toolRarityWeights().get(type);
            if (selected < weight) {
                return type;
            }
            selected -= weight;
        }
        throw new IllegalStateException("Crate rarity weights did not select a type");
    }

    private boolean roll(BigDecimal chance) {
        return chance.signum() > 0
                && ThreadLocalRandom.current().nextDouble() < chance.doubleValue();
    }

    private void validateChance(BigDecimal chance, String name) {
        Objects.requireNonNull(chance, name);
        if (chance.signum() < 0 || chance.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }
}
