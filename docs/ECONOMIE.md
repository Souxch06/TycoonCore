# Économie interne (ValoriaEconomy)

## Pourquoi un second plugin, et pas le code dans ValoriaTycoon

`ValoriaTycoon` résout son fournisseur d'argent dans son `onEnable` : il exige un plugin nommé `Vault`
(son bytecode contient la chaîne `Vault not found`) et lit le service `Economy` enregistré dans le
`ServicesManager` de Bukkit. Un fournisseur enregistré **par** ValoriaTycoon ne peut donc pas être visible
**par** ValoriaTycoon au moment où il le cherche. La seule solution propre est un plugin activé **avant**
lui : `load: STARTUP` dans `resources-economy/plugin.yml` (ValoriaTycoon est en `POSTWORLD`).

Conséquences heureuses : aucun fichier de ValoriaTycoon n'est touché, et tous les autres plugins du
serveur (boutiques, donneurs, téléports payants) voient le même argent sans configuration.

## Ce qui remplace quoi

| avant | après |
| --- | --- |
| EssentialsX (coffre + `/money`, `/pay`) | `ValoriaEconomy` (`/bal`, `/pay`, `/baltop`, `/eco`) |
| `plugins/Essentials/userdata/*.yml` | `plugins/ValoriaEconomy/economy.yml`, un seul fichier |
| config de l'argent éparse | `plugins/ValoriaEconomy/config.yml` (`starting-balance`, `currency.*`) |
| `Vault`/VaultUnlocked | **inchangé** : il reste obligatoire (c'est le pont d'API qu'exige ValoriaTycoon) |

## Installer

1. Déposer **les deux** jars dans `plugins/` : `ValoriaTycoon-v1.6.3.jar` et `ValoriaEconomy-v1.6.3.jar`.
2. Retirer `EssentialsX.jar` (ou le garder pour ses autres commandes, mais alors **une seule** des deux
   économies doit s'enregistrer : ValoriaEconomy refuse de prendre la place d'un fournisseur déjà
   enregistré et le dit dans le log).
3. Redémarrer. Le log attendu :
   `[ValoriaEconomy] fournisseur d'économie enregistré (N compte(s)).`
4. Premier lancement : chaque joueur reçoit `starting-balance` (500 par défaut, `0` pour désactiver).
   Pour importer les soldes existants d'EssentialsX, voir la note plus bas.

## Commandes et permissions

| commande | effet | permission |
| --- | --- | --- |
| `/bal [joueur]` (alias `/balance`, `/argent`) | consulter un solde | `valoriaeconomy.bal` (tous) |
| `/pay <joueur> <montant>` | envoyer de l'argent | `valoriaeconomy.pay` (tous) |
| `/baltop` | 10 plus gros soldes | `valoriaeconomy.top` (tous) |
| `/eco give\|take\|set <joueur> <montant>` | ajuster un solde | `valoriaeconomy.eco` (op) |
| `/eco stats`, `/eco reload` | nombre de comptes, recharger la config | `valoriaeconomy.eco` |

## Choix de conception à connaître

- **Un fichier, écriture atomique** : `economy.yml.tmp` puis `ATOMIC_MOVE`. Un crash en pleine sauvegarde
  laisse l'ancien fichier intact, jamais un YAML tronqué qui remettrait tout le monde à zéro.
- **Sauvegarde à chaque mutation** : pas de fenêtre de perte entre deux sauvegardes périodiques.
- **UUID seul compte** ; les pseudos ne servent qu'à l'affichage (changement de pseudo sans perte, et pas
  d'usurpation par pseudo approchant).
- **Jamais de solde négatif** : `withdraw` renvoie -1 et le demandeur affiche « solde insuffisant ».
- **`/pay` retire d'abord, dépose ensuite, annule le retrait si le dépôt échoue** : l'argent n'est jamais
  créée ni détruite au milieu d'une transaction.
- **Banques non implémentées**, réponse `NOT_IMPLEMENTED` (jamais d'exception) : un plugin qui en a besoin
  reste libre d'utiliser un autre fournisseur.
- **43 méthodes d'interface générées** : `VaultEconomy.java` est émis par
  `scripts/generate-vault-economy.py` depuis `docs/vault-economy-api.txt` (snapshot de l'API Vault), et
  `scripts/verify-economy-api.py` refuse tout écart. Une méthode oubliée = erreur de compilation ; une
  signature décalée = `AbstractMethodError` silencieux en jeu pour les plugins tiers — d'où le contrôle.

## Importer les soldes EssentialsX (une fois)

À faire **serveur arrêté**, avec une sauvegarde des deux dossiers :

```bash
python3 scripts/import-essentials-balances.py --dry-run   # compare sans rien écrire
python3 scripts/import-essentials-balances.py             # écrit economy.yml
```

Le script lit `plugins/Essentials/userdata/<uuid>.yml` (champ `money`), ignore les UUID invalides, et ne
touche pas à un compte déjà présent dans `economy.yml`.

## Rollback

Supprimer `ValoriaEconomy-v1.6.3.jar`, remettre `EssentialsX.jar` : EssentialsX réenregistre son propre
fournisseur au démarrage, `ValoriaTycoon` le retrouve, rien d'autre à changer. Les montants gagnés pendant
la période ValoriaEconomy restent dans `economy.yml` (à recopier à la main dans Essentials si besoin).
