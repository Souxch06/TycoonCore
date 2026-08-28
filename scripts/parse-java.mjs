#!/usr/bin/env node
/**
 * Contrôles Java sans JDK, avec le vrai parseur du langage (java-parser, grammaire Chevrotain).
 *
 *   node scripts/parse-java.mjs <dossier|fichier> …                  → syntaxe seule
 *   node scripts/parse-java.mjs --known <liste.txt> <cible> …       → syntaxe + types non résolus
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
if (args[0] === '--known') {
  knownFile = args[1];
  args.splice(0, 2);
}
if (args.length === 0) {
  console.error("usage: node scripts/parse-java.mjs [--known liste.txt] <dossier|fichier> …");
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

const JAVA_LANG = new Set(['String', 'Integer', 'Double', 'Long', 'Boolean', 'Object', 'Class',
  'System', 'Math', 'StringBuilder', 'StringBuffer', 'Runnable', 'Thread', 'Throwable', 'Exception',
  'RuntimeException', 'Error', 'Override', 'SuppressWarnings', 'Deprecated', 'Iterable', 'Number',
  'Comparable', 'CharSequence', 'Void', 'Enum', 'Record', 'FunctionalInterface', 'SafeVarargs',
  'IllegalStateException', 'IllegalArgumentException', 'NullPointerException',
  'UnsupportedOperationException', 'ArithmeticException', 'ClassCastException', 'LinkageError',
  'NoClassDefFoundError', 'NoSuchMethodError', 'NoSuchFieldError', 'Character', 'Byte', 'Short',
  'Float', 'Readable', 'Appendable', 'AutoCloseable', 'Cloneable', 'Package', 'Void', 'StackWalker',
  'Collection', 'List', 'Map', 'Set', 'Optional']);

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
  if (!knownFile) continue;

  const imports = byKey(tree, 'importDeclaration')
    .flatMap(imp => images(imp, 'Identifier').map(tok => tok.image));
  const declared = byKey(tree, 'typeIdentifier').flatMap(node => images(node, 'Identifier').map(tok => tok.image));
  const resolved = new Set([...known, ...imports, ...declared]);

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
    console.log(`${file}: ${line}: type \`${names.join('.')}\` dans une signature sans import (ni java.lang, ni déclaré dans le fichier, ni passé à --known)`);
  }
}

if (problems) {
  console.error(`${problems} problème(s) de forme ou d'import.`);
  process.exit(1);
}
console.error(`parse-java: ${files.length} fichier(s) valides${knownFile ? " (types des signatures contrôlés)" : ""}`);
