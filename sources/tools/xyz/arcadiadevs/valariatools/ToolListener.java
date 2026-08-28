package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
 * <h2>Aucune API incertaine n'est appelée en dur</h2>
 * <p>Pas de <code>getTargetBlock</code> (comportement de raycast redessiné selon les versions), pas de
 * reflection dans les méthodes privées du serveur pour la XP. Tout ce qui n'est pas garanti soit par
 * la doc Bukkit, soit par les classes livrées du plugin, est ou bien retiré, ou bien exécuté sous
 * <code>try/catch</code> avec un repli utile. C'est ce qui permet au même jar de tourner de 1.7 à 26.x
 * sans <code>NoSuchMethodError</code>.</p>
 */
public final class ToolListener implements Listener {

    private final JavaPlugin plugin;
    private final ToolsConfig config;
    private final ToolStore store;
    private final EconomyService economy;
    private final List<Material> treasurePool = new ArrayList<Material>();
    private boolean handling;
    private boolean warned;
    private final Map<UUID, Long> castCooldown = new HashMap<UUID, Long>();

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
    }

    /** Un joueur qui rejoint avec un outil doit voir la lore à jour (paliers rechargés, pas périmés). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = heldMultiTool(player);
        if (tool != null) {
            MultiTool.refresh(tool, this.config, this.store, player.getUniqueId());
        }
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
        ToolKind kind = ((ValoriaTools) this.plugin).matcher().kindOf(block);
        if (kind == null) {
            return;
        }
        ToolsConfig.KindConfig kindConfig = this.config.kind(kind);
        if (kindConfig == null) {
            return;
        }
        int tier = this.store.tierOf(player.getUniqueId(), kind, this.config.maxTier(kindConfig));

        event.setCancelled(true);
        this.handling = true;
        try {
            breakWithAbilities(player, block, kind, kindConfig, tier);
        } catch (RuntimeException | LinkageError failed) {
            warnOnce("cassure", failed);
        } finally {
            this.handling = false;
        }
    }

    /**
     * Le cœur du plugin : sélection des blocs (filon, arbre, ou bloc seul), drops calculés, capacités
     * appliquées, vente éventuelle, usure.
     *
     * <p>Le bloc d'origine est traité <em>en premier</em> et jamais retiré deux fois : un filon dont le
     * point de départ aurait été réordonné par le parcours laisserait un bloc fantôme au sol.</p>
     */
    private void breakWithAbilities(Player player, Block block, ToolKind kind,
            ToolsConfig.KindConfig kindConfig, int tier) {
        Abilities abilities = ((ValoriaTools) this.plugin).abilities();
        List<Block> targets = new ArrayList<Block>();
        ToolsConfig.Ability tree = this.config.ability(kindConfig, "TREE_FELL", tier);
        ToolsConfig.Ability vein = this.config.ability(kindConfig, "VEIN", tier);
        if (kind == ToolKind.AXE && tree != null) {
            targets.addAll(abilities.tree(block, tree, tier));
        } else if (vein != null) {
            targets.addAll(abilities.vein(block, vein, tier, vein.flag("similar-blocks-only", true)));
        }
        if (targets.isEmpty()) {
            targets.add(block);
        }

        List<ItemStack> drops = new ArrayList<ItemStack>();
        int broken = 0;
        for (Block target : targets) {
            if (target == null || target.getType() == Material.AIR || !sameWorld(target, block)) {
                continue;
            }
            drops.addAll(abilities.dropsOf(target, player));
            abilities.remove(target);
            broken++;
        }
        if (drops.isEmpty()) {
            chargeDurability(player, kindConfig, tier);
            return;
        }

        ToolsConfig.Ability smelt = this.config.ability(kindConfig, "AUTO_SMELT", tier);
        if (smelt != null) {
            drops = abilities.smelt(drops);
        }
        ToolsConfig.Ability fortune = this.config.ability(kindConfig, "FORTUNE", tier);
        if (fortune != null) {
            drops = abilities.multiply(drops, Math.min(0.95D, fortune.decimalAt("chance", tier - 1, 0.25D)),
                    fortune.valueAt("extra-min", tier - 1, 1), fortune.valueAt("extra-max", tier - 1, 2));
        }
        ToolsConfig.Ability twice = this.config.ability(kindConfig, "DOUBLE_DROP", tier);
        if (twice != null) {
            drops = abilities.multiply(drops, Math.min(0.95D, twice.decimalAt("chance", tier - 1, 0.2D)), 1, 1);
        }

        ToolsConfig.Ability sell = this.config.ability(kindConfig, "SELL_ON_BREAK", tier);
        boolean sellOnlySneaking = this.config.sellOnlyWhenSneaking();
        boolean selling = sell != null && (!sellOnlySneaking || player.isSneaking());
        List<ItemStack> kept = new ArrayList<ItemStack>();
        double total = 0.0D;
        int sold = 0;
        for (ItemStack drop : drops) {
            if (!isDroppable(drop)) {
                continue;
            }
            double price = selling ? ((ValoriaTools) this.plugin).sellPrice(kind, drop.getType()) : -1.0D;
            if (price > 0.0D && price >= this.config.sellMinValue()) {
                total += price * drop.getAmount();
                sold += drop.getAmount();
                continue;
            }
            kept.add(drop);
        }
        World world = block.getWorld();
        for (ItemStack drop : kept) {
            giveOrDrop(player, world, block, drop);
        }
        if (sold > 0 && total > 0.0D) {
            double amount = round(total);
            // La vente est un CREDIT : elle ne peut pas echouer faute de solde. Un refus ici vient du
            // fournisseur, et le perdre doit rester visible dans le log — pas dans le sac du joueur.
            EconomyService.Outcome credited = this.economy.deposit(player, amount);
            if (credited.success()) {
                player.sendMessage(MultiTool.color("&a+" + this.economy.format(amount) + "&7 (" + sold
                        + " item(s) vendu(s) par l'outil)"));
            } else {
                this.plugin.getLogger().warning("[multi-outil] credit refuse pour " + player.getName() + " : "
                        + credited.reason() + " — " + sold + " item(s) vendu(s) non payes");
            }
        }

        int xp = this.config.xpPerBlock(kindConfig) * Math.max(1, broken);
        if (xp > 0) {
            giveExperience(player, xp);
        }
        chargeDurability(player, kindConfig, tier);
    }

    // ------------------------------------------------------------------ usure

    /**
     * Une seule usure par geste, même sur un filon de 24 blocs : c'est le comportement d'un outil
     * vein‑mine, et un plafond par clic empêche un filon géant de briser l'item en un coup.
     */
    private void chargeDurability(Player player, ToolsConfig.KindConfig kindConfig, int tier) {
        if (this.config.ability(kindConfig, "INFINITE_DURABILITY", tier) != null || this.config.unbreakable()) {
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
            player.sendMessage(MultiTool.color("&cTon multi-outil s'est brise : ses capacites te restent,"
                    + " refais-en un avec /tools give."));
            return;
        }
        damageable.setDamage(damage);
        tool.setItemMeta(meta);
    }

    private void playBreakSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
        } catch (IllegalArgumentException | NoSuchFieldError | NoClassDefFoundError legacy) {
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
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError legacy) {
            // pas de XP sur ce serveur : aucun mecanique du plugin n'en depend
        }
    }

    /** L'item va dans l'inventaire s'il y a de la place, sinon au sol — jamais perdu. */
    private void giveOrDrop(Player player, World world, Block block, ItemStack drop) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
        if (overflow.isEmpty()) {
            return;
        }
        for (ItemStack left : overflow.values()) {
            try {
                world.dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), left);
            } catch (RuntimeException failed) {
                // monde decharge sous le joueur : l'item est perdu, on le dit plutot que de crasher
                this.plugin.getLogger().warning("[multi-outil] drop non rendu (monde inatteignable) : "
                        + left.getType());
            }
        }
    }

    // ------------------------------------------------------------------ clic droit

    /**
     * Le clic droit n'est intercepté que pour la canne (pour éviter le double‑lancer) ; ailleurs le
     * jeu garde la main — un outil ne doit jamais empêcher d'ouvrir un coffre ou d'activer un levier.
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
        Player player = event.getPlayer();
        if (heldMultiTool(player) == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block != null && hasInventory(block)) {
            return;
        }
        ToolKind kind = block == null ? ((ValoriaTools) this.plugin).matcher().fallbackKind()
                : ((ValoriaTools) this.plugin).matcher().kindOf(block);
        if (kind != ToolKind.ROD) {
            return;
        }
        if (block != null) {
            // on ne lance pas la ligne a travers un bloc : le geste serait perdu
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
        ToolsConfig.KindConfig kindConfig = this.config.kind(ToolKind.ROD);
        if (kindConfig == null) {
            return;
        }
        int tier = this.store.tierOf(player.getUniqueId(), ToolKind.ROD, this.config.maxTier(kindConfig));
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            // le bobber est en train de tomber : rien a faire, juste liberer le anti‑spam
            this.castCooldown.put(player.getUniqueId(), Long.valueOf(0L));
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (this.config.ability(kindConfig, "AUTO_REEL", tier) != null) {
            this.castCooldown.put(player.getUniqueId(), Long.valueOf(0L));
        }
        ToolsConfig.Ability luck = this.config.ability(kindConfig, "LUCK", tier);
        if (luck != null) {
            grantTreasure(player, luck, tier);
        }
        ToolsConfig.Ability sell = this.config.ability(kindConfig, "SELL_ON_BREAK", tier);
        Entity caught = event.getCaught();
        if (sell != null && caught instanceof org.bukkit.entity.Item) {
            ItemStack caughtItem = ((org.bukkit.entity.Item) caught).getItemStack();
            double price = ((ValoriaTools) this.plugin).sellPrice(ToolKind.ROD, caughtItem.getType());
            if (price > 0.0D) {
                caught.remove();
                double amount = round(price * Math.max(1, caughtItem.getAmount()));
                EconomyService.Outcome credited = this.economy.deposit(player, amount);
                player.sendMessage(credited.success()
                        ? MultiTool.color("&a+" + this.economy.format(amount) + "&7 (peche vendue)")
                        : MultiTool.color("&cPeche vendue mais non payee : " + credited.reason()));
            }
        }
    }

    /**
     * Un bonus de peche <b>honnête</b> : on ne rejoue pas la table de trésors du serveur (ce qui
     * demanderait de toucher aux méthodes privées du jeu). On ajoute un lot d'items déclaré dans la
     * config — donc le admin garde la main sur ce qui tombe, et le serveur garde le sien.
     */
    private void grantTreasure(Player player, ToolsConfig.Ability luck, int tier) {
        ensureTreasurePool();
        if (this.treasurePool.isEmpty()) {
            return;
        }
        double chance = Math.min(0.5D, luck.decimalAt("treasure-chance", tier - 1, 0.05D));
        if (Math.random() >= chance) {
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
            try {
                Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
                if (material != null && material != Material.AIR && material.isItem()) {
                    this.treasurePool.add(material);
                }
            } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError unknown) {
                // materiau inconnu du serveur : ignore, la peche reste normale
            }
        }
        // aucun tresor configure : on ne invente pas de recompense, la capacite reste muette
        // (le log l'a deja signalee au reload) plutot que de distribuer un item de remplissage.
        if (this.treasurePool.isEmpty()) {
            this.plugin.getLogger().warning("capacite LUCK active sans `tool.treasure.items` :"
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

        ToolsConfig.Ability crit = this.config.ability(kindConfig, "CRIT", tier);
        if (crit != null && Math.random() < Math.min(0.9D, crit.decimalAt("chance", tier - 1, 0.2D))) {
            event.setDamage(event.getDamage() * Math.max(1.0D, crit.decimalAt("multiplier", tier - 1, 1.5D)));
        }
        ToolsConfig.Ability knockback = this.config.ability(kindConfig, "KNOCKBACK", tier);
        if (knockback != null && event.getEntity() instanceof LivingEntity) {
            try {
                double strength = Math.max(0.0D, knockback.decimalAt("strength", tier - 1, 0.6D));
                org.bukkit.util.Vector push = event.getEntity().getLocation().toVector()
                        .subtract(player.getLocation().toVector());
                push.setY(Math.max(0.1D, push.getY() * 0.25D));
                event.getEntity().setVelocity(push.normalize().multiply(strength));
            } catch (IllegalArgumentException pushed) {
                // entite sans vélocité (armure, cadre) : rien a deplacer
            }
        }
        ToolsConfig.Ability steal = this.config.ability(kindConfig, "LIFE_STEAL", tier);
        if (steal != null && Math.random() < Math.min(0.95D, steal.decimalAt("chance", tier - 1, 0.25D))) {
            double heal = Math.max(0.5D, steal.decimalAt("heal-hearts", tier - 1, 1.0D));
            try {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            } catch (RuntimeException tooMuch) {   // setHealth leve un IllegalArgumentException, fils de celui-ci
                // hors bornes de sante du serveur : le soin est ignore, le coup reste porte
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isDroppable(ItemStack drop) {
        return drop != null && drop.getType() != Material.AIR && drop.getAmount() > 0;
    }

    private static boolean sameWorld(Block a, Block b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getWorld() != null && a.getWorld().equals(b.getWorld());
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
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError legacy) {
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
        } catch (RuntimeException | NoSuchMethodError | NoClassDefFoundError legacy) {
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

    /** Un seul avertissement par type de panne : un tick de tycoon produit des milliers d'événements. */
    private void warnOnce(String context, Throwable cause) {
        if (this.warned) {
            return;
        }
        this.warned = true;
        this.plugin.getLogger().warning("[multi-outil] " + context + " en echec (" + cause.getClass().getName()
                + " : " + cause.getMessage() + ") — les prochaines pannes du meme type ne seront plus journalisees.");
    }

    /** Le plugin sait si un joueur tient l'outil (commande /tools give, GUI). */
    public static boolean holding(Player player) {
        return heldMultiTool(player) != null;
    }
}
