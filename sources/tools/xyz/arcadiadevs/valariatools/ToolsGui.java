package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * L'interface d'amélioration : une rangée par âme, un bouton par décision.
 *
 * <h2>Un bouton = une intention</h2>
 * <p>Chaque âme occupe trois cases : <b>améliorer</b>, <b>statistiques</b>, <b>tout vendre</b>. Rien
 * d'autre n'est cliquable, et toute la grille est remplie d'un fond neutre : une case non prévue ne
 * doit jamais être un bouton caché. C'est la leçon tirée de l'ancienne interface des générateurs, où
 * deux cases faisaient la même chose et où le joueur cliquait au hasard.</p>
 *
 * <h2>Les vues sont suivies, pas les joueurs</h2>
 * <p>Le holder porte l'UUID du joueur et l'âme affichée : après un <code>/tools reload</code>, on peut
 * redessiner toutes les vues ouvertes sans qu'aucune ne pointe sur une configuration périmée. La vue est
 * retirée de la liste à la fermeture, y compris par ESC — sinon une vue fantôme réapparaîtrait au
 * joueur qui rouvre son inventaire.</p>
 */
public final class ToolsGui {

    /** L'inventière d'une vue ouverte : c'est ce qui permet de la retrouver et de la redessiner. */
    public static final class View implements InventoryHolder {

        private final UUID owner;
        private final Inventory inventory;
        private final List<ItemStack> slots = new ArrayList<ItemStack>();

        View(UUID owner, String title, int size) {
            this.owner = owner;
            this.inventory = Bukkit.createInventory(this, size, title);
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }

        UUID owner() {
            return this.owner;
        }

        void clear() {
            this.inventory.clear();
            this.slots.clear();
        }

        /**
         * Garde une reference sur un item pose : un bouton dont personne ne detient la copie ne peut
         * pas etre deplace, et le listener annule deja tout clic, les deux ensemble rendent la case
         * vraiment decoratif (sur les anciens serveurs, `setCancelled` seul laissait echapper l'item).
         */
        void track(ItemStack stack) {
            if (stack != null) {
                this.slots.add(stack);
            }
        }
    }

    /** Une seule vue par joueur : deux vues ouvertes = deux clics pour le même achat. */
    private static final java.util.Map<UUID, View> VIEWS = new java.util.HashMap<UUID, View>();
    private static final int SIZE = 45;
    /** Case de la vente globale, hors des.rangees d'âmes. */
    static final int SLOT_SELL_ALL = 40;

    private ToolsGui() {
    }

    /** Le clic est traité par ce listener, enregistre par le plugin a l'activation. */
    public static final class Handler implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            Inventory top = event.getView().getTopInventory();
            if (!(top.getHolder() instanceof View)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != top || !(event.getWhoClicked() instanceof Player)) {
                return;
            }
            View view = (View) top.getHolder();
            Player player = (Player) event.getWhoClicked();
            if (!player.getUniqueId().equals(view.owner())) {
                return;
            }
            int slot = event.getSlot();
            if (slot == SLOT_SELL_ALL) {
                final Player seller = player;
                Bukkit.getScheduler().runTask(ValoriaTools.get(), new Runnable() {

                    @Override
                    public void run() {
                        sellAll(seller);
                        render(seller);
                    }
                });
                return;
            }
            ToolKind kind = kindOfSlot(slot);
            if (kind == null) {
                return;
            }
            ValoriaTools plugin = ValoriaTools.get();
            if (plugin == null) {
                return;
            }
            // Les clics sont reports d'un tick : muter l'inventaire ou le solde pendant la
            // designation de l'evenement est le chemin le plus court vers un desync.
            final ToolKind chosen = kind;
            final int action = slot % 3;
            Bukkit.getScheduler().runTask(plugin, new Runnable() {

                @Override
                public void run() {
                    if (action == 0) {
                        upgrade(player, chosen);
                    } else if (action == 1) {
                        stats(player, chosen);
                    } else {
                        stats(player, chosen);
                    }
                    render(player);
                }
            });
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (event.getInventory().getHolder() instanceof View && event.getPlayer() instanceof Player) {
                VIEWS.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    /** Case 0, 9, 18, 27 = « améliorer » ; case +1 = stats ; case +2 = vendre. */
    static ToolKind kindOfSlot(int slot) {
        if (slot < 0 || slot >= 12) {
            return null;
        }
        ToolKind[] order = ToolKind.values();
        int index = slot / 3;
        return index < order.length ? order[index] : null;
    }

    static int slotOf(ToolKind kind, int action) {
        ToolKind[] order = ToolKind.values();
        for (int i = 0; i < order.length; i++) {
            if (order[i] == kind) {
                return i * 3 + action;
            }
        }
        return -1;
    }

    /** Ouvre (ou redessine) la vue du joueur. */
    public static void open(Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            player.sendMessage("§cValoriaTools n'est pas chargé.");
            return;
        }
        View view = VIEWS.get(player.getUniqueId());
        if (view == null) {
            view = new View(player.getUniqueId(), MultiTool.color("&8Multi-outil §7— amélioration"), SIZE);
            VIEWS.put(player.getUniqueId(), view);
        }
        render(view, player);
        player.openInventory(view.getInventory());
    }

    /** Redessine la vue d'un joueur déjà ouvert (après un achat, un reload). */
    public static void render(Player player) {
        View view = VIEWS.get(player.getUniqueId());
        if (view != null) {
            render(view, player);
        }
    }

    private static void render(View view, Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return;
        }
        ToolsConfig config = plugin.toolsConfig();
        ToolStore store = plugin.store();
        view.clear();
        Inventory inventory = view.getInventory();

        for (ToolKind kind : ToolKind.values()) {
            ToolsConfig.KindConfig kindConfig = config.kind(kind);
            if (kindConfig == null) {
                continue;
            }
            int tier = store.tierOf(player.getUniqueId(), kind, config.maxTier(kindConfig));
            int max = config.maxTier(kindConfig);
            boolean maxed = tier >= max;
            double price = config.priceOf(kindConfig, tier + 1);

            ItemStack upgrade = new ItemStack(kindConfig.material());
            ItemMeta upgradeMeta = upgrade.getItemMeta();
            if (upgradeMeta != null) {
                upgradeMeta.setDisplayName(MultiTool.color((maxed ? "&a" : "&e") + MultiTool.capitalize(kind.label())
                        + (maxed ? " — au maximum" : " — améliorer")));
                List<String> lines = new ArrayList<String>();
                lines.add(MultiTool.color("&7Palier actuel : &f" + tier + "&7/&f" + max));
                if (!maxed) {
                    lines.add(MultiTool.color("&7Prix de l'amélioration : &a" + plugin.economy().format(price)));
                    lines.add(MultiTool.color("&7Ce que le palier " + (tier + 1) + " apporte :"));
                    for (String detail : unlocksAt(config, kindConfig, tier + 1)) {
                        lines.add("  " + detail);
                    }
                    if (lines.size() == 3) {
                        lines.add("  &8(aucune capacite nouvelle a ce palier)");
                    }
                }
                if (!plugin.economy().available()) {
                    lines.add(MultiTool.color("&8Aucune economie : les ameliorations sont gratuites."));
                }
                upgradeMeta.setLore(lines);
                upgrade.setItemMeta(upgradeMeta);
            }
            view.track(upgrade);
            inventory.setItem(slotOf(kind, 0), upgrade);

            ItemStack info = new ItemStack(Material.PAPER);
            ItemMeta infoMeta = info.getItemMeta();
            if (infoMeta != null) {
                infoMeta.setDisplayName(MultiTool.color("&b" + MultiTool.capitalize(kind.label()) + " — statistiques"));
                List<String> lines = new ArrayList<String>();
                lines.add(MultiTool.color("&7Palier : &f" + tier + "&7/&f" + max));
                lines.add(MultiTool.color("&7Capacites actives :"));
                List<String> active = abilitiesAt(config, kindConfig, tier);
                if (active.isEmpty()) {
                    lines.add("  &8(aucune)");
                } else {
                    lines.addAll(active);
                }
                infoMeta.setLore(lines);
                info.setItemMeta(infoMeta);
            }
            view.track(info);
            inventory.setItem(slotOf(kind, 1), info);

            ItemStack next = new ItemStack(Material.BOOK);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(MultiTool.color("&f" + MultiTool.capitalize(kind.label())
                        + " — palier suivant"));
                List<String> lines = new ArrayList<String>();
                List<String> unlocks = unlocksAt(config, kindConfig, Math.min(max, tier + 1));
                if (maxed) {
                    lines.add(MultiTool.color("&aTout est débloqué."));
                } else if (unlocks.isEmpty()) {
                    lines.add(MultiTool.color("&8Aucune capacité nouvelle au palier " + (tier + 1) + "."));
                } else {
                    lines.addAll(unlocks);
                }
                nextMeta.setLore(lines);
                next.setItemMeta(nextMeta);
            }
            view.track(next);
            inventory.setItem(slotOf(kind, 2), next);
        }

        // La vente est globale (elle depend des prix declares, pas de l'âme cliquee) : une seule case,
        // sinon quatre boutons identiques dans la meme interface = quatre facons de cliquer travers.
        ItemStack sell = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta sellMeta = sell.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName(MultiTool.color("&6Vendre mon inventaire"));
            List<String> lines = new ArrayList<String>();
            lines.add(MultiTool.color("&7Vend tout ce que l'outil reconnait,"));
            lines.add(MultiTool.color("&7aux prix de &f" + (config.sellPricesDeclared()
                    ? "la grille de l'âme" : "&c(aucun prix declare)") + "&7."));
            lines.add(MultiTool.color("&7Le multi-outil en main n'est jamais vendu."));
            sellMeta.setLore(lines);
            sell.setItemMeta(sellMeta);
        }
        inventory.setItem(SLOT_SELL_ALL, sell);
        view.track(sell);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int slot = 12; slot < SIZE; slot++) {
            if (slot == SLOT_SELL_ALL) {
                continue;
            }
            view.track(filler);
            inventory.setItem(slot, filler);
        }
    }

    /** Les capacités que le palier demandé fait apparaître. */
    private static List<String> unlocksAt(ToolsConfig config, ToolsConfig.KindConfig kindConfig, int tier) {
        List<String> out = new ArrayList<String>();
        for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
            if (ability.fromTier() == tier) {
                out.add(MultiTool.color("&a+ " + ability.name()));
            }
        }
        return out;
    }

    /** Les capacités actives au palier courant, avec leurs valeurs : c'est ça, le « détail des capacités ». */
    private static List<String> abilitiesAt(ToolsConfig config, ToolsConfig.KindConfig kindConfig, int tier) {
        List<String> out = new ArrayList<String>();
        for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
            if (tier < ability.fromTier()) {
                out.add(MultiTool.color("&8✗ " + ability.name() + " §8(palier " + ability.fromTier() + ")"));
                continue;
            }
            out.add(MultiTool.color("&a✓ " + ability.name()) + values(ability, tier));
        }
        return out;
    }

    /** Résume les valeurs de la capacité au palier courant, sans inventer de libellé : les clés restent visibles. */
    private static String values(ToolsConfig.Ability ability, int tier) {
        StringBuilder out = new StringBuilder();
        for (String key : VALUES) {
            if (ability.numbers(key).isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(" §7/ ");
            }
            out.append("§7").append(key).append(" §f").append(ability.valueAt(key, tier - 1, 0));
        }
        for (String key : CHANCES) {
            if (ability.numbers(key).isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(" §7/ ");
            }
            out.append("§7").append(key).append(" §f").append(Math.round(ability.decimalAt(key, tier - 1, 0) * 100))
                    .append('%');
        }
        return out.length() == 0 ? "" : " §8(§f" + out + "§8)";
    }

    private static final String[] VALUES = {"max-blocks", "max-height", "extra-min", "extra-max"};
    private static final String[] CHANCES = {"chance", "strength", "multiplier", "treasure-chance", "heal-hearts"};

    // ------------------------------------------------------------------ actions

    private static void upgrade(Player player, ToolKind kind) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return;
        }
        ToolsConfig config = plugin.toolsConfig();
        ToolsConfig.KindConfig kindConfig = config.kind(kind);
        if (kindConfig == null) {
            player.sendMessage(MultiTool.color("&cAucune configuration pour cette âme d'outil."));
            return;
        }
        int tier = plugin.store().tierOf(player.getUniqueId(), kind, config.maxTier(kindConfig));
        int max = config.maxTier(kindConfig);
        if (tier >= max) {
            player.sendMessage(MultiTool.color("&e" + MultiTool.capitalize(kind.label())
                    + " est déjà à son palier maximum (" + max + ")."));
            return;
        }
        if (!ToolListener.holding(player)) {
            player.sendMessage(MultiTool.color("&cTiens le multi-outil en main pour l'améliorer."));
            return;
        }
        double price = config.priceOf(kindConfig, tier + 1);
        if (plugin.economy().available() && price > 0.0D && !plugin.economy().canAfford(player, price)) {
            player.sendMessage(MultiTool.color("&cPas assez d'argent : il manque &f"
                    + plugin.economy().format(price - plugin.economy().balance(player)) + "&c."));
            return;
        }
        EconomyService.Outcome taken = plugin.economy().withdraw(player, price);
        if (!taken.success()) {
            player.sendMessage(MultiTool.color("&cPaiement refuse : &f" + taken.reason()));
            return;
        }
        plugin.store().setTier(player, kind, tier + 1, max);
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (MultiTool.isMultiTool(tool)) {
            MultiTool.refresh(tool, config, plugin.store(), player.getUniqueId());
        }
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.4F);
        } catch (IllegalArgumentException | NoSuchFieldError | NoClassDefFoundError legacy) {
            // son decoratif
        }
        player.sendMessage(MultiTool.color("&a" + MultiTool.capitalize(kind.label()) + " → palier &f"
                + (tier + 1) + "&a (&7-" + plugin.economy().format(price) + "&a)"));
    }

    private static void stats(Player player, ToolKind kind) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return;
        }
        ToolsConfig config = plugin.toolsConfig();
        ToolsConfig.KindConfig kindConfig = config.kind(kind);
        if (kindConfig == null) {
            return;
        }
        int tier = plugin.store().tierOf(player.getUniqueId(), kind, config.maxTier(kindConfig));
        player.sendMessage(MultiTool.color("&8[&a" + MultiTool.capitalize(kind.label()) + "&8] &7palier &f"
                + tier + "&7/&f" + config.maxTier(kindConfig)));
        for (String line : abilitiesAt(config, kindConfig, tier)) {
            player.sendMessage("  " + line);
        }
    }

    private static void sellAll(Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return;
        }
        ToolsConfig config = plugin.toolsConfig();
        double total = 0.0D;
        int sold = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() == Material.AIR || MultiTool.isMultiTool(stack)) {
                continue;
            }
            ToolKind kind = plugin.matcher().kindOf(stack.getType());
            if (kind == null) {
                continue;
            }
            double price = plugin.sellPrice(kind, stack.getType());
            if (price <= 0.0D) {
                continue;
            }
            total += price * stack.getAmount();
            sold += stack.getAmount();
            player.getInventory().setItem(slot, null);
        }
        if (sold == 0) {
            player.sendMessage(MultiTool.color("&7Rien à vendre : aucun matériau reconnu (ou aucun prix"
                    + " déclaré dans la config)."));
            return;
        }
        double amount = ToolListener.round(total);
        EconomyService.Outcome credited = plugin.economy().deposit(player, amount);
        if (!credited.success()) {
            // le sac est deja vide : on rend tout, on ne laisse jamais un joueur sans items sans argent
            player.updateInventory();
            player.sendMessage(MultiTool.color("&cVente annulée : " + credited.reason()));
            return;
        }
        player.sendMessage(MultiTool.color("&a+" + plugin.economy().format(amount) + "&7 (" + sold + " vendu(s))"));
        player.updateInventory();
    }

    /** Vue ouverte d'un joueur, pour le reload. */
    public static void forget(UUID owner) {
        VIEWS.remove(owner);
    }

    /** Toutes les vues ouvertes : le plugin les redessine apres un reload. */
    public static java.util.Collection<View> views() {
        return new java.util.ArrayList<View>(VIEWS.values());
    }
}
