# Fragments Operations Runbook

This runbook is for staging and release-1 operations. Commands are examples;
adapt container names and paths if the deployment changes.

Never print secrets or full payloads in shared logs.

## Runtime Location

Staging runtime directory:

```bash
cd /srv/fragments/staging
```

Runtime configuration:

```text
.env
docker-compose.yml
Caddyfile
db/schema.sql
db/data.sql
```

## Health

```bash
curl -sS http://127.0.0.1:8080/actuator/health
docker compose ps
docker compose logs --tail=200 backend
```

## Backend Image Drift

`BACKEND_IMAGE` in `.env`, the resolved Compose image, and the running
container image must match after a deployment:

```bash
cd /srv/fragments/staging
grep '^BACKEND_IMAGE=' .env
docker compose config --format json \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["services"]["backend"]["image"])'
docker inspect "$(docker compose ps -q backend)" --format '{{.Config.Image}}'
```

If `.env` and `docker compose config` are newer than the running container,
recreate only the backend service through the deployment workflow. Do not edit
the container manually as the normal path.

## Outbox Diagnostics

Inspect pending or failed events without dumping payloads:

```bash
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "select id,event_id,event_type,aggregate_type,aggregate_id,status,retry_count,created_at from outbox_events where status <> 'SENT' order by id desc limit 50;"
```

Failed outbox rows mean the backend could not publish to the configured
transport. Check:

- SQS queue URLs in `.env`;
- AWS region;
- EC2 IAM permissions;
- backend logs around the outbox id.

Replay must follow the normal dispatcher path. Do not write projections
directly.

## Inbox Diagnostics

Inspect failed inbox rows:

```bash
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "select destination,event_id,event_type,status,received_at,processed_at,left(error_message,180) as error from inbox_messages where status <> 'PROCESSED' order by id desc limit 50;"
```

Semantics:

- `PROCESSED`: duplicate SQS redelivery is suppressed.
- `FAILED`: SQS redelivery may retry the handler.
- `RECEIVED`: redelivery may retry after visibility timeout or crash.

Do not delete inbox rows as a normal retry strategy.

## SQS And DLQ

Queue URLs are injected from CloudFormation outputs by the staging GitHub
workflow. Verify runtime values without printing secrets:

```bash
grep '^SQS_.*_URL=' .env | cut -d= -f1
```

Use AWS CLI to inspect approximate depth:

```bash
aws sqs get-queue-attributes \
  --queue-url "$SQS_COFFEES_EVENTS_URL" \
  --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible
```

Each source queue has its own DLQ. The historical shared DLQ is deliberately
retained under the `legacy-awaiting-triage` lifecycle tag until every existing
message has been classified. New failures must not be routed to it.

List queue-to-DLQ bindings without reading message bodies:

```bash
for queue_url in $(aws sqs list-queues \
  --queue-name-prefix fragments-staging- \
  --query 'QueueUrls[?ends_with(@, `-events`) || ends_with(@, `-requested`)]' \
  --output text); do
  aws sqs get-queue-attributes \
    --queue-url "$queue_url" \
    --attribute-names QueueArn RedrivePolicy
done
```

DLQ triage order:

1. Record queue URL, message id, event type, event id, receive count and sent
   timestamp. Do not copy the full payload into a ticket or shared log.
2. Correlate the event id with backend and inbox logs.
3. Fix or explicitly accept the cause before any redrive.
4. Redrive to the original source queue in a bounded batch.
5. Verify inbox state, projection convergence and DLQ depth.

Do not redrive the legacy shared DLQ as one batch: messages there belong to
different source queues. Classify and replay them individually through the
correct source queue. Deletion is allowed only after the effect is proven to
have converged or the event has been explicitly declared obsolete.

CloudWatch alarms cover:

- any visible message in every per-queue DLQ;
- any visible message remaining in the legacy shared DLQ;
- source messages older than the configured threshold for three of five
  consecutive one-minute periods.

The optional `OperationsAlarmEmail` CloudFormation parameter creates an SNS
email subscription. AWS sends a confirmation message; alarms are not delivered
to that address until the subscription is confirmed.

## Projection Sync / SSE

Inspect durable sync events:

```bash
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "select id,event_name,projection,scope,entity_id,version,changed_at from projection_sync_events order by id desc limit 50;"
```

Test stream locally from the server:

```bash
curl -N -H "Authorization: Bearer $ADMIN_SECURITY_TOKEN" \
  http://127.0.0.1:8080/api/admin/sync/events
```

SSE must emit projection-oriented events only. It must not emit Domain Events.

## Coffee Photos

Read model stores stable photo references. S3 or local URLs are resolved at the
read boundary.

Check photo projection:

```bash
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "select coffee_id,count(*) from coffee_photos_projection group by coffee_id order by count(*) desc limit 20;"
```

If a photo is visible in S3 but not in the UI, check:

- `coffee_photos_projection`;
- `/api/admin/coffees` response;
- latest `projection_sync_events` for `projection='coffees'` and hints
  containing `photos`.

## Schema Policy For Release 1

Current staging applies `schema.sql` directly. This is acceptable only while
schema history is intentionally simple and idempotent.

Do not perform destructive changes without an explicit backup/reset decision.

Move to Flyway or Liquibase before production has long-lived user data with
non-trivial migrations.
