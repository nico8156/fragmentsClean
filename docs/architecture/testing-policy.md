# Test Policy

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
- integration flow requires Postgres/Kafka legacy/dev infra

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

