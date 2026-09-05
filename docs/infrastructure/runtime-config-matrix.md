# Runtime configuration matrix

This matrix records ownership and handling rules, not secret values.

## Configuration classes

| Class | Examples | Source of truth | Runtime handling |
| --- | --- | --- | --- |
| Versioned non-secret config | region, service names, feature flags, paths | Git per environment | Compose/application configuration |
| AWS resource references | ECR repository, SQS URL, S3 bucket/prefix | CloudFormation outputs and environment config | Injected at deployment |
| Application secrets | JWT secret, database password, provider keys | SSM Parameter Store or Secrets Manager | Injected at runtime, never committed |
| Build-time public values | mobile API URL, Google public client id, Studio API URL | EAS/Vite environment | Embedded only in the relevant public bundle |
| Operator credentials | deploy role, SSH/SSM access | GitHub/AWS IAM | Short-lived OIDC or operator session |

## Server

| Area | Non-secret values | Secret values |
| --- | --- | --- |
| Fragments | Spring profile, SQS region/URLs, S3 bucket/prefixes, CORS origins | PostgreSQL password, JWT secret, Google Places key, OpenAI key, admin bootstrap values |
| Anchor | Spring profile, SQS destinations, S3 bucket, public URLs, wallet flags | PostgreSQL password, Apple signing credentials, wallet auth secret, provider keys |

Both servers must keep separate database credentials and separate application
secret namespaces even when they share an EC2 host.

## Mobile

Public build configuration only:

- `EXPO_PUBLIC_API_BASE_URL`;
- `EXPO_PUBLIC_GOOGLE_MOBILE_IOS_CLIENT_ID`;
- `EXPO_PUBLIC_GOOGLE_MOBILE_IOS_REDIRECT_URI`.

The mobile bundle must never receive backend secrets, AWS credentials, admin
tokens, or database credentials.

## Studio

Public build configuration only:

- `VITE_FRAGMENTS_BACKEND_URL`;
- `VITE_STUDIO_AUTH_MODE=oauth`;
- gateway paths;
- the confirmed Studio public origin through the backend OAuth config.

`VITE_ADMIN_IMPORT_BEARER_TOKEN` and
`VITE_PROJECTION_SYNC_BEARER_TOKEN` are forbidden in production builds. Studio
must use its OAuth session and must not embed a backend service token.

## Migration rule

The deployment renders the runtime `.env` from AWS SSM values on the host.
Committed files remain non-secret templates only. Secret ownership, parameter
names and IAM access boundaries are documented in `secrets-contract.md`.
