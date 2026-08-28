# Les 3 fichiers à coller (zéro frappe : copier/coller seulement)

Ordre important : **neutraliser d'abord**, sinon l'ancien pipeline (un seul jar) part à chaque merge.

| # | Sur GitHub | Contenu à coller (bouton « Copy raw content ») |
| --- | --- | --- |
| 1 | `main` → `.github/workflows/deploy.yml` → crayon → coller par‑dessus | https://github.com/Souxch06/ValoriaTycoon/raw/arena/01a043a8-valoriatycoon/docs/paste/deploy-neutralise.yml |
| 2 | `main` → `.github/workflows/build.yml` → coller par‑dessus (ou créer s'il manque) | https://github.com/Souxch06/ValoriaTycoon/raw/arena/01a043a8-valoriatycoon/docs/paste/build.yml |
| 3 | `main` → « Add file → Create new file » → nom `​.github/workflows/deploy-serveur.yml` → coller | https://github.com/Souxch06/ValoriaTycoon/raw/arena/01a043a8-valoriatycoon/docs/paste/deploy-serveur.yml |

Sur mobile : ouvre le lien *raw*, appui long → *Select all* → *Copy*, puis colle dans l'éditeur GitHub
(sélectionne tout avec un appui long dans la zone → *Replace*).

Après le collage n° 2, GitHub met à jour `main` → le build se lance → s'il est vert, la release
`build-latest` est publiée → `deploy-serveur.yml` dépose **les deux jar** sur le serveur, avec sauvegarde
préalable dans `plugins/_sauvegarde-<date>/`. Ensuite plus rien à faire : chaque `git push` que je pousse
relance la chaîne, et je te préviens quand le serveur doit être redémarré (ça, c'est le seul geste qui
reste humain).
