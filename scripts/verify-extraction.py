#!/usr/bin/env python3
"""Vérifie que artifacts/extracted contient tous les chemins de fichiers attendus après renommage."""

from pathlib import Path
from zipfile import ZipFile
import sys

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "artifacts" / "original" / "ValoriaTycoon-v1.6.3.jar"
EXTRACTED = ROOT / "artifacts" / "extracted"

if not JAR.is_file():
    print(f"JAR introuvable: {JAR}", file=sys.stderr)
    sys.exit(1)

if not EXTRACTED.is_dir():
    print(f"Dossier d'extraction introuvable: {EXTRACTED}", file=sys.stderr)
    sys.exit(1)

OLD_BRAND = "Gens" + "Plus"
OLD_PACKAGE = "gens" + "plus"
NEW_BRAND = "ValoriaTycoon"
NEW_PACKAGE = "valoriatycoon"

# Le pont NBT (scripts/install-nbt-bridge.py) conserve l'implémentation d'origine sous un nouveau nom
# binaire pour servir de repli ; NBTEditor est désormais fourni par la compilation depuis sources/.
NBT_PACKAGE = "io/github/bananapuncher714/nbteditor/"
NBT_ORIGINAL = NBT_PACKAGE + "NBTEditor"
NBT_REPLI = NBT_PACKAGE + "LegacyNbtBridge"


def rebranded_path(name: str) -> str:
    """Applique les renommages du dépôt aux chemins historiques du JAR."""
    return (
        name.replace(f"xyz/arcadiadevs/{OLD_PACKAGE}/", f"xyz/arcadiadevs/{NEW_PACKAGE}/")
        .replace(NBT_ORIGINAL, NBT_REPLI)
        .replace(OLD_BRAND, NEW_BRAND)
        .replace(OLD_PACKAGE, NEW_PACKAGE)
    )


with ZipFile(JAR) as jar:
    jar_files = sorted(rebranded_path(name) for name in jar.namelist() if not name.endswith("/"))

extracted_files = sorted(
    path.relative_to(EXTRACTED).as_posix()
    for path in EXTRACTED.rglob("*")
    if path.is_file()
)

missing = sorted(set(jar_files) - set(extracted_files))
extra = sorted(set(extracted_files) - set(jar_files))

print(f"Fichiers attendus          : {len(jar_files)}")
print(f"Fichiers dans l'extraction : {len(extracted_files)}")
print(f"Fichiers manquants         : {len(missing)}")
print(f"Fichiers en trop           : {len(extra)}")

if missing:
    print("\nManquants:")
    for name in missing[:50]:
        print(f"- {name}")
    if len(missing) > 50:
        print(f"... +{len(missing) - 50} autres")

if extra:
    print("\nEn trop:")
    for name in extra[:50]:
        print(f"- {name}")
    if len(extra) > 50:
        print(f"... +{len(extra) - 50} autres")

if missing or extra:
    sys.exit(1)

print("\nOK: artifacts/extracted contient tous les fichiers attendus du JAR.")
