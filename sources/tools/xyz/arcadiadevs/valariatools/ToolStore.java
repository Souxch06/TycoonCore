package xyz.arcadiadevs.valariatools;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Les paliers achetés, par joueur et par âme d'outil — dans <code>plugins/ValoriaTools/tools.yml</code>.
 *
 * <h2>Pourquoi le palier est stocké côté serveur et pas dans l'item</h2>
 * <p>Un multi-outil qui garde sa force <em>dans</em> son item (NBT / PersistentDataContainer) est un
 * ticket de loterie : on le dépose dans un coffre, on le donne, on le vend, et le palier change de
 * main — ou disparaît quand l'item casse. Stocké par UUID, le palier est impossible à dupliquer et
 * survit à la perte de l'item. C'est le même contrat que le système d'économie interne : un fichier,
 * écrit de façon <b>atomique</b> (fichier temporaire puis <code>ATOMIC_MOVE</code>), donc jamais un
 * YAML à moitié écrit après un arrêt brutal qui remettrait tout le monde au palier 1.</p>
 *
 * <p>Les paliers sont écrits en minuscules (<code>pickaxe</code>, <code>rod</code>…) : un nom de clé
 * lisible à la main par un admin vaut plus qu'une symétrie avec l'enum.</p>
 */
public final class ToolStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<ToolKind, Integer>> tiers =
            new LinkedHashMap<UUID, Map<ToolKind, Integer>>();
    private boolean dirty;

    public ToolStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tools.yml");
    }

    /** Lit le fichier ; une clé illisible vaut palier 1, jamais une exception. */
    public void load() {
        this.tiers.clear();
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
            Map<ToolKind, Integer> forPlayer = new EnumMap<ToolKind, Integer>(ToolKind.class);
            for (ToolKind kind : ToolKind.values()) {
                int tier = yaml.getInt(key + "." + name(kind), 1);
                if (tier < 1) {
                    tier = 1;   // un palier 0 ou negatif casserait toute la logique de deblocage
                }
                forPlayer.put(kind, Integer.valueOf(tier));
            }
            this.tiers.put(uuid, forPlayer);
        }
    }

    /** Écrit uniquement si quelque chose a changé (evite un disque touché pour rien à chaque palier). */
    public void save() {
        if (!this.dirty) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<ToolKind, Integer>> entry : this.tiers.entrySet()) {
            for (Map.Entry<ToolKind, Integer> tier : entry.getValue().entrySet()) {
                if (tier.getValue().intValue() > 1) {
                    yaml.set(entry.getKey() + "." + name(tier.getKey()), tier.getValue());
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
        if (owner == null || kind == null) {
            return 1;
        }
        Map<ToolKind, Integer> forPlayer = this.tiers.get(owner);
        if (forPlayer == null) {
            return 1;
        }
        Integer stored = forPlayer.get(kind);
        if (stored == null) {
            return 1;
        }
        int tier = stored.intValue();
        if (tier < 1) {
            return 1;
        }
        return Math.min(tier, Math.max(1, maxTier));
    }

    /** Applique un palier (et sauvegarde tout de suite : un palier payé ne se perd pas). */
    public void setTier(Player player, ToolKind kind, int tier, int maxTier) {
        if (player == null || kind == null) {
            return;
        }
        Map<ToolKind, Integer> forPlayer = this.tiers.get(player.getUniqueId());
        if (forPlayer == null) {
            forPlayer = new EnumMap<ToolKind, Integer>(ToolKind.class);
            this.tiers.put(player.getUniqueId(), forPlayer);
        }
        forPlayer.put(kind, Integer.valueOf(Math.max(1, Math.min(tier, Math.max(1, maxTier)))));
        this.dirty = true;
        save();
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
