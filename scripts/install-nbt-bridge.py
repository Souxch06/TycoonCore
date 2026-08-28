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
NBT_PACKAGE = "io/github/bananapuncher714/nbteditor/"
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
    """Controle le pont NBT tel que le BUILD le livre.

    Trois lecons de la serie de faux positifs (runs #33158576991 a #33160102783, six runs rouges pour
    un paquet qui etait bon depuis que Maven sort `exit 0`) :

    1. **le contrat se lit sur TOUT le paquet** : notre source declare des classes internes
       (`NBTEditor$Bukkit` porte le lookup des types PDC, `NBTEditor$Legacy` le repli, `NBTEditor$Keys`
       la lecture des cles) et javac repand ces chaines dans leurs .class respectifs ;
    2. **le pont resout ses types par `Class.forName`** : javac n'ecrit que le NOM QUALIFIE
       (`org.bukkit.NamespacedKey`, `org.bukkit.persistence.PersistentDataType`), jamais le nom simple —
       comparer des noms de symboles ne peut donc jamais marcher ;
    3. **les classes internes legitimes du pont ne sont pas des residus** : la liste des noms admis se
       lit dans la source (voir `pont_inner_classes`), pas dans une liste a la main.
    """
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        bridge = f"{NBT_PACKAGE}NBTEditor.class"
        legacy = f"{NBT_PACKAGE}LegacyNbtBridge.class"
        if bridge not in names:
            problems.append(f"{jar_path.name}: pont {bridge} absent — le build ne l'a pas compile ?")
        if legacy not in names:
            problems.append(f"{jar_path.name}: repli {legacy} absent")

        pont = sorted(n for n in names if n.startswith(f"{NBT_PACKAGE}NBTEditor") and n.endswith(".class"))
        allowed = pont_inner_classes()
        stale = [n for n in pont if inner_name(n) not in allowed]
        if stale:
            problems.append(f"{jar_path.name}: classes NBTEditor$… qui ne correspondent a aucune classe "
                            f"interne declaree par la source du pont : {sorted(map(inner_name, stale))[:4]}")

        if not pont:
            return
        values = set()
        for entry in pont:
            values.update(classfile.utf8_values(jar.read(entry)))
        pool = "\n".join(sorted(values))

        for member in ("contains", "getInt", "getString", "set", "CUSTOM_DATA", "Type"):
            if member not in pool:
                problems.append(f"{jar_path.name}: pont sans membre {member!r}")

        contract = {
            "org.bukkit.NamespacedKey": "clef PDC resolue par reflexion",
            "org.bukkit.persistence.PersistentDataType": "type de donnees PDC resolu par reflexion",
            "io.github.bananapuncher714.nbteditor.LegacyNbtBridge": "repli vers l'implementation historique",
            "getPersistentDataContainer": "acces au conteneur PDC",
            "valueOf": "desambiguisation des surcharges du conteneur",
            "java.lang.invoke.MethodHandles": "manipulation du conteneur sans dependance de compilation",
        }
        missing = [f"{name} ({why})" for name, why in contract.items() if name not in pool]
        if missing:
            qualified = sorted(v for v in values if "." in v and v[:1].islower())
            problems.append(f"{jar_path.name}: pont sans references {missing}")
            problems.append(f"noms qualifies vus dans {len(pont)} entree(s) "
                            f"({', '.join(inner_name(n) for n in pont[:6])}) : "
                            f"{', '.join(qualified[:12]) or 'AUCUN'}")
        if not missing:
            print(f"  pont verifie : {len(pont)} entree(s), {len(values)} constantes, contrat complet")


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
