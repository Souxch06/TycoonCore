package fr.valoriatycoon.gui;

import fr.valoriatycoon.commands.BalanceCommand;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
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

/** Modern configurable /is menu routing only to implemented authoritative systems. */
public final class IslandMainMenu implements Listener {
    private final IslandMenuSettings settings;
    private final BalanceCommand balance;
    private final FarmMenu farms;
    private final MachineShopPanel shop;
    private final PlotUpgradePanel upgrades;
    private final AutoSellPanel autoSell;
    private final QuestPanel quests;
    private final LeaderboardPanel leaderboards;
    private final RankPanel ranks;
    private final PetPanel pets;
    private final ItemVisualService visuals;
    private final MessageService messages;

    public IslandMainMenu(
            IslandMenuSettings settings,
            BalanceCommand balance,
            FarmMenu farms,
            MachineShopPanel shop,
            PlotUpgradePanel upgrades,
            AutoSellPanel autoSell,
            QuestPanel quests,
            LeaderboardPanel leaderboards,
            RankPanel ranks,
            PetPanel pets,
            ItemVisualService visuals,
            MessageService messages
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.farms = Objects.requireNonNull(farms, "farms");
        this.shop = Objects.requireNonNull(shop, "shop");
        this.upgrades = Objects.requireNonNull(upgrades, "upgrades");
        this.autoSell = Objects.requireNonNull(autoSell, "autoSell");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.leaderboards = Objects.requireNonNull(leaderboards, "leaderboards");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void open(Player player) {
        IslandMainMenuHolder holder = new IslandMainMenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder, settings.size(), messages.render(settings.title())
        );
        holder.bind(inventory);
        ItemStack filler = item(settings.filler(), " ", List.of());
        visuals.apply(filler, "ui/filler");
        for (int slot = 0; slot < settings.size(); slot++) inventory.setItem(slot, filler);
        settings.entries().forEach((slot, entry) -> inventory.setItem(slot, entryItem(entry)));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof IslandMainMenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        IslandMenuSettings.Entry entry = settings.entries().get(event.getRawSlot());
        if (entry == null) return;
        player.closeInventory();
        switch (entry.action()) {
            case BALANCE -> balance.showBalance(player);
            case STATS -> player.performCommand("is stats");
            case FARM -> farms.open(player);
            case MACHINES -> shop.open(player);
            case UPGRADES -> upgrades.open(player);
            case AUTOSELL -> autoSell.open(player);
            case SETTINGS -> player.performCommand("is settings");
            case TOOL_UPGRADES -> messages.send(player, "menu.hold-tool");
            case QUESTS -> quests.open(player);
            case LEADERBOARDS -> leaderboards.open(player);
            case RANKS -> ranks.open(player);
            case PETS -> pets.open(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof IslandMainMenuHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack entryItem(IslandMenuSettings.Entry entry) {
        ItemStack item = item(entry.material(), entry.name(), entry.lore());
        visuals.apply(item, "ui/main/" + ItemVisualService.segment(entry.action().name()));
        return item;
    }

    private ItemStack item(org.bukkit.Material material, String name, List<String> loreTemplates) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name));
        List<Component> lore = new ArrayList<>(loreTemplates.size());
        loreTemplates.forEach(line -> lore.add(messages.render(line)));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
