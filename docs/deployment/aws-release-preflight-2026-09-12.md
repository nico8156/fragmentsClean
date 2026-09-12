# AWS / infra — préflight de release, 12 septembre 2026

Lot 10, Astra High. AWS/infra est traité **maintenant, avant le build candidat
TestFlight**. Autorisation utilisateur : poursuivre le chantier. Cette tranche
effectue des lectures AWS et prépare des changements locaux ; aucune mutation
AWS, lecture de secret, lecture de message, modification de base ou mise en ligne.
La reprise 10B a ensuite exécuté deux diagnostics SSM en lecture seule du
runtime/DDL (traces SSM créées, aucune mutation applicative) ; voir le
[candidat d'upgrade SQL](app-store-schema-upgrade.md).

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
| Sauvegarde | Dump `fragments-20260912T032402Z.dump`, 447030 octets, et checksum présents | Dernière sauvegarde observée à 03:24 UTC ; pas encore restaurée ni vérifiée cryptographiquement |
| Volumes hôte | gp3, 30 et 80 Gio, tous deux chiffrés | Pas de besoin établi de nouvel hôte ou de nouveau volume |

Ces constats viennent de `describe-stacks`, `list-queues`, `get-queue-attributes`,
`describe-parameters` (métadonnées uniquement), `describe-alarms`, lecture de la
policy IAM nommée, contrôles S3 et métadonnées EC2/SSM. Aucun contenu de sauvegarde,
de message ou de paramètre chiffré n'a été consulté. Les compteurs SQS sont
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
| 10B — en cours | Runtime et schéma inspectés ; upgrade transactionnel testé sur DDL réel et données synthétiques. Restauration réelle et revue des change sets restent à faire | Aucun remplacement EC2/EBS, aucune modification Anchor, rollback identifié |
| 10C — infra après accord | Files/DLQ/alarmes ; liste IAM SQS, droits métriques et S3 restreints ; abonnement opérateur confirmé | Routes existantes conservées, aucune purge/relecture des 5 messages legacy |
| 10D — configuration | Fournir les valeurs Apple/chiffrement en SSM par un canal sûr ; confirmer capacité Apple et opérateur de modération/alertes | Ne jamais coller de clé privée dans le chat ni dans Git |
| 10E — migration et application | Upgrade SQL explicite et testé sur copie, puis image backend ARM64 Java 21 identifiée | Sauvegarde vérifiée, migration transactionnelle, santé et reprise correctes |
| 10F — recette déployée | Expérience libre, ticket, avatar/photo, signalement/blocage, suppression, commandes sans socket et SSE | Vérification mobile + Studio + S3, jobs, projections et DLQ |
| 10G — TestFlight | Build signé et campagne appareils sur cette candidate | Matrice du dossier TestFlight remplie, accord produit avant soumission |

Le déploiement actuel `deploy-via-ssm.sh` applique encore `schema.sql` à une base
existante. **Ce chemin n'est pas validé pour cette release.** Ne pas le remplacer
aveuglément par tous les scripts de `db/release` : certains supposent un schéma
initial déterminé, et des évolutions antérieures sont dans le bootstrap.
Le runtime identifié est `18ae517`, Java 21.0.12, PostgreSQL 15.19 et 46 tables.
Le schéma réel a été exporté sans données et le candidat
`db/release/app-store-2026-09.psql` est maintenant testé sur cette structure.
La restauration de sauvegarde avec ses lignes réelles, la traçabilité des
migrations et l'intégration sûre au déploiement restent à faire.

Un push de `main` touchant les chemins suivis déclenche le workflow de déploiement.
Ne pas utiliser ce push comme simple transfert de code tant que le SQL, SSM et
les changements infra ne sont pas prêts. Cette branche est intégrée localement
uniquement ; aucun workflow GitHub n'a été lancé.

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
manquantes. Une restauration de sauvegarde et une création de ressources sont
des actions distinctes à approuver avec leurs cibles exactes ; aucune action
irréversible n'est incluse dans ce préflight.
