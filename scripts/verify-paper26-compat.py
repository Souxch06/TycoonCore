#!/usr/bin/env python3
"""Vérifie que le plugin est prêt pour Paper 26.x (versionnage calendaire Minecraft 26.1+).

Deux modes :

    python3 scripts/verify-paper26-compat.py
        Contrôle l'arbre du dépôt : classes vendorisées patchées dans artifacts/extracted et
        sources de référence cohérentes (sources/plugin + sources/shaded).

    python3 scripts/verify-paper26-compat.py target/ValoriaTycoon-v1.6.3.jar
        Contrôle en plus le JAR réellement produit par le build : la classe ServerVersion
        recompilée doit y avoir écrasé la classe livrée, et l'API Bukkit ne doit jamais être
        embarquée dans le JAR.

Sort avec le code 1 dès qu'un contrôle échoue (utilisé par le workflow de CI).
"""

from importlib.machinery import SourceFileLoader
from pathlib import Path
import re
import struct
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))

import classfile  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
EXTRACTED = ROOT / "artifacts" / "extracted"
# nom de fichier avec des tirets : chargement explicite pour réutiliser la liste des correctifs
patcher = SourceFileLoader(
    "patch_class_version_patterns", str(ROOT / "scripts" / "patch-class-version-patterns.py")
).load_module()
PATCHES = patcher.PATCHES

SERVER_VERSION_SOURCE = ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon/utils/ServerVersion.java"
SERVER_VERSION_ENTRY = "xyz/arcadiadevs/valoriatycoon/utils/ServerVersion.class"
XMATERIAL_SOURCE = ROOT / "sources/shaded/com/cryptomorin/xseries/XMaterial.java"
XREFLECTION_SOURCE = ROOT / "sources/shaded/com/cryptomorin/xseries/reflection/XReflection.java"

# Motifs tels qu'ils sont écrits dans le source Java (backslashes doublés), comparés en sous-chaîne.
XMATERIAL_PATTERN_NEW = r'"MC: (?:1\\.)?(\\d{1,2})"'
XMATERIAL_PATTERN_OLD = r'"MC: \\d\\.(\\d+)"'
XREFLECTION_PATTERN_NEW = r'"^(?:1\\.)?(?<minor>(?<major>\\d{1,2}))(?:\\.(?<patch>\\d+))?"'
XREFLECTION_PATTERN_OLD = r'"^(?<major>\\d+)\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?"'


results = []


def check(label, ok, detail=""):
    results.append((label, bool(ok), detail))


def utf8_set(data: bytes):
    entries, _ = classfile.parse_header(data)
    return {e[4].decode("utf-8", "replace") for e in entries if e[1] == classfile.UTF8}


def verify_constant_pool(label, data, expected, forbidden):
    if data is None:
        check(label, False, "contenu introuvable")
        return None
    try:
        values = utf8_set(data)
    except classfile.ClassFormatError as error:
        check(label, False, f"constant-pool illisible: {error}")
        return None
    missing = [pattern for pattern in expected if pattern not in values]
    stale = [pattern for pattern in forbidden if pattern in values]
    detail = ""
    if missing:
        detail = "motif attendu absent: " + ", ".join(repr(m) for m in missing)
    elif stale:
        detail = "ancien motif toujours présent: " + ", ".join(repr(s) for s in stale)
    check(label, not missing and not stale, detail)
    return values


def verify_tree():
    for relative, old, new in PATCHES:
        path = EXTRACTED / relative
        data = path.read_bytes() if path.is_file() else None
        verify_constant_pool(f"classe livrée patchée : {relative}", data, [new], [old])
        if data is None:
            continue
        try:
            classfile.walk(data)
            check(f"structure .class intacte : {relative}", True)
        except classfile.ClassFormatError as error:
            check(f"structure .class intacte : {relative}", False, str(error))

    xmaterial = XMATERIAL_SOURCE.read_text(encoding="utf-8")
    xreflection = XREFLECTION_SOURCE.read_text(encoding="utf-8")
    check("source shaded XMaterial cohérente", XMATERIAL_PATTERN_NEW in xmaterial,
          "motif calendaire introuvable dans sources/shaded/com/cryptomorin/xseries/XMaterial.java")
    check("source shaded XMaterial : ancien motif retiré", XMATERIAL_PATTERN_OLD not in xmaterial)
    check("source shaded XReflection cohérente", XREFLECTION_PATTERN_NEW in xreflection,
          "motif calendaire introuvable dans sources/shaded/com/cryptomorin/xseries/reflection/XReflection.java")
    check("source shaded XReflection : ancien motif retiré", XREFLECTION_PATTERN_OLD not in xreflection)

    source = SERVER_VERSION_SOURCE.read_text(encoding="utf-8")
    for token, label in [("V26_2", "constante V26_2 déclarée"),
                         ("V26_1", "constante V26_1 déclarée"),
                         ("getBukkitVersion", "lecture de Bukkit#getBukkitVersion"),
                         ("getMinecraftVersion", "lecture de Server#getMinecraftVersion (Paper)"),
                         ("fromVersionString", "resolveur exposé pour les tests")]:
        check(f"ServerVersion.java : {label}", token in source)
    check("ServerVersion.java : plus de dépendance au paquet CraftBukkit pour la détection",
          "startsWith(serverVersion.name())" not in source)


def verify_jar(jar_path: Path):
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        server_version = jar.read(SERVER_VERSION_ENTRY) if SERVER_VERSION_ENTRY in names else None
        patched = {relative: (jar.read(relative) if relative in names else None) for relative, _o, _n in PATCHES}
        plugin_yml = jar.read("plugin.yml").decode("utf-8") if "plugin.yml" in names else None
        manifest = jar.read("META-INF/MANIFEST.MF") if "META-INF/MANIFEST.MF" in names else None

    print(f"\nJAR analysé : {jar_path.name}")
    check("JAR : META-INF/MANIFEST.MF présent", manifest is not None)
    check("JAR : plugin.yml présent", plugin_yml is not None)
    if plugin_yml:
        api_ok = re.search(r"^api-version:\s*['\"]?(?:1\.1[3-9]|1\.[2-9]\d|[2-9]\d)", plugin_yml, re.M) is not None
        check("JAR : plugin.yml api-version >= 1.13", api_ok, "api-version absente ou antérieure à 1.13")
    check("JAR : API Bukkit non embarquée", not any(n.startswith("org/bukkit/") for n in names),
          "le JAR ne doit pas contenir org/bukkit (fourni par le serveur)")

    for relative, old, new in PATCHES:
        verify_constant_pool(f"JAR : classe patchée {relative}", patched[relative], [new], [old])

    if server_version is None:
        check("JAR : ServerVersion.class recompilée", False, f"{SERVER_VERSION_ENTRY} absent du JAR")
        return
    values = verify_constant_pool(
        "JAR : ServerVersion.class recompilée (détection calendaire)",
        server_version,
        {"getBukkitVersion", "getMinecraftVersion", "V26_2", "V26_1"},
        set(),
    )
    if values is not None:
        # l'ancienne classe se reconnaissait à son heuristic startsWith(toUpperCase()) sur le paquet
        check("JAR : ancien heuristic par paquet CraftBukkit retiré",
              not {"toUpperCase", "startsWith"} & values,
              "ServerVersion.class semble être l'ancienne version")
        major = struct.unpack(">H", server_version[6:8])[0]
        check("JAR : ServerVersion.class en bytecode 17 (major 61)", major == 61, f"major = {major}")


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("-")]
    verify_tree()
    if args:
        jar_path = Path(args[0])
        if not jar_path.is_absolute():
            jar_path = ROOT / args[0]
        if not jar_path.is_file():
            print(f"ERREUR: JAR introuvable: {args[0]}", file=sys.stderr)
            return 1
        verify_jar(jar_path)

    width = max(len(label) for label, _ok, _d in results)
    failures = 0
    for label, ok, detail in results:
        line = f"  [{'OK  ' if ok else 'KO  '}] {label.ljust(width)}"
        if not ok and detail:
            line += f"  <- {detail}"
        print(line)
        failures += 0 if ok else 1
    print(f"\nContrôles: {len(results)}, en échec: {failures}")
    if failures:
        print("ÉCHEC: le plugin n'est pas prêt pour Paper 26.x.", file=sys.stderr)
        return 1
    print("OK: support Paper 26.x (versionnage calendaire) vérifié.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
