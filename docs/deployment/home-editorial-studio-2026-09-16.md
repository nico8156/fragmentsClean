# Home/editorial hardening — integration and Studio publication

## Authorized scope

On 2026-09-16 the operator authorized merging/pushing the three verified branches
and publishing Studio to S3. Backend runtime deployment and a mobile/TestFlight
build are not part of this operation. No database migration is needed.

## Git integration

All worktrees were clean and local `main` matched `origin/main` after fetch.
Each feature branch was merged with `--ff-only`, then pushed without force:

- backend: `4c7103a1472b39bb29afe71a2a911943076086c1`;
- mobile: `1b18e26820cdeae5f36eafd533003114a7d79636`;
- Studio: `826ba786b9f9e9166ed91c2bedd9d2c4fcc3a1b6`.

Feature branches are retained. Implementation, test evidence and remaining device
acceptance checks are in the [hardening receipt](../audits/home-startup-editorial-hardening-2026-09-16.md).

## Studio delivery

The existing push-triggered workflow is used, with no parallel manual upload:
[Deploy Studio staging, run 35077039577](https://github.com/nico8156/fragmentsAdmin/actions/runs/35077039577).

- AWS region: `eu-west-3`.
- Bucket: `fragments-studio-staging-851725375299`.
- CloudFront distribution: `E1A5H0256MH6EW`.
- Public URL: https://studio-staging.anchor-event.fr.
- Previous release: `b19ee7369261eafae1766d8f144a1041bf8ee6ee`.
- Candidate release: `826ba786b9f9e9166ed91c2bedd9d2c4fcc3a1b6`.

Pipeline checks the backend-owned API contract, runs Studio tests, builds in OAuth
mode without browser bearer secrets, verifies the distribution, uploads immutable
`releases/<sha>/` and current objects, updates the manifest, invalidates CloudFront
and checks the served HTML/manifest. It does not delete old release objects.

### Publication receipt

Run `35077039577`, job `104731804619`: **completed / success**. Contract check,
tests, build, distribution security check, S3 upload and CloudFront invalidation/
smoke all succeeded. Public manifest records generation at `2026-09-16T09:02:18Z`,
the expected candidate SHA and the previous release above.

An independent HTTPS check confirmed the manifest, HTTP 200 HTML with the React
root and both referenced assets (HTTP 200 with correct MIME types):

- `/assets/index-Be42SZSi.js`, 303117 bytes, SHA-256
  `c92e8646b321d5d31d76b0f301d6850a7b036cc59e22a1c2643b589fd6f9b38e`;
- `/assets/index-DN4RkSnY.css`, 13845 bytes, SHA-256
  `1fc7b97299048365c37ef0d0b179869c9067c781dc7f3c7152a02fd9873b6cec`.

The previous immutable release is retained for rollback; no rollback was needed.
Remote `main` revisions were checked against the local commits in all three repos.

## Remaining boundaries

The backend rank-concurrency correction is pushed but not deployed by this task.
Studio's preventive controls work with the existing contract, but stronger server
concurrency guarantees require the subsequent backend rollout. Mobile corrections
also need a new build/distribution before TestFlight testers receive them.

No editorial commands were executed against real content as a smoke test. Device
and two-operator acceptance remain open. Conservation/restoration remains paused.
