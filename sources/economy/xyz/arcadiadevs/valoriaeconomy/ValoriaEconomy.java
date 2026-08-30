package xyz.arcadiadevs.valoriaeconomy;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.arcadiadevs.valoriateconomy.Economy;

/**
 * ValoriaEconomy : l'économie du serveur, sans plugin tiers à installer.
 *
 * <p>Le plugin se charge de trois choses, et rien d'autre : garder les soldes dans un fichier
 * (écriture atomique, voir {@link Balances}), exposer ces soldes sous l'interface {@link Economy}
 * du dépôt, et fournir les commandes joueurs ({@code /bal}, {@code /pay}, {@code /baltop}) et
 * administratrices ({@code /eco}).</p>
 *
 * <h2>Pourquoi un plugin séparé de ValoriaTycoon</h2>
 * <p>ValoriaTycoon résout son fournisseur d'économie dans son {@code onEnable}. Un fournisseur
 * enregistré <em>par</em> ValoriaTycoon ne serait pas visible <em>par</em> ValoriaTycoon au moment
 * où il le cherche. {@code load: STARTUP} dans le {@code plugin.yml} règle l'ordre : ce plugin est
 * activé avant tous les plugins en {@code POSTWORLD}, donc le service est déjà enregistré quand
 * ValoriaTycoon s'éveille.</p>
 *
 * <h2>Pourquoi notre interface et pas celle de Vault, et où elle vit</h2>
 * <p>La version d'origine exigeait un plugin « Vault » (le pont d'API) en plus de la banque : deux
 * briques téléchargées, dont une gelée depuis 2020 et inconnue des serveurs à numérotation
 * calendaire (26.x). Ici l'interface vit dans le dépôt
 * ({@code sources/api/xyz/arcadiadevs/valoriateconomy/Economy.java}, générée depuis
 * {@code docs/economy-api.txt}) et est embarquée dans <b>ce jar</b> — côté fournisseur. Le serveur
 * n'a besoin que des jar construits par ce build.</p>
 *
 * <p><b>L'interface vit dans le jar du fournisseur, pas dans celui du consommateur.</b> Ce plugin
 * est chargé en {@code STARTUP}, avant que le classloader de ValoriaTycoon n'existe : si l'interface
 * n'était que dans le jar de ValoriaTycoon, le chargement de CE plugin échouerait dès sa première
 * classe ({@code Could not load plugin} — exactement l'Economy rouge du serveur du 2026-08-30).
 * Embarquée ici, elle est chargée par CE classloader ; ValoriaTycoon, qui déclare
 * {@code softdepend: [ValoriaEconomy]}, la résout par délégation vers ce jar — donc la MÊME classe
 * que celle du service enregistré, jamais deux objets {@code Class}.</p>
 */
public final class ValoriaEconomy extends JavaPlugin {

    private Balances balances;
    private Economy provider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.balances = new Balances(this);
        this.balances.configure();
        this.balances.load();
        this.provider = new ValoriaEconomyProvider(this.balances);

        ServicesManager services = getServer().getServicesManager();
        // `isRegistered` n'existe pas dans l'API Bukkit : la methode s'appelle `isProvidedFor`
        // (erreur javac relevee par le build #33154898463). getRegistration peut renvoyer null,
        // donc on le stocke avant d'en tirer le fournisseur.
        RegisteredServiceProvider<Economy> existing = services.getRegistration(Economy.class);
        if (existing != null) {
            Economy other = existing.getProvider();
            getLogger().warning("un fournisseur d'économie est déjà enregistré ("
                    + (other == null ? "?" : other.getName())
                    + ") : le mien n'est pas enregistré. Retirez l'autre plugin pour basculer sur ValoriaEconomy.");
        } else {
            services.register(Economy.class, this.provider, this, ServicePriority.Normal);
            getLogger().info("fournisseur d'économie enregistré (" + this.balances.accountCount() + " compte(s)).");
        }

        MoneyCommand executor = new MoneyCommand(this, this.balances);
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
        if (this.balances != null) {
            this.balances.save();
        }
    }

    /** Recharge la configuration sans redémarrer (utilisé par {@code /eco reload}). */
    public void reload() {
        reloadConfig();
        this.balances.configure();
    }

    public Balances balances() {
        return this.balances;
    }

    /** Le fournisseur exposé, pour un autre plugin du dépôt qui préférerait l'appeler directement. */
    public Economy provider() {
        return this.provider;
    }
}
