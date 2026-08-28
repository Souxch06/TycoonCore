package xyz.arcadiadevs.valariatools;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
 * Les paliers d'âme et les niveaux de capacités achetés, par joueur — dans
 * <code>plugins/ValoriaTools/tools.yml</code>.
 *
 * <h2>Pourquoi rien n'est stocké dans l'item</h2>
 * <p>Un multi-outil qui garde sa force <em>dans</em> son item (NBT / PersistentDataContainer) est un
 * ticket de loterie : on le dépose dans un coffre, on le donne, on le vend, et la progression change de
 * main — ou disparaît quand l'item casse. Stocké par UUID, l'état est impossible à dupliquer et survit à
 * la perte de l'item. C'est le même contrat que le système d'économie interne : un fichier, écrit de
 * façon <b>atomique</b> (fichier temporaire puis <code>ATOMIC_MOVE</code>), donc jamais un YAML à moitié
 * écrit après un arrêt brutal qui remettrait tout le monde au palier 1.</p>
 *
 * <h2>Deux compteurs, un seul fichier</h2>
 * <pre>
 * 069a…-uuid:
 *   pickaxe:
 *     tier: 12          # le palier d'ame (verrouille les capacites)
 *     abilities:        # le niveau achete de chaque capacite (cle = id de la config)
 *       fortune: 6
 *       onde-sismique: 40
 * </pre>
 * <p>La forme ancienne (<code>pickaxe: 12</code>, un entier brut) reste lue : une mise à jour du jar ne
 * doit jamais remettre un serveur à zéro parce que l'écriture a changé de forme.</p>
 *
 * <p>Les âmes sont écrites en minuscules (<code>pickaxe</code>, <code>rod</code>…) : un nom de clé lisible
 * à la main par un admin vaut plus qu'une symétrie avec l'enum.</p>
 */
public final class ToolStore {

    /** L'état d'une âme pour un joueur : son palier, et les niveaux de ses capacités. */
    private static final class State {

        private int tier = 1;
        private final Map<String, Integer> abilities = new LinkedHashMap<String, Integer>();

        int tier() {
            return this.tier;
        }

        void tier(int value) {
            this.tier = value;
        }

        int level(String id) {
            Integer stored = this.abilities.get(id);
            return stored == null ? 0 : stored.intValue();
        }

        void level(String id, int value) {
            if (value <= 0) {
                this.abilities.remove(id);
                return;
            }
            this.abilities.put(id, Integer.valueOf(value));
        }

        /** Lecture seule : le listener d'événements n'a rien à écrire ici. */
        Map<String, Integer> levels() {
            return Collections.unmodifiableMap(this.abilities);
        }

        boolean empty() {
            return this.tier <= 1 && this.abilities.isEmpty();
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<ToolKind, State>> states = new LinkedHashMap<UUID, Map<ToolKind, State>>();
    private boolean dirty;

    public ToolStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tools.yml");
    }

    /** Lit le fichier ; une clé illisible vaut palier 1 et zéro niveau, jamais une exception. */
    public void load() {
        this.states.clear();
        if (!this.file.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        for (String key : yaml.getKeys(false)) {
            UUID uuid = parse(key);
            if (uuid == null) {
                this.plugin.getLogger().warning("tools.yml : cle de joueur ignoree (" + key + ")");
                continue;
            }
            Map<ToolKind, State> forPlayer = new EnumMap<ToolKind, State>(ToolKind.class);
            for (ToolKind kind : ToolKind.values()) {
                String path = key + "." + name(kind);
                State state = new State();
                if (yaml.isConfigurationSection(path)) {
                    readState(yaml.getConfigurationSection(path), state);
                } else {
                    int legacy = yaml.getInt(path, 1);
                    state.tier(Math.max(1, legacy));
                }
                forPlayer.put(kind, state);
            }
            this.states.put(uuid, forPlayer);
        }
    }

    private void readState(ConfigurationSection section, State state) {
        if (section == null) {
            return;
        }
        int tier = section.getInt("tier", 1);
        state.tier(Math.max(1, tier));
        ConfigurationSection abilities = section.getConfigurationSection("abilities");
        if (abilities == null) {
            return;
        }
        // `getValues` et non `getInt(id)` : le nom vient de la config, il peut etre n'importe quoi,
        // et lire la table d'un bloc evite de reformer un chemin de cle a la main.
        for (Map.Entry<String, Object> entry : abilities.getValues(false).entrySet()) {
            int level = asInt(entry.getValue());
            if (level > 0) {
                state.level(entry.getKey().trim().toLowerCase(Locale.ROOT), level);
            }
        }
    }

    /** Écrit uniquement si quelque chose a changé (evite un disque touché pour rien à chaque niveau). */
    public void save() {
        if (!this.dirty) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<ToolKind, State>> entry : this.states.entrySet()) {
            for (Map.Entry<ToolKind, State> kind : entry.getValue().entrySet()) {
                State state = kind.getValue();
                if (state.empty()) {
                    continue;
                }
                String path = entry.getKey() + "." + name(kind.getKey());
                yaml.set(path + ".tier", Integer.valueOf(state.tier()));
                for (Map.Entry<String, Integer> ability : state.abilities.entrySet()) {
                    yaml.set(path + ".abilities." + ability.getKey(), ability.getValue());
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
            this.plugin.getLogger().severe("sauvegarde de tools.yml impossible : " + failed.getMessage());
            try {
                Files.deleteIfExists(tmp.toPath());
            } catch (IOException ignored) {
                // rien de plus à faire
            }
        } catch (UnsupportedOperationException saveFailed) {
            this.plugin.getLogger().severe("tools.yml : ecriture refusee par le disque (" + saveFailed.getMessage() + ")");
        }
    }

    /** Palier courant du joueur pour cette âme — jamais en dessous de 1, jamais au-dessus du max. */
    public int tierOf(UUID owner, ToolKind kind, int maxTier) {
        State state = state(owner, kind);
        if (state == null) {
            return 1;
        }
        return Math.max(1, Math.min(state.tier(), Math.max(1, maxTier)));
    }

    /** Applique un palier (et sauvegarde tout de suite : un palier payé ne se perd pas). */
    public void setTier(Player player, ToolKind kind, int tier, int maxTier) {
        if (player == null || kind == null) {
            return;
        }
        State state = state(player.getUniqueId(), kind, true);
        if (state == null) {
            return;
        }
        state.tier(Math.max(1, Math.min(tier, Math.max(1, maxTier))));
        this.dirty = true;
        save();
    }

    /**
     * Les niveaux achetés des capacités de cette âme, en lecture seule. La clé est l'<code>id</code>
     * de la config, la valeur le niveau (absent = 0, donc capacité non achetée).
     */
    public Map<String, Integer> levelsOf(UUID owner, ToolKind kind) {
        State state = state(owner, kind);
        return state == null ? Collections.<String, Integer>emptyMap() : state.levels();
    }

    /** Le niveau brut d'une capacité, sans plafond : c'est `ToolsConfig.levelOf` qui borne. */
    public int levelOf(UUID owner, ToolKind kind, String id) {
        State state = state(owner, kind);
        return state == null || id == null ? 0 : state.level(id.toLowerCase(Locale.ROOT));
    }

    /** Pose un niveau de capacité et sauvegarde : un achat payé ne se perd pas sur un arrêt brutal. */
    public void setLevel(Player player, ToolKind kind, String id, int level, int maxLevel) {
        if (player == null || kind == null || id == null) {
            return;
        }
        State state = state(player.getUniqueId(), kind, true);
        if (state == null) {
            return;
        }
        state.level(id.trim().toLowerCase(Locale.ROOT), Math.max(0, Math.min(level, Math.max(1, maxLevel))));
        this.dirty = true;
        save();
    }

    /** Somme des niveaux achetés d'une âme : c'est le « total » affiché dans la lore et /tools stats. */
    public int totalLevels(UUID owner, ToolKind kind) {
        State state = state(owner, kind);
        if (state == null) {
            return 0;
        }
        int total = 0;
        for (Integer level : state.abilities.values()) {
            total += level.intValue();
        }
        return total;
    }

    /** Remet une âme à zéro (palier et capacités) — `/tools reset`, sans toucher aux autres âmes. */
    public void reset(Player player, ToolKind kind) {
        if (player == null || kind == null) {
            return;
        }
        State state = state(player.getUniqueId(), kind, true);
        if (state == null) {
            return;
        }
        state.tier(1);
        for (String id : new java.util.ArrayList<String>(state.abilities.keySet())) {
            state.level(id, 0);
        }
        this.dirty = true;
        save();
    }

    /** Les ids connus d'une âme, pour `/tools ability …` (l'admin ne doit pas taper un id de mémoire). */
    public List<String> knownIds(ToolsConfig config, ToolKind kind) {
        List<String> out = new java.util.ArrayList<String>();
        ToolsConfig.KindConfig kindConfig = config == null ? null : config.kind(kind);
        if (kindConfig == null) {
            return out;
        }
        for (ToolsConfig.Ability ability : kindConfig.abilities) {
            out.add(ability.id());
        }
        return out;
    }

    private State state(UUID owner, ToolKind kind) {
        return state(owner, kind, false);
    }

    private State state(UUID owner, ToolKind kind, boolean create) {
        if (owner == null || kind == null) {
            return null;
        }
        Map<ToolKind, State> forPlayer = this.states.get(owner);
        if (forPlayer == null) {
            if (!create) {
                return null;
            }
            forPlayer = new EnumMap<ToolKind, State>(ToolKind.class);
            this.states.put(owner, forPlayer);
        }
        State state = forPlayer.get(kind);
        if (state == null && create) {
            state = new State();
            forPlayer.put(kind, state);
        }
        return state;
    }

    /** Un niveau lisible, quoi que le fichier contienne : 0 = « rien d'acheté », jamais une exception. */
    private static int asInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (RuntimeException malformed) {
            return 0;
        }
    }

    /** Le nom de clé utilisé dans le YAML. */
    public static String name(ToolKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private static UUID parse(String text) {
        try {
            return UUID.fromString(text);
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}
