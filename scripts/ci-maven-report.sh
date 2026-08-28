#!/usr/bin/env bash
# Build Maven + rapport d'echec auto-publie (PR, sinon commit sur la branche) + controles du depot.
#
# Pourquoi ce script existe : ce depot ne se compile pas en bloc (le paquet livre des classes
# precompilees, l'arbre decompiles complet n'est pas recompilable) — le build compile une liste
# explicite de fichiers contre le binaire livre. Un echec `javac` est donc leve frequent et le plus
# dur a lire : le journal de GitHub Actions est replie, les lignes utiles sont noyees, et il est
# materiallement inaccessibles depuis certains environnements (redirect Azure desactive).
#
# Ce script :
#   1. lance `mvn clean package` en capturant TOUTE la sortie dans un fichier ;
#   2. extrait les erreurs javac localisees (fichier, ligne) avec le detail et l'extrait du source ;
#   3. rejoue les controles du depot pour que le rapport soit complet meme si la compilation casse tot ;
#   4. publie le rapport en commentaire de la Pull Request (GITHUB_TOKEN) ; a defaut, le commit sur la
#      branche sous `docs/DERNIER-LOG-CI.md` — comme ca, le rapport est lisible par un agent sans
#      acces aux journaux d'Actions ;
#   5. quitte avec le code de Maven : une etape rouge = le deploiement SFTP n'est pas lance.
#
# Localisation :   bash scripts/ci-maven-report.sh
# Dans un workflow : - run: bash scripts/ci-maven-report.sh
#                      env: { GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }} }
#
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

LOG="${RUNNER_TEMP:-/tmp}/maven-build.log"
REPORT_FILE="${RUNNER_TEMP:-/tmp}/ci-report.md"
MVN_ARGS=(-B -ntp clean package -DskipTests)

echo "::group::mvn ${MVN_ARGS[*]}"
if command -v mvn >/dev/null 2>&1; then
  mvn "${MVN_ARGS[@]}" 2>&1 | tee "$LOG"
  MVN_STATUS=${PIPESTATUS[0]}
else
  {
    echo "[ERROR] Maven (mvn) introuvable dans le PATH"
    echo "[INFO] BUILD FAILURE"
  } | tee "$LOG"
  MVN_STATUS=127
fi
echo "::endgroup::"

build_report() {
  LOG_PATH="$LOG" MVN_STATUS="$MVN_STATUS" python3 - <<'PY'
import os
import re
from pathlib import Path

ROOT = Path.cwd()
log_path = Path(os.environ["LOG_PATH"])
status = os.environ["MVN_STATUS"]
lines = log_path.read_text(encoding="utf-8", errors="replace").splitlines() if log_path.is_file() else []

ERR = re.compile(r"^\[(ERROR|WARNING)\]\s*(.*)$")
# javac : `Chemin/Fichier.java:[ligne,colonne] message`, puis des lignes de detail decalessees.
LOCATED = re.compile(r"^(?P<file>[\w./\\-]+\.(?:java|xml|yml)):\[(?P<line>\d+),(?P<col>\d+)\]\s*(?P<msg>.*)$")
PLAIN = re.compile(r"^(?P<file>[\w./\\-]+\.(?:java|xml|yml)):(?P<line>\d+)\s+(?P<msg>.*)$")

errors, warnings, fatal = [], [], []
for raw in lines:
    stripped = raw.strip()
    if not ERR.match(stripped):
        if "BUILD FAILURE" in raw or "Could not resolve dependencies" in raw \
                or "Non-resolvable" in raw or "OutOfMemoryError" in raw:
            fatal.append(stripped)
        continue
    level, body = ERR.match(stripped).groups()
    if not body or body.startswith("-> [Help") or "To see the full stack trace" in body \
            or "Re-run Maven with" in body or "For more information about" in body:
        continue
    if errors and re.match(r"^(?:symbol|location|required|found:|reason:|and|\^)", body):
        errors[-1]["detail"].append(body)
        continue
    entry = {"file": None, "line": 0, "msg": body, "detail": []}
    for pattern in (LOCATED, PLAIN):
        loc = pattern.match(body)
        if loc:
            entry.update(file=loc.group("file").replace("\\", "/"), line=int(loc.group("line")),
                         msg=(loc.group("msg") or body).strip())
            break
    (errors if level == "ERROR" else warnings).append(entry)


def source_snippet(rel_path, around, before=4, after=4):
    if not rel_path:
        return ""
    candidates = [ROOT / rel_path]
    candidates += list(ROOT.glob("**/" + Path(rel_path).name))
    for path in candidates:
        if not path.is_file():
            continue
        try:
            src = path.read_text(encoding="utf-8", errors="replace").splitlines()
        except OSError:
            return ""
        start = max(0, around - before - 1)
        stop = min(len(src), around + after)
        block = "\n".join("%5d| %s" % (i + 1, src[i]) for i in range(start, stop))
        try:
            label = path.relative_to(ROOT)
        except ValueError:
            label = path
        return "`%s`\n  ```java\n%s\n  ```" % (label, block)
    return ""


out = []
if errors:
    out.append("### Erreurs de compilation (`javac`)")
    for i, entry in enumerate(errors[:12]):
        if entry["file"]:
            out.append("- **%s:%d** — %s" % (entry["file"], entry["line"], entry["msg"]))
        else:
            out.append("- %s" % entry["msg"])
        for detail in entry["detail"][:5]:
            out.append("  - %s" % detail)
        snippet = source_snippet(entry["file"], entry["line"])
        if snippet:
            out.append("  " + snippet)
    if len(errors) > 12:
        out.append("- … %d autres lignes `[ERROR]` dans le journal complet." % (len(errors) - 12))
else:
    out.append("### Aucune ligne `[ERROR]` extraite"
               + (" (maven a echoue avant la compilation — voir « causes bloquantes »)."
                  if status != "0" else " (build reussi)."))

if fatal:
    out.append("\n### Causes bloquantes")
    for line in fatal[:6]:
        out.append("- `%s`" % line)

out.append("\n<sub>exit maven %s · %d lignes de journal · genere par `scripts/ci-maven-report.sh`"
           " (le rapport est reecrit a chaque run)</sub>" % (status, len(lines)))
print("\n".join(out)[:15000])
PY
}

REPORT="$(build_report)"
echo
echo "$REPORT"
printf '%s\n' "$REPORT" > "$REPORT_FILE"

# Canal 1 : le resume de l'etape (visible en haut de la page du job, dans GitHub).
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  printf '%s\n' "$REPORT" >> "$GITHUB_STEP_SUMMARY"
fi

# Canal 2 : des annotations `::error::` / `::notice::`. C'est LE canal fiable : les annotations se
# lisent par l'API `/check-runs/{id}/annotations` sans permission particuliere, alors que le journal
# brut est servi par un stockage externe generalement bloque.
python3 - "$REPORT_FILE" <<'PY'
import os
import sys
from pathlib import Path

text = Path(sys.argv[1]).read_text(encoding="utf-8")
# Les decoupages doivent rester sous la taille max d'une annotation (~64 ko) et peu nombreux :
# on tronque le rapport a 14 ko puis on le coupe en blocs de ~3 ko.
text = text[:14000]
chunk = 3000
blocks = [text[i:i + chunk] for i in range(0, len(text), chunk)][:12]
for i, block in enumerate(blocks):
    one_line = block.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
    level = "error" if i == 0 else "notice"
    print(f"::{level} title=Rapport de compilation ({i + 1}/{len(blocks)})::{one_line}")
PY

# Les controles du depot, meme quand la compilation echoue : un seul rapport, toute la verite.
EXTRA=""
for check in "python3 scripts/verify-paper26-compat.py" \
             "python3 scripts/verify-economy-api.py" \
             "python3 scripts/selfmade-api-patch.py --check" \
             "python3 scripts/generate-economy-api.py --check" \
             "python3 scripts/build-reference-jar.py --check"; do
  if out="$(eval "$check" 2>&1)"; then
    EXTRA+=$'\n'"OK   $check"
  else
    EXTRA+=$'\n'"KO   $check"$'\n'"$(printf '%s\n' "$out" | tail -6 | sed 's/^/     /')"
  fi
done
if [ -n "$EXTRA" ]; then
  REPORT+=$'\n\n### Controles hors compilation\n```'"$EXTRA"$'\n```'
  printf '%s\n' "$REPORT" > "$REPORT_FILE"
fi

publish_report() {
  # hors CI : rien a publier
  [ -n "${GITHUB_SHA:-}" ] || return 0
  [ -s "$REPORT_FILE" ] || return 0
  if [ -z "${GITHUB_TOKEN:-}" ]; then
    if [ -n "${GH_TOKEN:-}" ]; then
      export GITHUB_TOKEN="$GH_TOKEN"
    else
      echo "(pas de token : le rapport reste uniquement dans le journal ci-dessus)"
      return 0
    fi
  fi
  REPORT_FILE="$REPORT_FILE" python3 - <<'PY'
import base64
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

api = os.environ.get("GITHUB_API_URL", "https://api.github.com")
repo = os.environ.get("GITHUB_REPOSITORY", "")
sha = os.environ.get("GITHUB_SHA", "HEAD")
branch = os.environ.get("GITHUB_REF_NAME", "")
body = Path(os.environ["REPORT_FILE"]).read_text(encoding="utf-8")
if not repo:
    raise SystemExit("(pas de GITHUB_REPOSITORY : rien a publier)")

headers = {"Accept": "application/vnd.github+json", "Authorization": "Bearer " + os.environ["GITHUB_TOKEN"],
           "X-GitHub-Api-Version": "2022-11-28", "Content-Type": "application/json"}


def call(path, payload=None, method=None):
    data = json.dumps(payload).encode() if payload is not None else None
    request = urllib.request.Request(api + path, data=data, headers=headers,
                                     method=method or ("PATCH" if payload else "GET"))
    with urllib.request.urlopen(request, timeout=40) as answer:
        raw = answer.read()
        return answer.status, (json.loads(raw) if raw else {})


# Numero de PR : sur un evenement pull_request, GITHUB_SHA est le commit de FUSION fabrique par
# GitHub — `/commits/<sha>/pulls` ne repond rien pour celui-la (c'etait le bug : le rapport partait
# dans le vide). On lit donc d'abord le contexte de l'evenement, puis on retombe sur le SHA de la
# branche (head), puis sur une recherche par branche.
pr = os.environ.get("PR_NUMBER", "")
if not pr:
    event = os.environ.get("GITHUB_EVENT_PATH", "")
    if event and os.path.exists(event):
        try:
            payload = json.load(open(event, encoding="utf-8"))
            pr = str((payload.get("pull_request") or {}).get("number")
                     or (payload.get("number") if payload.get("pull_request") is None else "") or "")
            pr = "" if pr == "None" else pr
        except Exception as error:
            print("(evenement illisible : %s)" % error)
if not pr:
    try:
        status, pulls = call("/repos/%s/commits/%s/pulls" % (repo, os.environ.get("GITHUB_HEAD_SHA") or sha))
        if pulls:
            pr = str(pulls[0]["number"])
    except Exception as error:
        print("(PR introuvable par sha : %s)" % error)
if not pr:
    try:
        status, found = call("/repos/%s/pulls?state=open&per_page=20" % repo)
        pr = str(next((x["number"] for x in found
                       if x.get("head", {}).get("ref") == os.environ.get("GITHUB_HEAD_REF")
                       or x.get("head", {}).get("ref") == os.environ.get("GITHUB_REF_NAME")), ""))
    except Exception as error:
        print("(PR introuvable par branche : %s)" % error)
print("contexte publication : repo=%s branch=%s pr=%s" % (repo, os.environ.get("GITHUB_REF_NAME"), pr or "aucune"))

# Diagnostic de permission : le token d'un job ne peut ecrire que si le workflow le declare ET si le
# depot l'autorise. Sans cette ligne, un 403 mu rend le rapport impossible a diagnostiquer.
try:
    status, perms = call("/repos/%s/actions/permissions/workflow" % repo)
    print("permissions effectives du workflow : %s" % json.dumps(perms))
    sys.stdout.flush()
    print("::notice title=Permissions du workflow::%s" % json.dumps(perms).replace("%", "%25"))
except Exception as error:
    print("(permissions du workflow illisibles : %s)" % error)
    sys.stdout.flush()

published = False
if pr:
    try:
        status, _ = call("/repos/%s/issues/%s/comments" % (repo, pr),
                         {"body": "<!-- ci-maven-report -->\n" + body}, method="POST")
        published = status in (200, 201)
        print("commentaire publie sur la PR #%s (HTTP %s)" % (pr, status))
    except urllib.error.HTTPError as error:
        detail = "%s %s" % (error.code, error.read()[:200].decode("utf-8", "replace"))
        print("(commentaire refuse HTTP %s)" % detail)
        sys.stdout.flush()
        raise SystemExit("commentaire impossible: " + detail[:180])
    except Exception as error:
        print("(commentaire impossible : %s)" % error)

if not published:
    print("(repli : tentative de commit du rapport sur la branche)")
if not published and branch:
    # Repli : commit du rapport sur la branche. Sans lui, l'erreur reste enfermee dans un journal
    # d'Actions inaccessible hors du navigateur, et le correcteur à distance tourne a l'aveugle.
    path = "/repos/%s/contents/docs/DERNIER-LOG-CI.md" % repo
    sha_existing = None
    try:
        status, existing = call(path + "?ref=" + branch)
        sha_existing = existing.get("sha") if status == 200 else None
    except Exception:
        sha_existing = None
    payload = {"message": "Rapport de compilation CI (genere par le build)",
               "content": base64.b64encode(body.encode()).decode(), "branch": branch}
    if sha_existing:
        payload["sha"] = sha_existing
    try:
        status, _ = call(path, payload, method="PATCH" if sha_existing else "PUT")
        print("rapport commit sur %s : docs/DERNIER-LOG-CI.md (HTTP %s)" % (branch, status))
    except urllib.error.HTTPError as error:
        print("(commit refuse HTTP %s : %s)" % (error.code, error.read()[:200].decode("utf-8", "replace")))
    except Exception as error:
        print("(repli par commit refuse : %s)" % error)
        print("(le rapport reste dans le journal ci-dessus)")
PY
}

if [ "$MVN_STATUS" != "0" ]; then
  publish_report
fi

exit "$MVN_STATUS"
