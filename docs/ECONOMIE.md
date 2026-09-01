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

## Le comptoir (/shop) : la sortie d'argent

Jusqu'ici le serveur ne faisait qu'**imprimer** de l'argent : un générateur produit un objet, `/sell` le
paie au `sellPrice` de sa ligne, le marché entre joueurs déplace la monnaie sans en créer. Le comptoir
fait l'autre bout — acheter la matière plus cher qu'elle ne se revend — et c'est ce qui donne un **prix de
référence** au bloc. Le prix d'une matière de générateur n'est jamais écrit à la main : c'est
`sellPrice × buy-multiplier` (1,75), et le rachat `buyback-ratio` (0,4) est calculé sur ce prix d'achat,
donc acheter puis se faire racheter laisse 30 % de perte — `/sell` reste le meilleur débouché.

### Le barème des générateurs et les rayons

<!-- bareme-comptoir:debut -->
> Bloc genere par `python3 scripts/verify-shop-economy.py --write`. Ne pas editer a la main : les
> chiffres du document sont relus depuis `resources/config.yml`, pas recopies d'une tete bien faite.

| palier | rayon | matière | rendu | `sellPrice` (via `/sell`) | achat comptoir | prix du générateur | amorti en |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 🌾 Agriculture | WHEAT | 1 toutes les 20 s | 10 $ | **17.5 $** (reprise 7 $) | 1 800 $ | 1.00 h |
| 2 | 🌾 Agriculture | MELON_SLICE | 1 toutes les 20 s | 20 $ | **35 $** (reprise 14 $) | 3 800 $ | 1.06 h |
| 3 | 💎 Minerais | COAL | 1 toutes les 20 s | 25 $ | **43.75 $** (reprise 17.5 $) | 5 000 $ | 1.11 h |
| 4 | 💎 Minerais | COAL_BLOCK | 1 toutes les 20 s | 50 $ | **87.5 $** (reprise 35 $) | 11 000 $ | 1.22 h |
| 5 | 💎 Minerais | IRON_INGOT | 1 toutes les 20 s | 75 $ | **131.25 $** (reprise 52.5 $) | 17 000 $ | 1.26 h |
| 6 | 💎 Minerais | IRON_BLOCK | 1 toutes les 20 s | 100 $ | **175 $** (reprise 70 $) | 24 000 $ | 1.33 h |
| 10 | 💎 Minerais | REDSTONE_BLOCK | 1 toutes les 20 s | 400 $ | **700 $** (reprise 280 $) | 120 000 $ | 1.67 h |
| 14 | 💎 Minerais | DIAMOND_BLOCK | 1 toutes les 20 s | 800 $ | **1 400 $** (reprise 560 $) | 300 000 $ | 2.08 h |
| 20 | 💎 Minerais | NETHERITE_INGOT | 1 toutes les 20 s | 1 400 $ | **2 450 $** (reprise 980 $) | 730 000 $ | 2.90 h |
| 28 | 🎲 Divers | DRAGON_EGG | 1 toutes les 20 s | 2 200 $ | **3 850 $** (reprise 1 540 $) | 1 780 000 $ | 4.49 h |

**La colonne « amorti en » n'est pas un constat, c'est la règle** : le prix d'un palier vaut de 1 h à 4,5 h de son propre rendement, en croissance geometrique, du premier au dernier palier de la table. La regle vit dans `scripts/verify-shop-economy.py` et `python3 scripts/verify-shop-economy.py --prices` la posee dans `resources/config.yml` ; le verificateur rouge des qu'un des deux bords sort de 0,75 h à 6 h, ou des qu'un prix du fichier ne vient plus de la regle.


**Les rayons** — ce que le comptoir presente, onglet par onglet :

| rayon | icône | matières listées | offres de générateur | offres écrites | total | pages |
| --- | --- | --- | --- | --- | --- | --- |
| 🧱 Construction | `BRICKS` | 42 | 2 | 41 | **43** | 2 |
| 🍖 Nourriture | `COOKED_BEEF` | 26 | 0 | 26 | **26** | 1 |
| 💎 Minerais | `DIAMOND` | 24 | 18 | 6 | **24** | 1 |
| 🐾 Mob Drops | `BONE` | 18 | 0 | 18 | **18** | 1 |
| 🌾 Agriculture | `WHEAT` | 21 | 2 | 19 | **21** | 1 |
| 🔴 Redstone | `REPEATER` | 18 | 1 | 17 | **18** | 1 |
| 🎲 Divers | `CHEST` | 22 | 5 | 17 | **22** | 1 |

> « matières listées » (`shop.categories[].materials`) ne vend rien : c'est la liste des matières que le rayon **réclame**. Une ligne de `generators:` dont le `spawnItem` y figure atterrit ici toute seule ; les autres n'y sont que pour la prochaine ligne qu'un admin ajoutera. Les offres payables, ce sont les 28 lignes venues du générateur et les 144 lignes écrites à la main dans `shop.extras`.

> Un rayon qui dépasse 36 offres ne disparaît pas : il se **page** — les offres se partagent au plus 4 rangées de neuf cases, et le panneau se coupe à la hauteur qu'elles occupent : les articles prennent la quasi-totalité de la fenêtre, la dernière rangée ne porte que le retour et les deux flèches. La grille des rayons, elle, tient sur une seule rangée : 9 rayons au plus y sont cliquables, au-delà le surplus ne l'est pas et le log du serveur le dit.

- Un arrivant (3 générateurs du palier 1, cf. `on-join.generator-amount`) : **5 400 $/h** — le salaire d'une journée de jeu, pas un pactole.
- Un tycoon de milieu de partie (20 × palier 10, plafond `limits.per-player`) : **1.44 M$/h**.
- Un tycoon maxé (20 × palier 28) : **7.92 M$/h**.
- Maxer **le multi-outil entier** (ses quatre âmes, leurs paliers et leurs 88 capacités) coûte 816.08 M$, soit **567 h** du tycoon de milieu de partie (bande admise : 400–1200 h, contrôlée par ce script). C'est l'objectif de fin de jeu, pas une étape.
- Maxer **une seule âme** (la pioche : ses quarante-neuf paliers et ses vingt-quatre capacités) coûte 210.63 M$, soit **146 h** au même revenu (bande admise : 80–400 h). `docs/MULTI-OUTIL.md` reprend la même division pour les quatre âmes.

Le prix d'achat n'est pas une ligne libre : c'est `sellPrice × buy-multiplier`, soit +75 % sur le prix `/sell`. Un aller-retour achat puis reprise laisse donc 30 % de perte : le comptoir encaisse, il ne distribue pas.
<!-- bareme-comptoir:fin -->

### L'extrait à coller sur un serveur déjà installé

Le plugin ne recopie jamais un `config.yml` existant (il écraserait le travail de l'admin) : les rayons
s'appliquent donc à la main, en remplaçant la section `shop:` du fichier par ce bloc :

```yaml
# plugins/ValoriaTycoon/config.yml — le rayon d'application du tour « comptoir ».
# Remplacez dans votre fichier la section `shop:` existante par ce bloc entier (ou ajoutez-le a la fin
# s'il n'y en a pas encore). Les commentaires se perdent si le chargeur reecrit le fichier : c'est
# normal, seul le contenu des clefs compte.
# ---------------------------------------------------------------------------
# Le comptoir (/shop) : la sortie d'argent.
#
# Le plugin ne connaissait qu'une entrée : le générateur crache un objet, /sell le paie au `sellPrice` de sa
# ligne, et le marché entre joueurs déplace la monnaie sans en créer. Le comptoir fait l'autre bout — acheter
# la matière plus cher qu'elle ne se revend — et c'est ce qui donne un prix de référence au bloc.
#
#   buy-multiplier  prix d'achat = `sellPrice` du générateur × ce facteur. 1,75 : la matière coûte 75 % de
#                   plus que ce que /sell en rend.
#   buyback-ratio   ce que le comptoir rachète, calculé sur SON prix d'achat (pas sur le `sellPrice`) :
#                   0,4 × 1,75 = 0,70, donc acheter puis se faire racheter laisse 30 % de perte. La reprise
#                   reste sous le `sellPrice` (0,7 × `sellPrice`) : /sell demeure le meilleur débouché.
#
# Règle vérifiée au chargement : `buy-multiplier` × `buyback-ratio` doit rester strictement sous 1. Au-dessus,
# le cycle est gagnant et la boutique devient une machine à imprimer — ces deux lignes sont ce qui l'empêche.
# Rien n'est persisté : le catalogue se recharge, les transactions vivent dans la monnaie et à l'inventaire.
shop:
  enabled: true
  title: "&a&lComptoir de Valoria"
  buy-multiplier: 1.75
  buyback-enabled: true
  buyback-ratio: 0.4
  # 36 piles de 64 : le plus gros lot qu'un clic peut livrer. Au-delà, le lot est réduit, pas refusé.
  max-per-transaction: 2304
  # Tailles de lot proposées par le panneau de quantité (un clic gauche ouvre ce choix).
  amounts: [1, 16, 32, 64]

  # Deux titres de repli : `generated-category` sert quand `categories` est absent (le config.yml d'un
  # serveur qui tourne depuis avant cette option) et `extras-category` nomme le rayon `divers` qui ramasse
  # ce qui n'est classé nulle part.
  generated-category: "&aMatières de générateur"
  extras-category: "&eDivers"

  # ===========================================================================
  # LES RAYONS. Une rangée = un onglet : une clef, un titre, une icône, et la liste des matières qu'elle
  # accueille. Le prix d'une matière de générateur n'est JAMAIS écrit ici — il reste `sellPrice ×
  # buy-multiplier` —, seul le classement est libre. Un classement figé dans le code serait un classement
  # qu'aucun admin ne peut corriger sans recompiler.
  #
  # Deux bornes, contrôlées toutes deux : 9 onglets au plus (la première ligne d'un coffre de six rangées)
  # et 36 offres par page (les quatre du milieu). Au-delà de trente-six le rayon s'étale sur une seconde
  # page, avec des flèches sous la ligne d'onglets ; au-delà de neuf rayons, le surplus ne serait pas
  # cliquable — et le log du serveur le dit à chaque chargement.
  #
  # `materials` est un ROUTAGE : ce que le comptoir range ici quand un générateur crache la matière. Une
  # matière de générateur absente de toutes les listes n'est pas perdue — elle est rangée à part, sous
  # `extras-category`, avec une alerte qui nomme le remède. Citer ici une matière que `shop.extras` vend
  # sert aussi de repli : si la ligne oublie son `category`, le rayon qui la réclame la reçoit.
  # ===========================================================================
  categories:
    - key: construction
      name: "&6🧱 Construction"
      icon: BRICKS
      description: ["&7Tout ce dont tu as besoin pour bâtir ton île :",
                    "&8• &7Blocs de base (pierre, bois, terre, sable…)",
                    "&8• &7Blocs décoratifs (verre, laine, argile, béton…)",
                    "&8• &7Matériaux de construction avancés"]
      materials: [COBBLESTONE, STONE, DEEPSLATE, GRANITE, DIORITE, ANDESITE, SAND, SANDSTONE, GRAVEL, DIRT,
                  GRASS_BLOCK, TERRACOTTA, GLASS, BRICKS, MOSS_BLOCK, SNOW_BLOCK, OAK_LOG, SPRUCE_LOG, BIRCH_LOG,
                  JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG, CRIMSON_STEM, WARPED_STEM,
                  OAK_PLANKS, OAK_SLAB, OAK_FENCE, WHITE_CARPET, FLOWER_POT, PAINTING, OBSIDIAN, END_STONE,
                  CRYING_OBSIDIAN, GLOWSTONE, NETHERRACK, SOUL_SAND, SOUL_SOIL, BLACKSTONE, BASALT, MAGMA_BLOCK]
    - key: nourriture
      name: "&c🍖 Nourriture"
      icon: COOKED_BEEF
      description: ["&7Pour ne jamais manquer d'énergie :", "&8• &7Viandes cuites et crues", "&8• &7Poissons",
                    "&8• &7Pain et autres aliments de base", "&8• &7Produits agricoles transformés"]
      materials: [BEEF, COOKED_BEEF, PORKCHOP, COOKED_PORKCHOP, CHICKEN, COOKED_CHICKEN, MUTTON, COOKED_MUTTON,
                  RABBIT, COOKED_RABBIT, COD, SALMON, COOKED_COD, COOKED_SALMON, TROPICAL_FISH, PUFFERFISH,
                  BREAD, COOKIE, PUMPKIN_PIE, CAKE, MUSHROOM_STEW, RABBIT_STEW, BEETROOT_SOUP, DRIED_KELP, APPLE,
                  EGG]
    - key: minerais
      name: "&b💎 Minerais"
      icon: DIAMOND
      description: ["&7Les ressources les plus précieuses :",
                    "&8• &7Minerais bruts (fer, or, diamant, émeraude, netherite…)",
                    "&8• &7Blocs de minerais compressés", "&8• &7Ressources utiles pour le craft"]
      materials: [COAL, IRON_INGOT, GOLD_INGOT, LAPIS_LAZULI, DIAMOND, EMERALD, QUARTZ, NETHERITE_SCRAP,
                  NETHERITE_INGOT, ANCIENT_DEBRIS, COPPER_INGOT, RAW_IRON, RAW_GOLD, RAW_COPPER, AMETHYST_SHARD,
                  GLOWSTONE_DUST, COAL_BLOCK, IRON_BLOCK, GOLD_BLOCK, REDSTONE_BLOCK, LAPIS_BLOCK, DIAMOND_BLOCK,
                  EMERALD_BLOCK, QUARTZ_BLOCK]
    - key: mobdrops
      name: "&5🐾 Mob Drops"
      icon: BONE
      description: ["&7Les loots obtenus sur les créatures :", "&8• &7Os, poudre à canon, perles de l'End",
                    "&8• &7Laines, plumes, cuir", "&8• &7Loots rares de mobs spéciaux"]
      materials: [ROTTEN_FLESH, BONE, BONE_MEAL, STRING, SPIDER_EYE, GUNPOWDER, SLIME_BALL, ENDER_PEARL,
                  BLAZE_ROD, GHAST_TEAR, PHANTOM_MEMBRANE, LEATHER, FEATHER, INK_SAC, GLOW_INK_SAC, MAGMA_CREAM,
                  SHULKER_SHELL, WHITE_WOOL]
    - key: agriculture
      name: "&a🌾 Agriculture"
      icon: WHEAT
      description: ["&7Tout pour développer tes champs :", "&8• &7Graines (blé, carottes, pommes de terre…)",
                    "&8• &7Cultures spéciales (cannes à sucre, cactus, citrouilles, pastèques…)",
                    "&8• &7Plants et pousses d'arbres"]
      materials: [WHEAT, MELON_SLICE, CARROT, POTATO, BEETROOT, SUGAR_CANE, CACTUS, PUMPKIN, BAMBOO, COCOA_BEANS,
                  NETHER_WART, HAY_BLOCK, SWEET_BERRIES, KELP, SEA_PICKLE, OAK_SAPLING, SPRUCE_SAPLING,
                  BIRCH_SAPLING, JUNGLE_SAPLING, ACACIA_SAPLING, DARK_OAK_SAPLING]
    - key: redstone
      name: "&c🔴 Redstone"
      icon: REPEATER
      description: ["&7Pour les amateurs de mécanique et d'automatisation :", "&8• &7Poudre de redstone",
                    "&8• &7Pistons, observateurs, comparateurs", "&8• &7Hoppers, droppers, répéteurs"]
      materials: [REDSTONE, REDSTONE_TORCH, REPEATER, COMPARATOR, OBSERVER, PISTON, STICKY_PISTON, HOPPER,
                  DROPPER, DISPENSER, SLIME_BLOCK, HONEY_BLOCK, RAIL, POWERED_RAIL, REDSTONE_LAMP,
                  DAYLIGHT_DETECTOR, LEVER, LIGHTNING_ROD]
    - key: divers
      name: "&e🎲 Divers"
      icon: CHEST
      description: ["&7Objets variés et utilitaires :", "&8• &7Seaux (eau, lave)", "&8• &7Objets spéciaux",
                    "&8• &7Matériaux rares ou uniques"]
      materials: [TORCH, LADDER, SCAFFOLDING, LANTERN, CAMPFIRE, SOUL_TORCH, GLASS_BOTTLE, BUCKET, WATER_BUCKET,
                  LAVA_BUCKET, SHEARS, RED_BED, MINECART, OAK_BOAT, BOOK, PAPER, NETHER_STAR, WITHER_ROSE,
                  REINFORCED_DEEPSLATE, BEDROCK, DRAGON_EGG, ENDER_EYE]

  # ===========================================================================
  # LE RESTE DU MAGASIN. Une matière qui ne sort d'aucun générateur n'a pas de prix de référence : celui-ci
  # s'écrit à la main, une offre par ligne, et `sellback` y reste l'exception (le rachat de ce que le
  # serveur produit ailleurs reste le rôle de /sell). Quand une reprise est écrite, elle reste vers 40 %
  # du prix d'achat : le comptoir encaisse, il ne distribue pas. `category` nomme le rayon qui reçoit
  # l'offre — et peut être oublié si un rayon réclame déjà cette matière dans `materials:`.
  extras:
    # Une ligne = une offre. Le prix d'une matiere de generateur ne s'ecrit pas ici (il vient de
    # `sellPrice × buy-multiplier`) ; celui d'une matiere qui ne sort d'aucun generateur si.
    # Les lignes sont rangees dans l'ordre des onglets, pour relire un rayon d'un coup d'oeil.
    # --- construction (41 offres)
    - { material: COBBLESTONE, name: "&7Pierre", buy: 12, sellback: 4, category: construction }
    - { material: STONE, name: "&7Pierre taillée", buy: 16, sellback: 6, category: construction }
    - { material: DEEPSLATE, name: "&8Ardoise", buy: 16, sellback: 6, category: construction }
    # … et 141 autres lignes, exactement de la meme facture
```

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
