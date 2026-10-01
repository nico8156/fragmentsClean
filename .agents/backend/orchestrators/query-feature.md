# Orchestrator - Backend Query Feature

Use this when exposing read-side data through HTTP.

## Responsibilities

- expose a read model, not a write aggregate
- keep controller mapping thin
- use explicit query handlers
- use JDBC/SQL read repositories by default

## Steps

Read [the iteration workflow](../../iteration-workflow.md) first. These are
boundary responsibilities, not a precomputed implementation sequence. For
`BEHAVIOUR`, express one observable example, inspect its RED and implement only
its minimum GREEN before the next example. Reuse existing contracts; introduce
new structures only when an example or invariant requires them. Continue on
`PASS`/`REVIEW`, escalate material ambiguity, then apply the targeted mutation
checkpoint to the green slice and pin missing protection. `REFACTORING` starts
green; `CHORE` uses proportionate checks.

1. Identify the owning read model.
2. Define query parameters and response DTO.
3. Write query handler or web contract test.
4. Implement SQL repository or fake-backed handler.
5. Add loading/empty/error semantics expected by mobile.
6. Verify auth rules.

## Pitfalls

- loading JPA aggregates for read endpoints
- calling command handlers from read paths
- leaking database entities to HTTP
- duplicating business decisions in SQL

## Validation

- read endpoint returns stable DTOs
- no write-side repository is used
- pagination/cursor behavior is explicit when lists can grow
- mobile can tolerate empty and stale read states
