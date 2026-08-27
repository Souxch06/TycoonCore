package xyz.arcadiadevs.valoriatycoon.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.guis.AuctionGui;

/**
 * Marché entre joueurs : dépôt d'annonces, achat partiel, expiration, retours, statistiques de prix.
 *
 * <h2>Invariants — ce qui rend le module sûr pour des objets et de la monnaie réels</h2>
 * <ol>
 *   <li><b>Séquestre serveur.</b> L'item déposé quitte l'inventaire du vendeur et vit dans
 *   {@code plugins/ValoriaTycoon/auction.yml}. Rien n'est jamais stocké dans une interface : pas de
 *   duplication à la déconnexion, pas de perte au crash, et l'état survit aux redémarrages.</li>
 *   <li><b>Prix unitaire.</b> Une annonce porte un prix <em>à la pièce</em> et une quantité, donc tout
 *   achat partiel est exact au centime près — pas d'arrondi qui crée de la monnaie ou perd un item.</li>
 *   <li><b>Livraison avant facturation, remboursement symétrique.</b> On tente de livrer ; ce qui n'a
 *   pas pu entrer est remboursé et rendu à l'annonce. Aucun chemin ne donne un item gratuit, un débit
 *   sans contrepartie, ni un vendeur crédité deux fois.</li>
 *   <li><b>Écriture atomique.</b> Sauvegarde en {@code .tmp} puis {@code ATOMIC_MOVE} : un crash pendant
 *   l'écriture laisse l'ancien fichier intact au lieu d'un YAML tronqué.</li>
 *   <li><b>Retours (mailbox).</b> Item expiré, annonce retirée par un admin ou vente annulée pour un
 *   vendeur déconnecté → l'item est rangé dans {@code returns.<uuid>} et rendu au prochain connect.
 *   Un item ne peut disparaître « parce que le joueur n'était pas là ».</li>
 *   <li><b>Contre-expertise de prix.</b> Le marché garde la moyenne par type d'item et refuse les
 *   annonces aberrantes (bande configurable), ce qui bloque dump, blanchiment et arnaques au prix ×1000.</li>
 * </ol>
 *
 * <p>Monnaie uniquement via Vault, sur des {@link OfflinePlayer} (seules signatures stables entre
 * versions). Aucun nom interne du serveur : {@code net.minecraft} et le paquet CraftBukkit ne sont pas
 * utilisés, donc le module ne casse pas quand Minecraft renomme ses classes.</p>
 */
public final class AuctionHouse {

    private static final String PLUGIN_NAMESPACE = "valoriatycoon";
    private static final int HISTORY_KEEP = 60;
    private static final int RETURN_KEEP_PER_PLAYER = 24;

    private static Store store;
    private static boolean started = false;

    private static boolean enabled = true;
    private static double listingFee = 0.0D;
    private static double salesTax = 0.02D;
    private static double minPrice = 0.5D;
    private static double maxPrice = 10000000.0D;
    private static double priceBand = 12.0D;
    private static boolean enforceBand = true;
    private static int maxListings = 6;
    private static int expiryHours = 72;
    private static int sweepTicks = 1200;
    private static Set<String> blacklist = new HashSet<String>();
    private static String title = "&aMarché des joueurs";

    private AuctionHouse() {
    }

    /** Charge fichier + configuration, et planifie le ménage des annonces expirées. Une seule fois. */
    public static synchronized void ensureStarted() {
        if (started) {
            return;
        }
        started = true;
        ValoriaTycoon plugin = ValoriaTycoon.getInstance();
        store = new Store(new File(plugin.getDataFolder(), "auction.yml"));
        readConfig(plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            AuctionHouse.sweep();
            AuctionGui.refreshAll();
        }, 40L, Math.max(200, sweepTicks));
    }

    private static void readConfig(ValoriaTycoon plugin) {
        enabled = plugin.getConfig().getBoolean("auction-house.enabled", true);
        listingFee = plugin.getConfig().getDouble("auction-house.listing-fee", 0.0D);
        salesTax = plugin.getConfig().getDouble("auction-house.sales-tax", 0.02D);
        minPrice = plugin.getConfig().getDouble("auction-house.min-price", 0.5D);
        maxPrice = plugin.getConfig().getDouble("auction-house.max-price", 10000000.0D);
        priceBand = Math.max(1.5D, plugin.getConfig().getDouble("auction-house.price-band", 12.0D));
        enforceBand = plugin.getConfig().getBoolean("auction-house.enforce-price-band", true);
        maxListings = Math.max(1, plugin.getConfig().getInt("auction-house.max-listings-per-player", 6));
        expiryHours = plugin.getConfig().getInt("auction-house.expiry-hours", 72);
        sweepTicks = plugin.getConfig().getInt("auction-house.sweep-ticks", 1200);
        title = plugin.getConfig().getString("auction-house.title", "&aMarché des joueurs");
        blacklist.clear();
        for (String entry : plugin.getConfig().getStringList("auction-house.blacklist")) {
            blacklist.add(entry.toUpperCase(Locale.ROOT).replace(' ', '_').trim());
        }
        if (blacklist.isEmpty()) {
            for (String entry : new String[]{"BEDROCK", "BARRIER", "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK",
                    "REPEATING_COMMAND_BLOCK", "STRUCTURE_BLOCK", "JIGSAW", "END_PORTAL", "END_GATEWAY",
                    "LIGHT", "STRUCTURE_VOID", "DEBUG_STICK"}) {
                blacklist.add(entry);
            }
        }
    }

    /** `/ah reload` : reprend les clés de config sans redémarrer le serveur. */
    public static String reload(ValoriaTycoon plugin) {
        plugin.reloadConfig();
        readConfig(plugin);
        return color("&aMarché rechargé &7(" + count() + " annonce(s), expire="
                + expiryHours + "h, taxe=" + (int) (salesTax * 100) + "%)");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String title() {
        return color(title);
    }

    // ------------------------------------------------------------------ mise en vente

    /**
     * Met en vente {@code quantity} exemplaires de l'item en main à {@code unitPrice} la pièce.
     * Renvoie le message à afficher, ou {@code null} quand la mise en vente a réussi.
     */
    public static String list(Player player, double unitPrice, int quantity) {
        ensureStarted();
        if (!enabled) {
            return color("&cLe marché des joueurs est désactivé.");
        }
        if (!player.hasPermission("valoriatycoon.ah.sell")) {
            return color("&cTu n'as pas le droit de vendre sur le marché.");
        }
        if (quantity <= 0) {
            return color("&7Quantité invalide : &f/ah sell <prix> [quantité]");
        }
        if (unitPrice < minPrice) {
            return color("&cPrix minimum à la pièce : &f" + money(minPrice) + "&c.");
        }
        if (unitPrice > maxPrice) {
            return color("&cPrix maximum à la pièce : &f" + money(maxPrice) + "&c.");
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return color("&cTiens un item en main pour le mettre en vente.");
        }
        if (blacklist.contains(hand.getType().name())) {
            return color("&cCet item n'est pas échangeable sur le marché.");
        }
        if (isPluginItem(hand)) {
            return color("&cLes items et blocs de générateur ne peuvent pas être vendus ici.");
        }
        if (quantity > hand.getAmount()) {
            quantity = hand.getAmount();
        }
        if (countFor(player.getUniqueId()) >= maxListings) {
            return color("&cTu as déjà &f" + maxListings + "&c annonces en cours (&f/ah cancel&c pour alléger).");
        }
        if (enforceBand) {
            double average = average(hand.getType());
            if (average > 0.0D && (unitPrice > average * priceBand || unitPrice < average / priceBand)) {
                return color("&cPrix hors bande : la moyenne du marché est &f" + money(average)
                        + "&c, accepté entre &f" + money(average / priceBand) + "&c et &f" + money(average * priceBand) + "&c.");
            }
        }
        double fee = unitPrice * quantity * listingFee;
        Economy economy = economy();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (fee > 0.0D && !economy.has(offline, fee)) {
            return color("&cFrais de mise en vente : " + money(fee) + ", solde insuffisant.");
        }
        ItemStack escrow = hand.clone();
        escrow.setAmount(quantity);
        int id = store.nextId();
        store.put(id, player.getUniqueId(), player.getName(), unitPrice, quantity, escrow, expiryHours);
        if (fee > 0.0D) {
            economy.withdrawPlayer(offline, fee);
        }
        int left = hand.getAmount() - quantity;
        if (left <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(left);
            player.getInventory().setItemInMainHand(hand);
        }
        player.updateInventory();
        AuctionGui.refreshAll();
        broadcast(player.getName() + " propose " + quantity + "× " + describe(escrow) + " à " + money(unitPrice));
        return color("&aAnnonce n°&f" + id + "&a : " + quantity + "× " + materialName(escrow.getType())
                + " à " + money(unitPrice) + "&a la pièce" + (fee > 0.0D ? " (frais " + money(fee) + ")" : "")
                + "&a." + (expiryHours > 0 ? " &7Elle expire dans &f" + expiryHours + "h&7." : ""));
    }

    // ------------------------------------------------------------------ achat

    /**
     * Achat. {@code wanted} = nombre de pièces voulues, ou {@code -1} pour tout le lot.
     *
     * <p>Ordre : débit, livraison, remboursement symétrique du non-livré, puis paiement du vendeur.
     * Toute sortie anticipée laisse le joueur et l'annonce dans un état cohérent.</p>
     */
    public static String buy(Player player, int id, int wanted) {
        ensureStarted();
        if (!store.has(id)) {
            return color("&cCette annonce vient d'être vendue, annulée ou expirée.");
        }
        UUID sellerId = store.sellerId(id);
        if (sellerId != null && sellerId.equals(player.getUniqueId())) {
            return color("&cTu ne peux pas acheter tes propres annonces.");
        }
        String sellerName = store.sellerName(id);
        ItemStack lot = store.item(id);
        if (lot == null) {
            store.remove(id);
            AuctionGui.refreshAll();
            return color("&cAnnonce incomplète (item illisible) : retirée, rien ne t'est débité.");
        }
        int available = store.amount(id);
        if (available <= 0) {
            available = lot.getAmount();
        }
        int want = wanted <= 0 || wanted > available ? available : wanted;
        double unit = store.unitPrice(id);
        double total = unit * want;
        Economy economy = economy();
        OfflinePlayer buyer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (!economy.has(buyer, total)) {
            return color("&cIl te faut " + money(total) + " pour " + want + "× " + materialName(lot.getType()) + ".");
        }
        EconomyResponse withdraw = economy.withdrawPlayer(buyer, total);
        if (!withdraw.transactionSuccess()) {
            return color("&cPaiement refusé par l'économie : " + withdraw.errorMessage);
        }
        ItemStack batch = lot.clone();
        batch.setAmount(want);
        Collection<ItemStack> refused = player.getInventory().addItem(batch).values();
        int notDelivered = 0;
        for (ItemStack leftover : refused) {
            notDelivered += leftover.getAmount();
        }
        int delivered = want - notDelivered;
        if (delivered <= 0) {
            // Rien n'a pu être remis : on rend l'intégralité et on ne touche pas à l'annonce.
            economy.depositPlayer(buyer, total);
            player.updateInventory();
            return color("&eInventaire plein : achat annulé, &a" + money(total) + "&e rendu.");
        }
        if (notDelivered > 0) {
            // Le non-livré est remboursé et rendu à l'annonce. Si le remboursement était refusé par
            // l'économie, les items sont posés aux pieds de l'acheteur : un objet ne se perd jamais,
            // au prix d'une monnaie compensée et tracée dans le log.
            EconomyResponse back = economy.depositPlayer(buyer, unit * notDelivered);
            if (!back.transactionSuccess()) {
                for (ItemStack leftover : refused) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                ValoriaTycoon.getInstance().getLogger().warning("[AH] remboursement refusé (" + back.errorMessage
                        + ") pour " + player.getName() + ", items rendus au sol, annonce n°" + id);
            }
            store.setAmount(id, available - delivered);
            total = unit * delivered;
        }
        double net = total - total * salesTax;
        if (sellerId != null) {
            economy.depositPlayer(Bukkit.getOfflinePlayer(sellerId), net);
        }
        player.updateInventory();
        Player onlineSeller = sellerId == null ? null : Bukkit.getPlayer(sellerId);
        if (onlineSeller != null && onlineSeller.isOnline()) {
            onlineSeller.sendMessage(color("&a" + player.getName() + " achète " + delivered + "× "
                    + materialName(lot.getType()) + " &7(+" + money(net) + ")"));
        }
        store.recordSale(lot.getType(), unit, delivered, player.getName(), sellerName);
        if (delivered >= available) {
            store.remove(id);
        }
        AuctionGui.refreshAll();
        broadcast(player.getName() + " achète " + delivered + "× " + materialName(lot.getType())
                + " à " + sellerName + " pour " + money(total));
        return color("&aAcheté " + delivered + "× " + materialName(lot.getType()) + " pour " + money(total) + ".");
    }

    // ------------------------------------------------------------------ annulation / admin

    /** Annule une annonce précise (id > 0) ou toutes celles du joueur (id <= 0). */
    public static String cancel(Player player, int id) {
        ensureStarted();
        if (id > 0) {
            if (!store.has(id)) {
                return color("&cAnnonce n°&f" + id + "&c introuvable (vendue ou expirée).");
            }
            if (!player.hasPermission("valoriatycoon.ah.admin") && !player.getUniqueId().equals(store.sellerId(id))) {
                return color("&cCette annonce ne t'appartient pas.");
            }
            ItemStack item = store.item(id);
            deliver(player, item);
            store.remove(id);
            AuctionGui.refreshAll();
            return color("&aAnnonce n°&f" + id + "&a annulée, item rendu.");
        }
        List<Integer> owned = store.sellerListings(player.getUniqueId());
        if (owned.isEmpty()) {
            return color("&cTu n'as aucune annonce en cours.");
        }
        for (Integer entry : owned) {
            deliver(player, store.item(entry.intValue()));
            store.remove(entry.intValue());
        }
        AuctionGui.refreshAll();
        return color("&a" + owned.size() + " annonce(s) annulée(s), items rendus.");
    }

    /**
     * Retrait administratif d'une annonce : l'objet revient au vendeur, en main propre s'il est
     * connecté, sinon déposé dans sa boîte de rendus (rien n'est détruit, même pour un absent).
     */
    public static String adminRemove(int id, Player admin) {
        ensureStarted();
        if (!store.has(id)) {
            return color("&cAnnonce n°&f" + id + "&c introuvable.");
        }
        UUID seller = store.sellerId(id);
        ItemStack item = store.item(id);
        String name = store.sellerName(id);
        if (seller != null && item != null) {
            Player online = Bukkit.getPlayer(seller);
            if (online != null && online.isOnline()) {
                deliver(online, item);
            } else {
                store.addReturn(seller, item);
            }
        }
        store.remove(id);
        AuctionGui.refreshAll();
        return color("&aAnnonce n°&f" + id + "&a retirée" + (seller == null ? "" : ", item renvoyé à &f" + name) + "&a.");
    }

    /** Rend les items en attente (expiration, retrait admin, vente interrompue). */
    public static void deliverReturns(Player player) {
        ensureStarted();
        List<ItemStack> pending = store.takeReturns(player.getUniqueId());
        if (pending.isEmpty()) {
            return;
        }
        for (ItemStack item : pending) {
            deliver(player, item);
        }
        player.sendMessage(color("&a" + pending.size() + " objet(s) du marché te "
                + (pending.size() == 1 ? "sont rendus" : " sont rendus") + "&a."));
    }

    /** Cycle d'expiration : à appeler périodiquement ; n'affecte que les annonces réellement échues. */
    public static void sweep() {
        if (store == null || expiryHours <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        int expired = 0;
        for (Integer id : store.ids()) {
            long expires = store.expires(id);
            if (expires <= 0L || expires > now) {
                continue;
            }
            UUID seller = store.sellerId(id);
            ItemStack item = store.item(id);
            if (seller != null && item != null) {
                Player online = Bukkit.getPlayer(seller);
                if (online != null && online.isOnline()) {
                    deliver(online, item);
                    online.sendMessage(color("&7Ton annonce n°&f" + id + "&7 a expiré : item rendu."));
                } else {
                    store.addReturn(seller, item);
                }
            }
            store.remove(id.intValue());
            expired++;
        }
        if (expired > 0) {
            store.save();
            AuctionGui.refreshAll();
        }
    }

    // ------------------------------------------------------------------ lectures pour la vue

    public static List<Integer> ids(String filter, UUID ownOnly) {
        ensureStarted();
        List<Integer> out = new ArrayList<Integer>();
        if (store == null) {
            return out;
        }
        String needle = filter == null ? "" : filter.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (Integer id : store.ids()) {
            ItemStack item = store.item(id.intValue());
            if (item == null) {
                continue;
            }
            if (!needle.isEmpty() && !item.getType().name().contains(needle)) {
                continue;
            }
            if (ownOnly != null && !ownOnly.equals(store.sellerId(id.intValue()))) {
                continue;
            }
            out.add(id);
        }
        return out;
    }

    public static List<Integer> sortedIds(String filter, UUID ownOnly, int sort) {
        List<Integer> out = ids(filter, ownOnly);
        if (sort == 1) {
            out.sort((a, b) -> Double.compare(unitPriceAt(a.intValue()), unitPriceAt(b.intValue())));
        } else if (sort == 2) {
            out.sort((a, b) -> Double.compare(unitPriceAt(b.intValue()), unitPriceAt(a.intValue())));
        } else if (sort == 3) {
            out.sort((a, b) -> Long.compare(store.created(b.intValue()), store.created(a.intValue())));
        }
        return out;
    }

    public static int count() {
        return store == null ? 0 : store.ids().size();
    }

    public static int countFor(UUID seller) {
        return store == null ? 0 : store.sellerListings(seller).size();
    }

    public static ItemStack itemAt(int id) {
        return store == null ? null : store.item(id);
    }

    public static double unitPriceAt(int id) {
        return store == null ? 0.0D : store.unitPrice(id);
    }

    /**
     * Total de l'annonce. Pour une annonce reprise de l'ancien format (prix au lot), on rend le prix
     * du lot tel qu'il était stocké plutôt qu'un aller-retour lot → pièce → lot qui perderait des
     * centimes à chaque affichage.
     */
    public static double totalAt(int id) {
        return store == null ? 0.0D : store.total(id);
    }

    public static int amountAt(int id) {
        return store == null ? 0 : store.amount(id);
    }

    public static UUID sellerIdOf(int id) {
        return store == null ? null : store.sellerId(id);
    }

    public static String sellerAt(int id) {
        return store == null ? "inconnu" : store.sellerName(id);
    }

    public static long expiresAt(int id) {
        return store == null ? 0L : store.expires(id);
    }

    public static double average(Material material) {
        return store == null ? 0.0D : store.average(material);
    }

    public static int salesOf(Material material) {
        return store == null ? 0 : store.sales(material);
    }

    /** Résumé pour {@code /ah info} et {@code /ah stats}. */
    public static String summary(Player player) {
        StringBuilder builder = new StringBuilder();
        builder.append(color("&8[&aAH&8] &f").append(count()).append(" annonce(s), &f")
                .append(countFor(player.getUniqueId())).append(" à toi (max ")
                .append(maxListings).append(")"));
        builder.append(color("&7, taxe ").append((int) (salesTax * 100)).append("%, expire ")
                .append(expiryHours <= 0 ? "jamais" : expiryHours + "h"));
        String blacklisted = String.join(", ", blacklist);
        builder.append(color("&7, blacklist ").append(blacklisted.length() > 60
                ? blacklisted.substring(0, 60) + "…" : blacklisted));
        return builder.toString();
    }

    public static String money(double value) {
        Economy economy = economy();
        return economy == null ? String.format(Locale.ROOT, "%.2f", value) : economy.format(value);
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // ------------------------------------------------------------------ utilitaires internes

    private static Economy economy() {
        ValoriaTycoon plugin = ValoriaTycoon.getInstance();
        return plugin == null ? null : plugin.getEcon();
    }

    private static void deliver(Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        Collection<ItemStack> refused = player.getInventory().addItem(item).values();
        for (ItemStack leftover : refused) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.updateInventory();
    }

    /** Un item porte-t-il une donnée posée par ce plugin (bloc/objet de générateur identifié) ? */
    private static boolean isPluginItem(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (String key : container.getKeys()) {
            if (key.startsWith(PLUGIN_NAMESPACE + ":")) {
                return true;
            }
        }
        try {
            return container.has(new NamespacedKey(PLUGIN_NAMESPACE, "spawnitem.tier"), PersistentDataType.INTEGER)
                    || container.has(new NamespacedKey(PLUGIN_NAMESPACE, "blocktype.tier"), PersistentDataType.INTEGER);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }

    private static void broadcast(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("valoriatycoon.ah.notify")) {
                online.sendMessage(color("&8[&aAH&8] &7" + message));
            }
        }
    }

    private static String describe(ItemStack item) {
        return item.getAmount() + "× " + materialName(item.getType());
    }

    private static String materialName(Material material) {
        return material.name().charAt(0) + material.name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    // ------------------------------------------------------------------ stockage YAML atomique

    /**
     * Un fichier, trois zones : {@code listings}, {@code stats} (moyenne par type, pour la bande de
     * prix), {@code returns} (mailbox des joueurs hors-ligne) et un {@code history} borné.
     * L'écriture passe par un fichier temporaire + {@code ATOMIC_MOVE}.
     */
    private static final class Store {
        private final File file;
        private YamlConfiguration yaml;

        private Store(File file) {
            this.file = file;
            this.yaml = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        }

        private void save() {
            Path target = this.file.toPath();
            Path temp = target.resolveSibling(this.file.getName() + ".tmp");
            try {
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.write(temp, this.yaml.saveToString().getBytes(StandardCharsets.UTF_8));
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch (IOException ioException) {
                ValoriaTycoon.getInstance().getLogger().warning("[AH] sauvegarde impossible : " + ioException.getMessage());
            }
        }

        private void reload() {
            this.yaml = this.file.isFile() ? YamlConfiguration.loadConfiguration(this.file) : new YamlConfiguration();
        }

        int nextId() {
            return this.yaml.getInt("next-id", 1);
        }

        void put(int id, UUID seller, String sellerName, double unitPrice, int amount, ItemStack item, int expiryHours) {
            String path = "listings." + id;
            this.yaml.set(path + ".seller", seller.toString());
            this.yaml.set(path + ".seller-name", sellerName);
            this.yaml.set(path + ".unit-price", unitPrice);
            this.yaml.set(path + ".amount", amount);
            this.yaml.set(path + ".created", System.currentTimeMillis());
            this.yaml.set(path + ".expires", expiryHours > 0L ? System.currentTimeMillis() + (long) expiryHours * 3600000L : 0L);
            this.yaml.set(path + ".item", item);
            this.yaml.set("next-id", id + 1);
            this.save();
        }

        void setAmount(int id, int amount) {
            if (amount <= 0) {
                this.remove(id);
                return;
            }
            this.yaml.set("listings." + id + ".amount", amount);
            this.save();
        }

        void remove(int id) {
            this.yaml.set("listings." + id, null);
            ConfigurationSection section = this.yaml.getConfigurationSection("listings");
            if (section == null || section.getKeys(false).isEmpty()) {
                this.yaml.set("listings", null);
            }
            this.save();
        }

        boolean has(int id) {
            return this.yaml.isConfigurationSection("listings." + id);
        }

        private ConfigurationSection section(int id) {
            return this.yaml.getConfigurationSection("listings." + id);
        }

        ItemStack item(int id) {
            ConfigurationSection section = this.section(id);
            if (section == null) {
                return null;
            }
            return section.getItemStack("item");
        }

        /**
         * Prix à la pièce. Les annonces écrites par la version précédente stockaient un prix AU LOT
         * (champ {@code price}) sans quantité : sans ce repli, une annonce existante se lirait
         * {@code unit-price = 0} et serait achetable gratuitement. On reconvertit donc lot/quantité,
         * la quantité étant déduite de la pile séquestrée.
         */
        double unitPrice(int id) {
            ConfigurationSection section = this.section(id);
            if (section == null) {
                return 0.0D;
            }
            if (section.contains("unit-price")) {
                return section.getDouble("unit-price", 0.0D);
            }
            double lotPrice = section.getDouble("price", 0.0D);
            int size = this.amount(id);
            return size > 1 ? lotPrice / size : lotPrice;
        }

        int amount(int id) {
            ConfigurationSection section = this.section(id);
            if (section == null) {
                return 1;
            }
            if (section.contains("amount")) {
                return Math.max(1, section.getInt("amount", 1));
            }
            ItemStack lot = section.getItemStack("item");
            return lot == null ? 1 : Math.max(1, lot.getAmount());
        }

        double total(int id) {
            ConfigurationSection section = this.section(id);
            if (section == null) {
                return 0.0D;
            }
            if (!section.contains("unit-price") && section.contains("price")) {
                return section.getDouble("price", 0.0D);
            }
            return section.getDouble("unit-price", 0.0D) * this.amount(id);
        }

        long expires(int id) {
            return this.yaml.getLong("listings." + id + ".expires", 0L);
        }

        long created(int id) {
            return this.yaml.getLong("listings." + id + ".created", 0L);
        }

        String sellerName(int id) {
            String name = this.yaml.getString("listings." + id + ".seller-name", null);
            return name == null ? "inconnu" : name;
        }

        UUID sellerId(int id) {
            String raw = this.yaml.getString("listings." + id + ".seller", null);
            if (raw == null) {
                return null;
            }
            try {
                return UUID.fromString(raw);
            }
            catch (IllegalArgumentException exception) {
                return null;
            }
        }

        List<Integer> ids() {
            List<Integer> ids = new ArrayList<Integer>();
            ConfigurationSection section = this.yaml.getConfigurationSection("listings");
            if (section == null) {
                return ids;
            }
            for (String key : section.getKeys(false)) {
                try {
                    ids.add(Integer.valueOf(Integer.parseInt(key)));
                }
                catch (NumberFormatException exception) {
                    // clé inattendue : laissée sur le disque, ignorée à l'affichage
                }
            }
            java.util.Collections.sort(ids);
            return ids;
        }

        List<Integer> sellerListings(UUID seller) {
            List<Integer> owned = new ArrayList<Integer>();
            for (Integer id : this.ids()) {
                UUID current = this.sellerId(id.intValue());
                if (current != null && current.equals(seller)) {
                    owned.add(id);
                }
            }
            return owned;
        }

        /** Vendeur encore en vie de l'annonce — utilisé pour les messages post-vente. */
        void recordSale(Material material, double unitPrice, int delivered, String buyer, String sellerName) {
            String path = "stats." + material.name();
            int sales = this.yaml.getInt(path + ".sales", 0);
            double average = this.yaml.getDouble(path + ".average", 0.0D);
            double updated = sales == 0 ? unitPrice : (average * sales + unitPrice) / (sales + 1);
            this.yaml.set(path + ".sales", sales + delivered);
            this.yaml.set(path + ".average", updated);
            this.yaml.set(path + ".last", unitPrice);
            this.yaml.set(path + ".updated", System.currentTimeMillis());
            int history = this.yaml.getInt("history-size", 0);
            ConfigurationSection section = this.yaml.createSection("history." + history);
            section.set("item", material.name());
            section.set("price", unitPrice);
            section.set("amount", delivered);
            section.set("buyer", buyer);
            section.set("seller", sellerName);
            section.set("at", System.currentTimeMillis());
            this.yaml.set("history-size", history + 1 > HISTORY_KEEP ? 0 : history + 1);
            this.save();
        }

        double average(Material material) {
            return this.yaml.getDouble("stats." + material.name() + ".average", 0.0D);
        }

        int sales(Material material) {
            return this.yaml.getInt("stats." + material.name() + ".sales", 0);
        }

        void addReturn(UUID seller, ItemStack item) {
            String base = "returns." + seller;
            int index = this.yaml.getInt(base + "-size", 0);
            if (index >= RETURN_KEEP_PER_PLAYER) {
                ValoriaTycoon.getInstance().getLogger().warning("[AH] boîte de rendements pleine pour " + seller
                        + " : un item a été écarté, à vérifier dans auction.yml");
            }
            this.yaml.set(base + "." + index + ".item", item);
            this.yaml.set(base + "-size", index + 1);
            this.save();
        }

        List<ItemStack> takeReturns(UUID seller) {
            List<ItemStack> items = new ArrayList<ItemStack>();
            String base = "returns." + seller;
            ConfigurationSection section = this.yaml.getConfigurationSection(base);
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ItemStack item = section.getItemStack(key + ".item");
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            if (!items.isEmpty()) {
                this.yaml.set(base, null);
                this.yaml.set(base + "-size", null);
                this.save();
            }
            return items;
        }
    }
}
