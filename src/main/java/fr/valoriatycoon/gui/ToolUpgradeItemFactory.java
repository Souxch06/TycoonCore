package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.farm.autosell.AutoSellProfile;
import fr.valoriatycoon.farm.autosell.AutoSellService;
import fr.valoriatycoon.professions.ProfessionProfile;
import fr.valoriatycoon.professions.ProfessionService;
import fr.valoriatycoon.professions.ProfessionType;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tools.ToolCapabilityDefinition;
import fr.valoriatycoon.tools.ToolDefinition;
import fr.valoriatycoon.tools.ToolProfile;
import fr.valoriatycoon.tools.ToolProgressionService;
import fr.valoriatycoon.tools.ToolSettings;
import fr.valoriatycoon.tools.ToolType;
import fr.valoriatycoon.utils.RomanNumerals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Renders all dynamic items in a tool-upgrade panel. */
public final class ToolUpgradeItemFactory {
    private final ToolSettings settings;
    private final ToolProgressionService tools;
    private final ProfessionService professions;
    private final AutoSellService autoSell;
    private final CurrencyFormatter currency;
    private final ItemVisualService visuals;
    private final MessageService messages;

    public ToolUpgradeItemFactory(
            ToolSettings settings,
            ToolProgressionService tools,
            ProfessionService professions,
            AutoSellService autoSell,
            CurrencyFormatter currency,
            ItemVisualService visuals,
            MessageService messages
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.professions = Objects.requireNonNull(professions, "professions");
        this.autoSell = Objects.requireNonNull(autoSell, "autoSell");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void fillMenu(org.bukkit.inventory.Inventory inventory) {
        visuals.fillMenu(inventory);
    }

    public ItemStack selectorItem(java.util.UUID playerId, ToolDefinition definition) {
        ToolProfile profile = tools.profile(playerId, definition.type());
        AutoSellProfile autoSellProfile = autoSell.profile(playerId);
        ItemStack item = new ItemStack(definition.icon());
        ItemMeta meta = item.getItemMeta();
        List<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.unparsed("tool_level_roman", RomanNumerals.format(profile.toolLevel())));
        resolvers.add(Placeholder.unparsed("coins", Long.toString(profile.specialCoins())));
        resolvers.add(Placeholder.unparsed("coin_name", definition.currencyName()));
        resolvers.add(Placeholder.unparsed("autosell_roman", RomanNumerals.format(autoSellProfile.level())));
        for (ToolCapabilityDefinition capability : settings.capabilities(definition.type())) {
            resolvers.add(Placeholder.unparsed(
                    capability.capability().storageKey() + "_roman",
                    RomanNumerals.format(profile.capabilityLevel(capability.capability()))
            ));
        }
        TagResolver[] placeholders = resolvers.toArray(TagResolver[]::new);
        meta.displayName(messages.render(definition.displayName(), placeholders));
        meta.lore(renderLore(definition.menuLore(), placeholders));
        visuals.apply(meta, "ui/tool/" + ItemVisualService.segment(definition.type().name()));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack infoItem(java.util.UUID playerId, ToolDefinition definition) {
        ToolProfile profile = tools.profile(playerId, definition.type());
        ProfessionType professionType = ProfessionType.fromTool(definition.type());
        ProfessionProfile profession = professions.profile(playerId, professionType);
        ItemStack item = new ItemStack(definition.icon());
        ItemMeta meta = item.getItemMeta();
        TagResolver[] placeholders = new TagResolver[]{
                Placeholder.unparsed("tool", plainName(definition.type())),
                Placeholder.unparsed("tool_level", Integer.toString(profile.toolLevel())),
                Placeholder.unparsed("tool_xp", Long.toString(profile.toolExperience())),
                Placeholder.unparsed("required_xp", Long.toString(tools.requiredExperience(profile.toolLevel()))),
                Placeholder.unparsed(
                        "profession",
                        professions.settings().definition(professionType).displayName()
                ),
                Placeholder.unparsed("profession_level", Integer.toString(profession.level())),
                Placeholder.unparsed("profession_xp", Long.toString(profession.experience())),
                Placeholder.unparsed(
                        "profession_required_xp",
                        Long.toString(professions.requiredExperience(profession.level()))
                ),
                Placeholder.unparsed("coins", Long.toString(profile.specialCoins())),
                Placeholder.unparsed("coin_name", definition.currencyName())
        };
        meta.displayName(messages.render(settings.menu().infoName(), placeholders));
        meta.lore(renderLore(settings.menu().infoLore(), placeholders));
        visuals.apply(meta, "ui/tool/info");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack capabilityItem(
            java.util.UUID playerId,
            ToolType type,
            ToolCapabilityDefinition definition
    ) {
        ToolProfile profile = tools.profile(playerId, type);
        int level = profile.capabilityLevel(definition.capability());
        boolean maximum = level >= definition.maximumLevel();
        ToolCapabilityDefinition.Level firstOrCurrent = definition.level(Math.max(1, level)).orElseThrow();
        BigDecimal currentValue = level == 0 ? BigDecimal.ZERO : firstOrCurrent.value();
        ToolCapabilityDefinition.Level next = maximum ? firstOrCurrent : definition.level(level + 1).orElseThrow();
        ItemStack item = new ItemStack(definition.icon());
        ItemMeta meta = item.getItemMeta();
        TagResolver[] placeholders = new TagResolver[]{
                Placeholder.unparsed("level", Integer.toString(level)),
                Placeholder.unparsed("max_level", Integer.toString(definition.maximumLevel())),
                Placeholder.unparsed("value", currentValue.toPlainString()),
                Placeholder.unparsed("next_value", next.value().toPlainString()),
                Placeholder.unparsed("value_percent", percent(currentValue)),
                Placeholder.unparsed("next_value_percent", percent(next.value())),
                Placeholder.unparsed("money_cost", maximum ? "MAX" : currency.format(next.moneyCostCents())),
                Placeholder.unparsed("coin_cost", maximum ? "MAX" : Long.toString(next.toolCoinCost())),
                Placeholder.unparsed("coins", Long.toString(profile.specialCoins())),
                Placeholder.unparsed("coin_name", settings.tool(type).currencyName())
        };
        meta.displayName(messages.render(definition.name(), placeholders));
        meta.lore(renderLore(definition.lore(), placeholders));
        visuals.apply(
                meta,
                "ui/tool/capability/" + ItemVisualService.segment(definition.capability().storageKey())
        );
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack autoSellItem(java.util.UUID playerId) {
        AutoSellProfile profile = autoSell.profile(playerId);
        boolean maximum = profile.level() >= autoSell.maximumLevel();
        ItemStack item = new ItemStack(maximum
                ? settings.menu().autoSellMaximumIcon()
                : settings.menu().autoSellIcon());
        ItemMeta meta = item.getItemMeta();
        BigDecimal current = profile.unlocked()
                ? autoSell.level(profile.level()).saleMultiplier()
                : BigDecimal.ONE;
        BigDecimal next = maximum ? current : autoSell.level(profile.level() + 1).saleMultiplier();
        long cost = maximum ? 0L : autoSell.level(profile.level() + 1).costCents();
        TagResolver[] placeholders = new TagResolver[]{
                Placeholder.unparsed("level", Integer.toString(profile.level())),
                Placeholder.unparsed("max_level", Integer.toString(autoSell.maximumLevel())),
                Placeholder.unparsed("value", current.toPlainString()),
                Placeholder.unparsed("next_value", next.toPlainString()),
                Placeholder.unparsed("cost", maximum ? "MAX" : currency.format(cost))
        };
        meta.displayName(messages.render(
                maximum ? settings.menu().autoSellMaximumName() : settings.menu().autoSellName(),
                placeholders
        ));
        meta.lore(renderLore(
                maximum ? settings.menu().autoSellMaximumLore() : settings.menu().autoSellLore(),
                placeholders
        ));
        visuals.apply(meta, maximum ? "ui/autosell/maximum" : "ui/autosell/upgrade");
        item.setItemMeta(meta);
        return item;
    }

    public String plainName(ToolType type) {
        return switch (type) {
            case PICKAXE -> "Pioche";
            case HOE -> "Houe";
            case AXE -> "Hache";
            case FISHING_ROD -> "Canne à pêche";
        };
    }

    private String percent(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
    }

    private List<Component> renderLore(List<String> templates, TagResolver... placeholders) {
        List<Component> result = new ArrayList<>(templates.size());
        for (String template : templates) {
            result.add(messages.render(template, placeholders));
        }
        return result;
    }
}
