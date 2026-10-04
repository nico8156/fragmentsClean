# Studio media catalogue — first source

The user approved a dedicated `mediaCatalogContext`. It owns a local read index,
not media aggregates or file lifecycle commands. Current coverage: EXPERIENCE.
Coffees, avatars and article revisions remain the next incremental sources.

## Read and event paths

- `GET /api/admin/media?q=&origin=&status=&ownerId=&cursor=&limit=`
- `GET /api/admin/media/{ORIGIN:uuid}`
- Existing admin JWT/identity policy covers both routes.
- Keyset pagination orders by `(origin,media_id)`, limit 1–100. Search currently
  matches media IDs or exact owner/resource IDs; no source filenames are invented.
- DTOs expose metadata, owner/resource references and optional signed previews,
  never storage keys. No upload, delete or replacement command is introduced.
- Experience media facts use their existing outbox envelopes and an additional
  `media-catalog-events` SQS destination/inbox. Catalogue SQL reads its own table.
- Source versions protect against old deliveries, replay and duplicate effects.
  Equal versions can fill a previously unknown creation timestamp.
- DELETED is a scrubbed tombstone without owner/resource/storage associations.
- Admin-only `media-catalog` freshness notifications trigger GETs in Studio.

Preview URLs require current source availability. The catalogue query calls a
primitive batch preview ACL, wired by `platform/configuration`, to an experience
query and its own source repository. No context imports another context and no
SQL joins cross-context tables. There is one bounded source lookup for a page,
not one lookup per row. Missing or withdrawn source media gets no URL; source
lookup failure fails the read. This synchronous ACL is explicit integration debt
to revisit for each new origin. Previously issued URLs retain existing expiry.

## Initial replay and reconciliation

`ReplayExperienceMediaCatalog` is producer-owned. It emits existing media facts
from source snapshots in batches of 100, persists a cursor and uses a row lock
with SKIP LOCKED. Cursor advancement and outbox insertion share a transaction.
After completing a pass, it waits a day before another reconciliation pass.

Set `MEDIA_CATALOG_REPLAY_ENABLED=true` to run the scheduled batches (default
15 seconds between batches). It is disabled by default for local/test startup.
The source cursor is in `experience_media_catalog_scan`; `completed_at` means
source enumeration completed, not that SQS deliveries reached the catalogue.
Monitor queue age, DLQ and read results before declaring a backfill complete.

New upload intents publish PENDING atomically. `createdAt` is an optional additive
field in experience media envelopes; older envelopes remain readable with an
unknown creation time. The field describes intent creation, not guessed upload
completion. Actual normalized size/type/dimensions are absent until known.

## Privacy and rollout

Account deletion now awaits a MEDIA_CATALOG acknowledgement as well. Its durable
erasure barrier serializes deletion against late projection mutations. Migration
copies pre-existing EXPERIENCE erasure barriers, so replay cannot resurrect an
already erased owner in the new projection. The physical media lifecycle remains
owned by experienceContext.

1. Provision the new SQS queue, DLQ, IAM entries and alarms from the existing
   CloudFormation template; configure `SQS_MEDIA_CATALOG_EVENTS_URL`.
2. Apply additive `media-catalogue-2026-10.psql`, after the existing
   `studio-community-2026-10` release. Previous manifests are unchanged.
3. Deploy producer and consumer backend together; enable replay and inspect
   queue health; then deploy Studio.

Old backends do not understand MEDIA_CATALOG erasure acknowledgements. Backend
rollback requires coordinated handling of in-flight events and deletion
processes; prefer a forward fix. Studio-only rollback leaves ingestion and
privacy processing active. No deployment was performed during implementation.

The full cross-origin library, replacement, retention and admin purge are not
part of this first source. A read index never authorizes deleting a used resource.

## Validation evidence

Evidence and remaining milestones are recorded in the Studio repository:
`docs/studio-media-catalogue-design.md` and `docs/studio-operations-milestones.md`.
The scoped suite exercises admin access, pagination/filtering, no arbitrary keys,
replay, source dates, erasure, migration replay, current-source preview checks,
existing community flows and architectural boundaries. The complete backend
suite, AWS deployment and load tests are not claimed.


## Photos de cafés — tranche 2b

Les lectures admin existantes `/api/admin/media` et `/api/admin/media/{id}`
annoncent désormais EXPERIENCE et COFFEE. Les identifiants café utilisent
`COFFEE:<photoId>`. Les métadonnées d’upload absentes du domaine restent nulles ;
`updatedAt` est la date de l’état source café, pas une date physique du fichier.
Un retrait de référence ne prouve pas une purge S3.

Le catalogue consomme les contrats existants `coffee.photo_added`,
`coffee.photo_deleted`, `coffee.photos_imported`, `coffee.photos_arranged`
et `coffee.deleted`, via la destination média déjà introduite. L’import et
l’arrangement transportent des inventaires complets. Un watermark par café
bloque les anciennes références encore inconnues ; les versions par photo
préservent les changements individuels plus récents. Le verrou de cette
référence locale sérialise les effets concurrents. La suppression du café
pose une barrière terminale et retire ses associations et références de stockage.
Cette table est une projection de consultation, pas un second cycle de vie café.

`ReplayCoffeeMediaCatalog` émet `CoffeeMediaCatalogSnapshotEvent`, enveloppé
comme `coffee.media_catalog_snapshot`, **uniquement vers media-catalog-events**.
Il ne rejoue pas un événement d’arrangement vers les consommateurs publics.
Le producteur parcourt 100 cafés par lot avec un curseur durable verrouillé,
une lecture SQL cohérente des parents et photos, puis outbox et checkpoint dans
la même transaction. Un inventaire vide est pertinent pour retirer une ancienne
galerie. La reprise est activée par le réglage existant
`MEDIA_CATALOG_REPLAY_ENABLED=true` et recommence quotidiennement.

Les aperçus sont vérifiés en lot par `AdminCoffeeMediaPreviewsQuery` sur les
paires photo/café courantes dans `coffeeContext`. Une photo portant le même ID
chez un autre café ne fournit pas d’aperçu pour l’association indexée. Le
resolver source existant signe les références S3 ; aucune URI de stockage
n’est renvoyée par le catalogue. Les chemins locaux sont limités au endpoint
photo-assets connu, puis résolus par l’adaptateur HTTP Studio vers le backend.
Le câblage platform étend l’ACL primitive existante. Une page mixte effectue
au plus trois lectures bornées : catalogue, expériences et cafés ; aucun GET
par ligne. Une erreur du domaine source fait échouer la lecture.

Livraison : appliquer `coffee-media-catalogue-2026-10` après
`media-catalogue-2026-10`, puis livrer le producteur/consommateur et le Studio.
La file, sa DLQ et ses permissions existantes sont réutilisées. Les manifestes
précédents restent immuables. Le backend précédent ne connaît pas les nouvelles
routes de consommation ; un rollback backend doit coordonner les producteurs,
la consommation et la reprise. Ne pas laisser un ancien consommateur acquitter
ces nouveaux événements. Le seul rollback Studio est indépendant.

Limites : le scan ne constitue pas un inventaire S3 et ne déduit pas qu’un
objet est orphelin. Une suppression source manquée doit être traitée par le
rejeu de son événement, la résolution de la DLQ ou une réconciliation explicite ;
le scan des cafés encore présents seul ne reconstitue pas les suppressions
physiques historiques. Les anciens flux de purge café restent une dette du
jalon 3 ; aucun nouveau bouton destructif n’est ajouté dans le catalogue.
Les contrôles de volumétrie, dont la taille des galeries et des messages,
restent dans la consolidation du jalon 2. Aucun déploiement réalisé ici.

## Avatar tranche (2c)

Ownership stays in `userApplicationContext` and its existing `AvatarMedia` model.
Upload intent, confirmation, replacement, removal and deletion completion publish
`AvatarMediaChangedEvent` through the transactional outbox. The stable
`avatar.media.changed` v1 contract targets `media-catalog-events` only; public
profile contracts and the mobile outbox protocol are unchanged.

The local AVATAR entry distinguishes owner (`ownerId`) from current profile usage
(`resourceId`). A source-owned batch query validates media/profile pairs, AVAILABLE,
ACTIVE account and the exact private avatar reference before signing. Platform
composition connects this primitive ACL to the catalogue. No business-table joins
across contexts, browser S3 access, arbitrary key input or public admin DTO exposure.
URLs already issued retain the existing TTL. Metadata comes from tracked uploads;
external OAuth avatar URLs are not invented as catalogue assets.

`ReplayAvatarMediaCatalog` scans batches of 100 with a locked durable checkpoint.
The source snapshot includes actual profile usage, original upload date and version.
Outbox publication and checkpoint advancement are transactional. Completed scans
resume daily; the existing replay property controls activation and defaults off.
The additive avatar migration/manifest follows the coffee baseline, is wired into
the existing renderer/deployment sequence and seeds historical USER_APPLICATION
ERASED barriers into MEDIA_CATALOG. Existing privacy scope/acknowledgements are reused.
DELETED facts and projection entries remove owner, usage, key and sensitive metadata;
late events cannot resurrect erased associations. Ownerless non-deleted facts are
ignored; opaque ownerless deletion tombstones are permitted.

This tranche adds consultation, not admin deletion commands. Existing avatar
cleanup remains source-owned. Admin retention, removal/restoration and purge policy
belong to milestone 3; an index entry alone never authorizes object deletion.

Tests cover source facts/idempotence, real HTTP upload/replacement/removal to stable
envelope and projection, admin access, filters, current source preview checks,
out-of-order facts, erasure, bounded replay with checkpoint continuation, migration
replay and historical erasure seeding. Four separate manual mutations were killed:
omitted replacement retirement fact, retained deleted key, missing current-profile
preview check, and bypassed avatar erasure barrier; all sources restored in finally.

FlowAtlas: Studio open/search bounded graphs each have 5 nodes/6 edges and are
complete within the requested projection. Java integration analysis reports
`Could not resolve integration destinations` for the avatar producer. No complete
Java graph is claimed; runtime envelope/destination/inbox tests validate that boundary.

Final local validation: 114 targeted backend tests in 23 classes pass on restored
sources, including community/public moderation, privacy barriers, source profile
API, catalogue, envelope, architecture, OpenAPI and release tests. A shared test
fixture initially tried to delete auth users still referenced by the admin audit;
its test-only cleanup was corrected and the same combined selection passed.
Studio: 228 tests/44 files, production OAuth/HTTPS build, generated contract,
distribution checks and three delivery checks pass. Chrome 1440/390 has no horizontal
overflow. These are local validations, not a full backend suite/load test or deployment.

## Article exploration: preview signing prerequisite (2d.1)

Article images currently live as `ArticleImageRef` references in covers and
revision images. Image row identifiers are recreated when sections are persisted;
they cannot identify physical files. Reuse between revisions must remain visible,
and revision timestamps/editorial authors must not be invented as upload metadata.
The Studio upload boundary currently returns assets without a durable media registry.
The article catalogue and upload registration work remain in progress.

Before reusing `DefaultArticleImageUriResolver` for catalogue queries, its signing
scope has been restricted to the configured article bucket and exact prefix boundary.
Foreign buckets, other namespaces, prefix lookalikes, URI decorations, ambiguous
encoded paths and dot segments are refused before signing. Missing bucket or a
root-only prefix cannot authorize signing. HTTP/local references and configured TTL
remain supported. Malformed-reference errors omit private paths and parser causes.
Historical S3 references outside this configured scope must be reconciled rather
than extending bucket access. No migration, new endpoint or browser SDK is added.

Validation: 28 targeted tests in 7 classes pass, including real SDK signing with
fake credentials and no AWS requests, PostgreSQL article projections/persistence,
editorial command handlers, generation, serialization and architecture. Two
separate scope mutations were killed and restored; the same combined selection
passed on restored sources. A further RED exposed the private-path parser exception
and drove its sanitization. No full backend suite/load test/deployment is claimed.
