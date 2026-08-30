# À FAIRE — checklist à jour (ValoriaTycoon / MCServerHost)

> Checklist d'exploitation, à relire en premier à chaque session. Vérifiée sur GitHub le 2026-08-30.
> L'état décrit ici est **réel**, pas un projet : le pipeline est en place, seul reste un blocage
> **identifiants SFTP**.

---

## État réel (au 30/08/2026)

- `main` = `addc0db` (PR #10, #11, #12 mergées).
- `.github/workflows/build.yml` : **20 étapes**, `permissions: actions: write`, étape « Déclencher le
  dépôt sur le serveur » présente — conforme à `docs/CI-A-COLLER.yml`. Il compile et contrôle les
  TROIS jar, il ne déploie jamais.
- `.github/workflows/deploy-serveur.yml` : **5 étapes**, `environment: production`. Il télécharge les
  trois jar de la release `build-latest`, vérifie leur contenu, puis les dépose sur le serveur.
- `.github/workflows/deploy.yml` : **neutralisé** (plus aucun `push:`, uniquement `workflow_dispatch`).
- Les 4 secrets SFTP existent et sont non vides : `SFTP_HOST`, `SFTP_PORT`, `SFTP_USERNAME`,
  `SFTP_PASSWORD`.
- Release `build-latest` : **3 jar + SHA256SUMS.txt**, réécrite à chaque build vert de `main`.
- `deploy-serveur.yml` a enfin des runs (il n'en avait **AUCUN** avant le 30/08).

## CE QUI RESTE : les identifiants SFTP, et rien d'autre

Dernier run de dépôt **33307920720** : `Permission denied (password,publickey)`. L'hôte et le port
sont **bons** (un pré-vol TCP les teste avant — sinon le run meurt plus tôt avec un message orienté
panneau). Donc l'une des causes suivantes :

- `SFTP_USERNAME` mauvais — il est souvent `u12345_xxxx`, **PAS** l'identifiant du site web ;
- `SFTP_PASSWORD` mauvais ou expiré — à **régénérer dans le panneau** (le mot de passe SFTP est
  distinct de celui du panneau) ;
- accès SFTP **désactivé** pour le compte ;
- **restriction d'IP** : il faut autoriser les IP des runners GitHub (ou la désactiver).

Diagnostic en place depuis le commit « Empreinte structurelle des secrets » : chaque tentative de
dépôt affiche quatre lignes « `empreinte …` » (longueur, espace/point/chiffre, casse du 1er
caractère — **jamais la valeur**). Référence calculable depuis CX File Explorer : hôte `24
caractères, point=oui, chiffre=non, 1er minuscule` ; port `4 caractères, chiffre=oui, 1er chiffre` ;
login `21 caractères, espace=oui, point=oui, chiffre=oui, 1er majuscule`. Une ligne qui dévie nomme
le secret fautif (espace insécable collée sur mobile = `espace=non`, suffixe `.id` perdu =
`point=non`, ancien mot de passe = autre longueur).

Le dernier build de `main` (**33307721428**) est **ROUGE à l'étape 20** : c'est **voulu**. Le build
attend maintenant le verdict du dépôt. Un build vert au-dessus d'un dépôt rouge serait un mensonge.
Ne **pas** « réparer » ça en assouplissant le contrôle.

### Les 7 étapes pour l'admin (panneau MCServerHost + GitHub)

1. Ouvrir le **panneau MCServerHost** du serveur → rubrique **SFTP / Accès par fichier** (ou l'onglet
   FTP) : noter l'**hôte** exact (sans `sftp://` ni chemin) et le **port** SFTP.
2. Prendre le **nom d'utilisateur SFTP** du panneau (souvent `u12345_xxxx`) — **pas** l'identifiant du
   site. C'est cette valeur qu'il faut mettre dans `SFTP_USERNAME`.
3. **Régénérer** le mot de passe SFTP (bouton « générer / réinitialiser »), ou créer un mot de passe
   explicite. C'est cette valeur qu'il faut mettre dans `SFTP_PASSWORD`.
4. Vérifier que l'**accès SFTP est activé** pour le compte (l'onglet peut être désactivé par défaut).
5. Vérifier la **restriction d'IP** : si elle est active, autoriser les IP de sortie des runners
   GitHub Actions (ou la désactiver) — sinon la connexion depuis la CI est refusée.
6. Mettre à jour les 4 secrets sur GitHub : **Settings → Secrets and variables → Actions** →
   `SFTP_HOST`, `SFTP_PORT`, `SFTP_USERNAME`, `SFTP_PASSWORD` (régénérée à l'étape 3 et l'utilisateur
   de l'étape 2).
7. **Tester sans envoyer** : **Actions → « Release et déploiement automatique (trois jar) » → Run
   workflow** → branche `main` → **dry_run coché** → Run. Verte = identifiants bons et dossier
   `plugins/` visible ; rouge = l'annotation nomme la cause.

## Quatre bugs déjà corrigés — ne pas les refaire

- **Run 33304552672** — `HTTP 422 '0' not in the list of allowed values` : `dry_run` est un
  `boolean`, or `gh workflow run -f` envoie la chaîne `"0"`. Correctif : `gh api -F` (booléen JSON).
- Juste derrière — le `jq` de détection du run était invalide (guillemets échappés) et aurait tué le
  script à l'étape suivante. Correctif : programme `jq` entre guillemets simples côté bash.
- **Run 33306383817** — étape 6 rouge sans cause : `sshpass -e` lit `$SSHPASS`, que rien n'exportait
  (le workflow publie `SFTP_PASSWORD`), et la sortie de `sftp` était jetée par `>/dev/null 2>&1`.
- **Run 33307097547** — `usage: sftp [-46AaCfNpqrv]` : sftp n'accepte ses options **qu'avant** la
  destination. Correctif : options dans le tableau, destination en dernier argument, et un vrai
  fichier batch (`-b fichier`) au lieu de `-b -`.

Corrigés aussi : `sftp -b "commande"` (c'est un fichier, pas une commande), `put` multi-sources,
`rename` sans préfixe `-` qui faisait avorter le batch, et `die()` qui émet désormais `::error::`.

Leçon : chaque correctif a révélé la panne suivante. **Ne jamais dire « fini » sans avoir vu le run
de dépôt VERT.**

## Contraintes du dépôt (elles ont coûté cher)

- **Pas de JDK ni de Maven en local** : seule la CI prouve que ça compile.
- Contrôles à passer : `python3 scripts/verify-ci-copies.py` (42) et
  `python3 scripts/verify-tools-config.py` (147).
- Les **journaux bruts** du run sont **inaccessibles** (domaine bloqué). Les **annotations** sont le
  seul canal lisible : `gh api repos/Souxch06/ValoriaTycoon/check-runs/<job_id>/annotations`.
- Les captures d'écran n'arrivent jamais : **demander du texte collé**.
- **Ne JAMAIS emporter `.github` dans un commit** : les workflows se collent à la main depuis `docs/`.
- **Squash-merge** : la branche finit en conflit au tour suivant. Recaler avant de committer avec
  `git fetch origin main && git reset --soft origin/main`.
- L'admin ne maîtrise pas le terminal : **décider à sa place**, ne lui demander que des clics dans le
  panneau MCServerHost ou l'interface GitHub.

## Comment l'admin valide sans l'agent

**Actions → « Release et déploiement automatique (trois jar) » → Run workflow** → branche `main` →
**dry_run coché** → Run. Depuis le commit « La simulation verifie les identifiants », la simulation
ouvre une vraie session en **LECTURE SEULE** : verte elle liste `plugins/`, rouge elle nomme la cause.
**~1 minute, aucun commit nécessaire.**

## Points signalés à l'admin (à faire en parallèle)

- L'environnement **production** s'est créé **SANS `Required reviewers`** : sans filet, chaque push
  sur `main` dépose les jar **sans rien demander à personne**. Le cocher dans
  **Settings → Environments → production** pour remettre une validation humaine avant chaque envoi.
