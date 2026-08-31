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
    "ValoriaTools.java", "ToolKind.java", "ToolsConfig.java", "ToolStore.java", "ToolStats.java",
    "EconomyService.java", "BlockMatcher.java", "Abilities.java", "ToolListener.java", "ToolGuard.java",
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


def normalise_ability(name: str) -> str:
    return name.strip().strip("{}").upper().replace("-", "_").replace(" ", "_")


DOC = ROOT / "docs/WIKI-GENTYCOON-OUTILS.md"
SOULS = ("pickaxe", "axe", "sword", "rod")
SOUL_OF_SECTION = {"pioche": "pickaxe", "houe": "axe", "épée": "sword", "epée": "sword",
                   "canne à pêche": "rod", "canne a peche": "rod"}


def parse_flow(text: str):
    """Un flux YAML `- {a: 1, b: "x, y", c: [1, 2]}` → dict. Mini-lecteur, pas de dependance.

    Les descriptions du wiki contiennent des virgules : la separation ne peut donc pas etre un simple
    `split(",")`. On ne decoupe qu'a profondeur 0 (hors crochets et hors guillemets) — c'est la meme
    regle que SnakeYAML, en trente lignes.
    """
    text = text.strip()
    if text.startswith("{"):
        text = text[1:]
    if text.endswith("}"):
        text = text[:-1]
    out = {}
    depth = 0
    quote = None
    current = []
    for char in text:
        if quote:
            current.append(char)
            if char == quote:
                quote = None
            continue
        if char in "\"'":
            quote = char
            current.append(char)
            continue
        if char in "[{":
            depth += 1
        elif char in "]}":
            depth -= 1
        if char == "," and depth == 0:
            _flow_put(out, "".join(current))
            current = []
            continue
        current.append(char)
    _flow_put(out, "".join(current))
    return out


def _flow_put(out, chunk):
    key, _colon, value = chunk.strip().partition(":")
    if not key or not _colon:
        return
    value = value.strip()
    if value.startswith("[") and value.endswith("]"):
        out[key.strip()] = [part.strip() for part in value[1:-1].split(",") if part.strip()]
        return
    if len(value) >= 2 and value[0] in "\"'" and value[-1] == value[0]:
        out[key.strip()] = value[1:-1]
        return
    out[key.strip()] = value


def number(value):
    try:
        return float(str(value).replace(",", "."))
    except (TypeError, ValueError):
        return None


def config_blocks(text: str):
    """Lit `tools.<ame> : capacites, paliers, prix, vente`, au sens YAML (mini parseur d'indentation).

    PyYAML n'est pas garanti sur le runner (le workflow n'installe que python3 et Maven), et une
    lecture ligne a ligne est plus sure qu'une regex multiligne sur un fichier edite a la main. Un
    controle qui ne lit rien est plus dangereux qu'un controle absent : il VALIDE n'importe quoi (c'est
    arrive ici, avec un compteur qui voyait 0 capacite sur 0 ligne `type:`). D'ou les autof-tests de
    `_self_test`, qui verifient que le parseur voit le fichier AVANT de le croire.
    """
    out = {}
    stack = []                      # (indentation, cle) des parents, du plus large au plus etroit
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

        if len(parents) == 2 and parents[0] == "tools" and not value:
            out.setdefault(parents[1], {"abilities": [], "prices": [], "max-tier": None,
                                        "sell-prices": 0, "price-base": None, "price-ratio": None,
                                        "jobs": {}})
            continue
        if len(parents) < 3 or parents[0] != "tools" or parents[1] not in SOULS:
            continue
        block = out.setdefault(parents[1], {"abilities": [], "prices": [], "max-tier": None,
                                            "sell-prices": 0, "price-base": None, "price-ratio": None})
        if stripped.startswith("- {") and parents[2:] == ("abilities",):
            entry = parse_flow(stripped[2:].strip())
            if entry.get("type"):
                entry["_id"] = entry.get("id") or entry["type"].lower()
                block["abilities"].append(entry)
            continue
        if parents[2:] == ("ability-price",) and key in ("base", "step", "cap"):
            block.setdefault("ability-price", {})[key] = number(value)
            continue
        if key == "max-tier" and value.isdigit():
            block["max-tier"] = int(value)
        elif key == "prices" and value.startswith("["):
            block["prices"] = [number(item) for item in re.findall(r"-?\d+(?:\.\d+)?", value)]
        elif key == "prices" and not value:
            continue                       # bloc `prices:` suivi d'une liste de cles de revente
        elif key == "price-base":
            block["price-base"] = number(value)
        elif key == "price-ratio":
            block["price-ratio"] = number(value)
        elif parents[2:] == ("jobs", "gains") or parents[2:] == ("jobs", "xp"):
            table = block.setdefault("jobs", {}).setdefault(parents[3], {})
            parsed = number(value)
            if parsed is not None:
                table[key] = parsed
        elif len(parents) >= 4 and parents[2] == "sell" and "prices" in parents[3:] and value:
            block["sell-prices"] += 1
    _self_test(out, text)
    return out


def _self_test(parsed, text):
    """Le parseur doit voir ce que le fichier declare, sinon tout le controle est decoratif."""
    declared = len(re.findall(r"^\s+-\s*\{\s*id:", text, re.M))
    seen = sum(len(block["abilities"]) for block in parsed.values())
    if seen != declared:
        raise SystemExit(f"ERREUR: le parseur de config voit {seen} capacite(s) pour {declared} "
                         "declarees — le controle produirait des faux negatifs. Corrige-le avant de "
                         "te fier au reste.")
    if declared < 60:
        raise SystemExit(f"ERREUR: seulement {declared} capacites lues dans la config livree ; le bareme "
                         "du wiki en fait 72 (plus les capacites propres a Valoria). Le fichier est-il "
                         "toujours genere par scripts/gen-tools-config.py ?")
    if re.search(r"^tools:\s*$", text, re.M) and not parsed:
        raise SystemExit("ERREUR: la section `tools:` existe mais le parseur n'y voit aucune âme — "
                         "corrige-le avant de te fier au reste.")
    # Autot-test du parseur de flux : une description avec virgule doit rester UNE seule valeur.
    probe = parse_flow('{id: x, type: VEIN, desc: "un, deux, trois", items: [A, B]}')
    if probe.get("desc") != "un, deux, trois" or probe.get("items") != ["A", "B"]:
        raise SystemExit("ERREUR: parse_flow ne respecte pas les virgules dans les chaines — "
                         f"lu {probe}")


JOB_SECTIONS = {"Mineur": "pickaxe", "Fermier": "axe", "Chasseur": "sword", "Pêcheur": "rod"}


def job_numbers():
    """Les paires `argent/XP` publiees par le wiki, relues dans docs/WIKI-GENTYCOON-OUTILS.md.

    Le document est la copie controlee des pages « Les Métiers » : chaque ligne y a la forme
    `Nom 0.05/0.01`. On en tire un ensemble de valeurs par âme, et la config n'a pas le droit d'en
    sortir — c'est ce qui empele d'inventer un prix tout en appelant ça « le barème du wiki ».
    """
    out = {kind: set() for kind in JOB_SECTIONS.values()}
    if not DOC.is_file():
        return out
    soul = None
    for line in DOC.read_text(encoding="utf-8").splitlines():
        title = re.match(r"^###\s+(\S+)", line)
        if title:
            head = title.group(1).replace("ê", "ê")
            soul = None
            for needle, kind in JOB_SECTIONS.items():
                if needle.lower().startswith(head.lower()[:5]):
                    soul = kind
                    break
            continue
        if soul is None:
            continue
        for value in re.findall(r"\d+\.\d+", line):
            out[soul].add(round(float(value), 4))
        if re.match(r"^\|", line):        # tableaux markdown eventuels
            continue
    return out


def wiki_rows():
    """Les tableaux du wiki, relus dans `docs/WIKI-GENTYCOON-OUTILS.md`.

    C'est la seule fagon de controler la fidelite sans re-aller chercher le site : le document est la
    copie verifiee des tableaux (nom, description, verrous, niveau max), et la config doit y coller.
    Une ligne est retenue si sa derniere colonne nomme un noyau entre backticks.
    """
    if not DOC.is_file():
        return []
    rows = []
    soul = None
    for line in DOC.read_text(encoding="utf-8").splitlines():
        heading = re.match(r"^##\s+(.+)$", line)
        if heading:
            title = heading.group(1).lower()
            for needle, kind in SOUL_OF_SECTION.items():
                if needle in title:
                    soul = kind
                    break
            continue
        if not line.strip().startswith("|") or soul is None:
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) < 4 or cells[0] in ("Enchantement", "") or set(cells[0]) <= {"-", " ", ":"}:
            continue
        kernel = re.search(r"`([A-Z_]+)`", cells[-1])
        if not kernel:
            continue
        numbers = [int(cell) for cell in cells[1:-1] if re.fullmatch(r"\d+", cell)]
        if not numbers:
            continue
        rows.append({"soul": soul, "label": cells[0], "kernel": kernel.group(1),
                     "max-level": numbers[-1], "unlock": max(numbers[:-1]) if len(numbers) > 1 else 0})
    return rows


def normalise_label(text):
    text = (text or "").lower().strip()
    text = text.replace("é", "e").replace("è", "e").replace("ê", "e").replace("à", "a")
    text = text.replace("'", "").replace("-", " ")
    return re.sub(r"\s+", " ", text)


def check_yaml_shape(text: str):
    """Vrai si aucun noeud n'a la fois des cles et des items de sequence (YAML invalide).

    Le piege est fin : `items:` suivi de `- NAME_TAG` est parfait (la sequence EST la valeur), alors que
    `abilities:` suivi de `price-base: 250` puis de `- {id: …}` est refuse par SnakeYAML. La difference
    est la : un noeud qui recoit les deux formes. Le mini-parseur du bloc, lui, avale les deux sans
    broncher — d'ou ce controle distinct, et son auto-test.
    """
    shapes = {}
    stack = []
    for raw in text.splitlines():
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        indent = len(raw) - len(raw.lstrip(" "))
        while stack and stack[-1][0] >= indent:
            stack.pop()
        parents = tuple(key for _i, key in stack)
        stripped = raw.strip()
        kind = "sequence" if stripped.startswith("- ") else "mapping"
        seen = shapes.setdefault(parents, set())
        seen.add(kind)
        if len(seen) > 1:
            return False, ("liste et clés au même niveau sous `" + ".".join(parents)
                           + "` : SnakeYAML refuse le fichier")
        if kind == "mapping":
            stack.append((indent, stripped.partition(":")[0]))
        # les cles d'une sequence (`- {…}` en flux) n'ont pas d'enfants : rien a empiler
    return True, ""


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


PRIVATE_KIND_FIELDS = (
    "abilities", "prices", "maxTier", "sellPrices", "jobPrices", "jobXp", "material", "displayName",
    "lore", "tags", "blockNames", "namespaces", "xpPerBlock", "durabilityCost", "sellMultiplier",
    "sellMinValue", "abilityPriceBase", "abilityPriceStep", "abilityPriceCap", "tierPriceBase",
    "tierPriceRatio", "tierPriceCap", "replant",
)

# Le paquet nomme systematiquement `kindConfig` les valeurs de ToolsConfig.KindConfig : c'est le seul
# recepteur qu'on puisse reconnaitre sans typer les expressions, et c'est exactement la forme du bug
# que la CI a attrape (ToolStore lisait `kindConfig.abilities`, champ prive).
KIND_RECEIVER = r"\bkindConfig\.(" + "|".join(PRIVATE_KIND_FIELDS) + r")(?!\s*\()"


def private_field_reads(root: Path, own: Path):
    """Les lectures `kindConfig.<champ-privé>` écrites hors de ToolsConfig (erreur de compilation)."""
    hits = []
    for path in sorted(root.glob("*.java")):
        if path == own:
            continue
        body = re.sub(r"/\*.*?\*/", "", path.read_text(encoding="utf-8"), flags=re.S)
        body = re.sub(r"//[^\n]*", "", body)
        body = re.sub(r"\"(?:[^\"\\]|\\.)*\"", '""', body)
        for line_no, line in enumerate(body.splitlines(), 1):
            found = re.search(KIND_RECEIVER, line)
            if found:
                hits.append(f"{path.name}:{line_no} -> {found.group(0).strip()}")
    return hits


def _private_field_self_test():
    """La regle doit tirer sur l'echantillon fautif et dormir sur l'echantillon correct."""
    import tempfile

    with tempfile.TemporaryDirectory() as tmp:
        folder = Path(tmp)
        (folder / "Bad.java").write_text(
            "class Bad { void x(KindConfig kindConfig) { for (Object a : kindConfig.abilities) { } } }\n",
            encoding="utf-8")
        if not private_field_reads(folder, folder / "Absent.java"):
            raise SystemExit("ERREUR: la regle de visibilite ne voit pas le cas qu'elle doit voir —"
                             " elle serait decorative")
        (folder / "Bad.java").unlink()
        (folder / "Good.java").write_text(
            "class Good { private final java.util.List<Object> tags = new java.util.ArrayList<>();\n"
            "  void x(ToolsConfig config, KindConfig kindConfig) {"
            " this.tags.addAll(config.abilities(kindConfig)); } }\n",
            encoding="utf-8")
        if private_field_reads(folder, folder / "Absent.java"):
            raise SystemExit("ERREUR: la regle de visibilite flagge un code correct (faux positif)")


INT_ACCESSORS = r"(?:value|levelValue|valueAt|maxTier|xpPerBlock|durabilityCost)"
LONG_LITERAL = r"\b\d+L\b"


def long_into_int_calls(root: Path):
    """Les appels `.<accesseur-int>(…, <n>L)` du paquet (erreur de compilation garantie)."""
    hits = []
    pattern = re.compile(r"\." + INT_ACCESSORS + r"\(([^()]*" + LONG_LITERAL + r"[^()]*)\)")
    for path in sorted(root.glob("*.java")):
        body = re.sub(r"/\*.*?\*/", "", path.read_text(encoding="utf-8"), flags=re.S)
        body = re.sub(r"//[^\n]*", "", body)
        for line_no, line in enumerate(body.splitlines(), 1):
            if re.search(LONG_LITERAL, line) and pattern.search(line):
                hits.append(f"{path.name}:{line_no} -> {line.strip()[:90]}")
    return hits


def _long_literal_self_test():
    """La regle doit voir le motif qui a couté un run, et pas son voisin legitime."""
    import tempfile

    with tempfile.TemporaryDirectory() as tmp:
        folder = Path(tmp)
        (folder / "Bad.java").write_text(
            "class Bad { void x(Effect e) { long t = Math.max(20L, e.value(\"duration\", 200L)); } }\n",
            encoding="utf-8")
        if not long_into_int_calls(folder):
            raise SystemExit("ERREUR: la regle du litteral long ne voit pas le motif qu'elle doit voir")
        (folder / "Bad.java").unlink()
        (folder / "Good.java").write_text(
            "class Good { void x(Effect e) { long t = Math.max(20L, (long) e.value(\"duration\", 200) * 50L); } }\n",
            encoding="utf-8")
        if long_into_int_calls(folder):
            raise SystemExit("ERREUR: la regle du litteral long flagge un code correct (faux positif)")


def bare_code(path: Path) -> str:
    """Le source sans commentaires, mais avec ses chaines : pour en EXTRAIRE des noms, pas des appels.

    `strip()` neutralise les chaines (juste pour reperer un appel), ce qui rend toute recherche de
    `case "x":` vide — et un controle vide passe, ce qui est le pire des resultats.
    """
    body = re.sub(r"/\*.*?\*/", "", path.read_text(encoding="utf-8"), flags=re.S)
    return re.sub(r"//[^\n]*", "", body)


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
        if name == "ToolListener.java":
            _related_self_test()   # une seule fois : l'auto-test ne depend pas du fichier
        if "ValoriaTools.java" == name:
            check("plugin enregistre le listener du GUI", "ToolsGui.Handler" in code)
            check("plugin relit l'economie apres l'activation", "runTaskLater" in code and "lookup" in code)

    private_hits = private_field_reads(PKG, PKG / "ToolsConfig.java")
    check("aucun champ privé de KindConfig lu depuis une autre classe (erreur de compilation)",
          not private_hits, f"{private_hits[:4]} : ces champs sont privés, seul ToolsConfig y a droit —"
          " passe par l'accesseur public (abilities(), maxTier(), …)")
    _private_field_self_test()
    long_hits = long_into_int_calls(PKG)
    check("aucun littéral long passé à un accesseur int (lossy conversion)", not long_hits,
          f"{long_hits[:3]} : `value`, `levelValue`, `valueAt` renvoient int — un `200L` y est refusé"
          " par javac")
    _long_literal_self_test()

    if CONFIG.is_file():
        config_text = CONFIG.read_text(encoding="utf-8")
        shape_ok, shape_detail = check_yaml_shape(config_text)
        check("config : aucune liste mélangée à des clés (SnakeYAML refuserait le fichier)",
              shape_ok, shape_detail + " — le plugin ne s'activerait pas du tout, et Maven ne voit rien")
        blocks = config_blocks(config_text)
        check("config : les quatre âmes sont déclarées", len(blocks) == 4, f"{sorted(blocks)}")
        for kind, data in sorted(blocks.items()):
            abilities = data["abilities"]
            types = [normalise_ability(a["type"]) for a in abilities]
            unknown = sorted({t for t in types if supported and t not in supported})
            check(f"config : capacités connues pour `{kind}`", not unknown,
                  f"inconnues : {unknown} — le moteur refuse au chargement, l'admin ne voit rien")
            ids = [a.get("_id", "") for a in abilities]
            check(f"config : ids uniques pour `{kind}`", len(ids) == len(set(ids)),
                  f"doublon = niveaux achetes partages : {[i for i in ids if ids.count(i) > 1]}")
            check(f"config : au moins une capacité pour `{kind}`", bool(abilities))
            max_tier = data["max-tier"] or 0
            too_high = [a.get("_id") for a in abilities
                        if int(a.get("unlock", 1) or 1) > max_tier]
            check(f"config : verrous dans la plage de paliers pour `{kind}`", not too_high,
                  f"{too_high} : une capacité ouverte au-delà de max-tier est inaccessible")
            bad = []
            for ability in abilities:
                level = number(ability.get("max-level", "1"))
                if level is None or level < 1:
                    bad.append(f"{ability.get('_id')}:max-level")
                for key, value in ability.items():
                    if key.endswith("chance") and value not in (None, ""):
                        chance = number(value)
                        if chance is None or not 0.0 <= chance <= 1.0:
                            bad.append(f"{ability.get('_id')}:{key}")
                    if key == "price" and (number(value) or 0) <= 0:
                        bad.append(f"{ability.get('_id')}:price")
            check(f"config : valeurs bornées pour `{kind}`", not bad, f"hors bornes : {bad[:6]}")
            prices = data["prices"]
            if max_tier and prices:
                expected = max(0, max_tier - 1)
                check(f"config : grille de prix cohérente pour `{kind}`", len(prices) == expected,
                      f"{len(prices)} prix pour {max_tier} paliers (attendu {expected})")
            else:
                check(f"config : formule de prix pour `{kind}`", data["price-base"] not in (None, 0),
                      "ni liste `upgrade.prices` ni `upgrade.price-base` : les paliers seraient gratuits")
                check(f"config : ratio de prix plausible pour `{kind}`",
                      data["price-ratio"] is None or 1.0 < data["price-ratio"] <= 2.0,
                      f"{data['price-ratio']} : au-delà de 2, le palier 50 devient astronomique")
            if "SELL_ON_BREAK" in types:
                check(f"config : prix de vente déclarés pour `{kind}` (SELL_ON_BREAK actif)",
                      data["sell-prices"] > 0,
                      "la capacité vend sans grille : rien ne sera jamais payé au joueur")
            for ability in abilities:
                if normalise_ability(ability["type"]) == "RANDOM_ENCHANT":
                    check(f"config : `enchants:` déclaré pour {kind}/{ability.get('_id')}",
                          bool(ability.get("enchants")),
                          "sinon la capacité ne peut rien poser et reste silencieuse")
        # --- branchement : une capacite que personne ne lit est un faux ami -----------------
        wired = ""
        for source in ("ToolListener.java", "ToolsConfig.java", "Abilities.java"):
            if (PKG / source).is_file():
                wired += (PKG / source).read_text(encoding="utf-8")
        for kind, data in sorted(blocks.items()):
            declared = {normalise_ability(a["type"]) for a in data["abilities"]}
            idle = sorted(name for name in declared
                          if '"' + name + '"' not in wired)
            check(f"config : capacités branchées sur un noyau du code pour `{kind}`", not idle,
                  f"{idle} : déclarées dans le YAML mais jamais lues par le plugin — l'admin"
                  " croirait avoir vendu un effet, le joueur ne verrait rien")
        if blocks:
            configured = {normalise_ability(a["type"]) for data in blocks.values()
                          for a in data["abilities"]}
            orphans = sorted(kernel for kernel in configured
                             if '"' + kernel + '"' not in wired)
            check("moteur : chaque noyau configuré a un appelant", not orphans,
                  f"{orphans} : le noyau se dit compris par le moteur, mais aucun code ne le consomme")

        # --- gains de metier : la config ne peut citer que des valeurs publiees
        published = job_numbers()
        # Les valeurs sont lues EN ENSEMBLE (0.07 parait quatre fois, il ne compte qu'une fois) : les
        # seuils sont donc petits, mais ils coupent un controle aveugle si le document est reecrit sans
        # les paires `argent/XP`.
        check("wiki : tableaux de métiers relus dans le document",
              sum(len(values) for values in published.values()) >= 50
              and all(len(values) >= 2 for values in published.values()),
              f"{ {k: len(v) for k, v in published.items()} } — le contrôle des prix serait aveugle")
        for kind, data in sorted(blocks.items()):
            table = data.get("jobs") or {}
            gains = table.get("gains") or {}
            xp = table.get("xp") or {}
            check(f"config : gains de métier déclarés pour `{kind}`", bool(gains) and bool(xp),
                  "sans `jobs.gains`/`jobs.xp`, l'âme ne paie que la revente des drops : le barème du"
                  " métier (publié par le wiki) n'est pas appliqué")
            allowed = published.get(kind) or set()
            invented = sorted(f"{key}={value}" for source in (gains, xp) for key, value in source.items()
                              if allowed and round(float(value), 4) not in allowed)
            check(f"config : prix de métier publiés par le wiki pour `{kind}`", not invented,
                  f"{invented[:6]} : valeur absente des tableaux du métier — soit le wiki a été mal relu,"
                  " soit c'est un réglage à documenter comme tel (et pas comme « le barème du serveur »)")
            paired = sorted(key for key in gains if key not in xp) + sorted(key for key in xp if key not in gains)
            check(f"config : chaque matériau a son argent ET son XP pour `{kind}`", not paired,
                  f"{paired[:6]} : un tarif sans contrepartie XP signe une table recopiée à moitié")
        # --- fidelite au wiki : le document est la source, la config doit y coller ligne par ligne
        rows = wiki_rows()
        check("wiki : tableaux relus dans docs/WIKI-GENTYCOON-OUTILS.md", len(rows) >= 72,
              f"{len(rows)} lignes lues — le contrôle de fidélité serait aveugle")
        missing = []
        wrong_max = []
        wrong_unlock = []
        for row in rows:
            data = blocks.get(row["soul"])
            if not data:
                continue
            match = None
            for ability in data["abilities"]:
                if normalise_label(ability.get("label")) == normalise_label(row["label"]):
                    match = ability
                    break
            if match is None:
                missing.append(f"{row['soul']}/{row['label']}")
                continue
            if number(match.get("max-level")) != row["max-level"]:
                wrong_max.append(f"{row['soul']}/{row['label']}:{match.get('max-level')}≠{row['max-level']}")
            if max(1, int(number(match.get("unlock", 1)) or 1)) != max(1, row["unlock"]):
                wrong_unlock.append(f"{row['soul']}/{row['label']}:{match.get('unlock')}≠{row['unlock']}")
            if normalise_ability(match["type"]) != row["kernel"]:
                wrong_unlock.append(f"{row['soul']}/{row['label']}:noyau {match['type']}≠{row['kernel']}")
        check("wiki : chaque capacité du barème est dans la config", not missing, str(missing)[:240])
        check("wiki : les niveaux max copient le barème", not wrong_max, str(wrong_max)[:240])
        check("wiki : les verrous copient le barème", not wrong_unlock, str(wrong_unlock)[:240])
        kernels = {row["kernel"] for row in rows}
        check("wiki : tous les noyaux cités sont compris par le moteur",
              not (kernels - supported) if supported else True,
              f"{sorted(kernels - supported) if supported else []} : le document promet un noyau absent")
        check("config : /tools ability documente dans la commande",
              (PKG / "ToolsCommand.java").is_file()
              and '"ability"' in (PKG / "ToolsCommand.java").read_text(encoding="utf-8"),
              "les niveaux de capacité ne sont réglables que dans le menu : trop lent pour un admin")
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
            mined = bare_code(PKG / "ToolsCommand.java")
            subs = sorted(set(re.findall(r'case "([a-z]+)":', mined)))
            check("commande : les sous-commandes sont lisibles par le controleur", "give" in subs,
                  f"{subs} : aucun `case` trouve — la section commande du plugin.yml ne serait pas verifiee")
            proposed = sorted(set(re.findall(r'add\(out, "([a-z]+)"', mined)))
            check("commande : la completion est lisible par le controleur", bool(proposed),
                  "aucun `add(out, …)` trouve — la regle de Tab serait decorative")
            # un alias n'est pas une fonctionnalite cachee : il est accepte par le switch, pas propose
            alias_pairs = re.findall(r'case "([a-z]+)":\s*\n\s*case "([a-z]+)":', mined)
            # un alias n'a rien a faire dans la liste de Tab, pourvu que son jumeau canonique y soit :
            # c'est le couple qui compte, pas le sens dans lequel le switch l'a ecrit
            covered = {main for main, alias in alias_pairs if alias in proposed}
            covered |= {alias for main, alias in alias_pairs if main in proposed}
            missing = [s for s in subs if s not in proposed and s not in covered and s != "default"]
            check("commande : chaque sous-commande est proposée par le Tab", not missing,
                  f"{missing} : routées par le switch mais absentes de onTabComplete — la complétion"
                  " ment sur ce que sait faire le plugin, et une entrée cachée est invisible pour un an")
            for main, alias in alias_pairs:
                check(f"commande : `{main}`/`{alias}` ne sont pas proposes tous les deux",
                      not (main in proposed and alias in proposed),
                      "les deux écritures dans la liste de Tab = deux lignes pour la même intention")
            for sub in proposed:
                check(f"plugin.yml : l'usage mentionne `{sub}`", sub in yml,
                      "une sous-commande proposee par le Tab mais absente de `usage:` : le joueur la"
                      " decouvre par accident et l'admin ne la documente jamais")
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

    def type_of(chunk):
        """` IllegalArgumentException broken` -> `IllegalArgumentException`.

        Le nom de la variable n'est pas un type : le garder faisait chercher la filiation d'une cle
        inexistante, donc _related repondait « non lies » sur le cas precis qu'il doit refuser.
        """
        tokens = chunk.strip().replace("final ", "").split()
        if len(tokens) > 1 and tokens[-1][:1].islower():
            tokens.pop()                       # `RuntimeException e` : le dernier mot est le parametre
        return tokens[0].split(".")[-1] if tokens else ""

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
        names = [type_of(part) for part in clause.split("|") if part.strip()]
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                if related(names[i], names[j]):
                    return True
    return False
def _related_self_test():
    """Le controle des multi-catch doit voir le couple fils/peres AVEC nom de variable, et dormir sur
    les freres (`RuntimeException | LinkageError` est legal depuis toujours dans ce depot)."""
    if not _related(["RuntimeException | IllegalArgumentException broken"]):
        raise SystemExit("ERREUR: _related ne voit plus `RuntimeException | IllegalArgumentException`"
                         " — le controle qui a deja protege ce paquet est decoratif")
    if _related(["RuntimeException | LinkageError failed", "ReflectiveOperationException | RuntimeException x"]):
        raise SystemExit("ERREUR: _related flagge des freres (multi-catch parfaitement legal)")
    if not _related(["java.io.IOException | FileNotFoundException e"]):
        raise SystemExit("ERREUR: _related ne resout plus les noms qualifies (IOException et son fils"
                         " FileNotFoundException passes)")


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


def _shape_self_test():
    """Le controle de forme doit echouer sur le melange, sinon il est decoratif."""
    bad = "tools:\n  pickaxe:\n    abilities:\n      price-base: 1\n      - {id: x, type: VEIN}\n"
    ok = "tools:\n  pickaxe:\n    ability-price:\n      base: 1\n    abilities:\n      - {id: x, type: VEIN}\n"
    refused, _ = check_yaml_shape(bad)
    accepted, detail = check_yaml_shape(ok)
    if refused or not accepted:
        raise SystemExit("ERREUR: check_yaml_shape est decoratif (refuse=" + str(refused)
                         + ", accepte=" + str(accepted) + " " + detail + ")")


def main() -> int:
    """Le corps des contrôles vit dans check_all() ; main ne fait que rendre le code de sortie."""
    _shape_self_test()
    check_all()
    return report()


if __name__ == "__main__":
    sys.exit(main())
