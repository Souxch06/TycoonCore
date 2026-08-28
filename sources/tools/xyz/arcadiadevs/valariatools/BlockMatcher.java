package xyz.arcadiadevs.valariatools;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Le « au contact d'un bloc » : décide quelle âme de l'outil s'active pour un bloc donné.
 *
 * <h2>Trois niveaux de reconnaissance, du plus fiable au plus large</h2>
 * <ol>
 *   <li><b>les tags du registre</b> (<code>#minecraft:logs</code>, <code>#minecraft:mineable/pickaxe</code>) :
 *       ce sont les seuls qui suivent automatiquement l'ajout de nouveaux blocs dans une nouvelle
 *       version du jeu ;</li>
 *   <li><b>les noms de blocs écrits par le admin</b> : pour les cas où le tag n'existe pas encore
 *       (le plugin vise 1.7 en référence, où les tags n'existent pas) ;</li>
 *   <li><b>les namespace</b> (<code>minecraft:</code>, <code>valoriatycoon:</code>) : filet pour tout ce
 *       qui appartient à une famille donnée, utile sur les blocs personnalisés.</li>
 * </ol>
 *
 * <p>Le résultat est <b>mémoïsé par matériau</b>, jamais par bloc : un serveur de tycoon évalue des
 * dizaines de milliers de <code>BlockBreakEvent</code> par minute, et <code>Tag#isTagged</code> n'est
 * pas gratuit. Le cache est vidé au reload de la configuration, jamais avant — un matériau ne change
 * pas de tag en cours de vie.</p>
 */
public final class BlockMatcher {

    /** Sentinelle « deja calcule, aucun outil ne correspond » : sans elle on recalculerait a l'infini. */
    private static final Object NONE = new Object();

    private final Map<Material, Object> cache = new HashMap<Material, Object>();
    private final Set<String> byName = new HashSet<String>();
    private final Map<String, ToolKind> nameToKind = new HashMap<String, ToolKind>();
    private final Map<String, ToolKind> byNamespace = new HashMap<String, ToolKind>();
    private final Map<ToolKind, Set<String>> tagsPerKind = new EnumMap<ToolKind, Set<String>>(ToolKind.class);
    private final JavaPlugin plugin;
    private final ToolKind fallback;
    private boolean tagWarned;

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
            Set<String> tags = new HashSet<String>();
            for (String tag : config.tags(kindConfig)) {
                String normalized = normalizeTag(tag);
                if (normalized != null) {
                    tags.add(normalized);
                }
            }
            this.tagsPerKind.put(kind, tags);
        }
    }

    /** L'âme à utiliser quand le bloc visé n'appartient à aucune liste (clic dans le vide). */
    public ToolKind fallbackKind() {
        return this.fallback;
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
        org.bukkit.NamespacedKey key = material.getKey();
        // 1) les tags, si le serveur les connaît : ce sont les seuls qui suivent les nouveaux blocs
        for (Map.Entry<ToolKind, Set<String>> entry : this.tagsPerKind.entrySet()) {
            for (String tagName : entry.getValue()) {
                if (tagMatches(tagName, material)) {
                    return entry.getKey();
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
        // 4) rien de reconnu : le bloc n'appartient à aucune âme, l'outil ne le touche pas
        return null;
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

    /**
     * Un tag du registre, résolu par réflexion : <code>Tag#isTagged</code> est apparu en 1.13 et sa
     * signature a bougé selon les versions (String puis NamespacedKey). Ici, une absence de méthode
     * ne casse rien : on passe aux noms de blocs.
     */
    private boolean tagMatches(String normalized, Material material) {
        if (normalized == null) {
            return false;
        }
        try {
            Class<?> tagRegistry = Class.forName("org.bukkit.Tag");
            Object tag = tagRegistry.getMethod("getTag", Class.class, org.bukkit.NamespacedKey.class)
                    .invoke(null, Material.class, org.bukkit.NamespacedKey.fromString(normalized));
            if (tag == null) {
                return false;
            }
            Object answer;
            try {
                answer = tag.getClass().getMethod("isTagged", Material.class).invoke(tag, material);
            } catch (NoSuchMethodException olderSignature) {
                answer = tag.getClass().getMethod("isTagged", String.class).invoke(tag,
                        material.getKey().getKey());
            }
            return answer instanceof Boolean && ((Boolean) answer).booleanValue();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            if (!this.tagWarned) {
                this.tagWarned = true;
                this.plugin.getLogger().warning("tags Bukkit indisponibles sur ce serveur : la"
                        + " reconnaissance des blocs se limite aux listes `matches.blocks` de la config");
            }
            return false;
        }
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
