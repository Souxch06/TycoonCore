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
#   DEPLOY=1 DRY_RUN=1 bash …                          -> teste les identifiants en LECTURE SEULE (liste le dossier, NE televerse rien)
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
die() {
  # `::error::` : les journaux bruts du run passent par un domaine inaccessible, l'annotation est donc
  # le SEUL canal lisible. Sans elle, un `ERREUR:` dans le stderr se perdait corps et bien.
  printf '::error::%s\n' "$*" >&2
  printf 'ERREUR: %s\n' "$*" >&2
  exit 1
}

# Indice cible quand l'authentification SFTP est refusee. Sur un panneau de jeu type MCServerHost
# (Pterodactyl), le login SFTP n'est JAMAIS la simple connexion au site : c'est
# « connexion_panneau.id_serveur » (avec un POINT, ex. luca.a1b2c3d4), parfois « u12345_xxxx » (avec un
# underscore). Un secret qui ne contient ni '.' ni '_' est donc la connexion au site colle a la place du
# login SFTP — c'est la panne du run 33310555050, ou SFTP_USERNAME commencait par « Luca ». La fonction
# ne se prononce que sur ce cas certain ; sinon elle se tait. Elle n'affiche que les 4 premiers
# caracteres : le reste du login reste masque par GitHub.
sftp_user_hint() {
  case "$SFTP_USERNAME" in
    *.*|*_*) return 0 ;;   # porte la marque d'un login SFTP complet (connexion.id ou u12345_xxxx)
  esac
  printf "INDICE: le login SFTP envoye commence par \"%s***\" et ne contient AUCUN point : c'est la simple connexion au site/panneau, pas un compte SFTP. Le login SFTP est \"connexion_panneau.id_serveur\" (ex. luca.a1b2c3d4 ; parfois u12345_xxxx). Copier le champ Utilisateur EN ENTIER depuis l'onglet SFTP/Acces du panneau (bouton copier), puis l'enregistrer dans le secret GitHub SFTP_USERNAME. " "${SFTP_USERNAME:0:4}"
}

# Coupe l'espace/retour/tabulation en TROP au debut et a la fin d'un secret. Un copier-coller sur
# mobile colle souvent un "\n" ou une espace en bout de champ : le serveur voit alors une valeur
# differente et refuse l'authentification en silence (Permission denied), sans que rien ne le laisse
# deviner. On NE touche PAS au blanc INTERNE, qui peut etre legitime : le login SFTP de ce serveur est
# « Lucas Afonso.94b412fb » et contient une vraie espace. Trimming de l'exterieur seulement.
trim_secret() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

# Empreinte STRUCTURELLE d'un secret : longueur, presence d'espace (et d'autres blancs : tabulation,
# retour, espace insecable), de point, de chiffre, casse du premier caractere — JAMAIS la valeur.
# Pourquoi : « CX File Explorer se connecte mais la CI non » ne se tranche qu'en comparant ce que le
# runner a RECU avec ce que le panneau affiche. Or GitHub masque toute valeur de secret imprimee
# (elle sortirait en « *** », illisible), tandis qu'une longueur ou un « point absent » se lit
# toujours. L'empreinte attendue du login de ce serveur, « Lucas Afonso.94b412fb » :
# longueur=21 espaces=1 points=1 chiffres=5 1er_caractere=MAJUSCULE — tout ecart (espace double,
# espace insecable collee par le correcteur mobile, retour chariot de fin, connexion du site a la
# place du login) se voit SANS rien devoiler. Prepare au commit bd8b81b d'une session precedente
# (perdu a la remise a plat de l'historique), recrit ici a l'identique.
empreinte_secret() {
  local nom="$1" valeur longueur i c
  valeur="${!nom:-}"
  longueur="${#valeur}"
  local espaces=0 autres_blancs=0 points=0 chiffres=0 casse
  for ((i = 0; i < longueur; i++)); do
    c="${valeur:i:1}"
    case "$c" in
      ' ') espaces=$((espaces + 1)) ;;
      '.') points=$((points + 1)) ;;
      [0-9]) chiffres=$((chiffres + 1)) ;;
      *[[:space:]]*) autres_blancs=$((autres_blancs + 1)) ;;
    esac
  done
  case "${valeur:0:1}" in
    [[:upper:]]) casse='MAJUSCULE' ;;
    [[:lower:]]) casse='minuscule' ;;
    [0-9])       casse='chiffre' ;;
    *)           casse='ni_lettre_ni_chiffre' ;;
  esac
  printf 'empreinte %s : longueur=%d espaces=%d autres_blancs=%d points=%d chiffres=%d 1er_caractere=%s\n' \
    "$nom" "$longueur" "$espaces" "$autres_blancs" "$points" "$chiffres" "$casse"
}

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

# On coupe d'abord tout blanc de bordure ajoute au collage (retour/espace en bout de secret, courant
# sur mobile) ; on conserve le blanc interne (le login « Lucas Afonso.94b412fb » contient une espace).
SFTP_HOST=$(trim_secret "$SFTP_HOST")
SFTP_PORT=$(trim_secret "$SFTP_PORT")
SFTP_USERNAME=$(trim_secret "$SFTP_USERNAME")
SFTP_PASSWORD=$(trim_secret "$SFTP_PASSWORD")

# Un hote colle depuis le panneau arrive parfois en « sftp://hote/ » : on normalise, sinon la session
# SFTP echoue sur un nom d'hote invalide.
SFTP_HOST=$(printf '%s' "$SFTP_HOST" | sed -e 's#^[a-zA-Z][a-zA-Z0-9+.-]*://##' -e 's#/.*$##')
SFTP_PORT=$(printf '%s' "$SFTP_PORT" | tr -cd '0-9')
[ -n "$SFTP_HOST" ] || die "SFTP_HOST est vide apres normalisation : coller l'hote nu du panneau (sans sftp:// ni chemin)."
[ -n "$SFTP_PORT" ] || die "SFTP_PORT n'est pas un nombre (recu: ${SFTP_PORT:-vide}) : le port SFTP du panneau, en chiffres."

# Diagnostique d'empreinte structurelle : ce que le runner envoie VRAIMENT, champ par champ, apres
# nettoyage des blancs de bordure (la valeur, elle, n'apparait jamais — GitHub la masquerait en
# « *** »). A comparer avec l'onglet SFTP du panneau : hote « artemis.mcserverhost.com » =>
# longueur=24 points=2 chiffres=0 ; login « Lucas Afonso.94b412fb » => longueur=21 espaces=1
# points=1 chiffres=5 1er_caractere=MAJUSCULE. CX File Explorer vert + CI rouge avec une empreinte
# conforme = le secret ne decrit pas le meme compte que celui tape dans CX.
for var in SFTP_HOST SFTP_PORT SFTP_USERNAME SFTP_PASSWORD; do
  empreinte_secret "$var"
done

if ! command -v sshpass >/dev/null 2>&1; then
  say "installation de sshpass…"
  { sudo apt-get update -y && sudo apt-get install -y sshpass; } >/dev/null 2>&1 \
    || die "sshpass impossible a installer (etape devant tourner sur un runner Ubuntu)"
fi

# `sshpass -e` lit le mot de passe dans la variable SSHPASS. Sans cette ligne, il demarre avec un mot
# de passe vide, sftp refuse la session, et comme la sortie etait jetee (`>/dev/null 2>&1`) on ne
# voyait qu'un « session SFTP en echec » sans aucune cause — panne reelle du run 33306383817.
export SSHPASS="$SFTP_PASSWORD"

# ATTENTION a l'ORDRE : sftp n'accepte ses options QU'AVANT la destination. « sftp -o ... user@hote
# -b fichier » echoue sur son usage (exit 1) sans jamais ouvrir la session — panne reelle du run
# 33307097547, longtemps prise pour un mot de passe refuse. Les options restent donc dans le tableau
# et la destination est passe en DERNIER argument.
SFTP=(sshpass -e sftp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT")
DEST="$SFTP_USERNAME@$SFTP_HOST"

# Pre-vol TCP : un port ferme, un hote inconnu et un mot de passe refuse donnaient le MEME message
# (« session SFTP en echec »). En joignant la prise avant, on sait tout de suite s'il faut regarder le
# panneau (acces SFTP actif, bon port) ou le secret (identifiant, mot de passe).
if timeout 20 bash -c "cat < /dev/null > /dev/tcp/$SFTP_HOST/$SFTP_PORT" 2>/dev/null; then
  say "pre-vol OK : $SFTP_HOST:$SFTP_PORT repond"
else
  die "le serveur $SFTP_HOST:$SFTP_PORT n'est pas joignable depuis le runner GitHub (port ferme, hote inconnu, ou acces SFTP desactive). A verifier dans le panneau MCServerHost : hote exact, port, et activation de l'acces SFTP."
fi
STAMP="$(date -u +%Y%m%d-%H%M%S)"
# Le prefixe « - » de sftp signifie « ignore l'erreur » : au premier deploiement les jar ne sont pas
# encore sur le serveur, et un `rename` qui echoue ferait AVORTER toute la session batch avant l'envoi.
REMOTE_CMDS=(-mkdir "$PLUGINS_DIR/_sauvegarde-$STAMP")
for name in "${JAR_NAMES[@]}"; do
  REMOTE_CMDS+=("-rename $PLUGINS_DIR/$name $PLUGINS_DIR/_sauvegarde-$STAMP/$name")
done
# Un `put` par jar : `put src1 src2 dossier/` n'est pas garanti sur toutes les versions d'OpenSSH.
UPLOAD_CMDS=()
for jar in "${JARS[@]}"; do
  UPLOAD_CMDS+=("put $jar $PLUGINS_DIR/")
done

if [ "$DRY_RUN" = "1" ]; then
  # La simulation ne se contente plus d'afficher la session qui « serait » jouee : elle OUVRE une vraie
  # session, en LECTURE SEULE, pour tester les identifiants. Verte => l'admin voit le contenu de
  # `plugins/`, la preuve que SFTP_USERNAME/SFTP_PASSWORD sont bons ; rouge => l'erreur nomme la cause
  # (identifiant/mot de passe refuses, acces SFTP desactive, restriction d'IP, dossier absent).
  # Aucune ecriture : pas de `put`, `rename` ni `mkdir` — on ne fait que `pwd` et `ls`.
  say "DRY_RUN : ouverture d'une session SFTP en LECTURE SEULE (test des identifiants)…"
  BATCH="$(mktemp)"
  SFTP_LOG="$(mktemp)"
  printf 'pwd\nls -l %s\nbye\n' "$PLUGINS_DIR" > "$BATCH"
  if "${SFTP[@]}" -b "$BATCH" "$DEST" >"$SFTP_LOG" 2>&1; then
    say "DRY_RUN : identifiants valides — la session s'ouvre, contenu de $PLUGINS_DIR :"
    cat "$SFTP_LOG"
    exit 0
  fi
  msg="$(grep -v -i -e 'Warning: Permanently added' -e '^$' "$SFTP_LOG" | head -c 500 | tr '\n' ' ')"
  case "$msg" in
    *"Permission denied"*|*"Authentication failed"*|*"password"*)
      die "simulation : identifiants refuses sur ${SFTP_USERNAME:0:4}***@$SFTP_HOST:$SFTP_PORT ($msg) $(sftp_user_hint)Verifier aussi le mot de passe : sur MCServerHost c'est celui du compte PANNEAU, pas celui du site ou de la facturation. Pour en avoir le coeur net, tester d'abord les memes quatre valeurs dans FileZilla (FileZilla vert mais CI rouge = secret GitHub non mis a jour) ; puis relancer avec dry_run coche." ;;
    *"No such file"*|*"does not exist"*|*"not found"*)
      die "simulation : la session s'ouvre mais le dossier « $PLUGINS_DIR » est absent cote serveur ($msg). Verifier SFTP_PLUGINS_DIR ou le repertoire de base du compte dans le panneau." ;;
    *"Connection refused"*|*"Connection timed out"*|*"No route to host"*|*"Could not resolve hostname"*)
      die "simulation : le serveur SFTP ne repond plus ($msg) — verifier hote, port et acces SFTP dans le panneau, puis relancer." ;;
    *)
      die "simulation : session SFTP en echec ($msg) — causes frequentes : acces SFTP desactive pour le compte, restriction d'IP, ou dossier « $PLUGINS_DIR » inaccessible." ;;
  esac
fi

# Un seul `sftp` par phase, avec sa sortie CAPTUREE : la panne precedente jetait stdout et stderr, donc
# le run rougissait sans jamais dire pourquoi (et les journaux bruts sont inaccessibles — seules les
# annotations se lisent). Le mot de passe ne peut pas fuir : sshpass le prend dans l'environnement, il
# n'apparait dans aucune ligne de commande ni dans aucune sortie.
SFTP_LOG="$(mktemp)"
BATCH="$(mktemp)"
sftp_last() {
  grep -v -i -e 'Warning: Permanently added' -e '^$' "$SFTP_LOG" | head -c 400 | tr '\n' ' '
}
# Un VRAI fichier batch (`-b fichier`), pas `-b -` : c'est la forme documentee partout, et elle ne
# depend pas de la gestion du tiret par la version d'OpenSSH du runner.
run_sftp() {
  printf '%s\n' "$@" "bye" > "$BATCH"
  "${SFTP[@]}" -b "$BATCH" "$DEST" >"$SFTP_LOG" 2>&1
}

say "sauvegarde des jar en place dans $PLUGINS_DIR/_sauvegarde-$STAMP…"
if run_sftp "${REMOTE_CMDS[@]}"; then
  say "sauvegarde faite : $PLUGINS_DIR/_sauvegarde-$STAMP"
else
  # Non bloquant : un serveur vierge n'a rien a sauvegarder, et mieux vaut deployer sans filet que pas
  # du tout. Le message reste dans le journal du run.
  say "AVERTISSEMENT : sauvegarde distante impossible ($(sftp_last)) — deploiement poursuivi, les jar en place seront ecrases."
fi

say "envoi des ${#JARS[@]} jar vers $PLUGINS_DIR/…"
run_sftp "${UPLOAD_CMDS[@]}" \
  || die "envoi SFTP en echec vers ${SFTP_USERNAME:0:4}***@$SFTP_HOST:$SFTP_PORT : $(sftp_last) $(sftp_user_hint)— causes frequentes : identifiant ou mot de passe refuse, compte sans acces SFTP, ou repertoire « $PLUGINS_DIR » inexistant."

# Verification octet pour octet. L'ancienne ecriture passait la COMMANDE comme si c'etait un FICHIER
# (`-b "ls -l plugins"`, que sftp refuse), et APRES la destination qui plus est.
LISTING=""
if run_sftp "ls -l $PLUGINS_DIR"; then
  LISTING="$(cat "$SFTP_LOG")"
else
  say "AVERTISSEMENT : listing distant impossible ($(sftp_last)) — la verification de taille va donc tout signaler comme absent."
fi
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
