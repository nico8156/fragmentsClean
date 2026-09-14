# Apple login — optional email, 2026-09-14

## Observed failure

Read-only staging logs at 19:18:26Z and 19:18:35Z showed Apple login reaching
`CompleteAppleLogin`, then failing on `AuthUserJpaEntity.email` being null.
The code exchange and JWT validation had returned successfully. No credentials,
raw identity tokens or personal email addresses were collected for this diagnosis.
The exact reason this Apple response omitted email is not established.

The new PostgreSQL HTTP regression reproduced that exception before the fix.
After relaxing persistence, it exposed a second failure: Spring's JWT builder
rejects `.claim("email", null)`. Both defects must be fixed together.

## Contract and architecture

- `authenticationContext` identifies an account by `(provider, provider_user_id)`.
- Email is optional metadata, not an account identifier. No synthetic email and
  no automatic Google/Apple account linking are introduced.
- The Apple adapter normalizes missing/blank email to null and marks it unverified.
  Existing issuer, audience, signature and expiration validation remain unchanged.
- Auth persistence permits null email. Fragments JWTs omit the email claim when absent;
  session refresh uses the same token-generation path.
- Existing emails and profile data are not backfilled or overwritten.
- `auth.user.created` retains its current shape; its existing nullable email field
  remains nullable through the integration mapper. The consumer uses a name fallback
  and retains duplicate/replay safety. No new cross-context reads or writes.
- The mobile transport already accepts nullable email and does not depend on this
  JWT claim. No mobile runtime changes or new native build are required for this fix.

Apple documents optional/empty email cases:
https://developer.apple.com/documentation/signinwithapple/receiving-a-users-identity-token

## Migration and deployment

Separate manifest: `src/main/resources/db/release/apple-login-2026-09.psql`.
Fragment: `2026-09-14-apple-optional-email.sql`.

The manifest uses a transaction, bounded lock wait, advisory lock, checksum and
`release_schema_history`, following existing release conventions. Earlier applied
manifests are unchanged. The deployment renderer now includes this follow-up.
It can also be rendered separately after the app-store baseline; this fix does not
require completing the paused retention/restoration work.

1. Verify tests and review an immutable source/image revision.
2. Follow the existing backup/deployment procedure; apply the new manifest before
   starting the corrected backend. Never rely on Hibernate to change staging DDL.
3. Verify history contains `apple-login-2026-09` and `auth_users.email` is nullable.
4. On the existing TestFlight build, use a fresh Apple authorization attempt,
   reconnect, check the profile and verify Google sign-in remains functional.
5. Do not replay captured Apple authorization codes; they are short-lived/single-use.

Rollback: do not restore NOT NULL after email-less accounts exist, and do not
invent emails or delete accounts to enable a rollback. Keep the expanded schema.
An older backend remains broken for these accounts (including token refresh), so
prefer a forward fix; any binary rollback must acknowledge that limitation.

## Verification

- Unit: Apple creation/reconnection with/without email; nullable/blank email adapter
  mapping; user-profile creation and replay; integration-event serialization.
- Infra: fresh PostgreSQL schema and upgrade of the observed staging schema,
  migration replay/concurrency/checksums and preservation of existing data.
- Vertical: HTTP Apple login, real JPA persistence, encrypted provider credential,
  real JWT signature validation and refresh; existing Google/auth verticals.
- Mobile: existing Jest suite (no runtime source change).

Local results, 2026-09-14:

These first results were obtained on the local working branch before isolation.
The staging candidate is now isolated on `fix/apple-login-staging`, based on
published `main` (`8efa662`), whose application runtime matches `c8dea6a`.
The suspended privacy/retention commit is explicitly excluded; this candidate
ships only the Apple follow-up migration after the existing app-store baseline.
The isolated revision must pass deployment CI on Java 21 before any live change.

The isolated candidate also passed targeted Java 21 tests locally: Apple HTTP
login/refresh and persistence, Apple adapter and use case, profile consumer,
integration serialization, release manifest and PostgreSQL upgrade/replay.
Only three application source files differ from the published backend: Apple
adapter, auth JPA email mapping and Fragments JWT generation.

- `scripts/test-release.sh`: **550 tests passed**, zero failures/errors/skips,
  including architecture, PostgreSQL, LocalStack and HTTP vertical tests (4m18s).
  Fresh reports: `target/release-verification.9cRMwK`. Local runtime: Java
  23-valhalla, compilation target 21; deployment CI runs on Temurin 21.
- Mobile `jest --runInBand --silent`: **88 suites / 336 tests passed**.
- Mobile `tsc --noEmit`: passed; no mobile files modified.
- `bash -n` for migration renderer and deployment script, `git diff --check`: passed.

Delivery status: implemented and verified locally, not deployed. Native Apple
login on TestFlight remains to be checked after the corrected backend and migration
are deployed. No AWS settings, keys, live accounts or production data were changed.

This change does not close the separately paused retention/restoration work.

## Deployment attempt — 2026-09-14

The operator authorized staging deployment. Read-only SSM inspection
`9687154e-36ad-4288-8bf5-4a58850738d5` confirmed the healthy running image
`sha-c8dea6a977ee032cd638820538727a5b27800c0f`, successful last backup service
result, and only `app-store-2026-09` in migration history.

The release was isolated in `/tmp/fragments-apple-release.sGNHIf` on branch
`fix/apple-login-staging`; **34 targeted tests passed on Java 21**. The original
working branch and suspended privacy work are preserved locally.

The environment approval guard rejected a combined commit/push operation because
it would update remote `main` without separate explicit authorization. That
operation did not run. No workflow was dispatched and no live data/runtime was
changed. The existing deployment workflow requires `main`; publication to that
branch awaits explicit operator permission. Do not bypass this guard.
