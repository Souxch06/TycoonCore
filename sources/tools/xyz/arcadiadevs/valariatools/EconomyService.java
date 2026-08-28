package xyz.arcadiadevs.valariatools;

import java.lang.reflect.Method;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * La monnaie du serveur, vue <b>sans dépendre d'aucune API</b>.
 *
 * <h2>Pourquoi par réflexion</h2>
 * <p>ValoriaTools est un jar autonome : s'il importait l'interface d'économie d'un autre plugin, il
 * ne démarrerait plus dès que ce plugin est absent (une classe manquante se règle au premier
 * chargement de méthode, pas au chargement du plugin). Le plugin cherche donc dans le
 * <code>ServicesManager</code> <em>n'importe quel</em> objet qui <em>sait</em> faire le petit
 * minimum — {@code getBalance}, {@code has}, {@code withdrawPlayer}, {@code depositPlayer},
 * {@code format} — et s'adapte à lui. Que la monnaie vienne de ValoriaEconomy, de Vault ou de
 * n'importe quel fournisseur futur, le multi-outil fonctionne ; et s'il n'y a aucune banque, le
 * plugin reste utilisable, juste sans achats.</p>
 *
 * <h2>Un seul sens de vérité</h2>
 * <p>Les résultats sont interprétés ainsi : une méthode qui renvoie un objet avec
 * <code>transactionSuccess()</code> est lue via cette méthode ; sinon un <code>boolean</code> est un
 * succès, un <code>double</code> est le nouveau solde et {@code < 0} signifie refus. Cela couvre à la
 * fois l'API Vault (objet réponse) et les fournisseurs qui renvoient directement un solde.</p>
 */
public final class EconomyService {

    /** Refus de transaction, avec le motif lisible à afficher au joueur. */
    public static final class Outcome {

        private final boolean success;
        private final String reason;

        Outcome(boolean success, String reason) {
            this.success = success;
            this.reason = reason;
        }

        public boolean success() {
            return this.success;
        }

        public String reason() {
            return this.reason;
        }

        @Override
        public String toString() {
            return this.success ? "succès" : "refus (" + this.reason + ")";
        }
    }

    private final JavaPlugin plugin;
    private Object provider;
    private Method getBalance;
    private Method has;
    private Method withdraw;
    private Method deposit;
    private Method format;
    private Method responseSuccess;
    /** `Method` (getter) ou `Field` (champ public) : les fournisseurs ne se ressemblent pas. */
    private Object responseError;
    private long lastLookup;
    private boolean warned;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Vrai si une banque est joignable. Relue si la recherche date de plus de 30 s. */
    public boolean available() {
        if (this.provider != null) {
            return true;
        }
        if (System.currentTimeMillis() - this.lastLookup < 30_000L) {
            return false;
        }
        lookup();
        return this.provider != null;
    }

    /** Recherche (ou re-recherche) le fournisseur. Appelé aussi après <code>/tools reload</code>. */
    public void lookup() {
        this.lastLookup = System.currentTimeMillis();
        this.provider = null;
        this.getBalance = null;
        this.has = null;
        this.withdraw = null;
        this.deposit = null;
        this.format = null;
        this.responseSuccess = null;
        this.responseError = null;
        ServicesManager services = this.plugin.getServer().getServicesManager();
        if (services == null) {
            return;
        }
        // `getRegistrations(Classe)` renvoie les enregistrements du SERVICE nommé par cette classe :
        // `getRegistrations(RegisteredServiceProvider.class)` ne cherche donc AUCUN fournisseur
        // d'economie (et le compilateur ne bronche pas). On enumere les services connus, puis leurs
        // fournisseurs — c'est le seul chemin qui trouve ValoriaEconomy comme Vault comme tout autre.
        Object best = null;
        for (Class<?> service : services.getKnownServices()) {
            for (RegisteredServiceProvider<?> registration : services.getRegistrations(service)) {
                Object candidate = registration.getProvider();
                if (candidate == null || candidate == this || candidate instanceof EconomyService) {
                    continue;
                }
                Class<?> owner = candidate.getClass();
                Method balance = method(owner, "getBalance", OfflinePlayer.class);
                if (balance == null || balance.getReturnType() != double.class) {
                    continue;
                }
                Method take = method(owner, "withdrawPlayer", OfflinePlayer.class, double.class);
                if (take == null) {
                    continue;
                }
                this.getBalance = balance;
                this.withdraw = take;
                this.has = method(owner, "has", OfflinePlayer.class, double.class);
                this.deposit = method(owner, "depositPlayer", OfflinePlayer.class, double.class);
                this.format = findFormat(owner);
                Class<?> response = take.getReturnType();
                if (response != void.class && response != double.class && response != boolean.class) {
                    this.responseSuccess = method(response, "transactionSuccess");
                    this.responseError = field(response, "errorMessage");
                }
                best = candidate;
                break;
            }
            if (best != null) {
                break;
            }
        }
        if (best == null && !this.warned) {
            this.warned = true;
            this.plugin.getLogger().warning("aucune economie detectee : les ameliorations du multi-outil"
                    + " seront gratuites et la vente des drops inactivee. Installe ValoriaEconomy pour"
                    + " la monnaie complete (le plugin reste fonctionnel sans elle).");
        }
        this.provider = best;
    }

    /** Le solde courant, ou 0 si aucune banque (un affichage à 0 vaut mieux qu'une exception). */
    public double balance(OfflinePlayer player) {
        if (!available() || player == null) {
            return 0.0D;
        }
        try {
            double value = ((Number) this.getBalance.invoke(this.provider, player)).doubleValue();
            return Double.isFinite(value) && value >= 0.0D ? value : 0.0D;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            fail("lecture du solde", failed);
            return 0.0D;
        }
    }

    public boolean canAfford(OfflinePlayer player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (!available()) {
            // sans economie, rien n'est facture : le serveur a choisi de ne pas mettre de banque
            return true;
        }
        if (this.has != null) {
            try {
                Object answer = this.has.invoke(this.provider, player, Double.valueOf(amount));
                if (answer instanceof Boolean) {
                    return ((Boolean) answer).booleanValue();
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
                fail("verification du solde", failed);
            }
        }
        return balance(player) + 1.0E-6D >= amount;
    }

    /** Débite. Sans banque, le débit est un succès (mode gratuit). */
    public Outcome withdraw(Player player, double amount) {
        if (amount <= 0.0D) {
            return new Outcome(true, null);
        }
        if (!available()) {
            return new Outcome(true, null);
        }
        try {
            Object answer = this.withdraw.invoke(this.provider, player, Double.valueOf(amount));
            return interpret(answer, "Paiement refuse");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            fail("debit", failed);
            return new Outcome(false, "Le fournisseur d'économie a échoué (" + failed.getClass().getSimpleName() + ")");
        }
    }

    /** Crédite — utilisé pour rembourser un débit appliqué puis refusé. */
    public Outcome deposit(OfflinePlayer player, double amount) {
        if (amount <= 0.0D) {
            return new Outcome(true, null);
        }
        if (!available() || this.deposit == null) {
            return new Outcome(false, "aucun moyen de créditer le compte");
        }
        try {
            Object answer = this.deposit.invoke(this.provider, player, Double.valueOf(amount));
            return interpret(answer, "Remboursement refuse");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            fail("credit", failed);
            return new Outcome(false, "Le fournisseur d'économie a échoué (" + failed.getClass().getSimpleName() + ")");
        }
    }

    /** Le texte de prix du fournisseur, avec repli maison s'il ne fournit pas de formatage. */
    public String format(double amount) {
        if (available() && this.format != null) {
            try {
                Object text = this.format.invoke(this.provider, Double.valueOf(amount));
                if (text instanceof String && !((String) text).isEmpty()) {
                    return (String) text;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
                // repli ci-dessous
            }
        }
        return String.format(java.util.Locale.US, "$%,.2f", Double.valueOf(amount));
    }

    /** Fournisseur trouvé, ou {@code null} — pour le <code>/tools stats</code>. */
    public String providerName() {
        if (!available()) {
            return "aucun";
        }
        return this.provider.getClass().getName();
    }

    // ------------------------------------------------------------------ interpretation

    private Outcome interpret(Object answer, String defaultReason) {
        if (answer == null) {
            return new Outcome(true, null);
        }
        if (answer instanceof Boolean) {
            return ((Boolean) answer).booleanValue() ? new Outcome(true, null) : new Outcome(false, defaultReason);
        }
        if (answer instanceof Number) {
            double value = ((Number) answer).doubleValue();
            if (!Double.isFinite(value) || value < 0.0D) {
                return new Outcome(false, defaultReason);
            }
            return new Outcome(true, null);
        }
        if (this.responseSuccess != null) {
            try {
                Object ok = this.responseSuccess.invoke(answer);
                boolean success = ok instanceof Boolean && ((Boolean) ok).booleanValue();
                String reason = defaultReason;
                if (!success && this.responseError != null) {
                    Object message = readMember(this.responseError, answer);
                    if (message instanceof String && !((String) message).isEmpty()) {
                        reason = (String) message;
                    }
                }
                return new Outcome(success, reason);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
                return new Outcome(false, "réponse d'économie illisible");
            }
        }
        // reponse inconnue : on considere l'operation refusee, jamais acceptee a l'aveugle
        return new Outcome(false, defaultReason + " (reponse inattendue : " + answer.getClass().getName() + ")");
    }

    private static Object readMember(Object accessor, Object target) {
        try {
            if (accessor instanceof Method) {
                return ((Method) accessor).invoke(target);
            }
            if (accessor instanceof java.lang.reflect.Field) {
                return ((java.lang.reflect.Field) accessor).get(target);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            return null;
        }
        return null;
    }

    // ------------------------------------------------------------------ reflexion

    private static Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            Method found = owner.getMethod(name, params);
            found.setAccessible(true);
            return found;
        } catch (NoSuchMethodException absent) {
            return null;
        } catch (RuntimeException | LinkageError broken) {
            return null;
        }
    }

    /** Un champ public (l'API d'economie expose {@code errorMessage} ainsi) ou son accesseur. */
    private static Object field(Class<?> owner, String name) {
        Method getter = method(owner, "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
        if (getter != null) {
            return getter;
        }
        try {
            java.lang.reflect.Field found = owner.getField(name);
            if (found.getType() != String.class) {
                return null;
            }
            found.setAccessible(true);
            return found;
        } catch (NoSuchFieldException absent) {
            return null;
        } catch (RuntimeException | LinkageError broken) {
            return null;
        }
    }

    private static Method findFormat(Class<?> owner) {
        Method formatMoney = method(owner, "formatMoney", double.class);
        return formatMoney != null ? formatMoney : method(owner, "format", double.class);
    }

    private void fail(String what, Throwable cause) {
        this.plugin.getLogger().warning("[multi-outil] " + what + " impossible : "
                + cause.getClass().getSimpleName() + " " + cause.getMessage());
        // L'absence d'economie ne doit jamais casser un achat deja valide : on le signale une fois.
        this.provider = null;
    }
}
