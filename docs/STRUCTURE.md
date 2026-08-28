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

## Racine de compilation et `module-info.java`

Le `pom.xml` compile uniquement une liste explicite de fichiers maintenus, avec `sources/` comme
`sourceDirectory`. Conséquence à connaître : **aucun `module-info.java` ne doit rester à la racine de
`sources/`**. javac y chercherait un descripteur de module et basculerait toute la compilation en mode
module, ce qui échoue immédiatement (`module not found: com.google.gson`, le résidu décompilé étant
précisément celui du module gson embarqué). Ce fichier vit donc dans
`sources/shaded/com/google/gson/module-info.java`, en miroir de `META-INF/versions/9/module-info.class`
du JAR ; il n'est jamais compilé. `scripts/verify-paper26-compat.py` contrôle cette invariant.

## Économie interne (`ValoriaEconomy`)

- `sources/api/xyz/arcadiadevs/valoriateconomy/` : l'**interface** d'économie du serveur
  (`Economy`, `EconomyResponse`), écrite ici à la place de l'API Vault. Elle est compilée dans le jar
  de `ValoriaTycoon` (le code du plugin l'appelle) et **uniquement** dans celui-là : si les deux jars
  portaient leur copie, le `ServicesManager` comparerait deux objets `Class` différents et ne
  trouverait aucun fournisseur — silencieusement.
- `sources/economy/xyz/arcadiadevs/valoriaeconomy/` : `ValoriaEconomy` (JavaPlugin), `Balances`
  (`plugins/ValoriaEconomy/economy.yml`, écriture atomique), `MoneyCommand` (`/bal`, `/pay`, `/baltop`,
  `/eco`), `ValoriaEconomyProvider` (le fournisseur du service, **généré**).
- `resources-economy/` : `plugin.yml` (avec `load: STARTUP`) et `config.yml` du plugin d'économie ; ils
  ne vont PAS dans le paquet de `ValoriaTycoon`.
- `src/assembly/economy.xml` : assemble `target/ValoriaEconomy-v<version>.jar` à partir des classes
  compilées et de `resources-economy/` ; le `pom.xml` exclut `xyz/arcadiadevs/valoriaeconomy/**` du jar
  principal.
- `docs/economy-api.txt` : snapshot des 43 signatures de l'interface (surface historique de VaultAPI
  1.7, reprise à l'identique puis augmentée de `formatMoney`) ; `scripts/generate-economy-api.py` en
  émet `Economy.java` **et** `ValoriaEconomyProvider.java`, et `scripts/verify-economy-api.py` refuse
  tout écart entre snapshot, interface, fournisseur et descripteurs réellement appelés par les `.class`
  livrés (méthode oubliée, signature décalée, fichier non régénéré, `load: STARTUP` absent).
- `scripts/import-essentials-balances.py` : import ponctuel des soldes EssentialsX vers `economy.yml`
  (`--dry-run` d'abord ; ne touche jamais un compte déjà présent).

## `sources/plugin/`

Contient le code Java principal du plugin :

- `xyz.arcadiadevs.valoriatycoon`

C'est le dossier à utiliser en priorité pour lire ou modifier la logique de ValoriaTycoon.

## `sources/shaded/`

Contient les librairies embarquées avec le plugin :

- `com.awaitquality`
- `com.cryptomorin.xseries`
- `com.google.gson`
- `io.github.bananapuncher714.nbteditor` (pont compilé ici, l'implémentation d'origine est conservée
  sous `LegacyNbtBridge` comme repli)
- `kotlin`
- `marcono1234.gson.recordadapter`
- `org.json`

Ces fichiers sont séparés du code plugin pour éviter de mélanger le cœur de ValoriaTycoon avec les
librairies embarquées.

**`org.holoeasy` n'y est plus** : la bibliothèque d'hologrammes embarquée (qui exigeait ProtocolLib,
donc un plugin à installer) a été retirée du dépôt et du jar, remplacée par le paquet
`sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/` — voir `docs/HOLOGRAMMES.md`. Le renommage
des appels dans les classes précompilées est rejouable et contrôlé par
`scripts/selfmade-api-patch.py` ; `scripts/verify-paper26-compat.py` échoue si l'arbre revient.

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

## Pont NBT (PersistentDataContainer)

`sources/shaded/io/github/bananapuncher714/nbteditor/NBTEditor.java` remplace la bibliothèque
embarquée d'origine, dont les cibles NMS obfusquées n'existent plus sur les serveurs Paper récents
(plus de relocation de paquet depuis 1.20.6, plus de remapper interne ni de jar obfusqué depuis 26.1).
L'implémentation d'origine est conservée comme repli sous le nom binaire `LegacyNbtBridge` : le
renommage des 27 classes livrées est fait par `scripts/install-nbt-bridge.py`, qui ne réécrit que les
constantes `CONSTANT_Utf8` (noms internes et descripteurs) et revalide la structure de chaque fichier.

```bash
python3 scripts/install-nbt-bridge.py                    # installe le pont
python3 scripts/install-nbt-bridge.py --check             # CI, arbre du dépôt
python3 scripts/install-nbt-bridge.py --check --jar <j>   # CI, JAR produit
```

Le pont est recompilé par le build (voir `<includes>` du `pom.xml`) ; c'est lui qui fournit
`io/github/bananapuncher714/nbteditor/NBTEditor.class` et `NBTEditor$Type.class` dans le JAR, là où la
bibliothèque d'origine les livrait précompilées.

## Compilation ciblée : le classpath de référence

Pour corriger une classe sans recompiler tout l'arbre décompilé (non compilable), le build compile
une liste explicite de fichiers et résout le reste contre le binaire livré :

- `scripts/build-reference-jar.py` emballe `artifacts/extracted/` dans
  `artifacts/reference/valoria-renamed.jar` (~1655 entrées) ; le `pom.xml` l'ajoute au classpath en
  portée `system`. À relancer après toute modification de `artifacts/extracted/`
  (`python3 scripts/verify-paper26-compat.py` échoue si le JAR est obsolète). Le script **refuse**
  désormais d'embarquer `net/milkbowl/` ou `org/holoeasy/` : une copie de ces API dans le classpath
  masquerait nos propres sources, et javac ne signalerait plus un membre oublié ;
- **une seule** dépendance `provided` complète le classpath : `io.papermc.paper:paper-api` (pour
  `org.bukkit.*`). L'API d'économie n'est plus un artifact distant (elle vit dans `sources/api/`),
  donc rien n'est embarqué dans le JAR et le build n'a plus besoin de réseau pour un dépôt tiers ;
- fichiers concernés : `ServerVersion.java`, `NBTEditor.java` (pont NBT), `UpgradeGui.java`,
  `HologramsUtil.java` + le paquet `hologram/`, `ScoreboardService.java`, le marché `/ah`, l'API et le
  fournisseur d'économie.
  Cette liste est celle des `<includes>` du `pom.xml` ; `scripts/check-sources-java.mjs` en vérifie
  la surface publique.

## CI de validation

`scripts/ci/build-workflow.yml` contient le workflow de validation des Pull Requests (compilation,
contrôle des classes patchées et renommées, vérification du JAR produit, publication de l'artefact).
Il est hors de `.github/workflows/` parce que la création d'un workflow demande la permission
`workflows` côté GitHub : à activer avec

```bash
cp scripts/ci/build-workflow.yml .github/workflows/build.yml
```

La compilation ciblée est déclarée dans `pom.xml` (`sourceDirectory` + `includes` du maven-compiler-plugin) :
seule `sources/plugin/.../utils/ServerVersion.java` est recompilée, et sa sortie écrase la classe livrée dans
`target/classes` avant la mise en JAR.
