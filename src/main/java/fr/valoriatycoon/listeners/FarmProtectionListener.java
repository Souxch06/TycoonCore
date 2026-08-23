package fr.valoriatycoon.listeners;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.farm.FarmBridgeDefinition;
import fr.valoriatycoon.farm.FarmDefinition;
import fr.valoriatycoon.farm.FarmSettings;
import fr.valoriatycoon.farm.FarmWorld;
import fr.valoriatycoon.farm.FarmWorldService;
import fr.valoriatycoon.farm.autosell.AutoSellBatchService;
import fr.valoriatycoon.farm.autosell.AutoSellService;
import fr.valoriatycoon.farm.regeneration.BlockRegenerationService;
import fr.valoriatycoon.farm.autosell.AutoSellValueCalculator;
import fr.valoriatycoon.pets.PetEffect;
import fr.valoriatycoon.pets.PetService;
import fr.valoriatycoon.quests.QuestService;
import fr.valoriatycoon.ranks.RankService;
import fr.valoriatycoon.tools.MultiToolItemService;
import fr.valoriatycoon.tools.ToolProgressionService;
import fr.valoriatycoon.tools.ToolType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Enforces generated-farm rules, delayed regeneration and optional per-player auto-sell. */
public final class FarmProtectionListener implements Listener {
    private static final long DENIAL_MESSAGE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final FarmWorldService worlds;
    private final FarmSettings.RankBarrier rankBarrier;
    private final BlockRegenerationService regeneration;
    private final AutoSellService autoSell;
    private final AutoSellBatchService saleBatches;
    private final ToolProgressionService tools;
    private final MultiToolItemService multiToolItems;
    private final QuestService quests;
    private final RankService ranks;
    private final PetService pets;
    private final MessageService messages;
    private final Map<UUID, Long> lastDenialMessage = new HashMap<>();

    public FarmProtectionListener(
            FarmWorldService worlds,
            FarmSettings.RankBarrier rankBarrier,
            BlockRegenerationService regeneration,
            AutoSellService autoSell,
            AutoSellBatchService saleBatches,
            ToolProgressionService tools,
            MultiToolItemService multiToolItems,
            QuestService quests,
            RankService ranks,
            PetService pets,
            MessageService messages
    ) {
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.rankBarrier = Objects.requireNonNull(rankBarrier, "rankBarrier");
        this.regeneration = Objects.requireNonNull(regeneration, "regeneration");
        this.autoSell = Objects.requireNonNull(autoSell, "autoSell");
        this.saleBatches = Objects.requireNonNull(saleBatches, "saleBatches");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.multiToolItems = Objects.requireNonNull(multiToolItems, "multiToolItems");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        FarmWorld farm = worlds.farm(event.getBlock().getWorld()).orElse(null);
        if (farm == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("tycoon.bypass")) {
            return;
        }
        FarmDefinition definition = farm.definition();
        if (!player.hasPermission("tycoon.farm")
                || !canUseZone(player, event.getBlock(), definition)
                || insideSpawnProtection(event.getBlock(), definition)
                || !definition.breakableBlocks().contains(event.getBlock().getType())) {
            event.setCancelled(true);
            deny(player);
            return;
        }

        regeneration.schedule(event.getBlock(), definition.regenerationDelay(event.getBlock().getType()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getPlayer().hasPermission("tycoon.bypass")) {
            return;
        }
        FarmWorld farm = worlds.farm(event.getTo().getWorld()).orElse(null);
        if (farm == null || !farm.definition().zoned()) {
            return;
        }
        if (!canEnterArea(
                event.getPlayer(),
                farm.definition(),
                event.getTo().getBlockX(),
                event.getTo().getBlockZ()
        )) {
            event.setCancelled(true);
            denyEntry(
                    event.getPlayer(),
                    farm.definition(),
                    event.getTo().getBlockX(),
                    event.getTo().getBlockZ(),
                    false
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null
                || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("tycoon.bypass")) {
            return;
        }
        FarmWorld farm = worlds.farm(event.getTo().getWorld()).orElse(null);
        if (farm == null || !farm.definition().zoned()) {
            return;
        }
        if (!canEnterArea(
                player,
                farm.definition(),
                event.getTo().getBlockX(),
                event.getTo().getBlockZ()
        )) {
            int blockedX = event.getTo().getBlockX();
            int blockedZ = event.getTo().getBlockZ();
            event.setTo(event.getFrom());
            denyEntry(
                    player,
                    farm.definition(),
                    blockedX,
                    blockedZ,
                    true
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        Player player = event.getVehicle().getPassengers().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(passenger -> !passenger.hasPermission("tycoon.bypass"))
                .findFirst()
                .orElse(null);
        if (player == null) {
            return;
        }
        FarmWorld farm = worlds.farm(event.getTo().getWorld()).orElse(null);
        if (farm == null || !farm.definition().zoned() || canEnterArea(
                player,
                farm.definition(),
                event.getTo().getBlockX(),
                event.getTo().getBlockZ()
        )) {
            return;
        }
        int blockedX = event.getTo().getBlockX();
        int blockedZ = event.getTo().getBlockZ();
        Vector rejection = rankBarrierVelocity(farm.definition(), blockedX, blockedZ);
        event.getVehicle().setVelocity(new Vector());
        if (event.getVehicle().teleport(event.getFrom())) {
            if (rejection != null) {
                event.getVehicle().setVelocity(rejection);
            }
        } else {
            player.leaveVehicle();
            player.teleport(event.getFrom());
            if (rejection != null) {
                player.setVelocity(rejection);
            }
        }
        denyEntry(
                player,
                farm.definition(),
                blockedX,
                blockedZ,
                false
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrops(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("tycoon.bypass")) {
            return;
        }
        FarmWorld farm = worlds.farm(event.getBlockState().getWorld()).orElse(null);
        if (farm == null) {
            return;
        }
        ToolType toolType = matchingTool(player, farm);
        if (toolType == null) {
            return;
        }
        if (!autoSell.isEnabled(player.getUniqueId())) {
            return;
        }
        List<Item> pricedEntities = new ArrayList<>();
        List<ItemStack> fallbackItems = new ArrayList<>();
        long total = 0L;
        try {
            for (Item entity : event.getItems()) {
                ItemStack stack = entity.getItemStack();
                long unitPrice = farm.definition().sellPrice(stack.getType());
                if (unitPrice <= 0) {
                    continue;
                }
                total = Math.addExact(total, Math.multiplyExact(unitPrice, stack.getAmount()));
                pricedEntities.add(entity);
                fallbackItems.add(stack.clone());
            }
        } catch (ArithmeticException exception) {
            messages.send(player, "farm.autosell-overflow");
            return;
        }
        if (total <= 0) {
            return;
        }
        try {
            total = autoSell.applyMultiplier(player.getUniqueId(), total);
            total = AutoSellValueCalculator.apply(
                    total,
                    tools.moneyMultiplier(player.getUniqueId(), toolType)
            );
            total = AutoSellValueCalculator.apply(total, ranks.revenueMultiplier(player.getUniqueId()));
            total = AutoSellValueCalculator.apply(
                    total,
                    pets.multiplier(player.getUniqueId(), PetEffect.MONEY)
            );
        } catch (IllegalArgumentException exception) {
            messages.send(player, "farm.autosell-overflow");
            return;
        }
        pricedEntities.forEach(Item::remove);
        event.getItems().removeAll(pricedEntities);
        saleBatches.queue(player, total, fallbackItems);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        FarmWorld farm = worlds.farm(event.getPlayer().getWorld()).orElse(null);
        if (farm == null || matchingTool(event.getPlayer(), farm) != ToolType.FISHING_ROD) {
            return;
        }
        tools.queueActionRewards(
                event.getPlayer().getUniqueId(),
                ToolType.FISHING_ROD,
                true
        );
        quests.recordToolAction(event.getPlayer().getUniqueId(), ToolType.FISHING_ROD);
        if (!autoSell.isEnabled(event.getPlayer().getUniqueId())) {
            return;
        }
        ItemStack item = caught.getItemStack();
        long unitPrice = farm.definition().sellPrice(item.getType());
        if (unitPrice <= 0) {
            return;
        }
        try {
            long baseValue = Math.multiplyExact(unitPrice, item.getAmount());
            long multipliedValue = autoSell.applyMultiplier(event.getPlayer().getUniqueId(), baseValue);
            multipliedValue = AutoSellValueCalculator.apply(
                    multipliedValue,
                    tools.moneyMultiplier(event.getPlayer().getUniqueId(), ToolType.FISHING_ROD)
            );
            multipliedValue = AutoSellValueCalculator.apply(
                    multipliedValue,
                    ranks.revenueMultiplier(event.getPlayer().getUniqueId())
            );
            multipliedValue = AutoSellValueCalculator.apply(
                    multipliedValue,
                    pets.multiplier(event.getPlayer().getUniqueId(), PetEffect.MONEY)
            );
            caught.remove();
            saleBatches.queue(event.getPlayer(), multipliedValue, List.of(item));
        } catch (ArithmeticException | IllegalArgumentException exception) {
            messages.send(event.getPlayer(), "farm.autosell-overflow");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        worlds.farm(event.getBlock().getWorld()).ifPresent(ignored -> {
            if (!event.getPlayer().hasPermission("tycoon.bypass")) {
                event.setCancelled(true);
                deny(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()
                && !event.getPlayer().hasPermission("tycoon.bypass")) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()
                && !event.getPlayer().hasPermission("tycoon.bypass")) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null
                || event.getItem() == null
                || event.getPlayer().hasPermission("tycoon.bypass")
                || worlds.farm(event.getClickedBlock().getWorld()).isEmpty()) {
            return;
        }
        if (event.getPlayer().isSneaking()
                && multiToolItems.isOwnedBy(
                        event.getItem(),
                        event.getPlayer().getUniqueId()
                )) {
            return;
        }
        String material = event.getItem().getType().name();
        if (material.endsWith("_AXE")
                || material.endsWith("_HOE")
                || material.endsWith("_SHOVEL")
                || material.endsWith("_BOAT")
                || material.endsWith("_RAFT")
                || material.equals("MINECART")
                || material.endsWith("_MINECART")
                || material.equals("FLINT_AND_STEEL")
                || material.equals("FIRE_CHARGE")
                || material.equals("BONE_MEAL")) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (worlds.farm(event.getLocation().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (worlds.farm(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    public void releasePlayer(UUID playerId) {
        lastDenialMessage.remove(playerId);
    }

    private ToolType matchingTool(Player player, FarmWorld farm) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!multiToolItems.isOwnedBy(item, player.getUniqueId())) {
            return null;
        }
        ToolType held = ToolType.fromMaterial(item.getType()).orElse(null);
        ToolType expected = switch (farm.definition().type()) {
            case MINE -> ToolType.PICKAXE;
            case FIELDS -> ToolType.HOE;
            case FOREST -> ToolType.AXE;
            case FISHING -> ToolType.FISHING_ROD;
        };
        return held == expected ? held : null;
    }

    private boolean canEnterArea(
            Player player,
            FarmDefinition definition,
            int x,
            int z
    ) {
        var requiredRank = definition.requiredRankAt(x, z);
        return player.hasPermission("tycoon.farm")
                && requiredRank.isPresent()
                && worlds.currentRank(player.getUniqueId()) >= requiredRank.getAsInt();
    }

    private void denyEntry(
            Player player,
            FarmDefinition definition,
            int x,
            int z,
            boolean launchPlayer
    ) {
        var requiredRank = definition.requiredRankAt(x, z);
        if (requiredRank.isPresent()
                && worlds.currentRank(player.getUniqueId()) < requiredRank.getAsInt()) {
            if (launchPlayer) {
                Vector rejection = rankBarrierVelocity(definition, x, z);
                if (rejection != null) {
                    player.setFallDistance(0.0F);
                    player.setVelocity(rejection);
                }
            }
            denyRank(player, requiredRank.getAsInt());
            return;
        }
        deny(player);
    }

    private Vector rankBarrierVelocity(FarmDefinition definition, int x, int z) {
        FarmBridgeDefinition bridge = definition.bridgeAt(x, z).orElse(null);
        if (bridge == null) {
            return null;
        }
        double midpoint = bridge.minimumX()
                + (bridge.maximumX() - bridge.minimumX()) / 2.0;
        double direction = x <= midpoint ? -1.0 : 1.0;
        return new Vector(
                direction * rankBarrier.horizontalKnockback(),
                rankBarrier.verticalKnockback(),
                0.0
        );
    }

    private boolean canUseZone(Player player, Block block, FarmDefinition definition) {
        if (!definition.zoned()) {
            return true;
        }
        var zone = definition.zoneAt(block.getX(), block.getZ()).orElse(null);
        return zone != null && worlds.canAccess(player.getUniqueId(), zone);
    }

    private boolean insideSpawnProtection(Block block, FarmDefinition definition) {
        var zone = definition.zoneAt(block.getX(), block.getZ()).orElse(null);
        long centerX = zone == null ? 0L : zone.centerX();
        long centerZ = zone == null ? 0L : zone.centerZ();
        long x = block.getX() - centerX;
        long z = block.getZ() - centerZ;
        long radius = definition.spawnProtectionRadius();
        return x * x + z * z <= radius * radius;
    }

    private void denyRank(Player player, int requiredRank) {
        if (!acquireDenialMessage(player)) {
            return;
        }
        player.sendActionBar(messages.component(
                "farm.rank-barrier",
                false,
                Placeholder.unparsed("rank", ranks.settings().name(requiredRank))
        ));
    }

    private void deny(Player player) {
        if (!acquireDenialMessage(player)) {
            return;
        }
        player.sendActionBar(messages.component("farm.protected", false));
    }

    private boolean acquireDenialMessage(Player player) {
        long now = System.nanoTime();
        Long previous = lastDenialMessage.get(player.getUniqueId());
        if (previous != null && now - previous < DENIAL_MESSAGE_INTERVAL_NANOS) {
            return false;
        }
        lastDenialMessage.put(player.getUniqueId(), now);
        return true;
    }
}
