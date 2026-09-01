#!/usr/bin/env python3
"""Génère `resources-tools/config.yml` à partir du barème du wiki GenTycoon.

Le wiki (https://wiki.gentycoon.fr/progression-metiers-and-outils/les-outils) publie pour chaque outil
un tableau « Enchantement / Description / Prestige mini / Level mini / Niveau max ». Ce script en est la
seule copie exploitable par le build : les noms, les descriptions, les verrous et les niveaux maximaux
sont recopiés de là, et les valeurs d'effet viennent du tableau des noyaux plus bas.

Le fichier généré reste un fichier d'admin : après génération, c'est lui qui est édité (et relu par
`scripts/verify-tools-config.py`). Régénérer écrase donc les ajustements — c'est voulu, le barème est
la source, pas le dépôt.
"""
from __future__ import annotations

from pathlib import Path

OUT = Path("resources-tools/config.yml")
VERSION = "1.6.3"

# --------------------------------------------------------------------------- valeurs par noyau
# (valeur au niveau 1, pas par niveau, plafond) — le wiki ne publie aucun chiffre d'effet, donc ces
# trois nombres sont le réglage Valoria. Un noyau sans `chance` est passif : il agit dès qu'il est achetable.
KERNELS = {
    "HASTE": [("amplifier", 1, 0.15, 5), ("duration", 60, 0, 0)],
    "SWIFT": [("amplifier", 1, 0.2, 4), ("duration", 60, 0, 0)],
    "AREA_BREAK": [("chance", 0.02, 0.0006, 0.5), ("radius", 1, 0.02, 5),
                   ("max-blocks", 27, 4, 160), ("particles", 1, 0, 0)],
    "VEIN": [("chance", 0.1, 0.001, 0.6), ("max-blocks", 16, 1.5, 200)],
    "EXTRA_BLOCK": [("chance", 0.05, 0.002, 0.5), ("count", 1, 0.02, 6)],
    "GHOST_MINES": [("chance", 0.02, 0.0004, 0.4), ("waves", 1, 0.01, 8),
                    ("radius", 2, 0.01, 4), ("max-blocks", 12, 1, 96)],
    "TREE_FELL": [("max-blocks", 64, 2, 200), ("max-height", 8, 0.1, 24)],
    "CROP_HARVEST": [("radius", 1, 0.02, 3), ("max-blocks", 9, 4, 81)],
    "FORTUNE": [("chance", 0.02, 0.015, 0.9), ("extra-min", 1, 0, 0), ("extra-max", 2, 0, 0)],
    "DOUBLE_DROP": [("chance", 0.02, 0.02, 0.6)],
    "AUTO_SMELT": [],
    "SELL_ON_BREAK": [],
    "INFINITE_DURABILITY": [],
    "MONEY_MULT": [("percent", 1, 0.5, 500)],
    "MONEY_DOUBLE": [("chance", 0.01, 0.0005, 0.5), ("multiplier", 2, 0, 0)],
    "MONEY_POUCH": [("chance", 0.01, 0.0004, 0.5), ("amount", 25, 1.5, 25000),
                    ("per-block", 1, 0.01, 8)],
    "XP_MULT": [("percent", 1, 0.4, 400)],
    "XP_FLAT": [("chance", 0.02, 0.0008, 0.6), ("amount", 2, 0.05, 120)],
    "TREASURE": [("chance", 0.005, 0.0003, 0.35), ("amount", 1, 0, 0)],
    "PROC_BOOSTER": [("percent", 1, 0.2, 300)],
    "RANDOM_ENCHANT": [("chance", 0.002, 0.0001, 0.1), ("level", 1, 0.01, 5)],
    "FURY": [("chance", 0.005, 0.0004, 0.3), ("duration", 200, 1, 1200),
             ("multiplier", 1.2, 0.01, 4)],
    "SOUL_SPEED": [("boost", 40, 10, 120)],
    "CRIT": [("chance", 0.15, 0.01, 0.6), ("multiplier", 1.5, 0.05, 3)],
    "DAMAGE_MULT": [("percent", 2, 2, 20)],
    "LIFE_STEAL": [("chance", 0.2, 0.01, 0.8), ("heal-hearts", 1, 0.05, 6)],
    "KNOCKBACK": [("strength", 0.6, 0.02, 2)],
    "POTION_APPLY": [("chance", 0.05, 0.01, 0.5), ("amplifier", 0, 0.5, 2), ("duration", 200, 0, 0)],
    "AUTO_SWING": [("chance", 0.01, 0.0006, 0.5), ("multiplier", 0.5, 0.001, 1),
                   ("interval", 4, 0, 0)],
    "MULTI_KILL": [("chance", 0.01, 0.002, 0.3), ("radius", 2, 0.05, 5),
                   ("multiplier", 0.5, 0.02, 1.5)],
    "AUTO_REEL": [],
    "FAST_REEL": [("bite-chance", 0.1, 0.005, 0.9), ("ticks", 60, -1, 0)],
    "MULTI_CATCH": [("chance", 0.02, 0.0008, 0.6), ("count", 1, 0.01, 8)],
    "LUCK": [("treasure-chance", 0.02, 0.001, 0.5)],
}

# capacites particulieres : drapeaux fixes du noyau
FLAGS = {
    "AREA_BREAK_PIOCHE": [("ores-only", "true")],
    "AREA_BREAK_JOIE": [("flat", "true"), ("ores-only", "false")],
    "VEIN": [("similar-blocks-only", "true")],
    "CROP_HARVEST": [("flat", "true")],
    "GHOST_MINES": [("ores-only", "true")],
    "POTION_APPLY": [("effects", "[strength]")],
    "RANDOM_ENCHANT": [("enchants", "[efficiency, fortune, unbreaking, mending]")],
    "FAST_REEL": [],
}

# --------------------------------------------------------------------------- barème du wiki
# (id, noyau, nom du wiki, description du wiki, palier requis, niveau max)
# Le palier repris est `max(Level minimum, Prestige mini)` du wiki : Valoria n'a pas de prestige, le
# palier d'âme joue les deux rôles (voir docs/WIKI-GENTYCOON-OUTILS.md).
PICKAXE = [
    ("efficacite", "HASTE", "Efficacité", "Augmente l'efficacité de la pioche permettant ainsi de casser les minerais plus rapidement", 1, 10),
    ("speed", "HASTE", "Speed", "Donne un boost de vitesse en cassant des minerais", 1, 5),
    ("celerite", "HASTE", "Célérité", "Augmente la vitesse de minage de la pioche en cassant des minerais", 1, 5),
    ("charognard", "RANDOM_ENCHANT", "Charognard", "Donne une chance de trouver des enchantements aléatoires pour votre pioche", 5, 500),
    ("fortune", "FORTUNE", "Fortune", "Augmente la quantité de minerais récupérée en minant", 10, 10),
    ("auto-smelt", "AUTO_SMELT", "Auto-smelt", "Fonte automatique des minerais (fonctionnalité annoncée en tête de page Pioche)", 15, 1),
    ("seconde-main", "EXTRA_BLOCK", "Seconde main", "Donne une chance de casser un minerai supplémentaire", 20, 100),
    ("money-pouch", "MONEY_POUCH", "Money Pouch", "Donne une chance de trouver une grande quantité d'argent en cassant des minerais", 20, 2000),
    ("minecoins-pouch", "MONEY_POUCH", "MineCoins Pouch", "Donne une chance de trouver une grande quantité de MineCoins en cassant des minerais", 20, 2000),
    ("trouvaille", "TREASURE", "Trouvaille", "Donne une chance de trouver une clé boost en cassant des minerais", 15, 1000),
    ("explosive", "AREA_BREAK", "Explosive", "Donne une chance de créer une explosion cassant les minerais dans une zone de 3x3", 25, 10),
    ("double-gain", "MONEY_DOUBLE", "Double gain", "Donne une chance de doubler l'argent gagné grâce à la vente automatique", 25, 1000),
    ("briseur", "VEIN", "Briseur", "Donne une chance de casser tous le filon de minerais", 30, 300),
    ("onde-sismique", "AREA_BREAK", "Onde sismique", "Donne une chance de provoquer un tremblement de terre qui casse tous les minerais à proximité", 40, 300),
    ("pioche-fantomatique", "GHOST_MINES", "Pioche fantomatique", "Donne une chance de faire apparaître des pioches fantômes qui cassent les minerais automatiquement", 35, 300),
    ("surcharge", "AREA_BREAK", "Surcharge", "Donne une chance d'envoyer une puissante onde de choc cassant tous les minerais sur ton passage", 45, 300),
    ("chercheur-credits", "MONEY_POUCH", "Chercheur de crédits", "Donne une chance de trouver des crédits en cassant des minerais", 45, 1000),
    ("proc-booster", "PROC_BOOSTER", "Proc booster", "Augmente le taux de déclenchement des enchantements", 40, 500),
    ("main-doree", "MONEY_MULT", "Main dorée", "Augmente les MineCoins gagnés en cassant des minerais", 1, 2000),
    ("braquage", "MONEY_MULT", "Braquage", "Augmente l'argent gagné en cassant des minerais", 1, 1000),
    ("booster-xp", "XP_MULT", "Booster d'xp", "Augmente l'expérience gagnée sur ta pioche en cassant des minerais", 1, 1000),
    ("chercheur-xp", "XP_FLAT", "Chercheur d'xp", "Donne une chance de trouver une plus ou moins grande quantité d'xp de pioche en minant", 1, 1000),
    ("chercheur-spawner", "TREASURE", "Chercheur de spawner", "Donne une chance de trouver des spawners en cassant des minerais", 2, 1000),
    ("vente-auto", "SELL_ON_BREAK", "Vente à la casse", "Vend sur-le-champ ce qui est cassé, aux prix de la grille — l'équivalent Valoria de la vente automatique du wiki", 1, 1),
]

HOE = [
    ("speed", "HASTE", "Speed", "Donne un boost de vitesse lorsque tu casses des cultures", 1, 5),
    ("celerite", "HASTE", "Célérité", "Augmente la vitesse de frappe de ta houe", 1, 5),
    ("recolte-auto", "CROP_HARVEST", "Récolte automatique", "Casse la culture visée et ses voisines mûres, puis replante (fonctionnalité de tête de page Houe)", 1, 200),
    ("main-de-gaia", "AREA_BREAK", "Main de Gaïa", "Donne une chance de casser les cultures en 3x3", 3, 300),
    ("tree-fell", "TREE_FELL", "Arbre abattu", "Emporte le tronc et la canopée d'un seul coup — propre à l'âme hache, le wiki ne décrit que la houe", 1, 300),
    ("main-doree", "MONEY_MULT", "Main dorée", "Augmente les FarmCoins gagnés en cassant des cultures", 1, 2000),
    ("boost-xp", "XP_MULT", "Boost XP", "Augmente l'expérience gagnée sur ta houe en cassant des cultures", 1, 1000),
    ("braquage", "MONEY_MULT", "Braquage", "Augmente l'argent gagné en cassant des cultures", 1, 1000),
    ("rendement", "DOUBLE_DROP", "Bonus de rendement", "Donne une chance de doubler la récolte (fonctionnalité de tête de page Houe)", 1, 200),
    ("trouvaille", "TREASURE", "Trouvaille", "Donne une chance de trouver une clé farm en cassant des cultures", 1, 1000),
    ("casino", "TREASURE", "Casino", "Donne une chance de trouver des FarmGen en cassant des cultures", 1, 100),
    ("furie", "FURY", "Furie", "Chance d'activer le mode Furie qui augmente tes gains d'argent et de FarmCoins", 1, 50),
    ("chercheur-spawner", "TREASURE", "Chercheur de spawner", "Donne une chance de trouver des spawners en cassant des cultures", 2, 1000),
    ("chercheur-bonbon", "TREASURE", "Chercheur de bonbon", "Donne une chance de trouver des bonbons d'xp de pets en cassant des cultures", 5, 1000),
    ("vitesse-ames", "SOUL_SPEED", "Vitesse des âmes", "Permet de marcher plus rapidement sur le sable des âmes", 5, 3),
    ("jugement-divin", "AREA_BREAK", "Jugement divin", "Donne une chance d'activer le jugement divin, qui moissonne une grande étendue autour du bloc", 10, 1000),
    ("farmcoins-pouch", "MONEY_POUCH", "Farmcoins Pouch", "Donne une chance de gagner une grande quantité de FarmCoins en cassant des cultures", 1, 2000),
    ("money-pouch", "MONEY_POUCH", "Money Pouch", "Donne une chance de gagner une grande quantité d'argent en cassant des cultures", 1, 2000),
    ("double-gain", "MONEY_DOUBLE", "Double gain", "Donne une chance de doubler l'argent gagné grâce à la vente automatique", 1, 1000),
    ("proc-booster", "PROC_BOOSTER", "Proc Booster", "Augmente le taux de déclenchement des enchantements", 15, 500),
    ("chercheur-credits", "MONEY_POUCH", "Chercheur de crédits", "Donne une chance de trouver des crédits en cassant des cultures", 20, 1000),
    ("chercheur-xp", "XP_FLAT", "Chercheur d'xp", "Donne une chance de trouver une quantité d'xp en cassant des cultures", 1, 500),
    ("vente-auto", "SELL_ON_BREAK", "Vente à la casse", "Vend sur-le-champ ce qui est récolté, aux prix de la grille", 1, 1),
]

SWORD = [
    ("tranchant", "DAMAGE_MULT", "Tranchant", "Augmente les dégâts de ton épée", 1, 5),
    ("booster-xp", "XP_MULT", "Booster d'xp", "Augmente l'expérience gagnée sur ton épée en tuant des monstres", 1, 1000),
    ("mobcoins-booster", "MONEY_MULT", "Booster MobCoins", "Augmente les MobCoins gagnés en tuant des monstres", 1, 2000),
    ("speed", "SWIFT", "Speed", "Donne un boost de vitesse en tuant des monstres", 1, 5),
    ("celerite", "HASTE", "Célérité", "Augmente la vitesse de frappe en tuant des monstres", 1, 5),
    ("force", "POTION_APPLY", "Force", "Donne une chance d'obtenir l'effet de force en tuant des monstres", 1, 3),
    ("chercheur-xp", "XP_FLAT", "Chercheur d'xp", "Donne une chance de trouver une plus ou moins grande quantité d'xp en tuant des monstres", 1, 500),
    ("pillage", "DOUBLE_DROP", "Pillage", "Donne une chance d'augmenter le butin obtenu en tuant des monstres", 2, 8),
    ("chercheur-spawner", "TREASURE", "Chercheur de spawner", "Donne une chance de trouver des spawners en tuant des monstres", 2, 1000),
    ("chercheur-bonbons", "TREASURE", "Chercheur de bonbons", "Donne une chance de trouver des bonbons de pets en tuant des monstres", 5, 1000),
    ("trouvaille", "TREASURE", "Trouvaille", "Donne une chance de trouver une clé commune en tuant des monstres", 10, 1000),
    ("mobcoins-pouch", "MONEY_POUCH", "MobCoins Pouch", "Donne une chance de trouver une grande quantité de MobCoins en tuant des monstres", 5, 2000),
    ("money-pouch", "MONEY_POUCH", "Money Pouch", "Donne une chance de trouver une grande quantité d'argent en tuant des monstres", 20, 2000),
    ("proc-booster", "PROC_BOOSTER", "Proc booster", "Augmente le taux de déclenchement des enchantements", 8, 500),
    ("casino", "TREASURE", "Casino", "Donne une chance de trouver un générateur en tuant des monstres", 8, 100),
    ("autoclicker", "AUTO_SWING", "Autoclicker", "Permet de tuer des monstres automatiquement", 10, 500),
    ("briseur-monstres", "MULTI_KILL", "Briseur de monstres", "Donne une chance de tuer une grande quantité de monstres en une fois", 10, 20),
    ("braquage", "MONEY_MULT", "Braquage", "Augmente l'argent gagné en tuant des monstres", 1, 1000),
    ("critique", "CRIT", "Coup critique", "Bonus Valoria (le wiki ne le liste pas) : chance de frapper beaucoup plus fort", 1, 200),
    ("vol-de-vie", "LIFE_STEAL", "Vol de vie", "Bonus Valoria (le wiki ne le liste pas) : chance de se soigner en touchant", 5, 200),
    ("souffle", "KNOCKBACK", "Souffle de recul", "Bonus Valoria (le wiki ne le liste pas) : repousse la cible touchée", 10, 100),
    ("double-butin", "MONEY_DOUBLE", "Double gain", "Donne une chance de doubler l'argent gagné grâce à la vente automatique", 20, 1000),
    ("vente-butin", "SELL_ON_BREAK", "Vente du butin", "Vend sur-le-champ les drops du monstre tué, aux prix de la grille", 15, 1),
]

ROD = [
    ("angler", "FAST_REEL", "Angler", "Augmente l'efficacité de ta canne à pêche en réduisant le temps de pêche", 1, 10),
    ("moulinet", "AUTO_REEL", "Moulinet rapide", "La prise va directement dans ton sac, sans mouliner (fonctionnalité « pêche plus rapide » de tête de page)", 1, 1),
    ("boost-xp", "XP_MULT", "Boost d'xp", "Augmente l'expérience gagnée sur ta canne à pêche en pêchant", 1, 1000),
    ("chercheur-xp", "XP_FLAT", "Chercheur d'xp", "Augmente le gain d'expérience vanilla en pêchant", 3, 500),
    ("peche-chanceuse", "LUCK", "Pêche chanceuse", "Augmente les chances d'obtenir des trésors (fonctionnalité de tête de page)", 1, 20),
    ("main-doree", "MONEY_MULT", "Main dorée", "Augmente les FishCoins gagnés en pêchant", 10, 2000),
    ("tsunami", "MULTI_CATCH", "Tsunami", "Donne une chance d'invoquer un tsunami qui ramène plusieurs prises d'un coup", 10, 1000),
    ("trouvaille", "TREASURE", "Trouvaille", "Donne une chance de trouver une clé boost, une clé farm ou une clé commune en pêchant", 20, 1000),
    ("chercheur-bonbons", "TREASURE", "Chercheur de bonbons", "Donne une chance de trouver des bonbons de pets en pêchant", 1, 1000),
    ("money-pouch", "MONEY_POUCH", "Money Pouch", "Donne une chance de trouver une grande quantité d'argent en pêchant", 1, 2000),
    ("fishcoins-pouch", "MONEY_POUCH", "FishCoins Pouch", "Donne une chance de trouver une grande quantité de FishCoins en pêchant", 1, 2000),
    ("braquage", "MONEY_MULT", "Braquage", "Augmente l'argent gagné en pêchant", 1, 1000),
    ("casino", "TREASURE", "Casino", "Donne une chance de trouver un générateur en pêchant", 1, 100),
    ("chercheur-spawner", "TREASURE", "Chercheur de spawner", "Donne une chance de trouver des spawners en pêchant", 25, 1000),
    ("chercheur-credits", "MONEY_POUCH", "Chercheur de crédits", "Donne une chance de trouver des crédits en pêchant", 30, 1000),
    ("proc-booster", "PROC_BOOSTER", "Proc booster", "Augmente le taux de déclenchement des enchantements", 15, 500),
    ("double-gain", "MONEY_DOUBLE", "Double gain", "Donne une chance de doubler l'argent gagné grâce à la vente automatique", 25, 1000),
    ("vente-peche", "SELL_ON_BREAK", "Vente de la pêche", "Vend sur-le-champ ce qui est pêché, aux prix de la grille", 5, 1),
]

TREASURE_ITEMS = {
    # Le wiki parle de « clés », de « bonbons », de « générateurs » : des objets que Valoria n'a pas.
    # Ils deviennent l'objet déclaré ici, et l'admin met ce qu'il veut (y compris un item de son propre
    # plugin de générateurs, tant que le matériau existe sur le serveur).
    "trouvaille": ["TRIPWIRE_HOOK"],
    "chercheur-spawner": ["SPAWNER"],
    "chercheur-bonbon": ["SUGAR"],
    "chercheur-bonbons": ["SUGAR"],
    "casino": ["CHEST"],
}

# --------------------------------------------------------------------------- gains de metier (wiki)
# Chiffres REELS publies par le wiki GenTycoon, pages « Les Métiers » (Mineur, Fermier, Chasseur,
# Pêcheur). Le métier paie le BLOC cassé (ou le poisson pêché, ou le monstre tué) — ce n'est pas le
# prix de revente de l'objet qui tombe, et les deux sont gardez separes dans le plugin.
JOBS = {
    "pickaxe": {
        "source": "Mineur — « Casser » (https://wiki.gentycoon.fr/progression-metiers-and-outils/les-metiers/mineur.md)",
        "gains": {
            "STONE": 0.05, "ANDESITE": 0.07, "GRANITE": 0.07, "DIORITE": 0.07,
            "CHISELED_SANDSTONE": 0.45, "CUT_SANDSTONE": 0.45,
            "COAL_ORE": 0.50, "DEEPSLATE_COAL_ORE": 0.50,
            "COPPER_ORE": 0.50, "DEEPSLATE_COPPER_ORE": 0.50,
            "REDSTONE_ORE": 0.50, "DEEPSLATE_REDSTONE_ORE": 0.50,
            "IRON_ORE": 0.88, "DEEPSLATE_IRON_ORE": 0.88,
            "GOLD_ORE": 2.00, "DEEPSLATE_GOLD_ORE": 2.00, "NETHER_GOLD_ORE": 2.00,
            "LAPIS_ORE": 2.00, "DEEPSLATE_LAPIS_ORE": 2.00,
            "DIAMOND_ORE": 3.00, "DEEPSLATE_DIAMOND_ORE": 3.00,
            "EMERALD_ORE": 5.00, "DEEPSLATE_EMERALD_ORE": 5.00,
            "NETHER_QUARTZ_ORE": 0.10,
            "OBSIDIAN": 2.00, "ANCIENT_DEBRIS": 4.00,
        },
        "xp": {
            "STONE": 0.01, "ANDESITE": 0.75, "GRANITE": 0.25, "DIORITE": 0.01,
            "CHISELED_SANDSTONE": 0.75, "CUT_SANDSTONE": 0.75,
            "COAL_ORE": 1.25, "DEEPSLATE_COAL_ORE": 1.25,
            "COPPER_ORE": 1.25, "DEEPSLATE_COPPER_ORE": 1.25,
            "REDSTONE_ORE": 2.25, "DEEPSLATE_REDSTONE_ORE": 2.25,
            "IRON_ORE": 1.75, "DEEPSLATE_IRON_ORE": 1.75,
            "GOLD_ORE": 2.00, "DEEPSLATE_GOLD_ORE": 2.00, "NETHER_GOLD_ORE": 2.00,
            "LAPIS_ORE": 2.13, "DEEPSLATE_LAPIS_ORE": 2.13,
            "DIAMOND_ORE": 4.13, "DEEPSLATE_DIAMOND_ORE": 4.13,
            "EMERALD_ORE": 5.13, "DEEPSLATE_EMERALD_ORE": 5.13,
            "NETHER_QUARTZ_ORE": 0.01,
            "OBSIDIAN": 2.63, "ANCIENT_DEBRIS": 4.00,
        },
    },
    "axe": {
        "source": "Fermier — « Récolte » (https://wiki.gentycoon.fr/progression-metiers-and-outils/les-metiers/fermier.md)",
        "gains": {
            "WHEAT": 7.65, "CARROTS": 7.95, "POTATOES": 7.95, "BEETROOTS": 8.10,
            "PUMPKIN": 8.10, "SUGAR_CANE": 7.80, "COCOA": 7.80,
            "BROWN_MUSHROOM": 8.25, "RED_MUSHROOM": 8.25,
            "BAMBOO": 7.73, "CACTUS": 7.80,
            "SUNFLOWER": 7.65, "LILAC": 7.65, "ROSE_BUSH": 7.65, "PEONY": 7.65,
            "RED_TULIP": 7.65, "ORANGE_TULIP": 7.65, "WHITE_TULIP": 7.65, "PINK_TULIP": 7.65,
            "DANDELION": 0.08, "POPPY": 0.08,
        },
        "xp": {
            "WHEAT": 5.10, "CARROTS": 5.25, "POTATOES": 5.25, "BEETROOTS": 5.40,
            "PUMPKIN": 5.40, "SUGAR_CANE": 5.20, "COCOA": 5.20,
            "BROWN_MUSHROOM": 5.40, "RED_MUSHROOM": 0.40,
            "BAMBOO": 5.20, "CACTUS": 5.20,
            "SUNFLOWER": 5.20, "LILAC": 5.20, "ROSE_BUSH": 5.20, "PEONY": 5.20,
            "RED_TULIP": 5.20, "ORANGE_TULIP": 5.20, "WHITE_TULIP": 5.20, "PINK_TULIP": 5.20,
            "DANDELION": 5.10, "POPPY": 5.10,
        },
    },
    "sword": {
        "source": "Chasseur (https://wiki.gentycoon.fr/progression-metiers-and-outils/les-metiers/chasseur.md)",
        "gains": {
            "CHICKEN": 0.33, "COW": 0.35, "PIG": 0.50, "SHEEP": 0.35, "RABBIT": 0.50,
            "WOLF": 2.50, "MOOSHROOM": 0.35, "SQUID": 0.50,
            "CREEPER": 0.50, "SKELETON": 0.50, "ZOMBIE": 0.50, "DROWNED": 0.38, "HUSK": 0.38,
            "SPIDER": 0.50, "CAVE_SPIDER": 0.50, "MAGMA_CUBE": 0.50, "BLAZE": 0.50,
            "ENDERMAN": 0.50, "ZOMBIFIED_PIGLIN": 0.50, "PIGLIN": 1.00, "SILVERFISH": 0.50,
            "GUARDIAN": 2.00, "VINDICATOR": 0.50, "EVOKER": 0.50, "PILLAGER": 0.50,
            "WITCH": 0.75, "WITHER_SKELETON": 1.50, "IRON_GOLEM": 4.00, "ZOGLIN": 4.00,
            "GHAST": 10.00, "WITHER": 50.00,
        },
        "xp": {
            "CHICKEN": 0.35, "COW": 0.38, "PIG": 0.30, "SHEEP": 0.38, "RABBIT": 0.30,
            "WOLF": 0.50, "MOOSHROOM": 0.38, "SQUID": 0.50,
            "CREEPER": 1.25, "SKELETON": 0.65, "ZOMBIE": 0.35, "DROWNED": 0.38, "HUSK": 0.38,
            "SPIDER": 0.80, "CAVE_SPIDER": 1.75, "MAGMA_CUBE": 0.65, "BLAZE": 1.15,
            "ENDERMAN": 2.18, "ZOMBIFIED_PIGLIN": 0.50, "PIGLIN": 2.50, "SILVERFISH": 0.50,
            "GUARDIAN": 4.00, "VINDICATOR": 2.18, "EVOKER": 2.18, "PILLAGER": 2.18,
            "WITCH": 4.50, "WITHER_SKELETON": 2.18, "IRON_GOLEM": 4.50, "ZOGLIN": 4.00,
            "GHAST": 10.00, "WITHER": 60.00,
        },
    },
    "rod": {
        "source": "Pêcheur (https://wiki.gentycoon.fr/progression-metiers-and-outils/les-metiers/pecheur.md)",
        "gains": {
            "COD": 1.50, "COOKED_COD": 1.50, "SALMON": 1.50, "COOKED_SALMON": 1.50,
            "TROPICAL_FISH": 3.00, "PUFFERFISH": 3.00,
        },
        "xp": {
            "COD": 1.50, "COOKED_COD": 1.50, "SALMON": 1.50, "COOKED_SALMON": 1.50,
            "TROPICAL_FISH": 3.00, "PUFFERFISH": 3.00,
        },
    },
}


SELL_PRICES = {
    "pickaxe": [("cobblestone", 1.0), ("deepslate", 1.2), ("diamond", 60.0), ("emerald", 45.0),
                ("gold_ingot", 20.0), ("iron_ingot", 12.0), ("copper_ingot", 3.0), ("redstone", 4.0),
                ("lapis_lazuli", 5.0), ("coal", 3.5), ("netherite_scrap", 400.0),
                ("netherrack", 0.5), ("stone", 1.0), ("granite", 1.0), ("diorite", 1.0),
                ("andesite", 1.0), ("blackstone", 1.5), ("end_stone", 1.5), ("gravel", 0.6),
                ("sand", 0.6), ("obsidian", 30.0)],
    "axe": [("oak_log", 4.0), ("spruce_log", 4.0), ("birch_log", 4.0), ("jungle_log", 5.0),
            ("acacia_log", 5.0), ("dark_oak_log", 5.0), ("mangrove_log", 5.0), ("cherry_log", 6.0),
            ("crimson_stem", 7.0), ("warped_stem", 7.0), ("oak_leaves", 0.4), ("wheat", 3.0),
            ("carrot", 2.5), ("potato", 2.5), ("beetroot", 3.0), ("melon_slice", 1.5),
            ("pumpkin", 4.0), ("sugar_cane", 2.0), ("cactus", 2.0), ("bamboo", 0.8),
            ("nether_wart", 4.0), ("cocoa_beans", 3.0)],
    "rod": [("cod", 6.0), ("salmon", 8.0), ("tropical_fish", 25.0), ("pufferfish", 20.0),
            ("leather", 5.0), ("saddle", 250.0), ("bow", 30.0), ("name_tag", 120.0),
            ("nautilus_shell", 60.0), ("lily_pad", 5.0), ("string", 2.5), ("bone", 2.0),
            ("ink_sac", 6.0), ("feather", 1.5)],
    "sword": [("rotten_flesh", 1.0), ("bone", 2.0), ("string", 2.5), ("spider_eye", 3.0),
              ("gunpowder", 6.0), ("ender_pearl", 40.0), ("blaze_rod", 25.0), ("slime_ball", 8.0),
              ("ghast_tear", 45.0), ("phantom_membrane", 20.0), ("leather", 5.0), ("feather", 1.5),
              ("porkchop", 4.0), ("beef", 4.0), ("mutton", 3.5), ("chicken", 3.0),
              ("zombie_head", 90.0), ("wither_rose", 120.0)],
}

MATCHES = {
    # Un seul tag pour la pioche : `#minecraft:ores` n'existe pas en vanille (c'est une convention
    # Fabric, namespace `c:`), et un tag inconnu ne fait que polluer le log. `mineable/pickaxe`
    # couvre deja tous les minerais, et isOre() filtre les capacites de zone.
    "pickaxe": (["\"#minecraft:mineable/pickaxe\""], [], []),
    # Pas de tag `crops` : son existence depend de la version (les cultures sont filtrees par les
    # noms explicites ci-dessous, valable sur toutes), et un tag absent se paie en avertissement
    # au demarrage du plugin pour rien.
    "axe": (["\"#minecraft:logs\"", "\"#minecraft:mineable/axe\"", "\"#minecraft:saplings\""],
            ["WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "MELON", "PUMPKIN", "SUGAR_CANE",
             "CACTUS", "BAMBOO", "COCOA", "NETHER_WART", "SWEET_BERRY_BUSH", "CARVED_PUMPKIN"],
            []),
    "rod": ([], [], []),
    "sword": ([], [], []),
}

HEADER = """# ValoriaTools — multi-outil à âmes commutantes, barème aligné sur le wiki GenTycoon
#
# Principe : UN item, quatre comportements, choisis par le bloc visé. Chaque âme (pioche, hache/houe,
# canne, épée) a SON palier, et chaque capacité du wiki a SON niveau, acheté case par case dans le menu.
#
# Source du barème : https://wiki.gentycoon.fr/progression-metiers-and-outils/les-outils (Pioche 24,
# Houe 23, Épée 23, Canne 18 lignes) et les pages « Les Métiers » pour les gains par bloc, poisson ou
# monstre (table `jobs:` ci-dessous). Voir docs/WIKI-GENTYCOON-OUTILS.md : tableaux complets,
# correspondances, et la liste de ce que le wiki NE publie PAS (prix des niveaux et chances : réglages
# Valoria, assumés comme tels — le contrôle de config refuse toute valeur `jobs:` non publiée).
#
# Une ligne de capacité :
#   id           : clé de stockage du niveau acheté (unique par âme) — c'est elle que lit tools.yml
#   type         : le noyau compris par le moteur (liste en bas de ce fichier)
#   label        : le nom du wiki, tel quel
#   desc         : la description du wiki, telle quelle
#   unlock       : le palier d'âme qui autorise l'achat (« Level minimum » ou « Prestige mini » du wiki)
#   max-level    : le « Niveau max d'enchantement » du wiki (1 = capacité simple on/off)
#   free         : true = le niveau 1 est offert dès que le palier le permet
#   cle: 0.02 / cle-step / cle-cap   : valeur au niveau 1, pas par niveau, plafond
#   price / price-step / price-ratio / price-cap : prix du niveau suivant (sinon: défauts de l'âme)
#
# Régénéré par `python3 scripts/gen-tools-config.py` ; éditable ensuite, `/tools reload` suffit.

# Coupe tout le plugin (aucun événement traité). Utile pour tester sans retirer le jar.
enabled: true

tool:
  # Matériau de départ de l'item. Le comportement ne vient PAS de là : un NETHERITE_PICKAXE marqué vaut
  # pioche + hache + canne + épée. Ce n'est que l'apparence (et la solidité de base) — et, avec
  # `morph-by-target: true` plus bas, seulement l'apparence INITIALE : dès qu'une âme sert, c'est le
  # matériau de cette âme (`tools.<ame>.material`) qui prend la main.
  material: NETHERITE_PICKAXE
  display-name: "&6⚒ Multi-outil de Valoria"
  lore:
    - "&7Un seul outil : il change d'âme"
    - "&7selon le bloc que tu regardes."
    - ""
  # true = l'item ne s'use jamais. Recommandé : un outil qui se casse emporte l'ergonomie
  # (le joueur doit le redemander) et les paliers, eux, restent dans tools.yml.
  unbreakable: true
  # Cache les counters d'enchantement et de durabilité dans la tooltip.
  hide-flags: true
  # ------------------------------------------------------------------ la garde de l'objet
  # Ces trois réglages vont ensemble : ils font du multi-outil UN objet, un par joueur, qui ne se perd
  # pas. L'objet ne se lâche pas (touche Q), ne se range pas (coffre, baril, cadre, entonnoir, cheval,
  # ender chest), ne se donne pas, survit à la mort, et une deuxième copie est détruite à la connexion.
  # Le point sensible est auto-give : un outil qu'on ne peut ni poser ni donner devient une prison si le
  # joueur peut le perdre quand même (/clear, un gamemode qui recrée l'inventaire, un plugin qui purge
  # les sacs). auto-give: true garantit qu'il revient, donc que les deux autres sont sans risque.
  undroppable: true
  single-per-player: true
  auto-give: true
  # ------------------------------------------------------------------ ce que l'item montre
  # true = l'item PREND LE MATÉRIAU de l'âme avec laquelle tu es en train d'interagir : pic sur la
  # pierre, hache sur le tronc, canne au lancer, épée au coup. Le moteur choisissait déjà son âme bloc
  # par bloc — seule l'apparence était figée sur `material` ci-dessus, et le joueur ne pouvait donc pas
  # savoir quelle âme allait payer le prochain geste. false = l'item garde `material`, la lore seule
  # suit l'usage (un serveur qui vend des skins par pack de textures préfère souvent ne pas changer
  # l'objet lui-même).
  morph-by-target: true
  # Combien de capacités payées la lore de l'item énumère (`nom + niveau`, l'âme en cours seulement).
  # Au-delà, la tooltip dépasse l'écran et le reste est annoncé en une ligne avec le raccourci /tools :
  # un outil au maximum du barème a vingt-deux capacités payées, ce n'est pas lisible d'un coup.
  lore-abilities: 8
  # La vitesse de minage des capacités HASTE est posée TANT QUE l'outil est en main : réappliquée une
  # fois par seconde et à chaque changement de main, retirée dès que l'outil quitte la main, qu'on entre
  # dans un monde hors de `worlds`, ou à la déconnexion. Avant ce réglage, la vitesse n'était re-posée
  # qu'APRÈS un bloc cassé : invisible sur le premier bloc de la session et perdue dès qu'on regardait
  # un coffre entre deux filons — c'est exactement ce qu'un joueur appelle « mes capacités ne sont pas
  # actives, je casse les blocs normalement ». false = ancien comportement (uniquement à la cassure).
  haste-while-held: true
  # L'âme utilisée quand le joueur ne vise aucun bloc reconnu (clic dans le vide).
  fallback-tool: PICKAXE
  # Ce que coûte l'outil lui-même, via /tools buy. 0 = gratuit (/tools buy devient alors un give).
  # Avec auto-give: true, l'objet est déjà rendu à la connexion : ce prix n'est donc débité QUE par
  # /tools buy, jamais pour récupérer un outil perdu. C'est voulu — on ne facture pas un joueur d'un
  # accident du serveur — mais cela veut dire que ce n'est pas la porte d'entrée de l'économie.
  # C'est la premiere depense d'un tycoon : laisse-le a 0 tant que ton economie n'est pas reglee,
  # mais ne le remonte pas sans regarder `sell.prices` — le joueur rembourse l'outil en quelques
  # milliers de blocs, c'est ce calcul qu'il faut faire, pas un chiffre au pif.
  price: 0
  # Réservoir des capacités TREASURE (Trouvaille, Chercheur de spawner, Casino, bonbons…). Une
  # capacité peut déclarer SON PROPRE réservoir avec `items: [...]` : celui-ci sert de repli. On ne
  # touche pas à la table de trésors interne du serveur (reflection dans du privé, cassable à chaque
  # version) : on ajoute les objets listés, avec la probabilité de la capacité.
  treasure:
    items:
      - NAME_TAG
      - TRIPWIRE_HOOK
      - SUGAR

sell-on-break:
  # Multiplicateur global appliqué aux prix de `sell.prices` ci-dessous.
  multiplier: 1.0
  # En dessous de ce prix unitaire, rien n'est vendu (le joueur ramasse, sinon le chat sature).
  min-value: 0.05
  # true = la vente automatique ne se déclenche que accroupi (le geste du « je veux vendre »).
  only-when-sneaking: false
  # true = les blocs reconnus par une âme mais sans prix déclaré sont quand même mis dans le sac ;
  # false = ils tombent au sol. Les deux évitent l'accumulation d'un inventaire qui ne vend rien.
  auto-sell-unmatched: true

# Ce que l'outil mesure (stats.yml) et ou il a le droit d'agir : regles de serveur, pas de bareme.
stats:
  # false = aucun compteur tenu, /tools top reste vide et le menu n'affiche pas les lignes de mesure.
  # Rien dans le jeu ne depend des compteurs (classement et diagnostic d'equilibrage seulement) : on
  # peut les couper sans rendre l'outil fou.
  enabled: true

tools:
  # Les mondes ou l'outil agit (les ames s'en absterrent ailleurs : rien n'est casse, rien n'est paye).
  # Liste vide = tous les mondes. C'est la porte « zone protegeable » du plugin, et elle est 100 %
  # Bukkit : exiger une claim d'ile demanderait l'API d'un plugin de skyblock, dont ce dépôt ne depend
  # volontairement pas (le controle « zéro API tierce » du build refuserait meme de compiler l'appel).
  allowed-worlds: []
"""


def flow(entry, soul):
    """Une capacité → une ligne de YAML en flux, pour que 24 capacités tiennent dans un écran."""
    ability_id, kernel, label, desc, unlock, max_level = entry
    parts = ["id: " + ability_id, "type: " + kernel]
    parts.append('label: "%s"' % label.replace('"', "'"))
    parts.append('desc: "%s"' % desc.replace('"', "'"))
    parts.append("unlock: %d" % unlock)
    parts.append("max-level: %d" % max_level)
    free = kernel in ("HASTE", "SELL_ON_BREAK", "TREE_FELL", "CROP_HARVEST", "AUTO_REEL") or max_level == 1
    if free:
        parts.append("free: true")
    for name, base, step, cap in KERNELS.get(kernel, []):
        parts.append("%s: %s" % (name, fmt(base)))
        if step:
            parts.append("%s-step: %s" % (name, fmt(step)))
        if cap:
            parts.append("%s-cap: %s" % (name, fmt(cap)))
    for flag, value in FLAGS.get(kernel, []):
        parts.append("%s: %s" % (flag, value))
    if kernel == "AREA_BREAK" and soul == "pickaxe":
        for flag, value in FLAGS["AREA_BREAK_PIOCHE"]:
            parts.append("%s: %s" % (flag, value))
    if kernel == "AREA_BREAK" and soul in ("axe",) :
        for flag, value in FLAGS["AREA_BREAK_JOIE"]:
            parts.append("%s: %s" % (flag, value))
    if kernel == "TREASURE":
        items = TREASURE_ITEMS.get(ability_id)
        if items:
            parts.append("items: [" + ", ".join(items) + "]")
    # Le prix d'un niveau est GEOMETRIQUE : `price × price-ratio^(niveau-1)`, borne par `price-cap`.
    base, ratio, cap = price_curve(max_level)
    parts.append("price: %d" % base)
    if ratio > 1.0:
        parts.append("price-ratio: %s" % ("%.6f" % ratio).rstrip("0").rstrip("."))
    parts.append("price-cap: %d" % cap)
    return "      - {" + ", ".join(parts) + "}"


# --------------------------------------------------------------------------- courbe de prix
# Le prix du niveau 1 de la capacite, par plage de `max-level`. Une capacite courte est CHERE au niveau 1 :
# elle n'a que trois ou cinq crans pour valoir sa place, et un « Tranchant 5 » a 250 $ le cran etait la
# capacite la plus forte du jeu offerte au prix d'une pile de pierre. Une capacite a 2000 crans part bas :
# c'est la somme de ses deux mille niveaux qui fait la depense, pas son premier.
PRICE_BASE = {1: 150000, 3: 60000, 5: 40000, 8: 24000, 10: 20000, 20: 10000, 50: 5000,
              100: 3000, 200: 1500, 300: 1000, 500: 700, 1000: 400, 2000: 250}

# Combien de fois le DERNIER niveau coute le premier. C'est ce facteur, et non le prix de depart, qui
# fait qu'une capacite se termine plus cher qu'elle ne commence : x8 sur trois crans (la marche est
# brutale, c'est voulu), x200 sur deux mille (chaque cran est a peine plus cher que le precedent, mais
# le dernier vaut deux cents fois le premier). Le ratio par niveau s'en deduit : span^(1/(n-1)).
# Le span est borne par le moteur : `ToolsConfig.ratio()` refuse un `price-ratio` au-dessus de 2, donc un
# span de x8 sur trois crans (ratio 2,83) serait ecrit dans le fichier et ignore a la lecture. Les plages
# courtes montent donc par leur PRIX DE DEPART plutot que par leur pente, ce qui revient au meme pour le
# joueur et garde le fichier et le moteur d'accord.
PRICE_SPAN = {1: 1, 3: 3.5, 5: 13, 8: 24, 10: 30, 20: 40, 50: 60,
              100: 80, 200: 100, 300: 120, 500: 140, 1000: 160, 2000: 200}

# Ce que `ToolsConfig.ratio()` accepte au maximum. Depasser = un fichier que le moteur relit autrement.
MAX_RATIO = 2.0

# Plafond dur d'un niveau, quelle que soit la courbe. Aligne sur `ToolsConfig.Ability.PRICE_CEILING` :
# le fichier ne doit pas pouvoir ecrire un prix que le moteur refuserait ensuite en silence.
PRICE_CAP = 100000000


def price_curve(max_level):
    """(prix du niveau 1, ratio par niveau, plafond) pour une capacite de `max_level` crans."""
    bucket = next((k for k in sorted(PRICE_BASE) if max_level <= k), 2000)
    base = PRICE_BASE[bucket]
    span = PRICE_SPAN[bucket]
    if max_level <= 1 or span <= 1:
        return base, 1.0, PRICE_CAP
    # `span` fois plus cher au dernier cran qu'au premier : le ratio est la racine (n-1)-ieme du span.
    ratio = span ** (1.0 / (max_level - 1))
    if ratio > MAX_RATIO:
        raise SystemExit(
            "ERREUR: max-level %d demande un price-ratio de %.3f, or `ToolsConfig.ratio()` plafonne a "
            "%.1f : le fichier ecrirait une courbe que le moteur ne relirait pas. Baisse PRICE_SPAN[%d] "
            "ou monte PRICE_BASE[%d]." % (max_level, ratio, MAX_RATIO, max_level, max_level))
    return base, ratio, PRICE_CAP


def fmt(value):
    if isinstance(value, float):
        text = ("%.4f" % value).rstrip("0").rstrip(".")
        return text if text else "0"
    return str(value)


def soul(name, title, abilities, notes):
    tags, blocks, namespaces = MATCHES[name]
    lines = []
    lines.append("  # " + "─" * 24 + " " + title + " " + "─" * 24)
    lines.append("  %s:" % name)
    lines.append("    # Le matériau de l'item QUAND cette âme sert : avec `tool.morph-by-target: true`,")
    lines.append("    # c'est lui que le joueur voit dans sa main (pic sur la pierre, hache sur le tronc),")
    lines.append("    # et c'est aussi l'icône de la rangée dans le panneau. Le comportement ne vient pas")
    lines.append("    # de là : il vient des capacités de cette âme.")
    lines.append("    material: %s" % {"pickaxe": "DIAMOND_PICKAXE", "axe": "DIAMOND_AXE",
                                      "rod": "FISHING_ROD", "sword": "DIAMOND_SWORD"}[name])
    if notes:
        for note in notes:
            lines.append("    # " + note)
    lines.append("    matches:")
    lines.append("      # Les tags suivent les nouveaux blocs tout seuls ; les noms sont le filet pour les")
    lines.append("      # serveurs anciens (1.7-1.12) qui n'ont pas de registre de tags.")
    lines.append("      tags:")
    for tag in tags:
        lines.append("        - %s" % tag)
    if not tags:
        lines.append("        []")
    lines.append("      blocks: [%s]" % ", ".join(blocks))
    lines.append("      namespaces: [%s]" % ", ".join(namespaces))
    lines.append("    harvest:")
    lines.append("      # « Replantation instantanée » de la page Houe : la culture mûre est coupée et")
    lines.append("      # remise à l'âge zéro dans le même geste. false = on récolte sans replanter.")
    lines.append("      replant: true")
    lines.append("    xp-per-block: %d" % (1 if name == "pickaxe" else 0))
    lines.append("    durability-cost: 1")
    lines.append("    upgrade:")
    lines.append("      # 50 paliers : le wiki verrouille certaines capacités au « level 45 », et le")
    lines.append("      # prestige (0 à 20) tient dans les vingt premiers. Un palier par capacité ouverte.")
    lines.append("      max-tier: 50")
    lines.append("      # Prix du palier suivant : base × ratio^palier, plafonné. Pas de liste de 49 prix :")
    lines.append("      # `upgrade.prices: [...]` reste lu s'il est écrit, et prend alors la main.")
    lines.append("      price-base: 1500")
    lines.append("      price-ratio: 1.12")
    lines.append("      price-cap: 2500000")
    lines.append("    ability-price:")
    lines.append("      # Repli pour une capacite ecrite a la main SANS `price:` — chaque ligne generee")
    lines.append("      # porte deja son propre `price` + `price-ratio` + `price-cap`, donc ce bloc ne sert")
    lines.append("      # qu'a l'admin qui ajoute une capacite sans reflechir a sa courbe.")
    lines.append("      # Ce bloc est FRERE de `abilities:` et non dedans : un bloc YAML a la fois table")
    lines.append("      # et sequence est invalide, et SnakeYAML jette a la lecture du config.yml du jar.")
    lines.append("      base: 2000")
    lines.append("      step: 400")
    lines.append("      cap: %d" % PRICE_CAP)
    lines.append("    abilities:")
    for entry in abilities:
        lines.append(flow(entry, name))
    jobs = JOBS[name]
    lines.append("    jobs:")
    lines.append("      # Gains du métier publiés par le wiki : " + jobs["source"])
    lines.append("      # Clé = matériau cassé, objet pêché ou type d'entité tuée ; `gains` = argent,")
    lines.append("      # `xp` = expérience vanilla rendue (le reliquat décimal est reporté, voir le")
    lines.append("      # listener — 0,01 XP par roche arrondi bloc par bloc ne paierait jamais rien).")
    lines.append("      # Ces montants s'AJOUTENT à la revente des drops ci-dessous, ils ne la remplacent pas.")
    lines.append("      gains:")
    for key in sorted(jobs["gains"]):
        lines.append("        %s: %s" % (key, fmt(jobs["gains"][key])))
    lines.append("      xp:")
    for key in sorted(jobs["xp"]):
        lines.append("        %s: %s" % (key, fmt(jobs["xp"][key])))
    lines.append("    sell:")
    lines.append("      multiplier: 1.0")
    lines.append("      min-value: 0.05")
    lines.append("      # Le barème de vente propre à cette âme : c'est ce que paient la capacité de vente")
    lines.append("      # à la casse et `/tools sell`. Mets-y les prix de ton serveur, un par matériau.")
    lines.append("      prices:")
    for material, price in SELL_PRICES[name]:
        lines.append("        %s: %s" % (material.upper(), fmt(price)))
    return "\n".join(lines)


FOOTER = """
# ─────────────────────────────────────────────────────────────────────────────────────────────────
# Noyaux compris par le moteur (toute autre clé est refusée au chargement, pas ignorée) :
#
#   VEIN              arrache les blocs voisins du même type (Briseur) — max-blocks, similar-blocks-only
#   TREE_FELL         abat tronc + canopée (max-blocks, max-height)
#   AREA_BREAK        casse autour du bloc : rayon, `ores-only` (minerais), `flat` (une seule assise),
#                     `particles` — Onde sismique, Explosive, Surcharge, Main de Gaïa, Jugement divin
#   EXTRA_BLOCK       casse N blocs identiques collés au bloc visé (Seconde main)
#   GHOST_MINES       vagues différées qui minent autour (Pioche fantomatique) — waves, interval
#   CROP_HARVEST      récolte les cultures mûres de la zone, et replante si harvest.replant est vrai
#   AUTO_SMELT        remplace les drops par leur cuisson (recettes du serveur, pas une table à nous)
#   FORTUNE / DOUBLE_DROP   ajoutent des drops (chance, extra-min, extra-max)
#   SELL_ON_BREAK     vend ce qui est cassé, aux prix de `sell.prices`
#   INFINITE_DURABILITY  usure désactivée pour cette âme (voir aussi tool.unbreakable)
#   MONEY_MULT / MONEY_DOUBLE / MONEY_POUCH  +Braquage, Double gain, les « Pouch », Main dorée
#   XP_MULT / XP_FLAT +Booster d'xp, Chercheur d'xp (l'XP est celle du joueur : giveExp)
#   TREASURE          donne un objet du réservoir (clés, spawners, bonbons, générateurs)
#   RANDOM_ENCHANT    pose un enchantement réel sur l'item (Charognard) — liste `enchants` obligatoire
#   PROC_BOOSTER      multiplie la chance de DÉCLENCHEMENT de toutes les autres capacités de l'âme
#   FURY              mode temporaires : multiplicateur d'argent pendant une durée (Furie)
#   SOUL_SPEED        marche plus vite sur le sable des âmes, vitesse rendue ensuite (Vitesse des âmes)
#   HASTE / SWIFT     vitesse de minage / vitesse de déplacement, en effet de potion court
#   CRIT / DAMAGE_MULT / LIFE_STEAL / KNOCKBACK / POTION_APPLY / AUTO_SWING / MULTI_KILL   (épée)
#   AUTO_REEL / FAST_REEL / MULTI_CATCH / LUCK   (canne)
#
# Le nom de noyau est insensible à la casse et aux tirets. `INSTANT_BREAK` (casser n'importe quel bloc
# au contact) n'est volontairement PAS fourni : il dépend du raycast client et de la capacité
# INSTANT_BREAK du joueur, deux choses dont le comportement change selon les versions — le plugin
# préfère ne rien promettre plutôt que de promettre un comportement incertain.
#
# Contrôle de ce fichier : `python3 scripts/verify-tools-config.py` (71+ assertions, dont une sur le
# nombre de capacités par âme et une sur les verrous qui dépassent max-tier).
# ─────────────────────────────────────────────────────────────────────────────────────────────────
"""


def main() -> None:
    body = [HEADER]
    body.append(soul("pickaxe", "PIOCHE — 24 capacités du wiki", PICKAXE,
                     ["Les verrous reprennent le wiki : « Level minimum 45 » = palier 45 de l'âme.",
                      "L'âme pioche est celle qui mine les blocs durs et les minerais."]))
    body.append("")
    body.append(soul("axe", "HACHE / HOUE — 23 capacités (barème de la Houe du wiki)", HOE,
                     ["Le wiki de GenTycoon ne décrit pas de hache : son barème de houe est donc porté",
                      "par cette âme, qui reconnaît aussi bien les troncs que les cultures."]))
    body.append("")
    body.append(soul("sword", "ÉPÉE — 23 capacités", SWORD,
                     ["L'âme épée ne cible pas de bloc : elle s'active sur les entités vivantes, et ses",
                      "gains sont calculés à la mort du monstre (le seul moment où le serveur a décidé",
                      "qui a tué — donc impossible à farm sur un mob qui survit)."]))
    body.append("")
    body.append(soul("rod", "CANNE À PÊCHE — 18 capacités", ROD,
                     ["La canne ne reconnaît aucun bloc : son âme s'active au lancer, pas au contact."]))
    body.append(FOOTER)
    text = "\n".join(body).replace("Déefaults", "Défauts")
    OUT.write_text(text + ("\n" if not text.endswith("\n") else ""))
    print("%s : %d lignes, %d capacités au total" % (OUT, len(text.splitlines()),
          len(PICKAXE) + len(HOE) + len(SWORD) + len(ROD)))


if __name__ == "__main__":
    main()
