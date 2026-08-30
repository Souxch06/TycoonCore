#!/usr/bin/env python3
"""Contrôle la surface de l'API d'économie interne : snapshot ↔ interface ↔ fournisseur ↔ appelants.

Pourquoi ce contrôle existe : le plugin (dont les classes sont précompilées) appelle l'interface
d'économie avec des signatures précises, relevées dans le bytecode livré. Une méthode d'interface
oubliée est une erreur de compilation — donc visible — mais **une signature qui dérive** (un type
de retour changé, un paramètre inversé) ne se voit qu'en jeu, sous forme d'`AbstractMethodError`
ou de `NoSuchMethodError`. Ce script compare donc quatre sources de vérité, sans JDK :

1. `docs/economy-api.txt` — le snapshot des signatures, unique endroit à modifier ;
2. `sources/api/…/valoriateconomy/Economy.java` — l'interface générée depuis ce snapshot ;
3. `sources/economy/…/ValoriaEconomyProvider.java` — le fournisseur, qui doit les implémenter toutes ;
4. les `.class` livrés — qui imposent la surface réellement appelée.

Il vérifie aussi l'invariant du dépôt : **plus aucune API Vault** (`net.milkbowl`) n'est référencée,
ni dans nos sources, ni dans les classes, ni dans un JAR construit.

    python3 scripts/verify-economy-api.py
    python3 scripts/verify-economy-api.py --jar target/ValoriaEconomy-v1.6.3.jar
"""

from pathlib import Path
import argparse
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / "docs/economy-api.txt"
API = ROOT / "sources/api/xyz/arcadiadevs/valoriateconomy"
INTERFACE = API / "Economy.java"
RESPONSE = API / "EconomyResponse.java"
PROVIDER = ROOT / "sources/economy/xyz/arcadiadevs/valoriaeconomy/ValoriaEconomyProvider.java"
MAIN_CLASS = ROOT / "artifacts/extracted/xyz/arcadiadevs/valoriatycoon/ValoriaTycoon.class"
ECONOMY_CALLERS = (
    "xyz/arcadiadevs/valoriatycoon/ValoriaTycoon.class",
    "xyz/arcadiadevs/valoriatycoon/utils/SellUtil.class",
    "xyz/arcadiadevs/valoriatycoon/guis/GeneratorsGui.class",
    "xyz/arcadiadevs/valoriatycoon/guis/UpgradeGui.class",
)
OUR_INTERFACE = "xyz/arcadiadevs/valoriateconomy/Economy"

sys.path.insert(0, str(ROOT / "scripts"))
import classfile  # noqa: E402

problems = []
notes = []


def fail(label, detail=""):
    problems.append((label, detail))


def note(text):
    notes.append(text)


def snapshot_methods():
    methods = []
    for line in SNAPSHOT.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        m = re.match(r"^([\w.]+(?:<[^>]*>)?)\s+(\w+)\s*\(([^()]*)\)$", line)
        if m is None:
            fail(f"snapshot : ligne invalide {line!r}")
            continue
        ret, name, params = m.groups()
        params = ", ".join(p.strip() for p in params.split(",") if p.strip())
        methods.append((ret, name, params))
    return methods


def interface_methods(text):
    out = []
    for m in re.finditer(r"^\s{4}(?:default\s+)?([\w<>\[\]., ]+?)\s+(\w+)\s*\(([^)]*)\)\s*[;{]", text, re.M):
        ret, name, params = m.groups()
        out.append((ret.strip(), name.strip(), ", ".join(p.strip() for p in params.split(",") if p.strip())))
    return out


def _signature(ret, name, params):
    return (ret.strip(), name.strip(), ", ".join(p.strip() for p in params.split(",") if p.strip()))


def interface_methods(text):
    """Membres d'une interface : `ret nom(params);` ou `default ret nom(params) {`, sans modificateur."""
    return [_signature(*m.groups()) for m in re.finditer(
        r"^\s{4}(?:default\s+)?([A-Za-z][\w<>\[\],. ]*?)\s+(\w+)\s*\(([^)]*)\)\s*[;{]", text, re.M)]


def provider_methods(text):
    """Methodes d'une classe : `public ret nom(params) {` (les champs et le constructeur sont exclus)."""
    out = []
    for m in re.finditer(r"^    public ([A-Za-z][\w<>\[\],. ]*?) (\w+)\(([^)]*)\) \{$", text, re.M):
        ret, name, params = m.groups()
        if name in ("ValoriaEconomyProvider",):  # constructeur
            continue
        out.append(_signature(ret, name, params))
    return out


def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def check_tree():
    for path in (SNAPSHOT, INTERFACE, RESPONSE, PROVIDER):
        if not path.is_file():
            fail(f"fichier manquant : {path.relative_to(ROOT)}")
            return
    snap = snapshot_methods()
    iface = interface_methods(INTERFACE.read_text(encoding="utf-8"))
    provider = provider_methods(PROVIDER.read_text(encoding="utf-8"))

    def key(entry):
        # Les espaces et le placement des sauts de ligne different d'un fichier a l'autre (le
        # snapshot est brut, l'interface est generee, le fournisseur a des corps) : on compare la
        # signature normale, sans blanc.
        ret, name, params = entry
        return (re.sub(r"\s+", "", ret), name, re.sub(r"\s+", "", params))

    snap_keys = [key(e) for e in snap]
    iface_keys = [key(e) for e in iface]
    provider_keys = [key(e) for e in provider]

    if len(set(snap_keys)) != len(snap_keys):
        dup = sorted({k for k in snap_keys if snap_keys.count(k) > 1})
        fail(f"snapshot : {len(dup)} signature(s) dupliquée(s)", str(dup[:3]))
    missing = [k for k in snap_keys if k not in iface_keys]
    if missing:
        fail(f"interface : {len(missing)} méthode(s) du snapshot absente(s)", str(missing[:4]))
    extra = [k for k in iface_keys if k not in snap_keys and k[1] != "format"]
    if extra:
        fail(f"interface : {len(extra)} méthode(s) hors snapshot (générée à la main ?)", str(extra[:4]))
    if ("String", "format", "doubleamount") not in iface_keys:
        fail("interface : le default format(double) manque — le bytecode livré l'appelle")

    # `format` est un default : la classe n'a pas a l'ecrase.
    not_impl = [k for k in iface_keys if k not in provider_keys and k[1] != "format"]
    if not_impl:
        fail(f"fournisseur : {len(not_impl)} méthode(s) d'interface non implémentée(s)", str(not_impl[:4]))
    orphan = [k for k in provider_keys if k not in iface_keys]
    if orphan:
        fail(f"fournisseur : {len(orphan)} méthode(s) n'appartenant plus à l'interface", str(orphan[:4]))
    note(f"{len(snap)} signatures au snapshot, {len(iface)} dans l'interface (dont le default), "
         f"{len(provider)} implémentées")

    response = RESPONSE.read_text(encoding="utf-8")
    for needle in ("public final double amount", "public final double balance",
                   "public final ResponseType type", "public final String errorMessage",
                   "public boolean transactionSuccess()", "enum ResponseType",
                   "public static final EconomyResponse NOT_IMPLEMENTED"):
        if needle not in response:
            fail(f"EconomyResponse : membre attendu absent : {needle}")
    order = re.findall(r"^\s{8}([A-Z_]+)\(", response, re.M)
    if order != ["SUCCESS", "FAILURE", "NOT_IMPLEMENTED", "FAILURE_PARTIAL", "UNSUPPORTED_OPERATION"]:
        fail("EconomyResponse : ordre des ResponseType modifié", str(order))

    for path in (INTERFACE, RESPONSE, PROVIDER):
        code = strip_comments(path.read_text(encoding="utf-8"))
        if "milkbowl" in code or "holoeasy" in code:
            fail(f"{path.relative_to(ROOT)} référence encore une API tierce")
    if "net.milkbowl" in INTERFACE.read_text(encoding="utf-8") + RESPONSE.read_text(encoding="utf-8"):
        fail("une API Vault (même en commentaire) traîne dans sources/api")

    # l'appel compilé : la surface réellement exigée par le plugin
    if MAIN_CLASS.is_file():
        values = classfile.utf8_values(MAIN_CLASS.read_bytes())
        if not any(OUR_INTERFACE in v for v in values):
            fail("classe principale : aucune référence à notre interface d'économie "
                 "(lancer scripts/selfmade-api-patch.py)")
        if any("milkbowl" in v for v in values):
            fail("classe principale : référence Vault encore présente")
            # Un Methodref ne stocke PAS « nom+descripteur » collés : les deux sont des constantes Utf8
        # distinctes. On vérifie donc les deux moitiés, ailleurs reliées par un NameAndType.
        blob = MAIN_CLASS.read_bytes() + b"".join(
            (ROOT / "artifacts/extracted" / rel).read_bytes()
            for rel in ECONOMY_CALLERS[1:] if (ROOT / "artifacts/extracted" / rel).is_file())
        needed = [
            (b"format", b"(D)Ljava/lang/String;"),
            (b"getBalance", b"(Lorg/bukkit/OfflinePlayer;)D"),
            (b"has", b"(Lorg/bukkit/OfflinePlayer;D)Z"),
            (b"withdrawPlayer", b"(Lorg/bukkit/OfflinePlayer;D)Lxyz/arcadiadevs/valoriateconomy/EconomyResponse;"),
            (b"depositPlayer", b"(Lorg/bukkit/OfflinePlayer;D)Lxyz/arcadiadevs/valoriateconomy/EconomyResponse;"),
        ]
        absent = [n.decode() for n, d in needed if n not in blob or d not in blob]
        if absent:
            fail("appels du plugin introuvables dans les .class (surface déplacée)", str(absent))
        if b"transactionSuccess" not in blob or b"errorMessage" not in blob:
            fail("EconomyResponse : transactionSuccess()/errorMessage attendus par le bytecode livré")
        note(f"{len(needed) - len(absent)}/{len(needed)} appels vérifiés dans les .class livrés")


def check_jar(jar_path: Path):
    if not jar_path.is_file():
        fail(f"JAR introuvable : {jar_path}")
        return
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        for needed in ("xyz/arcadiadevs/valoriateconomy/Economy.class",
                       "xyz/arcadiadevs/valoriateconomy/EconomyResponse.class"):
            if needed not in names:
                fail(f"JAR : classe d'API interne manquante : {needed}")
        if "plugin.yml" in names and b"ValoriaEconomy" not in jar.read("plugin.yml"):
            fail("JAR : plugin.yml ne cite plus ValoriaEconomy en softdepend")
        offenders = []
        for name in sorted(names):
            if not name.endswith(".class"):
                continue
            blob = jar.read(name)
            if b"net/milkbowl" in blob or b"org/holoeasy" in blob:
                offenders.append(name)
        if offenders:
            fail(f"JAR : {len(offenders)} classe(s) référencent encore une API tierce", str(offenders[:3]))
        note(f"{len(names)} entrées, aucune référence à Vault")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--jar", metavar="CHEMIN", help="contrôle aussi un JAR construit")
    args = parser.parse_args()

    check_tree()
    if args.jar:
        path = Path(args.jar)
        check_jar(path if path.is_absolute() else ROOT / args.jar)

    for line in notes:
        print("  - " + line)
    if problems:
        for label, detail in problems:
            print(f"ERREUR: {label}" + (f" — {detail}" if detail else ""), file=sys.stderr)
        print(f"\n{len(problems)} problème(s) : la surface d'économie n'est pas cohérente.", file=sys.stderr)
        return 1
    print("OK: snapshot, interface, fournisseur et appel compilés sont cohérents — et rien ne vient "
          "d'une API tierce.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
