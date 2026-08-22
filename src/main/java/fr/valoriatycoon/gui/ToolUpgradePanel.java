package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.farm.autosell.AutoSellProfile;
import fr.valoriatycoon.farm.autosell.AutoSellPurchaseResult;
import fr.valoriatycoon.farm.autosell.AutoSellPurchaseStatus;
import fr.valoriatycoon.farm.autosell.AutoSellService;
import fr.valoriatycoon.tools.MultiToolItemService;
import fr.valoriatycoon.tools.ToolCapabilityDefinition;
import fr.valoriatycoon.tools.ToolDefinition;

import fr.valoriatycoon.tools.ToolSettings;
import fr.valoriatycoon.tools.ToolType;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

/** Shift-right-click panel for global auto-sell and tool-specific capability progression. */
public final class ToolUpgradePanel implements Listener {
    private final ToolSettings settings;
    private final AutoSellService autoSell;
    private final MultiToolItemService multiToolItems;
    private final ToolUpgradeItemFactory itemFactory;
    private final ToolCurrencyChoiceController currencyChoice;
    private final CurrencyFormatter currency;
    private final MessageService messages;
    private final Executor mainThread;
    private final BooleanSupplier available;
    private final Set<UUID> operationsInFlight = ConcurrentHashMap.newKeySet();

    public ToolUpgradePanel(
            ToolSettings settings,
            AutoSellService autoSell,
            MultiToolItemService multiToolItems,
            ToolUpgradeItemFactory itemFactory,
            ToolCurrencyChoiceController currencyChoice,
            CurrencyFormatter currency,
            MessageService messages,
            Executor mainThread,
            BooleanSupplier available
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.autoSell = Objects.requireNonNull(autoSell, "autoSell");
        this.multiToolItems = Objects.requireNonNull(multiToolItems, "multiToolItems");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
        this.currencyChoice = Objects.requireNonNull(currencyChoice, "currencyChoice");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.available = Objects.requireNonNull(available, "available");
    }

    public void open(Player player, ToolType type) {
        ToolUpgradePanelHolder holder = new ToolUpgradePanelHolder(type);
        ToolDefinition tool = settings.tool(type);
        Inventory inventory = Bukkit.createInventory(
                holder,
                settings.menu().size(),
                messages.render(settings.menu().title(), Placeholder.unparsed("tool", itemFactory.plainName(type)))
        );
        holder.bind(inventory);
        itemFactory.fillMenu(inventory);
        for (ToolDefinition selector : settings.tools().values()) {
            inventory.setItem(
                    selector.menuSlot(),
                    itemFactory.selectorItem(player.getUniqueId(), selector)
            );
        }
        inventory.setItem(settings.menu().infoSlot(), itemFactory.infoItem(player.getUniqueId(), tool));
        for (ToolCapabilityDefinition capability : settings.capabilities(type)) {
            inventory.setItem(
                    capability.slot(),
                    itemFactory.capabilityItem(player.getUniqueId(), type, capability)
            );
        }
        inventory.setItem(settings.menu().autoSellSlot(), itemFactory.autoSellItem(player.getUniqueId()));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || !event.getPlayer().isSneaking()
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        org.bukkit.inventory.ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        ToolType type = ToolType.fromMaterial(held.getType()).orElse(null);
        if (type == null || !multiToolItems.isOwnedBy(held, event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (!available.getAsBoolean()) {
            messages.send(event.getPlayer(), "errors.initializing");
            return;
        }
        if (!event.getPlayer().hasPermission("tycoon.tools")) {
            messages.send(event.getPlayer(), "errors.no-permission");
            return;
        }
        open(event.getPlayer(), type);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ToolCurrencyChoicePanelHolder choiceHolder) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player
                    && event.getRawSlot() >= 0
                    && event.getRawSlot() < settings.purchaseMenu().size()) {
                if (!available.getAsBoolean() || !player.hasPermission("tycoon.tools")) {
                    messages.send(player, "errors.no-permission");
                    return;
                }
                currencyChoice.handleClick(
                        player,
                        choiceHolder,
                        event.getRawSlot(),
                        () -> open(player, choiceHolder.toolType())
                );
            }
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof ToolUpgradePanelHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= settings.menu().size()) {
            return;
        }
        if (!available.getAsBoolean()) {
            messages.send(player, "errors.unavailable");
            return;
        }
        if (!player.hasPermission("tycoon.tools")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        for (ToolDefinition selector : settings.tools().values()) {
            if (selector.menuSlot() == event.getRawSlot()) {
                open(player, selector.type());
                return;
            }
        }
        if (event.getRawSlot() == settings.menu().autoSellSlot()) {
            if (!player.hasPermission("tycoon.autosell")) {
                messages.send(player, "errors.no-permission");
                return;
            }
            purchaseAutoSell(player, holder, event.isShiftClick());
            return;
        }
        for (ToolCapabilityDefinition capability : settings.capabilities(holder.toolType())) {
            if (capability.slot() == event.getRawSlot()) {
                currencyChoice.open(player, holder.toolType(), capability);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        boolean toolPanel = event.getView().getTopInventory().getHolder() instanceof ToolUpgradePanelHolder;
        boolean choicePanel = event.getView().getTopInventory().getHolder() instanceof ToolCurrencyChoicePanelHolder;
        int size = choicePanel ? settings.purchaseMenu().size() : settings.menu().size();
        if ((toolPanel || choicePanel) && event.getRawSlots().stream().anyMatch(slot -> slot < size)) {
            event.setCancelled(true);
        }
    }

    private void purchaseAutoSell(Player player, ToolUpgradePanelHolder holder, boolean confirmed) {
        AutoSellProfile profile = autoSell.profile(player.getUniqueId());
        if (profile.level() >= autoSell.maximumLevel()) {
            messages.send(player, "farm.autosell-maximum");
            return;
        }
        FarmSettings.AutoSellLevel next = autoSell.level(profile.level() + 1);
        if (!confirmed) {
            messages.send(
                    player,
                    "farm.autosell-upgrade-confirm",
                    Placeholder.unparsed("cost", currency.format(next.costCents()))
            );
            return;
        }
        if (!operationsInFlight.add(player.getUniqueId())) {
            return;
        }
        autoSell.purchaseNext(player.getUniqueId()).whenCompleteAsync((result, error) -> {
            operationsInFlight.remove(player.getUniqueId());
            if (error != null || result == null) {
                messages.send(player, "errors.storage");
            } else {
                sendAutoSellPurchaseResult(player, result);
                reopen(player, holder);
            }
        }, mainThread);
    }

    private void sendAutoSellPurchaseResult(Player player, AutoSellPurchaseResult result) {
        if (result.status() == AutoSellPurchaseStatus.SUCCESS) {
            messages.send(
                    player,
                    "farm.autosell-upgraded",
                    Placeholder.unparsed("level", Integer.toString(result.profile().level())),
                    Placeholder.unparsed(
                            "multiplier",
                            autoSell.level(result.profile().level()).saleMultiplier().toPlainString()
                    )
            );
        } else if (result.status() == AutoSellPurchaseStatus.INSUFFICIENT_FUNDS) {
            messages.send(
                    player,
                    "farm.autosell-insufficient-funds",
                    Placeholder.unparsed("cost", currency.format(result.chargedCents())),
                    Placeholder.unparsed("balance", currency.format(result.balanceCents()))
            );
        } else if (result.status() == AutoSellPurchaseStatus.MAXIMUM_LEVEL) {
            messages.send(player, "farm.autosell-maximum");
        } else {
            messages.send(player, "farm.autosell-profile-refreshed");
        }
    }

    private void reopen(Player player, ToolUpgradePanelHolder holder) {
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == holder) {
            open(player, holder.toolType());
        }
    }


}
