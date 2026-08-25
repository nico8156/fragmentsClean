# Runtime secrets contract

This contract defines names and ownership. It contains no secret values.

## Source of truth

Runtime secrets must live in AWS SSM Parameter Store as `SecureString` values.
For the current staging account they are encrypted with the existing AWS-managed
`alias/aws/ssm` key. The platform stack receives its key ARN explicitly through
`SecretsKmsKeyArn`.

The mutualised EC2 runtime may read only these namespaces:

```text
/anchor/staging/*
/fragments/staging/*
```

Production uses the same structure with the `production` environment segment.

## Fragments parameters

| Parameter | Use | Rotation impact |
| --- | --- | --- |
| `/fragments/{env}/POSTGRES_PASSWORD` | PostgreSQL runtime password | Coordinated DB and application restart |
| `/fragments/{env}/AUTH_JWT_SECRET` | Access/refresh token signing | Invalidates existing tokens if replaced |
| `/fragments/{env}/ADMIN_SECURITY_TOKEN` | Legacy/admin transition token | Rotate after OAuth admin flow is confirmed |
| `/fragments/{env}/GOOGLE_PLACES_API_KEY` | Google Places provider | Provider key rotation |
| `/fragments/{env}/OPENAI_API_KEY` | Optional OCR/editorial provider | Provider key rotation |
| `/fragments/{env}/OPENAI_PROJECT_ID` | Optional provider project | Configuration change |

Public OAuth client IDs and URLs are not secrets and remain build/runtime
configuration rather than entering the secret store.

## Anchor parameters

| Parameter | Use | Rotation impact |
| --- | --- | --- |
| `/anchor/{env}/POSTGRES_PASSWORD` | PostgreSQL runtime password | Coordinated DB and application restart |
| `/anchor/{env}/ANCHOR_WALLET_APPLE_AUTHENTICATION_TOKEN_SECRET` | Apple Wallet web service auth | Rotate with wallet clients |
| `/anchor/{env}/ANCHOR_WALLET_APPLE_SIGNING_KEYSTORE_PASSWORD` | Pass signing keystore | Requires wallet signing validation |
| `/anchor/{env}/ANCHOR_WALLET_APPLE_APNS_CERTIFICATE_PASSWORD` | Wallet push certificate | Requires APNs validation |
| `/anchor/{env}/ANCHOR_ADMIN_EMAIL` | Bootstrap identity | Sensitive operational configuration |

Apple certificate files remain protected files on the host or move to a
separately designed certificate store. They are not placed in SSM by this
phase.

## Explicit exclusions

These values are not application secrets:

- mobile API URL;
- Google public mobile client id;
- Studio API URL;
- OAuth redirect URI;
- ECR repository URI;
- SQS queue URL;
- Caddy domain names.

## Operational rules

- Never print a SecureString value in CI, shell output, or logs.
- Never use `set -x` in a secret-loading command.
- Never pass a secret as a command-line argument.
- Use separate database passwords for Anchor and Fragments.
- Rotate JWT only through an explicit session-impacting change window.
- Keep a rollback value available until the new health check passes.
- Record parameter names, actor, time, and result; never record parameter values.

## Current staging KMS choice

The current staging key is:

```text
alias/aws/ssm
arn:aws:kms:eu-west-3:851725375299:key/6e2d9298-8432-48e2-a566-bc10cbc78f2a
```

No new key is required for the initial migration. This choice is sufficient
for the current staging risk and avoids unnecessary key administration.
