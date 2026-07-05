# FragmentsClean Architecture Audit - July 2026

Date: 2026-07-05  
Scope: `/Users/nicolasmaldiney/fragmentsClean` backend only.  
Mode: architecture audit, no code changes beyond this report.

## 1. Executive Summary

FragmentsClean has crossed an important threshold: the backend is no longer a simple CRUD/API application. It now has a credible modular monolith shape with command handlers, projections, outbox, SQS, inbox, projection sync SSE, admin import orchestration, S3-backed photos, and a staging deployment that exercises the real asynchronous path.

The architecture is directionally sound. The strongest choices are:

- coffee creation remains owned by `coffeeContext`;
- admin import delegates creation instead of writing coffee tables directly;
- Google Places preview is separated from durable import;
- projections are updated by event handlers, not controllers;
- the frontend receives `projection.updated`, not domain events;
- SQS is active in staging instead of being bypassed;
- S3 photo references are stored as stable `s3://...` values and resolved at read time.

The weak point is not the business flow. The weak point is architectural enforcement. Several boundaries are currently conventions rather than hard constraints:

- `sharedKernel` imports concrete domain events and handlers from every bounded context;
- `adminImportContext` imports coffee commands, query DTOs, projection repositories, and photo/opening-hour projection types;
- outbox metadata classification is centralized in `OutboxDomainEventPublisher` through `instanceof` checks against all event classes;
- SQS routing is centralized in `SqsIntegrationEventRouter` with direct handler dependencies;
- delete is physical today and can conflict with future mobile/offline semantics;
- S3 cleanup is not part of the photo/cafe delete flow;
- tests are broad, but the Maven/test script strategy is not cleanly separated into unit/integration/Testcontainers profiles.

The system is at an honest CQRS/EDA level for a staging MVP. It is not yet at a level where adding 5 more contexts would stay cheap without refactoring the messaging/routing boundaries.

## 2. What Is Very Solid

### Coffee creation ownership

`ImportGooglePlaceCoffee` delegates to `CreateCoffeeCommand` through `CoffeeCreationPort`. It does not write the `coffees` table or projection tables directly. This preserves the coffee context as owner of the real creation decision.

### Idempotence on Google identity

`CreateCoffeeCommandHandler` checks `existsByGooglePlaceId`, and the database has a partial unique index on `coffees.google_place_id`. This is the right combination: application-level clear behavior plus database-level race protection.

### Projection sync language

`ProjectionSyncEvent` exposes a projection-oriented event:

```text
projection.updated
projection=coffees
scope=entity
entityId=...
hints=[summary|photos|openingHours|deleted]
```

This is the correct language for clients. It avoids leaking `CoffeeCreatedEvent`, `CoffeePhotosImportedEvent`, etc. to Studio or mobile.

### Photo persistence model

The durable photo model is sane:

```text
Google temporary URI
-> backend download
-> CoffeePhotoStorage
-> s3:// stable reference
-> projection
-> presigned URL at read boundary
```

This avoids storing expiring Google photo URLs and keeps the frontend ignorant of Google/S3 internals.

### Staging realism

Staging uses:

- SQS enabled;
- Kafka disabled;
- local event bus disabled;
- CloudFormation-managed queues and DLQ;
- GitHub Actions resolving SQS URLs from stack outputs;
- Docker Compose with Caddy and HTTPS;
- EC2 runtime IAM permissions for SQS/S3.

That is materially better than a local-only architecture that only works in tests.

## 3. What Is Fragile

### `sharedKernel` is no longer purely technical

`sharedKernel` currently imports concrete classes from many contexts:

- `SqsIntegrationEventRouter` imports coffee, article, auth, social, ticket, and user handlers and events.
- `OutboxDomainEventPublisher` imports domain event classes to derive aggregate metadata.
- `DefaultDomainEventRouter` imports domain event classes to decide routing.
- `WebSocketOutboxEventSender` imports social and ticket domain events.

This means `sharedKernel` is acting partly as a platform kernel and partly as a global application composition module. That is workable in a monolith, but it violates the documented rule that sharedKernel should contain plumbing, not BC routing logic.

The risk is change amplification: adding a domain event forces edits in sharedKernel. That is the opposite direction of a stable kernel.

### `adminImportContext` knows too much about coffee internals

`adminImportContext` imports:

- `CreateCoffeeCommand`;
- `DeleteCoffeeCommand`;
- `AddCoffeePhotoCommand`;
- `DeleteCoffeePhotoCommand`;
- `ListCoffeesQuery`;
- coffee projection repositories;
- coffee projection view classes;
- `CoffeeSummaryResponse`;
- `CoffeePhotoUriResolver`;
- `CoffeeGooglePlaceLookupPort`.

Some of this is acceptable for a backend-for-admin orchestration module, but it is not a clean bounded context boundary. `AdminCoffeesReadController` is effectively an admin adapter over coffee read/write capabilities, not pure admin import.

Recommendation: rename the mental model. This is not one admin import bounded context anymore. It has become an admin API adapter layer for coffee operations plus a Google import orchestration context.

### Physical delete is dangerous

`DeleteCoffeeCommandHandler` deletes the coffee row. `CoffeeDeletedEventHandler` deletes summary, photos, and opening hours projections.

This is currently useful for staging cleanup, but it is dangerous for mobile/offline use:

- a mobile client may hold references to a deleted coffee;
- social/ticket/article contexts may later reference coffee IDs;
- command retries against a physically deleted aggregate become ambiguous;
- S3 objects are not deleted;
- auditability is lost.

For a product catalog, `ArchiveCoffee` is usually safer than `DeleteCoffee`. Physical delete should be admin-only maintenance or hard GDPR-like erasure with a separate name.

### Photo delete is projection-only cleanup today

`DeleteCoffeePhotoCommandHandler` publishes `CoffeePhotoDeletedEvent`, and the handler deletes from `coffee_photos_projection`. The S3 object remains.

This is acceptable as MVP debt, but it must be explicit. Otherwise photo delete means "hide from Fragments read model", not "delete object from storage".

### Logging can leak payloads

Several components log full payloads:

- `LoggingOutboxEventSender` logs `payload_json`;
- Kafka listeners log payloads on errors;
- some `System.out.println` remain in sharedKernel/social/ticket/websocket code.

Current payloads may be low-risk, but auth, ticket OCR, comments, and future user data make this unsafe. Logs should identify event id/type/aggregate, not full payload by default.

## 4. What Is Dangerous

### Inbox duplicate handling can suppress failed retries

`InboxMessageRepository.claim` inserts `(destination, event_id)` and returns false on duplicate. `SqsIntegrationEventRouter.route` marks a claimed message `FAILED` if handler fails.

On redelivery, `claim` sees the existing row and returns false, so the retry is suppressed even though the previous processing failed.

This is a correctness risk. In at-least-once messaging, a failed inbox record must not be treated the same as a processed duplicate. The claim logic should distinguish:

- `PROCESSED`: suppress;
- `RECEIVED`/stale: recover or retry carefully;
- `FAILED`: allow retry or move through a controlled replay path.

This is a P0/P1 boundary depending on whether current staging sees real handler failures. Architecturally it is the most important reliability issue found.

### Outbox dispatcher marks SENT after publishing but before consumer success

This is normal for outbox-to-transport semantics, but the naming matters. `SENT` means "sent to transport", not "business effect completed". Command status is marked `APPLIED` from the outbox event after send.

For commands whose visible result requires SQS consumer/projection completion, `APPLIED` can be interpreted too strongly by clients. The system currently uses SSE/GET for read freshness, so this is not immediately broken, but the command status documentation should distinguish:

- write decision applied;
- projection visible.

### SQS router mixes projection and process manager behavior

For `coffee.created`, `SqsIntegrationEventRouter` does three things:

```text
coffeeCreatedHandler.handle(event)
importGoogleOpeningHoursForCoffee.handle(event)
importGooglePhotosForCoffee.handle(event)
```

This works and kept the flow simple. But architecturally this is a process manager hidden in a transport router. It is deciding the follow-up workflow for a coffee creation.

In a Particular/Udi Dahan style, this should become a named process/policy handler owned by coffee context, e.g. `CoffeeCreatedEnrichmentPolicy`, not a special case in the shared SQS router.

### `spring.sql.init.mode=always` plus runtime schema execution

`application.properties` sets:

```properties
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
```

The deploy workflow also copies `schema.sql` and `data.sql`, then executes schema through psql. This creates drift risk and unclear ownership of schema lifecycle.

For staging MVP this is survivable. For production it should move to Flyway/Liquibase or at least a single explicit migration mechanism.

## 5. What Is Too Complex

### Multiple messaging paths still coexist

The codebase still contains:

- SQS transport;
- Kafka senders/listeners;
- local EventBus;
- WebSocket ACK sender;
- projection sync SSE.

Conceptually these have roles, but in code they overlap. `StableEnvelopeOutboxEventSender` is now primary, but legacy Kafka/local components remain active depending on properties. This requires discipline in every environment.

Recommendation: keep Kafka disabled for staging/prod, but either quarantine legacy Kafka behind a `legacy` package/profile or remove it once mobile flows no longer depend on it.

### Admin read aggregation in admin import controller package

`AdminCoffeesReadController` aggregates coffee summaries, photos, and opening hours directly from coffee read repositories. Functionally correct, but the class name/package suggests import while it has become admin coffee management.

This should become an explicit admin adapter over coffee read model:

```text
adminContext/adapters/primary/rest/AdminCoffeeController
coffeeContext/read/AdminCoffeeReadModelQuery
```

The aggregation should live on the coffee read side or behind a coffee read port, not in the import orchestration package.

## 6. What Is Missing

### Storage cleanup policy

Missing:

- delete S3 object on photo delete;
- delete all S3 objects on coffee hard delete;
- async cleanup retry/DLQ strategy;
- orphan object reconciliation job.

Immediate hard delete can be best-effort, but durable cleanup should be event-driven:

```text
CoffeePhotoDeletedEvent
-> PhotoStorageCleanupRequested
-> SQS
-> S3 delete
-> CoffeePhotoStorageCleanedEvent or log/metric
```

For now, a simpler port called from a handler is acceptable if documented.

### Inbox retry semantics

As noted above, failed inbox rows need a retry story.

### Operational replay tooling

There is manual knowledge around replaying FAILED outbox rows, but no clear script/runbook for:

- replay outbox by event type/status;
- inspect DLQ;
- redrive DLQ;
- replay projection sync;
- diagnose missing projection update.

### Architecture tests

The project needs tests that enforce boundaries:

- sharedKernel must not import bounded contexts except an explicit allowed composition package;
- adminImport business logic must not import coffee adapters/repositories;
- controllers must not write projection tables;
- projection handlers may publish `ProjectionSyncEvent`; command handlers may not.

## 7. What Should Be Simplified

### Move event metadata out of `OutboxDomainEventPublisher`

The publisher should not know every event class. Options:

1. Add metadata methods to `DomainEvent` or a companion interface:
   - `aggregateType()`
   - `aggregateId()`
   - `streamKey()`
2. Register per-context `DomainEventDescriptor` beans.
3. Use an annotation on event classes.

Option 2 is the best compromise: keeps domain events clean while moving classification ownership to contexts.

### Move SQS dispatch ownership out of sharedKernel

The SQS consumer should route an envelope to registered handlers by stable event type/destination. Each context should register handlers/policies. The shared router should not explicitly call `coffeeCreatedHandler` or enrichment handlers.

Target:

```text
SqsIntegrationEventConsumer
-> IntegrationEventHandlerRegistry
-> handler(s) registered by coffeeContext
```

This reduces sharedKernel coupling and makes new contexts cheaper.

### Split admin import from admin coffee management

Keep:

- `adminImportContext`: Google search/preview/import orchestration.

Introduce or rename:

- `adminCoffeeContext` or `adminContext/adapters`: admin routes over coffee read/write operations.

Do not create a second coffee use case. Just clarify ownership.

## 8. Immediate Corrections

### P0

1. Fix inbox retry semantics.
   - Do not suppress redelivery for `FAILED` rows.
   - Add tests for fail -> redelivery -> success.
   - Add tests for processed duplicate -> suppressed.

2. Stop logging full event payloads by default.
   - Remove `System.out.println`.
   - Change payload logs to event id/type/aggregate.
   - Keep full payload only behind explicit debug and never for auth/ticket/user-sensitive data.

3. Make hard delete semantics explicit.
   - Rename admin hard delete if it remains physical.
   - Prefer `ArchiveCoffeeCommand` for normal product behavior.
   - Document whether `/api/admin/coffees/{id}` is cleanup or product delete.

### P1

1. Extract event metadata/routing from `OutboxDomainEventPublisher`.
2. Extract SQS handler registration from `SqsIntegrationEventRouter`.
3. Add S3 cleanup for photo delete and coffee delete.
4. Add architecture tests for cross-context imports.
5. Introduce Maven profiles or scripts to separate unit vs Testcontainers.

### P2

1. Migrate schema management to Flyway/Liquibase.
2. Remove/quarantine Kafka legacy code.
3. Add CloudWatch alarms for outbox FAILED, DLQ depth, SQS age, backend health, and disk usage.
4. Add retention/compaction for `projection_sync_events`.

## 9. What Can Wait

- Multi-instance SSE fanout. Postgres polling is enough for current staging traffic.
- Dedicated S3 bucket for Fragments. Prefix isolation is acceptable short term if IAM remains prefix-scoped.
- Full saga framework. Named policy handlers are enough for now.
- Complex photo deduplication beyond deterministic source-based IDs.
- Per-projection SSE endpoints. A single stream is the right default.

## 10. Risks At 3 Months

- Adding more coffee admin features will deepen `adminImportContext` -> `coffeeContext` coupling unless admin coffee management is separated.
- More events will increase edits in sharedKernel routing files and create recurring routing bugs.
- Failed SQS messages may be silently suppressed after first failure because of current inbox duplicate behavior.
- Physical delete may break mobile assumptions once mobile stores coffee references offline.
- S3 orphan objects will accumulate.
- Test runs will remain inconsistent locally if Testcontainers failures are handled ad hoc.

## 11. Risks At 12 Months

- sharedKernel becomes a de facto application layer and blocks modular extraction.
- Stable event contracts become Java-class-name dependent because router/envelope logic still knows concrete classes.
- Projection sync table grows without retention and makes reconnect scans slower.
- SSE polling per connection becomes expensive beyond hundreds/thousands of clients.
- Docker Compose Postgres on EC2 becomes operational debt without backup/restore drills.
- Bucket sharing with Anchor becomes risky if more environments/prefixes are added without formal IAM conditions and lifecycle policies.

## 12. Coffee Context Audit

### `Coffee` aggregate

`Coffee` is a real aggregate structurally: it has identity, value objects, version, timestamps, and mutation methods. But current high-value flows mostly bypass aggregate behavior:

- photos are stored and emitted by use cases rather than `Coffee.addPhoto`;
- photo delete does not inspect aggregate photo membership;
- opening hours imported event replaces projection but does not update write aggregate;
- delete physically removes the row rather than calling aggregate behavior.

This is not fatal. It means coffee currently behaves as a reference/catalog aggregate with thin invariants. Do not overbuild it until real invariants emerge.

Potential real invariants:

- unique Google identity;
- valid geolocation;
- allowed photo content types/sizes;
- max number of admin photos;
- archived coffee cannot accept new photos;
- imported Google photos should not duplicate previous imports.

### Events

Good:

- `CoffeeCreatedEvent` is a real past-tense fact.
- `CoffeePhotosImportedEvent` and `CoffeeOpeningHoursImportedEvent` are separate enrichment facts.
- `CoffeePhotoAddedEvent` and `CoffeePhotoDeletedEvent` are clear enough for admin actions.

Weak:

- `CoffeeDeletedEvent` may be too strong for current product semantics. It means hard deletion, not archival.
- `CoffeeOpeningHoursImportedEvent` can publish empty descriptions. That may be fine, but it should mean "we checked and found none" or "we imported empty"; today that distinction is unclear.

### Projections

The three projection tables are coherent:

- `coffee_summaries_projection`;
- `coffee_photos_projection`;
- `coffee_openinghours_projection`.

Handlers publish sync only after projection mutation, which is the correct rule.

Weaknesses:

- no foreign keys from photo/opening hours projection to summary projection. This is normal for projections, but cleanup depends on handlers.
- `tags_json` is manually assembled/parsed as strings. Prefer Jackson/JSONB binding.
- projection versions for enrichment use event version, but the aggregate version is not necessarily changed for imported photos/opening hours. This weakens version semantics.

## 13. Admin Import Context Audit

The Google import flow is conceptually right:

```text
search
-> preview
-> generate coffee UUID
-> CreateCoffeeCommand
-> CoffeeCreatedEvent
-> async enrichment
```

The context is mostly orchestration, not coffee business logic.

Problems:

- `CoffeeCreationPort` accepts `CreateCoffeeCommand`, a coffee internal type. A cleaner port would accept an admin import primitive DTO and have the adapter build the command.
- admin read/delete/photo operations are now in `adminImportContext`, which stretches the name and responsibility.
- `CommandBusCoffeeCreationPort` checks existence through `CoffeeGooglePlaceLookupPort` before dispatch. This is reasonable for returning `ALREADY_IMPORTED`, but it duplicates the command handler idempotence decision. The DB unique index remains the true race protection.

Recommendation:

- keep the use case flow;
- introduce primitive `CoffeeCreationRequest` in admin import if coupling starts hurting;
- move generic admin coffee read/write endpoints out of admin import package.

## 14. Photos Audit

### Healthy

- temporary Google photo URIs are not persisted;
- backend downloads the bytes;
- durable storage uses `CoffeePhotoStorage`;
- S3 projection stores `s3://bucket/key`;
- read side resolves presigned URLs;
- import limit is configurable;
- LocalStack/S3 unit tests exist.

### Fragile

- S3 object cleanup is missing;
- content type trust is based on upload metadata;
- upload size is now configured, but business-level validation remains thin;
- Google author attribution is captured in preview but not persisted;
- imported Google photos replace photo projection for coffee; admin-added photos append. A later re-import can wipe admin-added photos if `CoffeePhotosImportedEventHandler.replaceForCoffee` runs after manual additions.

That last point is important. The system needs a photo source model if Google re-imports become recurring:

```text
photo source = GOOGLE | ADMIN_UPLOAD
externalSourceName
storageUri
status
```

For now, because import is initial post-create, replacement is acceptable. Future refresh requires changing this.

## 15. SQS / Outbox / Inbox Audit

### Strong

- outbox table exists with cursor id, event id, aggregate metadata, payload, status, retry count;
- SQS publisher uses stable envelopes;
- queue URLs are resolved from CloudFormation outputs;
- consumer long-polls unique queue URLs in parallel;
- messages are deleted only after handler success;
- DLQ is configured by CloudFormation;
- inbox uniqueness exists on `(destination, event_id)`.

### Severe issues

- failed inbox redelivery is likely suppressed;
- shared router directly invokes concrete handlers and policies;
- outbox failed replay is manual, not tooled;
- retry count has no exponential delay;
- outbox dispatcher processes events in one transaction while doing network sends;
- `SENT` can mean sent to logging publisher plus SQS, not fully consumed;
- no metrics/alarms for FAILED outbox, DLQ depth, age of oldest message.

### Exactly-once reality

The system should be described as at-least-once with idempotent handlers. It should not imply exactly-once. Projection handlers are mostly idempotent:

- summary upsert is idempotent;
- opening hours replace is idempotent;
- photo import replace is idempotent;
- photo add append uses deterministic photo id from storage and upsert;
- photo delete is idempotent.

That is good. The inbox retry issue is the main gap.

## 16. Projection Sync / SSE Audit

### Strong

- durable `projection_sync_events`;
- native SSE `id`;
- `Last-Event-ID` replay;
- heartbeat;
- frontend can ignore heartbeat and refetch via GET;
- Caddy disables compression on SSE and uses `flush_interval -1`;
- admin and non-admin routes share contract but differ by auth;
- no domain event leaks to client.

### Fragile

- dispatcher polls DB per connection every second. At 100 connections this is fine. At 1,000 it is 1,000 polls/sec. At 10,000 it is not viable.
- no retention policy for `projection_sync_events`;
- malformed old `Last-Event-ID` silently starts at current offset instead of telling client to resync;
- no explicit `sync.resync_required` implementation for cursor outside retention;
- no backpressure model if clients reconnect after a long outage.

### Recommendation

Keep the current mechanism for MVP. Before 1,000 persistent clients:

- move polling to a shared broadcaster per JVM;
- use Postgres `LISTEN/NOTIFY` or one scheduled poll feeding all emitters;
- add retention and `sync.resync_required`;
- add metrics for active emitters, send failures, replay batch sizes.

## 17. API / Security Audit

### Good

- admin token chain is isolated to `/api/admin/**`;
- Google social auth is separate;
- CORS is global and config-driven;
- admin routes require `Authorization: Bearer <ADMIN_SECURITY_TOKEN>`;
- missing admin token keeps routes closed;
- preflights are permitted.

### Concerns

- `/api/coffees/**` is currently public in `AuthSecurityConfiguration`. That may be a product choice, but it conflicts with earlier Studio motivation that `/api/coffees` was 401. Document the current choice.
- admin token is a shared secret. Acceptable for staging/admin tool, not long-term operator identity.
- multipart errors can be converted through security/error paths if not covered.
- local photo asset endpoint is public. For local backend this is fine; in S3 mode presigned URLs are better.
- logs should never include auth headers or full sensitive payloads.

Future target:

- admin user identity or OAuth client credentials;
- role-based admin scopes;
- audit log for destructive admin actions.

## 18. AWS / Staging Audit

### Healthy

- CloudFormation owns ECR, EC2, SQS, DLQ, IAM roles, EIP, security group;
- GitHub OIDC role is managed by stack;
- GitHub deploy role has bounded ECR/EC2/CloudFormation permissions;
- EC2 runtime role is scoped to SQS queues and S3 prefix;
- Caddy handles HTTPS and SSE correctly;
- backend port is bound to localhost;
- workflow resolves SQS URLs from CloudFormation outputs;
- workflow upserts `.env` without printing secrets.

### Fragile

- deployment still uses inbound SSH from GitHub runners;
- Docker Compose Postgres on EC2 has no documented backup/restore drill;
- schema is applied by psql, while Spring also has SQL init;
- data.sql is copied into initdb but not applied on existing DB, which is probably desired now, but should be explicit;
- bucket is shared with Anchor and isolated by prefix, not by bucket;
- workflow recently wrote `.env` but did not recreate backend until manually fixed/verified. The image mismatch guard is a good improvement, but it should be watched.

## 19. Tests Audit

Current rough counts:

- `src/main/java`: 375 files;
- `src/test/java`: 91 files;
- `*IT.java`: 30 files;
- Testcontainers-related files found: 2 by direct annotation/container search, plus broader integration tests using shared bases.

### Strengths

- command handler unit tests exist;
- admin security/controller tests exist;
- Google gateway tests exist;
- projection handler tests exist;
- SQS consumer tests exist, including LocalStack;
- S3 storage tests exist;
- SSE controller/dispatcher/repository tests exist;
- coffee idempotence and projection tests exist.

### Weaknesses

- no clean Maven separation between unit tests and integration tests;
- default `mvn test` can pull in Docker/Testcontainers depending on naming and Surefire config;
- no `failsafe` setup for `*IT`;
- no standard `scripts/test-unit.sh`, `scripts/test-integration.sh`, `scripts/test-all.sh`;
- boundary architecture tests are missing;
- failure mode tests for inbox FAILED redelivery are needed;
- S3 delete cleanup tests do not exist because cleanup does not exist.

### Anchor comparison

Anchor has `scripts/backend-testcontainers`. Fragments already has the equivalent at `scripts/backend-testcontainers`, adapted to repository root. The script:

- checks Docker availability;
- resolves `DOCKER_HOST`;
- sets `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` for Unix sockets;
- runs Maven.

So the next step is not to copy Anchor. The next step is to standardize test entrypoints around the existing runner:

```text
scripts/test-unit.sh
scripts/test-integration.sh
scripts/test-all.sh
scripts/testcontainers-check.sh
```

Recommended behavior:

- `test-unit.sh`: no Docker, Surefire includes `*Test`, excludes `*IT`.
- `test-integration.sh`: uses `scripts/backend-testcontainers`, runs `*IT` and LocalStack/S3/SQS tests.
- `test-all.sh`: unit then integration.
- `testcontainers-check.sh`: Docker context/info/probe only.

Maven should use:

- Surefire for unit tests;
- Failsafe for integration tests;
- naming convention: `*Test` vs `*IT`;
- CI jobs split accordingly.

## 20. Documentation Audit

The docs are unusually strong for an MVP. They cover:

- bounded contexts;
- backend architecture;
- SQS;
- SSE;
- command status;
- staging runbook;
- Testcontainers local runbook;
- coffee context README with photo flow.

Gaps:

- no explicit audit/runbook for inbox retry/replay;
- no S3 cleanup policy;
- no hard delete vs archive decision record;
- no admin API contract document;
- no current architecture diagram showing all actual paths after photos/delete;
- no matrix mapping environment variables to owner/source/default/risk;
- sharedKernel README says no business routing, but code currently has routing.

## 21. Test Matrix Recommendation

| Area | Unit | Integration | Testcontainers | E2E |
| --- | --- | --- | --- | --- |
| Coffee create/idempotence | Required | JPA unique index | Postgres | Optional |
| Coffee delete/archive | Required | projection cleanup | Postgres | Studio flow |
| Photo add/delete | Required | projection + storage fake | S3 LocalStack | Studio flow |
| Google import | Required with fake gateway | HTTP gateway mock server | no real Google | staging smoke |
| Outbox dispatch | Required | DB status transitions | Postgres | staging SQS |
| Inbox duplicate/retry | Required | DB unique/status | Postgres/SQS LocalStack | staging redrive |
| SQS consumer | Required | router registry | LocalStack | staging queue |
| SSE replay | Required | repository | Postgres | browser reconnect |
| Security/CORS | MockMvc | security chains | no | staging curl |
| Deployment | YAML validation | no | no | GitHub Actions smoke |

## 22. Prioritized Plan

### P0 - Reliability and safety

1. Fix inbox FAILED redelivery semantics.
2. Remove full payload logging and `System.out.println`.
3. Decide and document hard delete vs archive; make admin hard delete explicit if kept.
4. Add tests for outbox/SQS failure modes: duplicate, failed retry, delete-message failure.

### P1 - Boundary hardening

1. Extract event descriptors from `OutboxDomainEventPublisher`.
2. Replace concrete `SqsIntegrationEventRouter` dependencies with handler registry.
3. Move admin coffee management out of `adminImportContext` naming.
4. Add architecture tests for forbidden imports.
5. Add S3 cleanup for photo/cafe deletion.

### P2 - Operational maturity

1. Adopt Flyway/Liquibase.
2. Add CloudWatch alarms and dashboards.
3. Add sync event retention and `sync.resync_required`.
4. Split Maven Surefire/Failsafe and scripts.
5. Quarantine/remove Kafka legacy code from MVP runtime.

## 23. Concrete Next Actions

Suggested next sprint:

1. Add an architecture test suite:
   - sharedKernel forbidden BC imports except approved adapter package;
   - admin import business logic cannot import coffee adapters/repositories;
   - command handlers cannot publish projection sync directly.
2. Fix inbox retry semantics and add LocalStack/Postgres tests.
3. Remove payload logs and `System.out.println`.
4. Add `scripts/test-unit.sh`, `scripts/test-integration.sh`, `scripts/test-all.sh`, `scripts/testcontainers-check.sh`.
5. Write `docs/architecture/delete-vs-archive.md` and decide product semantics.

Second sprint:

1. Extract event metadata descriptors.
2. Extract SQS handler registry.
3. Add S3 cleanup policy.
4. Update runbooks for replay/DLQ.

## 24. Open Questions

1. Is `/api/coffees/**` intended to be public for mobile discovery, or should mobile always authenticate?
2. Should admin coffee delete be a product archive or a staging cleanup tool?
3. Should Google photo refresh preserve admin-uploaded photos?
4. Should photo attribution be persisted for compliance/product display?
5. What is the target retention for `projection_sync_events`?
6. What is the first non-admin client for SSE: mobile or Studio only?
7. When does Docker Compose Postgres become RDS?
8. Should Fragments get its own S3 bucket before production, or is prefix isolation enough?

## 25. Final Assessment

FragmentsClean is not over-engineered in the places that matter most: outbox/SQS and projection sync solve real product consistency problems. The architecture is serving the business when it keeps import, enrichment, projection freshness, and frontend refresh decoupled.

The current danger is not too much architecture. The danger is architecture without enforcement. The code has grown faster than the boundaries. The next improvements should focus on making the existing decisions enforceable:

- sharedKernel technical again;
- context-owned event metadata and handlers;
- reliable inbox retry semantics;
- explicit delete/archive semantics;
- disciplined test entrypoints.

Do that, and the platform can absorb articles, wallet, gamification, notifications, and mobile sync without reinventing the pipeline.
