# Fragments Architecture Patterns

This document is the short operational guide for adding new capabilities to
Fragments. It does not replace the deeper architecture documents. It exists so
new features can reuse the same platform mechanics without rethinking the
pipeline each time.

## Pattern 1: Write Decision

```text
HTTP command
-> command handler
-> aggregate decision
-> Domain Event
-> outbox
-> SQS
-> backend handlers
```

Rules:

- commands express intent;
- aggregates decide;
- Domain Events describe facts that happened;
- Domain Events stay inside the backend;
- handlers must tolerate duplicate delivery;
- technical retry is handled by transport redelivery, not by pretending the
  business completed.

Use this for:

- creating or updating business state;
- accepting asynchronous work;
- producing facts other backend contexts need.

## Pattern 2: Read Freshness

```text
Domain Event consumed
-> projection update
-> ProjectionSyncEvent
-> projection_sync_events
-> SSE
-> client GET
-> snapshot reducer
```

Rules:

- publish `ProjectionSyncEvent` only after the projection write succeeds;
- `ProjectionSyncEvent` is not a Domain Event;
- SSE never transports the read model itself;
- SSE never transports command results;
- clients always perform a GET after `projection.updated`;
- read reducers should use snapshot replace for the refreshed scope.

Use this for:

- coffees;
- comments;
- likes;
- tickets;
- articles;
- wallet;
- future projections.

## Pattern 2A: Context Integration Contract

```text
Domain Event interne
-> outbox
-> Integration Event public versionne
-> SQS
-> ACL consommateur
-> handler du contexte consommateur
```

Rules:

- producer Domain Events remain private to their bounded context;
- consumers deserialize public integration contracts, not producer Domain Event
  classes;
- public integration contracts live outside producer internals and use
  primitives only;
- the SQS envelope carries a stable `eventType` and `eventVersion`;
- the consumer ACL translates the public contract into its own local language;
- adding a consumer must not require importing the producer aggregate, command,
  repository, use case, or Domain Event class.

Current applied edges:

- `authenticationContext -> userApplicationContext` via
  `auth.user.created`;
- `userApplicationContext -> socialContext` via `app.user.created` and
  `app.user.profile_updated`.

Use this when one bounded context needs to react to facts owned by another
bounded context.

## Pattern 3: Technical Failure

```text
technical failure
-> throw
-> message not acknowledged
-> SQS redelivery
-> retry / DLQ
```

Examples:

- provider timeout;
- transient network failure;
- invalid technical dependency state;
- database unavailable;
- non-JSON provider response where JSON is the technical contract.

Do not publish a completed business event for a retryable technical failure.
Doing so acknowledges the SQS message and hides the failure from the retry
mechanism.

## Pattern 4: Business Rejection

```text
business invalid
-> CompletedEvent / RejectedEvent
-> projection update
-> ProjectionSyncEvent
-> SSE
-> GET snapshot
```

Examples:

- ticket is not a receipt;
- ticket OCR text is missing when the command requires text;
- ticket verification result is partial and cannot confirm the ticket;
- command violates a business invariant.

Business rejection is a fact. It should be visible in the read model. It should
not be retried by SQS as if it were a platform failure.

## Pattern 5: Frontend Synchronization

```text
SSE projection.updated
-> Redux Listener Middleware
-> GET read model
-> reducer snapshot
-> selectors
-> React / React Native
```

Never do this:

```text
SSE
-> direct read-store mutation
```

Rules:

- React components do not own SSE logic;
- SSE listeners dispatch retrieval actions only;
- retrieval gateways call REST read endpoints;
- reducers replace the refreshed read scope;
- optimistic write state remains separate from read snapshots;
- command status and outbox reconciliation are separate from projection
  freshness.

## Anti-Patterns

- Reading write repositories from read query handlers.
- Publishing `ProjectionSyncEvent` from command handlers.
- Sending Domain Events to frontend clients.
- Deserializing another bounded context's Domain Event from an SQS consumer.
- Treating SSE as command ACK.
- Encoding business state transitions directly in SSE payloads.
- Consuming SQS messages successfully after retryable technical failure.
- Letting frontend reducers infer business facts from sync hints.

## Quick Checklist

Before adding a feature:

1. What is the command?
2. What aggregate decides?
3. What Domain Event records the decision?
4. Which public Integration Event is needed by other contexts?
5. Which consumer ACL translates it?
6. Which projection changes?
7. Which `ProjectionSyncEvent` is published after the projection update?
8. Which GET returns the fresh snapshot?
9. Which Redux listener maps `projection.updated` to retrieval?
10. Which reducer performs snapshot replace?
11. What is retryable technical failure?
12. What is business rejection?
