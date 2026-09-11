# Private experience media and avatars

Lot 06 was implemented with GPT-5.6 Sol High on 11 September 2026. It adds
private photos to experiences and a single replaceable profile avatar without
changing bounded-context ownership.

## Ownership and boundaries

- `experienceContext` owns experience media references, their authorization,
  lifecycle and read projection.
- `userApplicationContext` owns avatar references and the public profile change.
- `sharedKernel` contains only the technical private-image port, S3 adapter,
  safe decoder/normalizer, generated object-key policy and signed-read helper.
- S3 is infrastructure, not a bounded context. No aggregate contains an AWS SDK
  type, URL or bucket policy.

Remote object operations never execute inside a database transaction. Upload
intent registration is a short transaction; presigning follows it. Confirmation
normalizes the binary before entering the durable command transaction. Cleanup
deletes objects before committing the local `DELETED` transition, making a
temporary S3 failure retryable.

## Write flows

```text
mobile durable outbox command
-> POST upload intent
-> direct PUT to a short-lived signed S3 URL
-> POST confirm with the same commandId
-> server reads, validates and normalizes the pending object
-> durable command + owning aggregate transition
-> transactional outbox
-> experience SQS/inbox projection or app.user.profile_updated
-> projection.updated SSE
-> authoritative GET refresh
```

Deletion is logical first (`DELETION_PENDING`) and physical through an
idempotent scheduled cleanup. Abandoned `PENDING` uploads are swept after the
configured retention. Account deletion marks the owning user's objects for the
same deferred cleanup; it never performs cross-context table writes.

The mobile copies selected images into application documents before enqueuing.
It removes that local copy only after backend acceptance or an explicit business
rejection. Offline, timeout, 5xx and missing socket/SSE keep the command and file
for retry. `/commands/{commandId}` remains the confirmation source of truth.

## Security and media policy

- The bucket and every uploaded object remain private.
- Object keys are generated server-side from owner ids and random media ids.
- Signed upload and download URLs are short-lived; upload signatures bind the
  expected content type and server-side AES-256 encryption header.
- JPEG and PNG are accepted. File signatures, decoded dimensions, pixel count
  and actual byte size are checked server-side; declared metadata is not trusted.
- Decoding and JPEG re-encoding remove EXIF, including GPS data. Avatars are
  center-cropped to a square; experience photos retain their aspect ratio.
- An experience cannot publish while one of its media items is still pending.
- Signed URLs exist only in transport read models and are never persisted.

The server supports an ordered collection with a configurable maximum of four
experience images. The App Store V1 interface deliberately exposes one image per
experience; widening that UI later does not require a storage or event-contract
migration. Avatars are unique per active user, and replacement marks the prior
object for deferred deletion before making the new one active.

## Events and freshness

`experience.media.changed` is a primitive, transport-neutral fact containing
the experience, coffee and author identifiers needed by consumers. The
experience projection uses version-monotonic upserts. Its SSE notification
invalidates both coffee-scoped and author-scoped experience views; clients then
retrieve the snapshot. Moderation receives the same projected media list and
never accesses the write table or S3 directly.

## Runtime and rollback

`PRIVATE_MEDIA_BUCKET` is mandatory when the real adapter is enabled. Region,
signed URL TTLs, limits, normalization dimensions, cleanup cadence and retention
are configuration-driven. Staging enables cleanup explicitly. IAM must grant
only the required object operations for this private bucket/prefix.

The SQL change is additive. Application rollback keeps the new tables and
objects. Do not drop them until all pending commands, cleanup candidates and the
rollback window are exhausted. Real AWS IAM/CORS, device camera/library behavior,
slow-network upload and deployed cleanup remain release-environment validation;
automated tests use the real PostgreSQL schema plus fake technical object stores.
