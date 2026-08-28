package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
 * Le menu d'amélioration : les quatre âmes en haut, les capacités du wiki en dessous, une case par
 * capacité — donc une seule façon de cliquer.
 *
 * <h2>Pourquoi une case par capacité et pas un catalogue paginé</h2>
 * <p>Le barème de GenTycoon compte jusqu'à 22 améliorations pour la pioche : elles tiennent toutes
 * dans les trois rangées centrales, sans page. Un bouton = une intention (acheter un niveau), et le
 * niveau maximal du wiki est écrit dans la tooltip du bouton : le joueur n'a jamais à deviner où s'arrête
 * la capacité.</p>
 *
 * <h2>Les vues sont suivies, pas les joueurs</h2>
 * <p>Le holder porte l'UUID du joueur et l'âme affichée : après un <code>/tools reload</code>, on peut
 * redessiner toutes les vues ouvertes sans qu'aucune ne pointe sur une configuration périmée. La vue est
 * retirée de la liste à la fermeture, y compris par ESC — sinon une vue fantôme réapparaîtrait au joueur
 * qui rouvre son inventaire.</p>
 */
public final class ToolsGui {

    /** L'inventière d'une vue ouverte : c'est ce qui permet de la retrouver et de la redessiner. */
    public static final class View implements InventoryHolder {

        private final UUID owner;
        private final Inventory inventory;
        private final List<ItemStack> slots = new ArrayList<ItemStack>();
        private ToolKind kind;

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

        void kind(ToolKind kind) {
            this.kind = kind;
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
    /** Rangée du haut : quatre âmes, l'achat de palier, la vente, les stats, la fermeture. */
    static final int SLOT_SOUL_FIRST = 0;
    static final int SLOT_TIER = 5;
    static final int SLOT_SELL_ALL = 6;
    static final int SLOT_STATS = 7;
    static final int SLOT_CLOSE = 8;
    /** Les dix-huit premières cases utiles de la rangée centrale sont les capacités ; au-delà, du fond. */
    static final int SLOT_ABILITY_FIRST = 9;
    static final int ABILITY_SLOTS = 27;

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
            // Les clics sont reportés d'un tick : muter l'inventaire ou le solde pendant la désignation
            // de l'événement est le chemin le plus court vers un desync.
            Bukkit.getScheduler().runTask(ValoriaTools.get(), new Runnable() {

                @Override
                public void run() {
                    handle(player, view, slot, bulk, shown);
                    render(player);
                }
            });
        }

        private void handle(Player player, View view, int slot, boolean bulk, ToolKind shown) {
            ValoriaTools plugin = ValoriaTools.get();
            if (plugin == null) {
                return;
            }
            if (slot == SLOT_CLOSE) {
                player.closeInventory();
                return;
            }
            if (slot == SLOT_SELL_ALL) {
                sellAll(player);
                return;
            }
            if (slot >= SLOT_SOUL_FIRST && slot < SLOT_SOUL_FIRST + ToolKind.values().length) {
                ToolKind picked = ToolKind.values()[slot - SLOT_SOUL_FIRST];
                view.kind(picked);
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
            int index = slot - SLOT_ABILITY_FIRST;
            if (index < 0 || index >= ABILITY_SLOTS) {
                return;
            }
            ToolsConfig.KindConfig kindConfig = plugin.toolsConfig().kind(shown);
            if (kindConfig == null) {
                player.sendMessage(MultiTool.color("&cAucune configuration pour cette âme d'outil."));
                return;
            }
            List<ToolsConfig.Ability> abilities = plugin.toolsConfig().abilities(kindConfig);
            if (index >= abilities.size()) {
                return;
            }
            buyLevel(player, shown, abilities.get(index), bulk ? 10 : 1);
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (event.getInventory().getHolder() instanceof View && event.getPlayer() instanceof Player) {
                VIEWS.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    /** L'âme affichée par défaut : celle de l'item tenu en main, pioche si le joueur ne vise rien. */
    private static ToolKind currentKind(Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return ToolKind.PICKAXE;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
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
            view = new View(player.getUniqueId(), MultiTool.color("&8Multi-outil §7— capacités"), SIZE,
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

    private static void render(View view, Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return;
        }
        ToolsConfig config = plugin.toolsConfig();
        ToolStore store = plugin.store();
        view.clear();
        Inventory inventory = view.getInventory();
        ToolKind shown = view.kind();
        ToolsConfig.KindConfig kindConfig = config.kind(shown);
        int tier = kindConfig == null ? 1 : store.tierOf(player.getUniqueId(), shown, config.maxTier(kindConfig));
        Map<String, Integer> levels = store.levelsOf(player.getUniqueId(), shown);

        for (int i = 0; i < ToolKind.values().length; i++) {
            ToolKind kind = ToolKind.values()[i];
            ToolsConfig.KindConfig selected = config.kind(kind);
            int kindTier = selected == null ? 1
                    : store.tierOf(player.getUniqueId(), kind, config.maxTier(selected));
            boolean active = kind == shown;
            ItemStack tab = new ItemStack(selected == null ? kind.fallbackMaterial() : selected.material());
            ItemMeta tabMeta = tab.getItemMeta();
            if (tabMeta != null) {
                tabMeta.setDisplayName(MultiTool.color((active ? "&a▶ " : "&7") + MultiTool.capitalize(kind.label())));
                List<String> lines = new ArrayList<String>();
                lines.add(MultiTool.color("&7Palier &f" + kindTier + "&7/&f"
                        + (selected == null ? 1 : config.maxTier(selected))));
                lines.add(MultiTool.color("&7Niveaux de capacités achetés : &f"
                        + store.totalLevels(player.getUniqueId(), kind)));
                lines.add(MultiTool.color(active ? "&aÂme affichée juste en dessous."
                        : "&7Clique pour afficher ses capacités."));
                tabMeta.setLore(lines);
                tab.setItemMeta(tabMeta);
            }
            view.track(tab);
            inventory.setItem(SLOT_SOUL_FIRST + i, tab);
        }

        renderTier(plugin, config, store, player, shown, kindConfig, tier, inventory, view);
        renderSell(plugin, config, inventory, view);
        renderStats(plugin, config, store, player, shown, kindConfig, tier, inventory, view);
        renderClose(inventory, view);

        if (kindConfig != null) {
            int index = 0;
            for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
                if (index >= ABILITY_SLOTS) {
                    break;
                }
                ItemStack button = abilityButton(plugin, config, player, shown, ability, tier, levels);
                view.track(button);
                inventory.setItem(SLOT_ABILITY_FIRST + index, button);
                index++;
            }
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int slot = 0; slot < SIZE; slot++) {
            if (inventory.getItem(slot) == null) {
                view.track(filler);
                inventory.setItem(slot, filler);
            }
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
            meta.setDisplayName(MultiTool.color((maxed ? "&a" : "&e") + "Palier de l'âme — "
                    + (maxed ? "maximum atteint" : "améliorer")));
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

    private static void renderSell(ValoriaTools plugin, ToolsConfig config, Inventory inventory, View view) {
        ItemStack sell = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta sellMeta = sell.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName(MultiTool.color("&6Vendre mon inventaire"));
            List<String> lines = new ArrayList<String>();
            lines.add(MultiTool.color("&7Vend tout ce que l'outil reconnaît,"));
            lines.add(MultiTool.color("&7aux prix de " + (config.sellPricesDeclared() ? "&bla grille de l'âme"
                    : "&c(aucun prix déclaré)") + "&7."));
            lines.add(MultiTool.color("&7Le multi-outil en main n'est jamais vendu."));
            sellMeta.setLore(lines);
            sell.setItemMeta(sellMeta);
        }
        view.track(sell);
        inventory.setItem(SLOT_SELL_ALL, sell);
    }

    private static void renderStats(ValoriaTools plugin, ToolsConfig config, ToolStore store, Player player,
            ToolKind kind, ToolsConfig.KindConfig kindConfig, int tier, Inventory inventory, View view) {
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(MultiTool.color("&b" + MultiTool.capitalize(kind.label()) + " — statistiques"));
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
            lines.add(MultiTool.color("&8Clic = +1 niveau · Maj+Clic = +10 niveaux"));
            lines.add(MultiTool.color("&8Le pourcentage affiché inclut déjà le Proc booster."));
            infoMeta.setLore(lines);
            info.setItemMeta(infoMeta);
        }
        view.track(info);
        inventory.setItem(SLOT_STATS, info);
    }

    private static void renderClose(Inventory inventory, View view) {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(MultiTool.color("&cFermer"));
            close.setItemMeta(closeMeta);
        }
        view.track(close);
        inventory.setItem(SLOT_CLOSE, close);
    }

    /** La case d'une capacité : nom du wiki, description du wiki, niveau, effet courant, prix. */
    private static ItemStack abilityButton(ValoriaTools plugin, ToolsConfig config, Player player,
            ToolKind kind, ToolsConfig.Ability ability, int tier, Map<String, Integer> levels) {
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
        StringBuilder name = new StringBuilder();
        name.append(locked ? "&8" : maxed ? "&a" : level > 0 ? "&e" : "&7").append(ability.name());
        meta.setDisplayName(MultiTool.color(name.toString()));
        List<String> lines = new ArrayList<String>();
        for (String part : wrap(ability.description(), 38)) {
            lines.add(MultiTool.color("&7" + part));
        }
        lines.add(MultiTool.color("&8" + ability.type() + " §8— niveau &f" + level + "&7/&f" + max));
        if (level > 0) {
            lines.add(MultiTool.color("&7Effet actuel :") + currentValues(ability, level));
        }
        if (!maxed) {
            lines.add(MultiTool.color("&7Effet au niveau " + (level + 1) + " :") + currentValues(ability, level + 1));
        }
        if (locked) {
            lines.add(MultiTool.color("&cVerrouillé : palier d'âme &f" + ability.unlock() + "&c requis."));
        } else if (maxed) {
            lines.add(MultiTool.color("&aCapacité au maximum du barème."));
        } else {
            lines.add(MultiTool.color("&7Prix du niveau " + (level + 1) + " : &a"
                    + plugin.economy().format(ability.priceAt(level + 1))));
        }
        if (!plugin.economy().available()) {
            lines.add(MultiTool.color("&8Aucune économie : les capacités sont gratuites."));
        }
        meta.setLore(lines);
        if (level > 0 && !locked && level >= max) {
            meta.setCustomModelData(Integer.valueOf(1));   // signe discret : la capacite est au max
        }
        button.setItemMeta(meta);
        return button;
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
        MultiTool.refresh(player.getInventory().getItemInMainHand(), config, plugin.store(), player.getUniqueId());
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.6F);
        } catch (RuntimeException | LinkageError legacy) {
            // son decoratif
        }
        player.sendMessage(MultiTool.color("&a" + ability.name() + " &7niveau &f" + (start + bought) + "&a/"
                + ceiling + " &8(-" + plugin.economy().format(cost) + "&8)"));
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
        MultiTool.refresh(player.getInventory().getItemInMainHand(), config, plugin.store(), player.getUniqueId());
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.4F);
        } catch (RuntimeException | LinkageError legacy) {
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
    }

    private static void sellAll(Player player) {
        ValoriaTools plugin = ValoriaTools.get();
        if (plugin == null) {
            return;
        }
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

    /** Toutes les vues ouvertes : le plugin les redessine après un reload. */
    public static java.util.Collection<View> views() {
        return new java.util.ArrayList<View>(VIEWS.values());
    }
}
