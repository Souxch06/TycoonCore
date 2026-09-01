# Tuto — voir les nouveautés en jeu (v1.6.3)

**En une phrase** : mettre les 3 nouveaux fichiers sur le serveur, redémarrer, récupérer l'outil,
et essayer les 4 âmes. 20 minutes, tu suis les étapes dans l'ordre.

> ⚠️ Si ton serveur n'a pas encore reçu les nouveaux fichiers (le problème SFTP du message
> précédent), commence par l'**étape 1** — sinon tu testes l'ancienne version et rien ne changera.

## Ce qui est nouveau dans cette version (ce que tu devrais remarquer)

- **L'outil a moins de capacités** (61 au lieu de 88) : les doublons ont disparu.
  Les anciens « Speed », « Casino », « Main dorée », « Chercheur de spawner »… n'existent plus —
  ils sont devenus **Efficacité**, **Trouvaille**, **Braquage**, **Money Pouch** (mêmes effets, un seul nom).
- **Les prix montent** : chaque niveau d'une capacité coûte plus que le précédent, et le dernier
  niveau coûte beaucoup plus cher que le premier.
- **La boutique** (`/shop`) est plus propre : plus d'icônes de décor, les panneaux font exactement
  la taille de leur contenu.

---

## Étape 1 — Mettre les 3 nouveaux fichiers sur le serveur

1. Ouvre la release : <https://github.com/Souxch06/ValoriaTycoon/releases/tag/build-latest>
2. Télécharge **les 3 fichiers** (les 3 obligatoires, pas un seul) :
   - `ValoriaTycoon-v1.6.3.jar`
   - `ValoriaEconomy-v1.6.3.jar`
   - `ValoriaTools-v1.6.3.jar`
3. Sur ton panneau MCServerHost, ouvre le **gestionnaire de fichiers** (ou connecte-toi en SFTP)
   et va dans le dossier `plugins/`.
4. Remplace les anciens fichiers par les 3 nouveaux (mêmes noms, les anciens seront écrasés).
5. **Redémarre le serveur** depuis le panneau.

> Le redémarrage est **obligatoire** : les plugins ne se chargent qu'au démarrage.
> Un simple `/reload` ne suffit pas.

## Étape 2 — Vérifier que c'est bien la nouvelle version

1. Connecte-toi au serveur.
2. Tape `/plugins`.

**Tu dois voir** (les 3, en version **1.6.3**) :

```
ValoriaEconomy 1.6.3
ValoriaTycoon 1.6.3
ValoriaTools 1.6.3
```

Si une version est différente ou qu'un plugin manque → les fichiers ne sont pas au bon endroit.
Refaire l'étape 1. Tout le reste de ce tuto est inutile sans ça.

## Étape 3 — Récupérer l'outil et se donner de l'argent

1. Tape `/tools buy` → **l'outil apparaît dans ta main** (gratuit par défaut).
2. Tape `/eco give <ton pseudo> 10000000` → 10 000 000 de monnaie pour tester (sinon rien
   ne s'achète).
3. Tape `/bal` → vérifie que le solde est bien là.

**Tu dois voir** : un item dans ta main qui s'appelle « ⚒ Multi-outil de Valoria ». Regarde le
mur et le bois : **le nom de l'item change** selon ce que tu vises (pic sur la pierre, hache sur
le tronc, épée sur un monstre). C'est normal, c'est l'outil qui « choisit son âme ».

## Étape 4 — Le geste magique : ouvrir le menu

**Accroupi-t-toi (touche Maj) et fais un clic droit** avec l'outil en main (en visant un bloc ou
le vide).

**Tu dois voir** : un menu de 6 rangées. Repère seulement 4 trucs :

- En haut à gauche : **4 carrés colorés** = les 4 âmes (pioche, hache, canne, épée).
  Cliquer dessus change l'âme affichée.
- En haut au milieu : le bouton **« Palier ↑ »** = faire monter le niveau de l'âme.
  C'est lui qui débloque les capacités grises. Il coûte de l'argent (~1 500 au départ).
- Au centre : **les capacités**. Clic gauche = +1 niveau. Le prix est écrit dans la description
  quand tu poses la souris dessus.
- En bas : **×1 / ×10 / ×100** = acheter en paquet (le prix affiché est celui du paquet).

> Astuce : les capacités grises avec un « ✖ » sont **verrouillées** par le palier — il faut
> d'abord cliquer sur « Palier ↑ » assez de fois.

## Étape 5 — Tester la pioche (le plus simple)

1. Dans le menu, reste sur l'âme **pioche** (premier carré).
2. Achète **Efficacité** au niveau 1 → **gratuit** (c'est marqué « offert »).
3. Va miner de la pierre.

**Tu dois voir** : tu casses la pierre **visiblement plus vite** qu'avec une pioche normale.
En bonus, l'âme pioche a aussi « Vente à la casse » (gratuite) : ce que tu mines est parfois
**vendu automatiquement** → un petit `+<montant>` dans le chat.

> Pour le fun (optionnel) : monte le palier jusqu'à **10** (bouton « Palier ↑ », environ
> 27 000 au total) et achète **Fortune** : parfois tu récupères **2 ou 3 fois plus** de minerai.

## Étape 6 — Tester la hache

1. Dans le menu, clique sur le carré **hache**.
2. Achète **Arbre abattu** niveau 1 → **gratuit**.
3. Va couper un tronc d'arbre.

**Tu dois voir** : **tout l'arbre tombe d'un coup** (plusieurs blocs), pas un par un.

> Bonus gratuit : plante 5 tiges de blé, fais pousser, et moissonne — « Récolte automatique »
> (gratuite) récolte **et replante** toute seule.

## Étape 7 — Tester l'épée

1. Trouve un zombie (ou en tant qu'OP, tape `/summon zombie` pour en faire apparaître un).
2. Dans le menu, carré **épée** : achète **Coup critique** niveau 1 → **2 500**.
3. Tape le zombie 10 à 20 fois.

**Tu dois voir** : la plupart des coups font des dégâts normaux, mais **certains coups font nettement
plus de dégâts** (chiffres plus gros). C'est le coup critique (il frappe 1,5× plus fort).
Au niveau 1, ça arrive environ 1 fois sur 6.

## Étape 8 — Tester la canne à pêche

1. Va au bord de l'eau, l'outil en main (il devient une canne tout seul).
2. Lâche la ligne (clic gauche).
3. Dans le menu, carré **canne** : achète **Moulinet rapide** niveau 1 → **gratuit**.

**Tu dois voir** : la ligne se **rembobine toute seule** quand il y a du poisson, sans cliquer.

> Optionnel : achète **Pêche chanceuse** (17 000) → tu remontes des **objets rares** (trésors)
> plus souvent.

## Étape 9 — Tester la boutique

1. Tape `/shop`.
2. Tu vois les **rayons** (catégories). Clique-en un.
3. Dans le rayon : chaque matière a **deux prix** — le **vert = acheter**, le **rouge = vendre**.
4. Note ton solde : `/bal`.
5. **Clic gauche** sur une matière → on te propose des quantités → achète-en 1.
6. Reviens en arrière, puis **clic droit** sur la même matière → tu la **revends**.
7. `/bal` une dernière fois.

**Tu dois voir** :
- La boutique **sans icônes de décor**, des panneaux bien calés sur leur contenu (c'est la
  refonte de cette version).
- Après l'achat puis la revente : ton solde a **baissé** (le prix de revente est toujours plus
  bas que le prix d'achat — c'est voulu, pour empêcher de faire du profit en achetant/revendant).

---

## Si ça ne marche pas (les 4 cas classiques)

| Tu vois… | C'est… | Tu fais… |
| --- | --- | --- |
| `/plugins` sans les 3 plugins, ou version fausse | Les jar ne sont pas en place | Réétape 1 + **redémarrer** |
| Pas d'outil dans la main | Tu ne l'as pas | `/tools buy`, sinon `/tools give <ton pseudo>` |
| Le menu ne s'ouvre pas | Mauvais geste | **Accroupi + clic droit**, outil en main, en visant un bloc ou le vide |
| Les capacités ne font rien | Config ou diagnostic | Tape `/tools stats` et lis la fin du message (il dit quoi coince), puis `/tools reload` |
| « Pas assez d'argent » | Solde vide | `/eco give <ton pseudo> 10000000` |

**C'est validé quand** : tu as vu l'outil changer 4 fois, miner plus vite, un arbre tomber tout
seul, un coup critique, la pêche se rembobiner, et acheter/revendre dans la boutique.

---

*Pour aller plus loin (vérifier précisément les prix et les chances à chaque niveau, le plan de
test complet avec les valeurs attendues) : voir `docs/PLAN-TESTS-DETAILLE.md`.*
