package xyz.arcadiadevs.valariatools;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Le « au contact d'un bloc » : décide quelle âme de l'outil s'active pour un bloc donné.
 *
 * <h2>Quatre niveaux de reconnaissance, du plus fiable au plus large</h2>
 * <ol>
 *   <li><b>les tags du registre</b> (<code>#minecraft:logs</code>, <code>#minecraft:mineable/pickaxe</code>) :
 *       ce sont les seuls qui suivent automatiquement l'ajout de nouveaux blocs dans une nouvelle
 *       version du jeu ;</li>
 *   <li><b>les noms de blocs écrits par le admin</b> (<code>matches.blocks</code>) ;</li>
 *   <li><b>les namespace</b> (<code>minecraft:</code>, <code>valoriatycoon:</code>) : filet pour tout ce
 *       qui appartient à une famille donnée, utile sur les blocs personnalisés ;</li>
 *   <li><b>la liste interne</b> ({@link #PICKAXE_BLOCKS}, {@link #AXE_BLOCKS}) : minimale mais toujours
 *       vraie — elle existe parce qu'un outil qui ne reconnaît rien ne fait <em>rien</em> du tout : pas de
 *       drop, pas d'argent, pas de vitesse. C'est exactement la panne qui a été signalée sur le serveur
 *       (« j'ai max ma multi-tool mais je casse les blocs normalement ») : la résolution de tag appelait
 *       <code>Tag#getTag(Class, NamespacedKey)</code>, une signature qui n'a jamais existé, levait une
 *       <code>NoSuchMethodException</code> avalée en silence, et laissait chaque bloc non reconnu.</li>
 * </ol>
 *
 * <h2>La signature de tag cherchée, pas supposée</h2>
 * <p><code>Tag</code>/<code>Bukkit#getTag</code> a changé trois fois de forme (1.13 :
 * <code>(String, NamespacedKey, Class)</code> ; 1.20.5 : déprécié au profit d'un
 * <code>RegistryKey</code> ; certaines versions ont renommé <code>isTagged</code>). Une seule signature
 * écrite en dur aurait donc cassé soit les serveurs anciens, soit les serveurs récents — et le pli de ce
 * paquet est de ne jamais appeler une API incertaine en dur. Les formes sont donc <b>essayées dans
 * l'ordre</b>, puis on se rabat sur le parcours de tous les tags, puis sur la liste interne. Le choix est
 * mémorisé par tag, jamais recalculé bloc par bloc.</p>
 *
 * <p>Le résultat est <b>mémoïsé par matériau</b>, jamais par bloc : un serveur de tycoon évalue des
 * dizaines de milliers de <code>BlockBreakEvent</code> par minute, et <code>Tag#isTagged</code> n'est
 * pas gratuit. Les deux caches sont vidés au reload de la configuration, jamais avant — un matériau ne
 * change pas de tag en cours de vie.</p>
 */
public final class BlockMatcher {

    /** Sentinelle « deja calcule, aucun outil ne correspond » : sans elle on recalculerait a l'infini. */
    private static final Object NONE = new Object();

    /** Le nom du registre des tags de blocs, tel que Bukkit l'a nommé de 1.13 a aujourd'hui. */
    private static final String BLOCK_REGISTRY = "blocks";

    /**
     * Les blocs durs que la pioche emporte, quand aucun tag n'est lisible. Liste <b>volontairement
     * courte</b> : elle n'est pas un catalogue, c'est le filet qui rend l'outil utile sur un serveur où
     * le registre de tags ne répond pas (1.7-1.12, ou une API renommée). Tout le reste se règle par
     * <code>tools.pickaxe.matches.blocks</code>.
     */
    static final Set<String> PICKAXE_BLOCKS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "stone", "granite", "diorite", "andesite", "deepslate", "tuff", "calcite", "dripstone_block",
            "cobblestone", "mossy_cobblestone", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks",
            "chiseled_stone_bricks", "smooth_stone", "bricks", "mud_bricks", "packed_mud",
            "obsidian", "crying_obsidian", "netherrack", "nether_bricks", "red_nether_bricks",
            "blackstone", "gilded_blackstone", "basalt", "smooth_basalt", "end_stone",
            "purpur_block", "prismarine", "prismarine_bricks", "dark_prismarine", "sandstone",
            "smooth_sandstone", "red_sandstone", "cut_sandstone", "chiseled_sandstone", "quartz_block",
            "smooth_quartz", "quartz_bricks", "glowstone", "ice", "packed_ice", "blue_ice",
            "coal_block", "iron_block", "copper_block", "gold_block", "redstone_block", "lapis_block",
            "diamond_block", "emerald_block", "netherite_block", "raw_iron_block", "raw_copper_block",
            "raw_gold_block", "ancient_debris", "nether_quartz_ore", "nether_gold_ore")));

    /** Les suffixes qui désignent une roche à miner (minerais de toute famille, y compris deepslate). */
    static final Set<String> PICKAXE_SUFFIXES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "_ore")));

    /** Les suffixes qui désignent un ouvrage en bois ou un végétal : l'âme hache. */
    static final Set<String> AXE_SUFFIXES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "_log", "_planks", "_wood", "_stem", "_leaves", "_hyphae", "_sapling")));

    /** Idem, en noms exacts (les cultures n'ont pas de suffixe commun). */
    static final Set<String> AXE_BLOCKS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "wheat", "carrots", "potatoes", "beetroots", "melon", "melon_block", "pumpkin",
            "carved_pumpkin", "sugar_cane", "cactus", "bamboo", "bamboo_block", "cocoa", "nether_wart",
            "sweet_berry_bush", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem",
            "crimson_fungus", "warped_fungus", "hay_block")));

    private final Map<Material, Object> cache = new HashMap<Material, Object>();
    private final Map<String, Object> tags = new HashMap<String, Object>();
    private final Set<String> byName = new HashSet<String>();
    private final Map<String, ToolKind> nameToKind = new HashMap<String, ToolKind>();
    private final Map<String, ToolKind> byNamespace = new HashMap<String, ToolKind>();
    private final Map<ToolKind, Set<String>> tagsPerKind = new EnumMap<ToolKind, Set<String>>(ToolKind.class);
    private final JavaPlugin plugin;
    private final ToolKind fallback;
    private boolean tagsProbed;
    private boolean tagsUsable;

    public BlockMatcher(JavaPlugin plugin, ToolsConfig config) {
        this.plugin = plugin;
        this.fallback = config.fallbackKind();
        for (ToolKind kind : ToolKind.values()) {
            ToolsConfig.KindConfig kindConfig = config.kind(kind);
            if (kindConfig == null) {
                continue;
            }
            Set<String> names = new HashSet<String>();
            for (String block : config.fallbackBlocks(kindConfig)) {
                if (block != null && !block.trim().isEmpty()) {
                    names.add(normalize(block));
                }
            }
            if (!names.isEmpty()) {
                this.byName.addAll(names);
                for (String name : names) {
                    this.nameToKind.put(name, kind);
                }
            }
            for (String namespace : config.namespaces(kindConfig)) {
                if (namespace != null && !namespace.trim().isEmpty()) {
                    this.byNamespace.put(namespace.trim().toLowerCase(Locale.ROOT), kind);
                }
            }
            Set<String> tagNames = new HashSet<String>();
            for (String tag : config.tags(kindConfig)) {
                String normalized = normalizeTag(tag);
                if (normalized != null) {
                    tagNames.add(normalized);
                }
            }
            this.tagsPerKind.put(kind, tagNames);
        }
    }

    /** L'âme à utiliser quand le bloc visé n'appartient à aucune liste (clic dans le vide). */
    public ToolKind fallbackKind() {
        return this.fallback;
    }

    /** Portée du regard : la même que celle du clic d'interaction, pour ne jamais promettre un bloc hors
     *  de portée. Six, parce que c'est la distance de minage vanilla en survie. */
    private static final int LOOK_RANGE = 6;

    /**
     * L'âme que ce joueur <b>vise</b> — le bloc sous son réticule, reconnu comme pour une cassure.
     * {@code null} quand il ne vise rien de reconnu (ciel, eau, entité) ; l'appelant choisit alors son
     * âme de secours.
     *
     * <p>Le raycast vit ici et non chez l'appelant pour deux raisons : l'API a changé de nom une fois
     * (<code>getTargetBlock</code> → <code>getTargetBlockExact</code>, avec un comportement différent sur
     * les liquides), et un appelant qui l'ignore ne doit pas faire tomber le plugin au premier clic. Le
     * {@code LinkageError} est là pour cette raison, pas par décor.</p>
     */
    public ToolKind targetedKind(Player player) {
        if (player == null) {
            return null;
        }
        try {
            Block target = player.getTargetBlockExact(LOOK_RANGE);
            if (target != null) {
                return kindOf(target);
            }
        } catch (RuntimeException | LinkageError unavailable) {
            // pas de raycast sur ce serveur : l'outil garde l'âme qu'il affiche déjà
        }
        return null;
    }

    /** Vrai si au moins une règle de reconnaissance existe (sinon l'outil ne prend rien en otage). */
    public boolean configured() {
        return !this.byName.isEmpty() || !this.byNamespace.isEmpty() || !this.tagsPerKind.isEmpty();
    }

    /** L'âme à utiliser pour ce bloc ; {@code null} si le bloc n'intéresse aucun outil. */
    public ToolKind kindOf(Block block) {
        if (block == null) {
            return null;
        }
        return kindOf(block.getType());
    }

    /** Idem, à partir du matériau — utilisé aussi pour décider quoi vendre. */
    public ToolKind kindOf(Material material) {
        if (material == null || material == Material.AIR) {
            return null;
        }
        if (!configured()) {
            return this.fallback;
        }
        Object cached = this.cache.get(material);
        if (cached != null) {
            return cached == NONE ? null : (ToolKind) cached;
        }
        ToolKind resolved = resolve(material);
        this.cache.put(material, resolved == null ? NONE : resolved);
        return resolved;
    }

    private ToolKind resolve(Material material) {
        NamespacedKey key = material.getKey();
        // 1) les tags, s'ils sont lisibles sur CE serveur (voir tagsAvailable)
        if (tagsAvailable()) {
            for (Map.Entry<ToolKind, Set<String>> entry : this.tagsPerKind.entrySet()) {
                for (String tagName : entry.getValue()) {
                    if (tagMatches(tagName, material)) {
                        return entry.getKey();
                    }
                }
            }
        }
        // 2) le nom exact, avec ou sans namespace (le admin ecrit `STONE` ou `minecraft:stone`)
        for (String candidate : new String[]{key.getKey(), key.toString()}) {
            ToolKind kind = this.nameToKind.get(normalize(candidate));
            if (kind != null) {
                return kind;
            }
        }
        // 3) le namespace (utile pour les blocs personnalisés d'autres plugins)
        ToolKind byNamespace = this.byNamespace.get(key.getNamespace().toLowerCase(Locale.ROOT));
        if (byNamespace != null) {
            return byNamespace;
        }
        // 4) le filet interne : sans lui, un serveur sans tag lisible ne minerait RIEN avec l'outil
        return internal(key.getKey());
    }

    /** La reconnaissance par nom, quand le registre de tags est muet (serveurs anciens ou API renommée). */
    private static ToolKind internal(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (PICKAXE_BLOCKS.contains(path) || endsWith(path, PICKAXE_SUFFIXES)) {
            return ToolKind.PICKAXE;
        }
        if (AXE_BLOCKS.contains(path) || endsWith(path, AXE_SUFFIXES)) {
            return ToolKind.AXE;
        }
        return null;
    }

    private static boolean endsWith(String path, Set<String> suffixes) {
        for (String suffix : suffixes) {
            if (path.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vrai si le registre de tags répond sur ce serveur. Testé <b>une fois</b> : un `getMethod` raté à
     * chaque bloc cassé coûterait plus cher que le minage lui-même, et un avertissement par bloc noierait
     * le log. Le premier tag déclaré de la config sert de sonde — c'est celui dont l'absence serait la
     * panne la plus silencieuse.
     */
    public boolean tagsAvailable() {
        if (this.tagsProbed) {
            return this.tagsUsable;
        }
        this.tagsProbed = true;
        for (Set<String> names : this.tagsPerKind.values()) {
            for (String name : names) {
                Object tag = resolveTag(name);
                if (tag != null) {
                    this.tagsUsable = true;
                    break;
                }
            }
            if (this.tagsUsable) {
                break;
            }
        }
        if (!this.tagsUsable && wantsTags()) {
            this.plugin.getLogger().warning("aucun tag Bukkit n'est résolvable sur ce serveur : la"
                    + " reconnaissance des blocs se rabat sur `matches.blocks` de la config et sur la liste"
                    + " interne du plugin. Les blocs hors de ces deux listes seront cassés normalement, sans"
                    + " capacité ni vente — renseigne `matches.blocks` si ton serveur ajoute des blocs.");
        }
        return this.tagsUsable;
    }

    /** Vrai si la config demande des tags (les âmes sans `matches.tags` n'ont rien à attendre). */
    private boolean wantsTags() {
        for (Set<String> names : this.tagsPerKind.values()) {
            if (!names.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ce que la reconnaissance sait faire sur ce serveur, en une ligne — affiché par
     * <code>/tools stats</code>. Un admin qui se demande « pourquoi mes capacités ne s'activent pas » doit
     * pouvoir lire la réponse, pas la deviner.
     */
    public String diagnose() {
        boolean tags = tagsAvailable();
        Material stone = Material.STONE;
        Material log = Material.matchMaterial("OAK_LOG");
        ToolKind stoneKind = kindOf(stone);
        ToolKind logKind = log == null ? null : kindOf(log);
        StringBuilder out = new StringBuilder();
        out.append(tags ? "tags résolus" : "tags indisponibles → filet interne");
        out.append(" · pierre → ").append(stoneKind == null ? "NON RECONNUE" : stoneKind.label());
        out.append(" · tronc → ").append(logKind == null ? "NON RECONNU" : logKind.label());
        out.append(" · filet interne : ").append(PICKAXE_BLOCKS.size() + AXE_BLOCKS.size())
                .append(" noms + suffixes");
        return out.toString();
    }

    private boolean tagMatches(String normalized, Material material) {
        Object tag = resolveTag(normalized);
        return tag != null && tag != NONE && contains(tag, material);
    }

    /** Le tag demandé, ou {@code NONE} s'il est introuvable (réponse mémorisée, une fois par nom). */
    private Object resolveTag(String normalized) {
        if (this.tags.containsKey(normalized)) {
            Object cached = this.tags.get(normalized);
            return cached == NONE ? null : cached;
        }
        Object found = lookupTag(normalized);
        this.tags.put(normalized, found == null ? NONE : found);
        return found;
    }

    /** Les formes de <code>getTag</code> essayées, de la plus courante à la dernière porte. */
    private Object lookupTag(String normalized) {
        NamespacedKey key = parseKey(normalized);
        if (key == null) {
            return null;
        }
        Class<?>[] byRegistry = new Class<?>[]{String.class, NamespacedKey.class, Class.class};
        Object[] byRegistryArgs = new Object[]{BLOCK_REGISTRY, key, Material.class};
        Object found = staticCall("org.bukkit.Bukkit", "getTag", byRegistry, byRegistryArgs);
        if (found == null) {
            found = staticCall("org.bukkit.Tag", "getTag", byRegistry, byRegistryArgs);
        }
        if (found == null) {
            found = staticCall("org.bukkit.Tag", "getTag", new Class<?>[]{Class.class, NamespacedKey.class},
                    new Object[]{Material.class, key});
        }
        if (found == null) {
            found = scanTags(key);
        }
        return found;
    }

    /**
     * La dernière porte : parcourir <em>tous</em> les tags de blocs et comparer les clés. Coûteux une fois,
     * gratuit ensuite (le résultat est mémorisé), et utile parce que <code>getTags</code> a survécu aux
     * trois renommages de <code>getTag</code>.
     */
    private Object scanTags(NamespacedKey wanted) {
        Class<?>[] types = new Class<?>[]{String.class, Class.class};
        Object[] args = new Object[]{BLOCK_REGISTRY, Material.class};
        Object all = staticCall("org.bukkit.Tag", "getTags", types, args);
        if (!(all instanceof Iterable)) {
            all = staticCall("org.bukkit.Bukkit", "getTags", types, args);
        }
        if (!(all instanceof Iterable)) {
            return null;
        }
        for (Object candidate : (Iterable<?>) all) {
            NamespacedKey key = keyOf(candidate);
            if (key != null && key.equals(wanted)) {
                return candidate;
            }
        }
        return null;
    }

    /** Vrai si le matériau figure dans le tag. Trois signatures, puis le parcours du tag. */
    private static boolean contains(Object tag, Material material) {
        Boolean answer = booleanCall(tag, "isTagged", new Class<?>[]{Material.class}, material);
        if (answer == null) {
            answer = booleanCall(tag, "isTagged", new Class<?>[]{org.bukkit.Keyed.class}, material);
        }
        if (answer == null) {
            answer = booleanCall(tag, "isTagged", new Class<?>[]{String.class}, material.getKey().getKey());
        }
        if (answer != null) {
            return answer.booleanValue();
        }
        if (tag instanceof Iterable) {      // Tag extends Iterable<T> : le parcours reste toujours valable
            for (Object element : (Iterable<?>) tag) {
                if (material.equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object staticCall(String className, String method, Class<?>[] types, Object[] args) {
        try {
            Class<?> owner = Class.forName(className);
            Method found = owner.getMethod(method, types);
            return found.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return null;   // forme absente sur ce serveur : l'appelant essaie la suivante
        }
    }

    private static Boolean booleanCall(Object target, String method, Class<?>[] types, Object argument) {
        try {
            Object answer = target.getClass().getMethod(method, types).invoke(target, argument);
            return answer instanceof Boolean ? (Boolean) answer : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return null;
        }
    }

    private static NamespacedKey keyOf(Object candidate) {
        if (candidate == null) {
            return null;
        }
        try {
            Object key = candidate.getClass().getMethod("getKey").invoke(candidate);
            return key instanceof NamespacedKey ? (NamespacedKey) key : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return null;
        }
    }

    /** Une clé de tag lisible, avec ou sans namespace écrit. */
    private static NamespacedKey parseKey(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        int colon = normalized.indexOf(':');
        try {
            if (colon < 0) {
                return NamespacedKey.minecraft(normalized);
            }
            return new NamespacedKey(normalized.substring(0, colon), normalized.substring(colon + 1));
        } catch (RuntimeException | LinkageError unsupported) {
            try {
                return NamespacedKey.fromString(normalized);
            } catch (RuntimeException | LinkageError alsoUnsupported) {
                return null;
            }
        }
    }

    private static String normalizeTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return null;
        }
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private static String normalize(String text) {
        String key = text.trim().toLowerCase(Locale.ROOT);
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        return key.replace("minecraft:", "");
    }
}
