# ValoriaTycoon

Framework Tycoon modulaire pour **Paper 26.2**, développé en Java 25.

> État actuel : spawn médiéval généré, warps extensibles, tutoriel d’onboarding, économie SQLite, grandes farms classées, multi-tool, compactage récursif, quatre métiers permanents, générateurs, clés/caisses physiques, pets évolutifs, Skyblocks privés, quêtes répétables, dix rangs médiévaux, classements asynchrones et hologrammes de spawn.

## Fonctionnalités disponibles

### Socle et économie

- bootstrap fail-closed et architecture par services ;
- configuration validée et messages MiniMessage ;
- SQLite en WAL avec worker dédié et migrations versionnées ;
- argent exact en centimes, transactions atomiques et journal d'audit ;
- cache write-through limité aux joueurs connectés ;
- `/balance`, `/pay <joueur> <montant>` ;
- `/isadmin reload|give|setmoney|petkey|cratekey|debug` ;
- contrat public asynchrone `EconomyService`.

### Farms publiques partagées

`/farm` ouvre une interface configurable vers quatre mondes générés :

- **mine** : quatre zones charbon → fer/cuivre → or/redstone/lapis → diamant/émeraude ;
- **champs** : quatre zones blé → carottes → pommes de terre → betteraves ;
- **pêche** : océan commun à tous les rangs avec plateforme centrale ;
- **forêt** : quatre zones chêne → bouleau → sapin → chêne noir.

Le fichier `farms.yml` contrôle notamment les mondes, seeds, bordures, cavernes, ponts, icônes, blocs récoltables, délais de régénération et prix de vente. La mine est composée de quatre **cavernes gigantesques de 2 048×2 048 blocs**, dimensionnées pour accueillir plusieurs dizaines de joueurs simultanément. Leur volume utile dépasse cent blocs de hauteur. Le sol et le plafond utilisent plusieurs couches de bruit cohérent : relief progressif, parois irrégulières, piliers rocheux naturels, concrétions de spéléothèmes et grandes poches de pierre, deepslate, tuf, andésite, granite, diorite et calcite. Le résultat se rapproche d’une immense grotte vanilla de survie plutôt que d’une salle carrée ou d’une carrière en terrasses. Les minerais normaux et leurs variantes deepslate restent limités à environ 2,25–8 % selon la grotte et chaque bloc cassé réapparaît avec son état d’origine via la file de régénération serveur. Les cavernes sont séparées par 1 024 blocs de vide et reliées uniquement par des ponts de quinze blocs de large. Chaque portique vérifie le rang sans SQL ; lorsqu’un joueur non autorisé tente de le franchir à pied, le déplacement est annulé puis une impulsion configurable le propulse vers sa grotte. La forêt utilise désormais quatre îles boisées de **2 048×2 048 blocs**, séparées par 1 024 blocs de vide et reliées par des ponts classés de treize blocs de large. Le relief est organique, les chemins secondaires serpentent entre les collines et chaque cellule d’arbre reçoit un décalage, une hauteur, un tronc et une couronne déterministes. Les chênes sont larges, les bouleaux plus fins, les sapins hauts et coniques, et les chênes noirs forment les canopées les plus massives. Le sol varie entre herbe, podzol, terre stérile et mousse, avec fougères, fleurs, champignons, rochers, éclairage de chemin et une place protégée. Les champs utilisent quatre îles monoculture de **5 120 blocs de diamètre**, soit environ vingt-cinq fois la surface précédente. Toute l’irrigation visible a été supprimée : les cultures sont générées hydratées, restent matures et sont régénérées directement par ValoriaTycoon. Il ne reste que d’immenses rangées continues, de rares routes de service espacées de 256 blocs, une place d’arrivée, une grange, un moulin et des décorations repoussées sur les côtés. Les îles agricoles et forestières sont séparées par 1 024 blocs de vide. Le monde de pêche conserve un océan commun de 8 000 blocs de bordure avec une plateforme centrale de 81×81. Les zones sont débloquées aux rangs Sans rang, Artisan, Chevalier et Comte. Un clic dans `/farm` rejoint la première zone ; les suivantes sont atteintes par les ponts dont les barrières serveur vérifient le rang à chaque déplacement, téléportation et mouvement en véhicule.

Garanties mises en place :

- génération déterministe et thread-safe des chunks ;
- protection du spawn de chaque zone et des blocs non autorisés ;
- validation du rang dans le menu, à la téléportation, au déplacement, en véhicule et à la casse ;
- blocage des placements, bateaux, wagonnets, seaux, explosions, feu, pistons et modifications par outils ;
- régénération via une seule file de priorité bornée, et non une tâche par bloc ;
- report automatique d’un bloc si sa régénération toucherait la hitbox d’un joueur ;
- régénérations sauvegardées en SQLite pour survivre aux redémarrages ;
- aucun chargement forcé de chunk lors d'une régénération ;
- téléportation Paper asynchrone avec cooldown ;
- permissions `tycoon.farm`, `tycoon.autosell` et `tycoon.bypass`.

### Vente automatique individuelle et niveaux

Chaque joueur commence au **niveau 0**, avec la vente automatique verrouillée. `/autosell` ouvre un panneau dédié uniquement à l'activation/désactivation. L'achat des niveaux se fait dans le panneau des améliorations d'outil. `/farm` reste réservé au choix des mondes de ressources.

Progression configurable par défaut :

| Niveau | Effet | Prix provisoire |
|---:|---|---:|
| 1 | Débloque l'auto-sell, multiplicateur x1 | 100 000 $ |
| 2 | Multiplicateur de vente x2 | 500 000 $ |
| 3 | Multiplicateur de vente x3 | 2 000 000 $ |
| 4 | Multiplicateur de vente x4 | 10 000 000 $ |
| 5 | Multiplicateur de vente x5 | 50 000 000 $ |

Ces prix sont volontairement élevés et se modifient dans `farms.yml` lorsque l'économie finale est équilibrée. Lors de la migration depuis le schéma v2, les comptes existants conservent le niveau 1, car l'auto-sell leur était déjà accessible.

Garanties :

- achat confirmé par `Shift + clic`, puis niveau et débit économique dans **une seule transaction SQLite** ;
- impossibilité d'activer l'auto-sell avant le niveau 1 ;
- niveau, état activé/désactivé et solde mis en cache après le commit ;
- multiplicateurs exacts en `BigDecimal`, sans calcul monétaire flottant ;
- prix exacts configurés par ressource et par farm ;
- support des drops Fortune/Silk Touch réellement produits par Paper ;
- support des prises de pêche ;
- ventes regroupées par joueur afin d'éviter une écriture SQL par bloc ;
- en cas d'échec connu de transaction, les objets originaux sont rendus ;
- flush sécurisé des ventes au logout et à l'arrêt du plugin.

### Outils et capacités

Le joueur utilise un **seul et unique objet Multi-tool de Valoria**, sans pelle, composé de quatre formes :

- pioche ;
- hache ;
- houe ;
- canne à pêche.

Il ne s’agit pas de quatre objets regroupés dans l’inventaire : le même `ItemStack`, dans le même emplacement, conserve son identifiant et change de matériau/modèle. Le multi-tool reconnaît le bloc ciblé grâce aux tags Paper : pierre, roche et minerais sélectionnent la pioche ; bois et troncs sélectionnent la hache ; cultures et blocs adaptés sélectionnent la houe. Un clic droit en visant l'eau transforme ce même objet en canne à pêche, en conservant son propriétaire, son rang et toutes ses métadonnées. Les quatre formes sont totalement incassables. Le premier clic change la forme, le suivant lance normalement la ligne.

Chaque compte reçoit automatiquement un seul multi-tool lié par un identifiant déterministe. Les anciens outils Valoria sont migrés ; si plusieurs copies locales existent, une seule conserve l’identité du multi-tool et les autres redeviennent des outils normaux. Les outils vanilla ordinaires ne déclenchent ni transformation, ni capacités, ni gains de métier/coins. Le multi-tool ne peut pas être jeté, placé dans un conteneur, utilisé dans une recette ou ramassé par un autre joueur ; il reste également conservé lors d’une mort sans `KEEP_INVENTORY`.

Son matériau physique évolue automatiquement après chaque promotion, et **chaque rang possède quatre textures exclusives**, même lorsque deux rangs partagent le même matériau :

| Rang | Matériau physique | Identité visuelle RP commune aux 4 formes |
|---|---|---|
| Sans rang | Bois | Outil brut, ébréché et lié sommairement |
| Citoyen | Bois | Chêne travaillé, sceau en laiton et liens civiques verts |
| Artisan | Pierre | Pierre renforcée avec rivets de cuivre |
| Marchand | Pierre | Pierre polie, sertissage doré et pièce d’émeraude |
| Écuyer | Fer | Fer clair et pampille de tabard bleu |
| Chevalier | Fer | Acier lumineux marqué d’une croix héraldique rouge |
| Baron | Or | Or flamboyant, garde élargie et rubis |
| Vicomte | Or | Or pâle, filigranes violets et doubles améthystes |
| Comte | Diamant | Cristal cyan avec étoile de saphir |
| Marquis | Diamant | Diamant noble, rayons dorés et joyau violet |
| Duc | Netherite | Netherite couronnée, runes cyan et poignée dorée |

Il ne s’agit pas d’un simple badge ajouté sur la même image : palette complète, manche, renforts, sertissage et motif héraldique évoluent à chaque rang. Pour un rang donné, ces codes graphiques sont déclinés de manière cohérente sur la pioche, la houe, la hache et la canne à pêche.

La promotion met immédiatement à jour les exemplaires présents dans l’inventaire et la main secondaire. À la connexion ou au changement d’emplacement, un ancien multi-tool est aussi resynchronisé depuis le rang persisté ; le joueur ne peut donc pas conserver artificiellement une apparence de rang supérieur. La pioche en or reçoit une règle serveur `correctForDrops` pour continuer à récolter les minerais accessibles à Baron et Vicomte malgré les limitations du tier or vanilla.

Un `Shift + clic droit` avec le multi-tool ouvre son panneau depuis n'importe quel monde. Les quatre icônes permettent de changer la vue sans modifier l'objet tenu. Leur lore affiche le niveau de l'outil et chaque amélioration en chiffres romains.

L'auto-sell est une amélioration **globale** : un niveau acheté depuis la pioche est immédiatement disponible pour les trois autres outils. Les autres capacités sont sauvegardées séparément pour chaque type d'outil :

- **Efficacité** : augmente la vitesse de casse ; pour la canne, réduit le temps d'attente de pêche ;
- **Boost de niveaux** : multiplie l'XP gagnée par cet outil ;
- **Boost d'argent** : multiplie les revenus auto-sell produits avec cet outil ;
- **Boost de coins spéciaux** : multiplie uniquement les coins gagnés avec cet outil.

Chaque forme possède une monnaie indépendante :

| Outil | Monnaie |
|---|---|
| Pioche | MineCoins |
| Houe | FarmCoins |
| Hache | WoodCoins |
| Canne à pêche | FishCoins |

Une action valide rapporte simultanément de l'XP et la monnaie de l'outil utilisé. Les MineCoins ne peuvent ni être transférés vers la houe, ni devenir des FarmCoins. Les quatre capacités commencent gratuitement au niveau 1 et restent propres à chaque outil.

Cliquer une capacité ouvre un second panneau : le joueur choisit explicitement entre payer avec l'argent normal ou avec les coins spéciaux de cet outil. Les deux coûts sont configurables indépendamment pour chaque niveau. L'auto-sell reste global et continue d'utiliser l'argent normal.

Les profils, capacités, XP et coins sont persistés dans SQLite. Les gains d'XP et de coins sont regroupés pour éviter une requête SQL par bloc, tandis que les achats sont atomiques et journalisés. La progression des niveaux d’outils utilise une hausse régulière puis des paliers de difficulté supplémentaires aux niveaux 21, 41, 61 et 81. La croissance et les multiplicateurs de chaque palier sont configurables dans `tools.yml` afin de garder les premiers niveaux accessibles tout en ralentissant fortement les niveaux avancés.

Chaque action valide fait également progresser un métier permanent séparé : **Mineur** avec la pioche, **Bûcheron** avec la hache, **Farmer** avec la houe et **Pêcheur** avec la canne. Ces métiers ont leur propre niveau et leur propre XP, suivent une courbe exponentielle de plus en plus difficile configurée dans `professions.yml`, et ne sont jamais réinitialisés lors d’une promotion de rang. Leur progression apparaît dans le panneau du multi-tool.

### Compactage des ressources

Les ressources de mine, de champs et de forêt configurées dans `compaction.yml` possèdent trois niveaux récursifs. Les cultures et les bûches conservent la recette directe : compact I = 9 ressources, compact II = 81 et compact III = 729. Les minerais passent obligatoirement par leur bloc vanilla : neuf blocs de fer donnent un bloc de fer compact I. Un compact minéral I représente donc 81 ressources, le niveau II 729 et le niveau III 6 561. Charbon, lingots de cuivre/fer/or, redstone, lapis, diamant et émeraude suivent cette règle ; les poissons restent exclus. Pour garantir le niveau PDC exact, les recettes se réalisent une unité à la fois et refusent le Shift-clic.

Les objets minéraux compacts utilisent visuellement leur bloc vanilla, tandis que cultures et bûches gardent leur matériau. Tous sont authentifiés avec des métadonnées PDC ValoriaTycoon : un objet simplement renommé à l’enclume ne peut pas satisfaire un prérequis. Les recettes refusent également de mélanger plusieurs niveaux. Une ressource compacte ne peut pas être placée, consommée ou détournée dans une recette vanilla. Le **Maître Décompacteur** transforme un compact minéral I en neuf blocs vanilla ; les niveaux supérieurs redonnent neuf compacts du niveau inférieur.

Les longues courbes sont volontairement lentes : boost d'argent et boost de coins possèdent chacun **1000 niveaux** et atteignent seulement environ x1.5 au maximum. Le boost de niveaux possède 250 niveaux. L'efficacité est volontairement limitée à **25 niveaux**, de +2 % à environ +11,6 %, avec un plafond serveur absolu de +12 % afin d'empêcher l'insta-mine et les niveaux sans différence visible.

Capacités réellement implémentées :

| Outil | Capacités spécifiques |
|---|---|
| Pioche | Mineur de zone 3×3 max, Fortune minière, Fonte automatique, Détecteur de gemmes, Filon de MineCoins |
| Houe | Récolte 3×3 puis 5×5 max, Récolte abondante, Replantation, Mutation de récolte, OVNI moissonneur, Cache de FarmCoins |
| Hache | Timber borné à 4 blocs, Bois abondant, Maître des pommes, Cache de WoodCoins |
| Canne | Double prise, Trésor des mers, Prise rare, Cache de FishCoins |

Les quatre outils possèdent aussi efficacité, boost de niveaux, boost d'argent et boost de coins. Aucune capacité de durabilité n'est nécessaire : les outils moddés sont toujours incassables. Pioche, houe et hache disposent en plus d'une **Ruée temporaire** très coûteuse : une petite chance par action d'obtenir un court Speed II, avec cooldown serveur. La canne n'a volontairement pas cette capacité.

Les casses multiples sont strictement bornées : 9 blocs maximum pour la pioche, 25 plants pour la houe et 4 blocs pour Timber. Aucun scan massif de chunks n'est utilisé. Timber commence gratuitement au niveau I afin de permettre la coupe normale d’un tronc, puis possède trois améliorations coûteuses pour atteindre 2, 3 et enfin 4 troncs.

**Maître des pommes** possède trois raretés successives : pomme classique, pomme dorée puis pomme de Notch. Une pomme dorée représente par défaut 10 % des réussites, et une pomme de Notch seulement 1 % ; ces taux sont configurables.

**Mutation de récolte** remplace le simple chasseur de graines : elle donne 1 à 3 ressources adaptées au plant, avec une très faible chance de culture dorée. L’**OVNI moissonneur**, exclusif à la houe, affiche temporairement un OVNI de particules autour du joueur puis récolte les cultures mûres en 5×5. Toute la courbe, les coûts, les outils applicables, les icônes et les probabilités sont configurables dans `tools.yml`.

### Spawn médiéval généré

ValoriaTycoon crée le monde protégé `valoria_spawn` : une île flottante de 240 blocs de diamètre avec grande place pavée, fontaine centrale, château fortifié, deux grandes maisons de guilde à colombages, six étals de marché, éclairage, arbres et quatre arches thématiques. Les arches ouvrent directement la mine, les champs, la forêt ou la pêche. `/spawn` téléporte vers le centre et les nouveaux joueurs y apparaissent lors de leur première connexion. Aucun chunk périphérique n’est préchargé artificiellement.

Le spawn, son point d’apparition, sa bordure, les matériaux des arches et leurs destinations sont configurables dans `spawn.yml`. Les constructions, conteneurs, fluides, explosions, feu, pistons, dégâts et vide sont protégés. Le Maître Décompacteur est placé dans ce monde via `compaction.yml`.

### Warps extensibles

`/warp` ouvre un panneau configurable. `/warp tuto`, `/warp tutoriel`, `/warp guide` ou `/warp aide` rejoint l’Académie Valoria dans la cour du château. `/warp caisse`, `/warp caisses`, `/warp crate` ou `/warp crates` téléporte directement vers le marché médiéval réservé aux caisses. Les destinations sont définies relativement au point `/spawn`, ce qui conserve leur position logique si la hauteur ou le point d’arrivée du hub change.

`warps.yml` contient le monde, le mode relatif/absolu, les coordonnées, l’orientation, les alias, le slot, l’icône, le modèle et le lore. Ajouter un futur warp dans ce fichier l’ajoute automatiquement au menu, à la commande directe et à l’auto-complétion. Les téléportations utilisent Paper `teleportAsync`, quittent proprement les véhicules et possèdent un cooldown individuel configurable sans SQL. Les anciens fichiers qui ne contiennent que le warp Caisses reçoivent aussi le warp Tutoriel en mémoire sans écraser leur configuration.

### Académie et guide permanent

La cour intérieure du château contient douze panneaux `TextDisplay` non persistants : démarrage, Skyblock, farms, multi-tool et coins, économie et compactage, générateurs, quêtes et métiers, rangs, commandes, caisses et pets. Tous les textes et toutes les positions sont configurables dans `tutorial-hub.yml`. Le service vérifie seulement les chunks naturellement chargés et ne provoque aucun chargement artificiel.

Cette académie est strictement informative. La rejoindre avec `/warp tuto` ne modifie aucune progression, ne relance jamais le tutoriel du premier rang et ne distribue aucune récompense.

Concepts visuels disponibles dans `docs/concepts/` :

- `medieval-spawn.png` ;
- `massive-vanilla-caverns-v18.png` : référence actuelle d’une caverne organique géante et de son portail répulsif ;
- `massive-ranked-forests-v19.png` : échelle, relief et ambiance des immenses zones forestières ;
- `large-fields-forest-zones.png` : ancienne référence agricole ;
- les anciennes images de mines restent archivées, mais ne représentent plus le générateur organique de la version 0.18.

### Tutoriel du premier rang

Le tutoriel est activé automatiquement uniquement tant que le joueur est **Sans rang**. Il commence après la création du Skyblock et affiche une mini-quête à la fois dans la barre d’action : 48 charbons, 55 blés, 20 bûches de chêne, 4 Morues crues, niveau XP vanilla 20, puis une validation de quête commune. Chaque étape rapporte 4 000 $, soit 24 000 $ de tutoriel. Avec les 1 000 $ de départ et les 5 000 $ de la quête commune, le joueur atteint environ 30 000 $, paie Citoyen à 25 000 $ et conserve une petite réserve.

La progression est persistée dans SQLite, les actions sont regroupées avant écriture et chaque récompense est créditée atomiquement avec audit économique. Une récompense ne peut pas être obtenue deux fois. Après les six étapes, la barre d’action indique `/rank`. Dès que Citoyen est obtenu, le tutoriel est marqué comme terminé de façon permanente et ne guide plus jamais le joueur. Une reconnexion, un redémarrage ou un reset du Skyblock ne recrée pas le tutoriel et ne permet pas de récupérer les récompenses. Les objectifs, textes, intervalles et récompenses sont configurables dans `tutorial.yml`.

### Menu principal Skyblock

`/is` et `/is menu` ouvrent un panneau principal configurable dans `menus.yml`. Il donne accès aux systèmes fonctionnels : argent, statistiques, farms, générateurs, améliorations, multi-tool, auto-sell, classements, pets, quêtes et rangs médiévaux.

### Pack de ressources Valoria complet

Le dossier `resource-pack/` contient le pack officiel Minecraft 26.2 de ValoriaTycoon. Il fournit **259 modèles et 259 textures premium 32×32** sans remplacer globalement les objets vanilla : chaque objet serveur est activé uniquement par `minecraft:item_model`. Les inventaires partagent désormais une façade fantasy complète — colonnes violettes, or, bandeau rouge, slots bordeaux et inventaire joueur indigo — au lieu du coffre gris vanilla.

Le pack couvre le menu principal `/is`, les farms et leurs zones, l’auto-sell, les améliorations de parcelle, les dix rangs, les quêtes, les huit pets, la boutique et le contrôle des générateurs, toutes les capacités du multi-tool et les écrans de paiement. Les objets physiques possèdent également leur apparence propre : clés, œufs normal/chromatique, quatre générateurs, sacs d’argent/coins, fioles XP, paquets de ressources, bons de récompense, seize ressources compactées sur trois niveaux et les quarante-quatre combinaisons rang/forme du multi-tool.

Les assets sont reproductibles avec `python3 scripts/generate-resource-pack.py`. `resource-pack.yml` contrôle le namespace et permet de désactiver tous les modèles personnalisés pour revenir aux matériaux vanilla sans recompilation. Le ZIP client est assemblé automatiquement pendant `mvn package` ou `mvn verify`.

### Caisses et clés physiques

`/warp caisse` mène à neuf caisses physiques modélisées dans le marché : Vote, Quêtes, Farm, Commune, Rare, Épique, Légendaire, **Valoria** et Pets. L’ancienne commande `/crates` et son panneau ont été supprimés : toutes les interactions passent désormais par les modèles 3D du monde. Les migrations SQLite v24-v26 créent les registres `issued_crate_keys` et `issued_crate_rewards` : chaque clé et chaque récompense générique possède un UUID physique et une ligne serveur. Elles sont échangeables, récupérées après crash et consommables une seule fois même si un objet est copié.

Sources déjà raccordées :

- **Vote** : une clé par événement NuVotifier/Votifier unique, avec pont optionnel sans dépendance dure ;
- **Quêtes** : une clé par complétion persistée, avec réconciliation automatique après crash ou reconnexion ;
- **Farm** : chance fournie par la capacité commune `Chercheur de clés Farm`, uniquement lors d’actions valides dans les mondes publics de farm ;
- **Commune/Rare/Épique/Légendaire** : tirage pondéré fourni par `Chasseur de clés rares`, disponible sur les quatre formes du multi-tool ;
- **Pets** : système d’œufs et de clés spécialisé déjà existant, conservé séparément.

La chaîne finale est validée dans `crates.yml` et `crate-rewards.yml` : Commune peut donner Rare ou Épique mais jamais directement Légendaire ; Rare peut donner Épique ou Légendaire ; Épique peut donner Légendaire. La Caisse Légendaire contient exactement **0,5 %** de chance d’émettre une **Clé Valoria**. Aucune caisse inférieure, aucun vote, aucune quête et aucune capacité de farm ne peut donner directement cette clé signature. Les huit ouvertures sont actives avec exactement 10 000 points de poids par table.

La **Caisse Valoria** remplace le nom provisoire « Mythique » : ce n’est pas un rang supplémentaire dans la chaîne F2W, mais la caisse emblématique du serveur, principalement obtenue en boutique. L’API `issueStorePurchase` lie chaque lot à l’identifiant de transaction du webstore ; rejouer la même transaction recrée les mêmes UUID sans dupliquer la livraison. Une acquisition gratuite reste néanmoins possible via le jackpot de la Caisse Légendaire. Sa table finale privilégie les sacs IV–V, coins élevés, fioles, générateurs et lots de clés ; ses jackpots restent limités à 0,5–1 %.

Chaque caisse du marché utilise une géométrie 3D et une texture propres, un `ItemDisplay`, une hitbox `Interaction`, un label `TextDisplay`, une animation et des particules configurables. Ces entités sont non persistantes, protégées, recréées uniquement lorsque leur chunk est naturellement chargé et supprimées proprement à l’arrêt. `crate-stations.yml` configure la grille et les effets. Un clic sur Pets ouvre son système spécialisé ; les huit autres stations consomment atomiquement la clé correspondante et livrent un jeton physique immuable.

Une ouverture donne exactement une récompense parmi les sacs d’argent I–V, sacs de coins I–V, sac universel, fioles XP I–IV, paquets de ressources I–III adaptés aux zones déjà débloquées, hoppers, Shulkers, Beacons, générateurs et clés. Le tirage exact — montant, monnaie, matériau et type de générateur inclus — est persisté avec la consommation de la clé. Un crash ne provoque donc aucun nouveau tirage. Le clic droit sur le jeton applique atomiquement argent, coins ou nouvelles clés ; les autres bons sont consommés une seule fois avant leur livraison physique.

Commandes de test : `/isadmin cratekey <joueur> <type> <quantité>` et `/isadmin petkey` pour la caisse spécialisée.

### Pets évolutifs

`/pets` ouvre directement la collection complète du joueur, également accessible depuis le panneau Pets de `/is`. Cette interface permet d’activer un seul compagnon et d’ouvrir la **Caisse Pets**. La caisse consomme une clé physique puis remet un **œuf physique échangeable** correspondant au pet tiré. Le tirage de rareté est pondéré ; la variante de l’œuf est déterminée une seule fois : environ 99 % normale et 1 % chromatique par défaut. Le chromatique est purement visuel et ne donne aucun bonus supplémentaire. Le joueur reçoit le résultat et toute obtention chromatique est annoncée globalement.

Tous les types de pets utilisent désormais le même œuf moddé `EGG`, avec un modèle ValoriaTycoon normal ou chromatique indépendant des apparences vanilla. Ces deux modèles font partie du pack global livré dans `resource-pack/` et généré automatiquement en ZIP par Maven. Le type du pet n’est volontairement pas reconnaissable à la silhouette : le survol de l’œuf affiche son nom, sa rareté, sa description, sa variante déjà fixée, le taux chromatique initial, son niveau, son XP et tous ses bonus actuels avec leur maximum.

Chaque clé et chaque œuf possèdent leur propre UUID PDC. La consommation de la clé et l’émission de l’œuf sont inscrites dans la même transaction SQLite. L’œuf possède également une ligne serveur qui fixe définitivement son type, sa variante, son niveau et son XP : modifier le NBT visible, copier l’objet ou remettre un pet en œuf ne permet jamais de relancer le 1 %. Le modèle client est lui aussi contrôlé lors de la lecture de l’objet ; les anciens spawn eggs authentifiés émis avant la v0.26 restent acceptés uniquement pour assurer la migration. Un crash après émission est compensé par une file de livraison persistante ; une copie déjà consommée devient simplement invalide. Les clés et œufs restent librement échangeables ou revendables.

Le clic droit sur l’œuf vérifie sa ligne serveur, le rang requis et l’unicité de possession avant d’ajouter et d’équiper le pet. Le **Gardien des Œufs**, placé au spawn, ouvre une interface permettant de retirer un pet de sa collection contre de l’argent et des niveaux XP vanilla. L’œuf recréé conserve exactement la variante normale/chromatique, le niveau et l’XP du pet. Les Clés Pets apparaissent dans les tables génériques configurées et restent également distribuables avec `/isadmin petkey` pour l’administration et les tests.

Les pets gagnent de l’XP grâce aux actions du multi-tool et aux cycles de générateurs. Chaque espèce possède des bonus configurables : revenus, XP des outils, XP des métiers, coins spéciaux et production des générateurs. Les raretés épiques et supérieures peuvent aussi déclencher une double récompense d’outil ou une double production. Une contrainte SQLite garantit qu’un seul pet reste actif.

Le pet actif apparaît comme une créature invulnérable, silencieuse, sans IA, gravité ni collision, qui suit le joueur sans charger artificiellement de chunk. Les visuels ne sont jamais persistés comme entités serveur : ils sont recréés depuis le profil lors d’une connexion et supprimés proprement au départ ou à l’arrêt.

Pets par défaut : Lapin fermier, Renard prospecteur, Loup des cavernes, Abeille dorée, Allay collectionneur, Golem industriel, Dragon miniature et Phénix de Valoria. Raretés, poids de caisse, chance chromatique, rangs d’activation, œufs, coût de remise en œuf, courbes d’XP, apparences et bonus sont configurés dans `pets.yml`.

### Classements asynchrones

`/top` — également accessible avec `/classement` ou `/classements` — ouvre cinq catégories : fortunes, niveau du Skyblock, rang médiéval, production totale et temps de jeu. Chaque catégorie possède un aperçu du podium puis un panneau Top 10 avec la position du joueur lorsqu’elle se trouve dans le cache.

Les cinq requêtes sont exécutées ensemble exclusivement sur le worker SQLite. Le résultat complet devient un `LeaderboardSnapshot` immuable publié atomiquement ; les menus ne lisent ensuite que ce cache et ne lancent jamais de SQL sur le thread principal. Les refreshs qui se chevauchent sont regroupés, les requêtes sont bornées et les Skyblocks non actifs sont exclus des classements d’île. L’argent reste classé pour tous les comptes connus.

`leaderboards.yml` configure l’intervalle de rafraîchissement, la profondeur du cache, le nombre affiché et les emplacements du menu. Par défaut, cent positions sont conservées en mémoire, dix sont affichées et le cache est recalculé toutes les soixante secondes.

Cinq hologrammes `TextDisplay` vanilla affichent maintenant les Tops directement autour du spawn médiéval. Ils lisent uniquement le même snapshot asynchrone que `/top`, ne lancent aucun SQL, ne chargent jamais de chunk et ne nécessitent ni Citizens ni plugin d’hologrammes. Les entités sont non persistantes, marquées en PDC, nettoyées au redémarrage et recréées seulement si leur chunk naturel est chargé. `leaderboard-holograms.yml` contrôle leurs offsets relatifs à `/spawn`, le nombre de lignes, la portée, la largeur et le fond.

### Quêtes et rangs médiévaux

`/is quests` affiche les quêtes répétables communes, rares, épiques et légendaires. Elles progressent réellement avec les actions de la pioche, de la houe, de la hache et de la canne. Les validations non consommées servent aux promotions.

`/rank` affiche le prochain des dix rangs : **Citoyen, Artisan, Marchand, Écuyer, Chevalier, Baron, Vicomte, Comte, Marquis et Duc**. Chaque promotion contrôle l’argent, les objets, les validations de quêtes, les niveaux d’outils, les quatre métiers permanents, le niveau XP vanilla et le temps de jeu total du Skyblock. Le prix, les objets, les validations et le nombre exact de niveaux XP vanilla demandés sont consommés ; les niveaux/XP d’outils repartent au niveau I. L’argent restant, les métiers et le temps cumulé ne sont jamais réinitialisés, comme le Skyblock, les générateurs, les capacités et les coins spéciaux.

Chaque rang active immédiatement des avantages permanents cumulatifs, inspirés des progressions F2W de serveurs Tycoon établis : bonus de revenus, XP des outils, XP des métiers, coins des outils, production des générateurs et emplacements de générateurs supplémentaires. Les valeurs restent volontairement plus modérées que les bonus très élevés observés ailleurs : Duc atteint +20 % de revenus, +50 % d’XP outils/métiers, +40 % de coins, +25 % de production et +15 emplacements. Tous ces avantages sont configurables dans `ranks.yml`, calculés depuis le rang persisté et affichés dans `/rank`.

La courbe vise environ **deux semaines de temps de jeu réel pour Duc** : 30 min pour Citoyen, 180 pour Artisan, 360 pour Marchand, 600 pour Écuyer, 1 200 pour Chevalier, 2 400 pour Baron, 4 800 pour Vicomte, 8 400 pour Comte, 13 200 pour Marquis et **20 160 minutes pour Duc**. Citoyen sert d’objectif d’onboarding rapide : 25 000 $, 20 niveaux XP vanilla, une quête commune, quelques niveaux d’outils et seulement 16 charbons, 16 blés, 8 bûches et 4 Morues crues. Artisan et Marchand restent faisables en quelques heures mais introduisent une difficulté plus nette. À partir d’Écuyer, la progression augmente fortement jusqu’à 2 milliards de dollars, **1 000 niveaux XP vanilla**, les quatre outils au niveau 100 et les quatre métiers au niveau 100 pour Duc.

Les objets de promotion correspondent toujours aux ressources accessibles **avant** d’obtenir le rang visé. La progression utilise quatre paliers effectifs : zone 1 charbon/blé/chêne, zone 2 fer-cuivre/carotte/bouleau, zone 3 or-redstone-lapis/pomme de terre/sapin et zone 4 diamant-émeraude/betterave/chêne noir. Les déblocages sont respectivement le départ, Artisan, Chevalier et Comte. La pêche reste commune et ses poissons sont demandés sous forme normale. À partir d’Écuyer, les minerais, cultures et bûches sont exigés sous forme compacte. Duc demande notamment deux blocs de diamant compact III et deux blocs d’émeraude compact III — soit 13 122 ressources de chaque minerai — ainsi que 2 916 betteraves, 2 187 bûches de chêne noir et 96 poissons tropicaux.

### PlaceholderAPI

Lorsque PlaceholderAPI est présent, ValoriaTycoon enregistre automatiquement une expansion non bloquante :

```text
%valoriatycoon_money%
%valoriatycoon_level%
%valoriatycoon_rank%
%valoriatycoon_rank_name%
%valoriatycoon_production%
%valoriatycoon_pets%
%valoriatycoon_pet_keys%
%valoriatycoon_pet_active%
%valoriatycoon_pet_level%
%valoriatycoon_pet_variant%
%valoriatycoon_pet_rarity%
%valoriatycoon_playtime%
%valoriatycoon_island_size%
%valoriatycoon_hopper_limit%
%valoriatycoon_machines%
```

Les placeholders lisent uniquement les caches en ligne et ne lancent jamais de requête SQL sur le thread principal. L’ancien identifiant `%tycoon_*%` reste enregistré comme alias de transition, mais toute nouvelle intégration doit utiliser `%valoriatycoon_*%`.

### Générateurs plaçables

`/shop` ouvre la boutique contenant actuellement les générateurs. Cette racine pourra accueillir d’autres catégories plus tard. Les générateurs peuvent être achetés avec l’argent normal ou avec les coins de l’outil associé. Ils sont également disponibles dans les caisses via des bons de générateurs uniques. Ils sont représentés uniquement par des blocs authentifiés avec PDC, sans hologramme ni entité persistante.

Premiers générateurs fonctionnels :

- générateur minier : produit du fer brut, avec MineCoins comme monnaie alternative ;
- générateur agricole : produit du blé, avec FarmCoins ;
- générateur forestier : produit des bûches, avec WoodCoins ;
- générateur de pêche : produit du saumon, avec FishCoins.

Il n’existe plus aucun système d’énergie. Chaque bloc produit directement sa ressource selon son intervalle.

Un clic droit ouvre l’interface propre à la machine. Son propriétaire choisit indépendamment entre :

- **stockage** : les ressources restent dans un inventaire virtuel comparable à un coffre, puis peuvent être collectées ;
- **vente automatique** : chaque cycle crédite directement le solde économique du propriétaire.

Chaque générateur possède deux améliorations payées uniquement avec l’argent normal :

- **vitesse** : jusqu’à 10 niveaux, avec un intervalle qui peut être réduit au maximum de moitié ;
- **prix de vente** : jusqu’à 10 niveaux, avec +5 % par niveau au-dessus du niveau I.

La production fonctionne uniquement lorsque le chunk du générateur est chargé. Aucun chunk n’est chargé artificiellement. Les cycles, le stockage, les ventes et les améliorations sont validés dans des transactions SQLite atomiques. Une file de priorité traite uniquement les générateurs arrivés à échéance et applique une limite de cycles par tick.

### Tycoons privés et parcelles

ValoriaTycoon génère un monde **Skyblock entièrement vide** et attribue atomiquement une île flottante libre en grille. Tous les joueurs reçoivent exactement le même modèle au moment de la création. Ensuite, le propriétaire et ses membres peuvent modifier librement le terrain et construire comme ils le souhaitent dans la limite débloquée.

```text
/is create
/is go
/is settings
/is visit <propriétaire>
/is stats
/is upgrades
/is reset
/is reset confirm
```

Les îles possèdent un propriétaire, un niveau, une progression, un rang médiéval, une production totale, un temps de jeu et une liste de membres. Les limites, origines, rayons, profondeurs, matériaux et hauteurs de construction sont configurables dans `tycoons.yml`.

Une bordure virtuelle individuelle affiche et impose la taille actuelle du Skyblock. Elle suit automatiquement l’upgrade de taille et évite les constructions hors limite sans poser de blocs de barrière dans le monde.

Les améliorations sont **uniques à chaque parcelle Skyblock** et sont accessibles avec `/is upgrades` :

- taille constructible : 28×28 → 29×29 → 30×30 → 31×31 → 32×32 ;
- limite de hoppers : 8 → 16 → 24 → 32 → 48 → 64 ;
- limite de membres : 3 → 4 → 5 → 6 → 8 → 10.

Acheter une limite de hoppers sur une parcelle ne modifie aucune autre parcelle. Les achats utilisent le solde économique partagé du propriétaire, mais les niveaux restent stockés dans le Tycoon concerné. Les hoppers sont suivis par coordonnées en SQLite, avec suppression en cascade lors du reset. Les courbes, prix, icônes et slots sont configurables dans `upgrades.yml`.

Le fly est actuellement accordé gratuitement au propriétaire et, si configuré, aux membres de confiance. Il est automatiquement retiré en quittant l’île sans supprimer un fly fourni par un autre plugin. Une interface `TycoonFlightAccessPolicy` prépare le remplacement futur par une permission, un rang, un achat ou une amélioration. Une protection anti-void ramène le joueur sur son île ou au spawn sûr.

Gestion des membres :

```text
/is invite <joueur>
/is accept <propriétaire>
/is kick <joueur>
/is members
```

Seuls le propriétaire et les membres acceptés peuvent casser, placer, utiliser les blocs, ouvrir les conteneurs ou interagir avec les entités. Les espaces entre parcelles sont protégés. Les pistons, explosions et fluides ne peuvent pas traverser une frontière.

La création et la suppression utilisent des états durables `PREPARING`, `ACTIVE` et `DELETING`. Le nettoyage est découpé sur plusieurs ticks, charge les chunks avec Paper avant le travail et reprend automatiquement après un crash. `/is reset` exige une double confirmation temporisée. Le temps de jeu est sauvegardé par lots une fois par minute.

> Migration d’identité v20 : arrêter le serveur, effectuer une sauvegarde complète, retirer l’ancien JAR `TycoonCore` puis installer uniquement `ValoriaTycoon`. Au premier démarrage, le plugin déplace automatiquement `plugins/TycoonCore/` vers `plugins/ValoriaTycoon/`, renomme `tycooncore.db` en `valoriatycoon.db`, met à jour les configurations et renomme les anciens dossiers de mondes vers `valoria_*`. La migration SQLite v20 met également à jour les mondes persistés des parcelles, hoppers, machines et régénérations. Deux dossiers anciens/nouveaux non vides ne sont jamais fusionnés automatiquement afin d’éviter tout écrasement.
>
> Les objets PDC de l’ancienne identité restent reconnus pendant la transition. Les nouvelles recettes, machines et métadonnées sont créées sous le namespace `valoriatycoon`. Les permissions historiques restent volontairement sous `tycoon.*` pour ne pas casser les groupes LuckPerms existants.

Commandes administratives ajoutées :

```text
/isadmin create <joueur>
/isadmin petkey <joueur> <quantité>
/isadmin cratekey <joueur> <vote|quest|farm|common|rare|epic|legendary|valoria> <quantité>
/isadmin reset <joueur>
/isadmin delete <joueur>
```

## Compatibilité Minecraft 26.2

ValoriaTycoon 0.36 cible explicitement `paper-api:26.2.build.112-stable`, utilise `api-version: '26.2'` et est compilé en bytecode Java 25. Les anciens alias de gamerules ont été remplacés par l’API `GameRules` de Paper 26.2. PlaceholderAPI 2.12.3 et SQLite JDBC 3.53.2.0 sont utilisés pour cette cible. Cette version du plugin ne doit pas être chargée sur Paper 1.21.x.

Avant de migrer un serveur existant, sauvegarder les mondes, les dossiers `plugins/TycoonCore/` et `plugins/ValoriaTycoon/` s’ils existent, ainsi que la base SQLite, puis effectuer le premier démarrage sur une copie de test.

## Prérequis

- JDK 25 ;
- Maven 3.9+ ;
- serveur Paper 26.2.

## Compiler

```bash
mvn clean verify
```

Le plugin ombré et le pack de ressources client sont générés dans :

```text
target/ValoriaTycoon-0.38.0-SNAPSHOT.jar
target/ValoriaTycoon-0.38.0-SNAPSHOT-resource-pack.zip
```

Pour afficher l’identité visuelle complète, héberger le ZIP du pack sur une URL HTTPS publique puis renseigner cette URL et son SHA-1 dans la configuration du pack de ressources du serveur. Le ZIP contient directement `pack.mcmeta`, `pack.png` et `assets/` à sa racine. Sur un serveur public, le pack doit être proposé ou rendu obligatoire. Pour conserver volontairement les apparences vanilla sans pack client, définir `item-models.enabled: false` dans `resource-pack.yml` puis redémarrer.

## Tester sur Paper

1. Compiler et copier le JAR dans `plugins/`.
2. Démarrer Paper 26.2 avec Java 25.
3. Vérifier que les quatre dossiers `valoria_farm_*` sont créés et que la console annonce quatre mondes publics.
4. Tester `/farm` : mine, champs et forêt doivent téléporter sur leur île centrale ; la pêche doit téléporter directement sur sa grande plateforme.
5. Avec un joueur Sans rang, entrer dans le premier portail et confirmer que le déplacement est annulé puis que le joueur est propulsé vers la première caverne ; vérifier aussi le blocage des téléportations, bateaux/wagonnets et contournements par le vide. Répéter aux rangs Artisan, Chevalier et Comte.
6. Dans la mine, vérifier les quatre cavernes organiques de 2 048×2 048, leur hauteur supérieure à cent blocs, les sols et plafonds irréguliers, les piliers naturels, le dripstone, les rampes, la passerelle éclairée et les trois ponts classés de 1 024 blocs.
7. Dans les champs, vérifier les îles de 5 120 blocs de diamètre, l’absence totale de trous/canaux d’eau, les longues rangées et la régénération serveur. Dans la forêt, vérifier les quatre îles de 2 048×2 048, le relief vallonné, les chemins, les quatre silhouettes d’arbres, la régénération des troncs/feuilles et les ponts classés.
8. Donner de l'argent au joueur avec `/isadmin give <joueur> 1000000`.
9. Tenir le multi-tool et viser successivement un minerai, un tronc, une culture puis de l'eau pour vérifier ses quatre formes.
10. Faire `Shift + clic droit`, survoler les quatre icônes et changer de vue.
11. Acheter le niveau 1 de l'auto-sell, puis ouvrir `/autosell` pour l'activer.
12. Vérifier que ce même niveau auto-sell apparaît avec la houe, la hache et la canne.
13. Améliorer l'efficacité de la pioche et confirmer que le niveau de la houe reste inchangé.
14. Miner avec la pioche et vérifier la progression de son niveau, de son XP et de ses MineCoins.
15. Récolter avec la houe et confirmer que les FarmCoins sont séparés des MineCoins.
16. Ouvrir une capacité, payer un niveau en coins puis un autre en argent normal.
17. Améliorer le boost de coins de la pioche et vérifier que les autres outils restent inchangés.
18. Acheter le boost d'argent et contrôler le multiplicateur dans `/balance`.
19. Redémarrer avant la régénération d'un bloc et confirmer son retour après le redémarrage.
20. Tester avec un joueur sans `tycoon.farm`, puis avec `tycoon.bypass`.
21. Créer deux Skyblocks et vérifier que leurs îles flottent dans un monde vide.
22. Entrer puis sortir de son île pour contrôler l’activation et le retrait du fly.
23. Tomber sous l’île et vérifier le sauvetage anti-void.
24. Modifier librement l’île, puis vérifier que la bordure bloque toute construction hors de la taille débloquée.
25. Acheter une taille avec `/is upgrades` et vérifier que le second Skyblock ne change pas.
26. Atteindre la limite de hoppers, améliorer la capacité, puis placer les hoppers supplémentaires.
27. Améliorer la limite de membres sur une seule parcelle et tester les invitations.
28. Exécuter `/is reset`, laisser expirer une confirmation puis confirmer une seconde tentative.
29. Redémarrer pendant une préparation/suppression et vérifier sa reprise automatique.
30. Placer neuf bûches de chêne en 3×3 et vérifier le compact I direct ; vérifier ensuite que le fer exige neuf blocs de fer et refuse neuf lingots.
31. Refaire chaque recette avec neuf niveaux I puis neuf niveaux II pour obtenir les niveaux II et III.
32. Vérifier que les poissons, les niveaux mélangés et le Shift-clic ne produisent aucun objet compact.
33. Décompacter un bloc de fer compact I et vérifier la restitution de neuf blocs de fer ; tester aussi les niveaux supérieurs.
34. Renommer une ressource normale à l’enclume et confirmer qu’elle ne satisfait pas un prérequis compact de rang.
35. Connecter un nouveau joueur Sans rang et vérifier que la barre d’action demande `/is create`, puis guide les six mini-quêtes dans l’ordre.
36. Vérifier que chaque étape ne crédite qu’une fois 4 000 $ et que le solde final approche 30 000 $ avec la quête commune.
37. Passer Citoyen via `/rank`, puis confirmer que la barre d’action du tutoriel disparaît définitivement.
38. Tester `/spawn`, la première connexion, la protection complète du hub et les quatre arches vers les farms.
39. Connecter plusieurs joueurs dans des zones différentes et confirmer qu’aucun chunk n’est chargé artificiellement hors de leur vue.
40. Installer le ZIP de ressources côté client et vérifier l’icône Valoria dans l’écran des packs.
41. Parcourir `/is`, `/farm`, `/autosell`, `/rank`, `/pets`, `/shop`, les quêtes, les upgrades et le multi-tool : chaque action doit avoir son icône violet/or dédiée sans modifier les objets vanilla ordinaires.
42. Désactiver temporairement `item-models.enabled`, redémarrer et confirmer le retour propre aux matériaux vanilla, puis réactiver le pack.
43. Fabriquer plusieurs ressources compactées et vérifier la couleur de la ressource ainsi que les trois marqueurs de niveau.
44. Passer successivement plusieurs rangs et transformer le multi-tool entre pioche, houe, hache et canne : son matériau doit suivre la table configurée et chacune des quarante-quatre combinaisons rang/forme doit garder sa texture propre et son état incassable.
45. Vérifier qu’un compte ne reçoit qu’un seul objet : le même identifiant doit rester présent après les quatre transformations, tandis qu’une copie locale supplémentaire redevient un outil normal.
46. Essayer de jeter le multi-tool, de le déposer dans un coffre, de le fabriquer ou de le ramasser avec un autre compte : toutes ces opérations doivent être refusées ; vérifier aussi sa conservation à la mort.
47. Tenir un outil vanilla ordinaire et confirmer qu’il ne se transforme pas, ne devient pas incassable et ne déclenche aucune capacité ou récompense du multi-tool.
48. Acheter puis placer les quatre générateurs et confirmer leurs modèles physiques et leurs icônes de contrôle.
49. Lancer `/pets`, vérifier la collection complète et la Caisse Pets, puis générer plusieurs clés avec `/isadmin petkey`.
50. Comparer plusieurs types de pets : tous les œufs normaux doivent partager le même modèle moddé, tandis que la variante chromatique utilise le modèle brillant commun.
51. Survoler chaque œuf et vérifier le pet, la rareté, la variante fixée, le taux initial de 1 %, le niveau, l’XP et tous les bonus ; aucun type ne doit dépendre d’un spawn egg vanilla.
52. Copier une clé et un œuf en environnement de test : chaque UUID ne doit produire respectivement qu’un tirage et qu’un pet.
53. Faire un clic droit sur un œuf, vérifier le rang d’équipement, le message de variante et l’annonce globale lorsqu’un chromatique est obtenu.
54. Faire progresser un pet, utiliser le Gardien des Œufs, payer argent/XP puis confirmer que le nouvel œuf conserve niveau, XP, texture de variante et couleur sans reroll chromatique.
55. Farmer avec le multi-tool et les générateurs pour vérifier les boosts spéciaux, les doubles récompenses et l’absence de duplication après redémarrage.
56. Lancer `/top` immédiatement après le démarrage : le menu doit indiquer le premier calcul, puis afficher les cinq podiums sans bloquer le serveur.
57. Ouvrir chaque catégorie et vérifier le Top 10, la position personnelle, les formats argent/rang/temps et l’exclusion des Skyblocks non actifs.
58. Ouvrir `/top` simultanément avec plusieurs joueurs pendant un rafraîchissement et confirmer qu’aucune requête SQL ne part du thread principal et qu’un snapshot complet remplace atomiquement le précédent.
59. Rejoindre `/spawn` et contrôler les cinq hologrammes : orientation vers le joueur, Top 5, formats cohérents avec `/top` et mise à jour après publication du snapshot suivant.
60. Redémarrer puis décharger/recharger naturellement les chunks du spawn : aucun doublon ne doit subsister et aucun chunk ne doit être chargé artificiellement par les hologrammes.
61. Utiliser `/warp caisse`, contrôler les neuf modèles 3D, animations et particules, puis ouvrir chaque type avec sa clé ; Pets doit garder son interface spécialisée.
62. Simuler deux fois le même vote Votifier : une seule clé Vote doit être inscrite ; un second vote réellement distinct doit donner une seconde clé.
63. Terminer plusieurs quêtes, redémarrer entre la complétion et la livraison puis vérifier une clé Quêtes par complétion, sans perte ni double crédit.
64. Acheter les deux capacités communes, effectuer des actions avec les quatre formes et contrôler les clés Farm et les tirages Commune/Rare/Épique/Légendaire pondérés.
65. Échantillonner les huit tables, vérifier les montants et tiers, l’adaptation des ressources au rang et le jackpot Valoria de la Légendaire.
66. Copier une clé puis un jeton de récompense en environnement de test : chaque UUID serveur doit être consommé exactement une fois et toutes ses copies locales supprimées.
67. Redémarrer après la transaction d’ouverture mais avant la livraison : le même jeton et le même payload doivent revenir, sans nouveau tirage.
68. Ouvrir avec un inventaire plein, tester argent, quatre coins, XP, ressources, items, clés Pets, clés génériques et générateurs, sans perte ni duplication visible.
69. Exécuter `/warp` et contrôler les icônes Académie et Caisses.
70. Tester `/warp tuto`, `/warp tutoriel`, `/warp guide` et `/warp aide` puis parcourir les douze panneaux sans reprise du tutoriel après Citoyen.
71. Tester `/warp caisse`, `/warp caisses` et `/warp crates` : les trois doivent mener au même marché avec téléportation Paper asynchrone.
72. Déclencher rapidement plusieurs warps, vérifier le cooldown, puis ajouter une définition temporaire dans `warps.yml` pour contrôler son menu, ses alias et son auto-complétion après redémarrage.

Les mondes ne sont jamais supprimés automatiquement. Pour régénérer entièrement un monde après un changement de seed ou de générateur, arrêter le serveur, sauvegarder puis supprimer manuellement le dossier concerné. Les changements de génération dans `farms.yml` nécessitent un redémarrage.

## Tests automatisés

Audit statique sans dépendance externe :

```bash
python3 scripts/verify-release.py
```

Le scénario de validation serveur est détaillé dans `docs/PAPER_TEST_GUIDE.md`.

Compilation et tests complets obligatoires avant publication :

```bash
python3 scripts/verify-release.py --full
# équivalent build : mvn clean verify
```

Les tests couvrent les transactions, migrations, récompenses atomiques du tutoriel, monnaies par outil, capacités, compactage récursif, métiers permanents, pets et raretés, prérequis et avantages permanents de rang, Skyblocks, upgrades, hoppers, classements SQLite ordonnés et cycles atomiques des générateurs avec stockage, auto-sell, vitesse, production et prix de vente. La checklist commerciale et les blocages restants sont détaillés dans `docs/RELEASE_CHECKLIST.md`.

## Prochaine étape

Configuration des objets et probabilités propres à chaque nouvelle caisse, puis activation transactionnelle de leur ouverture. MySQL/MariaDB et Vault resteront après cette étape de contenu.
