#!/usr/bin/env bash
# Publie les deux jar verifies sur une Release GitHub permanente (`build-latest`).
#
# Pourquoi : un artefact d'execution Actions est perime au bout de 90 jours, exige de naviguer dans
# le journal du job, et surtout n'est PAS telechargeable par un agent (le telechargement est servi par
# un stockage externe, bloque ici). Une Release, elle, vit a une URL stable et telechargeable par tout
# le monde — c'est le seul canal par lequel « le jar construit par la CI » devient utilisable sans
# open-blob-et-coller-quoi-que-ce-soit.
#
# Le tag est REECRIT a chaque build vert : la release porte toujours le dernier paquet qui a passe
# les 14 controles, et la signature sha256 du zip est ecrite a cote pour verification.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

TAG="${RELEASE_TAG:-build-latest}"
MAIN_JAR="target/ValoriaTycoon-v${PROJECT_VERSION:-1.6.3}.jar"
ECONOMY_JAR="target/ValoriaEconomy-v${PROJECT_VERSION:-1.6.3}.jar"

for jar in "$MAIN_JAR" "$ECONOMY_JAR"; do
  if [ ! -f "$jar" ]; then
    echo "ERREUR: $jar absent — rien a publier (le build doit produire les DEUX jars)" >&2
    exit 1
  fi
done

SUMMARY="SHA256SUMS.txt"
sha256sum "$MAIN_JAR" "$ECONOMY_JAR" > "$SUMMARY"

if [ -z "${GITHUB_TOKEN:-${GH_TOKEN:-}}" ]; then
  echo "(hors CI : publication simulee)"
  ls -l "$MAIN_JAR" "$ECONOMY_JAR"
  cat "$SUMMARY"
  exit 0
fi

# Le dépôt peut interdire la création de release par l'API : on tente, et on explique comment finir a
# la main sans faire echouer le build pour autant (les jar sont deja verifies a cette etape).
if ! gh release view "$TAG" >/dev/null 2>&1; then
  gh release create "$TAG" \
    --target "$(git rev-parse HEAD)" \
    --title "Dernier build vérifié (ValoriaTycoon + ValoriaEconomy)" \
    --notes "Construit par le workflow de validation, apres 14 controles (compilation, surface publique, contenu des deux jars, absence de toute API tierce). Telecharger le zip, dezipper, deposer les DEUX jar dans plugins/." \
    --prerelease || echo "AVERTISSEMENT: gh release create a echoue (droits de release ?) — les artefacts de l'execution restent disponibles." >&2
fi

gh release upload "$TAG" "$MAIN_JAR" "$ECONOMY_JAR" "$SUMMARY" --clobber \
  || echo "AVERTISSEMENT: gh release upload a echoue (droits de release ?) — voir l'etape « Publier les JAR construits »." >&2

echo "release $TAG -> $(gh release view "$TAG" --json url -q .url 2>/dev/null || echo "n/d")"
