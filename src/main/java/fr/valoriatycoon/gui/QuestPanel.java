package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.quests.QuestDefinition;
import fr.valoriatycoon.quests.QuestProfile;
import fr.valoriatycoon.quests.QuestProgress;
import fr.valoriatycoon.quests.QuestRarity;
import fr.valoriatycoon.quests.QuestService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tools.ToolType;
import java.util.List;
import java.util.Objects;
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

/** Read-only repeatable quest progress and available-validation interface. */
public final class QuestPanel implements Listener {
    private final QuestService quests;
    private final ItemVisualService visuals;
    private final MessageService messages;

    public QuestPanel(
            QuestService quests,
            ItemVisualService visuals,
            MessageService messages
    ) {
        this.quests = Objects.requireNonNull(quests, "quests");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void open(Player player) {
        if (!player.hasPermission("tycoon.quests")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        QuestPanelHolder holder = new QuestPanelHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                messages.render("<dark_gray>Quêtes répétables</dark_gray>")
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        QuestProfile profile = quests.profile(player.getUniqueId());
        int slot = 0;
        for (QuestDefinition definition : quests.settings().quests().values()) {
            QuestProgress progress = profile.progress().getOrDefault(
                    definition.id(),
                    new QuestProgress(definition.id(), 0, 0)
            );
            inventory.setItem(slot++, questItem(definition, progress));
        }
        int summarySlot = 45;
        for (QuestRarity rarity : QuestRarity.values()) {
            inventory.setItem(summarySlot++, summaryItem(rarity, profile.available(rarity)));
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof QuestPanelHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof QuestPanelHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack questItem(QuestDefinition definition, QuestProgress progress) {
        ItemStack item = new ItemStack(icon(definition.toolType()));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render("<gold>" + definition.id() + "</gold>"));
        meta.lore(List.of(
                messages.render("<gray>Rareté : <yellow>" + definition.rarity() + "</yellow></gray>"),
                messages.render("<gray>Progression : <aqua>" + progress.progress()
                        + "/" + definition.targetActions() + "</aqua></gray>"),
                messages.render("<gray>Complétions totales : " + progress.completions() + "</gray>")
        ));
        visuals.apply(
                meta,
                "ui/quest/tool/" + ItemVisualService.segment(definition.toolType().name())
        );
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack summaryItem(QuestRarity rarity, long available) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(
                "<yellow>" + rarity + " disponibles : " + available + "</yellow>"
        ));
        visuals.apply(meta, "ui/quest/summary/" + ItemVisualService.segment(rarity.name()));
        item.setItemMeta(meta);
        return item;
    }

    private Material icon(ToolType type) {
        return switch (type) {
            case PICKAXE -> Material.IRON_PICKAXE;
            case HOE -> Material.IRON_HOE;
            case AXE -> Material.IRON_AXE;
            case FISHING_ROD -> Material.FISHING_ROD;
        };
    }
}
