package fr.valoriatycoon.gui;

import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.machines.MachineDefinition;
import fr.valoriatycoon.machines.MachineItemService;
import fr.valoriatycoon.machines.MachineSettings;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tools.ToolCoinSpendStatus;
import fr.valoriatycoon.tools.ToolProgressionService;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
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

/** Machine shop with explicit base-money or matching tool-coin payment. */
public final class MachineShopPanel implements Listener {
    private final MachineSettings settings;
    private final MachineItemService items;
    private final EconomyService economy;
    private final ToolProgressionService tools;
    private final TycoonService tycoons;
    private final CurrencyFormatter currency;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;
    private final Set<UUID> purchases = ConcurrentHashMap.newKeySet();

    public MachineShopPanel(
            MachineSettings settings,
            MachineItemService items,
            EconomyService economy,
            ToolProgressionService tools,
            TycoonService tycoons,
            CurrencyFormatter currency,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.items = Objects.requireNonNull(items, "items");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void open(Player player) {
        var tycoon = tycoons.ownedBy(player.getUniqueId()).orElse(null);
        if (tycoon == null || tycoon.status() != TycoonStatus.ACTIVE) {
            messages.send(player, "tycoon.not-ready");
            return;
        }
        MachineShopHolder holder = new MachineShopHolder();
        Inventory inventory = Bukkit.createInventory(
                holder, settings.shop().size(), messages.render(settings.shop().title())
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        for (MachineDefinition definition : settings.machines().values()) {
            inventory.setItem(definition.shopSlot(), shopItem(definition));
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTopInventory().getHolder() instanceof MachineShopHolder) {
            event.setCancelled(true);
            MachineDefinition selected = settings.machines().values().stream()
                    .filter(machine -> machine.shopSlot() == event.getRawSlot())
                    .findFirst().orElse(null);
            if (selected != null) openPurchase(player, selected);
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof MachinePurchaseHolder holder) {
            event.setCancelled(true);
            if (event.getRawSlot() == 3) purchase(player, holder.machineType(), false);
            if (event.getRawSlot() == 5) purchase(player, holder.machineType(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MachineShopHolder
                || event.getView().getTopInventory().getHolder() instanceof MachinePurchaseHolder) {
            event.setCancelled(true);
        }
    }

    private void openPurchase(Player player, MachineDefinition definition) {
        MachinePurchaseHolder holder = new MachinePurchaseHolder(definition.id());
        Inventory inventory = Bukkit.createInventory(
                holder, 9, messages.component("machines.purchase-title", false,
                        Placeholder.unparsed("machine", definition.id()))
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        inventory.setItem(3, option(
                Material.SUNFLOWER,
                "<gold>Argent normal</gold>",
                currency.format(definition.moneyCostCents()),
                "ui/payment/money"
        ));
        inventory.setItem(5, option(
                tools.settings().tool(definition.coinType()).currencyIcon(),
                "<green>" + tools.settings().tool(definition.coinType()).currencyName() + "</green>",
                definition.coinCost() + " " + tools.settings().tool(definition.coinType()).currencyName(),
                "ui/payment/coins"
        ));
        player.openInventory(inventory);
    }

    private void purchase(Player player, String machineType, boolean useCoins) {
        if (!purchases.add(player.getUniqueId())) return;
        MachineDefinition definition = settings.machine(machineType);
        if (useCoins) {
            tools.spendCoins(
                    player.getUniqueId(), definition.coinType(), definition.coinCost(), "machine:" + machineType
            ).whenCompleteAsync((result, error) -> {
                purchases.remove(player.getUniqueId());
                if (error != null) {
                    messages.send(player, "errors.storage");
                } else if (result.status() == ToolCoinSpendStatus.INSUFFICIENT_COINS) {
                    messages.send(player, "machines.insufficient-coins");
                } else {
                    deliver(player, definition);
                }
            }, mainThread);
        } else {
            economy.removeMoney(
                    player.getUniqueId(), BigDecimal.valueOf(definition.moneyCostCents(), 2), "machine:purchase:" + machineType
            ).whenCompleteAsync((result, error) -> {
                purchases.remove(player.getUniqueId());
                if (error != null || result == null) {
                    messages.send(player, "errors.storage");
                } else if (!result.successful()) {
                    messages.send(player, "machines.insufficient-money");
                } else {
                    deliver(player, definition);
                }
            }, mainThread);
        }
    }

    private void deliver(Player player, MachineDefinition definition) {
        player.closeInventory();
        player.getInventory().addItem(items.create(definition)).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
        messages.send(player, "machines.purchased", Placeholder.unparsed("machine", definition.id()));
    }

    private ItemStack shopItem(MachineDefinition definition) {
        ItemStack item = new ItemStack(definition.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(definition.displayName()));
        List<Component> lore = new ArrayList<>();
        definition.lore().forEach(line -> lore.add(messages.render(line)));
        lore.add(messages.render("<gray>Argent : <red>" + currency.format(definition.moneyCostCents()) + "</red></gray>"));
        lore.add(messages.render("<gray>Coins : <green>" + definition.coinCost() + " "
                + tools.settings().tool(definition.coinType()).currencyName() + "</green></gray>"));
        meta.lore(lore);
        visuals.apply(meta, "ui/machine/" + ItemVisualService.segment(definition.id()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack option(
            Material material,
            String name,
            String cost,
            String modelPath
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name));
        meta.lore(List.of(messages.render("<gray>Prix : <yellow>" + cost + "</yellow></gray>")));
        visuals.apply(meta, modelPath);
        item.setItemMeta(meta);
        return item;
    }
}
