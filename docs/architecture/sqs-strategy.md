# SQS Strategy

## Decision

Fragments production MVP targets AWS SQS instead of Kafka.

Reason:
- lower operational complexity
- lower cost for MVP traffic
- simple AWS deployment
- built-in retry/DLQ model
- enough guarantees for current flows

## Queue Model

Use one queue per event destination or closely related consumer group.

Each queue must have:
- DLQ
- visibility timeout aligned with handler duration
- structured logs
- idempotent consumer

## Contract

Every SQS message carries a stable event envelope:
- event id
- command id when applicable
- event type
- version
- source context
- destination
- aggregate id/type when applicable
- occurred at
- payload JSON

## Consumer Rule

Delete message only after successful business handling.

If delete fails, redelivery must be safe because the inbox suppresses duplicate business work.

