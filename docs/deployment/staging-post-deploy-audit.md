# Fragments Staging Post-Deploy Audit

This document records the issues found during the first Fragments staging deployment and the platform changes that make the next deployment repeatable.

## Scope

- Spring Boot backend staging deployment.
- Single EC2 host with Docker Compose.
- ECR image registry.
- SQS queues and shared DLQ.
- Caddy-managed HTTPS.
- GitHub Actions deployment through AWS OIDC and temporary SSH.

No business architecture change is part of this audit.

## Incidents

### Invalid CloudFormation Resource Type

Symptom:

```text
Template format error: Unrecognized resource types: [AWS::EC2::InstanceProfile]
```

Real cause:

The CloudFormation resource type was incorrect. Instance profiles are IAM resources, not EC2 resources.

Correction applied:

Use `AWS::IAM::InstanceProfile`.

Prevention:

Run `aws cloudformation validate-template` before stack creation or update.

### Failed EC2 Bootstrap

Symptom:

The EC2 instance was reachable, but Docker, Docker Compose and AWS CLI were missing.

Real cause:

Ubuntu 24.04 ARM64 did not provide the `awscli` package through apt. Because UserData used `set -e`, the script stopped before Docker installation and data volume setup.

Correction applied:

- Install only available apt prerequisites.
- Install AWS CLI v2 from the official ARM64 archive.
- Install Docker CE and Docker Compose from Docker's official Ubuntu repository.
- Make the data volume setup idempotent.

Prevention:

UserData must be safe to rerun and must not depend on optional distro packages for critical bootstrap steps.

### Unsafe Data Volume Formatting

Symptom:

The initial bootstrap used `mkfs ... || true`, which hid failures and did not check whether a filesystem already existed.

Real cause:

The data volume setup was optimistic and not explicitly idempotent.

Correction applied:

- Wait for `/dev/nvme1n1`.
- Run `blkid` before formatting.
- Format only when no filesystem exists.
- Mount by UUID through `/etc/fstab`.

Prevention:

Never format a block device without checking for an existing filesystem first.

### Docker Image Invalid Reference

Symptom:

```text
invalid reference format
```

Real cause:

The GitHub secret `STAGING_ECR_REPOSITORY` contained an invalid value, likely whitespace, newline or a tag.

Correction applied:

The workflow now validates `STAGING_ECR_REPOSITORY` before Docker build.

Prevention:

Store only the bare ECR repository URI in GitHub:

```text
851725375299.dkr.ecr.eu-west-3.amazonaws.com/fragments/staging/backend
```

### GitHub Runner Could Not SSH

Symptom:

The workflow failed at `ssh-keyscan`.

Real cause:

The Security Group allowed SSH only from a personal IP. GitHub-hosted runners use different public IPs.

Correction applied:

The workflow now:

1. detects the runner public IP;
2. authorizes SSH `22/tcp` only from that `/32`;
3. runs the SSH deployment;
4. revokes the temporary rule in an `always()` cleanup step.

Prevention:

Do not leave SSH open to `0.0.0.0/0`. Long term, replace SSH deployment with AWS SSM Session Manager or another inbound-free deployment channel.

### Manual GitHub OIDC Role

Symptom:

The stack created runtime IAM resources, but `AWS_DEPLOY_ROLE_ARN` had to be created manually.

Real cause:

The first CloudFormation template did not own the GitHub Actions deploy role.

Correction applied:

CloudFormation now creates:

- GitHub OIDC provider when absent;
- GitHub deploy role;
- least-privilege ECR push policy;
- temporary SSH Security Group permissions;
- `GitHubDeployRoleArn` output.

Prevention:

Deployment credentials must be stack outputs, not tribal setup.

## Durable Target

The platform target after this audit:

- A fresh AWS account can create the stack from one CloudFormation template.
- UserData installs Docker, Compose and AWS CLI v2 without manual repair.
- GitHub Actions resolves deployment values from CloudFormation outputs; the
  stable, non-secret OIDC role ARN is versioned in the workflow.
- Runtime secrets stay only in `/srv/fragments/staging/.env`.
- CI/CD uses SSM Run Command and requires no inbound SSH.
- Kafka and Redis are absent from staging.

## Remaining Improvement

The deployment transport is now:

- no inbound SSH from GitHub;
- AWS Systems Manager Run Command;
- deploy role scoped to the platform instance and `AWS-RunShellScript` document.
