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
