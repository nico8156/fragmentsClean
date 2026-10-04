# Studio community exploration — milestone 1

This slice exposes application users and experiences to authenticated Studio
operators through the owning bounded contexts. It does not create a Post,
MediaAsset or generic admin domain, or join business tables across contexts.
The broader milestone plan lives in fragments-admin/docs/studio-operations-milestones.md.

## API

- GET /api/admin/users: q, UUID cursor, limit (1..100).
- GET /api/admin/users/{id}: application profile and lifecycle status, no email.
- GET /api/admin/experiences: q, authorId, moderation, publication, cursor, limit.
- GET /api/admin/experiences/{id}: snapshot, associated media, first audit page.
- GET /api/admin/experiences/{id}/actions: cursor and limit for audit history.
- GET /api/admin/experience-media/{id}: metadata and owning resource references.
- POST /api/admin/experiences/{experienceId}/moderation: commandId, actionId,
  decision (HIDDEN or VISIBLE), mandatory reason (up to 1000 characters), at.

All routes use the existing /api/admin/** JWT/admin access policy. The command
operator comes from JWT, not the request body. Queries use read ports and JDBC
within each owner. Media URLs are resolved from persisted references and only
returned for AVAILABLE assets; no arbitrary storage key is accepted from a client.

## Moderation and event compatibility

ModerateExperienceCommand retains the durable receipt and command status flow.
A direct decision has no reportId; report-based decisions still validate their
association. Both use the existing aggregate, repositories, outbox and audit
projection. A decision with unchanged visibility is still recorded for direct
admin moderation. Reviews advance aggregate version so a first review of an
already-visible experience can close its reports in the projection.

The integration contract experience.moderated is version 2: reportId is nullable.
The updated consumer accepts older version 1 payloads with reportId present.
Admin freshness refers to the experience for moderation, including direct actions.
Public visibility continues to require PUBLISHED + VISIBLE + no deletion.

## Deployment

Deploy migration 2026-10-04-studio-community.sql, then updated backend producers
and consumers, then Studio. Driver studio-community-2026-10.psql uses the existing
release ledger/checksum and requires outbox-delivery-2026-09. The release renderer
and deploy script include it. Existing migrations remain unchanged.

The migration relaxes the audit report reference and adds query indexes; it is
replay-safe. Do not roll consumers back to code requiring reportId while v2 direct
decisions remain queued. Studio rollback alone does not require backend rollback.
No production deployment was performed during this implementation.

## Verification

67 selected backend tests passed, including StudioCommunityIT (5), ExperienceFlowIT
(7), domain/handler tests, architecture boundaries, integration envelopes, OpenAPI
and release manifests. PostgreSQL tests verify admin denial, search/pagination,
missing resources, public hiding/restoration, audit attribution, command replay,
private media metadata, first-review projection closure and migration replay.

Manual mutations for blank reason acceptance, omitted audit and omitted review
version progression were killed by assertions. Original sources were restored and
the relevant tests passed. The full backend suite and production load tests were
not run. Studio separately passed 194 tests, build and delivery checks.

FlowAtlas confirms the HTTP command boundary (4 nodes / 3 edges). Source inspection
and PostgreSQL tests establish persistence, outbox, audit and visibility guarantees;
these are not inferred from static graph completeness.
