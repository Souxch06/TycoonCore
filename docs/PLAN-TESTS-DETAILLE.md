# Tuto — Tests en jeu des changements v1.6.3 (PR #21)

Plan de test **en jeu** pour vérifier les trois évolutions de `ValoriaTools` et la refonte du
comptoir : **boutique redimensionnée**, **courbe de prix géométrique**, **capacités dédupliquées
(88 → 61)** et **chances étalées**.

Prérequis : les **trois** jar de la release [`build-latest`](https://github.com/Souxch06/ValoriaTycoon/releases/tag/build-latest)
(`ValoriaTycoon-v1.6.3.jar`, `ValoriaEconomy-v1.6.3.jar`, `ValoriaTools-v1.6.3.jar`) posés dans
`plugins/` et le serveur **redémarré** après. Si le serveur n'a pas reçu les jar (pipeline SFTP en
panne), les tests ci-dessous ne testent rien — voir le déploiement manuel d'abord.

## 0. Préparation (5 min)

1. **Console du serveur** : au démarrage, les trois plugins doivent être chargés sans erreur rouge :
   `ValoriaEconomy 1.6.3 (STARTUP)`, `ValoriaTycoon 1.6.3`, `ValoriaTools 1.6.3`.
2. **En jeu**, en OP (ou avec les permissions `valoria.tools.admin`, `valoriaeconomy.eco`,
   `valoriatycoon.shop.admin`), vérifier l'économie : `/bal` renvoie un solde.
3. **Diagnostic de base** : `/tools stats` — lire la fin de la sortie (reconnaissance des blocs,
   garde de l'objet, effet de minage réellement dû). Tout est au vert avant de commencer.
4. **Se financer** : `/eco give <toi> 50000000` — les tests de prix débilitent vite.
5. **S'armer** : `/tools buy` (gratuit par défaut) ou `/tools give <toi>`.
6. **Banc d'essai** (recommandé, pour mesurer sans parcourir le monde) :
   - un **armor stand** armé (cible au combat),
   - un **champ de blé** 10×10 (rendement / récolte),
   - un **filon d'émeraudes** ≥ 5 blocs (Fortune),
   - un **puits** avec de l'eau (canne),
   - un **coin de zombies** ou un spawner (épée).

Comptes à zéro entre les tests : `/tools reset <toi>` (toutes les âmes) ou `/tools reset <toi> épée`.

---

## Test 1 — Comptoir `/shop` : panneaux à la taille de leur contenu

Le redesign (`1e07edd`) : les panneaux sont **dimensionnés à leur contenu** et les **icônes de
décor sont retirées**.

| # | Action | Ce qu'on doit voir |
| --- | --- | --- |
| 1.1 | `/shop` | Écran 1 : les rayons (catégories). Chaque panneau ne fait que la taille de son contenu — pas de rangée de remplissage, pas d'objet décoratif qui ne fait rien. |
| 1.2 | Cliquer un rayon | Écran 2 : les offres. Chaque item porte **son prix d'achat en vert** et **son prix de reprise en rouge**, sous la forme de son lot. Navigation retour/flèches en bas quand le rayon se page. |
| 1.3 | Clic **gauche** sur une offre | Écran 3 : les quantités (lots), chacune avec le prix total du lot. Un clic achète le lot, on revient aux offres. |
| 1.4 | Noter `/bal`, acheter un lot, refaire `/bal` | Le solde est débité du prix affiché, pas d'un centime de plus ni de moins. |
| 1.5 | Clic **droit** (vendre un stack), **Maj+clic droit** (vendre tout) sur une matière possédée | Le solde est crédité ; les items partent de l'inventaire. |
| 1.6 | `/shop <matière>` (ex. `/shop coal`) | Le comptoir s'ouvre **directement sur le rayon** qui vend cette matière, et sur la bonne page. |

**Anti-exploit (vente < achat)** — `buy-multiplier` strictement au-dessus de 1 :

1. Noter `/bal`. Acheter 1 unité d'une matière au comptoir (prix d'achat, vert).
2. La revendre immédiatement (prix de reprise, rouge).
3. `/bal` : le solde doit être **inférieur** au solde initial. *Acheter puis revendre doit perdre.*
4. Console : `/shop reload` ne doit afficher **aucun** `/!\ acheter puis revendre est GAGNANT`.

**Si ça passe pas** : le catalogue relit la table `generators:` + `shop.extras` — `/shop reload`
puis re-tester ; vérifier que `buy-multiplier > 1`.

---

## Test 2 — Multi-outil : régressions de base (le plugin tient toujours debout)

Ce ne sont pas des nouveautés de la PR, mais si l'un de ces points est cassé, les tests suivants
sont incompréhensibles.

| # | Action | Ce qu'on doit voir |
| --- | --- | --- |
| 2.1 | L'outil en main | Nom `&6⚒ Multi-outil de Valoria` + l'âme affichée, le palier d'âme, la liste des capacités **payées** (nom + niveau), la ligne `+ N autre(s) — /tools`, et `Autres âmes : pic … · canne … · épée …`. |
| 2.2 | Viser de la pierre / un tronc / un monstre, lancer dans l'eau | L'item **devient** pic / hache / épée / canne (`morph-by-target`), et c'est l'âme correspondante qui paie le geste. |
| 2.3 | **Accroupi + clic droit** (dans le vide ou sur un bloc) | Le menu s'ouvre : 4 âmes en haut à gauche (slots 0–3), `Palier ↑` + solde en haut à droite, les capacités au centre (27 cases/page), la légende des couleurs, et en bas `×1 / ×10 / ×100`, `vendre`, `bilan`, pagination. |
| 2.4 | Touche **Q** avec l'outil, puis clic dans un **coffre** | L'objet ne se pose pas et ne se range pas (garde `ToolGuard`). |
| 2.5 | Mourir avec l'outil (ou `/kill`) | L'outil **ne tombe pas** ; au relog, `auto-give` le rend. |
| 2.6 | `/tools` depuis la console | Message d'erreur poli (l'interface n'existe qu'en jeu) — pas de crash. |

---

## Test 3 — Déduplication des capacités : 88 → 61, aucun doublon

`9084990` : on a retiré 27 capacités qui partageaient le **même noyau** (même effet sous deux
noms) dans la même âme. Il ne reste **plus aucun doublon de noyau** dans aucune âme.

### 3.1 Compter les capacités dans le menu

| âme | cases du menu (attendu) |
| --- | --- |
| Pioche | **16** |
| Hache | **15** |
| Épée | **18** |
| Canne | **12** |
| **Total** | **61** |

Cliquer chaque âme (slots 0–3) et compter les cases du centre (plus la pagination s'il y en a).

### 3.2 Les 27 noms supprimés ne doivent nulle part exister

Dans les menus, les tootips, `/tools aide <nom>` et la complétion Tab de `/tools ability` :

| âme | noms à ne plus voir (27) |
| --- | --- |
| Pioche | Speed, Célérité, Explosive, Surcharge, MineCoins Pouch, Chercheur de spawner, Main dorée, Chercheur de crédits |
| Hache | Speed, Main de Gaïa, Main dorée, Casino, Farmcoins Pouch, Chercheur de spawner, Chercheur de bonbon, Chercheur de crédits |
| Épée | Booster MobCoins, Casino, Chercheur de spawner, Chercheur de bonbons, MobCoins Pouch |
| Canne | Main dorée, Chercheur de spawner, Chercheur de bonbons, Casino, FishCoins Pouch, Chercheur de crédits |

Vérification rapide : `/tools aide explosive` → « inconnue » ; `/tools aide onde-sismique` → fiche
complète. **Idem en Tab-complétion** : taper `/tools ability <toi> pioche Ex` ne doit plus proposer
`explosive`.

### 3.3 Les capacités d'accueil existent ET fonctionnent

| fusion | accueil (doit être présent et actif) |
| --- | --- |
| Speed + Célérité → | **Efficacité** (pioche, offerte au palier 1 : miner plus vite) |
| Explosive + Surcharge → | **Onde sismique** (pioche) — testé au §5 |
| MineCoins/Farmcoins/MobCoins/FishCoins Pouch + Chercheur de crédits → | **Money Pouch** (les 4 âmes) : gros `+<montant>` occasionnel à la casse |
| Main dorée / Booster MobCoins → | **Braquage** (les 4 âmes) : multiplicateur d'argent |
| Chercheur de spawner + Chercheur de bonbon + Casino → | **Trouvaille** (les 4 âmes) — testée au §5 |
| Speed (hache) → | **Célérité** (hache, offerte) |
| Main de Gaïa → | **Jugement divin** (hache) : moissonne une zone autour du bloc |

### 3.4 Le volontairement conservé

Les capacités **passives présentes dans plusieurs âmes** (Braquage, Chercheur d'xp, Double gain,
Proc Booster) sont **intentionnellement** achetées séparément par âme : `braquage` existe bien
4 fois, une par âme, et `/tools ability <toi> épée braquage 5` n'affecte que l'épée. Ce n'est PAS
un bug.

### 3.5 Pas de niveau fantôme

Les niveaux s'enregistrent par **id** dans `plugins/ValoriaTools/tools.yml`. Sur une installation
fraîche, aucun id supprimé ne doit y figurer. Sur un serveur qui a eu l'ancienne config : vérifier
que `tools.yml` ne contient plus de clés `speed`, `surcharge`, `casino`… (un niveau payé sur une
capacité supprimée n'a plus d'effet — c'est assumé : la fusion regroupe l'effet dans l'accueil,
elle ne reporte pas le niveau).

---

## Test 4 — Courbe de prix géométrique

`121c5d5` : le prix du niveau `n` vaut `price × price-ratio^(n−1)`, plafonné à **100 000 000**
(`PRICE_CEILING`). Deux bugs corrigés à vérifier spécifiquement :

1. **Aucun niveau « gratuit » en fin de courbe** (le `Math.pow` débordait *avant* le plafond et
   rendait le dernier niveau gratuit) — résultat non fini → retombe sur le plafond.
2. **Les plages courtes coûtent plus cher au niveau** (une capacité à 5 niveaux ne doit pas être
   anecdotique face à une capacité à 2000).

### 4.1 Lire les prix dans le menu

La **tooltip de chaque capacité** affiche le prix du paquet demandé (clic = +1, Maj = +10,
multiplié par le pas `×1/×10/×100` choisi en bas). Comparer avec les valeurs attendues :

| capacité (âme) | niveaux | niv. 1 | milieu | **dernier** (attendu) |
| --- | --- | --- | --- | --- |
| Coup critique (épée) | 200 | 2 500 | ~24 700 (niv. 100) | **~250 000** (×100) |
| Tranchant (épée) | 5 | 66 000 | — | **858 000** (×13 : 66 000 → 125 323 → 237 966 → 451 857 → 858 000) |
| Force (épée) | 3 | 100 000 | — | **350 000** (×3,5) |
| Fortune (pioche) | 10 | 33 000 | — | **990 000** (×30) |
| Money Pouch (toutes) | 2000 | 420 | — | **84 000** (×200) |

**Plage courte plus chère au niveau** : le niveau 1 de Fortune (10 crans) coûte **33 000**,
contre **2 500** pour le niveau 1 de Coup critique (200 crans). Si c'est l'inverse, la courbe
n'est pas la bonne.

### 4.2 Le piège « niveau gratuit » (régession majeure)

1. `/tools set <toi> épée 50` (débloquer tout) puis `/tools ability <toi> épée critique 199`.
2. Dans le menu, la tooltip de **Coup critique** doit afficher un prix **> 0** pour le dernier
   cran, **≤ 100 000 000** — ni `0`, ni « gratuit », ni un nombre absurde (overflow).
3. Idem sur une longue courbe : `/tools ability <toi> pioche money-pouch 1999` → dernier cran
   ~84 000, jamais 0.
4. Seuls les niveaux **`free: true`** sont gratuits par design (Efficacité, Auto-smelt, Vente à la
   casse, Célérité, Récolte automatique, Arbre abattu, Moulinet rapide, Vente du butin) — vérifier
   que c'est bien **seulement** eux.

### 4.3 Le palier d'âme

`Palier ↑` coûte `1 500 × 1.12^(palier−1)`, plafonné à 2 500 000 : palier 2 = 1 680, palier 3 ≈
1 882. Acheter un palier, vérifier le débit au `/bal` et que les capacités verrouillées
(`&8✖`) passent en achetable (`&e●`).

### 4.4 Les pas d'achat `×1 / ×10 / ×100`

Choisir `×100`, pointer une capacité : le prix affiché doit être la **somme des 100 niveaux**
(plus cher que 100 × le prix du niveau courant, car la courbe monte). Acheter, vérifier le débit
et que le niveau est bien +100.

**Si ça passe pas** : `/tools reload`, puis re-lire la config (`resources-tools/config.yml` :
chaque ligne porte `price`, `price-ratio`, `price-cap` ; un `price-ratio > 2.0` est refusé à la
lecture — le build l'aurait déjà bloqué).

---

## Test 5 — Chances étalées : le pas n'est plus une constante

`9084990` (suite) : le pas de chance est calculé `(plafond − base) / (max_level − 1)`, écrit en
pleine précision arrondi **vers le haut**. Conséquences testables :

- **le dernier niveau vaut EXACTEMENT le plafond** (avant, la troncature du pas faisait manquer le
  plafond d'une trentaine de niveaux sur une capacité à 2000 crans) ;
- **les crans intermédiaires sont utiles** (avant, le plafond tombait trop tôt — ex. Coup critique
  à 60 % dès le niv. 46) ;
- plus aucun cas de deux capacités d'une même âme qui combinaient leurs chances en
  `1 − Π(1 − c)` (invisible pour le joueur).

### 5.1 Méthode de mesure (à appliquer à chaque ligne du tableau)

1. Fixer le niveau : `/tools ability <toi> <âme> <capacité> <niveau>`
   (les noms du wiki, les ids et les noyaux marchent : `critique`, `Coup critique`, `CRIT`).
2. Faire **≥ 200 essais** (1000 pour être net) et compter les activations.
3. Attendu : `essais × chance`, avec une tolérance de **±3 %** à 200 essais (plus serré à 1000).
4. `/tools reset <toi> <âme>` avant de changer de capacité.

### 5.2 Tableau des chances attendues

| capacité (âme) | niv. 1 | milieu | **niv. max** (exactement le plafond) |
| --- | --- | --- | --- |
| Coup critique (épée) | **15 %** | ~37,4 % (niv. 100) | **60 %** (niv. 200) |
| Vol de vie (épée) | **20 %** | ~49,8 % (niv. 100) | **80 %** (niv. 200) |
| Bonus de rendement (hache) | **2 %** | ~30,9 % (niv. 100) | **60 %** (niv. 200) |
| Onde sismique (pioche) | **2 %** | ~25,9 % (niv. 150) | **50 %** (niv. 300) |
| Fortune (pioche) | **2 %** | ~41,1 % (niv. 5) | **90 %** (niv. 10) |
| Pillage (épée) | **2 %** | ~26,9 % (niv. 4) | **60 %** (niv. 8) |

### 5.3 Le piège « plafond trop tôt » (régression majeure)

Avec l'ancienne constante, le plafond était atteint **au milieu** de la courbe. Reprendre les
points qui étaient symptomatiques :

| capacité | niveau | **avant (bug)** | **après (attendu)** |
| --- | --- | --- | --- |
| Coup critique | 46 | 60 % (plafond atteint !) | **~25 %** |
| Vol de vie | 61 | 80 % (plafond atteint !) | **~38 %** |
| Bonus de rendement | 30 | 60 % (plafond atteint !) | **~10,5 %** |

Si tu mesures 60 % de critiques au niveau 46, l'ancienne courbe est encore en place : les jar ne
sont pas ceux de la release, ou le serveur n'a pas redémarré.

### 5.4 Protocoles concrets par capacité

- **Coup critique** : face à l'armor stand (ou un groupe de zombies), frapper 200 fois, compter
  les dégâts « gros » (le critique frappe ×1,5). Au niv. 200, ~120 gros coups sur 200.
- **Vol de vie** : descendre sous la moitié des cœurs (mais pas à 1), toucher un zombie 200 fois,
  compter les récupérations de cœurs (+1 cœur au niv. 1, jusqu'à +6 au plafond).
- **Bonus de rendement** : récolter le champ de blé niv. 200 : ~60 % des tiges rendent 2×.
  Comparer le sac (ou la vente auto si active) avant/après le même champ au niv. 1.
- **Onde sismique** : miner un filon d'émeraudes 100 fois : compter les « tremblements »
  (particules + blocs cassés en cercle autour). Le rayon et le nombre de blocs cassés **montent
  avec le niveau** (radius 1→5, max-blocks 27→160) — un deuxième point de contrôle gratuit.
- **Fortune** : miner 100 blocs d'émeraude au niv. 10 : ~90 fois le drop est **augmenté**
  (+1 à +2 blocs). Au niv. 1, c'est ~2 fois sur 100.
- **Trouvaille** (les 4 âmes, le réservoir commun est le **Tripwire Hook**) : miner / récolter /
  pêcher / tuer 200 fois au niveau choisi ; chaque activation donne le message bleu
  `✦ Tripwire Hook trouvé par l'outil.` **et** l'objet au sol. Chanter la couleur : si tu vois
  d'autres objets (name tag, sugar), c'est le réservoir de repli `tool.treasure.items` qui est lu —
  signal que la ligne de config n'est pas chargée.
- **Money Pouch** / **Braquage** / **Double gain** : les `+<montant>` et `vente ×2` du chat —
  même méthode de comptage ; Money Pouch niv. 2000 : chance 50 % ET montant jusqu'à 25 000.

### 5.5 Le plafonnement exact au dernier cran

C'est LE test de la précision du pas (la troncature à 4 décimales faisait manquer le plafond) :

1. `/tools ability <toi> épée critique 200` → mesurer : **60 %**, pas 59 %.
2. `/tools ability <toi> pioche fortune 10` → **90 %**.
3. `/tools ability <toi> axe rendement 200` → **60 %** sur les récoltes.

Une demi-pointe de moins (59 %) = le pas tronqué est encore là.

---

## Test 6 — Longévité (contrôle rapide, pas de session de 582 h)

`9084990` (fin) : retirer 27 capacités aurait fait tomber l'outil complet à 359 h ; `PRICE_BASE`
est remonté ×1,67 → **582 h** pour la multi-tool entière, **146 h par âme**.

En jeu, on ne mesure pas les heures : on **regarde**. Dans le menu, au pas `×100` :

- les capacités courtes (Tranchant, Force) affichent des prix à 6 chiffres dès le premier cran ;
- les Pouch (2000 crans) affichent des prix à 3 chiffres au début mais la tooltip annonce un
  dernier cran à 4 chiffres (84 000) ;
- aucun écran ne « sature » : la pagination et les prix restent lisibles jusqu'au max.

Si le total visuel d'une âme ressemble à « quelques millions » plutôt qu'à « des dizaines de
millions », la remontée de `PRICE_BASE` n'est pas en place.

---

## Check-list d'acceptation

À cocher au vert avant de fermer le ticket :

- [ ] **0** — Console : 3 plugins v1.6.3, aucune erreur rouge ; `/tools stats` au vert ; `/bal` OK
- [ ] **1** — `/shop` : 3 écrans (rayons / offres / quantités), panneaux à la taille de leur contenu, aucun décor
- [ ] **1** — Acheter → revendre = **perte** ; aucun `/!\ GAGNANT` en console après `/shop reload`
- [ ] **2** — Morphing des 4 âmes, menu 6 rangées, garde de l'objet (Q / coffre / mort / auto-give)
- [ ] **3** — 61 capacités au total : **16 / 15 / 18 / 12**
- [ ] **3** — Les 27 noms supprimés n'apparaissent nulle part (menu, aide, complétion)
- [ ] **3** — Accueils actifs : Efficacité, Onde sismique, Money Pouch, Braquage, Trouvaille, Célérité, Jugement divin
- [ ] **4** — Prix géométriques conformes au tableau 4.1 (dont Tranchant 66 000 → 858 000)
- [ ] **4** — **Aucun niveau gratuit** en fin de courbe (critique niv. 200 > 0, ≤ 100 000 000)
- [ ] **4** — Plage courte plus chère au niveau : Fortune niv. 1 (33 000) > Coup critique niv. 1 (2 500)
- [ ] **5** — Critique niv. 46 ≈ 25 % (et NON 60 %) ; niv. 200 = 60 % pile
- [ ] **5** — Fortune niv. 10 = 90 % ; Rendement niv. 200 = 60 % ; Vol de vie niv. 200 = 80 %
- [ ] **5** — Trouvaille : message bleu + Tripwire Hook au sol, dans les 4 âmes
- [ ] **6** — Total d'une âme visuellement « dizaines de millions » (146 h), pas « quelques millions »

**Si un point échoue** : `/tools stats` d'abord (le diagnostic nomme la panne la plus fréquente),
puis vérifier que le jar déployé est bien celui de la release (taille dans le listing SFTP :
`ValoriaTycoon-v1.6.3.jar` ≈ 2 823 714 octets, `ValoriaEconomy-v1.6.3.jar` ≈ 20 861,
`ValoriaTools-v1.6.3.jar` ≈ 169 011), puis relire la console du redémarrage.
