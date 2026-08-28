package xyz.arcadiadevs.valoriatycoon.hologram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;

/**
 * Ensemble des hologrammes du serveur, avec leur sauvegarde dans
 * <code>plugins/ValoriaTycoon/holograms.txt</code>.
 *
 * <h2>Pourquoi un fichier séparé</h2>
 * <p>Le plugin ne persiste qu'un {@code hologramId} par emplacement de générateur : sans contenu à
 * rejouer, un hologramme disparaît silencieusement au premier redémarrage. Les lignes étant résolues
 * depuis une configuration que le plugin recharge lui-même, ce pool en garde une copie et, surtout,
 * retrouve les entités encore présentes dans le monde (marquées en PDC) au lieu d'en dupliquer.
 * Toute erreur est répercutée dans le log, jamais jetée : un hologramme n'est pas une raison de
 * refuser de charger un serveur.</p>
 *
 * <h2>Contrat avec le bytecode livré</h2>
 * <p>{@code ValoriaTycoon} et {@code HologramsUtil} appellent exactement {@link #registerHolograms},
 * {@link #get(UUID)} et {@link #remove(UUID)} ; ces trois signatures ne doivent pas bouger.</p>
 */
public final class HologramPool {

    /** Écart vertical entre deux lignes : une tête d'armure habillée de texte fait ~0,3 bloc. */
    static final double LINE_HEIGHT = 0.30D;
    /** Le texte est posé au ras du bloc, l'item au-dessus de la dernière ligne. */
    static final double BASE_OFFSET = 0.15D;

    private final Plugin plugin;
    private final double viewDistance;
    private final Map<UUID, Hologram> holograms = new LinkedHashMap<UUID, Hologram>();
    private final Set<UUID> warned = new LinkedHashSet<UUID>();
    private final HologramStore store;
    private boolean started;
    private int failures;

    HologramPool(Plugin plugin, double viewDistance) {
        this.plugin = plugin;
        this.viewDistance = viewDistance;
        this.store = new HologramStore(plugin);
    }

    /** Vrai si ce pool a déjà été ouvert pour ce plugin et cette portée de vue. */
    boolean owns(Plugin other, double distance) {
        return this.started && other != null && other == this.plugin
                && Math.abs(distance - this.viewDistance) < 1.0E-6D;
    }

    /**
     * Ouvre le pool : reprise du fichier, puis des entités encore vivantes, puis purge des entités
     * orphelines (hologramme disparu du fichier mais armure restée dans le monde).
     */
    void start() {
        if (this.started) {
            return;
        }
        this.started = true;
        try {
            for (Hologram restored : this.store.load()) {
                this.holograms.put(restored.getId(), restored);
                restored.setPool(this);
            }
            adopt();
            sweepOrphans();
            int visible = 0;
            for (Hologram hologram : this.holograms.values()) {
                if (!hologram.entityIds().isEmpty()) {
                    visible++;
                }
            }
            this.plugin.getLogger().info("hologrammes : " + this.holograms.size() + " enregistré(s), "
                    + visible + " deja visible(s), portee de vue " + Math.round(this.viewDistance) + " bloc(s).");
        } catch (RuntimeException | LinkageError broken) {
            this.plugin.getLogger().warning("hologrammes desactives pour cette session : " + broken);
        }
    }

    /** Enregistre (ou remplace) un hologramme et le dessine. */
    Hologram register(Hologram hologram) {
        if (hologram == null) {
            return null;
        }
        this.holograms.put(hologram.getId(), hologram);
        hologram.setPool(this);
        render(hologram);
        save();
        return hologram;
    }

    /**
     * Fabrique l'hologramme décrit par le bloc du plugin, puis l'enregistre. Le bloc écrit le
     * résultat dans la variable locale que le plugin a lui-même choisie, d'où le {@code void}.
     */
    public void registerHolograms(HologramRegisterGroup group) {
        if (group == null) {
            return;
        }
        try {
            group.run();
        } catch (RuntimeException | LinkageError broken) {
            warn("creation d'hologramme ignoree : " + broken.getClass().getSimpleName() + " " + broken.getMessage());
        }
    }

    /** Hologramme par identifiant persisté ; recrée les entités si elles ont disparu. */
    public Hologram get(UUID id) {
        if (id == null) {
            return null;
        }
        Hologram hologram = this.holograms.get(id);
        if (hologram == null) {
            return null;
        }
        if (hologram.entityIds().isEmpty()) {
            render(hologram);
        }
        return hologram;
    }

    /** Retire l'hologramme de la sauvegarde, du monde et de la liste. */
    public Hologram remove(UUID id) {
        if (id == null) {
            return null;
        }
        Hologram hologram = this.holograms.remove(id);
        if (hologram == null) {
            return null;
        }
        HoloEasy.removeEntities(hologram);
        hologram.setPool(null);
        this.warned.remove(id);
        save();
        return hologram;
    }

    public int size() {
        return this.holograms.size();
    }

    public Collection<Hologram> values() {
        return Collections.unmodifiableCollection(new ArrayList<Hologram>(this.holograms.values()));
    }

    /** Redessine un hologramme dont le contenu a changé. */
    void redraw(Hologram hologram) {
        HoloEasy.removeEntities(hologram);
        render(hologram);
        save();
    }

    /** Fermeture : les entités sont retirées et rendues non persistantes, pour un serveur propre. */
    void shutdown() {
        for (Hologram hologram : this.holograms.values()) {
            for (UUID entityId : hologram.entityIds()) {
                Entity entity = HoloEasy.find(entityId);
                if (entity == null) {
                    continue;
                }
                release(entity);
                try {
                    entity.remove();
                } catch (RuntimeException ignored) {
                    // déjà retirée
                }
            }
            hologram.clearEntities();
        }
        save();
        this.holograms.clear();
        this.started = false;
    }

    private void render(Hologram hologram) {
        Location base = hologram.getLocation();
        if (base == null || base.getWorld() == null) {
            return;
        }
        List<String> lines = hologram.getLines();
        if (lines.isEmpty() && hologram.getItem() == null) {
            return;
        }
        HoloEasy.removeEntities(hologram);
        World world = base.getWorld();
        double y = base.getY() + BASE_OFFSET;
        for (String line : lines) {
            Location spot = base.clone();
            spot.setY(y);
            if (!spawnLine(world, spot, line, hologram)) {
                break;
            }
            y += LINE_HEIGHT;
        }
        Material item = hologram.getItem();
        if (item != null) {
            Location spot = base.clone();
            spot.setY(y);
            spawnItem(world, spot, item, hologram);
        }
        hologram.markClean();
    }

    private boolean spawnLine(World world, Location location, String text, Hologram hologram) {
        try {
            Entity spawned = world.spawnEntity(location, EntityType.ARMOR_STAND);
            if (!(spawned instanceof ArmorStand)) {
                return false;
            }
            ArmorStand stand = (ArmorStand) spawned;
            configure(stand, hologram);
            stand.setCustomNameVisible(true);
            stand.setCustomName(text);
            hologram.addEntity(stand.getUniqueId());
            return true;
        } catch (RuntimeException | LinkageError failed) {
            fail(hologram, "ligne '" + plain(text) + "' : " + failed.getClass().getSimpleName() + " " + failed.getMessage());
            return false;
        }
    }

    private boolean spawnItem(World world, Location location, Material material, Hologram hologram) {
        try {
            Entity spawned = world.spawnEntity(location, EntityType.ARMOR_STAND);
            if (!(spawned instanceof ArmorStand)) {
                return false;
            }
            ArmorStand stand = (ArmorStand) spawned;
            configure(stand, hologram);
            if (hold(stand, material)) {
                hologram.addEntity(stand.getUniqueId());
                return true;
            }
            // l'item n'a pas pu être tenu : la dernière ligne de texte porte déjà le nom du bloc,
            // on retire l'armure vide plutôt que de laisser un fantôme immobile
            stand.remove();
            return false;
        } catch (RuntimeException | LinkageError failed) {
            fail(hologram, "item " + material + " : " + failed.getClass().getSimpleName() + " " + failed.getMessage());
            return false;
        }
    }

    private static boolean hold(ArmorStand stand, Material material) {
        try {
            stand.setCanPickupItems(false);
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError legacy) {
            // Livres antérieurs a 1.14 : rien a desactiver
        }
        try {
            stand.setArms(true);
            stand.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(material));
            return true;
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unsupported) {
            return false;
        }
    }

    /** Réglages communs : invisible, immobile, invincible, muet. */
    private void configure(ArmorStand stand, Hologram hologram) {
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setCustomNameVisible(false);
        optional(stand, "setSilent", true);
        optional(stand, "setInvulnerable", true);
        optional(stand, "setPersistent", true);
        optional(stand, "setRemoveWhenFarAway", false);
        optional(stand, "setCanTick", false);
        HoloEasy.freeze(stand);
        HoloEasy.tag(stand, HoloEasy.KEY_ENTITY, hologram.getId().toString());
    }

    /**
     * Applique un réglage apparu à une version donnée du serveur. Un {@code NoSuchMethodError} est
     * attendu sur les anciens serveurs (le plugin vise 1.7 en référence) : il se traduit par
     * l'absence du réglage, pas par un échec d'affichage.
     */
    private static void optional(ArmorStand stand, String setter, boolean value) {
        try {
            java.lang.reflect.Method method = stand.getClass().getMethod(setter, boolean.class);
            method.invoke(stand, Boolean.valueOf(value));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError absent) {
            // non supporte : ignore volontairement
        }
    }

    /** Relie les armures encore vivantes du monde aux hologrammes de la sauvegarde. */
    private void adopt() {
        if (this.holograms.isEmpty() || HoloEasy.KEY_ENTITY == null) {
            return;
        }
        Map<String, Hologram> byId = new LinkedHashMap<String, Hologram>();
        for (Hologram hologram : this.holograms.values()) {
            byId.put(hologram.getId().toString(), hologram);
        }
        for (World world : worlds()) {
            if (world == null) {
                continue;
            }
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                String owner = HoloEasy.readTag(stand, HoloEasy.KEY_ENTITY);
                if (owner == null) {
                    continue;
                }
                Hologram hologram = byId.get(owner);
                if (hologram == null) {
                    continue;
                }
                HoloEasy.freeze(stand);
                hologram.addEntity(stand.getUniqueId());
            }
        }
    }

    /** Retire les armures marquées qui ne correspondent plus à aucun hologramme. */
    private void sweepOrphans() {
        if (HoloEasy.KEY_ENTITY == null) {
            return;
        }
        for (World world : worlds()) {
            if (world == null) {
                continue;
            }
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                String owner = HoloEasy.readTag(stand, HoloEasy.KEY_ENTITY);
                if (owner == null || this.holograms.containsKey(parse(owner))) {
                    continue;
                }
                release(stand);
                try {
                    stand.remove();
                } catch (RuntimeException ignored) {
                    // entité déjà partie
                }
            }
        }
    }

    /** Rend une entité ordinaire : plus de marque, plus de persistance, elle peut être nettoyée. */
    private static void release(Entity entity) {
        optional(entity, "setPersistent", false);
        try {
            entity.setRemoveWhenFarAway(true);
        } catch (RuntimeException ignored) {
            // deja marque
        }
        HoloEasy.untag(entity, HoloEasy.KEY_ENTITY);
        HoloEasy.untag(entity, HoloEasy.KEY_HOLOGRAM);
    }

    private static UUID parse(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException | RuntimeException malformed) {
            return null;
        }
    }

    private List<World> worlds() {
        ValoriaTycoon instance = HoloEasy.instance();
        if (instance == null) {
            return Collections.emptyList();
        }
        return new ArrayList<World>(instance.getServer().getWorlds());
    }

    private void save() {
        try {
            this.store.save(this.holograms.values());
        } catch (RuntimeException broken) {
            warn("sauvegarde des hologrammes impossible : " + broken.getMessage());
        }
    }

    private void fail(Hologram hologram, String detail) {
        this.failures++;
        if (this.failures > 64) {
            if (this.failures == 65) {
                warn("trop d'erreurs d'hologrammes, la suite n'est plus journalisee (derniere : " + detail + ")");
            }
            return;
        }
        if (hologram != null && !this.warned.add(hologram.getId())) {
            return;
        }
        warn("hologramme incomplet (" + detail + ")");
    }

    void warn(String message) {
        if (this.plugin != null) {
            this.plugin.getLogger().warning("[hologrammes] " + message);
            return;
        }
        HoloEasy.warn(message);
    }

    static String describe(Location location) {
        if (location == null) {
            return "aucune";
        }
        return location.getWorld() == null ? "sans monde" : location.getWorld().getName()
                + " " + (int) location.getX() + "," + (int) location.getY() + "," + (int) location.getZ();
    }

    private static String plain(String text) {
        return text == null ? "" : text.replaceAll("§.", "");
    }
}
