# Tuto : finir l'installation (12 minutes, 3 collages)

> Objectif : que chaque modification que je pousse finisse **toute seule** en `.jar` sur ton serveur,
> avec les deux fichiers (plugin + économie), une sauvegarde préalable, et un contrôle de taille.
> Tu n'as plus qu'à **redémarrer le serveur** — ça, aucun robot ne le fait à ta place.

Prérequis d'état (déjà vrai, vérifié) : build ✅ `aca3909` (15 étapes), PR #7 **mergeable**, les deux
`.jar` déjà testés sur ton serveur de test.

---

## Étape 1 — Merger la PR #7 (1 clic)

1. Ouvre **https://github.com/Souxch06/ValoriaTycoon/pull/7**
2. Onglet **Conversation** (le premier)
3. Descends tout en bas → bouton vert **Merge pull request** → **Confirm merge**

Sans le faire, rien ne bouge : tout le travail est sur la branche `arena/01a043a8-valoriatycoon`.
C'est sans danger : `main` n'a pour l'instant qu'un ancien `deploy.yml` qui se déclenche sur `push: main`,
et il ne sera neutralisé qu'à l'étape 2 — **enchaîne les deux tout de suite**, dans cet ordre.

---

## Étape 2 — Couper l'ancien déploiement (1 collage)

Pourquoi : l'ancien workflow n'envoie **qu'un seul** `.jar`. S'il tourne encore à chaque merge, il
laisse le serveur avec le plugin **sans son économie**.

1. Ouvre **https://github.com/Souxch06/ValoriaTycoon/edit/main/.github/workflows/deploy.yml**
2. Dans la zone de texte : **appui long → Select all → Delete** (ou Ctrl+A puis Suppr)
3. Colle le **bloc A** ci‑dessous (copie‑colle depuis le fichier `docs/paste/deploy-neutralise.yml` du
   dépôt, ou depuis ce tuto si tu le lis en version brute)
4. Déroule tout en bas → bouton vert **Commit changes** (laisse « Commit directly to `main` »)

## Étape 3 — Installer les deux nouveaux workflows (2 collages)

### 3.1 `build.yml` (celui qui publie la release qui déclenche le dépôt)
1. **https://github.com/Souxch06/ValoriaTycoon/edit/main/.github/workflows/build.yml**
   *(si « 404 / file not found » : « Add file → Create new file » et tape le nom
   `.github/workflows/build.yml`)*
2. Select all → Delete → colle le **bloc B**
3. **Commit changes**

### 3.2 `deploy-serveur.yml` (celui qui dépose les deux jar)
1. **https://github.com/Souxch06/ValoriaTycoon/new/main?filename=.github%2Fworkflows%2Fdeploy-serveur.yml**
   *(le nom du fichier est déjà rempli par ce lien)*
2. Colle le **bloc C**
3. **Commit changes** → GitHub affiche le fichier créé, c'est bon

⏱ Après ce dernier commit : `main` a changé → le build se relance → s'il est vert, la release
`build-latest` est publiée → le dépôt se déclenche **en mode simulation** (`dry_run` par défaut, il
n'envoie rien). C'est voulu : on regarde d'abord.

---

## Étape 4 — Tester sans risque, puis armer (2 clics)

1. **https://github.com/Souxch06/ValoriaTycoon/actions/workflows/deploy-serveur.yml**
2. **Run workflow** ▾ → branche `main` → laisse **`dry_run` coché** → **Run workflow**
3. Ouvre le run → étape **« Déploiement (simulation) »** : tu dois voir la liste exacte de ce qui serait
   fait, du genre
   ```
   -mkdir plugins/_sauvegarde-20260828-131502
   rename plugins/ValoriaTycoon-v1.6.3.jar plugins/_sauvegarde-…/ValoriaTycoon-v1.6.3.jar
   rename plugins/ValoriaEconomy-v1.6.3.jar plugins/_sauvegarde-…/ValoriaEconomy-v1.6.3.jar
   put target/ValoriaTycoon-v1.6.3.jar target/ValoriaEconomy-v1.6.3.jar plugins/
   ```
4. Si ça te va : refais **Run workflow**, **décoche `dry_run`** → les deux jar partent vraiment sur le
   serveur (les anciens sont d'abord sauvegardés), puis **redémarre le serveur**.

**Filet recommandé** (au choix, 30 s) : **Settings → Security → Environments → production →
Edit → Required reviewers → ajoute‑toi → Save**. Dès lors, chaque dépôt attendra ton
**Review deployments** avant d'écrire sur le serveur.

---

## Étape 5 — Ensuite, ton seul réflexe
| tu veux | tu fais |
| --- | --- |
| une correction / une nouvelle fonctionnalité | tu me le demandes → je pousse → build → je te dis « merge » → tu cliques → les jar arrivent sur le serveur |
| savoir si c'est bon | https://github.com/Souxch06/ValoriaTycoon/actions (vert = livré) |
| récupérer un jar à la main | https://github.com/Souxch06/ValoriaTycoon/releases/tag/build-latest |
| revenir en arrière | dans `plugins/_sauvegarde-<date>/`, un `rename` inverse + redémarrage |
| déployer sans rien changer | Actions → deploy-serveur → Run workflow (dry_run décoché) |

---

## Les 3 blocs (à copier tels quels, en entier)

> Ouvre ce tuto en `raw` (bouton **Copy raw content** en haut à droite du fichier) et copie les blocs
> depuis là : dans le rendu GitHub, les tabulations et les retraits restent intacts, mais en `raw` tu es
> certain de ne rien perdre.

### Bloc A → `.github/workflows/deploy.yml` (neutralisé)

```yaml
name: Build and Deploy ValoriaTycoon

on:
  # Neutralise le declencheur automatique : le depot des DEUX jars est assure par
  # deploy-serveur.yml (declenche par la release `build-latest`). Ce fichier ne sert plus
  # qu'a un declenchement manuel, et a l'historique.
  workflow_dispatch:

jobs:
  build-and-deploy:
    name: Build et Déploiement SFTP
    runs-on: ubuntu-latest

    steps:
      - name: Récupérer le code
        uses: actions/checkout@v4

      - name: Installer Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven

      - name: Compiler le plugin
        run: mvn clean package -DskipTests

      - name: Identifier le JAR final du plugin
        id: select-jar
        run: |
          mkdir -p target/deploy
          # Recherche du JAR final en excluant les JARs annexes (original-*, sources, javadoc)
          FINAL_JAR=$(find target -maxdepth 1 -type f -name "*.jar" \
            ! -name "original-*" \
            ! -name "*-sources.jar" \
            ! -name "*-javadoc.jar" \
            | head -n 1)

          if [ -z "$FINAL_JAR" ]; then
            echo "::error::Aucun JAR final du plugin trouvé dans target/ !"
            exit 1
          fi

          echo "JAR final identifié : $(basename "$FINAL_JAR")"
          cp "$FINAL_JAR" target/deploy/
          echo "jar_path=target/deploy/$(basename "$FINAL_JAR")" >> "$GITHUB_OUTPUT"

      - name: Envoyer le plugin sur MCServerHost via SFTP
        env:
          SFTP_HOST: ${{ secrets.SFTP_HOST }}
          SFTP_PORT: ${{ secrets.SFTP_PORT }}
          SFTP_USERNAME: ${{ secrets.SFTP_USERNAME }}
          SSHPASS: ${{ secrets.SFTP_PASSWORD }}
          JAR_PATH: ${{ steps.select-jar.outputs.jar_path }}
        run: |
          sudo apt-get update -y && sudo apt-get install -y sshpass
          sshpass -e sftp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT" "$SFTP_USERNAME@$SFTP_HOST" <<EOF
          put "$JAR_PATH" plugins/
          bye
          EOF
```

### Bloc B → `.github/workflows/build.yml`

```yaml
name: Build and Validate ValoriaTycoon

# Workflow de validation : il compile et contrôle les deux jar, il ne deploie JAMAIS
# (le SFTP reste uniquement dans .github/workflows/deploy.yml, reserve a main).

on:
  # chaque push sur la branche relance le build tout seul : plus rien a cliquer
  push:
    branches-ignore:
      - main
  pull_request:
  workflow_dispatch:

permissions:
  # sert UNIQUEMENT a publier le rapport d'erreurs (commentaire de PR, puis repli en commit de
  # docs/DERNIER-LOG-CI.md). Aucune etape ne deploie, aucun secret serveur n'est utilise.
  contents: write
  pull-requests: write

jobs:
  build-and-validate:
    name: Compilation et contrôles de compatibilité
    runs-on: ubuntu-latest

    steps:
      - name: Récupérer le code
        uses: actions/checkout@v4

      - name: Installer Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven

      - name: Vérifier le renommage de marque dans les classes livrées
        run: python3 scripts/rebrand-classes.py --check

      - name: Vérifier les correctifs des classes vendorisées
        run: python3 scripts/patch-class-version-patterns.py --check

      - name: Vérifier l'installation du pont NBT (PersistentDataContainer)
        run: python3 scripts/install-nbt-bridge.py --check

      - name: Vérifier que le plugin ne dépend d'aucune API tierce (Vault, HoloEasy retirés)
        run: python3 scripts/selfmade-api-patch.py --check

      - name: Vérifier les imports des fichiers compilés (sans JDK)
        run: python3 scripts/verify-source-imports.py

      - name: Vérifier la cohérence de l'extraction
        run: python3 scripts/verify-extraction.py

      - name: Compiler le plugin et le plugin d'économie
        # capte la sortie maven, publie les erreurs javac en annotation + commentaire de PR,
        # puis rejoue les controles du depot
        run: bash scripts/ci-maven-report.sh

      - name: Vérifier la surface publique des sources recompilées (sans JDK)
        run: |
          npm install --no-save java-parser@3
          node scripts/check-sources-java.mjs

      - name: Vérifier la couverture de l'API d'économie interne
        run: |
          python3 scripts/generate-economy-api.py --check
          python3 scripts/verify-economy-api.py

      - name: Vérifier la compatibilité Paper 26.x du JAR
        # Les echecs d'un simple « exit code 1 » ne sont exploitables a distance : chaque controle est
        # execute par scripts/ci-step.sh, qui recopie ses dernieres lignes dans le resume du job.
        run: |
          bash scripts/ci-step.sh "compatibilite du jar" \
            python3 scripts/verify-paper26-compat.py target/ValoriaTycoon-v1.6.3.jar -report-annotations
          bash scripts/ci-step.sh "renommage de marque dans le jar" \
            python3 scripts/rebrand-classes.py --check --jar target/ValoriaTycoon-v1.6.3.jar
          bash scripts/ci-step.sh "pont NBT dans le jar" \
            python3 scripts/install-nbt-bridge.py --check --jar target/ValoriaTycoon-v1.6.3.jar
          bash scripts/ci-step.sh "api tierces absentes du jar" \
            python3 scripts/selfmade-api-patch.py --check --jar target/ValoriaTycoon-v1.6.3.jar

      - name: Vérifier les deux JAR produits
        run: |
          bash scripts/ci-step.sh "contenu des deux jars" bash -c '
            set -e
            for jar in target/ValoriaTycoon-v1.6.3.jar target/ValoriaEconomy-v1.6.3.jar; do
              echo "--- $jar"; test -f "$jar"; ls -l "$jar"; unzip -l "$jar" | tail -3
            done
            unzip -l target/ValoriaEconomy-v1.6.3.jar | grep -q "plugin.yml"
            unzip -l target/ValoriaEconomy-v1.6.3.jar | grep -q "valoriaeconomy/ValoriaEconomyProvider.class"
            unzip -l target/ValoriaEconomy-v1.6.3.jar | grep -q "valoriaeconomy/ValoriaEconomy.class"
            unzip -l target/ValoriaTycoon-v1.6.3.jar | grep -q "valoriateconomy/Economy.class"
            unzip -l target/ValoriaTycoon-v1.6.3.jar | grep -q "valoriatycoon/hologram/HologramPool.class"
            unzip -l target/ValoriaTycoon-v1.6.3.jar | grep -q "module-info.class"
            ! unzip -l target/ValoriaTycoon-v1.6.3.jar | grep -q "xyz/arcadiadevs/valoriaeconomy/"
            ! unzip -l target/ValoriaTycoon-v1.6.3.jar | grep -qE "net/milkbowl|org/holoeasy"
            ! unzip -l target/ValoriaEconomy-v1.6.3.jar | grep -qE "net/milkbowl|org/holoeasy"
            echo "OK : contenu des deux jars conforme"
          '


      - name: Publier la release `build-latest` (déclencheur du déploiement)
        # Le script fait TOUT ici (logique dans le depot, pas dans le YAML que l'agent ne peut pas
        # ecrire) : il valide les deux jar, (re)ecrit le tag `build-latest`, televerse les assets,
        # verifie leur taille cote GitHub. La publication de la release est ce qui declenche le
        # workflow docs/CI-DEPLOY-A-COLLER.yml -> depot sur le serveur. Hors `main`, la release n'est
        # pas publiee pour eviter qu'une branche en cours deploie en production.
        # La release est le SEUL declencheur du depot : on ne la reecrit donc que depuis main, sinon
        # chaque push d'une branche en cours remplacerait `build-latest` et mettrait le serveur a jour
        # d'un etat non merge. Les runs de branche gardent leurs artefacts (telechargeables a la main).
        if: ${{ success() && github.ref == 'refs/heads/main' }}
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          PROJECT_VERSION: '1.6.3'
          DEPLOY: '0'
          RELEASE: '1'
        run: bash scripts/ci-release-and-deploy.sh

      - name: Publier les JAR construits
        uses: actions/upload-artifact@v4
        with:
          name: ValoriaTycoon-jar
          path: |
            target/ValoriaTycoon-v1.6.3.jar
            target/ValoriaEconomy-v1.6.3.jar
          if-no-files-found: error
```

### Bloc C → `.github/workflows/deploy-serveur.yml`

```yaml
name: Release et déploiement automatique

# Voie « tout automatique » : à chaque RELEASE GitHub (tag `build-latest` publié par le workflow de
# validation), ce workflow retélécharge les deux jar depuis les artefacts du run vérifié, puis les
# dépose sur le serveur — avec sauvegarde préalable et contrôle de taille octet par octet.
#
# Pourquoi un workflow séparé : `deploy.yml` se déclenche sur `push: main` et pousse un seul jar ; ce
# fichier-ci se déclenche sur un événement explicite (la release) et pousse les DEUX, ce qui est la
# condition pour que le serveur ne se retrouve jamais avec le plugin sans son économie.
#
# Il ne contient AUCUNE commande de build : il fait confiance au run déjà vert (artefact + 14 contrôles).
# Un `workflow_dispatch` manuel permet un déploiement à la demande, avec `dry_run` pour vérifier
# exactement ce qui serait fait sans rien envoyer.

on:
  release:
    types: [published]
  workflow_dispatch:
    inputs:
      tag:
        description: 'Nom de la release à déployer (asset du même nom que le tag)'
        type: string
        default: build-latest
      dry_run:
        description: 'Simuler (affiche la session SFTP, ne televerse rien)'
        type: boolean
        default: true

jobs:
  deploy:
    name: Déposer les deux jar sur le serveur
    runs-on: ubuntu-latest
    # Sur un événement `release`, on ne déploie que la release marquée « dernière version vérifiée » :
    # publier une release de test ne doit pas écraser le serveur.
    if: ${{ github.event_name != 'release' || github.event.release.tag_name == 'build-latest' }}
    environment: production   # règle la porte manuelle : Settings → Environments → production

    steps:
      - name: Récupérer le script de dépôt
        uses: actions/checkout@v4
        with:
          # On ne prend que le script : ce workflow ne construit rien, il deplace des fichiers deja
          # verifies. (Pas de `submodules`/`lfs` : inutiles, et c'est ce qui rend le checkout rapide.)
          sparse-checkout: |
            scripts/ci-release-and-deploy.sh
          sparse-checkout-mode: no-filter

      - name: Télécharger les jar depuis la release
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          # Passe par `env:`, JAMAIS par interpolation directe dans `run:` : une valeur d'entree
          # utilisateur injectee dans un shell est la faille classique des workflows.
          RELEASE_TAG: ${{ github.event.release.tag_name || inputs.tag }}
        run: |
          set -euo pipefail
          TAG="$RELEASE_TAG"
          mkdir -p target
          gh release download "$TAG" --pattern '*.jar' --dir target --clobber
          ls -l target
          # Le nom attend est pose par le pom (<finalName> de maven-jar-plugin et de l'assembleur).
          test -f target/ValoriaTycoon-v1.6.3.jar
          test -f target/ValoriaEconomy-v1.6.3.jar

      - name: Vérifier l'intégrité avant envoi
        run: |
          set -euo pipefail
          python3 - <<'PY'
          import zipfile, sys
          ok = True
          for path, need in (("target/ValoriaTycoon-v1.6.3.jar",
                              ("xyz/arcadiadevs/valoriatycoon/ValoriaTycoon.class",
                               "plugin.yml",
                               "xyz/arcadiadevs/valoriatycoon/hologram/HologramPool.class",
                               "xyz/arcadiadevs/valoriateconomy/Economy.class",
                               "module-info.class")),
                             ("target/ValoriaEconomy-v1.6.3.jar",
                              ("xyz/arcadiadevs/valoriaeconomy/ValoriaEconomy.class",
                               "plugin.yml"))):
              try:
                  names = set(zipfile.ZipFile(path).namelist())
              except Exception as error:
                  print("ERREUR: %s illisible (%s)" % (path, error)); ok = False; continue
              missing = [n for n in need if n not in names]
              if missing:
                  print("ERREUR: %s ampute de %s" % (path, missing)); ok = False
              if any(n.startswith(("net/milkbowl/", "org/holoeasy/")) for n in names):
                  print("ERREUR: %s embarque une API tierce" % path); ok = False
              if "plugin.yml" in names:
                  desc = zipfile.ZipFile(path).read("plugin.yml").decode("utf-8", "replace")
                  name = [l.split(":", 1)[1].strip() for l in desc.splitlines() if l.startswith("name:")][0]
                  api = [l.split(":", 1)[1].strip() for l in desc.splitlines() if l.startswith("api-version:")]
                  print("%s : %d entrees, plugin %s, api-version %s" % (path, len(names), name, api or "absente"))
                  if not api:
                      print("ERREUR: %s sans api-version (legacy material support, avertissement a chaque demarrage)" % path)
                      ok = False
              else:
                  print("ERREUR: %s sans plugin.yml" % path); ok = False
          sys.exit(0 if ok else 1)
          PY

      - name: Déploiement (simulation)
        if: ${{ github.event_name == 'workflow_dispatch' && inputs.dry_run }}
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          DEPLOY: '1'
          DRY_RUN: '1'
          RELEASE: '0'
          SFTP_HOST: ${{ secrets.SFTP_HOST }}
          SFTP_PORT: ${{ secrets.SFTP_PORT }}
          SFTP_USERNAME: ${{ secrets.SFTP_USERNAME }}
          SFTP_PASSWORD: ${{ secrets.SFTP_PASSWORD }}
        run: bash scripts/ci-release-and-deploy.sh

      - name: Déploiement sur MCServerHost
        if: ${{ github.event_name == 'release' || !inputs.dry_run }}
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          DEPLOY: '1'
          RELEASE: '0'
          SFTP_HOST: ${{ secrets.SFTP_HOST }}
          SFTP_PORT: ${{ secrets.SFTP_PORT }}
          SFTP_USERNAME: ${{ secrets.SFTP_USERNAME }}
          SFTP_PASSWORD: ${{ secrets.SFTP_PASSWORD }}
        run: bash scripts/ci-release-and-deploy.sh
```



Ils existent aussi en fichiers, avec bouton « Copy raw content », si c'est plus pratique :
https://github.com/Souxch06/ValoriaTycoon/tree/arena/01a043a8-valoriatycoon/docs/paste

Les trois blocs complets sont juste en dessous, dans ce fichier — copie depuis la version
brute (`raw`) pour ne pas casser l'indentation YAML.

Les blocs B et C sont **exactement** les fichiers `docs/CI-A-COLLER.yml` et `docs/CI-DEPLOY-A-COLLER.yml`
validés par le build vert `aca3909`. Une indentation YAML perdue = workflow invalide, et le refus
s'affiche sans détail : ne réécris rien à la main, copie. Le bloc A n'est que le `deploy.yml` actuel de
`main` avec son déclencheur `push:` retiré (rien d'autre n'a bougé, donc rien d'autre à re‑vérifier).

---

## Ce qui ne marchera pas, et pourquoi (pour ne pas chercher)
- **Je ne peux pas faire les étapes 2 et 3 à ta place** : GitHub refuse qu'une app sans la permission
  `workflows` écrive dans `.github/workflows/`, même avec un commit signé de ton nom (double test
  récent : `refusing to allow a GitHub App to create or update workflow`).
- **Ton token ne change rien** : la permission est évaluée sur l'app, pas sur le secret utilisé. Garde‑le,
  ne le colle nulle part (surtout pas dans le chat).
- **Redémarrer le serveur** reste manuel : le dépôt des fichiers est automatique, le `restart` non.
