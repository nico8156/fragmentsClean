# Orchestrator - Backend Command Feature

Use this when a backend feature changes state inside one bounded context.

## Responsibilities

- express user/system intent as a command
- load or create the aggregate
- enforce invariants in the domain model
- persist through a repository port
- append an outbox event when other read models or BCs must react
- return an HTTP response compatible with async mobile UX

## Steps

1. Identify the owning bounded context.
2. Identify the aggregate/entity that owns the decision.
3. Write or update a command handler/domain test first.
4. Add or adjust the command object.
5. Implement domain transition and event registration.
6. Persist aggregate and outbox in the same transaction.
7. Add integration coverage if persistence/outbox is involved.
8. Verify command status expectations if the command is mobile-facing.
9. If the command starts a long-running process, mark the command according to
   acceptance of that intent and expose process progress through a separate
   query model. Do not leave command status `PENDING` for the whole process.
10. Review BC boundaries.

## Pitfalls

- putting business decisions in controllers
- writing projections directly from command handlers
- calling another BC service directly
- emitting event payloads with domain objects
- treating network failure as command rejection
- calling a remote provider from the command transaction
- encoding long-running process progress as command status

## Validation

- tests prove happy path and relevant rejection path
- outbox event exists when downstream state must change
- controller remains thin
- command returns `202 Accepted` when processing is async
- `/commands/{commandId}` can eventually report `APPLIED` or `REJECTED`
- no remote call executes inside the command transaction
- long-running process state, when present, is not encoded as command status
