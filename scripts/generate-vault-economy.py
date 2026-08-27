#!/usr/bin/env python3
"""Régénère sources/economy/…/VaultEconomy.java depuis la surface réelle de l'API Vault.

Pourquoi générer : l'interface {@code net.milkbowl.vault.economy.Economy} de VaultAPI 1.7 compte 43
méthodes. En oublier une seule, ou en décaler une signature, se paie soit en échec de compilation,
soit — pire, car silencieux — en AbstractMethodError en jeu chez le premier plugin qui appelle la
méthode manquante (boutique, donneur, téléport payant…). Les signatures sont donc lues dans le source
officiel et stockées dans docs/vault-economy-api.txt ; cette classe les émet, et
scripts/verify-economy-api.py refuse tout écart entre le snapshot, la classe générée et les sources.

    # après avoir mis à jour l'API Vault :
    gh api "repos/MilkBowl/VaultAPI/contents/src/main/java/net/milkbowl/vault/economy/Economy.java?ref=master" \
        -q .content | base64 -d > /tmp/Economy.java
    python3 scripts/generate-vault-economy.py --refresh /tmp/Economy.java   # recrée le snapshot
    python3 scripts/generate-vault-economy.py                               # puis régénère la classe
    python3 scripts/verify-economy-api.py                                   # contrôle de couverture
"""

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / "docs" / "vault-economy-api.txt"
OUT = ROOT / "sources" / "economy" / "xyz" / "arcadiadevs" / "valoriaeconomy" / "VaultEconomy.java"

METHOD_RE = re.compile(r'\n\s*public\s+([\w<>,\[\]. ]+?)\s+(\w+)\s*\(([^;{)]*)\)\s*;')

# Corps par nom de méthode. Les paramètres sont toujours nommés comme dans l'API (player/playerName/
# amount/name/world) pour que le corps reste lisible et vérifiable à l'œil.
BODIES = {
    'isEnabled': ['        return true;'],
    'getName': ['        return NAME;'],
    'hasBankSupport': ['        return false;'],
    'fractionalDigits': ['        return 2;'],
    'format': ['        return this.balances.format(amount);'],
    'currencyNamePlural': ['        return this.balances.currencyPlural();'],
    'currencyNameSingular': ['        return this.balances.currencySingular();'],
    'getBanks': ['        return new ArrayList<String>();'],
}

PLAYER_ARG = {'playerName': 'playerName', 'player': 'player'}


def body_for(name, params):
    """Corps d'une méthode, déduit du nom et de la forme des paramètres."""
    if name in BODIES:
        return BODIES[name]
    who = 'playerName' if params.strip().startswith('String playerName') else 'player'
    world = 'world' in params or 'worldName' in params
    amount = re.search(r'double\s+(\w+)', params)
    value = amount.group(1) if amount else '0.0D'

    if name == 'hasAccount':
        return ['        return this.balances.exists(%s);' % who]
    if name == 'getBalance':
        return ['        return this.balances.balance(%s);' % who]
    if name == 'has':
        return ['        return this.balances.balance(%s) >= %s;' % (who, value)]
    if name == 'createPlayerAccount':
        return ['        return this.balances.ensureAccount(%s);' % who]
    if name == 'withdrawPlayer':
        return [
            '        double before = this.balances.balance(%s);' % who,
            '        double after = this.balances.withdraw(%s, %s);' % (who, value),
            '        if (after < 0.0D) {',
            '            return new EconomyResponse(0.0D, before, ResponseType.FAILURE, "Solde insuffisant");',
            '        }',
            '        return new EconomyResponse(%s, after, ResponseType.SUCCESS, null);' % value,
        ]
    if name == 'depositPlayer':
        return [
            '        double after = this.balances.deposit(%s, %s);' % (who, value),
            '        return new EconomyResponse(%s, after, ResponseType.SUCCESS, null);' % value,
        ]
    if name.startswith('bank') or name in ('createBank', 'deleteBank', 'isBankOwner', 'isBankMember'):
        return [
            '        return new EconomyResponse(0.0D, %s, ResponseType.NOT_IMPLEMENTED,' % (value if amount else '0.0D'),
            '                "ValoriaEconomy n\'implémente pas les banques ; garde un plugin de banques à côté si un",',
            '                "plugin tiers en dépend.");',
        ]
    raise SystemExit('générateur : méthode non couverte -> %s(%s)' % (name, params))


def parse_snapshot(text):
    out = []
    for line in filter(None, (raw.strip() for raw in text.splitlines())):
        match = re.match(r'^(.+?)\s+(\w+)\((.*)\)$', line, re.S)
        if not match:
            raise SystemExit('snapshot illisible : ' + line)
        out.append((re.sub(r'\s+', ' ', match.group(1)).strip(), match.group(2),
                    re.sub(r'\s+', ' ', match.group(3)).strip()))
    return out


def render(sigs):
    methods = []
    for ret, name, params in sigs:
        args = []
        for raw in filter(None, (p.strip() for p in params.split(','))):
            parts = raw.rsplit(' ', 1)
            if len(parts) == 2:
                args.append('final %s %s' % (parts[0], parts[1]))
            else:
                args.append('final %s arg%d' % (parts[0], len(args)))
        methods.append('    @Override\n    public %s %s(%s) {\n%s\n    }'
                       % (ret, name, ', '.join(args), '\n'.join(body_for(name, params))))
    return HEADER % (len(sigs), '\n\n'.join(methods))


HEADER = '''package xyz.arcadiadevs.valoriaeconomy;

import java.util.ArrayList;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;

/**
 * Fournisseur d'économie exposé à Vault. FICHIER GÉNÉRÉ — ne pas éditer à la main.
 *
 * <p>%d méthodes, émises depuis docs/vault-economy-api.txt par scripts/generate-vault-economy.py.
 * Si l'API Vault change (ou si un membre est ajouté ici à la main), lance le générateur puis
 * scripts/verify-economy-api.py : une méthode d'interface non implémentée est une erreur de
 * compilation, mais une signature qui ne correspond plus à l'interface est un AbstractMethodError
 * silencieux en jeu pour les autres plugins.</p>
 *
 * <p>Les banques renvoient ResponseType.NOT_IMPLEMENTED (jamais d'exception) : un plugin qui en a
 * besoin doit rester libre d'utiliser un autre fournisseur.</p>
 */
public final class VaultEconomy implements Economy {

    private static final String NAME = "ValoriaEconomy";

    private final Balances balances;

    public VaultEconomy(Balances balances) {
        this.balances = balances;
    }

%s
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--refresh', metavar='ECONOMY_JAVA',
                        help='recrée le snapshot docs/vault-economy-api.txt depuis le source Economy.java')
    parser.add_argument('--check', action='store_true', help='vérifie que la classe générée est à jour')
    args = parser.parse_args()

    if args.refresh:
        src = Path(args.refresh).read_text()
        sigs = [(re.sub(r'\s+', ' ', r).strip(), n, re.sub(r'\s+', ' ', p).strip())
                for r, n, p in METHOD_RE.findall(src)]
        if len(sigs) < 10:
            print('ERREUR : aucune méthode lue dans %s' % args.refresh, file=sys.stderr)
            return 1
        SNAPSHOT.parent.mkdir(exist_ok=True)
        SNAPSHOT.write_text('\n'.join('%s %s(%s)' % s for s in sigs) + '\n')
        print('snapshot régénéré : %d signatures' % len(sigs))
        return 0

    if not SNAPSHOT.is_file():
        print('ERREUR : %s manquant (voit --refresh)' % SNAPSHOT.relative_to(ROOT), file=sys.stderr)
        return 1

    sigs = parse_snapshot(SNAPSHOT.read_text())
    text = render(sigs)
    if args.check:
        if not OUT.is_file():
            print('ERREUR : %s manquant' % OUT.relative_to(ROOT), file=sys.stderr)
            return 1
        if OUT.read_text() != text:
            print('ERREUR : %s obsolète — relance scripts/generate-vault-economy.py'
                  % OUT.relative_to(ROOT), file=sys.stderr)
            return 1
        print('OK : %s à jour (%d méthodes)' % (OUT.relative_to(ROOT), len(sigs)))
        return 0
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text)
    print('écrit : %s (%d méthodes)' % (OUT.relative_to(ROOT), len(sigs)))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
