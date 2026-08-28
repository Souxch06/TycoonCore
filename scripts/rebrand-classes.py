#!/usr/bin/env python3
"""Applique (ou vérifie) le renommage de marque dans les classes livrées du JAR.

Le dépôt renomme la marque d'origine (`GensPlus` -> `ValoriaTycoon`, paquet
`xyz.arcadiadevs.gensplus` -> `xyz.arcadiadevs.valoriatycoon`) au niveau des chemins de fichiers et
des ressources, comme le décrit `scripts/verify-extraction.py`. Les constantes des fichiers `.class`
de `artifacts/extracted/`, elles, n'avaient jamais été renommées : la classe principale était encore
déclarée sous le nom `xyz/arcadiadevs/gensplus/GensPlus` alors que le fichier est
`xyz/arcadiadevs/valoriatycoon/ValoriaTycoon.class` et que `plugin.yml` pointe sur
`xyz.arcadiadevs.valoriatycoon.ValoriaTycoon`.

Conséquence : tout JAR reconstruit depuis `artifacts/extracted/` est inutilisable, sur toutes les
versions de serveur (le classloader de Bukkit rejette une classe dont le `this_class` ne correspond
pas au nom demandé : `NoClassDefFoundError ... (wrong name: ...)`), et les appelants de
`ServerVersion` ne pouvaient de toute façon pas être reliés à une classe recompilée.

Le renommage est donc une condition préalable à toute compatibilité, y compris Paper 26.x.
Il porte uniquement sur les constantes CONSTANT_Utf8 du constant-pool (noms internes, descripteurs,
textes de commande), jamais sur le bytecode : c'est équivalent à une recompilation depuis des sources
déjà renommées, mais reproductible et vérifiable sans JDK.

Utilisation :
    python3 scripts/rebrand-classes.py                 # applique si nécessaire
    python3 scripts/rebrand-classes.py --check         # contrôle l'arbre du dépôt
    python3 scripts/rebrand-classes.py --check --jar target/ValoriaTycoon-v1.6.3.jar
"""

from pathlib import Path
import argparse
import zipfile
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))

import classfile  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
EXTRACTED = ROOT / "artifacts" / "extracted"

# Dans cet ordre : le nom de paquet complet d'abord, puis les occurrences isolées.
REPLACEMENTS = [
    (b"xyz/arcadiadevs/gensplus", b"xyz/arcadiadevs/valoriatycoon"),
    (b"xyz.arcadiadevs.gensplus", b"xyz.arcadiadevs.valoriatycoon"),
    (b"gensplus", b"valoriatycoon"),
    (b"GensPlus", b"ValoriaTycoon"),
]
RESIDUAL = (b"gensplus", b"GensPlus")


def needs_patch(data: bytes) -> bool:
    return any(token in data for token, _ in REPLACEMENTS)


def check_tree(path: Path, relative: str, data: bytes, problems: list):
    """Contrôle l'absence de résidu et la cohérence this_class / chemin du fichier."""
    for token in RESIDUAL:
        if token in data:
            problems.append(f"{relative}: constante résiduelle {token.decode()!r}")
            return
    try:
        this_class, _super, _entries, _by_index, _utf8 = classfile.class_names(data)
    except classfile.ClassFormatError as error:
        problems.append(f"{relative}: fichier illisible ({error})")
        return
    if this_class == "module-info":
        return
    expected = relative[: -len(".class")]
    if this_class != expected:
        problems.append(f"{relative}: this_class = {this_class!r}, attendu {expected!r}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true", help="contrôle sans écrire")
    parser.add_argument("--jar", help="JAR à contrôler en plus de l'arbre (chemin relatif au dépôt accepté)")
    args = parser.parse_args()

    if not EXTRACTED.is_dir():
        print(f"ERREUR: {EXTRACTED.relative_to(ROOT)} introuvable", file=sys.stderr)
        return 1

    problems = []
    modified = 0
    scanned = 0
    for path in sorted(EXTRACTED.rglob("*.class")):
        relative = path.relative_to(EXTRACTED).as_posix()
        data = path.read_bytes()
        scanned += 1
        if not needs_patch(data):
            check_tree(path, relative, data, problems)
            continue
        if args.check:
            problems.append(f"{relative}: renommage de marque non appliqué")
            continue
        patched, _changed = classfile.replace_utf8(data, dict(REPLACEMENTS))
        path.write_bytes(patched)
        modified += 1
        check_tree(path, relative, patched, problems)

    if args.jar:
        jar_path = Path(args.jar)
        if not jar_path.is_absolute():
            jar_path = ROOT / args.jar
        if not jar_path.is_file():
            print(f"ERREUR: JAR introuvable: {args.jar}", file=sys.stderr)
            return 1
        with zipfile.ZipFile(jar_path) as jar:
            for name in jar.namelist():
                if not name.endswith(".class"):
                    continue
                check_tree(jar_path, name, jar.read(name), problems)

    print(f"Classes contrôlées : {scanned}" + (f", corrigées : {modified}" if modified else ""))
    if problems:
        print(f"\n{len(problems)} problème(s) de renommage :", file=sys.stderr)
        for problem in problems[:25]:
            print(f"  - {problem}", file=sys.stderr)
        if len(problems) > 25:
            print(f"  ... +{len(problems) - 25} autres", file=sys.stderr)
        return 1
    print("OK: marques et noms internes cohérents (this_class == chemin, aucun résidu gensplus).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
