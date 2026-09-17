# P0 account-erasure anti-resurrection remediation

Date: 2026-09-17
Branch: `fix/p0-inbox-sqs-ack-safety`
Status: implemented and verified locally together with restoration replay; not committed, pushed, migrated or deployed

## Problem closed by this tranche

Account deletion removed current rows but did not leave a durable local fact in
each bounded context. A delayed SQS event, a duplicated delivery with another
event identifier, or a still-authenticated command racing with deletion could
therefore recreate personal data after the relevant eraser had completed.

This tranche closes current-runtime resurrection. The separate restoration
mechanism is described in
`docs/audits/2026-09-17-p0-account-erasure-restoration.md`.

## Design

`account_erasure_barriers` stores one permanent lifecycle row per user and per
participating bounded context:

- `AUTHENTICATION`
- `USER_APPLICATION`
- `SOCIAL`
- `TICKET`
- `EXPERIENCE`

The shared-kernel `AccountErasureBarrier` is technical coordination only. It
does not erase business tables and does not infer event ownership. Each bounded
context explicitly identifies the user identifiers carried by its stable
contracts and retains ownership of its eraser.

The JDBC adapter uses PostgreSQL row locks and Spring's existing transaction
manager:

1. a normal mutation creates an `ACTIVE` row if absent and locks it before the
   mutation;
2. an eraser locks the same row, changes it to `ERASED`, performs the owning
   context cleanup and publishes the acknowledgement in one transaction;
3. if the mutation wins first, deletion waits and then removes its result;
4. if deletion wins first, the mutation observes `ERASED` and is skipped.

Multi-user moderation/block events lock user identifiers in stable UUID order.
Authenticated commands lock the five scopes in stable enum order inside the
durable command transaction. Once any erasure has completed, later commands are
stored as `REJECTED / ACCOUNT_ERASED` without invoking business code.

## Protected asynchronous paths

- ticket verification accepted/completed and admin ticket changes;
- social comments, likes, reports, moderation, blocks and user profiles;
- experience snapshots, media, reports, moderation, blocks and user profiles;
- saved-coffee projections;
- ticket and experience Pass contributions;
- delayed `auth.user.created` consumption by `userApplicationContext`.

Coffee and article events contain no user-owned account data and are not placed
behind this barrier.

## Persistence and deployment

Additive migration:

- `db/release/2026-09-17-account-erasure-barriers.sql`
- `db/release/account-erasure-safety-2026-09.psql`

The release depends on `messaging-safety-2026-09`, has its own immutable
checksum/history entry, and is rendered and downloaded by the staging deployment
scripts. The migration must be applied before starting the candidate backend.
Application rollback is safe because the table is additive; the tombstones must
not be dropped during rollback.

## Verification performed

Targeted unit verification:

```text
./mvnw -q -Dtest=DurableCommandExecutorTest,AccountDeletionContextHandlersTest,ReleaseManifestTest test
PASS
```

PostgreSQL concurrency and vertical verification:

```text
./scripts/backend-testcontainers -Dtest=AccountErasureBarrierIT,AccountDeletionFlowIT test
Tests run: 3, failures: 0, errors: 0, skipped: 0
```

The scenarios prove:

- an event holding the lock commits before deletion, then its data is removed;
- an event after deletion is skipped;
- old personal events delivered twice after deletion do not recreate saved
  coffees, ticket projections, experience views/profiles or social profiles;
- all five context barriers are `ERASED`;
- a command after deletion does not execute and its canonical receipt is
  `REJECTED / ACCOUNT_ERASED`.

Migration/deployment verification:

```text
./scripts/backend-testcontainers -q -Dtest=StagingReleaseUpgradeIT,DeploymentSafetyIT test
Tests run: 12, failures: 0, errors: 0, skipped: 0
```

Full release verification:

```text
./scripts/test-release.sh
Tests run: 554, failures: 0, errors: 0, skipped: 0
BUILD SUCCESS
```

Existing shutdown noise remains visible: Spring test contexts leave scheduled
projection workers active while their Hikari pools close, and Surefire force
terminates the otherwise successful fork 30 seconds after `System.exit(0)`.
This tranche neither introduced nor hid that operational-test debt.

## Operational closure

The code path is implemented. FR-003 remains operationally open until the AWS
stack and backend are deployed and a dated drill from a real pre-erasure backup
passes. A local test cannot prove Object Lock, IAM or the real backup contents.
