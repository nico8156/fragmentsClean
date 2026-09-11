# experienceContext

`experienceContext` owns a member's textual account of a coffee visit. An
experience does not require a ticket. Tickets remain evidence signals owned by
`ticketContext`; the Pass interprets both sources independently.

## Write model

The `Experience` aggregate owns author and coffee identity, draft/publication/
deletion state, moderation visibility and optimistic versioning. The
`ExperienceReport` aggregate owns a reporter's moderation request. Controllers
derive the requester from the JWT and dispatch authenticated commands; handlers
load aggregates through ports and publish domain events through the transactional
outbox.

Publication requires non-empty policy-compliant text in lot 05. Media eligibility
will extend that invariant in lot 06. Author deletion and moderation visibility
are separate transitions: restoration cannot republish content deleted by its
author.

## Integration boundaries

- Coffee availability comes from `experience_coffee_references`, maintained by
  stable coffee integration events. The one-time migration backfill is not a
  runtime cross-context query.
- Public author data comes from `experience_user_profiles`, maintained by
  `app.user.created` and `app.user.profile_updated`.
- Personal blocks come from `social.user_block.changed` and affect reads only;
  they do not alter the author's aggregate or Pass.
- `experience.lifecycle.changed` is routed to `app-users-events`; the Pass owns
  the decision whether a visible published experience contributes.
- Account deletion is consumed through the Experience inbox. The context erases
  its own write/read/reference data and emits its own acknowledgement.

All contracts are primitive and transport-neutral. Production delivery is
outbox → SQS destination → inbox → typed handler. Projection Sync publishes only
`projection.updated`; mobile then retrieves the canonical read model.

## Read model

JDBC projections provide:

- `GET /api/coffees/{coffeeId}/experiences`
- `GET /api/users/me/experiences`
- `GET /api/admin/experience-moderation/reports`

Queries cross `ExperienceReadRepository`; application handlers do not depend on
JDBC. Keyset pagination is ordered by immutable `created_at` and `experience_id`.
Projection updates are duplicate-safe and version-guarded, including moderation
decisions delivered out of order.

## Verification expectations

Keep pure aggregate/use-case tests, MockMvc/PostgreSQL full vertical tests,
outbox/envelope/routing/inbox/projection tests, account-deletion coverage and the
bounded-context architecture guards. Mobile writes must remain offline-first and
roll back only after an explicit business rejection.
