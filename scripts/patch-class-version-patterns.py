#!/usr/bin/env python3
"""Applique (ou vérifie) les correctifs de détection de version Paper 26.x dans les classes
vendorisées du JAR.

Contexte : `artifacts/extracted/` est le contenu exact du JAR distribué, `sources/` n'étant que
la référence relisible (le build ne recompile pas les bibliothèques embarquées). Les deux classes
XSeries ci-dessous sont des classes compilées tierces : le correctif porte donc uniquement sur une
constante CONSTANT_Utf8 (le regex de parsing de version) du constant-pool, aucun octet de bytecode
n'est modifié. Le remplacement est reproduit ici de façon déterministe et auditable.

  com/cryptomorin/xseries/XMaterial$Data.class
      "MC: \\d\\.(\\d+)"  ->  "MC: (?:1\\.)?(\\d{1,2})"
      Sans ce correctif, Bukkit.getVersion() = "... (MC: 26.2)" ne matche pas (un seul chiffre
      autorisé avant le point) : XMaterial$Data.<clinit> lève IllegalArgumentException, donc
      ExceptionInInitializerError, et le plugin ne se charge pas sur les versions calendaires.

  com/cryptomorin/xseries/reflection/XReflection.class
      "^(?<major>\\d+)\\.(?<minor>\\d+)..."  ->  "^(?:1\\.)?(?<minor>(?<major>\\d{1,2}))..."
      XReflection modélise la version par sa MINEURE (supports(17) == MINOR_NUMBER >= 17) : sur
      "26.2.build.112-stable" il lisait mineure=2, d'où supports(17)==false, un NMS_PACKAGE invalide
      ("net.minecraft.server.null") et RuntimeException("Unknown Minecraft mapping") dans <clinit>.
      Le nouveau motif saute le préfixe "1." lorsqu'il est présent : MINOR_NUMBER et PATCH_NUMBER
      restent identiques sur toutes les versions 1.x et valent 26 sur les versions calendaires.

Utilisation :
    python3 scripts/patch-class-version-patterns.py            # applique si nécessaire
    python3 scripts/patch-class-version-patterns.py --check    # échoue si non appliqué
"""

from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))

import classfile  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]

# (chemin relatif à artifacts/extracted, ancienne constante Utf8, nouvelle constante Utf8)
PATCHES = [
    (
        "com/cryptomorin/xseries/XMaterial$Data.class",
        r"MC: \d\.(\d+)",
        r"MC: (?:1\.)?(\d{1,2})",
    ),
    (
        "com/cryptomorin/xseries/reflection/XReflection.class",
        r"^(?<major>\d+)\.(?<minor>\d+)(?:\.(?<patch>\d+))?",
        r"^(?:1\.)?(?<minor>(?<major>\d{1,2}))(?:\.(?<patch>\d+))?",
    ),
]


def patch(data: bytes, old: str, new: str) -> bytes:
    """Remplace une constante CONSTANT_Utf8 puis revalide intégralement le fichier reconstruit."""
    old_bytes = old.encode("utf-8")
    new_bytes = new.encode("utf-8")
    entries, _header = classfile.parse_header(data)
    utf8_entries = [e for e in entries if e[1] == classfile.UTF8 and e[4]]
    exact = [e for e in utf8_entries if e[4] == old_bytes]
    if not exact:
        if any(e[4] == new_bytes for e in utf8_entries):
            return data  # déjà corrigé
        raise ValueError(f"constante {old!r} introuvable (et {new!r} absente aussi)")
    if len(exact) != 1:
        raise ValueError(f"constante {old!r} présente {len(exact)} fois, remplacement ambigu")
    # le motif ne doit apparaître nulle part ailleurs, même en sous-chaîne
    partial = [e for e in utf8_entries if old_bytes in e[4] and e[4] != old_bytes]
    if partial:
        raise ValueError(f"{old!r} apparaît aussi dans {len(partial)} autre(s) constante(s), remplacement risqué")

    patched, changed = classfile.replace_utf8(data, {old_bytes: new_bytes}, expect_changes=1)
    values = classfile.utf8_values(patched)
    if new not in values:
        raise ValueError("constante absente après réécriture")
    if old in values:
        raise ValueError("ancienne constante encore présente après réécriture")
    if len(patched) - len(data) != len(new_bytes) - len(old_bytes):
        raise ValueError("taille inattendue après réécriture")
    return patched


def main() -> int:
    check = "--check" in sys.argv[1:]
    failed = False
    for relative, old, new in PATCHES:
        path = ROOT / "artifacts" / "extracted" / relative
        if not path.is_file():
            print(f"ERREUR: fichier manquant {path.relative_to(ROOT)}", file=sys.stderr)
            failed = True
            continue
        data = path.read_bytes()
        try:
            patched = patch(data, old, new)
        except (ValueError, classfile.ClassFormatError) as error:
            print(f"ERREUR {relative}: {error}", file=sys.stderr)
            failed = True
            continue
        if patched == data:
            print(f"déjà corrigé : {relative}")
            continue
        if check:
            print(f"NON CORRIGÉ : {relative} (constant-pool à patcher)", file=sys.stderr)
            failed = True
            continue
        path.write_bytes(patched)
        print(f"corrigé       : {relative} ({len(patched) - len(data):+d} octet(s))")
    if failed:
        return 1
    print("\nOK: détection de version calendaire (Paper 26.x) appliquée dans les classes vendorisées.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
