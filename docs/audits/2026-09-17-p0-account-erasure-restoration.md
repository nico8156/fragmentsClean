# P0 account-erasure restoration and replay remediation

Date: 2026-09-17
Branch: `fix/p0-inbox-sqs-ack-safety`
Status: implementation complete locally; AWS application and real-backup drill pending

## Failure mode

A PostgreSQL backup taken before an accepted account deletion contains neither
the later cleanup nor its database barriers. Restoring that dump alone therefore
resurrects the account, projections, OCR/content payloads and media references.
The database cannot be the sole memory of its own erasures.

## Implemented design

Before `DELETE /api/users/me` can return `202`, `RequestAccountDeletion` writes
a create-only JSON marker to an independent S3 journal. A journal outage fails
the request before command acceptance; the mobile outbox can safely retry. The
marker key is deterministic by user and request, contains opaque UUIDs and a
timestamp only, and duplicate writes are accepted only when identity matches.

The dedicated CloudFormation bucket has:

- public access fully blocked;
- AES-256 default encryption;
- versioning;
- Object Lock governance retention for 45 days;
- expiry at 46 days, beyond the observed 30-day backup retention;
- a runtime policy with Get/Put/List but no Delete or retention bypass.

On normal deletion completion, `JdbcPersonalDataResidueStore` removes local
outbox payload copies and projection-sync payloads containing the erased
identity. Inbox rows are deliberately retained because they contain processing
metadata rather than payloads and are required for duplicate suppression;
deleting the currently leased acknowledgement row would make a successful
consumer retry. Business data remains erased by its owning bounded contexts.

`restore-postgres-drill.sh` now makes `replay-account-erasures.sh` mandatory.
For every retained marker the replay:

1. discovers avatar and experience object keys in the restored snapshot;
2. removes current objects, historical object versions and delete markers;
3. runs an idempotent recovery-only SQL purge across all participating contexts;
4. revokes tokens/admin access and anonymizes authentication/profile rows;
5. purges restored technical payload copies;
6. recreates all five `ERASED` barriers;
7. rejects the drill if any representative residual row or barrier is wrong.

The cross-context SQL is explicitly an offline disaster-recovery adapter. It is
not available to application traffic and does not weaken bounded-context
ownership during normal behavior.

## Files

- `AccountErasureJournal` and `S3AccountErasureJournal`
- `RequestAccountDeletion`
- `PersonalDataResidueStore` and `JdbcPersonalDataResidueStore`
- `infra/aws/cloudformation/platform-staging.yaml`
- `infra/aws/compose/platform/staging/fragments/replay-account-erasures.{sh,sql}`
- `infra/aws/compose/platform/staging/fragments/restore-postgres-drill.sh`
- `docs/deployment/operations-runbook.md`

## Verification

Passed locally on Java 21:

```text
./mvnw -q -DskipTests compile
./mvnw -q -Dtest=S3AccountErasureJournalTest,PostgresRecoveryGuardrailTest,DurableCommandExecutorTest,AccountDeletionContextHandlersTest test
./mvnw -q -Dtest=AuthMeIT test
./scripts/test-release.sh
```

The release suite was rerun against Docker Desktop 27.4.0 with real PostgreSQL
Testcontainers and LocalStack. Result on 2026-09-17:

```text
Tests run: 557, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 05:47 min
Fresh reports: target/release-verification.CUjlBe
```

`StagingReleaseUpgradeIT` passed all six scenarios, including an old snapshot,
mandatory erasure replay and a second replay proving idempotence. The first
complete run exposed that `AuthMeIT` did not provide its test journal; this was
fixed with a test-only port implementation while production remains fail-closed,
then validated by the targeted test and the successful full run above.

Shell parsing and whitespace checks pass for the deployment/recovery scripts.
The suite still logs known shutdown noise: scheduled projection/ticket workers
can race Spring/Testcontainers teardown, causing closed-connection warnings and
Surefire to terminate the otherwise successful fork after 30 seconds. This did
not fail or skip tests, but remains separate test-runtime hygiene debt.

Remote CloudFormation validation passed on 2026-09-17 after explicit export
authorization:

```text
aws cloudformation validate-template \
  --region eu-west-3 \
  --template-body file://infra/aws/cloudformation/platform-staging.yaml
```

AWS returned the expected template description and parameter catalog. This was
validation only: no change set was created and no AWS resource was mutated.

Required after AWS/backend deployment: create a synthetic account and private
media, take a backup, delete it, choose the earlier backup, run the restore
drill, retain only aggregate/no-PII evidence, and confirm zero residual rows,
five barriers and absent S3 versions.

## Remaining operational condition

The P0 implementation is code-complete but the audit blocker is not marked
closed until the real CloudFormation change is applied, the backend uses the
journal settings, and the dated staging restore drill succeeds. No production
restore or AWS mutation was performed by this local tranche.
