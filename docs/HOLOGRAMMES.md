# Hologrammes internes (au-dessus des générateurs)

## Ce qui a changé

À l'origine, ValoriaTycoon embarquait une bibliothèque d'hologrammes qui envoyait des **paquets**
au client, et pour ce faire exigeait deux plugins en plus : le plugin d'hologrammes lui-même et
ProtocolLib (la bibliothèque de paquets). Conséquences vécues sur ce serveur :

1. `plugins/` devait contenir deux briques téléchargées ;
2. le plugin **se désactivait tout seul** si la première manquait (`… not found. Disabling plugin.`),
   d'où `/plugins` en rouge ;
3. la bibliothèque cherchait des classes internes du serveur qui n'existent plus en 26.x, donc même
   installée elle ne garantissait plus rien.

Le dépôt contient maintenant son **propre moteur** : `sources/plugin/…/valoriatycoon/hologram/`.
Il n'envoie aucun paquet, il pose des **entités armures invisibles** dans le monde — uniquement de
l'API Bukkit, donc aucune dépendance, et un rendu qui suit le serveur plutôt que de le deviner.

## Ce qu'il faut installer

Rien. Les hologrammes viennent du jar `ValoriaTycoon-v1.6.3.jar`. **Désinstalle** HoloEasy et
ProtocolLib s'ils sont encore dans `plugins/`.

## Configuration

```yaml
# plugins/ValoriaTycoon/config.yml
holograms:
  enabled: true        # false = plus aucune entité créée, rien d'autre ne change
  view-distance: 300   # indicatif : le serveur gère la portée réelle d'affichage

default-hologram-lines:      # lignes par défaut, si un générateur n'a pas ses propres `hologramLines`
  - "&a%name%"
  - "&7Palier : &a%tier%"
```

Placeholders compris dans les lignes : `%name%`, `%tier%`, `%speed%`, `%spawnItem%`, `%sellPrice%`
(remplacés par le plugin avant affichage). Couleurs : `&a`… et `&#rrggbb` — les secondes sont traduites
à la main par `HoloEasy.color`, sans passer par les classes de chat du serveur (disparues en 26.x).

## Fichier `holograms.txt`

`plugins/ValoriaTycoon/holograms.txt` contient une ligne par hologramme :

```
#version 1
<uuid>|<monde>|<x>|<y>|<z>|<yaw>|<pitch>|<materiau ou ->|<ligne 1>|<ligne 2>|…
```

- écrit **atomiquement** (fichier temporaire puis `ATOMIC_MOVE`) : un arrêt brutal ne peut pas le
  tronquer ;
- relu au démarrage ; les entités encore vivantes dans le monde sont **récupérées** au lieu d'être
  dupliquées (elles portent une marque `valoriatycoon:hologram-entity` en PersistentDataContainer) ;
- les armures marquées qui ne correspondent plus à rien sont **supprimées** au démarrage (pas de
  fantômes qui s'accumulent après un `/valoriatycoon reload` ou une suppression de générateur) ;
- une ligne illisible est ignorée et signalée dans le log, jamais fatale.

## Comment c'est fait (pour intervenir sans casse)

| fichier | rôle |
| --- | --- |
| `hologram/HoloEasy.java` | façade : le bytecode livré de `ValoriaTycoon` appelle `startInteractivePool(Plugin,double,float,float)` — **cette signature ne doit pas bouger** |
| `hologram/HologramPool.java` | registre, rendu, adoption des entités existantes, purge ; `registerHolograms`, `get`, `remove` sont appelés en dur par les classes précompilées |
| `hologram/HologramBuilder.java` | `hologram(location, bloc)` + `textline(texte, …)` + `item(stack)`, appels statiques issus du bloc lambda du plugin |
| `hologram/Hologram.java` | l'objet ; `getId()` fournit l'UUID que `block_data.json` persiste sous `hologramId` |
| `hologram/HologramStore.java` | lecture/écriture de `holograms.txt` |
| `utils/HologramsUtil.java` | l'API utilisée par le reste du plugin (3 méthodes, signatures figées) |

Deux règles à retenir si tu modifies ce paquet :

1. **Ne jamais changer une signature publique** listée ci-dessus : le plugin est recompilé classe
   par classe, les appelants déjà compilés casseraient en `NoSuchMethodError` au premier clic.
   `scripts/check-sources-java.mjs` refuse justement ces dérives.
2. **Ne jamais laisser une exception sortir** du rendu d'une ligne : un hologramme raté doit être
   un avertissement dans le log, pas un générateur qui ne fonctionne plus. C'est pourquoi les
   réglages apparus à des versions précises du serveur (`setSilent`, `setPersistent`,
   `setTicksFrozen`, `setCanTick`) sont appliqués par réflexion tolérante.

## Pourquoi les réglages d'entités passent par la réflexion

`setSilent` (1.9), `setInvulnerable` (1.9), `setPersistent` (1.11), `setTicksFrozen` (1.19),
`setCanTick` (extension Paper), `setRemoveWhenFarAway` (côté `LivingEntity` uniquement) : citer l'une
de ces méthodes **en dur** dans une source transforme son absence en **erreur de compilation**, pas en
absence de réglage. C'est exactement ce qu'a révélé le build `#33154898463` (`cannot find symbol` sur
`setRemoveWhenFarAway`). Le dépôt applique donc la règle :

- appels directs réservés aux méthodes stables sur toute la plage visée : `setInvisible`, `setGravity`,
  `setBasePlate`, `setArms`, `setCustomName`, `setCustomNameVisible`, `setDisabledSlots`, `setItemInHand`,
  `spawnEntity`, `getEntitiesByClass` ;
- tout le reste passe par `HoloEasy.optional(entity, "setXxx", true)` (réflexion, `false` si la méthode
  n'existe pas) ;
- `scripts/verify-source-imports.py` **interdit** les six appels fragiles en dur et lève un échec si un
  futur commit les reintroduit.

## Limites assumées (et pourquoi)

- **Tout le monde voit le hologramme** : le rendu par entités est global. Le ciblage par joueur
  exige de lire/écrire des paquets — donc ProtocolLib — ce qu'on a précisément voulu supprimer.
- **Pas de clic sur un hologramme, pas d'animations** : idem, cela reposait sur les paquets.
- **Portée d'affichage** : gérée par le serveur (les entités suivent ses règles), le
  `view-distance` de la config n'est donc que documentaire.
- **Un hologramme = 1 armure par ligne** (+ 1 pour l'item). Sur un serveur à très beaucoup de
  générateurs, ça compte dans la limite d'entités : `holograms.enabled: false` reste le levier.

## Vérifier sans serveur

```bash
python3 scripts/verify-paper26-compat.py            # le moteur est en place, aucun paquet, aucun import tiers
node scripts/check-sources-java.mjs                 # signatures publiques conformes au bytecode livré
python3 scripts/selfmade-api-patch.py --check       # plus aucune trace de l'ancienne bibliothèque
```
