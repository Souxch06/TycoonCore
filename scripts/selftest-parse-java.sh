#!/usr/bin/env bash
# Auto-test du controleur Java : le fixture correct ne doit RIEN lever, le fixture fautif doit lever
# exactement ses deux fautes. Sans ce test, un controle qui cesse de voir quoi que ce soit (arrive deux
# fois dans ce depot : regex de compte trop large, puis portee de bloc trop etroite) passe inapercu et
# rend faux negatifs tous les runs suivants.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
PARSER="scripts/parse-java.mjs"
[ -f "$PARSER" ] || { echo "ERREUR: parseur absent"; exit 1; }
if ! node -e 'require("java-parser")' >/dev/null 2>&1 && [ ! -d node_modules/java-parser ]; then
  echo "SKIP : java-parser non installe (le CI fait `npm install --no-save java-parser@3` avant)"
  exit 0
fi

good_out="$(node "$PARSER" tests/java-selftest/good 2>&1)"
good_status=$?
bad_out="$(node "$PARSER" tests/java-selftest/bad 2>&1)"
bad_status=$?

fails=0
if [ "$good_status" -ne 0 ]; then
  echo "ECHEC: le fixture CORRECT est signale (faux positifs) :"; printf '%s\n' "$good_out" | sed 's/^/   /'; fails=1
fi
if [ "$bad_status" -eq 0 ]; then
  echo "ECHEC: le fixture FAUTIF passe (controle decoratif)"; fails=1
else
  for needle in "this.material" "déclarée deux fois"; do
    printf '%s' "$bad_out" | grep -q -- "$needle" \
      || { echo "ECHEC: la faute attendue <<$needle>> n'est plus signalee"; fails=1; }
  done
fi

if [ "$fails" -ne 0 ]; then
  printf '%s\n' "$bad_out" | sed 's/^/   /'
  exit 1
fi
echo "OK : auto-test du controleur Java (1 fixture propre, 2 fautes captees)"
