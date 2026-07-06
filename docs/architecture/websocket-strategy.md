# WebSocket Strategy

WebSocket is compatibility plumbing for opportunistic command ACKs.

It may reduce visible latency for:
- like ACKs
- ticket verification ACKs
- future lightweight user-facing acknowledgements

It is not the platform synchronization model. Projection freshness belongs to
Projection Sync SSE plus GET snapshots.

Comment ACKs are being removed domain by domain from the mobile runtime. The
comments pilot now uses Projection Sync SSE for read-model freshness and command
status polling for command reconciliation.

## Rules

- WebSocket is not a source of truth.
- WebSocket failure does not fail commands.
- Mobile must reconnect safely.
- Mobile must poll command status when ACK is missing.
- Backend must not put business invariants in socket delivery.
- Socket payloads should stay small and command-correlated.
- WebSocket origins must come from configuration, never from a wildcard.
- New read-model freshness work must use Projection Sync SSE, not WebSocket.

## Deployment

AWS MVP may run WebSocket through the same Spring Boot runtime.

The system must remain correct if:
- sticky sessions are absent
- socket reconnects
- a message is missed
- the mobile app is backgrounded

Correctness comes from command status and read-model GET snapshots, not socket
delivery.
