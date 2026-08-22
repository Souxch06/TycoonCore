package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.machines.MachineDefinition;
import fr.valoriatycoon.machines.MachineService;
import fr.valoriatycoon.machines.MachineSettings;
import fr.valoriatycoon.machines.MachineUpgradeStatus;
import fr.valoriatycoon.machines.MachineUpgradeType;
import fr.valoriatycoon.machines.PlacedMachine;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Virtual generator storage, auto-sell mode and money-only speed/price upgrades. */
public final class MachineControlPanel implements Listener {
    private final MachineService machines;
    private final MachineSettings settings;
    private final CurrencyFormatter currency;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;

    public MachineControlPanel(
            MachineService machines,
            MachineSettings settings,
            CurrencyFormatter currency,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.machines = Objects.requireNonNull(machines, "machines");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void open(Player player, PlacedMachine machine) {
        if (!machine.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "machines.not-owner");
            return;
        }
        MachineDefinition definition = settings.machine(machine.machineType());
        MachineControlHolder holder = new MachineControlHolder(machine.id());
        Inventory inventory = Bukkit.createInventory(
                holder, settings.control().size(),
                messages.render(settings.control().title(), Placeholder.unparsed("machine", definition.id()))
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        inventory.setItem(settings.control().statusSlot(), statusItem(machine, definition));
        inventory.setItem(settings.control().outputSlot(), outputItem(machine, definition));
        inventory.setItem(settings.control().collectSlot(), simple(
                Material.CHEST,
                "<green>Collecter les ressources</green>",
                "ui/machine/collect"
        ));
        inventory.setItem(settings.control().autoSellSlot(), simple(
                machine.autoSell() ? Material.LIME_DYE : Material.GRAY_DYE,
                machine.autoSell()
                        ? "<green>Vente automatique activée</green>"
                        : "<yellow>Stockage activé</yellow>",
                machine.autoSell() ? "ui/autosell/enabled" : "ui/autosell/disabled"
        ));
        inventory.setItem(settings.control().speedUpgradeSlot(), upgradeItem(machine, MachineUpgradeType.SPEED));
        inventory.setItem(settings.control().sellUpgradeSlot(), upgradeItem(machine, MachineUpgradeType.SELL_PRICE));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MachineControlHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlacedMachine machine = machines.byId(holder.machineId()).orElse(null);
        if (machine == null || !machine.ownerId().equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        if (slot == settings.control().collectSlot()) collect(player, machine);
        if (slot == settings.control().autoSellSlot()) toggle(player, machine);
        if (slot == settings.control().speedUpgradeSlot()) upgrade(player, machine, MachineUpgradeType.SPEED);
        if (slot == settings.control().sellUpgradeSlot()) upgrade(player, machine, MachineUpgradeType.SELL_PRICE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MachineControlHolder) {
            event.setCancelled(true);
        }
    }

    private void collect(Player player, PlacedMachine machine) {
        if (machine.storedAmount() <= 0) return;
        MachineDefinition definition = settings.machine(machine.machineType());
        machines.collect(machine.id()).whenCompleteAsync((before, error) -> {
            if (error != null) messages.send(player, "errors.storage");
            else {
                give(player, definition.outputMaterial(), before.storedAmount());
                machines.byId(machine.id()).ifPresent(updated -> open(player, updated));
            }
        }, mainThread);
    }

    private void toggle(Player player, PlacedMachine machine) {
        machines.toggleAutoSell(machine.id()).whenCompleteAsync((updated, error) -> {
            if (error != null) messages.send(player, "errors.storage");
            else open(player, updated);
        }, mainThread);
    }

    private void upgrade(Player player, PlacedMachine machine, MachineUpgradeType type) {
        machines.purchaseUpgrade(machine.id(), player.getUniqueId(), type)
                .whenCompleteAsync((result, error) -> {
                    if (error != null || result == null) {
                        messages.send(player, "errors.storage");
                    } else if (result.status() == MachineUpgradeStatus.INSUFFICIENT_FUNDS) {
                        messages.send(player, "machines.insufficient-money");
                    } else if (result.status() == MachineUpgradeStatus.MAXIMUM_LEVEL) {
                        messages.send(player, "machines.upgrade-maximum");
                    } else if (!result.successful()) {
                        messages.send(player, "machines.not-owner");
                    } else {
                        messages.send(player, "machines.upgraded");
                        open(player, result.machine());
                    }
                }, mainThread);
    }

    private ItemStack statusItem(PlacedMachine machine, MachineDefinition definition) {
        ItemStack item = new ItemStack(definition.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(definition.displayName()));
        meta.lore(List.of(
                messages.render("<gray>Vitesse niveau <yellow>" + machine.speedLevel() + "</yellow></gray>"),
                messages.render("<gray>Cycle : <yellow>" + (machines.intervalMillis(machine) / 1000.0) + "s</yellow></gray>"),
                messages.render("<gray>Prix niveau <yellow>" + machine.sellPriceLevel() + "</yellow></gray>"),
                messages.render("<gray>Vente/unité : <yellow>" + currency.format(machines.sellPriceCents(machine)) + "</yellow></gray>")
        ));
        visuals.apply(meta, "ui/machine/" + ItemVisualService.segment(definition.id()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack outputItem(PlacedMachine machine, MachineDefinition definition) {
        ItemStack item = new ItemStack(definition.outputMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render("<gold>Production</gold>"));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(messages.render("<gray>Stock : <yellow>" + machine.storedAmount()
                + "/" + definition.storageCapacity() + "</yellow></gray>"));
        lore.add(messages.render("<gray>Mode : "
                + (machine.autoSell() ? "<green>vente</green>" : "<aqua>stockage</aqua>") + "</gray>"));
        meta.lore(lore);
        visuals.apply(meta, "ui/machine/output");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack upgradeItem(PlacedMachine machine, MachineUpgradeType type) {
        int level = type == MachineUpgradeType.SPEED ? machine.speedLevel() : machine.sellPriceLevel();
        int maximum = type == MachineUpgradeType.SPEED
                ? settings.upgrades().speed().maximumLevel()
                : settings.upgrades().sellPrice().maximumLevel();
        Material icon = type == MachineUpgradeType.SPEED ? Material.SUGAR : Material.EMERALD;
        String label = type == MachineUpgradeType.SPEED ? "Vitesse" : "Prix de vente";
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render("<gold>Améliorer : " + label + "</gold>"));
        String cost = level >= maximum ? "MAX" : currency.format(machines.upgradeCost(type, level + 1));
        meta.lore(List.of(
                messages.render("<gray>Niveau : <yellow>" + level + "/" + maximum + "</yellow></gray>"),
                messages.render("<gray>Prix : <red>" + cost + "</red></gray>"),
                messages.render("<yellow>Cliquez pour acheter avec l’argent normal.</yellow>")
        ));
        visuals.apply(meta, type == MachineUpgradeType.SPEED
                ? "ui/machine/speed"
                : "ui/machine/sell_price");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack simple(Material material, String name, String modelPath) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name));
        visuals.apply(meta, modelPath);
        item.setItemMeta(meta);
        return item;
    }

    private void give(Player player, Material material, long amount) {
        long remaining = amount;
        while (remaining > 0) {
            int stackSize = (int) Math.min(material.getMaxStackSize(), remaining);
            ItemStack stack = new ItemStack(material, stackSize);
            player.getInventory().addItem(stack).values().forEach(leftover ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover)
            );
            remaining -= stackSize;
        }
    }
}
