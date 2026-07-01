# Backend Agent Guide - Fragments

This guide applies to `/Users/nicolasmaldiney/fragmentsClean` and must be read together with the root `AGENTS.md`.

## Backend Shape

The backend is a Spring Boot modular monolith with pragmatic DDD, CQRS, ports/adapters, transactional outbox, SQS-ready eventing, inbox idempotence, and explicit read models.

## Feature Classification

Before coding, classify the task:

1. Command feature: changes state inside one bounded context.
2. Query feature: exposes read-side data.
3. Projection feature: creates or updates a read model.
4. SQS consumer: consumes an integration event.
5. External adapter: talks to Google, OCR engine, filesystem, S3, SQS, SMTP, etc.
6. Architecture/doc feature: changes rules, docs, deployment policy, or guardrails.

Use the matching orchestrator in `.agents/backend/orchestrators`.

## Non-Negotiable Rules

- Controllers stay thin.
- Writes go through command handlers.
- Aggregates/domain models protect invariants.
- Read endpoints use query handlers.
- Read models are not aggregates.
- Write side must not read projection tables for decisions.
- Read side must not call command handlers.
- New cross-BC behavior must use events and local projections.
- New prod async transport is SQS, not Kafka.
- Consumers must use inbox idempotence.
- Event contracts must stay stable and primitive-only.
- WebSocket ACK is best-effort only.
- `/commands/{commandId}` is the canonical status source.
- Secrets come from environment or AWS secret mechanisms.

## Layering

Primary adapters:
- HTTP controllers
- SQS consumers
- legacy/dev pollers when explicitly enabled
- scheduled dispatchers

Application layer:
- command handlers
- query handlers
- projection handlers
- process managers only when a long-running process truly needs coordination state

Domain layer:
- aggregates/entities
- value objects local to one BC
- domain services where needed
- repository ports
- domain events

Secondary adapters:
- JPA repositories
- JDBC repositories
- SQS senders
- inbox/outbox stores
- external HTTP clients
- OCR/process adapters
- object storage adapters

## Persistence

Write side:
- JPA default for aggregates.
- One transaction should persist aggregate state and outbox event together.

Read side:
- JDBC / SQL default.
- Upsert projections when possible.
- Projection handlers must be idempotent.

Technical tables:
- outbox
- inbox
- command status
- delivery attempts
- configuration

## Testing

Expected tests:
- pure domain tests for invariants
- command handler tests with fake ports
- query handler tests with fake or test DB repositories
- repository integration tests with Testcontainers when persistence matters
- web tests with MockMvc for HTTP contracts
- SQS/inbox tests for duplicate delivery and retry behavior
- architecture tests for forbidden dependencies when practical

Testcontainers require Docker.

## Architecture Review Before Done

Before declaring a backend task complete, verify:
- no controller repository access
- no cross-BC domain imports
- no direct table access to another BC without documented exception
- command/event names express business intent/fact
- event contract is stable and versioned
- inbox/idempotence exists for event consumers
- SQS route does not run in parallel with the same legacy DB poller route
- command status behavior is compatible with mobile offline-first

