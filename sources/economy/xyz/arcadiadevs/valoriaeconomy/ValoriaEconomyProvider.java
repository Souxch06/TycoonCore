package xyz.arcadiadevs.valoriaeconomy;

import java.util.ArrayList;
import xyz.arcadiadevs.valoriateconomy.Economy;
import xyz.arcadiadevs.valoriateconomy.EconomyResponse;

/**
 * Fournisseur d'économie du serveur : il adapte {@link Balances} vers l'interface
 * {@link Economy} que consultent ValoriaTycoon et les autres plugins du dépôt.
 *
 * <p><b>Fichier généré</b> par {@code scripts/generate-economy-api.py} — ne pas éditer à la
 * main. {@code scripts/verify-economy-api.py} refuse tout écart entre l'interface, ce fichier et
 * le snapshot {@code docs/economy-api.txt} : une méthode d'interface non implémentée est une
 * erreur de compilation, mais une signature qui ne correspond plus à l'interface est un
 * {@code AbstractMethodError} silencieux en jeu.</p>
 *
 * <p>Les banques renvoient {@link EconomyResponse#NOT_IMPLEMENTED} (jamais d'exception) :</p>
 * <ul>
 *   <li>un plugin qui en a besoin reste libre d'utiliser un autre fournisseur ;</li>
 *   <li>le serveur ne peut pas être mis à genoux par une commande de banque mal formée.</li>
 * </ul>
 */
public final class ValoriaEconomyProvider implements Economy {

    private static final String NAME = "ValoriaEconomy";

    private final Balances balances;

    public ValoriaEconomyProvider(Balances balances) {
        this.balances = balances;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String formatMoney(double amount) {
        return this.balances.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return this.balances.currencyPlural();
    }

    @Override
    public String currencyNameSingular() {
        return this.balances.currencySingular();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return this.balances.exists(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return this.balances.exists(player);
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return this.balances.exists(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return this.balances.exists(player);
    }

    @Override
    public double getBalance(String playerName) {
        return this.balances.balance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return this.balances.balance(player);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return this.balances.balance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return this.balances.balance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return this.balances.balance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return this.balances.balance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return this.balances.balance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return this.balances.balance(player) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        double before = this.balances.balance(playerName);
        double after = this.balances.withdraw(playerName, amount);
        if (after < 0.0D || !Double.isFinite(after)) {
            return new EconomyResponse(0.0D, before, EconomyResponse.ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double before = this.balances.balance(player);
        double after = this.balances.withdraw(player, amount);
        if (after < 0.0D || !Double.isFinite(after)) {
            return new EconomyResponse(0.0D, before, EconomyResponse.ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        double before = this.balances.balance(playerName);
        double after = this.balances.withdraw(playerName, amount);
        if (after < 0.0D || !Double.isFinite(after)) {
            return new EconomyResponse(0.0D, before, EconomyResponse.ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        double before = this.balances.balance(player);
        double after = this.balances.withdraw(player, amount);
        if (after < 0.0D || !Double.isFinite(after)) {
            return new EconomyResponse(0.0D, before, EconomyResponse.ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        double after = this.balances.deposit(playerName, amount);
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double after = this.balances.deposit(player, amount);
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        double after = this.balances.deposit(playerName, amount);
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        double after = this.balances.deposit(player, amount);
        return new EconomyResponse(amount, after, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return EconomyResponse.NOT_IMPLEMENTED;
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<String>();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return this.balances.ensureAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return this.balances.ensureAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return this.balances.ensureAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return this.balances.ensureAccount(player);
    }

    // format(double) herite du default de Economy -> formatMoney(double) ci-dessus.
}
