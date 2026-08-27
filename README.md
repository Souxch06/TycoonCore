# GensPlus v1.6.3

Ce dépôt contient le plugin **GensPlus v1.6.3** réorganisé à partir du fichier `GensPlus-v1.6.3.jar`.

## Organisation rapide

```text
artifacts/
  original/              # JAR original conservé tel quel
  extracted/             # Extraction brute complète du JAR

sources/
  plugin/                # Sources Java décompilées du plugin GensPlus
  shaded/                # Sources Java décompilées des dépendances incluses dans le JAR
  module-info.java       # module-info décompilé

resources/               # Ressources principales du plugin, copiées pour accès rapide
  config.yml
  messages.yml
  plugin.yml
  data/

docs/
  STRUCTURE.md           # Détails de l'organisation du dépôt
  decompilation-summary.txt

scripts/
  verify-extraction.py   # Vérifie que artifacts/extracted correspond au JAR
```

## Important

Les fichiers dans `sources/` proviennent d'une **décompilation**. Ils servent surtout à lire, comprendre et retravailler le plugin. Ils peuvent nécessiter des corrections avant d'être recompilés.

Le contenu exact du `.jar` est conservé dans `artifacts/extracted/`.
