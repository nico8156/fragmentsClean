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

The read-only inventory is recorded in
`docs/audits/aws-legacy-stacks-audit-2026-09.md`. The two stopped hosts retain
120 GiB of gp3 storage and two billable public IPv4 addresses, for an estimated
baseline of USD 18.44/month before snapshots, ECR, SQS and tax. Neither stack
can be deleted wholesale because it still owns active application resources.

## Anchor legacy stack

- The `t4g.medium` instance is stopped.
- Its two attached gp3 volumes total 60 GiB.
- Its Elastic IP remains allocated.
- The stack still owns active Anchor ECR and SQS resources, so deleting the
  whole stack is unsafe.

## Fragments resource-owner / legacy stack

- The `t4g.small` instance is stopped.
- Its two attached gp3 volumes total 60 GiB.
- Its Elastic IP remains allocated.
- The stack still owns the active Fragments ECR repository, SQS queues and
  historical DLQ; it cannot be deleted as a cleanup shortcut.

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
- Anchor deployment credentials and current IAM ownership must be verified in
  the Anchor repository before its legacy role is removed.
- No Fragments RDS instance was observed; PostgreSQL currently runs inside
  Docker on the shared platform. A tested logical backup and restore procedure
  remains mandatory before long-lived data is accepted.

## Active platform constraints

1. Keep a single platform-owned Caddy process on ports 80 and 443.
2. Keep Anchor and Fragments PostgreSQL data directories and credentials
   separate.
3. Do not expose either backend directly on the public interface.
4. Do not merge the two application IAM policies into wildcard permissions.
5. Keep legacy volumes recoverable until logical restore and rollback drills
   have passed.

## Ownership split

The platform stack owns the shared EC2 host, networking, SSM, host-level
storage and the single Caddy instance. Application stacks own ECR, SQS,
application parameters and application-specific IAM permissions. The legacy
minimal stacks have not yet completed that separation and must be refactored
through resource import/change sets before compute cleanup.
