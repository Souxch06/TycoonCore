package xyz.arcadiadevs.valariatools;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ce que le multi-outil a réellement rapporté, par joueur et par âme — <code>stats.yml</code>.
 *
 * <h2>Pourquoi ces compteurs existent</h2>
 * <p>Dans un tycoon, un outil sans mesure est un outil qu'on n'améliore pas : le joueur ne voit pas ce
 * que le palier suivant lui a apporté, et l'admin n'a aucun moyen de dire si une capacité est trop forte.
 * Les compteurs servent donc à trois choses : le menu (la case statistiques), <code>/tools top</code>
 * (l'émulation du classement), et le diagnostic d'équilibrage (« 40 millions de blocs pour un joueur,
 * trois pour les autres » se voit ici, pas dans le log).</p>
 *
 * <h2>Une écriture, un seul endroit</h2>
 * <p>Le plugin calcule déjà tous les gestes dans {@code ToolListener} (l'événement est annulé, donc le
 * serveur ne compte rien à notre place). Les compteurs sont donc incrémentés <b>au même endroit que le
 * paiement</b> : un drop compté ailleurs serait compté deux fois, ou pas du tout après un rechargement.</p>
 *
 * <h2>Le fichier reste petit</h2>
 * <p>Un nul (compteur à zéro) n'est jamais écrit, et un joueur sans aucun gain n'a pas de section :
 * sur un serveur à 3 000 joueurs dont 200 minent, <code>stats.yml</code> fait 200 entrées, pas 3 000.
 * L'écriture est atomique (fichier temporaire puis <code>ATOMIC_MOVE</code>), comme les paliers.</p>
 */
public final class ToolStats {

    /** Ce que le classement sait mesurer. Une seule énumération, pour que la commande et le menu disent la même chose. */
    public enum Metric {

        BLOCKS("blocs minés", "blocks"),
        CROPS("cultures récoltées", "crops"),
        TREES("arbres abattus", "trees"),
        FISH("poissons pêchés", "fish"),
        KILLS("monstres tués", "kills"),
        MONEY("argent gagné", "money"),
        LEVELS("niveaux achetés", "levels");

        private final String label;
        private final String key;

        Metric(String label, String key) {
            this.label = label;
            this.key = key;
        }

        public String label() {
            return this.label;
        }

        /** Le nom de clé dans <code>stats.yml</code>. */
        public String key() {
            return this.key;
        }

        /** Résout `blocs`, `argent`, `top money`… {@code null} si le mot ne désigne rien. */
        public static Metric parse(String text) {
            if (text == null) {
                return null;
            }
            String needle = text.trim().toLowerCase(Locale.ROOT);
            for (Metric metric : values()) {
                if (metric.key.equals(needle) || metric.label.startsWith(needle) || metric.name().equalsIgnoreCase(needle)) {
                    return metric;
                }
            }
            if (needle.startsWith("bloc") || needle.startsWith("mine")) {
                return BLOCKS;
            }
            if (needle.startsWith("arg") || needle.startsWith("mone") || needle.startsWith("$")) {
                return MONEY;
            }
            if (needle.startsWith("niv") || needle.startsWith("up")) {
                return LEVELS;
            }
            if (needle.startsWith("poiss") || needle.startsWith("pech")) {
                return FISH;
            }
            if (needle.startsWith("kill") || needle.startsWith("monstr") || needle.startsWith("chass")) {
                return KILLS;
            }
            if (needle.startsWith("cul") || needle.startsWith("ferm") || needle.startsWith("recolt")) {
                return CROPS;
            }
            if (needle.startsWith("arb") || needle.startsWith("buch")) {
                return TREES;
            }
            return null;
        }
    }

    /** Une ligne de classement : qui, combien, sur quelle âme. */
    public static final class Entry {

        private final UUID owner;
        private final String name;
        private final double value;

        Entry(UUID owner, String name, double value) {
            this.owner = owner;
            this.name = name;
            this.value = value;
        }

        public UUID owner() {
            return this.owner;
        }

        public String name() {
            return this.name == null ? "inconnu" : this.name;
        }

        public double value() {
            return this.value;
        }
    }

    /** Les compteurs d'une âme. Paquet privé : seul ce fichier les écrit, on ne propage pas un setter. */
    private static final class Counters {

        private long blocks;
        private long crops;
        private long trees;
        private long fish;
        private long kills;
        private long levels;
        private double money;

        long of(Metric metric) {
            switch (metric) {
                case CROPS: return this.crops;
                case TREES: return this.trees;
                case FISH: return this.fish;
                case KILLS: return this.kills;
                case LEVELS: return this.levels;
                case MONEY: return (long) Math.round(this.money);
                case BLOCKS:
                default: return this.blocks;
            }
        }

        double exact(Metric metric) {
            return metric == Metric.MONEY ? this.money : of(metric);
        }

        void add(Metric metric, double amount) {
            switch (metric) {
                case CROPS: this.crops += (long) amount; break;
                case TREES: this.trees += (long) amount; break;
                case FISH: this.fish += (long) amount; break;
                case KILLS: this.kills += (long) amount; break;
                case LEVELS: this.levels += (long) amount; break;
                case MONEY: this.money += amount; break;
                case BLOCKS:
                default: this.blocks += (long) amount; break;
            }
        }

        boolean empty() {
            return this.blocks == 0 && this.crops == 0 && this.trees == 0 && this.fish == 0
                    && this.kills == 0 && this.levels == 0 && this.money <= 0.0D;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<ToolKind, Counters>> counters =
            new LinkedHashMap<UUID, Map<ToolKind, Counters>>();
    private final Map<UUID, String> names = new LinkedHashMap<UUID, String>();
    private boolean enabled = true;
    private boolean dirty;

    public ToolStats(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    /** La mesure est coupable du chat : un serveur qui ne veut pas de classement ne doit rien écrire. */
    public void enabled(boolean value) {
        this.enabled = value;
    }

    public boolean enabled() {
        return this.enabled;
    }

    /** Lit <code>stats.yml</code> ; une valeur illisible vaut zéro, jamais une exception au démarrage. */
    public void load() {
        this.counters.clear();
        this.names.clear();
        if (!this.file.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        for (String key : yaml.getKeys(false)) {
            if (key.equalsIgnoreCase("last")) {
                continue;                       // meta eventuelle, pas un joueur
            }
            UUID owner = parse(key);
            if (owner == null) {
                this.plugin.getLogger().warning("stats.yml : cle de joueur ignorée (" + key + ")");
                continue;
            }
            this.names.put(owner, yaml.getString(key + ".name", null));
            Map<ToolKind, Counters> forPlayer = new EnumMap<ToolKind, Counters>(ToolKind.class);
            for (ToolKind kind : ToolKind.values()) {
                Counters value = new Counters();
                ConfigurationSection section = yaml.getConfigurationSection(key + "." + ToolStore.name(kind));
                if (section != null) {
                    for (Metric metric : Metric.values()) {
                        double amount = number(section.get(metric.key()));
                        if (amount > 0.0D) {
                            value.add(metric, amount);
                        }
                    }
                }
                if (!value.empty()) {
                    forPlayer.put(kind, value);
                }
            }
            if (!forPlayer.isEmpty()) {
                this.counters.put(owner, forPlayer);
            }
        }
    }

    public void save() {
        if (!this.enabled || !this.dirty) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<ToolKind, Counters>> entry : this.counters.entrySet()) {
            String name = this.names.get(entry.getKey());
            if (name != null) {
                yaml.set(entry.getKey() + ".name", name);
            }
            for (Map.Entry<ToolKind, Counters> kind : entry.getValue().entrySet()) {
                Counters value = kind.getValue();
                for (Metric metric : Metric.values()) {
                    double amount = value.exact(metric);
                    if (amount > 0.0D) {
                        yaml.set(entry.getKey() + "." + ToolStore.name(kind.getKey()) + "." + metric.key(),
                                metric == Metric.MONEY ? round2(amount) : Long.valueOf((long) amount));
                    }
                }
            }
        }
        File tmp = new File(this.file.getParentFile(), this.file.getName() + ".tmp");
        try {
            this.file.getParentFile().mkdirs();
            yaml.save(tmp);
            try {
                Files.move(tmp.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(tmp.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            this.dirty = false;
        } catch (IOException failed) {
            this.plugin.getLogger().severe("sauvegarde de stats.yml impossible : " + failed.getMessage());
            try {
                Files.deleteIfExists(tmp.toPath());
            } catch (IOException ignored) {
                // rien de plus à faire
            }
        } catch (UnsupportedOperationException refused) {
            this.plugin.getLogger().severe("stats.yml : écriture refusée par le disque (" + refused.getMessage() + ")");
        }
    }

    /** Un geste de {@code amount} blocs (ou cultures, ou arbres) sur cette âme. */
    public void gesture(Player player, ToolKind kind, Metric metric, double amount) {
        if (!this.enabled || player == null || kind == null || amount <= 0.0D) {
            return;
        }
        Counters value = counters(player.getUniqueId(), kind, true);
        if (value == null) {
            return;
        }
        value.add(metric, amount);
        this.names.put(player.getUniqueId(), player.getName());
        this.dirty = true;
    }

    /** L'argent encaissé par l'outil (vente à la casse, pochettes, butins) — la seule mesure qui compte le tycoon. */
    public void money(Player player, ToolKind kind, double amount) {
        gesture(player, kind, Metric.MONEY, amount);
    }

    /** Total d'une âme pour un joueur (0 si rien n'a été mesuré). */
    public long total(UUID owner, ToolKind kind, Metric metric) {
        Counters value = counters(owner, kind, false);
        return value == null ? 0L : value.of(metric);
    }

    /** Total toutes âmes confondues — ce qu'affiche le menu, qui n'a pas à choisir une âme pour ça. */
    public long total(UUID owner, Metric metric) {
        long out = 0L;
        Map<ToolKind, Counters> forPlayer = owner == null ? null : this.counters.get(owner);
        if (forPlayer == null) {
            return 0L;
        }
        for (Counters value : forPlayer.values()) {
            out += value.of(metric);
        }
        return out;
    }

    /** Le nom connu le plus récent d'un joueur, pour un classement qui reste lisible quand il est hors ligne. */
    public String name(UUID owner) {
        String stored = owner == null ? null : this.names.get(owner);
        if (stored != null) {
            return stored;
        }
        Player online = this.plugin.getServer().getPlayer(owner);
        return online == null ? null : online.getName();
    }

    /**
     * Les {@code limit} premiers joueurs sur une âme (ou toutes âmes si {@code kind} est {@code null}).
     * Le tri est fait ici et nulle part ailleurs, pour que la commande et le menu ne puissent pas se
     * contredire sur qui est premier.
     */
    public List<Entry> top(Metric metric, ToolKind kind, int limit) {
        List<Entry> out = new ArrayList<Entry>();
        for (Map.Entry<UUID, Map<ToolKind, Counters>> entry : this.counters.entrySet()) {
            double value = 0.0D;
            if (kind == null) {
                for (Counters counters : entry.getValue().values()) {
                    value += counters.exact(metric);
                }
            } else {
                Counters counters = entry.getValue().get(kind);
                value = counters == null ? 0.0D : counters.exact(metric);
            }
            if (value > 0.0D) {
                out.add(new Entry(entry.getKey(), name(entry.getKey()), value));
            }
        }
        Collections.sort(out, (left, right) -> Double.compare(right.value(), left.value()));
        while (out.size() > Math.max(1, limit)) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    /** Combien de joueurs ont déjà été mesurés (le message de l'admin, « le classement n'est pas vide »). */
    public int measured() {
        return this.counters.size();
    }

    /** Remise à zéro d'un joueur (l'admin qui efface un compte triché ne doit pas éditer le YAML). */
    public void clear(UUID owner) {
        if (owner == null) {
            return;
        }
        if (this.counters.remove(owner) != null) {
            this.dirty = true;
        }
    }

    private Counters counters(UUID owner, ToolKind kind, boolean create) {
        if (owner == null || kind == null) {
            return null;
        }
        Map<ToolKind, Counters> forPlayer = this.counters.get(owner);
        if (forPlayer == null) {
            if (!create) {
                return null;
            }
            forPlayer = new EnumMap<ToolKind, Counters>(ToolKind.class);
            this.counters.put(owner, forPlayer);
        }
        Counters value = forPlayer.get(kind);
        if (value == null && create) {
            value = new Counters();
            forPlayer.put(kind, value);
        }
        return value;
    }

    private static double number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim().replace(",", "."));
        } catch (NumberFormatException malformed) {
            return 0.0D;
        }
    }

    private static double round2(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static UUID parse(String text) {
        try {
            return UUID.fromString(text);
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}
