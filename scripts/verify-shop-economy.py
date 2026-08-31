#!/usr/bin/env python3
"""Le comptoir (/shop) : ses garde-fous de prix, ses rayons, et l'ancre qui relie le tout au temps de jeu.

Trois choses sont contrôlées ici, et elles n'ont rien à voir l'une avec l'autre au premier regard :

1. **Les invariants du `shop:`** du `resources/config.yml` livré. Un achat doit rester plus cher que ce que
   le plugin rend (`sellPrice`), et le cycle « acheter puis se faire racheter » doit rester perdant. Ces deux
   règles ne laissent aucune trace visible quand on les casse : le comptoir continue de fonctionner, il
   devient juste une machine à imprimer de la monnaie. C'est donc un contrôle, pas une recommandation.

2. **Le classement en rayons** (`shop.categories`). Chaque matière crachée par un générateur doit être accueillie
   par exactement un rayon, chaque offre écrite à la main doit pointer une clef qui existe, et aucun rayon ne
   doit déborder de sa page sans qu'on le sache. Un classement n'est pas une décoration : c'est ce qui fait
   qu'un joueur trouve la terre ou le netherite en trois clics au lieu de faire défiler vingt-huit lignes.

3. **La disposition, relue dans le code.** Le nombre d'onglets affichables et la taille d'une page ne sont pas
   recopiés dans ce script : ils sont résolus dans `ShopGui.java` (`OFFERS_PER_PAGE = OFFER_ROWS * 9`,
   `OFFER_ROWS = ROWS - 2`). Un plafond recopié est un plafond qui ne bouge plus quand l'interface bouge — et
   un contrôle qui retombe sur une valeur de complaisance quand la constante change valide n'importe quoi.

4. **L'extrait à coller.** `docs/ECONOMIE.md` donne un bloc `shop:` à coller sur un serveur qui tourne déjà ;
   le plugin, lui, ne recopie jamais un `config.yml` existant. L'extrait est donc comparé au fichier livré,
   rayons, matières et lignes d'exemple compris : un document périmé ne casse rien en jeu, il fait perdre une
   après-midi.

5. **L'ancre de durée.** `sellPrice / speed × 3600` donne le revenu d'un générateur à l'heure, et le nombre de
   générateurs par joueur est borné par `limits.per-player.default-limit`. En divisant le coût d'une âme maxée
   (lu dans le `config.yml` des outils, mêmes formules que le moteur) par ce revenu, on obtient le temps de
   jeu que demande une âme. Le band [300, 900] h est la règle d'équilibre retenue (« maxer une âme ≈ une
   saison de tycoon de milieu de partie ») : `GRIND`, dans `scripts/gen-tools-config.py`, est la seule poignée
   qui le règle — et rien d'autre ne la protège d'une retouche enthousiaste.

Le bloc de tableaux dans `docs/ECONOMIE.md` (balises `bareme-comptoir`) est **généré** par ce script :

    python3 scripts/verify-shop-economy.py            # controler
    python3 scripts/verify-shop-economy.py --write    # regenerer le bloc du document
    python3 scripts/verify-shop-economy.py --hours 20000 200000   # hypothese de revenu
"""
from __future__ import annotations

import importlib.util
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG = os.path.join(ROOT, "resources", "config.yml")
MIRROR_CONFIG = os.path.join(ROOT, "artifacts", "extracted", "config.yml")
PLUGIN_YML = os.path.join(ROOT, "resources", "plugin.yml")
POM = os.path.join(ROOT, "pom.xml")
SHOP_SOURCE = os.path.join(ROOT, "sources", "plugin", "xyz", "arcadiadevs", "valoriatycoon",
                           "guis", "ShopGui.java")
HOOK_SOURCE = os.path.join(ROOT, "sources", "plugin", "xyz", "arcadiadevs", "valoriatycoon",
                           "commands", "SellCommandListener.java")
ECONOMIE_DOC = os.path.join(ROOT, "docs", "ECONOMIE.md")
TOOLS_DOC = os.path.join(ROOT, "docs", "MULTI-OUTIL.md")
# L'info-bulle d'un onglet grandit, elle ne se coupe pas : le client replie ce qui depasse. Ces deux bornes
# ne sont donc pas une mesure de la police — c'est le seuou une annonce devient un paragraphe qu'on ne lit plus.
MAX_DESC_LINES = 6
MAX_DESC_CHARS = 76

MARK_BEGIN = "<!-- bareme-comptoir:debut -->"
MARK_END = "<!-- bareme-comptoir:fin -->"
QUOTES = "'\""
# Nom des matieres que l'aplatissage 1.13 a renommees : les laisser dans une config livree, c'est une entree
# que le comptoir ignore silencieusement au chargement (elle n'apparait jamais dans un rayon).
LEGACY_MATERIALS = {"WOOD", "LOG", "LEAVES", "SAPLING", "PLANKS", "DIRT_WITH_GRASS", "IRON_BLOCK_LEGACY",
                    "SKULL_ITEM", "MONSTER_EGG", "FISH", "MELON_BLOCK", "REEDS", "SEEDS", "PUMPKIN_STEM",
                    "NETHER_BRICK_ITEM", "CLAY_BRICK", "BRICK_STAIR", "STEP"}
problems = []
notes = []


def check(label, ok, detail=""):
    if ok:
        notes.append(f"OK   {label}")
    else:
        problems.append(f"{label}" + (f" — {detail}" if detail else ""))


def read_text(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def number(value):
    try:
        return float(str(value).replace(",", "."))
    except (TypeError, ValueError):
        return None


def unquote(value):
    value = str(value).strip()
    if len(value) >= 2 and value[0] in QUOTES and value[-1] == value[0]:
        value = value[1:-1]
    return value.strip()


def dec(value):
    """Un nombre en francaise : une virgule, pas un point (le texte autour est lu par un humain)."""
    text = ("%s" % (round(float(value), 2),)).rstrip("0").rstrip(".")
    return text.replace(".", ",")


def decolor(value):
    return re.sub(r"(?i)&[0-9a-fk-or]", "", str(value)).strip()


# --------------------------------------------------------------------------- lecture du YAML


def join_flow(text):
    """Recolle les lignes d'une collection flux (`[A, B,` … `C]`) avant de lire quoi que ce soit.

    Le `config.yml` livre des `materials:` sur trois lignes pour rester lisible a l'editeur : sans ce
    recollement, un parseur ligne a ligne verrait un scalaire coupe, conclurait « matiere non classee » la
    ou il n'y a qu'un pliage, et ferait echouer la construction pour un choix de mise en page.
    """
    out = []
    buffer = None
    depth = 0
    for raw in text.splitlines():
        delta = raw.count("[") + raw.count("{") - raw.count("]") - raw.count("}")
        buffer = (buffer + " " + raw.strip()) if buffer is not None else raw
        depth += delta
        if depth <= 0:
            out.append(buffer)
            buffer = None
            depth = 0
    if buffer is not None:
        out.append(buffer)
    return out


def split_flow(body):
    """Decoupe une liste flux sur les virgules **hors** guillemets : `name: "a, b"` reste une valeur."""
    parts, current, quote = [], "", None
    for char in body:
        if quote:
            current += char
            if char == quote:
                quote = None
            continue
        if char in QUOTES:
            quote = char
            current += char
            continue
        if char == ",":
            parts.append(current)
            current = ""
            continue
        current += char
    parts.append(current)
    return parts


def flow_value(value):
    value = str(value).strip()
    if value.startswith("[") and value.endswith("]"):
        return [flow_value(item) for item in split_flow(value[1:-1]) if item.strip()]
    if len(value) >= 2 and value[0] in QUOTES and value[-1] == value[0]:
        inner = value[1:-1]
        return inner.replace("''", "'") if value[0] == "'" else inner
    return value


def flow_map(text):
    """Un `{ cle: valeur, cle: "chaine, avec virgule" }` en dict."""
    body = text.strip()
    if body.startswith("{"):
        body = body[1:]
    if body.endswith("}"):
        body = body[:-1]
    out = {}
    for part in split_flow(body):
        if not part.strip():
            continue
        key, _colon, value = part.partition(":")
        out[key.strip()] = flow_value(value.strip())
    return out


# --------------------------------------------------------------------------- l'extrait a coller

EXCERPT_EXTRAS = 3      # lignes d'`extras:` que le document donne en exemple, le reste est a l'identique


def listify(value):
    """Une clef lue en liste, qu'elle ait ete ecrite en liste ou en une seule chaine."""
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item).strip() for item in value]
    return [str(value).strip()]


def excerpt_block(text):
    """Le bloc `shop:` du fichier, tel qu'il doit etre colle — `extras:` réduit à ses trois premieres lignes.

    Le document ne recopie pas ce bloc a la main pour une raison simple : il a deja ete perimé deux fois (les
    rayons absents d'un cote, un facteur de marge oublie de l'autre). Ici il est decoupe du fichier livré, et
    `check_all` compare ce qui reste ligne a ligne — donc un extrait qui derive est une construction rouge.
    """
    lines = text.split("\n")
    try:
        start = lines.index("shop:")
    except ValueError:
        raise SystemExit("ERREUR: pas de bloc `shop:` dans resources/config.yml — le document n'a rien a "
                         "donner a coller.")
    # L'en-tete commente au-dessus de `shop:` fait partie de l'extrait: c'est elle qui explique les clefs.
    head = start
    while head > 0 and lines[head - 1].lstrip().startswith("#"):
        head -= 1
    # `shop:` lui-meme est une clef racine: la condition d'arret ne doit pas le manger.
    out, extras, kept = lines[head:start] + ["shop:"], 0, 0
    for line in lines[start + 1:]:
        if line.strip() and not line.startswith(" "):
            break                                     # prochaine clef racine: `shop:` est fini
        if line.strip() == "extras:":
            out.append(line)
            extras = 1
            continue
        if extras:
            stripped = line.strip()
            if stripped.startswith("- "):
                kept += 1
                if kept > EXCERPT_EXTRAS:
                    out.append("    # … et %d autres lignes, exactement de la meme facture"
                               % (count_extras(lines) - EXCERPT_EXTRAS))
                    break
            out.append(line)
            continue
        out.append(line)
    return "\n".join(out).rstrip()


def count_extras(lines):
    """Combien de lignes d'`extras:` le fichier contient (pour que l'ellipse du document soit un chiffre vrai)."""
    inside, total = False, 0
    for line in lines:
        if line.strip() == "extras:":
            inside = True
            continue
        if inside:
            if line.strip() and not line.startswith(" "):
                break
            if line.strip().startswith("- "):
                total += 1
    return total


def splice_excerpt(doc, block):
    """Remplace, dans le document, le bloc `yaml` qui contient `shop:` par `block`."""
    # Meme recherche que `doc_excerpt` — et pas une regex qui part du ` ```yaml ` : le bloc est parse de
    # commentaires qui citent `sellPrice` avec des accents graves, et une classe `[^`]` s'y arrete.
    for found in re.finditer(r"```(?:yaml|yml)?\n(.*?)```", doc, re.S):
        if re.search(r"^shop:$", found.group(1), re.M):
            return doc[:found.start()] + "```yaml\n" + block + "\n```" + doc[found.end():]
    raise SystemExit("ERREUR: aucun bloc `yaml` contenant `shop:` dans docs/ECONOMIE.md — l'extrait a "
                     "coller n'a nulle part où être écrit.")


def doc_excerpt(doc):
    """Le bloc `shop:` que `docs/ECONOMIE.md` donne à coller, ou une chaîne vide.

    L'extrait est le seul chemin par lequel un serveur déjà installé obtient les rayons : le plugin ne recopie
    jamais un `config.yml` existant (il écraserait le travail de l'admin). Un extrait périmé ne casse donc rien
    en jeu — mais il laisse croire que le classement dépend de la version du jar, alors qu'il dépend du fichier.
    """
    for block in re.findall(r"```(?:yaml|yml)?\n(.*?)```", doc, re.S):
        found = re.search(r"^shop:$", block, re.M)
        if found:
            return block[found.start():]
    return ""


def read_config(text, probe=False):
    """Le bloc `generators:`, les scalaires de `shop:`, ses `categories:` et ses `extras:`.

    PyYAML n'est pas garanti sur le runner (le workflow n'installe que python3 et Maven), et une lecture
    ligne a ligne est plus sure qu'une regex multiligne sur un fichier edite a la main. Un controle qui ne
    lit rien est plus dangereux qu'un controle absent : il VALIDE n'importe quoi. D'ou `self_test`, qui
    verifie que le parseur voit le fichier AVANT de le croire.
    """
    generators, shop, categories, extras = [], {}, [], []
    mode, current, bucket = None, None, None
    for raw in join_flow(text):
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        indent = len(raw) - len(raw.lstrip(" "))
        stripped = raw.strip()
        if indent == 0 and stripped.endswith(":"):
            key = stripped[:-1]
            mode = key if key in ("generators", "shop") else None
            current, bucket = None, None
            continue
        if mode == "generators":
            # Seule une entree de liste a l'indentation de la liste ouvre un generateur : les lignes
            # `- "&e%name%"` de `hologramLines:` sont plus profondes et ne sont pas des entrees.
            if indent == 2 and stripped.startswith("- "):
                current = {}
                generators.append(current)
                stripped = stripped[2:].strip()
            if current is None or indent not in (2, 4) or ":" not in stripped:
                continue
            key, _colon, value = stripped.partition(":")
            current[key.strip()] = flow_value(re.sub(r"\s+#.*$", "", value.strip()))
            continue
        if mode != "shop":
            continue
        if indent == 2 and stripped.endswith(":") and not stripped.startswith("- "):
            name = stripped[:-1]
            bucket = extras if name == "extras" else categories if name == "categories" else None
            current = None
            continue
        if indent == 4 and stripped.startswith("- "):
            body = stripped[2:].strip()
            row = flow_map(body) if body.startswith("{") else {}
            if not body.startswith("{") and ":" in body:
                key, _colon, value = body.partition(":")
                row[key.strip()] = flow_value(re.sub(r"\s+#.*$", "", value.strip()))
            (bucket if bucket is not None else extras).append(row)
            current = row
            continue
        if current is not None and indent >= 6 and ":" in stripped:
            key, _colon, value = stripped.partition(":")
            current[key.strip()] = flow_value(re.sub(r"\s+#.*$", "", value.strip()))
            continue
        if ":" not in stripped:
            continue
        key, _colon, value = stripped.partition(":")
        shop[key.strip()] = flow_value(re.sub(r"\s+#.*$", "", value.strip()))
    self_test(text, generators, shop, categories, extras, probe)
    return generators, shop, categories, extras


def self_test(text, generators, shop, categories, extras, probe=False):
    """Le parseur doit voir ce que le fichier declare, sinon tout le controle est decoratif."""
    rows = len(re.findall(r"^  - name:", text, re.M))
    # Un extrait de documentation ne porte pas la table des generateurs : on ne lui demande pas ce qu'il ne
    # contient pas, mais on continue d'exiger qu'il soit lu entier (rayons et extras comptes ci-dessous).
    if not probe and (len(re.findall(r"^generators:\s*$", text, re.M)) != 1 or len(generators) != rows
            or rows < 20):
        raise SystemExit(f"ERREUR: le parseur voit {len(generators)} generateur(s) pour {rows} lignes "
                         "`  - name:` declarees — le controle produirait des faux negatifs, corrige-le "
                         "avant de te fier au reste.")
    read_scalars = len([k for k in shop if k not in ("extras", "categories")])
    if read_scalars < (1 if probe else 9):
        raise SystemExit(f"ERREUR: la section `shop:` est lue avec {read_scalars} cle(s) — le parseur ne "
                         "voit pas le bloc, il ne faut pas le croire.")
    declared_extras = len(re.findall(r"^    - (?:material:|\{ material:)", text, re.M))
    if declared_extras != len(extras):
        raise SystemExit(f"ERREUR: {len(extras)} entree(s) `shop.extras` lues pour {declared_extras} "
                         "declarees — la partie `extras` du controle serait decorative.")
    declared_cats = len(re.findall(r"^    - (?:key: |\{ key: )", text, re.M))
    if declared_cats != len(categories):
        raise SystemExit(f"ERREUR: {len(categories)} rangee(s) lue(s) pour {declared_cats} declarees — les "
                         "rayons du comptoir ne seraient pas controles.")
    if not categories and not probe:
        raise SystemExit("ERREUR: aucune rangee lue alors que `shop.categories` est ecrit dans le fichier "
                         "livre — le controle des rayons serait decoratif.")
    routed = [m for row in categories for m in (row.get("materials") or [])]
    declared_routed = 0
    for line in join_flow(text):
        if line.strip().startswith("materials: ["):
            declared_routed += len(flow_value(line.partition(":")[2].strip()))
    if len(routed) != declared_routed:
        raise SystemExit(f"ERREUR: {len(routed)} matieres routees lues pour {declared_routed} declarees — "
                         "le pliage des listes flux est casse, le controle des rayons serait faux.")


# --------------------------------------------------------------------------- formats


def money(value):
    if value >= 1e9:
        return "%.2f Md$" % (value / 1e9)
    if value >= 1e6:
        return "%.2f M$" % (value / 1e6)
    if value >= 1e3:
        return "%.1f k$" % (value / 1e3)
    return "%.0f $" % value


def group(value):
    return f"{value:,.0f}".replace(",", " ")


def exact(value):
    """Un prix tel qu'il est calcule : deux decimales au plus, pas d'arrondi qui fait mentir le tableau."""
    text = "%.2f" % round(float(value), 2)
    text = text.rstrip("0").rstrip(".")
    whole, dot, frac = text.partition(".")
    if whole.lstrip("-").isdigit():
        whole = "{:,}".format(int(whole)).replace(",", " ")
    return whole + (dot + frac if frac else "") + " $"


# --------------------------------------------------------------------------- constantes du GUI


def gui_constants():
    """Les entiers de disposition relus dans `ShopGui.java` : le controle suit l'interface, pas l'inverse.

    Les valeurs sont resolues en chaine en chaine (`OFFERS_PER_PAGE = OFFER_ROWS * 9`, `OFFER_ROWS =
    ROWS - 2`) plutot que recopiees ici : un controle qui reimplemente la moitie du compilateur est un
    controle qui laisse passer les incoherences qu'il pretend surveiller. Seules les formes `N`, `A - N`,
    `A * B` sont reconnues ; toute autre ecriture rend la valeur absente, et les controles qui s'y appuient
    le disent au lieu de partir d'un 0 invente.
    """
    source = read_text(SHOP_SOURCE)
    names = {}
    for name in ("ROWS", "MAX_TABS", "OFFER_ROWS", "OFFERS_PER_PAGE"):
        found = re.search(r"private static final int %s = ([^;]+);" % name, source)
        names[name] = found.group(1).strip() if found else None

    def resolve(name, depth=0):
        """`N`, `A - N`, `A * B`, `A * 9` — et rien d'autre ; sinon None, jamais un 0 de complaisance."""
        raw = names.get(name)
        if raw is None or depth > 4:
            return None
        expr = raw.replace(" ", "")
        if re.fullmatch(r"\d+", expr):
            return int(expr)
        atom = r"([A-Za-z0-9_]+)"
        product = re.fullmatch(atom + "\*" + atom, expr)
        if product:
            sides = []
            for token in product.groups():
                sides.append(int(token) if token.isdigit() else resolve(token, depth + 1))
            return sides[0] * sides[1] if None not in sides else None
        # `ROWS - 2` : la rangee d'onglets et la rangee de reglages sont retirees de la meme facon.
        offset = re.fullmatch(atom + r"([+-])" + r"(\d+)", expr)
        if offset:
            left = resolve(offset.group(1), depth + 1)
            if left is None:
                return None
            return left + int(offset.group(3)) if offset.group(2) == "+" else left - int(offset.group(3))
        return None

    return {name: resolve(name) for name in names}


def gui_layout():
    """La meme chose, en dict `rows` / `max-tabs` / `offer-rows` / `offers-per-page` (0 = pas lu)."""
    constants = gui_constants()
    return {"rows": constants["ROWS"] or 0, "max-tabs": constants["MAX_TABS"] or 0,
            "offer-rows": constants["OFFER_ROWS"] or 0,
            "offers-per-page": constants["OFFERS_PER_PAGE"] or 0}


# --------------------------------------------------------------------------- reglages hors `shop:`

ROOT_SETTINGS = {
    "join-amount": "on-join.generator-amount",
    "join-tier": "on-join.generator-tier",
    "limit-enabled": "limits.per-player.enabled",
    "limit": "limits.per-player.default-limit",
    "listing-fee": "auction-house.listing-fee",
    "sales-tax": "auction-house.sales-tax",
}


def scalar_at(lines, path):
    """Le scalaire écrit a ce chemin (`a.b.c`) dans un YAML en bloc, ou None.

    Descend d'une clef a la fois en exigeant que l'enfant soit plus indentes que son parent : c'est ce que
    `YamlConfiguration#getDouble("limits.per-player.default-limit")` fait, et un controle qui lirait la
    premiere ligne contenant `default-limit:` irait chercher la valeur d'un autre bloc.
    """
    indent, block, found = 0, list(lines), None
    for depth, key in enumerate(path.split(".")):
        pattern = re.compile(r"^( {%d})%s\s*:\s*(.*)$" % (indent, re.escape(key)))
        position = None
        for index, line in enumerate(block):
            got = pattern.match(line)
            if got:
                found, position = got.group(2).strip(), index
                break
        if position is None:
            return None
        if depth == len(path.split(".")) - 1:
            return flow_value(re.sub(r"\s+#.*$", "", found))
        child = []
        for line in block[position + 1:]:
            if line.strip() and not line.lstrip().startswith("#"):
                if len(line) - len(line.lstrip(" ")) <= indent:
                    break
            child.append(line)
        indent += 2
        block = child
    return None


def read_settings(text):
    """Les quelques clefs hors `shop:` que l'ancre de duree et les controles du marche reutilisent."""
    lines = join_flow(text)
    return {name: scalar_at(lines, path) for name, path in ROOT_SETTINGS.items()}


def income_per_hour(gen):
    speed = number(gen.get("speed")) or 0.0
    sell = number(gen.get("sellPrice")) or 0.0
    return sell / speed * 3600.0 if speed > 0 else 0.0


def soul_total():
    """Ce que coûte une âme entièrement maxée : paliers + toutes les capacités de tous les niveaux.

    Les formules ne sont pas recopiees ici : elles sont lues la ou elles vivent, dans
    `scripts/longevity-tools-config.py`, deja seul auteur des tableaux de `docs/MULTI-OUTIL.md`. Un controle
    qui reinvente la formule est un controle qui la derive.
    """
    spec = importlib.util.spec_from_file_location(
        "longevity", os.path.join(ROOT, "scripts", "longevity-tools-config.py"))
    longevity = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(longevity)
    souls = longevity.read_souls(os.path.join(ROOT, "resources-tools", "config.yml"))
    out = {}
    for kind, soul in souls.items():
        total = sum(longevity.tier_price(soul, tier) for tier in range(2, int(soul["max-tier"]) + 1))
        for ability in soul["abilities"]:
            total += longevity.ability_total(soul, ability)
        out[kind] = total
    return out


def shelves_of(generators, categories, extras, shop):
    """Les rayons tels que le code les monte : matieres de generateur routees + offres ecrites a la main."""
    multiplier = number(shop.get("buy-multiplier")) or 1.0
    ratio = number(shop.get("buyback-ratio")) or 0.0
    buyback_on = str(shop.get("buyback-enabled", "true")).lower() == "true"
    by_material = {}
    shelves = []
    for row in categories:
        shelf = {"key": str(row.get("key") or "").strip().lower(), "title": decolor(row.get("name", "")),
                 "icon": str(row.get("icon") or "").strip().upper(),
                 "materials": [str(m).strip().upper() for m in (row.get("materials") or [])], "offers": []}
        for material in shelf["materials"]:
            by_material.setdefault(material, shelf)
        shelves.append(shelf)
    # Meme repli que `ShopGui.ensureLoaded` : une rangee `divers` declaree dans le fichier EST le panier de
    # secours (titre, icone, description et matieres compris) — pas un second onglet jumeau monte en douce.
    declared = next((sh for sh in shelves if sh["key"] == "divers"), None)
    divers = declared if declared is not None else {"key": "divers", "fallback": True,
            "title": decolor(shop.get("extras-category", "Divers")), "icon": "CHEST",
            "materials": [], "offers": []}
    gens = {}
    for gen in generators:
        material = str(gen.get("spawnItem") or "").strip().upper()
        gens[material] = gen
    for material, gen in sorted(gens.items(), key=lambda item: number(item[1].get("tier")) or 0):
        sell = number(gen.get("sellPrice")) or 0.0
        target = by_material.get(material)
        if target is None:
            target = divers
        target["offers"].append({
            "tier": int(number(gen.get("tier")) or 0), "material": material, "sell": sell,
            "buy": round(sell * multiplier, 2),
            "buyback": round(round(sell * multiplier, 2) * ratio, 2) if buyback_on and ratio else 0.0,
            "income": income_per_hour(gen), "price": number(gen.get("price")) or 0.0,
            "speed": number(gen.get("speed")) or 0, "hand": False})
    contradictions = []
    for row in extras:
        material = str(row.get("material") or "").strip().upper()
        buy = number(row.get("buy")) or 0.0
        # Meme precedence que `ShopGui.readExtras` : `category` d'abord, puis le rayon qui reclame deja la
        # matiere dans `materials:`. Ranger autrement compterait des offres dans des cases que personne clique.
        claimed = by_material.get(material)
        category = str(row.get("category") or "").strip().lower()
        target = next((sh for sh in shelves if sh["key"] == category), None) if category else None
        if target is None:
            target = claimed
        elif target is not claimed and claimed is not None:
            contradictions.append((material, category, claimed["key"]))
        if target is None:
            target = divers
        target["offers"].append({
            "tier": 0, "material": material, "sell": 0.0, "buy": round(buy, 2),
            "buyback": round(number(row.get("sellback")) or 0.0, 2), "income": 0.0, "price": 0.0,
            "speed": 0, "hand": True})
    if divers["offers"] and declared is None:
        shelves.append(divers)
    return shelves, gens, contradictions


# --------------------------------------------------------------------------- prix des generateurs

PAYBACK_FIRST = 1.0     # heures de son propre rendement pour rembourser le palier 1
PAYBACK_LAST = 4.5      # ... et le dernier palier de la table
PAYBACK_FLOOR, PAYBACK_CEILING = 0.75, 6.0     # bande admise, controlee : la regle doit rester lisible


def payback_target(tier, tiers):
    """Le nombre d'heures de rendement qu'un palier doit rembourser (croissance geometrique).

    Geometrique et pas lineaire parce que le revenu, lui, croite tres vite : une cible lineaire laisserait les
    premiers paliers chers pour ce qu'ils rapportent (le joueur decouvre, on ne le facture pas deux fois) et
    les derniers negligeables. Un facteur constant entre le premier et le dernier palier garde l'ecart lisible
    — ici 1 h au palier 1, 4,5 h au palier 28, soit le mur du fond sans le rendre decourageant.
    """
    span = max(1, len(tiers) - 1)
    index = tiers.index(tier) if tier in tiers else 0
    return PAYBACK_FIRST * (PAYBACK_LAST / PAYBACK_FIRST) ** (index / float(span))


def nice_price(amount):
    """Arrondit a un nombre que l'ecriture d'un config.yml supporte : 100, 1 000 ou 10 000 selon la taille."""
    for step in (100, 1_000, 10_000):
        if amount < step * 100:
            return int(round(amount / float(step)) * step)
    return int(round(amount / 10_000.0) * 10_000)


def rule_prices(generators):
    """Le prix que la regle donne pour chaque palier, en dict `tier -> prix`."""
    tiers = sorted(int(number(g.get("tier")) or 0) for g in generators)
    out = {}
    for gen in generators:
        tier = int(number(gen.get("tier")) or 0)
        income = income_per_hour(gen)
        if income <= 0.0:
            continue
        out[tier] = nice_price(income * payback_target(tier, tiers))
    return out


def write_prices(text):
    """Reecrit les lignes `price:` du bloc `generators:` selon `rule_prices` (seul endroit qui les decide)."""
    lines = text.split("\n")
    prices = rule_prices(read_config(text)[0])
    inside, changed, report = False, 0, []
    tier = None
    for index, line in enumerate(lines):
        if line == "generators:":
            inside = True
            continue
        if inside and line and not line.startswith(" "):
            break
        if not inside:
            continue
        if re.match(r"^  - name:", line):
            tier = None
        found = re.match(r"^(    tier: )(.+)$", line)
        if found:
            tier = int(number(found.group(2)))
            continue
        found = re.match(r"^(    price: )([^\n#]*?)(\s*(?:#.*)?)$", line)
        if found and tier in prices:
            wanted = "%s.0" % prices[tier] if float(prices[tier]).is_integer() else str(prices[tier])
            old = found.group(2).strip()
            if old != wanted:
                lines[index] = "%s%s%s" % (found.group(1), wanted, found.group(3))
                changed += 1
                report.append("  palier %-2d : %12s -> %12s" % (tier, old, wanted))
    return "\n".join(lines), report, changed


def table_rows(generators, shop):
    rows = []
    by_tier = {}
    for gen in generators:
        by_tier[int(number(gen.get("tier")) or 0)] = gen
    for tier in (1, 2, 3, 4, 5, 6, 10, 14, 20, 28):
        gen = by_tier.get(tier)
        if gen is None:
            continue
        sell = number(gen.get("sellPrice")) or 0.0
        rows.append({"tier": tier, "material": str(gen.get("spawnItem") or "").upper(), "sell": sell,
                     "buy": round(sell * (number(shop.get("buy-multiplier")) or 1.0), 2), "income":
                     income_per_hour(gen), "price": number(gen.get("price")) or 0.0,
                     "speed": number(gen.get("speed")) or 0})
    return rows


def anchor_rates(shop, generators, settings=None):
    """Les trois revenus de reference du serveur, calcules avec les reglages **actifs** du fichier.

    `limits.per-player.default-limit` remplace le 20 recopie, `on-join.generator-amount` le 3 : une ancre qui
    supposerait un plafond que le `config.yml` a desactive documenterait une economie qui n'existe pas.
    """
    settings = settings or {}
    by_tier = {}
    for gen in generators:
        by_tier[int(number(gen.get("tier")) or 0)] = gen
    join = int(number(settings.get("join-amount")) or 3)
    cap = int(number(settings.get("limit")) or 20)
    joiner = join * (income_per_hour(by_tier[1]) if 1 in by_tier else 0.0)
    mid = cap * (income_per_hour(by_tier[10]) if 10 in by_tier else 0.0)
    maxed = cap * (income_per_hour(by_tier[max(by_tier)]) if by_tier else 0.0)
    return joiner, mid, maxed


def render_block(generators, shop, categories, extras, hours):
    shelves, gens, _contradictions = shelves_of(generators, categories, extras, shop)
    by_tier = dict((row["tier"], row) for row in table_rows(generators, shop))
    shelf_of = {}
    for shelf in shelves:
        for offer in shelf["offers"]:
            shelf_of.setdefault(offer["material"], shelf["title"])
    multiplier = number(shop.get("buy-multiplier")) or 1.0
    ratio = number(shop.get("buyback-ratio")) or 0.0
    buyback_on = str(shop.get("buyback-enabled", "true")).lower() == "true"
    layout = gui_layout()

    out = [MARK_BEGIN,
           "> Bloc genere par `python3 scripts/verify-shop-economy.py --write`. Ne pas editer a la main : les",
           "> chiffres du document sont relus depuis `resources/config.yml`, pas recopies d'une tete bien faite.",
           "",
           "| palier | rayon | matière | rendu | `sellPrice` (via `/sell`) | achat comptoir | prix du générateur | amorti en |",
           "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    for row in table_rows(generators, shop):
        payback = row["price"] / row["income"] if row["income"] else 0.0
        buyback = round(row["buy"] * ratio, 2) if buyback_on and ratio else 0.0
        out.append("| %d | %s | %s | 1 toutes les %d s | %s | **%s** (reprise %s) | %s | %.2f h |" % (
            row["tier"], shelf_of.get(row["material"], "divers"), row["material"], row["speed"],
            exact(row["sell"]), exact(row["buy"]), exact(buyback) if buyback else "—", exact(row["price"]),
            payback))

    tiers = sorted(by_tier)
    out += ["",
            "**La colonne « amorti en » n'est pas un constat, c'est la règle** : le prix d'un palier vaut de "
            "%s à %s de son propre rendement, en croissance geometrique, du premier au dernier palier de la "
            "table. La regle vit dans `scripts/verify-shop-economy.py` et `python3 "
            "scripts/verify-shop-economy.py --prices` la posee dans `resources/config.yml` ; le verificateur "
            "rouge des qu'un des deux bords sort de %s à %s, ou des qu'un prix du fichier ne vient plus de la "
            "regle." % (dec(PAYBACK_FIRST) + " h", dec(PAYBACK_LAST) + " h", dec(PAYBACK_FLOOR) + " h",
                        dec(PAYBACK_CEILING) + " h"),
            ""]
    out += ["", "**Les rayons** — ce que le comptoir presente, onglet par onglet :", "",
            "| rayon | icône | matières listées | offres de générateur | offres écrites | total | pages |",
            "| --- | --- | --- | --- | --- | --- | --- |"]
    per_page = layout["offers-per-page"] or 36
    for shelf in shelves:
        routed = len(shelf["materials"])
        gen_offers = len([o for o in shelf["offers"] if not o["hand"]])
        hand_offers = len([o for o in shelf["offers"] if o["hand"]])
        total = len(shelf["offers"])
        out.append("| %s | `%s` | %d | %d | %d | **%d** | %d |" % (
            shelf["title"] or shelf["key"], shelf["icon"], routed, gen_offers, hand_offers, total,
            1 + max(0, total - 1) // per_page))

    out += ["",
            "> « matières listées » (`shop.categories[].materials`) ne vend rien : c'est la liste des matières "
            "que le rayon **réclame**. Une ligne de `generators:` dont le `spawnItem` y figure atterrit ici toute "
            "seule ; les autres n'y sont que pour la prochaine ligne qu'un admin ajoutera. Les offres payables, ce "
            "sont les %d lignes venues du générateur et les %d lignes écrites à la main dans `shop.extras`." % (
                sum(len([o for o in shelf["offers"] if not o["hand"]]) for shelf in shelves),
                sum(len([o for o in shelf["offers"] if o["hand"]]) for shelf in shelves)),
            "",
            "> Un rayon qui dépasse %d offres ne disparaît pas : il se **page** — les offres se partagent %d "
            "rangées de neuf cases, les flèches sont posées sous la ligne d'onglets. %d rayons au plus tiennent "
            "dans cette ligne ; au-delà, le surplus n'est pas cliquable et le log du serveur le dit." % (
                per_page, layout["offer-rows"] or 4, layout["max-tabs"] or 9)]
    joiner, mid, maxed = hours["rates"]
    join, cap = hours["join"], hours["cap"]
    out += ["",
            "- Un arrivant (%d générateurs du palier 1, cf. `on-join.generator-amount`) : **%s $/h** — le "
            "salaire d'une journée de jeu, pas un pactole." % (join, group(joiner)),
            "- Un tycoon de milieu de partie (%d × palier 10, plafond `limits.per-player`) : **%s**." % (
                cap, money(mid) + "/h"),
            "- Un tycoon maxé (%d × palier %d) : **%s**." % (
                cap, max(int(t) for t in by_tier), money(maxed) + "/h"),
            "- Maxer **l'âme de pioche** (ses quarante-neuf paliers et ses vingt-quatre capacités) coûte "
            "%s, soit **%s h** du tycoon de milieu de partie (bande admise : %.0f–%.0f h, contrôlée par ce "
            "script). `docs/MULTI-OUTIL.md` reprend la même division pour les quatre âmes." % (
                money(hours["pickaxe"]), group(hours["pickaxe_hours"]), SOUL_BAND[0], SOUL_BAND[1]),
            "",
            tail_line(shop)]
    out.append(MARK_END)
    return "\n".join(out)


def tail_line(shop):
    multiplier = number(shop.get("buy-multiplier")) or 1.0
    ratio = number(shop.get("buyback-ratio")) or 0.0
    buyback_on = str(shop.get("buyback-enabled", "true")).lower() == "true"
    surcharge = "+%.0f %% sur le prix `/sell`" % ((multiplier - 1.0) * 100.0)
    if buyback_on and ratio > 0:
        return ("Le prix d'achat n'est pas une ligne libre : c'est `sellPrice × buy-multiplier`, soit %s. Un "
                "aller-retour achat puis reprise laisse donc %.0f %% de perte : le comptoir encaisse, il ne "
                "distribue pas." % (surcharge, (1.0 - multiplier * ratio) * 100.0))
    return ("Le prix d'achat n'est pas une ligne libre : c'est `sellPrice × buy-multiplier`, soit %s. La "
            "reprise est coupée : ce que le comptoir vend ne revient que par `/sell`." % surcharge)


def splice(text, block, path):
    if MARK_BEGIN not in text or MARK_END not in text:
        raise SystemExit(f"ERREUR: balises `{MARK_BEGIN}` absentes de {os.path.basename(path)} — le bloc "
                         "généré n'a nulle part où être écrit.")
    start = text.index(MARK_BEGIN)
    end = text.index(MARK_END, start) + len(MARK_END)
    return text[:start] + block + text[end:]


# --------------------------------------------------------------------------- controles


SOUL_BAND = (300.0, 900.0)
ANCHOR_TIER = 10


def check_all(generators, shop, categories, extras, hours, settings):
    layout = gui_layout()
    check("la disposition de l'inventaire est relue dans le code, sans valeur inventee",
          all(layout[key] for key in ("rows", "max-tabs", "offer-rows", "offers-per-page")),
          f"lu {layout} — `ShopGui.java` a renomme une constante, un controle qui tombe sur un repli "
          "validait n'importe quelle grille")
    multiplier = number(shop.get("buy-multiplier"))
    ratio = number(shop.get("buyback-ratio"))
    max_lot = number(shop.get("max-per-transaction"))
    amounts = flow_value(shop.get("amounts", "[]")) if isinstance(shop.get("amounts"), str) \
        else shop.get("amounts") or []
    amounts = [int(number(a) or 0) for a in amounts]
    enabled = str(shop.get("enabled", "true"))
    buyback_on = str(shop.get("buyback-enabled", "true")).lower() == "true"
    product = (multiplier or 0.0) * (ratio if buyback_on else 0.0)

    # ---- la duree qu'on facture, et ce qui la borne
    prices = rule_prices(generators)
    tiers = sorted(prices)
    paybacks = {}
    for gen in generators:
        tier = int(number(gen.get("tier")) or 0)
        income = income_per_hour(gen)
        price = number(gen.get("price")) or 0.0
        if income > 0.0:
            paybacks[tier] = price / income
    written = {}
    for gen in generators:
        written[int(number(gen.get("tier")) or 0)] = number(gen.get("price")) or 0.0
    for tier in tiers:
        check(f"palier {tier} : son prix vient de la regle d'amorti", written.get(tier) == prices[tier],
              f"le fichier dit {group(written.get(tier, 0.0))} $, la regle donne {group(prices[tier])} $ — "
              "`python3 scripts/verify-shop-economy.py --prices` reecrit les 28 lignes, on ne les corrige "
              "pas a la main (la meme raison que pour `resources-tools/config.yml`)")
    band = "%s à %s h" % (dec(PAYBACK_FLOOR), dec(PAYBACK_CEILING))
    check(f"amorti du premier palier dans la bande ({band})",
          PAYBACK_FLOOR <= paybacks.get(tiers[0], 0.0) <= PAYBACK_CEILING,
          f"{paybacks.get(tiers[0], 0.0):.2f} h — en dessous, la montee n'est plus une decision ; au-dessus,"
          " le joueur n'a plus le temps de voir le resultat de son choix")
    check("l'amorti ne redescend pas en montant d'un palier", all(
        paybacks.get(b, 0.0) >= paybacks.get(a, 0.0) - 0.05 for a, b in zip(tiers, tiers[1:])),
        "le dernier palier doit couter plus cher de temps que le precedent, sinon la fin de partie "
        "s'accelere au lieu de se tendre")
    check(f"amorti du dernier palier dans la bande ({band})",
          PAYBACK_FLOOR <= paybacks.get(tiers[-1], 0.0) <= PAYBACK_CEILING,
          f"{paybacks.get(tiers[-1], 0.0):.2f} h")
    ordered = [written[t] for t in sorted(written)]
    check("le prix des paliers est strictement croissant", all(
        later > before for before, later in zip(ordered, ordered[1:])),
        "`getUpgradePrice` facture la DIFFERENCE entre deux paliers : un prix egal ou plus bas rend le saut "
        "gratuit, ou rembourse le joueur a chaque montee")
    fee, tax = number(settings.get("listing-fee")), number(settings.get("sales-tax"))
    check("frais de mise en vente dans une bande raisonnable (0 a 5 %)", fee is not None and 0.0 <= fee <= 0.05,
          f"lu {settings.get('listing-fee')!r} — au-dela de 5 %, personne n'ose poster, le marche meurt")
    check("taxe de vente dans une bande raisonnable (1 a 10 %)", tax is not None and 0.01 <= tax <= 0.10,
          f"lu {settings.get('sales-tax')!r} — l'argent preleve n'est redistribue a personne : c'est de la"
          " monnaie qui sort du serveur, pas qui change de poche")
    check("le plafond de générateurs qui sert d'ancre est réellement activé",
          str(settings.get("limit-enabled")).lower() == "true",
          "`limits.per-player.enabled` est a "
          f"{settings.get('limit-enabled')!r} : les documents parlent d'un tycoon de 20 blocs, le serveur en "
          "laisse poser une quantite illimitee — l'ancre de revenu (et donc le barème des âmes) est une fable")
    check("le plafond de generateurs est un nombre de blocs qui se tient",
          1 <= int(number(settings.get("limit")) or 0) <= 64,
          f"lu {settings.get('limit')!r} — le compte est en BLOCS de generateur (`getBlockLocations().size()`),"
          " pas en fermes : 3 x 3 coute 9")

    # ---- la section elle-meme
    check("config.yml declare une section `shop:`", bool(shop), "le comptoir ne lit que des defauts")
    for key in ("enabled", "title", "buy-multiplier", "buyback-enabled", "buyback-ratio",
                "max-per-transaction", "amounts", "generated-category", "extras-category"):
        check(f"`shop.{key}` est ecrit", key in shop, "sinon le reglage vit seulement dans le code")
    check("le comptoir est actif dans la config livree", str(enabled).lower() == "true", f"lu {enabled!r}")
    check("`shop.extras` est une liste non vide", bool(extras), "les matieres hors generateur n'ont "
          "nulle part ou s'ecrire")
    check("le parseur n'a pas melange les blocs (aucune cle de generateur dans `shop`)",
          not ({"tier", "sellPrice", "speed", "spawnItem"} & set(shop)), str(sorted(shop)[:6]))

    # ---- les deux garde-fous de prix
    check("buy-multiplier est un nombre", multiplier is not None, f"lu {shop.get('buy-multiplier')!r}")
    check("buy-multiplier > 1 (achat plus cher que /sell ne rend)", (multiplier or 0.0) > 1.0,
          f"lu {multiplier} ; a 1,0 le cycle est nul, en dessous il est gagnant")
    check("buyback-ratio est un nombre", ratio is not None, f"lu {shop.get('buyback-ratio')!r}")
    check("0 < buyback-ratio < 1", ratio is not None and 0.0 < ratio < 1.0, f"lu {ratio}")
    check("buy-multiplier × buyback-ratio < 1 (acheter-revendre est perdant)", product < 1.0,
          "%.3f : le comptoir imprime %.1f %% par aller-retour" % (product, (product - 1.0) * 100.0))
    check("la perte d'un aller-retour reste dans une bande qui a du sens (5 a 60 %)",
          0.05 <= 1.0 - product <= 0.60 if buyback_on and ratio else True,
          "l'aller-retour rend %.1f %% du prix paye : sous 95 %% le comptoir ne punit plus le cycle, au-"
          "dessus de 60 %% de perte il n'y a plus de marche, seulement une taxe" % (product * 100.0))
    check("le code applique le meme plancher que la config",
          "Math.max(1.0D, config.getDouble(\"shop.buy-multiplier\", 1.5D))" in read_text(SHOP_SOURCE),
          "le plancher de marge a ete deplace sans remplacer le controle")

    # ---- lots et bornes
    check("max-per-transaction est entier et positif", max_lot is not None and max_lot > 0, f"lu {max_lot}")
    check("max-per-transaction est un multiple de 64 (livraison sans reliquat)",
          max_lot is not None and max_lot % 64 == 0, f"lu {max_lot}")
    check("amounts est une liste de lots positifs", bool(amounts) and all(a > 0 for a in amounts),
          f"lus {amounts}")
    check("aucun lot ne depasse le plafond de transaction", all(a <= (max_lot or 0) for a in amounts),
          f"{amounts} vs {max_lot}")
    check("pas de lot en double", len(amounts) == len(set(amounts)), str(amounts))
    check("le titre du comptoir est ecrit en codes de couleur", "&" in str(shop.get("title", "")),
          "sinon le GUI herite du rendu par defaut du client")

    # ---- les generatrices
    tiers, materials, speeds, sells = [], [], [], []
    for gen in generators:
        tiers.append(int(number(gen.get("tier")) or 0))
        materials.append(str(gen.get("spawnItem") or "").strip().upper())
        speeds.append(number(gen.get("speed")) or 0.0)
        sells.append(number(gen.get("sellPrice")) or 0.0)
    check("toutes les lignes de generateur ont un `speed` strictement positif", all(s > 0 for s in speeds),
          "un `speed: 0` rend le revenu infini et le comptoir afficherait un prix decale")
    check("toutes les lignes de generateur ont un `sellPrice` strictement positif", all(v > 0 for v in sells),
          "une ligne a 0 est silencieusement absente du comptoir (le code la saute)")
    check("les paliers sont distincts et croissants", tiers == sorted(set(tiers)), str(tiers[:5]) + "…")
    check("une matiere par palier (pas de collision dans un rayon)",
          len(materials) == len(set(materials)), "deux generateurs qui crachent la meme matiere ne forment "
          "qu'une seule offre, la seconde est inatteignable")
    for tier, material, sell in zip(tiers, materials, sells):
        buy = round(sell * (multiplier or 0.0), 2)
        check(f"palier {tier} : l'achat ({exact(buy)}) depasse le rendu de /sell ({exact(sell)})", buy > sell,
              "le comptoir vendrait moins cher que ce qu'il rachete a /sell")
        check(f"palier {tier} : la reprise est inferieure au prix d'achat",
              not buyback_on or round(buy * (ratio or 0.0), 2) < buy, "le cycle court serait gagnant")

    # ---- les rayons
    keys, routed = [], []
    for shelf in categories:
        key = str(shelf.get("key") or "").strip().lower()
        name = shelf.get("name")
        icon = str(shelf.get("icon") or "").strip().upper()
        listing = [str(m).strip().upper() for m in (shelf.get("materials") or [])]
        keys.append(key)
        routed += listing
        check(f"rayon {key or '(clef absente)'} : clef en minuscules", bool(re.fullmatch(r"[a-z][a-z0-9_-]*",
              key)), f"lu {key!r} ; `shop.extras[].category` doit pouvoir la pointer")
        check(f"rayon {key} : un titre", bool(name), "sans nom, l'onglet affiche la clef")
        check(f"rayon {key} : une icone de matiere", bool(re.fullmatch(r"[A-Z][A-Z0-9_]*", icon)), f"lu {icon!r}")
        check(f"rayon {key} : une icone qui existe encore en 1.13+", icon not in LEGACY_MATERIALS,
              f"`{icon}` a ete renomme a l'aplatissage, le client afficherait l'item par defaut")
        check(f"rayon {key} : au moins une matiere routee", bool(listing),
              "une rangee sans `materials` n'accueille aucun generateur : elle n'existe que pour `extras`")
        desc = shelf.get("description")
        if isinstance(desc, str):
            desc = [desc]
        desc = [str(line) for line in (desc or [])]
        check(f"rayon {key} : sa description tient dans une info-bulle ({len(desc)} ligne(s))",
              len(desc) <= MAX_DESC_LINES,
              f"{len(desc)} ligne(s) au-dessus de {MAX_DESC_LINES} : lore de six lignes sur un onglet de "
              "premiere rangee, ca se referme avant d'etre lu — le compte des offres doit rester visible")
        long_lines = [decolor(line) for line in desc if len(decolor(line)) > MAX_DESC_CHARS]
        check(f"rayon {key} : des lignes lues d'un coup (au plus {MAX_DESC_CHARS} caracteres)",
              not long_lines, "; ".join(f"{len(decolor(line))} : {line}" for line in long_lines[:2])
              + " — une ligne trop longue se replie sur deux et l'info-bulle se deforme")
        check(f"rayon {key} : pas de ligne vide dans la description",
              all(decolor(line) for line in desc), "une ligne sans texte est un trou dans le lore, pas un "
              "separeteur : le separateur, c'est le `&r` que `hint()` pose lui-meme")
    check("au moins un rayon est declare", bool(categories), "le comptoir retomberait sur un onglet unique")
    check("clefs de rayons distinctes", len(keys) == len(set(keys)), str(keys))
    check("matieres routees distinctes (une matiere, un rayon)", len(routed) == len(set(routed)),
          "rangee deux fois = rangee a la premiere, la seconde ne recoit rien : retire-la")
    check("rayons <= %d onglets affichables" % layout["max-tabs"],
          len(categories) <= layout["max-tabs"],
          f"{len(categories)} rayons declares ; le surplus ne sera pas cliquable")
    check("l'aire d'offres derive bien de la hauteur du coffre",
          layout["rows"] == 6 and layout["offer-rows"] == layout["rows"] - 2
          and layout["offers-per-page"] == layout["offer-rows"] * 9,
          f"lu {layout} — premiere rangee aux onglets, derniere au solde ; un `Gui` Bukkit de plus de six "
          "rangees n'existe pas, et une page plus grande que l'aire disponible se dessinerait a moitie")
    for material in routed:
        check(f"matiere routee {material} : nom valide", bool(re.fullmatch(r"[A-Z][A-Z0-9_]*", material))
              and material not in LEGACY_MATERIALS, "le comptoir l'ignore au chargement, sans effet")
    missing = [m for m in materials if m not in set(routed)]
    check("chaque matiere de generateur a son rayon (%d/%d)" % (len(materials) - len(missing), len(materials)),
          not missing, "ajoute-la a `shop.categories[].materials` : sinon elle tombe dans `divers`")

    shelves, _gens, contradictions = shelves_of(generators, categories, extras, shop)
    for shelf in shelves:
        total = len(shelf["offers"])
        pages = 1 + max(0, total - 1) // layout["offers-per-page"]
        check(f"rayon {shelf['key']} : {total} offre(s) sur {pages} page(s), deux au plus",
              total <= 2 * layout["offers-per-page"],
              "au-dela de %d offres le comptoir fait une seconde page — c'est voulu, un rayon Construction de "
              "quarante lignes reste un rayon ; au-dela de deux pages ce n'est plus un rayon mais un "
              "fourre-tout, et plus personne ne le parcourt" % layout["offers-per-page"])
        check(f"rayon {shelf['key']} : au moins quatre matieres", total >= 4,
              "un rayon presque vide a l'air casse plutot que range")
    # Un `divers` DECLARE est un rayon voulu : ses offres sont celles que l'admin y a rangees. Ce qui serait
    # une dechet, c'est le panier de secours — le `divers` que le code monte quand le fichier n'en declare pas.
    check("aucun rayon dechet (le panier de secours reste vide quand tout est classé)",
          not any(s["key"] == "divers" and s.get("fallback") for s in shelves),
          "une matiere de generateur ou une offre ne correspond a aucun rayon : complete `materials` ou "
          "corrige `category`")

    # ---- `shop.extras`
    known = set(materials)
    claimers = {m for row in categories for m in (row.get("materials") or [])}
    seen = []
    for row in extras:
        material = str(row.get("material") or "").strip().upper()
        buy = number(row.get("buy"))
        sellback = number(row.get("sellback")) if "sellback" in row else None
        category = str(row.get("category") or "").strip().lower()
        label = f"offre {material or '(matiere absente)'}"
        check(f"{label} : cle de matiere en MAJUSCULES_SOUS_TIRETS", bool(re.fullmatch(r"[A-Z][A-Z0-9_]*",
              material)), f"lu {material!r} ; `Material.matchMaterial` ne trouverait rien")
        check(f"{label} : pas un nom d'avant l'aplatissage 1.13", material not in LEGACY_MATERIALS,
              "le comptoir ignorerait l'entree sans rien afficher")
        check(f"{label} : `buy` positif", buy is not None and buy >= 1.0, f"lu {row.get('buy')!r}")
        check(f"{label} : ne duplique pas une matiere de generateur", material not in known,
              "la seconde offre du meme `Material` est inatteignable, par clic comme par commande")
        check(f"{label} : pas deux fois dans la liste", material not in seen, "doublon dans `shop.extras`")
        seen.append(material)
        # Une `category` ecrite doit nommer un rayon, meme si le routage `materials:` la rattrape : le
        # comptoir suivrait `category` des qu'un autre rayon reclamerait la matiere, et une clef qui n'existe
        # pas n'est pas un rayon de secours, c'est une faute de frappe.
        check(f"{label} : `category` nomme un rayon, ou est absente",
              not category or category in set(keys),
              f"`{category}` ne nomme aucune rangee de `shop.categories`"
              + (" — le routage de `materials:` la rattrape, mais la ligne reste fausse"
                 if material in claimers else " et l'offre tombe dans `divers`"))
        check(f"{label} : un nom lisible ecrit", bool(row.get("name")), "sans `name`, le comptoir affiche "
              "le nom brut de l'enum, ce qui se corrige une fois puis jamais plus")
        if sellback is not None:
            check(f"{label} : reprise a 50 % du prix d'achat au plus", 0.0 < sellback <= 0.5 * (buy or 0.0),
                  f"racheter a {exact(sellback)} un achat a {exact(buy)} : le cycle se rapproche du profit")

    check("aucune offre d'`extras` ne se contredit", not contradictions,
          " ; ".join(f"`{material}` : `category: {category}` alors que le rayon `{claimed}` la réclame"
                     for (material, category, claimed) in contradictions[:4])
          + " — c'est `category` qui gagne, mais un fichier qui se contredit lui-meme ne se relit pas deux "
            "fois volontiers")

    # ---- branchement du hook et de la construction
    check("ShopGui.java est dans les <includes> du pom", "ShopGui.java</include>" in read_text(POM),
          "hors de cette liste la classe n'est pas compilee : elle ne fait rien sur le serveur")
    hook = read_text(HOOK_SOURCE)
    check("le hook /shop est branche dans SellCommandListener", '"/shop"' in hook
          and "ShopGui.command(player, ahArgs)" in hook, "la commande n'est declaree nulle part, c'est "
          "normal ; l'interception ne l'est pas")
    check("la vue du comptoir est liberee au depart du joueur", "ShopGui.forget" in hook,
          "un `Gui` GuiLib enregistre un auditeur a la construction et ne le desinscrit jamais")
    plugin = read_text(PLUGIN_YML)
    check("les permissions du comptoir sont declarees", "valoriatycoon.shop.use:" in plugin
          and "valoriatycoon.shop.admin:" in plugin, "sans declaration, `default: true` n'existe pas")
    check("/shop n'est PAS declare comme commande", not re.search(r"^  (shop|comptoir):\s*$", plugin, re.M),
          "une declaration concurrence l'interception du listener")
    check("le rechargement relit le disque", "plugin.reloadConfig();" in read_text(SHOP_SOURCE),
          "sinon `/shop reload` rend la copie en memoire et l'admin qui a edite le fichier ne voit rien")
    check("la disposition de l'inventaire est lue la ou elle vit", layout["offers-per-page"] > 0
          and layout["rows"] in (4, 5, 6), f"lu {layout}")

    # ---- ancre de duree (reglee par GRIND, pas par le comptoir)
    _joiner, mid, maxed = hours["rates"]
    check("l'ancre de revenu est lisible (palier %d present)" % ANCHOR_TIER, mid > 0.0,
          "le palier %d n'est pas dans la table `generators:`" % ANCHOR_TIER)
    check("maxer une âme demande une saison de tycoon (%.0f–%.0f h)" % SOUL_BAND,
          SOUL_BAND[0] <= hours["pickaxe_hours"] <= SOUL_BAND[1],
          "%.0f h a %s/h : la poignee est `GRIND` dans scripts/gen-tools-config.py" % (
              hours["pickaxe_hours"], money(mid)))
    check("le meme calcul reste jouable au tycoon maxe", hours["maxed_hours"] >= 40.0,
          "%.1f h : l'economie n'a plus de dent si une âme se rembourse en quelques soirees"
          % hours["maxed_hours"])

    # ---- l'extrait à coller, tel que le document le donne
    doc = read_text(ECONOMIE_DOC)
    excerpt = doc_excerpt(doc)
    check("docs/ECONOMIE.md porte un bloc `shop:` à coller", bool(excerpt),
          "sans extrait collable, un serveur déjà installé reste à l'onglet unique")
    if excerpt:
        _ex_gens, ex_shop, ex_cats, ex_extras = read_config(excerpt, probe=True)
        check("l'extrait du document déclare les mêmes rayons, dans le même ordre",
              [str(row.get("key")) for row in ex_cats] == [str(row.get("key")) for row in categories],
              "document : %s ; config : %s" % ([row.get("key") for row in ex_cats],
                                                [row.get("key") for row in categories]))
        drift = []
        for want, shown in zip(categories, ex_cats):
            key = str(want.get("key"))
            if set(want.get("materials") or []) != set(shown.get("materials") or []):
                drift.append("%s : %d matière(s) dans le fichier, %d dans l'extrait"
                             % (key, len(want.get("materials") or []), len(shown.get("materials") or [])))
            if decolor(want.get("name")) != decolor(shown.get("name")):
                drift.append("%s : titre %r au lieu de %r" % (key, shown.get("name"), want.get("name")))
            if str(want.get("icon")).upper() != str(shown.get("icon")).upper():
                drift.append("%s : icône %r au lieu de %r" % (key, shown.get("icon"), want.get("icon")))
            if listify(want.get("description")) != listify(shown.get("description")):
                drift.append("%s : la description du document n'est plus celle du fichier" % key)
        check("l'extrait du document route exactement les mêmes matières", not drift, " ; ".join(drift[:5]))
        shared = [k for k in ex_shop if k in shop and k not in ("extras", "categories")]
        wrong = [k for k in shared if str(ex_shop[k]) != str(shop[k])]
        check("les réglages cités dans le document sont ceux du fichier livré",
              len(shared) >= 9 and not wrong,
              ("extrait périmé sur : %s" % ", ".join(wrong)) if wrong
              else f"{len(shared)} réglage(s) en commun lu(s) sur 9 attendus")
        shipped = {str(row.get("material")).upper(): row for row in extras}
        fake = []
        for row in ex_extras:
            material = str(row.get("material")).upper()
            origin = shipped.get(material)
            if origin is None:
                fake.append(f"{material} n'existe pas dans `resources/config.yml`")
                continue
            for field in ("name", "buy", "sellback", "category"):
                if field in row and str(row[field]).strip() != str(origin.get(field, "")).strip():
                    fake.append("%s : `%s` dit %r dans le document, %r dans le fichier"
                                % (material, field, row[field], origin.get(field)))
        check("les exemples d'`extras` du document sont bien des lignes du fichier", not fake,
              " ; ".join(fake[:4]) or "aucun exemple écrit dans le document")

    # ---- couplage des documents
    check("docs/ECONOMIE.md porte le bloc genere du comptoir", MARK_BEGIN in doc and MARK_END in doc,
          "relance `python3 scripts/verify-shop-economy.py --write`")
    if MARK_BEGIN in doc and MARK_END in doc:
        shown = doc[doc.index(MARK_BEGIN):doc.index(MARK_END) + len(MARK_END)]
        check("le document et la config disent la meme chose",
              shown.strip() == render_block(generators, shop, categories, extras, hours).strip(),
              "relance `python3 scripts/verify-shop-economy.py --write`")
    tools_doc = read_text(TOOLS_DOC) if os.path.exists(TOOLS_DOC) else ""
    grand = sum(soul_total().values())
    missing = []
    for rate in hours["rates"]:
        if rate <= 0.0 or grand <= 0.0:
            continue
        line = "| %s $/h | %s h | %s h |" % (group(rate), group((grand / 4.0) / rate), group(grand / rate))
        if line not in tools_doc:
            missing.append(line)
    check("docs/MULTI-OUTIL.md divise les heures de montee par les trois revenus reels du serveur",
          not missing, "lignes attendues : %s — regerer avec `python3 scripts/longevity-tools-config.py "
          "--hours %s`" % (" | ".join(missing), " ".join(str(int(r)) for r in hours["rates"])))


def compute_hours(generators, shop, override=None, settings=None):
    totals = soul_total()
    rates = anchor_rates(shop, generators, settings)
    joiner, mid, maxed = rates
    pick = totals.get("pickaxe", 0.0)
    used = override or [mid]
    return {
        "totals": totals,
        # Les deux chiffres que le bloc genere doit savoir enoncer sans les recopier: combien de generateurs
        # un arrivant recoit, et le plafond qui borne un tycoon. Une ancre qui inventerait un reglage lu
        # ailleurs est exactement la derive que ce script existe pour attraper.
        "join": int(number((settings or {}).get("join-amount")) or 3),
        "cap": int(number((settings or {}).get("limit")) or 20),
        "pickaxe": pick,
        "pickaxe_hours": (pick / used[0]) if used and used[0] > 0 else 0.0,
        "maxed_hours": (pick / maxed) if maxed > 0 else 0.0,
        "rates": [r for r in (override or rates)] or list(rates),
    }


def report() -> int:
    if problems:
        print(f"{len(problems)} problème(s) :", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
    if "--quiet" not in sys.argv:
        for note in notes:
            print(f"  [OK ] {note[5:]}")
    if problems:
        return 1
    print(f"OK : comptoir, rayons et prix cohérents ({len(notes)} contrôles).")
    return 0


def main(argv) -> int:
    text = read_text(CONFIG)
    generators, shop, categories, extras = read_config(text)
    settings = read_settings(text)
    if "--prices" in argv:
        # Le prix d'un palier se relit de la regle, il ne se recopie pas : `write_prices` reecrit les 28
        # lignes du fichier (et son miroir), puis on repart controls dans le nez — la meme sequence que
        # `gen-tools-config.py` pour le `config.yml` des outils, pour la meme raison.
        updated, lines, changed = write_prices(text)
        if changed:
            for path in (CONFIG, MIRROR_CONFIG):
                if os.path.exists(os.path.dirname(path)):
                    with open(path, "w", encoding="utf-8") as handle:
                        handle.write(updated)
        print("prix des generateurs : " + ("%d ligne(s) reecrite(s)" % changed if changed
                                          else "deja conformes a la regle"))
        for line in lines:
            print(line)
        if changed and os.path.exists(MIRROR_CONFIG):
            print(f"(miroir {os.path.relpath(MIRROR_CONFIG, ROOT)} reecrit a l'identique)")
        text = updated if changed else text
        generators, shop, categories, extras = read_config(text)
        settings = read_settings(text)
        del problems[:]
        del notes[:]
    override = None
    if "--hours" in argv:
        values = []
        for item in argv[argv.index("--hours") + 1:]:
            if item.startswith("-"):
                break
            values.append(float(item))
        override = values or None
    hours = compute_hours(generators, shop, override, settings)
    if "--write" in argv:
        # Deux blocs du document viennent du fichier : le bloc genere (bareme + rayons) et l'extrait `shop:`
        # a coller. Les deux se regenere nt ici, parce qu'un document qu'on resynchronise la moitie seulement
        # est un document qui ment sur l'autre moitie.
        excerpt = splice_excerpt(current_text := read_text(ECONOMIE_DOC), excerpt_block(read_text(CONFIG)))
        if excerpt != current_text:
            with open(ECONOMIE_DOC, "w", encoding="utf-8") as handle:
                handle.write(excerpt)
            print(f"extrait `shop:` reecrit dans {os.path.relpath(ECONOMIE_DOC, ROOT)}")
        else:
            print(f"extrait deja a jour dans {os.path.relpath(ECONOMIE_DOC, ROOT)}")
        block = render_block(generators, shop, categories, extras, hours)
        # Le fichier est entierement reconstruit EN MEMOIRE avant d'etre ouvert : un `open(..., "w")`
        # place avant la lecture tronque le document, et si `splice` rechigne (balises absentes, ancre
        # mal orthographiee) il ne reste plus qu'un fichier vide a la place d'une page de documentation.
        current = read_text(ECONOMIE_DOC)
        updated = splice(current, block, ECONOMIE_DOC)
        if updated == current:
            print(f"bloc deja a jour dans {os.path.relpath(ECONOMIE_DOC, ROOT)}")
        else:
            with open(ECONOMIE_DOC, "w", encoding="utf-8") as handle:
                handle.write(updated)
            print(f"bloc ecrit dans {os.path.relpath(ECONOMIE_DOC, ROOT)} "
                  f"({len(block.splitlines())} lignes)")
        del problems[:]
        del notes[:]
    check_all(generators, shop, categories, extras, hours, settings)
    return report()


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
