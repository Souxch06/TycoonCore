#!/usr/bin/env python3
"""Installe (ou vérifie) le pont NBT -> PersistentDataContainer dans les classes livrées.

Le plugin identifie ses générateurs via la bibliothèque embarquée
``io.github.bananapuncher714.nbteditor.NBTEditor``, qui résout par réflexion des noms de classes et de
méthodes obfusqués. Ces noms n'existent plus sur les serveurs récents (CraftBukkit non relocaté depuis
1.20.6, plus de jar obfusqué ni de remapper interne depuis Paper 26.1) : sur Paper 26.x, la bibliothèque
ne lève aucune erreur mais n'écrit et ne lit plus rien, ce qui rend les générateurs inertes.

La correction est en deux temps :

1. ``NBTEditor`` (et toutes ses classes imbriquées) est **rebaptisé** ``LegacyNbtBridge`` dans le JAR
   livré : l'implémentation d'origine reste ainsi disponible comme repli (blocs, entités, données
   écrites avant le pont, serveurs sans PersistentDataContainer). Seules les constantes CONSTANT_Utf8
   sont réécrites (noms internes et descripteurs), jamais le bytecode ; les fichiers sont renommés en
   conséquence pour rester cohérents avec leur ``this_class``.
2. ``NBTEditor`` devient la classe compilée depuis ``sources/shaded/io/github/bananapuncher714/nbteditor/NBTEditor.java``
   (pont PDC, même signature binaire pour les 5 membres appelés par le plugin : le champ
   ``CUSTOM_DATA`` et les méthodes ``set``/``contains``/``getInt``/``getString``). Elle n'est pas dans
   ``artifacts/extracted`` : c'est le build qui la produit (voir <includes> du maven-compiler-plugin).

Références attendues et non modifiées : les classes du plugin continuent d'appeler
``io/github/bananapuncher714/nbteditor/NBTEditor`` et ``NBTEditor$Type`` (résolus vers le pont compilé).

Utilisation :
    python3 scripts/install-nbt-bridge.py            # applique si nécessaire
    python3 scripts/install-nbt-bridge.py --check    # contrôle (CI)
"""

from pathlib import Path
import argparse
import re
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))

import classfile  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
EXTRACTED = ROOT / "artifacts" / "extracted"
PACKAGE_DIR = EXTRACTED / "io" / "github" / "bananapuncher714" / "nbteditor"

# Le jeton complet est remplacé partout dans les seules classes legacy : il couvre à la fois les noms
# internes (« io/github/.../NBTEditor$ClassId »), les descripteurs (« Lio/github/.../NBTEditor$Type; »),
# les noms simples de l'attribut InnerClasses (« NBTEditor$ClassId ») et SourceFile (« NBTEditor.java »).
# La réécriture passe par classfile.replace_utf8, qui réémet les préfixes de longueur : un remplacement
# d'octets bruts corromprait le constant-pool.
TOKEN = "NBTEditor"
REPLACEMENT = "LegacyNbtBridge"

# Le plugin référence le pont par ces deux noms ; ils ne doivent surtout pas être renommés.
PONT_ALLOWED = {"io/github/bananapuncher714/nbteditor/NBTEditor",
                "io/github/bananapuncher714/nbteditor/NBTEditor$Type"}
CLASS_REF = re.compile(r"io/github/bananapuncher714/nbteditor/NBTEditor(?:\$[A-Za-z0-9_$]+)*")


def rename_class(data: bytes) -> bytes:
    patched, _changed = classfile.replace_utf8(data, {TOKEN.encode(): REPLACEMENT.encode()})
    return patched


def legacy_files() -> list:
    if not PACKAGE_DIR.is_dir():
        return []
    return sorted(p for p in PACKAGE_DIR.glob("NBTEditor*.class"))


def outside_references() -> list:
    """Classes hors du package legacy qui référencent une classe imbriquée de NBTEditor (inattendu)."""
    problems = []
    for path in sorted(EXTRACTED.rglob("*.class")):
        if PACKAGE_DIR in path.parents:
            continue
        try:
            values = set(classfile.utf8_values(path.read_bytes()))
        except (classfile.ClassFormatError, OSError):
            continue
        for value in values:
            if TOKEN not in value:
                continue
            # un descripteur (« L…NBTEditor$Type; ») ou une signature peuvent porter la référence :
            # on compare le nom de classe extrait, pas la chaîne entière.
            for match in CLASS_REF.findall(value):
                if match not in PONT_ALLOWED:
                    problems.append(f"{path.relative_to(EXTRACTED)}: référence legacy inattendue "
                                     f"{match!r} (dans {value!r})")
    return problems


def check_jar(jar_path: Path, problems: list):
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        bridge = "io/github/bananapuncher714/nbteditor/NBTEditor.class"
        legacy = "io/github/bananapuncher714/nbteditor/LegacyNbtBridge.class"
        if bridge not in names:
            problems.append(f"{jar_path.name}: pont {bridge} absent (le build ne l'a pas compilé ?)")
        if legacy not in names:
            problems.append(f"{jar_path.name}: repli {legacy} absent")
        stale = [n for n in names if n.startswith("io/github/bananapuncher714/nbteditor/NBTEditor$") and not n.endswith("NBTEditor$Type.class")]
        if stale:
            problems.append(f"{jar_path.name}: classes legacy non renommées : {sorted(stale)[:4]}")
        if bridge in names:
            blob = jar.read(bridge)
            values = set(classfile.utf8_values(blob))
            for member in ("contains", "getInt", "getString", "set", "CUSTOM_DATA"):
                if member not in values:
                    problems.append(f"{jar_path.name}: pont sans membre {member!r}")
            for token in (b"PersistentDataContainer", b"NamespacedKey", b"LegacyNbtBridge"):
                if token not in blob:
                    problems.append(f"{jar_path.name}: pont sans référence {token.decode()}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Installe le pont NBT -> PersistentDataContainer.")
    parser.add_argument("--check", action="store_true", help="contrôle sans écrire")
    parser.add_argument("--jar", help="JAR à contrôler en complément")
    args = parser.parse_args()

    if not PACKAGE_DIR.is_dir():
        print(f"ERREUR: {PACKAGE_DIR} introuvable", file=sys.stderr)
        return 1

    problems = []
    renamed = 0
    for path in legacy_files():
        relative = path.relative_to(PACKAGE_DIR).as_posix()
        target = PACKAGE_DIR / relative.replace("NBTEditor", "LegacyNbtBridge", 1)
        if args.check:
            problems.append(f"{relative}: l'ancien nom est encore présent (pont non installé)")
            continue
        data = path.read_bytes()
        patched = rename_class(data)
        try:
            classfile.walk(patched)
        except classfile.ClassFormatError as error:
            problems.append(f"{relative}: fichier invalide après renommage ({error})")
            continue
        target.write_bytes(patched)
        path.unlink()
        renamed += 1

    if not args.check:
        for path in legacy_files():
            problems.append(f"{path.name}: renaissance inattendue après renommage")

    problems.extend(outside_references())

    if args.jar:
        jar_path = Path(args.jar)
        if not jar_path.is_absolute():
            jar_path = ROOT / args.jar
        if not jar_path.is_file():
            print(f"ERREUR: JAR introuvable: {args.jar}", file=sys.stderr)
            return 1
        check_jar(jar_path, problems)

    if renamed:
        print(f"Classes legacy renommées : {renamed}")
    if problems:
        print(f"\n{len(problems)} problème(s) :", file=sys.stderr)
        for problem in problems[:20]:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    print("OK: pont NBT installé (LegacyNbtBridge en repli, NBTEditor fourni par la compilation).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
