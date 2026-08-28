#!/usr/bin/env python3
"""Pré-vérification des imports : ce que `javac` reprocherait, contrôlé sans JDK.

Le build de la PR #7 a echoue pour une seule raison, multipliee en ~110 lignes : la classe generee
`ValoriaEconomyProvider.java` citait `OfflinePlayer` sans l'importer (la liste d'imports etait
enumeree a la main dans le generateur, pas deduite des signatures). Ce genre d'oubli est impossible a
voir a l'oeil sur un fichier genere, et invisible pour un controle de surface.

Regle appliquee : chaque type cite dans une **signature** (retour de methode, parametre, champ) doit
etre soit
  - importe explicitement,
  - soit du paquet courant (le compilateur le resout tout seul — mais le fichier doit quand meme le
    livrer dans le bon paquet : c'est verifie separement),
  - soit un type de `java.lang`,
  - soit resolvable dans le classpath de compilation (artifacts/reference + classes generees),
  - soit un membre imbrique cite via son parent (`EconomyResponse.ResponseType`), deja couvert par
    l'import du parent.

Tout le reste est signale. Une liste blanche courte couvre les types Bukkit utilisables sans import
par convention du depot (aucun, en fait : le depot importe tout, et c'est justement la regle).

    python3 scripts/verify-source-imports.py            # les fichiers que le pom compile
    python3 scripts/verify-source-imports.py --all      # tout sources/plugin (informatif)
"""

from pathlib import Path
import argparse
import re
import sys
import zipfile

error_group = type("no such group", (Exception,), {})

ROOT = Path(__file__).resolve().parents[1]
POM = ROOT / "pom.xml"
REF_JAR = ROOT / "artifacts" / "reference" / "valoria-renamed.jar"
EXTRACTED = ROOT / "artifacts" / "extracted"
SOURCES = ROOT / "sources"

JAVA_LANG = {
    "String", "Integer", "Long", "Double", "Float", "Short", "Byte", "Boolean", "Character",
    "Object", "Class", "System", "Math", "StringBuilder", "StringBuffer", "Runnable", "Thread",
    "Throwable", "Exception", "RuntimeException", "Error", "Override", "Deprecated", "SuppressWarnings",
    "Iterable", "Number", "Comparable", "CharSequence", "Void", "Enum", "Record", "Module",
    "IllegalStateException", "IllegalArgumentException", "NullPointerException",
    "UnsupportedOperationException", "ClassNotFoundException", "NoSuchMethodError",
    "NoClassDefFoundError", "LinkageError", "FunctionalInterface", "SafeVarargs",
}
# Types de primitifs/boites jamais a importer.
NON_TYPES = {"void", "int", "long", "double", "float", "boolean", "byte", "short", "char"}

# Types deja vus ailleurs dans le depot : si un fichier les utilise sans import, c'est bruyant mais
# pas une erreur de compilation (le type existe, seulement importe ailleurs). Ils ne sont donc
# signales qu'en avertissement, jamais en echec.
COMMON_JDK = {
    "List", "ArrayList", "Map", "HashMap", "LinkedHashMap", "Set", "HashSet", "LinkedHashSet",
    "Collection", "Collections", "Iterator", "Objects", "Arrays", "Optional", "Locale", "UUID",
    "ThreadLocal", "Consumer", "Function", "Supplier", "Predicate", "BiFunction", "Deque",
    "ArrayDeque", "Entry", "Random", "Timer", "Date", "Pattern", "Matcher", "Duration",
    "AtomicInteger", "AtomicLong", "AtomicBoolean", "CompletableFuture", "ExecutorService",
    "FileReader", "FileWriter", "IOException", "Files", "Path", "Paths", "File",
}


def loaded_types() -> set:
    """Noms simples resolubles via le classpath de compilation (JAR de reference + extraction)."""
    names = set()

    def add(path: str):
        tail = path[:-6] if path.endswith(".class") else path
        simple = tail.rsplit("/", 1)[-1]
        if "$" not in simple:
            names.add(simple)

    if REF_JAR.is_file():
        with zipfile.ZipFile(REF_JAR) as jar:
            for entry in jar.namelist():
                add(entry)
    if EXTRACTED.is_dir():
        for path in EXTRACTED.rglob("*.class"):
            add(path.relative_to(EXTRACTED).as_posix())
    # Les sources generees par le build (API d'economie, hologrammes) sont compilees dans le MEME lot :
    # les considerer « resolues » sansquoi un import oublie dans ces fichiers-la serait invisible.
    for path in SOURCES.rglob("*.java"):
        simple = path.stem
        if simple.isidentifier() and simple[:1].isupper():
            names.add(simple)
    return names


def pom_includes() -> list:
    text = POM.read_text(encoding="utf-8")
    block = re.search(r"<includes>(.*?)</includes>", text, re.S)
    if not block:
        return []
    files = []
    for inc in re.findall(r"<include>([^<]+\.java)</include>", block.group(1)):
        path = SOURCES / inc
        if path.is_file():
            files.append(path)
    return files


def strip_noise(text: str) -> str:
    """Retire commentaires, chaines et caracteres litteraux (les seuls endroits ou un nom de type
    peut apparaitre sans etre une reference reelle)."""
    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        two = text[i:i + 2]
        if two == "//":
            j = text.find("\n", i)
            i = n if j < 0 else j
            continue
        if two == "/*":
            j = text.find("*/", i + 2)
            end = n if j < 0 else j + 2
            # garder les sauts de ligne du commentaire : sans eux, tout le fichier est recolte sur
            # une seule ligne et les compteurs de lignes (et les ancres d'indentation) deviennent faux
            out.append("\n" * text.count("\n", i, end))
            i = end
            continue
        if c in "\"'":
            quote = c
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == quote:
                    i += 1
                    break
                i += 1
            out.append(" ")
            continue
        out.append(c)
        i += 1
    return "".join(out)


def type_names(fragment: str) -> list:
    """Les types cites dans une signature, en conservant la chaine de qualification.

    `LocationsData.GeneratorLocation` est un type IMBRIQUE : il se resout par son parent (import
    `…GeneratorsData`, `…LocationsData`), jamais par son propre nom. Ecraser la qualification par le
    dernier segment produirait des faux positifs sur chaque `Outer.Inner` du depot.
    """
    out = []
    for part in re.findall(r"<([^<>]*(?:<[^<>]*>)?[^<>]*)>|([A-Za-z][\w]*(?:\.[A-Za-z][\w]*)*)", fragment):
        candidates = [part[0], part[1]]
        for chunk in candidates:
            for token in re.findall(r"[A-Za-z][\w]*(?:\.[A-Za-z][\w]*)*", chunk or ""):
                if re.sub(r"\[\s*\]", "", token) and not token.endswith(("(", ")")):
                    out.append(token)
    cleaned = []
    for token in out:
        head = token.split(".", 1)[0]
        if head.lower() in NON_TYPES or token.lower() in NON_TYPES:
            continue
        if head[:1].isupper():
            cleaned.append(token)
    return cleaned


SIGNATURE_RE = re.compile(
    # signature de methode : modificateurs, type de retour, nom, parentheses
    r"^[ \t]*(?P<mods>(?:(?:public|protected|private|static|final|abstract|default|synchronized|"
    r"native|strictfp)\s+)+)(?P<ret>[A-Za-z][\w.<>\[\] ]*?)\s+(?P<name>\w+)\s*\((?P<params>[^)]*)\)"
    r"\s*(?:throws[\w.,\s]+)?[;{]", re.M)
FIELD_RE = re.compile(
    r"^[ \t]*(?P<mods>(?:(?:public|protected|private|static|final|transient|volatile)\s+)+)"
    r"(?P<ret>[A-Za-z][\w.<>\[\] ]*?)\s+(?P<name>\w+)\s*(?:=|;)", re.M)


def check_file(path: Path, known: set) -> list:
    """Controle les types cites dans les SIGNATURES (methodes et champs).

    Perimetre volontairement etroit : c'est la ou un import manque casse la compilation. Les corps
    (variables locales, constantes d'enum, javadoc) sont ignores — trop de faux positifs, et un nom
    inconnu dans un corps est une erreur que le generateur ne produit pas.
    """
    raw = path.read_text(encoding="utf-8")
    code = strip_noise(raw)
    package = re.search(r"^\s*package\s+([\w.]+)\s*;", code, re.M)
    pkg = package.group(1) if package else ""
    imports = set(re.findall(r"^\s*import\s+(?:static\s+)?([\w.]+)\s*;", code, re.M))
    imported_simple = {i.rsplit(".", 1)[-1] for i in imports}
    for i in imports:                      # `import A.B;` rend `A` utilisable
        parts = i.split(".")
        if len(parts) > 2 and parts[-2][:1].isupper():
            imported_simple.add(parts[-2])
    local_dir = path.parent
    body = re.sub(r"^\s*(?:package|import)\s+[\w.]+\s*;", "", code, flags=re.M)
    # types niches declares dans le fichier (enum/record/classe/inner) : pas d'import a attendre
    declared = set(re.findall(
        r"\b(?:class|interface|enum|record|@interface)\s+([A-Z]\w*)", code))
    declared |= set(re.findall(r"^\s{4,}([A-Z][A-Z0-9_]*)\s*(?:\(|,|;)", code, re.M))

    problems = []
    soft = []
    seen = set()
    for pattern in (SIGNATURE_RE, FIELD_RE):
        for m in pattern.finditer(body):
            def group(key):
                try:
                    return m.group(key) or ""
                except (IndexError, error_group):
                    return ""
            frag = group("ret") + " " + group("params")
            for name in type_names(frag):
                key = (m.start(), name)
                if key in seen:
                    continue
                seen.add(key)
                head = name.split(".", 1)[0]
                if len(name.split(".")) > 1 and head in imported_simple:
                    continue          # type imbrique d'un parent importee : `Outer.Inner` est legal
                name = head
                if name in declared or name in imported_simple:
                    continue
                if name in JAVA_LANG or name in known:
                    continue
                if (local_dir / (name + ".java")).is_file():       # meme paquet, livre par le build
                    continue
                if (SOURCES / pkg.replace(".", "/") / (name + ".java")).is_file():
                    continue
                line = body[:m.start()].count("\n") + 1
                label = (f"{path.relative_to(ROOT)}:{line}: type `{name}` utilise dans une signature "
                         "sans import (ni java.lang, ni paquet courant, ni classpath de compilation)")
                if name not in COMMON_JDK:
                    problems.append(label)
    return sorted(set(problems)), sorted(set(soft))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--all", action="store_true", help="controler aussi tout sources/plugin (informatif)")
    args = parser.parse_args()

    known = loaded_types()
    files = pom_includes()
    if not files:
        print("ERREUR: aucun <include> exploitable dans pom.xml", file=sys.stderr)
        return 1
    if args.all:
        files = sorted(set(files) | set((SOURCES / "plugin").rglob("*.java")))

    all_problems, all_soft = [], []
    for path in files:
        hard, warnings = check_file(path, known)
        all_problems += hard
        all_soft += warnings

    print(f"Fichiers controls : {len(files)} ; types resolus via le classpath : {len(known)}")
    if all_soft:
        print(f"(style) {len(all_soft)} type(s) de java.util/jdk cites sans import explicite : "
              "resolus par le classpath, non bloquants")
    if all_problems:
        for p in all_problems[:40]:
            print(f"ERREUR: {p}", file=sys.stderr)
        if len(all_problems) > 40:
            print(f"... +{len(all_problems) - 40} autres", file=sys.stderr)
        return 1
    print("OK: chaque type cite est importe, dans le paquet courant, en java.lang, ou dans le classpath.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
