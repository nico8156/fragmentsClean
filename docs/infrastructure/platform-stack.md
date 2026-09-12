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
  - `fragments/staging/coffees/*`;
  - `fragments/staging/articles/*`;
  - `fragments/staging/private-media/*`;
  - `fragments/staging/backups/postgres/*`.

The stack deliberately does not grant wildcard ECR repository access, wildcard
SQS access, unrestricted bucket listing, or database administration permissions.
The candidate permits listing only the Fragments coffee prefix and metric
publication only in `Fragments/Staging`.

## Deployment safety

This template must first pass:

```bash
aws cloudformation validate-template \
  --region eu-west-3 \
  --template-body file://infra/aws/cloudformation/platform-staging.yaml
```

`InstanceImageId` is a required explicit AMI ID with no default. For updates,
use the image currently attached to the instance unless a separate OS migration
is approved. `UbuntuAmiParameter` remains as an unused compatibility parameter;
the moving SSM reference is no longer used by the candidate template.
The [corrected release previews](../deployment/aws-preserved-preview-2026-09-12.md)
must show no EC2/EBS/EIP changes before this messaging release is applied.

Every update must use a reviewed change set. The legacy stacks remain partial
resource owners and rollback references; they are not the active runtime.

The platform data disk is an instance block-device mapping with
`DeleteOnTermination: false`, not a standalone volume with a snapshot policy.
The separate legacy stack owns a standalone volume; neither template declares
the snapshot policies previously claimed here. Disk retention does not
substitute for a tested PostgreSQL backup/restore.

## Completed runtime work

- [Release messaging/alarms/IAM applied on 12 September](../deployment/aws-applied-2026-09-12.md),
  both stacks `UPDATE_COMPLETE`, existing instances preserved;
- Docker and isolated Compose runtimes are active;
- the shared data volume is mounted;
- platform Caddy owns ingress;
- Fragments secrets come from SSM;
- Fragments deployment targets the platform through SSM Run Command and OIDC.

## Remaining work

- confirm the operator SNS email subscription and prove notification delivery;
- validate the journaled deployment and its locking on the actual host (the
  approved local restore drill and implementation are already documented);
- verify Anchor OIDC/IAM ownership in the Anchor repository;
- separate active application resources from legacy compute stacks;
- observe memory, disk and CPU before changing `t4g.medium`.

No AWS mutation is authorized by this document.
