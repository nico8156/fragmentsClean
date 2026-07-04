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

## Runtime Polling Model

The backend runs one long-poll worker per unique SQS queue URL.

Rules:
- deduplicate queue URLs before starting workers;
- do not poll all queues sequentially from one global scheduler;
- use SQS long-polling, default `APP_MESSAGING_SQS_WAIT_TIME=PT20S`;
- keep `APP_MESSAGING_SQS_MAX_MESSAGES` bounded, default `5`;
- delete each message only after the routed handler succeeds;
- leave failed messages on the queue so SQS visibility timeout and DLQ policy can drive retry.

Reason:
- queue-to-queue latency must not grow with the number of configured queues;
- a quiet queue must not delay a busy queue;
- async chains such as `CoffeeCreatedEvent -> projection -> ProjectionSyncEvent -> SSE` need predictable propagation without bypassing the outbox/SQS architecture.

The consumer is technical sharedKernel infrastructure. It routes stable integration event envelopes; it must not contain bounded-context business logic.
