package xyz.arcadiadevs.valariatools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Tout le comportement du multi-outil : c'est ici que « un seul item, quatre âmes » devient réel.
 *
 * <h2>Deux règles structurelles</h2>
 * <ul>
 *   <li><b>un seul handler de vérité</b> : les cassures sont <em>toujours</em> calculées par le plugin
 *       (drops, fortune, fusion, vente) et l'événement Bukkit est annulé. Mélanger « le serveur le
 *       fait » et « je le fais » est la source n°1 de drops doublés ou perdus.</li>
 *   <li><b>un garde-fou de réentrance</b> ({@link #handling}) : retirer des blocs déclenche des
 *       <code>BlockBreakEvent</code> en cascade ; sans lui, un filon de 24 blocs se retrancherait
 *       lui‑même bloc par bloc, à l'infini.</li>
 * </ul>
 *
 * <h2>Une capacité = un tour de dé, pas une promesse</h2>
 * <p>Chaque noyau se branche au même endroit : sélection des blocs, transformation des drops, gains,
 * sensations. Les probabilités passent toutes par {@link Abilities#proc(double)}, donc le Proc booster
 * du wiki s'applique uniformément — un <code>if (Math.random() &lt; …)</code> éparpillé dans le fichier
 * est la façon la plus sûre d'oublier le multiplicateur dans une branche.</p>
 *
 * <h2>Aucune API incertaine n'est appelée en dur</h2>
 * <p>Pas de <code>getTargetBlock</code> (comportement de raycast redessiné selon les versions), pas de
 * reflection dans les méthodes privées du serveur. Ce qui n'est garanti ni par la doc Bukkit ni par nos
 * classes est fait par réflexion sur l'API <b>publique</b> (le temps d'attente du bobber, le plafond d'un
 * BlockData) avec un repli utile — jamais par contournement du serveur.</p>
 */
public final class ToolListener implements Listener {

    /** L'état de la Furie (capacité de la houe) : un multiplicateur d'argent, pour une durée. */
    private static final class Fury {

        private final long until;
        private final double multiplier;

        Fury(long until, double multiplier) {
            this.until = until;
            this.multiplier = multiplier;
        }

        boolean live() {
            return this.until > System.currentTimeMillis();
        }

        double multiplier() {
            return this.multiplier;
        }
    }

    /** La vitesse de marche remplacée par « Vitesse des âmes », par joueur, pour la rendre intacte. */
    private static final class Boost {

        private final float previous;

        Boost(float previous) {
            this.previous = previous;
        }

        float previous() {
            return this.previous;
        }
    }

    private final JavaPlugin plugin;
    private final ToolsConfig config;
    private final ToolStore store;
    private final EconomyService economy;
    private final List<Material> treasurePool = new ArrayList<Material>();
    private final Map<UUID, Long> castCooldown = new HashMap<UUID, Long>();
    private final Map<UUID, Fury> furies = new HashMap<UUID, Fury>();
    private final Map<UUID, Boost> boosts = new HashMap<UUID, Boost>();
    /** Le reliquat d'XP décimale, par joueur (voir {@link #settle(UUID, double)}). */
    private final Map<UUID, Double> xpCarry = new HashMap<UUID, Double>();
    private boolean handling;
    private boolean warned;

    public ToolListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ((ValoriaTools) plugin).toolsConfig();
        this.store = ((ValoriaTools) plugin).store();
        this.economy = ((ValoriaTools) plugin).economy();
    }

    /** Rafraîchit les interfaces ouvertes après un reload (le holder garde la référence, pas les données). */
    public void refreshViews() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory() != null
                    && player.getOpenInventory().getTopInventory().getHolder() instanceof ToolsGui.View) {
                ToolsGui.render(player);
            }
        }
        restoreSpeeds();
    }

    /** Un joueur qui rejoint avec un outil doit voir la lore à jour (paliers et niveaux rechargés). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = heldMultiTool(player);
        if (tool != null) {
            MultiTool.refresh(tool, this.config, this.store, player.getUniqueId());
        }
    }

    /** La vitesse de marche rendue au joueur, sinon un logout pendant une Furie le laisserait lent. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.castCooldown.remove(event.getPlayer().getUniqueId());
        this.xpCarry.remove(event.getPlayer().getUniqueId());   // un reliquat non rendu ne se promene pas
        this.furies.remove(event.getPlayer().getUniqueId());
        restore(event.getPlayer());
    }

    // ------------------------------------------------------------------ casser un bloc

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (this.handling || !this.config.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || heldMultiTool(player) == null) {
            return;
        }
        Block block = event.getBlock();
        if (!this.config.allowsWorld(block.getWorld() == null ? null : block.getWorld().getName())) {
            return;   // monde hors liste (monde d'eventement, lobby, pvp) : l'outil se conduit comme un item normal
        }
        ToolKind kind = ((ValoriaTools) this.plugin).matcher().kindOf(block);
        if (kind == null) {
            return;
        }
        ToolsConfig.KindConfig kindConfig = this.config.kind(kind);
        if (kindConfig == null) {
            return;
        }
        int tier = this.store.tierOf(player.getUniqueId(), kind, this.config.maxTier(kindConfig));
        Map<String, Integer> levels = this.store.levelsOf(player.getUniqueId(), kind);
        Abilities abilities = ((ValoriaTools) this.plugin).abilities();

        ToolsConfig.Effect crop = this.config.effect(kindConfig, "CROP_HARVEST", tier, levels);
        if (crop.active() && Abilities.isPlant(block) && !abilities.harvestable(block)) {
            // culture pas mûre : on ne casse RIEN et on ne cancel PAS — le joueur la voit repousser,
            // exactement comme la « récolte automatique » d'un wiki qui ne détruit pas les plants verts.
            return;
        }

        List<Block> targets = selectTargets(player, block, kind, kindConfig, tier, levels, abilities);
        if (!containsBlock(targets, block)) {
            // les capacites de zone excluent le centre (voir Abilities#area) : sans lui, un clic « onde
            // sismique » ne casserait que les voisins et laisserait le minerai vise debout sous les yeux
            // du joueur. Le bloc vise est donc ajoute d'office, en premiere position.
            targets.add(0, block);
        }

        event.setCancelled(true);
        this.handling = true;
        try {
            applyGesture(player, block, kind, kindConfig, tier, levels, targets, abilities, true);
        } catch (RuntimeException | LinkageError failed) {
            warnOnce("cassure", failed);
        } finally {
            this.handling = false;
        }
    }

    /**
     * Quels blocs le geste emporte : arbre, récolte de zone, filon, onde, ou un bloc de plus.
     *
     * <p>Un seul tour de dé par geste et par capacité : c'est ce qui rend un « 12 % de Briseur » égal à
     * 12 % de clics, et non à 12 % de blocs (auquel cas un filon de 20 blocs se déclencherait presque
     * tout le temps).</p>
     */
    private List<Block> selectTargets(Player player, Block block, ToolKind kind,
            ToolsConfig.KindConfig kindConfig, int tier, Map<String, Integer> levels, Abilities abilities) {
        List<Block> out = new ArrayList<Block>();
        Set<String> seen = new HashSet<String>();
        ToolsConfig.Effect tree = this.config.effect(kindConfig, "TREE_FELL", tier, levels);
        ToolsConfig.Effect crop = this.config.effect(kindConfig, "CROP_HARVEST", tier, levels);
        ToolsConfig.Effect vein = this.config.effect(kindConfig, "VEIN", tier, levels);
        ToolsConfig.Effect quake = this.config.effect(kindConfig, "AREA_BREAK", tier, levels);
        ToolsConfig.Effect second = this.config.effect(kindConfig, "EXTRA_BLOCK", tier, levels);
        if (kind == ToolKind.AXE && tree.active() && Abilities.proc(tree.chance("chance", 1.0D))) {
            add(out, seen, abilities.tree(block, tree));
        }
        if (out.isEmpty() && crop.active() && Abilities.isPlant(block)) {
            add(out, seen, abilities.area(block, crop, false, true, crop.flag("same-type", false)));
            // « Main de Gaia » : la capacite de zone agrandit la recolte, elle ne la remplace pas
            if (quake.active() && Abilities.proc(quake.chance("chance", 0.5D))) {
                add(out, seen, abilities.area(block, quake, false, true, false));
            }
            // le bloc vise est ajoute par l'appelant : une aire ne le compte jamais deux fois
        }
        if (out.isEmpty() && vein.active() && Abilities.proc(vein.chance("chance", 1.0D))) {
            add(out, seen, abilities.vein(block, vein, vein.flag("similar-blocks-only", true)));
        }
        if (out.isEmpty() && quake.active() && Abilities.proc(quake.chance("chance", 0.1D))) {
            add(out, seen, abilities.area(block, quake, quake.flag("ores-only", true), false,
                    quake.flag("same-type", false)));
        }
        if (out.isEmpty() && second.active() && Abilities.proc(second.chance("chance", 0.1D))) {
            add(out, seen, abilities.extra(block, Math.max(1, second.value("count", 1))));
        }
        return out;
    }

    private static void add(List<Block> out, Set<String> seen, List<Block> more) {
        for (Block block : more) {
            if (block == null || block.getType() == Material.AIR) {
                continue;
            }
            String key = block.getX() + ";" + block.getY() + ";" + block.getZ() + ";" + block.getWorld().getName();
            if (seen.add(key)) {
                out.add(block);
            }
        }
    }

    /**
     * Le corps du geste : drops calculés, capacités appliquées, vente, XP, usure. Partagé entre le clic
     * et les vagues différées de la « Pioche fantomatique », pour que les deux donnent la même chose.
     */
    private void applyGesture(Player player, Block origin, ToolKind kind, ToolsConfig.KindConfig kindConfig,
            int tier, Map<String, Integer> levels, List<Block> targets, Abilities abilities, boolean procs) {
        boolean harvest = kind == ToolKind.AXE;
        int harvested = 0;
        Map<Material, Integer> brokenBlocks = new LinkedHashMap<Material, Integer>();
        List<ItemStack> drops = new ArrayList<ItemStack>();
        int count = 0;
        for (Block target : targets) {
            if (target == null || target.getType() == Material.AIR || !sameWorld(target, origin)) {
                continue;
            }
            Material before = target.getType();
            drops.addAll(abilities.dropsOf(target, player));
            abilities.remove(target);
            count++;
            if (Abilities.isPlant(target)) {
                harvested++;
            }
            Integer already = brokenBlocks.get(before);
            brokenBlocks.put(before, Integer.valueOf(already == null ? 1 : already.intValue() + 1));
            if (harvest && kindConfig.replant() && Abilities.isPlant(target)) {
                abilities.replant(target, before);
            }
        }
        if (count == 0) {
            return;
        }
        ToolStats stats = ((ValoriaTools) this.plugin).stats();
        // un geste = une mesure par grandeur : BLOCS pour tout ce qui est tombe, CULTURES pour les
        // plantes, et UN arbre par abattage — compter les 24 blocs d'un tronc comme 24 arbres
        // fausserait le classement en faveur du bucheron, qui est deja l'ame la plus rapide.
        stats.gesture(player, kind, ToolStats.Metric.BLOCKS, count);
        if (harvested > 0) {
            stats.gesture(player, kind, ToolStats.Metric.CROPS, harvested);
        }
        if (kind == ToolKind.AXE && isLogBlock(origin.getType())) {
            stats.gesture(player, kind, ToolStats.Metric.TREES, 1.0D);
        }
        if (drops.isEmpty()) {
            chargeDurability(player, kindConfig, tier, levels);
            return;
        }

        if (procs) {
            ToolsConfig.Effect quake = this.config.effect(kindConfig, "AREA_BREAK", tier, levels);
            if (quake.active() && quake.value("particles", 0) > 0) {
                abilities.particles(origin, "EXPLOSION_LARGE");
            }
            ToolsConfig.Effect haste = this.config.effect(kindConfig, "HASTE", tier, levels);
            if (haste.active()) {
                abilities.haste(player, haste);
            }
            scheduleGhosts(player, origin, kind, kindConfig, tier, levels, abilities);
        }

        ToolsConfig.Effect smelt = this.config.effect(kindConfig, "AUTO_SMELT", tier, levels);
        if (smelt.active() && (smelt.flag("always", true)
                || Abilities.proc(smelt.chance("chance", 0.5D)))) {
            drops = abilities.smelt(drops);
        }
        ToolsConfig.Effect fortune = this.config.effect(kindConfig, "FORTUNE", tier, levels);
        if (fortune.active()) {
            drops = abilities.multiply(drops, fortune.chance("chance", 0.1D),
                    fortune.value("extra-min", 1), fortune.value("extra-max", 2));
        }
        ToolsConfig.Effect twice = this.config.effect(kindConfig, "DOUBLE_DROP", tier, levels);
        if (twice.active()) {
            drops = abilities.multiply(drops, twice.chance("chance", 0.1D), 1, 1);
        }

        double money = payForDrops(player, kind, kindConfig, tier, levels, drops, count)
                + jobMoney(kindConfig, brokenBlocks);
        grantTreasures(player, origin, kindConfig, tier, levels, procs);

        double xp = this.config.xpPerBlock(kindConfig) * count + jobXpOf(kindConfig, brokenBlocks);
        if (procs) {
            xp *= 1.0D + this.config.effect(kindConfig, "XP_MULT", tier, levels)
                    .amount("percent", 0.0D) / 100.0D;
            ToolsConfig.Effect seeker = this.config.effect(kindConfig, "XP_FLAT", tier, levels);
            if (seeker.active() && Abilities.proc(seeker.chance("chance", 0.1D))) {
                xp += seeker.value("amount", 5);
            }
        }
        int whole = settle(player.getUniqueId(), xp);
        if (whole > 0) {
            giveExperience(player, whole);
        }
        if (money > 0.0D) {
            stats.money(player, kind, money);
            credit(player, money, "bloc" + (count > 1 ? "s" : "") + " vendu" + (count > 1 ? "s" : "") + " par l'outil");
        }
        if (procs) {
            rollEnchant(player, kindConfig, tier, levels, abilities);
        }
        chargeDurability(player, kindConfig, tier, levels);
    }

    /**
     * Vente à la casse + Braquage + Double gain + Money Pouch + Furie : tout ce qui transforme un bloc
     * en monnaie, au même endroit, parce que ces quatre capacités du wiki se multiplient entre elles.
     *
     * @return le montant crédité (0 si rien n'est vendu)
     */
    private double payForDrops(Player player, ToolKind kind, ToolsConfig.KindConfig kindConfig, int tier,
            Map<String, Integer> levels, List<ItemStack> drops, int blocks) {
        double gross = 0.0D;
        int sold = 0;
        List<ItemStack> kept = new ArrayList<ItemStack>();
        ToolsConfig.Effect sell = this.config.effect(kindConfig, "SELL_ON_BREAK", tier, levels);
        boolean selling = sell.active() && (!this.config.sellOnlyWhenSneaking() || player.isSneaking());
        for (ItemStack drop : drops) {
            if (!isDroppable(drop)) {
                continue;
            }
            double price = selling ? ((ValoriaTools) this.plugin).sellPrice(kind, drop.getType()) : -1.0D;
            if (price > 0.0D && price >= this.config.sellMinValue()) {
                gross += price * drop.getAmount() * this.config.sellMultiplier();
                sold += drop.getAmount();
                continue;
            }
            kept.add(drop);
        }
        World world = player.getWorld();
        for (ItemStack drop : kept) {
            giveOrDrop(player, world, drop);
        }
        // meme sans une seule vente, les pouches et la Furie se declenchent : le wiki les attache au
        // bloc casse, pas a la transaction
        double amount = gross;
        amount *= 1.0D + this.config.effect(kindConfig, "MONEY_MULT", tier, levels)
                .amount("percent", 0.0D) / 100.0D;
        ToolsConfig.Effect twice = this.config.effect(kindConfig, "MONEY_DOUBLE", tier, levels);
        if (twice.active() && Abilities.proc(twice.chance("chance", 0.05D))) {
            amount *= Math.max(2.0D, twice.amount("multiplier", 2.0D));
        }
        Fury fury = this.furies.get(player.getUniqueId());
        if (fury != null && fury.live()) {
            amount *= fury.multiplier();
        } else if (fury != null) {
            this.furies.remove(player.getUniqueId());
        }
        ToolsConfig.Effect pouch = this.config.effect(kindConfig, "MONEY_POUCH", tier, levels);
        if (pouch.active() && Abilities.proc(pouch.chance("chance", 0.02D))) {
            amount += pouch.amount("amount", 25.0D) + pouch.value("per-block", 0) * blocks;
        }
        ToolsConfig.Effect furyEffect = this.config.effect(kindConfig, "FURY", tier, levels);
        if (furyEffect.active() && (fury == null || !fury.live())
                && Abilities.proc(furyEffect.chance("chance", 0.02D))) {
            long ticks = Math.max(20L, furyEffect.value("duration", 200) * 50L);
            double multiplier = Math.max(1.1D, furyEffect.amount("multiplier", 1.5D));
            this.furies.put(player.getUniqueId(), new Fury(System.currentTimeMillis() + ticks, multiplier));
            player.sendMessage(MultiTool.color("&6⚡ Furie &7: gains x&f" + trim(multiplier) + "&7 pendant &f"
                    + (ticks / 50L / 20L) + "s&7."));
        }
        return ToolListener.round(amount);
    }

    /**
     * L'argent que le métier attache au BLOC, et non à l'objet qui en tombe (table du wiki : « Minerai de
     * diamant : 3.00 »). Tenu séparé de la revente des drops, parce que les deux existent en jeu et que
     * les mélanger paierait le même minerai deux fois.
     */
    private double jobMoney(ToolsConfig.KindConfig kindConfig, Map<Material, Integer> brokenBlocks) {
        double total = 0.0D;
        for (Map.Entry<Material, Integer> entry : brokenBlocks.entrySet()) {
            total += this.config.jobGain(kindConfig, entry.getKey().name(), false)
                    * entry.getValue().intValue();
        }
        return Double.isFinite(total) ? total : 0.0D;
    }

    /** Idem pour l'XP (<code>jobs.block-xp</code>), en décimales : voir {@link #settle(UUID, double)}. */
    private double jobXpOf(ToolsConfig.KindConfig kindConfig, Map<Material, Integer> brokenBlocks) {
        double total = 0.0D;
        for (Map.Entry<Material, Integer> entry : brokenBlocks.entrySet()) {
            total += this.config.jobGain(kindConfig, entry.getKey().name(), true)
                    * entry.getValue().intValue();
        }
        return Double.isFinite(total) ? total : 0.0D;
    }

    /**
     * Le wiki paie 0,01 XP pour un bloc de roche : arrondi bloc par bloc, le joueur ne recevrait JAMAIS
     * rien. Le reliquat est donc reporté d'un geste à l'autre et seul l'entier accumulé est donné — un
     * joueur qui ne gagne que du décimal ne perd rien, il attend.
     */
    private int settle(UUID owner, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0;
        }
        double carry = value + this.xpCarry.getOrDefault(owner, Double.valueOf(0.0D)).doubleValue();
        int whole = (int) Math.floor(carry);
        this.xpCarry.put(owner, Double.valueOf(carry - whole));
        return whole;
    }

    /** Les capacités de type « chercheur » : un objet du réservoir (clé, bonbon, spawner, générateur). */
    private void grantTreasures(Player player, Block origin, ToolsConfig.KindConfig kindConfig, int tier,
            Map<String, Integer> levels, boolean procs) {
        if (!procs) {
            return;
        }
        ToolsConfig.Effect treasure = this.config.effect(kindConfig, "TREASURE", tier, levels);
        if (!treasure.active()) {
            return;
        }
        double chance = treasure.chance("chance", 0.02D);
        int rolls = Math.max(1, treasure.value("rolls", 1));
        List<String> declared = treasure.strings("items");
        List<String> names = declared.isEmpty() ? this.config.treasureItems() : declared;
        for (int roll = 0; roll < rolls; roll++) {
            if (!Abilities.proc(chance) || names.isEmpty()) {
                continue;
            }
            String name = names.get((int) (Math.random() * names.size()));
            Material material = material(name);
            if (material == null) {
                continue;
            }
            ItemStack stack = new ItemStack(material, Math.max(1, treasure.value("amount", 1)));
            World world = origin.getWorld();
            if (world == null) {
                continue;
            }
            world.dropItemNaturally(origin.getLocation().add(0.5D, 0.5D, 0.5D), stack);
            player.sendMessage(MultiTool.color("&b✦ " + pretty(material) + " &7trouvé par l'outil."));
        }
    }

    /** « Charognard » : un enchantement réel posé sur l'item, dans la liste autorisée par l'admin. */
    private void rollEnchant(Player player, ToolsConfig.KindConfig kindConfig, int tier,
            Map<String, Integer> levels, Abilities abilities) {
        ToolsConfig.Effect rogue = this.config.effect(kindConfig, "RANDOM_ENCHANT", tier, levels);
        if (!rogue.active() || !Abilities.proc(rogue.chance("chance", 0.01D))) {
            return;
        }
        List<String> allowed = rogue.strings("enchants");
        if (allowed.isEmpty()) {
            return;
        }
        ItemStack tool = heldMultiTool(player);
        org.bukkit.enchantments.Enchantment applied = abilities.enchant(tool, allowed,
                Math.max(1, rogue.value("level", 1)));
        if (applied != null && tool != null) {
            player.updateInventory();
            player.sendMessage(MultiTool.color("&d✦ Charognard &7: " + pretty(applied.getKey().getKey())
                    + " " + applied.getStartLevel() + "&7 ajouté à l'outil."));
        }
    }

    /**
     * La « Pioche fantomatique » : des vagues différées qui minent autour du point d'impact. Différées
     * volontairement (une vague toutes les 5 ticks) : tout casser dans le même tick serait un pic de
     * charge inutile, et le joueur veut <em>voir</em> les pioches travailler.
     */
    private void scheduleGhosts(final Player player, final Block origin, final ToolKind kind,
            final ToolsConfig.KindConfig kindConfig, final int tier, final Map<String, Integer> levels,
            final Abilities abilities) {
        final ToolsConfig.Effect ghost = this.config.effect(kindConfig, "GHOST_MINES", tier, levels);
        if (!ghost.active() || !Abilities.proc(ghost.chance("chance", 0.05D))) {
            return;
        }
        final int waves = Math.max(1, Math.min(8, ghost.value("waves", 2)));
        final long interval = Math.max(5L, ghost.value("interval", 10));
        final UUID owner = player.getUniqueId();
        for (int wave = 1; wave <= waves; wave++) {
            final int index = wave;
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new Runnable() {

                @Override
                public void run() {
                    if (!player.isValid() || heldMultiTool(player) == null) {
                        return;
                    }
                    if (origin.getWorld() == null || origin.getType() != Material.AIR) {
                        return;   // le bloc d'origine a ete replace entre-temps : on ne creuse pas ailleurs
                    }
                    try {
                        int radius = Math.max(1, ghost.value("radius", 2) + index / 2);
                        List<Block> targets = abilities.area(origin, ghost, ghost.flag("ores-only", true),
                                kind == ToolKind.AXE, false);
                        List<Block> picked = new ArrayList<Block>();
                        for (Block block : targets) {
                            if (picked.size() >= radius * 2) {
                                break;
                            }
                            picked.add(block);
                        }
                        if (picked.isEmpty()) {
                            return;
                        }
                        handling = true;
                        try {
                            applyGesture(player, origin, kind, kindConfig, tier, levels, picked, abilities, false);
                        } finally {
                            handling = false;
                        }
                        for (Block block : picked) {
                            abilities.particles(block, "CRIT_MAGIC");
                        }
                    } catch (RuntimeException | LinkageError failed) {
                        warnOnce("pioche fantomatique", failed);
                    }
                }
            }, interval * wave);
        }
    }

    // ------------------------------------------------------------------ usure

    /**
     * Une seule usure par geste, même sur un filon de 24 blocs : c'est le comportement d'un outil
     * vein‑mine, et un plafond par clic empêche un filon géant de briser l'item en un coup.
     */
    private void chargeDurability(Player player, ToolsConfig.KindConfig kindConfig, int tier,
            Map<String, Integer> levels) {
        if (this.config.unbreakable() || this.config.effect(kindConfig, "INFINITE_DURABILITY", tier, levels).active()) {
            return;
        }
        ItemStack tool = heldMultiTool(player);
        if (tool == null || !tool.hasItemMeta()) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return;
        }
        Damageable damageable = (Damageable) meta;
        int damage = damageable.getDamage() + this.config.durabilityCost(kindConfig);
        int max;
        try {
            max = tool.getType().getMaxDurability();
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError legacy) {
            return;
        }
        if (max > 0 && damage >= max) {
            player.getInventory().setItemInMainHand(null);
            playBreakSound(player);
            player.sendMessage(MultiTool.color("&cTon multi-outil s'est brisé : ses capacités te restent,"
                    + " refais-en un avec /tools give."));
            return;
        }
        damageable.setDamage(damage);
        tool.setItemMeta(meta);
    }

    private void playBreakSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
        } catch (RuntimeException | LinkageError legacy) {
            // nom de son different sur les anciens serveurs : le message texte suffit
        }
    }

    /**
     * La XP est donnée au joueur, pas via un orbe : l'événement étant annulé, le serveur n'en lâche
     * plus, et <code>Block#getXpDrop</code> n'existe que sur des versions récentes. {@code giveExp(int)}
     * est dans l'API Bukkit depuis 1.13 — sous try/catch pour les plus anciens.
     */
    private void giveExperience(Player player, int experience) {
        try {
            player.giveExp(experience);
        } catch (RuntimeException | LinkageError legacy) {
            // pas de XP sur ce serveur : aucune mécanique du plugin n'en dépend
        }
    }

    /** L'item va dans l'inventaire s'il y a de la place, sinon au sol — jamais perdu. */
    private void giveOrDrop(Player player, World world, ItemStack drop) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
        if (overflow.isEmpty()) {
            return;
        }
        for (ItemStack left : overflow.values()) {
            try {
                world.dropItemNaturally(player.getLocation(), left);
            } catch (RuntimeException failed) {
                // monde decharge sous le joueur : l'item est perdu, on le dit plutot que de crasher
                this.plugin.getLogger().warning("[multi-outil] drop non rendu (monde inatteignable) : "
                        + left.getType());
            }
        }
    }

    private void credit(Player player, double amount, String reason) {
        EconomyService.Outcome credited = this.economy.deposit(player, amount);
        if (credited.success()) {
            player.sendMessage(MultiTool.color("&a+" + this.economy.format(amount) + "&7 (" + reason + ")"));
            return;
        }
        this.plugin.getLogger().warning("[multi-outil] crédit refusé pour " + player.getName() + " : "
                + credited.reason() + " — montant " + amount);
    }

    // ------------------------------------------------------------------ clic droit (menu + canne)

    /**
     * Deux usages du clic droit, séparés parce qu'ils se contredisent :
     *
     * <ol>
     *   <li><b>le menu</b> — la convention du wiki : clic droit avec la pioche et la houe, sneak + clic
     *       droit avec l'épée et la canne. Ouvrir une interface à la place d'un coup d'épée serait
     *       rédhibitoire, d'où la distinction par âme ;</li>
     *   <li><b>le lancer de ligne</b> — anti‑spam : deux clics en moins de 400 ms feraient deux lancers,
     *       et le second avalerait le premier bobber.</li>
     * </ol>
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (this.handling || !this.config.enabled()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Player player = event.getPlayer();
        if (heldMultiTool(player) == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block != null && hasInventory(block)) {
            return;
        }
        ToolKind kind = block == null ? this.config.fallbackKind()
                : ((ValoriaTools) this.plugin).matcher().kindOf(block);
        if (kind == null) {
            kind = this.config.fallbackKind();
        }
        // Le wiki : « clic droit avec la pioche et la houe, sneak + clic droit avec l'epee et la canne ».
        // Les âmes qui minent ouvrent donc le menu sur le vide (un clic sur un minerai reste un minage) ;
        // les âmes de combat et de pêche ne l'ouvrent qu'accroupi, pour ne voler ni le coup ni le lancer.
        boolean menuSoul = kind == ToolKind.PICKAXE || kind == ToolKind.AXE;
        boolean wantsMenu = menuSoul ? block == null && !player.isSneaking() : player.isSneaking();
        if (wantsMenu) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            this.plugin.getServer().getScheduler().runTask(this.plugin, new Runnable() {

                @Override
                public void run() {
                    ToolsGui.open(player);
                }
            });
            return;
        }
        if (kind != ToolKind.ROD || block != null) {
            return;
        }
        Long last = this.castCooldown.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (last != null && now - last.longValue() < 400L) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            return;
        }
        this.castCooldown.put(player.getUniqueId(), Long.valueOf(now));
    }

    // ------------------------------------------------------------------ vitesse des âmes

    /**
     * « Vitesse des âmes » (houe) : marcher plus vite sur le sable des âmes. La vitesse d'origine est
     * mémorisée à chaque activation et rendue au bloc suivant, à la sortie du joueur, et au reload — un
     * bonus qui resterait après coup est la façon la plus classe de se faire détester par le reste du
     * serveur (les autres plugins ne reconnaissent plus le joueur).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!this.config.enabled() || player == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;   // un demi-pixel de souris ne doit pas déclencher de lecture de bloc
        }
        ItemStack tool = heldMultiTool(player);
        if (tool == null) {
            restore(player);
            return;
        }
        Block under = feet(player);
        boolean onSoul = under != null && soul(under);
        ToolsConfig.KindConfig kindConfig = this.config.kind(ToolKind.AXE);
        int tier = kindConfig == null ? 1 : this.store.tierOf(player.getUniqueId(), ToolKind.AXE,
                this.config.maxTier(kindConfig));
        ToolsConfig.Effect speed = kindConfig == null ? ToolsConfig.Effect.none()
                : this.config.effect(kindConfig, "SOUL_SPEED", tier,
                        this.store.levelsOf(player.getUniqueId(), ToolKind.AXE));
        // la vitesse Bukkit est relative (0,2 = marche normale) : on exprime un pourcentage de bonus,
        // jamais une vitesse brute — un admin qui ecrit « 12 » ne doit pas rendre le joueur plus lent
        float wanted = Math.min(1.0F, Math.max(0.05F, 0.2F * (1.0F + speed.value("boost", 40) / 100.0F)));
        if (onSoul && speed.active()) {
            if (!this.boosts.containsKey(player.getUniqueId())) {
                this.boosts.put(player.getUniqueId(), new Boost(player.getWalkSpeed()));
                try {
                    player.setWalkSpeed(Math.max(0.0F, Math.min(1.0F, wanted)));
                } catch (RuntimeException | LinkageError refused) {
                    // vitesse refusée par un plugin de protection : le reste de l'outil fonctionne
                }
            }
            return;
        }
        restore(player);
    }

    private void restore(Player player) {
        Boost boost = this.boosts.remove(player.getUniqueId());
        if (boost == null) {
            return;
        }
        try {
            player.setWalkSpeed(boost.previous());
        } catch (RuntimeException | LinkageError refused) {
            // le joueur est deja parti : rien a rattraper
        }
    }

    /** Toutes les vitesses rendues (reload : la config ne contient plus la capacité). */
    private void restoreSpeeds() {
        for (UUID owner : new ArrayList<UUID>(this.boosts.keySet())) {
            Player player = this.plugin.getServer().getPlayer(owner);
            if (player != null) {
                restore(player);
            } else {
                this.boosts.remove(owner);
            }
        }
    }

    private Block feet(Player player) {
        try {
            return player.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
        } catch (RuntimeException | LinkageError unreadable) {
            return null;
        }
    }

    private static boolean soul(Block block) {
        String name = block.getType().name().toLowerCase(Locale.ROOT);
        return name.contains("soul_sand") || name.contains("soul_soil") || name.contains("soul");
    }

    // ------------------------------------------------------------------ peche

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!this.config.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (heldMultiTool(player) == null) {
            return;
        }
        if (!this.config.allowsWorld(player.getWorld() == null ? null : player.getWorld().getName())) {
            return;
        }
        ToolsConfig.KindConfig kindConfig = this.config.kind(ToolKind.ROD);
        if (kindConfig == null) {
            return;
        }
        int tier = this.store.tierOf(player.getUniqueId(), ToolKind.ROD, this.config.maxTier(kindConfig));
        Map<String, Integer> levels = this.store.levelsOf(player.getUniqueId(), ToolKind.ROD);
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            this.castCooldown.put(player.getUniqueId(), Long.valueOf(0L));
            shortenWait(player, event, this.config.effect(kindConfig, "FAST_REEL", tier, levels));
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (this.config.effect(kindConfig, "AUTO_REEL", tier, levels).active()) {
            this.castCooldown.put(player.getUniqueId(), Long.valueOf(0L));
        }
        ToolsConfig.Effect luck = this.config.effect(kindConfig, "LUCK", tier, levels);
        if (luck.active()) {
            grantTreasure(player, luck);
        }
        ToolsConfig.Effect tsunami = this.config.effect(kindConfig, "MULTI_CATCH", tier, levels);
        Entity caught = event.getCaught();
        if (!(caught instanceof org.bukkit.entity.Item)) {
            return;
        }
        org.bukkit.entity.Item item = (org.bukkit.entity.Item) caught;
        ItemStack stack = item.getItemStack();
        // une prise = une mesure ; le bonus de Tsunami est paye mais ne compte pas comme prise de plus
        ((ValoriaTools) this.plugin).stats().gesture(player, ToolKind.ROD, ToolStats.Metric.FISH, 1.0D);
        if (tsunami.active() && Abilities.proc(tsunami.chance("chance", 0.05D))) {
            stack.setAmount(stack.getAmount() + Math.max(1, tsunami.value("count", 2)));
        }
        ToolsConfig.Effect sell = this.config.effect(kindConfig, "SELL_ON_BREAK", tier, levels);
        ToolsConfig.KindConfig rod = this.config.kind(ToolKind.ROD);
        // le metier du Pêcheur paie le poisson PECHÉ (Morue 1.50, Tropical 3.00) : le prix de revente
        // de l'objet est un autre barème, on garde le plus favorable des deux, jamais la somme
        double paid = rod == null ? 0.0D : this.config.jobGain(rod, stack.getType().name(), false);
        double price = sell.active()
                ? Math.max(paid, ((ValoriaTools) this.plugin).sellPrice(ToolKind.ROD, stack.getType()))
                : -1.0D;
        if (price > 0.0D) {
            item.remove();
            double amount = round(price * stack.getAmount());
            amount *= 1.0D + this.config.effect(kindConfig, "MONEY_MULT", tier, levels)
                    .amount("percent", 0.0D) / 100.0D;
            ((ValoriaTools) this.plugin).stats().money(player, ToolKind.ROD, amount);
            EconomyService.Outcome credited = this.economy.deposit(player, round(amount));
            player.sendMessage(credited.success()
                    ? MultiTool.color("&a+" + this.economy.format(round(amount)) + "&7 (pêche vendue)")
                    : MultiTool.color("&cPêche vendue mais non payée : " + credited.reason()));
        } else if (this.config.effect(kindConfig, "AUTO_REEL", tier, levels).active()) {
            // « peche plus rapide » : la prise va direct dans le sac, le joueur n'a pas a mouliner
            item.remove();
            giveOrDrop(player, player.getWorld(), stack);
            event.setCancelled(true);
        }
        double xp = Math.max(1, this.config.xpPerBlock(kindConfig))
                + (rod == null ? 0.0D : this.config.jobGain(rod, stack.getType().name(), true));
        xp *= 1.0D + this.config.effect(kindConfig, "XP_MULT", tier, levels)
                .amount("percent", 0.0D) / 100.0D;
        ToolsConfig.Effect seeker = this.config.effect(kindConfig, "XP_FLAT", tier, levels);
        if (seeker.active() && Abilities.proc(seeker.chance("chance", 0.1D))) {
            xp += seeker.value("amount", 5);
        }
        int whole = settle(player.getUniqueId(), xp);
        if (whole > 0) {
            giveExperience(player, whole);
        }
    }

    /**
     * « Angler » : raccourcir le temps d'attente du bobber. <code>setWaitTime</code> est du Paper (et sa
     * signature a changé d'une version à l'autre), donc l'appel se fait par réflexion sur l'API publique
     * de l'entité : sur un serveur qui ne l'a pas, la pêche garde son rythme vanilla et rien d'autre
     * n'est affecté.
     */
    private void shortenWait(final Player player, PlayerFishEvent event, ToolsConfig.Effect angler) {
        if (!angler.active() || !Abilities.proc(angler.chance("bite-chance", 0.5D))) {
            return;
        }
        final Entity hook;
        try {
            hook = event.getHook();
        } catch (RuntimeException | LinkageError unavailable) {
            return;
        }
        if (hook == null) {
            return;
        }
        final int ticks = Math.max(20, angler.value("ticks", 60));
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new Runnable() {

            @Override
            public void run() {
                try {
                    for (Method method : hook.getClass().getMethods()) {
                        if (!method.getName().equals("setWaitTime") || method.getParameterTypes().length == 0) {
                            continue;
                        }
                        if (method.getParameterTypes().length == 1) {
                            method.invoke(hook, Integer.valueOf(ticks));
                        } else {
                            method.invoke(hook, Integer.valueOf(Math.max(10, ticks / 2)), Integer.valueOf(ticks));
                        }
                        break;
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
                    // pas de setter public sur ce serveur : l'Angler se limite a son anti-rentree
                }
            }
        }, 1L);
    }

    /**
     * Un bonus de pêche <b>honnête</b> : on ne rejoue pas la table de trésors du serveur (ce qui
     * demanderait de toucher aux méthodes privées du jeu). On ajoute un lot d'items déclaré dans la
     * config — donc le admin garde la main sur ce qui tombe, et le serveur garde le sien.
     */
    private void grantTreasure(Player player, ToolsConfig.Effect luck) {
        ensureTreasurePool();
        if (this.treasurePool.isEmpty()) {
            return;
        }
        double chance = Math.min(0.9D, luck.chance("treasure-chance", 0.05D));
        if (!Abilities.proc(chance)) {
            return;
        }
        Material pick = this.treasurePool.get((int) (Math.random() * this.treasurePool.size()));
        player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(pick, 1));
    }

    private void ensureTreasurePool() {
        if (!this.treasurePool.isEmpty()) {
            return;
        }
        for (String name : this.config.treasureItems()) {
            Material found = material(name);
            if (found != null) {
                this.treasurePool.add(found);
            }
        }
        // aucun tresor configure : on ne invente pas de recompense, la capacite reste muette
        // (le log l'a deja signalee au reload) plutot que de distribuer un item de remplissage.
        if (this.treasurePool.isEmpty()) {
            this.plugin.getLogger().warning("capacité LUCK active sans `tool.treasure.items` :"
                    + " aucun objet additionnel ne tombera.");
        }
    }

    // ------------------------------------------------------------------ combat

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (this.handling || !this.config.enabled()) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getDamager();
        if (heldMultiTool(player) == null) {
            return;
        }
        ToolsConfig.KindConfig kindConfig = this.config.kind(ToolKind.SWORD);
        if (kindConfig == null) {
            return;
        }
        int tier = this.store.tierOf(player.getUniqueId(), ToolKind.SWORD, this.config.maxTier(kindConfig));
        Map<String, Integer> levels = this.store.levelsOf(player.getUniqueId(), ToolKind.SWORD);
        Abilities abilities = ((ValoriaTools) this.plugin).abilities();

        double damage = event.getDamage();
        damage *= 1.0D + this.config.effect(kindConfig, "DAMAGE_MULT", tier, levels).amount("percent", 0.0D) / 100.0D;
        ToolsConfig.Effect crit = this.config.effect(kindConfig, "CRIT", tier, levels);
        if (crit.active() && Abilities.proc(crit.chance("chance", 0.2D))) {
            damage *= Math.max(1.0D, crit.amount("multiplier", 1.5D));
        }
        event.setDamage(damage);

        ToolsConfig.Effect knockback = this.config.effect(kindConfig, "KNOCKBACK", tier, levels);
        if (knockback.active() && event.getEntity() instanceof LivingEntity) {
            push((LivingEntity) event.getEntity(), player, knockback.amount("strength", 0.6D));
        }
        ToolsConfig.Effect steal = this.config.effect(kindConfig, "LIFE_STEAL", tier, levels);
        if (steal.active() && Abilities.proc(steal.chance("chance", 0.25D))) {
            double heal = Math.max(0.5D, steal.amount("heal-hearts", 1.0D));
            try {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            } catch (RuntimeException tooMuch) {   // setHealth leve un IllegalArgumentException, fils de celui-ci
                // hors bornes de sante du serveur : le soin est ignore, le coup reste porte
            }
        }
        ToolsConfig.Effect force = this.config.effect(kindConfig, "POTION_APPLY", tier, levels);
        if (force.active() && Abilities.proc(force.chance("chance", 0.1D))) {
            abilities.empower(player, force);
        }
        ToolsConfig.Effect swift = this.config.effect(kindConfig, "SWIFT", tier, levels);
        if (swift.active()) {
            abilities.swift(player, swift);
        }
        ToolsConfig.Effect haste = this.config.effect(kindConfig, "HASTE", tier, levels);
        if (haste.active()) {
            abilities.haste(player, haste);
        }
        ToolsConfig.Effect cleave = this.config.effect(kindConfig, "MULTI_KILL", tier, levels);
        if (cleave.active() && Abilities.proc(cleave.chance("chance", 0.05D))) {
            cleave(event.getEntity(), Math.max(1, Math.min(Abilities.MAX_RADIUS, cleave.value("radius", 2))),
                    Math.max(0.1D, cleave.amount("multiplier", 0.6D)), damage);
        }
        final ToolsConfig.Effect auto = this.config.effect(kindConfig, "AUTO_SWING", tier, levels);
        if (auto.active() && Abilities.proc(auto.chance("chance", 0.05D)) && event.getEntity() instanceof LivingEntity) {
            final LivingEntity target = (LivingEntity) event.getEntity();
            final double extra = Math.max(1.0D, damage * Math.max(0.1D, auto.amount("multiplier", 0.8D)));
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new Runnable() {

                @Override
                public void run() {
                    try {
                        if (target.isDead() || !target.isValid()) {
                            return;
                        }
                        target.damage(extra);
                    } catch (RuntimeException | LinkageError refused) {
                        // entite protegee ou deja partie : le second coup est simplement perdu
                    }
                }
            }, Math.max(2L, auto.value("interval", 4)));
        }
    }

    /**
     * « Briseur de monstres » : une fraction du coup en cours se propage autour de la cible. Les joueurs
     * sont exclus — une capacité de fermage ne doit jamais devenir une arme contre d'autres joueurs.
     */
    private void cleave(Entity center, int radius, double multiplier, double damage) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        try {
            for (Entity nearby : center.getWorld().getNearbyEntities(center.getLocation(), radius, radius, radius)) {
                if (nearby == null || nearby.equals(center) || !(nearby instanceof LivingEntity)) {
                    continue;
                }
                LivingEntity living = (LivingEntity) nearby;
                if (living.isDead() || living instanceof Player) {
                    continue;   // on ne vole pas la vie des autres joueurs avec une capacite de fermage
                }
                living.damage(Math.max(1.0D, damage * multiplier));
            }
        } catch (RuntimeException | LinkageError failed) {
            warnOnce("briseur de monstres", failed);
        }
    }

    private void push(LivingEntity target, Player player, double strength) {
        try {
            org.bukkit.util.Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
            push.setY(Math.max(0.1D, push.getY() * 0.25D));
            target.setVelocity(push.normalize().multiply(Math.max(0.0D, strength)));
        } catch (RuntimeException | IllegalArgumentException broken) {
            // entite sans vélocité (armure, cadre) : rien a deplacer
        }
    }

    /**
     * Les gains du métier de chasseur passent par la <em>mort</em>, pas par le coup : c'est le seul
     * moment où le serveur a déjà décidé que la cible est tuée par ce joueur, et donc le seul où une
     * récompense ne peut pas être farmée sur un mob qui survit.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        if (this.handling || !this.config.enabled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        Player player = victim == null ? null : victim.getKiller();
        if (player == null || heldMultiTool(player) == null) {
            return;
        }
        if (!this.config.allowsWorld(victim.getWorld() == null ? null : victim.getWorld().getName())) {
            return;   // un monde hors liste ne rapporte rien, meme en PVP ou en evenement
        }
        ToolsConfig.KindConfig kindConfig = this.config.kind(ToolKind.SWORD);
        if (kindConfig == null) {
            return;
        }
        int tier = this.store.tierOf(player.getUniqueId(), ToolKind.SWORD, this.config.maxTier(kindConfig));
        Map<String, Integer> levels = this.store.levelsOf(player.getUniqueId(), ToolKind.SWORD);
        ToolsConfig.Effect twice = this.config.effect(kindConfig, "DOUBLE_DROP", tier, levels);
        if (twice.active() && Abilities.proc(twice.chance("chance", 0.1D))) {
            List<ItemStack> drops = event.getDrops();
            if (drops != null && !drops.isEmpty()) {
                for (ItemStack drop : new ArrayList<ItemStack>(drops)) {
                    if (drop != null && isDroppable(drop)) {
                        drops.add(drop.clone());
                    }
                }
            }
        }
        // le metier paie la MORT, une fois : c'est le seul moment ou le serveur a decide qui a tue
        ((ValoriaTools) this.plugin).stats().gesture(player, ToolKind.SWORD, ToolStats.Metric.KILLS, 1.0D);
        double money = this.config.jobGain(kindConfig, victim.getType().name(), false);
        ToolsConfig.Effect pouch = this.config.effect(kindConfig, "MONEY_POUCH", tier, levels);
        if (pouch.active() && Abilities.proc(pouch.chance("chance", 0.05D))) {
            money += pouch.amount("amount", 15.0D);
        }
        money *= 1.0D + this.config.effect(kindConfig, "MONEY_MULT", tier, levels).amount("percent", 0.0D) / 100.0D;
        ToolsConfig.Effect double_ = this.config.effect(kindConfig, "MONEY_DOUBLE", tier, levels);
        if (double_.active() && Abilities.proc(double_.chance("chance", 0.05D))) {
            money *= Math.max(2.0D, double_.amount("multiplier", 2.0D));
        }
        Fury fury = this.furies.get(player.getUniqueId());
        if (fury != null && fury.live()) {
            money *= fury.multiplier();
        }
        if (money > 0.0D) {
            ((ValoriaTools) this.plugin).stats().money(player, ToolKind.SWORD, money);
            credit(player, round(money), "monstre tué");
        }
        double xp = this.config.jobGain(kindConfig, victim.getType().name(), true)
                + Math.max(0, this.config.xpPerBlock(kindConfig));
        xp *= 1.0D + this.config.effect(kindConfig, "XP_MULT", tier, levels)
                .amount("percent", 0.0D) / 100.0D;
        ToolsConfig.Effect seeker = this.config.effect(kindConfig, "XP_FLAT", tier, levels);
        if (seeker.active() && Abilities.proc(seeker.chance("chance", 0.1D))) {
            xp += seeker.value("amount", 3);
        }
        int whole = settle(player.getUniqueId(), xp);
        if (whole > 0) {
            giveExperience(player, whole);
        }
        ToolsConfig.Effect sell = this.config.effect(kindConfig, "SELL_ON_BREAK", tier, levels);
        if (sell.active()) {
            sellDrops(player, event, kindConfig, tier, levels);
        }
    }

    /** Vend les drops du monstre sur-le-champ : la capacité « vente automatique » côté combat. */
    private void sellDrops(Player player, EntityDeathEvent event, ToolsConfig.KindConfig kindConfig,
            int tier, Map<String, Integer> levels) {
        List<ItemStack> drops = event.getDrops();
        if (drops == null || drops.isEmpty()) {
            return;
        }
        double total = 0.0D;
        int sold = 0;
        for (java.util.Iterator<ItemStack> iterator = drops.iterator(); iterator.hasNext();) {
            ItemStack drop = iterator.next();
            if (drop == null || drop.getType() == Material.AIR) {
                continue;
            }
            double price = ((ValoriaTools) this.plugin).sellPrice(ToolKind.SWORD, drop.getType());
            if (price <= 0.0D || price < this.config.sellMinValue()) {
                continue;
            }
            total += price * drop.getAmount() * this.config.sellMultiplier();
            sold += drop.getAmount();
            iterator.remove();
        }
        if (sold == 0) {
            return;
        }
        total *= 1.0D + this.config.effect(kindConfig, "MONEY_MULT", tier, levels).amount("percent", 0.0D) / 100.0D;
        EconomyService.Outcome credited = this.economy.deposit(player, round(total));
        if (credited.success()) {
            player.sendMessage(MultiTool.color("&a+" + this.economy.format(round(total)) + "&7 (" + sold
                    + " drop(s) de monstre vendu(s))"));
        }
    }

    /**
     * Bloc de bois (tronc ou feuillage) : le compteur « arbres » compte UN abattage, pas les 24 blocs
     * qui composent l'arbre — sinon le classement récompenserait le bucheron deux cents fois par arbre.
     */
    private static boolean isLogBlock(Material material) {
        String name = material == null ? "" : material.name().toLowerCase(Locale.ROOT);
        return name.contains("log") || name.contains("stem") || name.contains("hyphae")
                || name.contains("leaves") || name.contains("wood");
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isDroppable(ItemStack drop) {
        return drop != null && drop.getType() != Material.AIR && drop.getAmount() > 0;
    }

    /** Deux blocs identiques ? Comparaison par coordonnees : `Block#equals` n'est pas garanti partout. */
    private static boolean sameBlock(Block a, Block b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
                && a.getWorld() != null && b.getWorld() != null
                && a.getWorld().getName().equals(b.getWorld().getName());
    }

    private static boolean containsBlock(List<Block> blocks, Block needle) {
        for (Block block : blocks) {
            if (sameBlock(block, needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameWorld(Block a, Block b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }

    private static Material material(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            Material found = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
            return found == null || found == Material.AIR || !found.isItem() ? null : found;
        } catch (RuntimeException | LinkageError unknown) {
            return null;
        }
    }

    private static String pretty(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.length() == 0 ? enumName : out.toString();
    }

    /**
     * Un bloc qui a une interface ne doit jamais être « miné » par un clic droit : la liste est
     * textuelle (et non <code>instanceof Container</code>) pour rester correcte sur les versions où
     * l'API des conteneurs a bougé.
     */
    private static boolean hasInventory(Block block) {
        String name = block.getType().name().toLowerCase(Locale.ROOT);
        return name.contains("chest") || name.contains("furnace") || name.contains("barrel")
                || name.contains("shulker") || name.contains("hopper") || name.contains("brewing")
                || name.contains("enchanting") || name.contains("anvil") || name.contains("smoker")
                || name.contains("blast") || name.contains("lectern") || name.contains("grindstone");
    }

    /** L'outil tenu en main (les deux mains sont testées : le joueur peut le tenir à gauche). */
    private static ItemStack heldMultiTool(Player player) {
        ItemStack main = null;
        try {
            main = player.getInventory().getItemInMainHand();
        } catch (RuntimeException | LinkageError legacy) {
            ItemStack[] contents = player.getInventory().getStorageContents();
            if (contents.length > 0) {
                main = contents[0];
            }
        }
        if (MultiTool.isMultiTool(main)) {
            return main;
        }
        try {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (MultiTool.isMultiTool(off)) {
                return off;
            }
        } catch (RuntimeException | LinkageError legacy) {
            // pas de main secondaire sur ce serveur
        }
        return null;
    }

    static double round(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0.0D;
        }
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static double trim(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.round(value * 100.0D) / 100.0D;
    }

    /** Un seul avertissement par type de panne : un tick de tycoon produit des milliers d'événements. */
    private void warnOnce(String context, Throwable cause) {
        if (this.warned) {
            return;
        }
        this.warned = true;
        this.plugin.getLogger().warning("[multi-outil] " + context + " en échec (" + cause.getClass().getName()
                + " : " + cause.getMessage() + ") — les prochaines pannes du même type ne seront plus journalisées.");
    }

    /** Le plugin sait si un joueur tient l'outil (commande /tools give, GUI). */
    public static boolean holding(Player player) {
        return heldMultiTool(player) != null;
    }
}
