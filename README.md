# GensPlus v1.6.3 — version française

Ce dépôt contient une version organisée et francisée de **GensPlus v1.6.3**, un plugin Minecraft Bukkit/Spigot/Paper centré sur des générateurs de ressources.

Le code a été relu à partir des sources décompilées afin d'identifier les fonctionnalités, les commandes, les messages et les fichiers utiles du plugin.

## Ce que fait le plugin

GensPlus permet aux joueurs de placer des générateurs qui produisent automatiquement des drops. Le plugin gère notamment :

- les générateurs par paliers ;
- l'achat de générateurs via une interface ;
- l'amélioration d'un générateur ou de tous les générateurs connectés ;
- la vente des drops depuis la main, l'inventaire ou une interface ;
- une baguette de vente ;
- des limites de générateurs par joueur ou par île ;
- des hologrammes au-dessus des générateurs ;
- des événements temporaires de drops, de vente et de vitesse ;
- des particules et sons lors des améliorations ;
- l'intégration avec Vault, ProtocolLib, PlaceholderAPI, HoloEasy et plusieurs plugins SkyBlock.

## Organisation du dépôt

```text
artifacts/
  original/
    GensPlus-v1.6.3.jar       # JAR original conservé
  extracted/                  # Contenu extrait du JAR, avec les ressources principales traduites

sources/
  plugin/                     # Code principal décompilé du plugin GensPlus
  shaded/                     # Librairies incluses dans le JAR
  module-info.java

resources/                    # Ressources utiles traduites en français
  config.yml
  messages.yml
  plugin.yml
  data/

docs/
  STRUCTURE.md                # Explication de la structure du dépôt
  decompilation-summary.txt   # Résumé des limites de décompilation

scripts/
  verify-extraction.py        # Vérifie que tous les fichiers du JAR sont présents dans artifacts/extracted
```

## Traduction française

Les fichiers de configuration principaux ont été traduits en français :

- `resources/config.yml`
- `resources/messages.yml`
- `resources/plugin.yml`
- `artifacts/extracted/config.yml`
- `artifacts/extracted/messages.yml`
- `artifacts/extracted/plugin.yml`

Les textes visibles dans le code principal ont aussi été traduits lorsque c'était pertinent : messages joueurs, menus, logs utiles, erreurs et textes d'aide.

Les clés techniques, noms de permissions, matériaux Bukkit, actions Bukkit et placeholders ont été conservés tels quels afin de ne pas casser le fonctionnement du plugin.

Deux clés de configuration ont aussi été alignées sur les chemins réellement lus par le code décompilé : `can-items-be-*` et `developer-options.enabled`.

## Commandes principales

| Commande | Utilité |
| --- | --- |
| `/gensplus` | Affiche la version du plugin. |
| `/gensplus help` | Affiche l'aide des commandes. |
| `/gensplus give <joueur> <palier> [quantité]` | Donne un générateur à un joueur. |
| `/gensplus giveall <palier> [quantité]` | Donne un générateur à tous les joueurs. |
| `/gensplus wand sell <joueur> <utilisations> <multiplicateur>` | Donne une baguette de vente. |
| `/gensplus setlimit <joueur> <limite>` | Définit la limite de générateurs d'un joueur. |
| `/gensplus addlimit <joueur> <limite>` | Ajoute une valeur à la limite d'un joueur. |
| `/gensplus startevent <nom>` | Force le démarrage d'un événement. |
| `/gensplus stopevent` | Arrête l'événement en cours. |
| `/gensplus reload` | Recharge le plugin. |
| `/generators` | Ouvre l'interface des générateurs. |
| `/selldrops hand` | Vend les drops tenus en main. |
| `/selldrops all` | Vend tous les drops de l'inventaire. |
| `/selldrops gui` | Ouvre l'interface de vente. |

## Permissions repérées dans le code

| Permission | Utilité |
| --- | --- |
| `gensplus.admin` | Permission administrateur globale. |
| `gensplus.generator.open` | Accès à l'interface des générateurs. |
| `gensplus.admin.give` | Donner un générateur à un joueur. |
| `gensplus.admin.give.all` | Donner un générateur à tous les joueurs. |
| `gensplus.admin.reload` | Recharger le plugin. |
| `gensplus.admin.startevent` | Démarrer un événement. |
| `gensplus.admin.stopevent` | Arrêter un événement. |
| `gensplus.drop.sell.all` | Vendre tous les drops de l'inventaire. |
| `gensplus.drop.sell.hand` | Vendre les drops en main. |
| `gensplus.drop.sell.gui` | Utiliser l'interface de vente. |
| `gensplus.sell.multiplier.<montant>` | Multiplicateur de vente par permission. |
| `gensplus.limit.<montant>` | Limite de générateurs par permission. |
| `gensplus.radius.<montant>` | Rayon d'activation par permission. |
| `gensplus.admin.setlimit` | Définir une limite de générateurs. |
| `gensplus.admin.addlimit` | Ajouter une limite de générateurs. |
| `gensplus.admin.givewand` | Donner une baguette de vente. |

## Dépendances plugin

Le `plugin.yml` indique les dépendances suivantes :

- dépendances obligatoires : `Vault`, `ProtocolLib` ;
- dépendances optionnelles : `Oraxen`, `Essentials`, `ItemsAdder`, `IridiumSkyblock`, `SuperiorSkyblock2`, `ASkyBlock`, `AcidIsland`, `BentoBox`, `HoloEasy`.

## Notes importantes

Les sources dans `sources/` proviennent d'une décompilation. Elles sont utiles pour lire, comprendre et retravailler le plugin, mais elles peuvent nécessiter des corrections avant recompilation.

Le JAR original est conservé dans :

```text
artifacts/original/GensPlus-v1.6.3.jar
```
