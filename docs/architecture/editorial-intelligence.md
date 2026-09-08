# Editorial Intelligence and Article Lifecycle

Status: RSS collection adapter implemented. No production editorial source is
enabled by this document.

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

LLM calls belong only to this analysis boundary. Each call records a durable
`GenerationExecution` with operation, model, input/output tokens, estimated
cost, duration, result and safe failure category. The first release does not
auto-generate an article: an operator retains, defers or ignores a candidate in
Studio.

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

## Required tests

Every slice must include domain/unit, repository integration and full vertical
HTTP/SQS coverage where its flow crosses these boundaries. Collection coverage
includes duplicate items, duplicate completion, expired lease recovery,
malformed provider payload, checkpoint atomicity and provider timeout. Boundary
architecture tests must reject cross-context imports and direct cross-context
SQL.
