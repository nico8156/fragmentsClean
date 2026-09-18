# P1 — Révocation cohérente à la déconnexion

Date : 2026-09-17  
Constat traité : `FR-007`  
Périmètre : backend, mobile et Fragments Studio  
État : implémenté et validé localement ; non fusionné, non poussé, non déployé

## Résultat

La déconnexion n'est plus seulement un changement visuel local. Le mobile et
Studio présentent désormais leur refresh token à `POST /auth/logout`, vérifient
le statut HTTP et effacent immédiatement leur session locale. Le backend révoque
atomiquement toute la famille de rotation correspondant à la session.

`POST /auth/logout` est un endpoint de public client : il ne dépend pas d'un JWT
encore valide. Cette décision permet de fermer une session dont l'access token a
expiré. Le refresh token opaque est le secret présenté. La réponse est toujours
`204` pour un token valide, déjà révoqué ou inconnu afin de ne pas fournir
d'oracle d'existence. Une requête sans champ `refreshToken` reste une erreur
contractuelle `400`.

## Modèle et concurrence

Chaque login crée un `familyId` distinct. Chaque rotation conserve ce même
identifiant. Avant de lire ou modifier un token, rotation et logout verrouillent
la première ligne stable de la famille, puis travaillent dans une transaction.
Cet ordre commun évite :

- deux descendants actifs après deux rotations simultanées ;
- un descendant survivant lorsque logout et rotation courent ensemble ;
- la fuite d'une autre session du même utilisateur, qui possède une autre
  famille.

Le premier essai a mis au jour un détail JPA réel : lire l'entité avant le
verrou laissait une version obsolète dans le cache de premier niveau après
l'attente. La résolution de famille utilise donc une projection scalaire UUID,
puis charge l'entité seulement après acquisition du verrou. Le test concurrent
qui produisait `200 + 200` produit maintenant exactement `200 + 401` pour deux
rotations concurrentes.

La migration additive `logout-revocation-2026-09.psql` ajoute `family_id`,
rattache chaque éventuelle ligne existante à sa propre famille, impose `NOT NULL`
et crée l'index `ix_refresh_tokens_family_id`. Elle dépend explicitement de
`refresh-token-hardening-2026-09` et possède son propre reçu/checksum ; le
manifeste précédent n'a pas été modifié.

## Comportement des clients

Le mobile et Studio détruisent le secret local avant d'attendre le réseau. Cela
évite qu'une panne distante bloque l'utilisateur dans l'application. Mobile
tente indépendamment la révocation Fragments et la fermeture du fournisseur
OAuth : l'échec de l'une n'empêche plus l'autre. Studio expose maintenant une
action « Se déconnecter » lorsque le mode OAuth est actif.

Ce choix a une limite volontaire : hors ligne, le client ne conserve pas le
refresh token uniquement pour retenter le logout. La révocation distante ne peut
donc pas être garantie immédiatement ; le token expire naturellement ou sera
invalidé par un autre mécanisme serveur. Conserver ce secret après un logout
aurait contredit l'effacement local attendu.

Un access token JWT déjà émis est stateless et reste valable jusqu'à sa courte
expiration, 15 minutes par défaut. Ce lot ne prétend pas fournir une révocation
instantanée des JWT et n'introduit pas de denylist/Redis.

## FlowAtlas

FlowAtlas TypeScript a été utilisé avant modification avec `find` sur
`authSignOutRequested`, puis `context` à profondeur 3. La projection bornée
était complète (20 nœuds, 33 relations) et a correctement relié l'intention
`signOut`, le listener, `authSignedOut`, le reducer et l'arrêt de Projection Sync.
La lecture des sources est toutefois restée indispensable pour constater que le
gateway HTTP ignorait `response.ok` et que son `catch` empêchait toute
observabilité de l'échec.

## Vérifications exécutées

Preuves rouges initiales :

- backend `AuthRefreshIT` : 2 échecs attendus, `/auth/logout` répondait `401` ;
- mobile `authServerGateway.spec.ts` : le `503` était absorbé ;
- Studio `StudioAuthSession.test.ts` : aucune révocation distante appelée.

Preuves vertes ciblées :

```text
./mvnw -q -Dtest=RefreshTokenTest,ReleaseManifestTest,AccountDeletionContextHandlersTest test
  succès

./scripts/backend-testcontainers -q \
  -Dtest=AuthRefreshIT,StagingReleaseUpgradeIT,DeploymentSafetyIT test
  18 tests, 0 échec (avant ajout du scénario concurrent avancé)

./scripts/backend-testcontainers -q -Dtest=AuthRefreshIT test
  6 tests, 0 échec, dont les deux courses de rotation/logout

./scripts/test-release.sh
  576 tests, 0 échec, 0 erreur, 0 ignoré ; gate release vert

./mvnw -q \
  -Dtest=AppleLoginCommandHandlerTest,RefreshTokenTest,ReleaseManifestTest test
  succès après durcissement final du contrat TokenService

npx jest tests/core-logic/contextWl/userWl/gateways/authServerGateway.spec.ts \
  tests/core-logic/contextWl/userWl/usecases/auth/authFlow.spec.ts \
  --runInBand --no-watchman
  2 suites, 13 tests, 0 échec

npm test -- --run tests/authContext/StudioAuthSession.test.ts \
  tests/authContext/HttpStudioAuthGateway.test.ts
  2 fichiers, 5 tests, 0 échec

./mvnw -q -DskipTests package
  succès

npx tsc --noEmit
  succès

npm test -- --runInBand --no-watchman
  93 suites, 356 tests, 0 échec

npm run lint
  succès

npm run redux:map:check
  succès

npm test -- --run
  35 fichiers, 164 tests, 0 échec (Fragments Studio)

npm run contract:check
  succès (Fragments Studio)

npm run verify:dist
  succès (Fragments Studio)

VITE_ADMIN_IMPORT_BEARER_TOKEN= \
VITE_PROJECTION_SYNC_BEARER_TOKEN= \
VITE_STUDIO_AUTH_MODE=oauth \
VITE_ADMIN_IMPORT_GATEWAY=http \
VITE_PROJECTION_SYNC_GATEWAY=http \
VITE_FRAGMENTS_BACKEND_URL=https://fragments-staging.anchor-event.fr \
npm run build
  succès ; bundle JS 304,05 kB, 88,16 kB compressé gzip
```

Le premier lancement Jest sans `--no-watchman` a échoué car le sandbox ne
pouvait pas joindre le socket Watchman. Le premier build Studio a correctement
refusé les variables locales non conformes à une production OAuth ; il a été
relancé avec une configuration synthétique sans bearer navigateur.

Le gate backend a émis des avertissements Hikari lors de la fermeture de
plusieurs contextes Testcontainers : des tâches Projection Sync tentaient de
valider des connexions déjà fermées pendant le teardown. Maven a néanmoins
terminé avec succès. Ce bruit de fin de test n'invalide pas les scénarios de ce
lot, mais mérite un traitement séparé afin de garder les signaux CI lisibles.

## Critères de déploiement et recette

L'ordre requis est : migration et backend, puis Studio/mobile. Après déploiement :

1. connexion Google mobile, rotation, logout avec l'ancien refresh, nouveau
   refresh refusé `401` ;
2. même parcours Apple ;
3. connexion Studio, bouton de déconnexion, retour immédiat à l'écran OAuth ;
4. vérifier qu'une seconde session/appareil du même utilisateur reste active ;
5. vérifier les métriques/logs de `401` refresh sans journaliser les tokens.

La fermeture de `FR-007` ne doit être prononcée qu'après cette recette et le
déploiement du manifeste. `FR-024` (historique/reprise d'une réponse de rotation
perdue) reste distinct et ouvert.
