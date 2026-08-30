package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Le panneau d'amélioration : la disposition des captures du wiki GenTycoon, recopiée en API Bukkit
 * seule — <b>aucun pack de textures n'est nécessaire</b>, chaque case est un item vanilla.
 *
 * <h2>La grille</h2>
 * <pre>
 *  rangée 1 :  [pioche] [hache] [canne] [épée]  ·  [palier ↑]  ·  [solde]  [fermer]
 *  rangées 2-4 : une case par capacité (27 par page), posée sur le vitrage teinté de l'âme
 *  rangée 5 :  [✔ achetée] [● achetable] [✖ verrouillée]  ·  [aide]
 *  rangée 6 :  [◀ page] [vendre le sac] [statistiques] [mode ×1/×10/×100] · [page ▶]
 * </pre>
 * <p>Le haut est la <em>sélection</em> (quelle âme, quel palier, combien d'argent, sortir) ; le milieu est
 * le <em>barème</em> (une capacité = une case = une intention d'achat) ; le bas est la <em>navigation</em>
 * et les services. Les trois états d'une capacité se lisent sans ouvrir une tooltip : la rangée 5 en est
 * la légende, et la case commence par le signe de son état (✔ ● ✖ ★).</p>
 *
 * <h2>Pourquoi une case par capacité et pas un catalogue paginé</h2>
 * <p>Le barème de GenTycoon compte jusqu'à 24 améliorations pour une âme : elles tiennent dans les trois
 * rangées centrales, sans page. La pagination existe quand même — elle s'allume dès qu'un admin déclare
 * plus de 27 capacités — parce qu'un menu qui coupe une ligne de la config en silence est un menu qui ment
 * sur le barème.</p>
 *
 * <h2>Les vues sont suivies, pas les joueurs</h2>
 * <p>Le holder porte l'UUID du joueur, l'âme affichée, la page et le mode d'achat : après un
 * <code>/tools reload</code>, on peut redessiner toutes les vues ouvertes sans qu'aucune ne pointe sur une
 * configuration périmée. La vue est retirée à la fermeture, y compris par ESC — sinon une vue fantôme
 * réapparaîtrait au joueur qui rouvre son inventaire.</p>
 */
public final class ToolsGui {

    /** Les modes d'achat, dans l'ordre du cycle : un clic paie 1, 10 ou 100 niveaux. */
    static final int[] MODES = new int[]{1, 10, 100};

    /** L'inventière d'une vue ouverte : c'est ce qui permet de la retrouver et de la redessiner. */
    public static final class View implements InventoryHolder {

        private final UUID owner;
        private final Inventory inventory;
        private final List<ItemStack> slots = new ArrayList<ItemStack>();
        private ToolKind kind;
        private int page;
        private int mode = 1;

        View(UUID owner, String title, int size, ToolKind kind) {
            this.owner = owner;
            this.kind = kind;
            this.inventory = Bukkit.createInventory(this, size, title);
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }

        UUID owner() {
            return this.owner;
        }

        /** L'âme dont on affiche les capacités (le joueur la change en cliquant sur les icônes du haut). */
        ToolKind kind() {
            return this.kind;
        }

        /** Changer d'âme remet la page à zéro : une page 3 d'une âme qui en a 18 n'a pas de sens. */
        void kind(ToolKind kind) {
            this.kind = kind;
            this.page = 0;
        }

        int page() {
            return this.page;
        }

        void page(int page) {
            this.page = Math.max(0, page);
        }

        /** Le nombre de niveaux payés par un clic. */
        int mode() {
            return this.mode;
        }

        void nextMode() {
            for (int i = 0; i < MODES.length; i++) {
                if (MODES[i] == this.mode) {
                    this.mode = MODES[(i + 1) % MODES.length];
                    return;
                }
            }
            this.mode = MODES[0];
        }

        void clear() {
            this.inventory.clear();
            this.slots.clear();
        }

        /**
         * Garde une référence sur un item posé : un bouton dont personne ne détient la copie ne peut pas
         * être déplacé, et le listener annule déjà tout clic — les deux ensemble rendent la case vraiment
         * décorative (sur les anciens serveurs, <code>setCancelled</code> seul laissait échapper l'item).
         */
        void track(ItemStack stack) {
            if (stack != null) {
                this.slots.add(stack);
            }
        }
    }

    /** Une seule vue par joueur : deux vues ouvertes = deux clics pour le même achat. */
    private static final Map<UUID, View> VIEWS = new HashMap<UUID, View>();
    private static final int SIZE = 54;

    /** Rangée 1 — les quatre âmes, le palier, le solde, la fermeture. */
    static final int SLOT_SOUL_FIRST = 0;
    static final int SLOT_TIER = 5;
    static final int SLOT_MONEY = 7;
    static final int SLOT_CLOSE = 8;
    /** Rangées 2 à 4 — une case par capacité, 27 par page. */
    static final int SLOT_ABILITY_FIRST = 9;
    static final int ABILITY_SLOTS = 27;
    /** Rangée 5 — la légende des trois états, puis l'aide. */
    static final int SLOT_LEGEND = 36;
    static final int SLOT_HELP = 40;
    /** Rangée 6 — navigation et services. */
    static final int SLOT_PREV = 45;
    static final int SLOT_SELL = 46;
    static final int SLOT_STATS = 47;
    static final int SLOT_MODE = 48;
    static final int SLOT_NEXT = 53;

    /** Icônes des noyaux : une capacité se reconnaît à son symbole avant son nom. */
    private static final Map<String, Material> ICONS = icons();

    private ToolsGui() {
    }

    private static Map<String, Material> icons() {
        Map<String, Material> out = new HashMap<String, Material>();
        put(out, "VEIN", Material.IRON_PICKAXE);
        put(out, "TREE_FELL", Material.OAK_LOG);
        put(out, "AREA_BREAK", Material.TNT_MINECART);
        put(out, "EXTRA_BLOCK", Material.STONE);
        put(out, "GHOST_MINES", Material.GHAST_TEAR);
        put(out, "CROP_HARVEST", Material.WHEAT);
        put(out, "AUTO_SMELT", Material.FURNACE);
        put(out, "FORTUNE", Material.DIAMOND);
        put(out, "DOUBLE_DROP", Material.CHEST);
        put(out, "SELL_ON_BREAK", Material.GOLD_NUGGET);
        put(out, "INFINITE_DURABILITY", Material.NETHERITE_SCRAP);
        put(out, "MONEY_MULT", Material.GOLD_INGOT);
        put(out, "MONEY_DOUBLE", Material.GOLD_BLOCK);
        put(out, "MONEY_POUCH", Material.PAPER);
        put(out, "XP_FLAT", Material.EXPERIENCE_BOTTLE);
        put(out, "XP_MULT", Material.BOOK);
        put(out, "TREASURE", Material.TRIPWIRE_HOOK);
        put(out, "RANDOM_ENCHANT", Material.ENCHANTED_BOOK);
        put(out, "FURY", Material.BLAZE_POWDER);
        put(out, "PROC_BOOSTER", Material.CLOCK);
        put(out, "HASTE", Material.SUGAR);
        put(out, "SWIFT", Material.FEATHER);
        put(out, "SOUL_SPEED", Material.SOUL_SAND);
        put(out, "CRIT", Material.DIAMOND_SWORD);
        put(out, "DAMAGE_MULT", Material.IRON_SWORD);
        put(out, "LIFE_STEAL", Material.REDSTONE);
        put(out, "KNOCKBACK", Material.PISTON);
        put(out, "POTION_APPLY", Material.POTION);
        put(out, "AUTO_SWING", Material.BOW);
        put(out, "MULTI_KILL", Material.ROTTEN_FLESH);
        put(out, "AUTO_REEL", Material.TRIPWIRE_HOOK);
        put(out, "FAST_REEL", Material.CLOCK);
        put(out, "MULTI_CATCH", Material.COD);
        put(out, "LUCK", Material.RABBIT_FOOT);
        return Collections.unmodifiableMap(out);
    }

    private static void put(Map<String, Material> map, String key, Material material) {
        map.put(key, material);
    }

    /** Le clic est traité par ce listener, enregistré par le plugin à l'activation. */
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
            final View view = (View) top.getHolder();
            final Player player = (Player) event.getWhoClicked();
            if (!player.getUniqueId().equals(view.owner())) {
                return;
            }
            if (ValoriaTools.get() == null) {
                return;
            }
            final int slot = event.getSlot();
            final boolean bulk = event.isShiftClick();
            final ToolKind shown = view.kind();
            final int mode = view.mode();
            final int page = view.page();
            // Les clics sont reportés d'un tick : muter l'inventaire ou le solde pendant la désignation
            // de l'événement est le chemin le plus court vers un desync.
            Bukkit.getScheduler().runTask(ValoriaTools.get(), new Runnable() {

                @Override
                public void run() {
                    handle(player, view, slot, bulk, shown, mode, page);
                    render(player);
                }
            });
        }

        private void handle(Player player, View view, int slot, boolean bulk, ToolKind shown, int mode,
                int page) {
            ValoriaTools plugin = ValoriaTools.get();
            if (plugin == null) {
                return;
            }
            if (slot == SLOT_CLOSE) {
                player.closeInventory();
                return;
            }
            if (slot == SLOT_SELL) {
                sellAll(player);
                return;
            }
            if (slot == SLOT_MODE) {
                view.nextMode();
                return;
            }
            if (slot == SLOT_HELP) {
                guide(player, shown);
                return;
            }
            if (slot >= SLOT_SOUL_FIRST && slot < SLOT_SOUL_FIRST + ToolKind.values().length) {
                view.kind(ToolKind.values()[slot - SLOT_SOUL_FIRST]);   // changer d'ame, page 0
                return;
            }
            if (slot == SLOT_STATS) {
                stats(player, shown);
                return;
            }
            if (slot == SLOT_TIER) {
                upgradeTier(player, shown);
                return;
            }
            if (slot == SLOT_PREV || slot == SLOT_NEXT) {
                int pages = pages(player, shown);
                if (pages > 1) {
                    int moved = slot == SLOT_PREV ? view.page() - 1 : view.page() + 1;
                    view.page(Math.max(0, Math.min(pages - 1, moved)));
                }
                return;
            }
            if (slot < SLOT_ABILITY_FIRST || slot >= SLOT_ABILITY_FIRST + ABILITY_SLOTS) {
                return;
            }
            int index = page * ABILITY_SLOTS + slot - SLOT_ABILITY_FIRST;
            ToolsConfig.KindConfig kindConfig = plugin.toolsConfig().kind(shown);
            if (kindConfig == null) {
                player.sendMessage(MultiTool.color("&cAucune configuration pour cette âme d'outil."));
                return;
            }
            List<ToolsConfig.Ability> abilities = plugin.toolsConfig().abilities(kindConfig);
            if (index < 0 || index >= abilities.size()) {
                return;   // case de vitrage vide : rien a acheter ici
            }
            buyLevel(player, shown, abilities.get(index), bulk ? mode * 10 : mode);
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (event.getInventory().getHolder() instanceof View && event.getPlayer() instanceof Player) {
                VIEWS.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    /** L'âme affichée par défaut : celle du bloc visé, pioche si le joueur ne vise rien. */
    private static ToolKind currentKind(Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return ToolKind.PICKAXE;
        }
        try {
            Block target = player.getTargetBlockExact(6);
            if (target != null) {
                ToolKind kind = plugin.matcher().kindOf(target);
                if (kind != null) {
                    return kind;
                }
            }
        } catch (RuntimeException | LinkageError unavailable) {
            // pas de raycast sur ce serveur : l'âme de secours suffit
        }
        return plugin.toolsConfig().fallbackKind();
    }

    /** Ouvre le menu directement sur une âme (<code>/tools gui canne</code>). */
    public static void open(Player player, ToolKind kind) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null || kind == null) {
            open(player);
            return;
        }
        View existing = VIEWS.get(player.getUniqueId());
        if (existing != null) {
            existing.kind(kind);
        }
        open(player);
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
            view = new View(player.getUniqueId(), MultiTool.color("&8Multi-outil §7— améliorations"), SIZE,
                    currentKind(player));
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

    /** Combien de pages tient le barème de cette âme (1 tant qu'il rentre dans les 27 cases). */
    private static int pages(Player player, ToolKind kind) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return 1;
        }
        ToolsConfig.KindConfig kindConfig = plugin.toolsConfig().kind(kind);
        if (kindConfig == null) {
            return 1;
        }
        return pagesOf(plugin.toolsConfig().abilities(kindConfig).size());
    }

    /** Le calcul des pages, isolé pour que la légende et la navigation disent la même chose. */
    private static int pagesOf(int abilities) {
        return Math.max(1, (abilities + ABILITY_SLOTS - 1) / ABILITY_SLOTS);
    }

    private static void render(View view, Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return;
        }
        ToolsConfig config = plugin.toolsConfig();
        ToolStore store = plugin.store();
        Inventory inventory = view.getInventory();
        ToolKind shown = view.kind();
        ToolsConfig.KindConfig kindConfig = config.kind(shown);
        int tier = kindConfig == null ? 1
                : store.tierOf(player.getUniqueId(), shown, config.maxTier(kindConfig));
        Map<String, Integer> levels = store.levelsOf(player.getUniqueId(), shown);
        List<ToolsConfig.Ability> abilities = kindConfig == null
                ? Collections.<ToolsConfig.Ability>emptyList() : config.abilities(kindConfig);
        int pages = pagesOf(abilities.size());
        if (view.page() >= pages) {
            view.page(0);
        }
        Material tint = pane(tintOf(shown));
        view.clear();

        renderSouls(plugin, config, store, player, shown, inventory, view);
        renderTier(plugin, config, store, player, shown, kindConfig, tier, inventory, view);
        renderMoney(plugin, inventory, view);
        renderClose(inventory, view);

        int first = view.page() * ABILITY_SLOTS;
        for (int index = 0; index < ABILITY_SLOTS; index++) {
            int slot = SLOT_ABILITY_FIRST + index;
            ToolsConfig.Ability ability = first + index < abilities.size()
                    ? abilities.get(first + index) : null;
            ItemStack cell = ability == null
                    ? paneItem(tint, null)
                    : abilityButton(plugin, config, player, shown, ability, view.mode(), tier, levels);
            view.track(cell);
            inventory.setItem(slot, cell);
        }

        renderLegend(tint, inventory, view);
        renderHelp(view, pages, inventory);
        renderSelling(plugin, config, inventory, view);
        renderStats(plugin, config, store, player, shown, kindConfig, tier, inventory, view);
        renderMode(view, inventory);
        renderNavigation(view, pages, tint, inventory);

        for (int slot = 0; slot < SIZE; slot++) {
            if (inventory.getItem(slot) == null) {
                ItemStack filler = paneItem(tint, null);
                view.track(filler);
                inventory.setItem(slot, filler);
            }
        }
    }

    // ------------------------------------------------------------------ les cases fixes

    /** Le vitrage de fond prend la teinte de l'âme affichée : on sait d'un coup d'œil où on est. */
    private static String tintOf(ToolKind kind) {
        if (kind == ToolKind.AXE) {
            return "BROWN_STAINED_GLASS_PANE";
        }
        if (kind == ToolKind.ROD) {
            return "BLUE_STAINED_GLASS_PANE";
        }
        if (kind == ToolKind.SWORD) {
            return "RED_STAINED_GLASS_PANE";
        }
        return "GRAY_STAINED_GLASS_PANE";
    }

    /** Un vitrage teinté, avec repli : sur un serveur ancien le nom peut être inconnu du matériau. */
    private static Material pane(String name) {
        Material found = Material.matchMaterial(name);
        return found == null ? Material.PAPER : found;
    }

    private static ItemStack paneItem(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        if (name == null || name.isEmpty()) {
            return pane;
        }
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MultiTool.color(name));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static void renderSouls(ValoriaTools plugin, ToolsConfig config, ToolStore store, Player player,
            ToolKind shown, Inventory inventory, View view) {
        for (int i = 0; i < ToolKind.values().length; i++) {
            ToolKind kind = ToolKind.values()[i];
            ToolsConfig.KindConfig selected = config.kind(kind);
            int kindTier = selected == null ? 1
                    : store.tierOf(player.getUniqueId(), kind, config.maxTier(selected));
            boolean active = kind == shown;
            ItemStack tab = new ItemStack(selected == null ? kind.fallbackMaterial() : selected.material());
            ItemMeta tabMeta = tab.getItemMeta();
            if (tabMeta != null) {
                tabMeta.setDisplayName(MultiTool.color((active ? "&a▶ " : "&7")
                        + MultiTool.capitalize(kind.label())));
                List<String> lines = new ArrayList<String>();
                lines.add(MultiTool.color("&7Palier &f" + kindTier + "&7/&f"
                        + (selected == null ? 1 : config.maxTier(selected))));
                lines.add(MultiTool.color("&7Niveaux de capacités achetés : &f"
                        + store.totalLevels(player.getUniqueId(), kind)));
                lines.add(MultiTool.color("&7Capacités au barème : &f"
                        + (selected == null ? 0 : config.abilities(selected).size())));
                lines.add(MultiTool.color(active ? "&aÂme affichée juste en dessous."
                        : "&7Clique pour afficher ses capacités."));
                tabMeta.setLore(lines);
                tab.setItemMeta(tabMeta);
            }
            view.track(tab);
            inventory.setItem(SLOT_SOUL_FIRST + i, tab);
        }
    }

    private static void renderTier(ValoriaTools plugin, ToolsConfig config, ToolStore store, Player player,
            ToolKind kind, ToolsConfig.KindConfig kindConfig, int tier, Inventory inventory, View view) {
        int max = kindConfig == null ? 1 : config.maxTier(kindConfig);
        boolean maxed = kindConfig == null || tier >= max;
        double price = kindConfig == null ? -1.0D : config.priceOf(kindConfig, tier + 1);
        ItemStack button = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MultiTool.color((maxed ? "&a" : "&e") + "Palier " + tier + "&7/&f" + max
                    + (maxed ? " &a— maximum" : "&e — améliorer")));
            List<String> lines = new ArrayList<String>();
            lines.add(MultiTool.color("&7Palier actuel : &f" + tier + "&7/&f" + max));
            if (!maxed) {
                lines.add(MultiTool.color("&7Prix : &a" + plugin.economy().format(price)));
                List<String> unlocks = unlocksAt(config, kindConfig, tier + 1);
                lines.add(MultiTool.color("&7Ce que le palier " + (tier + 1) + " débloque :"));
                if (unlocks.isEmpty()) {
                    lines.add(MultiTool.color("&8  (aucune capacité nouvelle)"));
                } else {
                    lines.addAll(unlocks);
                }
                lines.add(MultiTool.color("&7Le palier ne donne pas de niveau de capacité : il les autorise."));
            }
            if (!plugin.economy().available()) {
                lines.add(MultiTool.color("&8Aucune économie : les améliorations sont gratuites."));
            }
            meta.setLore(lines);
            button.setItemMeta(meta);
        }
        view.track(button);
        inventory.setItem(SLOT_TIER, button);
    }

    /** Le solde, en haut : acheter dix niveaux d'un coup se décide avec l'argent sous les yeux. */
    private static void renderMoney(ValoriaTools plugin, Inventory inventory, View view) {
        ItemStack money = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = money.getItemMeta();
        if (meta != null) {
            Player viewer = Bukkit.getPlayer(view.owner());
            if (plugin.economy().available() && viewer != null) {
                meta.setDisplayName(MultiTool.color("&6Solde &f"
                        + plugin.economy().format(plugin.economy().balance(viewer))));
                meta.setLore(Collections.singletonList(MultiTool.color(
                        "&7Le prix d'une case est déjà le total de ce que tu achètes.")));
            } else {
                meta.setDisplayName(MultiTool.color("&6Aucune économie"));
                meta.setLore(Collections.singletonList(MultiTool.color(
                        "&7Améliorations gratuites, vente des drops désactivée.")));
            }
            money.setItemMeta(meta);
        }
        view.track(money);
        inventory.setItem(SLOT_MONEY, money);
    }

    /** La légende : trois vitrages qui expliquent les trois états d'une capacité. */
    private static void renderLegend(Material tint, Inventory inventory, View view) {
        ItemStack bought = legendItem(Material.LIME_STAINED_GLASS_PANE, "&a✔ capacité achetée",
                Arrays.asList("&7Son effet court. Le niveau payé est dans la tooltip.",
                        "&7Un nouveau clic achète le niveau suivant."));
        ItemStack buyable = legendItem(Material.YELLOW_STAINED_GLASS_PANE, "&e● achetable, pas encore payée",
                Arrays.asList("&7Clique pour acheter le premier niveau.",
                        "&8Niveau 0 = capacité non achetée = aucun effet."));
        ItemStack locked = legendItem(pane("GRAY_STAINED_GLASS_PANE"), "&8✖ verrouillée",
                Arrays.asList("&7Le palier d'âme demandé n'est pas atteint :",
                        "&7case &ePalier&7, en haut du panneau."));
        view.track(bought);
        inventory.setItem(SLOT_LEGEND, bought);
        view.track(buyable);
        inventory.setItem(SLOT_LEGEND + 1, buyable);
        view.track(locked);
        inventory.setItem(SLOT_LEGEND + 2, locked);
    }

    /**
     * L'enchantement qui sert de luisant, cherché par <em>clé</em> : les constantes Java de
     * <code>Enchantment</code> ont été débaptisées en 1.20.5 (<code>DURABILITY</code> →
     * <code>UNBREAKING</code>), la clé <code>minecraft:unbreaking</code>, elle, n'a jamais bougé. Un
     * panneau dont la lisibilité tiendrait à un nom d'enum serait cassé par un upgrade du serveur.
     */
    private static Enchantment glintEnchantment() {
        Enchantment found = null;
        try {
            found = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        } catch (RuntimeException | LinkageError unsupported) {
            found = null;
        }
        return found;
    }

    private static ItemStack legendItem(Material material, String name, List<String> details) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            List<String> lines = new ArrayList<String>();
            for (String detail : details) {
                lines.add(MultiTool.color(detail));
            }
            meta.setDisplayName(MultiTool.color(name));
            meta.setLore(lines);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static void renderHelp(View view, int pages, Inventory inventory) {
        ItemStack help = new ItemStack(Material.BOOK);
        ItemMeta meta = help.getItemMeta();
        if (meta != null) {
            List<String> lines = new ArrayList<String>();
            for (String line : guide()) {
                lines.add(MultiTool.color(line));
            }
            if (pages > 1) {
                lines.add(MultiTool.color("&7Page &f" + (view.page() + 1) + "&7/&f" + pages));
            }
            meta.setDisplayName(MultiTool.color("&bComment fonctionne ce panneau"));
            meta.setLore(lines);
            help.setItemMeta(meta);
        }
        view.track(help);
        inventory.setItem(SLOT_HELP, help);
    }

    private static void renderSelling(ValoriaTools plugin, ToolsConfig config, Inventory inventory, View view) {
        ItemStack sell = new ItemStack(Material.HOPPER);
        ItemMeta sellMeta = sell.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName(MultiTool.color("&6Vendre mon inventaire"));
            List<String> lines = new ArrayList<String>();
            lines.add(MultiTool.color("&7Vend tout ce que l'outil reconnaît,"));
            lines.add(MultiTool.color("&7aux prix de " + (config.sellPricesDeclared()
                    ? "&ala grille de l'âme" : "&c(aucun prix déclaré)") + "&7."));
            lines.add(MultiTool.color("&7Le multi-outil en main n'est jamais vendu."));
            sellMeta.setLore(lines);
            sell.setItemMeta(sellMeta);
        }
        view.track(sell);
        inventory.setItem(SLOT_SELL, sell);
    }

    private static void renderStats(ValoriaTools plugin, ToolsConfig config, ToolStore store, Player player,
            ToolKind kind, ToolsConfig.KindConfig kindConfig, int tier, Inventory inventory, View view) {
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(MultiTool.color("&b" + MultiTool.capitalize(kind.label())
                    + " — statistiques"));
            List<String> lines = new ArrayList<String>();
            lines.add(MultiTool.color("&7Palier &f" + tier + "&7/&f" + (kindConfig == null ? 1
                    : config.maxTier(kindConfig))));
            int bought = 0;
            if (kindConfig != null) {
                for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
                    bought += ToolsConfig.levelOf(ability, store.levelsOf(player.getUniqueId(), kind), tier);
                }
            }
            lines.add(MultiTool.color("&7Niveaux de capacités achetés : &f" + bought));
            ToolStats stats = plugin.stats();
            if (stats.enabled()) {
                lines.add(MultiTool.color("&7Ce que cette âme t'a rapporté :"));
                boolean any = false;
                for (ToolStats.Metric metric : ToolStats.Metric.values()) {
                    long value = stats.total(player.getUniqueId(), kind, metric);
                    if (value <= 0L) {
                        continue;
                    }
                    any = true;
                    lines.add(MultiTool.color("&8 " + metric.label() + " : &f" + shorten(value)));
                }
                if (!any) {
                    lines.add(MultiTool.color("&8  (rien de mesuré pour l'instant)"));
                }
            }
            lines.add(MultiTool.color("&8Clic dans le chat : détail capacité par capacité."));
            infoMeta.setLore(lines);
            info.setItemMeta(infoMeta);
        }
        view.track(info);
        inventory.setItem(SLOT_STATS, info);
    }

    /** Le sélecteur de mode : un clic = ×1, ×10 ou ×100 niveaux payés, au prix cumulé. */
    private static void renderMode(View view, Inventory inventory) {
        int mode = view.mode();
        ItemStack button = new ItemStack(mode >= 100 ? Material.ORANGE_DYE
                : mode >= 10 ? Material.YELLOW_DYE : Material.LIME_DYE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MultiTool.color("&7Mode d'achat : &f×" + mode));
            List<String> lines = new ArrayList<String>();
            lines.add(MultiTool.color("&7Un clic sur une capacité achète &f" + mode + " niveau(x)&7."));
            lines.add(MultiTool.color("&7Maj + clic achète &f×" + (mode * 10) + "&7, jusqu'au plafond."));
            lines.add(MultiTool.color("&8Le prix reste celui de chaque niveau : rien n'est offert"));
            lines.add(MultiTool.color("&8en achetant en gros."));
            lines.add(MultiTool.color("&eClique pour changer de mode."));
            meta.setLore(lines);
            button.setItemMeta(meta);
        }
        view.track(button);
        inventory.setItem(SLOT_MODE, button);
    }

    private static void renderNavigation(View view, int pages, Material tint, Inventory inventory) {
        boolean paginated = pages > 1;
        ItemStack previous = paginated
                ? arrow("&7◀ Page précédente &8(" + (view.page() + 1) + "/" + pages + ")")
                : paneItem(tint, null);
        ItemStack next = paginated
                ? arrow("&7Page suivante ▶ &8(" + (view.page() + 1) + "/" + pages + ")")
                : paneItem(tint, null);
        view.track(previous);
        inventory.setItem(SLOT_PREV, previous);
        view.track(next);
        inventory.setItem(SLOT_NEXT, next);
    }

    private static ItemStack arrow(String name) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MultiTool.color(name));
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    private static void renderClose(Inventory inventory, View view) {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(MultiTool.color("&cFermer"));
            closeMeta.setLore(Collections.singletonList(MultiTool.color(
                    "&7ESC fait la même chose ; tes paliers sont déjà sauvés.")));
            close.setItemMeta(closeMeta);
        }
        view.track(close);
        inventory.setItem(SLOT_CLOSE, close);
    }

    // ------------------------------------------------------------------ les cases de capacites

    /** La case d'une capacité : nom du wiki, description du wiki, niveau, effet courant, prix. */
    private static ItemStack abilityButton(ValoriaTools plugin, ToolsConfig config, Player player,
            ToolKind kind, ToolsConfig.Ability ability, int mode, int tier, Map<String, Integer> levels) {
        Material icon = ICONS.get(ability.type());
        ItemStack button = new ItemStack(icon == null ? Material.BOOK : icon);
        ItemMeta meta = button.getItemMeta();
        if (meta == null) {
            return button;
        }
        int level = ToolsConfig.levelOf(ability, levels, tier);
        int max = ability.maxLevel();
        boolean locked = tier < ability.unlock();
        boolean maxed = level >= max;
        String sign = locked ? "&8✖ " : maxed ? "&a★ " : level > 0 ? "&a✔ " : "&e● ";
        meta.setDisplayName(MultiTool.color(sign + ability.name() + (maxed ? " &a(max)" : "")));
        List<String> lines = new ArrayList<String>();
        for (String part : wrap(ability.description(), 38)) {
            lines.add(MultiTool.color("&7" + part));
        }
        lines.add(MultiTool.color("&8" + ability.type() + " &7— niveau &f" + level + "&7/&f" + max));
        if (level > 0) {
            lines.add(MultiTool.color("&7Effet actuel :") + currentValues(ability, level));
        }
        if (!maxed) {
            lines.add(MultiTool.color("&7Effet au niveau " + (level + 1) + " :")
                    + currentValues(ability, level + 1));
        }
        if (locked) {
            lines.add(MultiTool.color("&cVerrouillé : palier d'âme &f" + ability.unlock() + "&c requis &7→"
                    + " case &ePalier&7, en haut."));
        } else if (maxed) {
            lines.add(MultiTool.color("&aCapacité au maximum du barème (" + max + ")."));
        } else {
            int wanted = Math.max(1, Math.min(max - level, Math.max(1, mode)));
            double total = totalFor(ability, level + 1, wanted);
            lines.add(MultiTool.color("&7Acheter &f+" + wanted + "&7 pour &a" + plugin.economy().format(total)
                    + (wanted == 1 ? "" : " &8(" + plugin.economy().format(total / wanted) + "&8/niveau)")));
        }
        if (ability.free()) {
            lines.add(MultiTool.color("&8Niveau 1 offert dès le palier " + ability.unlock() + "."));
        }
        if (!plugin.economy().available()) {
            lines.add(MultiTool.color("&8Aucune économie : les capacités sont gratuites."));
        }
        meta.setLore(lines);
        // L'unique façon de peindre un « acheté » sur le client sans pack de textures est le luisant
        // d'enchantement : la couleur d'un item vanilla quelconque, elle, ne se pilote que par le modèle
        // 3D. Le compteur est masqué (HIDE_ENCHANTS) pour qu'il reste le reflet, pas une ligne « Solidité
        // I » qui ferait croire que l'outil s'use moins vite.
        if (level > 0) {
            Enchantment glint = glintEnchantment();
            if (glint != null) {
                try {
                    meta.addEnchant(glint, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } catch (RuntimeException | LinkageError unsupported) {
                    // Un serveur qui refuserait l'enchantement factice perd le reflet, pas l'information :
                    // le signe du nom et la tooltip disent déjà le niveau payé.
                }
            }
        }
        // Pas de CustomModelData : c'est le crochet d'un pack de textures, et ce panneau doit se lire
        // avec les seuls items vanilla. L'état passe par le signe du nom, le luisant et la tooltip.
        button.setItemMeta(meta);
        return button;
    }

    /** Le prix cumulé de {@code count} niveaux à partir de {@code from} : la grille, cran par cran. */
    private static double totalFor(ToolsConfig.Ability ability, int from, int count) {
        double total = 0.0D;
        for (int level = from; level < from + Math.max(1, count); level++) {
            total += Math.max(0.0D, ability.priceAt(level));
        }
        return ToolListener.round(total);
    }

    /** Les valeurs de la capacité, celles de la config : rien d'inventé, rien de masqué. */
    private static String currentValues(ToolsConfig.Ability ability, int level) {
        StringBuilder out = new StringBuilder();
        for (String key : ability.keys()) {
            if (key.indexOf('.') >= 0 || ability.numbers(key).isEmpty()) {
                continue;
            }
            double value = ability.levelDecimal(key, level, 0.0D);
            if (!Double.isFinite(value) || value == 0.0D) {
                continue;
            }
            if (out.length() > 0) {
                out.append(" §7/ ");
            }
            out.append("§7").append(prettyKey(key)).append(" §f").append(readable(key, value));
            if (out.length() > 150) {
                break;      // une tooltip qui depasse l'ecran n'est plus une information
            }
        }
        return out.length() == 0 ? " §8(passive)" : " §f" + out;
    }

    private static String prettyKey(String key) {
        return key.replace('-', ' ');
    }

    /** Un pourcentage pour ce qui en est un, un nombre simple pour le reste. */
    private static String readable(String key, double value) {
        boolean percent = key.contains("percent") || key.contains("chance");
        if (percent) {
            return Math.round(value * 100.0D) + "%";
        }
        if (Math.abs(value - Math.rint(value)) < 0.001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", Double.valueOf(value));
    }

    /** 12 480 → `12.5k` : une tooltip qui affiche un nombre de neuf chiffres ne se lit pas. */
    private static String shorten(long value) {
        if (value < 1000L) {
            return String.valueOf(value);
        }
        if (value < 1000000L) {
            return trim(value / 100.0D) + "k";
        }
        return trim(value / 100000.0D) + "M";
    }

    private static double trim(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<String>();
        if (text == null || text.trim().isEmpty()) {
            return out;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }

    /** Les capacités que le palier demandé fait apparaître. */
    private static List<String> unlocksAt(ToolsConfig config, ToolsConfig.KindConfig kindConfig, int tier) {
        List<String> out = new ArrayList<String>();
        for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
            if (ability.unlock() == tier) {
                out.add(MultiTool.color("&a+ " + ability.name()));
            }
        }
        return out;
    }

    /** Le mode d'emploi : une fois dans la tooltip de l'aide, une fois dans le chat (clic sur l'aide). */
    private static List<String> guide() {
        List<String> out = new ArrayList<String>();
        out.add("&7• Une case = une capacité du barème.");
        out.add("&7• Le panneau s'ouvre &foutil en main, accroupi + clic droit&7.");
        out.add("&7• Clic = acheter des niveaux ; Maj + clic = ×10 de plus.");
        out.add("&7• Le mode du bas (×1, ×10, ×100) fixe ce que vaut un clic.");
        out.add("&7• Niveau 0 = capacité non achetée = aucun effet.");
        out.add("&7• Le palier d'âme (case du haut) &fautorise&7 les capacités, il ne les paie pas.");
        out.add("&7• La vitesse de minage s'applique tant que l'outil est en main.");
        out.add("&7• L'outil ne se lâche pas : un seul exemplaire par joueur.");
        return out;
    }

    private static void guide(Player player, ToolKind kind) {
        player.sendMessage(MultiTool.color("&8[&a" + MultiTool.capitalize(kind.label())
                + "&8] &7panneau d'amélioration"));
        for (String line : guide()) {
            player.sendMessage(MultiTool.color(line));
        }
    }

    // ------------------------------------------------------------------ achats

    /**
     * Achète {@code wanted} niveaux d'une capacité. Les niveaux sont comptés <b>un par un</b> dans le
     * prix total : c'est la grille du wiki (le prix dépend du niveau visé), et un achat groupé à prix
     * unique serait une faille d'économie gros comme une île.
     */
    private static void buyLevel(Player player, ToolKind kind, ToolsConfig.Ability ability, int wanted) {
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
        if (tier < ability.unlock()) {
            player.sendMessage(MultiTool.color("&c" + ability.name() + " demande le palier d'âme &f"
                    + ability.unlock() + "&c : améliore l'âme d'abord (case du haut)."));
            return;
        }
        if (!ToolListener.holding(player)) {
            player.sendMessage(MultiTool.color("&cTiens le multi-outil en main pour le modifier."));
            return;
        }
        int start = ToolsConfig.levelOf(ability, plugin.store().levelsOf(player.getUniqueId(), kind), tier);
        int ceiling = ability.maxLevel();
        double total = 0.0D;
        int bought = 0;
        for (int level = start + 1; level <= ceiling && bought < Math.max(1, wanted); level++) {
            double price = ability.priceAt(level);
            if (price < 0.0D) {
                break;
            }
            total += price;
            bought++;
        }
        if (bought <= 0) {
            player.sendMessage(MultiTool.color("&e" + ability.name() + " est déjà à son niveau maximum ("
                    + ceiling + ")."));
            return;
        }
        double cost = ToolListener.round(total);
        if (plugin.economy().available() && cost > 0.0D && !plugin.economy().canAfford(player, cost)) {
            player.sendMessage(MultiTool.color("&cPas assez d'argent : il manque &f"
                    + plugin.economy().format(cost - plugin.economy().balance(player)) + "&c."));
            return;
        }
        EconomyService.Outcome taken = plugin.economy().withdraw(player, cost);
        if (!taken.success()) {
            player.sendMessage(MultiTool.color("&cPaiement refusé : &f" + taken.reason()));
            return;
        }
        plugin.store().setLevel(player, kind, ability.id(), start + bought, ceiling);
        plugin.stats().gesture(player, kind, ToolStats.Metric.LEVELS, bought);
        MultiTool.refresh(plugin.guard() == null ? null : plugin.guard().first(player), config,
                plugin.store(), player.getUniqueId());
        plugin.refreshPassive(player);   // un niveau d'Efficacité doit se sentir au bloc suivant
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.6F);
        } catch (RuntimeException | LinkageError legacy) {
            // son decoratif
        }
        player.sendMessage(MultiTool.color("&a" + ability.name() + " &7niveau &f" + (start + bought)
                + "&a/" + ceiling + " &8(-" + plugin.economy().format(cost) + "&8)"));
    }

    /** Achat du palier d'âme : ce qui autorise les capacités verrouillées par le wiki. */
    private static void upgradeTier(Player player, ToolKind kind) {
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
            player.sendMessage(MultiTool.color("&cPaiement refusé : &f" + taken.reason()));
            return;
        }
        plugin.store().setTier(player, kind, tier + 1, max);
        MultiTool.refresh(plugin.guard() == null ? null : plugin.guard().first(player), config,
                plugin.store(), player.getUniqueId());
        plugin.refreshPassive(player);
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.4F);
        } catch (RuntimeException | LinkageError legacy) {
            // son decoratif
        }
        player.sendMessage(MultiTool.color("&a" + MultiTool.capitalize(kind.label()) + " → palier &f"
                + (tier + 1) + "&a (&7-" + plugin.economy().format(price) + "&a)"));
    }

    /** Le détail dans le chat, capacité par capacité : la tooltip dit l'essentiel, ça dit tout. */
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
        Map<String, Integer> levels = plugin.store().levelsOf(player.getUniqueId(), kind);
        player.sendMessage(MultiTool.color("&8[&a" + MultiTool.capitalize(kind.label()) + "&8] &7palier &f"
                + tier + "&7/&f" + config.maxTier(kindConfig)));
        for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
            int level = ToolsConfig.levelOf(ability, levels, tier);
            String mark = tier < ability.unlock() ? "&8✗" : level > 0 ? "&a✓" : "&7·";
            player.sendMessage(MultiTool.color("  " + mark + " " + ability.name() + " §8niv. §f" + level
                    + "&8/§f" + ability.maxLevel() + (tier < ability.unlock()
                            ? " §8(palier " + ability.unlock() + ")" : "")));
        }
        ToolStats stats = plugin.stats();
        if (stats.enabled()) {
            for (ToolStats.Metric metric : ToolStats.Metric.values()) {
                long value = stats.total(player.getUniqueId(), kind, metric);
                if (value > 0L) {
                    player.sendMessage(MultiTool.color("&8  " + metric.label() + " : &f" + shorten(value)));
                }
            }
        }
    }

    /**
     * La case « vendre » : elle appelle <code>/tools sell all</code> par l'objet de commande déjà
     * enregistré, au lieu de rejouer la grille de prix ici. Deux implémentations de la même vente est la
     * façon la plus sûre d'avoir un menu qui annonce un montant et une commande qui en paie un autre —
     * et le remboursement quand Vault refuse le dépôt n'existe que d'un côté.
     */
    private static void sellAll(Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null || plugin.command() == null) {
            player.sendMessage(MultiTool.color("&cLa vente n'est pas disponible (commande non enregistrée)."));
            return;
        }
        plugin.command().sellFromGui(player);
    }

    /**
     * Oublie la vue d'un joueur : la table est statique, et une vue retenue pour un déconnecté est une vue
     * qui ne sera plus jamais redessinée ni libérée.
     */
    public static void forget(UUID owner) {
        VIEWS.remove(owner);
    }

    /**
     * Une copie des vues ouvertes (le reload itère dessus tout en en retirant : une vue live n'est pas une
     * vue à parcourir pendant qu'on la mute).
     */
    public static java.util.Collection<View> views() {
        return new java.util.ArrayList<View>(VIEWS.values());
    }
}
