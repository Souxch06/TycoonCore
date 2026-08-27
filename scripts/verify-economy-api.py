#!/usr/bin/env python3
"""Contrôle hors-ligne de la couverture de l'API Vault par le fournisseur généré.

Sans compilateur à disposition, c'est ce contrôle qui garantit qu'aucune méthode de
net.milkbowl.vault.economy.Economy n'est oubliée ou décalée : une méthode manquante est normalement une
erreur de compilation, mais une signature qui ne correspond plus à l'interface devient un
AbstractMethodError silencieux en jeu, chez le premier plugin tiers qui l'appelle.

    python3 scripts/verify-economy-api.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / "docs" / "vault-economy-api.txt"
IMPL = ROOT / "sources/economy/xyz/arcadiadevs/valoriaeconomy/VaultEconomy.java"
GEN = ROOT / "scripts/generate-vault-economy.py"


def main() -> int:
    problems = []
    if not SNAPSHOT.is_file():
        print("ERREUR : snapshot manquant :", SNAPSHOT.relative_to(ROOT), file=sys.stderr)
        return 1
    if not IMPL.is_file():
        print("ERREUR : fournisseur manquant :", IMPL.relative_to(ROOT), file=sys.stderr)
        return 1

    def norm(text: str) -> str:
        """Forme canonique : virgule suivie d'un espace, pas d'espaces redondants (final ôté)."""
        text = re.sub(r"\s*final\s+", " ", text)
        text = re.sub(r"\s*,\s*", ", ", text)
        text = re.sub(r"\(\s+", "(", text)
        text = re.sub(r"\s+\)", ")", text)
        return re.sub(r"\s+", " ", text).strip()

    snapshot = [norm(line) for line in SNAPSHOT.read_text().splitlines() if line.strip()]
    impl = IMPL.read_text()
    declared = re.findall(r"@Override\n\s+public ([\w<>,\[\]. ]+?) (\w+)\(([^)]*)\)", impl)
    declared_norm = {norm("%s %s(%s)" % (r, n, p)) for r, n, p in declared}

    missing = [sig for sig in snapshot if sig not in declared_norm]
    if missing:
        problems.append("méthodes de l'API non implémentées (%d) : %s" % (len(missing), "; ".join(missing[:6])))
    extra = sorted(set(declared_norm) - set(snapshot))
    if extra:
        problems.append("méthodes implémentées hors API (%d) : %s" % (len(extra), "; ".join(extra[:6])))
    if len(snapshot) < 10:
        problems.append("snapshot suspect : %d signatures seulement" % len(snapshot))
    overrides = impl.count("@Override")
    if overrides != len(snapshot):
        problems.append("le fournisseur déclare %d @Override pour %d signatures d'API" % (overrides, len(snapshot)))

    # le générateur doit rester la source de vérité du fichier
    if GEN.is_file():
        result = __import__("subprocess").run([sys.executable, str(GEN), "--check"], capture_output=True, text=True)
        if result.returncode != 0:
            problems.append((result.stdout + result.stderr).strip().splitlines()[-1])

    for name in ("ValoriaEconomy.java", "Balances.java", "MoneyCommand.java"):
        path = IMPL.parent / name
        if not path.is_file():
            problems.append("fichier manquant : " + path.relative_to(ROOT).as_posix())
    main_class = IMPL.parent / "ValoriaEconomy.java"
    if main_class.is_file():
        text = main_class.read_text()
        if "ServicesManager" not in text or "Economy.class" not in text:
            problems.append("ValoriaEconomy n'enregistre pas le service Economy : ValoriaTycoon ne le verra pas")
        if "load: STARTUP" not in (ROOT / "resources-economy/plugin.yml").read_text():
            problems.append("resources-economy/plugin.yml doit porter load: STARTUP (sinon le service est "
                            "enregistré après l'onEnable de ValoriaTycoon)")

    if problems:
        print("ÉCHEC : couverture de l'API Vault", file=sys.stderr)
        for problem in problems:
            print("  -", problem, file=sys.stderr)
        return 1
    print("OK : %d signatures de l'API Vault couvertes par le fournisseur généré." % len(snapshot))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
