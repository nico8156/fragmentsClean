# Test Policy

## Construction and mutation feedback

Follow [the iteration workflow](../../.agents/iteration-workflow.md).
Classify `BEHAVIOUR / PIN / REFACTORING / CHORE` independently of architecture.
Start behavior changes with a concrete example around the fast hexagon, inspect
RED, implement minimum GREEN, then refactor and revise the next example.
After a meaningful green slice, run bounded mutations on its decisions.
Inspect survivors before adding a `PIN`: the test passes on the original and
fails on the mutant. Equivalence and contract ambiguity require analysis.
Record execution and restoration evidence; coverage or proposed mutations do
not prove a kill. Infrastructure proof below remains required where relevant.

## Backend

Use fake-first tests for business behavior:
- domain tests for invariants
- command handler tests with fake ports
- projection tests with fake or JDBC repository
- query handler tests with fake or JDBC repository

Use Testcontainers when:
- SQL mapping matters
- transaction/outbox behavior matters
- repository behavior matters
- integration flow requires Postgres/SQS or object storage infrastructure

Use MockMvc when:
- HTTP status/body/auth contract matters

Mocks are acceptable at technical boundaries. They should not replace business fakes by default.

## Mobile

Use fake-first tests:
- reducers pure
- selectors pure
- listeners/use cases with fake gateways
- outbox retry/rollback/reconcile tests
- socket ACK routing tests
- bootstrap/runtime tests

Mock native modules only when they are technical boundaries.

## Required Critical Flow Tests

- command accepted -> outbox -> command status `APPLIED`
- duplicate event/message -> no duplicate business effect
- `REJECTED` -> rollback
- network error -> no rollback, retry
- no socket ACK -> polling reconciles
