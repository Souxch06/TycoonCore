#!/usr/bin/env python3
"""Régénère l'API d'économie interne du serveur : interface + fournisseur.

Pourquoi générer : la surface appelée par le plugin (et par le code déjà compilé du paquet livré) est
un ensemble de 44 signatures, figées dans le bytecode. En oublier une seule, ou en décaler une, se
paie soit en échec de compilation, soit — pire, car silencieux — en `AbstractMethodError` en jeu chez
le premier appelant. Les signatures sont donc stockées dans `docs/economy-api.txt` et cette interface
les émettrait mot pour mot.

Pourquoi une écriture maison et pas VaultAPI : `net.milkbowl.vault.economy.Economy` est un artifact
Maven tiers, et l'installer signifiait aussi installer un plugin Vault (ou son fork) sur le serveur.
Le serveur ne doit contenir que des plugins fabriqués ici, donc l'interface vit dans
`sources/api/…/valoriateconomy/`, compilée **deux fois** par le même build : dans le jar de
ValoriaTycoon (où le code du plugin la résout) et dans celui de ValoriaEconomy (où le fournisseur
l'implémente). C'est exactement le rôle que jouait le jar VaultAPI, sans la dépendance.

    python3 scripts/generate-economy-api.py            # (re)écrit les deux fichiers
    python3 scripts/generate-economy-api.py --check    # contrôle qu'ils sont à jour
"""

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / "docs" / "economy-api.txt"
API_DIR = ROOT / "sources" / "api" / "xyz" / "arcadiadevs" / "valoriateconomy"
INTERFACE = API_DIR / "Economy.java"
PROVIDER = ROOT / "sources" / "economy" / "xyz" / "arcadiadevs" / "valoriaeconomy" / "ValoriaEconomyProvider.java"

METHOD_RE = re.compile(r"\n\s*public\s+([\w<>,\[\]. ]+?)\s+(\w+)\s*\(([^;{)]*)\)\s*;")

# Format du snapshot : une signature par ligne, sans `public` ni `;`.
SNAPSHOT_RE = re.compile(r"^([\w.]+(?:<[^>]*>)?)\s+(\w+)\s*\(([^()]*)\)$")

# Types de l'API et leurs importations. « EconomyResponse » est un nom de NOTRE paquet : il n'est
# jamais importé (même paquet), d'où son absence ici.
IMPORTS = {
    "OfflinePlayer": "org.bukkit.OfflinePlayer",
    "List": "java.util.List",
    "String": None,
    "double": None,
    "boolean": None,
    "int": None,
}

# Corps du fournisseur, par nom de méthode. Les paramètres sont nommés comme dans l'API
# (player/playerName/amount/name/world) pour que le corps reste lisible et vérifiable à l'œil.
BODIES = {
    "isEnabled": ["        return true;"],
    "getName": ["        return NAME;"],
    "hasBankSupport": ["        return false;"],
    "fractionalDigits": ["        return 2;"],
    "formatMoney": ["        return this.balances.format(amount);"],
    "currencyNamePlural": ["        return this.balances.currencyPlural();"],
    "currencyNameSingular": ["        return this.balances.currencySingular();"],
    "getBanks": ["        return new ArrayList<String>();"],
}


def parse_snapshot(text: str):
    """Retourne [(retour, nom, params)] dans l'ordre du fichier de signature.

    Les deux formats sont acceptes : le snapshot (une signature par ligne) et un vritable fichier
    source d'API (methodes d'interface terminez par `;`), ce qui evite un passage manuel.
    """
    found = METHOD_RE.findall("\n" + text)
    if found:
        return [(ret.strip(), name.strip(), params.strip()) for ret, name, params in found]
    out = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        match = SNAPSHOT_RE.match(line)
        if match is None:
            raise SystemExit(f"ERREUR: ligne de snapshot invalide: {line!r}")
        ret, name, params = match.groups()
        out.append((shorten(ret), name, ", ".join(shorten(part.strip()) for part in params.split(",") if part.strip())))
    return out


def shorten(type_name: str) -> str:
    """`java.util.List<String>` -> `List<String>` : le fichier genere importe les types."""
    return re.sub(r"\b(?:[a-z][\w]*\.)+([A-Z]\w*)", r"\1", type_name)


def imports_for(methods):
    needed = set()
    for ret, _name, params in methods:
        for token in re.findall(r"[A-Za-z_][\w.]*", ret + " " + params):
            if token in IMPORTS and IMPORTS[token]:
                needed.add(IMPORTS[token])
    return sorted(needed)


def render_interface(methods) -> str:
    out = ["package xyz.arcadiadevs.valoriateconomy;", ""]
    for imp in imports_for(methods):
        out.append(f"import {imp};")
    out += [
        "",
        "/**",
        " * L'économie du serveur, telle que ValoriaTycoon (et n'importe quel autre plugin du dépôt)",
        " * la consultera : un service {@code ServicesManager} enregistré sous cette interface.",
        " *",
        " * <h2>Pourquoi cette interface existe ici</h2>",
        " * <p>Le plugin d'origine passait par l'API Vault, ce qui obligeait à installer un plugin Vault",
        " * (ou son fork maintenu) en plus de la banque elle-même. Le serveur ne devant contenir que des",
        " * plugins fabriqués dans ce dépôt, la monnaie est exposée sous notre propre interface, avec la",
        " * <b>même</b> surface de méthodes que l'appel compilé attend — les corps ont été relevés dans les",
        " * {@code .class} livrés (voir {@code scripts/verify-paper26-compat.py}).</p>",
        " *",
        " * <h2>Règles d'or</h2>",
        " * <ul>",
        " *   <li><b>Ne jamais retirer une méthode.</b> Une signature disparue = {@code NoSuchMethodError}",
        " *       au premier appel d'un plugin. Ajouter est permis.</li>",
        " *   <li><b>Aucune dépendance externe.</b> Cette interface n'importe que le JDK, Bukkit et",
        " *       {@link EconomyResponse}.</li>",
        " *   <li>Les fournisseurs qui ne gèrent pas les banques renvoient",
        " *       {@link EconomyResponse#NOT_IMPLEMENTED} au lieu de lever une exception : un appelant",
        " *       mal intentionné ne doit pas faire tomber le serveur.</li>",
        " * </ul>",
        " *",
        " * <p><b>Fichier généré</b> par {@code scripts/generate-economy-api.py} depuis",
        " * {@code docs/economy-api.txt} : ajouter une méthode se fait dans le snapshot, pas ici.</p>",
        " */",
        "public interface Economy {",
        "",
    ]
    for ret, name, params in methods:
        out.append("")
        out.append(f"    {ret} {name}({params});" if params else f"    {ret} {name}();")
    out += [
        "",
        "    /**",
        "     * Le texte de prix que le plugin place dans ses interfaces et ses hologrammes.",
        "     *",
        "     * <p>Le code déjà compilé de ValoriaTycoon appelle {@code format(double)} (relevé dans",
        "     * {@code SellUtil}, {@code GeneratorsGui}, {@code UpgradeGui}, {@code WandData$Wand} et",
        "     * {@code ValoriaTycoon}). La méthode porte donc ce nom, et {@link #formatMoney(double)} est",
        "     * le nom sous lequel l'implémentation l'expose : un fournisseur peut surcharger l'un ou",
        "     * l'autre sans casser l'autre.</p>",
        "     */",
        "    default String format(double amount) {",
        "        return formatMoney(amount);",
        "    }",
        "}",
        "",
    ]
    return "\n".join(out)


def body_for(name: str, params: str):
    if name in BODIES:
        return BODIES[name]
    who = "playerName" if params.startswith("String playerName") else "player"
    world = "world" in params
    amount = re.search(r"double\s+(\w+)", params)
    value = amount.group(1) if amount else "0.0D"

    if name == "hasAccount":
        return [f"        return this.balances.exists({who});"]
    if name == "getBalance":
        return [f"        return this.balances.balance({who});"]
    if name == "has":
        return [f"        return this.balances.balance({who}) >= {value};"]
    if name == "createPlayerAccount":
        return [f"        return this.balances.ensureAccount({who});"]
    if name == "withdrawPlayer":
        # `Balances.withdraw` renvoie -1 quand le solde est insuffisant : on ne propage jamais ce
        # sentinel comme « nouveau solde », un appelant qui afficherait -1,00 € ferait fuir un joueur.
        return [
            f"        double before = this.balances.balance({who});",
            f"        double after = this.balances.withdraw({who}, {value});",
            "        if (after < 0.0D || !Double.isFinite(after)) {",
            '            return new EconomyResponse(0.0D, before, EconomyResponse.ResponseType.FAILURE, "Solde insuffisant");',
            "        }",
            f"        return new EconomyResponse({value}, after, EconomyResponse.ResponseType.SUCCESS, null);",
        ]
    if name == "depositPlayer":
        return [
            f"        double after = this.balances.deposit({who}, {value});",
            f"        return new EconomyResponse({value}, after, EconomyResponse.ResponseType.SUCCESS, null);",
        ]
    if name.startswith("bank") or name.endswith("Bank") or name in ("isBankOwner", "isBankMember"):
        return ["        return EconomyResponse.NOT_IMPLEMENTED;"]
    return ["        return EconomyResponse.NOT_IMPLEMENTED;"]


def render_provider(methods) -> str:
    out = [
        "package xyz.arcadiadevs.valoriaeconomy;",
        "",
        "import java.util.ArrayList;",
        "import xyz.arcadiadevs.valoriateconomy.Economy;",
        "import xyz.arcadiadevs.valoriateconomy.EconomyResponse;",
        "",
        "/**",
        " * Fournisseur d'économie du serveur : il adapte {@link Balances} vers l'interface",
        " * {@link Economy} que consultent ValoriaTycoon et les autres plugins du dépôt.",
        " *",
        " * <p><b>Fichier généré</b> par {@code scripts/generate-economy-api.py} — ne pas éditer à la",
        " * main. {@code scripts/verify-economy-api.py} refuse tout écart entre l'interface, ce fichier et",
        " * le snapshot {@code docs/economy-api.txt} : une méthode d'interface non implémentée est une",
        " * erreur de compilation, mais une signature qui ne correspond plus à l'interface est un",
        " * {@code AbstractMethodError} silencieux en jeu.</p>",
        " *",
        " * <p>Les banques renvoient {@link EconomyResponse#NOT_IMPLEMENTED} (jamais d'exception) :</p>",
        " * <ul>",
        " *   <li>un plugin qui en a besoin reste libre d'utiliser un autre fournisseur ;</li>",
        " *   <li>le serveur ne peut pas être mis à genoux par une commande de banque mal formée.</li>",
        " * </ul>",
        " */",
        "public final class ValoriaEconomyProvider implements Economy {",
        "",
        '    private static final String NAME = "ValoriaEconomy";',
        "",
        "    private final Balances balances;",
        "",
        "    public ValoriaEconomyProvider(Balances balances) {",
        "        this.balances = balances;",
        "    }",
        "",
    ]
    for ret, name, params in methods:
        args = params if params else ""
        out.append("    @Override")
        out.append(f"    public {ret} {name}({args}) {{")
        out += body_for(name, params)
        out.append("    }")
        out.append("")
    # format(double) est un default dans l'interface : le fournisseur ne l'écrase pas, mais le
    # mentionne explicitement pour que la lecture du fichier suffise à comprendre d'où vient le texte.
    out += [
        "    // format(double) herite du default de Economy -> formatMoney(double) ci-dessus.",
        "}",
        "",
    ]
    return "\n".join(out)


def write_if_changed(path: Path, content: str, check: bool, label: str) -> int:
    current = path.read_text(encoding="utf-8") if path.is_file() else None
    if check:
        if current != content:
            print(f"ERREUR: {label} obsoete — lancer `python3 scripts/generate-economy-api.py`", file=sys.stderr)
            return 1
        print(f"OK: {label} a jour ({len(content.splitlines())} lignes).")
        return 0
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"écrit: {path.relative_to(ROOT)} ({len(content.splitlines())} lignes)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true", help="vérifie que les fichiers générés sont à jour")
    parser.add_argument("--refresh", metavar="FICHIER", help="recrée le snapshot depuis un Economy.java source")
    args = parser.parse_args()

    if args.refresh:
        source = Path(args.refresh).read_text(encoding="utf-8")
        methods = parse_snapshot(source)
        if not methods:
            print("ERREUR: aucune méthode trouvée dans le fichier source", file=sys.stderr)
            return 1
        lines = [f"{ret} {name}({params})" if params else f"{ret} {name}()" for ret, name, params in methods]
        SNAPSHOT.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"snapshot écrit: {SNAPSHOT.relative_to(ROOT)} ({len(lines)} signatures)")
        return 0

    if not SNAPSHOT.is_file():
        print(f"ERREUR: snapshot introuvable: {SNAPSHOT.relative_to(ROOT)}", file=sys.stderr)
        return 1

    methods = parse_snapshot(SNAPSHOT.read_text(encoding="utf-8"))
    if len(methods) < 40:
        print(f"ERREUR: {len(methods)} méthode(s) dans le snapshot, ce n'est pas la surface attendue",
              file=sys.stderr)
        return 1

    interface = render_interface(methods)

    failed = write_if_changed(INTERFACE, interface, args.check, "Economy.java (interface)")
    failed |= write_if_changed(PROVIDER, render_provider(methods), args.check, "ValoriaEconomyProvider.java")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
