# Article locale / carousel — staging delivery, 16 September 2026

## Scope and integration

Operator authorized merging/pushing the latest corrections, deploying the backend
and publishing the modified Studio to S3. The operator will build/distribute the
mobile app afterwards. All three clean feature branches were fast-forwarded to
`main` and pushed without force; feature branches are retained.

- Backend: `a1926001b1faaba9454f32dc25199d923914a514`.
- Mobile: `f72396ec68075986fc36bb05907e091d5a475263`.
- Studio: `4851f89c998fcc51e7691e100cbc38936c2d07d5`.

See the [diagnostic and local test evidence](../audits/article-carousel-locale-2026-09-16.md).
No new migration or editorial content rewrite is needed for locale compatibility.
The backend also includes the previously merged featured-rank concurrency fix.

## Studio — deployed and independently verified

[Deploy Studio staging, run 35096320584](https://github.com/nico8156/fragmentsAdmin/actions/runs/35096320584)
completed successfully. The existing pipeline publishes immutable releases and
current objects to `fragments-studio-staging-851725375299`, then invalidates
CloudFront distribution `E1A5H0256MH6EW`. No parallel manual upload was performed.

Independent HTTPS verification of https://studio-staging.anchor-event.fr:

- `release-manifest.json`: HTTP 200, current SHA as above, previous SHA
  `826ba786b9f9e9166ed91c2bedd9d2c4fcc3a1b6`, generated at
  `2026-09-16T12:32:23Z`.
- HTML: HTTP 200, application root present.
- `/assets/index-DnyasE_2.js`: HTTP 200, `text/javascript`, 303656 bytes.
- `/assets/index-DN4RkSnY.css`: HTTP 200, `text/css`, 13845 bytes.

## Backend — first deployment failed before runtime changes

The existing manual workflow was dispatched once for `main` with explicit staging
approval (GitHub HTTP 204):
[Deploy Staging Backend, run 35096606584](https://github.com/nico8156/fragmentsClean/actions/runs/35096606584).
Its head SHA is the backend revision above; job `104795410658` runs the complete
Java 21 release verification, including infra/vertical tests with no skips,
before packaging, building the ARM64 image and deploying through SSM.

The operator requested handoff while the Java 21 release verification was still
running. Monitoring stopped at that point. On the operator's subsequent alert,
the run was inspected: release verification failed (544 tests, zero assertion
failures, one error, zero skips). Packaging, image publication and SSM deployment
were skipped. No runtime replacement or migration was performed by this run.

`ArticleFeaturedRankConcurrencyIT.concurrent_rank_changes_on_one_article_keep_distinct_event_versions`
failed at its final `repository.byId` verification outside a transaction. The
aggregate repository's nested queries attempted a third connection while the
release pool contained only two; Hikari timed out after 30 seconds. Concurrent
commands and their distinct event-version assertions had already succeeded.

The targeted test correction reloads the persisted aggregate inside a fresh
transaction after both commands commit, matching the application use-case
boundary. The test explicitly retains a two-connection pool to guard against
regression. No assertions are removed, no pool/timeout is increased, and no
production/domain code or migration is changed by this follow-up.

Local verification with the same two-connection limit: `ArticleFeaturedRankConcurrencyIT`,
`JdbcArticleAggregateRepositoryIT`, and `ArticleLocaleReadCompatibilityIT` — six
tests passed, zero failures/errors/skips. PostgreSQL Testcontainers was used;
the original CI failure is the pre-fix evidence. Full release verification must
still succeed in the next workflow before deployment.

Follow-up merged/pushed: `7bac6146e5670a899a26da4b2a2429c9633412f9`.
The deployment workflow was dispatched for that revision (HTTP 204):
[retry run 35097611720](https://github.com/nico8156/fragmentsClean/actions/runs/35097611720),
observed `in_progress`. Final success and runtime/API checks are not yet attested.
The operator retains deployment monitoring as requested; no Studio or mobile
rebuild is required specifically for this backend-test-only follow-up.

Post-failure read-only checks: health HTTP 200 / `UP`, same pre-existing degraded
components; public `fr-FR` list still six articles with only featured rank 3.
The locale correction therefore remains undeployed until a new run succeeds.

After completion, verify the running image matches the backend SHA and check
`/api/articles?locale=fr-FR&limit=100`: expected ten published articles, including
five distinct featured ranks 1..5. Verify each featured detail returns HTTP 200
with canonical locale `fr-FR`, and that the `fr` alias returns the same catalogue.
These post-deployment checks remain pending. Baseline health was globally `UP`
with `articleAuthoringHealth` and `messagingRuntimeHealth` already `DEGRADED`;
this delivery does not claim to resolve those indicators.

## Boundaries retained

No real editorial command is issued as a smoke test. Five older public seed
articles without write-side ownership remain unchanged, pending the operator's
decision to import or remove them. Their Studio discrepancy is not fixed here.
Conservation/restoration work remains paused; a deployment backup does not resolve
that work. No mobile build/submission is performed by this task. Actual iPhone
swipe, pagination indicator, article navigation and vertical header acceptance
still require the next TestFlight build.
