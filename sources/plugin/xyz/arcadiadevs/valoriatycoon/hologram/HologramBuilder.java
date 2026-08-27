package xyz.arcadiadevs.valoriatycoon.hologram;

import java.util.ArrayDeque;
import java.util.Deque;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Constructeur d'hologrammes, appelé en style statique par le code déjà compilé du plugin :
 *
 * <pre>{@code
 * HologramBuilder.hologram(location, () -> {
 *     HologramBuilder.textline("&aNom", new Object[0]);
 *     HologramBuilder.item(new ItemStack(material));
 * });
 * }</pre>
 *
 * <p>Le bloc n'étant pas fourni sous forme d'objet à manipuler, les lignes s'ajoutent à
 * « l'hologramme en cours de construction », suivi dans une pile : un bloc peut légitimement être
 * imbriqué (un hologramme créé depuis le setup d'un autre), la pile garantit que chaque ligne va au
 * bon destinataire. La pile est vidée dans un {@code finally}, donc une exception dans le bloc ne
 * laisse pas un hologramme fantôme comme contexte pour les appels suivants.</p>
 */
public final class HologramBuilder {

    private static final ThreadLocal<Deque<Hologram>> BUILDING = new ThreadLocal<Deque<Hologram>>();

    private HologramBuilder() {
    }

    /**
     * Construit l'hologramme décrit par {@code group}. Aucune entité n'est encore créée : c'est le
     * pool qui le fait, pour que le nettoyage et la persistance aient un seul point de passage.
     *
     * @return l'hologramme, ou {@code null} si aucun pool n'est actif (hologrammes désactivés)
     */
    public static Hologram hologram(Location location, HologramSetupGroup group) {
        HologramPool pool = HoloEasy.activePool();
        if (location == null || pool == null) {
            return null;
        }
        Hologram hologram = new Hologram(null, location, null, null);
        Deque<Hologram> stack = BUILDING.get();
        if (stack == null) {
            stack = new ArrayDeque<Hologram>();
            BUILDING.set(stack);
        }
        stack.push(hologram);
        try {
            if (group != null) {
                group.run();
            }
        } catch (RuntimeException ex) {
            pool.warn("ligne d'hologramme invalide : " + ex.getClass().getSimpleName() + " " + ex.getMessage());
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                BUILDING.remove();
            }
        }
        return pool.register(hologram);
    }

    /**
     * Ajoute une ligne de texte. Les codes {@code &} et {@code &#rrggbb} sont traduits ici, car le
     * nom affiché d'une entité n'est pas passé par le traducteur du plugin. Les paramètres varargs
     * sont consommés par paires de {@code \{\}} (forme utilisée par la bibliothèque d'origine) ; un
     * texte sans {@code \{\}} est affiché tel quel.
     */
    public static void textline(String text, Object... placeholders) {
        Hologram target = current();
        if (target == null) {
            return;
        }
        String line = text == null ? "" : text;
        if (placeholders != null) {
            for (Object placeholder : placeholders) {
                line = line.replaceFirst("\\{\\}", java.util.regex.Matcher.quoteReplacement(
                        placeholder == null ? "" : String.valueOf(placeholder)));
            }
        }
        if (!line.isEmpty()) {
            target.addLine(HoloEasy.color(line));
        }
    }

    /**
     * Ajoute l'item au sommet de la pile. Seul le {@link Material} est retenu : un {@link ItemStack}
     * complet ne peut pas être rejoué après un redémarrage, et un hologramme ne doit jamais retenir
     * un objet réel en mémoire.
     */
    public static void item(ItemStack stack) {
        Hologram target = current();
        if (target == null) {
            return;
        }
        Material material = stack == null ? null : stack.getType();
        target.setItem(usableItem(material));
    }

    /** {@code null} quand aucun {@link #hologram} n'est en cours : l'appel est alors ignoré. */
    static Material usableItem(Material material) {
        if (material == null || material == Material.AIR) {
            return null;
        }
        try {
            return material.isItem() ? material : null;
        } catch (NoSuchMethodError | NoClassDefFoundError unsupported) {
            // Material#isItem a remplacé isEdible/est-intégrable selon les versions : si la
            // méthode manque, on fait confiance au demandeur plutôt que de casser l'affichage.
            return material;
        }
    }

    private static Hologram current() {
        Deque<Hologram> stack = BUILDING.get();
        Hologram target = stack == null ? null : stack.peek();
        if (target == null) {
            HoloEasy.debug("HologramBuilder.textline/item appele hors du bloc hologram(...) : ignore");
        }
        return target;
    }
}
