package fr.valoriatycoon.gui;

import fr.valoriatycoon.compaction.CompactionService;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.ranks.RankPromotionStatus;
import fr.valoriatycoon.ranks.RankRequirement;
import fr.valoriatycoon.ranks.RankService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tools.MultiToolItemService;
import fr.valoriatycoon.tycoon.TycoonService;
import java.math.BigDecimal;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Displays the medieval rank overview and performs secure promotion on click. */
public final class RankPanel implements Listener {
    private final RankService ranks;
    private final CompactionService compaction;
    private final TycoonService tycoons;
    private final CurrencyFormatter currency;
    private final ItemVisualService visuals;
    private final MultiToolItemService multiToolItems;
    private final MessageService messages;
    private final Executor mainThread;

    public RankPanel(
            RankService ranks,
            CompactionService compaction,
            TycoonService tycoons,
            CurrencyFormatter currency,
            ItemVisualService visuals,
            MultiToolItemService multiToolItems,
            MessageService messages,
            Executor mainThread
    ) {
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.compaction = Objects.requireNonNull(compaction, "compaction");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.multiToolItems = Objects.requireNonNull(multiToolItems, "multiToolItems");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    /** Opens the rank progression panel for an island owner. */
    public void open(Player player) {
        if (!player.hasPermission("tycoon.ranks")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        var island = tycoons.ownedBy(player.getUniqueId()).orElse(null);
        if (island == null) {
            messages.send(player, "tycoon.none");
            return;
        }

        int nextRank = island.prestige() + 1;
        RankRequirement requirement = ranks.settings().level(nextRank).orElse(null);
        RankPanelHolder holder = new RankPanelHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                messages.render("<font:valoriatycoon:gui>\uE000\uE001</font><gold><bold>RANGS MÉDIÉVAUX</bold></gold>")
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        if (requirement == null) {
            RankRequirement maximum = ranks.settings().level(
                    ranks.settings().maximumLevel()
            ).orElseThrow();
            List<String> maximumLore = new ArrayList<>();
            maximumLore.add("<gray>Vous avez atteint le dernier rang.</gray>");
            maximumLore.addAll(benefitLore(maximum));
            inventory.setItem(13, item(
                    Material.NETHER_STAR,
                    "<gold>Duc — rang maximum</gold>",
                    maximumLore,
                    "ui/rank/maximum"
            ));
        } else {
            inventory.setItem(13, item(
                    Material.NETHER_STAR,
                    "<gold>Promouvoir : " + requirement.name() + "</gold>",
                    lore(requirement, island.playtimeSeconds()),
                    "ui/rank/" + nextRank
            ));
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RankPanelHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() != 13) {
            return;
        }

        ranks.promote(player).whenCompleteAsync((result, error) -> {
            if (error != null) {
                if (root(error) instanceof RankService.MissingRankItemsException) {
                    messages.send(player, "ranks.missing-items");
                } else {
                    messages.send(player, "errors.storage");
                }
                return;
            }
            if (result.status() == RankPromotionStatus.SUCCESS) {
                RankRequirement promoted = ranks.settings()
                        .level(result.resultingRank())
                        .orElseThrow();
                multiToolItems.synchronizeInventory(player, result.resultingRank());
                messages.send(
                        player,
                        "ranks.promoted",
                        Placeholder.unparsed("rank", promoted.name()),
                        Placeholder.unparsed(
                                "balance",
                                currency.format(result.resultingBalanceCents())
                        )
                );
                open(player);
            } else if (result.status() == RankPromotionStatus.INSUFFICIENT_MONEY) {
                messages.send(player, "ranks.missing-money");
            } else if (result.status() == RankPromotionStatus.INSUFFICIENT_PLAYTIME) {
                messages.send(player, "ranks.missing-playtime");
            } else if (result.status() == RankPromotionStatus.INSUFFICIENT_VANILLA_EXPERIENCE) {
                messages.send(player, "ranks.missing-vanilla-xp");
            } else if (result.status() == RankPromotionStatus.INSUFFICIENT_QUESTS) {
                messages.send(player, "ranks.missing-quests");
            } else if (result.status() == RankPromotionStatus.INSUFFICIENT_TOOL_LEVELS) {
                messages.send(player, "ranks.missing-tools");
            } else if (result.status() == RankPromotionStatus.INSUFFICIENT_PROFESSION_LEVELS) {
                messages.send(player, "ranks.missing-professions");
            } else if (result.status() == RankPromotionStatus.MAXIMUM_RANK) {
                messages.send(player, "ranks.maximum");
            } else {
                messages.send(player, "ranks.not-ready");
            }
        }, mainThread);
    }

    private List<String> lore(RankRequirement requirement, long currentPlaytimeSeconds) {
        List<String> lines = new ArrayList<>();
        lines.add("<gray>Prix débité : <yellow>"
                + currency.format(requirement.requiredMoneyCents())
                + "</yellow></gray>");
        if (requirement.requiredPlaytimeSeconds() > 0L) {
            lines.add("<gray>Temps de jeu cumulé : <yellow>"
                    + completedMinutes(currentPlaytimeSeconds)
                    + " min / "
                    + completedMinutes(requirement.requiredPlaytimeSeconds())
                    + " min</yellow></gray>");
        }
        lines.add("<gray>Niveaux XP vanilla consommés : <yellow>"
                + requirement.requiredVanillaExperienceLevels()
                + "</yellow></gray>");
        requirement.quests().forEach((rarity, amount) -> lines.add(
                "<gray>Quêtes " + rarity.name() + " : <yellow>" + amount + "</yellow></gray>"
        ));
        requirement.toolLevels().forEach((tool, level) -> lines.add(
                "<gray>Outil " + tool.name() + " niveau <yellow>" + level + "</yellow></gray>"
        ));
        requirement.professionLevels().forEach((profession, level) -> lines.add(
                "<gray>Métier " + profession.displayName() + " niveau <gold>" + level + "</gold></gray>"
        ));
        requirement.items().forEach((material, amount) -> lines.add(
                "<gray>" + amount + "x " + material.name() + "</gray>"
        ));
        requirement.compactedItems().forEach((resource, amount) -> lines.add(
                "<light_purple>" + amount + "x " + compaction.displayName(resource)
                        + "</light_purple>"
        ));
        lines.addAll(benefitLore(requirement));
        lines.add("<red>Le prix sera débité ; niveaux et XP des outils seront réinitialisés.</red>");
        lines.add("<red>Objets, validations et niveaux XP vanilla seront consommés.</red>");
        lines.add("<green>Argent restant, temps de jeu et métiers seront conservés.</green>");
        lines.add("<green>Cliquez pour obtenir ce rang.</green>");
        return lines;
    }

    private List<String> benefitLore(RankRequirement requirement) {
        List<String> lines = new ArrayList<>();
        lines.add("<gold><bold>Avantages permanents de ce rang :</bold></gold>");
        lines.add("<green>+" + percentage(requirement.permanentRevenueBonus())
                + "% revenus de vente</green>");
        lines.add("<green>+" + percentage(requirement.toolExperienceBonus())
                + "% XP des outils</green>");
        lines.add("<green>+" + percentage(requirement.professionExperienceBonus())
                + "% XP des métiers</green>");
        lines.add("<green>+" + percentage(requirement.toolCoinBonus())
                + "% coins des outils</green>");
        lines.add("<green>+" + percentage(requirement.generatorProductionBonus())
                + "% production des générateurs</green>");
        if (requirement.generatorSlotBonus() > 0) {
            lines.add("<green>+" + requirement.generatorSlotBonus()
                    + " emplacements de générateurs</green>");
        }
        return lines;
    }

    private String percentage(double bonus) {
        return BigDecimal.valueOf(bonus)
                .movePointRight(2)
                .stripTrailingZeros()
                .toPlainString();
    }

    private ItemStack item(
            Material material,
            String name,
            List<String> lines,
            String modelPath
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name));
        meta.lore(lines.stream().map(messages::render).toList());
        visuals.apply(meta, modelPath);
        item.setItemMeta(meta);
        return item;
    }

    private long completedMinutes(long seconds) {
        return Math.max(0L, seconds) / 60L;
    }

    private Throwable root(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
