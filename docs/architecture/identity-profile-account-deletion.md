# Identity, profile and account deletion

This document records the Lot 03 boundaries and lifecycle. Authentication owns
trusted provider identities and sessions; `userApplicationContext` owns the
product profile and coordinates deletion without reaching into another bounded
context's tables.

## Profile ownership and command flow

The public display name belongs to `AppUser`. It is normalized, must contain
between 2 and 50 characters after whitespace normalization, and rejects control
characters. Only an `ACTIVE` account can change it.

```text
mobile profile intent
-> optimistic reducer + account-partitioned local outbox
-> PATCH /api/users/me/profile
-> UpdateAppUserProfileCommand
-> AppUser invariant and JPA repository
-> app.user.profile_updated through the transactional outbox
-> command receipt / projection retrieval
-> mobile reconciliation or business-only rollback
```

`GET /api/users/me` is a user-application query backed by explicit JDBC. The
removed authentication `/auth/me` query no longer leaks product-profile
ownership into `authenticationContext`.

## Sign in with Apple

The native iOS adapter obtains an identity token and one-time authorization
code. The backend validates the Apple JWT issuer and audience, exchanges the
authorization code server-side, creates or retrieves the authentication
identity, and issues Fragments tokens. Apple provider refresh credentials are
encrypted at rest with AES-256-GCM and are never returned to the mobile app.

The remote Apple exchange occurs before the local transactional completion so
no database transaction is held across provider I/O. On account deletion, Apple
revocation likewise succeeds before local authentication data is erased and
acknowledged. A provider failure is therefore retried through SQS/inbox instead
of recording a false completion.

Runtime requirements are `APPLE_CLIENT_ID`, `APPLE_TEAM_ID`, `APPLE_KEY_ID`,
`APPLE_PRIVATE_KEY` and `AUTH_PROVIDER_CREDENTIAL_ENCRYPTION_KEY`. The private
key is a PKCS#8 `.p8`; the encryption key is a Base64-encoded 32-byte key.

## Durable deletion process

`DELETE /api/users/me` accepts a stable `commandId`. In one transaction the
`AppUser` moves to `DELETION_REQUESTED`, public profile fields are anonymized,
an `AccountDeletionProcess` is created, and `app.user.deletion_requested` is
written to the outbox. The command receipt becoming `APPLIED` means that this
request was durably accepted; it does **not** claim that every context has
finished erasing data.

The stable integration event is routed independently to the owning contexts:

| Owner | Local action before acknowledgement |
| --- | --- |
| `userApplicationContext` | Remove saved coffees, Pass contributions and its user Pass projection |
| `authenticationContext` | Revoke Apple credential when applicable, revoke refresh sessions, remove provider credential, anonymize auth identity |
| `socialContext` | Remove authored comments, likes and social/user projections |
| `ticketContext` | Remove tickets, fingerprints, status projections and ticket entitlements |
| `experienceContext` | Remove experiences, reports, moderation references and local user/block projections |

Each consumer persists inbox idempotence and emits its own primitive
`account.data_erased` fact. The process manager completes only after all five
distinct acknowledgements. Its acknowledgement load is pessimistically locked,
preventing concurrent SQS deliveries from losing one context's completion.
Duplicate request or completion events are safe.

The mobile deletion action is deliberately not an optimistic destructive
outbox mutation. It requires an explicit confirmation and a live session. A
technical uncertainty keeps the same command identifier for retry, so the user
can safely retry without producing a second process; only backend acceptance
then triggers local sign-out.

## Extension rule

Lot 03 covers every current user-owned store. When Experience and media stores
are introduced, their owning contexts must become explicit participants before
release: erase/anonymize local data, emit an acknowledgement, and be added to the
process completion invariant. Cross-context cleanup SQL is forbidden.

## Verification and rollback

Pure domain/use-case tests cover lifecycle, validation and process invariants.
Adapter tests cover Apple token exchange/revocation and encrypted credentials.
MockMvc/Testcontainers vertical tests cover authenticated HTTP, PostgreSQL,
transactional outbox, routed SQS/inbox duplication, each local eraser and final
process completion. Architecture tests continue to enforce bounded-context
imports.

Rollback is application-first: deploy the prior image while retaining the
additive lifecycle, credential and process tables/columns. Do not drop them
until no pending deletion process and no rollback window remain. Deletion and
remote Apple revocation are intentionally irreversible.
