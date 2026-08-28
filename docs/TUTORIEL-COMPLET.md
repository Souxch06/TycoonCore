# Tutoriel complet — livrer ValoriaTycoon sans aucun plugin téléchargé

> Ce tuto est écrit pour être suivi **uniquement avec le navigateur** (aucune commande à taper).
> Chaque étape dit : sur quelle page cliquer, quoi coller, et **à quoi ça ressemble quand c'est bon**.
> Temps total : environ 15 minutes, dont 5 minutes de build en fond.

---

## Étape 0 — Comprendre en 6 lignes ce qu'on livre

1. Le serveur n'aura que **2 fichiers** à toi : `ValoriaTycoon-v1.6.3.jar` et `ValoriaEconomy-v1.6.3.jar`.
2. **Plus besoin** de Vault, VaultUnlocked, EssentialsX, HoloEasy, ProtocolLib : tout est écrit dans le dépôt.
3. Ces 2 jar sont **construits par GitHub** (le « build »), pas par moi : mon environnement n'a pas de compilateur Java.
4. Donc le build n'a **jamais tourné** sur ces changements → c'est l'étape 1 qui répond à « est-ce que ça compile ? ».
5. Si le build est rouge : tu me colles les lignes rouges, je corrige, on recommence (2 minutes par tour).
6. Si le build est vert : tu télécharges les jar (étape 2), tu testes (étape 3), puis tu merges (étape 5).

---

## Étape 1 — Coller le fichier de build (obligatoire, 2 minutes)

**1.1** Ouvre ce lien, il ouvre GitHub sur « créer un fichier » avec tout déjà rempli :

```
https://github.com/Souxch06/ValoriaTycoon/new/arena/01a043a8-valoriatycoon?filename=.github%2Fworkflows%2Fbuild.yml
```

**1.2** Va chercher le contenu à coller : ouvre la PR **#7** → onglet **Files changed** (à droite en haut)
→ dans la colonne de gauche, cherche `docs/CI-A-COLLER.yml` → clique sur l'icône **copy** (les deux
petits carrés) en haut à droite du fichier. Ça copie les ~96 lignes d'un coup.
*(Astuce : le bouton copy copie le fichier entier, sans les « + » verts.)*

**1.3** Clique dans la grande zone de texte de GitHub, **Ctrl + V**, puis descends en bas de page :

- titre du commit : laisse ce qui est écrit (ou écris `Ajouter le workflow de build`)
- coche **Commit directly to the `arena/01a043a8-valoriatycoon` branch**
- bouton vert **Commit changes**.

**✅ Ça y est si** : la page PR #7 affiche ton commit, et si tu ouvres
`https://github.com/Souxch06/ValoriaTycoon/actions` tu vois une nouvelle ligne
**« Build and Validate ValoriaTycoon »** en cours (point jaune) ou déjà verte.

**1.6 (le plus important) Le build est rouge ?** Ne recopie **rien** à la main :
- si tu as collé la version la plus récente de `docs/CI-A-COLLER.yml`, le rapport d'erreurs arrive
  **tout seul en commentaire de la PR #7** (fichier, ligne, extrait du code fautif) ;
- sinon : page du run → en haut à droite, les **trois petits points ⋯** → **Re-run all jobs** →
  attends ~3 min → clique l'étape rouge → **Ctrl+F** `ERROR` → colle-moi ce que tu vois.
- variante « zéro copier-coller », si tu préfères : ajoute les 3 blocs de `docs/DEPLOY-2-JARS.md`
  (section « Option rapport de compilation ») à `deploy.yml`, puis **Run workflow** sur la branche en
  cochant **Diagnostic** — le rapport est publié sur la PR, sans toucher au serveur.

**⚠️ Si GitHub refuse** avec un message du type *« this workflow requires approval »* : sur la page
Actions, clique sur le bouton **I understand my workflows, go ahead and enable them** (une seule fois
pour tout le dépôt), puis relance l'étape 1.4.

**1.4 Lancer le build** (si rien ne a démarré tout seul) :

- page `https://github.com/Souxch06/ValoriaTycoon/actions`
- à gauche, clique sur **Build and Validate ValoriaTycoon**
- à droite, bouton **Run workflow** ▾ → branche `arena/01a043a8-valoriatycoon` → bouton vert **Run workflow**.

**1.5 Lire le résultat** (le build dure ~3 minutes) :

| Ce que tu vois | Ce que tu fais |
| --- | --- |
| ✅ cercle vert | passe à l'étape 2 |
| ❌ croix rouge | clique sur la ligne du build → étape rouge **Compiler le plugin et le plugin d'économie** → dans le journal, **Ctrl+F** `ERROR` → tu me colles les 5 à 15 lignes autour |

Le texte exact que j'attends, par exemple :

```
[ERROR] /home/runner/work/ValoriaTycoon/ValoriaTycoon/sources/plugin/.../HologramsUtil.java:[57,24]
  incompatible types: ...
```

---

## Étape 2 — Récupérer les 2 jar (30 secondes, une fois le build vert)

**2.1** Ouvre `https://github.com/Souxch06/ValoriaTycoon/actions/workflows/build.yml`
**2.2** Clique sur la **dernière ligne verte** de la liste (le build qui vient de finir).
**2.3** Descends tout en bas de la page, section **Artifacts** :
→ **ValoriaTycoon-jar** → télécharge le `.zip`.
**2.4** Décompresse le zip : tu dois obtenir **2 fichiers** :

```
ValoriaTycoon-v1.6.3.jar
ValoriaEconomy-v1.6.3.jar
```

**✅ Ça y est si** : `ValoriaTycoon-v1.6.3.jar` fait environ **2,5 Mo** (il embarque les bibliothèques)
et `ValoriaEconomy-v1.6.3.jar` entre **20 et 60 Ko** (il est tout petit, c'est normal). Si le second
n'est pas dans le zip, dis-le-moi : ça veut dire que l'étape « Vérifier les deux JAR produits » n'a pas
fonctionné comme prévu.

---

## Étape 3 — Installer sur le serveur de test (5 minutes, serveur arrêté)

**3.1** Arrête complètement le serveur (commande `stop`, pas `kill -9`, pour que les données soient sauvegardées).

**3.2** Dans le dossier `plugins/` du serveur de test, **supprime** :

```
Vault.jar   (ou VaultUnlocked*.jar)
EssentialsX.jar
HoloEasy*.jar
ProtocolLib.jar
ValoriaTycoon-v1.6.3.jar   (l'ancien)
```

**3.3** **Copie** les 2 nouveaux jar à la place : `ValoriaTycoon-v1.6.3.jar` + `ValoriaEconomy-v1.6.3.jar`.

**3.4** Si tu veux garder l'argent des joueurs : avant de démarrer, lis `docs/ECONOMIE.md`
(section « Importer les soldes EssentialsX ») — sinon tout le monde repart avec le solde de départ
(500 $ par défaut, réglable dans `plugins/ValoriaEconomy/config.yml`).

**3.5** Démarre le serveur, **garde la console ouverte**.

**✅ Le démarrage est bon si** tu vois ces lignes (dans cet ordre à peu près) :

```
[ValoriaEconomy] fournisseur d'économie enregistré (0 compte(s)).
[ValoriaTycoon] hologrammes : 0 enregistré(s), 0 deja visible(s), portee de vue 300 bloc(s).
[ValoriaTycoon] ValoriaTycoon a été activé.
```

**et aucun** de ces messages : `Unknown/missing dependency plugins`, `HoloEasy not found`,
`Vault not found`, `NoClassDefFoundError`, `ExceptionInInitializerError`.

⚠️ Ne tiens **pas** compte du vieux `Initializing Legacy Material Support` /
`Legacy plugin MCServerHost does not specify an api-version` : c'est un autre plugin, pas celui-là.

---

## Étape 4 — Les 9 tests en jeu (10 minutes)

Connecte-toi avec **deux comptes** (ou un compte + la console) et fais-les dans l'ordre.

| # | Ce que tu tapes / fais | Ce qui doit se passer |
| --- | --- | --- |
| 1 | `/plugins` | `ValoriaEconomy` et `ValoriaTycoon` **en vert**, pas en rouge |
| 2 | `/eco set Souxch 10000` (depuis la console) | message de solde fixé |
| 3 | `/bal` | affiche `10,000.00 $` (la monnaie vient de **ton** plugin, pas d'Essentials) |
| 4 | `/generators`, achète un générateur, pose-le | l'argent baisse, **une étiquette apparaît au-dessus du bloc** |
| 5 | attends 30 s puis `/selldrops all` | l'argent remonte, les drops disparaissent du sac |
| 6 | `/generators` → clic sur le générateur → bouton **améliorer** | le prix s'affiche en chiffres (pas `%upgradePrice%`), l'argent baisse du bon montant |
| 7 | **redémarre le serveur** | l'étiquette est toujours là, **en un seul exemplaire** (pas de doublon) |
| 8 | `/ah sell 5` puis, avec le 2ᵉ compte, `/ah` → acheter le lot | l'acheteur reçoit **les 5 items d'un coup** ou rien du tout, le vendeur est crédité, personne n'a d'items au sol |
| 9 | `/sb` | le tableau de bord latéral s'affiche avec ton pseudo et ton solde |

**Test bonus (le plus utile, 1 minute)** : relance un `/pay` et tue le serveur **en plein milieu**
(`kill -9`), puis redémarre. Les deux soldes doivent être soit **les deux d'avant**, soit **les deux
après** — jamais un seul mouvement. C'est le point qui prouve que la monnaie maison est sûre.

**Si un test échoue** : copie-moi le message de la console (la ligne avec `ERROR` ou `WARN`, et les
3 lignes au-dessus). C'est exactement ce qu'il me faut pour corriger.

---

## Étape 5 — Livrer sur le vrai serveur (seulement après étapes 1 + 4 réussies)

Deux chemins, choisis-en un :

**5.1 — Le plus sûr (recommandé) : copier à la main.**
Tant que tu n'as pas fait le point 5.2, ne merge **pas** : `main` envoie automatiquement un jar sur
ton serveur de prod par SFTP, et **un seul** des deux (le plugin d'économie n'arriverait pas → économie
cassée). Donc : télécharge les 2 jar (étape 2), envoie-les dans `plugins/` de MCServerHost par FTP,
redémarre.

**5.2 — Pour que le déploiement automatique livre les 2 jar (à faire une fois, par un humain).**
Sur `https://github.com/Souxch06/ValoriaTycoon/edit/main/.github/workflows/deploy.yml`, remplace le bloc
déjà écrit pour toi dans `docs/DEPLOY-2-JARS.md` (2 petits blocs YAML : un `find` qui exclut le jar
d'économie, et une étape SFTP qui envoie les 2 fichiers) → **Commit changes**.
Sans ça, le déploiement automatique continuera de ne livrer que ValoriaTycoon.

**5.3 — Merger la PR #7.**
`https://github.com/Souxch06/ValoriaTycoon/pull/7` → onglet **Conversation** → bouton vert
**Merge pull request** (uniquement si le build est ✅ **et** les 9 tests ✅).

**5.4 — Rollback (si un jour ça casse en prod) :** sers-toi des jar de la dernière release/artefact
vert précédent, remets-les dans `plugins/`, redémarre. Garde les anciens jar quelque part : c'est ta
seule vraie ceinture.

---

## Ce que je ne peux pas faire à ta place (et pourquoi, en une ligne)

| Bloqué parce que | Détail |
| --- | --- |
| coller dans `.github/workflows/` | mon compte GitHub (l'appli « Arena ») n'a **pas** la permission `workflows` → push rejeté, API renvoie 403 |
| dire « ça compile » | aucun `javac` dans mon environnement → seul le build GitHub le prouve |
| jouer sur ton serveur | je n'ai pas accès à ton serveur de test ni à ta prod (et je ne mergerai pas sans ton feu vert) |

## Les 3 règles du dépôt si un jour tu modifies le code toi-même

1. **Une seule classe recompilée à la fois** : la liste est dans `<includes>` du `pom.xml`
   (21 fichiers aujourd'hui). Tout fichier ajouté là doit avoir un contrat dans
   `scripts/check-sources-java.mjs` — sinon le contrôle de surface est aveugle.
2. **Ne jamais toucher `artifacts/original/`** : c'est le jar d'origine `GensPlus`, jamais livrable.
3. **Après une modif de `artifacts/extracted/`** : relancer `python3 scripts/build-reference-jar.py`
   (le build échoue si ce jar de classpath est obsolète — c'est voulu).

## Où lire la suite, dans le dépôt

- `docs/TUTORIEL-PAPER-26.md` — pourquoi Paper 26.2 cassait tout, et ce qui a été réparé.
- `docs/ECONOMIE.md` — `/bal`, `/pay`, `/eco`, import des soldes, rollback.
- `docs/HOLOGRAMMES.md` — comment marche le moteur d'hologrammes, ses limites assumées.
- `docs/STRUCTURE.md` — à quoi sert chaque dossier et chaque script.
