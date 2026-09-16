# Article curation, Studio commands, and mobile freshness

Status: backend, Studio and mobile changes connected locally on 2026-09-15; not
deployed. This document is subordinate to `AGENTS.md`. Staging verification
is still required before calling the chantier release-ready.

## Product contract

Studio presents four editorial states:

| Studio label | Backend lifecycle / curation | Public mobile visibility |
| --- | --- | --- |
| Brouillon | `DRAFT` or `IN_REVIEW` | Hidden |
| Publié | `PUBLISHED`, `featuredRank = null` | Article catalogue and detail |
| À la une | `PUBLISHED`, `featuredRank = 1..5` | Catalogue and Home carousel |
| Supprimé | `ARCHIVED` | Hidden; retained editorial history |

"Supprimé" is a domain archive, never a physical deletion from Studio tables.
An article can be withdrawn from `PUBLISHED` to a new editable `DRAFT` revision;
the former published revision remains immutable. Its public projection becomes
hidden and its featured rank is cleared. Republishing requires normal review and
publication validation. `À la une` is a curation flag on a published article,
not a second lifecycle state. Ranks 1..5 are unique across currently published
articles; `null` removes an article from the featured set.

## Owned write and read paths

`adminImportContext` authenticates the operator and forwards primitive commands
through `ArticleAuthoringPort`. `articleContext` owns withdrawal, featured
curation, lifecycle and the aggregate. No Studio controller writes article tables.

```
Studio -> admin gateway -> admin use case -> article command handler
       -> ArticleAggregate -> article repository + command status + outbox
       -> articles-events SQS -> article inbox -> article projection
       -> projection.updated/articles SSE -> authoritative mobile GET
```

Backend routes introduced in this tranche:

- `POST /api/admin/studio/articles/{articleId}/withdraw`
- `PUT /api/admin/studio/articles/{articleId}/featured` with
  `{ "featuredRank": 1 }` through `5`, or `null` to unfeature.

The existing archive route remains `DELETE /api/admin/studio/articles/{articleId}`.
Its transport verb does not imply physical deletion. These routes require the
runtime admin token. Each successful command records an `APPLIED` command status;
an unchanged featured rank is a successful no-op, without a new domain event.
Technical failures must not be turned into `REJECTED` or clear mobile outbox
items. The separate command-status P0 work is not claimed as complete here.

Stable event types `article.withdrawn` and `article.featured_rank.changed` use
primitive identifiers and versioned envelopes. Article projections ignore old
versions and duplicate deliveries. A withdrawal or feature event arriving before
its publication projection raises a retryable error rather than being inbox-ACKed
without effect. SSE carries only a projection hint, never the article body.

Public `ArticleView` now includes nullable `featuredRank`. Mobile maps this
explicitly in its transport adapter; provider JSON must never be cast directly
to its domain `Article` model. Public list/detail queries continue to filter
`status = 'published'`.

## Migration and deployment order

`src/main/resources/db/release/2026-09-15-article-curation.sql` adds nullable
`featured_rank` to write and projection tables, a unique partial write-side
index, and a public read index. `schema.sql` also contains these additions for
fresh test databases. The separate, checksum-tracked
`article-curation-2026-09.psql` release manifest replays only after the Apple
login baseline, and the staging deployment script runs it after a fresh backup
and before starting the candidate backend. The release migration must run
before deploying the new backend binary. Do not rely on `schema.sql` to upgrade
staging. No existing
article is automatically promoted to featured. Rank choices are editorial.

Deploy order: database migration -> backend -> Studio -> mobile. Each step must
be checked against staging command status, article projection, SQS handler and
SSE behavior before the next. A backend-only deployment is not useful to testers
until Studio curation and mobile catalogue paths are connected.

## Delivery slices and remaining checks, in order

1. Studio controls are implemented locally: per-article archive pending state,
   withdrawal, featured rank and command receipt checks. Staging operator flows
   and independent parallel archives still need manual verification.
2. Mobile hero now follows featured ranks, with the newest published article as
   a one-item fallback before Studio curates the first feature. The existing
   visual and scroll band remain unchanged. "Tous les articles" opens the full
   published snapshot catalogue; draft/archive leakage still needs device QA.
3. Freshness already uses `projection.updated` SSE for authoritative article
   and coffee GETs. Foreground and reconnect now refresh public snapshots;
   Home pull-to-refresh shows progress and refreshes private sections when
   signed in. Device/network QA remains. SSE still does not mutate read stores.
4. Cross-app verification: Studio publish, unfeature, withdraw, archive and
   several parallel archive commands; mobile immediate and resumed visibility;
   offline, failed network and missed SSE cases.

The separate data conservation/restoration chantier stays paused. This article
work neither resolves nor narrows its privacy obligations.

### Previous defect and implemented rule for "À lire ensuite"

Current mobile code does **not** embed article titles or IDs. It downloads the
full published snapshot through cursor pages and orders it by newest publication.
`useArticlesHome` previously put the first five articles in the Home slider;
`buildHomeContent` then removed those five and showed the next three. This was a
hard-coded *position rule*, not hard-coded content. It produces an empty section
when there are at most five published articles and conflates "featured" with
"newest".

The local selection is deterministic and editorially understandable:

1. Home hero carousel: published articles with ranks 1..5, ordered by rank. If
   none has a rank, the newest published article stays as a one-item visual
   fallback so the established hero does not disappear during curation.
2. "À lire ensuite": three most recently published, non-featured articles,
   excluding any article already shown in the hero. If fewer than three exist,
   show only those; do not invent or duplicate content.
3. "Tous les articles": every published article, including featured ones, in
   newest-first order. The HTTP gateway traverses cursor pages into a complete
   snapshot; drafts and archived articles never appear in the public response.

No ranking algorithm or random rotation is needed for the first release. An
article's tags may style its card but do not select or reorder it. The existing
local icon is only a visual placeholder when a cover is missing, not editorial
content.

## Verification policy and current evidence

Behavior changes need pure domain tests, fake-port command tests, MockMvc route
tests, PostgreSQL/JDBC adapter tests and Studio/mobile vertical tests. Backend
targeted tests cover
withdrawal, rank invariants, no-op/idempotence, protected admin routes, public
serialization, outbox payload mapping, projection replay/order and aggregate
round-trip. Studio gateway/reducer tests and mobile transport, selection,
foreground and catalogue-route tests pass locally. A complete multi-repository
vertical run and staging deployment are still required. On 2026-09-15 the full
backend `./mvnw -q test` suite passed with Docker/LocalStack available; the full
Studio suite passed (152 tests, including the new vertical fake-gateway tests), and
the full mobile suite passed (341 tests). TypeScript checks passed in both front
repositories. Do not label this slice release-ready before the staging checks.

## Follow-up — 2026-09-16

Rank allocation now locks the article through its repository port and serializes
competing claims through a transaction-scoped PostgreSQL rank lock. The existing
unique index remains the final guard. Business rank conflicts retain an admin
REJECTED receipt outside the rolled-back write transaction. This is not a change
to requester-scoped mobile command processing. Studio disables known occupied
and locally pending positions and refreshes after conflicts.

Mobile boot, bounded image prewarming and refresh feedback were hardened without
changing editorial selection. See [delivery evidence and remaining device checks](../audits/home-startup-editorial-hardening-2026-09-16.md).
