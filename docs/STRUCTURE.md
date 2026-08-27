# Structure du dépôt

## `artifacts/original/`

Contient le fichier JAR original :

- `ValoriaTycoon-v1.6.3.jar`

## `artifacts/extracted/`

Contient tous les fichiers extraits du JAR. La structure reste identique à celle visible dans une application comme ZArchiver. Les ressources principales (`config.yml`, `messages.yml`, `plugin.yml`) ont été francisées pour garder le dépôt cohérent :

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

Cette zone sert de référence de structure : elle permet de vérifier qu'aucun fichier du JAR n'a été perdu.

## `sources/plugin/`

Contient les sources Java décompilées du package principal :

- `xyz.arcadiadevs.valoriatycoon`

C'est le dossier le plus utile pour modifier la logique du plugin.

## `sources/shaded/`

Contient les dépendances intégrées dans le JAR puis décompilées :

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

Copie pratique des fichiers de configuration principaux du plugin :

- `config.yml`
- `messages.yml`
- `plugin.yml`
- `data/block_data.json`
- `data/player_data.json`
- `data/wands_data.json`

Les mêmes fichiers existent aussi dans `artifacts/extracted/`. Les ressources principales y sont également en français afin que le dépôt reste homogène.

## `docs/decompilation-summary.txt`

Résumé généré par CFR pendant la décompilation. Il liste les classes/méthodes que le décompilateur n'a pas pu restituer parfaitement.

## Vérification

Pour vérifier que tous les chemins de fichiers du JAR sont présents dans l'extraction :

```bash
python3 scripts/verify-extraction.py
```
