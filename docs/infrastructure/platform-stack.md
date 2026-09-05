# Shared platform staging stack

`infra/aws/cloudformation/platform-staging.yaml` describes the deployed
mutualised Anchor and Fragments staging host. The observed stack is
`platform-staging`, currently `UPDATE_COMPLETE`.

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

Every update must use a reviewed change set. The legacy stacks remain partial
resource owners and rollback references; they are not the active runtime.

The data volume has `DeletionPolicy: Snapshot` and
`UpdateReplacePolicy: Snapshot`, but snapshots are not a substitute for a
tested PostgreSQL backup and restore procedure.

## Completed runtime work

- Docker and isolated Compose runtimes are active;
- the shared data volume is mounted;
- platform Caddy owns ingress;
- Fragments secrets come from SSM;
- Fragments deployment targets the platform through SSM Run Command and OIDC.

## Remaining work

- deploy and confirm queue/DLQ alarms and operator SNS subscription;
- execute and record the PostgreSQL restore drill;
- add deployment locking;
- verify Anchor OIDC/IAM ownership in the Anchor repository;
- separate active application resources from legacy compute stacks;
- observe memory, disk and CPU before changing `t4g.medium`.

No AWS mutation is authorized by this document.
