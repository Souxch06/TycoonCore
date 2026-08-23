# ValoriaTycoon — état de préparation V1

## Périmètre retenu

- serveur Paper 26.2 unique ;
- Java 25 ;
- stockage SQLite atomique ;
- pack de ressources distribué manuellement ;
- Vault et MySQL/MariaDB volontairement reportés ;
- permissions historiques `tycoon.*` conservées.

## Fonctionnalités internes terminées

- économie, paiements et audit ;
- spawn médiéval, warps, Académie tutorielle à douze panneaux et farms classées ;
- Skyblocks, protections, membres, resets et upgrades ;
- multi-tool unique, métiers, capacités et monnaies ;
- compactage et tutoriel ;
- générateurs ;
- quêtes et dix rangs ;
- pets, œufs, chromatiques et clés anti-duplication ;
- classements asynchrones et hologrammes ;
- huit caisses génériques ouvertes avec tables finales et Caisse Pets spécialisée ;
- clés et jetons de récompense physiques uniques, reprise après crash et consommation SQLite ;
- Caisse Valoria signature et jackpot Légendaire à 0,5 % ;
- pack Valoria de 268 modèles/textures.

## Blocage fonctionnel interne

Aucun module V1 connu ne reste volontairement désactivé. `opening-enabled` est à `true` et les huit tables de `crate-rewards.yml` totalisent chacune exactement 10 000 points. Les reports Vault, MySQL/MariaDB et distribution automatique du resource pack restent hors périmètre V1 par décision de conception.

## Gates techniques avant publication commerciale

- [ ] installer JDK 25 et Maven 3.9+ ;
- [ ] exécuter `mvn clean verify` puis `python3 scripts/verify-release.py --full` ;
- [ ] corriger toute erreur Paper 26.2 réelle ;
- [ ] démarrer un serveur Paper 26.2 vierge ;
- [ ] migrer une copie d’une base v23/v24/v25 vers v26 ;
- [ ] suivre `docs/PAPER_TEST_GUIDE.md` et la matrice manuelle du README ;
- [ ] tester le pack ZIP depuis son URL HTTPS finale ;
- [ ] vérifier les 44 formes de multi-tool, les caisses, les hologrammes et `/warp tuto` en jeu ;
- [ ] copier une clé et un jeton puis simuler un crash entre ouverture et livraison ;
- [ ] simuler crashs pendant paiements et émission/livraison d’œufs ;
- [ ] profiler avec plusieurs joueurs dans des farms/chunks distincts ;
- [ ] effectuer une sauvegarde puis tester restauration et rollback.

## Audit sans dépendance

```bash
python3 scripts/verify-release.py
```

Cet audit contrôle les packages Java, délimiteurs, imports, configurations installées, commandes déclarées/enregistrées, XML/JSON/PNG, références de modèles, génération déterministe et structure du ZIP.

L’option `--full` exige en plus une vraie compilation et tous les tests Maven :

```bash
python3 scripts/verify-release.py --full
```
