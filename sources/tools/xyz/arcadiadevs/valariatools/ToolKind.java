package xyz.arcadiadevs.valariatools;

import java.util.Locale;
import org.bukkit.Material;

/**
 * Les quatre « âmes » d'un même outil.
 *
 * <p>Chaque {@link ToolKind} possède ses propres paliers d'amélioration, ses capacités et ses
 * matériaux de blocs — c'est l'axe autour duquel tourne tout le plugin : un seul item, quatre
 * comportements, choisis par ce que le joueur regarde.</p>
 */
public enum ToolKind {

    /** Blocs à miner (minerais, roches, terres) : l'âme par défaut. */
    PICKAXE("pioche", Material.DIAMOND_PICKAXE),
    /** Bois et végétaux : abattage d'arbre, écorçage. */
    AXE("hache", Material.DIAMOND_AXE),
    /** Pêche : moulinet automatique, chance de trésor. */
    ROD("canne à pêche", Material.FISHING_ROD),
    /** Combat : critique, vol de vie, recul. */
    SWORD("épée", Material.DIAMOND_SWORD);

    private final String label;
    private final Material fallbackMaterial;

    ToolKind(String label, Material fallbackMaterial) {
        this.label = label;
        this.fallbackMaterial = fallbackMaterial;
    }

    /** Nom lisible, pour le chat et les interfaces. */
    public String label() {
        return this.label;
    }

    /** Matériau de secours quand la configuration ne précise rien. */
    public Material fallbackMaterial() {
        return this.fallbackMaterial;
    }

    /**
     * Résout un nom de configuration (`pickaxe`, `PIOCHE`, `canne`, `sword`…) en âme d'outil.
     *
     * @return l'âme correspondante, ou {@code null} si le nom ne désigne rien de connu
     */
    public static ToolKind parse(String text) {
        if (text == null) {
            return null;
        }
        String needle = text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return null;
        }
        for (ToolKind kind : values()) {
            if (kind.name().toLowerCase(Locale.ROOT).equals(needle) || kind.label.equals(needle)) {
                return kind;
            }
        }
        // Les joueurs francophones disent « pic » ou « canne » avant de dire « pickaxe » ou « rod ».
        if (needle.startsWith("pic") || needle.startsWith("min")) {
            return PICKAXE;
        }
        if (needle.startsWith("hach") || needle.startsWith("buch") || needle.startsWith("ax")) {
            return AXE;
        }
        if (needle.startsWith("canne") || needle.startsWith("pech") || needle.startsWith("rod")
                || needle.startsWith("fish")) {
            return ROD;
        }
        if (needle.startsWith("epee") || needle.startsWith("ép") || needle.startsWith("sword")
                || needle.startsWith("comb")) {
            return SWORD;
        }
        return null;
    }
}
