# Phase 2D - Mobile sync / STOMP convergence audit

Date: 2026-07-06

Scope:
- Backend: `/Users/nicolasmaldiney/fragmentsClean`
- Mobile: `/Users/nicolasmaldiney/fragmentsCleanFront`

This audit prepares the convergence from mobile STOMP ACKs toward the platform synchronization model:

```text
HTTP command
-> outbox / SQS / projection
-> ProjectionSyncEvent
-> SSE
-> Redux listener
-> GET read model
-> snapshot reducer
```

No code was changed during this audit.

## Executive Summary

The mobile app is halfway through the migration. The platform direction is sound, but STOMP cannot be removed safely in one pass.

Current state:
- `comments` already follow the target model: `projection.updated/comments` triggers a GET refresh.
- `likes` are ready to migrate next: backend projection sync exists and mobile already has a real GET gateway.
- `tickets` are not ready to migrate yet: backend projection sync exists, but mobile ticket retrieval still uses a test/no-op implementation instead of `GET /api/tickets/{ticketId}/status`.
- `entitlements` are not ready: they are still locally derived from ticket ACKs and need a real read model or an explicit temporary product decision.
- STOMP dependencies and transport remain active through `wsWl`, and some old STOMP code still leaks into core mobile files.

Recommendation:
1. Migrate `likes` first.
2. Add real ticket status retrieval before migrating `tickets`.
3. Do not remove STOMP until `tickets` and entitlements have a projection-backed replacement.

## Architecture Rule

The migration must not turn SSE into another ACK bus.

Allowed:

```text
projection.updated
-> dispatch retrieval GET
-> reducer replaces/merges read snapshot according to context rules
```

Forbidden:

```text
projection.updated
-> directly mutate likes/comments/tickets/entitlements state
```

Also forbidden:

```text
projection.updated
-> drop outbox command
```

The command lifecycle remains owned by:

```text
HTTP command response
-> command status polling /commands/{commandId}
-> explicit APPLIED or REJECTED
```

Projection freshness is not command completion.

## Mobile Inventory

### Active STOMP transport

Active files:
- `app/adapters/primary/socket/WsEventsGateway.ts`
- `app/core-logic/contextWL/wsWl/usecases/wsListenerFactory.ts`
- `app/core-logic/contextWL/wsWl/gateway/wsWl.gateway.ts`
- `app/core-logic/contextWL/wsWl/typeAction/ws.action.ts`
- `app/core-logic/contextWL/wsWl/typeAction/ws.type.ts`

Dependencies still present:
- `@stomp/stompjs`
- `sockjs-client`
- `@types/sockjs-client`

The runtime still starts both channels:
- `wsEnsureConnectedRequested()`
- `projectionSyncEnsureConnectedRequested()`

This is acceptable during migration, but it must not become permanent without a documented reason.

### STOMP code leaking outside `wsWl`

The mobile core still contains duplicated STOMP/SockJS imports in:
- `app/core-logic/contextWL/appWl/reducer/app.reducer.ts`
- `app/core-logic/contextWL/appWl/typeAction/appWl.type.ts`

This violates the intended boundary: concrete transport dependencies should stay in primary/secondary adapters or `wsWl`, not in app core reducers/types.

This should be cleaned before final STOMP removal, but it is not the next safest migration step.

## Current Workflow Matrix

| Workflow | Current STOMP use | Current SSE/read status | Migration readiness |
|---|---:|---:|---|
| Comments | Legacy ACK types still exist, but not routed | `projection.updated/comments` triggers `commentRetrieval(refresh)` | Mostly migrated |
| Likes | ACK routed from STOMP | Backend emits `projection.updated/likes`; mobile has GET | Ready |
| Tickets | ACK routed from STOMP | Backend emits `projection.updated/tickets`; mobile GET missing | Not ready |
| Entitlements | Derived from ticket ACK | No clear backend entitlement projection found | Not ready |
| Articles | No active STOMP workflow found in this pass | Not assessed as migration target | Later |
| Coffees | No mobile STOMP dependency found | Studio/backend uses projection sync separately | No action now |

## Comments

### Current state

`comments` already follow the target freshness flow.

Mobile:
- `projectionSyncListenerFactory` routes:

```text
projection="comments"
scope="target"
entityId=<targetId>
-> commentRetrieval({ targetId, op: REFRESH })
```

Reducer behavior:
- `commentsRetrieved` with `REFRESH` resets the target server snapshot while preserving optimistic local comments.

Backend:
- social comment projection handlers publish `projection.updated` with:
  - `projection="comments"`
  - `scope="target"`
  - `entityId=<targetId>`

### Remaining debt

Legacy comment ACK actions and tests still exist:
- `commentWl/usecases/read/ackReceivedBySocket.ts`
- `tests/core-logic/contextWl/commentWl/usecases/read/ackComments.spec.ts`

They are no longer routed by `wsListenerFactory`, but they keep the old mental model alive.

### Recommendation

Do not touch comments in Phase 2D. They are the reference implementation. Clean legacy comment ACK files only after command-status polling coverage is considered sufficient and documented.

## Likes

### Current state

Mobile STOMP still routes:
- `social.like.added_ack`
- `social.like.removed_ack`

These dispatch:
- `onLikeAddedAck`
- `onLikeRemovedAck`

Those handlers:
- reconcile the optimistic like state;
- call `dropCommitted(commandId)`.

Mobile already has a real read path:
- `likesRetrieval({ targetId })`
- `HttpLikesGateway.get({ targetId })`
- `GET /api/social/targets/{targetId}/likes`

Backend already has a projection-sync path:
- `LikeSetEventHandler` updates `social_likes_projection`.
- It then publishes:

```text
projection.updated
projection="likes"
scope="target"
entityId=<targetId>
hints=["set"]
```

### Risk

Removing STOMP ACKs for likes will remove the fastest `dropCommitted(commandId)` path.

This is acceptable architecturally only because the mobile watchdog already polls `/commands/{commandId}` and drops the outbox command when the command status is `APPLIED`.

Do not compensate by dropping outbox commands from SSE. That would confuse projection freshness with command acknowledgement.

### Recommended Phase 2D.1

Implement a small, contained migration:

Mobile:
- route `projection.updated/likes` in `projectionSyncListenerFactory`;
- dispatch `likesRetrieval({ targetId })`;
- remove like ACK routing from `wsListenerFactory`;
- keep `ackLike.ts` temporarily only if tests or rollback paths still reference it, otherwise delete it in the same branch.

Tests:
- `projection.updated/likes` dispatches `likesRetrieval`;
- `sync.heartbeat` remains ignored;
- like ACK events are no longer routed by `wsListenerFactory`;
- offline like command polling still drops the command when `/commands/{commandId}` returns `APPLIED`.

Expected result:
- UI freshness comes from GET.
- Command lifecycle comes from command-status polling.
- STOMP no longer owns likes.

## Tickets

### Backend state

Backend has the correct target pieces:
- `GET /api/tickets/{ticketId}/status`
- `ticket_status_projection`
- `TicketVerifyAcceptedEventHandler`
- `TicketVerificationCompletedEventHandler`

Projection sync publication exists after projection update:

```text
projection.updated
projection="tickets"
scope="entity"
entityId=<ticketId>
hints=["status", ...]
```

### Mobile state

Mobile STOMP still routes:
- `ticket.verification.completed_ack`

This dispatches:
- `onTicketConfirmedAck`
- `onTicketRejectedAck`

Those handlers:
- update ticket state;
- drop committed outbox command.

Mobile ticket read is not production-ready:
- `TicketsWlGateway` only exposes `verify(...)`;
- `HttpTicketsGateway` only implements `POST /api/tickets/verify`;
- `ticketRetrieval.ts` contains a test branch and then dispatches a fake `ANALYZING` state instead of calling the backend.

This means a ticket SSE event cannot currently trigger a real read-model refresh.

### Entitlement coupling

Ticket ACKs also trigger entitlement behavior:
- `ackEntitlementsListener` listens to ticket confirmation ACKs;
- entitlements are locally incremented from the ticket ACK.

This creates a hidden dependency:

```text
ticket STOMP ACK
-> ticket reducer
-> entitlement local state
```

Removing ticket ACK before replacing this flow would break entitlement UX or leave it inconsistent.

### Recommended Phase 2D.2

Prerequisite before removing ticket STOMP:

Mobile:
- add `TicketsWlGateway.getStatus({ ticketId })`;
- implement `HttpTicketsGateway.getStatus` against `GET /api/tickets/{ticketId}/status`;
- replace the fake/no-op `ticketRetrieval` branch with the real gateway call;
- route `projection.updated/tickets` to `ticketRetrieval({ ticketId })`.

Tests:
- ticket SSE event dispatches `ticketRetrieval`;
- `ticketRetrieval` maps backend status DTO to reducer payload;
- reducer snapshot behavior handles status transitions;
- command-status polling still handles outbox cleanup.

Only after that:
- remove `ticket.verification.completed_ack` routing from `wsListenerFactory`;
- decide what replaces `ackEntitlementsListener`.

## Entitlements

### Current state

Entitlements are not ready for STOMP removal.

The mobile app still appears to use a fake/local entitlement gateway, and ticket confirmation ACKs feed entitlement state.

This is not a platform synchronization model yet.

### Recommendation

Do not migrate entitlements as part of the ticket step unless a real backend read model exists.

Choose explicitly between:
1. short-term product compromise: entitlements remain local/derived for the release;
2. platform implementation: backend entitlement projection + `projection.updated/entitlements` + mobile GET.

Option 2 is architecturally cleaner, but it is a bigger product/backend step.

## Runtime and Connectivity

The runtime currently connects both:

```text
app active / online / signed in
-> wsEnsureConnectedRequested
-> projectionSyncEnsureConnectedRequested
```

This dual-channel runtime is acceptable only during the migration.

Target runtime:

```text
app active / online / signed in
-> projectionSyncEnsureConnectedRequested
-> outboxProcessOnce
-> watchdogTick
```

When all domains are migrated, `wsWl` should be removed from runtime wiring and dependencies.

## Security and Contract Notes

SSE connection uses Bearer token and Last-Event-ID.

The browser CORS problems seen in Studio are not directly relevant to React Native, but the same backend contract still matters:
- `Authorization` must be accepted;
- `Last-Event-ID` must be accepted when the client sends it;
- SSE must not carry domain event names or payloads.

Mobile logs should not include bearer tokens or full event payloads containing user-sensitive data.

## Recommended Migration Order

### P0 - Before deleting STOMP globally

1. Migrate likes to SSE + GET.
2. Add real ticket status GET gateway.
3. Route ticket SSE to `ticketRetrieval`.
4. Decide entitlement replacement.

### P1 - After each domain migration

1. Remove unused ACK routing for that domain.
2. Remove obsolete ACK tests or rewrite them as command-status polling tests.
3. Remove transport-specific imports from core mobile files.

### P2 - Final STOMP removal

Only after likes, tickets, and entitlements no longer depend on STOMP:

1. Remove `wsWl` runtime wiring.
2. Remove `WsEventsGateway`.
3. Remove SockJS/STOMP dependencies.
4. Remove backend WebSocket ACK senders.
5. Remove backend WebSocket configuration if no other product use remains.
6. Remove stale documentation that describes socket ACK as an active path.

## Tests to Add

Mobile:
- projection sync listener routes `likes` to `likesRetrieval`;
- projection sync listener routes `tickets` to `ticketRetrieval`;
- `ticketRetrieval` calls `GET /api/tickets/{ticketId}/status`;
- like/ticket read reducers behave as snapshot reducers where appropriate;
- no direct read model mutation from SSE;
- command-status polling still drops applied commands without STOMP.

Backend:
- ticket projection handlers publish `projection.updated/tickets` only after `ticket_status_projection` update;
- like projection handler publishes `projection.updated/likes` only after `social_likes_projection` update;
- SSE replay with `Last-Event-ID` covers missed projection events.

Architecture tests:
- no STOMP/SockJS imports outside `wsWl` and adapter layers during migration;
- eventually no STOMP/SockJS imports anywhere in mobile;
- backend Domain Events are not serialized to SSE.

## Decision

Do not remove STOMP yet.

Proceed with a domain-by-domain migration:
1. `likes` first, because both backend projection sync and mobile GET exist.
2. `tickets` second, after implementing real mobile ticket status retrieval.
3. `entitlements` only after a product/backend decision on the read model.

This keeps the platform doctrine intact:
- HTTP remains command transport.
- `/commands/{commandId}` remains command reconciliation.
- SSE remains projection freshness.
- Redux listeners orchestrate GET retrieval.
- React components stay transport-agnostic.
