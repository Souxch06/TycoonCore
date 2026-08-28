package xyz.arcadiadevs.valoriateconomy;

/**
 * Résultat d'une opération sur un solde : {@link #withdrawPlayer} et {@link #depositPlayer} en
 * renvoient un, et c'est le seul moyen pour l'appelant de savoir si le mouvement a réussi.
 *
 * <h2>Pourquoi ce n'est pas un booléen</h2>
 * <p>Un achat de générateur ou une vente doit pouvoir expliquer <em>pourquoi</em> il échoue (« solde
 * insuffisant ») et indiquer le solde <em>après</em> l'opération, pour rafraîchir l'affichage dans la
 * foulée. Le résultat porte donc les deux.</p>
 *
 * <h2>Surface imposée</h2>
 * <p>Le champ {@link #errorMessage} et la méthode {@link #transactionSuccess()} sont appelés en dur
 * par le bytecode livré ({@code guis/UpgradeGui}, {@code guis/GeneratorsGui}) : ils ne doivent pas
 * changer de nom, de type ou de visibilité. Les objets sont immuables et produits par le seul
 * constructeur public, ce qui rend impossible un {@code type} {@code null}.</p>
 */
public final class EconomyResponse {

    /** Nature du résultat. Un {@link ResponseType#NOT_IMPLEMENTED} n'est jamais une erreur. */
    public enum ResponseType {

        /** L'opération a été appliquée. */
        SUCCESS("Succès"),
        /** L'opération a été refusée (solde insuffisant, compte inconnu, montant invalide). */
        FAILURE("Echec"),
        /** Le fournisseur ne gère pas cette opération : l'appelant doit renoncer poliment. */
        NOT_IMPLEMENTED("Non implemente"),
        /** Échec partiel appliqué puis annulé (réservé aux fournisseurs à transaction). */
        FAILURE_PARTIAL("Echec partiel"),
        /** Le fournisseur a répondu mais sans garantir l'état : ne jamais considérer gagné. */
        UNSUPPORTED_OPERATION("Operation non supportee");

        private final String label;

        ResponseType(String label) {
            this.label = label;
        }

        /** Libellé lisible, pour un message de chat ou un log d'administrateur. */
        public String label() {
            return this.label;
        }

        /** Vrai pour {@link #SUCCESS} uniquement : {@code NOT_IMPLEMENTED} compte comme refus. */
        public boolean ok() {
            return this == SUCCESS;
        }
    }

    /** Réponse toute prête pour les opérations non gérées : évite d'allouer un objet par appel. */
    public static final EconomyResponse NOT_IMPLEMENTED = new EconomyResponse(0.0D, 0.0D,
            ResponseType.NOT_IMPLEMENTED, "Cette banque ne gere pas les comptes de type banque.");

    /** Le compte est introuvable : réponse réutilisable également. */
    public static final EconomyResponse NO_ACCOUNT = new EconomyResponse(0.0D, 0.0D,
            ResponseType.FAILURE, "Compte inconnu.");

    /** Montant de la transaction demandée. */
    public final double amount;
    /** Solde du compte après l'opération (solde avant, si l'opération a échoué). */
    public final double balance;
    /** Nature du résultat. */
    public final ResponseType type;
    /** Explication lisible, {@code null} quand tout s'est bien passé. */
    public final String errorMessage;

    public EconomyResponse(double amount, double balance, ResponseType type, String errorMessage) {
        this.amount = amount;
        this.balance = balance;
        this.type = type == null ? ResponseType.FAILURE : type;
        this.errorMessage = errorMessage;
    }

    /** {@code true} seulement pour {@link ResponseType#SUCCESS}. */
    public boolean transactionSuccess() {
        return this.type.ok();
    }

    /** Le solde annoncé est-il utilisable (fini, jamais négatif) : un fournisseur défaillant ne doit pas pouvoir le corrompre. */
    public boolean hasUsableBalance() {
        return transactionSuccess() && Double.isFinite(this.balance) && this.balance >= 0.0D;
    }

    /** Réponse de succès, pour les fournisseurs qui n'ont rien à expliquer. */
    public static EconomyResponse success(double amount, double balance) {
        return new EconomyResponse(amount, balance, ResponseType.SUCCESS, null);
    }

    /** Réponse d'échec avec motif. */
    public static EconomyResponse failure(double balance, String reason) {
        return new EconomyResponse(0.0D, balance, ResponseType.FAILURE, reason);
    }

    @Override
    public String toString() {
        return "EconomyResponse{type=" + this.type + ", amount=" + this.amount + ", balance=" + this.balance
                + (this.errorMessage == null ? "" : ", error=" + this.errorMessage) + "}";
    }
}
