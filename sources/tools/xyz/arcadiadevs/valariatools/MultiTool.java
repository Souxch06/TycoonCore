package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
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
 * <h2>Robustesse</h2>
 * <p>Chaque opération tolère l'absence de PersistentDataContainer (le plugin vise 1.7 en référence) :
 * dans ce cas l'item n'est pas marqué, le plugin fonctionne mais tout le monde reste au palier 1, et
 * le log le dit une fois — plutôt qu'une {@code NoClassDefFoundError} au premier clic.</p>
 */
public final class MultiTool {

    private static NamespacedKey markKey;
    private static boolean pdcUnavailableLogged;

    private MultiTool() {
    }

    /** Fabrique l'item, avec son nom, ses paliers affichés et son absence de casse. */
    public static ItemStack create(ToolsConfig config, ToolStore store, UUID owner) {
        ItemStack stack = new ItemStack(config.itemMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(color(config.itemDisplayName()));
        meta.setLore(loreFor(config, store, owner));
        mark(meta);
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

    /** Rafraîchit nom + lore (après un achat de palier, un reload, un changement de monde). */
    public static void refresh(ItemStack stack, ToolsConfig config, ToolStore store, UUID owner) {
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.setDisplayName(color(config.itemDisplayName()));
        meta.setLore(loreFor(config, store, owner));
        mark(meta);
        stack.setItemMeta(meta);
    }

    /**
     * La lore, avec l'état des quatre âmes. Elle reste volontairement courte : c'est ce que le
     * joueur voit en survolant l'item, donc uniquement ce qui lui évite d'ouvrir l'interface.
     */
    public static List<String> loreFor(ToolsConfig config, ToolStore store, UUID owner) {
        List<String> out = new ArrayList<String>();
        for (String line : config.itemLore()) {
            out.add(color(line));
        }
        if (owner != null && store != null) {
            for (ToolKind kind : ToolKind.values()) {
                ToolsConfig.KindConfig kindConfig = config.kind(kind);
                if (kindConfig == null) {
                    continue;
                }
                int tier = store.tierOf(owner, kind, config.maxTier(kindConfig));
                int max = config.maxTier(kindConfig);
                String bar = tier >= max ? "§a" + tier + "/" + max + " §a(max)" : "§e" + tier + "/" + max;
                out.add("§7" + capitalize(kind.label()) + " : " + bar + " §8— §f"
                        + unlockedSummary(config, kindConfig, tier, store, owner, kind));
            }
        }
        return out;
    }

    /**
     * « 6/24 capacités · 41 niveaux » : ce que le palier autorise, et ce que le joueur a vraiment payé.
     * Les deux nombres sont indispensables : le barème du wiki ouvre jusqu'à 22 capacités sur une âme,
     * et un joueur qui lit « 22 capacités » alors qu'il n'a rien acheté croit que l'outil est cassé.
     */
    private static String unlockedSummary(ToolsConfig config, ToolsConfig.KindConfig kindConfig, int tier,
            ToolStore store, UUID owner, ToolKind kind) {
        int allowed = 0;
        int bought = 0;
        Map<String, Integer> levels = store.levelsOf(owner, kind);
        for (ToolsConfig.Ability ability : config.abilities(kindConfig)) {
            if (ability.unlockedAt(tier)) {
                allowed++;
            }
            bought += ToolsConfig.levelOf(ability, levels, tier);
        }
        if (allowed == 0) {
            return "aucune capacité à ce palier";
        }
        return allowed + (allowed > 1 ? " capacités" : " capacité") + " · " + bought + " niveau"
                + (bought > 1 ? "x" : "");
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
