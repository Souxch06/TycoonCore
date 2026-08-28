package xyz.arcadiadevs.valoriatycoon.hologram;

/**
 * Corps du constructeur d'un hologramme : le code passé à
 * {@link HologramBuilder#hologram(org.bukkit.Location, HologramSetupGroup)} est exécuté pendant la
 * création, et chaque appel à {@link HologramBuilder#textline(String, Object...)} ou
 * {@link HologramBuilder#item(org.bukkit.inventory.ItemStack)} ajoute une partie à l'hologramme en
 * cours de construction.
 *
 * <p>Interface fonctionnelle : le bytecode livré par le plugin passe ces blocs sous forme de
 * lambda {@code () -> { ... }} sans valeur de retour, d'où la méthode unique {@code void run()}.</p>
 */
@FunctionalInterface
public interface HologramSetupGroup {

    void run();
}
