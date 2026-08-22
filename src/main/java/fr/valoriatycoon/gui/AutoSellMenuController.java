package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.farm.FarmSettings;
import fr.valoriatycoon.farm.autosell.AutoSellProfile;
import fr.valoriatycoon.farm.autosell.AutoSellService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Owns the dedicated auto-sell status rendering and persisted enable/disable toggle. */
public final class AutoSellMenuController {
    private final FarmSettings.AutoSellMenu menu;
    private final AutoSellService autoSell;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;
    private final Set<UUID> operationsInFlight = ConcurrentHashMap.newKeySet();

    public AutoSellMenuController(
            FarmSettings.AutoSellMenu menu,
            AutoSellService autoSell,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.autoSell = Objects.requireNonNull(autoSell, "autoSell");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void populateToggle(Inventory inventory, Player player) {
        visuals.fillMenu(inventory);
        inventory.setItem(menu.toggleSlot(), toggleItem(player));
    }

    public boolean handlesToggle(int rawSlot) {
        return rawSlot == menu.toggleSlot();
    }

    public void handleToggleClick(Player player, Runnable refreshMenu) {
        if (!player.hasPermission("tycoon.autosell")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        AutoSellProfile current = autoSell.profile(player.getUniqueId());
        if (!current.unlocked()) {
            messages.send(player, "farm.autosell-locked");
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!operationsInFlight.add(playerId)) {
            return;
        }
        autoSell.toggle(playerId).whenCompleteAsync((profile, error) -> {
            operationsInFlight.remove(playerId);
            if (error != null) {
                messages.send(player, "errors.storage");
                return;
            }
            messages.send(player, profile.enabled() ? "farm.autosell-enabled" : "farm.autosell-disabled");
            refreshMenu.run();
        }, mainThread);
    }

    private ItemStack toggleItem(Player player) {
        AutoSellProfile profile = autoSell.profile(player.getUniqueId());
        ItemStack item = new ItemStack(profile.unlocked()
                ? (profile.enabled() ? menu.enabledIcon() : menu.disabledIcon())
                : menu.lockedIcon());
        ItemMeta meta = item.getItemMeta();
        String name = profile.unlocked()
                ? (profile.enabled() ? menu.enabledName() : menu.disabledName())
                : menu.lockedName();
        TagResolver[] placeholders = new TagResolver[]{
                Placeholder.unparsed("level", Integer.toString(profile.level())),
                Placeholder.unparsed("max_level", Integer.toString(autoSell.maximumLevel())),
                Placeholder.unparsed("multiplier", profile.unlocked()
                        ? autoSell.level(profile.level()).saleMultiplier().toPlainString()
                        : "1")
        };
        meta.displayName(messages.render(name, placeholders));
        meta.lore(renderLore(menu.toggleLore(), placeholders));
        String state = !profile.unlocked() ? "locked" : profile.enabled() ? "enabled" : "disabled";
        visuals.apply(meta, "ui/autosell/" + state);
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> renderLore(List<String> templates, TagResolver... placeholders) {
        List<Component> lore = new ArrayList<>(templates.size());
        for (String line : templates) {
            lore.add(messages.render(line, placeholders));
        }
        return lore;
    }
}
