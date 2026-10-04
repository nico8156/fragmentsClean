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


## Article source usages — tranche 2d.2a

The global catalogue still covers experiences, coffee references and managed
avatars. Article source usage consultation is delivered independently at
`GET /api/admin/studio/articles/{articleId}/media?cursor=&limit=`. ArticleContext
owns controller → query handler → read port → JDBC. The adapter reads only
article tables, performs two fixed queries in a REPEATABLE_READ transaction,
distinguishes 404 from an empty page and keyset-paginates at 30/default, 100/max.
All retained revisions are included. Revision-row ids disambiguate cursor order,
never file identity; replacing rows requires restarting pagination. Paging across
requests is not a durable snapshot. No schema migration is needed. No deployment performed.

Reference identity is namespace `article-media-v1` plus the exact trimmed
reference, hashed deterministically to UUID. It does not prove physical file
existence, URL alias equivalence or permission to access/delete a resource.
Cover and section references retain separate revision/role/position usages.
Dimensions are declarations. Editorial author is not assumed to be uploader;
unknown upload metadata is absent. DTOs never expose raw S3 references.

Existing global admin JWT/RBAC applies. Preview resolution reuses the constrained
article S3 resolver. Disallowed references or absent signing configuration retain
the usage without a preview. Only HTTP(S) without credentials or the existing
local image asset path may be rendered. There is no new storage capability,
browser key parameter, destructive command or public read route.

Evidence: initial API RED 404→200; four real PostgreSQL/API tests for repeated
references, row recreation, pagination, authorization, missing/empty, invalid
queries and private-reference non-disclosure. Six named-fake query/identity tests;
existing signature, aggregate, public projection, authoring and architecture
checks also pass: 37 targeted tests / eight classes on restored code. Manual
constant-identity mutation KILLED by the distinct-reference assertion, restored
in finally. Full backend suite/load tests/deployment not claimed.

Studio inspector reuses its Redux listener/gateway flow and generated contract;
242 tests / 45 files, production build, bundle and delivery checks pass. Manual
stale-read mutation KILLED and restored. Chrome inspected at 1440/390 with no
horizontal overflow. FlowAtlas confirms the bounded Redux projection (5 nodes,
6 edges). The available Java command semantic request does not discover this
query controller; static completeness is not claimed for the Java read path.

Next tranche 2d.2b must add article-owned source facts/outbox, ordered multi-article
usage projection, replay and global Article ↔ Media navigation. Unreferenced
uploads still need owner-owned durable tracking. This source read is not a
cross-domain SQL fallback for MediaCatalogContext. Lifecycle admin and User 360
remain the authorized milestones 3 and 4 after catalogue consolidation.

## Article catalogue and tracked uploads — 2026-10-04

Article remains the source owner. `article_media_uploads` records newly stored
Studio/generated files, including uploads made before saving the article. The
source inventory contains all retained revision usages and tracked uploads.
A dedicated sequence orders observations even when an editorial transition does
not change the aggregate version. Producer replay uses a durable source-owned
checkpoint; default scheduler activation remains the existing opt-in setting.

`ArticleMediaCatalogSnapshotEvent` maps to the stable
`article.media_catalog_snapshot` v1 contract and the existing media catalogue
queue only. Each observation is split into bounded parts (max 100 references,
conservative 180000-byte budget). Consumer parts are staged locally and only
replaced after a complete observation, in the inbox transaction. Older completed
versions cannot resurrect retired usages. Shared references are aggregated over
all local Article usages and media locks are acquired in UUID textual order.

New use cases: `PublishArticleMediaCatalogSnapshot`, `ReplayArticleMediaCatalog`,
`RecordArticleMediaUpload`. Existing authoring/editorial handlers publish through
a mandatory port after persistence. Generation records stored files individually
and republishes the final inventory on completion; already stored files remain
tracked when a later generation step fails.

The admin catalogue supports optional metadata (original name, operator UUID,
provenance), server `usage` filters, filename/article/title search, and separate
keyset pagination of Article usages. `GET /api/admin/media/{id}/usages` exposes
only local DTOs. `GET /api/admin/studio/articles/{articleId}` opens the existing
source document without requiring a previously loaded Studio list.

Preview ACL reads current Article revision/upload references in one bounded
batch and verifies each reference identity before invoking the existing secure
resolver. A stale catalogue cannot authorize a retired source reference. No
client-supplied arbitrary S3 key, raw storage reference in admin response, direct
bucket access, source-domain table joins in the catalogue, or new purge action.

Metadata semantics: stored byte count is measured; dimensions use supported image
headers, falling back to existing declared dimensions when unavailable. The first
registration date/operator is preserved by the source UPSERT. Operator identity
is not mapped to AppUser ownership. Historical unknown values remain unknown.
`UNUSED` denotes no retained revision association for a tracked upload. It does
not authorize removal. Article `DELETED` means its untracked reference was retired,
not proof of S3 deletion. Long references retain identity/usages but references
over 8192 characters are omitted from preview transport; display facts are bounded.

Additive release `article-media-catalogue-2026-10.psql` follows the avatar baseline
and is wired into the renderer/SSM flow. Previous manifests remain immutable.
The SQL fragment is verified against PostgreSQL and can be reapplied. No deployment
or remote replay activation was performed. Reconciliation of already orphaned
historical files, and files left after a DB failure following object storage,
requires a later explicit lifecycle operation; storage and DB are not one
transaction. No unsafe compensating purge was introduced.

FlowAtlas TypeScript confirms new open-article and paginated-usage intent flows.
Java semantic analysis could not resolve the integration destination for the new
producer with the existing request fixture. The limitation is recorded rather
than represented as a complete Java graph. PostgreSQL/outbox tests verify the
actual stable envelope, sole queue destination, routed projection and replay.

Validation for this pass: 101 selected backend tests in 16 classes, zero failures
and errors (four origins, source/outbox/router, replay, uploads, shared usages,
metadata, editor/generation, pagination, authorization, stale preview revocation,
contract versions, repeatable PostgreSQL fragment, release checksums and context
architecture). This is the relevant selection, not a claim about the full backend
suite. Removing the committed-version guard causes two behavioral assertions to
fail; source restored automatically, selected suite rerun green. Studio: 255 tests,
production build, generated contract, bundle and delivery checks; real components
inspected at 1440/390 widths. No deployment, bucket access or physical purge.


## Article upload lifecycle — milestone 3a

Article owns ACTIVE/RETIRED for tracked uploads; the catalogue remains read-only.
The admin lifecycle query reports authoritative usage counts across every retained
revision, including other articles. Retirement locks the tracked row and refuses
any usage; restoration makes it active again. Source snapshot publication takes
shared locks on referenced tracked uploads inside existing write transactions and
rejects retired references, rolling back their association. New writers must preserve
this transaction boundary. No physical storage operation is part of these commands.

Admin GET /api/admin/studio/article-media/{mediaId}, POST .../lifecycle and GET
.../operations reuse JWT authorization, canonical command receipts and existing audit.
The operator is derived from JWT; reason is mandatory (1–240 trimmed characters).
The operations endpoint follows Coffee's bounded recent journal convention (default
30, maximum 100), not a complete paginated history. Same-command retries use the
existing actor/fingerprint receipt guard. Audit and source update are transactional.

Optional uploadStatus on version-1 snapshots is additive; old payloads remain
readable. The projection displays retired tracked Article uploads as DELETION_PENDING,
without feeding the Experience/Avatar cleaners. Article preview consults the source
and refuses retired references even before catalogue refresh. Retention explicitly
means INDEFINITE_NO_AUTOMATIC_PURGE. A later purge needs an explicit policy and an
authoritative check at execution, not inferred catalogue absence.

Additive article-media-lifecycle-2026-10.psql follows the article catalogue baseline;
renderer/SSM wiring and checksums are covered. No deployment was performed.
Validation: 110 selected backend tests in 17 classes, zero failures/errors/skips,
including true concurrent association/retirement, retained shared revisions,
restoration, stale preview revocation, admin authorization, journal and idempotence.
Two domain mutations were killed by assertions and restored before the green rerun.
The full backend suite is not claimed. Studio has 273 passing tests and its production,
contract, bundle and delivery checks pass. FlowAtlas confirms bounded Redux command
and reconciliation flows; the previous Java producer-resolution limitation remains,
with actual source/outbox/router/projection integration verified by PostgreSQL tests.

Milestone 3 remains in progress: this is its Article slice, not lifecycle support for
all origins or physical purge. Existing immediate Experience/Avatar cleaners require
source-domain adaptation before exposing reversible administrative retirement.


## Avatar lifecycle — milestone 3b

User Application now owns admin retirement/restoration of tracked unused avatars.
GET /api/admin/studio/avatar-media/{mediaId} exposes current source capabilities;
POST .../lifecycle uses JWT operator, mandatory reason and durable canonical receipt;
GET .../operations reuses the existing bounded recent journal. No raw storage key,
new audit table or global bucket access is exposed to Studio.

The handler follows existing avatar writer lock ordering (owner, then media),
counts exact references in all application profiles and refuses used files.
AVAILABLE → RETIRED retains bytes/metadata/ownership and emits the existing
AvatarMediaChangedEvent. RETIRED cannot be confirmed or selected by cleanup.
Restoration requires active ownership and no competing AVAILABLE avatar, preserves
the existing uniqueness constraint and does not assign/overwrite the profile.
PENDING/DELETION_PENDING/DELETED cannot be restored administratively. Source erasure
remains allowed to move RETIRED to DELETION_PENDING; privacy intent takes priority.

The stable version-1 envelope carries the existing textual status. The local
catalogue maps RETIRED into its existing DELETION_PENDING consultation status;
source capabilities distinguish conservative retirement from actual deletion.
Only AVAILABLE/RETIRED report INDEFINITE_NO_AUTOMATIC_PURGE; other states report
EXISTING_SOURCE_LIFECYCLE, not an invented retention guarantee. Existing preview
rules are preserved: signing requires an active profile referencing AVAILABLE media.
Additive avatar-media-lifecycle-2026-10.psql follows the Article lifecycle release;
previous manifests remain immutable. Deploy the compatible migration/backend
producer and consumer before enabling the new Studio actions. No deployment here.

Validation: 78 targeted tests in 14 classes, no failures/errors/skips, including
source guards, two-connection owner-lock concurrency, audit, receipt conflicts,
restoration, cleanup exclusion, privacy transition, stable outbox/router/replay,
repeatable PostgreSQL fragment, manifest checksums and context architecture.
Two manual domain mutations (used guard and replacement guard) are killed by
behavioral assertions, restored exactly, then the selected suite rerun green.
Studio: 301 tests, safe production build, contract, bundle/delivery checks and
Chrome 1440/390 inspection. Its premature-APPLIED mutant is likewise killed/restored.

FlowAtlas Java resolves the new HTTP/controller/command/handler boundary (4 nodes,
3 edges), and TypeScript resolves Community reconciliation/cross-catalogue refresh
(19 nodes, 34 edges). These are complete bounded projections, not full application
coverage. The existing Java integration-destination resolution limitation remains;
tests verify the actual producer/outbox/envelope/router/inbox/projection flow and
version protection after restoration. Milestone 3 remains open for Experience/Coffee,
replacement capabilities and explicit deferred-purge policy/mechanism.


## Experience : capacités média source et modération réutilisée (3c)

GET /api/admin/studio/experience-media/{mediaId} est une lecture authentifiée admin
de experience_media et experiences, exclusivement dans experienceContext. Controller
→ query handler → port → JDBC ; aucune clé/URL S3 supplémentaire retournée. Les
capabilities sont consultatives : ModerateExperienceCommand demeure l'unique
autorité pour masquer/restaurer une publication avec motif, outbox et audit existant.
Une association absente ou une publication DELETED ne permet aucune restauration.

Pas de RETIRED sur ExperienceMedia ni de commande parallèle : le fichier conserve
son association immuable. Masquer une publication ne supprime pas ses preuves.
Une suppression source DELETION_PENDING reste distincte et n'est pas réversible
par la restauration de la publication. Catalogue et projections restent des lectures.

Test API ajouté à StudioCommunityIT : source disponible sans projection, droits
401/403, capacités VISIBLE/HIDDEN/DELETED, absence de clé exposée, identifiant absent.
La suite existante vérifie modération, restauration, audit et visibilité publique
après routage des événements, avec rejeu. RED constaté : endpoint absent 404/200.
Mutation manuelle exécutée : suppression du garde DELETED dans la query, tuée par
l'assertion canRestorePublication ; source restaurée exactement avant vérification.

La durée future de rétention admin est confirmée à 30 jours, puis purge distincte
confirmée et auditée. Ce contrat Experience ne livre pas encore cette purge et
ne modifie pas les suppressions utilisateur ni l'effacement de compte.

Validation finale 3c : 24 tests backend ciblés / 3 classes, zéro échec/erreur ;
API PostgreSQL, architecture et catalogue. Suite backend entière non exécutée.


## Coffee : retrait/restauration source (3d)

coffeeContext conserve la Photo retirée dans coffee_photo_retirements (identité,
café, URI privée, couverture/ordre historiques, retired_at). Une restauration
utilise Coffee.addPhoto et son ordre normalisé, sans nouvel upload. Les anciennes
références retirées sans mémoire ne sont pas inventées. Un café ARCHIVED refuse
ces actions ; les identités ambiguës et restaurations vers un autre café sont refusées.

API admin : GET /api/admin/studio/coffee-media/{mediaId}, POST .../lifecycle,
GET .../operations (30 décisions récentes). Query/handler/port/JDBC pour les
capacités source, CommandBus/AuthenticatedCommand pour la décision. CommandId
et acteur JWT sont conservés ; motif de 1–240 caractères obligatoire. Le journal
est le port AdminAuditRecorder existant, dans la transaction. Aucune URI privée
n'est ajoutée au contrat client.

Les écrivains Coffee prennent un verrou SQL sur la ligne parent avant le chargement
JPA : le verrou follow-on ne suffit pas à relire l'état après attente. La mémoire
retenue bloque DeleteCoffeeCommand et son nettoyage physique. La reprise Google
s'arrête avant téléchargement/stockage si des photos sont retenues ; elle ne
réintroduit ni n'écrase leurs clés stables. Le verrou est propre au café, mais les
imports/uploads le conservent pendant leur transaction et peuvent retarder une
autre commande de ce café.

Les projecteurs photo Added/Deleted/Imported/Arranged réutilisent désormais la
convention du snapshot source Coffee : CoffeePhotoProjectionRefresh → port source
local → parent FOR SHARE → photos source → inventaire projeté courant. Les tests
ont reproduit un ancien Added réintroduisant un retrait et un ancien Deleted
annulant une restauration. Les deux sont corrigés sans nouveau transport ni
changement du contrat v1 des événements. Source et projection restent distinctes.

La politique Coffee fixe 30 jours complets et absence d'usage courant pour
l'éligibilité à une purge future ; aucune purge automatique ni exécution physique
n'est ajoutée ici. Le read model donne la date minimale, pas la preuve d'une
purge autorisée. Article/Avatar restent sans purge automatique jusqu'à la tranche
qui implémentera l'action distincte, confirmée et auditée.

Migration additive coffee-media-lifecycle-2026-10 après avatar-media-lifecycle :
FK source, index, registre/checksum/advisory lock, renderer et SSM raccordés.
Fragment testé en schéma isolé avec métadonnées conservées au second passage.
Aucun déploiement effectué.

Preuves : API RED absente, domaine/fake handler, garde import, conservation parent,
restauration ambiguë et deux inversions publiques RED via outbox/routeur. Droits
401/403, validation/absence, idempotence et audit attribué, concurrence à deux
connexions, JPA, catalogue, projections publiques et release couverts. Trois
mutations manuelles tuées par assertions : durée réduite à zéro, garde parent
retiré, client APPLIED prématuré ; exact restore puis vérifications finales vertes.

Résultat final : 72 tests backend ciblés / 17 classes, zéro échec/erreur/skipped ;
330 tests Studio / 53 fichiers. Build sûr, contrat, bundle, checks de livraison et
Chrome 1440/390 verts. Suite backend entière non exécutée. FlowAtlas HTTP Java
4 nœuds/3 arêtes et Redux réconciliation 22/35, complets dans leur frontière ;
les garanties transactionnelles et d'ordre viennent des tests réels.

Consolidation restante du jalon 3 : purge explicite après 30 jours, remplacement
compatible et fermeture de la suppression photo historique. La nouvelle page
Studio dirige les photos vers la bibliothèque ; le DELETE backend historique
ne crée pas encore cette mémoire (il ne purge pas les octets). La reconstruction
d'un catalogue effacé doit également reprendre la mémoire des photos retenues :
les snapshots Coffee actuels inventorient seulement les photos encore associées.
Ne pas marquer le jalon 3 terminé avant ces points.

### Consolidation 3e.1 : fermeture du DELETE Coffee historique

L’ancien DELETE `/api/admin/coffees/{coffeeId}/photos/{photoId}` renvoie 410 sans
commande ni écriture. Authentification/autorisation restent exigées (401/403).
Utiliser la commande `/api/admin/studio/coffee-media/{mediaId}/lifecycle` : motif,
identité JWT, mémoire source réversible, audit et statut canonique. Le contrat
OpenAPI marque la route historique deprecated. Déployer le backend puis le
Studio compatible ; les anciens bundles sont refusés au lieu de retirer une
photo sans mémoire/audit. Aucun changement physique S3 ni migration.

BEHAVIOUR command/sécurité : RED 410 attendu/202 reçu, minimum GREEN. Mutation
manuelle 410→202 exécutée et tuée par le test de route, restauration exacte puis
45 tests backend/3 classes réussis (AdminRoutesControllerTest,
CoffeeMediaLifecycleIT, BoundedContextArchitectureTest). Studio : 331 tests/53
fichiers, build OAuth/HTTPS, contrat généré, contrôle bundle et livraison réussis.
Mutation adaptateur qui invente un commandId détectée puis restaurée. FlowAtlas
Redux final ancien intent→listener→mediaCatalogOpened : 4 nœuds/3 liens complets
sur ce périmètre, sans appel gateway. Le handler interne legacy est conservé,
sans exposition par cette route admin. Replay des photos retirées et purge
explicitement demandée après 30 jours restent les prochaines tranches.

### Consolidation 3e.2 : réconciliation des photos Coffee conservées

Replay source : inventaire actif et coffee_photo_retirements dans une seule
instruction SQL/MVCC, batch de 100 cafés et checkpoint existants. Chaque réel
retrait/restauration émet aussi CoffeeMediaCatalogSnapshotEvent, vers l’index
admin uniquement. Aucun événement supplémentaire pour une décision idempotente.
Le contrat coffee.media_catalog_snapshot v2 distingue photos actives et références
retirées (photoId/retiredAt, aucune clé/URI de retrait). Le consommateur garde v1.

Catalogue local : DELETION_PENDING, resourceId café source, usageStatus UNUSED,
aucun aperçu ni fausse date d’upload. Retrouvable par café après reconstruction.
Les anciens inventaires de galerie ne retirent plus ces références. Seul un
inventaire complet peut enrichir un tombstone Coffee DELETED en DELETION_PENDING
à version égale, sous la barrière Coffee existante. Les versions des autres
origines restent strictes ; suppression parent et faits anciens restent protégés.
La durée de rétention et la décision de purge restent dans Coffee, jamais dans
ce read model. Aucun nouveau modèle média global ni migration.

Déploiement coordonné requis : driver coffee-media-lifecycle 3d déjà appliqué,
consommateurs v2 prêts avant émission v2 ; ne pas faire cohabiter un consommateur
ancien et ce producteur sur la queue catalogue pendant un rolling upgrade.
Cette passe ne déploie pas et ne touche aucun objet S3.

BEHAVIOUR projection/command : RED froid 404 au lieu de 200, inventaire manquant
après commande (un événement au lieu de deux), enrichissement DELETED au lieu
de DELETION_PENDING. Mutations manuelles EXECUTED/KILLED : omission d’inventaire
(test fake handler) et blocage d’enrichissement (outbox/routeur/JDBC réel).
Restauration exacte puis 73 tests/11 classes backend verts, incluant toutes les
origines du repository partagé ; 332 tests/53 fichiers Studio verts, build
OAuth/HTTPS, contrat/bundle/livraison vérifiés. FlowAtlas commande HTTP→command→
handler bornée 4/3 ; service ReplayCoffeeMediaCatalog non reconnu en Handler,
limite statique explicite. Tests runtime prouvent le parcours source→outbox→
enveloppe v2→inbox/routeur→projection→GET ainsi que v1, restauration, tombstones,
événements anciens et absence de preview privée. Pas de suite globale backend.

### Consolidation 3e.3 : demande explicite de purge Avatar après 30 jours

Réutilisation du cycle source userApplicationContext : RETIRED → DELETION_PENDING
sur demande admin PURGE_REQUESTED motivée, puis DELETED via le nettoyeur existant.
Pas de nouvel état persistant, table, migration ou job. Le domaine refuse avant
30 jours pleins et si un profil utilise le fichier. Verrouillage propriétaire
puis média, compte actif, motif 1–240 caractères et identité JWT inchangés.
Audit AVATAR_MEDIA_PURGE_REQUESTED, cible AVATAR_MEDIA, opérateur/reason/commandId
serveur dans la même transaction ; reçu canonique idempotent inchangé.

La query source fournit canPurge, retiredAt et purgeEligibleAt, sans clé de
stockage. Les dates viennent de la dernière mise en retrait source et du délai
domaine ; le Studio ne calcule aucun droit. L’ancienne valeur de rétention
INDEFINITE_NO_AUTOMATIC_PURGE reste correcte : aucun nettoyage par seul âge.
Le contrat de demande inclut PURGE_REQUESTED ; les états source restent distincts.

APPLIED signifie demande acceptée, jamais suppression physique acquise. Le
nettoyeur CleanAvatarMediaObjects utilise uniquement les clés source privées,
appelle le stockage hors transaction puis CompleteAvatarMediaDeletion. Une panne
laisse DELETION_PENDING, réessayable ; le fait DELETED/version/date est émis
seulement après nettoyage réussi. L’audit garde la demande initiale ; la fin
physique est attestée par le cycle source existant, pas par un second audit admin
inventant un opérateur. La purge dépend de fragments.media.cleanup.enabled ;
aucune activation/configuration production ou exécution S3 réelle dans cette passe.
Les règles d’effacement de compte restent prioritaires et inchangées.

Preuves BEHAVIOUR command/query/UI : RED 30 jours et usage rejetés comme statut
non supporté ; source canPurge absent ; confirmation UI absente ; retry contenant
error/statusOfCommand au lieu du seul intent. GREEN/contrats puis 3 mutations
manuelles EXECUTED/KILLED : délai 30→0, garde usage supprimé, canonical PENDING
forcé APPLIED. Restauration exacte et validation finale : 52 tests backend/9
classes, 339 tests Studio/53 fichiers, build OAuth/HTTPS, contrat/bundle/3 tests
livraison verts. Faux stockage prouve absence de purge par âge, panne partielle,
reprise et fin physique ; PostgreSQL/API prouvent capacités, refus, audit unique,
restauration interdite après demande, previews et circuits Avatar précédents.
Chrome 390/1440 : confirmation/motif/journal dans le design existant, aucune
largeur document excédentaire. FlowAtlas Java commande 4/3 borné ; Redux canonical
6/5 borné complet, couverture application non évaluée. Tests portent la preuve
runtime vers outbox/routeur/catalogue ; graphe statique non assimilé à l’exécution.
Pas de suite backend globale ni de déploiement.
