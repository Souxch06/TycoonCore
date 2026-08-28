# `docs/paste/` : le seul fichier qui se colle

Il ne reste ici qu'**un** contenu, parce qu'un workflow recopié à deux endroits finit par diverger — et
c'est exactement ce qui s'est passé dans ce dépôt : `build.yml` et `deploy-serveur.yml` y avaient leur
propre copie, qui décrivait encore un build « deux jar » sans l'étape de release, donc un pipeline muet.

| fichier | rôle |
| --- | --- |
| `deploy-neutralise.yml` | à coller dans `.github/workflows/deploy.yml` **sur `main`** : il retire le
  déclencheur `push: main`, qui enverait un seul jar sur le serveur à chaque merge |

Les deux autres contenus à coller se lisent à la source (jamais depuis ce dossier) :

- `.github/workflows/build.yml` ← `docs/CI-A-COLLER.yml`
- `.github/workflows/deploy-serveur.yml` ← `docs/CI-DEPLOY-A-COLLER.yml`

`scripts/verify-ci-copies.py`, appelé par une étape du build, vérifie que ces fichiers ne se contredisent
pas. Pour les récupérer en texte brut dans le navigateur :

```
https://github.com/Souxch06/ValoriaTycoon/raw/arena/01a043a8-valoriatycoon/docs/CI-A-COLLER.yml
https://github.com/Souxch06/ValoriaTycoon/raw/arena/01a043a8-valoriatycoon/docs/CI-DEPLOY-A-COLLER.yml
```
