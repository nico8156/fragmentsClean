# WebSocket Strategy

WebSocket is an opportunistic UX accelerator.

It exists to reduce visible latency for:
- like ACKs
- ticket verification ACKs
- future lightweight user-facing acknowledgements

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

## Deployment

AWS MVP may run WebSocket through the same Spring Boot runtime.

The system must remain correct if:
- sticky sessions are absent
- socket reconnects
- a message is missed
- the mobile app is backgrounded

Correctness comes from command status, not socket delivery.
