package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.leaderboards.LeaderboardEntry;
import fr.valoriatycoon.leaderboards.LeaderboardService;
import fr.valoriatycoon.leaderboards.LeaderboardSettings;
import fr.valoriatycoon.leaderboards.LeaderboardSnapshot;
import fr.valoriatycoon.leaderboards.LeaderboardType;
import fr.valoriatycoon.leaderboards.LeaderboardValueFormatter;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

/** Read-only cached leaderboard category and top-ten interfaces. */
public final class LeaderboardPanel implements Listener {
    private static final List<Integer> ENTRY_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16, 21, 22, 23
    );

    private final LeaderboardService leaderboards;
    private final LeaderboardSettings settings;
    private final LeaderboardValueFormatter values;
    private final ItemVisualService visuals;
    private final MessageService messages;

    public LeaderboardPanel(
            LeaderboardService leaderboards,
            LeaderboardValueFormatter values,
            ItemVisualService visuals,
            MessageService messages
    ) {
        this.leaderboards = Objects.requireNonNull(leaderboards, "leaderboards");
        this.settings = leaderboards.settings();
        this.values = Objects.requireNonNull(values, "values");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void open(Player player) {
        if (!player.hasPermission("tycoon.leaderboards")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        if (!settings.enabled()) {
            messages.send(player, "errors.unavailable");
            return;
        }
        LeaderboardPanelHolder holder = new LeaderboardPanelHolder(null);
        Inventory inventory = Bukkit.createInventory(
                holder,
                settings.menu().size(),
                messages.render(settings.menu().title())
        );
        holder.bind(inventory);
        fill(inventory);
        LeaderboardSnapshot snapshot = leaderboards.snapshot();
        for (LeaderboardType type : LeaderboardType.values()) {
            inventory.setItem(settings.menu().slot(type), categoryItem(type, snapshot));
        }
        inventory.setItem(53, updatedItem(snapshot));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof LeaderboardPanelHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (holder.type() == null) {
            for (LeaderboardType type : LeaderboardType.values()) {
                if (event.getRawSlot() == settings.menu().slot(type)) {
                    openDetails(player, type);
                    return;
                }
            }
        } else if (event.getRawSlot() == settings.menu().backSlot()) {
            open(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LeaderboardPanelHolder) {
            event.setCancelled(true);
        }
    }

    private void openDetails(Player player, LeaderboardType type) {
        LeaderboardPanelHolder holder = new LeaderboardPanelHolder(type);
        Inventory inventory = Bukkit.createInventory(
                holder,
                settings.menu().size(),
                messages.render(
                        settings.menu().detailTitle(),
                        Placeholder.unparsed("category", type.displayName())
                )
        );
        holder.bind(inventory);
        fill(inventory);
        LeaderboardSnapshot snapshot = leaderboards.snapshot();
        List<LeaderboardEntry> entries = snapshot.entries(type);
        int displayed = Math.min(settings.displayLimit(), Math.min(entries.size(), ENTRY_SLOTS.size()));
        for (int index = 0; index < displayed; index++) {
            inventory.setItem(ENTRY_SLOTS.get(index), entryItem(type, entries.get(index)));
        }
        if (displayed == 0) {
            inventory.setItem(13, loadingItem(snapshot));
        }
        entries.stream()
                .filter(entry -> entry.playerId().equals(player.getUniqueId()))
                .findFirst()
                .ifPresent(entry -> inventory.setItem(45, ownPositionItem(type, entry)));
        inventory.setItem(settings.menu().backSlot(), backItem());
        inventory.setItem(53, updatedItem(snapshot));
        player.openInventory(inventory);
    }

    private ItemStack categoryItem(LeaderboardType type, LeaderboardSnapshot snapshot) {
        List<Component> lore = new ArrayList<>();
        if (!snapshot.initialized()) {
            lore.add(messages.render("<yellow>Premier calcul asynchrone en cours...</yellow>"));
        } else {
            List<LeaderboardEntry> entries = snapshot.entries(type);
            for (int index = 0; index < Math.min(3, entries.size()); index++) {
                LeaderboardEntry entry = entries.get(index);
                lore.add(messages.render("<gray>#" + entry.position() + " <white>"
                        + entry.playerName() + "</white> — <gold>"
                        + values.format(type, entry.value()) + "</gold></gray>"));
            }
            if (entries.isEmpty()) {
                lore.add(messages.render("<gray>Aucune donnée classée.</gray>"));
            }
            lore.add(messages.render("<yellow>Cliquez pour afficher le Top "
                    + settings.displayLimit() + ".</yellow>"));
        }
        return item(
                icon(type),
                "<gold><bold>" + type.displayName() + "</bold></gold>",
                lore,
                "ui/leaderboard/" + model(type)
        );
    }

    private ItemStack entryItem(LeaderboardType type, LeaderboardEntry entry) {
        String medal = switch (entry.position()) {
            case 1 -> "gold";
            case 2 -> "silver";
            case 3 -> "bronze";
            default -> "standard";
        };
        return item(
                entry.position() <= 3 ? Material.NETHER_STAR : Material.PAPER,
                "<yellow>#" + entry.position() + "</yellow> <white>" + entry.playerName() + "</white>",
                List.of(messages.render("<gray>Valeur : <gold>"
                        + values.format(type, entry.value()) + "</gold></gray>")),
                "ui/leaderboard/entry/" + medal
        );
    }

    private ItemStack ownPositionItem(LeaderboardType type, LeaderboardEntry entry) {
        return item(
                Material.COMPASS,
                "<aqua>Votre position : #" + entry.position() + "</aqua>",
                List.of(messages.render("<gray>Valeur : <yellow>"
                        + values.format(type, entry.value()) + "</yellow></gray>")),
                "ui/leaderboard/me"
        );
    }

    private ItemStack updatedItem(LeaderboardSnapshot snapshot) {
        String text = snapshot.initialized()
                ? age(snapshot.generatedAt())
                : "calcul en cours";
        return item(
                Material.CLOCK,
                "<aqua>Cache asynchrone</aqua>",
                List.of(messages.render("<gray>Actualisé : <yellow>" + text + "</yellow></gray>")),
                "ui/leaderboard/updated"
        );
    }

    private ItemStack loadingItem(LeaderboardSnapshot snapshot) {
        return item(
                Material.CLOCK,
                snapshot.initialized() ? "<gray>Aucune entrée</gray>" : "<yellow>Calcul en cours...</yellow>",
                List.of(messages.render("<dark_gray>Aucune requête SQL n’est exécutée par ce menu.</dark_gray>")),
                "ui/leaderboard/updated"
        );
    }

    private ItemStack backItem() {
        return item(
                Material.ARROW,
                "<yellow>Retour aux catégories</yellow>",
                List.of(),
                "ui/leaderboard/back"
        );
    }

    private void fill(Inventory inventory) {
        ItemStack filler = new ItemStack(settings.menu().filler());
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        visuals.apply(meta, "ui/filler");
        filler.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack item(Material material, String name, List<Component> lore, String modelPath) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name));
        meta.lore(lore);
        visuals.apply(meta, modelPath);
        item.setItemMeta(meta);
        return item;
    }

    private Material icon(LeaderboardType type) {
        return switch (type) {
            case MONEY -> Material.SUNFLOWER;
            case ISLAND_LEVEL -> Material.EXPERIENCE_BOTTLE;
            case RANK -> Material.NETHER_STAR;
            case PRODUCTION -> Material.PISTON;
            case PLAYTIME -> Material.CLOCK;
        };
    }

    private String model(LeaderboardType type) {
        return type.configKey().replace('-', '_');
    }

    private String age(Instant generatedAt) {
        long seconds = Math.max(0L, Duration.between(generatedAt, Instant.now()).getSeconds());
        return seconds < 2L ? "à l’instant" : "il y a " + seconds + "s";
    }
}
