package fr.valoriatycoon.tools;

import fr.valoriatycoon.farm.FarmWorldService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Executes bounded, server-side implementations of all built-in tool-specific abilities. */
public final class ToolAbilityListener implements Listener {
    private static final BlockFace[] ADJACENT = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final JavaPlugin plugin;
    private final ToolProgressionService tools;
    private final MultiToolItemService multiToolItems;
    private final FarmWorldService farmWorlds;
    private final Set<UUID> massBreaking = new HashSet<>();

    public ToolAbilityListener(
            JavaPlugin plugin,
            ToolProgressionService tools,
            MultiToolItemService multiToolItems,
            FarmWorldService farmWorlds
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.multiToolItems = Objects.requireNonNull(multiToolItems, "multiToolItems");
        this.farmWorlds = Objects.requireNonNull(farmWorlds, "farmWorlds");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!massBreaking.add(player.getUniqueId())) {
            return;
        }
        try {
            ToolType type = heldType(player);
            if (type == ToolType.PICKAXE) {
                areaMine(player, event.getBlock());
            } else if (type == ToolType.HOE) {
                if (!ufoHarvest(player, event.getBlock())) {
                    areaHarvest(player, event.getBlock());
                }
            } else if (type == ToolType.AXE) {
                timber(player, event.getBlock());
            }
        } finally {
            massBreaking.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        ToolType type = heldType(player);
        if (type == null) {
            return;
        }
        Material broken = event.getBlockState().getType();
        if (type == ToolType.PICKAXE) {
            autoSmelt(player, type, event.getItems());
            duplicateDrops(player, type, ToolCapability.ORE_FORTUNE, event.getItems());
            bonusCoins(player, type, ToolCapability.GEM_FINDER);
            bonusCoins(player, type, ToolCapability.MINE_COIN_FINDER);
        } else if (type == ToolType.HOE && isCrop(broken)) {
            duplicateDrops(player, type, ToolCapability.HARVEST_FORTUNE, event.getItems());
            cropMutation(player, broken);
            bonusCoins(player, type, ToolCapability.FARM_COIN_FINDER);
            autoReplant(player, event.getBlock(), event.getBlockState().getBlockData());
        } else if (type == ToolType.AXE && isWood(broken)) {
            duplicateDrops(player, type, ToolCapability.WOOD_FORTUNE, event.getItems());
            appleFinder(player);
            bonusCoins(player, type, ToolCapability.WOOD_COIN_FINDER);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)
                || heldType(event.getPlayer()) != ToolType.FISHING_ROD) {
            return;
        }
        Player player = event.getPlayer();
        if (roll(player, ToolType.FISHING_ROD, ToolCapability.DOUBLE_CATCH)) {
            give(player, caught.getItemStack().clone());
        }
        if (roll(player, ToolType.FISHING_ROD, ToolCapability.TREASURE_LUCK)) {
            give(player, new ItemStack(Material.NAUTILUS_SHELL));
        }
        if (roll(player, ToolType.FISHING_ROD, ToolCapability.RARE_CATCH)) {
            give(player, new ItemStack(Material.TROPICAL_FISH));
        }
        bonusCoins(player, ToolType.FISHING_ROD, ToolCapability.FISH_COIN_FINDER);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (multiToolItems.isOwnedBy(event.getItem(), event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void areaMine(Player player, Block origin) {
        int size = tools.capabilityValue(
                player.getUniqueId(), ToolType.PICKAXE, ToolCapability.AREA_MINING
        ).intValue();
        if (size < 3 || ToolBlockResolver.resolve(origin.getType()).orElse(null) != ToolType.PICKAXE) {
            return;
        }
        org.bukkit.util.Vector direction = player.getEyeLocation().getDirection();
        List<Block> targets = new ArrayList<>(8);
        if (Math.abs(direction.getY()) > 0.6) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                targets.add(origin.getRelative(x, 0, z));
            }
        } else if (Math.abs(direction.getX()) > Math.abs(direction.getZ())) {
            for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                targets.add(origin.getRelative(0, y, z));
            }
        } else {
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
                targets.add(origin.getRelative(x, y, 0));
            }
        }
        targets.stream()
                .filter(block -> !block.equals(origin))
                .filter(block -> ToolBlockResolver.resolve(block.getType()).orElse(null) == ToolType.PICKAXE)
                .limit(8)
                .forEach(player::breakBlock);
    }

    private void areaHarvest(Player player, Block origin) {
        int size = tools.capabilityValue(
                player.getUniqueId(), ToolType.HOE, ToolCapability.AREA_HARVEST
        ).intValue();
        if (size < 3 || !isMatureCrop(origin)) {
            return;
        }
        int radius = Math.min(2, size / 2);
        int remaining = size * size - 1;
        for (int x = -radius; x <= radius && remaining > 0; x++) {
            for (int z = -radius; z <= radius && remaining > 0; z++) {
                Block target = origin.getRelative(x, 0, z);
                if (!target.equals(origin) && isMatureCrop(target) && player.breakBlock(target)) {
                    remaining--;
                }
            }
        }
    }

    private boolean ufoHarvest(Player player, Block origin) {
        if (!isMatureCrop(origin)
                || !roll(player, ToolType.HOE, ToolCapability.UFO_HARVEST)) {
            return false;
        }
        showUfo(player);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block target = origin.getRelative(x, 0, z);
                if (!target.equals(origin) && isMatureCrop(target)) {
                    player.breakBlock(target);
                }
            }
        }
        return true;
    }

    private void showUfo(Player player) {
        org.bukkit.Location center = player.getLocation().clone().add(0, 2.5, 0);
        player.getWorld().playSound(
                center,
                org.bukkit.Sound.BLOCK_BEACON_ACTIVATE,
                0.8F,
                1.6F
        );
        new org.bukkit.scheduler.BukkitRunnable() {
            private int lived;

            @Override
            public void run() {
                if (!player.isOnline() || lived >= tools.settings().abilities().ufoDisplayTicks()) {
                    cancel();
                    return;
                }
                org.bukkit.Location movingCenter = player.getLocation().clone().add(0, 2.5, 0);
                for (int index = 0; index < 16; index++) {
                    double angle = Math.PI * 2.0 * index / 16.0;
                    double x = Math.cos(angle) * 1.6;
                    double z = Math.sin(angle) * 1.6;
                    player.getWorld().spawnParticle(
                            org.bukkit.Particle.END_ROD,
                            movingCenter.clone().add(x, 0, z),
                            1,
                            0, 0, 0,
                            0
                    );
                }
                player.getWorld().spawnParticle(
                        org.bukkit.Particle.ELECTRIC_SPARK,
                        movingCenter.clone().add(0, -1.5, 0),
                        8,
                        0.4, 0.8, 0.4,
                        0.02
                );
                lived += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void timber(Player player, Block origin) {
        int configured = tools.capabilityValue(
                player.getUniqueId(), ToolType.AXE, ToolCapability.TIMBER
        ).intValue();
        int limit = Math.min(configured, tools.settings().abilities().maximumTimberBlocks());
        if (limit <= 1 || !isWood(origin.getType())) {
            return;
        }
        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(origin);
        int broken = 1;
        while (!queue.isEmpty() && broken < limit) {
            Block current = queue.poll();
            for (BlockFace face : ADJACENT) {
                Block next = current.getRelative(face);
                String key = next.getX() + ":" + next.getY() + ":" + next.getZ();
                if (visited.add(key)
                        && isWood(next.getType())
                        && !next.equals(origin)
                        && player.breakBlock(next)) {
                    queue.add(next);
                    broken++;
                    if (broken >= limit) {
                        break;
                    }
                }
            }
        }
    }

    private void duplicateDrops(
            Player player,
            ToolType type,
            ToolCapability capability,
            List<Item> entities
    ) {
        if (!roll(player, type, capability)) {
            return;
        }
        for (Item entity : entities) {
            give(player, entity.getItemStack().clone());
        }
    }

    private void autoSmelt(Player player, ToolType type, List<Item> entities) {
        if (!roll(player, type, ToolCapability.AUTO_SMELT)) {
            return;
        }
        for (Item entity : entities) {
            ItemStack stack = entity.getItemStack();
            Material smelted = switch (stack.getType()) {
                case RAW_IRON -> Material.IRON_INGOT;
                case RAW_GOLD -> Material.GOLD_INGOT;
                case RAW_COPPER -> Material.COPPER_INGOT;
                default -> null;
            };
            if (smelted != null) {
                stack.setType(smelted);
                entity.setItemStack(stack);
            }
        }
    }

    private void cropMutation(Player player, Material crop) {
        if (!roll(player, ToolType.HOE, ToolCapability.SEED_FINDER)) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.02) {
            give(player, new ItemStack(Material.GOLDEN_CARROT));
            return;
        }
        Material produce = switch (crop) {
            case WHEAT -> Material.WHEAT;
            case BEETROOTS -> Material.BEETROOT;
            case POTATOES -> Material.POTATO;
            case CARROTS -> Material.CARROT;
            default -> null;
        };
        if (produce != null) {
            give(player, new ItemStack(produce, ThreadLocalRandom.current().nextInt(1, 4)));
        }
    }

    private void appleFinder(Player player) {
        if (!roll(player, ToolType.AXE, ToolCapability.APPLE_FINDER)) {
            return;
        }
        double rarityRoll = ThreadLocalRandom.current().nextDouble();
        double notchChance = tools.settings().abilities().notchAppleRelativeChance().doubleValue();
        double goldenChance = tools.settings().abilities().goldenAppleRelativeChance().doubleValue();
        Material reward;
        if (rarityRoll < notchChance) {
            reward = Material.ENCHANTED_GOLDEN_APPLE;
        } else if (rarityRoll < notchChance + goldenChance) {
            reward = Material.GOLDEN_APPLE;
        } else {
            reward = Material.APPLE;
        }
        give(player, new ItemStack(reward));
    }

    private void bonusCoins(Player player, ToolType type, ToolCapability capability) {
        if (roll(player, type, capability)) {
            tools.queueBonusCoins(player.getUniqueId(), type, tools.settings().abilities().gemCoinBonus());
        }
    }

    private void autoReplant(Player player, Block block, BlockData original) {
        if (farmWorlds.farm(block.getWorld()).isPresent()
                || !(original instanceof Ageable ageable)
                || !roll(player, ToolType.HOE, ToolCapability.AUTO_REPLANT)) {
            return;
        }
        Ageable replanted = (Ageable) ageable.clone();
        replanted.setAge(0);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (block.getType().isAir()) {
                block.setBlockData(replanted, false);
            }
        });
    }

    private boolean roll(Player player, ToolType type, ToolCapability capability) {
        double chance = tools.capabilityValue(player.getUniqueId(), type, capability).doubleValue();
        return chance > 0 && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, chance);
    }

    private ToolType heldType(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!multiToolItems.isOwnedBy(held, player.getUniqueId())) {
            return null;
        }
        return ToolType.fromMaterial(held.getType()).orElse(null);
    }

    private boolean isMatureCrop(Block block) {
        return block.getBlockData() instanceof Ageable ageable
                && ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean isCrop(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS
                || material == Material.POTATOES || material == Material.BEETROOTS;
    }

    private boolean isWood(Material material) {
        String name = material.name();
        return Tag.LOGS.isTagged(material)
                || name.endsWith("_STEM")
                || name.endsWith("_HYPHAE");
    }

    private void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }
}
