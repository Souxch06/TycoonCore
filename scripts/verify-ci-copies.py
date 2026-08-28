#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Refuse les copies divergentes de workflow et la chaine de declenchement casseee.

Ce script est ne d'un incident precis : le depot contenait QUATRE textes des workflows
(`docs/CI-A-COLLER.yml`, sa copie de travail `scripts/ci/build-workflow.yml`, `docs/paste/build.yml`, et
un bloc colle dans `docs/TUTO-INSTALLATION-FINALE.md`). Deux de ces copies en etaient restees a un build
« deux jar, sans etape de release » : l'administrateur qui les colle obtient un pipeline vert qui ne
publie rien, donc un dossier `plugins/` qui ne bouge jamais, sans une seule ligne d'erreur. Puis, dans la
meme journee, deux regles de ce fichier se sont revelees faussement positives en run de PR (le `pull_request`
est joue sur le *merge ref*, qui contient les workflows de `main`) : un controle qui crie sur un code
correct est ignore des la deuxieme fois, donc il pis que rien.

Regles, dans l'ordre ou elles attrapent une panne :
  1. la source collable et son miroir de travail sont identiques octet pour octet ;
  2. le build se declenche sur `main` (un `branches-ignore: main` coupe toute la chaine en silence) et
     ne publie la release QUE depuis `main` ;
  3. le controle de contenu des jar est un script etiquette (et pas un `bash -c` de quarante lignes dont
     l'echec est un code de sortie muet) ;
  4. le depot exige les TROIS jar, et sauvegarde avant d'ecraser ;
  5. aucun contenu de workflow complet ne traine ailleurs que dans les deux fichiers canoniques ;
  6. cette branche n'ajoute ni ne modifie rien dans `.github/workflows` (sinon : conflit de merge add/add
     avec main, et merge impossible en silence) ;
  7. forme YAML des fichiers a coller (pas de tabulation, squelette `name`/`on`/`jobs`, parse js-yaml si
     l'outil est installe localement).

Sortie : 0 si tout est coherent, 1 sinon.

    python3 scripts/verify-ci-copies.py
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/CI-A-COLLER.yml"
MIRROR = ROOT / "scripts/ci/build-workflow.yml"
DEPLOY = ROOT / "docs/CI-DEPLOY-A-COLLER.yml"
NEUTRAL = ROOT / "docs/paste/deploy-neutralise.yml"
STUBS = ("docs/paste/build.yml", "docs/paste/deploy-serveur.yml")
CANONICAL = (SOURCE, MIRROR, DEPLOY, NEUTRAL)
TOP_KEYS = ("name", "on", "permissions", "concurrency", "env", "defaults", "jobs", "run-name")

problems = []
notes = []


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def check(label, ok, detail=""):
    (notes if ok else problems).append((label, detail))
    mark = "OK " if ok else "KO "
    print(f"  [{mark}] {label}{(' — ' + detail) if detail and not ok else ''}")


def body(path: Path) -> str:
    """Le fichier, commentaires et chaines conserves : on y cherche des NOMS, pas des appels."""
    return path.read_text(encoding="utf-8", errors="replace")


def looks_like_workflow(text: str) -> bool:
    """Vrai si le fichier pretend etre un workflow complet (`name` + `on` + `jobs` au niveau 0)."""
    return bool(re.search(r"^name:\s", text, re.M)
                and re.search(r"^on:\s", text, re.M)
                and re.search(r"^jobs:\s", text, re.M))


def trigger_block(text: str) -> str:
    """Le bloc `on:` jusqu'a `jobs:` — c'est la que se cache un filtre de branches silencieux."""
    match = re.search(r"^on:\n(?:.*\n)*?^jobs:", text, re.M)
    return match.group(0) if match else ""


def ignores_main(text: str) -> bool:
    block = trigger_block(text)
    return bool(re.search(r"branches-ignore:[^\n]*\n[ \t]*-[ \t]+main", block)) \
        or bool(re.search(r"branches-ignore:\s*\[[^\]]*main", block))


def workflows_touched_by_this_branch():
    """Les fichiers de `.github/workflows` touches par CETTE branche, ou `None` si indeterminable.

    `None` ne veut pas dire « rien n'est touche » : ca veut dire « je ne sais pas », et le controle
    s'abstient. Un run `pull_request` est joue sur le merge ref, qui contient legitimement les workflows
    de `main` : juger le contenu du merge ref accuserait la branche la plus propre du monde.
    """
    if not (ROOT / ".git").exists():
        return None
    if subprocess.run(("git", "rev-parse", "--verify", "--quiet", "refs/remotes/origin/main"),
                      cwd=str(ROOT), capture_output=True, text=True).returncode != 0:
        return None
    main_ref = subprocess.run(("git", "rev-parse", "refs/remotes/origin/main"),
                              cwd=str(ROOT), capture_output=True, text=True).stdout.strip()
    merge = subprocess.run(("git", "merge-base", main_ref, "HEAD"),
                           cwd=str(ROOT), capture_output=True, text=True)
    if merge.returncode != 0:
        return None
    diff = subprocess.run(("git", "diff", "--name-only", "--diff-filter=ACMRT",
                           merge.stdout.strip(), "HEAD", "--", ".github/workflows"),
                          cwd=str(ROOT), capture_output=True, text=True)
    if diff.returncode != 0:
        return None
    return [line.strip() for line in diff.stdout.splitlines() if line.strip()]


def parse_yaml(path: Path):
    """Parse complet si js-yaml est installe (outillage local) ; sinon `None` = abstention assumee."""
    node = subprocess.run(("node", "--version"), capture_output=True, text=True)
    if node.returncode != 0 or not (ROOT / "node_modules" / "js-yaml").exists():
        return None
    probe = ("const y=require('js-yaml'),f=require('fs');"
             "try{y.load(f.readFileSync(process.argv[1],'utf8'));process.exit(0)}"
             "catch(e){console.log(e.message);process.exit(1)}")
    res = subprocess.run(["node", "-e", probe, str(path)], cwd=str(ROOT),
                         capture_output=True, text=True)
    return res


def check_yaml_shape(path: Path) -> None:
    if not path.is_file():
        return
    raw = body(path)
    check(f"{rel(path)} n'emploie aucune tabulation", "\t" not in raw,
          "une tabulation dans un bloc literal est rejetee par le parseur YAML")
    top = [line.split(":", 1)[0] for line in raw.splitlines() if re.match(r"^[a-zA-Z_][\w-]*:", line)]
    check(f"{rel(path)} déclare name, on et jobs",
          all(key in top for key in ("name", "on", "jobs")),
          f"absents : {[k for k in ('name', 'on', 'jobs') if k not in top]}")
    strays = [key for key in top if key not in TOP_KEYS]
    check(f"{rel(path)} n'a aucune clé top-level hors du squelette GitHub", not strays, f"{strays}")
    parsed = parse_yaml(path)
    if parsed is not None:
        check(f"{rel(path)} se parse (js-yaml)", parsed.returncode == 0,
              parsed.stdout.strip().splitlines()[:1])


def main() -> int:
    if not SOURCE.is_file():
        print(f"ERREUR: source introuvable : {rel(SOURCE)}", file=sys.stderr)
        return 1
    source = body(SOURCE)

    # 1. une seule source, un seul miroir
    check("docs/CI-A-COLLER.yml et scripts/ci/build-workflow.yml sont identiques",
          source == (body(MIRROR) if MIRROR.is_file() else None),
          f"{rel(MIRROR)} diverge de {rel(SOURCE)} : coller l'un ou l'autre ne donnerait pas le meme"
          " pipeline. Copier la source par-dessus le miroir.")

    # 2. la chaine de declenchement
    for path in (SOURCE, MIRROR):
        text = body(path)
        check(f"{rel(path)} se déclenche sur les pushes de `main`", not ignores_main(text),
              "`push: branches-ignore: [main]` casse toute la chaîne : le merge ne construit rien, la"
              " release n'est pas publiée, deploy-serveur.yml ne part pas, et `plugins/` ne reçoit aucun"
              " fichier — sans une seule erreur affichée. C'est le défaut qui a été corrigé ici.")
        check(f"{rel(path)} ne publie la release que depuis `main`",
              "Publier la release" in text and "refs/heads/main" in text,
              "sans cette porte, n'importe quelle branche en cours mettrait le serveur à jour")
        check(f"{rel(path)} délègue le contrôle des jar à un script",
              "ci-check-jars.sh" in text,
              "un `bash -c` de quarante lignes dans le YAML rend un code de sortie muet : on ne sait"
              " pas quelle assertion est tombée (run 33200967570)")

    jar_check = ROOT / "scripts/ci-check-jars.sh"
    check("scripts/ci-check-jars.sh existe, nomme et publie chaque contrôle",
          jar_check.is_file() and "::error" in body(jar_check),
          "sans annotation, le contrôle est inexploitable à distance : le journal brut passe par un"
          " stockage externe indisponible")

    # 3. le depot : trois jar, une sauvegarde, un refus explicite
    deploy = body(DEPLOY) if DEPLOY.is_file() else ""
    check(f"{rel(DEPLOY)} exige les trois jar",
          all(token in deploy for token in ("ValoriaTycoon-v", "ValoriaEconomy-v", "ValoriaTools-v")),
          "le dépôt doit vérifier la présence des trois jar AVANT d'ouvrir une session SFTP")
    check(f"{rel(DEPLOY)} sauvegarde avant d'écraser",
          "_sauvegarde" in deploy or "ci-release-and-deploy" in deploy,
          "sans sauvegarde horodatée, un jar défectueux ne se rattrape que dans le panel du serveur")
    script = ROOT / "scripts/ci-release-and-deploy.sh"
    if script.is_file():
        text = body(script)
        check("le script de dépôt connaît le jar de ValoriaTools", "TOOLS_JAR" in text,
              "sinon le serveur reçoit le plugin et son économie sans le multi-outil, en silence")
        check("le script de dépôt refuse un dépôt incomplet",
              re.search(r"TOOLS_JAR.*DEPLOY", text, re.S) is not None,
              "l'absence du troisième jar doit faire échouer l'envoi, pas le compléter en douceur")
        check("le script de dépôt vérifie la taille après envoi", "stat -c %s" in text,
              "sans contrôle octet pour octet, un tranfert coupé passe pour un succès")
    if NEUTRAL.is_file():
        check("le deploy.yml neutralisé n'a plus de déclencheur `push`",
              not re.search(r"^\s+push:\s*$\n\s+branches:", body(NEUTRAL), re.M),
              "un `push: main` resté là envoie UN SEUL jar sur le serveur à chaque merge")

    # 4. rien d'autre ne se fait passer pour un workflow
    for path in sorted(ROOT.rglob("*.yml")):
        if any(part in {".git", "target", "node_modules"} for part in path.parts):
            continue
        if path in CANONICAL or rel(path).startswith(".github/workflows/"):
            continue
        if looks_like_workflow(body(path)):
            check(f"aucun contenu de workflow recopié dans {rel(path)}", False,
                  "supprimer ce contenu et pointer docs/CI-A-COLLER.yml ou docs/CI-DEPLOY-A-COLLER.yml")
    for name in STUBS:
        path = ROOT / name
        if path.is_file() and looks_like_workflow(body(path)):
            check(f"{name} reste un pointeur, pas une copie", False,
                  "une copie complète ici se colle par erreur et diverge de la source")

    # 5. la branche ne touche pas .github/workflows (sinon conflit add/add avec main)
    touched = workflows_touched_by_this_branch()
    if touched is None:
        print("  [--] workflows de la branche : historique indisponible, contrôle ignoré"
              " (le build installe `fetch-depth: 0` pour l'obtenir)")
    else:
        check("cette branche n'ajoute et ne modifie aucun fichier de `.github/workflows`", not touched,
              f"{touched} : ces fichiers se collent sur `main` depuis docs/. Les versionner ici a déjà"
              " rendu la PR inconciliable (CONFLICTING) et le merge impossible en silence")

    # 6. un script supprime ne doit plus etre cite
    here = Path(__file__).resolve()
    for path in sorted(list(ROOT.glob("scripts/*")) + list(ROOT.glob("docs/*")) + list(ROOT.glob("*.md"))):
        if not path.is_file() or path.resolve() == here:
            continue
        if "ci-publish-release.sh" in body(path):
            check(f"{rel(path)} ne cite plus le script de publication supprimé", False,
                  "scripts/ci-publish-release.sh a été retiré (il ne publiait que deux jar) : le"
                  " remplacer par scripts/ci-release-and-deploy.sh")

    # 7. forme des fichiers a coller (c'est ce que l'humain colle, un YAML casse = rien ne part)
    for path in (SOURCE, MIRROR, DEPLOY, NEUTRAL):
        check_yaml_shape(path)

    if problems:
        print(f"\n{len(problems)} incohérence(s) de workflow — le dépôt serait muet ou le merge bloqué.",
              file=sys.stderr)
        return 1
    print(f"\nOK : les copies des workflows sont cohérentes ({len(notes)} vérifications).")
    return 0


def _self_tests() -> None:
    """Chaque regle ci-dessus doit tirer sur son cas fautif — une regle decorative a deja couté six runs."""
    import tempfile

    with tempfile.TemporaryDirectory() as tmp:
        folder = Path(tmp)
        (folder / "a.yml").write_text("name: X\non:\n  push:\njobs:\n  a:\n", encoding="utf-8")
        (folder / "b.yml").write_text("name: Y\njobs:\n  a:\n", encoding="utf-8")
        seen = sorted(p.name for p in folder.glob("*.yml") if looks_like_workflow(body(p)))
        if seen != ["a.yml"]:
            raise SystemExit(f"ERREUR: looks_like_workflow voit {seen} au lieu de ['a.yml'] —"
                             " la chasse aux copies recolpees serait aveugle ou faussement positive")
        dead = "on:\n  push:\n    branches-ignore:\n      - main\n  pull_request:\njobs:\n  a:\n"
        alive = "on:\n  push:\n  pull_request:\njobs:\n  a:\n"
        if not ignores_main(dead) or ignores_main(alive):
            raise SystemExit("ERREUR: ignores_main ne voit plus `branches-ignore: [main]` (ou le voit la"
                             " ou il n'est pas) — la regle qui a sauve la chaine de depot est decorative")
        print("  [--] auto-tests des regles : OK")


if __name__ == "__main__":
    _self_tests()
    sys.exit(main())
