package fr.valoriatycoon.crates;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.machines.MachineItemService;
import fr.valoriatycoon.machines.MachineSettings;
import fr.valoriatycoon.pets.PetKeyService;
import fr.valoriatycoon.tools.ToolProgressionService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/** Coordinates generic crate openings, token recovery and atomic one-time claims. */
public final class CrateRewardService implements Listener {
    private final CrateSettings crateSettings;
    private final CrateRewardRepository repository;
    private final CrateRewardSelector selector;
    private final CrateRewardItemService rewardItems;
    private final CrateKeyItemService keyItems;
    private final CrateKeyService keys;
    private final PetKeyService petKeys;
    private final MachineItemService machineItems;
    private final MachineSettings machineSettings;
    private final InternalEconomyService economy;
    private final ToolProgressionService tools;
    private final MessageService messages;
    private final Executor mainThread;
    private final BooleanSupplier available;
    private final Logger logger;
    private final Set<UUID> openings = ConcurrentHashMap.newKeySet();
    private final Set<UUID> claims = ConcurrentHashMap.newKeySet();
    private final Set<UUID> deliveries = ConcurrentHashMap.newKeySet();
    private final Set<UUID> claimDeliveries = ConcurrentHashMap.newKeySet();
    private volatile CrateOpeningEffect openingEffect = CrateOpeningEffect.NONE;

    public CrateRewardService(
            CrateSettings crateSettings,
            CrateRewardRepository repository,
            CrateRewardSelector selector,
            CrateRewardItemService rewardItems,
            CrateKeyItemService keyItems,
            CrateKeyService keys,
            PetKeyService petKeys,
            MachineItemService machineItems,
            MachineSettings machineSettings,
            InternalEconomyService economy,
            ToolProgressionService tools,
            MessageService messages,
            Executor mainThread,
            BooleanSupplier available,
            Logger logger
    ) {
        this.crateSettings = Objects.requireNonNull(crateSettings, "crateSettings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.selector = Objects.requireNonNull(selector, "selector");
        this.rewardItems = Objects.requireNonNull(rewardItems, "rewardItems");
        this.keyItems = Objects.requireNonNull(keyItems, "keyItems");
        this.keys = Objects.requireNonNull(keys, "keys");
        this.petKeys = Objects.requireNonNull(petKeys, "petKeys");
        this.machineItems = Objects.requireNonNull(machineItems, "machineItems");
        this.machineSettings = Objects.requireNonNull(machineSettings, "machineSettings");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.available = Objects.requireNonNull(available, "available");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Recovers committed reward tokens after startup without redrawing them. */
    public void start() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            deliverPending(player.getUniqueId());
            recoverPendingClaims(player.getUniqueId());
        });
    }

    public void stop() {
        openings.clear();
        claims.clear();
        deliveries.clear();
        claimDeliveries.clear();
    }

    public boolean openingEnabled() {
        return crateSettings.openingEnabled();
    }

    /** Binds the physical station cinematic after composition without creating a dependency cycle. */
    public void setOpeningEffect(CrateOpeningEffect openingEffect) {
        this.openingEffect = Objects.requireNonNull(openingEffect, "openingEffect");
    }

    /** Starts one server-authoritative opening from the first matching physical key. */
    public void open(Player player, CrateType type) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Crates must be opened on the primary thread");
        }
        if (!crateSettings.openingEnabled()) {
            messages.send(player, "crates.opening-disabled");
            return;
        }
        if (!available.getAsBoolean()) {
            messages.send(player, "errors.unavailable");
            return;
        }
        CrateKeyItemService.KeyToken token = keyItems.firstToken(player, type).orElse(null);
        if (token == null) {
            messages.send(
                    player,
                    "crates.no-key",
                    Placeholder.unparsed("crate", type.displayName())
            );
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!openings.add(playerId)) {
            messages.send(player, "crates.in-progress");
            return;
        }
        CrateRewardSelection selection;
        try {
            selection = selector.select(playerId, type);
        } catch (RuntimeException exception) {
            openings.remove(playerId);
            logger.log(Level.SEVERE, "Could not select reward for " + type, exception);
            messages.send(player, "errors.unavailable");
            return;
        }
        repository.open(playerId, token, selection).whenCompleteAsync((result, error) -> {
            openings.remove(playerId);
            if (error != null || result == null) {
                logger.log(Level.SEVERE, "Generic crate opening failed for " + playerId, error);
                messages.send(player, "errors.storage");
                return;
            }
            if (result.status() != CrateOpenStatus.SUCCESS) {
                keyItems.removeCopies(player, token.keyId());
                messages.send(
                        player,
                        result.status() == CrateOpenStatus.KEY_ALREADY_USED
                                ? "crates.key-already-used"
                                : "crates.key-invalid"
                );
                return;
            }
            keyItems.removeCopies(player, token.keyId());
            celebrateOpening(player, type);
            openingEffect.play(player, type);
            CrateReward reward = result.reward();
            messages.send(
                    player,
                    "crates.opened",
                    Placeholder.unparsed("crate", type.displayName()),
                    Placeholder.component("reward", rewardItems.displayName(reward))
            );
            if (selector.broadcasts(type, reward.definitionId())) {
                Bukkit.broadcast(messages.component(
                        "crates.jackpot-broadcast",
                        false,
                        Placeholder.unparsed("player", player.getName()),
                        Placeholder.unparsed("crate", type.displayName()),
                        Placeholder.component("reward", rewardItems.displayName(reward))
                ));
            }
            deliverPending(playerId);
        }, mainThread);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (available.getAsBoolean()) {
            deliverPending(event.getPlayer().getUniqueId());
            recoverPendingClaims(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() == null
                || event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        CrateRewardItemService.RewardToken token = rewardItems.token(event.getItem()).orElse(null);
        if (token == null) {
            return;
        }
        event.setCancelled(true);
        claim(event.getPlayer(), token);
    }

    private void claim(Player player, CrateRewardItemService.RewardToken token) {
        if (!available.getAsBoolean()) {
            messages.send(player, "errors.unavailable");
            return;
        }
        if (!claims.add(token.rewardId())) {
            messages.send(player, "crates.in-progress");
            return;
        }
        UUID playerId = player.getUniqueId();
        repository.claim(playerId, token.rewardId(), token.kind()).whenCompleteAsync((result, error) -> {
            claims.remove(token.rewardId());
            if (error != null || result == null) {
                logger.log(Level.SEVERE, "Crate reward claim failed for " + token.rewardId(), error);
                messages.send(player, "errors.storage");
                return;
            }
            switch (result.status()) {
                case REWARD_INVALID, REWARD_KIND_MISMATCH -> {
                    rewardItems.removeCopies(player, token.rewardId());
                    messages.send(player, "crates.reward-invalid");
                }
                case REWARD_ALREADY_USED -> {
                    rewardItems.removeCopies(player, token.rewardId());
                    messages.send(player, "crates.reward-already-used");
                    recoverPendingClaims(playerId);
                }
                case BALANCE_OVERFLOW -> messages.send(player, "crates.balance-overflow");
                case SUCCESS -> completeClaim(player, result);
            }
        }, mainThread);
    }

    private void completeClaim(Player player, CrateClaimResult result) {
        CrateReward reward = result.reward();
        if (result.resultingMoneyCents() >= 0L) {
            economy.synchronizeCommittedBalance(player.getUniqueId(), result.resultingMoneyCents());
        }
        result.resultingToolCoins().forEach((type, balance) ->
                tools.synchronizeCommittedCoins(player.getUniqueId(), type, balance)
        );
        if (!player.isOnline()) {
            return;
        }
        rewardItems.removeCopies(player, reward.rewardId());
        try {
            grantPhysicalOutcome(player, reward);
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Committed crate reward could not be rendered " + reward.rewardId(), exception);
            messages.send(player, "crates.reward-delivery-failed");
            return;
        }
        if (!reward.claimDelivered()) {
            repository.markClaimDelivered(reward.rewardId()).exceptionally(error -> {
                logger.log(Level.WARNING, "Could not mark crate claim delivered " + reward.rewardId(), error);
                return null;
            });
        }
        messages.send(
                player,
                "crates.reward-claimed",
                Placeholder.component("reward", rewardItems.displayName(reward))
        );
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.35F);
    }

    private void grantPhysicalOutcome(Player player, CrateReward reward) {
        CrateRewardPayload payload = reward.payload();
        switch (reward.kind()) {
            case MONEY_BAG, COIN_BAG, UNIVERSAL_COIN_BAG -> {
                // Already applied atomically inside the claim transaction.
            }
            case XP_VIAL -> player.giveExpLevels(payload.requireInt("levels"));
            case RESOURCE_BUNDLE -> give(
                    player,
                    Material.valueOf(payload.require("material")),
                    payload.requireLong("amount")
            );
            case VANILLA_ITEM -> give(
                    player,
                    Material.valueOf(payload.require("material")),
                    payload.requireLong("amount")
            );
            case CRATE_KEYS -> keys.recoverPending(player.getUniqueId());
            case PET_KEYS -> {
                int amount = payload.requireInt("amount");
                List<UUID> identifiers = new ArrayList<>(amount);
                for (int index = 0; index < amount; index++) {
                    identifiers.add(UUID.nameUUIDFromBytes(
                            ("valoriatycoon:pet-key-reward:" + reward.rewardId() + ':' + index)
                                    .getBytes(StandardCharsets.UTF_8)
                    ));
                }
                petKeys.give(player, identifiers);
            }
            case GENERATORS -> {
                for (String type : payload.require("types").split(",")) {
                    give(player, machineItems.create(machineSettings.machine(type)));
                }
            }
        }
    }

    private void recoverPendingClaims(UUID playerId) {
        if (!available.getAsBoolean() || !claimDeliveries.add(playerId)) {
            return;
        }
        repository.pendingClaims(playerId).whenCompleteAsync((pending, error) -> {
            if (error != null) {
                claimDeliveries.remove(playerId);
                logger.log(Level.WARNING, "Could not load pending crate claims for " + playerId, error);
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || pending.isEmpty()) {
                claimDeliveries.remove(playerId);
                return;
            }
            List<CompletableFuture<Void>> marks = new ArrayList<>();
            for (CrateReward reward : pending) {
                try {
                    rewardItems.removeCopies(player, reward.rewardId());
                    grantPhysicalOutcome(player, reward);
                    messages.send(
                            player,
                            "crates.reward-recovered",
                            Placeholder.component("reward", rewardItems.displayName(reward))
                    );
                    marks.add(repository.markClaimDelivered(reward.rewardId()));
                } catch (RuntimeException exception) {
                    logger.log(Level.SEVERE, "Could not recover crate claim " + reward.rewardId(), exception);
                }
            }
            CompletableFuture.allOf(marks.toArray(CompletableFuture[]::new))
                    .whenComplete((ignored, markError) -> {
                        claimDeliveries.remove(playerId);
                        if (markError != null) {
                            logger.log(
                                    Level.WARNING,
                                    "Could not mark recovered crate claims for " + playerId,
                                    markError
                            );
                        } else if (!marks.isEmpty()) {
                            recoverPendingClaims(playerId);
                        }
                    });
        }, mainThread);
    }

    private void deliverPending(UUID playerId) {
        if (!available.getAsBoolean() || !deliveries.add(playerId)) {
            return;
        }
        repository.pending(playerId).whenCompleteAsync((pending, error) -> {
            if (error != null) {
                deliveries.remove(playerId);
                logger.log(Level.WARNING, "Could not load pending crate rewards for " + playerId, error);
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || pending.isEmpty()) {
                deliveries.remove(playerId);
                return;
            }
            CompletableFuture<?>[] marks = new CompletableFuture<?>[pending.size()];
            for (int index = 0; index < pending.size(); index++) {
                CrateReward reward = pending.get(index);
                rewardItems.give(player, reward);
                marks[index] = repository.markDelivered(reward.rewardId());
            }
            CompletableFuture.allOf(marks).whenComplete((ignored, markError) -> {
                deliveries.remove(playerId);
                if (markError != null) {
                    logger.log(Level.WARNING, "Could not mark crate rewards delivered for " + playerId, markError);
                } else {
                    deliverPending(playerId);
                }
            });
        }, mainThread);
    }

    private void celebrateOpening(Player player, CrateType type) {
        float pitch = 0.85F + type.ordinal() * 0.06F;
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.9F, pitch);
        player.getWorld().spawnParticle(
                Particle.END_ROD,
                player.getLocation().add(0.0, 1.1, 0.0),
                type == CrateType.VALORIA ? 24 : 12,
                0.45,
                0.55,
                0.45,
                0.02
        );
    }

    private void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }

    private void give(Player player, Material material, long amount) {
        long remaining = amount;
        while (remaining > 0L) {
            int stackSize = (int) Math.min(material.getMaxStackSize(), remaining);
            give(player, new ItemStack(material, stackSize));
            remaining -= stackSize;
        }
    }
}
