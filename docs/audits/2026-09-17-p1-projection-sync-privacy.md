# P1 — Cloisonnement de Projection Sync

Date : 2026-09-17  
Branche backend : `fix/p1-private-projection-sync`  
Branche mobile : `fix/p1-private-projection-sync`  
Branche Studio : `fix/studio-projection-sync-admin-route`  
Constat traité : `FR-004`

## Résultat recherché

Un utilisateur mobile ne doit recevoir que les signaux publics et ceux dont il
est le destinataire. Studio reçoit les signaux publics et administratifs, mais
pas les signaux privés des utilisateurs. SSE reste un simple déclencheur de GET
des projections ; il ne transporte ni événement de domaine ni read model.

## Modèle retenu

Le journal `projection_sync_events` persiste une audience explicite :

- `PUBLIC`, sans destinataire ;
- `USER`, avec `recipient_id` obligatoire ;
- `ADMIN`, sans destinataire.

Les producteurs utilisent trois factories distinctes. Les tickets, droits du
Pass, cafés enregistrés, blocages et expériences personnelles sont `USER`.
Les files de modération sont `ADMIN`. Cafés, articles, commentaires, likes et
expériences par café sont `PUBLIC`.

Le dispatcher filtre avant émission. Son curseur interne avance également sur
les événements invisibles, afin qu'une série d'événements appartenant à un
autre compte ne bloque pas les événements visibles suivants. L'adaptateur SSE
mappe vers un DTO public qui ne sérialise ni audience ni destinataire.

La migration historique échoue fermée : toutes les formes sont d'abord
classées `ADMIN`, puis seules les formes publiques et privées reconnues sont
reclassées. Pour les tickets, le propriétaire est repris depuis la projection
locale `ticket_status_projection`. Une forme inconnue ne devient donc jamais
publique par défaut. Le défaut SQL reste également `ADMIN` afin qu'un éventuel
producteur futur contournant le repository typé échoue fermé.

Le listener mobile conserve une défense en profondeur pour les projections de
scope `user` : experiences, blocked-users, entitlements et savedCoffees ne
déclenchent un GET que si `entityId` correspond au compte actif. Pour les
tickets, dont `entityId` est le ticket et non l'utilisateur, le filtrage serveur
reste la barrière d'autorisation.

Studio utilise par défaut `/api/admin/sync/events`. La variable publique
`VITE_PROJECTION_SYNC_EVENTS_PATH` peut toujours surcharger ce chemin, mais une
configuration omise ne rabat plus le client administrateur sur la route mobile.

## FlowAtlas

FlowAtlas a été utilisé sur le code Redux avec `find` puis `context` autour de
`projectionSyncListenerFactory`. La projection bornée a correctement relié les
actions de connexion/déconnexion, le cycle de session et le listener de
rafraîchissement. Elle a réduit l'exploration initiale du flux mobile. Elle ne
pouvait pas prouver l'autorisation côté serveur, la classification des
producteurs Java ou les propriétés du curseur : ces points ont été confirmés
dans les sources et les tests.

## Vérifications exécutées

- tests unitaires backend ciblés : dispatcher, contrôleur, DTO SSE, producteurs
  tickets et expériences ;
- `JdbcProjectionSyncRepositoryIT` avec PostgreSQL/Testcontainers : persistance
  et relecture de l'audience et du destinataire ;
- `StagingReleaseUpgradeIT` avec PostgreSQL 15 : migration depuis le schéma
  staging observé, classification public/user/admin, forme inconnue admin-only,
  rejeu et concurrence du manifeste ;
- test Jest ciblé du listener Redux : public inchangé, compte courant rafraîchi,
  notifications étrangères entitlements/savedCoffees/blocked-users ignorées ;
- suite Jest mobile complète avec `--runInBand --no-watchman` : **93 suites,
  353 tests**, 0 échec ;
- TypeScript `npx tsc --noEmit` : vert ;
- ESLint sans cache : aucune erreur, 16 avertissements préexistants hors lot.
- cartographie Redux `npm run redux:map:check` : à jour ;
- garde-fous de configuration `npm run test:release:config` : **8 tests**, 0
  échec ;
- contrôle natif `npm run native:release:check` : configuration source alignée,
  validation du binaire signé et sur appareil toujours requise ;
- test ciblé Projection Sync Studio : **9 tests**, 0 échec ;
- suite Vitest Studio complète : **34 fichiers, 161 tests**, 0 échec ;
- contrat généré Studio `npm run contract:check` : à jour ;
- build Studio avec authentification OAuth et jetons navigateur neutralisés :
  vert, bundle JavaScript 303,64 kB (88,06 kB gzip) ;
- gate backend `./scripts/test-release.sh` : **563 tests**, 0 échec, 0 erreur,
  0 ignoré ; tests unitaires, intégration, architecture, migrations,
  PostgreSQL, LocalStack et verticales inclus.

Le premier lancement Jest sans `--no-watchman` a échoué car le sandbox ne
pouvait pas joindre le socket Watchman. Le même test avec `--no-watchman` est
vert. `npm run lint` a d'abord échoué sur l'écriture du cache `.expo`; la passe
équivalente `npx eslint . --no-cache` s'est terminée sans erreur.

Le premier `npm run build` Studio a été refusé, comme attendu, par le garde-fou
de production : l'environnement local exposait encore le mode et les jetons de
développement. Le même build, exécuté sans jeton navigateur et avec
`VITE_STUDIO_AUTH_MODE=oauth`, est vert. Vite signale encore l'import sans
extension de `validateBuildEnv` avant son futur passage au chargeur natif ; cet
avertissement préexistant est hors du présent lot.

Le gate backend produit encore au shutdown des avertissements de tâches
planifiées qui observent la fermeture des pools Testcontainers et le message
Surefire indiquant qu'il termine le fork après 30 secondes. Le résultat Maven
et le contrôle des rapports sont néanmoins verts, sans test ignoré. Ce bruit de
shutdown est préexistant et ne constitue pas une preuve d'échec de ce lot.

## Déploiement

Le manifeste additif `projection-sync-audience-2026-09.psql` dépend de
`account-erasure-safety-2026-09`. Le pipeline staging télécharge, rend et joint
ce sixième manifeste au bundle signé par checksum/révision. La migration doit
précéder le démarrage du backend candidat, puisque son repository écrit les
nouvelles colonnes.

État au moment de ce document : implémentation locale testée, non encore mergée
ni déployée. `FR-004` ne doit être déclaré fermé en exploitation qu'après
migration staging, déploiement du backend et recette avec deux comptes et un
administrateur.
