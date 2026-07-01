# Orchestrator - Backend Projection Feature

Use this when creating or updating a read model from an event.

## Responsibilities

- consume a stable event contract
- update a local read model owned by the consuming BC
- be idempotent and replay-safe
- avoid cross-BC table reads

## Steps

1. Identify source event and producer BC.
2. Identify consuming BC and local projection table.
3. Write projection test for first application.
4. Write duplicate/replay test.
5. Implement projection handler.
6. Persist inbox before or around business effect according to the local pattern.
7. Use JDBC upsert where possible.
8. Verify query endpoint reads this projection.

## Pitfalls

- projection handler importing producer domain classes
- duplicate events creating duplicate rows
- replay overwriting enriched local state
- using inbox alone when the business effect also needs local deduplication

## Validation

- duplicate delivery is harmless
- replay is harmless
- read model belongs to consuming BC
- query path does not load aggregates

