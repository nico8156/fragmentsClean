# Backend Agent Routing - Fragments

Before implementing, classify the backend task.

## Command Feature

Use `orchestrators/command-feature.md` when a request changes state inside one bounded context.

Examples:
- submit ticket
- create/update comment
- toggle like
- refresh auth session state

## Query Feature

Use `orchestrators/query-feature.md` when exposing read-side data.

Examples:
- coffee list/detail
- article detail
- comments list
- ticket history
- command status

## Projection Feature

Use `orchestrators/projection-feature.md` when creating/updating a local read model from an event.

Examples:
- ticket event -> ticket read model
- social event -> social counts
- user event -> local user reference

## SQS Consumer

Use `orchestrators/sqs-consumer.md` when adding or migrating an event consumer to SQS.

Examples:
- social projection consumer
- ticket verification request consumer
- user projection consumer

## External Adapter

Use `orchestrators/external-adapter.md` when integrating technical systems.

Examples:
- S3
- SQS
- ticket OCR process
- Google auth

## Studio/Admin Feature

Combine `command-feature.md`, `query-feature.md`, and `external-adapter.md`
when an admin workflow spans UI input, object storage, and a bounded-context
command.

Examples:
- Studio article submission -> `CreateArticleCommand`
- Studio image upload -> article image storage adapter
- cafe import search/preview/import flow

Studio/admin controllers stay thin. They call admin use cases and ports, and the
owning bounded context still owns domain state, events, command status, and
read-model projection.

## Durable Process Manager

Use `orchestrators/process-manager.md` when a workflow spans transactions,
messages, remote work, or operator delays and must survive restart. Combine it
with command, SQS consumer, projection, query, and external-adapter
orchestrators as required. Do not use command status or SSE as process state.

## If Unsure

Do not code first.

Identify:
- owning bounded context
- whether state changes
- whether a read model is needed
- whether an event crosses BC boundaries
- whether mobile needs command status reconciliation
