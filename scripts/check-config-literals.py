#!/usr/bin/env python3
"""Refuse une clé de configuration qui n'est pas un littéral de chaîne, sur les fichiers compilés.

Pourquoi cette règle existe. `section.getDouble(sell.min-value, x)` est une expression Java VALIDE
(`sell . min - value`) : un contrôle de grammaire la laisse passer, `javac` répond `cannot find
symbol`. Ce défaut a été introduit par un correctif appliqué en shell avec `python3 -c "…"` — le shell
a mangé les guillemets de la chaîne de remplacement, et la clé est sortie du fichier sans guillemets.
Coût : trois minutes de CI par occurrence.

Pourquoi le périmètre est étroict. Une première version exigeait un littéral pour tout `.get(`/`.set(` :
786 faux positifs (Map.get, List.get, defaults().set(key, …)), puis 27 en s'élargissant à l'arbre
décompilé (clés portées par `Config.X.getPath()` ou une constante). Un contrôle qui signale du code
correct est ignoré, donc nocif. On ne regarde donc QUE :

  - les fichiers listés dans `<includes>` du `pom.xml` (ceux que le build compile vraiment) ;
  - les récepteurs d'interface de configuration identifiables sans typage complet : `getConfig()`, et
    toute variable/paramètre déclaré `ConfigurationSection`/`YamlConfiguration`/`FileConfiguration` ;
  - et on accepte la clé dès qu'un littéral existe à portée (`"…"`, `raw + ".balance"`,
    `Config.X.getPath()`, `MAJOR_CONSTANT`, `this.path`, `key`).

    python3 scripts/check-config-literals.py [--quiet] [cible …]
"""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
KEYED = ("getString", "getInt", "getDouble", "getLong", "getBoolean", "getStringList",
         "getIntegerList", "getDoubleList", "getConfigurationSection", "getList", "getMemorySection",
         "isSet", "contains", "getConfiguration")
DECL = re.compile(r"\b(?:ConfigurationSection|YamlConfiguration|FileConfiguration|MemoryConfiguration)"
                  r"\s+([a-z]\w*)\b")
CALL = re.compile(r"\b(\w+)\.(" + "|".join(KEYED) + r")\(\s*([^,()\n]*(?:\([^()]*\))?[^,()\n]*)")


def compiled_files():
    pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    block = re.search(r"<includes>(.*?)</includes>", pom, re.S)
    if not block:
        return []
    return [ROOT / "sources" / inc for inc in re.findall(r"<include>([^<]+\.java)</include>", block.group(1))]


def check_file(path: Path):
    text = path.read_text(encoding="utf-8", errors="replace")
    body = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    body = re.sub(r"//[^\n]*", "", body)
    receivers = {"getConfig"} | set(DECL.findall(body))
    key_names = set(re.findall(r"String\s+(\w+)\s*=\s*\"", body))
    problems = []
    for match in CALL.finditer(body):
        receiver, method, first = match.group(1), match.group(2), match.group(3).strip()
        if receiver not in receivers or not first:
            continue
        if first.startswith('"') or '"' in first:
            continue                                   # littéral, ou concat tenant un littéral
        if re.fullmatch(r"Config\.[A-Z_0-9]+\.getPath\(\s*\)", first):
            continue                                   # clé portée par l'enum Config du plugin
        if re.fullmatch(r"(?:this\.)?\w*(?:path|key|node|node_?name)", first, re.I):
            continue
        head = re.match(r"([A-Za-z_]\w*)", first)
        if head and (head.group(1) in key_names or head.group(1).isupper()):
            continue                                   # constante nommée, déclarée dans le fichier
        if "(" in first:
            continue                                   # cle calculee (`kind.name().toLowerCase(…)`)
        # Un identifiant avec un tiret n'existe pas en Java : `sell.min-value` est une SOUSTRACTION de
        # deux symboles inconnus. C'est la signature exacte d'une cle dont les guillemets ont disparu
        # (les fichiers decompiles du paquet kotlin en contiennent ailleurs, mais ils ne sont pas
        # compiles — d'ou le perimetre "fichiers du pom" plus haut).
        if re.search(r"[A-Za-z_]\w*-[A-Za-z_]\w*", first):
            problems.append(f"{path.relative_to(ROOT)}:{body[:match.start()].count(chr(10)) + 1}: "
                            f"`{first[:44]}` n'est pas un identifiant Java valide (tiret) — guillemets "
                            "de la clé perdus ?")
            continue
        line = body[:match.start()].count("\n") + 1
        problems.append(f"{path.relative_to(ROOT)}:{line}: `{receiver}.{method}({first[:44]})` demande "
                        "une clé en littéral de chaîne (le compilateur verrait un `cannot find symbol`)")
    return problems


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    targets = [Path(a) for a in args] if args else compiled_files()
    files = []
    for target in targets:
        files += sorted(target.rglob("*.java")) if target.is_dir() else [target]
    problems = []
    for path in files:
        if path.is_file():
            problems += check_file(path)
    if "--quiet" not in sys.argv:
        print(f"fichiers contrôlés : {len(files)}")
    if problems:
        print(f"{len(problems)} problème(s) :", file=sys.stderr)
        for problem in problems[:30]:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    print("OK : toute clé de configuration des fichiers compilés est un littéral (ou une constante).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
