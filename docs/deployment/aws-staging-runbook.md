# Fragments AWS Staging Runbook

This runbook describes the minimal AWS staging deployment for Fragments.

## Target

- one EC2 ARM64 instance
- Docker Compose on the host
- PostgreSQL in Docker with an encrypted EBS data volume
- ECR for the backend image
- SQS standard queues with a shared DLQ
- Caddy for HTTPS
- no Kafka/MSK
- no Redis/ElastiCache

## Infrastructure

Deploy the stack:

```bash
aws cloudformation deploy \
  --region eu-west-3 \
  --stack-name fragments-staging-minimal \
  --template-file infra/aws/cloudformation/staging-minimal.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    KeyPairName=<existing-keypair> \
    VpcId=<vpc-id> \
    SubnetId=<public-subnet-id> \
    AllowedSshCidr=<your-ip>/32
```

Read stack outputs:

```bash
aws cloudformation describe-stacks \
  --region eu-west-3 \
  --stack-name fragments-staging-minimal \
  --query 'Stacks[0].Outputs'
```

## Host Bootstrap

SSH to the instance and create the deployment env:

```bash
ssh -i ~/.ssh/<key>.pem ubuntu@<instance-public-ip>
cd /srv/fragments/staging
```

Create `.env` from `infra/aws/compose/staging/.env.example` and fill:

- `BACKEND_IMAGE`
- `APP_DOMAIN`
- `POSTGRES_PASSWORD`
- `AUTH_JWT_SECRET`
- `GOOGLE_OAUTH_CLIENT_ID`
- `GOOGLE_OAUTH_CLIENT_SECRET`
- `GOOGLE_OAUTH_REDIRECT_URI`
- `OPENAI_API_KEY`
- `OPENAI_PROJECT_ID`
- all `SQS_*_URL` values from CloudFormation outputs

Production runtime must keep:

```properties
SPRING_PROFILES_ACTIVE=prod
APP_MESSAGING_KAFKA_ENABLED=false
APP_MESSAGING_LOCAL_EVENT_BUS_ENABLED=false
APP_MESSAGING_SQS_ENABLED=true
```

## GitHub Actions

Required repository secrets:

- `AWS_DEPLOY_ROLE_ARN`
- `STAGING_ECR_REPOSITORY`
- `STAGING_EC2_HOST`
- `STAGING_EC2_USER`
- `STAGING_SSH_PRIVATE_KEY`

`STAGING_ECR_REPOSITORY` must be the `EcrRepositoryUri` CloudFormation output.

The workflow:

1. runs backend tests;
2. builds the backend Docker image for `linux/arm64`;
3. pushes immutable `sha-<commit>` and `staging-latest` tags to ECR;
4. syncs Compose, Caddy, `schema.sql`, and `data.sql`;
5. restarts `postgres` and `backend`;
6. starts Caddy when `COMPOSE_PROFILES=https`;
7. smoke-tests `GET /actuator/health`.

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
- Do not enable Kafka in staging/prod.
- Do not add Redis unless a critical runtime need is documented.
- Do not commit `.env` or mobile secrets.
- Treat the local backend `.env` as sensitive if it contains real OAuth credentials.
- This first staging uses Docker Postgres for speed; move to RDS before relying on production user data.
