package xyz.arcadiadevs.valariatools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Les capacités elles-mêmes : ce que le wiki appelle un « enchantement », et comment il se
 * matérialise sur ce serveur.
 *
 * <h2>Volontairement borné</h2>
 * <p>Une capacité n'est pas un script : c'est une clé connue ({@link #SUPPORTED}), avec des valeurs.
 * Ajouter un comportement vraiment nouveau demande donc du Java — c'est le prix d'un système qui ne
 * peut pas faire n'importe quoi (un <code>VEIN</code> sans plafond, c'est un serveur qui s'écroule).
 * En revanche <b>les valeurs, le verrou de palier, le niveau maximal et le prix</b> sont 100 %
 * configuration, ce qui suffit à recopier un barème de wiki sans recompiler.</p>
 *
 * <h2>Un effet par noyau, plusieurs capacités par noyau</h2>
 * <p>Le wiki de GenTycoon empile trois enchantements de vitesse sur une même pioche. Plutôt que de
 * faire gagner le plus fort et d'en perdre deux, {@link ToolsConfig.Effect} agrège les sources : les
 * chances se combinent, les portées se prennent au maximum, les bonus s'additionnent. Cette classe ne
 * choisit donc jamais « quelle capacité gagne ».</p>
 *
 * <h2>Toutes les écritures passent par le listener</h2>
 * <p>Les méthodes de cassure de bloc sont conçues pour être appelées depuis un événement annulé : elles
 * posent l'air, jouent l'effet, et rendent les <em>drops calculés</em> plutôt que de laisser le serveur
 * le faire. Cela évite les deux accidents classiques : doubler les drops, ou les perdre quand le bloc
 * est retiré par une méthode qui ne les calcule pas.</p>
 *
 * <h2>Aucune constante d'API incertaine n'est écrite en dur</h2>
 * <p>Les effets de potion et les enchantements sont résolus <b>par clé</b> au moment de l'appel, avec
 * repli sur le nom de champ historique : les enums de potion ont été renommés à mi-parcours du
 * support 1.20 → 1.21, et un <code>NoSuchFieldError</code> au milieu d'un minage coûterait plus cher
 * qu'un bonus absent.</p>
 */
public final class Abilities {

    /** Les clés comprises par le moteur. Toute autre clé est refusée au chargement, pas ignorée. */
    public static final Set<String> SUPPORTED = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            // selection de blocs
            "VEIN", "TREE_FELL", "AREA_BREAK", "EXTRA_BLOCK", "GHOST_MINES", "CROP_HARVEST",
            // transformation des drops
            "AUTO_SMELT", "FORTUNE", "DOUBLE_DROP", "SELL_ON_BREAK", "INFINITE_DURABILITY",
            // gains
            "MONEY_MULT", "MONEY_DOUBLE", "MONEY_POUCH", "XP_FLAT", "XP_MULT", "TREASURE",
            "RANDOM_ENCHANT", "FURY", "PROC_BOOSTER",
            // sensations
            "HASTE", "SWIFT", "SOUL_SPEED",
            // combat
            "CRIT", "DAMAGE_MULT", "LIFE_STEAL", "KNOCKBACK", "POTION_APPLY", "AUTO_SWING", "MULTI_KILL",
            // peche
            "AUTO_REEL", "FAST_REEL", "MULTI_CATCH", "LUCK")));

    /** Les six voisins d'un bloc, dans un ordre stable (le parcours d'un filon n'en est que plus reproductible). */
    private static final BlockFace[] NEIGHBOURS = new BlockFace[]{
        BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    /** Plafond de rayon d'une capacité de zone : 5 = 11³ cases examinées, assez pour un tycoon. */
    static final int MAX_RADIUS = 5;

    /** Plafond de blocs cassés par un seul geste, toutes capacités confondues. */
    static final int MAX_BLOCKS_PER_GESTURE = 256;

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
     * Les blocs d'un même filon, à partir du bloc cassé, plafonné par <code>max-blocks</code>.
     *
     * <p>Le parcours est <b>itératif avec un budget global</b> (pas une récursion) : un filon de fer
     * malformé ou un bloc collé à une structure de 10 000 pierres identiques ne doit pas pouvoir faire
     * exploser la pile Java ni le tick.</p>
     *
     * @param similar true pour n'accepter que le même matériau (minerais), false pour « tout ce que
     *                l'outil mine » (utile sur la pierre/terre des tycoons)
     */
    public List<Block> vein(Block start, ToolsConfig.Effect effect, boolean similar) {
        int max = clampBlocks(effect.value("max-blocks", 16));
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
                Block neighbour = relative(current, face);
                if (neighbour == null) {
                    continue;
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
    public List<Block> tree(Block start, ToolsConfig.Effect effect) {
        int max = clampBlocks(effect.value("max-blocks", 64));
        int height = Math.max(1, effect.value("max-height", 12));
        List<Block> out = new ArrayList<Block>();
        out.add(start);
        this.visited.clear();
        this.budget = max * 4;
        // 1) le tronc, vers le haut
        Block cursor = start.getRelative(BlockFace.UP);
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
                Block neighbour = relative(current, face);
                if (neighbour == null || this.visited.contains(key(neighbour))) {
                    continue;
                }
                this.visited.add(key(neighbour));
                queue.add(neighbour);
            }
        }
        return out;
    }

    /**
     * Le cube autour du bloc (onde sismique, explosive, surcharge, main de Gaïa, jugement divin).
     *
     * <p>Un seul parcours, trois filtres optionnels : la portée vient du niveau de la capacité, le
     * reste de la config. Le budget empêche un « jugement divin » configué en rayon 200 de casser le
     * tick, et l'ordre (du plus proche au plus loin) fait que le plafonnement tombe sur les blocs que
     * le joueur voit, pas sur le coin opposé.</p>
     */
    public List<Block> area(Block start, ToolsConfig.Effect effect, boolean oresOnly, boolean plantsOnly,
            boolean sameType) {
        int radius = Math.max(1, Math.min(MAX_RADIUS, effect.value("radius", 1)));
        int vertical = effect.flag("flat", false) ? 0 : radius;
        int max = clampBlocks(effect.value("max-blocks", radius * radius * radius));
        List<Block> out = new ArrayList<Block>();
        World world = start.getWorld();
        if (world == null) {
            return out;
        }
        for (int dy = -vertical; dy <= vertical && out.size() < max; dy++) {
            for (int dx = -radius; dx <= radius && out.size() < max; dx++) {
                for (int dz = -radius; dz <= radius && out.size() < max; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    Block block;
                    try {
                        block = world.getBlockAt(start.getX() + dx, start.getY() + dy, start.getZ() + dz);
                    } catch (RuntimeException | LinkageError outside) {
                        continue;   // chunk ou monde decharge sous le geste
                    }
                    if (areaAccepts(block, start, oresOnly, plantsOnly, sameType)) {
                        out.add(block);
                    }
                }
            }
        }
        return out;
    }

    /** Les {@code count} blocs identiques collés au bloc cassé (capacité « Seconde main »). */
    public List<Block> extra(Block start, int count) {
        List<Block> out = new ArrayList<Block>();
        if (count <= 0) {
            return out;
        }
        for (BlockFace face : NEIGHBOURS) {
            if (out.size() >= count) {
                break;
            }
            Block neighbour = relative(start, face);
            if (neighbour != null && neighbour.getType() == start.getType()) {
                out.add(neighbour);
            }
        }
        // Si le bloc est en bout de veine, on regarde aussi les voisins des voisins : « un minerai
        // supplementaire » doit rester vrai meme quand la veine est diagonale.
        if (out.size() < count) {
            Set<String> seen = new HashSet<String>();
            for (Block picked : out) {
                seen.add(key(picked));
            }
            for (Block face : new ArrayList<Block>(out)) {
                for (BlockFace direction : NEIGHBOURS) {
                    if (out.size() >= count) {
                        break;
                    }
                    Block next = relative(face, direction);
                    // comparaison par coordonnees et non par `List#contains` : `Block#equals` n'est pas
                    // garanti identique d'une version a l'autre, et un doublon ici = un bloc compte deux fois
                    if (next != null && next.getType() == start.getType() && seen.add(key(next))) {
                        out.add(next);
                    }
                }
            }
        }
        return out;
    }

    private static Block relative(Block block, BlockFace face) {
        try {
            return block.getRelative(face);
        } catch (IllegalArgumentException outside) {
            return null;   // bloc hors du monde charge
        }
    }

    /** Le bloc appartient-il à l'âme d'outil qui le frappe ? (utilisé par le filon et le veto de minage) */
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

    private boolean areaAccepts(Block block, Block start, boolean oresOnly, boolean plantsOnly, boolean sameType) {
        if (block == null || block.getType() == Material.AIR) {
            return false;
        }
        if (sameType && block.getType() != start.getType()) {
            return false;
        }
        if (!sameType && !sameSoul(block, start)) {
            return false;
        }
        if (oresOnly && !isOre(block)) {
            return false;
        }
        return !plantsOnly || isPlant(block);
    }

    /** Deux blocs relèvent-ils de la même âme ? Sans ça, une onde sismique de pioche casserait la ferme. */
    private boolean sameSoul(Block block, Block start) {
        ToolKind kind = this.matcher.kindOf(block);
        return kind != null && kind == this.matcher.kindOf(start);
    }

    /** Minerai, ou son équivalent (débris antiques, blocs marqués « ore » par un pack). */
    public static boolean isOre(Block block) {
        String name = nameOf(block);
        return name.contains("ore") || name.contains("debris");
    }

    /** Ce qui se récolte à la houe : tout ce qui est végétal et, si possible, mûr. */
    public static boolean isPlant(Block block) {
        String name = nameOf(block);
        if (name.isEmpty()) {
            return false;
        }
        return name.contains("crop") || name.contains("berries") || name.contains("melon")
                || name.contains("pumpkin") || name.contains("cactus") || name.contains("cane")
                || name.contains("bamboo") || name.contains("cocoa") || name.contains("wart")
                || name.contains("flower") || name.contains("grass") || name.contains("vine")
                || name.contains("mushroom") || name.contains("nether_wart") || name.contains("sweet");
    }

    /**
     * Un végétal est-il prêt à être récolté ? L'âge se lit sur l'API publique des BlockData, et le
     * plafond d'âge par recherche réflexive : la méthode s'appelle <code>maximumAge</code> sur 1.13 à
     * 1.20, <code>getMaximumAge</code> ensuite. Un melon ou une pastèque n'a pas d'âge : il est mûr.
     */
    public boolean harvestable(Block block) {
        if (!isPlant(block)) {
            return false;
        }
        try {
            BlockData data = block.getBlockData();
            if (!(data instanceof Ageable)) {
                return true;
            }
            Ageable ageable = (Ageable) data;
            int ceiling = maximumAge(ageable);
            return ceiling <= 0 || ageable.getAge() >= ceiling;
        } catch (RuntimeException | LinkageError unreadable) {
            return true;   // un vegetal dont on ne sait pas lire l'age se cueille quand meme
        }
    }

    private static int maximumAge(Ageable ageable) {
        for (String name : Arrays.asList("maximumAge", "getMaximumAge")) {
            try {
                Method method = ageable.getClass().getMethod(name);
                Object value = method.invoke(ageable);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError absent) {
                // methode renommer sur cette version : on tente la suivante
            }
        }
        return -1;
    }

    /** Replante ce qui vient d'être récolté (la « replantation instantanée » de la page Houe). */
    public void replant(Block block, Material previous) {
        if (block == null || previous == null || previous == Material.AIR) {
            return;
        }
        try {
            block.setType(previous, false);
            BlockData data = block.getBlockData();
            if (data instanceof Ageable) {
                ((Ageable) data).setAge(0);
                block.setBlockData(data, false);
            }
        } catch (RuntimeException | LinkageError failed) {
            // un bloc qui refuse la replantation (monde protege, plante particuliere) ne casse pas la récolte
        }
    }

    /**
     * Un bloc de tronc. Les noms sont testés en minuscules et sans passer par un tag : sur un serveur
     * ancien <code>Tag.LOG</code> n'existe pas, et un arbre doit continuer à tomber. Les types de bois
     * réels (log/wood/stem/hyphae) sont énumérés plutôt que cherchés par <code>contains("wood")</code>
     * pour que <code>STRIPPED_*</code> et <code>CRIMSON_HYPHAE</code> restent corrects.
     */
    private static boolean isLog(Block block) {
        if (block == null || block.getType() == Material.AIR) {
            return false;
        }
        String name = nameOf(block);
        return name.contains("log") || name.contains("wood") || name.contains("stem")
                || name.contains("hyphae");
    }

    /** Ce qui peut être emporté avec le tronc : le bois et la canopée. */
    private static boolean isWood(Block block) {
        if (block == null || block.getType() == Material.AIR) {
            return false;
        }
        String name = nameOf(block);
        return name.contains("leaves") || name.contains("log") || name.contains("wood")
                || name.contains("stem") || name.contains("hyphae") || name.contains("mushroom");
    }

    private static String nameOf(Block block) {
        return block == null || block.getType() == null ? "" : block.getType().name().toLowerCase(Locale.ROOT);
    }

    private static String key(Block block) {
        World world = block.getWorld();
        return (world == null ? "?" : world.getName()) + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    private static int clampBlocks(int requested) {
        return Math.max(1, Math.min(MAX_BLOCKS_PER_GESTURE, requested));
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
            block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType().getId());
        } catch (RuntimeException | LinkageError cosmetic) {
            // l'effet de pas est decoratif : son absence ne doit pas annuler la cassure
        }
        try {
            block.setType(Material.AIR, false);
        } catch (RuntimeException failed) {
            this.plugin.getLogger().warning("[multi-outil] bloc non retiré ("
                    + failed.getClass().getSimpleName() + ") : " + block.getX() + "," + block.getY() + "," + block.getZ());
        }
    }

    /** Un effet visuel raté ne doit jamais annuler une capacité : même enveloppe pour tous les appels. */
    public void particles(Block block, String name) {
        if (block == null || block.getWorld() == null) {
            return;
        }
        try {
            Particle particle = Particle.valueOf(name.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
            block.getWorld().spawnParticle(particle, block.getLocation().add(0.5D, 0.5D, 0.5D), 12);
        } catch (RuntimeException | LinkageError unknown) {
            // nom de particule inconnu du serveur (ou monde dechargé) : la capacite reste efficace
        }
    }

    /**
     * Remplace les drops bruts par leur version fondue (la capacité <code>AUTO_SMELT</code>).
     *
     * <p>On passe par la table de <em>recettes de cuisson</em> du serveur, jamais par une table codée :
     * c'est la seule façon que ça reste exact quand un pack ajoute un minerai fondu, et ça n'impose
     * aucune connaissance des noms internes du serveur. Un ingrédient sans recette (charbon, diamant)
     * ressort intact.</p>
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

    /** Un multiplicateur de quantité (fortune / double drop / pillage), appliqué avec une probabilité. */
    public List<ItemStack> multiply(List<ItemStack> drops, double chance, int extraMin, int extraMax) {
        if (chance <= 0.0D || extraMax < extraMin) {
            return drops;
        }
        List<ItemStack> out = new ArrayList<ItemStack>(drops);
        for (ItemStack drop : drops) {
            if (Math.random() > chance) {
                continue;
            }
            int span = extraMax - extraMin;
            int extra = extraMin + (span > 0 ? (int) Math.round(Math.random() * span) : 0);
            for (int i = 0; i < extra; i++) {
                ItemStack copy = drop.clone();
                copy.setAmount(1);
                out.add(copy);
            }
        }
        return out;
    }

    /** Vrai avec la probabilité demandée — un seul endroit où <code>Math.random()</code> est comparé. */
    public static boolean proc(double chance) {
        return chance > 0.0D && Math.random() < chance;
    }

    // ------------------------------------------------------------------ sensations (effets de potion)

    /**
     * Vitesse de minage (Efficacité, Speed, Célérité). L'amplificateur vient de la somme des sources,
     * plafonné à 4 : au-delà, le client mine plus vite que le serveur n'accepte et la partie devient un
     * combat contre l'anti-cheat.
     */
    public void haste(Player player, ToolsConfig.Effect effect) {
        int grade = grade(effect);
        if (hasHaste(player, grade - 1)) {
            return;   // deja au bon niveau : pas de paquet inutile a chaque bloc casse
        }
        duration(player, "fast_digging", "FAST_DIGGING", grade - 1, effect.value("duration", 60));
    }

    /**
     * L'amplificateur que la config donne pour cet agrégat de capacités, borné à 5. Exposé parce que
     * l'entretien « outil en main » (voir {@link ToolListener#refreshPassive(Player)}) doit savoir
     * <em>lequel</em> il a posé pour ne retirer que le sien.
     */
    public static int grade(ToolsConfig.Effect effect) {
        return Math.max(1, Math.min(5, effect == null ? 1 : effect.value("amplifier", 1)));
    }

    /**
     * Retire la vitesse de minage que <b>nous</b> avons posée, et elle seule : un plugin voisin qui aurait
     * donné sa propre Haste la garde (le comparant est l'amplificateur que nous avions appliqué).
     */
    public void clearHaste(Player player, int amplifier) {
        PotionEffectType type = effectType("fast_digging", "FAST_DIGGING");
        if (type == null || player == null) {
            return;
        }
        try {
            PotionEffect active = player.getPotionEffect(type);
            if (active != null && active.getAmplifier() == amplifier) {
                player.removePotionEffect(type);
            }
        } catch (RuntimeException | LinkageError refused) {
            // l'effet a ete retire entre-temps par un autre plugin : il n'y a rien a rendre
        }
    }

    /** Vitesse de déplacement (le « Speed » de l'épée, sous forme de rafale après un coup). */
    public void swift(Player player, ToolsConfig.Effect effect) {
        duration(player, "speed", "SPEED", Math.min(4, effect.value("amplifier", 1)), effect.value("duration", 60));
    }

    /** Effet appliqué au joueur lui-même (la « Force » de l'épée du wiki). */
    public void empower(Player player, ToolsConfig.Effect effect) {
        for (String name : effect.strings("effects")) {
            duration(player, name, name, Math.max(0, effect.value("amplifier", 0)), effect.value("duration", 100));
        }
    }

    private void duration(Player player, String key, String field, int amplifier, int ticks) {
        PotionEffectType type = effectType(key, field);
        if (type == null || amplifier < 0) {
            return;
        }
        try {
            player.addPotionEffect(new PotionEffect(type, Math.max(20, ticks), amplifier));
        } catch (RuntimeException | LinkageError failed) {
            // serveur sans cet effet (ou effet annule par un autre plugin) : le geste reste valide
        }
    }

    /** Vrai si le joueur a deja cet amplifier de vitesse de minage, avec de la marge avant expiration. */
    private boolean hasHaste(Player player, int amplifier) {
        PotionEffectType wanted = effectType("fast_digging", "FAST_DIGGING");
        if (wanted == null) {
            return true;   // effet indisponible sur ce serveur : on ne le repose pas en boucle
        }
        try {
            for (PotionEffect active : player.getActivePotionEffects()) {
                if (active != null && active.getType().equals(wanted)) {
                    return active.getAmplifier() >= amplifier && active.getDuration() > 40;
                }
            }
        } catch (RuntimeException | LinkageError failed) {
            return true;
        }
        return false;
    }

    /**
     * Résout un effet de potion par clé du registre, puis par nom de champ historique. Les deux
     * chemins existent depuis longtemps, mais pas sur les mêmes versions : c'est le seul façon
     * d'écrire <code>strength</code> sans se faire surprendre par le renommage 1.20 → 1.21.
     */
    private static PotionEffectType effectType(String... names) {
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            try {
                PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
                if (type != null) {
                    return type;
                }
            } catch (RuntimeException | LinkageError unknown) {
                // registre indisponible sur ce serveur : on tente le champ
            }
        }
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            try {
                Field field = PotionEffectType.class.getField(name.toUpperCase(Locale.ROOT));
                Object value = field.get(null);
                if (value instanceof PotionEffectType) {
                    return (PotionEffectType) value;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError absent) {
                // ni la clé ni le champ : l'effet est simplement indisponible
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ enchantements

    /**
     * Pose un enchantement réel sur l'item (la capacité <code>RANDOM_ENCHANT</code>, « Charognard » du
     * wiki). Le niveau est borné par celui que le serveur accepte, et la liste des candidats vient de
     * la config : pas de tirage dans toutes les tables du serveur, qui finirait par poser
     * <code>MACE_SMASH</code> sur une pioche.
     *
     * @return l'enchantement posé, ou {@code null} si aucun candidat n'était disponible
     */
    public Enchantment enchant(ItemStack tool, List<String> allowed, int level) {
        if (tool == null || !tool.hasItemMeta() || allowed == null || allowed.isEmpty()) {
            return null;
        }
        List<Enchantment> candidates = new ArrayList<Enchantment>();
        for (String name : allowed) {
            Enchantment found = enchantment(name);
            if (found != null && !candidates.contains(found)) {
                candidates.add(found);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Enchantment chosen = candidates.get((int) (Math.random() * candidates.size()));
        int applied = Math.max(1, level);
        try {
            applied = Math.min(applied, chosen.getMaxLevel());
        } catch (RuntimeException | LinkageError legacy) {
            applied = Math.min(applied, 5);
        }
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return null;
        }
        try {
            meta.addEnchant(chosen, applied, true);
            tool.setItemMeta(meta);
            return chosen;
        } catch (RuntimeException | LinkageError refused) {
            return null;
        }
    }

    /** Un enchantement par son nom, avec repli sur l'énumération du serveur si le registre refuse. */
    private static Enchantment enchantment(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String needle = name.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        try {
            Enchantment found = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft(needle));
            if (found != null) {
                return found;
            }
        } catch (RuntimeException | LinkageError unknown) {
            // registre indisponible : on cherche dans la liste
        }
        try {
            for (Enchantment candidate : Enchantment.values()) {
                if (candidate == null || candidate.getKey() == null) {
                    continue;
                }
                if (candidate.getKey().getKey().equalsIgnoreCase(needle)) {
                    return candidate;
                }
            }
        } catch (RuntimeException | LinkageError unready) {
            // values() leve si le registre n'est pas encore pret : aucun enchantement, aucun crash
        }
        return null;
    }
}
