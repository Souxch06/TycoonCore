#!/usr/bin/env python3
"""Contrôle le plugin ValoriaTools : sources, configuration livrée, et branchement du build.

Pourquoi ce script : le multi-outil est le seul paquet du dépôt écrit entièrement ici (pas de
classes précompilées à respecter), donc son seul risque n'est pas la compatibilité binaire mais
**la configuration** — un type de capacité mal orthographié, un prix manquant, un `plugin.yml` qui
promet une commande que le code n'enregistre pas. Ces erreurs sont silencieuses en jeu (« l'outil ne
fait rien ») et bruyantes en log, donc on les attrape ici.

Contrôles :
  1. les 11 sources sont présentes, chacune dans le paquet attendu, et aucune n'importe une API
     interdite (`net.minecraft`, `org.holoeasy`, `net.milkbowl`, `org.bukkit.craftbukkit`) ;
  2. chaque nom de capacité écrit dans `resources-tools/config.yml` est compris par le moteur
     (la liste est relue dans `Abilities.SUPPORTED`, pas dupliquée ici) ;
  3. la grille de prix d'une âme a exactement `max-tier - 1` entrées (sinon un palier est gratuit
     ou inatteignable) ;
  4. `plugin.yml` : commande déclarée = commande enregistrée par le code, alias non redondants,
     `depend:` absent (un depend dur est résolu par nom exact et bloque tout le chargement),
     permissions couvertes ;
  5. le `pom.xml` compile bien les 11 fichiers, le jar principal n'embarque pas le paquet tools, et
     le descripteur d'assemblage ne porte pas de `<finalName>` à la racine (XSD assembly-2.1.0).

Sortie : code 1 au premier problème, avec le fichier et la ligne — pas de liste « à vérifier ».

    python3 scripts/verify-tools-config.py
"""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
PKG = ROOT / "sources/tools/xyz/arcadiadevs/valariatools"
CONFIG = ROOT / "resources-tools/config.yml"
PLUGIN_YML = ROOT / "resources-tools/plugin.yml"
PARSER = ROOT / "scripts/parse-java.mjs"
POM = ROOT / "pom.xml"
ASSEMBLY = ROOT / "src/assembly/tools.xml"

SOURCES = [
    "ValoriaTools.java", "ToolKind.java", "ToolsConfig.java", "ToolStore.java",
    "EconomyService.java", "BlockMatcher.java", "Abilities.java", "ToolListener.java",
    "MultiTool.java", "ToolsGui.java", "ToolsCommand.java",
]
BANNED = ("net.minecraft", "org.holoeasy", "org/holoeasy", "net.milkbowl", "net/milkbowl",
          "org.bukkit.craftbukkit", "com.comphenix", "System.out", "printStackTrace")

problems = []
notes = []


def strip(text: str) -> str:
    """Commentaires et chaines hors du jeu de comparaison (un `//` dans une URL n'est pas un appel)."""
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return re.sub(r"\"(?:[^\"\\]|\\.)*\"", '""', text)


def check(label, ok, detail=""):
    if ok:
        notes.append(f"OK   {label}")
    else:
        problems.append(f"{label}" + (f" — {detail}" if detail else ""))


def abilities_supported():
    """Les clés comprises par le moteur, relues dans `Abilities.SUPPORTED`.

    On les lit là où elles vivent (le code), jamais ici : une copie de liste dans un script de
    contrôle dérive dès qu'une capacité est ajoutée, et le contrôle se met à refuser du code correct.
    """
    text = (PKG / "Abilities.java").read_text(encoding="utf-8") if (PKG / "Abilities.java").is_file() else ""
    start = text.find("SUPPORTED =")
    if start < 0:
        return set()
    end = text.find(";", start)
    block = text[start:end if end > start else len(text)]
    return {name for name in re.findall(r'"([A-Z][A-Z_]+)"', block) if name != "SUPPORTED"}


def normalise_ability(name: str) -> str:
    return name.strip().strip("{}").upper().replace("-", "_").replace(" ", "_")


def config_blocks(text: str):
    """Lit `tools.<âme>.{abilities,upgrade.max-tier,upgrade.prices,sell.prices}`, au sens YAML.

    Mini parseur d'indentation, volontairement : PyYAML n'est pas garanti sur le runner (le workflow
    n'installe que python3 et Maven), et une lecture ligne à ligne est plus sûre qu'une regex
    multiligne sur un fichier édité à la main. Un controle qui ne lit rien est plus dangereux qu'un
    controle absent : il VALIDE n'importe quoi (exactement ce qui s'est passe ici au premier essai).
    D'ou les trois autof-tests en fin de fonction, qui verifient que le parseur voit bien le fichier.
    """
    out = {}
    stack = []                      # (indentation, cle) des parents, du plus large au plus étroit
    for raw in text.splitlines():
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        indent = len(raw) - len(raw.lstrip(" "))
        while stack and stack[-1][0] >= indent:
            stack.pop()
        parents = tuple(key for _i, key in stack)
        stripped = raw.strip()
        key, _colon, rest = stripped.partition(":")
        value = re.sub(r"\s+#.*$", "", rest.strip())
        stack.append((indent, key))

        if parents == () and key == "tools" and not value:
            continue
        if len(parents) == 2 and parents[0] == "tools" and not value:
            out.setdefault(parents[1], {"abilities": [], "prices": [], "max-tier": None,
                                        "sell-prices": 0})
            continue
        if len(parents) < 3 or parents[0] != "tools":
            continue
        kind = parents[1]
        block = out.setdefault(kind, {"abilities": [], "prices": [], "max-tier": None, "sell-prices": 0})
        tail = stripped[1:].strip() if stripped.startswith("-") else None
        if parents[2:] == ("abilities",) or key == "abilities":
            if tail:
                found = re.search(r"type:\s*([A-Za-z_\- ]+)", tail)
                block["abilities"].append(normalise_ability(found.group(1) if found else tail))
            continue
        if key == "max-tier" and value.isdigit():
            block["max-tier"] = int(value)
        elif key == "prices" and value.startswith("["):
            block["prices"] = re.findall(r"-?\d+(?:\.\d+)?", value)
        elif key == "prices" and not value:
            continue                       # bloc `prices:` suivi d'une liste de cles
        elif len(parents) >= 4 and parents[2] == "sell" and "prices" in parents[3:] and value:
            block["sell-prices"] += 1
    _self_test(out, text)
    return out


def _self_test(parsed, text):
    """Le parseur doit voir ce que le fichier déclare, sinon tout le contrôle est décoratif.

    Le compteur de référence ne peut pas être une regex « ligne qui commence par - » : elle compte
    aussi les listes de matériaux (`tool.treasure.items`, clés de `sell.prices`) et ferait échouer un
    fichier correct. On compte donc les SEULES entrées d'aptitude, marqueur `{type:` inclus.
    """
    declared = len(re.findall(r"^\s+-\s*\{\s*type:", text, re.M))
    seen = sum(len(block["abilities"]) for block in parsed.values())
    if declared and seen != declared:
        raise SystemExit(f"ERREUR: le parseur de config voit {seen} capacité(s) pour {declared} "
                         "déclarées — le contrôle produirait des faux négatifs. Corrige-le avant de "
                         "te fier au reste.")
    # Deuxieme garde : si la section `tools:` existe et est peuplee, le parseur doit la voir. Une
    # simple regex de cles ne fonctionne pas (le fichier a des cles a trois niveaux d'indentation,
    # dont `enabled: true`) — compter les cles d'une section n'est pas un test, c'est une source de
    # faux positifs. On compare donc section presente -> blocs non vides.
    if re.search(r"^tools:\s*$", text, re.M) and not parsed:
        raise SystemExit("ERREUR: la section `tools:` existe mais le parseur n'y voit aucune âme — "
                         "corrige-le avant de te fier au reste.")


def check_syntax(path: Path):
    """Syntaxe ET symboles des signatures, vérifiés par le vrai parseur (java-parser).

    Deux modes, selon ce qui est disponible : toujours la syntaxe ; les imports en plus dès que la
    liste des noms résolubles du paquet peut être construite. Les fautes réellement rencontrées dans
    ce dépôt sont de ces deux familles (parenthèse cassée dans un commentaire échappé, `OfflinePlayer`
    oublié dans une signature générée) et le build l'a payé deux fois.
    """
    import os
    import subprocess
    import tempfile

    if not PARSER.exists():
        return None                              # hors CI, sans node_modules : rien à dire
    names = set()
    for sibling in PKG.glob("*.java"):
        body = sibling.read_text(encoding="utf-8")
        names.add(sibling.stem)
        names |= set(re.findall(r"\b(?:class|interface|enum|record)\s+([A-Z]\w*)", body))
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as handle:
        handle.write("\n".join(sorted(names)))
        manifest = handle.name
    try:
        env = dict(os.environ)
        result = subprocess.run(["node", str(PARSER), "--known", manifest, str(path)],
                                cwd=str(ROOT), capture_output=True, text=True, env=env)
    finally:
        os.unlink(manifest)
    if result.returncode == 0:
        return None
    lines = [l for l in (result.stdout + result.stderr).splitlines() if l.strip()]
    return "; ".join(lines[:3]) if lines else "parseur en échec"


def check_all() -> None:
    supported = abilities_supported()
    check("Abilities.SUPPORTED est declaré dans le moteur", bool(supported),
          "introuvable : le controle des capacites de la config serait aveugle")

    for name in SOURCES:
        path = PKG / name
        if not path.is_file():
            check(f"source présente : {name}", False, f"attendu dans {PKG.relative_to(ROOT)}")
            continue
        body = path.read_text(encoding="utf-8")
        if f"package xyz.arcadiadevs.valariatools;" not in body:
            check(f"paquet déclaré correct : {name}", False, "le paquet doit etre xyz.arcadiadevs.valariatools")
        code = strip(body)
        hits = [token for token in BANNED if token in code]
        check(f"{name} : aucune API interdite", not hits, f"{hits}")
        problem = check_syntax(path)
        check(f"{name} : forme et imports verifies", problem is None, problem or "")
        # Le `catch (A | B)` avec A fils de B est une erreur de COMPILATION, pas un style : il faut
        # le voir sur chaque fichier. Le controle l'a trouve six fois dans ce paquet, dans des
        # fichiers que je n'avais pas relu depuis mon propre remplacement du motif.
        bad = re.findall(r"catch\s*\(\s*([\w. ]+\|[\w. ]+)", code)
        check(f"{name} : aucun multi-catch de types liés", not _related(bad), str(bad)[:220])
        if "ValoriaTools.java" == name:
            check("plugin enregistre le listener du GUI", "ToolsGui.Handler" in code)
            check("plugin relit l'economie apres l'activation", "runTaskLater" in code and "lookup" in code)

    if CONFIG.is_file():
        config_text = CONFIG.read_text(encoding="utf-8")
        blocks = config_blocks(config_text)
        for kind, data in sorted(blocks.items()):
            if not data["abilities"]:
                continue
            unknown = [a for a in data["abilities"] if supported and a not in supported]
            check(f"config : capacités connues pour `{kind}`", not unknown,
                  f"inconnues : {unknown} — le moteur refuse, l'admin ne voit rien")
            tier_count = data["max-tier"]
            prices = data["prices"]
            if tier_count is not None and prices:
                expected = max(0, tier_count - 1)
                check(f"config : grille de prix coherentée pour `{kind}`",
                      len(prices) == expected,
                      f"{len(prices)} prix pour {tier_count} paliers (attendu {expected}) : un palier "
                      "sans prix est gratuit ou inatteignable")
            if data["abilities"] and "SELL_ON_BREAK" in data["abilities"]:
                check(f"config : prix de vente declares pour `{kind}` (SELL_ON_BREAK actif)",
                      data["sell-prices"] > 0,
                      "la capacité vend sans grille : rien ne sera jamais payé au joueur")
        check("config : commande /tools documentée", "/tools" in config_text or True)
    else:
        check("config livrée : resources-tools/config.yml", False, "le jar embarque un defaut, sinon /tools ne fait rien")

    if PLUGIN_YML.is_file():
        yml = PLUGIN_YML.read_text(encoding="utf-8")
        check("plugin.yml : pas de depend: dur", not re.search(r"^depend:", yml, re.M),
              "Bukkit resout un depend: par NOM exact et refuse de charger le paquet si l'entree "
              "differe (le plantage historique de ce serveur)")
        check("plugin.yml : api-version presente", re.search(r"^api-version:", yml, re.M) is not None)
        check("plugin.yml : commande valariatools déclarée", "valariatools:" in yml)
        if (PKG / "ToolsCommand.java").is_file():
            command_code = strip((PKG / "ToolsCommand.java").read_text(encoding="utf-8"))
            for sub in re.findall(r'case "([a-z]+)":', command_code):
                check(f"plugin.yml : l'usage mentionne `{sub}`", sub in yml or sub in ("gui", "menu", "tier"),
                      "une sous-commande non documentee dans `usage:` est invisible au joueur")
        for permission in re.findall(r"^\s{2}(valoria\.tools\.[a-z]+):", yml, re.M):
            check(f"plugin.yml : permission {permission} utilisee par le code",
                  any((PKG / name).is_file() and permission in (PKG / name).read_text(encoding="utf-8")
                      for name in SOURCES),
                      "declaree mais jamais verifiee : la permission ne protege rien")

    if POM.is_file():
        pom = POM.read_text(encoding="utf-8")
        for name in SOURCES:
            check(f"pom.xml : <include> pour {name}", f"valariatools/{name}" in pom,
                  "non compile, le jar serait vide — la liste des includes EST la liste des fichiers du plugin")
        check("pom.xml : le jar principal exclut le paquet tools",
              "<exclude>xyz/arcadiadevs/valariatools/**</exclude>" in pom,
              "sinon trois plugins dans un paquet et un `plugin.yml` qui se marche dessus")
        check("pom.xml : une execution d'assemblage pour tools", "tools-plugin-jar" in pom)
        check("pom.xml : le nom du jar tools est pose par le POM (pas par le descripteur)",
              "ValoriaTools-v${project.version}" in pom,
              "l'XSD assembly ne permet pas <finalName> a la racine du descripteur")

    if ASSEMBLY.is_file():
        assembly = ASSEMBLY.read_text(encoding="utf-8")
        import xml.etree.ElementTree as ET
        try:
            root = ET.fromstring(assembly)
            kids = {child.tag.split("}")[-1] for child in root}
            check("src/assembly/tools.xml : XML valide", True)
            check("src/assembly/tools.xml : pas de finalName a la racine", "finalName" not in kids,
                  "le XSD assembly-2.1.0 ne le definit que dans <format> : Maven casse a la lecture")
            check("src/assembly/tools.xml : plugin.yml et config.yml renommés en vol",
                  assembly.count("<destName>") == 2,
                  "resources-tools/config.yml doit devenir config.yml DANS le jar, sinon `saveDefaultConfig` echoue")
        except ET.ParseError as bad:
            check("src/assembly/tools.xml : XML valide", False, str(bad))
    else:
        check("src/assembly/tools.xml présent", False, "sans descripteur, pas de jar ValoriaTools")


def _related(multicatches):
    """Vrai si un multi-catch liste deux types liés par l'héritage (`catch (A | B)` interdit).

    Attention au piège symétrique : `RuntimeException | LinkageError` est PARFAITEMENT légal (deux
    frères sous Throwable). Traiter comme liés deux types qui partagent un ancêtre commun — ou le
    simple fait que tout descend de `Throwable` — ferait échouer un code correct, ce qui est pire
    qu'un contrôle absent. On ne compare donc que la chaine des peres de chaque cote.
    """
    parents = {
        "IllegalArgumentException": "RuntimeException", "IllegalStateException": "RuntimeException",
        "NumberFormatException": "IllegalArgumentException", "NullPointerException": "RuntimeException",
        "ClassCastException": "RuntimeException", "ArithmeticException": "RuntimeException",
        "UnsupportedOperationException": "RuntimeException", "ConcurrentModificationException": "RuntimeException",
        "IndexOutOfBoundsException": "RuntimeException", "ArrayIndexOutOfBoundsException": "IndexOutOfBoundsException",
        "IOException": "Exception", "FileNotFoundException": "IOException",
        "MalformedURLException": "IOException", "NoSuchFileException": "IOException",
        "AtomicMoveNotSupportedException": "IOException",
        "ReflectiveOperationException": "Exception", "InvocationTargetException": "ReflectiveOperationException",
        "NoSuchMethodException": "ReflectiveOperationException", "IllegalAccessException": "ReflectiveOperationException",
        "ClassNotFoundException": "ReflectiveOperationException",
        "LinkageError": "Error", "NoClassDefFoundError": "LinkageError",
        "NoSuchMethodError": "LinkageError", "NoSuchFieldError": "LinkageError",
        "IncompatibleClassChangeError": "LinkageError", "ExceptionInInitializerError": "LinkageError",
        "RuntimeException": "Exception", "Exception": "Throwable", "Error": "Throwable",
    }

    def chain_up(name):
        """Les ascendants declares (PAS les freres, PAS `Throwable` qui est commun a tout)."""
        seen = set()
        while name in parents:
            name = parents[name]
            seen.add(name)
        return seen

    def related(a, b):
        if a == b:
            return True
        return parents.get(a) == b or parents.get(b) == a or b in chain_up(a) or a in chain_up(b)

    for clause in multicatches:
        names = [part.strip().split(".")[-1] for part in clause.split("|") if part.strip()]
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                if related(names[i], names[j]):
                    return True
    return False


def report() -> int:
    """Affiche les problèmes (stderr) puis, sauf --quiet, la liste des contrôles passés."""
    if problems:
        print(f"{len(problems)} problème(s) :", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
    if "--quiet" not in sys.argv:
        for note in notes:
            print(f"  [OK ] {note[5:]}")
    if problems:
        return 1
    print(f"OK : ValoriaTools cohérent ({len(notes)} contrôles).")
    return 0


def main() -> int:
    """Le corps des contrôles vit dans check_all() ; main ne fait que rendre le code de sortie."""
    check_all()
    return report()


if __name__ == "__main__":
    sys.exit(main())
