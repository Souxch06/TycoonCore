package fr.valoriatycoon.ranks;

import fr.valoriatycoon.compaction.CompactedResource;
import fr.valoriatycoon.compaction.CompactionService;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.professions.ProfessionService;
import fr.valoriatycoon.quests.QuestService;
import fr.valoriatycoon.tools.ToolProgressionService;
import fr.valoriatycoon.tutorial.TutorialService;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Reserves synchronous requirements and coordinates authoritative rank promotion. */
public final class RankService {
    private final RankSettings settings;
    private final RankBenefitService benefits;
    private final RankRepository repository;
    private final QuestService quests;
    private final ToolProgressionService tools;
    private final ProfessionService professions;
    private final CompactionService compaction;
    private final TutorialService tutorial;
    private final TycoonService tycoons;
    private final InternalEconomyService economy;
    private final Executor mainThread;
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public RankService(
            RankSettings settings,
            RankBenefitService benefits,
            RankRepository repository,
            QuestService quests,
            ToolProgressionService tools,
            ProfessionService professions,
            CompactionService compaction,
            TutorialService tutorial,
            TycoonService tycoons,
            InternalEconomyService economy,
            Executor mainThread
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.benefits = Objects.requireNonNull(benefits, "benefits");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.professions = Objects.requireNonNull(professions, "professions");
        this.compaction = Objects.requireNonNull(compaction, "compaction");
        this.tutorial = Objects.requireNonNull(tutorial, "tutorial");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    /** Attempts to promote one online island owner. Must be called on the server thread. */
    public CompletableFuture<RankPromotionResult> promote(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Rank promotion must start on the server thread")
            );
        }
        UUID playerId = player.getUniqueId();
        Tycoon tycoon = tycoons.ownedBy(playerId).orElse(null);
        if (tycoon == null) {
            return completed(RankPromotionStatus.NO_ACTIVE_ISLAND, 0, -1L);
        }
        RankRequirement requirement = settings.level(tycoon.prestige() + 1).orElse(null);
        if (requirement == null) {
            return completed(RankPromotionStatus.MAXIMUM_RANK, tycoon.prestige(), -1L);
        }
        if (!inFlight.add(playerId)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Rank promotion already in progress")
            );
        }

        int currentVanillaExperienceLevel = player.getLevel();
        if (currentVanillaExperienceLevel < requirement.requiredVanillaExperienceLevels()) {
            inFlight.remove(playerId);
            return completed(
                    RankPromotionStatus.INSUFFICIENT_VANILLA_EXPERIENCE,
                    tycoon.prestige(),
                    cachedBalance(playerId)
            );
        }
        List<ItemStack> reservedItems = reserveItems(player, requirement);
        if (reservedItems == null) {
            inFlight.remove(playerId);
            return CompletableFuture.failedFuture(new MissingRankItemsException());
        }
        int reservedExperienceLevels = requirement.requiredVanillaExperienceLevels();
        player.giveExpLevels(-reservedExperienceLevels);
        PromotionReservation reservation = new PromotionReservation(
                player,
                reservedItems,
                reservedExperienceLevels
        );
        AtomicBoolean promotionCommitted = new AtomicBoolean();

        return CompletableFuture.allOf(
                        tools.flushPlayerRewards(playerId),
                        professions.flushPlayer(playerId)
                )
                .thenCompose(ignored -> quests.flushPlayer(playerId))
                .thenCompose(ignored -> tutorial.prepareForPromotion(
                        player,
                        currentVanillaExperienceLevel
                ))
                .thenCompose(ignored -> repository.promote(
                        playerId,
                        tycoon.prestige(),
                        requirement
                ))
                .thenApplyAsync(result -> {
                    inFlight.remove(playerId);
                    if (!result.successful()) {
                        reservation.restore();
                        return result;
                    }

                    promotionCommitted.set(true);
                    tycoons.applyRank(playerId, result.resultingRank());
                    economy.synchronizeCommittedBalance(playerId, result.resultingBalanceCents());
                    if (result.resultingRank() >= 1) {
                        tutorial.finish(playerId);
                    }
                    tools.activate(playerId);
                    quests.reload(playerId);
                    return result;
                }, mainThread)
                .exceptionallyCompose(error -> failedOnMainThread(
                        playerId,
                        reservation,
                        promotionCommitted,
                        error
                ));
    }

    public RankSettings settings() {
        return settings;
    }

    /** Returns the cached medieval rank without storage I/O. */
    public int currentRank(UUID playerId) {
        return tycoons.ownedBy(playerId).map(Tycoon::prestige).orElse(0);
    }

    /** Returns the permanent money multiplier granted by the current rank. */
    public BigDecimal revenueMultiplier(UUID playerId) {
        return benefits.revenueMultiplier(playerId);
    }

    /** Returns the permanent generated-resource multiplier granted by the current rank. */
    public BigDecimal generatorProductionMultiplier(UUID playerId) {
        return benefits.generatorProductionMultiplier(playerId);
    }

    /** Returns extra generator placement slots granted by the current rank. */
    public int generatorSlotBonus(UUID playerId) {
        return benefits.generatorSlotBonus(playerId);
    }

    private CompletableFuture<RankPromotionResult> failedOnMainThread(
            UUID playerId,
            PromotionReservation reservation,
            AtomicBoolean promotionCommitted,
            Throwable error
    ) {
        CompletableFuture<RankPromotionResult> failed = new CompletableFuture<>();
        mainThread.execute(() -> {
            inFlight.remove(playerId);
            if (!promotionCommitted.get()) {
                reservation.restore();
            }
            failed.completeExceptionally(error);
        });
        return failed;
    }

    private List<ItemStack> reserveItems(Player player, RankRequirement requirement) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (Map.Entry<Material, Integer> entry : requirement.items().entrySet()) {
            Predicate<ItemStack> normalItem = item -> item != null
                    && item.getType() == entry.getKey()
                    && compaction.level(item) == 0;
            if (count(contents, normalItem) < entry.getValue()) {
                return null;
            }
        }
        for (Map.Entry<CompactedResource, Integer> entry : requirement.compactedItems().entrySet()) {
            Predicate<ItemStack> compactedItem = item -> compaction.matches(item, entry.getKey());
            if (count(contents, compactedItem) < entry.getValue()) {
                return null;
            }
        }

        List<ItemStack> removed = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : requirement.items().entrySet()) {
            take(
                    contents,
                    entry.getValue(),
                    item -> item != null
                            && item.getType() == entry.getKey()
                            && compaction.level(item) == 0,
                    removed
            );
        }
        for (Map.Entry<CompactedResource, Integer> entry : requirement.compactedItems().entrySet()) {
            take(
                    contents,
                    entry.getValue(),
                    item -> compaction.matches(item, entry.getKey()),
                    removed
            );
        }
        player.getInventory().setStorageContents(contents);
        return removed;
    }

    private int count(ItemStack[] contents, Predicate<ItemStack> predicate) {
        int total = 0;
        for (ItemStack item : contents) {
            if (predicate.test(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void take(
            ItemStack[] contents,
            int amount,
            Predicate<ItemStack> predicate,
            List<ItemStack> removed
    ) {
        int remaining = amount;
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!predicate.test(stack)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getAmount());
            ItemStack copy = stack.clone();
            copy.setAmount(taken);
            removed.add(copy);
            stack.setAmount(stack.getAmount() - taken);
            if (stack.getAmount() <= 0) {
                contents[slot] = null;
            }
            remaining -= taken;
        }
    }

    private void restoreItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover)
            );
        }
    }

    private long cachedBalance(UUID playerId) {
        return economy.cachedBalanceCents(playerId).orElse(-1L);
    }

    private CompletableFuture<RankPromotionResult> completed(
            RankPromotionStatus status,
            int rank,
            long balance
    ) {
        return CompletableFuture.completedFuture(new RankPromotionResult(status, rank, balance));
    }

    private final class PromotionReservation {
        private final Player player;
        private final List<ItemStack> items;
        private final int experienceLevels;
        private final AtomicBoolean restored = new AtomicBoolean();

        private PromotionReservation(Player player, List<ItemStack> items, int experienceLevels) {
            this.player = player;
            this.items = List.copyOf(items);
            this.experienceLevels = experienceLevels;
        }

        private void restore() {
            if (!restored.compareAndSet(false, true)) {
                return;
            }
            try {
                restoreItems(player, items);
            } finally {
                if (experienceLevels > 0) {
                    player.giveExpLevels(experienceLevels);
                }
            }
        }
    }

    public static final class MissingRankItemsException extends RuntimeException {
    }
}
