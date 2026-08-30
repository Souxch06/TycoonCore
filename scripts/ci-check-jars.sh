#!/usr/bin/env bash
# Verifie le CONTENU des trois jar produits, une assertion a la fois, et dit laquelle a rate.
#
# Pourquoi ce script existe (et pas le bloc `bash -c '...'` qu'il remplace) :
#   - l'etape «Verifier les trois JAR produits » du workflow etait ecrite en line-inline dans le YAML.
#     Quand elle a echoue pour la premiere fois de son existence (run 33200967570, code 2), le journal
#     ne disait PAS quelle assertion avait casse : `set -e` dans un ici-doc-shell, c'est un coupable
#     inconnu. Ici, chaque controle porte une etiquette et la publie en `::error::` — donc dans les
#     annotations, le seul canal de log lisible depuis un agent comme depuis l'aperçu du job.
#   - un YAML qui contient un shell de 40 lignes est aussi la copie la plus facile a faire diverger.
#
# Sortie : 0 si tout est conforme, 1 sinon (avec une ligne `::error::` par controle rate).
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

VERSION="${VERSION:-1.6.3}"
MAIN="target/ValoriaTycoon-v${VERSION}.jar"
ECONOMY="target/ValoriaEconomy-v${VERSION}.jar"
TOOLS="target/ValoriaTools-v${VERSION}.jar"
fail=0

# Le nom du descripteur compte autant que son contenu : c'est lui que le depot recherche, et un
# `<finalName>` oublie donne un `ValoriaTycoon-1.6.3-jar.jar` que le serveur ne voit jamais.
need_file() {
  local path="$1" why="$2"
  if [ ! -f "$path" ]; then
    echo "::error file=$path::fichier absent — $why"
    fail=$((fail + 1))
    return 1
  fi
  echo "OK  fichier  $path"
}

# Une entree attendue dans le jar. `unzip -l` liste les noms, on cherche par sous-chaine.
need_entry() {
  local jar="$1" entry="$2"
  if ! unzip -l "$jar" 2>/dev/null | grep -q "$entry"; then
    echo "::error file=$jar::entrée absente du jar — $entry (le pom ne l'a pas embarquée, ou l'assemblage la filtre)"
    fail=$((fail + 1))
    return 1
  fi
  echo "OK  entrée   $jar  $entry"
}

# Une entree qui ne DOIT pas etre la : un deuxieme plugin dans le meme paquet, ou une API telechargee.
forbid_entry() {
  local jar="$1" entry="$2" why="$3"
  if unzip -l "$jar" 2>/dev/null | grep -q "$entry"; then
    echo "::error file=$jar::entrée interdite — $entry ($why)"
    fail=$((fail + 1))
    return 1
  fi
  echo "OK  absence  $jar  $entry"
}

echo "=== les trois jar existent ==="
need_file "$MAIN" "le finalName de maven-jar-plugin doit produire ce nom"
need_file "$ECONOMY" "l'execution economy-plugin-jar de maven-assembly-plugin (finalName dans l'execution, appendAssemblyId=false)"
need_file "$TOOLS" "l'execution tools-plugin-jar de maven-assembly-plugin"
[ "$fail" -eq 0 ] || { echo "ARRET : un ou plusieurs jar manquent, les controles de contenu sont sans objet"; exit 1; }

echo "=== chacun porte son descripteur et sa config ==="
for jar in "$MAIN" "$ECONOMY" "$TOOLS"; do
  need_entry "$jar" "plugin.yml"
  need_entry "$jar" "config.yml"
done

echo "=== aucun jar n'embarque un autre plugin ==="
# Trois plugin.yml dans un paquet = Bukkit choisit l'un des trois, au hasard du chargement : le serveur
# tourne alors avec un plugin qui croit etre l'autre (et l'admin ne voit qu'un comportement bizarre).
forbid_entry "$MAIN" "xyz/arcadiadevs/valoriaeconomy/" "ValoriaEconomy est son propre jar"
forbid_entry "$MAIN" "xyz/arcadiadevs/valariatools/" "ValoriaTools est son propre jar"
forbid_entry "$ECONOMY" "xyz/arcadiadevs/valariatools/" "ValoriaTools est son propre jar"
forbid_entry "$TOOLS" "xyz/arcadiadevs/valoriatycoon/" "ValoriaTycoon est son propre jar"
forbid_entry "$TOOLS" "xyz/arcadiadevs/valoriaeconomy/" "ValoriaEconomy est son propre jar"

echo "=== l'API d'economie n'existe qu'une fois : dans le jar du FOURNISSEUR ==="
# Le type du service enregistre doit etre LIT DEPUIS UNE SEULE SOURCE. Elle vit dans le jar de
# ValoriaEconomy (load: STARTUP, chargé avant tout POSTWORLD) : au chargement de ce plugin, le
# classloader de ValoriaTycoon n'existe pas encore — lui emprunter l'interface rendait le plugin
# invalide au chargement (« Could not load plugin », Economy rouge, serveur du 2026-08-30).
# ValoriaTycoon (softdepend: ValoriaEconomy) résout l'interface par délégation vers CE jar :
# une seule classe à l'exécution, celle du service enregistré. Une copie côté Tycoon recréerait
# deux objets Class et getRegistration(Economy.class) renverrait null sans erreur visible.
need_entry "$ECONOMY" "xyz/arcadiadevs/valoriateconomy/Economy.class"
need_entry "$ECONOMY" "xyz/arcadiadevs/valoriateconomy/EconomyResponse.class"
forbid_entry "$MAIN" "xyz/arcadiadevs/valoriateconomy/Economy.class" "l'API vit dans le jar du fournisseur (ValoriaEconomy, chargé en premier) ; une copie ici casserait la résolution du service"
need_entry "$ECONOMY" "xyz/arcadiadevs/valoriaeconomy/ValoriaEconomyProvider.class"
need_entry "$ECONOMY" "xyz/arcadiadevs/valoriaeconomy/ValoriaEconomy.class"

echo "=== le plugin de tycoon porte ses correctifs ==="
need_entry "$MAIN" "xyz/arcadiadevs/valoriatycoon/hologram/HologramPool.class"
need_entry "$MAIN" "module-info.class"
forbid_entry "$MAIN" "net/milkbowl/" "Vault est retire, remplacé par l'interne"
forbid_entry "$MAIN" "org/holoeasy/" "HoloEasy est retire, remplace par nos hologrammes"
forbid_entry "$ECONOMY" "net/milkbowl/" "l'economie interne ne depend de rien"
forbid_entry "$TOOLS" "net/milkbowl/" "ValoriaTools atteint la banque par reflexion"

echo "=== le multi-outil est complet ==="
for entry in ValoriaTools ToolListener ToolsConfig ToolStore ToolStats Abilities EconomyService BlockMatcher MultiTool ToolsGui ToolsCommand ToolKind; do
  need_entry "$TOOLS" "xyz/arcadiadevs/valariatools/${entry}.class"
done

if [ "$fail" -ne 0 ]; then
  echo "CONTROLE DES JARS : echec ($fail)"
  exit 1
fi
echo "OK : contenu des trois jars conforme ($(unzip -l "$MAIN" | tail -1 | awk '{print $2}') + $(unzip -l "$ECONOMY" | tail -1 | awk '{print $2}') + $(unzip -l "$TOOLS" | tail -1 | awk '{print $2}') entrees)"
