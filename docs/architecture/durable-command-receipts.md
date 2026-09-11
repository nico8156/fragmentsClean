# Durable authenticated command receipts

Mobile commands implementing `AuthenticatedCommand` are executed through a
shared technical application service. Business handlers and aggregates keep
their bounded-context ownership; they do not write `command_status`.

```text
authenticated HTTP adapter
-> bounded-context command
-> CommandBus
-> register PENDING in an independent transaction
-> lock receipt + execute handler + mark APPLIED in one transaction
-> 202
```

An explicit `BusinessCommandRejectedException` rolls the business transaction
back. The executor then persists `REJECTED` in an independent transaction and
the HTTP advice returns a typed `422 COMMAND_REJECTED`. Any other exception is
technical: it propagates as an error and leaves the receipt `PENDING` for a retry
with the same command id.

Each receipt binds together:

- the UUID supplied by the mobile outbox;
- the requester derived from the authenticated JWT, never from request JSON;
- a stable, explicitly versioned receipt type;
- a SHA-256 fingerprint of the complete command intent.

A reused id with another owner, type or fingerprint is rejected before the
handler runs. Concurrent retries serialize on `SELECT ... FOR UPDATE`, so only
one business execution reaches `APPLIED`. A successful no-op is still applied;
no synthetic domain event is needed.

`GET /commands/{commandId}` crosses a query handler and filters by requester.
Unknown, foreign and ownerless legacy rows all return the same `PENDING` view.
Studio uses its separately secured administrative query and can still inspect
unscoped receipts.

The additive release SQL backfills legacy owners only when retained outbox JSON
contains one unambiguous `userId` or `authorId`. It never infers ownership from
the first caller. A proven legacy `APPLIED` receipt is accepted for that owner
without re-execution; an unproven legacy collision returns a retryable `503`.

Deployment order for an existing database:

1. take the backup required by the operations runbook;
2. apply `db/release/2026-09-11-command-receipts.sql`;
3. deploy backend and mobile contracts;
4. verify owner, foreign-user, legacy and retry scenarios before promotion.
