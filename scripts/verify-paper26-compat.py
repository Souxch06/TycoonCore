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
UPGRADE_GUI_SOURCE = ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon/guis/UpgradeGui.java"
REFERENCE_JAR = ROOT / "artifacts/reference/valoria-renamed.jar"
SERVER_VERSION_ENTRY = "xyz/arcadiadevs/valoriatycoon/utils/ServerVersion.class"
XMATERIAL_SOURCE = ROOT / "sources/shaded/com/cryptomorin/xseries/XMaterial.java"
BRIDGE_SOURCE = ROOT / "sources/shaded/io/github/bananapuncher714/nbteditor/NBTEditor.java"
NBT_DIR = EXTRACTED / "io" / "github" / "bananapuncher714" / "nbteditor"
XREFLECTION_SOURCE = ROOT / "sources/shaded/com/cryptomorin/xseries/reflection/XReflection.java"

# Motifs tels qu'ils sont écrits dans le source Java (backslashes doublés), comparés en sous-chaîne.
XMATERIAL_PATTERN_NEW = r'"MC: (?:1\\.)?(\\d{1,2})"'
XMATERIAL_PATTERN_OLD = r'"MC: \\d\\.(\\d+)"'
XREFLECTION_PATTERN_NEW = r'"^(?:1\\.)?(?<minor>(?<major>\\d{1,2}))(?:\\.(?<patch>\\d+))?"'
XREFLECTION_PATTERN_OLD = r'"^(?<major>\\d+)\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?"'


results = []


def check(label, ok, detail=""):
    results.append((label, bool(ok), detail))


def top_list(text: str, key: str):
    """Éléments d'une liste de premier niveau d'un YAML (lignes «  - x »), commentaires ignorés."""
    items = []
    inside = False
    for line in (text or "").splitlines():
        if not line.startswith(" "):
            inside = line.strip().rstrip(":") == key
            continue
        if not inside:
            continue
        stripped = line.strip()
        if stripped.startswith("#") or not stripped:
            continue
        if stripped.startswith("- "):
            items.append(stripped[2:].strip().strip(chr(39) + chr(34)))
    return items


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

    # 4b. interface d'amélioration : un bouton d'action + une case statistiques, placeholders complets
    gui = UPGRADE_GUI_SOURCE.read_text(encoding="utf-8") if UPGRADE_GUI_SOURCE.is_file() else ""
    check("UpgradeGui.java : case statistiques configurée", "guis.upgrade-gui.stats.lore" in gui)
    check("UpgradeGui.java : %upgradePrice% substitué pour les deux cases",
          '"%upgradePrice%"' in gui and "private static List<String> fill(" in gui)
    check("UpgradeGui.java : plus de double bouton d'amélioration",
          "GUIS_UPGRADE_GUI_UPGRADE_ALL_FIRST_LINE" not in gui
          and "ORANGE_STAINED_GLASS_PANE" not in gui)
    check("UpgradeGui.java : un seul GuiItem cliquable (case 11)",
          gui.count("player.closeInventory()") == 1)
    for cfg_name in ("resources/config.yml", "artifacts/extracted/config.yml"):
        cfg = (ROOT / cfg_name).read_text(encoding="utf-8")
        check(f"{cfg_name} : bloc stats de l'interface d'upgrade",
              "stats:" in cfg and "Prochaine amélioration : &a%upgradePrice%" in cfg)

    # 4c. classpath de compilation nécessaire à ces fichiers
    if REFERENCE_JAR.is_file():
        with zipfile.ZipFile(REFERENCE_JAR) as ref:
            names = set(ref.namelist())
        needed = ["xyz/arcadiadevs/valoriatycoon/guis/UpgradeGui.class",
                  "xyz/arcadiadevs/valoriatycoon/ValoriaTycoon.class",
                  "xyz/arcadiadevs/valoriatycoon/utils/ServerVersion.class",
                  "io/github/bananapuncher714/nbteditor/LegacyNbtBridge.class",
                  "xyz/arcadiadevs/valoriatycoon/utils/config/message/Messages.class"]
        missing = [n for n in needed if n not in names]
        check("artifacts/reference : classes de résolution présentes", not missing, f"manquants: {missing}")
        check("artifacts/reference : pas de duplicata d'API serveur",
              not any(n.startswith("org/bukkit/") for n in names))
        check("artifacts/reference : sans manifest résiduel", "META-INF/MANIFEST.MF" not in names)
        # le pont est produit par la compilation : il ne doit pas traîner dans le classpath de
        # référence, sinon la vieille version masquerait la nouvelle dans target/classes
        check("artifacts/reference : le pont NBT n'y est pas (fourni par javac)",
              "io/github/bananapuncher714/nbteditor/NBTEditor.class" not in names)
    else:
        check("artifacts/reference/valoria-renamed.jar généré", False,
              "lancer scripts/build-reference-jar.py : sans ce classpath, UpgradeGui.java ne peut pas compiler")
    check("pom.xml : le JAR de référence est branché au build",
          "valoria-renamed.jar" in (ROOT / "pom.xml").read_text(encoding="utf-8"))

    # 4. invariant de build : une racine de compilation propre (sinon javac passe en mode module)
    root_marker = ROOT / "sources" / "module-info.java"
    check("racine de compilation sans module-info.java", not root_marker.exists(),
          "sources/module-info.java ferait basculer javac en mode module (« module not found: com.google.gson ») ; "
          "déplacer ce résidu sous sources/shaded/com/google/gson/")
    pom = (ROOT / "pom.xml").read_text()
    tree_yml = (ROOT / "resources" / "plugin.yml").read_text()
    depends = top_list(tree_yml, "depend")
    soft = top_list(tree_yml, "softdepend")
    check("plugin.yml : aucune dépendance dure (Vault, VaultUnlocked, ProtocolLib en souple)",
          not depends and {"Vault", "VaultUnlocked", "ProtocolLib"} <= set(soft),
          f"depend={depends} softdepend={soft} — un depend: dur est résolu par NOM exact par Bukkit : "
          "il refuse les équivalents (VaultUnlocked) et bloque tout le chargement")
    check("plugin.yml : miroir artifacts/extracted identique",
          (EXTRACTED / "plugin.yml").read_text() == tree_yml)
    check("pom.xml : descripteurs JPMS hors de portée de javac pendant compile",
          "<exclude>module-info.class</exclude>" in pom and "restore-module-descriptors" in pom,
          "il faut exclure module-info.class de la copie de ressources ET le réinsérer en prepare-package, "
          "sinon javac traite target/classes comme un module et échoue sur « module not found: com.google.gson »")
    check("pom.xml : compilation limitée aux sources maintenues",
          "<includes>" in pom and "ServerVersion.java" in pom and "nbteditor/NBTEditor.java" in pom,
          "les <includes> du maven-compiler-plugin doivent lister les fichiers maintenus")

    # 4. pont NBT -> PersistentDataContainer (sans lui, les générateurs sont inertes sur 26.x)
    bridge = BRIDGE_SOURCE.read_text(encoding="utf-8") if BRIDGE_SOURCE.is_file() else ""
    check("pont NBTEditor.java présent", bool(bridge), f"attendu dans {BRIDGE_SOURCE.relative_to(ROOT)}")
    if bridge:
        for label, pattern in [("contains", r"public static boolean contains\(Object object, Object \.\.\. objectArray\)"),
                               ("getInt", r"public static int getInt\(Object object, Object \.\.\. objectArray\)"),
                               ("getString", r"public static String getString\(Object object, Object \.\.\. objectArray\)"),
                               ("set", r"public static Object set\(Object object, Object object2, Object \.\.\. objectArray\)"),
                               ("champ CUSTOM_DATA typé NBTEditor$Type", r"public static final Type CUSTOM_DATA"),
                               ("enum imbriqué Type", r"public enum Type")]:
            check(f"pont NBTEditor : {label}", re.search(pattern, bridge) is not None)
        check("pont NBTEditor : aucun import Bukkit (build hors-ligne)",
              not re.search(r"^import org\.bukkit", bridge, re.M))
        check("pont NBTEditor : repli vers LegacyNbtBridge", "LegacyNbtBridge" in bridge)
    for relative in sorted(NBT_DIR.glob("NBTEditor*.class")) if NBT_DIR.is_dir() else []:
        check(f"pont : ancien nom retiré du JAR ({relative.name})", False, "devrait être LegacyNbtBridge.class")
    check("pont : repli historique livré", (NBT_DIR / "LegacyNbtBridge.class").is_file(),
          "artifacts/extracted/io/github/bananapuncher714/nbteditor/LegacyNbtBridge.class manquant")


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
    # les descripteurs JPMS sont exclus de target/classes pendant compile puis réinsérés en
    # prepare-package : s'ils manquent dans le paquet, c'est que le pom a perdu l'étape de restauration
    check("JAR : aucun bloc depend: (sinon refus de chargement si le nom cité manque)",
          plugin_yml is not None and not top_list(plugin_yml, "depend"),
          "le JAR contient encore un bloc depend: — Bukkit refusera de charger le plugin si le nom "
          "exact cité n'est pas installé (Vault, ProtocolLib), même avec un équivalent présent")
    check("JAR : descripteurs de module réinsérés", "module-info.class" in names,
          "module-info.class absent du paquet : l'exécution 'restore-module-descriptors' (prepare-package) "
          "du maven-resources-plugin est manquante ou mal ordonnée")

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
    upgrade_entry = "xyz/arcadiadevs/valoriatycoon/guis/UpgradeGui.class"
    if upgrade_entry in names:
        blob = jar.read(upgrade_entry)
        check("JAR : UpgradeGui.class recompilée (case statistiques câblée)",
              b"guis.upgrade-gui.stats.lore" in blob,
              "la classe livrée est encore l'ancienne (le build n'a pas compilé UpgradeGui.java)")
        check("JAR : plus de remplissage aléatoire à deux couleurs",
              b"ORANGE_STAINED_GLASS_PANE" not in blob)
    else:
        check("JAR : UpgradeGui.class recompilée", False, f"{upgrade_entry} absent du JAR")
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
