# Déployer les deux jars

## Voie « tout automatique » : le dépôt sur le serveur par la CI

Deux blocs, un seul collage chacun (les fichiers `.github/workflows/*` sont les seuls que l'agent ne
peut pas écrire — permission `workflows` refusée par GitHub, vérifiée quatre fois) :

| coller dans | contenu à coller | effet |
| --- | --- | --- |
| `.github/workflows/build.yml` | `docs/CI-A-COLLER.yml` | build + contrôles + **publication de la release `build-latest`** (qui déclenche le déploiement) |
| `.github/workflows/deploy-serveur.yml` (nouveau fichier) | `docs/CI-DEPLOY-A-COLLER.yml` | **télécharge les deux jar de la release, vérifie leur contenu, les dépose sur le serveur** via les secrets SFTP déjà en place, avec sauvegarde préalable dans `plugins/_sauvegarde-<horodatage>/` |

Circulation : `git push` → build vert → release `build-latest` réécrite → **workflow de déploiement
déclenché par la release** → serveur à jour. Aucune étape manuelle, et le déploiement ne peut partir
que d'une release, donc jamais d'un build rouge ou d'une branche en cours.

Sécurité intégrée (dans `scripts/ci-release-and-deploy.sh`, testé ici hors CI) :
- les DEUX jar sont exigés (jamais le plugin sans son économie) ;
- sauvegarde des jar en place avant écrasement → rollback = `rename` inverse dans `plugins/` ;
- contrôle de taille **côté serveur** après envoi : un SFTP qui tronque fait échouer l'étape ;
- `dry_run` possible depuis *Run workflow* (affiche la session SFTP sans rien envoyer) ;
- `environment: production` : si tu actives une règle de protection sur cet environnement, GitHub
  demandera une validation humaine **explicite** avant l'envoi — c'est le bouton à cocher si tu veux
  garder un feu vert manuel.

## Récupérer les jars (3 voies, de la plus simple à la plus automatique)

| voie | comment |
| --- | --- |
| **Release permanente** | https://github.com/Souxch06/ValoriaTycoon/releases/tag/build-latest → les deux `.jar` en téléchargement direct, sans jamais expirer. Actif dès que le workflow collé contient l'étape « Publier les JAR sur la release `build-latest` » (elle est dans `docs/CI-A-COLLER.yml`) |
| **Artefact du run** | https://github.com/Souxch06/ValoriaTycoon/actions/workflows/build.yml → dernière ligne verte → en bas, **Artifacts → ValoriaTycoon-jar** (zip des deux jars, expire au bout de 90 jours) |
| **Build local** | `mvn -B clean package` puis `target/ValoriaTycoon-v1.6.3.jar` + `target/ValoriaEconomy-v1.6.3.jar` |

Ce que l'agent **ne peut pas** faire, mesuré : rapatrier les octets. Les trois domaines de fichiers de
GitHub (`productionresultssa*.blob.core.windows.net` pour les artefacts, `objects.githubusercontent.com`
pour les assets, `uploads.github.com` pour le téléversement) sont refusés par le réseau de la sandbox,
tandis que `api.github.com` et `github.com` passent — d'où la publication par le runner, pas par moi.

> **Le second jar s'appelle `ValoriaEconomy-v<version>.jar`, et un seul endroit le décide** :
> `<finalName>` de l'exécution `economy-plugin-jar` du `maven-assembly-plugin`, avec
> `appendAssemblyId=false`. Trois pièges vérifiés en build, tous bloqués par des contrôles de
> `scripts/verify-paper26-compat.py` (101 points) :
>
> | piège | symptôme | règle |
> | --- | --- | --- |
> | `<finalName>` dans le **descripteur** | `Unrecognised tag: 'finalName'` (#33155795647) | le schéma `assembly-2.1.0` ne le définit que dans un `<format>` (multi‑formats) |
> | `appendAssemblyId=true` sans finalName | le paquet sort sous le **`finalName` global** : `ValoriaTycoon-v1.6.3-economy.jar` (#33156892660) | nommer dans le pom, pas dans le descripteur |
> | une 2ᵉ étape qui renomme (`antrun` + `<move>`) | `Could not find file …economy.jar` dès que le nom réel diffère | supprimer : l'assembleur écrit déjà le bon nom |
>
> Bonus : les commentaires XML n'ont **pas** le droit de contenir `--` (ni `<`) — un `--` ferme le
> commentaire et Maven refuse le pom avec un simple « not well‑formed ». Le contrôle
> « aucun commentaire illégal » protège `pom.xml` et `src/assembly/economy.xml`.

`deploy.yml` ne copie qu'un seul fichier (`find target -maxdepth 1 -name "*.jar" | head -n 1`). Depuis
l'ajout de `ValoriaEconomy`, il faut déposer **les deux** jars dans `plugins/`.

Remplacer l'étape `Envoyer le plugin sur MCServerHost via SFTP` par :

```yaml
      - name: Envoyer les plugins sur MCServerHost via SFTP
        env:
          SFTP_HOST: ${{ secrets.SFTP_HOST }}
          SFTP_PORT: ${{ secrets.SFTP_PORT }}
          SFTP_USERNAME: ${{ secrets.SFTP_USERNAME }}
          SSHPASS: ${{ secrets.SFTP_PASSWORD }}
          JAR_PATH: ${{ steps.select-jar.outputs.jar_path }}
        run: |
          sudo apt-get update -y && sudo apt-get install -y sshpass
          ECONOMY_JAR=$(find target -maxdepth 1 -type f -name "ValoriaEconomy-*.jar" | head -n 1)
          sshpass -e sftp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT" "$SFTP_USERNAME@$SFTP_HOST" <<EOF
          put "$JAR_PATH" plugins/
          EOF
          if [ -n "$ECONOMY_JAR" ]; then
            sshpass -e sftp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT" "$SFTP_USERNAME@$SFTP_HOST" <<EOF
          put "$ECONOMY_JAR" plugins/
          EOF
          fi
```

Et l'étape `Identifier le JAR final du plugin` doit exclure le jar d'économie, sinon `find … | head -1`
peut choisir le mauvais :

```yaml
          FINAL_JAR=$(find target -maxdepth 1 -type f -name "*.jar" \\
            ! -name "original-*" ! -name "*-sources.jar" ! -name "*-javadoc.jar" \\
            ! -name "ValoriaEconomy-*" | head -n 1)
```

Note : `.github/workflows/` est protégé côté application GitHub de cette session (permission
`workflows` refusée), donc ces deux modifications sont à coller par un humain — soit via l'éditeur web
du dépôt, soit en local avec `git add .github/workflows/deploy.yml && git push`.

## Option « rapport de compilation » (le plus utile quand le build est rouge)

`deploy.yml` se déclenche sur `push: main` **et** sur `workflow_dispatch` (bouton *Run workflow*,
n'importe quelle branche). Sur une branche, le SFTP ne s'exécute que si la compilation passe — donc un
run manuel sur `arena/…` est **sans danger** pour la prod.

Pour ne plus avoir à recopier le journal Maven à la main, ajouter ces 3 blocs à
`.github/workflows/deploy.yml` (à coller par un humain, l'API refusant l'écriture dans
`.github/workflows/` pour le compte de la session) :

**1. en tête de fichier** — remplacer le bloc `name:`/`on:` par :

```yaml
name: Build and Deploy ValoriaTycoon

on:
  push:
    branches:
      - main
  workflow_dispatch:
    inputs:
      report:
        description: 'Diagnostic : compile et publie le rapport sur la PR, SANS rien envoyer sur le serveur'
        type: boolean
        default: false

permissions:
  contents: read
  pull-requests: write
```

**2. remplacer l'étape `Compiler le plugin`** par :

```yaml
      - name: Compiler le plugin
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: bash scripts/ci-maven-report.sh

      - name: Rapport de compilation (mode diagnostic, sans deploiement)
        if: ${{ github.event.inputs.report == 'true' }}
        run: exit 1
```

**3. ajouter une garde au SFTP** (une ligne, sous `- name: Envoyer le plugin sur MCServerHost via SFTP`) :

```yaml
        if: ${{ github.event.inputs.report != 'true' }}
```

Usage : **Actions** → *Build and Deploy ValoriaTycoon* → **Run workflow** → branche `arena/…` →
coche **Diagnostic** → *Run workflow*. Le rapport (fichier, ligne, extrait du source, contrôles du
dépôt) arrive **tout seul en commentaire de la PR #7** : il n'y a plus rien à copier.

Le script `scripts/ci-maven-report.sh` est utilisable hors CI aussi (`bash scripts/ci-maven-report.sh`),
il se contente alors d'imprimer le rapport — pratique si Maven est installé sur ta machine.

## Et pour livrer les DEUX jars sur la prod

Remplacer l'étape `Envoyer le plugin sur MCServerHost via SFTP` par :
