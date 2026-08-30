package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Le contrat de l'item : <b>un seul</b> multi-outil par joueur, et il ne quitte pas son sac.
 *
 * <h2>Pourquoi une garde, et pas seulement un <code>give</code> poli</h2>
 * <p>Tout ce qui fait la valeur de l'outil vit dans <code>tools.yml</code>, par UUID (voir
 * {@link ToolStore}) ; l'item, lui, ne porte qu'une marque. Deux conséquences, et cette classe les tient
 * toutes les deux :</p>
 * <ul>
 *   <li><b>un doublon ne rapporte rien et pourrit tout</b> : deux outils dans un sac, c'est deux lore à
 *       lire, et surtout un objet qu'on peut échanger comme un matériau. La garde en laisse exactement
 *       un, et retire les autres.</li>
 *   <li><b>un outil au sol est un outil qui n'est plus à personne</b> : a fortiori sur un serveur où il
 *       n'a aucun prix. Le clic Q, le transfert vers un coffre, le hopper, l'accroche à un cadre, le lâcher
 *       à la mort : chaque voie de sortie est fermée, et ce qui traîne malgré tout (copie déposée avant la
 *       mise à jour, item recraché par un autre plugin) va dans la main de son joueur, pas au premier
 *       passant.</li>
 * </ul>
 *
 * <h2>Ce qui reste permis</h2>
 * <p>Le joueur garde la main sur <em>son</em> inventaire : il déplace l'outil, le passe à sa main
 * secondaire, le range où il veut chez lui. Ce qui est refusé est ce qui le fait <em>sortir</em> de son
 * sac. Les trois réglages <code>tool.undroppable</code>, <code>tool.single-per-player</code> et
 * <code>tool.auto-give</code> coupent chacun un volet sans toucher au reste du plugin : les capacités
 * continuent de marcher sur un serveur où l'objet se transmet.</p>
 *
 * <h2>Deux détails qui comptent</h2>
 * <p>Le clic de sac est traité <b>sans</b> <code>ignoreCancelled</code> : sinon un plugin de protection qui
 * annule déjà le clic nous aveugle, et l'outil sort par la porte d'à côté. Le menu, lui, n'est pas
 * concerné — son propre listener annule chacun de ses clics. Ensuite : un
 * <code>InventoryClickEvent</code> de type <code>DROP</code> ne passe <em>pas</em> par
 * <code>PlayerDropItemEvent</code> ; compter sur ce dernier pour empêcher de jeter l'outil hors d'un
 * coffre ouvert aurait laissé la faille grande ouverte.</p>
 */
public final class ToolGuard implements Listener {

    /** Le rappel du règlement intérieur, au plus une fois par minute et par joueur. */
    private static final long NAG_MILLIS = 60_000L;

    private final ValoriaTools plugin;
    private final Map<UUID, Long> nagged = new HashMap<UUID, Long>();

    public ToolGuard(ValoriaTools plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ regles

    /** Vrai si l'outil ne doit pas pouvoir quitter le sac de son joueur. */
    private boolean protects() {
        return this.plugin.active() && this.plugin.toolsConfig().undroppable();
    }

    /** Vrai si la règle « un seul exemplaire par joueur » s'applique. */
    private boolean counts() {
        return this.plugin.active() && this.plugin.toolsConfig().singlePerPlayer();
    }

    // ------------------------------------------------------------------ ce qui sort du sac est annule

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (!protects()) {
            return;
        }
        if (MultiTool.isMultiTool(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            remind(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!protects() || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top == null || top.getHolder() instanceof ToolsGui.View) {
            return;      // le menu annule deja tous ses clics
        }
        Player player = (Player) event.getWhoClicked();
        Inventory acted = event.getClickedInventory();
        if (acted == null) {
            return;      // clic dans le vide, hors d'un inventaire
        }
        boolean currentTool = MultiTool.isMultiTool(event.getCurrentItem());
        boolean cursorTool = MultiTool.isMultiTool(event.getCursor());
        if (!currentTool && !cursorTool) {
            return;
        }
        if (event.getClick() == ClickType.CREATIVE || event.getClick() == ClickType.MIDDLE) {
            event.setCancelled(true);       // le clic du milieu (creatif) clonerait la marque
            remind(player);
            return;
        }
        boolean own = acted == player.getInventory();
        boolean onlyOwnInventory = top == player.getInventory();
        if ((currentTool || cursorTool) && !own) {
            event.setCancelled(true);       // vider un coffre de son outil, ou y reposer une copie
            remind(player);
            return;
        }
        if (currentTool && own && !onlyOwnInventory && movesOut(event.getClick())) {
            event.setCancelled(true);       // shift, 1-9, suppression : tout ce qui saute dans l'autre sac
            remind(player);
        }
    }

    /**
     * Ce qu'un clic fait à l'item : tout ce qui le transfère hors de l'inventaire en cours. La touche F
     * ({@code HOTBAR_SWAP}) en est absente exprès : elle échange deux cases <em>du joueur</em>, elle ne
     * fait donc pas sortir l'outil, et l'interdire rendrait le sac désagréable pour rien. Le geste
     * « 1-9 depuis l'autre inventaire » ({@code HOTBAR_MOVE_AND_READD}) en est absent lui aussi, mais pour
     * une autre raison : ce nom n'existe pas dans toutes les versions de l'API (le build le refuse en
     * 1.20.4), et il est déjà couvert — un clic porté dans l'autre inventaire tombe sous la règle
     * « l'outil ne sort pas de ton sac », qui annule avant.
     */
    private static boolean movesOut(ClickType click) {
        if (click == null) {
            return false;
        }
        return click.isShiftClick() || click == ClickType.NUMBER_KEY || click == ClickType.DOUBLE_CLICK
                || click == ClickType.DROP || click == ClickType.CONTROL_DROP;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!protects() || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top == null || top == event.getWhoClicked().getInventory()
                || top.getHolder() instanceof ToolsGui.View) {
            return;   // vue = son seul inventaire : le joueur range chez lui, c'est permis
        }
        Player player = (Player) event.getWhoClicked();
        boolean spreads = MultiTool.isMultiTool(event.getOldCursor());
        if (!spreads) {
            for (Integer slot : event.getRawSlots()) {
                if (slot.intValue() < top.getSize() && MultiTool.isMultiTool(top.getItem(slot.intValue()))) {
                    spreads = true;
                    break;
                }
            }
        }
        if (spreads) {
            event.setCancelled(true);
            remind(player);
        }
    }

    /** Les hoppers ne déplacent pas l'outil, dans un sens ni dans l'autre. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMoveItem(InventoryMoveItemEvent event) {
        if (protects() && MultiTool.isMultiTool(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Un cadre ou un socle ne sert pas de coffre déguisé. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!protects()) {
            return;
        }
        Player player = event.getPlayer();
        if (!MultiTool.isMultiTool(player.getInventory().getItemInMainHand())
                && !MultiTool.isMultiTool(player.getInventory().getItemInOffHand())) {
            return;
        }
        String type = event.getRightClicked().getType().name().toUpperCase(Locale.ROOT);
        if (type.contains("ITEM_FRAME") || type.contains("GLOW_FRAME") || type.contains("ARMOR_STAND")) {
            event.setCancelled(true);
            remind(player);
        }
    }

    /** Ce qui traîne malgré tout va à son joueur, pas au premier passant. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!protects() || !(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack ground = event.getItem().getItemStack();
        if (!MultiTool.isMultiTool(ground)) {
            return;
        }
        Player player = (Player) event.getEntity();
        event.setCancelled(true);
        event.getItem().remove();
        if (count(player) > 0) {
            tell(player, "&eDoublon ramassé puis retiré : un seul multi-outil par joueur.");
            return;
        }
        grant(player);
        tell(player, "&aRécupéré. Ton multi-outil revient dans ta main : il ne se ramasse pas au sol.");
    }

    /**
     * À la mort l'outil ne tombe pas — à la condition expresse qu'il revienne au joueur au respawn. Sans
     * <code>tool.auto-give</code>, le retirer des drops serait le détruire : on le laisse alors tomber
     * normalement, et le joueur le voit plutôt que de le deviner.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!protects() || !this.plugin.toolsConfig().autoGive()) {
            return;
        }
        // On reconstruit la liste plutôt que de retirer à l'itérateur : `getDrops()` n'est pas garanti
        // modifiable selon les implémentations, alors que `setDrops(liste)` l'est depuis la 1.16.
        List<ItemStack> keep = new ArrayList<ItemStack>();
        int removed = 0;
        for (ItemStack drop : event.getDrops()) {
            if (MultiTool.isMultiTool(drop)) {
                removed++;
            } else {
                keep.add(drop);
            }
        }
        if (removed <= 0) {
            return;
        }
        try {
            event.setDrops(keep);
            this.plugin.getLogger().info("multi-outil retenu à la mort de " + event.getEntity().getName()
                    + " (tool.auto-give: il revient au respawn)");
        } catch (RuntimeException | LinkageError hostile) {
            // Un serveur qui refuserait la liste laisse l'outil à terre : l'auto-don au respawn le
            // rattrape. Le log dit pourquoi, le joueur n'est pas pollué d'un échec qu'il ne voit pas.
            this.plugin.getLogger().warning("drops de mort non filtrables (" + hostile.getClass().getSimpleName()
                    + ") : multi-outil laissé à terre, il sera rendu au respawn.");
        }
    }

    // ------------------------------------------------------------------ un seul exemplaire

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        ensureSoon(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        ensureSoon(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            ensureSoon((Player) event.getPlayer());
        }
    }

    /** Le clic du milieu créatif clone l'item avec sa marque : la garde passe juste après. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameMode(PlayerGameModeChangeEvent event) {
        ensureSoon(event.getPlayer());
    }

    /**
     * Met l'inventaire du joueur en conformité : un seul exemplaire, et un exemplaire quand il manque
     * (<code>tool.auto-give</code>). Les cases d'armure sont balayées avec le reste, parce qu'un outil
     * glissé dans un slot de bottes échapperait à toute autre lecture.
     *
     * @return vrai si quelque chose a été retiré ou donné
     */
    public boolean ensure(Player player) {
        if (player == null || !this.plugin.toolsConfig().enabled()) {
            return false;
        }
        ToolsConfig config = this.plugin.toolsConfig();
        boolean touched = false;
        int kept = -1;
        int removed = 0;
        if (counts()) {
            ItemStack[] contents = storage(player);
            for (int slot = 0; slot < contents.length; slot++) {
                if (!MultiTool.isMultiTool(contents[slot])) {
                    continue;
                }
                if (kept < 0) {
                    kept = slot;
                    continue;
                }
                try {
                    player.getInventory().setItem(slot, null);
                    removed++;
                } catch (RuntimeException | LinkageError refused) {
                    // un plugin de sac verrouille l'ecriture : le doublon reste, rien ne casse
                }
            }
            if (removed > 0) {
                player.updateInventory();
                tell(player, "&eUn seul multi-outil par joueur : &f" + removed
                        + " &edoublon(s) retiré(s).");
                touched = true;
            }
        }
        if (kept < 0) {
            if (config.autoGive() && counts()) {
                grant(player);
                tell(player, "&aTon multi-outil t'a été rendu &7(sneak + clic droit : le panneau"
                        + " d'amélioration&7).");
                touched = true;
            }
            return touched;
        }
        ItemStack held = first(player);
        if (held != null) {
            MultiTool.refresh(held, config, this.plugin.store(), player.getUniqueId());
        }
        return touched;
    }

    /** La même chose, reportée d'un tick : toucher un sac pendant sa fermeture est déconseillé. */
    public void ensureSoon(final Player player) {
        if (player == null) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(this.plugin, new Runnable() {

                @Override
                public void run() {
                    ensure(player);
                }
            });
        } catch (RuntimeException | LinkageError refused) {
            ensure(player);   // planificateur indisponible (déchargement en cours) : en direct
        }
    }

    /**
     * L'exemplaire unique, créé s'il manque. C'est le <b>seul</b> chemin par lequel l'item arrive dans un
     * sac : <code>/tools give</code> et <code>/tools buy</code> passent par là, ce qui rend la promesse
     * « un seul outil » vraie même pour l'administration.
     *
     * @return l'item en main, jamais {@code null} pour un joueur connecté
     */
    public ItemStack grant(Player player) {
        ItemStack existing = first(player);
        if (existing != null) {
            MultiTool.refresh(existing, this.plugin.toolsConfig(), this.plugin.store(),
                    player.getUniqueId());
            return existing;
        }
        ItemStack tool = MultiTool.create(this.plugin.toolsConfig(), this.plugin.store(),
                player.getUniqueId());
        putInHand(player, tool);
        return tool;
    }

    /** L'outil atterrit dans la main courante ; ce qu'elle tenait est rangé, et tombe si le sac est plein. */
    private void putInHand(Player player, ItemStack tool) {
        int slot = 0;
        ItemStack displaced = null;
        try {
            slot = Math.max(0, Math.min(8, player.getInventory().getHeldItemSlot()));
            displaced = player.getInventory().getItem(slot);
        } catch (RuntimeException | LinkageError legacy) {
            slot = 0;
        }
        try {
            player.getInventory().setItem(slot, tool);
        } catch (RuntimeException | LinkageError refused) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(tool);
            for (ItemStack left : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            return;
        }
        player.updateInventory();
        if (displaced == null || displaced.getType() == Material.AIR) {
            return;
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(displaced);
        for (ItemStack left : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);   // sac plein : on rend, on ne jette pas
        }
        tell(player, "&7L'objet que tu tenais a été rangé dans ton inventaire.");
    }

    /** Le premier exemplaire trouvé dans le sac du joueur, ou {@code null}. */
    public ItemStack first(Player player) {
        if (player == null) {
            return null;
        }
        for (ItemStack stack : storage(player)) {
            if (MultiTool.isMultiTool(stack)) {
                return stack;
            }
        }
        return null;
    }

    /** Combien d'exemplaires ce joueur porte sur lui. */
    public int count(Player player) {
        int out = 0;
        for (ItemStack stack : storage(player)) {
            if (MultiTool.isMultiTool(stack)) {
                out++;
            }
        }
        return out;
    }

    /** Les 41 cases du joueur (main, armure, main secondaire) — une seule façon de les lire. */
    private static ItemStack[] storage(Player player) {
        try {
            ItemStack[] contents = player.getInventory().getStorageContents();
            return contents == null ? new ItemStack[0] : contents;
        } catch (RuntimeException | LinkageError legacy) {
            try {
                ItemStack[] contents = player.getInventory().getContents();
                return contents == null ? new ItemStack[0] : contents;
            } catch (RuntimeException | LinkageError alsoLegacy) {
                return new ItemStack[0];
            }
        }
    }

    /** Le petit rappel, une fois par minute : assez pour comprendre, pas assez pour spammer. */
    private void remind(Player player) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = this.nagged.get(player.getUniqueId());
        if (last != null && now - last.longValue() < NAG_MILLIS) {
            return;
        }
        this.nagged.put(player.getUniqueId(), Long.valueOf(now));
        tell(player, "&cLe multi-outil ne quitte pas ton sac &7(&fQ&c, coffres, cadres, hoppers : fermé)."
                + " &7Tes paliers sont dans &f/tools&7, pas dans l'objet.");
    }

    private void tell(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(MultiTool.color(message));
        }
    }

    /** Ce que la garde applique, en une ligne — lu par <code>/tools stats</code>. */
    public String describe() {
        return "non-droppable=" + protects() + ", un-seul-exemplaire=" + counts()
                + ", rendu-au-respawn=" + this.plugin.toolsConfig().autoGive();
    }
}
