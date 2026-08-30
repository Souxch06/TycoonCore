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
    *.*|*_|*) return 0 ;;   # porte la marque d'un login SFTP complet (connexion.id ou u12345_xxxx)
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

# Empreinte STRUCTURELLE d'un secret : longueur, presence d'une espace / d'un point / d'un chiffre,
# casse du premier caractere — JAMAIS la valeur, pas meme un extrait. GitHub masque de toute facon
# les secrets dans les journaux, mais l'empreinte ne doit pas dependre de ce filet : elle ne calcule
# que des formes, donc reste lisible meme pour un secret court ou mal masque.
#
# Pourquoi c'est l'outil qu'il faut ici : CX File Explorer se connecte AVEC LES MÊMES QUATRE VALEURS
# (hote artemis.mcserverhost.com, port 2022, login « Lucas Afonso.94b412fb ») alors que la CI recoit
# « Permission denied (password,publickey) ». Les empreintes de reference, calculables a la main :
#   SFTP_HOST     24 caracteres, espace=non, point=oui, chiffre=non, 1er caractere minuscule
#   SFTP_PORT      4 caracteres, espace=non, point=non, chiffre=oui, 1er caractere chiffre
#   SFTP_USERNAME 21 caracteres, espace=oui, point=oui, chiffre=oui, 1er caractere majuscule
# Si les lignes « empreinte … » du run ne montrent pas ces formes, le secret GitHub n'est pas la
# valeur du panneau — ex. une espace INSECABLE collee sur mobile parait identique a l'oeil mais
# apparait ici comme espace=non ; un login sans suffixe « .id » perd son point ; l'ancien mot de
# passe n'a souvent ni la meme longueur ni les memes caracteres. Le secret fautif se nomme SANS
# qu'aucune valeur ne fuie dans le journal.
#
# Double canal volontaire : say() pour le journal brut, PLUS une annotation notice. Les journaux
# bruts d'un run passent par un domaine inaccessible depuis l'agent (voir die()) : l'annotation est
# le seul canal vraiment lisible a distance, et cette empreinte est faite pour etre LUE, pas juste
# ecrite.
empreinte_secret() {
  local nom="$1" val="$2" ligne premier
  local espace=non point=non chiffre=non casse="absent (valeur vide)"
  case "$val" in
    *" "*) espace=oui ;;
  esac
  case "$val" in
    *"."*) point=oui ;;
  esac
  case "$val" in
    *[0-9]*) chiffre=oui ;;
  esac
  if [ -n "$val" ]; then
    premier="${val:0:1}"
    if   [[ "$premier" =~ [[:upper:]] ]]; then casse="une majuscule"
    elif [[ "$premier" =~ [[:lower:]] ]]; then casse="une minuscule"
    elif [[ "$premier" =~ [[:digit:]] ]]; then casse="un chiffre"
    else                                    casse="un autre caractere"
    fi
  fi
  # %-13s aligne les quatre lignes (SFTP_HOST/SFTP_PORT = 9 lettres, SFTP_USERNAME/SFTP_PASSWORD = 13).
  ligne="$(printf 'empreinte %-13s : %s caracteres, espace=%s, point=%s, chiffre=%s, 1er caractere %s' \
            "$nom" "${#val}" "$espace" "$point" "$chiffre" "$casse")"
  say "$ligne"
  printf '::notice::%s\n' "$ligne"
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

# Empreinte APRES trimming/normalisation : elle decrit exactement les valeurs qui partent vers le
# serveur. Comparee aux empreintes de reference (celles de CX File Explorer, voir la fonction), elle
# nomme le secret de mauvaise forme AVANT meme que l'authentification echoue — et reste dans le
# journal si elle echoue quand meme.
empreinte_secret SFTP_HOST     "$SFTP_HOST"
empreinte_secret SFTP_PORT     "$SFTP_PORT"
empreinte_secret SFTP_USERNAME "$SFTP_USERNAME"
empreinte_secret SFTP_PASSWORD "$SFTP_PASSWORD"

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
# et la destination est passee en DERNIER argument.
#
# ET `BatchMode=no`, sans lequel RIEN ne marche : `-b fichier` met sftp en mode batch, et le mode
# batch fait que ssh N'ESSAIE JAMAIS le mot de passe (il n'a pas le droit de demander quoi que ce
# soit). sshpass attend donc une invite « password: » qui ne vient pas, et le serveur repond
# « Permission denied (password,publickey) » MOTEUR A IDENTIFIANTS CORRECTS — c'est la panne des dix
# runs rouges consecutifs, tenue a tort pour un mauvais secret (CX File Explorer, lui, demande le
# mot de passe interactivement, donc passe). Prouve sur OpenSSH 9.2 contre un vrai sshd, pty de
# sshpass reproduit : `-b` seul => aucune invite, Permission denied ; `-b -o BatchMode=no` =>
# invite, session ouverte. L'option garde au passage tout l'interet de `-b` (arret a la premiere
# commande fausse, prefixe « - » tolerant).
SFTP=(sshpass -e sftp -o BatchMode=no -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT")
DEST="$SFTP_USERNAME@$SFTP_HOST"

# Pre-vol TCP : un port ferme, un hote inconnu et un mot de passe refuse donnaient le MEME message
# (« session SFTP en echec »). En joignant la prise avant, on sait tout de suite s'il faut regarder le
# panneau (acces SFTP actif, bon port) ou le secret (identifiant, mot de passe).
if timeout 20 bash -c "cat < /dev/null > /dev/tcp/$SFTP_HOST/$SFTP_PORT" 2>/dev/null; then
  say "pre-vol OK : $SFTP_HOST:$SFTP_PORT repond"
else
  die "le serveur $SFTP_HOST:$SFTP_PORT n'est pas joignable depuis le runner GitHub (port ferme, hote inconnu, ou acces SFTP desactive). A verifier dans le panneau MCServerHost : hote exact, port, et activation de l'acces SFTP."
fi
SFTP_LOG="$(mktemp)"
BATCH="$(mktemp)"
sftp_last() {
  # tr -d '\r' : sshpass fait tourner sftp dans un pty, dont la sortie revient en CRLF — sans ce
  # nettoyage, chaque message d'erreur garde ses \r et la verification awk echoue a identifier un
  # fichier qui est pourtant la (taille annoncee « absente » alors que l'envoi a reussi).
  grep -v -i -e 'Warning: Permanently added' -e '^$' "$SFTP_LOG" | tr -d '\r' | head -c 400 | tr '\n' ' '
}
# Un VRAI fichier batch (`-b fichier`), pas `-b -` : c'est la forme documentee partout, et elle ne
# depend pas de la gestion du tiret par la version d'OpenSSH du runner. Le batch n'interdit pas le
# mot de passe tant que `-o BatchMode=no` ouvre le droit de demander (voir le tableau SFTP).
run_sftp() {
  printf '%s\n' "$@" "bye" > "$BATCH"
  "${SFTP[@]}" -b "$BATCH" "$DEST" >"$SFTP_LOG" 2>&1
}

STAMP="$(date -u +%Y%m%d-%H%M%S)"
# Sauvegarde PILOTEE PAR LISTING, pas par le prefixe « - » : ce prefixe (« ignorer l'erreur ») casse
# la ligne de commande sous sshpass — le pty coupe « -mkdir chemin » en « -mkdir » + chemin, et la
# sauvegarde echoue en serie (constate en test local contre un vrai sshd). On liste d'abord
# `plugins/`, on ne renomme que les jar REELLEMENT presents, et le dossier d'horodatage etant
# unique au run, son mkdir ne peut pas echouer pour « existe deja ».
PRE_LIST="$(mktemp)"
if run_sftp "ls -l $PLUGINS_DIR"; then
  tr -d '\r' < "$SFTP_LOG" > "$PRE_LIST"
else
  say "AVERTISSEMENT : listing pre-envoi impossible ($(sftp_last)) — sauvegarde ecartee, les jar en place seront ecrases."
  : > "$PRE_LIST"
fi
REMOTE_CMDS=("mkdir $PLUGINS_DIR/_sauvegarde-$STAMP")
for name in "${JAR_NAMES[@]}"; do
  # $NF vaut « plugins/nom.jar » quand le dossier est liste avec son prefixe (forme de `ls -l plugins`
  # sur le serveur) et « nom.jar » quand il est liste nu : accepter les deux, sinon la sauvegarde
  # croit toujours qu'il n'y a rien a renommer (constate en test local : dossiers _sauvegarde vides).
  if awk -v n="$name" '$NF == n || $NF ~ ("/" n "$")' "$PRE_LIST" | grep -q .; then
    REMOTE_CMDS+=("rename $PLUGINS_DIR/$name $PLUGINS_DIR/_sauvegarde-$STAMP/$name")
  fi
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
  # Aucune ecriture : pas de `put`, `rename` ni `mkdir` — on ne fait que `pwd`, `ls` et `get`
  # (lecture des journaux, voir le bloc diagnostic ci-dessous).
  say "DRY_RUN : ouverture d'une session SFTP en LECTURE SEULE (test des identifiants)…"
  if run_sftp "pwd" "ls -l $PLUGINS_DIR"; then
    say "DRY_RUN : identifiants valides — la session s'ouvre, contenu de $PLUGINS_DIR :"
    tr -d '\r' < "$SFTP_LOG"

    # ------------------------------------------------------------------ diagnostic ValoriaEconomy
    # Symptome : /plugins montre ValoriaEconomy ROUGE alors que le jar (17 029 octets) est pose et
    # verifie. La cause est ECRITE dans logs/latest.log du serveur — mais la console du panneau et
    # les journaux bruts des runs sont inaccessibles depuis l'agent : seules les ANNOTATIONS se
    # lisent a distance. SFTP sait LIRE ce fichier : on le rapatrie (plus le .log.gz precedent) et
    # on publie le diagnostic. Toujours AUCUNE ecriture cote serveur : des `ls` et des `get`.
    #
    # DEUX limites apprises sur le terrain (runs 33325114747, 33325513670, 33326512242) :
    #   1. une coupe `head -c` au milieu d'un accent laisse de l'UTF-8 invalide et GitHub rejette
    #      l'annotation EN SILENCE -> iconv -c partout ou un extrait peut etre coupe ;
    #   2. le runner plafonne une ETAPE a DIX annotations, empreintes comprises : au-dela, tout est
    #      JETE sans erreur. Les quatre empreintes ci-dessus en occupent quatre : ce bloc n'en emet
    #      donc que SIX, les faits essentiels, dans un ordre qui garde les plus utiles en dernier
    #      (si une limite imprévue mord, ce sont les listing qui tranchent, pas le verdict) ; le
    #      DETAIL part par l'API Checks (POST check-runs/{id}/annotations, 50 max) juste apres.
    DIAG_DIR="$(mktemp -d)"
    diag_annot() {
      printf '::notice::%s\n' "$(printf '%s' "$1" \
        | iconv -f UTF-8 -t UTF-8 -c \
        | sed -e 's/%/%25/g' -e 's/\r$//' -e 's/\r//g' \
        | awk 'BEGIN { ORS = "%0A" } { print }' | sed -e 's/%0A$//')"
    }
    # Les messages destines a l'API Checks (meme hygiene UTF-8, une ligne = une annotation).
    diag_msgfile="$DIAG_DIR/messages.txt"
    : > "$diag_msgfile"
    diag_line() { printf '%s\n' "$1" | iconv -f UTF-8 -t UTF-8 -c >> "$diag_msgfile"; }

    # --- rapatriement (les avatars d'echec sont COLLECTES, pas emis : le budget d'etape est fini) ---
    LOG_LISTING="$DIAG_DIR/logs-ls.txt"
    DIAG_NOTE="journaux : "
    if run_sftp "ls -l logs"; then
      tr -d '\r' < "$SFTP_LOG" | grep -v -E "Warning: Permanently|sftp> |^ls: " > "$LOG_LISTING"
    else
      : > "$LOG_LISTING"
      DIAG_NOTE="$DIAG_NOTE listing logs/ IMPOSSIBLE ($(sftp_last)) ;"
    fi
    if run_sftp "get logs/latest.log $DIAG_DIR/latest.log"; then
      DIAG_NOTE="$DIAG_NOTE latest.log $(wc -c < "$DIAG_DIR/latest.log" | tr -d ' ') octets / $(wc -l < "$DIAG_DIR/latest.log" | tr -d ' ') lignes ;"
    else
      : > "$DIAG_DIR/latest.log"
      DIAG_NOTE="$DIAG_NOTE get latest.log REFUSE ($(sftp_last)) ;"
    fi
    PREV_GZ="$(awk '$NF ~ /\.log\.gz$/ { print $NF }' "$LOG_LISTING" | tail -1)"
    if [ -n "$PREV_GZ" ]; then
      if run_sftp "get logs/$PREV_GZ $DIAG_DIR/prev.log.gz"; then
        [ -s "$DIAG_DIR/prev.log.gz" ] && zcat "$DIAG_DIR/prev.log.gz" > "$DIAG_DIR/prev.log" 2>/dev/null || true
        DIAG_NOTE="$DIAG_NOTE $PREV_GZ $(wc -l < "$DIAG_DIR/prev.log" 2>/dev/null | tr -d ' ') lignes"
      else
        : > "$DIAG_DIR/prev.log"
        DIAG_NOTE="$DIAG_NOTE get $PREV_GZ refuse ($(sftp_last))"
      fi
    fi
    [ -s "$DIAG_DIR/prev.log" ] || : > "$DIAG_DIR/prev.log"

    # Le journal a interpreter : le courant (latest.log), sinon le precedent.
    SOURCE="$DIAG_DIR/latest.log"
    SOURCE_NOM="latest.log"
    if [ ! -s "$SOURCE" ]; then
      SOURCE="$DIAG_DIR/prev.log"
      SOURCE_NOM="journal precedent (latest.log absent ou vide !)"
    fi

    # Digest complet (journal du run + resume) : tout le contexte, sans contrainte de taille.
    DIGEST="$DIAG_DIR/digest.txt"
    : > "$DIGEST"
    for name in latest prev; do
      f="$DIAG_DIR/$name.log"
      [ -s "$f" ] || continue
      {
        echo "===== extrait de $name.log ====="
        grep -m 2 -E "Starting minecraft server version|This server is running|Loading [0-9]+ plugin" "$f" || true
        echo "----- lignes Valoria / Economy -----"
        grep -n -i -m 40 "valoria\|econom" "$f" || true
        echo "----- erreurs / exceptions (avec 14 lignes de contexte) -----"
        awk '/Exception|ERROR|SEVERE|Error occurred|Caused by/ { print; n = 0
              while (n < 14 && (getline ligne) > 0) { if (ligne ~ /^(Caused by|[[:space:]]+at |\.\.\. [0-9]+ more)/) { print ligne; n++ } else break } }' "$f" || true
        echo "----- cycle de vie des plugins -----"
        grep -n -E "Enabling|Disabling|Ambiguous|Could not load" "$f" | head -30 || true
        echo "----- 15 premieres lignes -----"
        head -15 "$f" || true
        echo "----- 25 dernieres lignes -----"
        tail -25 "$f" || true
      } >> "$DIGEST"
    done
    say "DRY_RUN : diagnostic — extrait des journaux du serveur :"
    cat "$DIGEST"
    if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
      { echo "## Diagnostic serveur (lecture seule)"; echo '```'; cat "$DIGEST"; echo '```'; } >> "$GITHUB_STEP_SUMMARY"
    fi

    # --- les SIX faits essentiels (budget runner : 4 empreintes + 6 ici = 10 par etape) ---
    # 1. ce qui a ete rapatrie (et les refus — un diagnostic qui ne dit pas qu'il lui manque le
    #    journal courant ressemble a un diagnostic complet : c'est le piege du run 33325114747).
    diag_annot "DIAGNOSTIC : $DIAG_NOTE — source analysee : $SOURCE_NOM"
    # 2. listing plugins/ avec dates : un doublon ou une date d'avant le depot se voit ici.
    diag_annot "DIAGNOSTIC : plugins/ (nom [octets, date du ls]) = $(awk '$NF ~ /\.jar$/ { printf "%s [%s o, %s %s %s] ; ", $NF, $5, $6, $7, $8 }' "$PRE_LIST" | head -c 700)"
    # 3. listing logs/ : quand latest.log a-t-il ete ecrit pour la derniere fois.
    diag_annot "DIAGNOSTIC : listing logs/ = $(grep -v '^total' "$LOG_LISTING" | tr '\n' ' ' | head -c 900)"
    # 4. VERDICT d'horloge unique : mtime des jars vs mtime de latest.log DANS LE MEME ls.
    #    Les horloges du serveur et du runner ne sont pas comparables ; celles d'un meme listing, si.
    t_log="$(awk '$NF == "latest.log" { print $6, $7, $8 }' "$LOG_LISTING" | head -1)"
    e_log="$(date -d "$t_log" +%s 2>/dev/null || printf 0)"
    jar_le_plus_recent=""
    e_best=0
    for nom in $(awk '$NF ~ /\.jar$/ { print $NF }' "$PRE_LIST"); do
      d="$(awk -v n="$nom" '$NF == n { print $6, $7, $8 }' "$PRE_LIST" | head -1)"
      e="$(date -d "$d" +%s 2>/dev/null || printf 0)"
      if [ "$e" -gt "$e_best" ]; then e_best="$e"; jar_le_plus_recent="$nom ($d)"; fi
    done
    if [ -z "$t_log" ]; then
      diag_annot "DIAGNOSTIC VERDICT : indeterminable — latest.log absent du listing logs/ (dossier illisible ?)"
    elif [ "$e_best" -gt "$e_log" ]; then
      diag_annot "DIAGNOSTIC VERDICT : $jar_le_plus_recent est PLUS RECENT que latest.log ($t_log) — le serveur n'a pas redemarre depuis le dernier depot : REDEMARRER LE SERVEUR, le rouge observe est celui des anciens jars."
    else
      diag_annot "DIAGNOSTIC VERDICT : latest.log ($t_log) est plus recent que tous les jars — le dernier demarrage a bien charge les jars deposes."
    fi
    # 5. compteurs + version (une seule annotation pour les deux).
    VERS="$(grep -m 1 -E "Starting minecraft server version|This server is running" "$SOURCE" 2>/dev/null || true)"
    VERS_COURT="$(printf '%s' "$VERS" | grep -o -E "version [0-9][^ ]*|Paper [0-9][^ ]*" | head -1)"
    n_en="$(grep -c 'Enabling' "$SOURCE" 2>/dev/null)"; n_en="${n_en:-0}"
    n_er="$(grep -c 'ERROR' "$SOURCE" 2>/dev/null)"; n_er="${n_er:-0}"
    n_do="$(grep -c 'Done (' "$SOURCE" 2>/dev/null)"; n_do="${n_do:-0}"
    n_va="$(grep -ci 'valoria' "$SOURCE" 2>/dev/null)"; n_va="${n_va:-0}"
    diag_annot "DIAGNOSTIC : $SOURCE_NOM ($VERS_COURT) : $n_en Enabling, $n_er ERROR, $n_do Done, $n_va lignes valoria — Enabling=0 : demarrage jamais arrive aux plugins ; Done=0 : demarrage inacheve."
    # 6. la premiere exception avec le haut de sa pile (le coupable, en une ligne).
    PREMIERE_EXC="$(awk '/Exception|ERROR|SEVERE|Error occurred/ { print; n = 0
          while (n < 3 && (getline ligne) > 0) { if (ligne ~ /^(Caused by|[[:space:]]+at )/) { print " <- " ligne; n++ } else break } exit }' "$SOURCE" 2>/dev/null)"
    if [ -n "$PREMIERE_EXC" ]; then
      diag_annot "DIAGNOSTIC : premiere erreur de $SOURCE_NOM : $(printf '%s' "$PREMIERE_EXC" | tr '\n' ' ' | head -c 420)"
    else
      diag_annot "DIAGNOSTIC : aucune ligne d'erreur dans $SOURCE_NOM."
    fi

    # --- le DETAIL par l'API Checks : le budget d'etape (10) est epuise, l'API en accepte 50. ---
    # job id == check run id pour un job Actions (les annotations des runs precedents se lisaient
    # deja sur ce chemin). En dehors de la CI (script lance a la main), on saute sans erreur.
    if [ -n "${GITHUB_RUN_ID:-}" ] && [ -n "${GITHUB_TOKEN:-${GH_TOKEN:-}}" ] && command -v gh >/dev/null 2>&1; then
      head -8 "$SOURCE" 2>/dev/null | while IFS= read -r ligne; do diag_line "DEBUT | ${ligne:0:240}"; done
      tail -10 "$SOURCE" 2>/dev/null | while IFS= read -r ligne; do diag_line "FIN  | ${ligne:0:240}"; done
      grep -E "Enabling|Disabling|Ambiguous|Could not load" "$SOURCE" 2>/dev/null | head -14 | while IFS= read -r ligne; do diag_line "${ligne:0:480}"; done
      awk '/Exception|ERROR|SEVERE|Error occurred/ { bloc = $0; n = 0
            while (n < 14 && (getline ligne) > 0) { if (ligne ~ /^(Caused by|[[:space:]]+at |\.\.\. [0-9]+ more)/) { bloc = bloc "\n" ligne; n++ } else break }
            print bloc }' "$SOURCE" 2>/dev/null | head -6 | while IFS= read -r bloc; do diag_line "${bloc:0:480}"; done
      grep -n -i -m 30 "valoria\|econom" "$SOURCE" 2>/dev/null | while IFS= read -r ligne; do diag_line "${ligne:0:480}"; done
      if ! grep -q -i "valoria" "$SOURCE" 2>/dev/null; then
        diag_line "AVERTISSEMENT : aucune ligne valoria dans $SOURCE_NOM"
      fi
      if [ -s "$diag_msgfile" ]; then
        jq -R -s 'split("\n") | map(select(length > 0)) | .[0:39]
                  | map({path: "logs/latest.log", start_line: 1, end_line: 1,
                         annotation_level: "notice", message: ., title: "DIAGNOSTIC serveur"})' \
          "$diag_msgfile" > "$DIAG_DIR/annots.json" 2>/dev/null \
          && JOB_ID="$(gh api repos/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID/jobs --jq '.jobs[0].id' 2>/dev/null)" \
          && [ -n "$JOB_ID" ] \
          && gh api --method POST -H "Content-Type: application/json" \
               "repos/$GITHUB_REPOSITORY/check-runs/$JOB_ID/annotations" --input "$DIAG_DIR/annots.json" >/dev/null 2>&1 \
          && say "DIAGNOSTIC : $(wc -l < "$diag_msgfile" | tr -d ' ') lignes de detail publiees par l'API Checks (run $GITHUB_RUN_ID)." \
          || say "AVERTISSEMENT : publication du detail par l'API Checks impossible — les six faits essentiels restent en annotations d'etape."
      fi
    fi
    exit 0
  fi
  msg="$(grep -v -i -e 'Warning: Permanently added' -e '^$' "$SFTP_LOG" | tr -d '\r' | head -c 500 | tr '\n' ' ')"
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
  LISTING="$(tr -d '\r' < "$SFTP_LOG")"
else
  say "AVERTISSEMENT : listing distant impossible ($(sftp_last)) — la verification de taille va donc tout signaler comme absent."
fi
fail=0
for name in "${JAR_NAMES[@]}"; do
  want=$(stat -c %s "target/$name")
  got=$(printf '%s\n' "$LISTING" | awk -v n="$name" '$NF == n || $NF ~ ("/" n "$") {print $5}')
  if [ "${got:-}" = "$want" ]; then
    say "serveur OK : $name ($got octets)"
  else
    say "ERREUR : le serveur annonce '${got:-absent}' octets pour $name, attendu $want"
    fail=1
  fi
done
[ "$fail" = 0 ] || die "envoi non verifie : les jar du serveur ne correspondent pas octet pour octet (rollback : restaurer $PLUGINS_DIR/_sauvegarde-$STAMP/)"

say "deploiement termine et verifie. Redemarrer le serveur pour charger les nouveaux jar."
