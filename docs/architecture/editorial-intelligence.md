# Editorial Intelligence and Article Lifecycle

Status: phases 1 through 10 implemented locally. Runtime activation and the
CloudFormation update remain deployment decisions.

## Purpose

Fragments separates discovering an editorial opportunity from deciding to write
and publish an article about it.

```text
EditorialSource
-> SourceSignal
-> TopicCandidate
-> human retention decision
-> ArticleBrief
-> articleContext authoring saga
```

The collection layer never calls OpenAI, creates an article, or publishes one.
The scheduler only advances time; domain commands retain all business
decisions.

## Context ownership

`editorialIntelligenceContext` owns:

- `EditorialSource`: name, access mode, authority level, language, geography,
  topics, cadence, enabled state and operational status;
- `SourceCheckpoint`: provider cursor, ETag, Last-Modified, last external id,
  content fingerprint and successful collection position;
- `SourceConsultation`: durable operational history and backoff decision;
- `SourceSignal`: normalized immutable source content;
- `TopicCandidate`: grouped signals, suggested angle, freshness, proximity,
  score and human decision;
- editorial planning projections.

`articleContext` owns articles, revisions, `ArticleAuthoringSaga`, editorial
validation, approval, planning of a retained article, publication and archive.
It does not own sources, signals or candidates.

`adminImportContext` remains the Studio ACL. Studio never writes editorial or
article tables directly.

## Studio source cockpit

The phase-6 administrative contract exposes source definitions, operational
state/checkpoints and immutable collected signals. A Studio request contains
only primitives; `adminImportContext` maps it through an ACL port to a named
`editorialIntelligenceContext` command. The controller neither sees the
`EditorialSource` aggregate nor queries its tables directly.

An operator may revise a source, pause it, or reactivate it. A source under an
active collection lease cannot be revised, preventing a definition from changing
while a provider response is being normalized. Pause preserves checkpoints and
signals; reactivation makes the source due immediately. Signal classification,
retention and topic grouping deliberately remain phase 7 responsibilities.

## Boundary contract

A retained candidate can start authoring with a primitive-only `ArticleBrief`:

```text
ArticleBrief
- subject
- editorialAngle
- locale
- proposedTags[]
- provenance[] { sourceName, sourceUrl, publishedAt, authorityLevel }
- topicCandidateId? 
```

`topicCandidateId` is opaque to `articleContext`. Provenance is copied as an
immutable snapshot so that a future source or signal modification cannot change
the explanation of an existing article. The implementation must use a versioned
integration event or a documented ACL port; direct SQL and cross-context domain
imports are forbidden.

The first implementation uses `ArticleBriefV1`, a primitive-only immutable
handoff object. It is constructed only after a candidate is `RETAINED`. Its
evidence is resolved inside `editorialIntelligenceContext` by joining that
context's own signal and source tables; missing evidence rejects the hand-off.
The admin ACL copies subject, angle, candidate identity and attributable source
references into the existing article-generation theme. `articleContext`
therefore receives an immutable, source-grounded request, never the candidate,
its repository, or an editorial domain object. Persisting provenance as a
separately queryable article field remains a future compatible contract change.

## Collection flow

One generic primary scheduler runs regularly, initially every fifteen minutes:

```text
ConsultDueEditorialSourcesJob
-> find enabled sources with nextCheckAt <= now
-> ConsultEditorialSource(sourceId)
```

The consultation command claims a short durable lease before external work. The
remote call happens outside a database transaction. A second idempotent
completion command persists new signals, advances the checkpoint and schedules
the next check in a short transaction. A provider failure is also persisted in
a separate short transaction, tied to the claiming worker; a stale worker cannot
fail or complete another worker's lease.

Each source owns its cadence. A source failure only changes that source's retry
state; it never blocks another source.

## Provider ACL

The initial access modes are RSS and YouTube channel feeds. RSS and YouTube
Atom adapters are implemented; each remains independently selectable by source
access mode. All adapters follow:

```text
provider XML/JSON
-> provider DTO with schema/version validation
-> normalized DiscoveredSourceItem
-> explicit mapper
-> SourceSignal domain factory
```

Provider DTOs, XML parsers, HTTP clients and raw payloads remain secondary
adapter details. Unsupported or malformed payloads are observable technical
failures and never become domain content.

The RSS adapter performs conditional HTTP requests with the source checkpoint's
`ETag` and `Last-Modified` values. A `304 Not Modified` is a successful empty
discovery, not a failure. Its XML parser rejects DTDs and external entities
before the explicit RSS-to-`DiscoveredSourceItem` mapping. Fingerprints are
SHA-256 values over the normalized provider fields; Java object hashes are not
durable fingerprints.

## Idempotence and recovery

The primary persistence invariant is `UNIQUE(source_id, external_id)`. A
content fingerprint records meaningful provider updates separately from duplicate
delivery. Checkpoints advance only after the signal write commits.

Lease expiry allows a later worker to recover abandoned work. Completion checks
source id, lease owner, expected state and version, so a stale worker cannot
advance a newer consultation. Integration consumers use the existing inbox
pattern. Outbox events are written in the same transaction as domain state.

## Editorial analysis

Analysis is independent from collection and runs in batches, initially once a
day. It classifies new signals, groups nearby themes, compares them with
existing editorial references and creates or enriches `TopicCandidate`.

Staging explicitly enables both schedulers. The first run happens on process
startup, then collection repeats every fifteen minutes and analysis every
twenty-four hours. This makes a deployment a deterministic first-run boundary
without turning editorial analysis into a high-frequency cost source.

Studio also exposes an explicit **Analyze pending signals** operator action.
It is intended for editorial review and staging verification: Studio calls its
admin application use case, crosses the primitive ACL, and dispatches
`AnalyzeEditorialSignalsCommand` through the CommandBus. It invokes exactly
the same batch use case as the scheduler; it never creates candidates in the
controller or browser. Studio lists raw signals per source (`NEW` or
`ANALYZED`) so collection can be verified before candidates exist.

`TopicCandidate` is a durable editorial decision object, not an article draft.
It stores a suggested subject, an angle and immutable references to its source
signals. Only an explicit human retention may later create an `ArticleBrief`.

LLM calls belong only to this analysis boundary. Each call records a durable
`GenerationExecution` with operation, model, input/output tokens, estimated
cost, duration, result and safe failure category. The first release does not
auto-generate an article: an operator retains, defers or ignores a candidate in
Studio.

Until the OpenAI analysis adapter is enabled, the MVP uses a deterministic
adapter that proposes one attributable topic per new signal at zero external
cost. This keeps the scheduled flow observable and reversible without claiming
semantic clustering that has not yet been paid for or reviewed.

Authority levels express intended use, not truth by themselves:

- `AUTHORITATIVE`: evidence for factual assertions;
- `SPECIALIZED_MEDIA`: confirmation and context;
- `FIELD_SIGNAL`: discovery only, requiring corroboration for claims.

## Delivery sequence

1. Boundary documentation and architecture guard.
2. Sources, consultation state, checkpoints, leases and persistence.
3. Generic due-source scheduler and retry/recovery.
4. RSS adapter and complete vertical path.
5. YouTube feed adapter and complete vertical path.
6. Studio source and signal read/write workflow.
7. Batched analysis, cost accounting and `TopicCandidate`.
8. `ArticleBrief` refactor and retained-candidate hand-off to `articleContext`.
9. Editorial calendar, scheduled publication/depublication and rotation.
10. DLQ, metrics, alerts and operational hardening.

## Operational hardening

Editorial collection and planning currently run as durable database-backed
jobs, not as SQS consumers. They therefore recover through optimistic versions,
leases, persisted retry dates and canonical command status. Creating an
editorial DLQ without an editorial queue would give a false recovery guarantee.
All integration-event queues continue to use the platform rule: one source
queue, one DLQ, inbox idempotence, no delete before successful handling, and a
bounded operator-controlled redrive.

`editorialOperationsHealth` observes only tables owned by
`editorialIntelligenceContext`. It reports degraded sources, expired source and
planning leases, overdue scheduled operations, stale dispatched commands,
rejected operations and failed analyses during the last 24 hours. These are
Micrometer gauges and Actuator health details; they never drive a domain
transition.

In staging, a five-minute systemd probe exports only the aggregate
`EditorialOperationsDegraded` value to the `Fragments/Staging` CloudWatch
namespace. CloudWatch alerts through the existing operations SNS topic. The EC2
role can publish only to that namespace. Missing probe data is considered an
alarm, because absence of monitoring is not healthy.

Dispatch failure handling distinguishes two situations using the canonical
command status:

```text
adapter throws + command PENDING
-> retain CLAIMED state
-> lease expires
-> safe retry with the same scheduleId/commandId

adapter throws + command APPLIED or REJECTED
-> mark DISPATCHED
-> normal reconciliation records the terminal result
```

The schedule id remains the idempotency key. A scheduler exception is isolated
to reconciliation or dispatch and is logged with the worker identity; one half
of the tick does not suppress the other.

## Editorial planning

Planning stores a durable `EditorialPublicationSchedule` intent with `PUBLISH`
or `ARCHIVE`, a due date, optimistic version and a short execution lease. Its
states are:

```text
SCHEDULED -> CLAIMED -> DISPATCHED -> COMPLETED
                                `-> REJECTED
SCHEDULED -> CANCELLED
CLAIMED -- lease expiry --> CLAIMED by another worker
```

The scheduler runs every minute when
`fragments.editorial.planning.schedule.enabled=true`. It first reconciles
dispatched work, then claims at most twenty due operations for five minutes.
Remote or cross-context work never happens in the claim transaction.

`scheduleId` is also the article command id. A crash after dispatch and before
the `DISPATCHED` transition can therefore resend the same command without
duplicating the article transition. A schedule becomes `COMPLETED` only after
the canonical command-status store reports `APPLIED`; a business rejection is
persisted as `REJECTED` with its safe reason. Merely dispatching a command is
never reported as publication success.

The current transport is an explicit primitive ACL in `adminImportContext`:

```text
editorial schedule
-> ScheduledArticleOperationPort (primitives)
-> admin ACL adapter
-> articleContext PublishArticleRevisionCommand / ArchiveArticleCommand
-> article aggregate and publication policy
-> command status
-> schedule reconciliation
```

This is documented modular-monolith debt, isolated behind a replaceable port.
The target when editorial planning is split operationally is a versioned
integration command/event route with its own SQS destination and inbox. No
editorial class imports an article class today.

At execution time, `articleContext` remains the sole owner of approval,
revision and capacity rules. Its hard limit of 30 published articles is the
single invariant. Rotation is intentionally explicit: Studio schedules an
`ARCHIVE` before or alongside a publication. Phase 9 never guesses which
article should disappear and never physically deletes content.

The Studio calendar is a GET-backed monthly read model. Redux listeners own
side effects; the HTTP adapter performs strict transport validation. Operators
can schedule publication/archive and cancel only `SCHEDULED` work. Claimed,
dispatched and terminal operations remain visible for audit.

## Required tests

Every slice must include domain/unit, repository integration and full vertical
HTTP/SQS coverage where its flow crosses these boundaries. Collection coverage
includes duplicate items, duplicate completion, expired lease recovery,
malformed provider payload, checkpoint atomicity and provider timeout. Boundary
architecture tests must reject cross-context imports and direct cross-context
SQL.
