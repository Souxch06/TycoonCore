# Pack de ressources complet ValoriaTycoon 26.2

Ce pack donne une identité fantasy royale violet/or à tous les systèmes de
ValoriaTycoon. Les objets utilisent le composant moderne `minecraft:item_model`
de Minecraft 26.2, tandis que les inventaires emploient un cadre complet
Valoria compatible avec les conteneurs vanilla de 1 à 6 rangées.

## Contenu

- interface intégrale violet impérial, or, colonnes, bandeau rouge et slots
  différenciés entre contenu serveur et inventaire joueur ;
- logo VALORIA flottant, tours et ruban héraldique via une police GUI privée ;
- interfaces dédiées : inventaire, établi, fourneau, haut-fourneau, fumoir,
  hopper, Shulker et conteneurs serveur de 1 à 6 rangées ;
- 156 textures 32×32 du monde Tycoon : roches, sols, minerais et blocs minéraux,
  briques médiévales, bois, feuillages, cultures et leurs stades, plantes,
  redstone, établis, fours, pistons, lanternes, rails et blocs de machines ;
- 42 textures d’objets vanilla utiles : ressources, monnaies, nourritures,
  poissons, composants, navigation et récompenses ;
- neuf reliquaires 3D de 12 à 22 éléments et neuf sceaux magiques orbitaux ;
- menu principal `/is` : argent, statistiques, farms, générateurs, upgrades,
  multi-tool, auto-sell, paramètres, pets, quêtes et rangs ;
- menus secondaires : zones de farm, états verrouillés, achats, générateurs,
  améliorations du Skyblock, capacités du multi-tool, quêtes et résumés ;
- classements `/top` : cinq catégories, podium or/argent/bronze, position
  personnelle, cache et navigation ;
- menu `/warp`, icônes Académie/Caisses et neuf modèles 3D uniques de marché ;
- collection des huit pets et Caisse Pets ;
- dix rangs médiévaux et rang maximum ;
- objets physiques : clés Vote, Quêtes, Farm, Commune, Rare, Épique,
  Légendaire, Valoria et Pets, œufs normal/chromatique, quatre générateurs,
  sacs d’argent/coins, fioles XP, paquets de ressources, quatre bons de
  récompense, seize ressources compactées sur trois niveaux et quarante-quatre
  variantes rang/forme du multi-tool ;
- progression du multi-tool : Sans rang/Citoyen en bois, Artisan/Marchand en
  pierre, Écuyer/Chevalier en fer, Baron/Vicomte en or, Comte/Marquis en
  diamant et Duc en netherite, avec une texture complète exclusive à chacun
  des onze états de rang ;
- identité RP évolutive commune aux quatre formes : outil brut, laiton civique,
  rivets d’artisan, émeraude marchande, tabard d’écuyer, croix de chevalier,
  rubis de baron, améthystes de vicomte, saphir de comte, joyau de marquis puis
  netherite couronnée et runes cyan du Duc ;
- icône de pack Valoria.

Le pack contient **268 modèles d'items** et autant de textures premium 32×32, avec une interface de conteneur Valoria complète. Les
sources sont générées de façon déterministe, sans outil propriétaire :

```bash
python3 scripts/generate-resource-pack.py
```

Le build Maven crée automatiquement :

```text
target/ValoriaTycoon-0.38.0-SNAPSHOT-resource-pack.zip
```

Hébergez ce ZIP sur une URL HTTPS publique, puis configurez cette URL et son
SHA-1 dans les paramètres de pack de ressources du serveur Paper. Le serveur
peut revenir aux icônes vanilla sans recompilation avec :

```yaml
# plugins/ValoriaTycoon/resource-pack.yml
item-models:
  enabled: false
```

Sans le pack client alors que les modèles sont activés, Minecraft affiche son
modèle manquant. Pour un serveur public, le pack doit donc être proposé ou
rendu obligatoire dans la configuration Paper.
