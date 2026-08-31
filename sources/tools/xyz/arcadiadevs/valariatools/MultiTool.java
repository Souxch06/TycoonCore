package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * L'item lui-même : fabrication, reconnaissance, étiquettes.
 *
 * <h2>Reconnaissance par marque, pas par matériau</h2>
 * <p>Un <code>NETHERITE_PICKAXE</code> ordinaire traîne partout dans un inventaire. Le plugin marque
 * donc son item d'une clé <code>valariatools:multi</code> en PersistentDataContainer — la même voie que
 * celle qui a remplacé l'ancienne lecture NBT côté générateurs, donc fiable sur les serveurs récents.
 * La clé est écrite en <b>dur</b> (chaîne <code>"1"</code>) et ne porte <em>aucun</em> palier : le
 * palier vit dans <code>tools.yml</code> par joueur (voir {@link ToolStore}), pour qu'aucun item ne
 * puisse être fabriqué, échangé ou renommé avec des capacités qu'on n'a pas payées.</p>
 *
 * <h2>Deux marques, ni palier ni niveau dedans</h2>
 * <p>La clé <code>valariatools:multi</code> dit « c'est LE multi-outil », et
 * <code>valariatools:soul</code> dit « il affiche telle âme » — donc quel matériau il prend et quelles
 * capacités il liste dans sa lore. Ces deux marques sont cosmétiques : tout ce qui compte (paliers,
 * niveaux) vit dans <code>tools.yml</code>. Un item dupliqué par un autre plugin montre donc les
 * capacités de son joueur et n'en active aucune de plus.</p>
 *
 * <h2>Robustesse</h2>
 * <p>Chaque opération tolère l'absence de PersistentDataContainer (le plugin vise 1.7 en référence) :
 * dans ce cas l'item n'est pas marqué, le plugin fonctionne mais tout le monde reste au palier 1, et
 * le log le dit une fois — plutôt qu'une {@code NoClassDefFoundError} au premier clic.</p>
 */
public final class MultiTool {

    private static NamespacedKey markKey;
    private static NamespacedKey soulKey;
    private static boolean pdcUnavailableLogged;

    private MultiTool() {
    }

    /** Fabrique l'item, avec son nom, son âme affichée et son absence de casse. */
    public static ItemStack create(ToolsConfig config, ToolStore store, UUID owner) {
        ItemStack stack = new ItemStack(config.itemMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        ToolKind soul = config.fallbackKind();
        meta.setDisplayName(color(displayNameFor(config, soul)));
        meta.setLore(loreFor(config, store, owner, soul));
        mark(meta);
        writeSoul(meta, soul);
        // Aucun enchantement n'est pose a la fabrication : les capacites viennent des paliers et des
        // niveaux achetes, pas de l'item, sinon le joueur verrait ses bonus disparaitre des qu'il le
        // poserait dans un coffre. Seule la capacite RANDOM_ENCHANT (« Charognard ») en ajoute un, et
        // c'est un bonus reellement lie a cet item-la — d'ou `tool.hide-flags` pour ne pas l'afficher.
        try {
            meta.setUnbreakable(config.unbreakable());
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError legacy) {
            // ItemMeta#setUnbreakable est arrive en 1.11 ; sur un serveur plus ancien, l'item
            // s'use normalement, ce qui n'empeche aucun mecanique du plugin.
        }
        try {
            if (config.hideFlags()) {
                java.lang.reflect.Method setter = meta.getClass().getMethod("setItemFlags",
                        java.util.Collection.class);
                setter.invoke(meta, java.util.EnumSet.allOf(org.bukkit.inventory.ItemFlag.class));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            // les flags d'affichage ne sont que du confort
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** Vrai si cet item est bien le multi-outil de ce serveur. */
    public static boolean isMultiTool(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        NamespacedKey key = key();
        if (key == null) {
            // sans PDC, on se fie au nom : imparfait (un joueur peut renommer), mais mieux que rien
            String name = stack.getItemMeta().getDisplayName();
            return name != null && name.contains("Multi-outil");
        }
        try {
            return "1".equals(stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unavailable) {
            return false;
        }
    }

    /**
     * Rafraîchit nom + lore d'un item, <b>sans toucher à son matériau</b> : c'est ce qu'on appelle après
     * un achat de palier, un reload, une commande. Un outil rendu par {@code /tools give} garde donc
     * l'apparence choisie par l'admin ({@code tool.material}) jusqu'à ce que le joueur interagisse —
     * changer l'objet « pour rien » pendant qu'on ne fait que relire la config serait un changement visuel
     * que personne n'a demandé.
     *
     * <p>Attention au piège classique : {@code getInventory().getItem(...)} rend une <b>copie</b>. Écrire
     * dans cette copie ne met à jour que la copie — le joueur voyait donc sa lore inchangée après un
     * <code>/tools max</code>, jusqu'au prochain bloc cassé. C'est pour ça que ce fichier expose aussi
     * {@link #refreshHeld(Player, ToolsConfig, ToolStore)}, qui écrit dans la main.</p>
     */
    public static void refresh(ItemStack stack, ToolsConfig config, ToolStore store, UUID owner) {
        write(stack, soulOf(stack, config.fallbackKind()), config, store, owner, false);
    }

    /**
     * Met à jour l'item tenu par ce joueur (nom + lore de l'âme qu'il affiche) et <b>l'écrit dans la
     * main</b>. C'est la méthode à vouloir dire quand on sort d'une commande ou du panneau : les quatre
     * appelants précédents écrivaient dans une copie.
     */
    public static void refreshHeld(Player player, ToolsConfig config, ToolStore store) {
        if (player == null) {
            return;
        }
        ItemStack tool = held(player);
        if (tool == null) {
            return;
        }
        if (write(tool, soulOf(tool, config.fallbackKind()), config, store, player.getUniqueId(), false)) {
            writeHeld(player, tool);
        }
    }

    /**
     * Matérialise une âme sur l'item : <b>son matériau</b> (si <code>tool.morph-by-target</code> est vrai),
     * son nom, et la lore des capacités payées de <b>cette</b> âme. Renvoie {@code true} quand l'item a
     * vraiment changé — le lecteur d'événements s'en sert pour ne pas réécrire la main du joueur toutes
     * les secondes, ce qui ferait clignoter la tooltip et rejouerait l'animation de bras.
     */
    public static boolean applySoul(ItemStack stack, ToolKind soul, ToolsConfig config, ToolStore store,
            UUID owner) {
        return write(stack, soul, config, store, owner, true);
    }

    /**
     * @param morph
     *            {@code true} : l'âme visée devient aussi l'apparence de l'item (matériau configuré de
     *            l'âme) ; {@code false} : seul le texte est recalculé, l'objet garde le matériau qu'il a.
     */
    private static boolean write(ItemStack stack, ToolKind soul, ToolsConfig config, ToolStore store,
            UUID owner, boolean morph) {
        if (stack == null || soul == null || !stack.hasItemMeta() || !isMultiTool(stack)) {
            return false;   // un objet qui n'est pas LE multi-outil ne devient pas l'outil de quelqu'un
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        Material wanted = morph ? soulMaterial(config, soul) : null;
        boolean typeChanged = wanted != null && stack.getType() != wanted;
        boolean soulChanged = soul != soulOf(stack, null);
        String name = color(displayNameFor(config, soul));
        List<String> lore = loreFor(config, store, owner, soul);
        boolean textChanged = !name.equals(meta.getDisplayName()) || !lore.equals(meta.getLore());
        if (!typeChanged && !soulChanged && !textChanged) {
            return false;
        }
        if (typeChanged) {
            // Le type se change avant la meta : `setType` reconstruit l'item côté serveur, et poser la
            // meta après garantit que nom, lore, marque PDC et drapeaux d'affichage survivent au voyage.
            try {
                stack.setType(wanted);
            } catch (RuntimeException | LinkageError refused) {
                return false;   // materiau refuse par le serveur : on laisse l'item tel quel
            }
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        mark(meta);
        writeSoul(meta, soul);
        stack.setItemMeta(meta);
        return true;
    }

    /** Le matériau qui représente cette âme : celui de sa config, sinon celui de l'enum. */
    public static Material soulMaterial(ToolsConfig config, ToolKind soul) {
        if (!config.morphByTarget()) {
            return null;      // pas de morphing demande : on garde `tool.material`, l'apparence choisie
        }
        ToolsConfig.KindConfig kindConfig = config.kind(soul);
        Material material = kindConfig == null ? null : kindConfig.material();
        if (material != null && material != Material.AIR) {
            return material;
        }
        return soul.fallbackMaterial();
    }

    /** L'âme que cet item affiche ; {@code defaut} pour un item fabriqué avant cette version. */
    public static ToolKind soulOf(ItemStack stack, ToolKind defaut) {
        if (stack == null || !stack.hasItemMeta()) {
            return defaut;
        }
        NamespacedKey key = soulKey();
        if (key == null) {
            return defaut;      // sans PDC, on ne peut pas le savoir : le rechargement a la main suffit
        }
        try {
            String stored = stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (stored == null) {
                return defaut;
            }
            ToolKind parsed = ToolKind.parse(stored);
            return parsed == null ? defaut : parsed;
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unavailable) {
            return defaut;
        }
    }

    /** L'item tenu qui est le multi-outil : main principale d'abord, secondaire ensuite, sinon {@code null}. */
    public static ItemStack held(Player player) {
        if (player == null) {
            return null;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack main = null;
        try {
            main = inventory.getItemInMainHand();
        } catch (RuntimeException | LinkageError legacy) {
            ItemStack[] contents = inventory.getStorageContents();
            if (contents.length > 0) {
                main = contents[0];
            }
        }
        if (isMultiTool(main)) {
            return main;
        }
        try {
            ItemStack off = inventory.getItemInOffHand();
            if (isMultiTool(off)) {
                return off;
            }
        } catch (RuntimeException | LinkageError legacy) {
            // pas de main secondaire sur ce serveur
        }
        return null;
    }

    /**
     * Repose l'item dans la main qui le portait. Sans cette écriture, tout le travail de
     * {@link #applySoul} finit à la poubelle avec la copie sur laquelle il travaillait.
     */
    public static void writeHeld(Player player, ItemStack tool) {
        if (player == null || tool == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        try {
            if (isMultiTool(inventory.getItemInMainHand())) {
                inventory.setItemInMainHand(tool);
            } else {
                inventory.setItemInOffHand(tool);
            }
        } catch (RuntimeException | LinkageError legacy) {
            ItemStack[] contents = inventory.getStorageContents();
            for (int slot = 0; slot < contents.length; slot++) {
                if (isMultiTool(contents[slot])) {
                    inventory.setItem(slot, tool);
                    break;
                }
            }
        }
        try {
            player.updateInventory();
        } catch (RuntimeException | LinkageError refused) {
            // certains serveurs n'aiment pas ce rafraîchissement explicite : l'item est écrit quand même
        }
    }

    /** Le nom de l'item porte l'âme affichée : un coup d'œil à la barre chaude suffit au joueur. */
    public static String displayNameFor(ToolsConfig config, ToolKind soul) {
        String base = config.itemDisplayName();
        return base + (soul == null ? "" : " &8— &f" + capitalize(soul.label()));
    }

    /**
     * La lore de l'item : ce que le joueur lit en survolant, donc <b>l'âme qu'il est en train
     * d'utiliser</b> et <b>les capacités qu'il a payées pour cette âme</b> — nom et niveau, ligne par
     * ligne. Les deux règles viennent du joueur, pas du design : « je ne sais jamais quelle âme est
     * active » et « je ne vois pas ce que j'ai acheté ».
     *
     * <p>Les capacités non achetées sont absentes, volontairement : une liste de vingt lignes grisées ne
     * dit rien de plus que le panneau. Le plafond ({@code tool.lore-abilities}) borne la longueur — un
     * outil au maximum du barème a vingt-deux capacités payées, et une tooltip plus haute que l'écran
     * n'est plus une information ; ce qui dépasse est annoncé en une ligne, avec le raccourci.</p>
     */
    public static List<String> loreFor(ToolsConfig config, ToolStore store, UUID owner, ToolKind soul) {
        List<String> out = new ArrayList<String>();
        for (String line : config.itemLore()) {
            out.add(color(line));
        }
        if (owner == null || store == null) {
            return out;
        }
        ToolsConfig.KindConfig kindConfig = config.kind(soul);
        int tier = kindConfig == null ? 1 : store.tierOf(owner, soul, config.maxTier(kindConfig));
        int max = kindConfig == null ? 1 : config.maxTier(kindConfig);
        out.add(color("&8Âme &f" + (soul == null ? "?" : capitalize(soul.label())) + " &8— palier &f"
                + tier + "&8/&f" + max + (tier >= max ? " &a(max)" : "")));
        appendPaidAbilities(out, config, store, owner, soul, kindConfig, tier);
        appendOtherSouls(out, config, store, owner, soul);
        return out;
    }

    /** Les capacités payées de cette âme, les plus avancées en premier ; le reste en une ligne. */
    private static void appendPaidAbilities(List<String> out, ToolsConfig config, ToolStore store, UUID owner,
            ToolKind soul, ToolsConfig.KindConfig kindConfig, int tier) {
        if (kindConfig == null) {
            out.add(color("&8Âme coupée dans la configuration &7— rien à afficher."));
            return;
        }
        Map<String, Integer> levels = store.levelsOf(owner, soul);
        final Map<String, Integer> paid = new HashMap<String, Integer>();
        List<ToolsConfig.Ability> bought = new ArrayList<ToolsConfig.Ability>();
        for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
            int level = ToolsConfig.levelOf(ability, levels, tier);
            if (level <= 0) {
                continue;
            }
            paid.put(ability.id(), Integer.valueOf(level));
            bought.add(ability);
        }
        if (bought.isEmpty()) {
            out.add(color("&7Aucune capacité payée &8— accroupi + clic droit pour le panneau."));
            return;
        }
        // Les niveaux les plus hauts d'abord : sous un plafond de lignes, ce sont celles-là qui comptent.
        // Le tri est stable, donc à niveau égal on garde l'ordre du barème (celui du panneau).
        Collections.sort(bought, new Comparator<ToolsConfig.Ability>() {

            @Override
            public int compare(ToolsConfig.Ability left, ToolsConfig.Ability right) {
                int mine = paid.get(left.id()).intValue();
                int other = paid.get(right.id()).intValue();
                return other - mine;
            }
        });
        int ceiling = Math.max(1, config.loreMaxAbilities());
        int listed = 0;
        for (ToolsConfig.Ability ability : bought) {
            if (listed >= ceiling) {
                break;
            }
            listed++;
            out.add(color("&a" + ability.name() + " &f" + paid.get(ability.id()).intValue()));
        }
        if (bought.size() > listed) {
            out.add(color("&8+ " + (bought.size() - listed) + " autre(s) &7— &f/tools"));
        }
    }

    /** Les trois autres âmes, résumées en une ligne : le palier des âmes qu'on n'utilise pas en ce moment. */
    private static void appendOtherSouls(List<String> out, ToolsConfig config, ToolStore store, UUID owner,
            ToolKind soul) {
        StringBuilder others = new StringBuilder();
        for (ToolKind kind : ToolKind.values()) {
            if (kind == soul) {
                continue;
            }
            ToolsConfig.KindConfig kindConfig = config.kind(kind);
            if (kindConfig == null) {
                continue;
            }
            if (others.length() > 0) {
                others.append(" &8·&r ");
            }
            others.append(kind.label().split(" ")[0]).append(" &f").append(store.tierOf(owner, kind,
                    config.maxTier(kindConfig)));
        }
        if (others.length() > 0) {
            out.add(color("&8Autres âmes &r") + others.toString());
        }
    }

    /** L'âme affichée est écrite à côté de la marque : le matériau seul ne suffit pas (deux âmes peuvent
     *  partager un matériau, et un serveur qui refuse le morphing doit quand même savoir quoi lister). */
    private static void writeSoul(ItemMeta meta, ToolKind soul) {
        NamespacedKey key = soulKey();
        if (key == null || soul == null) {
            return;
        }
        try {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, soul.name());
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unavailable) {
            // sans PDC, l'âme affichée ne survit pas au relog : la lore se recalcule a la prochaine
            // interaction, ce n'est pas bloquant
        }
    }

    private static void mark(ItemMeta meta) {
        NamespacedKey key = key();
        if (key == null) {
            return;
        }
        try {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "1");
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unavailable) {
            if (!pdcUnavailableLogged) {
                pdcUnavailableLogged = true;
                java.util.logging.Logger.getLogger("ValoriaTools").warning(
                        "PersistentDataContainer indisponible sur ce serveur : les multi-outils ne seront"
                        + " pas marques (les paliers restent dans tools.yml, la reconnaissance se fait"
                        + " par le nom de l'item).");
            }
        }
    }

    /**
     * La cle de marque, resolue une fois. Le constructeur (String, String) de NamespacedKey est
     * utilise plutot que (Plugin, String) : un item marque d'un cote doit etre reconnu de l'autre
     * quel que soit l'ordre de chargement des plugins, et la namespace litterale reste identique.
     */
    private static synchronized NamespacedKey soulKey() {
        if (soulKey != null) {
            return soulKey;
        }
        try {
            soulKey = new NamespacedKey("valariatools", "soul");
        } catch (RuntimeException | NoClassDefFoundError unavailable) {
            return null;
        }
        return soulKey;
    }

    private static synchronized NamespacedKey key() {
        if (markKey != null) {
            return markKey;
        }
        try {
            markKey = new NamespacedKey("valariatools", "multi");
        } catch (RuntimeException | NoClassDefFoundError unavailable) {
            return null;
        }
        return markKey;
    }

    /** Traduit {@code &a}… (le plugin n'utilise pas le traducteur embarqué, qui dépend de classes de chat disparues). */
    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String translated = ChatColor.translateAlternateColorCodes('&', text);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(translated);
        if (!matcher.find()) {
            return translated;
        }
        matcher.reset();
        StringBuilder out = new StringBuilder(translated.length());
        while (matcher.find()) {
            String rgb = matcher.group(1);
            StringBuilder hex = new StringBuilder("§x");
            for (int i = 0; i < 6; i++) {
                hex.append('§').append(Character.toLowerCase(rgb.charAt(i)));
            }
            matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(hex.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase(Locale.ROOT);
    }
}
