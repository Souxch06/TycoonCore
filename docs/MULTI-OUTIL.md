# Multi-outil (ValoriaTools) — un item, quatre âmes

## Ce que c'est

Un **seul** item dans la main, qui se comporte comme pioche, hache, canne à pêche ou épée **selon le
bloc que tu regardes** — et dont **chaque âme s'améliore séparément** avec de l'argent (capacités
débloquées par palier). Le comportement d'inspiration hGensPickaxe, étendu à quatre outils, écrit ici :
aucun plugin à télécharger, y compris pour la monnaie.

| ce que tu vises | l'outil devient | ce que les paliers donnent |
| --- | --- | --- |
| pierre, minerai, bloc minable | pioche | `VEIN` (filon entier) → `FORTUNE` → `AUTO_SMELT` → `DOUBLE_DROP` → `SELL_ON_BREAK` |
| tronc, bois | hache | `TREE_FELL` (arbre entier) → `DOUBLE_DROP` → `SELL_ON_BREAK` |
| rien (clic dans l'eau) | canne à pêche | `AUTO_REEL` → `LUCK` → `SELL_ON_BREAK` |
| une entité vivante | épée | `CRIT` → `KNOCKBACK` → `LIFE_STEAL` |

## Installer

Le plugin est le **troisième** jar du build : `ValoriaTools-v1.6.3.jar`, à poser dans `plugins/` avec
`ValoriaTycoon-v1.6.3.jar` et `ValoriaEconomy-v1.6.3.jar`. Rien d'autre : pas de Vault, pas de
ProtocolLib, pas de plugin d'outil. `plugin.yml` ne déclare **aucun `depend:`** (un `depend:` est résolu
par nom exact et bloque tout le chargement si une seule entrée diffère — c'est le plantage historique
de ce serveur).

En jeu :

```
/tools            ouvre l'interface d'amélioration
/tools give       reçoit le multi-outil
/tools sell [all] vend ce que l'outil reconnaît
/tools stats      état des services (économie trouvée, âmes, capacités)
/tools reload     recharge la configuration (admin)
```

## Aligner les valeurs sur un barème existant (wiki gentycoon, etc.)

**Honnête d'abord** : `gentycoon.fr` et SpigotMC n'étaient **pas joignables depuis mon environnement**
(réseau bloqué, `000` sur chaque domaine), et `hGensPickaxe` n'a aucun dépôt public trouvable. Donc
**je n'ai recopié aucun chiffre** : les valeurs dans `resources-tools/config.yml` sont des **réglages
plausibles**, pas le barème de ce serveur. Tout est fait pour que la mise à jour soit un simple
remplacement de nombres, **sans retoucher le code ni recompiler** :

```yaml
tools:
  pickaxe:
    upgrade:
      max-tier: 5
      prices: [5000, 15000, 40000, 100000]   # prix du passage 1→2, 2→3, 3→4, 4→5
    abilities:
      # `from-tier` = palier d'ouverture ; les listes = UNE valeur PAR palier
      - {type: VEIN, label: "Arrachage de filon", from-tier: 1, max-blocks: [8, 12, 18, 24, 32], similar-blocks-only: true}
      - {type: FORTUNE, label: "Chance minérale", from-tier: 2, chance: [0.0, 0.15, 0.25, 0.35, 0.5], extra-min: 1, extra-max: 2}
    sell:
      prices:            # ce que rend chaque bloc, pour SELL_ON_BREAK et /tools sell
        DIAMOND: 60.0
        IRON_INGOT: 12.0
```

Règles de lecture du moteur :

- `type` est insensible à la casse et aux tirets (`DOUBLE_DROP`, `double-drop` = même capacité) ;
- les listes (`max-blocks`, `chance`, `multiplier`, `strength`, …) sont indexées sur le **palier** :
  le premier nombre s'applique au palier 1. Une liste trop courte est simplement **figée sur sa
  dernière valeur** (le palier 5 d'une liste de 3 nombres utilise le 3ᵉ) — jamais un plantage ;
- une capacité `from-tier: 6` avec `max-tier: 5` est **ramenée** au dernier palier, avec un avertissement au log ;
- `prices` doit contenir **exactement `max-tier - 1` entrées** : c'est vérifié par
  `scripts/verify-tools-config.py` (sinon un palier serait gratuit ou inatteignable) ;
- `SELL_ON_BREAK` sans grille `sell.prices` est **refusé au contrôle** : la capacité ne paierait rien ;
- une capacité que le moteur ne connaît pas fait **échouer le contrôle de config** — pas un silence.

Capacités comprises : `VEIN`, `TREE_FELL`, `AUTO_SMELT`, `FORTUNE`, `DOUBLE_DROP`,
`INFINITE_DURABILITY`, `SELL_ON_BREAK`, `AUTO_REEL`, `LUCK`, `CRIT`, `KNOCKBACK`, `LIFE_STEAL`.

## Choix de conception à connaître (et pourquoi)

1. **Les paliers sont stockés par joueur, pas dans l'item** (`tools.yml`, écriture atomique). Un palier
   porté par l'item se donne, se vend, se duplique, et se perd quand l'item casse. Le `lore` n'affiche
   que ce que le fichier dit.
2. **L'économie est vue par réflexion** (`EconomyService`) : le plugin cherche dans le `ServicesManager`
   n'importe quel objet sachant faire `getBalance`/`withdrawPlayer`/`depositPlayer`/`format`. Aucun
   import d'une API de banque → ValoriaTools démarre **aussi** si ValoriaEconomy ou Vault sont absents
   (les améliorations deviennent alors gratuites, et le log le dit).
3. **`AUTO_SMELT` passe par les recettes du serveur** (`getRecipesFor` + `FurnaceRecipe`), jamais par
   une table codée : un pack qui ajoute un minerai fondu est fondu correctement sans patch. Le prix à
   payer, honnête : pas d'XP de four (le serveur ne la doit plus quand on ne cuit pas) — d'où
   `xp-per-block` pour compenser, réglable.
4. **Pas d'`getTargetBlock`, pas de réflexions dans le privé du serveur.** Deux tentatives de ce genre
   ont été écrites ici puis supprimées pendant la conception : elles cassent au premier changement de
   version, silencieusement. Conséquence: `INSTANT_BREAK` (casser d'un clic dans le vide à distance)
   n'existe **pas**, et la portée reste celle du jeu. `LUCK` ajoute des objets de
   `tool.treasure.items` (ton choix) au lieu de rejouer la table de trésors de la pêche.
5. **Les drops sont toujours calculés par le plugin** et l'événement Bukkit est annulé : c'est le seul
   moyen qu'un filon ne double pas (ou ne perde) les items. Un `BlockBreakEvent` de trop, et un joueur
   duplique de la valeur : d'où le garde-fou de réentrance.
6. **Usure** : `tool.unbreakable: true` par défaut. Un outil qui se casse emporte l'ergonomie (il faut
   le redemander) même si les paliers, eux, restent. Si tu préfères l'usure, passe `unbreakable: false`
   et règle `durability-cost` par âme (1 par geste, même sur un filon de 30 blocs : un filon ne doit
   pas être 30 fois plus coûteux qu'un bloc).

## Limites assumées

- **Aucune capacité n'est stockée dans l'item** : deux joueurs qui ont acheté des paliers différents
  tiennent le même item — c'est voulu (l'item n'est pas un portefeuille).
- La canne ne « vise » pas de bloc : son âme s'active au lancer, donc l'auto-reel et la vente de pêche
  agissent sur l'événement de pêche, pas sur un clic.
- `TREE_FELL` descend le tronc **vers le haut** depuis le bloc frappé, puis la canopée autour du
  sommet : un arbre dont on casse la souche d'un bloc collatéral perd ses racines (elles ne sont pas
  dans le graphe de saule). Rien de cassé, juste moins spectaculaire qu'un mod de physique d'arbres.
- Les paliers ne sont **pas** sauvegardés dans un fichier de world-scoped : ils suivent le joueur sur
  tous les mondes du serveur (c'est le comportement d'une économie, pas d'une île).

## Vérifier sans serveur, ni JDK

```bash
python3 scripts/verify-tools-config.py          # config + plugin.yml + branchement du build
node scripts/parse-java.mjs sources/tools       # grammaire + types des signatures (avec java-parser)
python3 scripts/verify-source-imports.py        # imports des 32 fichiers recompilés
python3 scripts/verify-paper26-compat.py        # 97+ contrôles sur l'arbre
```

## Si ça casse en jeu

| symptôme | cause probable | où regarder |
| --- | --- | --- |
| l'outil ne casse rien | `matches.tags` vide et `blocks` mal nommé | le log dit « tags Bukkit indisponibles » ; complète `matches.blocks` |
| rien n'est vendu | `sell.prices` absent pour cette âme | `sellPrice()` du log, ou `/tools stats` |
| « aucune économie détectée » | ValoriaEconomy absent ou enregistré après | `/tools stats` affiche le fournisseur trouvé |
| paliers remis à 1 | `tools.yml` non sauvegardé (disque plein) | le log `sauvegarde de tools.yml impossible` |
