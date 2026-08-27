package xyz.arcadiadevs.valoriatycoon.hologram;

import java.util.ArrayList;
import java.util.Collections;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Un hologramme : une pile d'entités (portées par le serveur, pas de paquet maison) posée au-dessus
 * d'un bloc, avec une ligne de texte chacune et éventuellement un item au sommet.
 *
 * <h2>Pourquoi cette classe existe</h2>
 * <p>La bibliothèque embarquée d'origine pilotait les hologrammes en envoyant des paquets via
 * <b>ProtocolLib</b>, un plugin tiers à installer. Ce paquet est écrit
 * ici avec la seule API Bukkit : rien n'est ajouté dans <code>plugins/</code>, et l'affichage reste
 * correct sur les serveurs 26.x où l'ancienne approche ne trouvait plus ses classes.</p>
 *
 * <p>Un hologramme est identifié par son {@link #getId() UUID} : c'est cette valeur que le plugin
 * persiste dans <code>block_data.json</code> (champ {@code hologramId} d'un emplacement de
 * générateur), donc l'identifiant doit rester stable et la lookup doit tolérer un hologramme
 * disparu.</p>
 */
public final class Hologram {

    private final UUID id;
    private final List<String> lines = new ArrayList<String>();
    private final Set<UUID> entities = new LinkedHashSet<UUID>();
    private Location location;
    private Material item;
    private boolean dirty;
    private HologramPool pool;

    Hologram(UUID id, Location location, List<String> lines, Material item) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.location = location == null ? null : location.clone();
        if (lines != null) {
            this.lines.addAll(lines);
        }
        this.item = item;
    }

    /** L'identifiant persisté par le plugin dans <code>block_data.json</code>. */
    public UUID getId() {
        return this.id;
    }

    public Location getLocation() {
        return this.location == null ? null : this.location.clone();
    }

    public List<String> getLines() {
        return Collections.unmodifiableList(this.lines);
    }

    public Material getItem() {
        return this.item;
    }

    /**
     * Remplace les lignes et redessine l'hologramme. Réservé au pool et aux reconstructions depuis
     * le fichier : un appelant du plugin ne doit pas recréer l'objet, il perdrait l'identifiant
     * persisté dans <code>block_data.json</code>.
     */
    void setLines(List<String> lines) {
        this.lines.clear();
        if (lines != null) {
            this.lines.addAll(lines);
        }
        this.dirty = true;
    }

    /** Ajoute une ligne pendant la construction (voir {@link HologramBuilder#textline}). */
    void addLine(String line) {
        this.lines.add(line);
        this.dirty = true;
    }

    /** {@code true} quand les entités ne reflètent pas encore le contenu voulu. */
    boolean isDirty() {
        return this.dirty;
    }

    void markClean() {
        this.dirty = false;
    }

    void setLocation(Location location) {
        this.location = location == null ? null : location.clone();
    }

    void setItem(Material material) {
        this.item = material;
    }

    void setPool(HologramPool pool) {
        this.pool = pool;
    }

    Set<UUID> entityIds() {
        return this.entities;
    }

    void addEntity(UUID entityId) {
        this.entities.add(entityId);
    }

    void clearEntities() {
        this.entities.clear();
    }

    /**
     * Retire les entités et désinscrit l'hologramme de son pool. Appeler cette méthode plutôt que
     * {@code pool.remove(...)} quand on tient déjà l'objet : le pool de {@code ValoriaTycoon} peut
     * avoir été remplacé entre-temps (rechargement de la configuration).
     */
    public void remove() {
        HologramPool owner = this.pool;
        if (owner != null) {
            owner.remove(this.id);
            return;
        }
        HoloEasy.removeEntities(this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Hologram && ((Hologram) other).id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "Hologram{id=" + this.id + ", lines=" + this.lines.size()
                + ", location=" + (this.location == null ? "aucune" : HologramPool.describe(this.location)) + "}";
    }
}
