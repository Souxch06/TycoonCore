package xyz.arcadiadevs.valoriatycoon.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.hologram.HoloEasy;
import xyz.arcadiadevs.valoriatycoon.hologram.Hologram;
import xyz.arcadiadevs.valoriatycoon.hologram.HologramBuilder;
import xyz.arcadiadevs.valoriatycoon.hologram.HologramPool;
import xyz.arcadiadevs.valoriatycoon.utils.ServerVersion;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

/**
 * Fabrique d'hologrammes au-dessus des générateurs.
 *
 * <p>Le nom de la classe et ses trois signatures publiques
 * ({@link #createHologram(Location, List, Material)}, {@link #getHologram(String)},
 * {@link #removeHologram(Hologram)}) sont imposées par les classes déjà compilées du paquet
 * {@code artifacts/extracted} : elles ne doivent pas changer, sous peine de
 * {@code NoSuchMethodError} en jeu. Le contenu, lui, est écrit ici — la bibliothèque embarquée
 * d'origine envoyait des paquets via ProtocolLib, un plugin tiers.</p>
 */
public class HologramsUtil {

    private HologramsUtil() {
    }

    /**
     * Crée (et affiche) l'hologramme d'un générateur. Renvoie {@code null} sans erreur quand les
     * hologrammes sont désactivés ou que le serveur refuse l'entité : un hologramme absent ne doit
     * jamais empêcher un générateur de fonctionner.
     *
     * @param location centre du générateur ; l'hologramme est posé un bloc plus bas, comme avant
     * @param list     lignes déjà traduites (placeholders remplacés) par l'appelant
     * @param material item à montrer au sommet, ou {@code null}
     */
    public static Hologram createHologram(Location location, List<String> list, Material material) {
        if (!Config.HOLOGRAMS_ENABLED.getBoolean() || location == null) {
            return null;
        }
        HologramPool pool = HoloEasy.activePool();
        if (pool == null) {
            return null;
        }
        final List<String> lines = list == null ? Collections.<String>emptyList()
                : new ArrayList<String>(list);
        final Material item = material;
        final Location base = location.clone().subtract(0.0D, 1.0D, 0.0D);
        final Hologram[] created = new Hologram[1];
        pool.registerHolograms(new xyz.arcadiadevs.valoriatycoon.hologram.HologramRegisterGroup() {

            @Override
            public void run() {
                created[0] = HologramBuilder.hologram(base, new xyz.arcadiadevs.valoriatycoon.hologram.HologramSetupGroup() {

                    @Override
                    public void run() {
                        for (String line : lines) {
                            HologramBuilder.textline(line, new Object[0]);
                        }
                        if (item != null && ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
                            HologramBuilder.item(new ItemStack(item));
                        }
                    }
                });
            }
        });
        return created[0];
    }

    /** Retrouve l'hologramme persisté par un emplacement de générateur, ou {@code null}. */
    public static Hologram getHologram(String string) {
        if (string == null) {
            return null;
        }
        HologramPool pool = HoloEasy.activePool();
        if (pool == null) {
            return null;
        }
        UUID id;
        try {
            id = UUID.fromString(string);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
        return pool.get(id);
    }

    /** Retire un hologramme ; {@code null} est ignoré, comme avant. */
    public static void removeHologram(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        ValoriaTycoon instance = ValoriaTycoon.getInstance();
        HologramPool pool = instance == null ? null : instance.getHologramPool();
        if (pool != null) {
            pool.remove(hologram.getId());
        } else {
            hologram.remove();
        }
    }
}
