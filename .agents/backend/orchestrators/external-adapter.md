# Orchestrator - Backend External Adapter

Use this when integrating a technical system outside the domain model.

Examples:
- S3
- SQS
- Google OAuth
- OCR/ticket verification binary
- filesystem
- CloudWatch/logging

## Responsibilities

- keep infrastructure details outside domain/application logic
- expose a small port
- make failures explicit
- test with fakes first and real adapter tests where useful

## Steps

1. Define the application/domain port.
2. Write use case test with a fake adapter.
3. Implement secondary adapter.
4. Map external DTO/errors to internal result types.
5. Add adapter integration test if the contract is risky.
6. Add config via environment variables, never hardcoded secrets.

## Pitfalls

- leaking SDK types into command handlers
- placing retries in domain logic
- hardcoding local paths/secrets
- making external availability part of a domain invariant

## Validation

- fake-first tests cover business behavior
- adapter tests cover external mapping
- config is environment-driven
- failures are observable and do not corrupt state

