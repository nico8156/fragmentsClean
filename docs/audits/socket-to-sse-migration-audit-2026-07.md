# Audit migration WebSocket/STOMP vers HTTP + SSE - Juillet 2026

Date: 2026-07-05  
Scope backend: `/Users/nicolasmaldiney/fragmentsClean`  
Scope mobile lu: `/Users/nicolasmaldiney/fragmentsCleanFront`  
Mode: phase 1, audit uniquement, aucune modification de code applicatif.

## Decision d'architecture

La cible est une suppression complete de WebSocket/STOMP, pas une deprecation longue.

Flux cible:

```text
HTTP
-> Command
-> Outbox backend
-> SQS
-> Projection
-> ProjectionSyncEvent
-> SSE projection.updated
-> Redux Listener Middleware
-> GET read model
-> Reducer snapshot replace
-> Selectors
-> React Native
```

Regles:

- Le frontend ne recoit jamais de Domain Events.
- Le frontend recoit uniquement des signaux de projection (`projection.updated`).
- SSE ne transporte pas les donnees metier.
- SSE declenche toujours un GET.
- HTTP reste le canal des commandes.
- `/commands/{commandId}` reste le fallback canonique des writes offline.
- SQS/outbox reste le chemin de propagation backend.

## Executive Summary

STOMP est aujourd'hui concentre dans un nombre limite de fichiers, mais son influence fonctionnelle est plus large: les ACK socket alimentent les reconcilations likes, comments, tickets et entitlements. Les cafes/articles ne dependent pas de STOMP cote mobile.

Le backend envoie les ACK WebSocket directement depuis les Domain Events de l'outbox via `WebSocketOutboxEventSender`. C'est le point architectural a supprimer: meme si le payload est renomme en `*_ack`, il reste derive de Domain Events et contient parfois le payload domaine complet (`TicketVerificationCompletedEvent.payloadJson`).

Le remplacement par SSE est faisable, mais pas en big bang. Les projections read existent pour comments et tickets. Les cafes ont deja le modele Projection Sync propre. Les likes sont le point le plus fragile: le GET mobile fonctionne, mais le query handler lit encore le repository JPA write (`SpringLikeRepository`) au lieu d'une vraie projection read. Entitlements sont fake cote mobile et n'ont pas de read contract backend produit.

Ordre recommande apres audit:

1. Comments: meilleur domaine pilote.
2. Tickets: projection et endpoint existent, mais mobile gateway read a creer.
3. Likes: simple UX, mais dette read model backend a corriger avant migration propre.
4. Entitlements: necessite un vrai read model/endpoint produit.
5. Articles/autres: pas de dependance STOMP actuelle, seulement ajouter Projection Sync si besoin de freshness.

## Inventaire Mobile STOMP / WebSocket / SockJS

### Dependances npm

- `@stomp/stompjs`
- `sockjs-client`

### Fichiers transport actifs

- `app/adapters/primary/socket/WsEventsGateway.ts`
  - importe `@stomp/stompjs` et `sockjs-client`;
  - connecte a `wsUrl`;
  - envoie `Authorization: Bearer <token>` en STOMP connect headers;
  - subscribe `/user/queue/acks`;
  - parse les messages en `WsInboundEvent`.
- `app/adapters/primary/socket/ws.type.ts`
  - definit les ACK socket acceptes.
- `app/core-logic/contextWL/wsWl/usecases/wsListenerFactory.ts`
  - connecte/deconnecte le gateway;
  - route les ACK vers actions Redux de domaine.
- `app/core-logic/contextWL/wsWl/reducer/ws.reducer.ts`
  - stocke seulement `connected`, `lastConnectedAt`, `lastDisconnectedAt`.
- `app/core-logic/contextWL/wsWl/typeAction/ws.action.ts`
  - `WS/ENSURE_CONNECTED_REQUESTED`
  - `WS/DISCONNECT_REQUESTED`
  - `WS/CONNECTED`
  - `WS/DISCONNECTED`
- `app/adapters/primary/wiring/infrastructure.ts`
  - instancie `new WsStompEventsGateway()`.
- `app/adapters/primary/wiring/listeners.ts`
  - branche `wsListenerFactory`.
- `app/core-logic/contextWL/appWl/usecases/runtimeListenerFactory.ts`
  - declenche `wsEnsureConnectedRequested` au foreground/online/auth.

### Duplications a supprimer a terme

Deux duplications anormales contiennent aussi `WsStompEventsGateway` dans le core:

- `app/core-logic/contextWL/appWl/typeAction/appWl.type.ts`
- `app/core-logic/contextWL/appWl/reducer/app.reducer.ts`

Ces fichiers ne devraient jamais contenir de client STOMP/SockJS.

### Vieux sync parking

Le dossier suivant reintroduit le langage ACK sous un nom sync:

- `app/core-logic/contextWL/outboxWl/sync_PARKING/parking/syncEventsListenerFactory.ts`
- `app/core-logic/contextWL/outboxWl/typeAction/syncEvent.type.ts`

Il mappe des `like.addedAck`, `comment.createdAck`, `ticket.confirmedAck` vers les actions ACK actuelles. Il ne doit pas servir de base au futur SSE. Le futur SSE doit parler `projection.updated`.

## ACK Socket Actuels

Types acceptes par `WsInboundEvent`:

- `social.like.added_ack`
- `social.like.removed_ack`
- `social.comment.created_ack`
- `social.comment.updated_ack`
- `social.comment.deleted_ack`
- `ticket.verification.completed_ack`

Le backend envoie aussi `ticket.verify.accepted_ack`, mais le mobile ne le route pas dans `ws.type.ts`/`wsListenerFactory.ts`. Il est donc soit obsolete, soit non consomme.

## Actions Redux Dispatch depuis ACK Socket

### Likes

Socket ACK:

- `social.like.added_ack`
- `social.like.removed_ack`

`wsListenerFactory` dispatch:

- `onLikeAddedAck`
- `onLikeRemovedAck`

`ackLikesListenerFactory` dispatch ensuite:

- `likeReconciled`
- `likeSyncAcked`
- `dropCommitted`

State modifie:

- `likeWl.byTarget[targetId]`: count, me, version, updatedAt, optimistic, sync UI.
- `outboxWl`: suppression de la commande par `dropCommitted`.

Read endpoint existant:

- `GET /api/social/targets/{targetId}/likes`

Probleme:

- Le backend lit le write repository `SpringLikeRepository`, pas une projection read. Pour une migration SSE propre, il faut soit creer une projection likes, soit accepter cette dette explicitement pour une premiere passe.

### Comments

Socket ACK:

- `social.comment.created_ack`
- `social.comment.updated_ack`
- `social.comment.deleted_ack`

`wsListenerFactory` dispatch:

- `onCommentCreatedAck`
- `onCommentUpdatedAck`
- `onCommentDeletedAck`

`ackListenerFactory` dispatch ensuite:

- create: `createReconciled`, `dropCommitted`
- update: `updateReconciled`, `dropCommitted`
- delete: `deleteReconciled`, `dropCommitted`

State modifie:

- `commentWl.entities`
- `commentWl.byTarget[targetId]`
- `outboxWl`

Read endpoint existant:

- `GET /api/social/comments?targetId=:targetId&op=refresh|retrieve|older&cursor=&limit=`

Projection existante:

- `social_comments_projection`

Probleme:

- Le reducer `commentsRetrieved` upsert et merge selon `op`. Pour un refresh declenche par projection sync, `op=refresh` prepend sans supprimer les items disparus. Pour remplacer totalement l'ACK par GET, il faut definir une semantique claire:
  - soit `refresh` est incremental et conserve optimistics;
  - soit un nouveau mode snapshot remplace la view cible en conservant uniquement les optimistic locaux.

### Tickets

Socket ACK:

- `ticket.verification.completed_ack`

`wsListenerFactory` mappe via `mapWsTicketCompletedAck`, puis dispatch:

- `onTicketConfirmedAck`
- `onTicketRejectedAck`

`ackTicketsListenerFactory` dispatch ensuite:

- confirmed: `ticketReconciledConfirmed`, `userBadgeProgressUpdated`, `dropCommitted`
- rejected: `ticketReconciledRejected`, `dropCommitted`

State modifie:

- `ticketWl.byId[ticketId]`
- `userWl` badge progress
- `outboxWl`

Read endpoint backend existant:

- `GET /api/tickets/{ticketId}/status`

Projection existante:

- `ticket_status_projection`

Probleme mobile:

- `HttpTicketsGateway` ne contient aujourd'hui que `verify`.
- `ticketRetrieval` n'appelle pas de gateway read reel; il simule un resultat ou hydrate un `ANALYZING` no-op.
- Pour migrer tickets, il faut ajouter un port/gateway read mobile vers `/api/tickets/{ticketId}/status`.

Probleme backend:

- Le WebSocket ACK ticket embarque `payloadJson` complet du Domain Event. C'est contraire a la trajectoire cible.

### Entitlements

Pas d'ACK socket direct.

`ackEntitlementsListener` ecoute `onTicketConfirmedAck` et dispatch:

- `entitlementsHydrated`

State modifie:

- `entitlementWl.byUser[userId]`

Read endpoint actuel:

- Aucun endpoint backend produit identifie.
- Le mobile utilise `FakeEntitlementWlGateway`.

Conclusion:

- Entitlements ne peuvent pas etre migres proprement vers SSE + GET sans creer un read model/endpoint produit.
- En attendant, ne pas les presenter comme coherence backend; ce sont des droits calcules localement a partir d'ACK ticket.

### Articles

Pas de dependance STOMP identifiee.

Read endpoints:

- `GET /api/articles?locale=&limit=&cursor=`
- `GET /api/articles/{slug}?locale=`

Projection:

- `articles_projection`

Manque pour SSE:

- `ArticleCreatedEventHandler` met a jour la projection mais ne publie pas `ProjectionSyncEvent`.

### Coffees

Pas de dependance STOMP mobile identifiee.

Read endpoints:

- `GET /api/coffees`
- `GET /api/coffees/photos`
- `GET /api/coffees/opening-hours`

Projection sync:

- Deja en place cote backend avec `projection.updated projection="coffees"` et hints `summary`, `photos`, `openingHours`, etc.

Probleme mobile:

- Les reducers cafes/photos/horaires sont encore partiellement merge/upsert. Pour SSE mobile, ils doivent tendre vers snapshot replace.

## Inventaire Backend STOMP / WebSocket / SockJS

### Configuration

- `sharedKernel/adapters/primary/springboot/configuration/webSocket/WebSocketConfig.java`
  - `@EnableWebSocketMessageBroker`
  - endpoint `/ws`
  - `.withSockJS()`
  - `setAllowedOriginPatterns("*")`
  - broker `/topic`, `/queue`
  - user destination `/user`
- `JwtStompChannelInterceptor.java`
  - lit le header STOMP `Authorization`;
  - decode JWT;
  - fixe `Principal.getName()` avec le user id.
- `WsAckEnvelope.java`
  - record ACK generique, peu utilise directement.

### Sender ACK

- `platform/eventing/WebSocketOutboxEventSender.java`
  - implemente `ClientAckOutboxEventSender`;
  - construit des ACK depuis les `OutboxEventJpaEntity`;
  - envoie vers `/user/queue/acks` si `streamKey=user:{userId}`;
  - fallback broadcast vers `/topic/{streamKey}`.

### Integration avec outbox sender

- `StableEnvelopeOutboxEventSender`
  - envoie event bus local si configure;
  - publie integration messages SQS;
  - appelle toujours `sendWebSocketBestEffort(event)`.

- `RoutingOutboxEventSender`
  - legacy Kafka/local routing;
  - peut envoyer vers `webSocketSender` selon `routing.sendToWebSocket()`.

Conclusion:

- La suppression finale devra retirer `ClientAckOutboxEventSender` du sender principal, pas seulement supprimer le controller WebSocket.

## Events Backend Utilises pour Produire les ACK

`WebSocketOutboxEventSender` transforme:

- `LikeSetEvent` -> `social.like.added_ack` ou `social.like.removed_ack`
- `CommentCreatedEvent` -> `social.comment.created_ack`
- `CommentUpdatedEvent` -> `social.comment.updated_ack`
- `CommentDeletedEvent` -> `social.comment.deleted_ack`
- `TicketVerifyAcceptedEvent` -> `ticket.verify.accepted_ack`
- `TicketVerificationCompletedEvent` -> `ticket.verification.completed_ack`
- fallback -> `unknown` avec payload brut

Risque:

- Ce sender est un convertisseur Domain Event -> frontend event. C'est precisement le couplage que la cible interdit.
- Le fallback `unknown` expose potentiellement des payloads bruts.

## Equivalents Projection Sync Existants / Manquants

### Existant

- Infrastructure SSE:
  - `ProjectionSyncEvent`
  - `ProjectionSyncPublisher`
  - `projection_sync_events`
  - `ProjectionSyncDispatcher`
  - `GET /api/sync/events`
  - `GET /api/admin/sync/events`
- Coffee projection sync:
  - `CoffeeCreatedEventHandler`
  - `CoffeePhotosImportedEventHandler`
  - `CoffeeOpeningHoursImportedEventHandler`
  - delete/archive/photo added/photo deleted handlers

### Manquant

- Social comments: projection handlers ne publient pas encore `ProjectionSyncEvent`.
- Likes: pas de vraie projection likes exploitee par le GET mobile; le query lit le write repository.
- Tickets: projection handlers ne publient pas encore `ProjectionSyncEvent`.
- Articles: projection handler ne publie pas encore `ProjectionSyncEvent`.
- Entitlements: pas de read model/endpoint produit identifie.

## Matrice Workflow Actuel -> Workflow Cible

| Domaine | Commande HTTP actuelle | ACK socket actuel | Action Redux depuis ACK | State modifie | Projection cible | SSE cible | GET cible |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Comments create | `POST /api/social/comments` | `social.comment.created_ack` | `onCommentCreatedAck` -> `createReconciled` + `dropCommitted` | comments + outbox | `social_comments_projection` | `projection.updated projection="comments" scope="target" entityId=targetId hints:["created"]` | `GET /api/social/comments?targetId=...&op=refresh` |
| Comments update | `PUT /api/social/comments` | `social.comment.updated_ack` | `onCommentUpdatedAck` -> `updateReconciled` + `dropCommitted` | comments + outbox | `social_comments_projection` | `projection.updated projection="comments" scope="target" entityId=targetId hints:["updated"]` | `GET /api/social/comments?targetId=...&op=refresh` |
| Comments delete | `DELETE /api/social/comments` | `social.comment.deleted_ack` | `onCommentDeletedAck` -> `deleteReconciled` + `dropCommitted` | comments + outbox | `social_comments_projection` | `projection.updated projection="comments" scope="target" entityId=targetId hints:["deleted"]` | `GET /api/social/comments?targetId=...&op=refresh` |
| Like add | `POST /api/social/likes` value true | `social.like.added_ack` | `onLikeAddedAck` -> `likeReconciled` + `likeSyncAcked` + `dropCommitted` | likes + outbox | likes read model a creer/aliger | `projection.updated projection="likes" scope="target" entityId=targetId hints:["set"]` | `GET /api/social/targets/{targetId}/likes` |
| Like remove | `POST /api/social/likes` value false | `social.like.removed_ack` | `onLikeRemovedAck` -> `likeReconciled` + `likeSyncAcked` + `dropCommitted` | likes + outbox | likes read model a creer/aligner | `projection.updated projection="likes" scope="target" entityId=targetId hints:["set"]` | `GET /api/social/targets/{targetId}/likes` |
| Ticket verify | `POST /api/tickets/verify` | `ticket.verification.completed_ack` | `onTicketConfirmedAck`/`onTicketRejectedAck` -> ticket reconcile + badge progress + `dropCommitted` | tickets + user badges + outbox | `ticket_status_projection` | `projection.updated projection="tickets" scope="entity" entityId=ticketId hints:["status"]` | `GET /api/tickets/{ticketId}/status` |
| Entitlements | derive ticket ACK | aucun ACK propre | `ackEntitlementsListener` ecoute ticket confirmed | entitlements | manquant | `projection.updated projection="entitlements" scope="user" entityId=userId hints:["ticketConfirmed"]` | endpoint a creer, ex `GET /api/users/me/entitlements` |
| Articles | n/a | aucun | n/a | articles | `articles_projection` | `projection.updated projection="articles" scope="collection/entity" entityId=articleId hints:["created"]` | `GET /api/articles` ou `GET /api/articles/{slug}` |
| Coffees | admin/studio, pas mobile socket | aucun | n/a | coffees/photos/hours | deja present | deja present `projection="coffees"` | `GET /api/coffees`, `/photos`, `/opening-hours` |

## Reducers Read Models: Merge vs Snapshot Replace

### Merge / Upsert actuellement

- `coffeeWlReducer.coffeesHydrated`: upsert, ne supprime pas les cafes absents.
- `cfPhotoReducer.photosHydrated`: merge par URI, ne supprime pas les photos absentes.
- `commentWlReducer.commentsRetrieved`: upsert + merge ids selon op, conserve les optimistic.
- `openingHoursReducer.openingHoursHydrated`: merge descriptions.

### Plus proche de snapshot

- `likeWlReducer.likesRetrieved`: remplace l'agregat d'un target.
- `openingHoursReducer.hoursHydrated`: remplace les windows parsees par cafe pour les IDs presents.
- `ticketRetrieved`: upsert ticket par id.

Recommandation:

- Avant de brancher SSE mobile largement, definir une convention:
  - GET collection complet -> snapshot replace;
  - GET target (`comments?targetId`) -> replace view cible + conserver optimistic locaux;
  - GET entity (`ticket status`, `likes status`) -> replace entity/agregat;
  - SSE ne mute jamais directement ces stores.

## Tests qui Dependent de STOMP / ACK

### Mobile

Tests directs:

- `tests/core-logic/contextWl/commentWl/usecases/read/ackComments.spec.ts`
- `tests/core-logic/contextWl/likeWl/usecases/read/ackLike.spec.ts`
- `tests/core-logic/contextWl/ticketWl/usecases/read/ackTicket.spec.ts`
- `tests/core-logic/contextWl/ticketWl/usecases/read/ack.ticket-badge.integration.spec.ts`
- `tests/core-logic/contextWl/entitlementWl/usecases/read/ackEntitlement.spec.ts`

Tests runtime/outbox indirects:

- `tests/core-logic/contextWl/appWl/usecases/runtimeListener.spec.ts`
  - attend `wsEnsureConnectedRequested` / `wsDisconnectRequested`.
- `tests/core-logic/contextWl/outboxWl/offlineLikeCommandPolling.spec.ts`
  - scenario "no socket -> /commands polling".

Code documentaire/tests a adapter:

- README/flows `commentFlow.mmd`, `ticketFlow.mmd`, `entitlementFlow.mmd`, `like.toggle.org.schema.mmd`.
- `sync_PARKING` si conserve dans repo.

### Backend

Tests directs:

- `socialContextTest/unit/WebSocketOutboxEventSenderTest`
- `socialContextTest/endtoend/.../SocialLikeOutboxRoutingIT`
- `socialContextTest/endtoend/.../SocialCommentOutboxRoutingIT`
- `sharedKernel/eventing/StableEnvelopeOutboxEventSenderTest`

Ces tests devront etre remplaces par:

- projection handler publishes `ProjectionSyncEvent`;
- outbox sender does not call WebSocket;
- command status still transitions correctly;
- SQS projection pipeline remains intact.

## Risques

### Risques immediats

- Supprimer STOMP sans remplacer les reconciliations ferait rester des commandes en `awaitingAck` jusqu'au watchdog.
- Le watchdog `APPLIED` droppe actuellement l'outbox mais ne refresh pas le read model concerne.
- Tickets ne peuvent pas etre migres sans gateway read mobile.
- Entitlements ne peuvent pas etre migres sans read model backend.
- Likes n'ont pas un read model CQRS propre.
- Certains reducers merge ne permettent pas de reflechir les suppressions apres GET.

### Risques architecturaux

- Faire du SSE qui dispatch `onLikeAddedAck` ou `onCommentCreatedAck` recreerait le mauvais pattern.
- Lire les Domain Events dans le dispatcher SSE violerait la cible.
- Publier SSE avant update projection casserait l'invariant "projection maintenant coherente".
- Migrer tous les domaines en une PR rendrait les regressions outbox/mobile difficiles a isoler.

## Ordre de Migration Recommande

### 1. Comments comme pilote

Pourquoi:

- Projection read existe.
- Endpoint GET existe.
- Les actions create/update/delete sont bien isolees.
- Le read model est par `targetId`, donc mapping SSE simple.

Travail:

- Backend: apres `projectionRepository.apply(event)`, publier `projection.updated projection="comments" scope="target" entityId=targetId`.
- Mobile: listener SSE recoit `comments`, dispatch `commentRetrieval({ targetId, op:"refresh" })`.
- Mobile: ne plus utiliser ACK socket comments pour reconciliation UI; conserver `/commands` pour drop/rollback.
- Tests: projection sync event + listener SSE -> GET.

Point a trancher:

- `commentsRetrieved(op=refresh)` doit-il retirer les commentaires absents? Pour un vrai snapshot cible, oui, sauf optimistic locaux.

### 2. Tickets

Pourquoi:

- Projection `ticket_status_projection` existe.
- Endpoint backend status existe.

Travail:

- Backend: publier `projection.updated projection="tickets" scope="entity" entityId=ticketId`.
- Mobile: ajouter `tickets.getStatus(ticketId)` dans gateway.
- Mobile: SSE ticket -> `ticketRetrieval(ticketId)` reel.
- Mobile: entitlements ne doivent plus etre alimentes par ACK ticket; soit attendre leur propre projection, soit recalculer uniquement depuis read model ticket si assumee temporairement.

### 3. Likes

Pourquoi pas premier:

- Le GET existe mais lit le write repository.
- Il faut aligner avec CQRS avant de le presenter comme projection freshness.

Travail:

- Creer/brancher une projection likes ou expliciter une projection cible.
- Backend: handler `LikeSetEvent` cote read, puis `projection.updated projection="likes" scope="target" entityId=targetId`.
- Mobile: SSE likes -> `likesRetrieval({ targetId })`.

### 4. Entitlements

Pourquoi tard:

- Pas de backend read model produit.
- Mobile fake gateway.

Travail:

- Definir le bounded context proprietaire.
- Creer read model/endpoint.
- Publier `projection.updated projection="entitlements" scope="user" entityId=userId`.
- Mobile: SSE entitlements -> GET entitlements.

### 5. Articles / autres

Pas bloque par STOMP. Ajouter Projection Sync seulement si le besoin de freshness mobile est reel.

## Code a Supprimer a la Fin

### Mobile

- `@stomp/stompjs` du `package.json`.
- `sockjs-client` du `package.json`.
- `app/adapters/primary/socket/WsEventsGateway.ts`
- `app/adapters/primary/socket/ws.gateway.ts`
- `app/adapters/primary/socket/ws.type.ts`
- `app/core-logic/contextWL/wsWl/*`
- imports/wiring `ws` dans infrastructure/listeners/runtime.
- duplications `WsStompEventsGateway` dans `appWl/typeAction` et `appWl/reducer`.
- ACK listener factories devenus inutiles:
  - `ackReceivedBySocket.ts`
  - `ackLike.ts`
  - `ackTicket.ts`
  - `ackEntitlement.ts` sous forme actuelle
- tests ACK socket obsoletes.
- `sync_PARKING` et types `Sync*Ack`.

Attention: ne supprimer les actions de reconciliation qu'apres remplacement par:

- command status drop/rollback;
- refresh GET via SSE;
- reducers snapshot.

### Backend

- dependency Spring WebSocket/STOMP dans `pom.xml`.
- `WebSocketConfig`
- `JwtStompChannelInterceptor`
- `WsAckEnvelope`
- `WebSocketOutboxEventSender`
- `ClientAckOutboxEventSender`
- injection `webSocketSender` dans `StableEnvelopeOutboxEventSender`.
- chemin WebSocket dans `RoutingOutboxEventSender`.
- tests WebSocketOutboxEventSender et outbox routing vers WebSocket.
- documentation sharedKernel mentionnant WebSocket sender.

## Endpoints GET Necessaires

Existants:

- `GET /api/social/comments?targetId=:targetId&op=refresh&limit=...`
- `GET /api/social/targets/{targetId}/likes`
- `GET /api/tickets/{ticketId}/status`
- `GET /api/articles`
- `GET /api/articles/{slug}`
- `GET /api/coffees`
- `GET /api/coffees/photos`
- `GET /api/coffees/opening-hours`
- `GET /commands/{commandId}`

A creer/renforcer:

- Mobile ticket gateway vers `GET /api/tickets/{ticketId}/status`.
- Entitlements endpoint produit.
- Eventuellement endpoint likes projection-backed si on ne veut plus lire le write repository.
- Eventuellement endpoint comments snapshot strict par target si `op=refresh` reste incremental.

## Tests a Ajouter

### Backend par domaine

- Projection handler applique projection puis publie `ProjectionSyncEvent`.
- L'event SSE contient:
  - `eventName="projection.updated"`;
  - `projection`;
  - `scope`;
  - `entityId`;
  - `version`;
  - `hints`.
- Pas de Domain Event dans payload SSE.
- Consumer SQS -> projection -> projection sync.
- Command status reste `APPLIED/REJECTED` independamment de SSE.

### Mobile par domaine

- SSE `projection.updated` ignore heartbeats/connected.
- `projection="comments"` dispatch `commentRetrieval`.
- `projection="likes"` dispatch `likesRetrieval`.
- `projection="tickets"` dispatch `ticketRetrieval`.
- Le listener SSE ne modifie jamais directement les stores read.
- Reducers snapshot:
  - comments refresh conserve optimistic locaux mais remplace serveur;
  - likes remplace target;
  - ticket remplace entity;
  - photos/cafes/hours idem pour les futurs cafes mobile SSE.
- Watchdog `APPLIED` ne depend pas du socket.

### Tests de suppression finale

- Aucun import `@stomp/stompjs`.
- Aucun import `sockjs-client`.
- Aucun `/ws` dans app mobile.
- Aucun `SimpMessagingTemplate` backend.
- Aucun `@EnableWebSocketMessageBroker`.
- Aucun `WebSocketOutboxEventSender`.

## Plan Phase 2 Propose

1. Formaliser un `projectionSyncWl` mobile generique:
   - gateway SSE;
   - Last-Event-ID;
   - reconnect/backoff;
   - Redux listener de routing par projection;
   - aucun lien vers ACK actions.

2. Migrer comments:
   - backend projection sync;
   - mobile SSE -> comments GET;
   - tests;
   - retirer usage ACK comments du listener WS.

3. Migrer tickets:
   - backend projection sync;
   - mobile ticket GET reel;
   - tests;
   - retirer ACK tickets.

4. Migrer likes:
   - corriger read model backend;
   - backend projection sync;
   - mobile SSE -> likes GET;
   - tests;
   - retirer ACK likes.

5. Definir entitlements:
   - read model + endpoint;
   - SSE;
   - mobile GET;
   - retirer derivation depuis ACK ticket.

6. Supprimer STOMP complet:
   - mobile deps/code/tests/docs;
   - backend config/sender/tests/docs;
   - verifier build backend/mobile.

## Recommendation Finale Phase 1

Le domaine pilote devrait etre `comments`, pas `likes`.

Raison:

- comments ont deja une projection read explicite;
- les handlers projection sont simples;
- l'endpoint GET existe et est deja utilise;
- la migration expose vite les vrais sujets mobiles: refresh par target, preservation des optimistic, drop outbox par command status.

`likes` semble plus simple en UI, mais il cache une dette CQRS backend: le read handler lit le write repository. Si on le migre en premier, on risque de valider une mauvaise frontiere.

