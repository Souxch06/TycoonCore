package xyz.arcadiadevs.valoriatycoon.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.guis.AuctionGui;

/**
 * Marché entre joueurs (« auction house ») : un joueur dépose un item et un prix, les autres
 * l'achètent depuis une interface partagée.
 *
 * <p><b>Séquestre serveur.</b> L'item déposé est retiré de l'inventaire du vendeur et écrit dans
 * {@code plugins/ValoriaTycoon/auction.yml}, sauvegardé à chaque mutation. Rien ne vit « en l'air »
 * dans un inventaire d'interface : pas de duplication à la déconnexion, pas de perte au crash, et
 * l'annulation rend l'item (au sol si l'inventaire est plein).</p>
 *
 * <p><b>Synchronisation.</b> Toute mutation (dépôt, achat, annulation) appelle
 * {@link AuctionGui#refreshAll()} : les interfaces ouvertes des autres joueurs sont redessinées
 * immédiatement, sans les refermer. Les mutations n'ont lieu que sur le thread principal (commande
 * en jeu, clic d'inventaire), donc aucune synchronisation n'est nécessaire entre elles.</p>
 *
 * <p><b>Monnaie.</b> Tous les mouvements passent par Vault ({@link Economy}), donc par le plugin
 * d'économie déjà en place (EssentialsX, etc.). Les API Vault sont appelées avec des
 * {@link OfflinePlayer}, seule signature stable d'une version à l'autre.</p>
 */
public final class AuctionHouse {

    /** Espace de noms des données posées par le plugin sur les items de générateur. */
    private static final String PLUGIN_NAMESPACE = "valoriatycoon";

    private static File file;
    private static YamlData data;
    private static boolean enabled = true;
    private static double feeRate = 0.02D;
    private static double minPrice = 1.0D;
    private static double maxPrice = 1000000.0D;
    private static String title = "&aMarché des joueurs";

    private AuctionHouse() {
    }

    /** Charge fichier et configuration à la première utilisation (le plugin n'a pas à être modifié). */
    private static synchronized void ensureLoaded() {
        if (data != null) {
            return;
        }
        ValoriaTycoon plugin = ValoriaTycoon.getInstance();
        file = new File(plugin.getDataFolder(), "auction.yml");
        data = YamlData.load(file);
        enabled = plugin.getConfig().getBoolean("auction-house.enabled", true);
        feeRate = plugin.getConfig().getDouble("auction-house.sell-fee", 0.02D);
        minPrice = plugin.getConfig().getDouble("auction-house.min-price", 1.0D);
        maxPrice = plugin.getConfig().getDouble("auction-house.max-price", 1000000.0D);
        title = plugin.getConfig().getString("auction-house.title", "&aMarché des joueurs");
    }

    public static boolean isEnabled() {
        AuctionHouse.ensureLoaded();
        return enabled;
    }

    public static String title() {
        AuctionHouse.ensureLoaded();
        return AuctionHouse.color(title);
    }

    /** Met en vente l'item tenu en main. Renvoie le message à afficher, ou {@code null} si succès. */
    public static String list(Player player, double price) {
        AuctionHouse.ensureLoaded();
        if (!enabled) {
            return AuctionHouse.color("&cLe marché des joueurs est désactivé.");
        }
        if (!player.hasPermission("valoriatycoon.ah.sell")) {
            return AuctionHouse.color("&cTu n'as pas le droit de vendre sur le marché.");
        }
        if (price < minPrice) {
            return AuctionHouse.color("&cPrix minimum : &f" + AuctionHouse.money(minPrice) + "&c.");
        }
        if (price > maxPrice) {
            return AuctionHouse.color("&cPrix maximum : &f" + AuctionHouse.money(maxPrice) + "&c.");
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return AuctionHouse.color("&cTiens un item en main pour le mettre en vente.");
        }
        if (AuctionHouse.isPluginItem(hand)) {
            return AuctionHouse.color("&cLes items et blocs de générateur ne peuvent pas être vendus ici.");
        }
        double fee = price * feeRate;
        Economy economy = ValoriaTycoon.getInstance().getEcon();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (fee > 0.0D && !economy.has(offline, fee)) {
            return AuctionHouse.color("&cFrais de mise en vente : " + AuctionHouse.money(fee) + ", solde insuffisant.");
        }
        ItemStack escrow = hand.clone();
        if (escrow.getAmount() > 64) {
            escrow.setAmount(64);
        }
        int id = data.nextId();
        data.put(id, player.getUniqueId(), player.getName(), price, escrow);
        if (fee > 0.0D) {
            economy.withdrawPlayer(offline, fee);
        }
        player.getInventory().setItemInMainHand(null);
        AuctionGui.refreshAll();
        AuctionHouse.broadcast(player.getName() + " met en vente " + AuctionHouse.describe(escrow) + " pour " + AuctionHouse.money(price));
        return AuctionHouse.color("&aAnnonce n°&f" + id + "&a : " + AuctionHouse.describe(escrow) + " à " + AuctionHouse.money(price) + " (frais " + AuctionHouse.money(fee) + ").");
    }

    /** Achète une annonce. Renvoie le message à afficher, ou {@code null} si succès. */
    public static String buy(Player player, int id) {
        AuctionHouse.ensureLoaded();
        if (!data.has(id)) {
            return AuctionHouse.color("&cCette annonce vient d'être vendue ou annulée.");
        }
        ItemStack item = data.item(id);
        double price = data.price(id);
        if (item == null) {
            data.remove(id);
            AuctionGui.refreshAll();
            return AuctionHouse.color("&cAnnonce incomplète (item manquant) : retirée du marché.");
        }
        UUID sellerId = data.sellerId(id);
        String sellerName = data.sellerName(id);
        if (sellerId != null && sellerId.equals(player.getUniqueId())) {
            return AuctionHouse.color("&cTu ne peux pas acheter tes propres annonces.");
        }
        Economy economy = ValoriaTycoon.getInstance().getEcon();
        OfflinePlayer buyer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (!economy.has(buyer, price)) {
            return AuctionHouse.color("&cIl te faut " + AuctionHouse.money(price) + ".");
        }
        // L'item est réservé seulement si le joueur peut le recevoir, sinon rien n'est débité.
        Collection<ItemStack> refused = player.getInventory().addItem(item).values();
        if (!refused.isEmpty()) {
            for (ItemStack leftover : refused) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            player.updateInventory();
            return AuctionHouse.color("&eInventaire plein : l'item est tombé à tes pieds, &cpaiement annulé&c.");
        }
        EconomyResponse withdraw = economy.withdrawPlayer(buyer, price);
        if (!withdraw.transactionSuccess()) {
            player.getInventory().removeItem(item);
            player.updateInventory();
            return AuctionHouse.color("&cPaiement refusé : " + withdraw.errorMessage);
        }
        double net = price - price * feeRate;
        if (sellerId != null) {
            economy.depositPlayer(Bukkit.getOfflinePlayer(sellerId), net);
        }
        data.remove(id);
        player.updateInventory();
        AuctionGui.refreshAll();
        Player onlineSeller = sellerId == null ? null : Bukkit.getPlayer(sellerId);
        if (onlineSeller != null && onlineSeller.isOnline()) {
            onlineSeller.sendMessage(AuctionHouse.color("&a" + player.getName() + " a acheté " + AuctionHouse.describe(item) + " &7(+" + AuctionHouse.money(net) + ")"));
        }
        AuctionHouse.broadcast(player.getName() + " achète " + AuctionHouse.describe(item) + " à " + sellerName + " pour " + AuctionHouse.money(price));
        return AuctionHouse.color("&aAcheté " + AuctionHouse.describe(item) + " pour " + AuctionHouse.money(price) + ".");
    }

    /** Rend au joueur toutes ses annonces en cours. Renvoie le message à afficher. */
    public static String cancel(Player player) {
        AuctionHouse.ensureLoaded();
        List<Integer> owned = data.sellerListings(player.getUniqueId());
        if (owned.isEmpty()) {
            return AuctionHouse.color("&cTu n'as aucune annonce en cours.");
        }
        for (Integer id : owned) {
            ItemStack item = data.item(id.intValue());
            if (item != null) {
                Collection<ItemStack> refused = player.getInventory().addItem(item).values();
                for (ItemStack leftover : refused) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            data.remove(id.intValue());
        }
        player.updateInventory();
        AuctionGui.refreshAll();
        return AuctionHouse.color("&a" + owned.size() + " annonce(s) annulée(s), items rendus.");
    }

    public static int count() {
        AuctionHouse.ensureLoaded();
        return data.ids().size();
    }

    public static List<Integer> ids() {
        AuctionHouse.ensureLoaded();
        return data.ids();
    }

    public static ItemStack itemAt(int id) {
        return data.item(id);
    }

    public static double priceAt(int id) {
        return data.price(id);
    }

    public static String sellerAt(int id) {
        return data.sellerName(id);
    }

    public static String money(double value) {
        Economy economy = ValoriaTycoon.getInstance() == null ? null : ValoriaTycoon.getInstance().getEcon();
        return economy == null ? String.format("%.2f", value) : economy.format(value);
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /** Un item porte-t-il une donnée posée par ce plugin (bloc/objet de générateur identifié) ? */
    private static boolean isPluginItem(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Set<String> keys = container.getKeys();
        for (String key : keys) {
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
                online.sendMessage(AuctionHouse.color("&8[&aAH&8] &7" + message));
            }
        }
    }

    private static String describe(ItemStack item) {
        return item.getType().name().toLowerCase().replace('_', ' ') + " x" + item.getAmount();
    }

    /**
     * Stockage YAML du séquestre : {@code listings.<id>.item}, prix, vendeur, date.
     * Écriture immédiate à chaque mutation — c'est ce qui rend une perte d'objet impossible, au prix
     * d'un écriture disque par action (negligeable : le marché est un trafic de clics joueurs).
     */
    private static final class YamlData {
        private final File file;
        private final org.bukkit.configuration.file.YamlConfiguration yaml;

        private YamlData(File file, org.bukkit.configuration.file.YamlConfiguration yaml) {
            this.file = file;
            this.yaml = yaml;
        }

        static YamlData load(File file) {
            return new YamlData(file, file.isFile()
                    ? org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)
                    : new org.bukkit.configuration.file.YamlConfiguration());
        }

        private void save() {
            try {
                this.yaml.save(this.file);
            }
            catch (IOException ioException) {
                ValoriaTycoon.getInstance().getLogger().warning("[AH] sauvegarde impossible : " + ioException.getMessage());
            }
        }

        int nextId() {
            return this.yaml.getInt("next-id", 1);
        }

        void put(int id, UUID seller, String sellerName, double price, ItemStack item) {
            String path = "listings." + id;
            this.yaml.set(path + ".seller", seller.toString());
            this.yaml.set(path + ".seller-name", sellerName);
            this.yaml.set(path + ".price", price);
            this.yaml.set(path + ".created", System.currentTimeMillis());
            this.yaml.set(path + ".item", item);
            this.yaml.set("next-id", id + 1);
            this.save();
        }

        void remove(int id) {
            this.yaml.set("listings." + id, null);
            org.bukkit.configuration.ConfigurationSection section = this.yaml.getConfigurationSection("listings");
            if (section == null || section.getKeys(false).isEmpty()) {
                this.yaml.set("listings", null);
            }
            this.save();
        }

        boolean has(int id) {
            return this.yaml.isConfigurationSection("listings." + id);
        }

        ItemStack item(int id) {
            return this.yaml.getItemStack("listings." + id + ".item");
        }

        double price(int id) {
            return this.yaml.getDouble("listings." + id + ".price");
        }

        String sellerName(int id) {
            return this.yaml.getString("listings." + id + ".seller-name", "inconnu");
        }

        UUID sellerId(int id) {
            String raw = this.yaml.getString("listings." + id + ".seller", null);
            if (raw == null) {
                return null;
            }
            try {
                return UUID.fromString(raw);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                return null;
            }
        }

        List<Integer> ids() {
            List<Integer> ids = new ArrayList<Integer>();
            org.bukkit.configuration.ConfigurationSection section = this.yaml.getConfigurationSection("listings");
            if (section == null) {
                return ids;
            }
            for (String key : section.getKeys(false)) {
                try {
                    ids.add(Integer.valueOf(Integer.parseInt(key)));
                }
                catch (NumberFormatException numberFormatException) {
                    // clé inattendue dans le fichier : ignorée, elle reste sur le disque
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
    }
}
