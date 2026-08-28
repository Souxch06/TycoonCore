#!/usr/bin/env bash
# Execote une etape de CI et, en cas d'echec, publie la sortie utile en resume du job.
#
# Pourquoi : les etapes « verifier le jar / verifier les produits » ne font que `grep`/`test` ;
# leur echec laisse dans le journal une ligne `exit code 1` parfaitement muette. Ici, les dernieres
# lignes sont ecrites dans $GITHUB_STEP_SUMMARY (resume visible en haut du job, et lu par l'API des
# annotations) AVANT de rejouer le code de sortie — le controle reste rouge, mais il dit pourquoi.
set -uo pipefail

LABEL="$1"
shift
OUT="$(mktemp)"
STATUS=0
"$@" 2>&1 | tee "$OUT"
STATUS=${PIPESTATUS[0]}

if [ "$STATUS" -ne 0 ]; then
  {
    printf '### Etape en echec : %s\n\n```text\n' "$LABEL"
    tail -n 60 "$OUT"
    printf '\n```\n\n<sub>sortie complete : etape `%s` du job, code %s</sub>\n' "$LABEL" "$STATUS"
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
  echo "sortie de l'etape publiee dans le resume du job (${STATUS})"
fi

rm -f "$OUT"
exit "$STATUS"
