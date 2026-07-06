# Backend Architecture

## Shape

Fragments backend is a Spring Boot modular monolith.

Defaults:
- DDD where behavior exists
- CQRS where read/write models diverge
- command handlers for writes
- query handlers for reads
- JPA write side
- JDBC read side
- outbox/inbox for asynchronous effects
- SQS as target transport

## Write Flow

```text
Controller
-> Command
-> CommandHandler
-> Aggregate/domain model
-> Repository port
-> JPA/JDBC adapter
-> Domain event
-> Outbox
-> Dispatcher
```

Controllers do not contain business decisions and do not access repositories directly.

## Read Flow

```text
Controller
-> Query
-> QueryHandler
-> JDBC read repository
-> DTO/read model
```

Read side must not load aggregates for normal query endpoints.

## Async Flow

```text
Outbox row
-> stable envelope
-> SQS
-> SQS consumer
-> inbox check
-> typed handler
-> projection/domain transition
```

## Client Synchronization

Command lifecycle is exposed through `GET /commands/{commandId}`.

Read-model freshness is exposed through Projection Sync SSE:

```text
projection update
-> ProjectionSyncEvent
-> /api/sync/events
-> client GET snapshot
```

The backend no longer exposes a STOMP/WebSocket ACK path. SSE does not acknowledge
commands and does not carry domain events.
