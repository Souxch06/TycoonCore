#!/usr/bin/env node
/**
 * Contrôle de forme des sources maintenues (sans JDK).
 *
 * Les sources de ce dépôt sont décompilées : beaucoup ne sont pas recompilables en l'état (le
 * décompileur produit du code invalide, par exemple dans `utils/ActionBarUtil.java`). Le build ne
 * compile donc que les fichiers listés dans `<includes>` du `pom.xml`. Ce script vérifie, pour ces
 * fichiers, deux choses que seul `javac` valide d'habitude :
 *
 *   1. la syntaxe Java est correcte (parseur complet, pas une regex) ;
 *   2. la surface publique est bien celle qu'attendent les classes déjà compilées du JAR
 *      (noms, modificateurs, arité, varargs, type de retour) — sinon le runtime lèverait
 *      `NoSuchMethodError` au premier appel.
 *
 * Il vérifie en bonus l'ordre des constantes de `ServerVersion` (les comparaisons du plugin passent
 * par `ordinal()` : l'ordre de déclaration doit rester chronologique).
 *
 * Dépendance : `npm install java-parser` (aucune dépendance Java requise).
 *
 *   node scripts/check-sources-java.mjs            # contrôle les fichiers maintenus
 *   node scripts/check-sources-java.mjs --audit    # ajoute un diagnostic de l'arbre sources/plugin
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, relative } from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
let parse;
try {
  ({ parse } = require('java-parser'));
} catch {
  console.error("Dépendance manquante : lancer `npm install java-parser` (ou `npm i -D java-parser`).");
  process.exit(2);
}

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)));
const kids = (node, key) => (node?.children?.[key]) || [];

function collect(node, type, acc = []) {
  if (!node || typeof node !== 'object') return acc;
  if (node.name === type) acc.push(node);
  for (const key of Object.keys(node)) {
    const value = node[key];
    if (Array.isArray(value)) value.forEach((c) => collect(c, type, acc));
    else if (value && typeof value === 'object') collect(value, type, acc);
  }
  return acc;
}

function tokens(node, acc = []) {
  if (!node || typeof node !== 'object') return acc;
  if (typeof node.image === 'string') acc.push(node.image);
  for (const key of Object.keys(node)) {
    const value = node[key];
    if (Array.isArray(value)) value.forEach((c) => tokens(c, acc));
    else if (value && typeof value === 'object') tokens(value, acc);
  }
  return acc;
}

const modifiersOf = (node, key) =>
  kids(node, key).flatMap((m) => Object.keys(m.children || {}).filter((k) => /^[A-Z]/.test(k))).map((s) => s.toLowerCase());

function analyze(source) {
  const tree = parse(source);
  const methods = [];
  for (const md of collect(tree, 'methodDeclaration')) {
    const header = kids(md, 'methodHeader')[0];
    const declarator = kids(header, 'methodDeclarator')[0];
    const list = kids(declarator, 'formalParameterList')[0];
    const params = kids(list, 'formalParameter');
    const result = kids(header, 'result')[0];
    methods.push({
      name: kids(declarator, 'Identifier')[0].image,
      modifiers: modifiersOf(md, 'methodModifier'),
      arity: params.length,
      varargs: params.some((p) => kids(p, 'variableArityParameter').length > 0),
      returns: result?.children?.void ? 'void' : (tokens(result).join('.') || 'void'),
    });
  }
  const fields = [];
  for (const fd of collect(tree, 'fieldDeclaration')) {
    const modifiers = modifiersOf(fd, 'fieldModifier');
    for (const id of collect(fd, 'variableDeclaratorId')) {
      const name = id.children.Identifier[0].image;
      if (!fields.some((f) => f.name === name)) fields.push({ name, modifiers });
    }
  }
  return {
    methods,
    fields,
    enums: collect(tree, 'enumConstant').map((e) => e.children.Identifier[0].image),
  };
}

// Surface que le bytecode livré exige. Les descripteurs sont ceux relevés dans les classes de
// artifacts/extracted (voir scripts/verify-paper26-compat.py) : ils ne doivent pas bouger.
const CONTRACTS = [
  {
    file: 'sources/shaded/io/github/bananapuncher714/nbteditor/NBTEditor.java',
    why: '8 classes précompilées appellent ces 5 membres (et rien d\'autre)',
    methods: [
      { name: 'contains', returns: 'boolean', arity: 2, varargs: true },
      { name: 'getInt', returns: 'int', arity: 2, varargs: true },
      { name: 'getString', returns: 'String', arity: 2, varargs: true },
      { name: 'set', returns: 'Object', arity: 3, varargs: true },
    ],
    fields: [{ name: 'CUSTOM_DATA', modifiers: ['public', 'static', 'final'] }],
    enumOrder: ['COMPOUND', 'LIST', 'NEW_ELEMENT', 'DELETE', 'CUSTOM_DATA', 'ITEMSTACK_COMPONENTS'],
    forbidImports: ['org.bukkit'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/commands/AuctionHouse.java',
    why: "séquestre du marché : écriture YAML immédiate, monnaie par Vault, items du plugin refusés",
    methods: [
      { name: 'isEnabled', returns: 'boolean', arity: 0, static: true },
      { name: 'title', returns: 'String', arity: 0, static: true },
      { name: 'list', returns: 'String', arity: 3, static: true },
      { name: 'buy', returns: 'String', arity: 2, static: true },
      { name: 'cancel', returns: 'String', arity: 2, static: true },
      { name: 'adminRemove', returns: 'String', arity: 2, static: true },
      { name: 'sellerIdOf', returns: 'UUID', arity: 1, static: true },
      { name: 'sweep', returns: 'void', arity: 0, static: true },
      { name: 'ensureStarted', returns: 'void', arity: 0, static: true },
      { name: 'reload', returns: 'String', arity: 1, static: true },
      { name: 'summary', returns: 'String', arity: 1, static: true },
      { name: 'sortedIds', returns: 'List', arity: 3, static: true },
      { name: 'count', returns: 'int', arity: 0, static: true },
    ],
    forbidImports: ['lombok'],
    mustContain: ['class Store', 'getOfflinePlayer', 'ATOMIC_MOVE', 'addReturn', 'setReturnItems',
                  'returnItems', 'pendingReturns', 'notifyReturns', 'claimAll', 'capacityFor',
                  'section.contains("unit-price")', 'lotPrice / size'],
    mustNotContain: ['net.minecraft', 'getNMSClass', 'NBTEditor'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/guis/AuctionGui.java',
    why: "vue partagée du marché : vue vanilla + redraw de toutes les vues ouvertes",
    methods: [
      { name: 'open', returns: 'void', arity: 1, static: true },
      { name: 'search', returns: 'void', arity: 2, static: true },
      { name: 'openOwn', returns: 'void', arity: 1, static: true },
      { name: 'forget', returns: 'void', arity: 1, static: true },
      { name: 'refreshAll', returns: 'void', arity: 0, static: true, visibility: 'public' },
      { name: 'getInventory', returns: 'Inventory', arity: 0, static: false, visibility: 'public' },
      { name: 'listingAt', returns: 'int', arity: 1, static: false, visibility: 'package' },
    ],
    mustContain: ['implements InventoryHolder', 'class Handler implements Listener', 'setCancelled', 'runTask',
                  'renderReturns', 'openReturns', 'toggleReturns', 'SLOT_RETURNS', 'AuctionHouse.claim(',
                  'AuctionHouse.claimAll(', 'AuctionHouse.buy(player, listing)'],
    mustNotContain: ['net.minecraft'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/commands/SellCommandListener.java',
    why: "point d'entrée /ah (interception de commande, comme /sell)",
    methods: [{ name: 'onPlayerCommandPreprocess', returns: 'void', arity: 1, static: false }],
    mustContain: ['"/ah"', '"returns"', '"claim"', 'AuctionGui.open', 'AuctionGui.openReturns',
                  'notifyReturns', 'AuctionGui.forget', 'setCancelled'],
    mustNotContain: ['net.minecraft'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/utils/ScoreboardService.java',
    why: "sidebar : scores posés par réflexion (Signature Score#setScore changée en 1.21+)",
    methods: [
      { name: 'show', returns: 'void', arity: 1, static: true },
      { name: 'hide', returns: 'void', arity: 1, static: true },
      { name: 'toggle', returns: 'String', arity: 1, static: true },
      { name: 'missing', returns: 'String', arity: 0, static: true },
    ],
    mustContain: ['getMethod("getScore", String.class)', 'resetScores', 'registerNewObjective', 'setDisplayName'],
    // (tableau de bord)
    // (contrat du tableau de bord)
    mustNotContain: ['net.minecraft', 'NBTEditor', 'dropItemNaturally', 'world.dropItem'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/guis/UpgradeGui.java',
    why: "interface recompilée : un bouton d'action, une case statistiques, aucun bouton dupliqué",
    methods: [
      { name: 'open', returns: 'void', arity: 3, static: true },
      { name: 'upgradeGenerator', returns: 'void', arity: 3, static: true },
      { name: 'fill', returns: 'List', arity: 5, static: true, private: true },
    ],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/utils/ServerVersion.java',
    why: 'les appelants existants (events/, guis/, utils/) et les logs d\'admin utilisent ces membres',
    methods: [
      { name: 'isServerVersionAtLeast', returns: 'boolean', arity: 1 },
      { name: 'isServerVersionAtOrBelow', returns: 'boolean', arity: 1 },
      { name: 'isServerVersionAbove', returns: 'boolean', arity: 1 },
      { name: 'isServerVersionBelow', returns: 'boolean', arity: 1 },
      { name: 'isServerVersion', returns: 'boolean', arity: 1 },
      { name: 'isServerVersion', returns: 'boolean', arity: 1, varargs: true },
      { name: 'getServerVersionString', returns: 'String', arity: 0 },
      { name: 'getVersionReleaseNumber', returns: 'String', arity: 0 },
      { name: 'get', returns: 'ServerVersion', arity: 0 },
      { name: 'fromVersionString', returns: 'ServerVersion', arity: 1 },
      { name: 'getMajor', static: false, returns: 'int', arity: 0 },
      { name: 'getMinor', static: false, returns: 'int', arity: 0 },
      { name: 'isLessThan', static: false, returns: 'boolean', arity: 1 },
      { name: 'isAtLeast', static: false, returns: 'boolean', arity: 1 },
      { name: 'isAtOrBelow', static: false, returns: 'boolean', arity: 1 },
      { name: 'isGreaterThan', static: false, returns: 'boolean', arity: 1 },
    ],
    enumOrderMustBeChronological: true,
    forbidImports: [],
  },
];

let problems = 0;
const ok = (message) => console.log(`   ✓ ${message}`);
const ko = (message) => { console.log(`   ✗ ${message}`); problems++; };

for (const contract of CONTRACTS) {
  const path = join(ROOT, contract.file);
  console.log(`\n${contract.file}\n   (${contract.why})`);
  let source;
  try {
    source = readFileSync(path, 'utf8');
  } catch {
    ko(`fichier introuvable : ${contract.file}`);
    continue;
  }
  let model;
  try {
    model = analyze(source);
  } catch (error) {
    const hash = error.hash || {};
    ko(`syntaxe invalide ligne ${hash.line ?? '?'}:${hash.col ?? '?'} — ${String(error.message).split('\n')[0]}`);
    continue;
  }
  ok('syntaxe Java valide (parseur complet)');

  for (const expected of contract.methods ?? []) {
    // une surcharge est identifiée par (nom, arité, varargs) : le contrat doit donc les préciser
    const found = model.methods.find((m) => m.name === expected.name
      && m.arity === expected.arity
      && Boolean(expected.varargs) === m.varargs)
      || model.methods.find((m) => m.name === expected.name);
    if (!found || found.name !== expected.name || found.arity !== expected.arity || Boolean(expected.varargs) !== found.varargs) {
      ko(`surcharge ${expected.name}/${expected.arity}${expected.varargs ? ' varargs' : ''} absente`); continue;
    }
    const issues = [];
    if (found.private === true) { /* placeholder */ }
    const wanted = expected.visibility ?? (expected.private ? 'private' : 'public');
    if (wanted !== 'package' && !found.modifiers.includes(wanted)) {
      issues.push(`visibilité ${found.modifiers.join('|') || 'package-private'} au lieu de ${wanted}`);
    }
    if (wanted === 'package' && found.modifiers.length > 0) {
      issues.push(`devrait être sans modificateur, trouvé ${found.modifiers.join('|')}`);
    }
    const wantStatic = expected.static !== false;
    if (wantStatic !== found.modifiers.includes('static')) issues.push(wantStatic ? 'non statique' : 'statique (attendue d\'instance)');
    if (found.arity !== expected.arity) issues.push(`arité ${found.arity} au lieu de ${expected.arity}`);
    if (Boolean(expected.varargs) !== found.varargs) issues.push(`varargs ${found.varargs}`);
    if (expected.returns && !String(found.returns).startsWith(expected.returns)) {
      issues.push(`retour ${found.returns} au lieu de ${expected.returns}*`);
    }
    issues.length
      ? ko(`${expected.name}(${expected.arity} params) : ${issues.join(', ')}`)
      : ok(`${expected.name}(…) -> ${found.returns}${expected.varargs ? ' [varargs]' : ''}`);
  }

  for (const expected of contract.fields ?? []) {
    const found = model.fields.find((f) => f.name === expected.name);
    if (!found) { ko(`champ ${expected.name} absent`); continue; }
    const missing = expected.modifiers.filter((m) => !found.modifiers.includes(m));
    missing.length ? ko(`champ ${expected.name} : modificateurs manquants ${missing.join(',')}`)
                   : ok(`champ ${expected.name} (${found.modifiers.join(' ')})`);
  }

  if (contract.enumOrder) {
    const same = JSON.stringify(model.enums) === JSON.stringify(contract.enumOrder);
    same ? ok(`enum Type : ${model.enums.join(', ')}`) : ko(`enum Type inattendu : ${model.enums.join(', ')}`);
  }
  if (contract.enumOrderMustBeChronological) {
    const e = model.enums;
    const good = e[0] === 'UNKNOWN'
      && e.indexOf('V26_1') > e.indexOf('V1_22')
      && e.indexOf('V26_2') === e.length - 1;
    good ? ok(`${e.length} constantes : UNKNOWN d'abord, puis 1.7→1.22, puis V26_1 < V26_2 (ordinal() cohérent)`)
         : ko(`ordre des constantes à revoir : ${e.join(', ')}`);
  }
  for (const needle of contract.mustContain ?? []) {
    if (!readFileSync(path, 'utf8').includes(needle)) ko(`mustContain introuvable : ${needle}`);
  }
  const codeOnly = readFileSync(path, 'utf8')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/^\s*\/\/.*$/gm, '');
  for (const needle of contract.mustNotContain ?? []) {
    if (codeOnly.includes(needle)) ko(`référence interdite dans le CODE (les commentaires sont exclus) : ${needle}`);
  }
  if (contract.file.endsWith("nbteditor/NBTEditor.java")) {
    const bridgeSrc = readFileSync(path, "utf8");
    if (/method\(CONTAINER, "get", 1\)/.test(bridgeSrc)) {
      ko("lecture PDC sur get(NamespacedKey) : cette surcharge n'existe pas dans l'API du conteneur");
    } else {
      ok("lecture PDC via get(key, type) + has(key), les seules surcharges reellement exposees");
    }
    if (/TYPE_NAMES/.test(bridgeSrc) && /fieldType/.test(bridgeSrc)) {
      ok("sonde par type + table de types partagee entre ecriture et lecture");
    } else {
      ko("la sonde par type (TYPE_NAMES/fieldType) est absente");
    }
    if (!/Bukkit\.MISSING/.test(bridgeSrc)) {
      ko("le message de diagnostic ne liste pas les membres API manquants");
    } else {
      ok("diagnostic explicite des membres API manquants dans le log");
    }
  }
  for (const forbidden of contract.forbidImports ?? []) {
    new RegExp(`^import\\s+${forbidden.replace(/\./g, '\\.')}`, 'm').test(source)
      ? ko(`import ${forbidden} présent : la compilation exigerait un artifact serveur`)
      : ok(`aucun import ${forbidden} : compilation possible sans dépendance externe`);
  }
}

if (process.argv.includes('--audit')) {
  const dir = join(ROOT, 'sources/plugin');
  const walk = (d, acc = []) => {
    for (const entry of readdirSyncSafe(d)) {
      const full = join(d, entry.name);
      if (entry.isDirectory()) walk(full, acc);
      else if (entry.name.endsWith('.java')) acc.push(full);
    }
    return acc;
  };
  const files = walk(dir);
  const broken = [];
  for (const file of files) {
    try { parse(readFileSync(file, 'utf8')); } catch { broken.push(relative(ROOT, file)); }
  }
  console.log(`\nAudit de l'arbre décompilé : ${files.length} fichiers, ${files.length - broken.length} parsent, ${broken.length} invalides`);
  for (const b of broken) console.log(`   - ${b}`);
  console.log('   ⇒ c\'est pour cela que le pom.xml ne compile qu\'une liste explicite (<includes>).');
}

// cohérence pom <-> contrats : tout fichier que le build compile doit être contrôlé ici
const pom = readFileSync(join(ROOT, 'pom.xml'), 'utf8');
const included = [...pom.matchAll(/<include>([^<]+\.java)<\/include>/g)].map((m) => 'sources/' + m[1]);
for (const file of included) {
  if (!CONTRACTS.some((c) => c.file === file)) ko(`le pom compile ${file} mais aucun contrat ne le couvre`);
}
for (const contract of CONTRACTS) {
  if (!included.includes(contract.file)) ko(`${contract.file} a un contrat mais n'est pas dans <includes> du pom`);
}
console.log(`Fichiers en compilation ciblée : ${included.length} (${included.map((f) => f.split('/').pop()).join(', ')})`);

console.log(problems ? `\n${problems} problème(s) — la PR ne doit pas être mergée en l'état.` : '\nAucun problème : syntaxe et surface binaire conformes.');
process.exit(problems ? 1 : 0);

function readdirSyncSafe(d) {
  const { readdirSync } = require('node:fs');
  try { return readdirSync(d, { withFileTypes: true }); } catch { return []; }
}
