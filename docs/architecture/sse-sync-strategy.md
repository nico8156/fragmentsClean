# SSE Sync Strategy

SSE is the platform synchronization channel for read models.

It is not a domain-event transport and it is not a command acknowledgement
mechanism. It tells clients that a projection is now coherent and may be
refetched.

## Position In The Architecture

Target flow:

```text
Command
-> Decision
-> Domain Event
-> Outbox
-> SQS
-> Consumer
-> Projection update
-> Projection sync notification
-> SSE
-> Redux listener
-> Query/refresh
-> Selectors
-> React
```

SQS and SSE have different responsibilities:

- SQS propagates business decisions between backend components.
- SSE propagates read-model freshness information to clients.

The frontend must never receive domain events such as `CoffeeCreatedEvent`,
`CoffeePhotoStoredEvent`, or `UserRegisteredEvent`.

## Recommendation

Create one platform stream:

```http
GET /api/sync/events
```

The endpoint may accept optional filters:

```http
GET /api/sync/events?projections=coffees,articles
```

Admin-specific authentication can expose the same contract under the same
endpoint if the security model supports it, or under:

```http
GET /api/admin/sync/events
```

The event contract must remain identical. The difference is authorization, not
payload shape.

Prefer one stream over one endpoint per context. Browsers and mobile runtimes
have connection limits, mobile backgrounding is easier with a single
connection, and `Last-Event-ID` is simpler when there is one ordered cursor.
Projection filters give clients the practical benefit of scoped traffic without
creating many transport contracts.

Per-context streams such as `/events/coffees` should be avoided as the default.
They multiply reconnect logic, make global ordering harder, and push transport
topology into clients. They remain acceptable only for a future high-volume
projection that has a proven need for isolation.

## Event Language

Use a projection-oriented event:

```text
event: projection.updated
id: 000000000000012345
data: {
  "schemaVersion": 1,
  "projection": "coffees",
  "scope": "entity",
  "entityId": "f6b55982-6721-4358-828f-ef2949c6a7af",
  "version": 42,
  "changedAt": "2026-07-03T19:24:34.333Z",
  "hints": ["summary"]
}
```

Fields:

- `schemaVersion`: version of the SSE payload contract.
- `projection`: stable read-model name known by clients.
- `scope`: `collection`, `entity`, or `user`.
- `entityId`: optional read-model entity id when useful.
- `version`: optional monotonic projection/entity version.
- `changedAt`: backend timestamp after the projection write.
- `hints`: optional coarse read-model fragments such as `summary`, `photos`,
  `openingHours`, `status`, or `count`.

Allowed projection names are platform contracts, for example:

- `coffees`
- `coffeePhotos`
- `coffeeOpeningHours`
- `articles`
- `comments`
- `likes`
- `tickets`
- `walletPasses`
- `users`
- `notifications`

Do not use Java class names, aggregate names, command names, event names, queue
names, or internal table names in SSE payloads.

## Mobile Migration Pilot: Comments

Comments are the first mobile domain migrated from STOMP ACK freshness to
projection sync.

Backend publication:

```text
CommentCreatedEvent / CommentUpdatedEvent / CommentDeletedEvent
-> comments projection handler
-> social_comments_projection update
-> projection_sync_events insert
```

The SSE payload is projection-oriented:

```json
{
  "eventName": "projection.updated",
  "projection": "comments",
  "scope": "target",
  "entityId": "<targetId>",
  "hints": ["created"]
}
```

Allowed hints for the pilot are:

- `created`
- `updated`
- `deleted`

Mobile behavior:

```text
projection.updated comments/target
-> commentRetrieval({ targetId, op: refresh })
-> GET comments read model
-> reducer snapshot for target, preserving local optimistic comments
```

The mobile listener must not dispatch comment read-model mutations directly.
The command outbox remains responsible for retry and command-status based
reconciliation.

## Granularity

Use the smallest notification that lets the client avoid unnecessary work, but
do not turn SSE into a data replication protocol.

Recommended levels:

- Collection changed:
  `{ "projection": "coffees", "scope": "collection", "version": 42 }`
- Entity changed:
  `{ "projection": "coffees", "scope": "entity", "entityId": "...", "version": 42 }`
- Fragment changed:
  `{ "projection": "coffees", "scope": "entity", "entityId": "...", "hints": ["photos"] }`
- User-private projection changed:
  `{ "projection": "walletPasses", "scope": "user", "entityId": "..." }`

The payload should normally not include the full read model. Clients refetch
through existing query APIs. This preserves read-side ownership and lets each
client choose the cheapest refresh path.

Full read-model payloads are allowed only for tiny, high-frequency counters
where the read contract is already stable and the optimization is documented.

## Versioning And Reconnect

SSE must use the native `id:` field. `Last-Event-ID` is the client resume
cursor.

Recommended backend model:

```text
projection_sync_events
  id            BIGSERIAL PRIMARY KEY
  projection    VARCHAR NOT NULL
  scope         VARCHAR NOT NULL
  entity_id     VARCHAR
  version       BIGINT
  changed_at    TIMESTAMPTZ NOT NULL
  payload_json  JSONB NOT NULL
```

Each projection handler writes the projection and its sync notification in the
same transaction, after the projection mutation. The SSE broadcaster reads from
this durable log and emits rows in `id` order.

Reconnect behavior:

- Client connects with no `Last-Event-ID`: server starts live events and may
  send a small `sync.connected` event.
- Client reconnects with `Last-Event-ID`: server replays
  `projection_sync_events.id > Last-Event-ID` before live streaming.
- If the cursor is older than retention: server emits `sync.resync_required`.
  The client performs a full refresh of relevant projections and stores the new
  stream cursor.

Use `version` to avoid unnecessary refreshes. A Redux slice can keep the last
seen version per projection/entity and ignore stale notifications.

Use `changedAt` for observability and UI diagnostics, not as the primary
ordering mechanism. Ordering should come from the SSE `id`.

## Backend Publication Rule

Publish SSE only after the projection has been updated.

Do not publish from:

- command handlers
- aggregates
- domain event constructors
- outbox dispatch before SQS
- SQS message receipt before projection commit

Publish from the read-side projection boundary:

```text
SQS consumer
-> inbox claim
-> projection handler
-> projection repository transaction
-> insert projection_sync_events row
-> commit
-> SSE broadcaster emits committed notification
```

This guarantees that when a client receives `projection.updated`, a subsequent
GET can observe the new read model.

## Frontend Integration

React components must not own SSE logic.

Each client should have a sync client adapter and Redux listener middleware:

```text
SSE adapter
-> syncEventReceived(action)
-> listener middleware
-> projection-specific refresh action
-> existing gateway/query
-> reducer
-> selectors
-> React
```

Example rule:

```text
projection == "coffees"
-> dispatch(existingCoffeesRefreshRequested())
```

More specific rule:

```text
projection == "coffees"
scope == "entity"
entityId is currently selected
-> dispatch(existingCoffeeDetailsRefreshRequested(entityId))
```

The listener, not the component, decides whether to refresh a list, a detail
view, or nothing. Components continue to read selectors.

The client should debounce/coalesce refreshes per projection. If five
`coffees` notifications arrive in one second, one refresh is usually enough.

## Bounded Context Rules

SSE belongs to platform synchronization, not to a business bounded context.

Allowed:

- bounded context projection handler records a projection sync notification
  through a primitive shared port;
- shared SSE infrastructure streams projection notifications;
- clients map projection names to query refreshes.

Forbidden:

- sending domain events over SSE;
- exposing Java class names;
- letting SSE consumers infer business decisions;
- using SSE as a command result source of truth;
- allowing one bounded context to mutate another context's projection in order
  to trigger SSE.

The sync event is a view contract:

```text
"coffees projection changed"
```

not a domain fact:

```text
"CoffeeCreatedEvent happened"
```

## Scalability

This model works in the current monolith because the durable sync log can live
in PostgreSQL next to projections.

It also scales to services:

- each service owns its projection tables and local `projection_sync_events`;
- each service may expose its own internal sync feed;
- an edge sync gateway can merge feeds by source plus offset;
- clients still see projection names, not domain events.

For the MVP, PostgreSQL polling of `projection_sync_events` is enough. Avoid
Redis unless a measured fanout or multi-instance problem justifies it. If the
backend runs multiple instances, each instance may stream from the same durable
log; clients reconnecting to another instance can resume by `Last-Event-ID`.

Retention should be explicit. For example, keep sync rows for 24-72 hours. If a
client resumes beyond retention, it receives `sync.resync_required`.

## Relation To WebSocket ACKs

Existing WebSocket ACKs are command-correlated UX accelerators. They are not a
projection synchronization strategy.

Keep the concepts separate:

- WebSocket ACK: "your command was accepted/applied/rejected" when available.
- Command status endpoint: canonical command reconciliation.
- SSE sync: "this projection is now updated."

Long term, command ACKs may remain WebSocket-based or move to the same SSE
transport as a separate non-domain event type. Even then, command lifecycle
events and projection sync events must stay separate contracts.

## Error And Lifecycle Events

Recommended non-domain SSE events:

```text
event: sync.connected
data: { "schemaVersion": 1, "serverTime": "..." }
```

```text
event: sync.heartbeat
data: { "schemaVersion": 1, "serverTime": "..." }
```

```text
event: sync.resync_required
data: {
  "schemaVersion": 1,
  "reason": "cursor_expired",
  "projections": ["coffees", "articles"]
}
```

Do not send stack traces or infrastructure errors to clients. Operational
errors belong in logs and metrics.

## Security

The stream must apply the same authorization rules as the read APIs it
invalidates.

Public/mobile authenticated clients should receive only projections they are
allowed to query. Admin clients may receive admin-only projections through admin
authorization. User-private projections must be filtered by user identity.

The payload should not contain sensitive read-model data unless the same data
is already available to that client through a GET endpoint.

## Implementation Sequence

1. Define the `ProjectionSyncPublisher` port in shared infrastructure terms.
2. Add durable `projection_sync_events`.
3. Update one projection handler, for example coffees, to write a notification
   in the same transaction as the projection update.
4. Add `GET /api/sync/events` with replay by `Last-Event-ID`.
5. Add Redux sync adapter/listener in Studio.
6. Extend to mobile and other projections.
7. Add retention cleanup and monitoring.

The first production use should be `coffees`, because the import/admin Studio
workflow already depends on projection freshness.

## Sprint 1 Baseline

Sprint 1 creates the transport plumbing only. It deliberately does not connect
any business projection.

Implemented contract:

```http
GET /api/sync/events
GET /api/admin/sync/events
Accept: text/event-stream
Last-Event-ID: optional
```

Initial events:

```text
event: sync.connected
data: { "schemaVersion": 1, "changedAt": "..." }
```

```text
event: sync.heartbeat
data: { "schemaVersion": 1, "changedAt": "..." }
```

Configuration:

```properties
fragments.sync.sse.timeout-ms=${FRAGMENTS_SYNC_SSE_TIMEOUT_MS:300000}
fragments.sync.sse.heartbeat-interval-ms=${FRAGMENTS_SYNC_SSE_HEARTBEAT_INTERVAL_MS:25000}
fragments.sync.sse.retry-ms=${FRAGMENTS_SYNC_SSE_RETRY_MS:5000}
```

Current invariant:

- no domain event is read by the SSE controller;
- no domain event is emitted to clients;
- no projection-specific notification exists yet;
- `Last-Event-ID` is accepted at the boundary but real replay starts in Sprint
  2 with the durable `projection_sync_events` log.

## Sprint 2 Durable Replay

Sprint 2 introduces the durable projection sync log:

```sql
projection_sync_events
  id            BIGSERIAL PRIMARY KEY
  event_name    VARCHAR(100) NOT NULL
  projection    VARCHAR(100)
  scope         VARCHAR(50)
  entity_id     VARCHAR(100)
  version       BIGINT
  changed_at    TIMESTAMPTZ NOT NULL
  payload_json  JSONB NOT NULL
```

Options considered:

- In-memory emitter registry only:
  simple, but loses events on restart and cannot implement reliable
  `Last-Event-ID`.
- Reusing domain outbox:
  durable, but violates the boundary because SSE would read domain events.
- Reusing SQS:
  durable for backend consumers, but the client sync concern would become tied
  to business propagation queues and fanout semantics.
- Dedicated PostgreSQL projection sync log:
  durable, ordered, cheap for the current monolith, and explicitly
  projection-oriented.

Decision: use dedicated PostgreSQL persistence. The dispatcher reads only
`ProjectionSyncEvent` rows. It never reads domain events or outbox rows.

Reconnect behavior in Sprint 2:

- no `Last-Event-ID`: start at `currentOffset()` and receive only new events;
- valid `Last-Event-ID`: replay `projection_sync_events.id > Last-Event-ID`;
- malformed `Last-Event-ID`: start at `currentOffset()` for now. A future
  hardening step may emit `sync.resync_required` explicitly.

Runtime configuration:

```properties
fragments.sync.sse.poll-interval-ms=${FRAGMENTS_SYNC_SSE_POLL_INTERVAL_MS:1000}
fragments.sync.sse.replay-batch-size=${FRAGMENTS_SYNC_SSE_REPLAY_BATCH_SIZE:100}
```

Testing status:

- controller and dispatcher tests run without external infrastructure;
- repository integration test uses PostgreSQL/Testcontainers;
- if Docker is unavailable, that integration test cannot run locally but remains
  the required verification in CI or a Docker-enabled workstation.

## Sprint 3 First Projection Hook

Sprint 3 connects the first real projection: `coffees`.

Internal backend flow:

```text
CoffeeCreatedEvent
-> CoffeeCreatedEventHandler
-> coffee_summaries_projection upsert
-> ProjectionSyncPublisher
-> projection_sync_events append
```

External SSE contract:

```text
event: projection.updated
data: {
  "schemaVersion": 1,
  "projection": "coffees",
  "scope": "entity",
  "entityId": "<coffeeId>",
  "version": <projectionVersion>,
  "changedAt": "<eventOccurredAt>",
  "hints": ["summary"]
}
```

The `CoffeeCreatedEvent` class name, payload, and domain vocabulary remain
backend-only. The sync event says only that the `coffees` read model changed.

The projection update and sync append must happen in the same transaction. If
the sync append fails, the projection handler fails and SQS retry/DLQ semantics
apply to the backend message. This is intentional: clients must not be told that
a projection is fresh unless the projection freshness marker is durable.

## Sprint 4 Studio Redux Integration

Fragments Studio owns no SSE logic in React components.

Client flow:

```text
ProjectionSyncGateway
-> projectionSyncEventReceived
-> Projection Sync listener middleware
-> existingCoffeesRefreshRequested
-> ExistingCoffeesGateway GET /api/admin/coffees
-> existingCoffeesLoaded
-> selectors
-> React
```

The listener never writes coffee data directly into the store. It only maps a
generic sync notification to an existing read-model refresh.

Initial Studio mapping:

```text
eventName == "projection.updated"
projection == "coffees"
-> existingCoffeesRefreshRequested()
```

Ignored by Studio:

- `sync.connected`
- `sync.heartbeat`
- projections other than `coffees`

Studio configuration:

```properties
VITE_PROJECTION_SYNC_GATEWAY=fake|http
VITE_PROJECTION_SYNC_EVENTS_PATH=/api/admin/sync/events
VITE_PROJECTION_SYNC_BEARER_TOKEN=<optional-token>
```

The fake gateway remains the default. HTTP sync must be explicitly enabled per
environment.

## Sprint 5 Automatic Coherence

Sprint 5 removes direct/manual refresh from the import workflow.

Final Studio flow:

```text
Import
-> backend command
-> domain decision
-> outbox
-> SQS
-> projection
-> projection_sync_events
-> SSE projection.updated
-> Redux Projection Sync listener
-> existingCoffeesRefreshRequested
-> GET /api/admin/coffees
-> React renders read model
```

Removed:

- `coffeeImportSucceeded -> existingCoffeesRefreshRequested`;
- visible manual refresh button from the existing coffees panel.

Kept:

- initial read-model load when Studio opens;
- explicit `existingCoffeesRefreshRequested` action as an internal Redux
  command used by listeners.

This preserves the rule that SSE never writes read-model data into Redux. It
only invalidates a projection and triggers the normal read API.
