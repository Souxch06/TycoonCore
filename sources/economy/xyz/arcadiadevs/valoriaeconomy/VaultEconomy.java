package xyz.arcadiadevs.valoriaeconomy;

import java.util.ArrayList;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;

/**
 * Fournisseur d'économie exposé à Vault. FICHIER GÉNÉRÉ — ne pas éditer à la main.
 *
 * <p>43 méthodes, émises depuis docs/vault-economy-api.txt par scripts/generate-vault-economy.py.
 * Si l'API Vault change (ou si un membre est ajouté ici à la main), lance le générateur puis
 * scripts/verify-economy-api.py : une méthode d'interface non implémentée est une erreur de
 * compilation, mais une signature qui ne correspond plus à l'interface est un AbstractMethodError
 * silencieux en jeu pour les autres plugins.</p>
 *
 * <p>Les banques renvoient ResponseType.NOT_IMPLEMENTED (jamais d'exception) : un plugin qui en a
 * besoin doit rester libre d'utiliser un autre fournisseur.</p>
 */
public final class VaultEconomy implements Economy {

    private static final String NAME = "ValoriaEconomy";

    private final Balances balances;

    public VaultEconomy(Balances balances) {
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
    public String format(final double amount) {
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
    public boolean hasAccount(final String playerName) {
        return this.balances.exists(playerName);
    }

    @Override
    public boolean hasAccount(final OfflinePlayer player) {
        return this.balances.exists(player);
    }

    @Override
    public boolean hasAccount(final String playerName, final String worldName) {
        return this.balances.exists(playerName);
    }

    @Override
    public boolean hasAccount(final OfflinePlayer player, final String worldName) {
        return this.balances.exists(player);
    }

    @Override
    public double getBalance(final String playerName) {
        return this.balances.balance(playerName);
    }

    @Override
    public double getBalance(final OfflinePlayer player) {
        return this.balances.balance(player);
    }

    @Override
    public double getBalance(final String playerName, final String world) {
        return this.balances.balance(playerName);
    }

    @Override
    public double getBalance(final OfflinePlayer player, final String world) {
        return this.balances.balance(player);
    }

    @Override
    public boolean has(final String playerName, final double amount) {
        return this.balances.balance(playerName) >= amount;
    }

    @Override
    public boolean has(final OfflinePlayer player, final double amount) {
        return this.balances.balance(player) >= amount;
    }

    @Override
    public boolean has(final String playerName, final String worldName, final double amount) {
        return this.balances.balance(playerName) >= amount;
    }

    @Override
    public boolean has(final OfflinePlayer player, final String worldName, final double amount) {
        return this.balances.balance(player) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(final String playerName, final double amount) {
        double before = this.balances.balance(playerName);
        double after = this.balances.withdraw(playerName, amount);
        if (after < 0.0D) {
            return new EconomyResponse(0.0D, before, ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(final OfflinePlayer player, final double amount) {
        double before = this.balances.balance(player);
        double after = this.balances.withdraw(player, amount);
        if (after < 0.0D) {
            return new EconomyResponse(0.0D, before, ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(final String playerName, final String worldName, final double amount) {
        double before = this.balances.balance(playerName);
        double after = this.balances.withdraw(playerName, amount);
        if (after < 0.0D) {
            return new EconomyResponse(0.0D, before, ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(final OfflinePlayer player, final String worldName, final double amount) {
        double before = this.balances.balance(player);
        double after = this.balances.withdraw(player, amount);
        if (after < 0.0D) {
            return new EconomyResponse(0.0D, before, ResponseType.FAILURE, "Solde insuffisant");
        }
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(final String playerName, final double amount) {
        double after = this.balances.deposit(playerName, amount);
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(final OfflinePlayer player, final double amount) {
        double after = this.balances.deposit(player, amount);
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(final String playerName, final String worldName, final double amount) {
        double after = this.balances.deposit(playerName, amount);
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(final OfflinePlayer player, final String worldName, final double amount) {
        double after = this.balances.deposit(player, amount);
        return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse createBank(final String name, final String player) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse createBank(final String name, final OfflinePlayer player) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse deleteBank(final String name) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse bankBalance(final String name) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse bankHas(final String name, final double amount) {
        return new EconomyResponse(0.0D, amount, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse bankWithdraw(final String name, final double amount) {
        return new EconomyResponse(0.0D, amount, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse bankDeposit(final String name, final double amount) {
        return new EconomyResponse(0.0D, amount, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse isBankOwner(final String name, final String playerName) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse isBankOwner(final String name, final OfflinePlayer player) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse isBankMember(final String name, final String playerName) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public EconomyResponse isBankMember(final String name, final OfflinePlayer player) {
        return new EconomyResponse(0.0D, 0.0D, ResponseType.NOT_IMPLEMENTED,
                "ValoriaEconomy n'implémente pas les banques ; garde un plugin de banques à côté si un",
                "plugin tiers en dépend.");
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<String>();
    }

    @Override
    public boolean createPlayerAccount(final String playerName) {
        return this.balances.ensureAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(final OfflinePlayer player) {
        return this.balances.ensureAccount(player);
    }

    @Override
    public boolean createPlayerAccount(final String playerName, final String worldName) {
        return this.balances.ensureAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(final OfflinePlayer player, final String worldName) {
        return this.balances.ensureAccount(player);
    }
}
