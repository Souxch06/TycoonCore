#!/usr/bin/env python3
"""(Re)construit artifacts/reference/valoria-renamed.jar, le classpath de compilation.

Pourquoi ce fichier existe : le dépôt ne contient que des sources décompilées, dont beaucoup ne
compilent pas (type conflicts, résidus du décompileur). On ne peut donc pas recompiler le plugin
entier pour corriger une seule classe. La compilation est ciblée (liste <includes> du pom.xml), et
javac a besoin de résoudre les symboles référencés par la fichier compilé : GuiLib, XSeries,
ChatUtil, et les classes du plugin lui-même. Ils sont tous disponibles... en tant que .class, dans
artifacts/extracted. Ce script en fait un JAR minimal que le pom.xml ajoute au classpath (portée
« system »), ce qui permet de recompiler une classe isolément contre le binaire réellement livré.

Contenu : uniquement les classes et ressources non-API du paquet (pas org/bukkit, qui vient du
serveur). Le fichier est régénéré à chaque correction de classe ; la vérification CI s'assure qu'il
est cohérent avec artifacts/extracted.

    python3 scripts/build-reference-jar.py            # régénère
    python3 scripts/build-reference-jar.py --check     # échoue si obsolète
"""

from pathlib import Path
import argparse
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXTRACTED = ROOT / "artifacts" / "extracted"
OUT = ROOT / "artifacts" / "reference" / "valoria-renamed.jar"

# Le manifest du paquet livré n'a pas de sens pour un classpath de compilation.
EXCLUDED = {"META-INF/MANIFEST.MF"}

# Arbres qui ne doivent JAMAIS réapparaître dans le classpath de compilation : ce sont les API
# tierces que le dépôt remplace par du code écrit ici (l'interface d'économie à la place de Vault,
# notre moteur d'hologrammes à la place de HoloEasy). Les laisser passer masquerait nos sources :
# javac résoudrait `Economy` depuis ce JAR et non depuis `sources/api`, et un membre oublié ne
# serait jamais signalé.
FORBIDDEN_PREFIXES = ("net/milkbowl/", "org/holoeasy/")


def build() -> bytes:
    from io import BytesIO

    buffer = BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in sorted(EXTRACTED.rglob("*")):
            if not path.is_file():
                continue
            relative = path.relative_to(EXTRACTED).as_posix()
            if relative in EXCLUDED:
                continue
            if relative.startswith(FORBIDDEN_PREFIXES):
                raise SystemExit(
                    f"ERREUR: {relative} est une API tierce : artifacts/extracted ne doit plus la "
                    "livrer (lancer scripts/selfmade-api-patch.py --check)"
                )
            jar.write(path, relative)
    return buffer.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true", help="vérifie que le JAR reflète l'extraction")
    args = parser.parse_args()

    if not EXTRACTED.is_dir():
        print(f"ERREUR: {EXTRACTED} introuvable", file=__import__("sys").stderr)
        return 1

    payload = build()
    if args.check:
        if not OUT.is_file():
            print(f"ERREUR: {OUT.relative_to(ROOT)} manquant", file=__import__("sys").stderr)
            return 1
        with zipfile.ZipFile(OUT) as existing, zipfile.ZipFile(__import__("io").BytesIO(payload)) as fresh:
            old_names = set(existing.namelist())
            new_names = set(fresh.namelist())
        stale = sorted(n for n in old_names if n.startswith(FORBIDDEN_PREFIXES))
        if stale:
            print(f"ERREUR: {OUT.relative_to(ROOT)} embarque encore une API tierce: {stale[:5]}",
                  file=__import__("sys").stderr)
            return 1
        if old_names != new_names:
            missing = sorted(new_names - old_names)[:10]
            extra = sorted(old_names - new_names)[:10]
            print(f"ERREUR: {OUT.relative_to(ROOT)} obsolète\n  manquants: {missing}\n  en trop: {extra}",
                  file=__import__("sys").stderr)
            return 1
        print(f"OK: {OUT.relative_to(ROOT)} cohérent avec artifacts/extracted ({len(new_names)} entrées).")
        return 0

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(payload)
    with zipfile.ZipFile(OUT) as jar:
        count = len(jar.namelist())
    print(f"écrit: {OUT.relative_to(ROOT)} ({count} entrées, {len(payload) // 1024} Kio)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
