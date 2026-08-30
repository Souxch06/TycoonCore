package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.Arrays;
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
 * <p>Choix assumé : <b>rien n'est codé en dur</b>. Les capacités d'une âme sont la liste exacte que le
 * admin écrit dans <code>config.yml</code> — nom, description, palier d'ouverture, niveau maximal,
 * valeurs — ce qui permet de recopier un barème de wiki sans toucher une ligne de Java, et de le
 * corriger par <code>/tools reload</code> plutôt qu'en recompiler.</p>
 *
 * <h2>Le modèle à deux étages, calqué sur le wiki GenTycoon</h2>
 * <ol>
 *   <li>le <b>palier d'âme</b> (<code>upgrade.max-tier</code>) : le niveau de l'outil lui-même. Il
 *       verrouille l'accès aux capacités (le « Level minimum » / « Prestige mini » du wiki) ;</li>
 *   <li>le <b>niveau de capacité</b> : chaque amélioration a SON propre compteur, de 0 à
 *       <code>max-level</code> (le « Niveau max d'enchantement » du wiki : 5, 10, 300, 2000…). C'est
 *       lui qui fait grossir l'effet, et c'est lui que le joueur achète case par case dans le menu.</li>
 * </ol>
 *
 * <p>Un admin qui écrit <code>chance: [0.02, 0.05]</code> obtient une table explicite ; un admin qui
 * écrit <code>chance: 0.002</code> sans liste obtient une croissance arithmétique par niveau. Les deux
 * formes sont lues par les mêmes lignes, parce qu'un barème de wiki se corrige plus vite avec un nombre
 * qu'avec un tableau de trois cents cases.</p>
 *
 * <p>Un seul point d'entrée lit le YAML : ce constructeur. Les autres classes ne touchent jamais
 * <code>plugin.getConfig()</code>, ce qui rend impossible le grand classique « la config a changé sous
 * le nez de l'appelant en pleine task ».</p>
 */
public final class ToolsConfig {

    /**
     * Les clés dont l'agrégat est le <b>maximum</b> des capacités plutôt que la somme. Une portée de
     * zone ou un plafond de blocs qui s'additionneraient ferait exploser le budget de cassure dès
     * qu'un admin empile deux capacités de zone ; une amplifier de vitesse, elle, s'additionne
     * utilement (et se plafonne par <code>amplifier-cap</code>).
     */
    static final List<String> MAX_KEYS = Collections.unmodifiableList(Arrays.asList(
            "max-blocks", "max-height", "radius", "waves", "count", "interval", "duration"));

    /** Clés qui sont des probabilités : combinées en « au moins un déclenchement », jamais additionnées. */
    static final List<String> CHANCE_KEYS = Collections.unmodifiableList(Arrays.asList(
            "chance", "bite-chance", "pouch-chance", "treasure-chance", "enchant-chance"));

    /** Plafond de sécurité d'une probabilité agrégée, quelle que soit la config. */
    static final double CHANCE_CEILING = 0.95D;

    /** Une capacité : son noyau, son nom de wiki, son verrou de palier, son niveau max, ses valeurs. */
    public static final class Ability {

        private final String id;
        private final String type;
        private final String name;
        private final String description;
        private final int unlock;
        private final int maxLevel;
        private final boolean free;
        private final double priceBase;
        private final double priceStep;
        private final double priceRatio;
        private final double priceCap;
        private final Map<String, Object> raw;

        Ability(String id, String type, String name, String description, int unlock, int maxLevel,
                boolean free, double priceBase, double priceStep, double priceRatio, double priceCap,
                Map<String, Object> raw) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.description = description;
            this.unlock = unlock;
            this.maxLevel = maxLevel;
            this.free = free;
            this.priceBase = priceBase;
            this.priceStep = priceStep;
            this.priceRatio = priceRatio;
            this.priceCap = priceCap;
            this.raw = raw;
        }

        /** Clé de stockage du niveau (unique par âme) : <code>minecoins-pouch</code>, <code>briseur</code>… */
        public String id() {
            return this.id;
        }

        /** Clé de comportement comprise par le moteur (<code>VEIN</code>, <code>HASTE</code>…). */
        public String type() {
            return this.type;
        }

        /** Libellé affiché — le nom du wiki, tel quel. */
        public String name() {
            return this.name;
        }

        /** Description du wiki, reprise dans la tooltip du menu. */
        public String description() {
            return this.description;
        }

        /** Palier d'âme à partir duquel la capacité s'achète (le « Level minimum » du wiki). */
        public int unlock() {
            return this.unlock;
        }

        /** Nom historique de {@link #unlock()}, gardé pour la lecture de la lore. */
        public int fromTier() {
            return this.unlock;
        }

        /** « Niveau max d'enchantement » du wiki : 1 = capacité simple on/off. */
        public int maxLevel() {
            return this.maxLevel;
        }

        /** Vrai si le niveau 1 est offert dès que le palier d'âme l'autorise (pas d'achat pour démarrer). */
        public boolean free() {
            return this.free;
        }

        /** Vrai si le palier d'âme courant autorise cette capacité. */
        public boolean unlockedAt(int tier) {
            return tier >= this.unlock;
        }

        /**
         * Une valeur typée de la capacité. Les listes YAML sont lues « à la main » (pas de
         * <code>getConfigurationSection</code> imbriqué) parce que les capacités sont écrites en
         * flux inline : <code>{id: veine, type: VEIN, max-blocks: [10, 20]}</code>.
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

        /** Une liste de chaînes (matériaux de trésor, enchantements autorisés), vide si absente. */
        public List<String> strings(String key) {
            Object value = this.raw.get(key);
            List<String> out = new ArrayList<String>();
            if (value instanceof List) {
                for (Object element : (List<?>) value) {
                    if (element != null && !String.valueOf(element).trim().isEmpty()) {
                        out.add(String.valueOf(element).trim());
                    }
                }
            } else if (value != null) {
                for (String part : String.valueOf(value).split(",")) {
                    if (!part.trim().isEmpty()) {
                        out.add(part.trim());
                    }
                }
            }
            return out;
        }

        /**
         * La valeur au {@code level}-ième niveau de la capacité. Une liste explicite est lue case par
         * case (la dernière s'applique au-delà) ; sinon c'est <code>base + (clé)-step × (niveau − 1)</code>,
         * plafonné par <code>(clé)-cap</code>. C'est ce qui rend un <code>max-level: 2000</code> lisible
         * dans trois lignes de YAML au lieu de deux mille.
         */
        public double levelDecimal(String key, int level, double defaut) {
            List<Double> list = numbers(key);
            int at = Math.max(0, level - 1);
            double value;
            if (list.isEmpty()) {
                value = defaut;
            } else if (list.size() == 1) {
                value = list.get(0).doubleValue() + scalar(key + "-step", 0.0D) * at;
            } else {
                value = list.get(Math.min(at, list.size() - 1)).doubleValue();
            }
            double cap = scalar(key + "-cap", Double.NaN);
            if (Double.isFinite(cap)) {
                value = Math.min(value, cap);
            }
            return Double.isFinite(value) ? value : defaut;
        }

        /** Idem, arrondi à l'entier (blocs, portée, amplifier, points de durabilité). */
        public int levelValue(String key, int level, int defaut) {
            return (int) Math.round(levelDecimal(key, level, defaut));
        }

        /** Le i-ème nombre de la liste (lecture « par palier », gardée pour les configs héritées). */
        public int valueAt(String key, int index, int defaut) {
            List<Double> list = numbers(key);
            return list.isEmpty() ? defaut
                    : (int) Math.round(list.get(Math.min(index, list.size() - 1)).doubleValue());
        }

        /** Idem, en gardant la partie décimale. */
        public double decimalAt(String key, int index, double defaut) {
            List<Double> list = numbers(key);
            return list.isEmpty() ? defaut
                    : list.get(Math.min(index, list.size() - 1)).doubleValue();
        }

        /**
         * Ce que coûte le passage au niveau {@code level}. En géométrique si <code>price-ratio</code>
         * est posé, en arithmétique sinon — et l'exposant est tronqué à 60 crans, parce qu'un ratio à
         * puissance 2000 ne donne plus un prix mais l'infini.
         */
        public double priceAt(int level) {
            int at = Math.max(0, level - 1);
            double price;
            if (this.priceRatio > 1.0D) {
                price = this.priceBase * Math.pow(this.priceRatio, Math.min(60, at));
            } else {
                price = this.priceBase + this.priceStep * at;
            }
            if (Double.isFinite(this.priceCap) && this.priceCap > 0.0D) {
                price = Math.min(price, this.priceCap);
            }
            return Double.isFinite(price) && price > 0.0D ? price : 0.0D;
        }

        /**
         * Les reglages reels de la capacite, dans l'ordre alphabetique (le GUI n'affiche que ceux qui
         * contiennent des nombres : une liste de materiaux n'a rien a faire dans une tooltip de valeurs).
         */
        public List<String> keys() {
            List<String> out = new ArrayList<String>(this.raw.keySet());
            Collections.sort(out);
            return out;
        }

        private double scalar(String key, double defaut) {
            Object value = this.raw.get(key);
            if (value == null) {
                return defaut;
            }
            double parsed = toDouble(value, defaut);
            return Double.isFinite(parsed) ? parsed : defaut;
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

    /**
     * L'état agrégé d'un noyau pour un joueur : toutes les capacités du même noyau, au niveau acheté.
     *
     * <p>Le wiki superpose plusieurs enchantements de vitesse ou plusieurs « pouch » sur un même
     * outil ; le moteur, lui, ne connaît qu'un comportement par clé. L'agrégat est donc la règle qui
     * transforme N capacités en un effet : les chances se combinent en « au moins un déclencheur », les
     * portées se prennent au maximum, les bonus d'argent et d'XP s'additionnent. Une seule implémentation
     * de cette règle, ici, plutôt que trois façons différentes chez les appelants.</p>
     */
    public static final class Effect {

        private final List<Ability> abilities;
        private final List<Integer> levels;
        private final double boost;

        Effect(List<Ability> abilities, List<Integer> levels, double boost) {
            this.abilities = abilities;
            this.levels = levels;
            this.boost = boost;
        }

        /** Un effet vide n'est pas une erreur : le noyau est simplement absent de la config du joueur. */
        public static Effect none() {
            return new Effect(Collections.<Ability>emptyList(), Collections.<Integer>emptyList(), 1.0D);
        }

        public boolean active() {
            return !this.abilities.isEmpty();
        }

        /** Somme des niveaux achetés sur les capacités de ce noyau (le GUI l'affiche, le log s'en sert). */
        public int level() {
            int total = 0;
            for (Integer level : this.levels) {
                total += level.intValue();
            }
            return total;
        }

        /** Combien de capacités contribuent (deux « HASTE » = deux sources, un seul effet). */
        public int sources() {
            return this.abilities.size();
        }

        /** Multiplicateur de déclenchement du Proc booster, déjà appliqué dans {@link #chance}. */
        public double boost() {
            return this.boost;
        }

        /** Probabilité combinée, plafonnée à 95 % : le proc booster ne rend jamais certain. */
        public double chance(String key, double defaut) {
            double none = 1.0D;
            boolean any = false;
            for (int i = 0; i < this.abilities.size(); i++) {
                double value = this.abilities.get(i).levelDecimal(key, this.levels.get(i).intValue(), defaut);
                if (!Double.isFinite(value) || value <= 0.0D) {
                    continue;
                }
                any = true;
                double single = Math.min(CHANCE_CEILING, value * this.boost);
                none *= 1.0D - single;
            }
            return any ? 1.0D - none : 0.0D;
        }

        /** Somme des valeurs (pourcentages d'argent, d'XP, amplifier de vitesse). */
        public double amount(String key, double defaut) {
            double total = 0.0D;
            boolean any = false;
            for (int i = 0; i < this.abilities.size(); i++) {
                double value = this.abilities.get(i).levelDecimal(key, this.levels.get(i).intValue(), defaut);
                if (!Double.isFinite(value)) {
                    continue;
                }
                if (value != 0.0D) {
                    any = true;
                }
                total += value;
            }
            return any ? total : defaut;
        }

        /** Le maximum des valeurs (portée, plafond de blocs) ou la somme, selon la clé. */
        public int value(String key, int defaut) {
            if (this.abilities.isEmpty()) {
                return defaut;
            }
            boolean takeMax = MAX_KEYS.contains(key);
            int out = takeMax ? Integer.MIN_VALUE : 0;
            for (int i = 0; i < this.abilities.size(); i++) {
                int value = this.abilities.get(i).levelValue(key, this.levels.get(i).intValue(), defaut);
                out = takeMax ? Math.max(out, value) : out + value;
            }
            return takeMax ? Math.max(out, 0) : out;
        }

        /** Un booléen partagé : toutes les sources doivent l'autoriser (un veto ne s'ignore pas). */
        public boolean flag(String key, boolean defaut) {
            if (this.abilities.isEmpty()) {
                return defaut;
            }
            for (int i = 0; i < this.abilities.size(); i++) {
                if (!this.abilities.get(i).flag(key, defaut)) {
                    return false;
                }
            }
            return true;
        }

        /** L'union des listes de chaînes (objets de trésor, enchantements autorisés). */
        public List<String> strings(String key) {
            List<String> out = new ArrayList<String>();
            for (int i = 0; i < this.abilities.size(); i++) {
                for (String value : this.abilities.get(i).strings(key)) {
                    if (!out.contains(value)) {
                        out.add(value);
                    }
                }
            }
            return out;
        }
    }

    /** Ce qu'un palier coûte, ce qu'il donne, et quelles capacités il ouvre. */
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
        private int maxTier = 50;
        private double sellMultiplier;
        private double sellMinValue;
        private int xpPerBlock = 1;
        private int durabilityCost = 1;
        private double tierPriceBase = 1000.0D;
        private double tierPriceRatio = 1.15D;
        private double tierPriceCap = 0.0D;
        private double abilityPriceBase = 250.0D;
        private double abilityPriceStep = 120.0D;
        private double abilityPriceCap = 0.0D;
        private boolean replant = true;
        /** Prix de revente par matériau (cle = nom de bloc, sans namespace). */
        private final Map<String, Double> sellPrices = new HashMap<String, Double>();
        /** Gains publies par le metier : argent par BLOC casse (ou par monstre tue). */
        private final Map<String, Double> jobPrices = new HashMap<String, Double>();
        /** Idem, en XP. Les valeurs du wiki sont decimales (0,01 pour la roche) : le reliquat est
         *  reporté d'un geste a l'autre par le listener, jamais arrondi a zero bloc par bloc. */
        private final Map<String, Double> jobXp = new HashMap<String, Double>();

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

        /** Nom affiché de l'âme, ou le libellé par défaut de l'enum. */
        public String displayName() {
            return this.displayName == null ? MultiTool.capitalize(this.kind.label()) : this.displayName;
        }

        /** La lore propre à l'âme, sous le palier, dans la tooltip de l'item. */
        public List<String> lore() {
            return Collections.unmodifiableList(this.lore);
        }

        /** Nombre de paliers payables : <code>max-tier - 1</code>, le palier 1 étant offert. */
        public int payableTiers() {
            return Math.max(0, this.maxTier - 1);
        }

        /** Vrai si cette âme déclare au moins un prix de revente. */
        public boolean hasSellPrices() {
            return !this.sellPrices.isEmpty();
        }

        /** Récolter les cultures mûres et les replanter (capacités de la houe du wiki). */
        public boolean replant() {
            return this.replant;
        }

        /** Les capacités déclarées, dans l'ordre du fichier (le menu les affiche dans cet ordre). */
        public List<Ability> abilities() {
            return Collections.unmodifiableList(this.abilities);
        }
    }

    private final JavaPlugin plugin;
    private final Map<ToolKind, KindConfig> kinds = new EnumMap<ToolKind, KindConfig>(ToolKind.class);
    private String itemMaterialName = "NETHERITE_PICKAXE";
    private String itemDisplayName = "&6⚒ Multi-outil de Valoria";
    private final List<String> itemLore = new ArrayList<String>();
    private boolean unbreakable = true;
    private boolean hideFlags = true;
    private boolean undroppable = true;
    private boolean singlePerPlayer = true;
    private boolean autoGive = true;
    private boolean requireClaimed;
    private double toolPrice;
    private final List<String> allowedWorlds = new ArrayList<String>();
    private double sellMultiplier = 1.0D;
    private double sellMinValue = 0.0D;
    private boolean sellOnlyWhenSneaking;
    private boolean autoSellUnmatched;
    private boolean enabled = true;
    private boolean hastePassive = true;

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
        // Le contrat de l'item (voir ToolGuard) : un seul exemplaire, qui ne quitte pas le sac. Trois
        // reglages separes parce qu'ils ne protegent pas la meme chose : `undroppable` ferme les issues,
        // `single-per-player` retire les doublons, `auto-give` rend l'outil a qui ne l'a plus.
        this.undroppable = root.getBoolean("tool.undroppable", true);
        this.singlePerPlayer = root.getBoolean("tool.single-per-player", true);
        this.autoGive = root.getBoolean("tool.auto-give", true);
        this.hastePassive = root.getBoolean("tool.haste-while-held", true);
        this.requireClaimed = root.getBoolean("tools.require-claimed", false);
        this.toolPrice = Math.max(0.0D, root.getDouble("tool.price", 0.0D));
        this.allowedWorlds.clear();
        for (String world : root.getStringList("tools.allowed-worlds")) {
            if (world != null && !world.trim().isEmpty()) {
                this.allowedWorlds.add(world.trim().toLowerCase(Locale.ROOT));
            }
        }
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
            ConfigurationSection section = tools == null ? null
                    : tools.getConfigurationSection(kind.name().toLowerCase(Locale.ROOT));
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
        config.replant = section.getBoolean("harvest.replant", true);
        config.maxTier = Math.max(1, section.getInt("upgrade.max-tier", 50));
        config.tierPriceBase = Math.max(0.0D, section.getDouble("upgrade.price-base", 1000.0D));
        config.tierPriceRatio = ratio(section.getDouble("upgrade.price-ratio", 1.15D));
        config.tierPriceCap = Math.max(0.0D, section.getDouble("upgrade.price-cap", 0.0D));
        // Les defauts de prix sont des FRERES de la liste `abilities`, pas des cles de cette liste :
        // un bloc YAML qui serait a la fois table et sequence est invalide, et le plugin ne
        // s'activerait plus du tout (SnakeYAML jette avant notre premiere ligne de log).
        config.abilityPriceBase = Math.max(0.0D, section.getDouble("ability-price.base", 250.0D));
        config.abilityPriceStep = Math.max(0.0D, section.getDouble("ability-price.step", 120.0D));
        config.abilityPriceCap = Math.max(0.0D, section.getDouble("ability-price.cap", 0.0D));
        config.sellMultiplier = section.getDouble("sell.multiplier", this.sellMultiplier);
        config.sellMinValue = section.getDouble("sell.min-value", this.sellMinValue);
        // Une seule table d'argent et une seule d'XP, quel que soit ce qui declenche le gain : un nom de
        // bloc (WHEAT), un objet peche (COD) et un type d'entite (ZOMBIE) ne se recouvrent jamais, donc
        // les trois sources partagent la meme cle sans risque d'ecrire deux fois la meme ligne.
        readJobTable(config, section, "jobs.gains", config.jobPrices);
        readJobTable(config, section, "jobs.xp", config.jobXp);
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
        // Une grille de prix plus courte que max-tier n'est PAS complee au hasard : la formule
        // `upgrade.price-ratio` prend le relais des que la liste s'arrete (voir priceOf). Un admin
        // qui passe de 5 a 50 paliers n'a donc pas 45 nombres a ecrire.
        List<?> abilities = section.getList("abilities");
        config.abilities.clear();
        if (abilities != null) {
            for (Object element : abilities) {
                Ability ability = toAbility(element, config);
                if (ability != null) {
                    config.abilities.add(ability);
                }
            }
        }
        warnOnDuplicateIds(config);
    }

    /**
     * Une table de gains du métier (<code>jobs.gains</code> et <code>jobs.xp</code>). Les clés sont lues dans la section, jamais
     * reconstruites à la main : un nom de matériau ou d'entité inconnu du serveur est ignoré avec un
     * avertissement, et ne casse pas le chargement de la config.
     */
    private void readJobTable(KindConfig config, ConfigurationSection section, String path,
            Map<String, Double> into) {
        ConfigurationSection table = section.getConfigurationSection(path);
        if (table == null) {
            return;
        }
        for (String key : table.getKeys(false)) {
            double value = number(table.get(key), Double.NaN);
            if (!Double.isFinite(value)) {
                this.plugin.getLogger().warning(path + "." + key + " : valeur numérique attendue, ignorée");
                continue;
            }
            into.put(key.trim().toUpperCase(Locale.ROOT), Double.valueOf(value));
        }
    }

    /** Deux capacités avec le meme `id` ecraseraient le niveau achete de la premiere : on previent. */
    private void warnOnDuplicateIds(KindConfig config) {
        List<String> seen = new ArrayList<String>();
        for (Ability ability : config.abilities) {
            if (seen.contains(ability.id())) {
                this.plugin.getLogger().warning("capacite `" + ability.id() + "` declaree deux fois pour "
                        + config.kind().label() + " : les niveaux achetes seraient partages, donne-lui un id unique");
                continue;
            }
            seen.add(ability.id());
        }
    }

    @SuppressWarnings("unchecked")
    private Ability toAbility(Object element, KindConfig kindConfig) {
        Map<String, Object> map;
        if (element instanceof Map) {
            map = new LinkedHashMap<String, Object>((Map<String, Object>) element);
        } else if (element instanceof String) {
            // Forme courte : `- VEIN` = capacite sans reglage, offerte des le palier 1.
            map = new LinkedHashMap<String, Object>();
            map.put("type", ((String) element).trim().toUpperCase(Locale.ROOT));
        } else {
            this.plugin.getLogger().warning("capacite ignoree (format inconnu) : " + element);
            return null;
        }
        Object type = map.remove("type");
        if (type == null) {
            type = map.remove("kernel");
        }
        if (type == null) {
            return null;
        }
        String kernel = String.valueOf(type).trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        Object id = map.remove("id");
        String key = id == null ? kernel.toLowerCase(Locale.ROOT)
                : String.valueOf(id).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        Object label = map.remove("label");
        Object description = map.remove("desc");
        if (description == null) {
            description = map.remove("description");
        }
        Object unlock = map.remove("unlock");
        if (unlock == null) {
            unlock = map.remove("from-tier");
        }
        if (unlock == null) {
            unlock = map.remove("tier");
        }
        int maxTier = Math.max(1, kindConfig.maxTier);
        int required = (int) Math.max(1, number(unlock, 1.0D));
        if (required > maxTier) {
            this.plugin.getLogger().warning("capacite " + key + " ouverte au palier " + required
                    + ", au-dela du max-tier " + maxTier + " : ramenee au dernier palier");
            required = maxTier;
        }
        int maxLevel = (int) Math.max(1, number(map.remove("max-level"), 1.0D));
        boolean free = truthy(map.remove("free"));
        double priceBase = positive(number(map.remove("price"), -1.0D), kindConfig.abilityPriceBase);
        double priceStep = positive(number(map.remove("price-step"), -1.0D), kindConfig.abilityPriceStep);
        double priceRatio = ratio(number(map.remove("price-ratio"), -1.0D));
        double priceCap = positive(number(map.remove("price-cap"), -1.0D), kindConfig.abilityPriceCap);
        return new Ability(key, kernel, label == null ? prettify(kernel) : String.valueOf(label),
                description == null ? "" : String.valueOf(description), required, maxLevel,
                free, priceBase, priceStep, priceRatio, priceCap, map);
    }

    /**
     * Un booléen de config, sous les trois formes qu'un admin écrit vraiment : <code>true</code> (YAML),
     * <code>"oui"</code> (français), <code>1</code> (habitué des drapeaux numériques).
     */
    private static boolean truthy(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() > 0.5D;
        }
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return text.equals("true") || text.equals("yes") || text.equals("oui") || text.equals("1");
    }

    /** Un reglage negatif ou absent n'ecrase pas le defaut de l'ame : 0 = « gratuit » est un cas reel. */
    private static double positive(double value, double defaut) {
        if (!Double.isFinite(value) || value < 0.0D) {
            return defaut;
        }
        return value;
    }

    /** Un ratio de croissance doit etre superieur a 1 pour vouloir dire quelque chose ; 0 = lineaire. */
    private static double ratio(double value) {
        if (!Double.isFinite(value) || value <= 1.0D) {
            return 0.0D;
        }
        return Math.min(value, 2.0D);
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

    /** Vrai si la vitesse de minage est appliquée pendant que l'outil est tenu (voir `tool.haste-while-held`). */
    public boolean hasteWhileHeld() {
        return this.hastePassive;
    }

    /** Vrai si l'outil ne peut pas sortir du sac de son joueur (clic Q, coffre, hopper, cadre, drop). */
    public boolean undroppable() {
        return this.undroppable;
    }

    /** Vrai si le plugin force un seul exemplaire par joueur et retire les doublons. */
    public boolean singlePerPlayer() {
        return this.singlePerPlayer;
    }

    /**
     * Vrai si l'outil est rendu à qui ne l'a plus (connexion, respawn, sac fermé). C'est ce qui rend
     * <code>undroppable</code> supportable : un objet qu'on ne peut pas lâcher ne doit pas non plus se
     * perdre. Le prix de l'outil (<code>tool.price</code>) n'est alors plus prélevé qu'une fois, à
     * l'achat — c'est assumé, et documenté.
     */
    public boolean autoGive() {
        return this.autoGive;
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

    /**
     * Ce que coûte l'outil lui-même. {@code 0} = gratuit, et <code>/tools buy</code> devient un simple
     * <code>/tools give</code> : un serveur qui n'a pas encore décidé de son prix ne doit pas être
     * bloqué par notre propre économie.
     */
    public double toolPrice() {
        return this.toolPrice;
    }

    /**
     * Les mondes où l'outil fonctionne ; liste vide = tous. Une règle 100 % Bukkit : la claim d'île
     * demanderait l'API d'un plugin de skyblock, et ce dépôt a fait le choix de n'en dépendre d'aucun
     * (le contrôle « zéro API tierce » du build le refuse, et à juste titre).
     */
    public List<String> allowedWorlds() {
        return Collections.unmodifiableList(this.allowedWorlds);
    }

    /** Vrai si le monde courant a le droit de voir l'outil agir. */
    public boolean allowsWorld(String name) {
        if (this.allowedWorlds.isEmpty() || name == null) {
            return true;
        }
        return this.allowedWorlds.contains(name.toLowerCase(Locale.ROOT));
    }

    /** Les compteurs de <code>stats.yml</code> sont-ils activés ? (l'admin peut vouloir s'en passer) */
    public boolean statsEnabled() {
        return this.plugin.getConfig().getBoolean("stats.enabled", true);
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

    /** Le réservoir de trésors par défaut, déclaré par le admin (jamais la table privée du serveur). */
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

    /**
     * Le gain que le métier attache à un bloc cassé (ou à un type de monstre tué) : {@code 0} quand le
     * admin ne l'a pas déclaré. Un seul accesseur pour l'argent et l'XP, posé ici parce que les tables de
     * KindConfig sont privées — un listener qui lirait `kindConfig.jobPrices` ne compilerait pas.
     */
    public double jobGain(KindConfig config, String key, boolean experience) {
        if (config == null || key == null) {
            return 0.0D;
        }
        Map<String, Double> table = experience ? config.jobXp : config.jobPrices;
        if (table.isEmpty()) {
            return 0.0D;
        }
        Double declared = table.get(key.toUpperCase(Locale.ROOT));
        return declared == null || !Double.isFinite(declared.doubleValue()) ? 0.0D : declared.doubleValue();
    }

    /** Vrai si cette âme déclare des gains de métier (le menu l'annonce au joueur). */
    public boolean hasJobGains(KindConfig config) {
        return config != null && !config.jobPrices.isEmpty();
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

    /**
     * Prix du passage au palier {@code tier} : la grille explicite d'abord, la formule
     * <code>price-base × price-ratio^palier</code> ensuite. {@code < 0} = âme à son maximum.
     */
    public double priceOf(KindConfig config, int tier) {
        if (tier > config.maxTier || tier <= 0) {
            return -1.0D;
        }
        if (tier - 1 < config.prices.size()) {
            return config.prices.get(tier - 1).doubleValue();
        }
        double base = config.tierPriceBase;
        if (config.prices.size() > 1) {
            // la fin de la grille fait foi : une formule recollée sur des prix écrits a la main
            // ne doit pas les contredire brutalement au palier suivant
            int written = config.prices.size() - 1;
            double last = config.prices.get(written).doubleValue();
            double first = config.prices.get(0).doubleValue();
            base = last <= first ? last * 1.25D : last * (last / first);
        }
        if (config.tierPriceRatio > 1.0D && base > 0.0D) {
            double price = base * Math.pow(config.tierPriceRatio, Math.min(120, tier - 1));
            if (config.tierPriceCap > 0.0D) {
                price = Math.min(price, config.tierPriceCap);
            }
            return Double.isFinite(price) && price > 0.0D ? price : 0.0D;
        }
        return base;
    }

    public int maxTier(KindConfig config) {
        return config.maxTier;
    }

    public List<Ability> abilities(KindConfig config) {
        return config.abilities();
    }

    /**
     * L'effet agrégé d'un noyau pour un joueur donné.
     *
     * @param levels les niveaux achetés de l'âme (cle = id de capacité), jamais modifiés ici
     */
    public Effect effect(KindConfig config, String type, int tier, Map<String, Integer> levels) {
        List<Ability> sources = new ArrayList<Ability>();
        List<Integer> used = new ArrayList<Integer>();
        double boost = 1.0D;
        for (Ability ability : config.abilities) {
            if (!ability.unlockedAt(tier)) {
                continue;
            }
            int level = levelOf(ability, levels, tier);
            if (level < 1) {
                continue;
            }
            if (ability.type().equalsIgnoreCase("PROC_BOOSTER")) {
                boost *= 1.0D + ability.levelDecimal("percent", level, 0.0D) / 100.0D;
                if (!type.equalsIgnoreCase("PROC_BOOSTER")) {
                    continue;
                }
            }
            sources.add(ability);
            used.add(Integer.valueOf(level));
        }
        if (sources.isEmpty()) {
            return Effect.none();
        }
        return new Effect(sources, used, boost);
    }

    /**
     * Le niveau d'une capacité pour ce joueur : ce qu'il a acheté, ou 1 si la capacité est marquée
     * <code>free</code> et que son palier d'ouverture est atteint. Un niveau ne dépasse jamais
     * <code>max-level</code>, même si <code>tools.yml</code> a été édité à la main.
     */
    public static int levelOf(Ability ability, Map<String, Integer> levels, int tier) {
        int ceiling = Math.max(1, ability.maxLevel());
        Integer stored = levels == null ? null : levels.get(ability.id());
        int level = stored == null ? 0 : stored.intValue();
        if (level < 1 && ability.free() && ability.unlockedAt(tier)) {
            level = 1;
        }
        return Math.max(0, Math.min(level, ceiling));
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
