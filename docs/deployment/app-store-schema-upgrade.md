# Upgrade SQL App Store — candidat du 12 septembre 2026

Lot 10B, Astra High. **Testé sur une restauration réelle locale, pas appliqué au staging.**
Ce document complète le [préflight AWS](aws-release-preflight-2026-09-12.md).
Implémentation locale : `184e2ba` ; aucun push ni déploiement.

## Point de départ effectivement observé

Diagnostic SSM sur `i-004d3e9cbca327d01`, compte `851725375299`, `eu-west-3` :

- backend `staging-fragments-backend-1`, image
  `851725375299.dkr.ecr.eu-west-3.amazonaws.com/fragments/staging/backend:sha-18ae517ab13dc2f8c8812914eae501ab3f1dfe55` ;
- image locale Docker `sha256:29ee34bb84e63394d232a9011be241c18762b4f9360696bb7c2a1e111c6dddfc`
  (identifiant Docker observé, pas présenté comme digest du manifeste ECR) ;
- Temurin `21.0.12+8`, PostgreSQL `15.19` ARM64 ;
- conteneur base `fragments-staging-fragments-postgres-1` ;
- 46 tables publiques ; pas de table de suivi de migration dans cet inventaire ;
- table legacy `admin_studio_articles` encore présente : aucune lecture de
  contenu, migration de contenu ou suppression effectuée.

Les commandes SSM `5ff46c92-9237-48af-a308-0a44a401d799` (runtime) et
`2f2064db-eadd-4da7-9928-1b38ddbad8e1` (DDL) sont terminées avec succès.
SSM a exécuté des diagnostics, donc créé ses traces de commande, mais aucune
ressource applicative, configuration ni donnée métier n'a été modifiée.
PostgreSQL a été interrogé avec `default_transaction_read_only=on`.

Le dump **schema-only**, sans propriétaires/permissions, lignes applicatives ou
valeurs courantes des séquences, est devenu la fixture
`src/test/resources/db/staging-2026-09-12-schema-only.sql`. Seuls les commentaires
générés et directives psql restrict/unrestrict ont été retirés, ainsi que le
commentaire vide du schéma public. Les anciens objets restent dans la fixture
pour vérifier leur conservation ; ils ne sont pas réintroduits dans le runtime.

## Chemin explicite

Entrée : `src/main/resources/db/release/app-store-2026-09.psql`.

Ce script psql, et non un script JDBC de démarrage, assemble dans un ordre fixé :
receipts → cycle de vie compte → Pass → historique tickets → modération sociale
→ expériences → médias privés → jobs de vérification/inbox.

- Une transaction couvre DDL et backfills, avec `ON_ERROR_STOP`, verrou
  transactionnel de migration, limite d'attente des verrous et des requêtes.
- Les scripts médias et jobs ne terminent plus eux-mêmes la transaction.
  Exécutés seuls, ils nécessitent `psql --single-transaction -v ON_ERROR_STOP=1`.
- Absence d'une table prérequise : rejet avant ajout d'objets.
- Pas de `DROP`, pas de purge de commandes, inbox, outbox ou données legacy.
- Backfills limités aux migrations déjà définies : propriétaire non ambigu
  démontré par outbox, empreintes tickets, contributions/acquis Pass, références
  café/profil d'Experience. Ce SQL de migration exceptionnel n'est pas une
  autorisation de jointures inter-contextes dans le comportement applicatif.
- Les jobs de vérification commencent vides : aucun rejeu implicite des anciens
  tickets ou messages de DLQ.
- Tous les fichiers doivent venir de la même révision Git immuable revue.

Ce candidat n'est pas encore un moteur général de migrations : pas de journal
versionné/checksummé ni de détection exhaustive de dérive. La présence des tables
prérequises ne prouve pas à elle seule la compatibilité de leurs colonnes.
L'inspection, la restauration et les tests ci-dessous restent donc obligatoires.
Ne pas lancer ce backfill à chaque démarrage ou à chaque déploiement.

## Preuves automatisées

`StagingReleaseUpgradeIT` utilise PostgreSQL 15 et le vrai exécutable psql dans
Testcontainers. La fixture ne contient que le DDL observé ; toutes les données
des tests sont synthétiques.

1. Upgrade du schéma observé, conservation du profil, du document legacy et
   des tickets ; déduplication des contributions sans suppression des tickets ;
   acquis Pass conservés ; références Experience initialisées ; reçus sans
   preuve ou avec propriétaires contradictoires laissés sans propriétaire.
2. Colonnes comparées au bootstrap cible ; contraintes et index existants
   conservés ; contraintes/index des tables modifiées comparés à la cible.
   L'unicité est contrôlée par les index uniques, indépendamment de leur nom.
3. Réexécution sans écraser profil/progression plus récents.
4. Erreur volontaire dans le dernier fragment : rollback de tous les ajouts et
   backfills précédents. Ce scénario a d'abord échoué avec les `COMMIT` internes.
5. Base sans prérequis : rejet sans schéma partiellement créé.

Ces scénarios sont regroupés en trois tests d'intégration. Ils ne prouvent pas
la restauration du vrai backup, le comportement sur ses lignes réelles, la
durée des verrous au staging, ni la compatibilité de toutes les données legacy.

Suite backend complète : **500 tests / 207 classes, zéro échec, erreur ou
ignoré**, terminée le 12 septembre à 12:30:03 CEST en 2 min 30. Rapports neufs :
`target/release-verification.tv3Agm`. Compilation Java 21, JVM locale Java 23 ;
le runtime staging Java 21 identifié n'est pas encore celui de la candidate.
Les avertissements Hikari et la fermeture forcée Surefire après 30 s restent
présents en fin de run, comme avant cette tranche. Aucun changement mobile ou
Studio ; leurs suites ne sont pas présentées comme réexécutées ici.

## Avant application réelle

Les étapes 1 et 2 ci-dessous ont été autorisées puis réalisées le 12 septembre
(preuve détaillée en fin de document). Les étapes 3 à 5 restent ouvertes ; une
nouvelle sauvegarde sera nécessaire avant une migration réelle du staging.

1. Accord explicite sur la cible de restauration et le traitement de sa copie.
   Proposition : télécharger uniquement le dump du 12 septembre et son checksum
   dans un dossier temporaire privé local ; vérifier le checksum ; restaurer
   dans PostgreSQL 15 Docker isolé, sans exposition réseau publique, sans
   connexion au backend ni aux files AWS. Ne jamais imprimer les lignes restaurées.
2. Vérifier le candidat sur cette copie, préserver les nombres de lignes des
   tables sources, mesurer l'upgrade, contrôler les résultats agrégés puis
   supprimer uniquement la copie temporaire et son conteneur/volume dédiés.
   La sauvegarde contient potentiellement des données personnelles et tokens :
   cette étape n'est pas couverte par le seul accord d'inventaire en lecture seule.
3. Préparer le déploiement avec application explicite de cette migration et
   traçabilité de révision/checksums ; retirer l'application de `schema.sql` à
   la base existante. Aucun changement du script de déploiement dans cette tranche.
4. Avant DDL sur staging : backup frais vérifié, arrêt contrôlé des writers et
   consommateurs Fragments, aucune interruption des services Anchor. Les
   backfills ne doivent pas courir en concurrence avec une ancienne version.
5. Appliquer dans la fenêtre approuvée, démarrer la candidate et faire la recette
   de bout en bout. Le rollback DDL sur erreur est transactionnel ; après commit,
   ne pas assimiler retour à l'ancienne image et restauration de données. Décider
   du rollback avec l'état des écritures survenues depuis la sauvegarde.

**Toujours aucun push `main` : le workflow actuel déclencherait un déploiement
avant que ces conditions soient réunies.**

## Restauration réelle autorisée — preuve du 12 septembre, 12:50:06 CEST

L'utilisateur a explicitement autorisé le téléchargement local de la sauvegarde,
sa restauration/migration isolée et la suppression des copies et de leur
environnement. Cette autorisation ne couvre pas un déploiement ou une migration
de la base staging.

- Objet : `s3://anchor-assets-prod-851725375299/fragments/staging/backups/postgres/fragments-20260912T032402Z.dump`,
  447030 octets, modifié à `2026-09-12T03:24:04Z`, avec son fichier `.sha256`.
- SHA-256 calculé et conforme au checksum publié :
  `e4756abb388704327990f787025249d4681bff21b9764d368cef4651115b535a`.
  Ce contrôle prouve la concordance dump/checksum, pas une signature indépendante.
- PostgreSQL local 15.19, image Docker
  `sha256:aad6289ca337b3ce76896f2e7e61480490152886c7828120371fb28e6b779e1d`.
  Exécution locale x86_64, distincte du runtime staging ARM64.
- Conteneur sans réseau (`none`), aucun port publié, écoute TCP désactivée,
  racine en lecture seule ; base et scripts en tmpfs ; limite 512 Mio / 1 CPU.
  Aucun backend, worker, accès SQS, valeur SSM ou credential AWS dans le conteneur.
  Authentification PostgreSQL trust limitée aux sockets de ce conteneur isolé.
- Dump et logs dans un dossier temporaire privé (mode 0700, umask 077) ; aucune
  ligne restaurée, valeur de token ou erreur contenant des données affichée.
- `pg_restore --no-owner --no-privileges --exit-on-error` réussi en 1 seconde
  mesurée à la seconde ; 46 tables sources retrouvées.
- Candidat SQL `184e2ba`, présent dans le checkout `cf23e04`, appliqué en 1 seconde
  mesurée à la seconde. Ces durées ne sont pas celles du drill complet ni une
  estimation des verrous sur le staging actif.
- Conservation contrôlée pour les 46 tables : nombres de lignes et empreintes
  ordonnées des **colonnes d'origine** inchangés. Les empreintes MD5 de lignes
  servent au contrôle de non-régression ; elles sont restées privées et ont été
  supprimées, sans prétention de preuve cryptographique indépendante.
- Contrôles de backfill verts : références café/profil, empreintes canoniques
  tickets, contributions et compteurs Pass, attribution des reçus uniquement sur
  preuve non ambiguë, unicité des positions d'historique ; jobs, expériences et
  credentials fournisseur initialement vides. Pas de rejeu des travaux legacy.
- Deuxième application : empreintes et nombres de lignes inchangés sur les
  **69 tables** du schéma final.

Deux essais préalables ont corrigé le harness opérateur, pas le SQL produit :
le contrôle de disponibilité détectait le serveur temporaire d'initdb, puis
`docker cp` refusait la racine en lecture seule. Le contrôle attend désormais
le processus PostgreSQL définitif ; le transfert passe par tar/exec vers tmpfs,
sans retirer les protections. Les copies ont été nettoyées après chaque essai.

Nettoyage final vérifié séparément : trois dossiers privés et les trois
conteneurs de drill absents, ainsi que le conteneur de diagnostic vide. Base
en tmpfs, aucun volume persistant de restauration conservé ; logs, dumps,
checksums et empreintes temporaires supprimés. Il s'agit de suppression normale,
pas d'une garantie d'effacement forensique. La sauvegarde S3 d'origine a été
revérifiée présente, même taille et même date de modification. Aucune écriture
sur la base staging, aucun push et aucun déploiement.

Cette tranche apporte une **preuve opérationnelle de restauration/upgrade**,
pas une nouvelle exécution des 500 tests backend, des tests mobile ou Studio :
aucun code produit n'a changé. Restent le chemin de déploiement versionné,
les changements infra approuvés, les secrets Apple, puis la recette déployée.
