# Command Status Strategy

`GET /commands/{commandId}` is the canonical source for mobile command status.

## Purpose

It decouples mobile correctness from WebSocket delivery.

Mobile uses it when an outbox record is `awaitingAck` and the ACK deadline expires.

## Statuses

`PENDING`
- command is unknown
- command accepted but not applied yet
- command applied but status projection not updated yet

`APPLIED`
- command was applied successfully
- mobile may reconcile/drop the outbox record

`REJECTED`
- command was explicitly rejected by business rules
- mobile may rollback/drop the outbox record

## Rule

`PENDING` is not failure.

Mobile should re-check later.

