package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Les capacités elles-mêmes : ce que le palier débloque, et comment ça se matérialise.
 *
 * <h2>Volontairement borné</h2>
 * <p>Une capacité n'est pas un script : c'est une clé connue, avec des valeurs. Ajouter une capacité
 * vraiment nouvelle demande donc du Java — c'est le prix d'un système qui ne peut pas faire n'importe
 * quoi (un <code>VEIN</code> sans plafond, c'est un serveur qui s'écroule). En revanche <b>les valeurs,
 * le palier d'ouverture et l'âme concernée</b> sont 100 % configuration, ce qui suffit à reproduire un
 * barème existant.</p>
 *
 * <h2>Toutes les écritures passent par le listener</h2>
 * <p>Les méthodes de cassure de bloc sont conçues pour être appelées depuis un événement annulé :
 * elles posent l'air, jouent l'effet, et rendent les <em>drops calculés</em> plutôt que de laisser le
 * serveur le faire. Cela évite les deux accidents classiques : doubler les drops, ou les perdre quand
 * le bloc est retiré par une méthode qui ne les calcule pas.</p>
 */
public final class Abilities {

    /** Les clés comprises par le moteur. Toute autre clé est refusée au chargement, pas ignorée. */
    public static final Set<String> SUPPORTED = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "VEIN", "TREE_FELL", "AUTO_SMELT", "FORTUNE", "DOUBLE_DROP", "INFINITE_DURABILITY",
            "AUTO_REEL", "LUCK", "CRIT", "LIFE_STEAL", "KNOCKBACK", "INSTANT_BREAK", "SELL_ON_BREAK")));

    /** Les six voisins d'un bloc, dans un ordre stable (le parcours d'un filon n'en est que plus reproductible). */
    private static final BlockFace[] NEIGHBOURS = new BlockFace[]{
        BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private final JavaPlugin plugin;
    private final BlockMatcher matcher;
    private final ToolsConfig config;
    private final Set<String> visited = new HashSet<String>();
    private int budget;

    public Abilities(JavaPlugin plugin, BlockMatcher matcher, ToolsConfig config) {
        this.plugin = plugin;
        this.matcher = matcher;
        this.config = config;
    }

    /** Liste des capacités comprises, pour l'aide et le contrôle de config. */
    public static List<String> known() {
        return new ArrayList<String>(SUPPORTED);
    }

    // ------------------------------------------------------------------ selection de blocs

    /**
     * Les blocs d'un même filon, à partir du bloc cassé, plafonné par {@code max-blocks} du palier.
     *
     * <p>Le parcours est <b>itératif avec un budget global</b> (pas une récursion) : un filon de fer
     * malformé ou un bloc collé à une structure de 10 000 pierres identiques ne doit pas pouvoir
     * faire exploser la pile Java ni le tick.</p>
     *
     * @param similar true pour n'accepter que le même matériau (minerais), false pour « tout ce que
     *                l'outil mine » (utile sur la pierre/terre des tycoons)
     */
    public List<Block> vein(Block start, ToolsConfig.Ability ability, int tier, boolean similar) {
        int max = Math.max(1, ability.valueAt("max-blocks", tier - 1, 16));
        List<Block> out = new ArrayList<Block>();
        this.visited.clear();
        this.budget = max * 8;              // le nombre de cases examinees, pas seulement celles retenues
        List<Block> queue = new ArrayList<Block>();
        queue.add(start);
        this.visited.add(key(start));
        while (!queue.isEmpty() && out.size() < max && this.budget-- > 0) {
            Block current = queue.remove(0);
            if (!accepts(current, start, similar)) {
                continue;
            }
            out.add(current);
            for (BlockFace face : NEIGHBOURS) {
                Block neighbour;
                try {
                    neighbour = current.getRelative(face);
                } catch (IllegalArgumentException broken) {
                    continue;   // bloc hors du monde charge
                }
                String key = key(neighbour);
                if (this.visited.contains(key) || !accepts(neighbour, start, similar)) {
                    continue;
                }
                this.visited.add(key);
                queue.add(neighbour);
            }
        }
        return out;
    }

    /** Le tronc et la canopée d'un arbre, à partir du bloc frappé. */
    public List<Block> tree(Block start, ToolsConfig.Ability ability, int tier) {
        int max = Math.max(1, ability.valueAt("max-blocks", tier - 1, 64));
        int height = Math.max(1, ability.valueAt("max-height", tier - 1, 12));
        List<Block> out = new ArrayList<Block>();
        this.visited.clear();
        this.budget = max * 4;
        // 1) le tronc, vers le haut
        Block cursor = start;
        int grown = 0;
        while (grown < height && this.budget-- > 0 && isLog(cursor)) {
            out.add(cursor);
            cursor = cursor.getRelative(BlockFace.UP);
            grown++;
        }
        // 2) la canopee, autour du sommet atteint
        List<Block> queue = new ArrayList<Block>();
        queue.add(cursor);
        this.visited.add(key(cursor));
        while (!queue.isEmpty() && out.size() < max && this.budget-- > 0) {
            Block current = queue.remove(0);
            if (!isWood(current)) {
                continue;
            }
            out.add(current);
            for (BlockFace face : NEIGHBOURS) {
                Block neighbour;
                try {
                    neighbour = current.getRelative(face);
                } catch (IllegalArgumentException broken) {
                    continue;
                }
                if (this.visited.contains(key(neighbour))) {
                    continue;
                }
                this.visited.add(key(neighbour));
                queue.add(neighbour);
            }
        }
        return out;
    }

    /** Le bloc appartient-il à l'âme d'outil qui le frappe ? (utilisé par le filon et par le veto de minage) */
    private boolean accepts(Block block, Block start, boolean similar) {
        if (block == null || block.getType() == Material.AIR) {
            return false;
        }
        ToolKind kind = this.matcher.kindOf(block);
        if (kind == null || kind != this.matcher.kindOf(start)) {
            return false;
        }
        return !similar || block.getType() == start.getType();
    }

    /**
     * Un bloc de tronc. Les noms sont testes en minuscules et sans passer par un tag : sur un
     * serveur ancien (le plugin vise 1.7 en reference) `Tag.LOG` n'existe pas, et un arbre doit
     * continuer a tomber. Les types de bois reels (log/wood/stem/hyphae) sont enumeres plutot que
     * cherches par `contains("wood")` pour que `STRIPPED_*` et `CRIMSON_HYPHAE` restent corrects.
     */
    private static boolean isLog(Block block) {
        if (block == null || block.getType() == Material.AIR) {
            return false;
        }
        String name = nameOf(block);
        return name.contains("log") || name.contains("wood") || name.contains("stem")
                || name.contains("hyphae");
    }

    /** Ce qui peut etre emporte avec le tronc : le bois et la canopee. */
    private static boolean isWood(Block block) {
        if (block == null || block.getType() == Material.AIR) {
            return false;
        }
        String name = nameOf(block);
        return name.contains("leaves") || name.contains("log") || name.contains("wood")
                || name.contains("stem") || name.contains("hyphae") || name.contains("mushroom");
    }

    private static String nameOf(Block block) {
        return block.getType().name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String key(Block block) {
        World world = block.getWorld();
        return (world == null ? "?" : world.getName()) + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    // ------------------------------------------------------------------ effets

    /** Les drops d'un bloc, tels que le serveur les aurait calculés — avec repli sur le matériau lui-même. */
    public List<ItemStack> dropsOf(Block block, Player player) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        try {
            Collection<ItemStack> natural = block.getDrops(player.getInventory().getItemInMainHand());
            if (natural != null) {
                out.addAll(natural);
            }
        } catch (RuntimeException | LinkageError failed) {
            out.add(new ItemStack(block.getType()));
        }
        if (out.isEmpty()) {
            out.add(new ItemStack(block.getType()));
        }
        return out;
    }

    /** Retire le bloc proprement (effet + son), sans laisser de particules fantômes. */
    public void remove(Block block) {
        try {
            block.getWorld().playEffect(block.getLocation(), org.bukkit.Effect.STEP_SOUND, block.getType().getId());
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError cosmetic) {
            // l'effet de pas est decoratif : son absence ne doit pas annuler la cassure
        }
        try {
            block.setType(Material.AIR, false);
        } catch (RuntimeException failed) {
            this.plugin.getLogger().warning("[multi-outil] bloc non retire ("
                    + failed.getClass().getSimpleName() + ") : " + block.getX() + "," + block.getY() + "," + block.getZ());
        }
    }

    /**
     * Remplace les drops bruts par leur version fondue (la capacité <code>AUTO_SMELT</code>).
     *
     * <p>On passe par la table de <em>recettes de cuisson</em> du serveur, jamais par une table
     * codée : c'est la seule façon que ça reste exact quand un pack ajoute un minerai fondu, et ça
     * n'impose aucune connaissance des noms internes du serveur. Un ingrédient sans recette
     * (charbon, diamant) ressort intact.</p>
     */
    public List<ItemStack> smelt(List<ItemStack> drops) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (ItemStack drop : drops) {
            ItemStack result = smelted(drop);
            if (result == null) {
                out.add(drop);
                continue;
            }
            result.setAmount(Math.max(1, drop.getAmount()));
            out.add(result);
        }
        return out;
    }

    /** Le résultat de cuisson d'un matériau, ou {@code null} s'il n'en a pas. */
    public ItemStack smelted(ItemStack source) {
        if (source == null) {
            return null;
        }
        try {
            for (org.bukkit.inventory.Recipe recipe : this.plugin.getServer().getRecipesFor(source)) {
                if (recipe instanceof org.bukkit.inventory.FurnaceRecipe) {
                    ItemStack result = ((org.bukkit.inventory.FurnaceRecipe) recipe).getResult();
                    if (result != null && result.getType() != Material.AIR) {
                        return result.clone();
                    }
                }
                if (recipe instanceof org.bukkit.inventory.CookingRecipe) {
                    ItemStack result = ((org.bukkit.inventory.CookingRecipe<?>) recipe).getResult();
                    if (result != null && result.getType() != Material.AIR) {
                        return result.clone();
                    }
                }
            }
        } catch (RuntimeException | LinkageError failed) {
            return null;
        }
        return null;
    }

    /** Un multiplicateur de quantité (fortune / double drop), appliqué avec une probabilite. */
    public List<ItemStack> multiply(List<ItemStack> drops, double chance, int extraMin, int extraMax) {
        if (chance <= 0.0D || extraMax < extraMin) {
            return drops;
        }
        List<ItemStack> out = new ArrayList<ItemStack>(drops);
        java.util.Random random = new java.util.Random();
        for (ItemStack drop : drops) {
            if (random.nextDouble() > chance) {
                continue;
            }
            int extra = extraMin + (extraMax > extraMin ? random.nextInt(extraMax - extraMin + 1) : 0);
            for (int i = 0; i < extra; i++) {
                ItemStack copy = drop.clone();
                copy.setAmount(1);
                out.add(copy);
            }
        }
        return out;
    }
}
