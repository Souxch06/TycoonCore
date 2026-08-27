package xyz.arcadiadevs.valoriateconomy;

import java.util.List;
import org.bukkit.OfflinePlayer;

/**
 * L'économie du serveur, telle que ValoriaTycoon (et n'importe quel autre plugin du dépôt)
 * la consultera : un service {@code ServicesManager} enregistré sous cette interface.
 *
 * <h2>Pourquoi cette interface existe ici</h2>
 * <p>Le plugin d'origine passait par l'API Vault, ce qui obligeait à installer un plugin Vault
 * (ou son fork maintenu) en plus de la banque elle-même. Le serveur ne devant contenir que des
 * plugins fabriqués dans ce dépôt, la monnaie est exposée sous notre propre interface, avec la
 * <b>même</b> surface de méthodes que l'appel compilé attend — les corps ont été relevés dans les
 * {@code .class} livrés (voir {@code scripts/verify-paper26-compat.py}).</p>
 *
 * <h2>Règles d'or</h2>
 * <ul>
 *   <li><b>Ne jamais retirer une méthode.</b> Une signature disparue = {@code NoSuchMethodError}
 *       au premier appel d'un plugin. Ajouter est permis.</li>
 *   <li><b>Aucune dépendance externe.</b> Cette interface n'importe que le JDK, Bukkit et
 *       {@link EconomyResponse}.</li>
 *   <li>Les fournisseurs qui ne gèrent pas les banques renvoient
 *       {@link EconomyResponse#NOT_IMPLEMENTED} au lieu de lever une exception : un appelant
 *       mal intentionné ne doit pas faire tomber le serveur.</li>
 * </ul>
 *
 * <p><b>Fichier généré</b> par {@code scripts/generate-economy-api.py} depuis
 * {@code docs/economy-api.txt} : ajouter une méthode se fait dans le snapshot, pas ici.</p>
 */
public interface Economy {


    boolean isEnabled();

    String getName();

    boolean hasBankSupport();

    int fractionalDigits();

    String formatMoney(double amount);

    String currencyNamePlural();

    String currencyNameSingular();

    boolean hasAccount(String playerName);

    boolean hasAccount(OfflinePlayer player);

    boolean hasAccount(String playerName, String worldName);

    boolean hasAccount(OfflinePlayer player, String worldName);

    double getBalance(String playerName);

    double getBalance(OfflinePlayer player);

    double getBalance(String playerName, String world);

    double getBalance(OfflinePlayer player, String world);

    boolean has(String playerName, double amount);

    boolean has(OfflinePlayer player, double amount);

    boolean has(String playerName, String worldName, double amount);

    boolean has(OfflinePlayer player, String worldName, double amount);

    EconomyResponse withdrawPlayer(String playerName, double amount);

    EconomyResponse withdrawPlayer(OfflinePlayer player, double amount);

    EconomyResponse withdrawPlayer(String playerName, String worldName, double amount);

    EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount);

    EconomyResponse depositPlayer(String playerName, double amount);

    EconomyResponse depositPlayer(OfflinePlayer player, double amount);

    EconomyResponse depositPlayer(String playerName, String worldName, double amount);

    EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount);

    EconomyResponse createBank(String name, String player);

    EconomyResponse createBank(String name, OfflinePlayer player);

    EconomyResponse deleteBank(String name);

    EconomyResponse bankBalance(String name);

    EconomyResponse bankHas(String name, double amount);

    EconomyResponse bankWithdraw(String name, double amount);

    EconomyResponse bankDeposit(String name, double amount);

    EconomyResponse isBankOwner(String name, String playerName);

    EconomyResponse isBankOwner(String name, OfflinePlayer player);

    EconomyResponse isBankMember(String name, String playerName);

    EconomyResponse isBankMember(String name, OfflinePlayer player);

    List<String> getBanks();

    boolean createPlayerAccount(String playerName);

    boolean createPlayerAccount(OfflinePlayer player);

    boolean createPlayerAccount(String playerName, String worldName);

    boolean createPlayerAccount(OfflinePlayer player, String worldName);

    /**
     * Le texte de prix que le plugin place dans ses interfaces et ses hologrammes.
     *
     * <p>Le code déjà compilé de ValoriaTycoon appelle {@code format(double)} (relevé dans
     * {@code SellUtil}, {@code GeneratorsGui}, {@code UpgradeGui}, {@code WandData$Wand} et
     * {@code ValoriaTycoon}). La méthode porte donc ce nom, et {@link #formatMoney(double)} est
     * le nom sous lequel l'implémentation l'expose : un fournisseur peut surcharger l'un ou
     * l'autre sans casser l'autre.</p>
     */
    default String format(double amount) {
        return formatMoney(amount);
    }
}
