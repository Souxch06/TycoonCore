#!/usr/bin/env python3
"""Installe (ou vérifie) le pont NBT -> PersistentDataContainer dans les classes livrées.

Le plugin identifie ses générateurs via la bibliothèque embarquée
``io.github.bananapuncher714.nbteditor.NBTEditor``, qui résout par réflexion des noms de classes et de
méthodes obfusqués. Ces noms n'existent plus sur les serveurs récents (CraftBukkit non relocaté depuis
1.20.6, plus de jar obfusqué ni de remapper interne depuis Paper 26.1) : sur Paper 26.x, la bibliothèque
ne lève aucune erreur mais n'écrit et ne lit plus rien, ce qui rend les générateurs inertes.

La correction est en deux temps :

1. ``NBTEditor`` (et toutes ses classes imbriquées) est **rebaptisé** ``LegacyNbtBridge`` dans le JAR
   livré : l'implémentation d'origine reste ainsi disponible comme repli (blocs, entités, données
   écrites avant le pont, serveurs sans PersistentDataContainer). Seules les constantes CONSTANT_Utf8
   sont réécrites (noms internes et descripteurs), jamais le bytecode ; les fichiers sont renommés en
   conséquence pour rester cohérents avec leur ``this_class``.
2. ``NBTEditor`` devient la classe compilée depuis ``sources/shaded/io/github/bananapuncher714/nbteditor/NBTEditor.java``
   (pont PDC, même signature binaire pour les 5 membres appelés par le plugin : le champ
   ``CUSTOM_DATA`` et les méthodes ``set``/``contains``/``getInt``/``getString``). Elle n'est pas dans
   ``artifacts/extracted`` : c'est le build qui la produit (voir <includes> du maven-compiler-plugin).

Références attendues et non modifiées : les classes du plugin continuent d'appeler
``io/github/bananapuncher714/nbteditor/NBTEditor`` et ``NBTEditor$Type`` (résolus vers le pont compilé).

Utilisation :
    python3 scripts/install-nbt-bridge.py            # applique si nécessaire
    python3 scripts/install-nbt-bridge.py --check    # contrôle (CI)
"""

from pathlib import Path
import argparse
import re
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))

import ci_publish  # noqa: E402
import classfile  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
EXTRACTED = ROOT / "artifacts" / "extracted"
BRIDGE_SOURCE = ROOT / "sources/shaded/io/github/bananapuncher714/nbteditor/NBTEditor.java"
PACKAGE_DIR = EXTRACTED / "io" / "github" / "bananapuncher714" / "nbteditor"

# Le jeton complet est remplacé partout dans les seules classes legacy : il couvre à la fois les noms
# internes (« io/github/.../NBTEditor$ClassId »), les descripteurs (« Lio/github/.../NBTEditor$Type; »),
# les noms simples de l'attribut InnerClasses (« NBTEditor$ClassId ») et SourceFile (« NBTEditor.java »).
# La réécriture passe par classfile.replace_utf8, qui réémet les préfixes de longueur : un remplacement
# d'octets bruts corromprait le constant-pool.
TOKEN = "NBTEditor"
REPLACEMENT = "LegacyNbtBridge"

# Le plugin référence le pont par ces deux noms ; ils ne doivent surtout pas être renommés.
PONT_ALLOWED = {"io/github/bananapuncher714/nbteditor/NBTEditor",
                "io/github/bananapuncher714/nbteditor/NBTEditor$Type"}
CLASS_REF = re.compile(r"io/github/bananapuncher714/nbteditor/NBTEditor(?:\$[A-Za-z0-9_$]+)*")


def rename_class(data: bytes) -> bytes:
    patched, _changed = classfile.replace_utf8(data, {TOKEN.encode(): REPLACEMENT.encode()})
    return patched


def legacy_files() -> list:
    if not PACKAGE_DIR.is_dir():
        return []
    return sorted(p for p in PACKAGE_DIR.glob("NBTEditor*.class"))


def outside_references() -> list:
    """Classes hors du package legacy qui référencent une classe imbriquée de NBTEditor (inattendu)."""
    problems = []
    for path in sorted(EXTRACTED.rglob("*.class")):
        if PACKAGE_DIR in path.parents:
            continue
        try:
            values = set(classfile.utf8_values(path.read_bytes()))
        except (classfile.ClassFormatError, OSError):
            continue
        for value in values:
            if TOKEN not in value:
                continue
            # un descripteur (« L…NBTEditor$Type; ») ou une signature peuvent porter la référence :
            # on compare le nom de classe extrait, pas la chaîne entière.
            for match in CLASS_REF.findall(value):
                if match not in PONT_ALLOWED:
                    problems.append(f"{path.relative_to(EXTRACTED)}: référence legacy inattendue "
                                     f"{match!r} (dans {value!r})")
    return problems


def inner_name(jar_entry: str) -> str:
    """`…/NBTEditor$Keys.class` -> `NBTEditor$Keys`."""
    return jar_entry.rsplit("/", 1)[-1][: -len(".class")]


def pont_inner_classes() -> set:
    """Les classes internes LEGITIMES du pont : celles que nos sources declarent.

    Le pont NBT est recompilé depuis sources/shaded/…/NBTEditor.java ; javac en tire des classes
    internes (`NBTEditor$Type`, `NBTEditor$Keys`, `NBTEditor$Bukkit`, `NBTEditor$Legacy`). Les traiter
    comme des residus de l'ancienne librairie faisait echouer le controle « Pont NBT installe dans le
    JAR » sur un paquet PARFAITEMENT valide (run #33158279751) : le controle doit lire les sources,
    pas une liste tenue a la main.
    """
    if not BRIDGE_SOURCE.is_file():
        return {"NBTEditor$Type"}
    import re as _re

    text = BRIDGE_SOURCE.read_text(encoding="utf-8", errors="replace")
    text = _re.sub(r"/\*.*?\*/", "", text, flags=_re.S)
    names = set(_re.findall(r"\b(?:class|interface|enum|record)\s+([A-Z]\w*)", text))
    out = {f"NBTEditor${name}" for name in names if name != "NBTEditor"}
    out.add("NBTEditor")
    return out


def check_jar(jar_path: Path, problems: list):
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        bridge = "io/github/bananapuncher714/nbteditor/NBTEditor.class"
        legacy = "io/github/bananapuncher714/nbteditor/LegacyNbtBridge.class"
        if bridge not in names:
            problems.append(f"{jar_path.name}: pont {bridge} absent (le build ne l'a pas compilé ?)")
        if legacy not in names:
            problems.append(f"{jar_path.name}: repli {legacy} absent")
        stale = [n for n in names if n.startswith("io/github/bananapuncher714/nbteditor/NBTEditor$")
                 and inner_name(n) not in pont_inner_classes()]
        if stale:
            problems.append(f"{jar_path.name}: classes legacy non renommées : {sorted(stale)[:4]}")
        if bridge in names:
            blob = jar.read(bridge)
            values = set(classfile.utf8_values(blob))
            for member in ("contains", "getInt", "getString", "set", "CUSTOM_DATA"):
                if member not in values:
                    problems.append(f"{jar_path.name}: pont sans membre {member!r}")
            # Le pont resout tout par reflexion : ces noms n'existent pas en tant que CONSTANT_Class,
            # mais bien en chaines du constant-pool. On les verifie la, ET sur le fichier produit par
            # javac dans target/classes — la difference entre les deux designe le coupable :
            #   present dans target/classes + absent du jar  -> la copie de ressources a gagne (conflit
            #                                                    de doublons dans le paquet)
            #   absent des deux                              -> le pom ne compile pas notre source
            # Sous-chaine des constantes reunies, PAS equality sur un nom : le pont resout ces types par
            # `Class.forName("org.bukkit.persistence.PersistentDataType")` (donc en nom QUALIFIE, jamais
            # en simple nom de symbole) et `LegacyNbtBridge` n'apparait que dans une phrase de diagnostic.
            # Les chercher comme noms exacts condamnait un paquet valide (runs #33158841547 ->
            # #33159581656 : quatre runs rouges pour un controle faux, le plugin etait bon).
            pool = "\n".join(sorted(values))
            # Le pont resout TOUT par Class.forName : javac n'ecrit donc que le NOM QUALIFIE, jamais le
            # nom simple. Tester « NamespacedKey » comme nom de symbole etait systematiquement faux
            # (cinq runs rouges #33158576991 -> #33159866541, pour rien : le paquet est bon). Les
            # sous-chaines ci-dessous sont celles que javac produit reellement pour notre source :
            #   lookup("org.bukkit.NamespacedKey")
            #   lookup("org.bukkit.persistence.PersistentDataType")
            #   lookup("io.github.bananapuncher714.nbteditor.LegacyNbtBridge")
            CONTRACT = {
                "org.bukkit.NamespacedKey": "clef PDC resolvee par reflexion",
                "org.bukkit.persistence.PersistentDataType": "type de donnees PDC resolu par reflexion",
                "io.github.bananapuncher714.nbteditor.LegacyNbtBridge": "repli vers l'implementation historique",
                "getPersistentDataContainer": "acces au conteneur PDC",
                "valueOf": "desambiguisation des surcharges (le conteneur n'a pas de get a 1 argument)",
            }
            missing = [f"{name} ({why})" for name, why in CONTRACT.items() if name not in pool]
            if missing:
                problems.append(f"{jar_path.name}: pont sans references {missing}")
            # Epreuve de taille : si l'entree du paquet a exactement la taille de l'ANCIENNE
            # implementation (LegacyNbtBridge.class, livree), c'est qu'un fichier perime a gagne.
            stale = PACKAGE_DIR / "LegacyNbtBridge.class"
            if stale.is_file() and stale.stat().st_size == len(blob):
                problems.append(f"{jar_path.name}: NBTEditor.class a la TAILLE de l'ancienne "
                                f"implementation ({len(blob)} o) — paquet perime, pas notre pont")
            if missing:
                preview = ", ".join(sorted(v for v in values if "." in v and v[:1].islower())[:10])
                problems.append(f"constant-pool du paquet (echantillon de 10 noms qualifies) : {preview}")
            compiled = PACKAGE_DIR.parent.parent / "target/classes/io/github/bananapuncher714/nbteditor/NBTEditor.class"
            matches = sorted(ROOT.glob("target/classes/**/NBTEditor.class")) or [compiled]
            for target_file in matches[:1]:
                if target_file.is_file():
                    tv = "\n".join(sorted(set(classfile.utf8_values(target_file.read_bytes()))))
                    ok_missing = [n for n in CONTRACT if n not in tv]
                    problems.append(
                        f"cible: {target_file.relative_to(ROOT)} = {target_file.stat().st_size} o, "
                        f"taille du paquet = {len(blob)} o -> "
                        + ("MEME FICHIER (le paquet embarque bien la sortie du build)"
                           if target_file.stat().st_size == len(blob) else "FICHIERS DIFFERENTS")
                        + (f" ; references manquantes aussi cote cible : {ok_missing}" if ok_missing
                           else " ; contrat complet cote cible"))
            else:
                problems.append("cible: aucun target/classes/**/NBTEditor.class (le pom ne compile pas "
                                "la source du pont)")


def main() -> int:
    parser = argparse.ArgumentParser(description="Installe le pont NBT -> PersistentDataContainer.")
    parser.add_argument("--check", action="store_true", help="contrôle sans écrire")
    parser.add_argument("--jar", help="JAR à contrôler en complément")
    args = parser.parse_args()

    if not PACKAGE_DIR.is_dir():
        print(f"ERREUR: {PACKAGE_DIR} introuvable", file=sys.stderr)
        return 1

    problems = []
    renamed = 0
    for path in legacy_files():
        relative = path.relative_to(PACKAGE_DIR).as_posix()
        target = PACKAGE_DIR / relative.replace("NBTEditor", "LegacyNbtBridge", 1)
        if args.check:
            problems.append(f"{relative}: l'ancien nom est encore présent (pont non installé)")
            continue
        data = path.read_bytes()
        patched = rename_class(data)
        try:
            classfile.walk(patched)
        except classfile.ClassFormatError as error:
            problems.append(f"{relative}: fichier invalide après renommage ({error})")
            continue
        target.write_bytes(patched)
        path.unlink()
        renamed += 1

    if not args.check:
        for path in legacy_files():
            problems.append(f"{path.name}: renaissance inattendue après renommage")

    problems.extend(outside_references())

    if args.jar:
        jar_path = Path(args.jar)
        if not jar_path.is_absolute():
            jar_path = ROOT / args.jar
        if not jar_path.is_file():
            message = f"JAR introuvable: {args.jar}"
            print(f"ERREUR: {message}", file=sys.stderr)
            ci_publish.fail("Contrôle NBT : paquet absent", [message,
                              "le build n'a pas produit target/ValoriaTycoon-v1.6.3.jar"])
            return 1
        check_jar(jar_path, problems)
        # Diagnostic obligatoire : « pont absent » ne veut rien dire sans savoir ce que le paquet
        # contient. On liste le paquet controlé (extrait pertinent) dans l'annotation.
        if problems:
            with zipfile.ZipFile(jar_path) as jar:
                listing = [n for n in jar.namelist() if "/nbteditor/" in n]
            problems.append(f"contenu reel du paquet dans le jar ({len(listing)} entree(s)) : "
                            f"{sorted(x.rsplit('/', 1)[-1] for x in listing)[:8]}")

    if renamed:
        print(f"Classes legacy renommées : {renamed}")
    if problems:
        print(f"\n{len(problems)} problème(s) :", file=sys.stderr)
        for problem in problems[:20]:
            print(f"  - {problem}", file=sys.stderr)
        ci_publish.fail("Contrôle NBT : " + ("JAR " + args.jar if args.jar else "arbre du dépôt"),
                        problems[:40])
        return 1
    print("OK: pont NBT installé (LegacyNbtBridge en repli, NBTEditor fourni par la compilation).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
