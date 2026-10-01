# Orchestrator - Backend Projection Feature

Use this when creating or updating a read model from an event.

## Responsibilities

- consume a stable event contract
- update a local read model owned by the consuming BC
- be idempotent and replay-safe
- avoid cross-BC table reads

## Steps

Read [the iteration workflow](../../iteration-workflow.md) first. These are
boundary responsibilities, not a precomputed implementation sequence. For
`BEHAVIOUR`, express one observable example, inspect its RED and implement only
its minimum GREEN before the next example. Reuse existing contracts; introduce
new structures only when an example or invariant requires them. Continue on
`PASS`/`REVIEW`, escalate material ambiguity, then apply the targeted mutation
checkpoint to the green slice and pin missing protection. `REFACTORING` starts
green; `CHORE` uses proportionate checks.

1. Identify source event and producer BC.
2. Identify consuming BC and local projection table.
3. Write and inspect the RED for first application, then implement minimum GREEN.
4. Introduce a duplicate example and its minimum GREEN.
5. Introduce replay/enrichment examples one at a time, refactoring from green.
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
- process progress, when projected, remains distinct from command status
- `projection.updated` contains only freshness metadata and triggers a GET
