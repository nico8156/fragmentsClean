# AWS / infra — préflight de release, 12 septembre 2026

Lot 10, Astra High. AWS/infra est traité **maintenant, avant le build candidat
TestFlight**. Autorisation utilisateur : poursuivre le chantier. Cette tranche
effectue des lectures AWS et prépare des changements locaux ; aucune mutation
AWS, lecture de secret, lecture de message, modification de base ou mise en ligne.
La reprise 10B a ensuite exécuté deux diagnostics SSM en lecture seule du
runtime/DDL (traces SSM créées, aucune mutation applicative) ; voir le
[candidat d'upgrade SQL](app-store-schema-upgrade.md).
Après autorisation explicite, le backup du 12 septembre a été restauré et migré
localement avec succès à 12:50:06 CEST ; copies supprimées et staging inchangé.

## Cible résolue

- Compte `851725375299`, région `eu-west-3`, identité `anchor-admin` via le
  profil CLI `default` (le nom d'utilisateur IAM n'est pas un profil CLI).
- Stack hôte actif : `platform-staging`, instance `i-004d3e9cbca327d01`, SSM Online.
- Rôle partagé : `fragments-platform-staging-runtime`.
- `fragments-staging-minimal` possède les files et ECR, mais expose aussi une
  ancienne instance `i-04984df67caf3099b` : ne pas la confondre avec l'hôte actif.
- Bucket partagé `anchor-assets-prod-851725375299` ; périmètre Fragments limité
  à `fragments/staging/`. Les données/permissions Anchor sont hors modification.

## Constats observés en lecture seule

| Élément | Observation | Conséquence |
| --- | --- | --- |
| Stacks | `UPDATE_COMPLETE`, dernières mises à jour 26 août (plateforme) et 4 juillet (standalone) | L'état local ne décrit pas à lui seul l'état déployé |
| SQS | 7 files sources, une DLQ partagée, aucune file `experiences-events` | Les événements Experience et certains événements partagés ne peuvent pas suivre leur route cible |
| Redelivery ticket | `ticket-events` pointe encore vers la shared DLQ, maxReceiveCount 5 ; SSE SQS activé | La migration vers DLQ par destination n'est pas déployée |
| Shared DLQ | Environ 5 messages visibles, 0 en vol au contrôle | Conserver ; triage séparé avant tout redrive, aucune purge |
| Alarmes | Aucune alarme dont le nom commence par `fragments-staging` | Les alarmes prévues dans le template ne sont pas déployées sous ces noms |
| IAM runtime | ARN Experience absent de la liste SQS ; S3 encore `fragments/staging/*` ; pas de `PutMetricData` dans la policy runtime inspectée | Mettre à jour la liste des ARN et le namespace métrique, puis appliquer le resserrement S3 préparé |
| SSM | `APPLE_TEAM_ID`, `APPLE_KEY_ID`, `APPLE_PRIVATE_KEY`, `AUTH_PROVIDER_CREDENTIAL_ENCRYPTION_KEY` absents de l'inventaire `/fragments/staging/` | Le bootstrap actuel échouerait avant génération de `.env` ; fournir ces prérequis avant déploiement |
| S3 confidentialité | Quatre blocages publics actifs, chiffrement AES256 par défaut | Bonne base ; ne prouve pas les autorisations ni l'effacement du nouveau parcours |
| S3 conservation | Versioning non activé ; aucune configuration lifecycle | Aucun changement global automatique sur ce bucket partagé ; politique par préfixe à définir |
| Sauvegarde | Dump `fragments-20260912T032402Z.dump`, 447030 octets, et checksum présents | SHA-256 vérifié, restauration/upgrade locaux réussis après autorisation ; copie supprimée, objet S3 intact |
| Volumes hôte | gp3, 30 et 80 Gio, tous deux chiffrés | Pas de besoin établi de nouvel hôte ou de nouveau volume |

Ces constats viennent de `describe-stacks`, `list-queues`, `get-queue-attributes`,
`describe-parameters` (métadonnées uniquement), `describe-alarms`, lecture de la
policy IAM nommée, contrôles S3 et métadonnées EC2/SSM. Au préflight initial, aucun
contenu de sauvegarde n'avait été consulté ; le drill ultérieur autorisé est
consigné séparément. Aucun message ni paramètre chiffré consulté. Les compteurs SQS sont
approximatifs et l'inventaire d'alarmes est limité au préfixe indiqué.

## Corrections locales de cette tranche

Commit backend local : `2689493`. Aucun push ni déploiement associé.

1. Destination Experience : file, DLQ dédiée chiffrée, alarmes d'âge et de DLQ,
   sorties CloudFormation, permission dans le rôle du template standalone.
2. Runtime : `SQS_EXPERIENCES_EVENTS_URL` ajouté au bootstrap et à l'exemple de
   configuration. Le publisher échoue explicitement si une URL manque : ce
   n'était pas une simple omission documentaire.
3. Rôle plateforme : autorisation `cloudwatch:PutMetricData` limitée au namespace
   `Fragments/Staging`. Les droits Anchor restent inchangés.
4. Garde-fou : le test d'infrastructure dérive les destinations du catalogue
   Java de production au lieu d'une seconde liste manuelle ; il vérifie aussi
   le câblage `application.properties` → bootstrap → exemple d'environnement.
5. CI : la suite complète `scripts/test-release.sh` remplace la sélection partielle
   avant packaging, sur Java 21. La CI distante n'a pas été exécutée ici.

Le paramètre `FragmentsSqsQueueArns` du stack **plateforme** doit être étendu
explicitement avec
`arn:aws:sqs:eu-west-3:851725375299:fragments-staging-experiences-events` lors du
changement. Préserver tous les ARN existants et `AnchorSqsQueueArns` ; un simple
`UsePreviousValue` conserverait l'omission. Les consommateurs n'ont pas besoin
de droits supplémentaires sur les DLQ pour traiter les files sources ; le
redrive opérateur reste une autorisation distincte.

## Ordre d'intervention AWS

Vérification locale : tests d'infrastructure d'abord rouges sur les omissions
Experience puis sur la permission métrique, ensuite suite complète verte :
**497 tests / 206 classes, 0 échec, 0 erreur, 0 ignoré**, le 12 septembre à
12:13:17 CEST, en 2 min 33. Rapports : `target/release-verification.2W5I8L`.
La JVM locale reste Java 23 (compilation Java 21) ; l'exécution CI Java 21 reste
à obtenir. Surefire signale encore une fermeture forcée de sa JVM après 30 s et
des connexions Hikari fermées en fin de tests : le build réussit, mais ce bruit
de terminaison reste à investiguer. YAML et syntaxe shell validés localement ;
ce n'est ni une validation sémantique CloudFormation ni un change set AWS.
Mobile et Studio n'ont pas changé dans cette tranche ; leurs dernières preuves
restent celles du [dossier TestFlight](testflight-app-store-release.md).

| Étape | Action | Critère de passage |
| --- | --- | --- |
| 10A — réalisé | Inventaire en lecture seule et préparation locale ci-dessus | Constats et tests consignés |
| 10B — préparation locale et restauration validées | Journal/checksums, saut des reprises de données, workflow manuel et séquence stop/backup/migration préparés ; revue du futur change set reste à faire | Aucun remplacement EC2/EBS, aucune modification Anchor ; application staging toujours non autorisée |
| 10C — infra après accord | Files/DLQ/alarmes ; liste IAM SQS, droits métriques et S3 restreints ; abonnement opérateur confirmé | Routes existantes conservées, aucune purge/relecture des 5 messages legacy |
| 10D — configuration | Fournir les valeurs Apple/chiffrement en SSM par un canal sûr ; confirmer capacité Apple et opérateur de modération/alertes | Ne jamais coller de clé privée dans le chat ni dans Git |
| 10E — migration et application | Upgrade SQL explicite et testé sur copie, puis image backend ARM64 Java 21 identifiée | Sauvegarde vérifiée, migration transactionnelle, santé et reprise correctes |
| 10F — recette déployée | Expérience libre, ticket, avatar/photo, signalement/blocage, suppression, commandes sans socket et SSE | Vérification mobile + Studio + S3, jobs, projections et DLQ |
| 10G — TestFlight | Build signé et campagne appareils sur cette candidate | Matrice du dossier TestFlight remplie, accord produit avant soumission |

Le déploiement historique appliquait `schema.sql` à une base existante. Le
[chemin local journalisé](journaled-staging-deployment.md) remplace maintenant
cette application par un bundle explicite, avec contrôle de checksum et un
backup après arrêt du writer Fragments. Ce chemin n'est pas encore déployé.
Le runtime identifié est `18ae517`, Java 21.0.12, PostgreSQL 15.19 et 46 tables.
Le schéma réel a été exporté sans données et le candidat
`db/release/app-store-2026-09.psql` est maintenant testé sur cette structure.
La restauration du backup et l'upgrade sur ses lignes réelles sont maintenant
validés localement (46 tables sources préservées, 69 tables après upgrade).
La traçabilité et l'intégration au script sont préparées et testées localement ;
la validation du binaire et du déroulement sur l'hôte partagé reste ouverte.

Le workflow local devient manuel avec approbation explicite, mais il n'a pas été
poussé : le workflow distant historique est toujours celui de l'ancien pipeline.
Aucun push n'est autorisé implicitement ; aucun workflow GitHub n'a été lancé.
La proposition précise (28 ajouts, ou 29 avec abonnement email ; OIDC existant
conservé) est détaillée dans le dossier journalisé. Ce diff n'est pas un change set.

## Périmètre d'accord et risques

Avant application : présenter les changements CloudFormation/IAM exacts, la
liste de ressources et les impacts de coût des alarmes/SNS/SQS. Ne pas ajouter
Kafka, Redis, NAT Gateway, ALB ou nouvelle instance sans besoin documenté.
Ne pas appliquer le template complet `staging-minimal` sans revue : il contient
des ressources d'une ancienne topologie et un AMI résolu via SSM.

Pas de lifecycle/versioning global sur le bucket partagé sans décision : une
règle de rétention doit cibler les préfixes Fragments et tenir compte des
uploads non confirmés, effacements, sauvegardes et fenêtre de rollback.
Une URL présignée privée ne remplace pas un test d'autorisation applicative.

Les paramètres Apple et l'adresse recevant les alarmes sont des entrées opérateur
manquantes. L'accord de restauration locale a été obtenu et exécuté ; il ne
couvre pas la création/modification de ressources AWS ou la migration staging,
qui nécessitent toujours une revue des cibles et un accord distinct.
