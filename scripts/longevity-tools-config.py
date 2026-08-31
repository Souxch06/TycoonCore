#!/usr/bin/env python3
"""Longévité du multi-outil : ce que coûtent les paliers, les niveaux de capacité, et une âme entière.

Tout part du `config.yml` **livré** (`resources-tools/config.yml`), avec les formules réellement appliquées
par le moteur — `ToolsConfig.priceOf` pour un palier (`price-base × price-ratio^palier`, plafonné par
`upgrade.price-cap` s'il est écrit) et `ToolsConfig.Ability.priceAt` pour un niveau (`price + price-step ×
(niveau-1)`, plafonné par `price-cap` puis par `ability-price.cap`). Rien n'est recopié d'un tableau : ce
script EST le tableau de `docs/MULTI-OUTIL.md`.

    python3 scripts/longevity-tools-config.py            # tableau pret a coller
    python3 scripts/longevity-tools-config.py --hours 2000 20000 200000

Les revenus ($/heure) sont hypothetiques : le script ne devine pas l'économie du serveur, il divise.
"""
from __future__ import annotations

import os
import re
import sys

KINDS = ("pickaxe", "axe", "sword", "rod")
LABEL = {"pickaxe": "Pioche", "axe": "Hache / houe", "sword": "Épée", "rod": "Canne à pêche"}


def money(value):
    if value >= 1e9:
        return "%.2f Md$" % (value / 1e9)
    if value >= 1e6:
        return "%.1f M$" % (value / 1e6)
    if value >= 1e3:
        return "%.1f k$" % (value / 1e3)
    return "%.0f $" % value


def read_souls(config_path):
    """Les quatre âmes : courbe des paliers, défauts de prix des capacités, lignes `abilities:`."""
    souls = dict((kind, {"abilities": [], "ap": {}, "label": LABEL[kind]}) for kind in KINDS)
    kind = None
    with open(config_path, encoding="utf-8") as handle:
        for line in handle.read().splitlines():
            head = re.match(r"^  (%s):$" % "|".join(KINDS), line)
            if head:
                kind = head.group(1)
                continue
            if kind is None:
                continue                        # les commentaires d'en-tete du fichier
            soul = souls[kind]
            match = re.match(r"^      (price-base|price-ratio|max-tier|price-cap): (-?[\d.]+)$", line)
            if match:
                soul[match.group(1)] = float(match.group(2))
                continue
            if re.match(r"^    ability-price:$", line):
                soul["_in_ap"] = True
                continue
            if soul.get("_in_ap"):
                match = re.match(r"^      (base|step|cap): (-?[\d.]+)$", line)
                if match:
                    soul["ap"][match.group(1)] = float(match.group(2))
                    continue
                if line.strip() and not line.lstrip().startswith("#"):
                    soul["_in_ap"] = False
            match = re.match(r"^      - \{id: ([a-z0-9-]+),(.*)\}$", line)
            if match:
                body = match.group(2)

                def number(key):
                    found = re.search(r"(?<![a-z-])%s: (-?[\d.]+)" % re.escape(key), body)
                    return float(found.group(1)) if found else None
                soul["abilities"].append({
                    "id": match.group(1),
                    "n": int(number("max-level") or 1),
                    "price": number("price"),
                    "step": number("price-step"),
                    "cap": number("price-cap"),
                    "unlock": int(number("unlock") or 1),
                    "free": "free: true" in body,
                })
    for soul in souls.values():
        soul.setdefault("price-base", 0.0)
        soul.setdefault("price-ratio", 1.0)
        soul.setdefault("max-tier", 1.0)
        soul["ap"].setdefault("base", 0.0)
        soul["ap"].setdefault("step", 0.0)
        soul["ap"].setdefault("cap", 0.0)
    return souls


def tier_price(soul, tier):
    """Ce que le moteur demande pour MONTER AU palier `tier` (le palier 1 est offert a la fabrication)."""
    price = soul["price-base"] * soul["price-ratio"] ** min(120, int(tier) - 1)
    cap = soul.get("price-cap") or 0.0
    return min(price, cap) if cap > 0 else price


def level_price(soul, ability, level):
    base = ability["price"] if ability["price"] is not None else soul["ap"]["base"]
    step = ability["step"] if ability["step"] is not None else soul["ap"]["step"]
    cap = ability["cap"] or soul["ap"].get("cap") or 0.0
    if ability["free"] and level <= 1:
        return 0.0
    price = base + step * (level - 1)
    return min(price, cap) if cap > 0 else price


def ability_total(soul, ability):
    return sum(level_price(soul, ability, level) for level in range(1, ability["n"] + 1))


def main(argv):
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    hours = [float(a) for a in argv if a.replace(".", "").isdigit()]
    souls = read_souls(os.path.join(here, "resources-tools", "config.yml"))
    pick = souls["pickaxe"]
    max_tier = int(pick["max-tier"])

    print("### La ligne d'âme (paliers)\n")
    print("| monter au palier | prix | cumulé depuis le palier 1 |")
    print("| --- | --- | --- |")
    running = 0.0
    for tier in range(2, max_tier + 1):
        running += tier_price(pick, tier)
        if tier in (2, 3, 5, 10, 20, 30, 40, 45, max_tier):
            print("| %d | %s | %s |" % (tier, money(tier_price(pick, tier)), money(running)))
    print("\nLa grille est identique sur les quatre âmes ; le palier 1 est celui de la fabrication, il est "
          "offert.\n")

    print("### La ligne de capacité (plage publiée par le wiki → prix)\n")
    print("| plage de niveaux | le premier niveau | le dernier | maxer la capacité |")
    print("| --- | --- | --- | --- |")
    for span in sorted({ability["n"] for soul in souls.values() for ability in soul["abilities"]}):
        rows = [a for soul in souls.values() for a in soul["abilities"] if a["n"] == span]
        # un echantillon payant : une capacite offerte au palier 1 n'a rien a faire dans un tableau de prix
        sample = next((a for a in rows if not a["free"]), rows[0])
        if sample is None:
            continue
        first = level_price(pick, sample, 2 if sample["free"] else 1)
        print("| %s | %s | %s | %s |" % (
            "1 (binaire)" if span == 1 else "1 → %s" % ("{:,}".format(span).replace(",", " ")),
            money(first), money(level_price(pick, sample, span)), money(ability_total(pick, sample))))

    print("\n### Une âme entière\n")
    print("| âme | capacités | paliers 1→50 | toutes les capacités maxées | total |")
    print("| --- | --- | --- | --- | --- |")
    grand = 0.0
    for kind in KINDS:
        soul = souls[kind]
        tiers = sum(tier_price(soul, tier) for tier in range(2, int(soul["max-tier"]) + 1))
        abilities = sum(ability_total(soul, row) for row in soul["abilities"])
        grand += tiers + abilities
        print("| %s | %d | %s | %s | %s |" % (
            soul["label"], len(soul["abilities"]), money(tiers), money(abilities), money(tiers + abilities)))
    print("| **les quatre** | | | | **%s** |" % money(grand))
    if hours:
        print("\n### Et en temps de jeu\n")
        print("(le revenu dépend du serveur : une âme qui revend la pierre 1 $ le bloc, comme le livre le "
              "barème, fait `blocs minés × 1 $`)\n")
        print("| revenu | une âme | les quatre âmes |")
        print("| --- | --- | --- |")
        for rate in sorted(hours):
            print("| %s $/h | %s h | %s h |" % (
                "{:,.0f}".format(rate).replace(",", " "),
                "{:,.0f}".format((grand / 4) / rate).replace(",", " "),
                "{:,.0f}".format(grand / rate).replace(",", " ")))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
