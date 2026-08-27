# Article Authoring and Generation Saga

Status: accepted implementation baseline for the article authoring program.

This document defines the target architecture before implementation. It is
subordinate to the root `AGENTS.md`; implementation must satisfy both.

## Decision summary

Fragments will model editorial content as a rich `Article` aggregate with
immutable revisions. Long-running assisted authoring will be coordinated by a
durable application-layer process manager. The process manager is called a
saga in product and delivery discussions, but it is not a domain aggregate and
does not replace the command bus, transactional outbox, SQS, inbox, command
status, projections, or Projection Sync SSE.

The existing mobile article response remains stable during the migration.
Structured write-side content is projected into the current ordered block read
contract until clients deliberately adopt another contract.

## Context ownership

`articleContext` owns:

- article identity and lifecycle;
- revisions and editorial invariants;
- publication policy;
- authoring saga state and transitions;
- generation run history;
- article events and projections.

`adminImportContext` is an admin anti-corruption boundary. It may authenticate
an operator, validate an admin request, and dispatch an article command. It
must not own a second durable article draft or mutate article tables.

Fragments Studio owns editing ergonomics and transient form state. The backend
remains the source of truth for drafts, saga progress, validation, publication,
and read models.

## Rich domain model

The target write model is:

```text
Article
|- ArticleId
|- ArticleLifecycle
|- workingRevisionId
|- publishedRevisionId
|- version
`- ArticleRevision[]
   |- ArticleRevisionId
   |- ArticleContent
   |  |- ArticleTitle
   |  |- ArticleIntroduction
   |  |- ArticleSection[]
   |  |  |- ArticleSectionTitle
   |  |  |- ArticleParagraph[]
   |  |  `- ArticleImageRef[]
   |  `- ArticleConclusion
   |- ArticleCover
   |- ArticleTag[]
   |- ArticleAuthor
   `- revision metadata
```

This is a behavioral model, not a mutable DTO graph.

Rules:

- no public setters in domain packages;
- no Lombok `@Data` on aggregates, entities, value objects, policies, or saga
  state;
- constructors are private or restricted when an invariant must be protected;
- named factories create valid objects or return an explicit rejection;
- methods express intent, for example `replaceCover`, `appendParagraph`,
  `submitForReview`, and `publishRevision`;
- value objects protect their local invariants;
- entities protect invariants involving their own identity and children;
- the aggregate protects consistency across revisions and lifecycle state;
- domain policies own configurable decisions that do not naturally belong to
  one entity;
- collections exposed by the domain are immutable views or defensive copies;
- accessors exist only for a business decision, persistence mapping, event
  mapping, or projection mapping.

Tests must exercise behavior and rejected transitions rather than getters.
Architecture tests should reject public domain setters and Lombok `@Data`.

## Article and revision lifecycle

The article lifecycle is:

```text
DRAFT -> IN_REVIEW -> PUBLISHED -> ARCHIVED
```

A published revision is immutable. Editing a published article creates a new
working revision while the published revision remains visible to mobile. A new
revision replaces the visible one only after an explicit successful publish
command.

The aggregate is the only component allowed to select the working and
published revision. Persistence adapters reconstruct state; they do not make
lifecycle decisions.

## Editorial validation

The first format is `DISCOVERY_GUIDE`, compatible with the existing mobile
rendering: cover, metadata, title, introduction, ordered sections containing
headings, paragraphs and images, conclusion, and tags.

Validation has three levels:

1. External schema validation checks that a provider response is structurally
   readable and version-compatible.
2. Value objects reject invalid elementary values such as blank titles,
   invalid lengths, missing image alternatives, and invalid positions.
3. `ArticleRevision` and an editorial policy validate the complete format:
   section count, paragraphs per section, word budget, media requirements,
   tags, and conclusion.

Provider output never bypasses domain creation methods.

## Durable authoring saga

`ArticleAuthoringSaga` is application coordination state with its own
repository port. It coordinates durable work but does not contain article
content or duplicate article invariants.

Initial states:

```text
REQUESTED
-> GENERATION_PENDING
-> GENERATING
-> VALIDATING
-> READY_FOR_REVIEW
-> NOTIFICATION_PENDING
-> NOTIFIED
-> EDITING
-> PUBLICATION_REQUESTED
-> PUBLISHED
```

Exceptional terminal states are `REJECTED`, `FAILED`, `EXPIRED`, and
`CANCELLED`. Every transition is explicit and tested. Arbitrary state setters
are forbidden.

### Phase 9 implementation boundary

The first orchestration slice is wired through the existing command bus and
`articles-events` SQS destination:

```text
RequestArticleGeneration
-> saga GENERATION_PENDING + command status + outbox
-> inbox idempotence
-> short lease transaction
-> OpenAI call outside the transaction
-> run SUCCEEDED + saga VALIDATING + completion outbox event
```

Each attempt is recorded in `article_generation_runs` with its worker lease,
provider response metadata and schema version. The generated domain draft is
validated by the provider boundary, but is deliberately not copied into this
technical attempt table. Materialising it into an article revision is the next
phase; this slice therefore cannot yet send the review email or expose a
ready-to-review article in Studio.

The persisted saga records only coordination information:

- saga, article and revision identifiers;
- current state and optimistic version;
- manual or scheduled trigger;
- normalized theme and deduplication fingerprint;
- generation run and notification references;
- attempt counters, lease information and timestamps;
- normalized failure category, never provider secrets or raw credentials.

## Command status versus process status

`GET /commands/{commandId}` remains the canonical command status endpoint.
It answers whether one submitted intent was accepted and applied or explicitly
rejected.

For example, `RequestArticleGeneration` becomes `APPLIED` when the article
shell, working revision identity, saga request, command status and outbox event
are durably committed. It does not remain `PENDING` for the whole generation.

Saga progress is exposed through a backend-owned query/read model. Projection
Sync SSE may announce that this read model changed, but the Studio must issue a
GET for the authoritative snapshot. SSE is never a command ACK and never
contains domain events.

## Transaction boundaries

### Command transaction

One short transaction persists:

```text
aggregate transition
+ saga transition when applicable
+ command status
+ outbox event
```

Failure of any element rolls back all elements. Command handlers publish
through the existing transactional domain-event/outbox mechanism; they do not
send SQS messages directly.

### External work

OpenAI, email, S3, and other remote calls must not execute while a database
transaction is held open.

Long work uses a durable work/lease protocol:

1. a short transaction claims eligible work using an owner token and expiry;
2. the external port is invoked outside the transaction;
3. a completion command starts a new transaction;
4. that command verifies saga identity, expected state, run identity and
   version;
5. domain state, saga state, command outcome and outbox events commit together;
6. a timeout leaves reclaimable work rather than an ambiguous in-memory state.

External exactly-once delivery cannot be assumed. Business effects must be
idempotent, and provider idempotency keys are used when supported.

### Concurrency

- JPA optimistic versions protect `Article` and saga coordination state.
- Publish commands carry the expected revision identity.
- Command ids suppress duplicate command effects.
- Inbox identity is consumer name plus stable event/message id.
- Lease expiry permits recovery after process death.
- Publication capacity is checked against the write model under a transactional
  lock; projections are never used for this decision.

## Serialization boundaries

The serialization path for generated content is:

```text
provider JSON
-> versioned adapter DTO
-> strict structural validation
-> anti-corruption mapper
-> domain named factories
-> rich value objects
-> aggregate validation
```

Rules:

- Jackson and provider DTOs remain in secondary adapters;
- provider/SDK types never enter application commands or domain methods;
- unknown or incompatible schema versions are rejected explicitly;
- malformed data is retained only as safe diagnostic metadata when required,
  never as article content and never with secrets;
- integration events use explicit, versioned, primitive-only public contracts;
- consumers deserialize integration contracts, never producer domain events;
- round-trip and compatibility tests cover provider DTOs, event envelopes and
  read projections.

The write model will use relationally structured revisions, sections,
paragraphs, media references and tags. The existing `blocks_json` shape remains
a read-side projection detail during compatibility migration.

## Integration events and messaging

The initial catalog is:

- `article.generation.requested.v1`
- `article.draft.generated.v1`
- `article.draft.validation-failed.v1`
- `article.review.ready.v1`
- `article.review.notification-requested.v1`
- `article.revision.published.v1`
- `article.archived.v1`

Names and exact payloads are finalized with the implementing slice. Every
public payload contains primitives and stable identifiers. Incompatible changes
create a new event version.

Every asynchronous consumer follows:

```text
SQS envelope
-> public contract deserialization
-> inbox duplicate check
-> typed BC-local handler
-> transition and/or projection
-> optional outbox event
-> commit
-> SQS delete
```

Retryable technical failures throw and leave the SQS message available.
Business rejections are recorded explicitly and are not disguised as platform
failures. Delete-failure redelivery must remain harmless.

## OpenAI generation boundary

The provider port receives a provider-neutral generation request and returns a
provider-neutral raw generation result or typed technical failure. Prompting,
model selection, credentials, retries, JSON schema and provider error mapping
belong to the adapter.

`ArticleGenerationRun` records attempts, safe provider metadata, schema version,
timings and outcome. A successful attempt is stored separately as a normalized,
versioned generation artifact before the saga reaches `READY_FOR_REVIEW`.
Materialising that artifact into the relational article revision aggregate is a
separate persistence step; generated content never publishes itself.

### Phase 11 revision materialisation

The validated artifact is materialised transactionally into the relational
revision structure: `article_revisions`, ordered sections, paragraphs and
editorial tags. The adapter verifies that the owning article already exists,
assigns the next revision number, computes the read-time reading estimate and
is idempotent for an existing revision. A missing article is an explicit
technical failure; the adapter does not create an article shell or bypass the
article aggregate lifecycle.

### Phase 12 Studio generation entry point

The authenticated Studio entry point accepts only the editorial subject and
locale. The admin ACL creates command, saga, article and revision identities,
then dispatches one `RequestArticleGeneration` command. Its transaction creates
an `ArticleAggregate` shell in `DRAFT`, persists the saga in
`GENERATION_PENDING`, marks the command accepted and appends the generation
event to the outbox. The generated revision later replaces the compatibility
placeholder and becomes the article working revision; it is never published by
generation.

Manual Studio generation and scheduled generation dispatch the same
`RequestArticleGeneration` use case. A scheduler is only a primary adapter.

## Media ownership

Durable article media references point to Fragments-owned storage. Each image
records its S3 key/reference, role, alternative text, dimensions, source and
licensing/attribution metadata when applicable.

Temporary provider, Google, browser object, or presigned upload URLs never
become durable domain content. Storage adapters translate domain references to
delivery URLs on the read side.

## Review notification and publication approval

Notification is initiated by an outbox event and processed asynchronously.
Delivery attempts use a stable idempotency identity. The email may contain a
visual summary built from the article read model, but email rendering is not a
domain responsibility.

An email link must not publish through HTTP GET. Approval is signed, expiring,
single-use, and bound to the exact article revision. It opens an authenticated
Studio confirmation route, which submits a publish command. Editing the
revision invalidates the approval.

## Publication capacity

`ArticlePublicationPolicy` controls the maximum number of simultaneously
published articles. The initial configurable proposal is a warning at 24 and a
hard limit at 30. The backend enforces the hard limit transactionally. Studio
may display the policy but must not duplicate it as the source of truth.

No generated article is automatically published. Scheduled generation also
obeys generation budgets, pending-review capacity, topic deduplication and a
single-run lease.

## Persistence target

The target write schema separates:

- articles;
- article revisions;
- sections;
- paragraphs;
- media references;
- tags;
- authoring sagas;
- generation runs;
- publication policy/configuration.

Exact table names belong to the persistence task. JPA entities are adapter
models and may expose framework-required constructors/accessors without
weakening domain encapsulation.

## Migration strategy

Current implementation debt includes immediate publication, write-side
`blocksJson`, and a durable Studio document in `adminImportContext`.

Migration is incremental:

1. characterize the existing mobile and Studio contracts;
2. introduce the rich revision model and new persistence alongside compatible
   reads;
3. route manual authoring commands to `articleContext`;
4. project structured revisions to the current mobile block response;
5. migrate useful existing data with explicit mapping;
6. remove the parallel Studio document only after all commands and reads use
   the owning context;
7. remove legacy fields/routes in a separate reviewed cleanup.

No implementation task may silently combine migration cleanup with a behavior
change.

## Required test matrix

### Domain

- every named factory accepts boundaries and rejects invalid values;
- revision and article lifecycle transitions;
- published revision immutability;
- revision replacement and stale revision rejection;
- editorial and publication policies;
- saga allowed and forbidden transitions.

### Application

- command handler success, duplicate, rejection and technical failure with fake
  ports;
- command status semantics independent from saga progress;
- lease claim, expiry, recovery and stale completion;
- provider success, malformed output and provider failure;
- notification and approval idempotence.

### Persistence and transactions

- JPA round-trip of aggregate and saga state;
- aggregate/saga/command status/outbox atomic commit;
- rollback produces neither partial state nor outbox event;
- optimistic concurrency conflict;
- publication-capacity lock under concurrent requests.

### Messaging and projections

- stable envelope serialization compatibility;
- first delivery, duplicate delivery and replay;
- SQS delete failure followed by safe redelivery;
- retryable failure remains on the queue and reaches DLQ policy;
- projection update precedes `projection.updated` publication;
- current mobile response remains compatible.

### Architecture

- no public setters or Lombok `@Data` in domain packages;
- no controller repository access;
- no provider SDK/Jackson DTO in domain/application contracts;
- no `adminImportContext` persistence of article business state;
- no cross-BC aggregate/entity/repository imports;
- no external call inside the command transaction.

## Delivery sequence

Implementation follows the approved numbered plan. Each task uses one branch
and one focused commit, including its tests and necessary documentation. Merge
and deployment happen only after explicit review. This ADR must be updated in a
dedicated documentation task if an implementation decision changes it.
