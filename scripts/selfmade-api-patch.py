#!/usr/bin/env python3
"""Remplace les API tierces appelées par les classes livrées par nos propres classes.

Le serveur ne doit contenir **aucun plugin téléchargé** : ni Vault (le pont d'API d'économie), ni
HoloEasy (les hologrammes, eux-mêmes dépendus de ProtocolLib). Or le paquet livré n'est pas un
projet recompilable en bloc — 61 classes décompilées, dont beaucoup ne compilent plus — donc le
plugin ne peut pas être corrigé en changeant ses ``import``. Les classes de ``artifacts/extracted``
sont recompilées **une par une** (liste ``<includes>`` du ``pom.xml``) et le reste du paquet est
repris tel quel.

Ce script traite donc le reste directement dans le constant-pool, en ne touchant **que** les
constantes ``CONSTANT_Utf8`` (noms internes et descripteurs) : c'est exactement ce que produirait
une recompilation depuis des sources qui importent nos classes, sans réécrire un seul octet de
bytecode. Les noms remplacés ont été relevés dans les ``.class`` livrés — voir
``scripts/verify-paper26-compat.py``, qui échoue si un seul subsiste.

Renommages effectués :

1. ``net/milkbowl/vault/economy/Economy`` (et ``EconomyResponse``) →
   ``xyz/arcadiadevs/valoriateconomy/…``, notre interface d'économie, compilée dans les deux jar.
2. ``org/holoeasy/…`` → ``xyz/arcadiadevs/valoriatycoon/hologram/…``, nos hologrammes (API
   identique : ``HologramBuilder.hologram``, ``textline``, ``item``, ``Hologram.getId``,
   ``IHologramPool.registerHolograms/get/remove``), et suppression de la bibliothèque embarquée.
3. Les deux gardes ``getPlugin("Vault")`` et ``getPlugin("HoloEasy")`` de la classe principale,
   qui refusaient de charger le plugin sans ces deux plugins : la chaîne cherchée devient
   ``ValoriaEconomy`` (notre second jar, toujours présent). Remplacée **en mode exact** uniquement,
   pour ne pas abîmer les phrases de log qui contiennent ces mots.
4. Le contrôle de licence SpigotMC : la classe principale ouvrait une connexion vers
   ``api.spigotmc.org`` à chaque démarrage et désactivait le plugin si la réponse valait
   exactement ``false``. L'URL est remplacée par un schéma invalide : ``openConnection`` lève une
   ``MalformedURLException`` (une ``IOException``), avalée par le ``catch`` déjà en place — le
   plugin ne contacte plus rien, et le fichier est sinon byte-identique.

Utilisation :
    python3 scripts/selfmade-api-patch.py            # applique (idempotent)
    python3 scripts/selfmade-api-patch.py --check    # contrôle l'arbre du dépôt
    python3 scripts/selfmade-api-patch.py --check --jar target/ValoriaTycoon-v1.6.3.jar
    python3 scripts/selfmade-api-patch.py --sources  # applique aussi aux sources décompilées
"""

from pathlib import Path
import argparse
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

import classfile  # noqa: E402

EXTRACTED = ROOT / "artifacts/extracted"
SOURCES = ROOT / "sources"

# --- 1. API d'économie : Vault -> la nôtre ---------------------------------------------------------
# L'ordre compte : `EconomyResponse` doit être traité avant `Economy`, dont il extend le préfixe.
ECONOMY_SUBSTRINGS = {
    b"net/milkbowl/vault/economy/EconomyResponse": b"xyz/arcadiadevs/valoriateconomy/EconomyResponse",
    b"net/milkbowl/vault/economy/Economy": b"xyz/arcadiadevs/valoriateconomy/Economy",
}
# Le message d'erreur historique suit la recherche : il serait mensonger de réclamer « Vault »
# alors que le plugin cherche désormais notre jar d'économie.
VAULT_GUARD_NAME = {
    b"Vault": b"ValoriaEconomy",
    b"Vault not found": b"ValoriaEconomy not found: install target/ValoriaEconomy-v1.6.3.jar",
}

# --- 2. Hologrammes : HoloEasy (paquets) -> les nôtres -------------------------------------------
# Les types utilisés par le bytecode livré, relevés dans les .class : cinq, et rien d'autre.
HOLO_SUBSTRINGS = {
    b"org/holoeasy/builder/interfaces/HologramRegisterGroup":
        b"xyz/arcadiadevs/valoriatycoon/hologram/HologramRegisterGroup",
    b"org/holoeasy/builder/interfaces/HologramSetupGroup":
        b"xyz/arcadiadevs/valoriatycoon/hologram/HologramSetupGroup",
    b"org/holoeasy/builder/HologramBuilder":
        b"xyz/arcadiadevs/valoriatycoon/hologram/HologramBuilder",
    b"org/holoeasy/hologram/Hologram":
        b"xyz/arcadiadevs/valoriatycoon/hologram/Hologram",
    b"org/holoeasy/pool/IHologramPool":
        b"xyz/arcadiadevs/valoriatycoon/hologram/HologramPool",
    b"org/holoeasy/HoloEasy":
        b"xyz/arcadiadevs/valoriatycoon/hologram/HoloEasy",
    # traces de paquets (frequence de log, metadonnees eventuelles)
    b"org.holoeasy": b"xyz.arcadiadevs.valoriatycoon.hologram",
}
# Le nom nu, uniquement en correspondance exacte : la phrase « HoloEasy not found. Disabling
# plugin. » garde son sens historique (elle ne se déclenche plus, voir HOLO_GUARD_NAME).
HOLO_EXACT = {b"HoloEasy": b"ValoriaEconomy"}
# Garde d'initialisation : le plugin exigeait un plugin HoloEasy pour activer les hologrammes.
HOLO_GUARD_NAME = {
    b"HoloEasy": b"ValoriaEconomy",
    b"HoloEasy not found. Disabling plugin.":
        b"Holograms unavailable: internal hologram engine failed. Disabling plugin.",
}

# --- 3. Contrôle de licence à distance -----------------------------------------------------------
LICENSE_URL = (b"https://api.spigotmc.org/legacy/premium.php?user_id=7516772&resource_id=110947"
               b"&nonce=-88465393")
DEAD_URL = b"valoriatycoon://aucun-controle-distant"

# Bibliothèques à retirer du paquet livré : plus aucune classe tierce sous ces chemins.
REMOVED_TREES = ("org/holoeasy", "net/milkbowl")
REMOVED_METADATA = (
    "META-INF/holoeasy-core.kotlin_module",
    "META-INF/maven/org.holoeasy",
)


def apply_to_bytes(data: bytes):
    """Applique tous les renommages à un fichier ``.class``. Retourne (nouveaux_octets, nb entrées)."""
    values = classfile.utf8_values(data)
    joined = "\n".join(values)
    current = data
    changed = 0

    substrings = {}
    if "milkbowl" in joined:
        substrings.update(ECONOMY_SUBSTRINGS)
    if "holoeasy" in joined:
        substrings.update(HOLO_SUBSTRINGS)
    if substrings:
        current, count = classfile.replace_utf8(current, substrings)
        changed += count

    # Correspondances exactes, en une seule passe : une constante déjà renommée ne doit pas
    # rencontrer une autre règle (« HoloEasy » -> « ValoriaEconomy » n'a rien à voir avec Economy).
    exacts = {}
    if "Vault" in joined:
        exacts.update(VAULT_GUARD_NAME)
    if "HoloEasy" in joined:
        exacts.update(HOLO_GUARD_NAME)
    if LICENSE_URL.decode() in values:
        exacts[LICENSE_URL] = DEAD_URL
    if exacts:
        current, count = classfile.replace_utf8(current, exacts, exact=True)
        changed += count
    return current, changed


def stale_values(data: bytes):
    """Constantes qui ne devraient plus exister dans un fichier après passage du correctif.

    On ne cherche que des chemins de paquet (``org/holoeasy``, ``net/milkbowl``), jamais le mot seul :
    la phrase de log « HoloEasy not found. Disabling plugin. » reste dans le fichier (elle ne peut
    plus se déclencher) et n'est pas une dépendance.
    """
    bad = []
    for value in classfile.utf8_values(data):
        low = value.lower()
        if "org/holoeasy" in low or "org.holoeasy" in low or "net/milkbowl" in low or "net.milkbowl" in low:
            bad.append(value)
        if LICENSE_URL.decode() in value:
            bad.append(value[:60] + "…")
    return bad


def stale_sources():
    """Sources décompilées qui parlent encore des API remplacées (documentation, pas compilation)."""
    problems = []
    for path in sorted(SOURCES.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        low = text.lower()
        if ("milkbowl" in low or "org.holoeasy" in low or "org/holoeasy" in low
                or "spigotmc.org/legacy/premium" in low):
            problems.append(path.relative_to(ROOT))
    return problems


def apply_tree() -> int:
    touched = 0
    for path in sorted(EXTRACTED.rglob("*.class")):
        original = path.read_bytes()
        patched, changed = apply_to_bytes(original)
        if changed:
            path.write_bytes(patched)
            touched += 1
    removed = 0
    for tree in REMOVED_TREES:
        target = EXTRACTED / tree
        if target.is_dir():
            for path in sorted(target.rglob("*"), reverse=True):
                if path.is_file():
                    path.unlink()
                    removed += 1
            for path in sorted(target.rglob("*"), reverse=True):
                if path.is_dir():
                    path.rmdir()
            if target.exists():
                target.rmdir()
    for name in REMOVED_METADATA:
        target = EXTRACTED / name
        if target.is_dir():
            for path in sorted(target.rglob("*"), reverse=True):
                if path.is_file():
                    path.unlink()
                    removed += 1
            for path in sorted(target.rglob("*"), reverse=True):
                if path.is_dir():
                    path.rmdir()
            target.rmdir()
        elif target.is_file():
            target.unlink()
            removed += 1
    print(f"classes corrigées : {touched} ; entrées tierces supprimées : {removed}")
    return 0


def check_tree() -> int:
    failures = 0
    for path in sorted(EXTRACTED.rglob("*.class")):
        bad = stale_values(path.read_bytes())
        if bad:
            failures += 1
            print(f"ERREUR: {path.relative_to(ROOT)} contient encore {len(bad)} référence(s) tierce(s) : "
                  f"{bad[:3]}", file=sys.stderr)
    for tree in REMOVED_TREES:
        if (EXTRACTED / tree).exists():
            failures += 1
            print(f"ERREUR: {tree} est encore livré dans artifacts/extracted", file=sys.stderr)
    for name in REMOVED_METADATA:
        if (EXTRACTED / name).exists():
            failures += 1
            print(f"ERREUR: {name} est encore livré (métadonnées d'une bibliothèque retirée)", file=sys.stderr)
    if failures:
        print(f"\n{failures} fichier(s) à corriger — lancer `python3 scripts/selfmade-api-patch.py`.",
              file=sys.stderr)
        return 1
    print("OK: plus aucune référence à Vault ni à HoloEasy dans les classes livrées, et le contrôle "
          "de licence à distance est neutralisé.")
    return 0


def check_jar(jar_path: Path) -> int:
    failures = 0
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        for tree in REMOVED_TREES:
            if any(n.startswith(tree + "/") for n in names):
                failures += 1
                print(f"ERREUR: le JAR embarque encore {tree}/", file=sys.stderr)
        for name in sorted(names):
            if not name.endswith(".class"):
                continue
            bad = stale_values(jar.read(name))
            if bad:
                failures += 1
                print(f"ERREUR: {name} référence encore une API tierce : {bad[:3]}", file=sys.stderr)
        our_classes = [
            "xyz/arcadiadevs/valoriateconomy/Economy.class",
            "xyz/arcadiadevs/valoriateconomy/EconomyResponse.class",
            "xyz/arcadiadevs/valoriatycoon/hologram/Hologram.class",
            "xyz/arcadiadevs/valoriatycoon/hologram/HologramPool.class",
            "xyz/arcadiadevs/valoriatycoon/hologram/HologramBuilder.class",
            "xyz/arcadiadevs/valoriatycoon/hologram/HoloEasy.class",
            "xyz/arcadiadevs/valoriatycoon/utils/HologramsUtil.class",
        ]
        for needed in our_classes:
            if needed not in names:
                failures += 1
                print(f"ERREUR: {needed} manque dans le JAR : le renommage vise des classes qui "
                      f"doivent être compilées par le build", file=sys.stderr)
    if failures:
        print(f"\n{failures} problème(s) dans {jar_path.name}.", file=sys.stderr)
        return 1
    print(f"OK: {jar_path.name} ne dépend d'aucune API tierce (nos classes remplacent Vault et HoloEasy).")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true", help="contrôle sans rien écrire")
    parser.add_argument("--sources", action="store_true",
                        help="rapporte aussi les sources décompilées qui parlent encore des API tierces")
    parser.add_argument("--jar", metavar="CHEMIN", help="contrôle un JAR compilé")
    args = parser.parse_args()

    if args.jar:
        return check_jar(Path(args.jar))
    if args.check:
        status = check_tree()
        if args.sources:
            problems = stale_sources()
            for path in problems:
                print(f"ATTENTION: {path} mentionne encore une API tierce (documentation à aligner)",
                      file=sys.stderr)
            if problems:
                print(f"{len(problems)} source(s) décompilée(s) à aligner (elles ne sont pas compilées).",
                      file=sys.stderr)
        return status
    return apply_tree()


if __name__ == "__main__":
    sys.exit(main())
