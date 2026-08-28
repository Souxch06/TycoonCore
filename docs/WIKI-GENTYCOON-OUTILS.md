# Barème du wiki GenTycoon — section « Les Outils »

Source : `https://wiki.gentycoon.fr/progression-metiers-and-outils/les-outils` (et ses quatre
sous-pages), récupérée le 2026-08-28 en Markdown (`<url>.md`, index : `/llms.txt`). Le site
principal (`gentycoon.fr`) est en maintenance « V2 » ; seul le wiki répond.

Ce fichier est **la référence** de `resources-tools/config.yml` : les noms, les descriptions, les
niveaux maximaux et les verrous d'accès sont recopiés du wiki. Les *valeurs d'effet* (chance par
niveau, multiplicateur, portée) et les *prix* ne sont **pas** sur le wiki — les pages renvoient à
des captures d'écran (`/files/…`) sans chiffres. Ces nombres-là restent donc des réglages Valoria,
documentés plus bas.

## Règle d'accès au menu (wiki, page « Les Outils »)

> « Pour accéder au menu, il vous suffit de faire un clic droit avec la pioche, la houe ; sneak +
> clic droit avec l'épée et la canne à pêche. »

Implémenté dans `ToolListener#onInteract` : les âmes « mine » (pioche, hache/houe) ouvrent le menu
au clic droit dans le vide ou sur un bloc non reconnu ; les âmes de combat/pêche ne l'ouvrent
qu'accroupi, pour ne pas voler le lancer de ligne ni le coup d'épée.

## Liste des outils (wiki, page « Les Outils »)

| Outil | Fonctionnalités annoncées |
|---|---|
| Houe | Récolte automatique, bonus de culture, amélioration de rendement |
| Pioche | Minage rapide, fortune, auto-smelt, boost d'XP |
| Épée | Dégâts augmentés, loots améliorés, effets spéciaux en combat |
| Canne à pêche | Pêche rapide, chances de trésors, bonus de métier pêcheur |

Chaque âme de ValoriaTools porte le barème de la page correspondante. **Notre âme « hache » reçoit
celle de la « houe »** : le wiki de GenTycoon ne décrit pas de hache, et l'âme coupe-bois de
l'outil est aussi celle qui récolte les cultures (les deux sont dans
`tools.axe.matches`, donc les capacités de zone et de récolte s'appliquent aux troncs comme aux
cultures). Les capacités d'abattage (`TREE_FELL`) et de vente (`SELL_ON_BREAK`) sont propres à
Valoria : elles sont signalées comme telles dans `config.yml`.

## Pioche — 22 améliorations

Colonnes du wiki : *Enchantement*, *Description*, *Prestige minimum*, *Level minimum*,
*Niveau max d'enchantement*. Une colonne vide = aucune exigence.

| Enchantement | Description | Prestige min | Level min | Max | Noyau ValoriaTools |
|---|---|---|---|---|---|
| Onde sismique | Chance de provoquer un séisme qui casse tous les minerais à proximité | | 40 | 300 | `AREA_BREAK` (minerais seulement) |
| Efficacité | Augmente l'efficacité de la pioche, casse les minerais plus rapidement | | | 10 | `HASTE` |
| Fortune | Augmente la quantité de minerais récupérée | | 10 | 10 | `FORTUNE` |
| Speed | Boost de vitesse en cassant des minerais | | | 5 | `HASTE` |
| Célérité | Augmente la vitesse de minage | | | 5 | `HASTE` |
| Chercheur d'xp | Chance de trouver une quantité d'XP de pioche | | | 1000 | `XP_FLAT` |
| Trouvaille | Chance de trouver une clé boost | | 15 | 1000 | `TREASURE` (objets configurables) |
| Double gain | Chance de doubler l'argent gagné par la vente automatique | | 25 | 1000 | `MONEY_DOUBLE` |
| Money Pouch | Chance de trouver une grande quantité d'argent | | 20 | 2000 | `MONEY_POUCH` |
| MineCoins Pouch | Chance de trouver une grande quantité de MineCoins | | 20 | 2000 | `MONEY_POUCH` |
| Chercheur de spawner | Chance de trouver des spawners | 2 | | 1000 | `TREASURE` |
| Booster d'xp | Augmente l'expérience gagnée sur la pioche | | | 1000 | `XP_MULT` |
| Proc booster | Augmente le taux de déclenchement des enchantements | | 40 | 500 | `PROC_BOOSTER` |
| Main dorée | Augmente les MineCoins gagnés en cassant | | | 2000 | `MONEY_MULT` |
| Braquage | Augmente l'argent gagné en cassant | | | 1000 | `MONEY_MULT` |
| Charognard | Chance de trouver un enchantement aléatoire pour la pioche | | 5 | 500 | `RANDOM_ENCHANT` |
| Explosive | Chance de créer une explosion qui casse les minerais en 3×3 | | 25 | 10 | `AREA_BREAK` |
| Seconde main | Chance de casser un minerai supplémentaire | | 20 | 100 | `EXTRA_BLOCK` |
| Briseur | Chance de casser tout le filon | | 30 | 300 | `VEIN` |
| Pioche fantomatique | Chance de faire apparaître des pioches fantômes qui minent seules | | 35 | 300 | `GHOST_MINES` |
| Surcharge | Chance d'envoyer une onde de choc cassant les minerais sur ton passage | | 45 | 300 | `AREA_BREAK` (grande portée) |
| Chercheur de crédits | Chance de trouver des crédits | | 45 | 1000 | `MONEY_POUCH` |

Fonctionnalités de tête de page, reprises aussi : *auto-smelt* → `AUTO_SMELT`, *bonus de rendement
et d'XP* → `FORTUNE` + `XP_MULT`, *minage rapide et fluide* → `HASTE`.

## Houe — 18 améliorations (barème de notre âme « hache »)

Colonnes du wiki : *Enchantement*, *Description*, *Prestige mini*, *Niveau max*. Ce tableau n'a pas
de colonne « Level minimum » : seul le prestige verrouille.

| Enchantement | Description | Prestige min | Max | Noyau ValoriaTools |
|---|---|---|---|---|
| Main de Gaïa | Chance de casser les cultures en 3×3 | 3 | 300 | `AREA_BREAK` (cultures seules) + `CROP_HARVEST` |
| Speed | Boost de vitesse en cassant des cultures | 0 | 5 | `HASTE` |
| Célérité | Augmente la vitesse de frappe de la houe | 0 | 5 | `HASTE` |
| Main dorée | Augmente les FarmCoins gagnés | 0 | 2000 | `MONEY_MULT` |
| Trouvaille | Chance de trouver une clé farm | 0 | 1000 | `TREASURE` |
| Boost XP | Augmente l'expérience gagnée sur la houe | 0 | 1000 | `XP_MULT` |
| Braquage | Augmente l'argent gagné en cassant des cultures | 0 | 1000 | `MONEY_MULT` |
| Casino | Chance de trouver des FarmGen | 0 | 100 | `TREASURE` |
| Farmcoins Pouch | Chance de gagner beaucoup de FarmCoins | 0 | 2000 | `MONEY_POUCH` |
| Money Pouch | Chance de gagner beaucoup d'argent | 0 | 2000 | `MONEY_POUCH` |
| Double gain | Chance de doubler l'argent de la vente automatique | 0 | 1000 | `MONEY_DOUBLE` |
| Furie | Chance d'activer un mode Furie (gains augmentés) | 1 | 50 | `FURY` |
| Proc Booster | Augmente le taux de déclenchement des enchantements | 15 | 500 | `PROC_BOOSTER` |
| Vitesse des âmes | Marcher plus vite sur le sable des âmes | 5 | 3 | `SOUL_SPEED` |
| Chercheur de spawner | Chance de trouver des spawners | 2 | 1000 | `TREASURE` |
| Jugement divin | Chance d'activer le jugement divin | 10 | 1000 | `AREA_BREAK` (portée maximale) |
| Chercheur de bonbon | Chance de trouver des bonbons d'XP de pets | 5 | 1000 | `TREASURE` |
| Chercheur de crédits | Chance de trouver des crédits | 20 | 1000 | `MONEY_POUCH` |

Fonctionnalités de tête de page : *récolte automatique* → `CROP_HARVEST`, *replantation
instantanée* → l'option `harvest.replant`, *bonus de rendement* → `DOUBLE_DROP`.

## Épée — 18 améliorations

| Enchantement | Description | Prestige min | Level min | Max | Noyau ValoriaTools |
|---|---|---|---|---|---|
| Booster d'xp | Augmente l'XP gagnée en tuant des monstres | | | 1000 | `XP_MULT` |
| Booster MobCoins | Augmente les MobCoins gagnés | | | 2000 | `MONEY_MULT` |
| Trouvaille | Chance de trouver une clé commune | | 10 | 1000 | `TREASURE` |
| Casino | Chance de trouver un générateur | 8 | | 100 | `TREASURE` |
| Briseur de monstres | Chance de tuer beaucoup de monstres d'un coup | 10 | | 20 | `MULTI_KILL` |
| Speed | Boost de vitesse en tuant des monstres | | | 5 | `SWIFT` |
| Célérité | Augmente la vitesse de frappe | | | 5 | `HASTE` |
| Chercheur d'xp | Chance de trouver une quantité d'XP | | | 500 | `XP_FLAT` |
| Chercheur de spawner | Chance de trouver des spawners | 2 | | 1000 | `TREASURE` |
| Proc booster | Augmente le taux de déclenchement | 8 | | 500 | `PROC_BOOSTER` |
| Tranchant | Augmente les dégâts de l'épée | | | 5 | `DAMAGE_MULT` |
| Chercheur de bonbons | Chance de trouver des bonbons de pets | 5 | | 1000 | `TREASURE` |
| Money Pouch | Chance de trouver beaucoup d'argent | | 20 | 2000 | `MONEY_POUCH` |
| Pillage | Chance d'augmenter le butin obtenu | 2 | | 8 | `DOUBLE_DROP` |
| MobCoins Pouch | Chance de trouver beaucoup de MobCoins | | 5 | 2000 | `MONEY_POUCH` |
| Autoclicker | Permet de tuer des monstres automatiquement | 10 | | 500 | `AUTO_SWING` |
| Force | Chance d'obtenir l'effet Force | | | 3 | `POTION_APPLY` |
| Braquage | Augmente l'argent gagné en tuant | | | 1000 | `MONEY_MULT` |

## Canne à pêche — 14 améliorations

| Enchantement | Description | Prestige min | Level min | Max | Noyau ValoriaTools |
|---|---|---|---|---|---|
| Boost d'xp | Augmente l'XP gagnée en pêchant | | | 1000 | `XP_MULT` |
| Main dorée | Augmente les FishCoins gagnés | | 10 | 2000 | `MONEY_MULT` |
| Braquage | Augmente l'argent gagné en pêchant | | | 1000 | `MONEY_MULT` |
| Chercheur de spawner | Chance de trouver des spawners | | 25 | 1000 | `TREASURE` |
| Trouvaille | Chance de trouver une clé boost, farm ou commune | | 20 | 1000 | `TREASURE` |
| Angler | Réduit le temps de pêche | | | 10 | `FAST_REEL` |
| Chercheur de bonbons | Chance de trouver des bonbons de pets | | | 1000 | `TREASURE` |
| Proc booster | Augmente le taux de déclenchement | | 15 | 500 | `PROC_BOOSTER` |
| Money Pouch | Chance de trouver beaucoup d'argent | | | 2000 | `MONEY_POUCH` |
| Casino | Chance de trouver un générateur | | | 100 | `TREASURE` |
| Chercheur d'xp | Augmente le gain d'XP vanilla | | 3 | 500 | `XP_FLAT` |
| Tsunami | Chance d'invoquer un tsunami | | 10 | 1000 | `MULTI_CATCH` |
| FishCoins Pouch | Chance de trouver beaucoup de FishCoins | | | 2000 | `MONEY_POUCH` |
| Chercheur de crédits | Chance de trouver des crédits | | 30 | 1000 | `MONEY_POUCH` |

Fonctionnalités de tête de page : *pêche plus rapide* → `AUTO_REEL` (la prise va directement dans
l'inventaire, sans mouliner), *chances de trésors* → `LUCK` + `TREASURE`.

## Ce qui n'est PAS transposé tel quel, et pourquoi

Le serveur GenTycoon possède des monnaies et des systèmes que ValoriaTycoon n'a pas. Plutôt que
d'inventer un faux système, chaque capacité de ce type est **mappée** sur l'équivalent réel de
Valoria, et le lien exact se règle dans `config.yml` :

| Ce que le wiki donne | Ce que ValoriaTools donne | Clé de config à changer |
|---|---|---|
| MineCoins / FarmCoins / MobCoins / FishCoins | de l'argent (la seule monnaie du plugin de base) | `amount`, `multiplier` |
| Crédits | de l'argent, à part du gain de bloc | `amount` |
| Clés (boost, farm, commune) | un objet du réservoir de trésors | `items:` de la capacité |
| Spawners | l'objet `SPAWNER`, si l'admin le laisse dans `items:` | `items:` |
| Générateurs (Casino) | l'objet déclaré (coffre, item de ton plugin de générateurs) | `items:` |
| Bonbons d'XP de pets | l'objet déclaré (sucre par défaut) | `items:` |
| XP de métier (Jobs) | l'XP du palier d'âme + l'XP vanilla du joueur | `xp-per-block`, `amount` |
| Prestige | le palier d'âme (`unlock:`) | `tools.<âme>.abilities[].unlock` |
| Level minimum | le palier d'âme (`unlock:`) | `tools.<âme>.abilities[].unlock` |
| Niveau max d'enchantement | le niveau max de la capacité, acheté à l'unité dans le menu | `max-level` |

Une capacité du wiki sans équivalent honnête côté Bukkit n'est **pas** simulée : `INSTANT_BREAK`
(casser n'importe quel bloc instantanément) demande un raycast et la capacité `INSTANT_BREAK` du
joueur, et n'est donc pas exposée — le choix est documenté plutôt que bricolé.

## Gains du métier (chiffres réels, pas des réglages)

Les pages « Les Métiers » du wiki publient ce que **le bloc cassé** (et non l'objet qui en tombe)
rapporte en argent et en XP. Ces nombres-là sont recopiés **tels quels** dans `resources-tools/config.yml`
(section `jobs:` de chaque âme), et contrôlés par `scripts/verify-tools-config.py`.

| Métier | Ce que la clé désigne | Page |
| --- | --- | --- |
| Mineur (pioche) | matériau du bloc cassé — `STONE`, `DIAMOND_ORE`, `ANCIENT_DEBRIS` | `/les-metiers/mineur.md` |
| Fermier (hache/houe) | plante récoltée — `WHEAT`, `CARROTS`, `CACTUS` | `/les-metiers/fermier.md` |
| Chasseur (épée) | type d'entité tuée — `ZOMBIE`, `GHAST`, `WITHER` | `/les-metiers/chasseur.md` |
| Pêcheur (canne) | objet pêché — `COD`, `TROPICAL_FISH` | `/les-metiers/pecheur.md` |

### Mineur — casser (💰 / XP 📈)

Roche 0.05/0.01 · Andésite 0.07/0.75 · Granite 0.07/0.25 · Diorite 0.07/0.01 · Grès sculpté
0.45/0.75 · Grès taillé 0.45/0.75 · Charbon 0.50/1.25 · Cuivre 0.50/1.25 · Redstone 0.50/2.25 ·
Fer 0.88/1.75 · Or 2.00/2.00 · Lapis 2.00/2.13 · Diamant 3.00/4.13 · Émeraude 5.00/5.13 ·
Quartz du Nether 0.10/0.01 · Obsidienne 2.00/2.63 · Débris antiques 4.00/4.00.

Deux libertés assumées, signalées ici et nulle part ailleurs :

- les **déclinaisons profondes** (`DEEPSLATE_*`) reprennent le tarif de la version pierre — le wiki ne
  les liste pas séparément, et un joueur qui creuse en dessous ne doit pas être payé moins ;
- le wiki **punit la repose** d'un minerai (−0.50 charbon, −5.00 émeraude). Le multi-outil ne *pose*
  jamais de minerai (seule la replantation d'une culture écrit un bloc, et elle passe par
  `setType`, sans événement de pose) : la pénalité n'a donc pas d'objet ici, et n'est pas simulée.

### Fermier — récolter

Blé 7.65/5.10 · Carottes 7.95/5.25 · Pommes de terre 7.95/5.25 · Betteraves 8.10/5.40 · Citrouille
8.10/5.40 · Canne à sucre 7.80/5.20 · Cacao 7.80/5.20 · Champignon marron 8.25/5.40 · Champignon rouge
8.25/0.40 · Bambou 7.73/5.20 · Cactus 7.80/5.20 · Fleurs (tulipes, marguerite, tournesol) 7.65/5.20 ·
Pissenlit/Coquelicot 0.08/5.10.

### Chasseur — tuer

Poulet 0.33/0.35 · Vache 0.35/0.38 · Cochon 0.50/0.30 · Mouton 0.35/0.38 · Lapin 0.50/0.30 · Loup
2.50/0.50 · Mooshroom 0.35/0.38 · Poulpe 0.50/0.50 · Creeper 0.50/1.25 · Squelette 0.50/0.65 · Zombie
0.50/0.35 · Noyé 0.38/0.38 · Husk 0.38/0.38 · Araignée 0.50/0.80 · Araignée venimeuse 0.50/1.75 ·
Magma Cube 0.50/0.65 · Blaze 0.50/1.15 · Enderman 0.50/2.18 · Piglin zombifié 0.50/0.50 · Piglin
1.00/2.50 · Silverfish 0.50/0.50 · Guardian 2.00/4.00 · Vindicator 0.50/2.18 · Evoker 0.50/2.18 ·
Pillager 0.50/2.18 · Sorcière 0.75/4.50 · Wither squelette 1.50/2.18 · Golem de fer 4.00/4.50 · Zoglin
4.00/4.00 · Ghast 10.00/10.00 · Wither 50.00/60.00.

### Pêcheur — pêcher

Morue 1.50/1.50 · Morue cuite 1.50/1.50 · Saumon 1.50/1.50 · Poisson tropical 3.00/3.00 · Poisson-globe
3.00/3.00.

### Ce qui reste réglé par Valoria

Les **prix des niveaux de capacité** et les **chances par niveau** : le wiki ne les publie pas (réponse
de son propre assistant documentaire : « aucune information de prix / coût par niveau d'enchantement
dans les docs accessibles »). Ces deux-là restent des réglages dans `config.yml`, éditables sans
recompiler.

## Les nombres qui viennent de nous

Chance par niveau, multiplicateurs, portée des zones, durées et prix : le wiki ne les publie pas
(chiffres introuvables dans le texte des pages). Ils sont donc réglés par Valoria, dans un ordre
d'idée simple et vérifiable :

- une chance par niveau ≈ `chance` + `chance-step` × (niveau − 1), plafonnée à 90 % ;
- les niveaux max énormes du wiki (1000, 2000) ne sont atteignables que sur un très long terme :
  le prix croît en linéaire (`price` + `price-step` × niveau), plafonné par `price-cap` ;
- un palier d'âme coûte `price-base` × `price-ratio`^palier, plafonné par `price-cap` — c'est ce
  qui permet d'ouvrir les capacités verrouillées « level 45 » sans écrire 49 lignes de prix.

Si tu récupères les vraies tables de prix du serveur (capture du menu d'enchantement d'outil, ou
texte du Discord), tout se corrige dans `resources-tools/config.yml` : aucun recompilage nécessaire,
`/tools reload` suffit.
