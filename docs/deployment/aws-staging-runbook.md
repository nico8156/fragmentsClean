# Fragments AWS Staging Runbook

This runbook describes the minimal AWS staging deployment for Fragments.

For incident and release-1 verification commands, see
[operations-runbook.md](operations-runbook.md).

## Target

- one EC2 ARM64 instance
- Docker Compose on the host
- PostgreSQL in Docker with an encrypted EBS data volume
- ECR for the backend image
- SQS standard queues with a shared DLQ
- Caddy for HTTPS
- no Kafka/MSK
- no Redis/ElastiCache

## Lessons From First Deployment

| Symptom | Real cause | Fix | Prevention |
| --- | --- | --- | --- |
| CloudFormation rejected the template with `Unrecognized resource types: AWS::EC2::InstanceProfile`. | The instance profile resource type was wrong. | Use `AWS::IAM::InstanceProfile`. | Always run `aws cloudformation validate-template` before deploy. |
| EC2 came up without Docker. | Ubuntu 24.04 ARM64 does not provide the `awscli` apt package used by UserData; `set -e` stopped the bootstrap before Docker. | Install AWS CLI v2 from the official ARM64 archive and keep Docker install independent. | UserData must be idempotent and avoid distro packages that are absent on target AMIs. |
| Docker image build failed with `invalid reference format`. | `STAGING_ECR_REPOSITORY` contained whitespace/newline or an image tag. | Correct the secret to the bare ECR repository URI. | The workflow validates the secret before build. |
| GitHub Actions failed at `ssh-keyscan`. | The legacy workflow targeted an obsolete stopped EC2 and still depended on inbound SSH. | Deployment moved to the active `platform-staging` instance through SSM Run Command. | CI no longer owns an SSH key or mutates Security Group ingress. |
| Runtime `.env` risked becoming unreadable. | Secrets and operational values were initially mixed without ownership sections. | `.env.example` is grouped by responsibility and contains placeholders only. | Runtime config changes must preserve the documented sections. |

## Ticketverify Engine

The backend image builds the native `ticketverify` CLI in a dedicated Docker
stage from `nico8156/ticket_engine` at the pinned commit declared in the
`Dockerfile`.

Do not copy a locally built `bin/ticketverify` into the staging image manually:
local development binaries may be macOS artifacts. At runtime staging expects:

```text
TICKETVERIFY_BINARY_PATH=/app/bin/ticketverify
```

After deployment, ticket verification should be validated through the mobile
write flow or `scripts/demo.sh`. The command status endpoint may acknowledge the
accepted command before the ticket read model reaches its final
`CONFIRMED`/`REJECTED` status.

## Infrastructure

Deploy the stack:

```bash
aws cloudformation validate-template \
  --region eu-west-3 \
  --template-body file://infra/aws/cloudformation/staging-minimal.yaml

aws cloudformation deploy \
  --region eu-west-3 \
  --stack-name fragments-staging-minimal \
  --template-file infra/aws/cloudformation/staging-minimal.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    KeyPairName=<existing-keypair> \
    VpcId=<vpc-id> \
    SubnetId=<public-subnet-id> \
    AllowedSshCidr=<your-ip>/32 \
    GitHubRepository=nico8156/fragmentsClean \
    GitHubBranch=main
```

If the AWS account already has a GitHub OIDC provider, pass it explicitly to avoid creating a duplicate:

```bash
ExistingGitHubOidcProviderArn=arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com
```

Read stack outputs:

```bash
aws cloudformation describe-stacks \
  --region eu-west-3 \
  --stack-name fragments-staging-minimal \
  --query 'Stacks[0].Outputs'
```

## Host Bootstrap

Read the current shared staging host from the platform stack:

```bash
aws cloudformation describe-stacks \
  --region eu-west-3 \
  --stack-name platform-staging \
  --query "Stacks[0].Outputs[?OutputKey=='InstanceId'].OutputValue" \
  --output text
```

Use SSM for operator access. Inbound SSH is not part of CI/CD:

```bash
aws ssm start-session --region eu-west-3 --target <instance-id>
```

Create `.env` from `infra/aws/compose/staging/.env.example` and fill:

- `BACKEND_IMAGE`
- `APP_DOMAIN`
- `POSTGRES_PASSWORD`
- `AUTH_JWT_SECRET`
- `GOOGLE_MOBILE_IOS_CLIENT_ID`
- `GOOGLE_MOBILE_IOS_REDIRECT_URI`
- `GOOGLE_PLACES_API_KEY`
- `GOOGLE_PLACES_PHOTO_IMPORT_LIMIT`
- `COFFEE_PHOTOS_STORAGE_DIRECTORY`
- `COFFEE_PHOTOS_PUBLIC_BASE_URL`
- `COFFEE_PHOTOS_STORAGE_BACKEND`
- `COFFEE_PHOTOS_S3_BUCKET`
- `COFFEE_PHOTOS_S3_PREFIX`
- `COFFEE_PHOTOS_S3_REGION`
- `COFFEE_PHOTOS_S3_PRESIGN_TTL`
- `ARTICLE_IMAGES_STORAGE_BACKEND`
- `ARTICLE_IMAGES_S3_BUCKET`
- `ARTICLE_IMAGES_S3_PREFIX`
- `ARTICLE_IMAGES_S3_REGION`
- `ARTICLE_IMAGES_S3_PRESIGN_TTL`
- `OPENAI_API_KEY`
- `OPENAI_PROJECT_ID`
- all `SQS_*_URL` values from CloudFormation outputs

Production runtime must keep:

```properties
SPRING_PROFILES_ACTIVE=prod
APP_MESSAGING_LOCAL_EVENT_BUS_ENABLED=false
APP_MESSAGING_SQS_ENABLED=true
APP_MESSAGING_SQS_MAX_MESSAGES=5
APP_MESSAGING_SQS_WAIT_TIME=PT20S
APP_MESSAGING_SQS_VISIBILITY_TIMEOUT=PT30S
APP_MESSAGING_SQS_SHUTDOWN_TIMEOUT=PT5S
GOOGLE_PLACES_PHOTO_IMPORT_LIMIT=15
COFFEE_PHOTOS_STORAGE_DIRECTORY=/srv/fragments/coffee-photos
COFFEE_PHOTOS_PUBLIC_BASE_URL=https://<APP_DOMAIN>
COFFEE_PHOTOS_STORAGE_BACKEND=s3
COFFEE_PHOTOS_S3_BUCKET=anchor-assets-prod-851725375299
COFFEE_PHOTOS_S3_PREFIX=fragments/staging/coffees
COFFEE_PHOTOS_S3_REGION=eu-west-3
COFFEE_PHOTOS_S3_PRESIGN_TTL=PT15M
ARTICLE_IMAGES_STORAGE_DIRECTORY=/srv/fragments/article-images
ARTICLE_IMAGES_PUBLIC_BASE_URL=https://<APP_DOMAIN>
ARTICLE_IMAGES_STORAGE_BACKEND=s3
ARTICLE_IMAGES_S3_BUCKET=anchor-assets-prod-851725375299
ARTICLE_IMAGES_S3_PREFIX=fragments/staging/articles
ARTICLE_IMAGES_S3_REGION=eu-west-3
ARTICLE_IMAGES_S3_PRESIGN_TTL=PT15M
```

The staging backend reuses the Anchor asset bucket with an isolated Fragments prefix:

```text
s3://anchor-assets-prod-851725375299/fragments/staging/coffees/...
s3://anchor-assets-prod-851725375299/fragments/staging/articles/...
```

The EC2 runtime IAM role is limited to object operations under `fragments/staging/*`.

## GitHub Actions

No repository deployment secret is required. The stable, non-secret OIDC role
ARN is versioned in the workflow. Runtime secrets remain exclusively in
encrypted SSM parameters and are read by the EC2 role.

The role is owned by the dedicated
`fragments-staging-deploy-role.yaml` stack. It can push only the Fragments
backend repository and send `AWS-RunShellScript` only to the shared platform
instance. It has no Security Group mutation permission.

ECR and the target EC2 instance are resolved at runtime from the
`fragments-staging-minimal` and `platform-staging` stack outputs.

The workflow:

1. runs backend tests;
2. builds the backend Docker image for `linux/arm64`;
3. pushes immutable `sha-<commit>` and `staging-latest` tags to ECR;
4. resolves the active instance from the `platform-staging` stack;
5. invokes an immutable revision of `deploy-via-ssm.sh` through SSM Run Command;
6. rebuilds `/srv/fragments/staging/.env` from encrypted SSM parameters on the host;
7. applies the idempotent schema and recreates only the Fragments backend;
8. verifies the exact image and `GET /actuator/health` through SSM.

The workflow never receives database, JWT, Google or application secrets.

## Mobile / EAS

For the production EAS build, set:

```bash
eas secret:create --scope project --name EXPO_PUBLIC_API_BASE_URL --value https://fragments-staging.anchor-event.fr
```

Then build:

```bash
cd /Users/nicolasmaldiney/fragmentsCleanFront
npm test
npx eas build --profile production --platform ios
```

`app.config.js` intentionally fails production builds when `EXPO_PUBLIC_API_BASE_URL` is missing.

## Smoke Checks

From the EC2 host:

```bash
cd /srv/fragments/staging
docker compose ps
curl -i http://127.0.0.1:8080/actuator/health
docker compose logs backend --tail 200
```

From outside after DNS/HTTPS:

```bash
curl -i https://fragments-staging.anchor-event.fr/actuator/health
curl -i https://fragments-staging.anchor-event.fr/api/coffees
```

## Rollback

List existing image tags in ECR, then edit `/srv/fragments/staging/.env`:

```properties
BACKEND_IMAGE=<repository-uri>:sha-<previous-commit>
```

Redeploy:

```bash
cd /srv/fragments/staging
docker compose pull backend
docker compose up -d backend
curl -i http://127.0.0.1:8080/actuator/health
```

## Guardrails

- Do not expose backend port `8080` publicly; Compose binds it to `127.0.0.1`.
- Do not leave SSH `0.0.0.0/0` open. Use the workflow's temporary runner `/32` rule or SSM.
- Do not add Kafka/MSK to staging/prod. SQS is the backend propagation transport.
- Do not add Redis unless a critical runtime need is documented.
- Do not commit `.env` or mobile secrets.
- Treat the local backend `.env` as sensitive if it contains real OAuth credentials.
- This first staging uses Docker Postgres for speed; move to RDS before relying on production user data.

## Fresh AWS Account Checklist

1. Create or choose a key pair.
2. Choose a VPC and public subnet.
3. Validate the CloudFormation template.
4. Deploy the stack, leaving `ExistingGitHubOidcProviderArn` empty if no GitHub OIDC provider exists.
5. Read outputs and configure DNS.
6. Restrict Google Places to the EC2 Elastic IP.
7. Create GitHub Secrets from stack outputs.
8. Copy `infra/aws/compose/staging/.env.example` to `/srv/fragments/staging/.env`.
9. Fill runtime secrets directly on the EC2.
10. Run the manual workflow.
