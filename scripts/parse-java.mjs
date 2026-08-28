#!/usr/bin/env node
/**
 * Contrôles Java sans JDK, avec le vrai parseur du langage (java-parser, grammaire Chevrotain).
 *
 *   node scripts/parse-java.mjs <dossier|fichier> …
 *
 * Trois règles, toutes automatiques (aucun argument à ne pas oublier) :
 *   1. grammaire ;
 *   2. chaque type cité dans une signature ou une déclaration locale doit être importé, déclaré dans
 *      le fichier, ou exister dans le paquet ;
 *   3. `this.X` doit viser un champ de la classe courante (ou d'une englobante), et deux variables du
 *      même nom ne peuvent pas être déclarées dans le même bloc.
 *
 * Les deux modes correspondent aux DEUX familles de fautes qui ont fait échouer le build de ce dépôt
 * (et que l'oeil ne voit pas dans un fichier généré) :
 *   1. `Sad sad panda, parsing errors detected` — parenthèse ou accolade orpheline, échap cassé ;
 *   2. `cannot find symbol` — un type cité dans une signature sans import (`OfflinePlayer`, run
 *      #33144799989 : une seule cause, ~110 lignes d'erreur).
 * java-parser ne résout pas les symboles ; il dit QUELS identifiants sont des types. La liste de ce
 * qui est résoluble (java.lang, imports du fichier, noms passés par l'appelant) est fournie à côté,
 * et tout le reste est signalé avec numéro de ligne.
 *
 * Note d'implémentation (piège réel, coûté un faux négatif) : dans la CST de java-parser, un token
 * n'a PAS de propriété `name` — il est RANGE SOUS la clé qui le nomme (`children.Identifier = [{image}]`).
 * Chercher des nœuds `Identifier` ne remonte donc RIEN, et un contrôle qui ne voit rien passe
 * toujours : c'est plus dangereux qu'aucun contrôle.
 *
 * Sortie : `CHEMIN: ligne: colonne message` par problème, code 1 si problème.
 * Sans java-parser installé : avertissement et code 0 (le CI du dépôt l'installe avant d'appeler).
 */
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { createRequire } from 'node:module';

let parse;
const bases = [process.cwd(), dirname(dirname(new URL(import.meta.url).pathname))];
for (const base of bases) {
  try {
    parse = createRequire(join(base, 'noop.js'))('java-parser').parse;
    break;
  } catch { /* next candidate */ }
}
if (!parse) {
  console.error("java-parser introuvable — `npm install java-parser` (le CI de ce dépôt le fait avant).");
  process.exit(0);
}

const args = process.argv.slice(2);
let knownFile = null;
if (args[0] === '--known') {          // option conservee pour les tests manuels, plus exigee
  knownFile = args[1];
  args.splice(0, 2);
}
if (args[0] === '--from-pom') {
  // La liste des fichiers controles est LUE DU POM: la demander en dur dans le workflow (ou la laisser
  // dans un script) garantit qu'elle derive du pom et donc qu'elle ne peut pas rester en retard quand
  // un fichier est ajoute a <includes>.
  const pomText = readFileSync('pom.xml', 'utf8');
  const block = pomText.match(/<includes>([\s\S]*?)<\/includes>/);
  if (!block) {
    console.error('ERREUR: <includes> introuvable dans pom.xml');
    process.exit(2);
  }
  args.shift();
  for (const inc of block[1].matchAll(/<include>([^<]+\.java)<\/include>/g)) args.push(join('sources', inc[1]));
}
if (args.length === 0) {
  console.error('usage: node scripts/parse-java.mjs [--from-pom | <dossier|fichier> …]');
  process.exit(2);
}

const files = [];
const walk = (p) => {
  let st;
  try { st = statSync(p); } catch { return; }
  if (st.isDirectory()) readdirSync(p).forEach(name => walk(join(p, name)));
  else if (p.endsWith('.java')) files.push(p);
};
args.forEach(walk);

// Types de java.lang. Liste etendue a tout ce qu'un plugin Bukkit a une raison de citer sans import.
// Un oubli ici ne rend pas le code faux : le message plus bas explique comment etendre la liste, pour
// qu'un faux positif se repare en une ligne et non en ignorant le controle.
const JAVA_LANG = new Set(['Object', 'String', 'CharSequence', 'StringBuilder', 'StringBuffer', 'Math',
  'StrictMath', 'System', 'Runtime', 'Process', 'ProcessBuilder', 'Thread', 'ThreadGroup', 'Runnable',
  'Callable', 'Iterable', 'Cloneable', 'Comparable', 'Readable', 'Appendable', 'AutoCloseable',
  'Exception', 'RuntimeException', 'Error', 'Throwable', 'StackTraceElement', 'Class', 'ClassLoader',
  'ClassNotFoundException', 'ClassCastException', 'CloneNotSupportedException', 'Enum', 'Record',
  'Number', 'Integer', 'Long', 'Short', 'Byte', 'Float', 'Double', 'Boolean', 'Character', 'Void',
  'Package', 'Module', 'ModuleLayer', 'ServiceLoader', 'Layer', 'StackWalker', 'StackFrame',
  'InheritableThreadLocal', 'ThreadLocal', 'SecurityManager', 'IllegalArgumentException',
  'IllegalStateException', 'IllegalThreadStateException', 'IndexOutOfBoundsException',
  'ArrayIndexOutOfBoundsException', 'NullPointerException', 'ArithmeticException',
  'NumberFormatException', 'UnsupportedOperationException', 'InterruptedException', 'LinkageError',
  'NoClassDefFoundError', 'NoSuchMethodError', 'NoSuchFieldError', 'IncompatibleClassChangeError',
  'AbstractMethodError', 'BootstrapMethodError', 'ClassFormatError', 'ExceptionInInitializerError',
  'FunctionalInterface', 'Override', 'Deprecated', 'SuppressWarnings', 'SafeVarargs', 'Native']);

// Membres implicites d'un enum (herites de java.lang.Enum, jamais ecrits dans le fichier). Sans eux,
// `this.ordinal()` dans un enum est signale comme champ absent — faux positif constate des la premiere
// sortie de la regle 2 sur ServerVersion.
const IMPLICIT_ENUM_MEMBERS = new Set(['ordinal', 'name', 'values', 'valueOf', 'clone', 'equals',
  'hashCode', 'toString', 'compareTo', 'getDeclaringClass', 'finalize']);

/** Enfants nommés d'un nœud CST, sous forme de paires (clé, tableau). */
function entries(node) {
  const children = (node && node.children) || {};
  return Object.keys(children).map(key => [key, Array.isArray(children[key]) ? children[key] : [children[key]]]);
}

/** Tous les descendants dont la clé CST porte `name`. */
function byKey(node, name, out = []) {
  if (!node || typeof node !== 'object') return out;
  if (Array.isArray(node)) { node.forEach(child => byKey(child, name, out)); return out; }
  for (const [key, value] of entries(node)) {
    if (key === name) {
      for (const item of value) {
        if (item && typeof item === 'object' && (item.children || item.image !== undefined)) out.push(item);
        else if (item && typeof item === 'object') out.push(item);
      }
    }
    for (const value2 of value) byKey(value2, name, out);
  }
  return out;
}

/** Images des tokens rangés sous `key` dans un sous-arbre, dans l'ordre d'apparition. */
function images(node, key, source) {
  const out = [];
  const visit = (n) => {
    if (!n || typeof n !== 'object') return;
    if (Array.isArray(n)) { n.forEach(visit); return; }
    for (const [k, list] of entries(n)) {
      if (k === key) list.forEach(tok => { if (tok && typeof tok.image === "string") out.push(tok); });
      list.forEach(visit);
    }
  };
  visit(node);
  return out;
}

/** Identifiants d'un `unannType` : ce sont les types écrits dans le code. */
function typeTokens(typeNode, source) {
  // `ToolsConfig.KindConfig` est rendu en deux `Identifier` separes par un point : on garde la chaine
  // reconstituee pour le message, le premier segment pour la resolution, et la ligne du TOKEN (celle du
  // noeud parent pointerait plus haut et enverrait le correcteur sur une ligne sans rapport).
  const tokens = [...images(typeNode, 'Identifier')];
  const first = tokens[0];
  const line = first
    ? (typeof first.startLine === 'number' ? first.startLine
                                            : source.slice(0, first.startOffset).split('\n').length)
    : undefined;
  return { names: tokens.map(tok => tok.image), line };
}

function inClass(node, cls, inner) {
  const start = node?.location?.startOffset
  const end = node?.location?.endOffset
  if (start === undefined) return true
  if (start < cls.start || end > cls.end) return false
  return !inner.some(o => start >= o.start && end <= o.end)   // appartient a une classe imbriquee
}

const known = new Set(JAVA_LANG);
if (knownFile && existsSync(knownFile)) {
  for (const line of readFileSync(knownFile, 'utf8').split('\n')) {
    const name = line.trim();
    if (name && !name.startsWith('#')) known.add(name);
  }
}

let problems = 0;
for (const file of files.sort()) {
  const source = readFileSync(file, 'utf8');
  let tree;
  try {
    tree = parse(source);
  } catch (error) {
    problems++;
    const hash = error.hash || {};
    console.log(`${file}: ${hash.line ?? error?.hash?.line ?? '?'}: ${String(error.message).split('\n')[0]}`);
    continue;
  }
  // Les commentaires et chaines sont MASQUES (remplaces par des espaces), jamais retirés : retirer
  // decale les offsets, et les comparaisons aux ranges de la CST deviennent faux — ce bug a produit 20
  // faux positifs avant d'etre trouve. La longueur du texte doit donc rester identique.
  // Regles 1 a 3 : la liste de ce qui est resoluble est construite depuis le paquet du fichier lu
  // (noms de classes, internes comprises) plus ses imports — pas depuis un manifeste externe : un
  // controle qu'il faut rappeler avec le bon argument ne sera pas rappele.
  const siblings = readdirSync(dirname(file)).filter(name => name.endsWith('.java'));
  const packageKnown = new Set();
  for (const name of siblings) {
    const text = readFileSync(join(dirname(file), name), 'utf8');
    for (const match of text.matchAll(/\b(?:class|interface|enum|record)\s+([A-Z]\w*)/g)) packageKnown.add(match[1]);
  }
  for (const extra of known) packageKnown.add(extra);

  const codeOnly = source
    .replace(/\/\*[\s\S]*?\*\//g, m => ' '.repeat(m.length))
    .replace(/\/\/[^\n]*/g, m => ' '.repeat(m.length))
    .replace(/"(?:[^"\\\n]|\\.)*"/g, m => '"' + ' '.repeat(m.length - 2) + '"')
    .replace(/'(?:[^'\\\n]|\\.)*'/g, m => "'" + ' '.repeat(m.length - 2) + "'");
  const imports = byKey(tree, 'importDeclaration')
    .flatMap(imp => images(imp, 'Identifier').map(tok => tok.image));
  const declared = byKey(tree, 'typeIdentifier').flatMap(node => images(node, 'Identifier').map(tok => tok.image));
  const resolved = new Set([...packageKnown, ...imports, ...declared]);

  // Signatures ET declarations locales : une methode supprimee en nettoyant le code mort a laisse ici
  // un `Player viewer = …` sans import — le meme `cannot find symbol`, dans un corps. Les declarations
  // locales coutent trois lignes a couvrir et ferment la derniere porte ouverte.
  const signatures = [
    ...byKey(tree, 'formalParameter').flatMap(fp => byKey(fp, 'unannType')),
    ...byKey(tree, 'result').flatMap(res => byKey(res, 'unannType')),
    ...byKey(tree, 'localVariableDeclaration').flatMap(decl => byKey(decl, 'unannType')),
    ...byKey(tree, 'fieldDeclaration').flatMap(field => byKey(field, 'unannType')),
  ];
  const seen = new Set();
  for (const typeNode of signatures) {
    const { names, line } = typeTokens(typeNode, source);
    if (!names.length) continue;
    const head = names[0];
    if (resolved.has(head) || /^[a-z]/.test(head) || seen.has(head)) continue;
    seen.add(head);
    problems++;
    console.log(`${file}: ${line}: type \`${names.join('.')}\` dans une signature sans import (ni un type de java.lang, ni declare dans le fichier, ni une classe du paquet)`);
  }
  // ── regle 2 : `this.X` doit exister dans la classe courante (champ ou methode selon l'usage) ────────────────────────────
  // Un parseur de grammaire laisse passer un champ inexistant (`this.material` est une expression
  // valide) ; seul javac grogne, trois minutes plus tard. Faute vécue : des accesseurs de la classe
  // imbriquée `KindConfig` posés sur la classe externe `ToolsConfig` — `this.material` n'y existait
  // plus. Les classes et leurs champs viennent de la CST (fiables), l'occurrence `this.X` du texte
  // déjà débarrassé de ses commentaires et chaînes (inoffensif ici).
  const classNodes = [...byKey(tree, 'classDeclaration'), ...byKey(tree, 'interfaceDeclaration'),
                      ...byKey(tree, 'enumDeclaration')].map(node => ({
    node,
    start: node.location?.startOffset ?? 0,
    end: node.location?.endOffset ?? Number.MAX_SAFE_INTEGER,
  }));
  for (const cls of classNodes) {
    const nested = classNodes.filter(o => o !== cls && o.start >= cls.start && o.end <= cls.end);
    const inOwn = (node) => !nested.some(o => (node.location?.startOffset ?? 0) >= o.start
                                           && (node.location?.endOffset ?? 0) <= o.end);
    // champs ET methodes : `this.save()` est un appel, pas un champ — la premiere version de cette
    // regle ne collectait que les champs et signalait ~40 méthodes légitimes, soit exactement le bruit
    // qui fait ignorer un controle.
    // Deux collections SEPAREES, et c'est le ceur du controle : `this.material` est une lecture de
    // CHAMP, `this.save()` un appel de METHODE. Les melanger ouvrait une faille — un acces a un champ
    // inexistant etait accepte des qu'une methode portait le meme nom (constate sur un fixture).
    cls.fieldNames = new Set(byKey(cls.node, 'fieldDeclaration').filter(inOwn)
      .flatMap(f => images(f, 'Identifier').map(t => t.image)));
    cls.methodNames = new Set([
      ...byKey(cls.node, 'methodDeclaration').filter(inOwn)
        .flatMap(m => byKey(m, 'methodDeclarator').flatMap(md => images(md, 'Identifier').map(t => t.image))),
      ...byKey(cls.node, 'constructorDeclaration').filter(inOwn)
        .flatMap(c => byKey(c, 'constructorDeclarator').flatMap(cd => images(cd, 'Identifier').map(t => t.image))),
    ]);
  }
  const thisUses = [];
  const thisRe = /\bthis\s*\.\s*([A-Za-z_]\w*)\s*(\()?/g;
  let tm;
  while ((tm = thisRe.exec(codeOnly)) !== null) thisUses.push({ name: tm[1], call: tm[2] === '(', at: tm.index });
  const isEnumClass = (cls) => /\benum\s+\w+/.test(source.slice(cls.start, Math.min(cls.end, cls.start + 600)));
  for (const use of thisUses) {
    const enclosing = classNodes
      .filter(c => use.at >= c.start && use.at <= c.end)
      .sort((a, b) => (a.end - a.start) - (b.end - b.start));
    const innermost = enclosing[0];
    if (!innermost) continue;
    // Une classe interne non statique voit les champs de ses englobantes ; une classe STATIQUE imbriquee
    // ne les voit pas, mais elle declare les siens — et une erreur la-dessus couterait un faux positif
    // sur du code correct. On est donc permissif cote portee, et strict cote existence du nom.
    const holders = use.call ? 'methodNames' : 'fieldNames';
    const known2 = new Set();
    for (const cls of enclosing) (cls[holders] || new Set()).forEach(name => known2.add(name));
    if (use.call && isEnumClass(innermost)) for (const name of IMPLICIT_ENUM_MEMBERS) known2.add(name);
    if (known2.has(use.name)) continue;
    problems++;
    const line = source.slice(0, use.at).split('\n').length;
    console.log(`${file}: ${line}: \`this.${use.name}\` ne correspond a aucun champ ni methode de cette`
      + " classe (champ oublie, ou methode placee dans la mauvaise classe). Si c'est un membre herite"
      + " d'un parent, ajoute-le a IMPLICIT_ENUM_MEMBERS/JAVA_LANG en tete de ce script.");
  }

  // ── regle 3 : deux declarations du meme nom dans le MEME bloc ─────────────────────────────────
  // Portee la plus etroite possible : `List<String> lines` declare trois fois dans trois blocs freres
  // de ToolsGui.render est LEGAL, et le signaler tuerait la regle. Seul le doublon dans le bloc
  // identique est une erreur (`variable prices is already defined`, vecu dans ToolsConfig.readKind).
  // Portee = noeud englobant le plus etroit parmi `block` ET les enonces qui introduisent leur propre
  // portee (for, foreach, switch, try, methodes). Sans eux, deux boucles for d'une meme methade
  // (`for (ItemStack drop : drops)` puis `for (ItemStack drop : kept)`) tombent dans le meme bloc et
  // la regle hurle sur un code parfaitement legal — le genre de faux positif qui fait ignorer un
  // controle, ce qui est pire que de ne pas l'ecrire.
  const SCOPES = ['block', 'forStatement', 'forControl', 'switchStatement', 'switchBlock', 'tryStatement',
                  'catchClause', 'methodBody', 'constructorBody', 'lambdaBody'];
  const scopes = [];
  for (const kind of SCOPES) for (const node of byKey(tree, kind)) {
    if (node.location) scopes.push({ kind, start: node.location.startOffset, end: node.location.endOffset, names: [] });
  }
  for (const decl of byKey(tree, 'localVariableDeclaration')) {
    const at = decl.location?.startOffset;
    const name = (byKey(decl, 'variableDeclaratorId').flatMap(v => images(v, 'Identifier'))[0] || {}).image;
    if (at === undefined || !name) continue;
    const holder = scopes.filter(b => at >= b.start && at <= b.end)
      .sort((a, b) => (a.end - a.start) - (b.end - b.start))[0];
    if (!holder) continue;
    if (holder.names.includes(name)) {
      problems++;
      const line = source.slice(0, at).split('\n').length;
      console.log(`${file}: ${line}: variable \`${name}\` déclarée deux fois dans le même bloc`);
    } else {
      holder.names.push(name);
    }
  }
}

if (problems) {
  console.error(`${problems} problème(s) de forme ou d'import.`);
  process.exit(1);
}
console.error(`parse-java: ${files.length} fichier(s) valides${knownFile ? " (types des signatures contrôlés)" : ""}`);
