package xyz.arcadiadevs.valoriaeconomy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ValoriaEconomy : l'économie du serveur, sans plugin tiers.
 *
 * <p>Le plugin se charge de trois choses, et rien d'autre : garder les soldes dans un fichier
 * (atomes, voir {@link Balances}), exposer ces soldes à <b>Vault</b> comme fournisseur
 * {@code Economy}, et fournir les commandes joueurs ({@code /bal}, {@code /pay}, {@code /baltop}) et
 * administrateurs ({@code /eco}).</p>
 *
 * <h2>Pourquoi un plugin séparé de ValoriaTycoon</h2>
 * <p>ValoriaTycoon résout son fournisseur d'économie dans son {@code onEnable} (il exige aussi un plugin
 * nommé {@code Vault}, vérifié dans son bytecode). Un fournisseur enregistré <em>par</em> ValoriaTycoon
 * ne peut donc pas être visible <em>par</em> ValoriaTycoon au moment où il le cherche. {@code load:
 * STARTUP} dans {@code plugin.yml} règle l'ordre : ce plugin est activé avant les plugins en
 * {@code POSTWORLD}, donc le service est déjà enregistré quand ValoriaTycoon s'éveille — et les autres
 * plugins du serveur (boutiques, donneurs, téléports payants) voient la même monnaie, sans rien changer
 * chez eux.</p>
 */
public final class ValoriaEconomy extends JavaPlugin {

    private Balances balances;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        balances = new Balances(this);
        balances.configure();
        balances.load();

        ServicesManager services = getServer().getServicesManager();
        if (services.isRegistered(Economy.class)) {
            getLogger().warning("un fournisseur d'économie est déjà enregistré ("
                    + services.getRegistration(Economy.class).getProvider().getName()
                    + ") : le mien n'est pas enregistré, retire l'autre plugin (ex. EssentialsX) pour basculer.");
        } else {
            services.register(Economy.class, new VaultEconomy(balances), this, ServicePriority.Normal);
            getLogger().info("fournisseur d'économie enregistré (" + balances.accountCount() + " compte(s)).");
        }

        MoneyCommand executor = new MoneyCommand(this, balances);
        PluginDescriptionFile description = getDescription();
        if (description.getCommands() != null) {
            for (String label : description.getCommands().keySet()) {
                if (getCommand(label) == null) {
                    continue;
                }
                getCommand(label).setExecutor(executor);
                getCommand(label).setTabCompleter(executor);
            }
        }
    }

    @Override
    public void onDisable() {
        if (balances != null) {
            balances.save();
        }
    }

    /** Recharge la configuration sans redémarrer (utilisé par {@code /eco reload}). */
    public void reload() {
        reloadConfig();
        balances.configure();
    }

    public Balances balances() {
        return balances;
    }
}
