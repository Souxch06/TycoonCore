# Brancher le système automatique (les deux .jar jusqu'au serveur)

Le moteur est **écrit et testé** : `scripts/ci-release-and-deploy.sh` (143 lignes, testé hors CI :
refus si un jar manque, refus si un jar n'est pas un zip, `DRY_RUN`, sauvegarde `plugins/_sauvegarde-<date>/`,
contrôle de taille **côté serveur**, refus si un secret manque). Le build publie déjà l'artefact des
deux jar. Ce qui reste à faire est **uniquement de l'installation de fichiers de workflow**, et
GitHub refuse que l'agent écrive dans `.github/workflows/` :

```
! [remote rejected] … (refusing to allow a GitHub App to create or update workflow
  `.github/workflows/build.yml` without `workflows` permission)
```

D'où deux options, au choix.

---

## Option A — tu donnes la permission, je fais tout (1 clic de ton côté)

1. Ouvre **https://github.com/settings/installations**
2. Clique sur l'application **Arena** (celle qui pousse sur ce dépôt) → **Configure**
3. En bas, rubrique **Repository permissions** → **Workflows** → passe de *Read-only* à **Read & write**
   (puis **Save**)
4. Écris‑moi « permission donnée »

Je m'occupe alors de : ouvrir une PR sur `main` avec `build.yml` + `deploy-serveur.yml`, neutraliser
l'ancien `deploy.yml`, merger, et déclencher un `dry_run` pour te montrer ce qui **serait** envoyé,
puis l'envoi réel quand tu dis go.

> Reversible : tu peux remettre *Read-only* juste après, la permission ne sert qu'à écrire ces fichiers.

---

## Option B — tu colles 3 fois (5 minutes), dans cet ordre

Le pipeline complet est : `push` → **build** → `merge` sur `main` → **release `build-latest`** →
**dépôt automatique des deux jar** sur le serveur.

### B.1 — DÉSACTIVER l'ancien déploiement (sinon il écrase le nouveau avec un seul jar)
Édite **https://github.com/Souxch06/ValoriaTycoon/edit/main/.github/workflows/deploy.yml**
et supprime les **4 lignes** du bloc `push:` (lignes 3 à 6 : `push:`, `branches:`, `- main`, et la ligne
`  workflow_dispatch:` reste, elle) :

```yaml
on:
  workflow_dispatch:
```
→ **Commit changes** (sur `main`). L'ancien envoi SFTP ne se déclenchera donc plus jamais tout seul ;
il reste utilisable à la main si besoin.

### B.2 — Merger la PR #7
**https://github.com/Souxch06/ValoriaTycoon/pull/7** → **Merge pull request**.
C'est sans risque **maintenant** : plus aucun `push:` sur `main` ne déploie (B.1 fait exprès), et les
fichiers de workflow ne sont lus que depuis `main` — donc `main` contient juste le plugin sans pipeline
pendant quelques minutes.

### B.3 — Installer les deux nouveaux workflows sur `main`

| créer/éditer | coller le contenu de |
| --- | --- |
| https://github.com/Souxch06/ValoriaTycoon/edit/main/.github/workflows/build.yml | https://github.com/Souxch06/ValoriaTycoon/raw/main/docs/CI-A-COLLER.yml |
| https://github.com/Souxch06/ValoriaTycoon/new/main?filename=.github%2Fworkflows%2Fdeploy-serveur.yml | https://github.com/Souxch06/ValoriaTycoon/raw/main/docs/CI-DEPLOY-A-COLLER.yml |

Pour chacun : **Ctrl+A** dans l'éditeur → **Suppr** → **Ctrl+V** (le contenu du lien *raw*, ouvrir dans
un onglet puis copier) → **Commit changes** directement sur `main`.

Le dépôt sur `main` de `build.yml` **déclenche** la chaîne : build → vert → publication de la release
`build-latest` → `deploy-serveur.yml` se lance → **les deux jar partent sur le serveur**.

---

## Ce que le système fait, et ce qu'il ne fait pas

| | |
| --- | --- |
| ✅ automatique | build + 15 contrôles, release `build-latest` réécrite, téléchargement des jar depuis la release, vérification du **contenu** (classes attendues, `plugin.yml` avec `api-version`, **aucune** API tierce), sauvegarde des jar en place dans `plugins/_sauvegarde-<horodatage>/`, envoi des **deux** jar, contrôle de taille octet par octet côté serveur |
| ⛔ jamais automatique | **redémarrer le serveur** (aucun robot ne peut le faire), et la release n'est publiée que depuis `main` (`if: github.ref == 'refs/heads/main'`) : une branche en cours ne peut pas toucher la prod |
| 🔐 optionnel, recommandé | **Settings → Environments → production → Required reviewers** : GitHub attendra une validation humaine **avant chaque envoi**. À cocher si tu veux garder un filet |

## Try before you trust
Une fois B.3 en place, teste sans risque : **Actions → Déploiement auto (deploy-serveur) → Run
workflow** → branche `main` → laisse **`dry_run` coché** → Run. Le log affiche exactement la session
SFTP qui **serait** jouée, sans rien envoyer. Décoche `dry_run` quand tu veux l'envoi réel.

## Si ça casse
- l'étape d'envoi échoue → les anciens jar sont intacts dans `plugins/_sauvegarde-<horodatage>/` :
  un `rename` inverse dans `plugins/` et un redémarrage reviennent en arrière ;
- `gh release create` refusé (droits de release) → l'étape est un **avertissement**, le build reste vert
  et l'artefact du run contient toujours les deux jar (téléchargement manuel) ;
- plus rien ne se déploie → vérifie d'abord que `deploy.yml` n'a plus de `push:` (B.1) : c'est le seul
  endroit où les deux pipelines se marchent dessus.
