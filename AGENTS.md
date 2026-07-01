# AGENTS.md - Fragments

## Authority

This document is the architecture doctrine for Fragments.

All backend work, mobile-facing contracts, agent orchestration, documentation, and code generation must comply with it. When a local document is more specific, it may add constraints, but it must not weaken this doctrine.

Fragments is not a monorepo:
- backend: `/Users/nicolasmaldiney/fragmentsClean`
- mobile: `/Users/nicolasmaldiney/fragmentsCleanFront`

## Mission

Fragments is a mobile-first coffee product built as:
- a Spring Boot modular monolith backend
- a React Native / Expo offline-first mobile app
- PostgreSQL as the system database
- AWS SQS as the target asynchronous transport for MVP production

The goal is not to maximize architecture ceremony. The goal is to keep business ownership clear, mobile interactions resilient, deployment realistic, and future changes predictable.

## Product Principles

- Backend is the source of truth.
- Mobile owns the user experience.
- Mobile writes are offline-first.
- Commands express intent.
- Events describe facts that happened.
- WebSocket ACKs are opportunistic.
- `GET /commands/{commandId}` is the canonical source of command status.
- A missing socket ACK must not block reconciliation.
- Network failure must not mean business rejection.

## Architecture Defaults

Fragments uses:
- pragmatic DDD
- pragmatic CQRS
- modular monolith
- ports and adapters
- event-driven architecture
- transactional outbox
- inbox-backed idempotence
- stable event envelopes
- SQS as production transport
- explicit local projections for consuming bounded contexts

No Kafka in production MVP.
No Redis in production unless a critical, documented need is proven.

## Current Bounded Contexts

Active backend bounded contexts:
- `authenticationContext`: OAuth/JWT/session identity.
- `userApplicationContext`: application user profile and product identity.
- `coffeeContext`: coffee places, details, photos, opening hours, discovery read models.
- `articleContext`: editorial content.
- `socialContext`: likes, comments, social interactions and their projections.
- `ticketContext`: ticket submission, OCR/verification pipeline, entitlements source signals.
- `sharedKernel`: technical abstractions only.

The mobile app mirrors these capabilities as `*Wl` client contexts where useful:
- `appWl`
- `userWl`
- `coffeeWl`
- `articleWl`
- `likeWl`
- `commentWl`
- `ticketWl`
- `outboxWl`
- `wsWl`
- `locationWl`
- `entitlementWl`

## Ownership Rules

Each bounded context owns its concepts.

A bounded context must not:
- import another bounded context's aggregate, entity, value object, repository, use case, command handler, adapter, or internal DTO
- read or write another bounded context's business tables directly for normal behavior
- mutate another bounded context's projections
- publish another bounded context's business events

A bounded context may:
- consume stable integration event contracts
- maintain local references/projections from those events
- expose primitive-only ACL ports as temporary documented debt
- call sharedKernel technical abstractions

The target model is:

```text
owning BC emits stable event
-> SQS destination
-> consuming BC inbox
-> consuming BC handler
-> local projection/reference/read model
```

## Backend Flow Rules

Write path:

```text
HTTP controller
-> command
-> command handler
-> aggregate/domain model
-> repository port
-> adapter
-> domain event
-> transactional outbox
-> dispatcher
-> SQS / optional local event bus / opportunistic WebSocket ACK
```

Read path:

```text
HTTP controller
-> query object
-> query handler
-> read repository
-> read model DTO
```

Async consumption path:

```text
SQS message
-> stable event envelope
-> inbox idempotence check
-> typed handler
-> domain transition and/or projection update
-> optional outbox event
-> delete message after success
```

## Mobile Flow Rules

Critical write path:

```text
screen
-> view model callback
-> Redux listener/use case
-> optimistic reducer
-> local outbox
-> HTTP command
-> awaiting ACK
-> socket ACK if available
-> /commands/{commandId} polling fallback
-> reconcile or rollback
-> drop outbox item
```

Network error, timeout, offline, 5xx, or socket loss:
- keep optimistic UI
- keep or requeue command
- retry with backoff
- do not rollback

Rollback is allowed only when:
- backend returns an explicit business rejection, or
- `/commands/{commandId}` returns `REJECTED`.

## Persistence Policy

Write side:
- JPA by default for aggregates and business transactions.
- JDBC is allowed for technical tables, outbox/inbox, command status, simple configuration, or documented MVP debt.

Read side:
- JDBC / explicit SQL by default.
- Read models are projections and query models, not aggregates.
- Read side must not load JPA aggregates or call command handlers.

Cross-BC SQL:
- forbidden for new normal behavior
- tolerated only behind explicit primitive ACL/reference/lookup adapters and documented as debt
- target replacement is event-fed local projections

## Messaging Policy

Production MVP target:
- SQS queues
- DLQ per queue
- stable event envelopes
- event type catalog
- event versioning
- outbox sender
- inbox idempotence
- duplicate-safe handlers
- command status records

Kafka is not the production MVP transport.
If Kafka remains in dev/legacy code, it must be behind an explicit property and disabled in prod.

Event contracts:
- use primitives: `UUID`, `String`, `Instant`, numbers, booleans, arrays of primitives
- do not expose aggregates or value objects across BCs
- must remain transport-neutral
- must be versioned when the shape changes incompatibly

Idempotence:
- inbox uniqueness is keyed by handler/consumer name plus stable event/message id
- business replay safety is still required when duplicate effects can happen
- deleting inbox rows is not a normal retry strategy

## WebSocket Policy

WebSocket exists to improve UX latency only.

It must not be the source of truth.

Rules:
- socket ACK failure must not fail command processing
- mobile must fall back to `/commands/{commandId}`
- backend may emit ACKs best-effort
- no business invariant may depend on socket delivery
- production deployment must tolerate socket reconnects and missed ACKs

## Command Status Policy

`/commands/{commandId}` is the canonical command status endpoint.

It exists because the mobile app is offline-first and sockets are opportunistic.

Expected statuses:
- `PENDING`: command unknown, accepted but not projected, or still processing
- `APPLIED`: command was applied successfully
- `REJECTED`: command was explicitly rejected by business rules

The mobile app must use this endpoint to reconcile awaiting ACK records when no socket ACK arrives.

## Deployment Policy

AWS MVP target:
- Spring Boot backend container or simple EC2 runtime
- PostgreSQL, preferably RDS when budget allows
- SQS queues + DLQs
- S3 for image/object storage when remote ticket images are introduced
- CloudWatch logs and alarms
- environment variables / AWS secrets for secrets
- HTTPS through ALB or reverse proxy
- no Kafka
- no Redis
- WebSocket non-critical
- command status polling mandatory

Dangerous cost/ops items:
- MSK/Kafka
- ElastiCache without critical need
- ALB kept idle for very low traffic
- excessive CloudWatch log retention
- NAT Gateway for a tiny MVP

## Test Policy

Default:
- fake-first business tests
- mocks only at technical boundaries
- domain tests pure when invariants exist
- use case tests with fake ports
- adapter tests with real lightweight infra or Testcontainers
- web tests with MockMvc where HTTP contracts matter
- SQS/inbox tests must cover duplicate delivery and delete-failure redelivery where implemented

Mockito is acceptable at framework boundaries, but not as the default way to test business behavior.

Architecture tests are encouraged for:
- BC boundary imports
- read/write separation
- forbidden transport/config in prod
- no direct repository access from controllers

## Delivery Rule

Use iterative delivery:
1. classify the feature
2. choose the matching orchestrator under `.agents`
3. write or update tests first when behavior changes
4. implement the minimum compliant change
5. run targeted verification
6. review architecture boundaries
7. update docs when conventions change

If a requested feature cannot be implemented without violating this doctrine:
- do not approximate
- do not bypass the rule silently
- explain the violation
- propose a compliant alternative

Architecture correctness is part of delivery, not a cleanup task.

