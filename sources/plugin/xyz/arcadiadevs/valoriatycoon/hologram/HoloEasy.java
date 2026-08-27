package xyz.arcadiadevs.valoriatycoon.hologram;

import java.util.List;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

/**
 * Point d'entrée des hologrammes du plugin — écriture maison, qui remplace la bibliothèque
 * embarquée d'origine (celle qui exigeait ProtocolLib, un plugin à installer).
 *
 * <p>Le bytecode livré de {@code ValoriaTycoon} appelle {@link #startInteractivePool} avec la
 * signature exacte de l'ancienne bibliothèque ; cette classe fournit donc la même forme, et rien
 * d'autre. Toutes ses méthodes sont écrites pour ne jamais faire échouer le plugin : un hologramme
 * qui ne peut pas s'afficher se traduit par un avertissement dans le log, jamais par une
 * {@code ExceptionInInitializerError} ni par une désactivation.</p>
 */
public final class HoloEasy {

    /** Marque une entité comme appartenant à un hologramme (reprise après redémarrage, purge). */
    static final NamespacedKey KEY_ENTITY;
    /** Marque l'hologramme lui-même (réservé aux diagnostics croisés avec block_data.json). */
    static final NamespacedKey KEY_HOLOGRAM;

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    static {
        NamespacedKey entity = null;
        NamespacedKey hologram = null;
        try {
            entity = new NamespacedKey("valoriatycoon", "hologram-entity");
            hologram = new NamespacedKey("valoriatycoon", "hologram");
        } catch (RuntimeException | NoClassDefFoundError unavailable) {
            // Sans PDC, les hologrammes restent fonctionnels mais ne sont plus retrouvables après
            // un redémarrage : dégradation acceptable, jamais un plantage.
        }
        KEY_ENTITY = entity;
        KEY_HOLOGRAM = hologram;
    }

    private static volatile HologramPool pool;

    private HoloEasy() {
    }

    /**
     * Ouvre le pool d'hologrammes, ou le renvoie s'il est déjà ouvert à l'identique : un
     * {@code /valoriatycoon reload} rejoue {@code loadHolograms()} et ne doit jamais créer un second
     * pool (deux écritures sur le même fichier, deux lots d'entités pour un générateur).
     *
     * <p>{@code textSpacing} et {@code itemSpacing} ne sont pas ignorés par hasard : ils font partie
     * de la signature appelée par le bytecode livré. Notre géométrie étant portée par des entités,
     * l'espacement réel vient de {@link HologramPool}.</p>
     *
     * @return le pool, jamais {@code null} : en cas d'erreur un pool inerte est renvoyé
     */
    public static HologramPool startInteractivePool(Plugin plugin, double viewDistance, float textSpacing,
            float itemSpacing) {
        HologramPool current = pool;
        if (current != null && current.owns(plugin, viewDistance)) {
            return current;
        }
        HologramPool created = new HologramPool(plugin, viewDistance);
        pool = created;
        created.start();
        return created;
    }

    /** Ferme le pool : entités retirées du monde puis rendues non persistantes. */
    public static void stopPool() {
        HologramPool current = pool;
        pool = null;
        if (current != null) {
            current.shutdown();
        }
    }

    /** Le pool actif, résolu du côté de {@code ValoriaTycoon} si la statique a été perdue. */
    public static HologramPool activePool() {
        HologramPool current = pool;
        if (current != null) {
            return current;
        }
        try {
            ValoriaTycoon instance = ValoriaTycoon.getInstance();
            if (instance == null) {
                return null;
            }
            current = instance.getHologramPool();
        } catch (RuntimeException | NoClassDefFoundError unavailable) {
            return null;
        }
        if (current != null) {
            pool = current;
        }
        return current;
    }

    /** Vrai si les hologrammes sont activés dans la configuration. */
    public static boolean enabled() {
        try {
            return Config.HOLOGRAMS_ENABLED.getBoolean();
        } catch (RuntimeException | NoClassDefFoundError unavailable) {
            return false;
        }
    }

    static void removeEntities(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        for (java.util.UUID entityId : hologram.entityIds()) {
            Entity entity = find(entityId);
            if (entity != null) {
                try {
                    entity.remove();
                } catch (RuntimeException ignored) {
                    // entité déjà partie : rien à faire
                }
            }
        }
        hologram.clearEntities();
    }

    /** Retrouve une entité par son UUID sans dépendre d'une méthode absente sur les vieux serveurs. */
    static Entity find(java.util.UUID entityId) {
        if (entityId == null) {
            return null;
        }
        ValoriaTycoon instance = instance();
        if (instance == null) {
            return null;
        }
        for (World world : instance.getServer().getWorlds()) {
            if (world == null) {
                continue;
            }
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                if (entityId.equals(entity.getUniqueId())) {
                    return entity;
                }
            }
        }
        return null;
    }

    static ValoriaTycoon instance() {
        try {
            return ValoriaTycoon.getInstance();
        } catch (RuntimeException | NoClassDefFoundError unavailable) {
            return null;
        }
    }

    /**
     * Traduit {@code &a}… et {@code &#rrggbb}. {@link #legacyHex} fait le travail des couleurs RVB à
     * la main : {@code net.md_5.bungee.api.ChatColor}, qu'utilise le traducteur embarqué, n'est plus
     * garanti présent sur les serveurs récents.
     */
    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String translated = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
        java.util.regex.Matcher matcher = HEX.matcher(translated);
        if (!matcher.find()) {
            return translated;
        }
        matcher.reset();
        StringBuilder out = new StringBuilder(translated.length());
        while (matcher.find()) {
            String replacement = legacyHex(matcher.group(1));
            if (replacement == null) {
                continue;
            }
            matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Une couleur RVB devient une chaîne de codes {@code §x§r§r§g§g§b§b}. */
    static String legacyHex(String rgb) {
        if (rgb == null || rgb.length() != 6) {
            return null;
        }
        StringBuilder out = new StringBuilder("§x");
        for (int i = 0; i < 6; i++) {
            out.append('§').append(Character.toLowerCase(rgb.charAt(i)));
        }
        return out.toString();
    }

    static void warn(String message) {
        ValoriaTycoon instance = instance();
        if (instance != null) {
            instance.getLogger().warning("[hologrammes] " + message);
            return;
        }
        java.util.logging.Logger.getLogger("ValoriaTycoon").warning("[hologrammes] " + message);
    }

    /** Trace de diagnostic : uniquement avec {@code developer-options: true}. */
    static void debug(String message) {
        try {
            if (!Config.DEVELOPER_OPTIONS.getBoolean()) {
                return;
            }
        } catch (RuntimeException | NoClassDefFoundError unavailable) {
            return;
        }
        warn("[debug] " + message);
    }

    /** Écrit une marque sur une entité, sans jamais propager une exception. */
    static void tag(Entity entity, NamespacedKey key, String value) {
        if (entity == null || key == null || value == null) {
            return;
        }
        try {
            entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unsupported) {
            // pas de PDC : le hologramme s'affiche, il n'est juste pas retrouvé après un redémarrage
        }
    }

    static String readTag(Entity entity, NamespacedKey key) {
        if (entity == null || key == null) {
            return null;
        }
        try {
            return entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unsupported) {
            return null;
        }
    }

    static void untag(Entity entity, NamespacedKey key) {
        if (entity == null || key == null) {
            return;
        }
        try {
            entity.getPersistentDataContainer().remove(key);
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unsupported) {
            // rien à faire
        }
    }

    /** Congèle une armure (pas de collision, pas d'interaction) quand le serveur sait le faire. */
    static void freeze(Entity entity) {
        if (!(entity instanceof ArmorStand)) {
            return;
        }
        ArmorStand stand = (ArmorStand) entity;
        try {
            stand.setTicksFrozen(true);
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError legacy) {
            // méthode apparue en 1.19 : sur un serveur plus ancien, l'hologramme reste simplement
            // visible mais non interactif grâce a setInvulnerable + setSilent
        }
    }

    static Location parseLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        ValoriaTycoon instance = instance();
        if (instance == null || worldName == null) {
            return null;
        }
        World world = instance.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    /** Le matériau de l'item sommital, résolu depuis son nom persisté. */
    static Material material(String name) {
        if (name == null || name.isEmpty() || "-".equals(name) || name.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return HologramBuilder.usableItem(Material.matchMaterial(name));
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unknown) {
            return null;
        }
    }

    /** Liste de lignes immuable vide : évite un {@code null} à propager dans le rendu. */
    static List<String> noLines() {
        return java.util.Collections.emptyList();
    }
}
