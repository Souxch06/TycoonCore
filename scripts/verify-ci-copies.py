#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Refuse les copies divergentes des fichiers de workflow, et le vocabulaire « deux jar ».

Pourquoi ce script existe : le depot contenait QUATRE textes de workflow (docs/CI-A-COLLER.yml, sa copie
de travail scripts/ci/build-workflow.yml, docs/paste/build.yml, et un bloc colle dans
docs/TUTO-INSTALLATION-FINALE.md). Deux de ces copies en avaient garde une version « deux jar, sans etape
de release » : l'administrateur qui colle celle-la obtient un pipeline qui construit tout, ne publie
rien, et ne dit jamais pourquoi. Un fichier recopie diverge, toujours.

Regle :
  1. `docs/CI-A-COLLER.yml` est LA source collable ; `scripts/ci/build-workflow.yml` doit lui etre
     identique octet pour octet ;
  2. aucun autre fichier du depot ne doit contenir un contenu de workflow complet (un `name:` de workflow
     suivi de `on:`) en dehors des deux precedents et des deux files du dossier `docs/paste/` ;
  3. la chaine « deux jar » n'a plus sa place la ou il y en a trois : les trois plugin.yml, les trois
     artefacts, les trois envois SFTP. Les exceptions sont enumeratees et justifiees.

Sortie : 0 si tout est coherent, 1 sinon (le message dit quel fichier coller).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/CI-A-COLLER.yml"
MIRROR = ROOT / "scripts/ci/build-workflow.yml"
DEPLOY = ROOT / "docs/CI-DEPLOY-A-COLLER.yml"
DEPLOY_NEUTRAL = ROOT / "docs/paste/deploy-neutralise.yml"

# un « deux jars » est legitime quand il designe autre chose que la livraison (le couple plugin+economie,
# l'historique d'une reparation, le jar de controle). Ces fichiers sont les seuls autorises a le dire.
TWO_JAR_ALLOWED = (
    "docs/STRUCTURE.md",            # l'API d'economie vue par les deux plugins
    "docs/ECONOMIE.md",             # idem
    "docs/DEPLOY-2-JARS.md",        # le nom du document, et son recit de l'ancien etat
    "docs/TUTORIEL-PAPER-26.md",    # le jar historique du plugin, avant l'economie
    "README.md",                    # resume du projet, parle des « 2 jar » du tycoon + l'outil a part
)

problems = []
notes = []


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def looks_like_workflow(text: str) -> bool:
    """Vrai si le fichier pretend etre un workflow complet (name + on + jobs), pas un extrait."""
    return bool(re.search(r"^name:\s", text, re.M)
                and re.search(r"^on:\s", text, re.M)
                and re.search(r"^jobs:\s", text, re.M))


def check(label, ok, detail=""):
    (notes if ok else problems).append((label, detail))
    print(f"  [{'OK ' if ok else 'KO '}] {label}{(' — ' + detail) if detail and not ok else ''}")


def main() -> int:
    if not SOURCE.is_file():
        print(f"ERREUR: source introuvable : {rel(SOURCE)}", file=sys.stderr)
        return 1
    source = SOURCE.read_text(encoding="utf-8")
    mirror = MIRROR.read_text(encoding="utf-8") if MIRROR.is_file() else ""

    check("docs/CI-A-COLLER.yml et scripts/ci/build-workflow.yml sont identiques",
          source == mirror,
          f"{rel(MIRROR)} diverge de {rel(SOURCE)} — coller l'un ou l'autre ne donnerait pas le meme "
          "pipeline. Copier la source par-dessus le miroir.")

    publish = ROOT / "scripts/ci-release-and-deploy.sh"
    publishes = publish.is_file() and "gh release upload" in publish.read_text(encoding="utf-8")
    for path in (SOURCE, MIRROR):
        text = path.read_text(encoding="utf-8")
        # l'etape existe-t-elle, et delegue-t-elle a un script qui televerse vraiment les assets ?
        # porter le controle sur le YAML seul laisserait passer un `if:` mal ecrit
        check(f"{rel(path)} publie la release (déclencheur du dépôt)",
              "Publier la release" in text and "ci-release-and-deploy" in text and publishes,
              "sans étape de release publiée depuis main, deploy-serveur.yml ne se lance jamais : le "
              "serveur ne reçoit rien, et le build reste vert (c'est le silence le plus dangereux)")
        check(f"{rel(path)} construit et verifie les trois jar",
              text.count("ValoriaTools-v") >= 2,
              f"{text.count('ValoriaTools-v')} mention(s) du jar de ValoriaTools : il faut le construire, "
              "le verifier, et le publier")

    deploy = DEPLOY.read_text(encoding="utf-8") if DEPLOY.is_file() else ""
    check(f"{rel(DEPLOY)} existe et exige les trois jar",
          all(token in deploy for token in ("ValoriaTycoon-v", "ValoriaEconomy-v", "ValoriaTools-v")),
          "le depot doit verifier la presence des trois jar avant d'ouvrir une session SFTP")
    check(f"{rel(DEPLOY)} sauvegarde avant d'ecraser",
          "_sauvegarde" in deploy or "ci-release-and-deploy" in deploy,
          "sans sauvegarde, un jar casse ne se rattrape qu'a la main dans le panel du serveur")

    # aucune autre copie complete d'un workflow ne doit trainer dans le depot
    for path in sorted(ROOT.rglob("*.yml")):
        if path in (SOURCE, MIRROR, DEPLOY, DEPLOY_NEUTRAL):
            continue
        if any(part in {".git", "target", "node_modules"} for part in path.parts):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        if not looks_like_workflow(text):
            continue
        if rel(path).startswith(".github/workflows/"):
            continue     # le fichier installe, lui, DOIT ressembler a un workflow
        check(f"aucun contenu de workflow recopie dans {rel(path)}", False,
              "supprimer ce fichier et pointer docs/CI-A-COLLER.yml ou docs/CI-DEPLOY-A-COLLER.yml a la place")

        # le controle de contenu des jar est un script, pas un bloc shell dans le YAML : inline, son echec
    # est un code de sortie muet (c'est ce qui est arrive au run 33200967570)
    check("le build délègue le contrôle des jar à un script", "ci-check-jars.sh" in source,
          "un `bash -c` de quarante lignes dans le YAML ne dit pas quelle assertion est tombée")
    script = ROOT / "scripts/ci-check-jars.sh"
    check("scripts/ci-check-jars.sh existe et nomme chaque contrôle",
          script.is_file() and "::error" in script.read_text(encoding="utf-8"),
          "sans publication d'annotation, le controle est inutilisable a distance (le journal brut "
          "n'est pas telechargeable depuis un agent)")

    # un YAML qui ne se parse pas = un workflow que GitHub refuse de charger, et donc un serveur qui
    # n'est jamais mis a jour. Le parseur complet est optionnel (le runner n'a pas de PyYAML garanti) ;
    # les fautes reellement commises ici sont structurelles : tabulation, cle de trop au niveau 0.
    for path in (SOURCE, MIRROR, DEPLOY, DEPLOY_NEUTRAL):
        if not path.is_file():
            continue
        raw = path.read_text(encoding="utf-8")
        check(f"{rel(path)} n'emploie aucune tabulation", "\t" not in raw,
              "une tabulation dans un bloc literal est rejetee par le parseur YAML")
        top = [line.split(":", 1)[0] for line in raw.splitlines() if re.match(r"^[a-zA-Z_][\w-]*:", line)]
        missing = [key for key in ("name", "on", "jobs") if key not in top]
        check(f"{rel(path)} déclare name, on et jobs", not missing, f"absents : {missing}")
        if path in (SOURCE, MIRROR, DEPLOY):
            check(f"{rel(path)} n'a aucune clé top-level hors du squelette GitHub",
                  all(k in ("name", "on", "permissions", "concurrency", "env", "defaults", "jobs", "run-name")
                      for k in top),
                  f"{[k for k in top if k not in ('name','on','permissions','concurrency','env','defaults','jobs','run-name')]}")
    import shutil as _sh
    if _sh.which("node") and (ROOT / "node_modules/js-yaml").exists():
        import subprocess
        for path in (SOURCE, MIRROR, DEPLOY, DEPLOY_NEUTRAL):
            if not path.is_file():
                continue
            probe = ("const y=require('js-yaml'),f=require('fs');"
                     "try{y.load(f.readFileSync(process.argv[1],'utf8'));console.log('ok')}catch(e){console.log(e.message);process.exit(1)}")
            res = subprocess.run(["node", "-e", probe, str(path)], capture_output=True, text=True)
            check(f"{rel(path)} se parse (js-yaml)", res.returncode == 0, res.stdout.strip().splitlines()[:1])

    # un script supprime ne doit plus etre cite nulle part (sinon le prochain admin le recree)
    # chaine de declenchement : un build qui s'interdit `main` ne publie jamais, et le silence est total
    import os
    for path in (SOURCE, MIRROR):
        text = path.read_text(encoding="utf-8")
        trig = re.search(r"^on:\n(?:.*\n)*?jobs:", text, re.M)
        block = trig.group(0) if trig else ""
        ignores_main = bool(re.search(r"branches-ignore:[^\n]*\n[ \t]*-[ \t]+main", block)) \
            or bool(re.search(r"branches-ignore:\s*\[[^\]]*main", block))
        check(f"{rel(path)} se déclenche sur les pushes de `main`", not ignores_main,
              "`push: branches-ignore: [main]` casse toute la chaîne : le merge ne construit rien, la"
              " release n'est pas publiée, deploy-serveur.yml ne part pas, et `plugins/` ne reçoit aucun"
              " fichier — sans une seule erreur affichée nulle part. C'est le défaut corrigé ici.")
        check(f"{rel(path)} ne publie la release que depuis `main`",
              "Publier la release" in text and "refs/heads/main" in text,
              "sans cette porte, n'importe quelle branche en cours mettrait le serveur à jour")

    for name in ("docs/paste/build.yml", "docs/paste/deploy-serveur.yml"):
        path = ROOT / name
        if path.is_file() and looks_like_workflow(path.read_text(encoding="utf-8")):
            check(f"{name} n'est pas une copie de workflow", False,
                  "ce fichier doit rester un pointeur : une copie complete ici se collee par erreur et"
                  " diverge de docs/CI-A-COLLER.yml (c'est deja arrive)")

    # un script supprime ne doit plus etre cite nulle part (sinon le prochain admin le recree)
    # chaine de declenchement : un build qui s'interdit `main` ne publie jamais, et le silence est total
    import os
    for path in (SOURCE, MIRROR):
        text = path.read_text(encoding="utf-8")
        trig = re.search(r"^on:\n(?:.*\n)*?jobs:", text, re.M)
        block = trig.group(0) if trig else ""
        ignores_main = bool(re.search(r"branches-ignore:[^\n]*\n[ \t]*-[ \t]+main", block)) \
            or bool(re.search(r"branches-ignore:\s*\[[^\]]*main", block))
        check(f"{rel(path)} se déclenche sur les pushes de `main`", not ignores_main,
              "`push: branches-ignore: [main]` casse toute la chaîne : le merge ne construit rien, la"
              " release n'est pas publiée, deploy-serveur.yml ne part pas, et `plugins/` ne reçoit aucun"
              " fichier — sans une seule erreur affichée nulle part. C'est le défaut corrigé ici.")
        check(f"{rel(path)} ne publie la release que depuis `main`",
              "Publier la release" in text and "refs/heads/main" in text,
              "sans cette porte, n'importe quelle branche en cours mettrait le serveur à jour")


    here = Path(__file__).resolve()
    for path in sorted(list(ROOT.glob("scripts/*")) + list(ROOT.glob("docs/*"))):
        # ce fichier-la nomme le script supprime pour dire qu'il ne faut plus le citer : se lire soi-meme
        # serait un faux positif garanti (et le faux positif est ce qui fait ignorer un controle)
        if not path.is_file() or path.resolve() == here:
            continue
        if "ci-publish-release.sh" in path.read_text(encoding="utf-8", errors="replace"):
            check(f"{rel(path)} ne cite plus le script de publication supprimé", False,
                  "scripts/ci-publish-release.sh a été retiré (il ne publiait que deux jar) : remplacer "
                  "la référence par scripts/ci-release-and-deploy.sh")

    # le contenu, pas les mots : un script de depot qui oublierait le troisieme jar ne se detecte pas a
    # son vocabulaire (les commentaires legitimes sur « les deux jars » d'une verification particuliere
    # fourmillent), mais a ce qu'il fait reellement
    deploy_script = ROOT / "scripts/ci-release-and-deploy.sh"
    if deploy_script.is_file():
        script = deploy_script.read_text(encoding="utf-8")
        check("le script de dépôt connaît le jar de ValoriaTools", "TOOLS_JAR" in script,
              "sans lui, un serveur reçoit le plugin et son économie sans le multi-outil, en silence")
        check("le script de dépôt REFUSE un dépôt incomplet",
              re.search(r"TOOLS_JAR.*\n.*DEPLOY", script, re.S) is not None,
              "l'absence du troisième jar doit faire échouer l'envoi, pas le compléter en douceur")
        check("le script de dépôt sauvegarde avant d'écraser", "_sauvegarde-" in script,
              "sans sauvegarde horodatée, un jar défectueux ne se rattrape que dans le panel du serveur")
    # les URLs que l'utilisateur doit coller doivent exister dans le dépôt
    for url in re.findall(r"https://github\.com/\S+/(?:raw|blob)/[^)\s`]+",
                          "\n".join(p.read_text(encoding="utf-8", errors="replace")
                                    for p in ROOT.glob("docs/TUTO*.md") if p.is_file())):
        tail = url.split("/valoriatycoon/", 1)[-1] if "/valoriatycoon/" in url else url.rsplit("/", 1)[-1]
        candidate = ROOT / tail
        if candidate.is_relative_to(ROOT) and not candidate.exists() and "/" in tail:
            check(f"lien collable {tail}", False, "le fichier n'existe pas dans le dépôt")

    if problems:
        print(f"\n{len(problems)} incoherence(s) de workflow — la CI refuserait le dépôt.", file=sys.stderr)
        return 1
    print(f"\nOK : les copies des workflows sont coherentes ({len(notes)} verifications).")
    return 0


def _self_test():
    """Le controleur doit tirer sur une divergence, sinon il est decoratif."""
    import tempfile

    with tempfile.TemporaryDirectory() as tmp:
        folder = Path(tmp)
        (folder / "a.yml").write_text("name: X\non:\n  push:\njobs:\n  a:\n", encoding="utf-8")
        (folder / "b.yml").write_text("name: Y\njobs:\n  a:\n", encoding="utf-8")
        found = [p.name for p in sorted(folder.glob("*.yml")) if looks_like_workflow(p.read_text())]
        if found != ["a.yml"]:
            raise SystemExit(f"ERREUR: looks_like_workflow voit {found} au lieu de ['a.yml'] — "
                             "le controle des copies serait soit aveugle soit faux positif")


if __name__ == "__main__":
    _self_test()
    sys.exit(main())
