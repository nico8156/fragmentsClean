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
