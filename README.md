# ValoriaTycoon v1.6.3

**ValoriaTycoon** est un plugin Minecraft Bukkit/Spigot/Paper basé sur un système de générateurs de ressources pour les serveurs Tycoon et SkyBlock.

Les joueurs peuvent placer des générateurs, récupérer automatiquement des drops, améliorer leurs générateurs, vendre leurs ressources et progresser grâce à une économie configurable.

## Fonctionnalités

- Générateurs de ressources par paliers.
- Achat de générateurs via une interface en jeu.
- Amélioration d'un générateur ou de tous les générateurs connectés.
- Vente des drops depuis la main, l'inventaire ou une interface dédiée.
- Baguette de vente configurable.
- Limites de générateurs par joueur ou par île.
- Hologrammes au-dessus des générateurs.
- Événements temporaires : drops, vente et vitesse.
- Sons et particules lors des améliorations.
- Support des permissions pour les limites, rayons et multiplicateurs.
- Compatibilité avec plusieurs plugins SkyBlock.

## Structure du projet

```text
artifacts/
  original/                  # Fichier JAR distribué
  extracted/                 # Contenu du JAR organisé par packages et ressources

sources/
  plugin/                    # Code principal de ValoriaTycoon
  shaded/                    # Librairies incluses avec le plugin
  module-info.java

resources/
  config.yml                 # Configuration principale
  messages.yml               # Messages du plugin
  plugin.yml                 # Métadonnées Bukkit/Spigot
  data/                      # Fichiers de données par défaut

docs/
  STRUCTURE.md               # Détails sur l'organisation du dépôt
  decompilation-summary.txt  # Rapport technique

scripts/
  verify-extraction.py       # Script de vérification des fichiers extraits
```

## Installation

1. Placez le fichier `ValoriaTycoon-v1.6.3.jar` dans le dossier `plugins/` de votre serveur.
2. Installez les dépendances obligatoires.
3. Redémarrez le serveur.
4. Modifiez les fichiers générés dans `plugins/ValoriaTycoon/` selon vos besoins.
5. Utilisez `/valoriatycoon reload` après modification de la configuration.

## Dépendances

### Obligatoires

- `Vault`
- `ProtocolLib`
- Un plugin d'économie compatible Vault, par exemple `EssentialsX`

### Optionnelles

- `Oraxen`
- `ItemsAdder`
- `Essentials`
- `IridiumSkyblock`
- `SuperiorSkyblock2`
- `ASkyBlock`
- `AcidIsland`
- `BentoBox`
- `HoloEasy`
- `PlaceholderAPI`

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
| `/generators` | Ouvre l'interface d'achat des générateurs. |
| `/selldrops hand` | Vend les drops tenus en main. |
| `/selldrops all` | Vend tous les drops de l'inventaire. |
| `/selldrops gui` | Ouvre l'interface de vente. |

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
