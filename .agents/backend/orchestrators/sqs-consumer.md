# Orchestrator - Backend SQS Consumer

Use this when adding or migrating an asynchronous event consumer to SQS.

## Responsibilities

- consume stable event envelopes from one queue
- route to a typed handler
- record inbox idempotence
- delete SQS message only after successful handling
- let DLQ handle poison messages

## Steps

1. Identify queue, DLQ, producer event type, and consuming BC.
2. Define or reuse a stable event contract and version.
3. Add destination routing in the outbox sender configuration.
4. Implement a thin SQS wrapper that delegates to a BC-local handler.
5. Add inbox duplicate suppression.
6. Ensure no equivalent legacy transport route exists for the same message.
7. Test successful handling.
8. Test duplicate delivery.
9. Test handler failure does not delete message.
10. Document queue name and DLQ expectation.

## Pitfalls

- BC-specific logic in sharedKernel
- running another transport consumer and SQS consumer for the same route in prod
- keying idempotence on technical database PKs
- deleting inbox rows as a retry shortcut
- acknowledging SQS before business success

## Validation

- duplicate messages do not duplicate state
- unknown event type/version is logged and handled deliberately
- delete-after-success behavior is explicit
- queue can be observed through logs/CloudWatch
