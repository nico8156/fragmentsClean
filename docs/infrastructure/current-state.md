# Infrastructure current state

This document is the non-secret baseline for the Fragments / Anchor staging
infrastructure. It is an inventory, not an authorization to change AWS.

Audit date: 2026-09-05.

## AWS resources observed

AWS region: `eu-west-3`.

| Scope | Stack | Observed state | Runtime model |
| --- | --- | --- | --- |
| Shared platform | `platform-staging` | `UPDATE_COMPLETE` | One mutualised ARM64 EC2 host, SSM-managed deployment, Docker Compose and isolated application runtimes |
| Anchor legacy rollback | `anchor-staging-minimal` | `UPDATE_COMPLETE` | Retained pending an explicit cleanup decision |
| Fragments resource owner / legacy rollback | `fragments-staging-minimal` | `UPDATE_COMPLETE` | Owns Fragments ECR and SQS resources; its former host remains a rollback/cleanup concern |

The shared platform is now the active staging host. Legacy resources must not
be removed until their volumes, costs and rollback value have been inventoried
and an explicit cleanup decision has been recorded.

## Anchor staging

- Instance type observed: `t4g.medium`.
- Docker services: PostgreSQL, Spring Boot backend, Next.js web, Caddy.
- Persistent PostgreSQL path: `/srv/anchor-postgres/postgres-data`.
- Caddy currently owns ports 80 and 443 on its host.
- The current security group still exposes port 8080 publicly; this must be
  removed before or during shared-host migration.
- SQS is provisioned in CloudFormation, while the staging runtime currently
  keeps the application SQS mode disabled in its documented environment.

## Fragments staging

- Instance type observed: `t4g.small`.
- Docker services: PostgreSQL, Spring Boot backend, Caddy.
- Persistent PostgreSQL path: `/srv/fragments-postgres/postgres-data`.
- Coffee photo working directory: `/srv/fragments/staging/coffee-photos`.
- Caddy currently owns ports 80 and 443 on its host.
- Runtime target: SQS enabled, local event bus disabled, S3 storage enabled for
  coffee and article images.
- The staging IAM role is scoped to the Fragments ECR repository, Fragments
  queues, and the `fragments/staging/*` prefix in the shared asset bucket.

## Shared AWS observations

- ECR repositories have scan-on-push enabled.
- The deployed SQS queues still use a shared staging DLQ and a five-attempt
  redrive policy. The next infrastructure change set introduces one DLQ per
  source queue and retains the shared DLQ only for historical triage.
- EC2 instance metadata requires IMDSv2.
- S3 public access block is enabled on the shared asset bucket.
- GitHub Actions has an OIDC deploy role for Fragments restricted to the
  `main` branch. Backend deployment targets the shared platform instance through
  SSM Run Command; it does not open port 22 or carry an SSH private key.
- Anchor still uses static AWS credentials in its deployment workflow and must
  be aligned with OIDC before the hosts are consolidated.
- No Fragments RDS instance was observed; PostgreSQL currently runs inside
  Docker on the shared platform. A tested logical backup and restore procedure
  remains mandatory before long-lived data is accepted.

## Constraints for the next phase

1. Do not run the two existing Caddy services on one host: only one process may
   bind ports 80 and 443.
2. Keep Anchor and Fragments PostgreSQL data directories and credentials
   separate.
3. Do not expose either backend directly on the public interface.
4. Do not merge the two application IAM policies into wildcard permissions.
5. Keep the existing stacks and volumes recoverable until the shared-host
   migration has passed smoke tests and a rollback rehearsal.

## Target ownership split

The future platform stack should own the shared EC2 host, networking, SSM,
host-level storage, and the single Caddy instance. Application stacks should
continue to own their ECR repositories, SQS queues, application parameters,
and application-specific IAM permissions.
