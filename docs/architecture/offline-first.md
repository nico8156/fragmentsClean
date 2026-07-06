# Offline-First Strategy

Fragments mobile writes must survive:
- offline mode
- app background/foreground
- process restart after local persistence
- network timeout
- missed projection sync event
- backend eventual consistency delay

## Command Lifecycle

```text
queued
-> processing
-> awaitingAck
-> APPLIED or REJECTED
```

Transport failure returns to `queued` with retry scheduling.

## Rollback Rule

Do not rollback on:
- offline
- network error
- timeout
- 5xx
- missed projection sync event

Rollback only on:
- explicit backend business rejection
- `/commands/{commandId}` returning `REJECTED`

## Reconciliation

Preferred fast path:
- socket ACK

Canonical fallback:
- `/commands/{commandId}`

The fallback is mandatory for production because sockets are opportunistic.
