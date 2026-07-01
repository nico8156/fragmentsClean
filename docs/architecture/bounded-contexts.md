# Bounded Contexts

## Principle

Each bounded context owns its concepts, decisions, write model, and events.

Other contexts do not read the owning context's business tables. They build local projections from events.

## Active Contexts

### authenticationContext

Owns authentication facts:
- Google exchange
- JWT lifecycle
- refresh tokens/session semantics

Does not own product profile behavior.

### userApplicationContext

Owns application user profile and product user identity.

Legitimate dependencies:
- authentication facts through stable events or primitive identity.

Forbidden:
- owning OAuth/token lifecycle.

### coffeeContext

Owns coffee place catalog and discovery read models:
- coffee summaries
- details
- photos
- opening hours
- map/search read models

Most coffee data is read-heavy. JDBC read models are expected.

### articleContext

Owns editorial content:
- articles
- slugs
- publication state
- article read models

### socialContext

Owns social interactions:
- likes
- comments
- social counts
- social projections

It must not own coffee details or user profile truth. It may keep local target/user references when required.

### ticketContext

Owns ticket verification:
- ticket submission
- OCR/analysis lifecycle
- verification result
- ticket read models

Ticket verification may emit facts that other contexts consume for entitlements or profile progress.

### sharedKernel

Technical only:
- command abstractions
- event envelope abstractions
- outbox/inbox technical support
- date/time provider
- transaction helpers

No business use cases or BC routing logic should live here.

## Target Dependency Model

```text
producer BC domain event
-> stable integration envelope
-> SQS destination
-> consumer BC inbox
-> consumer projection/handler
-> local read/reference model
```

## Temporary Exceptions

Any cross-BC SQL must be documented near the adapter as debt and should expose primitive-only contracts.

New features should not add cross-BC SQL as their default design.

