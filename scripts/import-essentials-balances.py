#!/usr/bin/env python3
"""Importe les soldes EssentialsX vers plugins/ValoriaEconomy/economy.yml.

Usage serveur arrêté :

    python3 scripts/import-essentials-balances.py --dry-run
    python3 scripts/import-essentials-balances.py

Un compte déjà présent dans economy.yml n'est JAMAIS écrasé (le solde en cours gagne). Les fichiers de
joueurs illisibles ou sans champ `money` sont signalés et ignorés. Rien n'est écrit en mode --dry-run.
"""

import argparse
import re
import sys
import uuid as uuidlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read_money(path: Path):
    """Extrait `money: <valeur>` d'un fichier userdata d'Essentials (YAML simple, sans dépendance)."""
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError as error:
        return None, "lecture impossible (%s)" % error
    match = re.search(r"^money:\s*([-+0-9.eE]+)\s*$", text, re.M)
    if not match:
        return None, "champ money absent"
    try:
        value = float(match.group(1))
    except ValueError:
        return None, "montant illisible (%r)" % match.group(1)
    if value != value or value < 0.0:
        return None, "montant refusé (%s)" % match.group(1)
    return round(value * 100.0) / 100.0, None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dry-run", action="store_true", help="affiche sans écrire")
    parser.add_argument("--userdata", default="plugins/Essentials/userdata")
    parser.add_argument("--economy", default="plugins/ValoriaEconomy/economy.yml")
    args = parser.parse_args()

    userdata = Path(args.userdata)
    if not userdata.is_dir():
        print("ERREUR : %s introuvable (serveur arrêté, dossier Essentials présent ?)" % userdata, file=sys.stderr)
        return 1
    economy = Path(args.economy)

    existing = set()
    if economy.is_file():
        existing = set(re.findall(r"^\s{2}([0-9a-fA-F-]{36}):", economy.read_text(), re.M))

    lines, added, skipped = [], 0, 0
    for path in sorted(userdata.glob("*.yml")):
        candidate = path.stem
        try:
            identity = str(uuidlib.UUID(candidate))
        except ValueError:
            skipped += 1
            continue
        money, problem = read_money(path)
        if problem:
            print("  ignore %s : %s" % (candidate[:8], problem))
            skipped += 1
            continue
        if identity in existing:
            skipped += 1
            continue
        nickname = path.read_text(encoding="utf-8", errors="replace")
        match = re.search(r"^nickname:\s*(\S+)\s*$", nickname, re.M)
        name = match.group(1).strip(chr(39) + chr(34)) if match else candidate[:8]
        lines.append("  %s:\n    balance: %.2f\n    name: %s" % (identity, money, name))
        added += 1

    print("à importer : %d compte(s), ignorés : %d" % (added, skipped))
    if not lines or args.dry_run:
        for line in lines[:5]:
            print("  " + line.replace("\n", " | "))
        if args.dry_run and added:
            print("(dry-run : rien n'a été écrit)")
        return 0

    body = "accounts:\n" + "\n".join(lines) + "\nsaved-at: 0\n"
    economy.parent.mkdir(parents=True, exist_ok=True)
    if economy.is_file():
        current = economy.read_text()
        if "accounts:" in current:
            body = current.replace("accounts:\n", "accounts:\n" + "\n".join(lines) + "\n", 1)
    (economy.parent / (economy.name + ".tmp")).write_text(body, encoding="utf-8")
    (economy.parent / (economy.name + ".tmp")).replace(economy)
    print("écrit : %s (%d compte(s) ajoutés)" % (economy, added))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
