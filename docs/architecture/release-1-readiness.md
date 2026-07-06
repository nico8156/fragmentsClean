# Release 1.0 Architecture Readiness

This document records the architecture conditions for shipping Fragments 1.0.
It is intentionally pragmatic: the goal is a reliable product release, not
architectural purity.

## Decision

Fragments 1.0 ships as a Spring Boot modular monolith with:

- HTTP commands;
- transactional outbox;
- SQS propagation;
- inbox-backed idempotence;
- read projections;
- Projection Sync SSE for read-model freshness;
- `/commands/{commandId}` as canonical command status.

The dangerous debt would be confusing current compromises with final
principles. Every compromise below is acceptable only while it remains visible
and bounded.

## Blocking Before Release

### Mobile Critical Path

The release is blocked unless these flows are verified against the deployed
backend:

- auth login/refresh/me;
- coffee list/details/photos/opening hours;
- admin import if Studio is part of release operations;
- ticket verify command and status read model;
- likes/comments if exposed in the release UX;
- offline write retry;
- `/commands/{commandId}` reconciliation;
- foreground refresh;
- network error handling without false business rollback.

Reason: Fragments is mobile-first. Backend correctness is not enough if mobile
state reconciliation is unreliable.

### Outbox, SQS, Inbox Failure Semantics

The release is blocked unless the SQS path preserves at-least-once correctness:

- a handler failure must not delete the SQS message;
- an inbox `PROCESSED` duplicate is suppressed;
- an inbox `FAILED` duplicate can be retried;
- an inbox `RECEIVED` redelivery can be retried;
- duplicate deliveries do not duplicate business effects;
- DLQ depth can be inspected.

Reason: SQS is the backend propagation channel. If retry semantics are wrong,
projection freshness and async workflows become unreliable.

### Archive vs Hard Delete

Product removal from the coffee catalog must use `ArchiveCoffee`.

Hard delete is reserved for explicit technical maintenance and must not become
the product default for mobile-visible catalog entities.

Reason: mobile clients can hold offline references. Physical deletion creates
ambiguous states for retries, projections, social references, and future
features.

### Client Synchronization Model

The release must have a clear operational model:

```text
HTTP command
-> command status

Projection update
-> ProjectionSyncEvent
-> SSE
-> client GET
-> snapshot reducer
```

STOMP/WebSocket ACKs are not part of the active architecture. SSE must not be
used as an ACK replacement; clients use command status for command lifecycle and
Projection Sync only for read-model freshness.

## Important Before Release

### Runtime Schema Strategy

`schema.sql` is still applied directly during staging deploys and Spring SQL
init is enabled. This is acceptable only while the database has no long-lived
migration history requiring ordered migrations.

Before user data becomes non-disposable, move to Flyway or Liquibase.

Until then:

- schema statements must be idempotent;
- destructive schema changes require an explicit runbook;
- staging reset must be intentional;
- deploy logs must not dump data or secrets.

### Sensitive Logging

Main code must not print through `System.out`.

Logs must identify events by:

- event id;
- event type;
- command id when applicable;
- aggregate id/type;
- projection sync id.

Logs must not dump:

- tokens;
- OAuth payloads;
- ticket OCR text;
- full comments;
- full event payloads by default;
- S3 presigned URLs when avoidable.

### Architecture Guardrails In CI

Architecture tests must remain part of CI. At minimum they guard:

- sharedKernel does not import bounded contexts;
- bounded contexts do not import each other except documented temporary edges;
- write side does not publish `ProjectionSyncEvent`;
- main code does not use `System.out`.

## Debt Accepted After Release

These are acceptable only as visible debt:

- some consumers still deserialize producer Domain Event classes;
- sharedKernel remains broad;
- schema migrations are not yet Flyway/Liquibase-managed.

## Exit Criteria For Phase 1

- targeted backend guard tests pass;
- full backend tests pass when Docker/Testcontainers is available;
- staging runbook covers outbox/inbox/DLQ/SSE/photos;
- admin coffee removal dispatches archive, not hard delete;
- no new Kafka dependency, listener, or runtime property exists.
