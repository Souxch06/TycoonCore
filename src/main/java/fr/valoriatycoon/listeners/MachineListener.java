package fr.valoriatycoon.listeners;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.gui.MachineControlPanel;
import fr.valoriatycoon.machines.MachineDefinition;
import fr.valoriatycoon.machines.MachineItemService;
import fr.valoriatycoon.machines.MachinePosition;
import fr.valoriatycoon.machines.MachineService;
import fr.valoriatycoon.machines.PlacedMachine;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Authenticated placement, owner control and safe recovery of block-only machines. */
public final class MachineListener implements Listener {
    private final JavaPlugin plugin;
    private final MachineService machines;
    private final MachineItemService items;
    private final MachineControlPanel controls;
    private final TycoonService tycoons;
    private final MessageService messages;
    private final Executor mainThread;

    public MachineListener(
            JavaPlugin plugin,
            MachineService machines,
            MachineItemService items,
            MachineControlPanel controls,
            TycoonService tycoons,
            MessageService messages,
            Executor mainThread
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.machines = Objects.requireNonNull(machines, "machines");
        this.items = Objects.requireNonNull(items, "items");
        this.controls = Objects.requireNonNull(controls, "controls");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String machineType = items.machineType(event.getItemInHand()).orElse(null);
        if (machineType == null) return;
        if (!event.getPlayer().hasPermission("tycoon.machines")) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "errors.no-permission");
            return;
        }
        Block block = event.getBlockPlaced();
        Tycoon tycoon = tycoons.at(block.getWorld().getName(), block.getX(), block.getZ()).orElse(null);
        if (tycoon == null
                || tycoon.status() != TycoonStatus.ACTIVE
                || !tycoons.canBuild(event.getPlayer().getUniqueId(), tycoon)
                || !tycoons.isInsideBuildArea(tycoon, block.getX(), block.getZ())) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "machines.invalid-location");
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled()) return;
            machines.place(
                    tycoon.id(), event.getPlayer().getUniqueId(), machineType, position(block)
            ).whenCompleteAsync((placed, error) -> {
                if (error == null) return;
                if (block.getType() == machines.settings().machine(machineType).blockMaterial()) {
                    block.setType(Material.AIR, false);
                    returnItem(event.getPlayer(), machines.settings().machine(machineType));
                }
                messages.send(event.getPlayer(), "machines.placement-failed");
            }, mainThread);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        PlacedMachine machine = machines.at(position(event.getBlock())).orElse(null);
        if (machine == null) return;
        event.setCancelled(true);
        if (!machine.ownerId().equals(event.getPlayer().getUniqueId())) {
            messages.send(event.getPlayer(), "machines.not-owner");
            return;
        }
        MachineDefinition definition = machines.settings().machine(machine.machineType());
        event.getBlock().setType(Material.AIR, false);
        machines.remove(machine.id()).whenCompleteAsync((removed, error) -> {
            if (error != null || removed.isEmpty()) {
                event.getBlock().setType(definition.blockMaterial(), false);
                messages.send(event.getPlayer(), "errors.storage");
                return;
            }
            PlacedMachine removedMachine = removed.get();
            returnItem(event.getPlayer(), definition);
            if (definition.outputMaterial() != Material.AIR && removedMachine.storedAmount() > 0) {
                give(event.getPlayer(), definition.outputMaterial(), removedMachine.storedAmount());
            }
        }, mainThread);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> machines.at(position(block)).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> machines.at(position(block)).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> machines.at(position(block)).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> machines.at(position(block)).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        PlacedMachine machine = machines.at(position(event.getClickedBlock())).orElse(null);
        if (machine == null) return;
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("tycoon.machines")) {
            messages.send(event.getPlayer(), "errors.no-permission");
            return;
        }
        controls.open(event.getPlayer(), machine);
    }

    private MachinePosition position(Block block) {
        return new MachinePosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private void returnItem(Player player, MachineDefinition definition) {
        player.getInventory().addItem(items.create(definition)).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }

    private void give(Player player, Material material, long amount) {
        long remaining = amount;
        while (remaining > 0) {
            int size = (int) Math.min(material.getMaxStackSize(), remaining);
            ItemStack stack = new ItemStack(material, size);
            player.getInventory().addItem(stack).values().forEach(leftover ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover)
            );
            remaining -= size;
        }
    }
}
