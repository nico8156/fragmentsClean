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

## Backend — deployment in progress

The existing manual workflow was dispatched once for `main` with explicit staging
approval (GitHub HTTP 204):
[Deploy Staging Backend, run 35096606584](https://github.com/nico8156/fragmentsClean/actions/runs/35096606584).
Its head SHA is the backend revision above; job `104795410658` runs the complete
Java 21 release verification, including infra/vertical tests with no skips,
before packaging, building the ARM64 image and deploying through SSM.

The operator requested handoff while the Java 21 release verification was still
running. Monitoring stopped at that point; backend deployment success is not
attested by this receipt. The operator is monitoring the run linked above.

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
