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
  // Les methodes d'INTERFACE sont des `constantDeclaration` (et non des `methodDeclaration`) :
  // elles sont collectees pareil, sinon un contrat ne peut pas controler une surface d'interface.
  const declarations = [
    ...collect(tree, 'methodDeclaration'),
    ...collect(tree, 'interfaceMethodDeclaration'),
    ...collect(tree, 'constantDeclaration'),
  ];
  for (const md of declarations) {
    const header = kids(md, 'methodHeader')[0] || md;
    const declarator = collect(header, 'methodDeclarator')[0];
    if (!declarator) continue;
    const list = kids(declarator, 'formalParameterList')[0];
    const params = kids(list, 'formalParameter');
    const result = kids(header, 'result')[0] || kids(md, 'type')[0];
    methods.push({
      name: kids(declarator, 'Identifier')[0].image,
      modifiers: [...modifiersOf(md, 'methodModifier'), ...modifiersOf(md, 'constantModifier')],
      arity: params.length,
      varargs: params.some((x) => kids(x, 'variableArityParameter').length > 0),
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
const E = 'sources/economy/xyz/arcadiadevs/valoriaeconomy/';

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
    file: E + 'ValoriaEconomyProvider.java',
    why: "fournisseur d'economie genere : toute la surface de l'interface, banques en NOT_IMPLEMENTED",
    mustContain: ['implements Economy', 'EconomyResponse.NOT_IMPLEMENTED', 'Solde insuffisant', 'Double.isFinite'],
    mustNotContain: ['net.minecraft', 'System.out', 'printStackTrace', 'milkbowl/', 'holoeasy/'],
    forbidImports: ['net.milkbowl'],
    generated: true,
  },
  {
    file: 'sources/api/xyz/arcadiadevs/valoriateconomy/Economy.java',
    why: "surface appelee par le bytecode livre du plugin : 44 signatures, aucune ne doit disparaitre",
    methods: [
      { name: 'isEnabled', returns: 'boolean', arity: 0, static: false },
      { name: 'formatMoney', returns: 'String', arity: 1, static: false },
      { name: 'format', returns: 'String', arity: 1, static: false },
      { name: 'getBalance', returns: 'double', arity: 1, static: false },
      { name: 'has', returns: 'boolean', arity: 2, static: false },
      { name: 'withdrawPlayer', returns: 'EconomyResponse', arity: 2, static: false },
      { name: 'depositPlayer', returns: 'EconomyResponse', arity: 2, static: false },
      { name: 'currencyNameSingular', returns: 'String', arity: 0, static: false },
      { name: 'createPlayerAccount', returns: 'boolean', arity: 1, static: false },
    ],
    interfaceMembers: true,
    mustContain: ['interface Economy', 'default String format(double amount)'],
    mustNotContain: ['net.minecraft', 'milkbowl/', 'holoeasy/'],
    forbidImports: ['net.milkbowl', 'org.holoeasy'],
    generated: true,
  },
  {
    file: 'sources/api/xyz/arcadiadevs/valoriateconomy/EconomyResponse.java',
    why: "UpgradeGui/GeneratorsGui (bytecode livre) lisent errorMessage et appellent transactionSuccess()",
    fields: [
      { name: 'amount', modifiers: ['public', 'final'] },
      { name: 'balance', modifiers: ['public', 'final'] },
      { name: 'type', modifiers: ['public', 'final'] },
      { name: 'errorMessage', modifiers: ['public', 'final'] },
    ],
    methods: [
      { name: 'transactionSuccess', returns: 'boolean', arity: 0, static: false },
      { name: 'hasUsableBalance', returns: 'boolean', arity: 0, static: false },
    ],
    enumOrder: ['SUCCESS', 'FAILURE', 'NOT_IMPLEMENTED', 'FAILURE_PARTIAL', 'UNSUPPORTED_OPERATION'],
    interfaceMembers: false,
    mustContain: ['static final EconomyResponse NOT_IMPLEMENTED', 'public EconomyResponse(double amount, double balance, ResponseType type, String errorMessage)'],
    mustNotContain: ['net.minecraft', 'milkbowl/'],
  },
  {
    file: E + 'Balances.java',
    why: "coffre des soldes \u00e9criture atomique, pas de solde n\u00e9gatif",
    mustContain: ['ATOMIC_MOVE', 'economy.yml.tmp', 'Math.round(value * 100.0D)'],
    mustNotContain: ['net.minecraft', 'System.out', 'printStackTrace', 'deleteBank('],
  },
  {
    file: E + 'MoneyCommand.java',
    why: "/bal /pay /baltop /eco \u2014 pay retire d'abord, d\u00e9pose ensuite, annule si \u00e9chec",
    mustContain: ['withdraw(', 'deposit(', 'Solde insuffisant', 'Double.isFinite'],
    mustNotContain: ['net.minecraft', 'System.out'],
  },
  {
    file: E + 'ValoriaEconomy.java',
    why: "enregistrement du service avant l'onEnable de ValoriaTycoon",
    mustContain: ['ServicesManager', 'Economy.class', 'saveDefaultConfig'],
    mustNotContain: ['net.minecraft', 'System.out'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/utils/HologramsUtil.java',
    why: "3 signatures imposees par 4 classes precompilees (ValoriaTycoon, LocationsData, GeneratorLocation)",
    methods: [
      { name: 'createHologram', returns: 'Hologram', arity: 3, static: true },
      { name: 'getHologram', returns: 'Hologram', arity: 1, static: true },
      { name: 'removeHologram', returns: 'void', arity: 1, static: true },
    ],
    mustContain: ['Config.HOLOGRAMS_ENABLED.getBoolean()', 'subtract(0.0D, 1.0D, 0.0D)', 'pool.registerHolograms(', 'UUID.fromString'],
    mustNotContain: ['net.minecraft', 'printStackTrace', 'milkbowl/'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/HologramPool.java',
    why: "le pool est stocke dans un champ de ValoriaTycoon (bytecode livre) : ces 3 methodes sont appelees en dur",
    methods: [
      { name: 'registerHolograms', returns: 'void', arity: 1, static: false },
      { name: 'get', returns: 'Hologram', arity: 1, static: false },
      { name: 'remove', returns: 'Hologram', arity: 1, static: false },
    ],
    mustContain: ['group.run()', 'spawnEntity(location, EntityType.ARMOR_STAND)', 'holograms.txt', 'adopt()', 'sweepOrphans()', 'setInvisible(true)', 'HoloEasy.optional(stand, "setPersistent", true)', 'HoloEasy.optional(entity, "setRemoveWhenFarAway", true)'],
    mustNotContain: ['net.minecraft', 'comphenix', 'HologramBuilder.hologram(', 'printStackTrace'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/Hologram.java',
    why: "Hologram.getId() est appele par LocationsData$GeneratorLocation pour persister hologramId",
    methods: [
      { name: 'getId', returns: 'UUID', arity: 0, static: false },
      { name: 'getLines', returns: 'List', arity: 0, static: false },
      { name: 'remove', returns: 'void', arity: 0, static: false },
    ],
    mustNotContain: ['net.minecraft', 'comphenix'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/HologramBuilder.java',
    why: "hologram/textline/item : les trois methodes statiques appelees par le bloc lambda du plugin",
    methods: [
      { name: 'hologram', returns: 'Hologram', arity: 2, static: true },
      { name: 'textline', returns: 'void', arity: 2, varargs: true, static: true },
      { name: 'item', returns: 'void', arity: 1, static: true },
    ],
    mustContain: ['ThreadLocal<Deque<Hologram>>', 'stack.pop()', 'usableItem'],
    mustNotContain: ['net.minecraft', 'comphenix'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/HologramSetupGroup.java',
    interfaceMembers: true,
    why: "interface fonctionnelle visee par le invokedynamic du bytecode livre",
    methods: [{ name: 'run', returns: 'void', arity: 0, static: false }],
    mustContain: ['@FunctionalInterface'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/HologramRegisterGroup.java',
    interfaceMembers: true,
    why: "idem, pour pool.registerHolograms(() -> ...)",
    methods: [{ name: 'run', returns: 'void', arity: 0, static: false }],
    mustContain: ['@FunctionalInterface'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/HologramStore.java',
    why: "un hologramme qui ne survit pas a un restart est une perte de donnees visible par les joueurs",
    mustContain: ['ATOMIC_MOVE', 'holograms.txt', 'HEADER', 'escape(', 'unescape('],
    mustNotContain: ['net.minecraft', 'printStackTrace', 'System.out'],
  },
  {
    file: 'sources/plugin/xyz/arcadiadevs/valoriatycoon/hologram/HoloEasy.java',
    why: "façade appelee par ValoriaTycoon.startInteractivePool : la signature ne doit pas bouger",
    methods: [
      { name: 'startInteractivePool', returns: 'HologramPool', arity: 4, static: true },
      { name: 'activePool', returns: 'HologramPool', arity: 0, static: true },
      { name: 'color', returns: 'String', arity: 1, static: true },
      { name: 'applySoul', returns: 'boolean', arity: 5, static: true },
      { name: 'refreshHeld', returns: 'void', arity: 3, static: true },
      { name: 'soulOf', returns: 'ToolKind', arity: 2, static: true },
      { name: 'held', returns: 'ItemStack', arity: 1, static: true },
    ],
    mustContain: ['new NamespacedKey("valoriatycoon", "hologram-entity")', 'legacyHex', 'instanceof ArmorStand',
                  'getMethod(setter, boolean.class)', 'setDisabledSlots', 'setItemInHand'],
    mustNotContain: ['net.minecraft', 'comphenix', 'printStackTrace', 'md_5'],
  },
  // ── ValoriaTools (multi-outil) : les fichiers de sources/tools/, tous écrits ici, donc sous contrat —
  // la règle du dépôt
  // est « tout fichier dans <includes> du pom a un contrat ». Sans contrat, une methode renommee dans
  // un fichier recompille ne serait vue que par javac (et par le serveur, en jeu).
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ValoriaTools.java',
    why: "plugin : enregistre UN SEUL exemplaire de chaque listener, relit l'economie apres l'activation",
    methods: [
      { name: 'reload', returns: 'void', arity: 0, static: false },
      { name: 'active', returns: 'boolean', arity: 0, static: false },
      { name: 'guiLive', returns: 'boolean', arity: 0, static: false },
      { name: 'sellPrice', returns: 'double', arity: 2, static: false },
    ],
    mustContain: ['this.listener = new ToolListener(this)', 'registerListener(this.listener)',
                  'this.guiRegistered = registerListener(new ToolsGui.Handler())',
                  'private boolean registerListener(Listener candidate)', 'runTaskLater(this, 20L)',
                  // la garde de l'item et l'entretien de la vitesse sont enregistres ICI, pas ailleurs :
                  // un listener oublie = regle promue et non appliquee, et rien ne le dit au demarrage
                  'this.guard = new ToolGuard(this)', 'public ToolGuard guard()',
                  'listener.refreshPassives()'],
    mustNotContain: ['net.minecraft', 'milkbowl/', 'holoeasy/', 'System.out', 'printStackTrace'],
    forbidImports: ['net.milkbowl', 'org.holoeasy'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolKind.java',
    why: "les quatre âmes + la reconnaissance des noms en français (un joueur dit « canne », pas « rod »)",
    methods: [{ name: 'parse', returns: 'ToolKind', arity: 1, static: true }],
    mustContain: ['PICKAXE', 'AXE', 'ROD', 'SWORD', 'toLowerCase(Locale.ROOT)'],
    mustNotContain: ['net.minecraft', 'milkbowl/'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolsConfig.java',
    why: "toute la puissance vient de la config : les capacités sont des clés + des valeurs par palier",
    methods: [
      { name: 'load', returns: 'void', arity: 0, static: false },
      { name: 'effect', returns: 'Effect', arity: 4, static: false },
      { name: 'levelOf', returns: 'int', arity: 3, static: true },
      { name: 'priceOf', returns: 'double', arity: 2, static: false },
      { name: 'priceOf', returns: 'double', arity: 2, static: false },
      { name: 'maxTier', returns: 'int', arity: 1, static: false },
      { name: 'sellPriceOf', returns: 'double', arity: 2, static: false },
    ],
    mustContain: ['class Ability', 'class KindConfig', 'class Effect', 'fromTier', 'treasure.items',
                  // les trois cles de la garde sont lues ici, une seule fois, avec un defaut sur
                  'tool.undroppable', 'tool.single-per-player', 'tool.auto-give',
                  'tool.morph-by-target', 'tool.lore-abilities',
                  'Math.max(0, this.maxTier - 1)', 'CHANCE_CEILING', 'PROC_BOOSTER', 'priceAt',
                  'ability-price.base'],
    mustNotContain: ['net.minecraft', 'milkbowl/', 'System.out'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolStore.java',
    why: "les paliers vivent par JOUEUR, pas dans l'item (autrement ils se dupliquent)",
    methods: [
      { name: 'load', returns: 'void', arity: 0, static: false },
      { name: 'save', returns: 'void', arity: 0, static: false },
      { name: 'tierOf', returns: 'int', arity: 3, static: false },
      { name: 'setTier', returns: 'void', arity: 4, static: false },
      { name: 'levelsOf', returns: 'Map', arity: 2, static: false },
      { name: 'setLevel', returns: 'void', arity: 5, static: false },
      { name: 'reset', returns: 'void', arity: 2, static: false },
    ],
    mustContain: ['ATOMIC_MOVE', '.tmp', 'this.dirty', 'Math.min(tier, Math.max(1, maxTier))',
                  'isConfigurationSection', 'getValues(false)', 'Math.max(1, legacy)'],
    mustNotContain: ['net.minecraft', 'System.out', 'printStackTrace'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolStats.java',
    why: "les compteurs sont ecrits au meme endroit que le paiement ; ecriture atomique ; aucun nul enregistre",
    methods: [
      { name: 'load', returns: 'void', arity: 0, static: false },
      { name: 'save', returns: 'void', arity: 0, static: false },
      { name: 'gesture', returns: 'void', arity: 4, static: false },
      { name: 'money', returns: 'void', arity: 3, static: false },
      { name: 'top', returns: 'List', arity: 3, static: false },
      { name: 'total', returns: 'long', arity: 2, static: false },
    ],
    mustContain: ['ATOMIC_MOVE', 'stats.yml', 'this.dirty', 'static Metric parse', 'Collections.sort'],
    mustNotContain: ['net.minecraft', 'System.out', 'printStackTrace'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/EconomyService.java',
    why: "la banque est atteinte par reflexion : aucun import d'API de banque, sinon le plugin meurt sans elle",
    methods: [
      { name: 'available', returns: 'boolean', arity: 0, static: false },
      { name: 'lookup', returns: 'void', arity: 0, static: false },
      { name: 'withdraw', returns: 'Outcome', arity: 2, static: false },
      { name: 'deposit', returns: 'Outcome', arity: 2, static: false },
      { name: 'format', returns: 'String', arity: 1, static: false },
    ],
    mustContain: ['getKnownServices()', 'getBalance', 'withdrawPlayer', 'transactionSuccess',
                  'ReflectiveOperationException'],
    mustNotContain: ['net.milkbowl', 'milkbowl/', 'getRegistrations(RegisteredServiceProvider.class)'],
    forbidImports: ['net.milkbowl'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/BlockMatcher.java',
    why: "le « au contact d'un bloc » : tags d'abord, noms ensuite, namespace enfin, avec cache",
    methods: [
      { name: 'kindOf', returns: 'ToolKind', arity: 1, static: false },
      { name: 'fallbackKind', returns: 'ToolKind', arity: 0, static: false },
      { name: 'configured', returns: 'boolean', arity: 0, static: false },
    ],
    mustContain: ['isTagged', 'NONE', 'getNamespace', 'NamespacedKey.fromString', 'configured()',
                  // le regard du joueur se lit ici (API renommée une fois), et nulle part ailleurs
                  'targetedKind(Player player)', 'getTargetBlockExact'],
    mustNotContain: ['net.minecraft', 'craftbukkit'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/Abilities.java',
    why: "filon et arbre en parcours iteratif a budget (une recursion sur un filon geometrique casse la pile)",
    methods: [
      { name: 'vein', returns: 'List', arity: 3, static: false },
      { name: 'tree', returns: 'List', arity: 2, static: false },
      { name: 'area', returns: 'List', arity: 5, static: false },
      { name: 'extra', returns: 'List', arity: 2, static: false },
      { name: 'enchant', returns: 'Enchantment', arity: 3, static: false },
      { name: 'haste', returns: 'void', arity: 2, static: false },
      { name: 'smelt', returns: 'List', arity: 1, static: false },
      { name: 'multiply', returns: 'List', arity: 4, static: false },
      { name: 'remove', returns: 'void', arity: 1, static: false },
    ],
    mustContain: ['this.budget', 'visited.add', 'getRecipesFor', 'FurnaceRecipe', "block.setType(Material.AIR, false)",
                  'MAX_BLOCKS_PER_GESTURE', 'harvestable', 'MAX_RADIUS',
                  // le passif de minage doit pouvoir etre RETIRE : on ne rend que l'amplificateur pose
                  // par nous, jamais celui d'un autre plugin
                  'static int grade', 'clearHaste'],
    mustNotContain: ['net.minecraft', 'getNMSClass', 'dropItemNaturally'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolListener.java',
    why: "le plugin calcule TOUJOURS les drops et annule l'evenement (double drop sinon) + garde de reentrance",
    methods: [
      { name: 'onBreak', returns: 'void', arity: 1, static: false },
      { name: 'onInteract', returns: 'void', arity: 1, static: false },
      { name: 'onFish', returns: 'void', arity: 1, static: false },
      { name: 'onDamage', returns: 'void', arity: 1, static: false },
      { name: 'refreshViews', returns: 'void', arity: 0, static: false },
      { name: 'onDeath', returns: 'void', arity: 1, static: false },
      { name: 'onMove', returns: 'void', arity: 1, static: false },
    ],
    mustContain: ['event.setCancelled(true)', 'this.handling = true', 'this.handling = false',
                  'giveOrDrop(player, world, drop)', 'applyGesture', 'selectTargets',
                  'Abilities.proc(', 'containsBlock(targets, block)',
                  // la vitesse de minage est un effet ENTRETENU (changement de main + tache periodique),
                  // plus une re-pose « au bloc d'apres » : c'est la correction du « rien ne s'active »
                  'PlayerItemHeldEvent', 'refreshPassive(Player player)', 'holdingPassiveHaste(player)',
                  // l'item matérialise l'âme qui sert (matériau + lore des capacités payées) : la
                  // partie visible du changement d'âme, sans quoi le joueur ne sait pas ce qui va payer
                  'showKind(player, kind)', 'targetedKind(player)', 'morphByTarget'],
    mustNotContain: ['net.minecraft', 'craftbukkit', 'getTargetBlock', 'printStackTrace'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/MultiTool.java',
    why: "l'item est marque en PDC (cle litterale, pas un nom de paquet) ; les paliers ne sont JAMAIS dans l'item",
    methods: [
      { name: 'create', returns: 'ItemStack', arity: 3, static: true },
      { name: 'isMultiTool', returns: 'boolean', arity: 1, static: true },
      { name: 'refresh', returns: 'void', arity: 4, static: true },
      { name: 'color', returns: 'String', arity: 1, static: true },
    ],
    mustContain: ['new NamespacedKey("valariatools", "multi")', 'PersistentDataType.STRING',
                  'translateAlternateColorCodes',
                  // l'âme affichée est une marque, pas un matériau deviné : deux âmes peuvent partager
                  // un matériau, et un serveur sans morphing doit quand même savoir quoi lister
                  'new NamespacedKey("valariatools", "soul")', 'applySoul(',
                  // `refresh` sur la copie rendue par le sac ne mettait à jour que la copie : la lore
                  // d'un joueur restait périmée après /tools max. C'est `refreshHeld` qui écrit.
                  'writeHeld(player, tool)', 'setItemInMainHand'],
    mustNotContain: ['net.minecraft', 'md_5', 'printStackTrace'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolsGui.java',
    why: "une case = une intention ; vue suivie par UUID, clic reporte d'un tick, items traces ; etat lisible sans pack de textures",
    methods: [
      { name: 'open', returns: 'void', arity: 1, static: true },
      { name: 'render', returns: 'void', arity: 1, static: true },
      { name: 'forget', returns: 'void', arity: 1, static: true },
      { name: 'views', returns: 'java.util.Collection', arity: 0, static: true },
    ],
    mustContain: ['implements InventoryHolder', 'class Handler implements Listener', 'setCancelled(true)',
                  'Bukkit.getScheduler().runTask(', 'view.track(', 'SLOT_SELL', 'SLOT_MODE', 'SLOT_HELP',
                  'SLOT_ABILITY_FIRST', 'buyLevel(player, shown', 'isShiftClick()',
                  // le panneau doit rester lisible en vanilla : pas de modele 3D, un materiau resolu avec
                  // repli, et un mode d'achat qui paie plusieurs niveaux a la fois
                  'Material.matchMaterial', 'nextMode()', 'MODES', 'priceAt'],
    mustNotContain: ['net.minecraft', 'printStackTrace'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolsCommand.java',
    why: "une seule commande, aliases compris : la permission est verifiee dans le code, pas que dans le YAML",
    methods: [
      { name: 'onCommand', returns: 'boolean', arity: 4, static: false },
      { name: 'onTabComplete', returns: 'List', arity: 4, static: false },
    ],
    mustContain: ['hasPermission("valoria.tools.use")', 'hasPermission("valoria.tools.admin")',
                  'getPlayerExact', "case \"give\"", "case \"sell\"",
                  // give et buy passent par la garde (un give qui double l'objet rend la regle fausse) et
                  // la vente du menu est deleguee, pour qu'il n'existe qu'une grille de prix
                  'guard().grant(target)', 'sellFromGui', 'matcher().diagnose()'],
    mustNotContain: ['net.minecraft', 'System.out'],
  },
  {
    file: 'sources/tools/xyz/arcadiadevs/valariatools/ToolGuard.java',
    why: "un seul exemplaire par joueur et il ne sort pas du sac : toute voie de sortie est fermee, y "
         + "compris celles qui ne déclenchent PAS PlayerDropItemEvent",
    methods: [
      { name: 'ensure', returns: 'boolean', arity: 1, static: false },
      { name: 'ensureSoon', returns: 'void', arity: 1, static: false },
      { name: 'grant', returns: 'ItemStack', arity: 1, static: false },
      { name: 'first', returns: 'ItemStack', arity: 1, static: false },
      { name: 'count', returns: 'int', arity: 1, static: false },
      { name: 'describe', returns: 'String', arity: 0, static: false },
      { name: 'onDrop', returns: 'void', arity: 1, static: false },
      { name: 'onClick', returns: 'void', arity: 1, static: false },
      { name: 'onDeath', returns: 'void', arity: 1, static: false },
    ],
    mustContain: [
      // les deux gestes qui RATENT PlayerDropItemEvent depuis un inventaire ouvert : sans eux, la garde
      // est décorative et l'admin croit le multi-outil protégé
      'ClickType.DROP', 'ClickType.CONTROL_DROP', 'movesOut(ClickType click)',
      'InventoryMoveItemEvent',            // les entonnoirs, dans les deux sens
      'getStorageContents()',              // 41 cases : armure et main secondaire comprises
      'event.getDrops().iterator()',       // la mort, sans toucher au keepInventory du serveur
      'instanceof ToolsGui.View',          // le menu annule déjà ses propres clics
    ],
    mustNotContain: ['net.minecraft', 'craftbukkit', 'System.out', 'printStackTrace'],
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
    // `implicit` : membre d'interface, donc public de droit sans que le mot-cle soit ecrit
    const implicitOk = wanted === 'public' && contract.interfaceMembers === true && found.modifiers.length === 0;
    if (wanted !== 'package' && !implicitOk && !found.modifiers.includes(wanted)) {
      issues.push(`visibilité ${found.modifiers.join('|') || 'package-private'} au lieu de ${wanted}`);
    }
    if (wanted === 'package' && found.modifiers.length > 0) {
      issues.push(`devrait être sans modificateur, trouvé ${found.modifiers.join('|')}`);
    }
    const wantStatic = expected.static !== false && !(contract.interfaceMembers === true && expected.static === undefined);
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
