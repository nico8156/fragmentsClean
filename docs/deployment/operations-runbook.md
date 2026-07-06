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

DLQ redrive must be explicit and should be preceded by reading backend logs for
the failed event ids.

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
