package xyz.arcadiadevs.valariatools;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * ValoriaTools : le multi-outil à âmes commutantes, troisième brique du serveur (après le plugin de
 * générateurs et l'économie interne).
 *
 * <h2>Ce que c'est</h2>
 * <p>Un seul item dans la main qui se comporte comme une pioche, une hache, une canne à pêche ou une
 * épée <em>selon ce que le joueur regarde</em>, et dont chaque âme s'améliore séparément avec de
 * l'argent. Inspiré d'hGensPickaxe, mais réécrit ici : aucune dépendance à un plugin externe, y compris
 * pour la monnaie (voir {@link EconomyService}, qui s'adapte à n'importe quelle banque par réflexion).</p>
 *
 * <h2>Pourquoi un plugin séparé de ValoriaTycoon</h2>
 * <p>ValoriaTycoon est un paquet dont les classes sont <em>précompilées</em> (sources décompilées,
 * non recompilables en bloc) : on ne peut pas y ajouter un système de cette taille. Un jar séparé,
 * assemblé par le même build, garde la même discipline : dépendance souple vers le plugin de
 * générateurs, aucune dépendance codée en dur, et un plugin qui fonctionne même tout seul.</p>
 *
 * <h2>Accès statiques</h2>
 * <p>Les classes de ce paquet accèdent au service par {@link #get()} plutôt qu'en charriant une
 * référence de plugin : les listeners Bukkit sont instanciés par le serveur, pas par nous, et un
 * listener ne devrait jamais avoir à connaître l'ordre de construction du plugin. C'est aussi le
 * seul moyen de garder un {@link #reload()} propre (reconstruction des services sans recréer les
 * listeners).</p>
 */
public final class ValoriaTools extends JavaPlugin {

    private static ValoriaTools instance;

    private ToolsConfig toolsConfig;
    private ToolStore store;
    private EconomyService economy;
    private BlockMatcher matcher;
    private Abilities abilities;
    private final Map<ToolKind, Boolean> sellWarnings = new EnumMap<ToolKind, Boolean>(ToolKind.class);
    private ToolListener listener;
    private boolean guiRegistered;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.toolsConfig = new ToolsConfig(this);
        this.toolsConfig.load();
        this.store = new ToolStore(this);
        this.store.load();
        this.economy = new EconomyService(this);
        this.matcher = new BlockMatcher(this, this.toolsConfig);
        this.abilities = new Abilities(this, this.matcher, this.toolsConfig);

        this.listener = new ToolListener(this);
        registerListener(this.listener);
        // L'interface a son propre listener : la separer du comportement de l'outil evite qu'un bug
        // de GUI ne desactive le minage, et inversement.
        this.guiRegistered = registerListener(new ToolsGui.Handler());

        PluginCommand command = getCommand("valariatools");
        if (command != null) {
            ToolsCommand executor = new ToolsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("commande /valariatools absente du plugin.yml : "
                    + "impossible de donner d'outil ni d'ouvrir l'interface");
        }

        if (!this.toolsConfig.enabled()) {
            getLogger().info("désactivé par la configuration (enabled: false) : aucun événement traité.");
            return;
        }
        // L'economie peut s'enregistrer juste apres nous (ordre de chargement) : on relit apres un
        // tick plutot que de rester sur un « aucune economie trouvee » définitif.
        new BukkitRunnable() {

            @Override
            public void run() {
                economy.lookup();
                if (economy.available()) {
                    getLogger().info("économie connectée : " + economy.providerName());
                } else {
                    getLogger().warning("aucune économie détectée : les améliorations restent"
                            + " gratuites et la vente des drops est désactivée.");
                }
                getLogger().info("multi-outil prêt — " + describeKinds());
            }
        }.runTaskLater(this, 20L);
    }

    @Override
    public void onDisable() {
        if (this.store != null) {
            this.store.save();
        }
        instance = null;
    }

    /** Recharge la configuration, les paliers, et la reconnaissance des blocs, sans redémarrer. */
    public void reload() {
        if (this.store != null) {
            this.store.save();
        }
        super.reloadConfig();
        this.toolsConfig.load();
        this.store.load();
        this.matcher = new BlockMatcher(this, this.toolsConfig);
        this.abilities = new Abilities(this, this.matcher, this.toolsConfig);
        this.sellWarnings.clear();
        if (this.listener != null) {
            this.listener.refreshViews();
        }
        for (ToolsGui.View view : ToolsGui.views()) {
            Player viewer = getServer().getPlayer(view.owner());
            if (viewer != null) {
                ToolsGui.render(viewer);
            } else {
                ToolsGui.forget(view.owner());
            }
        }
        this.economy.lookup();
    }

    /**
     * Un seul enregistrement par listener, avec garde : un listener enregistré deux fois doublerait
     * tous les drops — le pire bug possible sur un plugin de minage, et invisible en test rapide.
     */
    private boolean registerListener(Listener candidate) {
        try {
            getServer().getPluginManager().registerEvents(candidate, this);
            return true;
        } catch (RuntimeException | LinkageError failed) {
            getLogger().severe("événements non enregistrés (" + failed.getClass().getSimpleName() + " : "
                    + failed.getMessage() + ") : le multi-outil est inerte, vérifie la version du serveur.");
            return false;
        }
    }

    /** Vrai si l'interface réagit aux clics (le /tools ouvre une vue inutile sinon). */
    public boolean guiLive() {
        return this.guiRegistered;
    }

    // ------------------------------------------------------------------ acces

    public static ValoriaTools get() {
        return instance;
    }

    public ToolsConfig toolsConfig() {
        return this.toolsConfig;
    }

    public ToolStore store() {
        return this.store;
    }

    public EconomyService economy() {
        return this.economy;
    }

    public BlockMatcher matcher() {
        return this.matcher;
    }

    public Abilities abilities() {
        return this.abilities;
    }

    public boolean active() {
        return instance != null && this.toolsConfig != null && this.toolsConfig.enabled();
    }

    /** L'interface et les commandes ne doivent rien casser si le plugin est coupé. */
    public String describeKinds() {
        StringBuilder out = new StringBuilder();
        for (ToolKind kind : ToolKind.values()) {
            ToolsConfig.KindConfig config = this.toolsConfig.kind(kind);
            if (config == null) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(kind.label()).append(" (").append(this.toolsConfig.maxTier(config))
                    .append(" paliers, ").append(this.toolsConfig.abilities(config).size())
                    .append(" capacités)");
        }
        return out.length() == 0 ? "aucune âme configurée" : out.toString();
    }

    /**
     * Prix de revente d'un matériau pour une âme, en signalant <b>une seule fois</b> l'absence de
     * grille tarifaire : un admin qui active SELL_ON_BREAK sans prix doit être prévenu, pas spammé.
     */
    public double sellPrice(ToolKind kind, org.bukkit.Material material) {
        ToolsConfig.KindConfig config = this.toolsConfig.kind(kind);
        if (config == null) {
            return -1.0D;
        }
        double price = this.toolsConfig.sellPriceOf(config, material);
        if (price < 0.0D && this.sellWarnings.get(kind) == null) {
            this.sellWarnings.put(kind, Boolean.TRUE);
            getLogger().warning("sell-on-break actif pour " + kind.label() + " sans grille de prix"
                    + " (outils." + ToolStore.name(kind) + ".sell.prices) : rien n'est vendu pour cette âme."
                    + " Renseigne les prix, ou retire la capacité SELL_ON_BREAK.");
        }
        return price;
    }

    /** Nom du plugin, pour les messages : évite de recopier une chaîne en dur. */
    public static String label() {
        ValoriaTools plugin = instance;
        if (plugin == null) {
            return "ValoriaTools";
        }
        PluginDescriptionFile description = plugin.getDescription();
        return description == null ? "ValoriaTools" : description.getFullName();
    }

    /** Le fichier de paliers est écrit à chaud : on le sauvegarde aussi périodiquement. */
    public void saveSoon() {
        if (this.store == null) {
            return;
        }
        new BukkitRunnable() {

            @Override
            public void run() {
                store.save();
            }
        }.runTaskLater(this, 40L);
    }
}
