# Tuto : valider et installer le correctif Paper 26.2

Ce qu'on va faire, en une phrase : GitHub doit **compiler** le plugin et **vérifier** le fichier produit
à ta place (mon environnement n'a pas de Java pour le faire), puis tu déposes le fichier obtenu sur ton
serveur et tu testes en jeu.

Ce dont tu as besoin : **un navigateur**, connecté à GitHub sur le compte `Souxch06`. C'est tout.
Aucun terminal, aucune installation.

---

## Étape 1 — Créer le fichier de vérification

Ouvre ce lien (il ouvre directement la page de création sur la bonne branche) :

```
https://github.com/Souxch06/ValoriaTycoon/new/arena/01a043a8-valoriatycoon
```

Si le lien ne fonctionne pas : va sur `github.com/Souxch06/ValoriaTycoon` → bouton **Add file** →
**Create new file**.

> ⚠️ Vérifie en haut de page que le sélecteur de branche affiche bien `arena/01a043a8-valoriatycoon`
> et non `main`. C'est important : il ne faut surtout pas créer ce fichier sur `main`.

## Étape 2 — Donner le nom du fichier

Dans le champ « Name your file », tape exactement (les points et les `/` compris) :

```
.github/workflows/build.yml
```

## Étape 3 — Coller le contenu

Dans la grande zone de texte, colle exactement ceci :

```yaml
name: Build and Validate ValoriaTycoon
on:
  pull_request:
  workflow_dispatch:
jobs:
  build-and-validate:
    name: Compilation et controles de compatibilite
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - name: Controle renommage de marque
        run: python3 scripts/rebrand-classes.py --check
      - name: Controle correctifs classes vendorisees
        run: python3 scripts/patch-class-version-patterns.py --check
      - name: Controle pont NBT PersistentDataContainer
        run: python3 scripts/install-nbt-bridge.py --check
      - name: Controle coherence extraction
        run: python3 scripts/verify-extraction.py
      - name: Compiler le plugin
        run: mvn -B -ntp clean package -DskipTests
      - name: Verifier le JAR (compat Paper 26.x)
        run: |
          python3 scripts/verify-paper26-compat.py target/ValoriaTycoon-v1.6.3.jar
          python3 scripts/rebrand-classes.py --check --jar target/ValoriaTycoon-v1.6.3.jar
          python3 scripts/install-nbt-bridge.py --check --jar target/ValoriaTycoon-v1.6.3.jar
      - name: Publier le JAR construit
        uses: actions/upload-artifact@v4
        with:
          name: ValoriaTycoon-jar
          path: target/ValoriaTycoon-v1.6.3.jar
          if-no-files-found: error
```

## Étape 4 — Enregistrer

En bas de page : bouton vert **Commit changes…** → fenêtre qui s'ouvre → laisse sur
**Commit directly to the `arena/01a043a8-valoriatycoon` branch** → **Commit changes**.

## Étape 5 — Lancer la vérification

1. Onglet **Actions** (dans le dépôt).
2. À gauche, clique sur **Build and Validate ValoriaTycoon**.
3. Bouton vert **Run workflow** → **Run workflow** (garde la branche `arena/01a043a8-valoriatycoon`).
4. Attends 1 à 3 minutes, recharge la page.

Deux issues possibles :

| Ce que tu vois | Ce que ça veut dire | Ce que tu fais |
| --- | --- | --- |
| ✅ rond vert sur `Compilation et controles de compatibilite` | le plugin compile et le JAR est conforme | passe à l'étape 6 |
| ❌ rond rouge | une étape a échoué | ouvre le job rouge, clique sur l'étape en rouge, copie les 30 dernières lignes du log, **colle-les moi dans le chat** — je corrige et je repousse |

## Étape 6 — Récupérer le bon fichier

Dans la page du run vert, tout en bas, rubrique **Artifacts** → clique sur **ValoriaTycoon-jar**.
Tu obtiens un `.zip` ; extrais-le, tu y trouves **`ValoriaTycoon-v1.6.3.jar`**.

> ⚠️ N'utilise **pas** le fichier `artifacts/original/ValoriaTycoon-v1.6.3.jar` du dépôt : c'est l'ancien
> JAR `GensPlus`, sans aucun correctif.

## Étape 7 — Préparer le serveur

Ton serveur doit tourner sur :

- **Paper 26.2** (dernier build stable sur `papermc.io`) ;
- **Java 25** ;
- plugins installés : **Vault**, un plugin d'économie (par ex. **EssentialsX**), **ProtocolLib** ;
- et **HoloEasy** seulement si tu laisses `holograms-enabled: true` dans `config.yml`
  (sinon le plugin se désactive tout seul au démarrage, c'est voulu).

## Étape 8 — Installer et tester

1. Arrête le serveur.
2. Mets le `ValoriaTycoon-v1.6.3.jar` téléchargé à l'étape 6 dans le dossier `plugins/` (écrase l'ancien).
3. Démarre le serveur, garde la console ouverte.
4. En jeu, dans l'ordre :
   - `/valoriatycoon reload`
   - `/generators` → achète un générateur
   - pose-le au sol
   - attends un drop, puis `/selldrops all`
   - `/generators` → améliore le palier
5. Sauvegarde/arrête le serveur.

## Étape 8 bis — Le piège qui fait rougir le plugin (hologrammes)

Le plugin **se désactive lui-même** si les hologrammes sont activés alors que HoloEasy n'est pas
installé. Dans la console, ça donne exactement ceci :

```
[ValoriaTycoon] HoloEasy not found. Disabling plugin.
[ValoriaTycoon] Disabling ValoriaTycoon v1.6.3
```

Deux issues, au choix :
- **sans hologrammes** (le plus simple pour tester) : ouvre `plugins/ValoriaTycoon/config.yml`,
  cherche la ligne `holograms:` et, en dessous, passe `enabled: true` à `enabled: false`, enregistre,
  redémarre ;
- **avec hologrammes** : installe le plugin **HoloEasy** (et ProtocolLib, qu'il utilise pour les
  paquets), puis redémarre.

Sans ce choix, `/plugins` affichera ValoriaTycoon en rouge même quand tout le reste va bien.

## Étape 9 — Comment savoir si c'est bon

Dans la console de démarrage et de test :

| Recherche | Résultat attendu |
| --- | --- |
| `ExceptionInInitializerError` | **absent** |
| `Unknown Minecraft mapping` | **absent** |
| stack-trace contenant `nbteditor` | **absent** (c'était le symptôme du générateur mort) |
| `PersistentDataContainer inutilisable (membres API manquants : …)` | **absent** sur 26.2. Si elle apparaît, la liste entre parenthèses dit exactement quel point de l'API Bukkit n'a pas été résolu : envoie-la moi telle quelle |

Et en jeu : le générateur posé est **reconnu** (il produit, il est amélioré, ses drops se vendent).

## Étape 10 — Me rendre compte

Colle-moi dans le chat, en texte brut :

1. la ligne de version du serveur (`This server is running Paper version …`) ;
2. le log entre `Enabling ValoriaTycoon` et la fin du démarrage ;
3. si un point du test en jeu n'a pas marché, lequel.

Je corrige sur la même branche ; la PR se met à jour toute seule et tu relances l'étape 5.

---

## Interface d'amélioration (depuis la correction 26.2)

L'interface `Améliorer le générateur` n'a plus qu'une case cliquable, plus une case de lecture seule :

| case | rôle |
| --- | --- |
| 11 (au centre) | améliorer **ce** générateur — un clic, débit du prix, fermeture |
| 15 | **statistiques** du générateur — aucun clic, aucune dépense possible |

Le texte et la description sont configurables, sans toucher au code :

```yaml
guis:
  upgrade-gui:
    stats:
      first-line: "&e》 &fStatistiques du générateur&e 《"
      lore:
        - "&fArgent : &a%money%"
        - "&fProchaine amélioration : &a%upgradePrice%"
```

Placeholders disponibles dans les deux cases : `%money%`, `%upgradePrice%`, `%tier%`, `%speed%`,
`%price%`, `%sellPrice%`, `%spawnItem%`, `%blockType%`, `%nextTier%`, `%nextSpeed%`, `%nextPrice%`,
`%nextSellPrice%`, `%nextSpawnItem%`, `%nextBlockType%`. (`%upgradePrice%` était affiché brut sur la
case « améliorer » avant la correction : la substitution n'existait que pour l'amélioration groupée.)

À savoir : l'amélioration **groupée** de tous les générateurs connectés n'a plus de bouton dans cette
interface (méthode conservée dans le code). Si tu la veux, dis-le — soit on lui rend une case, soit on
passe par `guis.upgrade-gui.enabled: false` + shift+clic droit sur le bloc.

## Le marché entre joueurs (`/ah`)

Commandes (interceptées comme `/sell`, rien à déclarer ailleurs ; `/auctionhouse` et `/marche`
fonctionnent aussi) :

| commande | effet |
| --- | --- |
| `/ah` | ouvre le marché (45 cases, 36 annonces par page) |
| `/ah sell <prix> [quantité]` | met en vente l'item en main, **prix à la pièce** (défaut : la pile entière) |
| `/ah search <motif>` | filtre par nom d'item (`/ah search` seul = tout revoir) |
| `/ah own` | ne voit que ses annonces |
| `/ah cancel` / `/ah cancel <id>` | récupère tout, ou une annonce précise |
| `/ah stats` | annonces totales, les tiennes, taxe, expiration, blacklist |
| `/ah remove <id>`, `/ah reload` | administration (`valoriatycoon.ah.admin`) |

Dans l'interface : **clic gauche = 1 pièce**, **clic droit = 1 stack**, **Maj = tout le lot** ;
sur **tes** annonces, **Maj + clic = annuler et récupérer**. En bas : pages, filtre, tri
(numéro / prix croissant / prix décroissant / plus récentes), bascule marché↔mes annonces, aide.

Config (`plugins/ValoriaTycoon/config.yml`) :

```yaml
auction-house:
  enabled: true
  listing-fee: 0.0        # frais de mise en vente (fraction du total), anti-spam
  sales-tax: 0.02         # taxe sur le vendeur à chaque vente
  min-price: 0.5
  max-price: 10000000.0
  enforce-price-band: true
  price-band: 12.0        # prix hors [moyenne/12 ; moyenne×12] refusé
  max-listings-per-player: 6
  expiry-hours: 72        # 0 = pas d'expiration
  sweep-ticks: 1200       # ménage des expirations, 60 s
  blacklist: [ BEDROCK, BARRIER, COMMAND_BLOCK, ... ]
```

Garanties de conception (c'est ce qui rend un marché utilisable en communauté) :

- **Séquestre serveur** : l'item déposé quitte l'inventaire et vit dans `auction.yml`. Rien ne reste
  dans une interface → pas de dupe à la déconnexion, pas de perte au crash, l'état survit au restart.
- **Prix unitaire** : une annonce = prix à la pièce + quantité. Les achats partiels sont donc exacts,
  sans arrondi qui crée ou détruit de la monnaie.
- **Livraison avant facturation, remboursement symétrique** : ce qui n'a pas pu être donné est remboursé
  et rendu à l'annonce ; si l'économie refuse le remboursement, l'item est posé aux pieds du joueur et
  tracé dans le log. Un objet ne se perd jamais.
- **Écriture atomique** : `auction.yml.tmp` puis `ATOMIC_MOVE`. Un crash en pleine sauvegarde laisse
  l'ancien fichier intact, pas un YAML tronqué.
- **Boîte de rendus** : item expiré, annonce retirée par un admin, vendeur hors-ligne → déposé dans
  `returns.<uuid>` et rendu au prochain connect. Aucun « ton item a disparu parce que tu n'étais pas là ».
- **Contre-expertise de prix** : moyenne par type d'item tenue à jour par vente réelle ; une annonce hors
  bande est refusée → bloque dump, cadeau à 1 $ et blanchiment.
- **Items du plugin refusés** : un item marqué en `PersistentDataContainer` par ValoriaTycoon (bloc ou
  pierre de générateur) ne peut pas être vendu, pour ne pas créer de générateur sans propriétaire.
- **Aucun nom interne du serveur** dans ce module : ni `net.minecraft`, ni paquet CraftBukkit — donc pas
  de `NoSuchMethodError` quand Minecraft renomme. Les contrôles du dépôt le vérifient dans le *code*
  (commentaires exclus) et sur le `.class` livré.

Reste hors de ce module (à demander si besoin) : encherès, paniers, historique par joueur, notification
de vente par mail/discord, recherche par texte libre côté GUI.

## Tableau de bord (`/sb`)

Une sidebar légère, dans le plugin (aucun plugin de scoreboard requis) :

| commande | effet |
| --- | --- |
| (aucune) | elle s'affiche toute seule au connect |
| `/sb` | l'active ou la coupe pour soi (`/scoreboard`, `/tableau` identiques) |

Personnalisation, dans `plugins/ValoriaTycoon/config.yml` :

```yaml
scoreboard:
  enabled: true
  update-ticks: 40            # 2 s ; ne pas descendre sous 10
  title: "&a&lValoriaTycoon"
  lines:                      # 15 lignes max, dans l'ordre
    - "&7Joueur : &f%player%"
    - "&7Solde : &a%money%"
    - "&7Connectés : &f%server%"
```

Placeholders disponibles : `%player%`, `%money%` (format de l'économie), `%balance%` (nombre brut),
`%generators%` (blocs posés), `%server%` (joueurs connectés), `%ping%`. En ajouter un = une ligne de
plus dans `ScoreboardService.placeholder(...)`. Permission : `valoriatycoon.scoreboard` (ouverte à tous).

Robustesse : `Score#setScore(int)` a changé de type de retour en 1.21+ (méthode binairement
incompatible) et `registerNewObjective`/`setDisplayName` ont oscillé entre `String` et `Component`
Adventure selon les versions. Ces trois appels sont donc résolus **à l'exécution** : le tableau
s'adapte au serveur au lieu de produire un `NoSuchMethodError`. Si un serveur refuse malgré tout
l'affichage, rien d'autre du plugin n'est touché, et la liste de ce qui a manqué est lisible via
`ScoreboardService.missing()`.

## Ce qu'il ne faut PAS faire

- **Ne pas fusionner la PR #7** avant d'avoir eu ✅ à l'étape 5 **et** un test serveur concluant :
  fusionner sur `main` envoie automatiquement le JAR sur ton serveur de prod par SFTP.
- Ne pas modifier `plugin.yml` (l'`api-version: 1.13` et la dépendance `ProtocolLib` sont laissés exprès).
- Ne pas supprimer les fichiers `scripts/*.py` : ils sont la preuve que le JAR est conforme, et ils
  resserviront à chaque nouvelle version de Minecraft.

## Ce que ce correctif change et ne change pas

- Il **répare** : chargement du plugin (noms de classes incohérents), détection de version sur 26.x,
  bibliothèques XSeries qui plantaient au démarrage, identification des générateurs (passée sur le
  `PersistentDataContainer` de Bukkit au lieu des noms internes du serveur).
- Il ne **change pas** le format des mondes. Conséquence à connaître : si un monde a déjà été joué en 26.2
  **avant** ce correctif, les générateurs posés à ce moment-là n'avaient plus de marqueur lisible → il faut
  les reposer une fois après l'installation.
- La seule chose encore incertaine est l'affichage des **barres d'action** (texte au-dessus de la hotbar),
  qui dépend d'une vieille API de chat d'un serveur à l'autre. Si tu vois une erreur `NoSuchMethodError`
  côté `ActionBar`, signale-la : c'est cosmétique et je bascule ce chemin sur l'API Paper.
