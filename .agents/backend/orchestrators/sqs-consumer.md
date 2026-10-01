# Orchestrator - Backend SQS Consumer

Use this when adding or migrating an asynchronous event consumer to SQS.

## Responsibilities

- consume stable event envelopes from one queue
- route to a typed handler
- record inbox idempotence
- delete SQS message only after successful handling
- let DLQ handle poison messages

## Steps

Read [the iteration workflow](../../iteration-workflow.md) first. These are
boundary responsibilities, not a precomputed implementation sequence. For
`BEHAVIOUR`, express one observable example, inspect its RED and implement only
its minimum GREEN before the next example. Reuse existing contracts; introduce
new structures only when an example or invariant requires them. Continue on
`PASS`/`REVIEW`, escalate material ambiguity, then apply the targeted mutation
checkpoint to the green slice and pin missing protection. `REFACTORING` starts
green; `CHORE` uses proportionate checks.

1. Identify queue, DLQ, producer event type, and consuming BC.
2. Define or reuse a stable event contract and version.
3. Drive successful local handling through one RED/minimum GREEN example.
4. Drive duplicate suppression through the next RED/minimum GREEN example.
5. Drive failure-without-delete through another RED/minimum GREEN example.
6. Prove destination routing and the thin SQS wrapper with boundary tests first.
7. Keep the SQS wrapper delegating to the BC-local handler.
8. Verify inbox-backed effects with integration tests.
9. Ensure no equivalent legacy transport route exists for the same message.
10. Document queue name and DLQ expectation.

## Pitfalls

- BC-specific logic in sharedKernel
- running another transport consumer and SQS consumer for the same route in prod
- keying idempotence on technical database PKs
- deleting inbox rows as a retry shortcut
- acknowledging SQS before business success
- deserializing a producer domain event instead of a versioned public contract
- calling a slow remote provider while holding the inbox/business transaction
  open

When a consumed event starts long remote work, record/claim durable work and
commit the inbox-backed handling first. Execute remote work outside that
transaction and complete it through an idempotent command.

## Validation

- duplicate messages do not duplicate state
- unknown event type/version is logged and handled deliberately
- delete-after-success behavior is explicit
- queue can be observed through logs/CloudWatch
