package xyz.arcadiadevs.valoriatycoon.guis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.guilib.Gui;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;
import xyz.arcadiadevs.valoriateconomy.Economy;
import xyz.arcadiadevs.valoriateconomy.EconomyResponse;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.commands.AuctionHouse;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;

/**
 * Le comptoir d'achat ({@code /shop}) : la moitié « ça sort de la poche » de l'économie.
 *
 * <p>Jusqu'ici le serveur ne faisait qu'<em>imprimer</em> de l'argent : un générateur produit un objet,
 * {@code /sell} le paie au {@code sellPrice} de sa ligne, le marché entre joueurs déplace l'argent de l'un à
 * l'autre sans en créer. La seule sortie était l'achat de générateurs. Ce comptoir en ouvre une seconde, et
 * surtout il <strong>fixe le prix de la matière</strong> : {@code buy-multiplier} strictement au-dessus de 1
 * est ce qui rend le cycle « acheter puis revendre » perdant, donc ce qui empêche la boutique d'être un
 * distributeur automatique vu de l'autre bout.
 *
 * <p>Le catalogue n'invente aucun prix : il relit la table {@code generators:} déjà chargée par le plugin
 * ({@link ValoriaTycoon#getGeneratorsData()}) et lui applique le facteur de marge. Une matière qui ne sort
 * d'aucun générateur n'a pas de référence : elle s'écrit à la main dans {@code shop.extras} et, par défaut,
 * ne se rachète pas — sinon le comptoir rachèterait ce qu'il vient de vendre, au pire à profit.
 *
 * <p>Le classement en rayons — construction, nourriture, minerais, mob drops, agriculture, redstone, divers —
 * est lui aussi de la configuration : {@code shop.categories} déclare une clef, un titre, une icône, ce que le
 * rayon annonce contenir ({@code description}, lu pour l'info-bulle de l'onglet) et les matières qu'il
 * accueille ; {@code shop.extras[].category} y rattache une offre écrite à la main. Rien n'est figé ici, parce
 * qu'un classement dans le code est un classement qu'aucun admin ne peut corriger sans recompiler. Une matière
 * de générateur que personne n'accueille tombe dans le rayon {@code divers} et le log le dit — jamais dans une
 * case inexistante ; et si le fichier <em>declare</em> un rayon {@code divers}, c'est lui qui sert de panier,
 * titre, icône et description compris, pas un second onglet du même nom. Un rayon plus fourni que la page se
 * <em>page</em> (flèches sous la ligne d'onglets), et {@code /shop <matiere>} ouvre d'emblée le rayon et la
 * page où elle se vend.
 *
 * <p>Aucun fichier d'état : une transaction est atomique du point de vue du joueur (monnaie <em>puis</em>
 * place libre, débit, remise, remboursement de la part non livrée), le catalogue se recharge, rien ne se
 * persiste. C'est voulu : un dossier de plus à corrompre pour un résultat déjà garanti par l'économie.
 *
 * <p>Les items remis sont <strong>neutres</strong> — un {@code Material} et une quantité, sans nom posé ni
 * donnée persistée. Les stacks crachés par un générateur portent une clef que le marché refuse : acheter
 * l'objet décoré, ce serait acheter un item invendable.
 */
public final class ShopGui {

    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;
    /**
     * Neuf onglets : la première ligne entière leur est laissée, les réglages étant descendus en dernière
     * ligne. Un `Inventory` de coffre ne dépasse pas six rangées — ajouter une rangée d'onglets aurait donc
     * coûté une rangée d'offres, pas gagné de place.
     */
    private static final int MAX_TABS = 9;
    private static final int SLOT_TAB_FIRST = 0;
    private static final int OFFER_ROWS = ROWS - 2;
    /** Les 36 cases entre les deux rangées fixes : plafond d'offres affichables par catégorie. */
    private static final int OFFERS_PER_PAGE = OFFER_ROWS * 9;
    private static final int FIRST_OFFER_SLOT = 9;
    private static final int SETTINGS_ROW = ROWS - 1;
    private static final int SLOT_INFO = SETTINGS_ROW * 9;
    private static final int SLOT_MODE = SETTINGS_ROW * 9 + 1;
    private static final int SLOT_AMOUNT = SETTINGS_ROW * 9 + 2;
    private static final int SLOT_PAGE_LABEL = SETTINGS_ROW * 9 + 3;
    private static final int SLOT_PAGE_BACK = SETTINGS_ROW * 9 + 4;
    private static final int SLOT_PAGE_NEXT = SETTINGS_ROW * 9 + 5;
    private static final int SLOT_RELOAD = SETTINGS_ROW * 9 + 7;
    private static final int SLOT_CLOSE = SETTINGS_ROW * 9 + 8;
    private static final String PERMISSION_USE = "valoriatycoon.shop.use";
    private static final String PERMISSION_ADMIN = "valoriatycoon.shop.admin";

    private static final Map<UUID, ShopGui> VIEWS =
            Collections.synchronizedMap(new HashMap<UUID, ShopGui>());
    private static final List<Shelf> SHELVES = new ArrayList<Shelf>();

    // ------------------------------------------------------------------ etat du comptoir

    private static boolean loaded;
    private static boolean enabled = true;
    private static boolean buybackAllowed = true;
    private static double buyMultiplier = 1.5D;
    private static double buybackRatio = 0.5D;
    private static int maxPerTransaction = 2304;
    private static int[] amounts = new int[]{1, 16, 64};

    // ------------------------------------------------------------------ vue par joueur

    private final Player player;
    private final Gui gui;
    private int shelf;
    private int page;
    private int amountIndex;
    /**
     * L'ecran courant : la grille des rayons ({@code /shop} seul), les offres d'un rayon (un clic sur un
     * rayon), ou le panneau de quantite (un clic gauche sur un bloc). Deux actions au clic remplacent
     * l'ancienne bascule acheter/reprendre : gauche = acheter (panneau de quantite), droite = vendre un
     * stack, shift + droite = vendre tout le bloc.
     */
    private Screen screen = Screen.CATEGORIES;
    /** Le bloc retenu par le panneau de quantite (clic gauche sur une offre). */
    private Offer quantityOffer;

    /** Les trois ecrans du comptoir. */
    private enum Screen { CATEGORIES, BLOCKS, QUANTITY }

    private ShopGui(Player player, ValoriaTycoon plugin) {
        this.player = player;
        String title = plugin == null ? "&a&lComptoir"
                : readString(plugin.getConfig(), "shop.title", "&a&lComptoir de Valoria");
        this.gui = new Gui(AuctionHouse.color(title), ROWS, (Plugin) plugin);
        // Volontairement UNE seule page, redessinee a chaque changement d'ecran et a chaque clic. Deux
        // raisons, toutes deux mesurees dans GuiLib : `Gui#setItem` leve une IllegalArgumentException des
        // qu'un item d'un type autre que ITEM est pose ailleurs que sur la page 0, et `addPage()` recopie
        // les non-ITEM de la page 0 tels qu'ils existent a la seconde ou on l'appelle — donc rien du tout
        // si on l'appelle avant le premier rendu. Un ecran n'a pas besoin d'etre une page.
    }

    // ------------------------------------------------------------------ entree

    /** Ouvre le comptoir, ou le redessine s'il est déjà ouvert chez ce joueur. */
    public static void open(Player player) {
        open(player, null);
    }

    /**
     * Ouvre le comptoir <em>sur le rayon qui vend cette matière</em> — et sur la page où elle tombe.
     *
     * Un joueur qui tape `/shop TRIDENT` cherche une chose, pas un menu : atterrir sur le premier onglet et
     * lui laisser faire la recherche à la souris serait un non-sens dès qu'il y a neuf onglets et des pages.
     * Une matière qu'aucun rayon ne vend n'est pas une erreur pour autant : la vue s'ouvre normalement, le
     * message de la commande a déjà dit ce qu'il avait à dire.
     */
    public static void open(Player player, Material material) {
        if (player == null) {
            return;
        }
        ValoriaTycoon plugin = ValoriaTycoon.getInstance();
        ensureLoaded(plugin);
        if (!enabled) {
            player.sendMessage(AuctionHouse.color("&cLe comptoir est fermé (&fshop.enabled&c)."));
            return;
        }
        if (!player.hasPermission(PERMISSION_USE)) {
            player.sendMessage(AuctionHouse.color("&cPas accès au comptoir (&f" + PERMISSION_USE + "&c)."));
            return;
        }
        ShopGui view = VIEWS.get(player.getUniqueId());
        if (view == null) {
            // Un `Gui` de GuiLib enregistre son propre auditeur a la construction : une vue par joueur, pas
            // une vue par clic, sinon chaque changement d'onglet laisserait un auditeur orphelin dans le
            // gestionnaire d'evenements jusqu'au prochain redemarrage du serveur.
            view = new ShopGui(player, plugin);
            VIEWS.put(player.getUniqueId(), view);
        }
        ensureHandler();
        // Trois ecrans : `/shop` ouvre la grille des rayons, `/shop <matiere>` saute directement au rayon
        // qui la vend (et a la page ou elle tombe). Le retour se fait par le bouton « rayons ».
        view.screen = material == null ? Screen.CATEGORIES : Screen.BLOCKS;
        view.quantityOffer = null;
        focus(view, material);
        view.render();
        player.openInventory(view.gui.getInventory());
    }

    /** Pose la vue sur le rayon et la page où cette matière se vend. Ne touche à rien si elle n'y est pas. */
    private static void focus(ShopGui view, Material material) {
        if (view == null || material == null) {
            return;
        }
        for (int index = 0; index < SHELVES.size(); ++index) {
            List<Offer> offers = SHELVES.get(index).offers;
            for (int position = 0; position < offers.size(); ++position) {
                if (offers.get(position).material == material) {
                    view.shelf = index;
                    view.page = position / OFFERS_PER_PAGE;
                    return;
                }
            }
        }
    }

    /** Ce que le joueur doit oublier quand il quitte : sa vue, pas le catalogue. */
    public static void forget(UUID uuid) {
        if (uuid != null) {
            VIEWS.remove(uuid);
        }
    }

    /** Vrai si le comptoir répond — lu par le hook de commande avant d'intercepter quoi que ce soit. */
    public static boolean isEnabled() {
        ensureLoaded(ValoriaTycoon.getInstance());
        return enabled;
    }

    /** Le nombre d'offres vendables, pour le log de démarrage comme pour les contrôles. */
    public static int offerCount() {
        int total = 0;
        for (Shelf shelf : SHELVES) {
            total += shelf.offers.size();
        }
        return total;
    }

    /**
     * Les noms de matières vendues, tous rayons confondus — la complétion Tab de {@code /shop}. Rend le nom
     * {@code Material} (a plat), celui que {@link Material#matchMaterial} relit exactement.
     */
    public static List<String> materialNames() {
        ensureLoaded(ValoriaTycoon.getInstance());
        List<String> names = new ArrayList<String>();
        for (Shelf shelf : SHELVES) {
            for (Offer offer : shelf.offers) {
                names.add(offer.material.name());
            }
        }
        Collections.sort(names);
        return names;
    }

    /** Le texte d'aide, renvoyé tel quel au joueur (une ligne, pas un pavé). */
    public static String usage() {
        return AuctionHouse.color("&7/shop&8 · &foutir le comptoir&8 · &f/shop <matiere>&8 : &faller au rayon"
                + " qui la vend&8 · &f/shop acheter <matiere> [quantite]&8 · &f/shop vendre <matiere>"
                + " [quantite]&8 · &f/shop reload");
    }

    /**
     * Recharge le catalogue : les prix repartent de la table {@code generators:} et de {@code shop.extras}.
     * La phrase rend compte de la marge — et avertit si le réglage fait du comptoir une machine à imprimer.
     */
    public static String reload(ValoriaTycoon plugin) {
        if (plugin != null) {
            // L'admin n'a pas de terminal : il colle le bloc `shop:` dans le fichier avec le gestionnaire de
            // fichiers du panneau, et `getConfig()` lui rendrait la copie gardee en memoire depuis le
            // demarrage. `reloadConfig()` relit le disque — le plugin ne fait jamais l'inverse (aucun
            // `saveConfig()` de notre cote), donc rien n'est ecrit par ce chemin-la.
            plugin.reloadConfig();
        }
        loaded = false;
        ensureLoaded(plugin);
        if (!enabled) {
            return AuctionHouse.color("&eComptoir &cfermé&7 : catalogue vide ou &fshop.enabled: false&7.");
        }
        StringBuilder builder = new StringBuilder(AuctionHouse.color("&aComptoir rechargé : &f"
                + SHELVES.size() + "&a onglet(s), &f" + offerCount() + "&a offre(s), achat à &f+"
                + percent(buyMultiplier - 1.0D) + "&7 au-dessus du prix de vente des générateurs"));
        if (buybackAllowed && buybackRatio > 0.0D) {
            builder.append(AuctionHouse.color("&7, reprise à &f" + Math.round(buybackRatio * 100.0D) + " %&7"));
        }
        else {
            builder.append(AuctionHouse.color("&7, sans reprise"));
        }
        if (buyMultiplier * (buybackAllowed ? buybackRatio : 0.0D) >= 1.0D) {
            builder.append(AuctionHouse.color("&c  /!\\  acheter puis revendre est GAGNANT : "
                    + "&fbuy-multiplier x buyback-ratio >= 1&c, la boutique imprime de l'argent."));
        }
        for (ShopGui view : VIEWS.values()) {
            if (view.player.isOnline()) {
                // Vue retiree a la deconnexion, mais le reload peut partir d'une commande pendant qu'un
                // joueur se deconnecte : redraw d'un fantome = NullPointerException dans le log du serveur.
                view.render();
            }
        }
        return builder.toString();
    }

    /**
     * {@code /shop [acheter|vendre <matiere> [quantité]]}. Rend {@code null} quand la commande n'a rien à
     * envoyer (le cas de l'ouverture simple), pour ne pas coller un « ok » parasite dans le fil du joueur.
     */
    public static String command(Player player, String[] args) {
        ValoriaTycoon plugin = ValoriaTycoon.getInstance();
        ensureLoaded(plugin);
        if (!enabled) {
            return AuctionHouse.color("&cLe comptoir est fermé (&fshop.enabled&c).");
        }
        // `args` est le message brut decopte par le hook : args[0] est le libelle `/shop` lui-meme, la
        // premiere valeur utile est donc args[1] (meme convention que `/ah sell <prix>`).
        if (args.length < 2) {
            open(player);
            return null;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("reload") || action.equals("recharger")) {
            if (!player.hasPermission(PERMISSION_ADMIN)) {
                return AuctionHouse.color("&cRéservé (&f" + PERMISSION_ADMIN + "&c).");
            }
            return reload(plugin);
        }
        boolean selling = action.equals("vendre") || action.equals("rendre") || action.equals("sell");
        if (!selling && !action.equals("acheter") && !action.equals("buy")) {
            // `/shop <matiere>` : un raccourci de navigation, pas une transaction. Le nom est lu comme une
            // matiere ; s'il n'en est pas un, le comptoir s'ouvre la ou le joueur l'avait laisse.
            open(player, Material.matchMaterial(action.toUpperCase(Locale.ROOT)));
            return null;
        }
        if (args.length < 3) {
            return usage();
        }
        Material material = Material.matchMaterial(args[2].toUpperCase(Locale.ROOT));
        Offer offer = material == null ? null : offerOf(material);
        if (offer == null) {
            return AuctionHouse.color("&cLe comptoir ne connait pas &f" + args[2] + "&c. La liste : &f/shop&c.");
        }
        int quantity = amounts[Math.min(amountIndex(player), amounts.length - 1)];
        if (args.length > 3) {
            quantity = parseQuantity(args[3]);
            if (quantity <= 0) {
                return AuctionHouse.color("&cQuantité invalide : &f" + args[3]);
            }
        }
        return selling ? sellBack(player, offer, quantity) : buy(player, offer, quantity);
    }

    private static int amountIndex(Player player) {
        ShopGui view = VIEWS.get(player.getUniqueId());
        return view == null ? 0 : view.amountIndex;
    }

    // ------------------------------------------------------------------ caisse

    /** Achat : monnaie <em>puis</em> place libre, avant tout débit. */
    private static String buy(Player player, Offer offer, int quantity) {
        if (offer == null || quantity <= 0 || offer.buy <= 0.0D) {
            return usage();
        }
        if (quantity > maxPerTransaction) {
            quantity = maxPerTransaction;
        }
        int room = roomFor(player, offer.material);
        if (room < quantity) {
            return AuctionHouse.color("&ePlace libre : &f" + room + "&e pour cette matière (lot maximum &f"
                    + maxPerTransaction + "&e). Rien n'a été débité.");
        }
        Economy economy = economy();
        if (economy == null) {
            return AuctionHouse.color("&cAucun fournisseur d'économie : le comptoir ne peut encaisser.");
        }
        double total = offer.buy * quantity;
        OfflinePlayer buyer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (!economy.has(buyer, total)) {
            return AuctionHouse.color("&cIl te faut &f" + AuctionHouse.money(total) + "&c pour &f" + quantity
                    + "× " + offer.name + "&c (solde &f" + AuctionHouse.money(economy.getBalance(buyer)) + "&c).");
        }
        EconomyResponse withdraw = economy.withdrawPlayer(buyer, total);
        if (!withdraw.transactionSuccess()) {
            return AuctionHouse.color("&cPaiement refusé : &f" + text(withdraw));
        }
        int refused = deliver(player, offer.material, quantity);
        if (refused > 0) {
            // Inatteignable après le contrôle de place, donc traité en sûreté : la part non livrée est rendue,
            // jamais lâchée au sol (un item au sol peut brûler, couler ou être ramassé par un tiers).
            economy.depositPlayer(buyer, offer.buy * refused);
        }
        int delivered = quantity - refused;
        return AuctionHouse.color("&aAcheté &f" + delivered + "× " + offer.name + "&a pour &f"
                + AuctionHouse.money(offer.buy * delivered) + "&a (solde &f"
                + AuctionHouse.money(economy.getBalance(buyer)) + "&a).");
    }

    /** Reprise : ce que le comptoir rend, volontairement moins que {@code /sell}. */
    private static String sellBack(Player player, Offer offer, int quantity) {
        if (!buybackAllowed || offer == null || offer.buyback <= 0.0D) {
            return AuctionHouse.color("&cCette matière n'est pas reprise. Tes drops se vendent avec &f/sell&c.");
        }
        int available = count(player, offer.material);
        if (available <= 0) {
            return AuctionHouse.color("&cTu n'as rien de ce type à rendre.");
        }
        Economy economy = economy();
        if (economy == null) {
            return AuctionHouse.color("&cAucun fournisseur d'économie : rien n'est repris.");
        }
        int wanted = Math.min(Math.max(1, quantity), Math.min(available, maxPerTransaction));
        int removed = take(player, offer.material, wanted);
        if (removed <= 0) {
            return AuctionHouse.color("&cRien n'a pu être retiré de ton inventaire.");
        }
        double total = offer.buyback * removed;
        OfflinePlayer seller = Bukkit.getOfflinePlayer(player.getUniqueId());
        EconomyResponse deposit = economy.depositPlayer(seller, total);
        if (!deposit.transactionSuccess()) {
            // L'argent n'est pas arrivé : la marchandise retourne dans le sac, pas dans une promesse.
            deliver(player, offer.material, removed);
            return AuctionHouse.color("&cDépôt refusé : &f" + text(deposit) + "&7 (objet rendu).");
        }
        return AuctionHouse.color("&aRevendu &f" + removed + "× " + offer.name + "&a pour &f"
                + AuctionHouse.money(total) + "&a (solde &f" + AuctionHouse.money(economy.getBalance(seller))
                + "&a).");
    }

    // ------------------------------------------------------------------ rendu

    /** Redessine la vue courante : grille des rayons, offres d'un rayon, ou panneau de quantite. */
    private void render() {
        if (SHELVES.isEmpty()) {
            return;
        }
        switch (this.screen) {
            case BLOCKS:
                renderBlocks();
                return;
            case QUANTITY:
                renderQuantity();
                return;
            default:
                renderCategories();
        }
    }

    /**
     * Ecran 1 : la grille des rayons, rien d'autre. Chaque case est un rayon cliquable (icone + titre + la
     * description que le fichier lui donne) ; un clic ouvre ses offres. La ligne de reglages reste visible
     * (solde, legendes, rechargement, fermeture).
     */
    private void renderCategories() {
        Inventory view = this.gui.getInventory();
        ItemStack pane = branded(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "&r", null);
        for (int slot = 0; slot < SLOTS; ++slot) {
            place(view, slot, pane, null);
        }
        place(view, SLOT_TAB_FIRST, branded(new ItemStack(Material.GOLD_BLOCK),
                "&2&lComptoir de Valoria",
                Arrays.asList("&7Choisis un rayon : &f" + SHELVES.size() + "&7 rayons, " + offerCount()
                        + "&7 matieres.", "&8Clic gauche : ouvrir le rayon")), null);
        int shown = Math.min(SHELVES.size(), MAX_TABS);
        int start = categoryStart(shown);
        for (int index = 0; index < shown; ++index) {
            Shelf shelf = SHELVES.get(index);
            ItemStack tile = branded(new ItemStack(shelf.icon),
                    "&a▶ &f" + shelf.title, hint(shelf, false));
            place(view, start + index, tile, null);
        }
        place(view, SLOT_INFO, soldeButton(), null);
        place(view, SLOT_MODE, legendButton(), null);
        place(view, SLOT_RELOAD, reloadButton(), null);
        place(view, SLOT_CLOSE, closeButton(), null);
    }

    /**
     * Ecran 2 : les offres du rayon courant. La case (0,0) est un bouton « retour » vers la grille des
     * rayons ; la ligne de reglages (solde, legendes, pages, rechargement, fermeture) reste en bas, et les
     * offres remplissent les cases restantes, avec pagination. Chaque offre porte son prix d'achat (vert) et
     * de reprise (rouge), et repond au clic gauche (acheter), droit (vendre un stack), shift+droit (tout).
     */
    private void renderBlocks() {
        this.shelf = Math.max(0, Math.min(this.shelf, SHELVES.size() - 1));
        List<Offer> all = SHELVES.get(this.shelf).offers;
        int pages = 1 + Math.max(0, all.size() - 1) / OFFERS_PER_PAGE;
        this.page = Math.max(0, Math.min(this.page, pages - 1));
        Inventory view = this.gui.getInventory();
        ItemStack pane = branded(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "&r", null);
        for (int slot = 0; slot < SLOTS; ++slot) {
            place(view, slot, pane, null);
        }
        place(view, SLOT_TAB_FIRST, branded(new ItemStack(Material.ARROW), "&e&l‹ Rayons",
                Collections.singletonList("&7Clic : revenir au choix du rayon")), null);
        place(view, SLOT_TAB_FIRST + 1, branded(new ItemStack(SHELVES.get(this.shelf).icon),
                "&f&l" + SHELVES.get(this.shelf).title,
                Arrays.asList("&7" + all.size() + " matiere(s), rayon « " + SHELVES.get(this.shelf).title
                        + " »", "&8Clic gauche : acheter · &cClic droit : vendre")), null);

        place(view, SLOT_INFO, soldeButton(), null);
        place(view, SLOT_MODE, legendButton(), null);
        place(view, SLOT_PAGE_LABEL, pageLabelButton(all.size(), pages), null);
        if (pages > 1) {
            place(view, SLOT_PAGE_BACK, pageNavButton(true), null);
            place(view, SLOT_PAGE_NEXT, pageNavButton(false), null);
        }
        place(view, SLOT_RELOAD, reloadButton(), null);
        place(view, SLOT_CLOSE, closeButton(), null);

        int first = this.page * OFFERS_PER_PAGE;
        int shown = Math.min(all.size(), first + OFFERS_PER_PAGE);
        for (int index = first; index < shown; ++index) {
            place(view, FIRST_OFFER_SLOT + (index - first), blockItem(all.get(index)), null);
        }
    }

    /**
     * Ecran 3 : le panneau de quantite, ouvert par un clic gauche sur un bloc. Une case par lot de la
     * config (`shop.amounts`), chacune en vert (achat) avec son prix total ; un clic achete ce lot et
     * revient aux offres. Un bouton « retour » ramene au rayon.
     */
    private void renderQuantity() {
        Inventory view = this.gui.getInventory();
        ItemStack pane = branded(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "&r", null);
        for (int slot = 0; slot < SLOTS; ++slot) {
            place(view, slot, pane, null);
        }
        Offer offer = resolveOffer(this.quantityOffer == null ? null : this.quantityOffer.material);
        if (offer == null) {
            this.screen = Screen.CATEGORIES;
            this.render();
            return;
        }
        place(view, SLOT_TAB_FIRST, branded(new ItemStack(Material.ARROW), "&e&l‹ Retour",
                Collections.singletonList("&7Clic : revenir au rayon")), null);
        List<String> head = new ArrayList<String>();
        head.add("&aAchat : &f" + AuctionHouse.money(offer.buy) + "&7 l'unite");
        head.add("&7Dans ton sac : &f" + count(this.player, offer.material));
        ItemStack headItem = branded(new ItemStack(offer.material), "&a" + offer.name, head);
        headItem.setAmount(1);
        place(view, SLOT_TAB_FIRST + 1, headItem, null);

        int room = roomFor(this.player, offer.material);
        int start = FIRST_OFFER_SLOT;
        for (int index = 0; index < amounts.length; ++index) {
            final int qty = amounts[index];
            List<String> lines = new ArrayList<String>();
            lines.add("&aPrix : &f" + AuctionHouse.money(offer.buy * qty));
            lines.add("&7Place libre : &f" + room + "&7 · plafond &f" + maxPerTransaction);
            lines.add("&8Clic gauche : acheter ×" + qty);
            ItemStack button = branded(new ItemStack(offer.material), "&a×&f" + qty, lines);
            button.setAmount(Math.min(64, Math.max(1, qty)));
            place(view, start + index, button, null);
        }
        place(view, SLOT_INFO, soldeButton(), null);
        place(view, SLOT_MODE, legendButton(), null);
        place(view, SLOT_RELOAD, reloadButton(), null);
        place(view, SLOT_CLOSE, closeButton(), null);
    }

    /** La case d'un bloc dans le rayon : nom en vert (achat), prix d'achat et de reprise colores. */
    private ItemStack blockItem(Offer offer) {
        List<String> lines = new ArrayList<String>();
        lines.add("&aAchat : &f" + AuctionHouse.money(offer.buy) + "&7 l'unite");
        if (buybackAllowed && offer.buyback > 0.0D) {
            lines.add("&cReprise : &f" + AuctionHouse.money(offer.buyback) + "&7 l'unite");
        }
        else {
            lines.add("&8Reprise : coupee");
        }
        lines.add("&7Dans ton sac : &f" + count(this.player, offer.material));
        lines.add("&r");
        lines.add(offer.detail);
        lines.add("&r");
        lines.add("&a&lClic gauche &f» acheter (quantite)");
        lines.add("&c&lClic droit &f» vendre un stack (&f" + stackSize(offer.material) + "&c)");
        lines.add("&c&lShift + droit &f» vendre tout ce bloc");
        ItemStack item = branded(new ItemStack(offer.material), "&a" + offer.name, lines);
        item.setAmount(1);
        return item;
    }

    /**
     * Ecrit la case dans les deux mondes : le tableau de {@code Gui} (pour que le clic soit annule par
     * GuiLib) et l'inventaire (pour que le joueur voie le resultat). Toute la logique de clic vit dans
     * {@link Handler} : l'action GuiLib est volontairement {@code null}, sinon un clic droit declencherait
     * aussi l'action de gauche.
     */
    private void place(Inventory view, int slot, ItemStack item, Runnable action) {
        this.gui.setItem(slot, new GuiItem(GuiItemType.ITEM, item, action));
        view.setItem(slot, item);
    }

    /** La premiere case de la grille d'offres pour un nombre donne de rayons alignes au centre. */
    private static int categoryStart(int shown) {
        return FIRST_OFFER_SLOT + (9 - shown) / 2 + (9 - shown) % 2;
    }

    /** La taille d'une pile complète pour cette matiere (stack simple ou de bloc). */
    private static int stackSize(Material material) {
        int max = new ItemStack(material).getMaxStackSize();
        return max <= 0 ? 64 : max;
    }

    /** Renvoie l'offre courante pour cette matiere, ou celle retenue si le catalogue a change. */
    private Offer resolveOffer(Material material) {
        Offer resolved = material == null ? null : offerOf(material);
        return resolved == null ? this.quantityOffer : resolved;
    }

    private ItemStack soldeButton() {
        return branded(new ItemStack(Material.GOLD_NUGGET), "&6&lSolde", soldeLines(this.player));
    }

    private ItemStack legendButton() {
        return branded(new ItemStack(Material.BOOK), "&6&lActions",
                Arrays.asList("&a&lClic gauche &7: acheter (panneau de quantite)",
                        "&c&lClic droit &7: vendre un stack",
                        "&c&lShift + droit &7: vendre tout ce bloc"));
    }

    private ItemStack reloadButton() {
        return branded(new ItemStack(Material.COMPARATOR), "&e&lRecharger les prix",
                Arrays.asList("&7Relit &fgenerators:&7 et &fshop:&7 du &fconfig.yml",
                        "&8Reserve a &f" + PERMISSION_ADMIN));
    }

    private ItemStack closeButton() {
        return branded(new ItemStack(Material.BARRIER), "&c&lFermer",
                Collections.singletonList("&7Clic : fermer le comptoir"));
    }

    private ItemStack pageLabelButton(int total, int pages) {
        return branded(new ItemStack(Material.PAPER), "&f&lPage " + (this.page + 1) + "&7/&f" + pages,
                Collections.singletonList("&7" + total + " matieres dans ce rayon"));
    }

    private ItemStack pageNavButton(boolean back) {
        return branded(new ItemStack(Material.ARROW),
                back ? "&7&l‹ page " + this.page : "&7&lpage " + (this.page + 2) + " ›",
                Collections.singletonList(back ? "&7Revenir d'une page" : "&7Avancer d'une page"));
    }

    // ------------------------------------------------------------------ clics

    /** Dispatche le clic selon l'ecran courant. Execute hors de l'evenement (voir {@link Handler}). */
    private void handleClick(Player player, int slot, boolean right, boolean shift) {
        if (this.screen == Screen.CATEGORIES) {
            onCategoriesClick(player, slot);
        }
        else if (this.screen == Screen.BLOCKS) {
            onBlocksClick(player, slot, right, shift);
        }
        else {
            onQuantityClick(player, slot);
        }
    }

    private void onCategoriesClick(Player player, int slot) {
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == SLOT_RELOAD) {
            reloadThrough(player);
            return;
        }
        if (slot == SLOT_INFO || slot == SLOT_MODE) {
            this.render();
            return;
        }
        int shown = Math.min(SHELVES.size(), MAX_TABS);
        int start = categoryStart(shown);
        int index = slot - start;
        if (index >= 0 && index < shown) {
            this.shelf = index;
            this.page = 0;
            this.quantityOffer = null;
            this.screen = Screen.BLOCKS;
            this.render();
        }
    }

    private void onBlocksClick(Player player, int slot, boolean right, boolean shift) {
        if (slot == SLOT_TAB_FIRST) {
            this.screen = Screen.CATEGORIES;
            this.quantityOffer = null;
            this.render();
            return;
        }
        if (slot == SLOT_PAGE_BACK) {
            this.page = Math.max(0, this.page - 1);
            this.render();
            return;
        }
        if (slot == SLOT_PAGE_NEXT) {
            int pages = 1 + Math.max(0, SHELVES.get(this.shelf).offers.size() - 1) / OFFERS_PER_PAGE;
            this.page = Math.min(pages - 1, this.page + 1);
            this.render();
            return;
        }
        if (slot == SLOT_RELOAD) {
            reloadThrough(player);
            return;
        }
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == SLOT_INFO || slot == SLOT_MODE || slot == SLOT_AMOUNT || slot == SLOT_PAGE_LABEL) {
            this.render();
            return;
        }
        List<Offer> all = SHELVES.get(this.shelf).offers;
        int index = slot - FIRST_OFFER_SLOT + this.page * OFFERS_PER_PAGE;
        if (index >= 0 && index < all.size()) {
            Offer offer = all.get(index);
            if (right) {
                int quantity = shift ? Integer.MAX_VALUE : stackSize(offer.material);
                this.player.sendMessage(sellBack(this.player, offer, quantity));
            }
            else {
                this.quantityOffer = offer;
                this.screen = Screen.QUANTITY;
            }
            this.render();
        }
    }

    private void onQuantityClick(Player player, int slot) {
        if (slot == SLOT_TAB_FIRST) {
            this.screen = Screen.BLOCKS;
            this.render();
            return;
        }
        if (slot == SLOT_RELOAD) {
            reloadThrough(player);
            return;
        }
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == SLOT_INFO || slot == SLOT_MODE) {
            this.render();
            return;
        }
        int index = slot - FIRST_OFFER_SLOT;
        if (index >= 0 && index < amounts.length && this.quantityOffer != null) {
            Offer offer = resolveOffer(this.quantityOffer.material);
            if (offer == null) {
                this.player.sendMessage(AuctionHouse.color("&cCette matiere n'est plus vendue au comptoir."));
                this.screen = Screen.BLOCKS;
                this.render();
                return;
            }
            int quantity = amounts[index];
            this.player.sendMessage(buy(this.player, offer, quantity));
            this.screen = Screen.BLOCKS;
            this.render();
        }
    }

    private void reloadThrough(Player player) {
        if (!player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage(AuctionHouse.color("&cReserve (&f" + PERMISSION_ADMIN + "&c)."));
            return;
        }
        player.sendMessage(reload(ValoriaTycoon.getInstance()));
    }

    // ------------------------------------------------------------------ auditeur partage

    private static boolean handlerRegistered = false;

    private static void ensureHandler() {
        if (handlerRegistered) {
            return;
        }
        handlerRegistered = true;
        Bukkit.getPluginManager().registerEvents(new Handler(), ValoriaTycoon.getInstance());
    }

    /**
     * Gestionnaire unique pour toutes les vues. La logique est deferree d'un tick (via
     * {@code Bukkit.getScheduler()}) pour ne pas modifier l'inventaire pendant l'evenement de clic.
     */
    private static final class Handler implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            ShopGui view = viewOf(event.getView().getTopInventory());
            if (view == null) {
                return;
            }
            // Annule TOUT clic tant que le comptoir est ouvert, y compris ceux de l'inventaire du joueur :
            // sinon un shift-clic de la bas versait un item dans la vue du comptoir (inventaire a holder
            // null), et le comptoir ne doit jamais se remplir de ce que le joueur trie.
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }
            final Player player = (Player) event.getWhoClicked();
            final int slot = event.getRawSlot();
            final boolean right = event.isRightClick();
            final boolean shift = event.isShiftClick();
            Bukkit.getScheduler().runTask(ValoriaTycoon.getInstance(),
                    () -> view.handleClick(player, slot, right, shift));
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (viewOf(event.getView().getTopInventory()) != null) {
                event.setCancelled(true);
            }
        }

        private ShopGui viewOf(Inventory top) {
            for (ShopGui candidate : VIEWS.values()) {
                if (candidate.gui.getInventory() == top) {
                    return candidate;
                }
            }
            return null;
        }
    }

    private static List<String> soldeLines(Player player) {
        Economy economy = economy();
        if (economy == null) {
            return Arrays.asList("&7Solde : &céconomie indisponible",
                    "&7Marge du comptoir : &f+" + percent(buyMultiplier - 1.0D));
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player.getUniqueId());
        return Arrays.asList("&7Solde : &a" + AuctionHouse.money(economy.getBalance(offline)),
                "&7Marge : &f+" + percent(buyMultiplier - 1.0D) + "&7 sur le prix générateur",
                "&8Le prix de référence reste le &fsellPrice&8 du générateur");
    }

    // ------------------------------------------------------------------ catalogue

    /** Une offre : la matière, comment l'appeler, ses deux prix, et la ligne qui dit d'où elle vient. */
    private static final class Offer {
        private final Material material;
        private final String name;
        private final double buy;
        private final double buyback;
        private final String detail;

        private Offer(Material material, String name, double buy, double buyback, String detail) {
            this.material = material;
            this.name = name;
            this.buy = buy;
            this.buyback = buyback;
            this.detail = detail;
        }
    }

    /**
     * Un onglet : sa clef de config (celle que `shop.extras[].category` pointe), son titre, son icône, et
     * ses offres dans l'ordre d'affichage.
     */
    private static final class Shelf {
        private final String key;
        private final String title;
        private final Material icon;
        /**
         * Ce que le rayon annonce qu'il contient ({@code shop.categories[].description}), affiche sous le titre
         * de l'onglet. Vide par defaut : un onglet sans description se presente par son icone et son titre,
         * comme avant cette clef — ce n'est pas une config fautive, c'est une config muette.
         */
        private final List<String> description;
        private final List<Offer> offers = new ArrayList<Offer>();

        private Shelf(String key, String title, Material icon) {
            this(key, title, icon, Collections.<String>emptyList());
        }

        private Shelf(String key, String title, Material icon, List<String> description) {
            this.key = key;
            this.title = title;
            this.icon = icon;
            this.description = description;
        }
    }

    /** Reconstruit le catalogue depuis la config. Une fois par chargement, puis à chaque {@code /shop reload}. */
    private static void ensureLoaded(ValoriaTycoon plugin) {
        if (loaded || plugin == null) {
            return;
        }
        loaded = true;
        SHELVES.clear();
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("shop.enabled", true);
        buyMultiplier = Math.max(1.0D, config.getDouble("shop.buy-multiplier", 1.5D));
        buybackRatio = Math.min(1.0D, Math.max(0.0D, config.getDouble("shop.buyback-ratio", 0.5D)));
        buybackAllowed = config.getBoolean("shop.buyback-enabled", true);
        maxPerTransaction = Math.max(1, config.getInt("shop.max-per-transaction", 2304));
        amounts = readAmounts(config);
        Shelf generated = new Shelf("generateurs", strip(config.getString("shop.generated-category",
                "&aMatières de générateur")), Material.IRON_INGOT);
        List<Shelf> shelves = new ArrayList<Shelf>();
        Map<String, Shelf> byMaterial = new HashMap<String, Shelf>();
        Map<String, Shelf> byKey = new HashMap<String, Shelf>();
        readCategories(plugin, config, shelves, byMaterial, byKey);
        // `divers` est un rayon comme un autre ET le panier de secours. Si le fichier en declare un, c'est LUI
        // qui ramasse ce qui n'est classe nulle part — titre, icone et description compris — et non un second
        // onglet du meme nom monte en douce a cote du premier. Sans rangee declaree, `extras-category` garde
        // la main : c'est le cas d'un serveur dont le config.yml precede l'option, et il a le droit de rester muet.
        Shelf declared = byKey.get("divers");
        Shelf other = declared == null
                ? new Shelf("divers", strip(config.getString("shop.extras-category", "&eDivers")), Material.CHEST)
                : declared;
        if (shelves.isEmpty()) {
            // Pas de `shop.categories` (le cas d'un serveur dont le config.yml précède cette option) : on
            // reste sur l'ancien comportement, une page de matières et une page de divers.
            shelves.add(generated);
        }
        boolean classified = !byMaterial.isEmpty();
        int unclassified = 0;
        GeneratorsData data = null;
        try {
            data = plugin.getGeneratorsData();
        }
        catch (RuntimeException | LinkageError runtimeException) {
            plugin.getLogger().warning("[shop] table des generateurs illisible : " + runtimeException
                    + " — le comptoir n'affichera que `shop.extras`.");
        }
        if (data != null && data.generators() != null) {
            for (GeneratorsData.Generator generator : data.generators()) {
                if (generator == null) {
                    continue;
                }
                Material material = materialOf(generator);
                if (material == null) {
                    continue;
                }
                double unit = generator.sellPrice();
                if (unit <= 0.0D) {
                    continue;
                }
                double buy = round(unit * buyMultiplier);
                String detail = "&7Vient du palier &f" + generator.tier() + "&7, rendu &f" + unit
                        + "&7 toutes les &f" + generator.speed() + " s";
                String name = strip(generator.name());
                Shelf target = byMaterial.get(material.name());
                if (target == null) {
                    target = classified ? other : generated;
                    unclassified++;
                }
                target.offers.add(new Offer(material, name.isEmpty() ? pretty(material) : name, buy,
                        round(buy * buybackRatio), detail));
            }
        }
        readExtras(plugin, config, other, byKey, byMaterial);
        // `!shelves.contains(other)` : un `divers` declare est deja dans la liste, le rajouter ferait deux
        // onglets jumeaux — et le second recevrait les matieres que le premier affiche aussi.
        if (!other.offers.isEmpty() && !shelves.contains(other)) {
            shelves.add(other);
        }
        SHELVES.addAll(shelves);
        if (unclassified > 0 && classified) {
            plugin.getLogger().warning("[shop] " + unclassified + " matiere(s) de generateur sans rangee : "
                    + "les lister dans `shop.categories[].materials` les fait sortir de l'onglet `divers`.");
        }
        else if (!classified) {
            // Un serveur dont le config.yml precede cette option n'a rien d'anormal : c'est une invitation,
            // pas une faute, et le dire en `info` evite qu'un log de chargement se lise comme une alerte.
            plugin.getLogger().info("[shop] `shop.categories` absent : les matieres de generateur restent "
                    + "rangées ensemble. Ecrire des rayons dans le config.yml les classe — c'est de la "
                    + "configuration, pas une recompilation.");
        }
        for (Shelf shelf : SHELVES) {
            if (shelf.offers.size() > OFFERS_PER_PAGE) {
                // Pas une erreur : le rayon se page. Le log sert a l'admin qui vient d'ecrire cinquante
                // matieres dans un seul rayon et se demande pourquoi les fleches sont apparues.
                plugin.getLogger().info("[shop] rayon « " + shelf.title + " » : " + shelf.offers.size()
                        + " offres sur " + OFFERS_PER_PAGE + " cases, le comptoir le page en "
                        + (1 + (shelf.offers.size() - 1) / OFFERS_PER_PAGE) + " pages.");
            }
        }
        if (offerCount() == 0) {
            enabled = false;
            plugin.getLogger().warning("[shop] catalogue vide : aucun générateur exploitable et aucun "
                    + "`shop.extras` — comptoir désactivé plutôt qu'une interface vide.");
        }
    }

    /** Le {@code Material} d'une ligne de générateur, ou {@code null} si elle n'est pas vendable en l'état. */
    private static Material materialOf(GeneratorsData.Generator generator) {
        ItemStack sample;
        try {
            sample = generator.spawnItem();
        }
        catch (RuntimeException | LinkageError runtimeException) {
            return null;
        }
        Material material = sample == null ? null : sample.getType();
        return material == null || !material.isItem() || material.isAir() ? null : material;
    }

    /**
     * Les rangées du comptoir, telles que la config les déclare : chaque entrée porte une {@code key} (que
     * pointent {@code shop.extras[].category}), un {@code name}, une {@code icon} et la liste des
     * {@code materials} qui s'y rangent.
     *
     * La classification vit entièrement dans le {@code config.yml} parce que c'est une décision de goût, pas
     * de calcul : le jour où une table de mots croûte dans le code, plus aucun admin ne peut la corriger sans
     * recompiler. Un {@code Material} cité dans deux rangées serait rangé dans la première et signalé — le
     * double emploi rend une offre inatteignable, ce qui est exactement le genre de panne silencieuse que ce
     * fichier refuse.
     */
    private static void readCategories(ValoriaTycoon plugin, FileConfiguration config, List<Shelf> shelves,
            Map<String, Shelf> byMaterial, Map<String, Shelf> byKey) {
        List<Map<String, Object>> rows;
        try {
            rows = (List<Map<String, Object>>) (List<?>) config.getMapList("shop.categories");
        }
        catch (RuntimeException runtimeException) {
            plugin.getLogger().warning("[shop] `shop.categories` illisible : " + runtimeException
                    + " — toutes les matières retombent dans l'onglet unique.");
            return;
        }
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            String key = String.valueOf(row.get("key")).trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty() || "null".equals(key)) {
                plugin.getLogger().warning("[shop] rangée ignorée (`key` absente) : " + row);
                continue;
            }
            if (byKey.containsKey(key)) {
                plugin.getLogger().warning("[shop] rangée ignorée (clef déjà prise) : " + key);
                continue;
            }
            Object name = row.get("name");
            Material icon = iconOf(row.get("icon"));
            Shelf shelf = new Shelf(key, name == null || String.valueOf(name).isEmpty()
                    ? key : strip(String.valueOf(name)), icon, descriptionOf(row.get("description")));
            byKey.put(key, shelf);
            shelves.add(shelf);
            Object materials = row.get("materials");
            if (!(materials instanceof List)) {
                plugin.getLogger().warning("[shop] rangée `" + key + "` sans liste `materials` : elle "
                        + "n'accueillera rien, seulement les matières qui la pointent par `category`.");
            }
            else {
                for (Object value : (List<?>) materials) {
                    String raw = value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
                    if (raw.isEmpty()) {
                        continue;
                    }
                    if (Material.matchMaterial(raw) == null) {
                        plugin.getLogger().warning("[shop] `" + key + ".materials` : matière inconnue du "
                                + "serveur, entrée sans effet : " + raw);
                        continue;
                    }
                    Shelf previous = byMaterial.put(raw, shelf);
                    if (previous != null && previous != shelf) {
                        plugin.getLogger().warning("[shop] `" + raw + "` rangé deux fois (`" + previous.key
                                + "` puis `" + key + "`) : la première rangée gagne, l'autre entrée est à "
                                + "retirer.");
                        byMaterial.put(raw, previous);
                    }
                }
            }
        }
        if (shelves.size() > MAX_TABS) {
            plugin.getLogger().warning("[shop] " + shelves.size() + " rangées, " + MAX_TABS
                    + " onglets affichables : les dernières sont atteignables par `/shop <matiere>` mais pas "
                    + "au clic. Fusionne-les ou coupe-les.");
        }
    }

    /** L'icône d'une rangée : un `Material` nommé, celui de la première offre sinon — jamais une tête vide. */
    private static Material iconOf(Object raw) {
        Material material = raw == null ? null : Material.matchMaterial(String.valueOf(raw)
                .trim().toUpperCase(Locale.ROOT));
        return material == null || !material.isItem() || material.isAir() ? Material.PAPER : material;
    }

    /**
     * Une description de rayon, lue dans {@code shop.categories[].description}. Une liste ou une chaîne seule
     * se valent — une rangée qui ne veut qu'un rappel d'une ligne ne doit pas écrire un liste pour ça. Les
     * lignes vides sautent : une case de lore vide se voit, elle, et ferait flotter le reste de l'info-bulle.
     */
    private static List<String> descriptionOf(Object raw) {
        List<String> lines = new ArrayList<String>();
        if (raw instanceof List) {
            for (Object entry : (List<?>) raw) {
                String line = entry == null ? "" : String.valueOf(entry).trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            return lines;
        }
        String only = raw == null ? "" : String.valueOf(raw).trim();
        if (!only.isEmpty() && !"null".equals(only)) {
            lines.add(only);
        }
        return lines;
    }

    /**
     * L'info-bulle d'un onglet : ce que le rayon <em>annonce</em> (la {@code description} du fichier), puis ce
     * qu'il <em>contient</em> (le catalogue monté). Une ligne neutre sépare les deux pour que le compte des
     * offres ne se lise pas comme une puce de plus.
     */
    private static List<String> hint(Shelf shelf, boolean active) {
        List<String> lines = new ArrayList<String>(shelf.description);
        if (!lines.isEmpty()) {
            lines.add("&r");
        }
        lines.add("&7" + shelf.offers.size() + " matière(s), "
                + (1 + Math.max(0, shelf.offers.size() - 1) / OFFERS_PER_PAGE) + " page(s)");
        lines.add(active ? "&aOnglet actif" : "&7Clic : ouvrir cet onglet");
        return lines;
    }

    /**
     * Les lignes écrites à la main — prix libre, pas de reprise par défaut —, rangées elles aussi.
     *
     * Deux façons de nommer un rayon, dans l'ordre : {@code category} — la réponse explicite, celle qui permet
     * d'inventer une matière sans toucher au routage — puis, à défaut, le rayon qui <em>réclame</em> déjà cette
     * matière dans {@code shop.categories[].materials}. La deuxième n'est pas un détail de confort : sans
     * elle, une liste {@code materials} bien tenue et un {@code category} oublié rangeraient l'offre à part,
     * et l'admin chercherait une heure une offre qu'il voit dans son fichier.
     */
    private static void readExtras(ValoriaTycoon plugin, FileConfiguration config, Shelf fallback,
            Map<String, Shelf> byKey, Map<String, Shelf> byMaterial) {
        List<Map<String, Object>> rows;
        try {
            rows = (List<Map<String, Object>>) (List<?>) config.getMapList("shop.extras");
        }
        catch (RuntimeException runtimeException) {
            plugin.getLogger().warning("[shop] `shop.extras` illisible : " + runtimeException);
            rows = Collections.emptyList();
        }
        if (rows == null) {
            return;
        }
        int contradicted = 0;
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            Object raw = row.get("material");
            Material material = raw == null ? null
                    : Material.matchMaterial(String.valueOf(raw).toUpperCase(Locale.ROOT));
            if (material == null || !material.isItem() || material.isAir()) {
                plugin.getLogger().warning("[shop] entrée ignorée (matière inconnue) : " + raw);
                continue;
            }
            double buy = number(row.get("buy"), 0.0D);
            if (buy <= 0.0D) {
                plugin.getLogger().warning("[shop] entrée ignorée (`buy` absent ou nul) : " + material);
                continue;
            }
            Object name = row.get("name");
            double buyback = Math.max(0.0D, number(row.get("sellback"), 0.0D));
            Shelf shelf = fallback;
            // Le rayon qui reclame deja cette matiere : de quoi rangera une offre sans `category` ecrite.
            Shelf claimed = byMaterial.get(material.name());
            Object category = row.get("category");
            if (category != null && !String.valueOf(category).trim().isEmpty()) {
                Shelf target = byKey.get(String.valueOf(category).trim().toLowerCase(Locale.ROOT));
                if (target == null) {
                    plugin.getLogger().warning("[shop] `category: " + category + "` sur " + material
                            + " ne nomme aucune rangée de `shop.categories` : "
                            + (claimed == null ? "l'offre est rangée à part." : "rangée là où le routage la veut."));
                }
                else {
                    shelf = target;
                    if (claimed != null && claimed != target) {
                        // Deux desaccords dans le meme fichier : `category` gagne, mais le log le dit, parce
                        // qu'un fichier qui se contredit lui-meme ne se relit jamais deux fois volontairement.
                        contradicted++;
                    }
                }
            }
            else if (claimed != null) {
                shelf = claimed;
            }
            shelf.offers.add(new Offer(material, name == null || String.valueOf(name).isEmpty()
                    ? pretty(material) : strip(String.valueOf(name)), round(buy), round(buyback),
                    "&7Prix libre : &fshop.extras&7 (aucun générateur derrière)"));
        }
        if (contradicted > 0) {
            plugin.getLogger().warning("[shop] " + contradicted + " offre(s) de `shop.extras` dont la "
                    + "`category` contredit `shop.categories[].materials` : c'est `category` qui gagne, mais "
                    + "aligner les deux evite de relire le fichier deux fois.");
        }
    }

    /** L'offre du comptoir pour cette matière, tous onglets confondus ; {@code null} si inconnue. */
    private static Offer offerOf(Material material) {
        for (Shelf shelf : SHELVES) {
            for (Offer offer : shelf.offers) {
                if (offer.material == material) {
                    return offer;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ inventaire

    /**
     * Places libres pour cette matière : cases vides × taille de pile, plus le reste des piles neutres du
     * même type. Une pile renommée n'est pas comptée — on préfère sous-estimer la place disponible, donc
     * refuser un achat, que promettre un lot qui ne rentrera pas.
     */
    private static int roomFor(Player player, Material material) {
        int max = new ItemStack(material).getMaxStackSize();
        if (max <= 0) {
            max = 64;
        }
        int room = 0;
        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current == null || current.getType().isAir()) {
                room += max;
            }
            else if (current.getType() == material && !current.hasItemMeta()) {
                room += Math.max(0, max - current.getAmount());
            }
        }
        return room;
    }

    private static int count(Player player, Material material) {
        int total = 0;
        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current != null && current.getType() == material && !current.hasItemMeta()) {
                total += current.getAmount();
            }
        }
        return total;
    }

    /** Retire jusqu'à {@code quantity} objets de ce type. Rend ce qui a été effectivement retiré. */
    private static int take(Player player, Material material, int quantity) {
        int left = quantity;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length && left > 0; ++slot) {
            ItemStack current = contents[slot];
            if (current == null || current.getType() != material || current.hasItemMeta()) {
                continue;
            }
            int taken = Math.min(left, current.getAmount());
            left -= taken;
            if (taken >= current.getAmount()) {
                player.getInventory().setItem(slot, null);
            }
            else {
                current.setAmount(current.getAmount() - taken);
                player.getInventory().setItem(slot, current);
            }
        }
        player.updateInventory();
        return quantity - left;
    }

    /** Remet la marchandise. Rend ce qui n'a pas tenu — à rembourser par l'appelant. */
    private static int deliver(Player player, Material material, int quantity) {
        int max = new ItemStack(material).getMaxStackSize();
        if (max <= 0) {
            max = 64;
        }
        int refused = 0;
        for (int sent = 0; sent < quantity; sent += max) {
            ItemStack stack = new ItemStack(material, Math.min(max, quantity - sent));
            for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
                refused += leftover == null ? 0 : leftover.getAmount();
            }
        }
        player.updateInventory();
        return refused;
    }

    // ------------------------------------------------------------------ menu du rendu

    /** Nom et ligne d'info, codes de couleur traduits : la seule touche que le comptoir ajoute à un item. */
    private static ItemStack branded(ItemStack stack, String name, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        if (name != null) {
            meta.setDisplayName(AuctionHouse.color(name));
        }
        List<String> lines = new ArrayList<String>();
        if (lore != null) {
            for (String line : lore) {
                if (line != null) {
                    lines.add(AuctionHouse.color(line));
                }
            }
        }
        meta.setLore(lines);
        stack.setItemMeta(meta);
        return stack;
    }

    // ------------------------------------------------------------------ menus de lecture

    private static String strip(String raw) {
        return raw == null ? "" : raw.replaceAll("(?i)&[0-9a-fk-or]", "");
    }

    private static String pretty(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static String percent(double ratio) {
        return Math.round(ratio * 1000.0D) / 10.0D + " %";
    }

    private static int parseQuantity(String raw) {
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        }
        catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private static Economy economy() {
        ValoriaTycoon plugin = ValoriaTycoon.getInstance();
        return plugin == null ? null : plugin.getEcon();
    }

    private static String text(EconomyResponse response) {
        return response == null || response.errorMessage == null ? "raison inconnue" : response.errorMessage;
    }

    /**
     * Les quantités par clic, dans l'ordre de la config. Une liste vide ou illisible n'est pas une panne : le
     * comptoir garde le triple habituel, et l'admin le découvre au prochain {@code /shop reload}.
     */
    private static int[] readAmounts(FileConfiguration config) {
        List<String> raw = config.getStringList("shop.amounts");
        List<Integer> kept = new ArrayList<Integer>();
        if (raw != null) {
            for (String value : raw) {
                int parsed = parseQuantity(value);
                if (parsed > 0 && !kept.contains(Integer.valueOf(parsed))) {
                    kept.add(Integer.valueOf(parsed));
                }
            }
        }
        if (kept.isEmpty()) {
            return new int[]{1, 16, 64};
        }
        int[] out = new int[kept.size()];
        for (int index = 0; index < out.length; ++index) {
            out[index] = kept.get(index).intValue();
        }
        return out;
    }

    private static String readString(FileConfiguration config, String path, String fallback) {
        return config == null ? fallback : config.getString(path, fallback);
    }

    private static double number(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).replace(',', '.'));
        }
        catch (NumberFormatException numberFormatException) {
            return fallback;
        }
    }
}
