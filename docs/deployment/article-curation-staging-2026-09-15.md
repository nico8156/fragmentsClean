# Article curation — staging release, 2026-09-15

## Authorized scope

The operator requested the article curation migration followed by a staging
backend deployment. The already-pushed source branch `main` contains the
curation behavior but not the paused privacy/restoration branch. This follow-up
adds a separately versioned migration manifest to the staging deployment
pipeline; it does not alter the earlier App Store or Apple migration checksums.

## Preflight and order

- AWS identity: account `851725375299`, operator `anchor-admin`.
- Target: `platform-staging` instance `i-004d3e9cbca327d01` and the existing
  `fragments/staging/backend` ECR repository in `eu-west-3`.
- Before this release, the running backend image is `sha-df222cd`, and
  `release_schema_history` contains `app-store-2026-09` and
  `apple-login-2026-09` only. No article curation migration has run.
- Source and image must use the same immutable Git revision. The existing
  `deploy-staging-backend.yml` workflow verifies the full Java 21 release suite,
  builds ARM64, and invokes the SSM deployment script.
- The deployment script resolves one database and one backend container,
  downloads and renders all three immutable manifests, checks configuration,
  pulls the candidate image, stops only the backend, runs and uploads a fresh
  PostgreSQL backup, then applies the migration stream. Only a successful SQL
  exit permits candidate backend startup and health checks.
- `article-curation-2026-09.psql` requires the Apple baseline, uses a transaction
  and advisory lock, records a checksum/source revision, and replays without
  mutating article rows. A failed or uncertain migration does not automatically
  restart the old image or erase history.

## Verification and remaining checks

Targeted PostgreSQL release and deployment safety tests passed locally before
workflow dispatch. The first full local release run found final Spring proxy
classes in the newly added article handlers/adapter; those were corrected and
the Spring integration test passed. The fresh full local release run completed
on 2026-09-15 at 13:24 CEST: **537 tests, zero failures/errors/skips**, including
infrastructure and vertical tests. The CI workflow will repeat it on Java 21.

After rollout, record the workflow run, backup receipt, migration history,
running image and HTTPS health. Then verify Studio commands and mobile
visibility on a real device. The conservation/restoration chantier remains
separate and paused; this release does not solve it.
