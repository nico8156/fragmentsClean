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
| GitHub Actions failed at `ssh-keyscan`. | The EC2 Security Group allowed SSH only from a personal IP, not from the GitHub runner. | First deploy used a temporary broad SSH rule. | The workflow now opens SSH only for the current runner `/32` and revokes it at the end. |
| Runtime `.env` risked becoming unreadable. | Secrets and operational values were initially mixed without ownership sections. | `.env.example` is grouped by responsibility and contains placeholders only. | Runtime config changes must preserve the documented sections. |

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

Read the current staging host from CloudFormation:

```bash
aws cloudformation describe-stacks \
  --region eu-west-3 \
  --stack-name fragments-staging-minimal \
  --query "Stacks[0].Outputs[?OutputKey=='InstancePublicIp'].OutputValue" \
  --output text
```

SSH to the instance with the Ubuntu AMI user:

```bash
ssh -i ~/.ssh/anchor-staging-key.pem ubuntu@<instance-public-ip>
cd /srv/fragments/staging
```

If the key has another local filename, keep the same `ubuntu@<instance-public-ip>` target and replace only the `-i` path.
The public IP can change if the EC2 instance is replaced, so prefer the CloudFormation output over a hardcoded address.

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
GOOGLE_PLACES_PHOTO_IMPORT_LIMIT=3
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

Required repository secrets:

- `AWS_DEPLOY_ROLE_ARN`
- `STAGING_ECR_REPOSITORY`
- `STAGING_EC2_HOST`
- `STAGING_EC2_USER`
- `STAGING_SSH_PRIVATE_KEY`
- `STAGING_SECURITY_GROUP_ID`

`STAGING_ECR_REPOSITORY` must be the `EcrRepositoryUri` CloudFormation output.
It must not include a protocol, whitespace, or an image tag.

`AWS_DEPLOY_ROLE_ARN` must be the `GitHubDeployRoleArn` CloudFormation output.

`STAGING_SECURITY_GROUP_ID` must be the `InstanceSecurityGroupId` CloudFormation output.

The workflow:

1. runs backend tests;
2. builds the backend Docker image for `linux/arm64`;
3. pushes immutable `sha-<commit>` and `staging-latest` tags to ECR;
4. temporarily authorizes SSH only from the GitHub runner public IP;
5. syncs Compose, Caddy, `schema.sql`, and `data.sql`;
6. upserts `BACKEND_IMAGE`, Google Places, coffee photo storage, S3 photo storage, CORS, SQS URLs, and SQS consumer settings in `/srv/fragments/staging/.env`;
7. restarts `postgres` and `backend`;
8. starts Caddy when `COMPOSE_PROFILES=https`;
8. smoke-tests `GET /actuator/health`;
9. revokes the temporary runner SSH rule.

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
