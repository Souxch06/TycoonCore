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
import xml.etree.ElementTree as ET

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


def strip_comments(text: str) -> str:
    """Retire javadoc et commentaires : un contrôle de code ne doit pas échouer sur une explication."""
    without_blocks = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"^[ \t]*//.*$", "", without_blocks, flags=re.M)


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
    check("plugin.yml : aucune dépendance dure (tout le monde du serveur reste facultatif)",
          not depends,
          f"depend={depends} — un depend: dur est résolu par NOM exact par Bukkit : il refuse les "
          "équivalents et bloque tout le chargement si l'écriture du nom diffère")
    check("plugin.yml : le seul bloc cité obligatoire est NOTRE plugin d'économie",
          "ValoriaEconomy" in soft,
          f"softdepend={soft} — ValoriaEconomy (jar du dépôt, load: STARTUP) doit être listé pour "
          "l'ordre de chargement du service Economy")
    banned = {"Vault", "VaultUnlocked", "ProtocolLib", "HoloEasy", "Essentials", "EssentialsX"}
    check("plugin.yml : plus aucune dépendance à un plugin à télécharger",
          not (banned & set(soft)),
          f"softdepend cite {sorted(banned & set(soft))} — le serveur ne doit rien installer "
          "d'extérieur : l'économie et les hologrammes viennent du dépôt")
    check("plugin.yml : miroir artifacts/extracted identique",
          (EXTRACTED / "plugin.yml").read_text() == tree_yml)
    # 4c. assemblage du second plugin : le schema du descripteur est petit et ne dit rien de clair,
    # donc on le verifie a la main. `finalName` n'est licite que dans un <format> (assemblages
    # multi-formats) ; au niveau racine, maven-assembly-plugin meurt sur `Unrecognised tag` AVANT
    # d'emballer quoi que ce soit (build #33155795647). Le nom final vient alors du pom.
    assembly = (ROOT / "src/assembly/economy.xml").read_text(encoding="utf-8") if (ROOT / "src/assembly/economy.xml").is_file() else ""
    if assembly:
        import xml.etree.ElementTree as _ET
        try:
            root_tag = _ET.fromstring(assembly)
        except _ET.ParseError as bad:
            root_tag = None
            check("src/assembly/economy.xml : XML valide", False, str(bad))
        if root_tag is not None:
            stripped = root_tag.tag.split("}")[-1]
            kids = {c.tag.split("}")[-1] for c in root_tag}
            formats = [f.text.strip() for f in root_tag.iter() if f.tag.endswith("}format") and f.text]
            check("src/assembly/economy.xml : XML valide et racine <assembly>", stripped == "assembly")
            # Liste des elements autorises enfant de <assembly>, relevee dans le XSD officiel
            # content/resources/xsd/assembly-2.1.0.xsd du depot apache/maven-site (verifiee en ligne :
            # `finalName` n'y figure PAS, d'ou l'echec du build #33155795647).
            ASSEMBLY_CHILDREN = {
                "id", "formats", "format", "includeBaseDirectory", "includeSiteDirectory",
                "baseDirectory", "file", "files", "fileSet", "fileSets", "dependencySet",
                "dependencySets", "moduleSet", "moduleSets", "repository", "repositories",
                "componentDescriptor", "componentDescriptors", "containerDescriptorHandler",
                "containerDescriptorHandlers",
            }
            foreign = sorted({c.tag.split("}")[-1] for c in root_tag} - ASSEMBLY_CHILDREN)
            check("src/assembly/economy.xml : uniquement des elements connus du schema 2.1.0",
                  not foreign,
                  f"element(s) hors XSD: {foreign} — maven-assembly-plugin echoue a la LECTURE du "
                  "descripteur, sans detail, avant meme de compiler")
            check("src/assembly/economy.xml : pas de <finalName> au niveau racine",
                  "finalName" not in kids,
                  "l'XSD ne definit finalName QUE comme enfant de <format> (assemblage multi-formats) ; "
                  "au niveau racine Maven lit le descripteur et echoue sur `Unrecognised tag: 'finalName'`")
            check("src/assembly/economy.xml : <id> et <formats> presents",
                  "id" in kids and "formats" in kids and len(formats) == 1,
                  f"formats lus: {formats}")
            # Le nom du paquet d'assemblage se decide dans le POM : `appendAssemblyId=true` herite du
            # finalName global (<build>) et produit `ValoriaTycoon-v1.6.3-economy.jar`; une etape de
            # renommage externe (antrun) cherche alors un fichier qui n'existe pas — vu sur le build
            # #33156892660. Un seul endroit decideur, pas de `mv` apres coup.
            assembly_block = pom[pom.index("maven-assembly-plugin"):] if "maven-assembly-plugin" in pom else ""
            check("pom.xml : le nom final du jar d'economie est pose par <finalName> de l'assembleur",
                  "<finalName>ValoriaEconomy-v${project.version}</finalName>" in assembly_block
                  and "<appendAssemblyId>false</appendAssemblyId>" in assembly_block,
                  "l'assembleur doit ecrire exactement target/ValoriaEconomy-v<version>.jar (nom attendu "
                  "par plugin.yml, la CI et docs/DEPLOY-2-JARS.md)")
            check("pom.xml : aucune etape de renommage du jar d'economie (antrun/move) ne relance le piege",
                  "maven-antrun-plugin" not in pom,
                  "un <move> vers un nom construit la main casse des que le nom reel change : le nom doit "
                  "venir de l'assembleur, pas d'une seconde etape")

    for xml_file in ("pom.xml", "src/assembly/economy.xml"):
        path = ROOT / xml_file
        if not path.is_file():
            continue
        raw = path.read_text(encoding="utf-8")
        try:
            ok = ET.fromstring(raw) is not None
            why = ""
        except Exception as bad:
            ok, why = False, str(bad)
        check(f"{xml_file} : XML bien forme pour Maven", ok, why or "les commentaires XML n'ont pas le droit "
              "de contenir `--` ni `<` : `--` clot le commentaire, et Maven casse avec un message d'octet")
        illegal = [m.group(0)[:48] for m in re.finditer(r"<!--(.*?)-->", raw, re.S)
                   if "--" in m.group(1) or "<" in m.group(1)]
        check(f"{xml_file} : aucun commentaire illegal (`--` ou chevron)", not illegal,
              f"{len(illegal)} commentaire(s) a revoir : {illegal[:2]} — `--` est interdit dans un "
              "commentaire XML, c'est exactement ce qui a rendu le pom illisible")

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
    # 4d. marché entre joueurs (/ah)
    for cfg_name in ("resources/config.yml", "artifacts/extracted/config.yml"):
        cfg_text = (ROOT / cfg_name).read_text(encoding="utf-8")
        check(f"{cfg_name} : clés auction-house complètes",
              all(key in cfg_text for key in ("auction-house:", "listing-fee:", "sales-tax:", "expiry-hours:",
                                              "max-listings-per-player:", "enforce-price-band:", "blacklist:")),
              "les clés du marché doivent être livrées avec le plugin, sinon les valeurs par défaut du code seule font foi")
    for yml_name in ("resources/plugin.yml", "artifacts/extracted/plugin.yml"):
        yml_text = (ROOT / yml_name).read_text(encoding="utf-8")
        check(f"{yml_name} : permissions valoriatycoon.ah.* déclarées",
              all(node in yml_text for node in ("valoriatycoon.ah.use", "valoriatycoon.ah.sell", "valoriatycoon.ah.notify")),
              "sans nœuds de permission par défaut, /ah est réservé aux OP par Bukkit")
    ah_src = (ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon/commands/AuctionHouse.java").read_text(encoding="utf-8")
    ah_code = strip_comments(ah_src)
    check("AuctionHouse : séquestre écrit à chaque mutation", 'this.save()' in ah_src)
    check("AuctionHouse : sauvegarde atomique (tmp + ATOMIC_MOVE)",
          "ATOMIC_MOVE" in ah_src and '.tmp' in ah_src,
          "sans écriture atomique, un crash pendant la sauvegarde corromprait le marché")
    check("AuctionHouse : achats/annulations passent par le prix unitaire",
          'section.contains("unit-price")' in ah_src and "store.total(id)" in ah_src
          and "store.unitPrice(id) * notDelivered" in ah_src,
          "le débit doit venir du total stocké et le remboursement de la part non livrée du prix unitaire")
    check("AuctionHouse : annonces de l'ancien format (prix au lot) reprises sans perte ni gratuité",
          'section.contains("unit-price")' in ah_src and 'getDouble("price"' in ah_src,
          "sans ce repli, une annonce existante aurait un prix unitaire de 0 et serait achetable gratuitement")
    check("AuctionHouse : coffre de récupération (retours indexés, claim, notification)",
          "addReturn" in ah_src and "setReturnItems" in ah_src and "returnItems" in ah_src
          and "public static String claim(" in ah_src and "notifyReturns" in ah_src)
    check("AuctionHouse : rien n'est lâché au sol (tout passe par l'inventaire ou le coffre)",
          "dropItemNaturally" not in ah_code and "dropItem" not in ah_code,
          "un item tombé au sol peut brûler, couler ou être ramassé : interdit dans le module AH")
    check("AuctionHouse : achat au lot entier, capacité vérifiée avant le débit",
          "capacityFor(player, lot.getMaxStackSize()) < size" in ah_code
          and "public static String buy(Player player, int id)" in ah_code)
    check("AuctionHouse : expiration traitée par tâche périodique", "sweep()" in ah_src and "runTaskTimer" in ah_src)
    check("AuctionHouse : bande de prix et blacklist appliquées à la mise en vente",
          "enforceBand" in ah_src and "blacklist.contains" in ah_src)
    ahgui = (ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon/guis/AuctionGui.java").read_text(encoding="utf-8")
    check("AuctionGui : clics reportés d'un tick (pas de mutation pendant l'événement)",
          "runTask(" in ahgui and "setCancelled(true)" in ahgui)
    check("AuctionGui : une seule vue par joueur + oubli au quit",
          "VIEWS.remove(gui.player.getUniqueId())" in ahgui and "static void forget" in ahgui)
    check("AuctionGui : aucun accès aux noms internes du serveur", "net.minecraft" not in strip_comments(ahgui))
    check("AuctionHouse : monnaie via l'économie interne du dépôt, jamais via Vault",
          "xyz.arcadiadevs.valoriateconomy.Economy" in ah_code and "net.milkbowl" not in ah_code,
          "le marché doit débiter/créditer par notre interface d'économie (aucun plugin Vault à installer)")
    check("AuctionHouse : un retrait admin va au coffre du vendeur, jamais à la poubelle",
          "store.addReturn(seller, item)" in ah_code and "returnItems" in ah_code)
    check("AuctionHouse : aucun accès aux noms internes du serveur",
          "net.minecraft" not in ah_code and "NBTEditor" not in ah_code
          and "org.bukkit.craftbukkit" not in ah_code,
          "le code du marché ne doit rien résoudre dans les noms internes du serveur")
    check("AuctionHouse : items de générateur refusés à la vente", "spawnitem.tier" in ah_src and "isPluginItem" in ah_src)

    # 4e. tableau de bord
    for cfg_name in ("resources/config.yml", "artifacts/extracted/config.yml"):
        cfg_text = (ROOT / cfg_name).read_text(encoding="utf-8")
        check(f"{cfg_name} : clés scoreboard présentes",
              "scoreboard:" in cfg_text and "update-ticks:" in cfg_text and "%money%" in cfg_text)
    sb = (ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon/utils/ScoreboardService.java").read_text(encoding="utf-8")
    check("ScoreboardService : aucun appel direct à Score#setScore (binaire-incompatible 1.21+)",
          ".setScore(" not in sb, "passer par la réflexion pour poser un score")
    check("ScoreboardService : lignes configurables et placeholders documentés",
          'getStringList("scoreboard.lines")' in sb or "scoreboard.lines" in sb)
    check("ScoreboardService branché sur le listener déjà enregistré",
          "ScoreboardService.show" in (ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon/commands/SellCommandListener.java").read_text(encoding="utf-8"))

    check("pont : repli historique livré", (NBT_DIR / "LegacyNbtBridge.class").is_file(),
          "artifacts/extracted/io/github/bananapuncher714/nbteditor/LegacyNbtBridge.class manquant")


    # 4f. « que du maison » : le serveur ne doit contenir AUCUN plugin téléchargé
    for name, needle in (("AuctionHouse", "commands/AuctionHouse.java"),
                         ("UpgradeGui", "guis/UpgradeGui.java"),
                         ("SellUtil", "utils/SellUtil.java"),
                         ("GeneratorsGui", "guis/GeneratorsGui.java"),
                         ("ScoreboardService", "utils/ScoreboardService.java"),
                         ("ValoriaTycoon", "ValoriaTycoon.java"),
                         ("LocationsData", "models/LocationsData.java")):
        src = ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon" / needle
        if src.is_file():
            code = strip_comments(src.read_text(encoding="utf-8"))
            # « HoloEasy » designe desormais NOTRE facade (valoriatycoon.hologram.HoloEasy) : seul le
            # paquet de la bibliotheque tierce est interdit.
            check(f"{name} : aucun import d'une API tierce (Vault/HoloEasy)",
                  "milkbowl" not in code and "org.holoeasy" not in code and "org/holoeasy" not in code)
    for banned_tree in ("org/holoeasy", "net/milkbowl"):
        check(f"artifacts/extracted : {banned_tree}/ n'est plus livré",
              not (EXTRACTED / banned_tree).exists(),
              f"{banned_tree} est une bibliothèque tierce ; le dépôt doit fournir sa propre version")
        check(f"sources : pas de copie décompilée de {banned_tree}",
              not (ROOT / "sources/shaded" / banned_tree).exists(),
              "conserver la source d'un paquet remplacé laisse croire qu'il est encore utilisé")
    main_class = EXTRACTED / "xyz/arcadiadevs/valoriatycoon/ValoriaTycoon.class"
    if main_class.is_file():
        blob = main_class.read_bytes()
        check("classe principale : nos types économiquement autonomes résolus",
              b"xyz/arcadiadevs/valoriateconomy/Economy" in blob,
              "le renommage de l'API d'économie n'a pas été appliqué aux classes livrées "
              "(lancer scripts/selfmade-api-patch.py)")
        check("classe principale : hologrammes internes résolus",
              b"xyz/arcadiadevs/valoriatycoon/hologram/HologramPool" in blob,
              "le renommage HoloEasy -> moteur interne n'a pas été appliqué")
        check("classe principale : le garde d'activation cite notre jar d'économie",
              b"ValoriaEconomy" in blob and b"\x01Vault\x01" not in blob)
        check("classe principale : aucun contrôle de licence à distance",
              b"spigotmc.org/legacy/premium" not in blob,
              "le plugin appelait api.spigotmc.org à chaque démarrage et se désactivait sur réponse "
              "« false » — un serveur doit rester autonome")
    for engine in ("HoloEasy.java", "HologramPool.java", "Hologram.java", "HologramBuilder.java",
                   "HologramStore.java", "HologramSetupGroup.java", "HologramRegisterGroup.java"):
        check(f"moteur d'hologrammes écrit ici : {engine}",
              (ROOT / "sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram" / engine).is_file())
    api = ROOT / "sources/api/xyz/arcadiadevs/valoriateconomy"
    check("API d'économie écrite ici : Economy.java + EconomyResponse.java",
          (api / "Economy.java").is_file() and (api / "EconomyResponse.java").is_file())
    check("pom.xml : plus aucune dépendance Maven à un artifact tiers d'API",
          "VaultAPI" not in pom and "jitpack" not in pom,
          "l'interface d'économie est compilée depuis sources/api, un dépôt distant n'a plus lieu d'être")


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

    for banned_tree in ("org/holoeasy", "net/milkbowl"):
        check(f"JAR : {banned_tree}/ absent (bibliothèque tierce retirée)",
              not any(n.startswith(banned_tree + "/") for n in names))
    check("JAR : aucune métadonnée de bibliothèque retirée",
          "META-INF/holoeasy-core.kotlin_module" not in names)
    our_entries = ("xyz/arcadiadevs/valoriateconomy/Economy.class",
                   "xyz/arcadiadevs/valoriateconomy/EconomyResponse.class",
                   "xyz/arcadiadevs/valoriatycoon/hologram/HoloEasy.class",
                   "xyz/arcadiadevs/valoriatycoon/hologram/HologramPool.class",
                   "xyz/arcadiadevs/valoriatycoon/hologram/Hologram.class",
                   "xyz/arcadiadevs/valoriatycoon/hologram/HologramBuilder.class",
                   "xyz/arcadiadevs/valoriatycoon/hologram/HologramStore.class",
                   "xyz/arcadiadevs/valoriatycoon/utils/HologramsUtil.class")
    missing = [n for n in our_entries if n not in names]
    check("JAR : nos classes d'API et d'hologrammes compilées", not missing,
          f"manquants: {missing} — le renommage vise des types que le build doit compiler (<includes>)")
    for entry in ("xyz/arcadiadevs/valoriatycoon/ValoriaTycoon.class",
                  "xyz/arcadiadevs/valoriatycoon/utils/SellUtil.class",
                  "xyz/arcadiadevs/valoriatycoon/guis/GeneratorsGui.class"):
        if entry in names:
            blob = jar.read(entry)
            check(f"JAR : {entry.split('/')[-1]} sans référence à Vault/HoloEasy",
                  b"milkbowl" not in blob and b"org/holoeasy" not in blob)
            check(f"JAR : {entry.split('/')[-1]} sans contrôle de licence",
                  b"spigotmc.org/legacy/premium" not in blob)

    if server_version is None:
        check("JAR : ServerVersion.class recompilée", False, f"{SERVER_VERSION_ENTRY} absent du JAR")
        return
    values = verify_constant_pool(
        "JAR : ServerVersion.class recompilée (détection calendaire)",
        server_version,
        {"getBukkitVersion", "getMinecraftVersion", "V26_2", "V26_1"},
        set(),
    )
    for ah_entry in ("xyz/arcadiadevs/valoriatycoon/commands/AuctionHouse.class",
                     "xyz/arcadiadevs/valoriatycoon/guis/AuctionGui.class",
                     "xyz/arcadiadevs/valoriatycoon/commands/SellCommandListener.class"):
        check(f"JAR : {ah_entry.split('/')[-1]} compilée", ah_entry in names,
              "le pom n'a pas compilé le marché des joueurs (voir <includes> du maven-compiler-plugin)")
    if "xyz/arcadiadevs/valoriatycoon/commands/AuctionHouse.class" in names:
        ah_blob = jar.read("xyz/arcadiadevs/valoriatycoon/commands/AuctionHouse.class")
        check("JAR : AuctionHouse sans NMS", b"net/minecraft" not in ah_blob and b"craftbukkit" not in ah_blob)
        check("JAR : AuctionHouse branché sur le rafraîchissement des vues", b"AuctionGui" in ah_blob)
    for sb_entry in ("xyz/arcadiadevs/valoriatycoon/utils/ScoreboardService.class",):
        check(f"JAR : {sb_entry.split('/')[-1]} compilée", sb_entry in names, "le pom ne compile pas le tableau de bord")
    if "xyz/arcadiadevs/valoriatycoon/commands/SellCommandListener.class" in names:
        listener_blob = jar.read("xyz/arcadiadevs/valoriatycoon/commands/SellCommandListener.class")
        check("JAR : SellCommandListener branché sur /ah et le scoreboard",
              b"AuctionGui" in listener_blob and b"ScoreboardService" in listener_blob,
              "le .class livré ne référence ni AuctionGui ni ScoreboardService")
    if "xyz/arcadiadevs/valoriatycoon/guis/AuctionGui.class" in names:
        blob = jar.read("xyz/arcadiadevs/valoriatycoon/guis/AuctionGui.class")
        check("JAR : AuctionGui sans NMS", b"net/minecraft" not in blob and b"craftbukkit" not in blob)
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
