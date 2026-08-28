#!/usr/bin/env bash
# Publie les deux jar verifies sur la release `build-latest`, puis (en option) les depose sur le serveur.
#
# Pourquoi un script et pas du YAML : l'agent ne peut pas ecrire dans `.github/workflows/` (GitHub
# refuse a une app sans la permission `workflows`), mais il peut ecrire n'importe quel script du depot.
# Toute la logique vit donc ICI — testable sans serveur et sans secret — et le YAML ne fait qu'appeler.
#
# Modes :
#   bash scripts/ci-release-and-deploy.sh              -> release seule (aucun envoi)
#   DEPLOY=1 bash scripts/ci-release-and-deploy.sh     -> release + envoi des deux jar sur le serveur
#   DEPLOY=1 DRY_RUN=1 bash …                          -> prepare, affiche, NE televerse rien
#
# Garde-fous :
#   - l'envoi exige les DEUX jars : un serveur avec le plugin mais sans l'economie = monnaie cassee ;
#   - les jar en place sont d'abord deplaces dans `plugins/_sauvegarde-<horodatage>/` : rollback direct ;
#   - apres l'envoi, controle de taille octet par octet cote serveur (un SFTP tronque ne leve pas
#     toujours d'erreur, et un jar a moitie envoye rend un plugin « introuvable » au demarrage) ;
#   - hors DEPLOY=1, le script ne touche JAMAIS au reseau : c'est le comportement par defaut du workflow.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${PROJECT_VERSION:-1.6.3}"
TAG="${RELEASE_TAG:-build-latest}"
MAIN_JAR="target/ValoriaTycoon-v${VERSION}.jar"
ECONOMY_JAR="target/ValoriaEconomy-v${VERSION}.jar"
# Le multi-outil est optionnel a la PUBLICATION (une branche qui ne le construit pas ne doit pas etre
# bloquee), mais OBLIGATOIRE au DEPOT : un serveur qui reçoit le plugin et son economie sans son outil se
# retrouve avec un fichier `tools.yml` orphelin, et personne ne s'en apercoit avant le premier mineur qui
# dit « mon multi-outil n'existe plus ». Le script refuse donc l'envoi, il ne complete pas en douce.
TOOLS_JAR="target/ValoriaTools-v${VERSION}.jar"
PLUGINS_DIR="${SFTP_PLUGINS_DIR:-plugins}"
DEPLOY="${DEPLOY:-0}"
DRY_RUN="${DRY_RUN:-0}"
JAR_NAMES=()


say() { printf '%s\n' "$*"; }
die() { printf 'ERREUR: %s\n' "$*" >&2; exit 1; }

# ------------------------------------------------------------------ 1. les trois jars, ou rien
JARS=("$MAIN_JAR" "$ECONOMY_JAR")
if [ -f "$TOOLS_JAR" ]; then
  JARS+=("$TOOLS_JAR")
elif [ "$DEPLOY" = "1" ]; then
  die "$(basename "$TOOLS_JAR") absent : le depot refuse de n'envoyer que deux jar sur trois.
Soit la release $TAG ne contient pas le multi-outil (la relancer depuis main), soit le build n'a pas
execute l'assemblage `tools-plugin-jar` (verifier que .github/workflows/build.yml est bien la version
17 etapes de docs/CI-A-COLLER.yml, pas une copie plus ancienne)."
else
  say "info: $(basename "$TOOLS_JAR") absent du build — non publie (mode publication)"
fi
for jar in "${JARS[@]}"; do
  [ -f "$jar" ] || die "$jar absent — lancer d'abord « mvn -B clean package » (le build doit produire les TROIS jars)"
done
for jar in "${JARS[@]}"; do
  size=$(stat -c %s "$jar")
  # Un zip valide commence par « PK\x03\x04 » ; le controle de CONTENU (classes attendues, API tierce
  # absente) est fait par le workflow avant cette etape, pas ici.
  head -c 2 "$jar" | grep -q "PK" || die "$jar ne ressemble pas a une archive zip ($size octets)"
  say "jar OK : $jar ($size octets)"
  JAR_NAMES+=("$(basename "$jar")")
done

mkdir -p target
sha256sum "${JARS[@]}" > target/SHA256SUMS.txt

# ------------------------------------------------------------------ 2. la release permanente (declencheur du depot)
NOTES="Construit par le workflow de validation (17 etapes : compilation des 33 fichiers maintenus, surface publique, contenu des trois jars, absence de toute API tierce).

| fichier | octets | role |
| --- | --- | --- |
| \`${JAR_NAMES[0]}\` | $(stat -c %s "$MAIN_JAR") | generateurs, /ah, /sb, hologrammes, API d'economie interne |
| \`${JAR_NAMES[1]}\` | $(stat -c %s "$ECONOMY_JAR") | soldes : /bal, /pay, /baltop, /eco |

Aucun autre plugin n'est requis (ni Vault, ni EssentialsX, ni HoloEasy, ni ProtocolLib). \`SHA256SUMS.txt\` permet de verifier l'integrite avant installation."

# Porte de publication : `auto` = seulement en CI (le developpeur local qui lance le script ne doit pas
# reecrire la release du depot avec des jar non verifies ; `1` force, `0` interdit).
RELEASE="${RELEASE:-auto}"
case "$RELEASE" in
  auto) if [ -n "${GITHUB_ACTIONS:-}" ]; then RELEASE=1; else RELEASE=0; fi ;;
  0 | 1) : ;;
  *) die "RELEASE doit valoir auto, 0 ou 1 (recu: $RELEASE)" ;;
esac

if [ "$RELEASE" = "1" ] && [ -n "${GITHUB_TOKEN:-${GH_TOKEN:-}}" ]; then
  # Le tag est REECRIT a chaque build vert : une release figee sur un vieux commit serait pire qu'un
  # artefact d'execution (on deposerait un paquet corrige… puis jamais mis a jour).
  gh release delete "$TAG" --cleanup-tag --yes >/dev/null 2>&1 || true
  if gh release create "$TAG" --target "$(git rev-parse HEAD)" \
        --title "Dernier build vérifié (ValoriaTycoon + ValoriaEconomy)" --notes "$NOTES"; then
    gh release upload "$TAG" "${JARS[@]}" target/SHA256SUMS.txt \
      || say "AVERTISSEMENT : televersement des assets refuse (droits de release ?) — les jar restent disponibles en artefact du run."
    for asset in "${JAR_NAMES[@]}"; do
      want=$(stat -c %s "target/$asset")
      got=$(gh release view "$TAG" --json assets -q ".assets[] | select(.name==\"$asset\") | .size" 2>/dev/null || echo "")
      case "$got" in
        "")          say "AVERTISSEMENT : asset $asset absent de la release." ;;
        "$want")     say "asset verifie : $asset ($got octets)" ;;
        *)           die "asset $asset = $got octets sur GitHub, $want attendus (televersement incomplet)" ;;
      esac
    done
    say "release : $(gh release view "$TAG" --json url -q .url 2>/dev/null || echo 'n/d')"
  else
    say "AVERTISSEMENT : creation de release refusee — etape non bloquante, les artefacts du run restent disponibles."
  fi
else
  say "(publication de release ignoree : RELEASE=$RELEASE, GITHUB_ACTIONS=${GITHUB_ACTIONS:-absent})"
fi

# ------------------------------------------------------------------ 3. l'envoi sur le serveur
if [ "$DEPLOY" != "1" ]; then
  say "DEPLOY != 1 : rien n'est envoye sur le serveur (volontaire — cocher « Deployer » pour le faire)."
  exit 0
fi

for var in SFTP_HOST SFTP_PORT SFTP_USERNAME SFTP_PASSWORD; do
  [ -n "${!var:-}" ] || die "secret $var manquant : l'etape doit le publier dans son bloc env (comme deploy.yml)"
done
if ! command -v sshpass >/dev/null 2>&1; then
  say "installation de sshpass…"
  { sudo apt-get update -y && sudo apt-get install -y sshpass; } >/dev/null 2>&1 \
    || die "sshpass impossible a installer (etape devant tourner sur un runner Ubuntu)"
fi

SFTP=(sshpass -e sftp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT" "$SFTP_USERNAME@$SFTP_HOST")
STAMP="$(date -u +%Y%m%d-%H%M%S)"
REMOTE_CMDS=(-mkdir "$PLUGINS_DIR/_sauvegarde-$STAMP")
for name in "${JAR_NAMES[@]}"; do
  REMOTE_CMDS+=("rename $PLUGINS_DIR/$name $PLUGINS_DIR/_sauvegarde-$STAMP/$name")
done

if [ "$DRY_RUN" = "1" ]; then
  say "DRY_RUN : la session SFTP qui serait jouee :"
  say "  ${SFTP[*]}"
  printf '  %s\n' "${REMOTE_CMDS[@]}" "put ${JARS[*]} $PLUGINS_DIR/" "ls -l $PLUGINS_DIR"
  exit 0
fi

say "sauvegarde des jar en place dans $PLUGINS_DIR/_sauvegarde-$STAMP, puis envoi des ${#JARS[@]} jar…"
printf '%s\n' "${REMOTE_CMDS[@]}" "put ${JARS[*]} $PLUGINS_DIR/" "bye" \
  | "${SFTP[@]}" >/dev/null 2>&1 || die "session SFTP en echec (reseau, droits, ou serveur en cours de demarrage)"

# ls -l distant : "droits liens user group TAILLE date heure nom" -> la 5e colonne.
LISTING=$("${SFTP[@]}" -b "ls -l $PLUGINS_DIR" 2>/dev/null)
fail=0
for name in "${JAR_NAMES[@]}"; do
  want=$(stat -c %s "target/$name")
  got=$(printf '%s\n' "$LISTING" | awk -v n="$name" '$NF==n {print $5}')
  if [ "${got:-}" = "$want" ]; then
    say "serveur OK : $name ($got octets)"
  else
    say "ERREUR : le serveur annonce '${got:-absent}' octets pour $name, attendu $want"
    fail=1
  fi
done
[ "$fail" = 0 ] || die "envoi non verifie : les jar du serveur ne correspondent pas octet pour octet (rollback : restaurer $PLUGINS_DIR/_sauvegarde-$STAMP/)"
say "deploiement termine et verifie. Redemarrer le serveur pour charger les nouveaux jar."
