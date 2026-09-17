# P1 — Dispatch outbox durable, borné et multi-instance

Date : 2026-09-17
Constat traité : `FR-008`
Périmètre : backend `sharedKernel`, messaging, observabilité et migration de release
Modèle : GPT-6 Astra, raisonnement High
Branche : `fix/p1-outbox-durable-dispatch`
Base du lot : `77a05bf` (`fix(editorial): canonicalize approval timestamps`)
État : implémenté, documenté et validé localement ; non fusionné, non poussé, non déployé

## Résultat

Le dispatcher ne conserve plus une transaction et une connexion PostgreSQL
pendant l'envoi vers le bus local ou SQS. Chaque passage suit désormais ce
protocole :

```text
transaction courte : claim PostgreSQL + lease
-> transaction fermée
-> envoi externe
-> transaction courte : SENT + command status APPLIED
```

Les claims utilisent un `UPDATE` atomique fondé sur `FOR UPDATE SKIP LOCKED`.
Une lease identifie le worker et empêche deux instances de traiter simultanément
la même ligne. Une lease expirée est récupérable après arrêt brutal. Les mises à
jour de succès et d'échec vérifient encore le propriétaire de la lease : un
worker lent ne peut pas finaliser le claim déjà repris par un autre.

Le batch est borné à 10 par défaut. Le dispatcher réclame au plus un événement
par stream à la fois, puis boucle après sa finalisation afin de préserver l'ordre
d'un même `stream_key` tout en laissant les autres streams avancer.

## Retry et sémantique de livraison

Une erreur d'envoi remet l'événement en attente avec un backoff exponentiel,
un jitter stable et un plafond configurables. Après 10 échecs par défaut, la
ligne passe à `FAILED`. Un événement terminal bloque volontairement les faits
ultérieurs du même stream : publier la suite en sautant un fait impossible à
traiter aurait violé l'ordre métier.

La garantie reste **at-least-once**. Un crash après l'envoi SQS mais avant le
`markSent` entraînera une nouvelle livraison après expiration de la lease. Ce
comportement est attendu et dépend de l'inbox et de l'idempotence métier des
consommateurs. Il n'est pas présenté comme de l'exactly-once.

La transition de `PENDING` à `SENT` et le passage associé du reçu de commande à
`APPLIED` ont lieu dans la même transaction courte. Un échec de cette
transaction laisse l'événement récupérable au lieu de produire un succès
partiel durable côté base.

## Compatibilité et rollback

Le gate complet a découvert une compatibilité PostgreSQL non couverte par les
premiers tests : `@Lob` stocke historiquement `payload_json` comme identifiant de
Large Object dans la colonne texte. Une première correction convertissait ces
objets en JSON direct, mais la revue de rollback a montré qu'un ancien binaire
n'aurait plus su les relire.

La solution retenue est donc un expand compatible :

- l'écriture JPA historique `@Lob` est conservée pendant cette release ;
- l'adaptateur JDBC reconnaît un OID valide et lit son contenu avec `lo_get` ;
- un JSON direct reste également accepté ;
- la migration ne transforme et ne supprime aucun Large Object existant.

Le nouveau binaire lit ainsi les deux représentations et l'ancien binaire reste
compatible avec les nouvelles écritures en cas de rollback. Une éventuelle
normalisation future devra être une migration expand/contract séparée, après la
fin de la fenêtre de rollback.

## Architecture

- `OutboxDeliveryStore` est le port applicatif des transactions courtes de
  claim/finalisation ; `JdbcOutboxDeliveryStore` porte le SQL PostgreSQL.
- `OutboxMessage` et `OutboxEventData` empêchent les senders et les contrats
  d'intégration de dépendre de l'entité JPA.
- `OutboxRetryPolicy` contient la politique pure de retry ; aucune décision de
  backoff n'est placée dans l'adaptateur SQL.
- le réseau reste dans `OutboxEventSender`, hors transaction ; le store ne fait
  aucun appel externe.
- l'ancien port JPA de dispatch et son adaptateur mal orthographié ont été
  supprimés ; le repository JPA reste propriétaire de l'écriture transactionnelle
  initiale de l'outbox.

Ce lot ne modifie ni le format des événements d'intégration ni les destinations
SQS. Il respecte les frontières existantes du modular monolith.

## Migration, exploitation et observabilité

La release additive `outbox-delivery-2026-09.psql` ajoute :

- `next_attempt_at` ;
- `lease_until` ;
- `lease_owner` ;
- `last_error` ;
- les indexes de sélection due et d'ordre par stream.

Le manifeste est immuable, vérifie son checksum, exige la release précédente et
est inclus dans le rendu et le déploiement SSM. Aucun ancien manifeste n'a été
modifié.

`messagingRuntimeHealth` expose maintenant les leases expirées dans le détail
`outboxExpiredLeases` et la métrique
`fragments.outbox.expired.leases`. Le runbook documente le diagnostic, les cinq
paramètres du dispatcher et un redrive manuel conditionné à la correction de la
cause. Le redrive ne modifie jamais directement une projection.

Ordre de promotion : appliquer la migration additive, puis démarrer le nouveau
backend. Le rollback applicatif reste compatible ; les colonnes et indexes
additifs peuvent rester en place.

## Preuves rouges rencontrées

Le premier gate officiel n'a pas été maquillé : 594 scénarios sur 595 étaient
verts, mais `CoffeeReadIT` échouait parce que le premier adaptateur JDBC lisait
l'OID Large Object (`"17593"` lors de cet essai) au lieu du JSON. La verticale
a conduit à la lecture duale décrite plus haut.

Une première variante de migration contenant un bloc de contrôle transactionnel
interne a aussi été rejetée par le renderer de release. Elle a été supprimée ;
la migration finale est purement additive.

Après une première correction de fixture, `StagingReleaseUpgradeIT` a révélé
que la migration historique des reçus avait légitimement travaillé avant la
lecture duale ajoutée par cette nouvelle release. Les assertions documentent
maintenant cet ordre immuable au lieu de simuler la réexécution d'une ancienne
migration.

## Vérifications finales

```text
./scripts/backend-testcontainers -q \
  -Dtest=CoffeeReadIT,StagingReleaseUpgradeIT,JdbcOutboxDeliveryStoreIT test
  succès, PostgreSQL réel ; 14 tests, 0 échec

./scripts/test-release.sh
  596 tests, 0 échec, 0 erreur, 0 ignoré
  BUILD SUCCESS

git diff --check
  succès

bash -n infra/aws/compose/platform/staging/fragments/deploy-via-ssm.sh \
  infra/aws/compose/platform/staging/fragments/render-release-migration.sh
  succès
```

La matrice spécifique couvre : double claim, expiration et reprise de lease,
fencing d'un ancien owner, ordre d'un stream, progression inter-streams,
finalisation atomique avec le reçu de commande, backoff, appel sender sans
transaction DB active, vidage borné et ordonné, échec après send avant
finalisation, quarantaine terminale, migration/replay, et lecture non destructive
d'un ancien payload Large Object.

Le gate émet encore du bruit Hikari/Projection Sync pendant la fermeture de ses
nombreux contextes Spring et Surefire force finalement l'arrêt du fork après son
timeout de teardown. Le résultat Maven est néanmoins un succès sans test ignoré.
Ce bruit reste un sujet distinct de qualité du gate ; il ne doit pas être pris
pour un échec SQS/outbox ni rester indéfiniment sans traitement.

## FlowAtlas

Deux appels MCP ont été tentés avec :

```json
{
  "query": "OutboxEventDispatcher",
  "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
  "adapter": "java",
  "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-integration-event-request.json",
  "kind": "Handler",
  "limit": 5
}
```

Les deux appels ont échoué dans
`scripts/javaMavenSemanticContext.mjs` / `JavaSemanticSpike.java`, avant de
produire le moindre nœud. Le message MCP est dominé par le classpath Maven et
tronqué avant le diagnostic racine utile. Aucun résultat FlowAtlas n'a donc été
utilisé comme preuve pour ce lot ; la cartographie provient des sources, des
diffs et des tests.

Retour concret pour FlowAtlas : préserver et remonter séparément `stderr`, le
code de sortie et les dernières lignes du processus Java, au lieu de préfixer
l'erreur par l'intégralité du classpath. Un mode `find` capable de réutiliser les
classes Maven déjà compilées éviterait aussi deux scans sémantiques coûteux et
sans résultat. Le contexte recherché devrait à terme représenter explicitement
le triplet `claim transactionnel -> effet externe -> finalisation
conditionnelle`, avec lease/fencing et frontière réseau.

## Limites restantes

- `StableEnvelopeOutboxEventSender` peut appeler le bus local puis plusieurs
  publishers dans une même tentative. Un échec tardif peut donc dupliquer un
  effet antérieur ; c'est conforme à l'at-least-once, mais il n'existe pas de
  checkpoint par destination.
- Le circuit breaker n'est pas introduit : le backoff borne la pression ; une
  politique de coupure devra être justifiée par des mesures.
- La recette sur deux instances AWS et l'indisponibilité SQS réelle restent à
  faire après déploiement.
- `FR-009` (routes inconnues), `FR-015` (santé globale/incident) et le bruit de
  teardown Projection Sync restent ouverts ; ce lot ne les déclare pas corrigés.

`FR-008` peut être fermé localement. Sa fermeture opérationnelle exige la
fusion, l'application du manifeste, le déploiement du backend et la recette
staging à deux workers avec interruption/rétablissement du transport.
