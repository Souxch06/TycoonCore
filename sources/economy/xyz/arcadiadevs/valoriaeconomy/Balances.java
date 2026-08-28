package xyz.arcadiadevs.valoriaeconomy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Coffre-fort des soldes : un fichier YAML, une copie en mémoire, écriture atomique.
 *
 * <ul>
 *   <li><b>Toujours cohérent.</b> Chaque écriture passe par {@code economy.yml.tmp} puis
 *   {@code ATOMIC_MOVE} : un crash en pleine sauvegarde laisse l'ancien fichier intact, jamais un YAML
 *   tronqué qui remettrait tout le monde à zéro.</li>
 *   <li><b>Sauvegarde à chaque mutation.</b> Quelques octets par commande, et un paiement n'est jamais
 *   perdu parce que le serveur est tombé entre deux sauvegardes périodiques.</li>
 *   <li><b>L'UUID seul compte.</b> Les noms ne servent qu'à l'affichage : changer de pseudo ne change pas
 *   de solde, et un pseudo approchant ne peut pas détourner un compte.</li>
 *   <li><b>Jamais de solde négatif.</b> {@link #withdraw} renvoie -1 si l'opération est impossible, plutôt
 *   que de répondre « ok » et laisser un compte à -5.</li>
 * </ul>
 */
public final class Balances {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Double> amounts = new HashMap<UUID, Double>();
    private final Map<UUID, String> names = new HashMap<UUID, String>();
    private double startingBalance = 500.0D;
    private String currencyPlural = "$";
    private String currencySingular = "$";
    private String currencyFormat = "%,.2f";

    public Balances(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "economy.yml");
    }

    public void configure() {
        startingBalance = plugin.getConfig().getDouble("starting-balance", 500.0D);
        currencyPlural = plugin.getConfig().getString("currency.plural", "$");
        currencySingular = plugin.getConfig().getString("currency.singular", "$");
        currencyFormat = plugin.getConfig().getString("currency.format", "%,.2f");
    }

    public void load() {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("dossier de données inaccessible : " + parent);
        }
        if (!file.isFile()) {
            save();
            return;
        }
        ConfigurationSection accounts = YamlConfiguration.loadConfiguration(file).getConfigurationSection("accounts");
        if (accounts == null) {
            return;
        }
        for (String raw : accounts.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                amounts.put(uuid, Double.valueOf(accounts.getDouble(raw + ".balance", 0.0D)));
                names.put(uuid, accounts.getString(raw + ".name", abbreviate(uuid)));
            }
            catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("compte ignoré (UUID invalide) : " + raw);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : amounts.entrySet()) {
            String base = "accounts." + entry.getKey();
            yaml.set(base + ".balance", entry.getValue().doubleValue());
            yaml.set(base + ".name", names.get(entry.getKey()));
        }
        yaml.set("saved-at", System.currentTimeMillis());
        Path target = file.toPath();
        Path temp = target.resolveSibling(file.getName() + ".tmp");
        try {
            Files.write(temp, yaml.saveToString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException exception) {
            plugin.getLogger().severe("sauvegarde des soldes impossible : " + exception.getMessage());
        }
    }

    public boolean exists(OfflinePlayer player) {
        return player != null && amounts.containsKey(player.getUniqueId());
    }

    public boolean exists(String name) {
        return lookup(name) != null;
    }

    /** Crée le compte avec le solde de départ. Renvoie {@code true} si un compte a été créé. */
    public boolean ensureAccount(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (amounts.containsKey(uuid)) {
            return false;
        }
        amounts.put(uuid, Double.valueOf(round(Math.max(0.0D, startingBalance))));
        names.put(uuid, player.getName() == null ? abbreviate(uuid) : player.getName());
        save();
        return true;
    }

    public double balance(OfflinePlayer player) {
        if (player == null) {
            return 0.0D;
        }
        ensureAccount(player);
        Double value = amounts.get(player.getUniqueId());
        return value == null ? 0.0D : value.doubleValue();
    }

    public double balance(String name) {
        UUID uuid = lookup(name);
        return uuid == null ? 0.0D : balance(Bukkit.getOfflinePlayer(uuid));
    }

    /** @return le nouveau solde, ou -1 si l'opération est impossible */
    public double withdraw(OfflinePlayer player, double amount) {
        if (player == null || amount < 0.0D) {
            return -1.0D;
        }
        double before = balance(player);
        if (before + 1.0E-6D < amount) {
            return -1.0D;
        }
        double after = round(before - amount);
        amounts.put(player.getUniqueId(), Double.valueOf(after));
        save();
        return after;
    }

    public double withdraw(String name, double amount) {
        UUID uuid = lookup(name);
        return uuid == null ? -1.0D : withdraw(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public double deposit(OfflinePlayer player, double amount) {
        if (player == null || amount < 0.0D) {
            return 0.0D;
        }
        double after = round(balance(player) + amount);
        amounts.put(player.getUniqueId(), Double.valueOf(after));
        save();
        return after;
    }

    public double deposit(String name, double amount) {
        UUID uuid = lookup(name);
        return uuid == null ? 0.0D : deposit(Bukkit.getOfflinePlayer(uuid), amount);
    }

    /** Fixe un solde (administration). */
    public double set(OfflinePlayer player, double amount) {
        if (player == null) {
            return 0.0D;
        }
        double value = round(Math.max(0.0D, amount));
        ensureAccount(player);
        amounts.put(player.getUniqueId(), Double.valueOf(value));
        save();
        return value;
    }

    public String nameOf(UUID uuid) {
        String name = names.get(uuid);
        return name == null ? abbreviate(uuid) : name;
    }

    /** Plus gros soldes, en paires {@code nom|montant}. */
    public List<String> top(int limit) {
        List<Map.Entry<UUID, Double>> entries = new ArrayList<Map.Entry<UUID, Double>>(amounts.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue()));
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < entries.size() && i < limit; ++i) {
            out.add(nameOf(entries.get(i).getKey()) + "|" + format(entries.get(i).getValue().doubleValue()));
        }
        return out;
    }

    public String format(double amount) {
        try {
            return currencyPlural + String.format(Locale.US, currencyFormat, Double.valueOf(amount));
        }
        catch (RuntimeException exception) {
            return currencyPlural + String.format(Locale.US, "%.2f", Double.valueOf(amount));
        }
    }

    public String currencyPlural() {
        return currencyPlural;
    }

    public String currencySingular() {
        return currencySingular;
    }

    public int accountCount() {
        return amounts.size();
    }

    private UUID lookup(String name) {
        if (name == null) {
            return null;
        }
        String needle = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<UUID, String> entry : names.entrySet()) {
            if (entry.getValue() != null && entry.getValue().toLowerCase(Locale.ROOT).equals(needle)) {
                return entry.getKey();
            }
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline == null ? null : offline.getUniqueId();
    }

    private static String abbreviate(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }

    /** Deux décimales : assez pour un serveur, et ça évite les résidus flottants qui cumulent. */
    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
