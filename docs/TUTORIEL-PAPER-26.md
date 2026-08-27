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

Commandes (interceptées comme `/sell`, donc pas besoin de les déclarer ailleurs ; `/auctionhouse`
et `/marche` sont des équivalents) :

| commande | effet |
| --- | --- |
| `/ah` | ouvre la interface du marché (18 annonces par page) |
| `/ah sell <prix>` | met en vente **l'item tenu en main** |
| `/ah cancel` | annule toutes tes annonces et te rend les items |

Dans l'interface : clic sur une annonce = achat immédiat (débit de ton solde, versement au vendeur
moins la commission) ; `Page précédente/suivante` pour naviguer ; `Récupérer mes annonces` pour
tout annuler ; `Vendre ce que tu tiens` rappelle la commande. Toutes les interfaces ouvertes sont
redessinées dès qu'une annonce bouge : personne ne peut acheter un item déjà vendu (le clic renvoie
« cette annonce vient d'être vendue »).

Config (`plugins/ValoriaTycoon/config.yml`, à la fin du fichier) :

```yaml
auction-house:
  enabled: true
  title: "&aMarché des joueurs"
  sell-fee: 0.02      # commission prélevée à la mise en vente
  min-price: 1.0
  max-price: 1000000.0
```

Permissions (déclarées dans `plugin.yml`) : `valoriatycoon.ah.use` et `valoriatycoon.ah.sell`
ouvertes à tous par défaut, `valoriatycoon.ah.notify` pour les annonces de vente dans le chat
(OP par défaut).

Choix de conception qui protègent contre la duplication et la perte d'objets :

- **séquestre serveur** : l'item quittte l'inventaire du vendeur et est écrit dans
  `plugins/ValoriaTycoon/auction.yml`, sauvegardé à chaque opération. Rien ne vit dans l'interface.
- **achat = d'abord livraison, ensuite paiement** : inventaire plein → l'item tombe à tes pieds et
  le paiement est annulé ; paiement refusé par Vault → l'item est retiré de nouveau. Aucun chemin
  ne donne un item gratuit ou un solde créédité deux fois.
- **items du plugin refusés** : un bloc/objet de générateur (marqué en `PersistentDataContainer`)
  ne peut pas être mis en vente, pour ne pas créer de générateur hors sol sans propriétaire.
- **aucun nom interne du serveur** : ni `net.minecraft`, ni paquet CraftBukkit — donc ce module ne
  se casse pas quand Minecraft change de nommage, contrairement à l'ancienne bibliothèque NBT.

Non fait volontairement dans cet increment (à demander si tu le veux) : expiration automatique des
annonces, recherche par nom d'item, minimum/maximum par transaction, annulation forcee par un admin
(`/ah remove <id>`), et sauvegarde asynchrone du fichier.

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
