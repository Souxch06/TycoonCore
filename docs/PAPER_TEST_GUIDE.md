# Test local ValoriaTycoon 0.38.0

## 1. Prérequis

- JDK 25 ;
- Maven 3.9+ ;
- Paper 26.2 build compatible avec `paper.version` du `pom.xml` ;
- un serveur de test vide ou une copie sauvegardée du serveur existant.

## 2. Compiler et auditer

```bash
mvn clean verify
python3 scripts/verify-release.py --full
```

Artefacts attendus :

```text
target/ValoriaTycoon-0.38.0-SNAPSHOT.jar
target/ValoriaTycoon-0.38.0-SNAPSHOT-resource-pack.zip
```

Copier le JAR dans `plugins/`, héberger le ZIP du pack sur une URL HTTPS puis configurer son URL/SHA-1 côté Paper. Ne jamais tester une migration sans sauvegarder la base SQLite.

## 3. Premier démarrage

Vérifier dans la console :

- aucune exception de configuration ;
- migration SQLite jusqu’à `user_version = 26` ;
- chargement du spawn, des quatre farms et des mondes Skyblock ;
- message `ValoriaTycoon is ready` ;
- 259 modèles/textures dans l’audit.

Commandes de découverte :

```text
/spawn
/warp
/warp tuto
/warp caisse
/is
/farm
/rank
/pets
/top
```

## 4. Test prioritaire des caisses

Donner une clé de chaque type :

```text
/isadmin cratekey <joueur> vote 1
/isadmin cratekey <joueur> quest 1
/isadmin cratekey <joueur> farm 1
/isadmin cratekey <joueur> common 1
/isadmin cratekey <joueur> rare 1
/isadmin cratekey <joueur> epic 1
/isadmin cratekey <joueur> legendary 1
/isadmin cratekey <joueur> valoria 1
/isadmin petkey <joueur> 1
```

Pour chaque caisse générique :

1. cliquer sa station avec la clé dans l’inventaire ou la main secondaire ;
2. vérifier qu’une seule clé disparaît et qu’un seul jeton est livré ;
3. lire le montant/contenu déjà fixé dans le lore ;
4. utiliser le jeton par clic droit ;
5. vérifier le crédit ou les objets obtenus ;
6. réutiliser une copie du même UUID : aucun second gain ne doit être possible.

Contrôler particulièrement :

- argent exact et audit économique ;
- MineCoins, FarmCoins, WoodCoins et FishCoins ;
- sac universel créditant les quatre monnaies ;
- ressources limitées aux zones déjà débloquées ;
- aucun poisson, compactage, validation de quête, métier, niveau d’outil ou temps de jeu ;
- clés issues d’un bon récupérées après reconnexion ;
- Clé Valoria à 0,5 % dans la table Légendaire ;
- Caisse Pets toujours indépendante.

## 5. Tests de reprise et inventaire

- ouvrir avec l’inventaire plein : le jeton ou l’excédent doit tomber au sol ;
- arrêter le serveur après la transaction d’ouverture et avant la livraison : au retour, le même `reward_id` et le même payload doivent être livrés ;
- interrompre la livraison physique d’un bon consommé : `pendingClaims` doit la reprendre à la connexion ;
- rejouer le même identifiant de transaction boutique : aucune nouvelle Clé Valoria ;
- terminer une quête puis redémarrer avant livraison : une seule Clé Quêtes ;
- rejouer exactement le même vote Votifier : une seule Clé Vote.

## 6. Non-régression rapide

- créer une île, suivre les six étapes du tutoriel puis devenir Citoyen ;
- reconnecter et reset l’île : le tutoriel ne doit jamais recommencer ;
- transformer le multi-tool dans ses quatre formes et ouvrir son panneau ;
- vérifier auto-sell, compactage, générateurs, quêtes, promotions et pets ;
- vérifier les douze panneaux de `/warp tuto` ;
- décharger/recharger naturellement les chunks du spawn sans doublon d’entités ;
- tester `/top` pendant son premier calcul asynchrone.
