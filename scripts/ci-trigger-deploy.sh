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
# Usage :
#   bash scripts/ci-trigger-deploy.sh            -> déclenche le dépôt réel de `build-latest`
#   DRY_RUN=1 bash scripts/ci-trigger-deploy.sh  -> déclenche la simulation (n'envoie rien)
#
# Sortie : 0 si un run de dépôt a bien été créé, 1 sinon (avec une ligne `::error::` nommant la cause,
# lisible dans les annotations du run — un échec muet ici vaudrait un `plugins/` qui ne bouge plus).
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

if ! out=$(gh workflow run "$WORKFLOW" --repo "$REPO" --ref main \
             -f tag="$TAG" -f dry_run="$DRY_RUN" 2>&1); then
  case "$out" in
    *"403"* | *"Resource not accessible"*)
      die "dispatch refuse (403) : le workflow de build doit declarer \`permissions: actions: write\`. Sans elle, la release se publie et le serveur n'est jamais mis a jour." ;;
    *"404"* | *"Not Found"*)
      die "workflow « $WORKFLOW » introuvable sur la branche main : le coller depuis docs/CI-DEPLOY-A-COLLER.yml dans .github/workflows/$WORKFLOW." ;;
    *)
      die "dispatch refuse : $(printf '%s' "$out" | head -c 300)" ;;
  esac
fi

# `gh workflow run` rend la main des que la demande est acceptee : sans ce controle, un dispatch
# accepte mais jamais materialise en run passerait pour un succes.
run_url=""
cutoff=$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%SZ)
for _ in $(seq 1 20); do
  run_url=$(gh api "repos/$REPO/actions/workflows/$WORKFLOW/runs?per_page=5" \
    --jq "[.workflow_runs[] | select(.event==\"workflow_dispatch\")] | sort_by(.created_at) | last | select(.created_at > \"$cutoff\") | .html_url" \
    2>/dev/null | grep -v '^null$' || true)
  [ -n "$run_url" ] && break
  sleep 3
done

[ -n "$run_url" ] || die "le dispatch a ete accepte mais aucun run de depot n'est apparu sous 60 s : voir https://github.com/$REPO/actions/workflows/$WORKFLOW (bouton « Run workflow », dry_run=$DRY_RUN)."
say "run de depot cree : $run_url"
