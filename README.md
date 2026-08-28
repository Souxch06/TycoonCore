# ValoriaTycoon v1.6.3 

**ValoriaTycoon** est un plugin Minecraft Bukkit/Spigot/Paper basé sur un système de générateurs de ressources pour les serveurs Tycoon et SkyBlock.

Les joueurs peuvent placer des générateurs, récupérer automatiquement des drops, améliorer leurs générateurs, vendre leurs ressources et progresser grâce à une économie configurable.

## Compatibilité versions

- Serveurs Bukkit / Spigot / Paper de **1.7 à 1.21.11** et versions **calendaires 26.1 / 26.2** (Paper 26.2 recommandé).
- Depuis Minecraft 26.1, les numéros de version sont calendaires (`26.1`, `26.1.1`, `26.2`, ...). Paper n'expose
  plus non plus de paquet CraftBukkit déplacé (`org.bukkit.craftbukkit.v1_20_R1`) ni de suffixe `-R0.1-SNAPSHOT` :
  la version d'API vaut par exemple `26.2.build.112-stable`.
- La détection de version (`utils/ServerVersion.java`) lit la version d'API Bukkit, puis la version Minecraft
  exposée par Paper, et se rabat sur le nom du paquet CraftBukkit pour les serveurs plus anciens. Une version
  inconnue plus récente que 26.2 est rattachée à la dernière version connue, afin de ne pas désactiver les
  fonctionnalités modernes.
- Les bibliothèques embarquées XSeries (`XMaterial`, `XReflection`) portent un correctif de parsing de version,
  reproduit de façon déterministe par `scripts/patch-class-version-patterns.py` et vérifié par
  `scripts/verify-paper26-compat.py`.
- L'identification des générateurs ne passe plus par les noms NMS obfusqués de `NBTEditor` (inexistants sur
  les serveurs récents) mais par le `PersistentDataContainer` de Bukkit, via un pont compilé depuis
  `sources/shaded/io/github/bananapuncher714/nbteditor/NBTEditor.java` ; l'implémentation d'origine reste
  disponible en repli sous le nom `LegacyNbtBridge` (`scripts/install-nbt-bridge.py`).
- Le JAR est en bytecode Java 17 (exécutable sur Java 17+) ; un serveur 26.x exige Java 25.
- Contrôle rapide, sans JDK ni serveur : `python3 scripts/verify-paper26-compat.py` (l'arbre du dépôt)
  puis `python3 scripts/verify-paper26-compat.py target/ValoriaTycoon-v1.6.3.jar` (JAR compilé).
  Un workflow de validation pour les Pull Requests est fourni dans `scripts/ci/build-workflow.yml`.

## Économie du serveur

Le dépôt contient **deux** plugins, construits par le même build Maven :

| plugin | rôle | livrable |
| --- | --- | --- |
| `ValoriaTycoon` | générateurs, marché `/ah`, tableau de bord `/sb`, interfaces | `target/ValoriaTycoon-v1.6.3.jar` |
| `ValoriaEconomy` | soldes (`/bal`, `/pay`, `/baltop`, `/eco`) et fournisseur du service d'économie | `target/ValoriaEconomy-v1.6.3.jar` |

`ValoriaEconomy` est chargé en `STARTUP` pour que le service `Economy` existe avant l'`onEnable` de
`ValoriaTycoon`, et pour que l'interface `xyz/arcadiadevs/valoriateconomy/Economy` (embarquée dans le jar
de ValoriaTycoon) soit la seule version vue par les deux plugins. Les deux jars suffisent : **aucun plugin
à télécharger**, ni Vault, ni EssentialsX — voir `docs/ECONOMIE.md`, et `docs/DEPLOY-2-JARS.md` pour la
pipeline qui ne copiera pas qu'un seul jar.

## Fonctionnalités

- Générateurs de ressources par paliers.
- Achat de générateurs via une interface en jeu.
- Amélioration d'un générateur ou de tous les générateurs connectés.
- Vente des drops depuis la main, l'inventaire ou une interface dédiée.
- Baguette de vente configurable.
- Limites de générateurs par joueur ou par île.
- Hologrammes au-dessus des générateurs (moteur interne : entités armures, sans HoloEasy ni ProtocolLib).
- Événements temporaires : drops, vente et vitesse.
- Sons et particules lors des améliorations.
- Support des permissions pour les limites, rayons et multiplicateurs.
- Compatibilité avec plusieurs plugins SkyBlock.

## Structure du projet

```text
artifacts/
  original/                  # Fichier JAR distribué
  extracted/                 # Contenu complet organisé par packages et ressources

sources/
  plugin/                    # Code principal de ValoriaTycoon (dont le moteur d'hologrammes maison)
  api/                       # Interface d'économie interne (embarquée dans le jar ValoriaTycoon
                             #  seul : les deux plugins doivent voir le MÊME objet Class)
  economy/                   # ValoriaEconomy : soldes, /bal /pay /baltop /eco
  shaded/                    # Librairies incluses avec le plugin
                             # (dont com/google/gson/module-info.java : descripteur décompilé, hors
                             #  racine pour ne pas mettre javac en mode module)

resources/
  config.yml                 # Configuration principale
  messages.yml               # Messages du plugin
  plugin.yml                 # Métadonnées Bukkit/Spigot
  data/                      # Fichiers de données par défaut

docs/
  TUTORIEL-COMPLET.md        # Tuto pas à pas, 100 % navigateur (coller le build, tester, livrer)
  CI-A-COLLER.yml            # Contenu exact à coller dans .github/workflows/build.yml
  STRUCTURE.md               # Détails sur l'organisation du dépôt
  TUTORIEL-PAPER-26.md       # Tutoriel pas à pas (Paper 26.2)
  ECONOMIE.md                # ValoriaEconomy : /bal, /pay, /eco, marché /ah
  HOLOGRAMMES.md             # Moteur d'hologrammes interne (sans HoloEasy ni ProtocolLib)
  DEPLOY-2-JARS.md           # Livrer les DEUX jar par la pipeline
  technical-report.txt       # Rapport technique

scripts/
  verify-extraction.py       # Cohérence entre le JAR d'origine et artifacts/extracted
  selfmade-api-patch.py      # Remplace les API tierces (Vault, hologrammes) par les nôtres
  generate-economy-api.py     # Émet l'interface d'économie et son fournisseur depuis docs/economy-api.txt
  verify-economy-api.py      # Refuse tout écart snapshot ↔ interface ↔ fournisseur ↔ .class livrés
  verify-paper26-compat.py   # 90+ contrôles sur l'arbre et sur le JAR compilé
```

## Installation

1. Construisez les deux jar (`mvn package`, voir `docs/DEPLOY-2-JARS.md` pour la pipeline) :
   `target/ValoriaTycoon-v1.6.3.jar` et `target/ValoriaEconomy-v1.6.3.jar`.
2. Placez **les deux** dans le dossier `plugins/` du serveur.
3. Redémarrez le serveur.
4. Modifiez les fichiers générés dans `plugins/ValoriaTycoon/` et `plugins/ValoriaEconomy/` si besoin.
5. Utilisez `/valoriatycoon reload` après modification de la configuration (sauf les lignes du
   tableau de bord, lues une seule fois : `docs/TUTORIEL-PAPER-26.md`).

## Dépendances

### Obligatoires

**Aucune, hors ce dépôt.** Les deux jar construits ici suffisent : l'économie (`/bal`, `/pay`,
`/baltop`, `/eco`), le service d'économie consulté par ValoriaTycoon et les hologrammes sont écrits
dans le dépôt. Rien n'est téléchargé, et le plugin ne contacte plus aucun service distant.

Les plugins d'origine à installer ne sont donc **plus** nécessaires, et doivent être retirés s'ils
sont encore là :

| avant | maintenant |
| --- | --- |
| Vault / VaultUnlocked (pont d'API) | interface interne `xyz/arcadiadevs/valoriateconomy/Economy`, embarquée dans le jar |
| EssentialsX (soldes) | `ValoriaEconomy-v1.6.3.jar` |
| HoloEasy (hologrammes) | moteur interne `xyz/arcadiadevs/valoriatycoon/hologram/`, entités armures, API Bukkit seule |
| ProtocolLib (exigé par HoloEasy) | inutile : plus aucun paquet maison |
| api.spigotmc.org (contrôle de licence au démarrage) | supprimé du bytecode livré |

### Optionnelles (aucune n'est requise pour jouer)

Uniquement des ponts déjà prévus par le plugin, activés si le plugin est présent : `PlaceholderAPI`
(placeholders dans les hologrammes et le chat), `Oraxen` / `ItemsAdder` (items custom comme drop),
`Essentials`, et les bridges SkyBlock `IridiumSkyblock`, `SuperiorSkyblock2`, `ASkyBlock`,
`AcidIsland`, `BentoBox`. Sur un serveur 100 % maison, laissez-les absents : le plugin fonctionne.

Les bibliothèques toujours embarquées dans le jar (gson pour `block_data.json`, XSeries pour les
noms de matériaux, `LegacyNbtBridge` en repli NBT) ne demandent rien à installer ; elles sont
corrigées en interne par les scripts du dépôt (`XSeries` et le pont NBT ne fonctionnaient plus sur
26.x).

## Commandes

| Commande | Description |
| --- | --- |
| `/valoriatycoon` | Affiche la version du plugin. |
| `/valoriatycoon help` | Affiche l'aide des commandes. |
| `/valoriatycoon give <joueur> <palier> [quantité]` | Donne un générateur à un joueur. |
| `/valoriatycoon giveall <palier> [quantité]` | Donne un générateur à tous les joueurs connectés. |
| `/valoriatycoon wand sell <joueur> <utilisations> <multiplicateur>` | Donne une baguette de vente à un joueur. |
| `/valoriatycoon setlimit <joueur> <limite>` | Définit la limite de générateurs d'un joueur. |
| `/valoriatycoon addlimit <joueur> <limite>` | Ajoute une valeur à la limite d'un joueur. |
| `/valoriatycoon startevent <nom>` | Démarre un événement manuellement. |
| `/valoriatycoon stopevent` | Arrête l'événement en cours. |
| `/valoriatycoon reload` | Recharge la configuration du plugin. |
| `/generators` ou `/gen` | Ouvre l'interface d'achat des générateurs. |
| `/sell` | Ouvre l'interface de vente. |
| `/ah` | Marché entre joueurs (voir `docs/ECONOMIE.md`). |
| `/ah sell <quantité>` | Met en vente le contenu de la main. |
| `/sb` | Affiche ou masque le tableau de bord. |
| `/bal`, `/pay <joueur> <montant>`, `/baltop` | Soldes (ValoriaEconomy). |
| `/eco give\|take\|set <joueur> <montant>` | Administration des soldes. |
| `/selldrops hand` ou `/sell hand` | Vend les drops tenus en main. |
| `/selldrops all` ou `/sell all` | Vend tous les drops de l'inventaire. |

## Permissions

| Permission | Description |
| --- | --- |
| `valoriatycoon.admin` | Accès administrateur global. |
| `valoriatycoon.generator.open` | Ouvrir l'interface des générateurs. |
| `valoriatycoon.admin.give` | Donner un générateur à un joueur. |
| `valoriatycoon.admin.give.all` | Donner un générateur à tous les joueurs. |
| `valoriatycoon.admin.reload` | Recharger le plugin. |
| `valoriatycoon.admin.startevent` | Démarrer un événement. |
| `valoriatycoon.admin.stopevent` | Arrêter un événement. |
| `valoriatycoon.drop.sell.all` | Vendre tous les drops de l'inventaire. |
| `valoriatycoon.drop.sell.hand` | Vendre les drops en main. |
| `valoriatycoon.drop.sell.gui` | Utiliser l'interface de vente. |
| `valoriatycoon.sell.multiplier.<montant>` | Définir un multiplicateur de vente. |
| `valoriatycoon.limit.<montant>` | Définir une limite de générateurs. |
| `valoriatycoon.radius.<montant>` | Définir un rayon d'activation. |
| `valoriatycoon.admin.setlimit` | Modifier la limite d'un joueur. |
| `valoriatycoon.admin.addlimit` | Ajouter une limite à un joueur. |
| `valoriatycoon.admin.givewand` | Donner une baguette de vente. |

## Configuration

Les principaux fichiers de configuration se trouvent dans `resources/` :

- `config.yml` : réglages des générateurs, interfaces, événements, hologrammes, particules et limites.
- `messages.yml` : messages affichés aux joueurs.
- `plugin.yml` : informations Bukkit/Spigot du plugin.

Les générateurs sont configurables avec leur nom, palier, prix, prix de vente, vitesse, item généré, bloc utilisé, description, hologramme et option de cassage instantané.
