# Studio audit — consolidation des recherches

05/10/2026. Chore de read-model et livraison, sans nouvelle règle métier.

## Mesure avant/après

StudioAuditVolumeIT peuple 100 000 faits synthétiques sur PostgreSQL 13.1.
Les 50 faits de l’opérateur/commande rares sont les plus anciens. Le test
capture le SQL et les arguments réels du JdbcAdminAuditLogRepository au niveau
technique JdbcTemplate, puis exécute EXPLAIN ANALYZE BUFFERS et la vraie query.
Chaque première page contient 30 faits et un curseur (limit + 1).

Avant : 99 950 lignes éliminées par filtre pour chaque recherche, balayage puis
tri. Après : scan direct de ix_admin_audit_actor_cursor et
ix_admin_audit_command_cursor, 31 lignes, aucune ligne éliminée par filtre.
Sur la dernière exécution locale, opérateur : 17,477 → 0,031 ms ; commande :
environ 26,229 → 0,04 ms. Ces observations synthétiques à chaud ne constituent
ni SLA ni promesse de performance en production. Le test protège le travail
SQL (<500 lignes éliminées), pas une durée dépendante de la machine.
Les plans JSON sont produits dans target/studio-audit-volume.json.

## Choix et livraison

Deux index (actor_user_id, occurred_at DESC, id DESC) et
(command_id, occurred_at DESC, id DESC), le second partiel sur command_id non
nul. Pas d’index spéculatif pour toutes les combinaisons de filtres.
Schéma bootstrap et migration additive cohérents ; driver distinct
studio-admin-audit-search-2026-10.psql intégré au renderer et au déploiement
staging existants. Baseline, checksum, verrou, transaction et reçu conservés ;
aucun ancien manifeste modifié. La création transactionnelle peut attendre
les écritures : lock_timeout borne l’attente ; prévoir une fenêtre de release
adaptée à la volumétrie réelle. Aucun SQL exécuté sur staging ici.

StudioAuditIndexMigrationIT rend et exécute le véritable manifeste deux fois
sur PostgreSQL 15 jetable : données conservées, deux index et un seul reçu.
ReleaseManifestTest et DeploymentSafetyIT vérifient aussi rendu et orchestration.

## Validation

46 tests ciblés / 5 classes verts : StudioAuditVolumeIT, StudioAdminAuditIT,
StudioAuditIndexMigrationIT, ReleaseManifestTest et DeploymentSafetyIT.
Commande : -Dtest=StudioAuditVolumeIT,StudioAdminAuditIT,StudioAuditIndexMigrationIT,ReleaseManifestTest,DeploymentSafetyIT test.
JDK 21 et MAVEN_OPTS=-XX:TieredStopAtLevel=1 utilisés.
Le RED de volume est le balayage excessif ; les erreurs préalables de varargs
et compilation sont des erreurs de setup, pas des RED métier. Mutation de
règle métier non applicable à cette indexation ; les décisions d’audit restent
couvertes par les tests/mutations de la tranche précédente.
Le benchmark ne couvre pas la volumétrie Users/Posts/Media ni le système entier.
Aucun nouveau contrôleur, port métier, projection, permission ou endpoint.
