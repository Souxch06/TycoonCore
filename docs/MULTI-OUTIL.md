# Multi-outil (ValoriaTools) — un item, quatre âmes, barème du wiki GenTycoon

## Ce que c'est

Un **seul** item dans la main, qui se comporte comme pioche, hache/houe, canne à pêche ou épée **selon
le bloc que tu regardes** — et qui se améliore **capacité par capacité**, comme les enchantements custom
du serveur de référence. Aucun plugin à télécharger, y compris pour la monnaie.

Le barème (noms, descriptions, verrous, niveaux maximaux) est celui du wiki **GenTycoon**, récupéré le
2026-08-28 sur `https://wiki.gentycoon.fr/progression-metiers-and-outils/les-outils` et recopié à
l'identique dans `docs/WIKI-GENTYCOON-OUTILS.md`. Le site principal est en maintenance « V2 » ; seule la
version Markdown du wiki (`<url>.md`, index `/llms.txt`) répond.

| âme | ce qu'elle reconnaît | capacités du wiki | + propres à Valoria |
| --- | --- | --- | --- |
| pioche | `#minecraft:mineable/pickaxe` | 22 (Onde sismique → Fortune → Briseur → Surcharge…) | `AUTO_SMELT`, vente à la casse |
| hache / houe | logs, `mineable/axe`, 13 blocs de cultures | 18 (Main de Gaïa, Furie, Vitesse des âmes, Jugement divin…) | `TREE_FELL`, récolte+replantation, vente |
| épée | entités vivantes | 18 (Tranchant, Autoclicker, Briseur de monstres, Force…) | critique, vol de vie, recul |
| canne | le lancer (aucun bloc) | 14 (Angler, Tsunami, Proc booster, Mains dorées…) | moulinet, vente de la pêche |

## Les deux compteurs

1. **le palier d'âme** (1 → 50) : il **autorise** les capacités, comme le « Level minimum » / « Prestige
   mini » du wiki. Achat dans la case centrale du haut du menu ; prix `price-base × price-ratio^palier`.
2. **le niveau de capacité** (0 → `max-level`, jusqu'à 2000 pour les « Pouch ») : c'est le « Niveau max
   d'enchantement » du wiki. Une case = une capacité ; **clic = +1 niveau**, **Maj+clic = +10**.

Un niveau 0 = capacité non achetée = aucun effet. Quatre capacités sont `free: true` (vitesse de
minage, abattage, récolte, vente) pour que l'outil serve dès le palier 1 — c'est le seul écart assumé
au modèle « tout s'achète ».

## Le menu

Ouverture : **clic droit** avec la pioche ou la houe (dans le vide), **sneak + clic droit** avec l'épée
ou la canne — la convention du wiki, page « Les Outils ».

```
rangée 1 :  [pioche] [hache] [canne] [épée]   .   [palier ↑]  [vendre]  [stats]  [fermer]
rangées 2-4 : une case par capacité du wiki, dans l'ordre du fichier de config
```

La tooltip d'une capacité donne : sa description du wiki, son noyau, `niveau actuel / max`, l'effet au
niveau courant et au niveau suivant, le prix, et le palier requis si elle est verrouillée.

## Commandes

```
/tools [âme]                               ouvre le menu (eventuellement sur une âme)
/tools buy                                 achète l'outil au prix de `tool.price`
/tools give [joueur] [palier]              reçoit l'outil (administration)
/tools sell [all]                          vend ce que l'outil reconnaît
/tools top [mesure] [âme] [n]              classement : blocs, cultures, arbres, poissons, kills, argent, niveaux
/tools aide <capacité>                     fiche d'une capacité (nom du wiki, id ou noyau)
/tools set <joueur> <âme> [palier]         voir / forcer un palier          (admin)
/tools ability <joueur> <âme> <capacité> [niveau]   régler une capacité      (admin)
/tools reset <joueur> [âme]                remettre une âme à zéro           (admin)
/tools stats                               état des services
/tools reload                              recharge config + paliers + vues  (admin)
```

`/tools ability` accepte l'**id** (`fortune`), le **nom du wiki** (`Fortune`) ou le **noyau** (`FORTUNE`) :
la complétion par Tab propose les ids de l'âme choisie. Le niveau est borné par `max-level`, donc un
réglage hors barème ne peut pas rendre une capacité plus forte que ce que le fichier déclare.

## Comment une capacité du wiki devient un effet

Tout est dans `resources-tools/config.yml`, régénéré par `python3 scripts/gen-tools-config.py` (la table
du wiki y est la seule source). Le nom de **noyau** (`type:`) est la seule chose que le Java connaît :

| noyau | effet | capacités du wiki qui l'utilisent |
| --- | --- | --- |
| `VEIN` | casse le filon entier, au tour de dé près | Briseur |
| `AREA_BREAK` | cube (ou plan si `flat: true`) autour du bloc, filtré `ores-only` / cultures | Onde sismique, Explosive, Surcharge, Main de Gaïa, Jugement divin |
| `EXTRA_BLOCK` | N blocs identiques collés au bloc visé | Seconde main |
| `GHOST_MINES` | vagues différées qui minent autour (1 tâche toutes les `interval` ticks) | Pioche fantomatique |
| `TREE_FELL` / `CROP_HARVEST` | tronc+canopée / récolte des cultures mûres + replantation | Arbre, Récolte automatique |
| `HASTE` / `SWIFT` / `SOUL_SPEED` | vitesse de minage (effet court re-posé à chaque bloc), vitesse de fuite, marche sur sable des âmes avec **rendu de la vitesse d'origine** | Efficacité, Speed, Célérité, Vitesse des âmes |
| `FORTUNE` / `DOUBLE_DROP` / `AUTO_SMELT` | drops additionnels, cuisson via les recettes du serveur | Fortune, Pillage, Bonus de rendement, Auto-smelt |
| `MONEY_MULT` / `MONEY_DOUBLE` / `MONEY_POUCH` / `FURY` | pourcentage, chance de ×2, cagnotte, mode temporaires | Braquage, Main dorée, Double gain, les cinq « Pouch », Furie |
| `XP_MULT` / `XP_FLAT` | multiplicateur et lot d'XP (`giveExp`, jamais d'orbe) | Booster d'xp, Chercheur d'xp |
| `TREASURE` | objet du réservoir **déclaré par la capacité** (`items: [...]`) | Trouvaille, Chercheur de spawner/bonbons/crédits, Casino |
| `RANDOM_ENCHANT` | un enchantement réel posé sur l'item, dans la liste `enchants:` | Charognard |
| `PROC_BOOSTER` | multiplie la chance de **toutes** les autres capacités de l'âme | Proc booster |
| `DAMAGE_MULT` / `CRIT` / `LIFE_STEAL` / `KNOCKBACK` / `POTION_APPLY` / `AUTO_SWING` / `MULTI_KILL` | combat ; les gains de butin sont calculés **à la mort** (`EntityDeathEvent`), pas au coup | Tranchant, Force, Autoclicker, Briseur de monstres, Pillage |
| `AUTO_REEL` / `FAST_REEL` / `MULTI_CATCH` / `LUCK` | la prise va au sac sans mouliner ; temps d'attente du bobber raccourci (`setWaitTime` par réflexion sur l'API publique, Paper seulement) ; rafales de prises ; trésors | Moulinet rapide, Angler, Tsunami, Pêche chanceuse |
| `SELL_ON_BREAK` | vend au prix de `sell.prices` | la « vente automatique » que le wiki suppose côté serveur |

Règles communes, appliquées par `ToolsConfig.Effect` et non par chaque noyau :

- **plusieurs capacités, un effet** : Efficacité + Speed + Célérité additionnent leur amplifier, plafonné
  à 5 (au-delà, le client mine plus vite que le serveur n'accepte) ;
- **les chances ne s'additionnent pas** : `1 − Π(1 − c)`, plafonné à 95 % ; le Proc booster multiplie `c`
  avant combinaison, jamais après ;
- **un tour de dé par geste**, pas par bloc : sinon un « Briseur 12 % » sur un filon de 20 blocs
  déclencherait presque à chaque clic ;
- **budgets** : rayon ≤ 5, blocs ≤ 256 par geste, vagues ≤ 8 — un barème de wiki avec `max-level: 2000`
  ne doit jamais pouvoir écrire un tick de 20 000 blocs.

## Ce qui n'est pas fait, et pourquoi

- `INSTANT_BREAK` (casser n'importe quel bloc au contact) : dépend du raycast client et de la capacité
  `INSTANT_BREAK` du joueur, tous deux redessinés selon les versions. Le plugin ne promet rien plutôt que
  de promettre un comportement incertain.
- MineCoins / FarmCoins / MobCoins / FishCoins / crédits / clés / spawners / générateurs / bonbons de
  pets : **Valoria n'a pas ces monnaies ni ces systèmes**. Les capacités sont quand même là, nommées et
  tarifées comme au wiki, mais récompensent de l'argent ou un objet que **tu** déclares (`items:`). Le
  tableau de correspondance est dans `docs/WIKI-GENTYCOON-OUTILS.md`.
- Les **prix** et les **valeurs d'effet** : le wiki ne les publie pas (ses pages renvoient à des captures
  d'écran, sans chiffres). Ces nombres-là sont des réglages Valoria, éditables dans `config.yml`.

## D'où vient chaque nombre

| ce que c'est | source | où le changer |
| --- | --- | --- |
| noms, descriptions, verrous (`unlock`), niveaux max (`max-level`) | **wiki GenTycoon**, pages Les Outils | `scripts/gen-tools-config.py` → `config.yml` |
| argent et XP par bloc / poisson / monstre (`jobs.gains`, `jobs.xp`) | **wiki GenTycoon**, pages Les Métiers (Mineur, Fermier, Chasseur, Pêcheur) | idem, table `JOBS` |
| prix d'un niveau de capacité, prix d'un palier, chances par niveau | **réglages Valoria** : le wiki ne les publie pas (son propre assistant confirme : « aucune information de prix / coût par niveau d'enchantement dans les docs accessibles ») | `ability-price`, `upgrade.price-*`, `chance`/`*-step` |

Le contrôle de config refuse toute valeur de `jobs.*` absente des tableaux recopiés dans
`docs/WIKI-GENTYCOON-OUTILS.md` : un prix inventé ne peut pas se faire passer pour « le barème du serveur ».

## Ce que l'outil mesure (`stats.yml`)

| mesure | quand elle compte | à quoi elle sert |
| --- | --- | --- |
| `blocks` | chaque bloc cassé par l'outil (filon, zone, arbre compris) | `/tools top blocs`, équilibrage |
| `crops` | chaque culture récoltée (mûre, sinon rien n'est cassé) | classement Fermier |
| `trees` | **un par abattage**, pas par bloc de tronc | classement Bûcheron |
| `fish` | une par prise (le bonus de Tsunami est payé, pas compté comme prise de plus) | classement Pêcheur |
| `kills` | une par mort causée avec l'âme épée | classement Chasseur |
| `money` | l'argent réellement crédité (vente à la casse, pochettes, butins, métier) | ce qui intéresse le tycoon |
| `levels` | les niveaux de capacité payés en jeu | « qui a tout maxé » |

Les compteurs sont incrémentés **au même endroit que le paiement** (dans `ToolListener`, événement déjà
annulé) : un drop compté ailleurs serait compté deux fois, ou pas du compte après un reload. Un joueur à
zéro n'a pas de section dans le fichier, et `stats.enabled: false` désactive toute la mesure sans toucher
au reste — le classement n'est qu'un affichage, jamais une condition de fonctionnement.

## Acheter l'outil, et où il a le droit d'agir

- `tool.price` (0 par défaut) : `/tools buy` retire ce montant et donne l'outil. À 0, la commande devient
  un give — un serveur qui n'a pas encore calé son économie ne doit pas être bloqué par notre tarif, mais
  il ne doit pas non plus être forcé à distribuer l'outil : c'est `buy` qui est ouvert aux joueurs,
  `give` reste l'outil de l'administration.
- `tools.allowed-worlds` (vide = tous) : hors liste, l'outil ne casse rien, ne pêche rien, ne tue rien,
  ne paie rien. C'est la porte « zone protégée » du plugin, en API Bukkit seule.

`plugin.yml` ne déclare plus que deux `softdepend` (ValoriaEconomy, ValoriaTycoon) : PlaceholderAPI,
IridiumSkyblock, SuperiorSkyblock2 et BentoBox y figuraient **sans une seule ligne de code qui les
utilise**. Une dépendance déclarée sans appel fait croire à l'admin à une intégration qui n'existe pas — c'est
retiré plutôt que commenté.

## Contrôles automatiques

```
python3 scripts/verify-tools-config.py     100 contrôles : config, noyaux, plugin.yml, pom, assemblage
python3 scripts/check-config-literals.py   les clés de config appelées en Java sont des littéraux
node scripts/parse-java.mjs --from-pom     syntaxe + types des 32 fichiers compilés
```

Le contrôle de config ne se contente pas de compter les lignes :

- il relit les **tableaux du wiki** dans `docs/WIKI-GENTYCOON-OUTILS.md` (72 lignes) et exige que chaque
  capacité existe dans la bonne âme, **avec le même `max-level`, le même verrou, le même noyau** ;
- il refuse un **YAML malformé de forme** (liste et clés au même niveau) que ni `javac` ni Maven ne
  voient, et que SnakeYAML paie d'un plugin qui ne s'active pas ;
- ses autof-tests coupent l'herbe sous le pied du contrôle décoratif : si le parseur voit 0 capacité sur
  88 déclarées, ou 60 capacités au lieu des 72 du wiki, le script **échoue** au lieu de valider.

Un contrôle qui ne lit rien est pire qu'un contrôle absent : c'est exactement ce qui s'est produit au
premier essai sur ce fichier (le marqueur `{type:` ne existait plus, le compteur voyait 0 = « tout bon »).

## Tester en jeu (10 minutes)

1. `/tools give` puis `/tools` : les quatre icônes d'âmes répondent, la pioche est sélectionnée, les
   cases de capacités affichent les 24 lignes du barème pioche.
2. Miner une pierre : `Vente à la casse` + `Efficacité` sont offertes au palier 1 → l'argent tombe, la
   vitesse monte. Aucun drop doublé, pas de particules fantômes.
3. `/tools set moi pioche 12` puis `/tools ability moi pioche fortune 5` : la case Fortune passe à
   niveau 5, l'effet s'affiche dans la tooltip, le minerai rapporte plus.
4. `/tools set moi pioche 46` : Surcharge (verrou 45) devient achetable ; Onde sismique et Briseur
   cassent plusieurs blocs d'un clic, avec **une seule** usure (ou aucune si `tool.unbreakable: true`).
5. Hache sur un arbre : tronc + canopée. Sur du blé mûr : la 3×3 est récoltée **et replantée** ; sur du
   blé vert, **rien ne casse** (le plant repousse, l'événement n'est pas annulé).
6. Épée sur un mob : dégâts, `Chercheur d'xp`, et `/tools ability moi épée vente-butin 1` pour voir les
   drops vendus à la mort. Un mob qui survit au coup ne rapporte rien (volontaire).
7. Canne : lancer normal, puis niveau 1 d'`Angler` (attente raccourcie sur Paper) et de `Moulinet rapide`
   (poisson directement dans le sac, sans clic de moulinet).
8. `/tools ability moi pioche briseur 0` : la capacité se **désactive** — le palier d'âme ne doit rien
   rendre d'irrémovable.
9. `/tools reload` avec le menu ouvert : la vue se redessine, aucune case cliquable ne reste d'un ancien
   barème.
10. Redémarrer le serveur : `/tools stats` et les niveaux relus depuis `tools.yml` sont intacts, y compris
    le format ancien (`pickaxe: 12`, sans bloc `abilities:`).

## Rollback

`/tools reset <joueur>` remet une âme à zéro ; désinstaller = retirer `ValoriaTools-v1.6.3.jar` des
`plugins/` et redémarrer. `tools.yml` reste dans `plugins/ValoriaTools/` (à supprimer pour repartir de
zéro) : aucune donnée n'est écrite dans les items ni dans le monde.
