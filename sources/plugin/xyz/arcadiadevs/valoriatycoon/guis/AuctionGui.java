package xyz.arcadiadevs.valoriatycoon.guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.commands.AuctionHouse;

/**
 * Interface du marché des joueurs : navigation, recherche, tri, vue « mes annonces », achat
 * partiel et annulation, le tout redessiné pour tous les joueurs quand le marché bouge.
 *
 * <p>Les clics ne mutent jamais l'inventaire pendant leur propre événement : l'action est reportée
 * d'un tick par le scheduler, ce qui évite les désynchronisations client/serveur (le clic fantôme
 * qui duplique ou fait perdre un objet). Les mutations elles-mêmes sont dans
 * {@link AuctionHouse}, la vue ne faisant que lire et afficher.</p>
 */
public class AuctionGui implements InventoryHolder {

    private static final int SIZE = 45;
    private static final int LIST_SLOTS = 36;
    private static final int SLOT_PREVIOUS = 36;
    private static final int SLOT_SEARCH = 38;
    private static final int SLOT_REFRESH = 39;
    private static final int SLOT_SORT = 40;
    private static final int SLOT_OWN = 42;
    private static final int SLOT_HELP = 41;
    private static final int SLOT_NEXT = 44;

    /** Une seule vue ouverte par joueur : c'est ce qui rend le rafraîchissement global simple et sûr. */
    private static final Map<UUID, AuctionGui> VIEWS = java.util.Collections.synchronizedMap(new HashMap<UUID, AuctionGui>());
    private static boolean handlerRegistered = false;

    private final Player player;
    private final List<Integer> shownIds = new ArrayList<Integer>();
    private Inventory inventory;
    private int page = 0;
    private int sort = 0;
    private boolean ownOnly = false;
    private String filter = "";

    private AuctionGui(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, AuctionHouse.title());
        this.render();
    }

    /** Ouvre le marché (ou rafraîchit la vue déjà ouverte) pour ce joueur. */
    public static void open(Player player) {
        ensureHandler();
        AuctionGui gui = VIEWS.get(player.getUniqueId());
        if (gui == null) {
            gui = new AuctionGui(player);
            VIEWS.put(player.getUniqueId(), gui);
        } else {
            gui.render();
        }
        player.openInventory(gui.inventory);
    }

    /** Recherche (laisse aussi l'interface ouverte si elle l'était). */
    public static void search(Player player, String filter) {
        ensureHandler();
        AuctionGui gui = VIEWS.get(player.getUniqueId());
        if (gui == null) {
            gui = new AuctionGui(player);
            VIEWS.put(player.getUniqueId(), gui);
        }
        gui.filter = filter == null ? "" : filter.trim();
        gui.page = 0;
        gui.render();
        player.openInventory(gui.inventory);
    }

    /** Vue filtrée sur les annonces du joueur. */
    public static void openOwn(Player player) {
        ensureHandler();
        AuctionGui gui = VIEWS.get(player.getUniqueId());
        if (gui == null) {
            gui = new AuctionGui(player);
            VIEWS.put(player.getUniqueId(), gui);
        }
        gui.ownOnly = true;
        gui.page = 0;
        gui.render();
        player.openInventory(gui.inventory);
    }

    /** Redessine toutes les vues ouvertes : la partie « synchronisé » du marché. */
    public static void refreshAll() {
        if (VIEWS.isEmpty()) {
            return;
        }
        for (AuctionGui gui : new ArrayList<AuctionGui>(VIEWS.values())) {
            gui.render();
        }
    }

    /** Réinitialise les vues d'un joueur qui quitte (la prochaine ouverture repart fraîche). */
    public static void forget(UUID uuid) {
        VIEWS.remove(uuid);
    }

    private static void ensureHandler() {
        if (handlerRegistered) {
            return;
        }
        handlerRegistered = true;
        Bukkit.getPluginManager().registerEvents(new AuctionGui.Handler(), (org.bukkit.plugin.Plugin) ValoriaTycoon.getInstance());
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    private List<Integer> currentIds() {
        return AuctionHouse.sortedIds(this.filter, this.ownOnly ? this.player.getUniqueId() : null, this.sort);
    }

    private int pages(List<Integer> ids) {
        if (ids.isEmpty()) {
            return 1;
        }
        return (ids.size() - 1) / LIST_SLOTS + 1;
    }

    /** Reconstruit le contenu de la vue, en gardant la même {@link Inventory} pour ne pas faire clignoter. */
    private void render() {
        List<Integer> ids = this.currentIds();
        int pages = this.pages(ids);
        if (this.page > pages - 1) {
            this.page = pages - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }
        this.inventory.clear();
        this.shownIds.clear();
        int start = this.page * LIST_SLOTS;
        for (int slot = 0; slot < LIST_SLOTS; ++slot) {
            int index = start + slot;
            if (index >= ids.size()) {
                break;
            }
            int id = ids.get(index).intValue();
            this.shownIds.add(Integer.valueOf(id));
            this.inventory.setItem(slot, this.listingItem(id));
        }
        this.inventory.setItem(SLOT_PREVIOUS, this.button(this.page > 0 ? Material.ARROW : Material.BARRIER,
                "&ePage précédente", "&7Page &f" + (this.page + 1) + "&7/&f" + pages));
        this.inventory.setItem(SLOT_NEXT, this.button(this.page < pages - 1 ? Material.ARROW : Material.BARRIER,
                "&ePage suivante", "&7Page &f" + (this.page + 2) + "&7/&f" + pages));
        this.inventory.setItem(SLOT_SEARCH, this.button(Material.CAMPFIRE,
                this.filter.isEmpty() ? "&eRechercher" : "&aFiltre : &f" + this.filter,
                "&7Tape &f/ah search <motif>", "&7dans le chat pour filtrer par nom d'item.",
                "&7/ah search &7pour tout revoir."));
        this.inventory.setItem(SLOT_SORT, this.button(Material.HOPPER, "&eTrier : " + this.sortName(),
                "&7Clic pour changer de tri.", "&7Actuel : &f" + this.sortName()));
        this.inventory.setItem(SLOT_OWN, this.button(this.ownOnly ? Material.CHEST : Material.ENDER_CHEST,
                this.ownOnly ? "&aToutes les annonces" : "&eMes annonces uniquement",
                "&7Bascule entre le marché entier", "&7et ce que tu as déposé."));
        this.inventory.setItem(SLOT_REFRESH, this.button(Material.CLOCK, "&eActualiser",
                "&7Recharge les prix et les quantités."));
        this.inventory.setItem(SLOT_HELP, this.button(Material.OAK_SIGN, "&aAide du marché",
                "&8» &7Clic gauche : acheter &f1 pièce",
                "&8» &7Clic droit : acheter &fun stack",
                "&8» &7Maj + clic : acheter &ftout le lot",
                "&8» &7Sur &ftes propres &7objets, Maj + clic : &canuler",
                "",
                "&8» &7Mettre en vente : &f/ah sell <prix> [quantité]",
                "&8» &7Récupérer tout : &f/ah cancel",
                "&8» &7Statistiques : &f/ah stats"));
        if (ids.isEmpty()) {
            this.inventory.setItem(13, this.button(Material.CAULDRON, "&eRien sur le marché",
                    "&7Sois le premier : &f/ah sell 100", "&7avec l'item en main."));
        }
    }

    private String sortName() {
        switch (this.sort) {
            case 1:
                return "prix croissant";
            case 2:
                return "prix décroissant";
            case 3:
                return "plus récentes";
            default:
                return "numéro d'annonce";
        }
    }

    /** L'item de l'annonce, enrichi du prix, de la quantité, de la moyenne du marché et de l'échéance. */
    private ItemStack listingItem(int id) {
        ItemStack base = AuctionHouse.itemAt(id);
        if (base == null) {
            return this.button(Material.BEDROCK, "&cAnnonce illisible", "&7Maj + clic pour la retirer.");
        }
        ItemStack view = base.clone();
        ItemMeta meta = view.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
            int amount = AuctionHouse.amountAt(id);
            double unit = AuctionHouse.unitPriceAt(id);
            lore.add("");
            lore.add(AuctionHouse.color("&6Prix unitaire : &a" + AuctionHouse.money(unit)));
            lore.add(AuctionHouse.color("&6Quantité : &f" + amount + " &7(total &f" + AuctionHouse.money(AuctionHouse.totalAt(id)) + "&7)"));
            lore.add(AuctionHouse.color("&6Vendeur : &f" + AuctionHouse.sellerAt(id) + " &8(n°" + id + ")"));
            double average = AuctionHouse.average(base.getType());
            if (average > 0.0D) {
                lore.add(AuctionHouse.color("&6Marché : &f" + AuctionHouse.money(average)
                        + " &7en moyenne, " + AuctionHouse.salesOf(base.getType()) + " vente(s)"));
            }
            long expires = AuctionHouse.expiresAt(id);
            if (expires > 0L) {
                long left = expires - System.currentTimeMillis();
                lore.add(AuctionHouse.color(left <= 0L ? "&cExpirée" : "&7Expire dans &f" + Math.max(1L, left / 3600000L) + "h"));
            }
            boolean mine = this.player.getUniqueId().equals(AuctionHouse.sellerIdOf(id));
            lore.add(AuctionHouse.color(mine ? "&eMaj + clic : annuler et récupérer" : "&eClique pour acheter"));
            meta.setLore(lore);
            view.setItemMeta(meta);
        }
        return view;
    }

    private ItemStack button(Material material, String name, String... lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(AuctionHouse.color(name));
            List<String> lore = new ArrayList<String>();
            for (String line : lines) {
                lore.add(AuctionHouse.color(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Gestionnaire unique pour toutes les vues, enregistré au premier accès au marché. */
    private static final class Handler implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            Inventory top = event.getView().getTopInventory();
            if (!(top.getHolder() instanceof AuctionGui)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != top) {
                return;
            }
            final AuctionGui gui = (AuctionGui) top.getHolder();
            final Player player = gui.player;
            final int slot = event.getRawSlot();
            final boolean shift = event.isShiftClick();
            final boolean right = event.isRightClick();
            final int listing = gui.listingAt(slot);
            Bukkit.getScheduler().runTask(ValoriaTycoon.getInstance(), () -> Handler.handle(gui, player, slot, listing, shift, right));
        }

        private static void handle(AuctionGui gui, Player player, int slot, int listing, boolean shift, boolean right) {
            String message = null;
            if (listing > 0) {
                boolean mine = player.getUniqueId().equals(AuctionHouse.sellerIdOf(listing));
                if (mine && shift) {
                    message = AuctionHouse.cancel(player, listing);
                } else if (mine && player.hasPermission("valoriatycoon.ah.admin")) {
                    message = AuctionHouse.cancel(player, listing);
                } else if (right) {
                    message = AuctionHouse.buy(player, listing, 64);
                } else if (shift) {
                    message = AuctionHouse.buy(player, listing, -1);
                } else {
                    message = AuctionHouse.buy(player, listing, 1);
                }
            } else if (slot == SLOT_PREVIOUS) {
                gui.page = Math.max(0, gui.page - 1);
                gui.render();
                return;
            } else if (slot == SLOT_NEXT) {
                gui.page = gui.page + 1;
                gui.render();
                return;
            } else if (slot == SLOT_SORT) {
                gui.sort = (gui.sort + 1) % 4;
                gui.render();
                return;
            } else if (slot == SLOT_OWN) {
                gui.ownOnly = !gui.ownOnly;
                gui.page = 0;
                gui.render();
                return;
            } else if (slot == SLOT_REFRESH || slot == SLOT_SEARCH || slot == SLOT_HELP) {
                gui.render();
                message = slot == SLOT_HELP ? AuctionHouse.summary(player) : null;
            } else {
                return;
            }
            gui.render();
            if (message != null) {
                player.sendMessage(message);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getView().getTopInventory().getHolder() instanceof AuctionGui) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            Inventory inventory = event.getInventory();
            if (inventory.getHolder() instanceof AuctionGui) {
                AuctionGui gui = (AuctionGui) inventory.getHolder();
                if (gui.inventory == inventory) {
                    VIEWS.remove(gui.player.getUniqueId());
                }
            }
        }
    }

    /** Numéro d'annonce affiché à ce slot, ou {@code 0} si le slot n'est pas une annonce. */
    int listingAt(int slot) {
        if (slot < 0 || slot >= this.shownIds.size()) {
            return 0;
        }
        return this.shownIds.get(slot).intValue();
    }
}
