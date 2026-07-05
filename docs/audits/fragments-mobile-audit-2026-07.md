# Audit mobile Fragments - Juillet 2026

Date: 2026-07-05  
Scope: `/Users/nicolasmaldiney/fragmentsCleanFront` lu depuis le backend `/Users/nicolasmaldiney/fragmentsClean`  
Mode: audit uniquement, aucune modification du backend, de Studio ou du mobile.

## Executive Summary

L'app mobile Fragments a une base client plus solide qu'une simple app React Native CRUD: elle a des contexts `*Wl`, Redux Toolkit, listener middleware, une outbox offline-first, un watchdog via `/commands/{commandId}`, des gateways HTTP explicites, des fakes et des tests de core-logic. C'est cohérent avec la doctrine Fragments: le mobile possede l'experience, le backend reste source de verite, et les sockets ne doivent pas etre la source de verite.

Le risque principal avant App Store n'est pas le backend: les endpoints publics mobiles principaux existent et sont globalement compatibles avec staging. Le risque est cote client: boot trop fragile, refresh read models insuffisant au foreground, reducers de read models qui fusionnent des snapshots au lieu de les remplacer, STOMP duplique dans des fichiers de core/reducer, logs trop bavards, permissions iOS incompletes pour le scan ticket, et absence de tests de contrats HTTP reels.

Position sur STOMP vs SSE: il ne faut pas utiliser STOMP comme mecanisme de "readiness" des read models. Dans le code actuel, STOMP sert surtout aux ACK opportunistes de commandes (`/user/queue/acks`), ce qui est acceptable si `/commands/{commandId}` reste canonique. Pour la fraicheur des projections, la bonne cible est le meme modele que Studio: `projection.updated` via SSE, puis Redux listener, puis GET. Pour la premiere release App Store, je recommande toutefois de ne pas brancher SSE immediatement: stabiliser d'abord boot + foreground refresh + reconciliation par GET. Ensuite seulement, ajouter `/api/sync/events` mobile comme optimisation de freshness.

## Architecture Actuelle

Le repo mobile suit une architecture client explicite:

- `app/core-logic/contextWL/*`: contexts client (`coffeeWl`, `cfPhotosWl`, `openingHoursWl`, `likeWl`, `commentWl`, `ticketWl`, `outboxWl`, `wsWl`, `appWl`, etc.).
- `app/adapters/primary/react`: ecrans, navigation, bootstrap React.
- `app/adapters/secondary/gateways`: gateways HTTP, auth, location, fakes.
- `app/adapters/secondary/viewModel`: hooks de view model (`useCafeFull`, `useCoffeesForMarkers`, etc.).
- Redux Toolkit + listener middleware pilotent les workflows.

Ce qui est sain:

- Les ecrans ne font pas directement les appels HTTP principaux.
- Les hooks view model lisent Redux via selectors.
- L'outbox est separee des ecrans et testee.
- Les commandes offline ont un watchdog qui interroge `/commands/{commandId}`.
- Les gateways sont remplaçables par fakes.

Ce qui est fragile:

- `AppStateWl` decrit des cles (`coffees`, `cfPhotos`, `openingHours`) mais le store reel monte des cles alias (`cfState`, `pState`, `ohState`, etc.). Les selectors utilisent les alias. Cela fonctionne mais affaiblit le typage et rend le store difficile a raisonner.
- `serializableCheck` est desactive, ce qui masque possiblement des erreurs de payloads Redux.
- `WsStompEventsGateway` existe dans l'adapter primaire, mais aussi duplique dans `appWl/typeAction/appWl.type.ts` et `appWl/reducer/app.reducer.ts`. Avoir un client STOMP dans un reducer/core type est une violation nette de frontiere.
- Les gateways HTTP sont par contexte, ce qui est acceptable, mais aucune convention ne centralise la gestion d'erreurs HTTP/token/retry.

## Connexion Backend Staging

Configuration:

- `app.config.js` lit `EXPO_PUBLIC_API_BASE_URL` ou `API_BASE_URL`.
- En production, `app.config.js` echoue si `EXPO_PUBLIC_API_BASE_URL` est absent.
- `app.json` garde un fallback `extra.apiBaseUrl = http://localhost:8080`.
- `WS_URL` est derive de `API_BASE_URL` par remplacement `http -> ws`, donc `https://...` devient `wss://.../ws`.

Endpoints consommes:

- Auth: `POST /auth/google/exchange`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`.
- Cafes: `GET /api/coffees`, `GET /api/coffees/{id}`, `GET /api/coffees/photos`, `GET /api/coffees/opening-hours`.
- Articles: `GET /api/articles`, `GET /api/articles/{slug}?locale=...`.
- Social: `GET /api/social/targets/{targetId}/likes`, `POST /api/social/likes`, `GET/POST/PUT/DELETE /api/social/comments`.
- Tickets: `POST /api/tickets/verify`, `GET /api/tickets/{ticketId}/status`.
- Command status: `GET /commands/{commandId}`.
- STOMP: `/ws`, subscription `/user/queue/acks`.

Compatibilite backend staging:

- `GET /api/coffees`, `/photos`, `/opening-hours` existent et correspondent globalement aux DTOs mobiles.
- Les photos S3 sont exposees au mobile par `photoUri`, donc le mobile n'a pas a connaitre S3.
- Les horaires importes sont lisibles via `/api/coffees/opening-hours`.
- `GET /api/coffees/{id}` est appele par le mobile mais je n'ai pas trouve de controller public correspondant cote backend. Si cet appel est utilise par un ecran detail ou un flow futur, il retournera probablement 404/401 selon la security chain. Aujourd'hui les details peuvent etre reconstruits depuis la liste globale + photos + horaires.
- `/api/sync/events` existe cote backend et est protege par auth utilisateur; il ne doit pas etre confondu avec `/api/admin/sync/events`.
- `/ws` existe avec STOMP/SockJS, mais `setAllowedOriginPatterns("*")` est trop large pour un durcissement production.

HTTPS/certificat:

- CORS n'est pas le sujet principal pour mobile natif.
- Il faut valider la chaine TLS staging/prod sur device reel iOS/Android, pas seulement simulateur.

## Read Models Mobiles

### Cafes

`HttpCoffeeGateway.getAllSummaries()` lit `GET /api/coffees` et hydrate `coffeeWl` via `coffeesHydrated`.

Probleme: le reducer fait un upsert. Si un cafe est archive/supprime cote backend, un refresh de la liste ne le retire pas du state. Pour un GET snapshot, le reducer doit soit remplacer le snapshot, soit recevoir des tombstones.

### Photos

`HttpCfPhotoGateway.getAllphotos()` lit `GET /api/coffees/photos` et mappe:

- backend `coffeeId` -> mobile `coffee_id`
- backend `photoUri` -> mobile `photo_uri`

Probleme critique: `cfPhotoReducer` fusionne les photos et ne supprime jamais les photos absentes du nouveau snapshot. Une suppression de photo cote backend peut rester visible apres refresh.

Autre point: `selectCoffeeFullVM` injecte une photo Unsplash par defaut quand aucune photo n'existe. C'est un artifice UI dans un selector metier client. Cela peut masquer des donnees absentes et donner l'impression qu'un cafe a une photo durable alors que non.

### Horaires

`HttpOpeningHoursGateway.getAllOpeningHours()` lit `GET /api/coffees/opening-hours`.

Le reducer a deux chemins:

- `openingHoursHydrated`: fusionne des descriptions.
- `hoursHydrated`: regroupe et remplace les windows parsees par cafe.

Il faut clarifier quelle action est l'API officielle de snapshot. Le comportement de remplacement est preferable pour les GET.

### Articles

`HttpArticleWlGateway` lit `GET /api/articles` et `GET /api/articles/{slug}?locale=...`. Le contrat semble compatible avec le backend `ArticleListView`.

### Likes / Comments / Tickets

Ces contexts passent par auth Bearer et outbox. Le principe est sain: mutation optimiste, envoi HTTP, attente ACK opportuniste, fallback `/commands/{commandId}`.

Risque: `processOutbox` classe actuellement `401/403` comme rejet explicite et peut rollback. Pour une app offline-first, `401` peut aussi signifier token expire/session a rafraichir. Il faut distinguer business rejection de auth failure technique.

### Entitlements

Le wiring utilise encore `FakeEntitlementWlGateway`. Si les droits/gamification/tickets conditionnent des parcours App Store, ce n'est pas pret pour une release produit.

## Synchronisation: STOMP, Lifecycle Refresh, SSE

### Ce que fait STOMP aujourd'hui

Le mobile connecte `WsStompEventsGateway` a `/ws` et subscribe `/user/queue/acks`.

Events routes:

- `social.like.added_ack`
- `social.like.removed_ack`
- `social.comment.created_ack`
- `social.comment.updated_ack`
- `social.comment.deleted_ack`
- `ticket.verification.completed_ack`

Donc STOMP sert a accelerer la reconciliation de commandes. Ce n'est pas un flux de projection sync.

Cette utilisation est acceptable si elle reste opportuniste:

- le socket peut tomber;
- l'ACK peut manquer;
- `/commands/{commandId}` reste canonique;
- aucun read model ne depend de STOMP.

### Pertinence de STOMP pour la readiness des read models

Je ne recommande pas de persister un pattern "readiness du read model" via STOMP. Il melangerait deux langages:

- ACK commande: "ta commande a ete appliquee/rejetee";
- Projection sync: "un read model est maintenant coherent".

Ces deux choses ne doivent pas etre portees par le meme contrat. C'est precisement ce que l'architecture backend vient de separer avec Projection Sync.

### Strategie recommandee pour App Store v1

Pour la premiere release:

1. Garder STOMP uniquement pour ACK opportunistes si le backend staging le supporte correctement.
2. Garder `/commands/{commandId}` comme reconciliation obligatoire.
3. Ajouter/renforcer refresh lifecycle:
   - boot;
   - retour foreground;
   - retour online;
   - apres command status `APPLIED` quand l'ecran depend du read model.
4. Les refresh doivent refaire des GET et remplacer les snapshots pertinents.

Cela suffit pour une release sans introduire un nouveau flux mobile SSE a valider.

### Strategie cible apres v1

Ajouter un `projectionSyncWl` mobile:

```text
/api/sync/events
-> projection.updated
-> Redux listener
-> dispatch coffee/photos/openingHours/articles refresh requested
-> GET read model
-> reducers snapshot replace
-> selectors
-> React
```

Regles:

- Le mobile ne reçoit jamais `CoffeeCreatedEvent`, `CoffeePhotoStoredEvent`, etc.
- Le mobile ne modifie jamais directement les stores depuis SSE.
- SSE ne remplace pas `/commands/{commandId}` pour les writes offline.
- L'admin route `/api/admin/sync/events` ne doit jamais etre utilisee par le mobile.

## Boot Sequence

`AppBootstrap` fait:

1. monte NetInfo et AppState;
2. initialise l'auth;
3. rehydrate l'outbox;
4. kick outbox si online/signed-in;
5. demande localisation;
6. lit cafes, photos, horaires;
7. lit entitlements si user.

Risques:

- La localisation est demandee au boot. Apple prefere une demande contextualisee, au moment ou l'utilisateur comprend la valeur.
- `coffeeGlobalRetrieval` peut faire echouer le boot complet si `/api/coffees` echoue.
- Photos/horaires retombent sur liste vide, mais cafes pas pareil.
- Le foreground refresh hydrate auth/user/outbox/WS, mais pas cafes/photos/horaires/articles.

Recommandation:

- Ne pas bloquer l'app sur la lecture des cafes; afficher empty/error state et retry.
- Ajouter un listener lifecycle pour les read models publics avec debounce/TTL.
- Ne demander la localisation qu'a l'entree map ou via action utilisateur claire.

## Donnees Cafes

L'app peut afficher les cafes importes depuis Studio si:

- `/api/coffees` expose bien les summaries publiques;
- `/api/coffees/photos` expose des URLs signees valides;
- `/api/coffees/opening-hours` expose les horaires;
- les reducers remplacent correctement les snapshots.

Actuellement:

- cafes: affichage possible;
- photos: affichage possible, mais suppression serveur non fiable cote state;
- horaires: affichage possible, mais deux chemins reducers a clarifier;
- coordonnees: presentes via `location.lat/lon`;
- tags: supportes;
- details: composes par selectors.

Point a corriger: `PhotosSection` sait afficher "Aucune photo", mais le selector `selectCoffeeFullVM` force une photo fallback. Le fallback doit etre un choix UI local, pas une donnee du VM.

## Auth et Securite

Points solides:

- Session stockee via Expo SecureStore.
- Backend exchange Google centralise.
- Les commandes authentifiees passent par Bearer.

Fragilites:

- Google client IDs sont hardcodes dans `googleOAuthGateway.ts` alors que le reste de la config passe par Expo config. Ce ne sont pas des secrets, mais ce sont des parametres d'environnement produit.
- Certains logs utilisent encore `console.log`, par exemple refresh/auth et gateways. Le logger redige seulement les strings Bearer, pas les objets complexes.
- `logger.warn/error` loggue aussi hors `__DEV__`; c'est acceptable pour erreurs techniques mais il faut eviter payloads complets et tokens.
- WebSocket backend autorise toutes les origins.
- `401/403` en outbox sont traites comme rejection business potentielle; il faut passer par refresh token/retry avant rollback.

## Qualite App Store

### Permissions

Declare:

- iOS: `NSLocationWhenInUseUsageDescription`
- Android: `ACCESS_FINE_LOCATION`

Utilise:

- localisation;
- camera via `expo-image-picker` dans `useScanTicketScreenVM`;
- OCR via `react-native-text-recognition`;
- SecureStore;
- Google Sign-In.

Manque probable:

- `NSCameraUsageDescription` si le scan ticket reste accessible.
- Eventuellement `NSPhotoLibraryUsageDescription` si une selection library est ajoutee ou deja exposee.
- Revue precise des permissions Android camera si le scan ticket est dans la build.

### UX et robustesse

Risques App Store / beta:

- demander la localisation au boot peut surprendre;
- boot global peut echouer sur incident reseau;
- fake entitlements peuvent creer des parcours incoherents;
- absence d'etats "retry" coherents pour certains read models;
- STOMP reconnect/logs a tester sur vrai device en background/foreground.

### Build config

`eas.json` est minimal. C'est acceptable, mais avant soumission il faut documenter:

- variables EAS par profil;
- `EXPO_PUBLIC_API_BASE_URL`;
- Google Sign-In config iOS;
- bundle id final;
- icons/splash assets;
- privacy answers App Store Connect.

## Tests

Ce qui existe:

- tests core-logic par context;
- reducers/usecases outbox;
- process outbox;
- watchdog command status;
- runtime listener;
- auth flow;
- likes/comments/tickets;
- coffee/photos/openingHours retrieval avec fakes.

Ce qui manque:

- tests de gateways HTTP avec fetch mocke pour chaque endpoint staging;
- tests de mapping DTO backend -> types mobiles;
- tests du cas 401 token expire -> refresh/retry, pas rollback immediat;
- tests des reducers snapshot replace, en particulier suppression cafe/photo;
- tests AppState foreground -> refresh cafes/photos/hours;
- tests STOMP gateway connect/reconnect/deconnect sans logger de payload;
- tests de boot degrade: `/api/coffees` en erreur ne bloque pas toute l'app;
- tests UI/navigation critiques;
- tests sur device ou E2E minimal App Store smoke.

Je n'ai pas lance les tests: demande d'audit uniquement et repo mobile lu sans modification.

## Problemes Critiques

### P0 - Avant test App Store contre staging

1. Corriger la semantique des read reducers pour les snapshots:
   - `coffeesHydrated` doit pouvoir remplacer la liste ou gerer tombstones.
   - `photosHydrated` doit remplacer l'ensemble des photos par cafe ou tout le snapshot.
   - horaires doivent utiliser un chemin unique de snapshot.

2. Ajouter refresh read models au lifecycle:
   - boot;
   - app foreground;
   - retour online;
   - eventuellement apres command `APPLIED`.

3. Clarifier `GET /api/coffees/{id}`:
   - soit ajouter un endpoint backend public si necessaire;
   - soit supprimer l'appel mobile et lire le detail depuis le snapshot.

4. Retirer les duplications STOMP dans `appWl/typeAction` et `appWl/reducer`.

5. Ne pas rollback sur `401/403` sans tentative auth refresh/retry.

6. Mettre les client IDs Google dans la config Expo, pas dans le gateway.

7. Ajouter les permissions iOS/Android necessaires au scan ticket ou retirer le parcours de la build.

8. Nettoyer les logs console/payloads avant build release.

### P1 - Avant soumission Apple

1. Stabiliser les erreurs HTTP avec une erreur typee (`status`, `kind`, `retryable`).
2. Ajouter tests de contrats HTTP pour tous les gateways.
3. Ajouter tests boot degrade et foreground refresh.
4. Verifier `/ws` sur device reel et durcir origin backend.
5. Remplacer le fallback photo dans selector par un fallback purement UI.
6. Revoir demande de localisation: demande contextualisee dans la map.
7. Remplacer `FakeEntitlementWlGateway` si les entitlements sont visibles produit.
8. Documenter EAS/staging/prod env.

### P2 - Apres premiere release

1. Introduire `projectionSyncWl` SSE mobile sur `/api/sync/events`.
2. Ajouter cache read models durable offline avec TTL/versions.
3. Ajouter pagination/geobox cote cafe si le catalogue grandit.
4. Ajouter E2E Detox/Maestro minimal.
5. Ajouter observabilite crash/reporting si conforme privacy.

## Plan de Reconnexion au Backend

Phase 1 - Sans SSE:

```text
Boot
-> GET /api/coffees
-> GET /api/coffees/photos
-> GET /api/coffees/opening-hours
-> snapshot replace
-> selectors
-> React
```

```text
Foreground / online
-> debounce + TTL
-> refresh read models publics
-> snapshot replace
```

```text
Write offline
-> optimistic reducer
-> outbox
-> HTTP command
-> awaiting ACK
-> STOMP ACK si disponible
-> /commands/{commandId} fallback
-> refresh affected read model on APPLIED
```

Phase 2 - Avec SSE:

```text
/api/sync/events
-> projection.updated(projection="coffees", hints=["photos"])
-> listener Redux
-> dispatch coffee/photos/hours refresh requested
-> GET
-> snapshot replace
```

Le SSE doit rester un signal de fraicheur, pas un transport de donnees metier.

## Plan de Test

Minimum avant build interne:

- `npm test` complet.
- Tests gateway:
  - `/api/coffees`;
  - `/api/coffees/photos`;
  - `/api/coffees/opening-hours`;
  - `/auth/google/exchange`;
  - `/commands/{commandId}`.
- Tests reducers snapshot:
  - cafe disparu du backend disparait du state;
  - photo supprimee disparait;
  - horaires remplaces.
- Tests runtime:
  - foreground online declenche refresh read models;
  - offline ne casse pas le boot;
  - auth 401 declenche refresh/retry.
- Test device:
  - Google sign-in;
  - boot staging HTTPS;
  - carte + localisation;
  - details cafe avec photo S3 signee;
  - like/comment/ticket outbox;
  - background/foreground.

## Recommandation STOMP vs SSE

Decision recommandee:

- Conserver STOMP a court terme uniquement comme canal d'ACK opportuniste des commandes.
- Ne pas ameliorer STOMP pour la readiness des read models.
- Ne pas brancher SSE mobile tant que les snapshots/read reducers ne sont pas corrects.
- Apres stabilisation App Store v1, ajouter SSE mobile via un nouveau `projectionSyncWl`, calque sur Studio mais avec auth utilisateur et `/api/sync/events`.

Raison:

- STOMP ACK et Projection Sync ne parlent pas le meme langage.
- La doctrine Fragments dit deja que WebSocket ACK est opportuniste et que `/commands/{commandId}` est canonique.
- Le backend a maintenant une colonne Projection Sync propre; le mobile doit la consommer quand il sera pret, sans recevoir de Domain Events.

## Questions Ouvertes

1. Le mobile doit-il exposer les tickets/scan dans la premiere soumission App Store?
2. Les entitlements sont-ils produit ou demo pour v1?
3. Faut-il un endpoint public `GET /api/coffees/{id}` ou le detail doit-il rester compose depuis les snapshots?
4. Le backend `/api/sync/events` doit-il etre accessible aux utilisateurs mobiles avec JWT standard en staging?
5. Quelle est la politique officielle de suppression/archive cafe pour les apps publiques?
6. Veut-on un cache offline durable des read models des la v1 ou seulement une experience online-first avec outbox pour writes?

