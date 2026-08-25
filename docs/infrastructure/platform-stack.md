# Shared platform staging stack

`infra/aws/cloudformation/platform-staging.yaml` is the proposed CloudFormation
stack for the future mutualised Anchor and Fragments staging host.

## Scope

The stack owns platform resources only:

- one ARM64 EC2 instance;
- encrypted root and data EBS volumes;
- one security group exposing only 80/443 plus the explicitly supplied SSH
  operator CIDR;
- IMDSv2 enforcement;
- SSM managed instance access;
- one runtime IAM role;
- one Elastic IP.

It does not create, delete, or replace the existing Anchor/Fragments ECR,
SQS, S3, or PostgreSQL application resources.

## Runtime permission boundary

The role receives:

- pull access to the supplied Anchor and Fragments ECR repositories;
- receive/delete/change-visibility/send access to the supplied Anchor and
  Fragments SQS queues;
- object operations only below:
  - `anchor/staging/*`;
  - `fragments/staging/*`.

The stack deliberately does not grant wildcard ECR repository access, wildcard
SQS access, bucket listing, or database administration permissions.

## Deployment safety

This template must first pass:

```bash
aws cloudformation validate-template \
  --region eu-west-3 \
  --template-body file://infra/aws/cloudformation/platform-staging.yaml
```

It must then be deployed in a change set, never directly over an existing
application stack. Existing Anchor and Fragments stacks remain the rollback
reference until application data and smoke tests have been validated on the
new host.

The data volume has `DeletionPolicy: Snapshot` and
`UpdateReplacePolicy: Snapshot`, but snapshots are not a substitute for a
tested PostgreSQL backup and restore procedure.

## Known follow-up work

- install and harden Docker through idempotent bootstrap or image baking;
- mount the data volume without formatting an existing disk;
- create the platform-owned Caddy runtime;
- move application secrets to SSM Parameter Store or Secrets Manager;
- pass the existing `alias/aws/ssm` key ARN for the initial staging migration;
- migrate Anchor deployment credentials to GitHub OIDC;
- add CloudWatch alarms and deployment locking;
- decide whether `t4g.medium` is sufficient after observing both applications
  under load.

No AWS deployment is authorized by this file or by the phase 2 commit.
