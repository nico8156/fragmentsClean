# Apple login — staging rollout, 2026-09-14

## Authorized scope

Operator approved staging deployment, then explicitly approved merge/push to
`main`. Corrective source revision: `df222cd172aa4080d6f9292e2f000633b822c7a9`.
Published with a fast-forward from `8efa662`, also retained on
`fix/apple-login-staging`. No forced remote update.

The candidate was extracted from the working branch to exclude suspended
privacy/retention work (`5fd0fc3`). Only Apple authentication, optional-email
persistence/JWT generation and the separate `apple-login-2026-09` migration ship.
The original working branch remains intact; no paused cleanup was activated.

## Evidence

- Before rollout, SSM `9687154e-36ad-4288-8bf5-4a58850738d5` showed the backend
  running `sha-c8dea6a977ee032cd638820538727a5b27800c0f` and only the original
  `app-store-2026-09` migration.
- Isolated candidate: 34 targeted tests passed locally on Java 21, including
  native credential HTTP exchange at a fake provider boundary, actual PostgreSQL,
  signed Fragments JWTs, refresh and migration upgrade/replay.
- GitHub workflow: https://github.com/nico8156/fragmentsClean/actions/runs/34891986167
  Job: `104136680352`. Source SHA verified before monitoring.
- Workflow performs the full Java 21 release suite, packages/builds ARM64,
  backs up PostgreSQL before migration and verifies the deployed image and health.

Rollout status: **successful**, workflow completed 2026-09-14 around 20:32 UTC.

- Full CI release verification on Java 21: **523 tests**, zero failures, errors
  or skipped tests. This isolated baseline deliberately excludes the paused
  privacy work; the earlier 550-test local result covered that other branch.
- Post-deployment read-only SSM command:
  `08ae0334-3b3d-4418-baa2-29d48efe41ca`, status `Success`.
- Running image:
  `851725375299.dkr.ecr.eu-west-3.amazonaws.com/fragments/staging/backend:sha-df222cd172aa4080d6f9292e2f000633b822c7a9`.
  Container started at `2026-09-14T20:32:01.412838535Z`.
- Migration history contains exactly `app-store-2026-09` (unchanged source
  `c8dea6a`) and `apple-login-2026-09` (source `df222cd`); email nullability is
  `YES`. No technical-retention migration was applied.
- Pre-deployment backup service succeeded (exit 0), from `20:31:58` to `20:32:01`
  UTC, uploading
  `s3://anchor-assets-prod-851725375299/fragments/staging/backups/postgres/fragments-20260914T203158Z.dump`.
  This proves a backup upload, not a new restoration drill.
- Public HTTPS `/actuator/health`: global `UP`; database, readiness, liveness,
  disk, ticket verification and editorial operations `UP`.
- `articleAuthoringHealth` and `messagingRuntimeHealth` remain `DEGRADED`.
  Both were already documented in the September 12 deployment receipt; this
  rollout does not claim to repair them. No outbox/inbox purge or forced replay
  was performed. Global health is not proof of all business journeys.

Native Apple login on the actual TestFlight app remains to be confirmed by the
operator. The backend correction is live; no app rebuild was requested.

## Device acceptance after rollout

On the existing TestFlight build, start a fresh Sign in with Apple request,
check the profile, then sign out and back in. Check Google sign-in separately.
No new iOS binary is required for this server-only fix. Do not reuse an expired
Apple authorization code. Actual Apple device acceptance is an operator check,
not proven by fake-provider automated tests or an HTTP health check.

Rollback constraints and original diagnosis:
[Apple optional-email correction](apple-login-null-email-2026-09-14.md).
The broader conservation/restoration work remains paused and unresolved.
