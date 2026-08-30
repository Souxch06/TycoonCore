#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Déclenche `deploy-serveur.yml` après la publication de la release `build-latest`.
#
# Pourquoi ce script existe (panne réelle, run 33207244834 du 2026-08-28) :
#   la release `build-latest` a bien été publiée avec ses TROIS jar (2 795 940 + 17 029 + 140 273
#   octets), l'étape « Publier la release » était verte… et `deploy-serveur.yml` n'a jamais démarré.
#   Aucun run, aucune erreur : GitHub refuse qu'un événement produit avec le `GITHUB_TOKEN` du dépôt
#   crée un nouveau run, pour couper les boucles récursives. `release:` n'est PAS dans ses exceptions.
#   Le dépôt reposait donc sur un déclencheur qui ne peut pas se produire en automatique.
#
#   `workflow_dispatch` EST l'une des deux exceptions documentées (avec `repository_dispatch`) :
#   c'est donc le build qui APPELLE le dépôt, explicitement, au lieu d'attendre un événement.
#   https://docs.github.com/en/actions/security-for-github-actions/security-guides/automatic-token-authentication
#
# Ce script ne déploie rien lui-même : il demande au workflow de dépôt de le faire. La séparation
# reste la même (le build n'ouvre aucune connexion vers le serveur de jeu), et les trois jar restent
# téléchargés depuis la release, donc vérifiés une seconde fois avant d'être posés.
#
# Deuxième panne réelle, corrigée ici (run 33304552672 du 2026-08-30) : l'appel partait, mais
# `deploy-serveur.yml` déclare `dry_run` en `type: boolean` et le script envoyait la CHAÎNE « 0 ».
# GitHub répondait alors `HTTP 422 : Provided value '0' for input 'dry_run' not in the list of allowed
# values` — la valeur autorisée d'un booléen est `true` ou `false`, pas 0/1. Le build rougissait sur
# sa dernière étape, la release était publiee, et le serveur ne bougeait toujours pas.
#   -> `gh api ... -F` convertit « false » en booléen JSON ; `-f` l'envoie tel quel, en chaîne. Les
#      deux formes sont tentées (booléen d'abord) pour absorber les deux lectures de l'API.
#
# Usage :
#   bash scripts/ci-trigger-deploy.sh            -> déclenche le dépôt réel de `build-latest`
#   DRY_RUN=1 bash scripts/ci-trigger-deploy.sh  -> déclenche la simulation (n'envoie rien)
#
# Sortie : 0 si le run de dépôt a bien été créé (et n'a pas déjà rougi), 1 sinon (avec une ligne
# `::error::` nommant la cause, lisible dans les annotations du run — un échec muet ici vaudrait un
# `plugins/` qui ne bouge plus, et un build vert au-dessus d'un dépôt rouge mentirait tout autant).
# ---------------------------------------------------------------------------
set -uo pipefail

TAG="${TAG:-build-latest}"
WORKFLOW="${WORKFLOW:-deploy-serveur.yml}"
DRY_RUN="${DRY_RUN:-0}"
REPO="${GITHUB_REPOSITORY:-$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)}"

say() { printf '%s\n' "$*"; }
die() { printf '::error::%s\n' "$*" >&2; exit 1; }

[ -n "${GH_TOKEN:-${GITHUB_TOKEN:-}}" ] || die "aucun jeton : ce script a besoin de GH_TOKEN (secrets.GITHUB_TOKEN) pour appeler le workflow de depot."
[ -n "$REPO" ] || die "depot introuvable (GITHUB_REPOSITORY absent et \`gh repo view\` muet) : impossible de viser un workflow."
case "$DRY_RUN" in
  0 | 1) : ;;
  *) die "DRY_RUN doit valoir 0 ou 1 (recu: $DRY_RUN)" ;;
esac

say "depot vise : $WORKFLOW (release « $TAG », simulation=$DRY_RUN) sur $REPO"

# La verification de la release precede l'appel : declencher un depot sur une release sans ses trois
# jar produirait un echec plus loin, moins lisible que celui-ci.
assets=$(gh release view "$TAG" --repo "$REPO" --json assets -q '.assets[].name' 2>/dev/null || true)
for jar in ValoriaTycoon ValoriaEconomy ValoriaTools; do
  printf '%s\n' "$assets" | grep -q "^$jar-" \
    || die "la release « $TAG » ne contient pas de jar $jar : le depot est refuse (le serveur ne doit jamais recevoir un plugin sans son economie ni son outil)."
done

# Un booleen d'abord (`-F` : « false » devient le JSON `false`), la chaine ensuite (`-f`).
DRY_RUN_JSON=false
[ "$DRY_RUN" = "1" ] && DRY_RUN_JSON=true

dispatch_once() {  # $1 = -F (booleen JSON) ou -f (chaine brute)
  gh api -X POST "repos/$REPO/actions/workflows/$WORKFLOW/dispatches" \
    -f ref=main -f "inputs[tag]=$TAG" "$1" "inputs[dry_run]=$DRY_RUN_JSON" 2>&1
}

out=""
if ! out=$(dispatch_once -F); then
  if second=$(dispatch_once -f); then
    out=""
  else
    printf '%s\n' "second essai (chaine) refuse : $second"
    out="$out | second essai (chaine) : $second"
  fi
fi

if [ -n "$out" ]; then
  case "$out" in
    *"403"* | *"Resource not accessible"*)
      die "dispatch refuse (403) : le workflow de build doit declarer \`permissions: actions: write\`. Sans elle, la release se publie et le serveur n'est jamais mis a jour." ;;
    *"404"* | *"Not Found"*)
      die "workflow « $WORKFLOW » introuvable sur la branche main : le coller depuis docs/CI-DEPLOY-A-COLLER.yml dans .github/workflows/$WORKFLOW." ;;
    *"422"* | *"not in the list of allowed values"*)
      die "dispatch refuse (422) : l'entree \`dry_run\` de $WORKFLOW est declaree en \`boolean\` et l'API n'accepte que true ou false (ni 0/1, ni vide). Booleen puis chaine essayes. Message : $(printf '%s' "$out" | tr '\n' ' ' | head -c 400)" ;;
    *)
      die "dispatch refuse : $(printf '%s' "$out" | tr '\n' ' ' | head -c 400)" ;;
  esac
fi

# L'appel rend la main des que la demande est acceptee : sans ce controle, un dispatch accepte mais
# jamais materialise en run passerait pour un succes.
run_url=""
cutoff=$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%SZ)
for _ in $(seq 1 20); do
  run_url=$(gh api "repos/$REPO/actions/workflows/$WORKFLOW/runs?per_page=5" \
    --jq '[.workflow_runs[] | select(.event == "workflow_dispatch")] | sort_by(.created_at) | last | select(.created_at > "'"$cutoff"'") | .html_url' \
    2>/dev/null | grep -v '^null$' || true)
  [ -n "$run_url" ] && break
  sleep 3
done

[ -n "$run_url" ] || die "le dispatch a ete accepte mais aucun run de depot n'est apparu sous 60 s : voir https://github.com/$REPO/actions/workflows/$WORKFLOW (bouton « Run workflow », dry_run=$DRY_RUN)."
say "run de depot cree : $run_url"

# On attend maintenant le VERDICT, pas seulement l'existence du run : un build vert au-dessus d'un
# depot rouge est un mensonge — la release est publiee, `plugins/` n'a pas bouge, et rien ne le dit.
# Fenetre bornee : au-dela, le run est laisse a sa vie et le build reste vert (le depot est en cours,
# il n'a pas echoue). En cas d'echec, la ligne `::error::` porte l'URL du run a annoter.
run_id=${run_url##*/}
for _ in $(seq 1 20); do
  state=$(gh api "repos/$REPO/actions/runs/$run_id" --jq '.status + " " + (.conclusion // "")' 2>/dev/null || true)
  case "$state" in
    "completed success") say "depot termine : succes ($run_url)"; exit 0 ;;
    completed*)          die "le depot a echoue ($state) : $run_url — lire SES annotations. La release est bonne, plugins/ n'a PAS ete ecrase." ;;
  esac
  sleep 15
done
say "depot encore en cours apres 5 min : $run_url (le build ne l'attend pas plus longtemps)"
