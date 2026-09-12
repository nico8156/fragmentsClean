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

The `messagingRuntimeHealth` actuator component reports pending, failed and
stale outbox/inbox work plus the age of the latest Projection Sync event.
Micrometer also registers `fragments.outbox.pending`,
`fragments.outbox.failed`, `fragments.inbox.failed` and
`fragments.projection.latest.age`. These meters remain internal unless a
secured registry/exporter is configured; do not expose `/actuator/metrics`
through the public reverse proxy.

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
- editorial operational degradation, sampled every five minutes from the
  backend health component; missing samples are also alarming.

The optional `OperationsAlarmEmail` CloudFormation parameter creates an SNS
email subscription. AWS sends a confirmation message; alarms are not delivered
to that address until the subscription is confirmed.

## Editorial operations

Inspect the health summary without exposing article or source payloads:

```bash
curl --silent http://127.0.0.1:8080/actuator/health \
  | jq '.components.editorialOperationsHealth'
```

Inspect only actionable state:

```sql
select source_id,name,status,failure_count,next_check_at,lease_until
from editorial_sources
where enabled = true and (status = 'DEGRADED' or lease_until < now());

select schedule_id,article_id,operation,due_at,status,lease_until,rejection_reason
from editorial_publication_schedule
where status in ('SCHEDULED','CLAIMED','DISPATCHED','REJECTED')
order by due_at;

select operation,model,outcome,occurred_at
from editorial_generation_executions
where outcome = 'FAILED' and occurred_at >= now() - interval '24 hours'
order by occurred_at desc;
```

Recovery rules:

1. Do not edit a lease or schedule row manually.
2. A failed source follows its persisted backoff. Correct its endpoint or
   disable it from Studio if the provider is permanently unavailable.
3. An expired source or schedule lease is reclaimable automatically.
4. For `DISPATCHED`, inspect `/commands/{scheduleId}`; that endpoint is the
   source of truth. Never infer publication from the scheduler log alone.
5. `REJECTED` is a domain decision requiring editorial correction or a new
   schedule; it must not be blindly retried.
6. For an SQS DLQ incident, follow the queue-specific redrive procedure above.

The staging probe is managed by `fragments-editorial-health.timer`. Verify it
with:

```bash
systemctl status fragments-editorial-health.timer --no-pager
journalctl -u fragments-editorial-health.service --no-pager -n 50
```

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

## Social moderation

The App Store release requires an explicitly assigned moderation operator and a
real support address. Before enabling user-generated content in production:

- set `EXPO_PUBLIC_SUPPORT_EMAIL` in the mobile release environment;
- assign a named Studio operator with the `MODERATOR` or `SUPER_ADMIN` role;
- review the `OPEN` moderation queue at least once per working day;
- treat threats, harassment and personal-data exposure as urgent and review them
  within 24 hours;
- configure `FRAGMENTS_SOCIAL_MODERATION_FORBIDDEN_TERMS` as a comma-separated
  list for additional high-confidence phrases. This first barrier supplements,
  but never replaces, human review.

Studio moderation decisions must go through the domain command endpoint. Their
persisted Studio operation carries the `commandId`; `/commands/{commandId}` is
the source of truth after a timeout. Do not edit comments, report projections,
or moderation-action rows manually.

To triage without exposing unnecessary user content, start with aggregate
counts:

```sql
select status,count(*)
from social_content_reports_projection
group by status;
```

Every hide or restore decision is projected in
`social_moderation_actions_projection`. A restore is a new audited domain
transition; it must not delete prior history.

## Private Media And Release Rate Limits

The iOS application uploads directly through short-lived S3 presigned URLs.
The configured bucket must keep all four public-access blocks enabled and a
default encryption rule. Native iOS requests do not use browser CORS; no bucket
CORS policy is therefore the least permissive staging configuration. If a web
uploader is introduced later, add only its exact HTTPS origin and the signed
`PUT` headers instead of a wildcard rule.

Verify configuration without listing user objects:

```bash
aws s3api get-public-access-block --bucket "$PRIVATE_MEDIA_S3_BUCKET"
aws s3api get-bucket-encryption --bucket "$PRIVATE_MEDIA_S3_BUCKET"
aws s3api get-bucket-cors --bucket "$PRIVATE_MEDIA_S3_BUCKET"
```

`NoSuchCORSConfiguration` is expected for the native-only flow. The runtime IAM
policy grants `GetObject`, `PutObject` and `DeleteObject` only below the known
coffee, article, private-media and PostgreSQL-backup prefixes. It must not regain
the broad `fragments/staging/*` object resource.

Staging enables per-user, single-node rate limits for costly authenticated
intentions:

- ticket verification: 10 per minute;
- private media upload/confirmation: 30 per minute;
- experience, comment, report and block writes: 60 per minute.

A limited call returns HTTP `429`, the stable `RATE_LIMITED` error and a
`Retry-After` header. Mobile treats it as a technical retryable response: the
optimistic intent and outbox record remain. These counters intentionally live in
memory because release staging is a single backend node. Before horizontal
scaling, move enforcement to a shared edge/gateway facility or another explicitly
approved technical mechanism; do not introduce Redis implicitly.

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

## PostgreSQL Backup And Restore Drill

The active runtime installs a daily systemd timer. A deployment also performs
a synchronous backup before applying `schema.sql`; schema mutation is refused
if the backup cannot be uploaded.

Backups are custom-format `pg_dump` artifacts with a SHA-256 sidecar under:

```text
s3://anchor-assets-prod-851725375299/fragments/staging/backups/postgres/
```

S3 server-side encryption is explicitly requested. Never download an artifact
to an operator workstation unless an incident requires it.

Inspect the timer and recent backups:

```bash
systemctl status fragments-postgres-backup.timer
journalctl -u fragments-postgres-backup.service --since '2 days ago'
aws s3 ls s3://anchor-assets-prod-851725375299/fragments/staging/backups/postgres/
```

Run an on-demand backup:

```bash
sudo systemctl start fragments-postgres-backup.service
sudo systemctl status fragments-postgres-backup.service
```

The restore drill never overwrites the live database. It downloads one
allow-listed artifact, verifies its checksum, restores it into a uniquely named
temporary database, verifies that public tables exist, then removes that
temporary database:

```bash
sudo /srv/fragments/staging/restore-postgres-drill.sh \
  s3://anchor-assets-prod-851725375299/fragments/staging/backups/postgres/fragments-YYYYMMDDTHHMMSSZ.dump
```

A successful upload is not proof of recoverability. Run and record a restore
drill after this mechanism is first deployed, then at least monthly and after a
PostgreSQL image upgrade. A live restore remains a separate incident procedure
requiring an explicit recovery decision and maintenance window.

## Schema Policy For Release 1

Current staging applies `schema.sql` directly. This is acceptable only while
schema history is intentionally simple and idempotent.

Before deploying the owner-scoped command receipt change to an existing
environment, apply the additive script
`src/main/resources/db/release/2026-09-11-command-receipts.sql`, then deploy the
application. Existing ownerless rows remain readable by Studio administrators
but intentionally resolve as `PENDING` on the authenticated mobile endpoint.

Do not perform destructive changes without an explicit backup/reset decision.

Move to Flyway or Liquibase before production has long-lived user data with
non-trivial migrations.
