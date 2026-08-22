package fr.valoriatycoon.gui;

import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tools.ToolCapabilityDefinition;
import fr.valoriatycoon.tools.ToolEffectService;
import fr.valoriatycoon.tools.ToolProfile;
import fr.valoriatycoon.tools.ToolProgressionService;
import fr.valoriatycoon.tools.ToolSettings;
import fr.valoriatycoon.tools.ToolType;
import fr.valoriatycoon.tools.ToolUpgradeCurrency;
import fr.valoriatycoon.tools.ToolUpgradeResult;
import fr.valoriatycoon.tools.ToolUpgradeStatus;
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
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Opens and handles the explicit base-money versus tool-coins purchase choice. */
public final class ToolCurrencyChoiceController {
    private final ToolSettings settings;
    private final ToolProgressionService tools;
    private final ToolEffectService effects;
    private final EconomyService economy;
    private final CurrencyFormatter currency;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;
    private final Set<UUID> purchasesInFlight = ConcurrentHashMap.newKeySet();

    public ToolCurrencyChoiceController(
            ToolSettings settings,
            ToolProgressionService tools,
            ToolEffectService effects,
            EconomyService economy,
            CurrencyFormatter currency,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void open(Player player, ToolType type, ToolCapabilityDefinition definition) {
        economy.getBalance(player.getUniqueId()).whenCompleteAsync((balance, error) -> {
            if (error != null) {
                messages.send(player, "errors.storage");
                return;
            }
            openLoaded(player, type, definition, balance);
        }, mainThread);
    }

    public void handleClick(
            Player player,
            ToolCurrencyChoicePanelHolder holder,
            int rawSlot,
            Runnable reopenParent
    ) {
        ToolUpgradeCurrency selected;
        if (rawSlot == settings.purchaseMenu().moneySlot()) {
            selected = ToolUpgradeCurrency.BASE_MONEY;
        } else if (rawSlot == settings.purchaseMenu().coinSlot()) {
            selected = ToolUpgradeCurrency.TOOL_COINS;
        } else {
            return;
        }
        if (!purchasesInFlight.add(player.getUniqueId())) {
            return;
        }
        tools.purchase(player.getUniqueId(), holder.toolType(), holder.capability(), selected)
                .whenCompleteAsync((result, error) -> {
                    purchasesInFlight.remove(player.getUniqueId());
                    if (error != null || result == null) {
                        messages.send(player, "errors.storage");
                    } else {
                        sendResult(player, result);
                        effects.refresh(player);
                        reopenParent.run();
                    }
                }, mainThread);
    }

    private void openLoaded(
            Player player,
            ToolType type,
            ToolCapabilityDefinition definition,
            BigDecimal moneyBalance
    ) {
        ToolProfile profile = tools.profile(player.getUniqueId(), type);
        int currentLevel = profile.capabilityLevel(definition.capability());
        if (currentLevel >= definition.maximumLevel()) {
            messages.send(player, "tools.maximum-level");
            return;
        }
        ToolCapabilityDefinition.Level next = definition.level(currentLevel + 1).orElseThrow();
        ToolSettings.PurchaseMenu menu = settings.purchaseMenu();
        ToolCurrencyChoicePanelHolder holder = new ToolCurrencyChoicePanelHolder(type, definition.capability());
        Inventory inventory = Bukkit.createInventory(
                holder,
                menu.size(),
                messages.render(
                        menu.title(),
                        Placeholder.unparsed("tool", toolDisplayName(type))
                )
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        TagResolver[] placeholders = new TagResolver[]{
                Placeholder.unparsed("money_cost", currency.format(next.moneyCostCents())),
                Placeholder.unparsed("money_balance", currency.format(MoneyCodec.toCents(moneyBalance))),
                Placeholder.unparsed("coin_cost", Long.toString(next.toolCoinCost())),
                Placeholder.unparsed("coins", Long.toString(profile.specialCoins())),
                Placeholder.unparsed("coin_name", settings.tool(type).currencyName())
        };
        inventory.setItem(menu.moneySlot(), optionItem(
                menu.moneyIcon(), menu.moneyName(), menu.moneyLore(), "ui/payment/money", placeholders
        ));
        inventory.setItem(menu.coinSlot(), optionItem(
                settings.tool(type).currencyIcon(),
                menu.coinName(),
                menu.coinLore(),
                "ui/payment/coins",
                placeholders
        ));
        player.openInventory(inventory);
    }

    private ItemStack optionItem(
            org.bukkit.Material material,
            String name,
            List<String> loreTemplates,
            String modelPath,
            TagResolver... placeholders
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name, placeholders));
        List<Component> lore = new ArrayList<>(loreTemplates.size());
        for (String template : loreTemplates) {
            lore.add(messages.render(template, placeholders));
        }
        meta.lore(lore);
        visuals.apply(meta, modelPath);
        item.setItemMeta(meta);
        return item;
    }

    private String toolDisplayName(ToolType type) {
        return switch (type) {
            case PICKAXE -> "Pioche";
            case HOE -> "Houe";
            case AXE -> "Hache";
            case FISHING_ROD -> "Canne à pêche";
        };
    }

    private void sendResult(Player player, ToolUpgradeResult result) {
        if (result.status() == ToolUpgradeStatus.SUCCESS) {
            String payment = result.currency() == ToolUpgradeCurrency.BASE_MONEY
                    ? currency.format(result.chargedAmount())
                    : result.chargedAmount() + " " + settings.tool(result.toolType()).currencyName();
            messages.send(
                    player,
                    "tools.upgraded-with-currency",
                    Placeholder.unparsed("capability", result.capability().storageKey()),
                    Placeholder.unparsed("level", Integer.toString(result.resultingLevel())),
                    Placeholder.unparsed("payment", payment)
            );
        } else if (result.status() == ToolUpgradeStatus.INSUFFICIENT_FUNDS) {
            messages.send(
                    player,
                    "tools.insufficient-funds",
                    Placeholder.unparsed("cost", currency.format(result.chargedAmount())),
                    Placeholder.unparsed("balance", currency.format(result.balanceCents()))
            );
        } else if (result.status() == ToolUpgradeStatus.INSUFFICIENT_TOOL_COINS) {
            messages.send(
                    player,
                    "tools.insufficient-coins",
                    Placeholder.unparsed("cost", Long.toString(result.chargedAmount())),
                    Placeholder.unparsed("balance", Long.toString(result.toolCoins())),
                    Placeholder.unparsed("coin_name", settings.tool(result.toolType()).currencyName())
            );
        } else if (result.status() == ToolUpgradeStatus.MAXIMUM_LEVEL) {
            messages.send(player, "tools.maximum-level");
        } else {
            messages.send(player, "tools.profile-refreshed");
        }
    }
}
