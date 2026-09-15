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

## Rollout receipt

- [GitHub staging run](https://github.com/nico8156/fragmentsClean/actions/runs/34963268908),
  job `104361658421`: **completed/success**. Java 21 release verification,
  ARM64 image build/push, SSM deployment and post-deployment image/health check
  all succeeded.
- Read-only SSM check `01c0da3f-0fad-440d-894c-a956e700b149`:
  running image is ECR `fragments/staging/backend:sha-e5c587a4cc2ba0d87b15aa5207309d620856345f`.
  `release_schema_history` retains the App Store and Apple receipts unchanged
  and adds `article-curation-2026-09` with source revision `e5c587a` at
  `2026-09-15 11:41:51.952772 UTC`. Both article rank columns are queryable;
  zero existing write/projection rows are featured. No automatic editorial
  promotion occurred.
- Fresh pre-migration backup service: `Result=success`, `ExecMainStatus=0`.
  S3 contains `fragments-20260915T114149Z.dump` (713,026 bytes) and its
  `.dump.sha256` companion under the exact Fragments staging backup prefix.
  This verifies upload/receipt, **not** a fresh restoration drill.
- Public HTTPS `/actuator/health`: global `UP`; database, readiness, liveness,
  ticket verification and editorial operations `UP`. The
  `articleAuthoringHealth` and `messagingRuntimeHealth` indicators remain
  `DEGRADED`, previously documented and not fixed by this release.
- `GET /api/articles?locale=fr-FR&limit=5`: five published items, pagination
  cursor present, and nullable `featuredRank` present in every returned item.
  No ranks are assigned yet, as intended until an editor curates Studio.

## Operator acceptance still required

Verify Studio withdrawal/featured-rank commands, projection/SSE freshness and
mobile catalogue visibility on a real device. The Studio staging site was
published automatically from its own `main` commit `b19ee73`; its deployment
run completed successfully and the CloudFront manifest reports that same SHA.
No new Studio publication was needed during this backend verification.

The conservation/restoration chantier remains separate and paused; this
release does not solve it.
