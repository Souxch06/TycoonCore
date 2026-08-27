package xyz.arcadiadevs.valoriatycoon.hologram;

/**
 * Fabrique paresseuse d'un hologramme, telle que l'utilise le bytecode livré :
 * {@code pool.registerHolograms(() -> hologram[0] = HologramBuilder.hologram(...))}.
 *
 * <p>Le plugin encapsule le résultat dans un tableau d'une case (idiome de lambda qui doit rester
 * « effectively final ») : l'interface ne renvoie donc rien, c'est le callback qui remplit la case.
 * Le pool appelle {@link #run()} une seule fois, immédiatement, sur le fil principal.</p>
 */
@FunctionalInterface
public interface HologramRegisterGroup {

    void run();
}
