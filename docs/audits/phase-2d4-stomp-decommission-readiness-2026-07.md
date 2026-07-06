# Phase 2D.4 - STOMP decommission readiness

Date: 2026-07-06

## Executive Summary

Status update after Phase 2D.5: STOMP/WebSocket ACK removal is complete.

The active model is now:

```text
HTTP command
-> /commands/{commandId} for command lifecycle

projection.updated SSE
-> GET snapshot
-> reducer snapshot / reconciliation
```

Verified active-code state:

- mobile has no active `wsWl`, SockJS, STOMP, socket ACK routes, or ACK tests;
- backend has no `/ws` configuration, WebSocket ACK sender, Spring WebSocket
  dependency, or `SimpMessagingTemplate` usage;
- comments, likes, tickets, and entitlements use Projection Sync for freshness;
- frontend clients do not receive Domain Events.

The rest of this audit is retained as historical context from the readiness
pass that led to the removal.

Decision: remove STOMP/WebSocket in the next implementation pass.

The product workflows that previously depended on STOMP ACKs now have the target
platform path:

```text
HTTP command
-> /commands/{commandId} for command lifecycle
-> projection.updated SSE
-> GET snapshot
-> reducer snapshot / reconciliation
```

STOMP is therefore no longer required for read-model freshness. Keeping it would
preserve a second real-time synchronization channel, keep backend domain-event
payload mapping alive, and keep mobile runtime complexity without clear product
value.

Do not remove it inside this audit pass. The next pass should remove it
deliberately with tests.

## Current Mobile Usage

Active STOMP transport remains wired through:

- `app/adapters/primary/socket/WsEventsGateway.ts`
- `app/adapters/primary/socket/ws.gateway.ts`
- `app/adapters/primary/socket/ws.type.ts`
- `app/core-logic/contextWL/wsWl/usecases/wsListenerFactory.ts`
- `app/core-logic/contextWL/wsWl/gateway/wsWl.gateway.ts`
- `app/core-logic/contextWL/wsWl/typeAction/ws.action.ts`
- `app/core-logic/contextWL/wsWl/typeAction/ws.type.ts`
- `app/core-logic/contextWL/wsWl/reducer/ws.reducer.ts`
- `app/adapters/primary/wiring/infrastructure.ts`
- `app/adapters/primary/wiring/listeners.ts`
- `app/adapters/primary/wiring/config.ts`
- `app/store/reduxStoreWl.ts`

Dependencies still present:

- `@stomp/stompjs`
- `sockjs-client`
- `@types/sockjs-client`

Important leak still present:

- `app/core-logic/contextWL/appWl/typeAction/appWl.type.ts`
- `app/core-logic/contextWL/appWl/reducer/app.reducer.ts`

Those files still contain duplicated STOMP/SockJS implementation code. This is
not active product architecture and should be removed with the STOMP cleanup.

## Current Mobile ACK Routing

`wsListenerFactory` now ignores likes:

```text
social.like.added_ack / removed_ack
-> ignored
```

It still routes tickets:

```text
ticket.verification.completed_ack
-> onTicketConfirmedAck / onTicketRejectedAck
```

The remaining ticket ACK listeners still perform:

- ticket local reconciliation;
- badge progress recalculation;
- `dropCommitted(commandId)`;
- entitlements local increment through `ackEntitlementsListener`.

This is now compatibility behavior. It is not the target correctness path.

Target replacements already exist:

- tickets: `projection.updated projection="tickets"` -> `ticketRetrieval({ ticketId })` -> `GET /api/tickets/{ticketId}/status`
- entitlements: `projection.updated projection="entitlements"` -> `entitlementsRetrieval({ userId })` -> `GET /api/users/me/entitlements`
- command lifecycle: `/commands/{commandId}` through outbox watchdog

## Current Backend Usage

Active backend STOMP/WebSocket pieces:

- `src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/configuration/webSocket/WebSocketConfig.java`
- `src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/configuration/webSocket/JwtStompChannelInterceptor.java`
- `src/main/java/com/nm/fragmentsclean/platform/eventing/WebSocketOutboxEventSender.java`
- `src/main/java/com/nm/fragmentsclean/platform/eventing/StableEnvelopeOutboxEventSender.java`
- `src/main/java/com/nm/fragmentsclean/sharedKernel/businesslogic/models/gateways/ClientAckOutboxEventSender.java`

`StableEnvelopeOutboxEventSender` still invokes `sendWebSocketBestEffort(event)`
after local event bus / SQS integration publication.

`WebSocketOutboxEventSender` maps backend domain events to frontend ACK payloads:

- `LikeSetEvent` -> `social.like.added_ack` / `social.like.removed_ack`
- `CommentCreatedEvent` -> `social.comment.created_ack`
- `CommentUpdatedEvent` -> `social.comment.updated_ack`
- `CommentDeletedEvent` -> `social.comment.deleted_ack`
- `TicketVerifyAcceptedEvent` -> `ticket.verify.accepted_ack`
- `TicketVerificationCompletedEvent` -> `ticket.verification.completed_ack`

This class still imports social and ticket domain events. That coupling is now
unnecessary once clients rely on command status + projection sync.

## Workflow Matrix

| Workflow | Command channel | Command lifecycle | Freshness signal | Snapshot GET | STOMP dependency |
| --- | --- | --- | --- | --- | --- |
| Comments | HTTP + outbox | `/commands/{commandId}` | `projection.updated/comments` | `GET /api/social/comments?...` | No product need |
| Likes | HTTP + outbox | `/commands/{commandId}` | `projection.updated/likes` | `GET /api/social/targets/{targetId}/likes` | No product need |
| Tickets | HTTP + outbox | `/commands/{commandId}` | `projection.updated/tickets` | `GET /api/tickets/{ticketId}/status` | Compatibility only |
| Entitlements | Derived from ticket projection | n/a | `projection.updated/entitlements` | `GET /api/users/me/entitlements` | Compatibility only |

## Tests Still Depending On ACK/STOMP

Mobile:

- `tests/core-logic/contextWl/wsWl/wsListenerFactory.spec.ts`
- `tests/core-logic/contextWl/appWl/usecases/runtimeListener.spec.ts`
- `tests/core-logic/contextWl/ticketWl/usecases/read/ackTicket.spec.ts`
- `tests/core-logic/contextWl/ticketWl/usecases/read/ack.ticket-badge.integration.spec.ts`
- `tests/core-logic/contextWl/entitlementWl/usecases/read/ackEntitlement.spec.ts`
- `tests/core-logic/contextWl/commentWl/usecases/read/ackComments.spec.ts`
- `tests/core-logic/contextWl/likeWl/usecases/read/ackLike.spec.ts`

Backend:

- `src/test/java/com/nm/fragmentsclean/socialContextTest/unit/WebSocketOutboxEventSenderTest.java`
- `src/test/java/com/nm/fragmentsclean/sharedKernel/eventing/StableEnvelopeOutboxEventSenderTest.java`
- `src/test/java/com/nm/fragmentsclean/architecture/BoundedContextArchitectureTest.java` contains a WebSocket origin guard.

Some ACK unit tests are still useful only until the cleanup lands. After removal,
their equivalent coverage must live in:

- command status watchdog tests;
- projection sync listener tests;
- read model retrieval tests.

## Files To Remove In The Decommission Pass

Mobile candidates:

- `app/adapters/primary/socket/WsEventsGateway.ts`
- `app/adapters/primary/socket/ws.gateway.ts`
- `app/adapters/primary/socket/ws.type.ts`
- `app/adapters/primary/socket/README.md`
- `app/core-logic/contextWL/wsWl/**`
- `app/core-logic/contextWL/ticketWl/usecases/read/helper/ticketAckFromWs.ts`
- `app/core-logic/contextWL/ticketWl/usecases/read/ackTicket.ts`, if no non-STOMP producer remains
- `app/core-logic/contextWL/entitlementWl/usecases/read/ackEntitlement.ts`
- legacy ACK listeners for comments/likes if no fake/test path still uses them
- STOMP/SockJS duplicate code from `appWl/typeAction/appWl.type.ts`
- STOMP/SockJS duplicate code from `appWl/reducer/app.reducer.ts`

Mobile wiring to update:

- remove `ws` from `GatewaysWl`;
- remove `WsStompEventsGateway` from `createInfrastructure`;
- remove `WS_URL`;
- remove `wsListenerFactory` from `createWlListeners`;
- remove `wsEnsureConnectedRequested`, `wsDisconnectRequested`, and `wsConnected`
  handling from `runtimeListenerFactory`;
- remove `wsState` reducer from `reduxStoreWl` unless UI still displays it.

Mobile dependencies to remove:

- `@stomp/stompjs`
- `sockjs-client`
- `@types/sockjs-client`

Backend candidates:

- `sharedKernel/adapters/primary/springboot/configuration/webSocket/WebSocketConfig.java`
- `sharedKernel/adapters/primary/springboot/configuration/webSocket/JwtStompChannelInterceptor.java`
- `platform/eventing/WebSocketOutboxEventSender.java`
- `sharedKernel/businesslogic/models/gateways/ClientAckOutboxEventSender.java`
- `sendWebSocketBestEffort` and `ClientAckOutboxEventSender` dependency from `StableEnvelopeOutboxEventSender`
- Spring WebSocket/STOMP dependency in `pom.xml`, if no other usage remains

Backend tests to remove or rewrite:

- `WebSocketOutboxEventSenderTest`
- WebSocket branch in `StableEnvelopeOutboxEventSenderTest`
- WebSocket origin guard in `BoundedContextArchitectureTest`

## Files To Keep

Keep:

- `projectionSyncWl` mobile gateway/listener/reducer/tests;
- `/api/sync/events` backend;
- `/commands/{commandId}`;
- command status watchdog and outbox reconciliation;
- all read model GET gateways;
- ticket/entitlements projection handlers and SSE events.

## Risks If Removed Too Early

The main risk is not read freshness anymore. It is UX latency for outbox cleanup:
without ACKs, the mobile app relies on `/commands/{commandId}` polling to drop
pending commands. This is acceptable and matches the doctrine, but the cleanup
pass must verify:

- watchdog runs after command submission;
- APPLIED/REJECTED statuses drop records promptly enough;
- no UI relies on `wsState`;
- badge progress is recomputed from snapshots or explicit read refresh, not ACKs.

## Recommendation

Do not keep STOMP as a permanent compatibility layer.

Implement Phase 2D.5 as a removal pass:

1. Remove mobile WS runtime wiring first.
2. Keep command status watchdog as the sole command reconciliation path.
3. Remove ACK listeners once tests prove comments/likes/tickets/entitlements use
   projection sync + GET snapshots.
4. Remove backend WebSocket sender/config and Spring dependencies.
5. Update docs to state that Fragments client synchronization is HTTP commands +
   SQS/outbox backend propagation + ProjectionSync SSE + GET snapshots.

If a very short-lived safeguard is desired, confine STOMP behind a single mobile
feature flag defaulting to off and a backend property disabling
`sendWebSocketBestEffort`. That should be a transitional operational fallback,
not a product architecture.
