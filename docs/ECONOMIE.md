# Économie interne (ValoriaEconomy)

## Pourquoi un second plugin, et pas le code dans ValoriaTycoon

`ValoriaTycoon` résout son fournisseur d'argent dans son `onEnable` : il cherche un plugin à activer
**avant** lui, puis lit le service `Economy` enregistré dans le `ServicesManager` de Bukkit. Un
fournisseur enregistré par ValoriaTycoon lui-même ne serait pas visible au moment où il le cherche :
d'où un second plugin, `load: STARTUP` dans `resources-economy/plugin.yml` (ValoriaTycoon est en
`POSTWORLD`).

Dans la version d'origine, ce service passait par l'API **Vault**, ce qui obligeait à installer aussi un
plugin Vault (ou son fork VaultUnlocked). Ce n'est plus le cas : l'interface est écrite dans le dépôt,
`sources/api/xyz/arcadiadevs/valoriateconomy/Economy.java`, générée depuis `docs/economy-api.txt` et
embarquée dans le jar de ValoriaTycoon. Le bytecode livré a été renommé en conséquence
(`scripts/selfmade-api-patch.py`) : `getPlugin("Vault")` est devenu `getPlugin("ValoriaEconomy")`, et
tous les descripteurs `net/milkbowl/…` pointent sur nos classes.

Conséquences heureuses : aucun fichier de config de ValoriaTycoon n'est touché, et tous les autres plugins
du serveur (boutiques, donneurs, téléports payants) voient le même argent en implémentant la même
interface.

## Ce qui remplace quoi

| avant | après |
| --- | --- |
| EssentialsX (coffre + `/money`, `/pay`) | `ValoriaEconomy` (`/bal`, `/pay`, `/baltop`, `/eco`) |
| `plugins/Essentials/userdata/*.yml` | `plugins/ValoriaEconomy/economy.yml`, un seul fichier |
| config de l'argent éparse | `plugins/ValoriaEconomy/config.yml` (`starting-balance`, `currency.*`) |
| `Vault` / VaultUnlocked (pont d'API) | **supprimé** : l'interface d'économie vit dans le dépôt et dans le jar |
| `ProtocolLib` (exigé par les hologrammes HoloEasy) | **supprimé** : hologrammes rendus par des entités Bukkit |

## Installer

1. Déposer **les deux** jars dans `plugins/` : `ValoriaTycoon-v1.6.3.jar` et `ValoriaEconomy-v1.6.3.jar`.
2. Retirer `EssentialsX.jar`, `Vault.jar`/`VaultUnlocked.jar` et `HoloEasy.jar` : ils ne servent plus.
   Si un autre plugin s'enregistre déjà comme fournisseur, **une seule** économie gagne : ValoriaEconomy
   refuse de prendre la place d'un fournisseur déjà enregistré et le dit dans le log.
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
- **44 signatures, dont une générée deux fois** : l'interface `Economy` et le fournisseur
  `ValoriaEconomyProvider.java` sont émis par `scripts/generate-economy-api.py` depuis
  `docs/economy-api.txt`, et `scripts/verify-economy-api.py` refuse tout écart entre snapshot, interface,
  fournisseur **et** les `.class` livrés. Une méthode oubliée = erreur de compilation ; une signature
  décalée = `AbstractMethodError` silencieux en jeu — d'où le contrôle.
- **Une seule copie de l'interface à l'exécution** : elle n'est embarquée que dans le jar de
  ValoriaEconomy (côté fournisseur). Si elle était dans les deux, `getRegistration(Economy.class)`
  chercherait un `Class` objet différent selon le classloader et renverrait `null` sans erreur
  visible. Et si elle n'était QUE dans le jar de ValoriaTycoon, ValoriaEconomy — chargé en `STARTUP`,
  avant le classloader de ValoriaTycoon — échouerait dès son chargement (`Could not load plugin`,
  l'Economy rouge du 2026-08-30). Le consommateur résout l'interface par délégation via son
  `softdepend`, jamais il ne l'embarque.

## Importer les soldes EssentialsX (une fois)

À faire **serveur arrêté**, avec une sauvegarde des deux dossiers :

```bash
python3 scripts/import-essentials-balances.py --dry-run   # compare sans rien écrire
python3 scripts/import-essentials-balances.py             # écrit economy.yml
```

Le script lit `plugins/Essentials/userdata/<uuid>.yml` (champ `money`), ignore les UUID invalides, et ne
touche pas à un compte déjà présent dans `economy.yml`.

## Rollback

Supprimer `ValoriaEconomy-v1.6.3.jar`, remettre `EssentialsX.jar` **plus** un plugin exposant le
service sous `xyz.arcadiadevs.valoriateconomy.Economy` (EssentialsX seul ne suffit pas : il parle
l'API Vault). En pratique, un rollback complet = revenir au jar `ValoriaTycoon` livré par
`artifacts/original/` **et** réinstaller Vault + EssentialsX. Les montants gagnés pendant la période
ValoriaEconomy restent dans `economy.yml` (le script `scripts/import-essentials-balances.py` se relit
dans l'autre sens à la main, un fichier par joueur).
