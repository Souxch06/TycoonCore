package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Toute la configuration du plugin, lue une fois et figée.
 *
 * <p>Choix assumé : <b>rien n'est codé en dur</b>. Les capacités d'un outil sont la liste exacte que
 * le admin écrit dans <code>config.yml</code>, avec le palier où chacune s'ouvre et les valeurs
 * propres à chaque palier. C'est ce qui permet d'aligner le plugin sur un barème existant (prix,
 * portées, chances) sans toucher une ligne de Java — et donc de corriger un écart de wiki en
 * rechargeant la config plutôt qu'en recompiler.</p>
 *
 * <p>Un seul point d'entrée lit le YAML : ce constructeur. Les autres classes ne touchent jamais
 * <code>plugin.getConfig()</code>, ce qui rend impossible le grand classique « la config a changé
 * sous le nez de l'appelant en pleine task ».</p>
 */
public final class ToolsConfig {

    /** Une capacité : son type, son libellé, le palier qui l'ouvre, et ses valeurs par palier. */
    public static final class Ability {

        private final String type;
        private final String name;
        private final int fromTier;
        private final Map<String, Object> raw;

        Ability(String type, String name, int fromTier, Map<String, Object> raw) {
            this.type = type;
            this.name = name;
            this.fromTier = fromTier;
            this.raw = raw;
        }

        /** Clé de comportement (<code>VEIN</code>, <code>TREE_FELL</code>, <code>CRIT</code>…). */
        public String type() {
            return this.type;
        }

        /** Libellé affiché dans l'interface ; retombe sur le type si le admin l'a oublié. */
        public String name() {
            return this.name;
        }

        /** Palier à partir duquel la capacité est active (1 = dès le départ). */
        public int fromTier() {
            return this.fromTier;
        }

        /**
         * Une valeur typée de la capacité. Les listes YAML sont lues « à la main » (pas de
         * <code>getConfigurationSection</code> imbriqué) parce que les capacités sont écrites en
         * flux inline : <code>{type: VEIN, max-blocks: [10, 20]}</code>.
         */
        public List<Double> numbers(String key) {
            Object value = this.raw.get(key);
            List<Double> out = new ArrayList<Double>();
            if (value instanceof List) {
                for (Object element : (List<?>) value) {
                    double parsed = toDouble(element, Double.NaN);
                    if (!Double.isNaN(parsed)) {
                        out.add(Double.valueOf(parsed));
                    }
                }
            } else if (value != null) {
                double parsed = toDouble(value, Double.NaN);
                if (!Double.isNaN(parsed)) {
                    out.add(Double.valueOf(parsed));
                }
            }
            return out;
        }

        /** Une valeur booléenne, avec repli sur {@code defaut} quand la clé est absente ou sale. */
        public boolean flag(String key, boolean defaut) {
            Object value = this.raw.get(key);
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue();
            }
            if (value instanceof String) {
                String text = ((String) value).trim().toLowerCase(Locale.ROOT);
                if (text.equals("true") || text.equals("yes") || text.equals("oui")) {
                    return true;
                }
                if (text.equals("false") || text.equals("no") || text.equals("non")) {
                    return false;
                }
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue() > 0.5D;
            }
            return defaut;
        }

        /** Le i-ème nombre de la liste, ou {@code defaut} si la liste est plus courte que le palier. */
        public int valueAt(String key, int index, int defaut) {
            List<Double> numbers = numbers(key);
            if (numbers.isEmpty()) {
                return defaut;
            }
            int at = Math.min(index, numbers.size() - 1);
            double value = numbers.get(at).doubleValue();
            if (!Double.isFinite(value)) {
                return defaut;
            }
            return (int) Math.round(value);
        }

        /** Idem, en gardant la partie décimale (chances, multiplicateurs, pourcentages). */
        public double decimalAt(String key, int index, double defaut) {
            List<Double> numbers = numbers(key);
            if (numbers.isEmpty()) {
                return defaut;
            }
            double value = numbers.get(Math.min(index, numbers.size() - 1)).doubleValue();
            return Double.isFinite(value) ? value : defaut;
        }

        private static double toDouble(Object value, double defaut) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value == null) {
                return defaut;
            }
            try {
                return Double.parseDouble(String.valueOf(value).trim().replace(",", "."));
            } catch (NumberFormatException malformed) {
                return defaut;
            }
        }
    }

    /** Ce qu'un palier coûte, et ce qu'il donne. */
    public static final class KindConfig {

        private final ToolKind kind;
        private Material material;
        private String displayName;
        private final List<String> lore = new ArrayList<String>();
        private final List<String> tags = new ArrayList<String>();
        private final List<String> blockNames = new ArrayList<String>();
        private final List<String> namespaces = new ArrayList<String>();
        private final List<Double> prices = new ArrayList<Double>();
        private final List<Ability> abilities = new ArrayList<Ability>();
        private int maxTier = 1;
        private double sellMultiplier;
        private double sellMinValue;
        private int xpPerBlock = 1;
        private int durabilityCost = 1;
        /** Prix de revente par matériau (cle = nom de bloc, sans namespace). */
        private final Map<String, Double> sellPrices = new HashMap<String, Double>();

        KindConfig(ToolKind kind, Material material) {
            this.kind = kind;
            this.material = material;
            this.displayName = null;
        }

        /** Le matériau qui sert d'icône à l'âme (pioche → son item de pioche, etc.). */
        public Material material() {
            return this.material;
        }

        public ToolKind kind() {
            return this.kind;
        }

        /** Nombre de paliers payables : `max-tier - 1`, le palier 1 étant offert. */
        public int payableTiers() {
            return Math.max(0, this.maxTier - 1);
        }

        /** Vrai si cette âme déclare au moins un prix de revente. */
        public boolean hasSellPrices() {
            return !this.sellPrices.isEmpty();
        }
    }

    private final JavaPlugin plugin;
    private final Map<ToolKind, KindConfig> kinds = new EnumMap<ToolKind, KindConfig>(ToolKind.class);
    private String itemMaterialName = "NETHERITE_PICKAXE";
    private String itemDisplayName = "&6⚒ Multi-outil de Valoria";
    private final List<String> itemLore = new ArrayList<String>();
    private boolean unbreakable = true;
    private boolean hideFlags = true;
    private boolean requireClaimed;
    private double sellMultiplier = 1.0D;
    private double sellMinValue = 0.0D;
    private boolean sellOnlyWhenSneaking;
    private boolean autoSellUnmatched;
    private boolean enabled = true;

    public ToolsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * (Re)charge tout depuis le YAML. Appelé à l'activation et après <code>/tools reload</code>.
     *
     * <p>Chaque clé a un repli raisonnable : une config tronquée ou écrite à la main doit donner un
     * plugin utile, pas un plugin muet.</p>
     */
    public void load() {
        ConfigurationSection root = plugin.getConfig();
        this.enabled = root.getBoolean("enabled", true);
        this.itemMaterialName = root.getString("tool.material", this.itemMaterialName);
        this.itemDisplayName = root.getString("tool.display-name", this.itemDisplayName);
        this.itemLore.clear();
        this.itemLore.addAll(root.getStringList("tool.lore"));
        this.unbreakable = root.getBoolean("tool.unbreakable", true);
        this.hideFlags = root.getBoolean("tool.hide-flags", true);
        this.requireClaimed = root.getBoolean("tools.require-claimed", false);
        this.sellMultiplier = root.getDouble("sell-on-break.multiplier", 1.0D);
        this.sellMinValue = root.getDouble("sell-on-break.min-value", 0.0D);
        this.sellOnlyWhenSneaking = root.getBoolean("sell-on-break.only-when-sneaking", false);
        this.autoSellUnmatched = root.getBoolean("sell-on-break.auto-sell-unmatched", true);
        if (!Double.isFinite(this.sellMultiplier) || this.sellMultiplier < 0.0D) {
            this.plugin.getLogger().warning("sell-on-break.multiplier invalide (" + this.sellMultiplier
                    + ") : 1.0 appliqué");
            this.sellMultiplier = 1.0D;
        }

        this.kinds.clear();
        ConfigurationSection tools = root.getConfigurationSection("tools");
        for (ToolKind kind : ToolKind.values()) {
            KindConfig config = new KindConfig(kind, kind.fallbackMaterial());
            ConfigurationSection section = tools == null ? null : tools.getConfigurationSection(kind.name().toLowerCase(Locale.ROOT));
            if (section != null) {
                readKind(config, section);
            }
            this.kinds.put(kind, config);
        }
    }

    private void readKind(KindConfig config, ConfigurationSection section) {
        String materialName = section.getString("material", "");
        Material parsed = material(materialName);
        if (parsed != null) {
            config.material = parsed;
        }
        config.displayName = section.getString("display-name", config.displayName);
        config.lore.clear();
        config.lore.addAll(section.getStringList("lore"));
        config.tags.addAll(section.getStringList("matches.tags"));
        config.blockNames.addAll(section.getStringList("matches.blocks"));
        config.namespaces.addAll(section.getStringList("matches.namespaces"));
        config.maxTier = Math.max(1, section.getInt("upgrade.max-tier", 5));
        config.sellMultiplier = section.getDouble("sell.multiplier", this.sellMultiplier);
        config.sellMinValue = section.getDouble("sell.min-value", this.sellMinValue);
        ConfigurationSection sellPrices = section.getConfigurationSection("sell.prices");
        config.sellPrices.clear();
        if (sellPrices != null) {
            for (String key : sellPrices.getKeys(false)) {
                double value = number(sellPrices.get(key), Double.NaN);
                if (Double.isFinite(value) && value >= 0.0D) {
                    config.sellPrices.put(key.trim().toLowerCase(Locale.ROOT), Double.valueOf(value));
                }
            }
        }
        List<?> tierPrices = section.getList("upgrade.prices");
        config.prices.clear();
        if (tierPrices != null) {
            for (Object price : tierPrices) {
                double value = number(price, Double.NaN);
                if (Double.isFinite(value) && value >= 0.0D) {
                    config.prices.add(Double.valueOf(value));
                }
            }
        }
        // Un palier de plus que de prix : le dernier palier s'obtient avec le dernier prix connu
        // (un admin qui oublie un prix ne doit pas rendre le palier gratuit… ni atteignable).
        while (config.prices.size() < config.maxTier - 1) {
            double last = config.prices.isEmpty() ? 1000.0D : config.prices.get(config.prices.size() - 1).doubleValue();
            config.prices.add(Double.valueOf(last * 2.5D));
        }
        List<?> abilities = section.getList("abilities");
        config.abilities.clear();
        if (abilities != null) {
            for (Object element : abilities) {
                Ability ability = toAbility(element, config.maxTier);
                if (ability != null) {
                    config.abilities.add(ability);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Ability toAbility(Object element, int maxTier) {
        Map<String, Object> map;
        if (element instanceof Map) {
            map = new LinkedHashMap<String, Object>((Map<String, Object>) element);
        } else if (element instanceof String) {
            // Forme courte : `- VEIN` = capacité sans réglage, ouverte au palier 1.
            map = new LinkedHashMap<String, Object>();
            map.put("type", ((String) element).trim().toUpperCase(Locale.ROOT));
        } else {
            this.plugin.getLogger().warning("capacite ignoree (format inconnu) : " + element);
            return null;
        }
        Object type = map.remove("type");
        if (type == null) {
            type = map.remove("name");
            if (type != null && map.isEmpty()) {
                map.put("type", type);
                type = map.get("type");
            }
        }
        if (type == null) {
            return null;
        }
        String kind = String.valueOf(type).trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        Object label = map.remove("label");
        Object from = map.remove("from-tier");
        if (from == null) {
            from = map.remove("tier");
        }
        int fromTier = (int) Math.max(1, number(from, 1.0D));
        if (fromTier > maxTier) {
            this.plugin.getLogger().warning("capacite " + kind + " ouverte au palier " + fromTier
                    + ", au-dela du max-tier " + maxTier + " : ramenee au dernier palier");
            fromTier = maxTier;
        }
        return new Ability(kind, label == null ? prettify(kind) : String.valueOf(label), fromTier, map);
    }

    private static String prettify(String type) {
        StringBuilder out = new StringBuilder(type.length() + 4);
        boolean first = true;
        for (String part : type.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!first) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            out.append(part.substring(1).toLowerCase(Locale.ROOT));
            first = false;
        }
        return out.length() == 0 ? type : out.toString();
    }

    private static Material material(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
            return material == null || material == Material.AIR ? null : material;
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unknown) {
            return null;
        }
    }

    private static double number(Object value, double defaut) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return defaut;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim().replace(",", "."));
        } catch (NumberFormatException malformed) {
            return defaut;
        }
    }

    // ------------------------------------------------------------------ accès

    public boolean enabled() {
        return this.enabled;
    }

    public KindConfig kind(ToolKind kind) {
        return this.kinds.get(kind);
    }

    public Map<ToolKind, KindConfig> kinds() {
        return Collections.unmodifiableMap(this.kinds);
    }

    public String itemMaterialName() {
        return this.itemMaterialName;
    }

    /** Matériau de l'item lui-même ; si le nom de la config est inconnu du serveur, on prévient. */
    public Material itemMaterial() {
        Material material = material(this.itemMaterialName);
        if (material == null) {
            this.plugin.getLogger().warning("tool.material inconnu (" + this.itemMaterialName
                    + ") : NETHERITE_PICKAXE utilise a la place");
            material = Material.NETHERITE_PICKAXE;
        }
        return material;
    }

    public String itemDisplayName() {
        return this.itemDisplayName;
    }

    public List<String> itemLore() {
        return Collections.unmodifiableList(this.itemLore);
    }

    public boolean unbreakable() {
        return this.unbreakable;
    }

    public boolean hideFlags() {
        return this.hideFlags;
    }

    public boolean requireClaimed() {
        return this.requireClaimed;
    }

    public double sellMultiplier() {
        return this.sellMultiplier;
    }

    public double sellMinValue() {
        return this.sellMinValue;
    }

    public boolean sellOnlyWhenSneaking() {
        return this.sellOnlyWhenSneaking;
    }

    public boolean autoSellUnmatched() {
        return this.autoSellUnmatched;
    }

    /**
     * XP rendue par bloc cassé. Le serveur n'en donne plus quand l'événement est annulé (c'est notre
     * cas) : sans ce réglage, miner avec l'outil serait une perte sèche d'XP par rapport à la pioche.
     */
    public int xpPerBlock(KindConfig config) {
        return Math.max(0, config.xpPerBlock);
    }

    /** Points de durabilité retirés par geste (1 = comme un outil normal, même sur un filon). */
    public int durabilityCost(KindConfig config) {
        return Math.max(1, config.durabilityCost);
    }

    /** Le réservoir de trésors de la pêche, déclaré par le admin (jamais la table privée du serveur). */
    public List<String> treasureItems() {
        return Collections.unmodifiableList(this.plugin.getConfig().getStringList("tool.treasure.items"));
    }

    /** Prix de revente d'un matériau pour cette âme, ou {@code < 0} si le admin ne l'a pas déclaré. */
    public double sellPriceOf(KindConfig config, Material material) {
        if (material == null) {
            return -1.0D;
        }
        String key = material.getKey().getKey().toLowerCase(Locale.ROOT);
        Double declared = config.sellPrices.get(key);
        if (declared == null) {
            declared = config.sellPrices.get(material.getKey().toString().toLowerCase(Locale.ROOT));
        }
        return declared == null ? -1.0D : declared.doubleValue();
    }

    /** Vrai si au moins une âme déclare une grille de prix (l'interface s'en sert pour son texte). */
    public boolean sellPricesDeclared() {
        for (KindConfig config : this.kinds.values()) {
            if (!config.sellPrices.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Le prix du passage au {@code tier} demandé, ou {@code < 0} si l'outil est au max. */
    public double priceOf(KindConfig config, int tier) {
        if (tier > config.maxTier || tier <= 0) {
            return -1.0D;
        }
        if (config.prices.isEmpty()) {
            return 1000.0D;
        }
        int index = Math.min(tier - 1, config.prices.size() - 1);
        return config.prices.get(index).doubleValue();
    }

    public int maxTier(KindConfig config) {
        return config.maxTier;
    }

    public List<Ability> abilities(KindConfig config) {
        return Collections.unmodifiableList(config.abilities);
    }

    /** Vrai si {@code type} est débloqué au palier donné pour cette âme d'outil. */
    public Ability ability(KindConfig config, String type, int tier) {
        for (Ability ability : config.abilities) {
            if (ability.type().equalsIgnoreCase(type) && tier >= ability.fromTier()) {
                return ability;
            }
        }
        return null;
    }

    /** Liste des capacités connues, pour l'aide de <code>/tools</code> et le contrôle de config. */
    public static List<String> knownAbilityTypes() {
        return Collections.unmodifiableList(new ArrayList<String>(Abilities.SUPPORTED));
    }

    /**
     * L'âme utilisée quand rien n'est reconnu (clic dans le vide, minage hors listes). C'est le
     * minerai qui sert d'outil par défaut, donc la pioche — un joueur qui configure mal sa liste
     * `blocks` ne doit pas se retrouver avec un outil qui ne mine plus rien.
     */
    public ToolKind fallbackKind() {
        String configured = this.plugin.getConfig().getString("tool.fallback-tool", "PICKAXE");
        ToolKind kind = ToolKind.parse(configured);
        return kind == null ? ToolKind.PICKAXE : kind;
    }

    /** Les blocs reconnus par une âme, quand aucun tag n'est disponible (serveurs anciens). */
    public List<String> fallbackBlocks(KindConfig config) {
        return Collections.unmodifiableList(config.blockNames);
    }

    public List<String> tags(KindConfig config) {
        return Collections.unmodifiableList(config.tags);
    }

    public List<String> namespaces(KindConfig config) {
        return Collections.unmodifiableList(config.namespaces);
    }
}
