package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonBoundaryService;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonStatus;
import fr.valoriatycoon.upgrades.PlotUpgradeDefinition;
import fr.valoriatycoon.upgrades.PlotUpgradeResult;
import fr.valoriatycoon.upgrades.PlotUpgradeSettings;
import fr.valoriatycoon.upgrades.PlotUpgradeStatus;
import fr.valoriatycoon.upgrades.PlotUpgradeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Purchases upgrades unique to the player's own Skyblock parcel. */
public final class PlotUpgradePanel implements Listener {
    private final PlotUpgradeSettings settings;
    private final TycoonService tycoons;
    private final TycoonBoundaryService boundaries;
    private final CurrencyFormatter currency;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;
    private final Set<UUID> purchasesInFlight = ConcurrentHashMap.newKeySet();

    public PlotUpgradePanel(
            PlotUpgradeSettings settings,
            TycoonService tycoons,
            TycoonBoundaryService boundaries,
            CurrencyFormatter currency,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.boundaries = Objects.requireNonNull(boundaries, "boundaries");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void open(Player player) {
        Tycoon tycoon = tycoons.ownedBy(player.getUniqueId()).orElse(null);
        if (tycoon == null) {
            messages.send(player, "tycoon.none");
            return;
        }
        if (tycoon.status() != TycoonStatus.ACTIVE) {
            messages.send(player, "tycoon.not-ready");
            return;
        }
        PlotUpgradePanelHolder holder = new PlotUpgradePanelHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                settings.menuSize(),
                messages.render(settings.menuTitle())
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        for (PlotUpgradeDefinition definition : settings.definitions().values()) {
            inventory.setItem(definition.slot(), item(tycoon, definition));
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PlotUpgradePanelHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= settings.menuSize()) {
            return;
        }
        if (!player.hasPermission("tycoon.upgrades")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        PlotUpgradeDefinition selected = settings.definitions().values().stream()
                .filter(definition -> definition.slot() == event.getRawSlot())
                .findFirst().orElse(null);
        if (selected == null) {
            return;
        }
        Tycoon tycoon = tycoons.ownedBy(player.getUniqueId()).orElse(null);
        if (tycoon == null || tycoon.status() != TycoonStatus.ACTIVE) {
            messages.send(player, "tycoon.not-ready");
            return;
        }
        int level = tycoons.upgradeLevel(tycoon, selected.type());
        if (level >= selected.maximumLevel()) {
            messages.send(player, "upgrades.maximum");
            return;
        }
        PlotUpgradeDefinition.Level next = selected.level(level + 1).orElseThrow();
        if (!event.isShiftClick()) {
            messages.send(
                    player,
                    "upgrades.confirm",
                    Placeholder.unparsed("cost", currency.format(next.costCents()))
            );
            return;
        }
        purchase(player, holder, selected);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PlotUpgradePanelHolder
                && event.getRawSlots().stream().anyMatch(slot -> slot < settings.menuSize())) {
            event.setCancelled(true);
        }
    }

    private void purchase(Player player, PlotUpgradePanelHolder holder, PlotUpgradeDefinition definition) {
        UUID playerId = player.getUniqueId();
        if (!purchasesInFlight.add(playerId)) {
            return;
        }
        tycoons.purchaseUpgrade(playerId, definition.type()).whenCompleteAsync((result, error) -> {
            purchasesInFlight.remove(playerId);
            if (error != null || result == null) {
                messages.send(player, "errors.storage");
            } else {
                sendResult(player, result);
                if (result.successful() && result.type() == PlotUpgradeType.PLOT_SIZE) {
                    boundaries.refresh(player);
                }
                if (player.getOpenInventory().getTopInventory().getHolder() == holder) {
                    open(player);
                }
            }
        }, mainThread);
    }

    private void sendResult(Player player, PlotUpgradeResult result) {
        if (result.status() == PlotUpgradeStatus.SUCCESS) {
            messages.send(
                    player,
                    "upgrades.purchased",
                    Placeholder.unparsed("upgrade", result.type().configKey()),
                    Placeholder.unparsed("level", Integer.toString(result.resultingLevel()))
            );
        } else if (result.status() == PlotUpgradeStatus.INSUFFICIENT_FUNDS) {
            messages.send(
                    player,
                    "upgrades.insufficient-funds",
                    Placeholder.unparsed("cost", currency.format(result.chargedCents())),
                    Placeholder.unparsed("balance", currency.format(result.balanceCents()))
            );
        } else if (result.status() == PlotUpgradeStatus.MAXIMUM_LEVEL) {
            messages.send(player, "upgrades.maximum");
        } else if (result.status() == PlotUpgradeStatus.PROFILE_STALE) {
            messages.send(player, "upgrades.refreshed");
        } else {
            messages.send(player, "tycoon.not-ready");
        }
    }

    private ItemStack item(Tycoon tycoon, PlotUpgradeDefinition definition) {
        int level = tycoons.upgradeLevel(tycoon, definition.type());
        boolean maximum = level >= definition.maximumLevel();
        PlotUpgradeDefinition.Level current = definition.level(level).orElseThrow();
        PlotUpgradeDefinition.Level next = maximum ? current : definition.level(level + 1).orElseThrow();
        TagResolver[] placeholders = new TagResolver[]{
                Placeholder.unparsed("level", Integer.toString(level)),
                Placeholder.unparsed("max_level", Integer.toString(definition.maximumLevel())),
                Placeholder.unparsed("value", Integer.toString(current.value())),
                Placeholder.unparsed("next_value", Integer.toString(next.value())),
                Placeholder.unparsed("cost", maximum ? "MAX" : currency.format(next.costCents()))
        };
        ItemStack item = new ItemStack(definition.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(definition.name(), placeholders));
        meta.lore(renderLore(definition.lore(), placeholders));
        visuals.apply(meta, "ui/upgrade/" + ItemVisualService.segment(definition.type().configKey()));
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> renderLore(List<String> templates, TagResolver... placeholders) {
        List<Component> result = new ArrayList<>(templates.size());
        for (String template : templates) {
            result.add(messages.render(template, placeholders));
        }
        return result;
    }
}
