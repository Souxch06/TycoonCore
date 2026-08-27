# Structure du dépôt

## `artifacts/original/`

Contient le fichier JAR distribué :

- `ValoriaTycoon-v1.6.3.jar`

## `artifacts/extracted/`

Contient le contenu complet du plugin organisé avec la même structure que celle visible dans une archive JAR :

- `com/`
- `data/`
- `io/`
- `kotlin/`
- `marcono1234/`
- `META-INF/`
- `org/`
- `xyz/`
- `config.yml`
- `messages.yml`
- `module-info.class`
- `plugin.yml`

Cette zone sert de référence de structure et permet de vérifier qu'aucun fichier attendu n'est absent.

## `sources/plugin/`

Contient le code Java principal du plugin :

- `xyz.arcadiadevs.valoriatycoon`

C'est le dossier à utiliser en priorité pour lire ou modifier la logique de ValoriaTycoon.

## `sources/shaded/`

Contient les librairies embarquées avec le plugin :

- `com.awaitquality`
- `com.cryptomorin.xseries`
- `com.google.gson`
- `io.github.bananapuncher714.nbteditor`
- `kotlin`
- `marcono1234.gson.recordadapter`
- `org.holoeasy`
- `org.json`

Ces fichiers sont séparés du code plugin pour éviter de mélanger le cœur de ValoriaTycoon avec les librairies embarquées.

## `resources/`

Contient les fichiers de configuration principaux :

- `config.yml`
- `messages.yml`
- `plugin.yml`
- `data/block_data.json`
- `data/player_data.json`
- `data/wands_data.json`

## `docs/technical-report.txt`

Rapport technique lié aux sources et aux fichiers générés.

## Vérification

Pour vérifier que tous les chemins de fichiers attendus sont présents :

```bash
python3 scripts/verify-extraction.py
```

## Correctifs de classes vendorisées et compatibilité Paper 26.x

`scripts/patch-class-version-patterns.py` applique (ou vérifie avec `--check`) les correctifs de parsing
de version dans les classes compilées tierces présentes dans `artifacts/extracted/` : `XMaterial$Data` et
`XReflection`. Ces classes étant livrées précompilées, le correctif porte sur une constante du constant-pool
et non sur du bytecode ; le remplacement est donc reproductible et auditable.

```bash
python3 scripts/patch-class-version-patterns.py           # applique si nécessaire
python3 scripts/patch-class-version-patterns.py --check    # échoue si non appliqué
```

`scripts/verify-paper26-compat.py` contrôle l'état du dépôt, et, en lui passant un JAR, le contenu réel du
paquet (classe `ServerVersion` recompilée, classes patchées, absence de l'API Bukkit embarquée) :

```bash
python3 scripts/verify-paper26-compat.py
python3 scripts/verify-paper26-compat.py target/ValoriaTycoon-v1.6.3.jar
```

`scripts/classfile.py` est le lecteur de fichiers `.class` partagé par ces deux scripts.

La compilation ciblée est déclarée dans `pom.xml` (`sourceDirectory` + `includes` du maven-compiler-plugin) :
seule `sources/plugin/.../utils/ServerVersion.java` est recompilée, et sa sortie écrase la classe livrée dans
`target/classes` avant la mise en JAR.
