#!/usr/bin/env bash
# Build Maven + rapport automatique des echecs de compilation sur la Pull Request.
#
# Pourquoi ce script existe : ce depot est compile classe par classe (le paquet livre des classes
# precompilees, l'arbre decompiles complet ne compile pas). Un echec `javac` est donc l'evenement le
# plus frequent et le plus dur a lire dans GitHub Actions — le journal est replie, les lignes utiles
# sont noyees dans 4000 lignes de telechargements Maven. Ce script :
#
#   1. lance `mvn clean package` et capte TOUTE la sortie dans un fichier ;
#   2. extrait les lignes [ERROR] « parlantes » (fichier + ligne + colonne) et remonte, pour
#      chacune, l'extrait du fichier source concerne ;
#   3. publie ce resume en commentaire de la PR (via GITHUB_TOKEN, sans secret a creer) ;
#   4. rejoue les controles du depot (surface des sources, cohérence de l'API d'economie) pour que
#      le rapport soit complet meme quand la compilation echoue tot ;
#   5. quitte avec le code de maven : une etape rouge = le deploiement SFTP n'est pas lance.
#
# Utilisation locale :   bash scripts/ci-maven-report.sh              (sans GITHUB_ENV, pas de post)
# Dans un workflow :     - run: bash scripts/ci-maven-report.sh
#
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

LOG="${RUNNER_TEMP:-/tmp}/maven-build.log"
MVN_ARGS=(-B -ntp clean package -DskipTests)

echo "::group::mvn ${MVN_ARGS[*]}"
if command -v mvn >/dev/null 2>&1; then
  mvn "${MVN_ARGS[@]}" 2>&1 | tee "$LOG"
  MVN_STATUS=${PIPESTATUS[0]}
else
  echo "[ERROR] Maven (mvn) introuvable dans le PATH" | tee "$LOG" >/dev/null
  echo "mvn: commande introuvable — le build ne peut pas demarrer" >> "$LOG"
  MVN_STATUS=127
fi
echo "::endgroup::"

report() {
  python3 - "$LOG" "$MVN_STATUS" <<'PY'
import re
import sys
from pathlib import Path

ROOT = Path.cwd()
log_path, status = Path(sys.argv[1]), Path(sys.argv[2])
status = str(status)
lines = log_path.read_text(encoding="utf-8", errors="replace").splitlines() if log_path.is_file() else []

ERR = re.compile(r"^\[(ERROR|WARNING)\]\s*(.*)$")
# javac ecrit `Chemin/Fichier.java:[ligne,colonne] message`, puis les lignes de detail commencent par
# deux espaces : on les rattache a l'erreur precedente au lieu d'en faire des points de liste.
LOCATED = re.compile(r"^(?P<file>[\w./\\-]+\.(?P<ext>java|xml))(?::\[|:\[|:\[)?(?::)?(?P<line>\d+)[,\]](?P<col>\d+)\]?\s*(?P<msg>.*)$")
PLAIN = re.compile(r"^(?P<file>[\w./\\-]+\.(?P<ext>java|xml)):(?P<line>\d+)\s*(?P<msg>.*)$")

errors, warnings, fatal = [], [], []
for raw in lines:
    m = ERR.match(raw.strip())
    if not m:
        if "BUILD FAILURE" in raw or "Could not resolve dependencies" in raw or "No such file or directory" in raw:
            fatal.append(raw.strip())
        continue
    level, body = m.groups()
    if not body or body.startswith("-> [Help") or "To see the full stack trace" in body \
            or "Re-run Maven with" in body or "For more information about" in body:
        continue
    if re.match(r"^\s{2,}\S", raw) and errors:          # detail de l'erreur precedente
        errors[-1]["detail"].append(body.strip())
        continue
    entry = {"file": None, "line": 0, "msg": body, "detail": []}
    for pattern in (LOCATED, PLAIN):
        loc = pattern.match(body)
        if loc:
            entry.update(file=loc.group("file").replace("\\", "/"), line=int(loc.group("line") or 0),
                         msg=(loc.group("msg") or body).strip())
            break
    (errors if level == "ERROR" else warnings).append(entry)

def read_source(rel_path, around, before=3, after=3):
    """Extrait du fichier cite, autour de la ligne fautive : l'erreur se lit sans le journal."""
    if not rel_path:
        return ""
    candidates = [ROOT / rel_path]
    candidates += list(ROOT.glob("*/" + rel_path.split("/", 1)[-1])) if "/" not in rel_path else []
    if "/" in rel_path:
        candidates += list(ROOT.glob("**/" + Path(rel_path).name))
    for path in candidates:
        if not path.is_file():
            continue
        try:
            src = path.read_text(encoding="utf-8", errors="replace").splitlines()
        except OSError:
            return ""
        start = max(0, around - before - 1) if around else 0
        stop = min(len(src), (around + after) if around else 24)
        block = "\n".join(f"{i+1:5d}| {src[i]}" for i in range(start, stop))
        return f"`{path.relative_to(ROOT)}`\n  ```java\n{block}\n  ```"
    return ""

out = []
if errors:
    out.append("### Erreurs de compilation (`javac`)")
    shown = 0
    for entry in errors:
        if shown >= 12:
            out.append(f"- … {len(errors) - shown} autres lignes `[ERROR]` dans le journal complet.")
            break
        head = f"- **`{entry['file']}`:{entry['line']}** — {entry['msg']}" if entry["file"] else f"- {entry['msg']}"
        out.append(head)
        for d in entry["detail"][:4]:
            out.append(f"  - {d}")
        snippet = read_source(entry["file"], entry["line"])
        if snippet:
            out.append("  " + snippet)
        shown += 1
else:
    out.append("### Compilation : aucune ligne `[ERROR]` extraite"
               + (" (maven a echoue tres tot — voir le journal)." if status != "0" else "."))

if fatal:
    out.append("\n### Causes bloquantes")
    for f in fatal[:8]:
        out.append(f"- `{f}`")

if warnings and len(out) < 6:
    out.append("\n### Avertissements utiles")
    for w in warnings[:10]:
        out.append(f"- {w}")

out.append(f"\n<sub>statut maven : exit {status} · {len(lines)} lignes de journal · "
           "regenere par `scripts/ci-maven-report.sh`</sub>")
print("\n".join(out)[:15000])
PY
}

REPORT="$(report)"
echo
echo "$REPORT"

# Les controles du depot, meme en cas d'echec de compilation : le rapport doit tout dire d'un coup.
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
[ -n "$EXTRA" ] && REPORT+=$'\n\n### Controles hors compilation (exécutés localement dans le runner)\n```'"$EXTRA"$'\n```'

post_to_pr() {
  # hors CI (test local) : rien a publier
  [ -n "${GITHUB_SHA:-}" ] || return 0
  # `GITHUB_TOKEN` n'est injecte que dans les etapes `uses:` : un `env:` d'etape doit le declarer.
  # On accepte donc aussi GH_TOKEN, et labsence de token ne doit jamais faire echouer le build.
  if [ -z "${GITHUB_TOKEN:-}" ]; then
    if [ -n "${GH_TOKEN:-}" ]; then
      export GITHUB_TOKEN="$GH_TOKEN"
    else
      echo "(pas de token : le rapport reste uniquement dans le journal ci-dessus)"
      return 0
    fi
  fi
  local api repo pr body_file
  api="${GITHUB_API_URL:-https://api.github.com}"
  repo="${GITHUB_REPOSITORY:-}"
  [ -n "$repo" ] || return 0
  pr="$(curl -s --max-time 20 -H "Authorization: Bearer $GITHUB_TOKEN" \
        "$api/repos/$repo/commits/${GITHUB_SHA:-HEAD}/pulls" \
        | python3 -c 'import json,sys
try:
    print(json.load(sys.stdin)[0]["number"])
except Exception:
    pass')"
  [ -n "$pr" ] || return 0
  body_file="$(mktemp)"
  printf '%s' "$REPORT" > "$body_file"
  python3 - "$api" "$repo" "$pr" "$body_file" <<'PY'
import json
import os
import sys
import urllib.request

api, repo, pr, body_file = sys.argv[1:5]
body = open(body_file, encoding="utf-8").read()
marker = "<!-- ci-maven-report -->"
payload = json.dumps({"body": marker + "\n" + body}).encode()
url = f"{api}/repos/{repo}/issues/{pr}/comments"
request = urllib.request.Request(url, data=payload, method="POST", headers={
    "Accept": "application/vnd.github+json",
    "Authorization": "Bearer " + os.environ["GITHUB_TOKEN"],
    "X-GitHub-Api-Version": "2022-11-28",
})
try:
    with urllib.request.urlopen(request, timeout=30) as answer:
        print(f"commentaire publie sur PR #{pr} (HTTP {answer.status})")
except Exception as error:  # jamais faire echouer le build a cause d'un commentaire
    print(f"commentaire non publie ({error}) — le rapport reste dans le journal ci-dessus")
PY
  rm -f "$body_file"
}

if [ "$MVN_STATUS" != "0" ]; then
  post_to_pr
fi

exit "$MVN_STATUS"
