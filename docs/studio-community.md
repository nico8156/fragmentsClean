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

## User 360: author moderation history (milestone 4a)

GET /api/admin/experiences/actions requires authorId and accepts cursor and
limit (1..100, default 30). The existing admin identity authorization applies.
A missing author projection returns an empty page; malformed queries return 400.
ListUserExperienceActionsQuery -> its handler -> AdminExperienceReadRepository
uses only experience_moderation_actions_projection and experience_views.
One bounded SQL query returns actionId, experienceId, operatorId, decision,
nullable historical reason and occurredAt. No profile/email, reporter identity,
media key or cross-context join is added. Existing author/experience indexes
are reused; no migration or integration event change is needed.

Keyset pagination sorts by occurredAt and actionId descending. Joining the
existing experience projection means an erased experience's orphaned direct
audit cannot be returned for that author. Existing account erasure remains
unchanged; direct audit may remain stored according to that existing eraser.

Evidence: StudioUserModerationHistoryIT (3), StudioCommunityIT (6), architecture
boundaries (11), all green after exact mutation restoration. Two manual mutants
(author filtering removed, strict cursor changed to inclusive) each triggered
behavioral assertions with zero test errors. Logs /tmp/user-history-backend-*.
The full backend suite and production load testing were not run.
FlowAtlas moderation command remains 4 nodes/3 edges within its configured
scope. The new JDBC query is validated by source inspection and PostgreSQL
integration tests, not claimed as an end-to-end Java graph. No deployment.

## User 360: received experience reports (milestone 4b)

GET /api/admin/experiences/reports requires authorId, with optional status
(OPEN/RESOLVED/DISMISSED), cursor and limit 1..100 (default 30). A missing author
returns an empty page; invalid query parameters return 400. Existing admin
identity authorization remains mandatory. ListUserExperienceReportsQuery and
its handler delegate to AdminExperienceReadRepository; one bounded query joins
only experience_reports_projection and experience_views within this BC.

DTO: reportId, experienceId, reason, nullable details, status and createdAt.
Reporter identity, private profiles, media keys, report counters and nested
histories are deliberately omitted. The publication link reuses existing
moderation; no command, audit subsystem or integration contract is added.
Account erasure removes report projections; the query also requires an existing
experience projection matching the stored author.

Deploy the additive studio-user-reports-2026-10 manifest after
studio-community-2026-10. It adds the author/createdAt/reportId pagination index
and reuses ledger/checksum/lock limits. Older manifests stay immutable. The
renderer and deployment script include the new driver; no deployment occurred.
Large production volumes and migration lock durations were not measured.

Final selected verification: 44 tests/5 classes green after exact restoration,
including PostgreSQL filtering, equal-time pagination, admin denial, erasure,
migration replay, release rendering and BC architecture. Manual mutations
removing author and status filters each caused assertion failures (zero test
errors), then were restored. Logs /tmp/user-reports-backend-*. The full backend
suite was not run. Studio: 412 tests/58 files, production OAuth/HTTPS build,
contract, distribution and delivery checks green; four manual Studio mutations
caught and restored. Chrome 390/1440 checked with fake data.

FlowAtlas userReportsRequested: 15 nodes/20 edges complete within the configured
bounds. Existing Java moderation command remains 4/3. Java JDBC query coverage
is established by inspected code and integration tests, not a claimed static
end-to-end graph. User 360 comments and final activity consolidation remain
subsequent milestone 4 work.
