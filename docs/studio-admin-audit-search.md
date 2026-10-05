# Studio admin audit search — milestone 5b

## Ownership and coverage

BEHAVIOUR / query + external adapter iteration. adminImportContext already owns
admin_audit_log and RecordAdminAudit. Search reuses that durable table, with a
separate read port implemented by the existing JDBC adapter. No new Audit BC,
projection, storage or cross-context SQL is introduced. Domain-owned moderation
and media decision journals remain independent; the Studio labels this scope.
Authentication operator/user target UUIDs are not AppUser identifiers.

GET `/api/admin/operations/audit` follows controller → SearchAdminAuditQuery →
SearchAdminAuditQueryHandler → AdminAuditReadRepository → one prepared JDBC query.
Optional exact filters: targetType, targetId, actorId, commandId, action, outcome.
Default limit 30, valid range 1–100; timestamp/UUID descending keyset pagination
with limit + 1 and an opaque cursor preserving PostgreSQL microseconds.
Nullable historical target/command/reason fields remain readable. No aggregate
loading, private profile joins, S3 access or N+1. The GET never appends its own
read audit, avoiding a feedback loop. Existing write behavior is unchanged.
The existing admin authorization chain protects the route, including exclusive
owner mode. Invalid filters/UUID/cursor/limit receive 400; 401/403 apply outside
admin authorization. No data mutation is involved.

## Evidence

Initial RED: 404 on the global read route while per-target audit existed.
Minimum green introduced only the owner-local read capability. Tests extended
the examples to individual filters, exact cursor order, nulls and empty pages.
Refactoring clarified names/DTOs from green; OpenAPI defines the response.
Manual mutations: omit commandId predicate (2 records instead of 1), truncate
cursor to milliseconds (missing next-page record). Both killed by behavioral
assertions in StudioAdminAuditIT, source bytes restored exactly, final green.

Final targeted suite: 29 tests across StudioAdminAuditIT (13),
StudioExclusiveOwnerIT (3), AdminAccessSecurityIT (2),
BoundedContextArchitectureTest (11). Not the full backend suite.
Filters are tested independently as well as jointly, SQL metacharacters stay
literal, invalid queries are rejected, the 30/100 limits apply, and repeated
reads do not generate additional audit rows.

## Delivery and deferred checks

No migration/deployment/cloud change. Existing timestamp and target indexes
remain; large-volume actor/command filtering and index needs are part of 5d,
not a demonstrated performance guarantee of this slice. Deploy the compatible
backend route before the new Studio bundle. Actual Google access and browser
visual acceptance remain pending on the deployed environment.

FlowAtlas recognizes the Studio intent/listener/result/state flow (5 nodes,
6 edges, bounded graph complete; application not assessed). Java query sources,
real PostgreSQL API tests and architecture tests provide the backend evidence;
no claim of full-system correctness from the graph.
