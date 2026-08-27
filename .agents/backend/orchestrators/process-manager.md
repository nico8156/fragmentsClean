# Orchestrator - Backend Durable Process Manager

Use this only when a workflow spans transactions, asynchronous messages,
external systems, or operator delays and therefore needs durable coordination
state. Article assisted authoring is the reference workflow.

Read `docs/architecture/article-authoring-saga.md` before changing the article
authoring saga.

## Responsibilities

- keep coordination in the application layer;
- leave business invariants in the owning aggregate and domain policies;
- persist process state through an owning-context repository port;
- expose explicit, tested state transitions;
- recover after process death, timeout and message redelivery;
- preserve command status, outbox, SQS, inbox and projection boundaries.

## Required design

1. Identify the owning bounded context and aggregate.
2. Prove that durable coordination is necessary; do not introduce a saga for a
   single transaction.
3. Define states, allowed transitions, terminal states and recovery paths.
4. Separate command status from long-running process status.
5. Persist business transitions, process transitions and outbox events in one
   short transaction when they form one consistency boundary.
6. Use a durable lease/work record before remote work.
7. Execute remote calls outside database transactions.
8. Complete remote work through a new idempotent command/transaction.
9. Consume public integration contracts through inbox-backed handlers.
10. Project process progress for queries; use SSE only to request a GET refresh.

## Domain quality

- A process manager is not an anemic state record with public setters.
- Transition methods must use business/process language and reject invalid
  source states.
- It must not duplicate aggregate content or decide aggregate invariants.
- Provider DTOs, SDK types and serialization annotations stay in adapters.
- Optimistic versioning and stale completion handling are mandatory.

## External work protocol

```text
short transaction: claim lease
-> remote call outside transaction
-> completion command
-> short transaction: verify identity/state/version
-> aggregate/process transition + command status + outbox
```

Do not keep a transaction open across OpenAI, email, S3, or another network
call. Do not rely on in-memory job state for recovery.

## Validation

- pure transition tests cover every allowed and forbidden edge;
- handler tests use fake ports;
- persistence tests cover round-trip and optimistic conflicts;
- transaction tests prove atomic state/outbox and rollback;
- lease tests cover expiry, reclaim and stale worker completion;
- inbox tests cover duplicate and delete-failure redelivery;
- serialization tests cover stable envelopes and provider DTO boundaries;
- restart/recovery tests prove no operator intent is lost.

## Pitfalls

- treating command `PENDING` as the process state;
- using SSE or WebSocket as workflow truth;
- calling remote systems from a transaction;
- allowing generic `setState` transitions;
- storing raw provider payload as domain content;
- publishing directly from a scheduler or external adapter;
- claiming exactly-once behavior for an external side effect.
