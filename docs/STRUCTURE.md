# Structure du dépôt

## `artifacts/original/`

Contient le fichier JAR original :

- `GensPlus-v1.6.3.jar`

## `artifacts/extracted/`

Contient l'extraction brute complète du JAR. Ce dossier garde la même structure que celle visible dans une application comme ZArchiver :

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

Cette zone sert de référence : elle permet de vérifier que rien n'a été perdu par rapport au JAR.

## `sources/plugin/`

Contient les sources Java décompilées du package principal :

- `xyz.arcadiadevs.gensplus`

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

Ces fichiers sont séparés du code plugin pour éviter de mélanger le cœur de GensPlus avec les librairies embarquées.

## `resources/`

Copie pratique des fichiers de configuration principaux du plugin :

- `config.yml`
- `messages.yml`
- `plugin.yml`
- `data/block_data.json`
- `data/player_data.json`
- `data/wands_data.json`

Les mêmes fichiers existent aussi dans `artifacts/extracted/`, qui reste l'extraction brute complète.

## `docs/decompilation-summary.txt`

Résumé généré par CFR pendant la décompilation. Il liste les classes/méthodes que le décompilateur n'a pas pu restituer parfaitement.

## Vérification

Pour vérifier que l'extraction brute correspond au JAR :

```bash
python3 scripts/verify-extraction.py
```
