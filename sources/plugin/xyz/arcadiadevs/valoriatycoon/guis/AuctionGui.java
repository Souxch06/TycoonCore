package xyz.arcadiadevs.valoriatycoon.guis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
 * Vue du marché des joueurs : une page = 18 annonces, deux touches de navigation, deux touches
 * d'action. Les vues de tous les joueurs connectés sont redessinées dès qu'une annonce bouge,
 * ce qui est la partie « synchronisée » du dispositif (aucun joueur ne voit un item déjà vendu).
 *
 * <p>Les mutations sont reportées d'un tick via le scheduler : on ne modifie jamais l'inventaire
 * en cours de clic, ce qui évite les désynchronisations client/serveur (le clic fantôme qui dupique
 * ou fait perdre un item).</p>
 */
public class AuctionGui implements InventoryHolder {

    private static final int SIZE = 27;
    private static final int FIRST_SLOT = 0;
    private static final int LIST_SLOTS = 18;
    private static final int SLOT_PREVIOUS = 18;
    private static final int SLOT_NEXT = 26;
    private static final int SLOT_CANCEL = 22;
    private static final int SLOT_HELP = 24;

    private static final Set<AuctionGui> OPEN = java.util.Collections.synchronizedSet(new LinkedHashSet<AuctionGui>());
    private static boolean handlerRegistered = false;

    private final Player player;
    private int page;
    private Inventory inventory;

    private AuctionGui(Player player, int page) {
        this.player = player;
        this.page = Math.max(0, page);
        this.inventory = Bukkit.createInventory(this, SIZE, AuctionHouse.title());
        this.render();
    }

    /** Ouvre (ou remplace la vue de) une page du marché pour ce joueur. */
    public static void open(Player player, int page) {
        AuctionGui.ensureHandler();
        for (AuctionGui opened : new ArrayList<AuctionGui>(OPEN)) {
            if (opened.player.equals(player)) {
                opened.page = Math.max(0, page);
                opened.render();
                player.openInventory(opened.inventory);
                return;
            }
        }
        AuctionGui gui = new AuctionGui(player, page);
        OPEN.add(gui);
        player.openInventory(gui.inventory);
    }

    /** Redessine toutes les vues ouvertes : c'est là que passe la synchronisation entre joueurs. */
    public static void refreshAll() {
        if (OPEN.isEmpty()) {
            return;
        }
        for (AuctionGui gui : new ArrayList<AuctionGui>(OPEN)) {
            gui.render();
        }
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

    private int pages() {
        int count = AuctionHouse.count();
        if (count == 0) {
            return 1;
        }
        return (count - 1) / LIST_SLOTS + 1;
    }

    /** Reconstruit le contenu de la vue. Peut être appelé à tout moment, y compris pendant un clic. */
    private void render() {
        if (this.page > this.pages() - 1) {
            this.page = this.pages() - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }
        this.inventory.clear();
        List<Integer> ids = AuctionHouse.ids();
        for (int slot = 0; slot < LIST_SLOTS; ++slot) {
            int index = this.page * LIST_SLOTS + slot;
            if (index >= ids.size()) {
                break;
            }
            int id = ids.get(index).intValue();
            this.inventory.setItem(FIRST_SLOT + slot, this.listingItem(id));
        }
        this.inventory.setItem(SLOT_PREVIOUS, this.button(this.page > 0 ? Material.ARROW : Material.GRAY_DYE,
                "&ePage précédente", this.page > 0 ? "&7Page &f" + this.page + "&7/&f" + this.pages() : "&7Première page"));
        this.inventory.setItem(SLOT_NEXT, this.button(this.page < this.pages() - 1 ? Material.ARROW : Material.GRAY_DYE,
                "&ePage suivante", this.page < this.pages() - 1 ? "&7Page &f" + (this.page + 2) + "&7/&f" + this.pages() : "&7Dernière page"));
        this.inventory.setItem(SLOT_CANCEL, this.button(Material.BARRIER, "&cRécupérer mes annonces",
                "&7Annule tes annonces en cours et", "&7te rend les items.", "&7Annonces en vente sur le marché : &f" + ids.size()));
        this.inventory.setItem(SLOT_HELP, this.button(Material.OAK_SIGN, "&aVendre ce que tu tiens",
                "&7Tiens l'item en main puis tape :", "&f/ah sell <prix>", "", "&7Exemple : &f/ah sell 250"));
        if (ids.isEmpty()) {
            this.inventory.setItem(13, this.button(Material.CHEST_MINECART, "&eAucune annonce", "&7Sois le premier à vendre,&a", "&7le marché est vide."));
        }
    }

    /** L'item d'une annonce, avec le prix et le vendeur ajoutés à la description. */
    private ItemStack listingItem(int id) {
        ItemStack base = AuctionHouse.itemAt(id);
        if (base == null) {
            return this.button(Material.BEDROCK, "&cAnnonce cassée", "&7Utilise « récupérer mes annonces ».");
        }
        ItemStack view = base.clone();
        ItemMeta meta = view.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
            lore.add("");
            lore.add(AuctionHouse.color("&6Prix : &a" + AuctionHouse.money(AuctionHouse.priceAt(id))));
            lore.add(AuctionHouse.color("&6Vendeur : &f" + AuctionHouse.sellerAt(id) + " &8(n°" + id + ")"));
            lore.add(AuctionHouse.color("&eClique pour acheter."));
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

    /** Un seul gestionnaire pour toutes les vues, enregistré au premier /ah. */
    private static final class Handler implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent inventoryClickEvent) {
            Inventory top = inventoryClickEvent.getView().getTopInventory();
            if (!(top.getHolder() instanceof AuctionGui)) {
                return;
            }
            inventoryClickEvent.setCancelled(true);
            if (inventoryClickEvent.getClickedInventory() != top) {
                return;
            }
            final AuctionGui gui = (AuctionGui) top.getHolder();
            final Player player = gui.player;
            final int slot = inventoryClickEvent.getRawSlot();
            final int id = gui.listingIdAt(slot);
            Bukkit.getScheduler().runTask(ValoriaTycoon.getInstance(), () -> Handler.handle(gui, player, slot, id));
        }

        private static void handle(AuctionGui gui, Player player, int slot, int id) {
            String message = null;
            if (id > 0) {
                message = AuctionHouse.buy(player, id);
            } else if (slot == SLOT_CANCEL) {
                message = AuctionHouse.cancel(player);
            } else if (slot == SLOT_PREVIOUS) {
                gui.page = Math.max(0, gui.page - 1);
                gui.render();
                return;
            } else if (slot == SLOT_NEXT) {
                gui.page = Math.min(gui.pages() - 1, gui.page + 1);
                gui.render();
                return;
            } else if (slot == SLOT_HELP) {
                message = AuctionHouse.color("&7Tiens l'item en main puis : &f/ah sell <prix>");
            } else {
                return;
            }
            if (message != null) {
                player.sendMessage(message);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent inventoryDragEvent) {
            if (inventoryDragEvent.getView().getTopInventory().getHolder() instanceof AuctionGui) {
                inventoryDragEvent.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent inventoryCloseEvent) {
            if (inventoryCloseEvent.getInventory().getHolder() instanceof AuctionGui) {
                OPEN.remove(inventoryCloseEvent.getInventory().getHolder());
            }
        }
    }

    /** Numéro d'annonce affiché à ce slot, ou {@code 0} si le slot n'est pas une annonce. */
    int listingIdAt(int slot) {
        if (slot < FIRST_SLOT || slot >= FIRST_SLOT + LIST_SLOTS) {
            return 0;
        }
        List<Integer> ids = AuctionHouse.ids();
        int index = this.page * LIST_SLOTS + slot - FIRST_SLOT;
        if (index < 0 || index >= ids.size()) {
            return 0;
        }
        return ids.get(index).intValue();
    }
}
