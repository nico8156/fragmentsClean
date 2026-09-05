# AWS legacy staging stacks audit — 2026-09-05

This is a read-only inventory. It does not authorize stopping, deleting,
detaching or redriving any AWS resource.

## Executive decision

Neither legacy stack can be deleted as a whole today. Both still own durable or
active application resources such as ECR repositories and SQS queues. Their
stopped EC2 hosts, attached EBS volumes and Elastic IPs are cleanup candidates,
but they must first be separated from the application resources through a
reviewed CloudFormation change set.

The active runtime is `platform-staging` instance `i-004d3e9cbca327d01`
(`t4g.medium`, running, public IP `13.39.97.191`).

## Fragments legacy host

Stack: `fragments-staging-minimal`, `UPDATE_COMPLETE`.

- stopped `t4g.small`: `i-04984df67caf3099b`;
- Elastic IP: `15.224.134.41`;
- 30 GiB gp3 root volume `vol-0a3632261567e26cd`, delete on termination;
- 30 GiB gp3 data volume `vol-070d003ac2281d223`, retained on termination;
- legacy security group, instance profile and runtime role;
- active Fragments ECR repository;
- active Fragments SQS queues and historical shared DLQ;
- old GitHub deploy role whose usage must be checked before removal.

The stack remains the CloudFormation owner of resources consumed by the active
platform. Deleting it would therefore remove more than the stopped host.

## Anchor legacy host

Stack: `anchor-staging-minimal`, `UPDATE_COMPLETE`.

- stopped `t4g.medium`: `i-0e41cd6228bd947e2`;
- Elastic IP: `13.37.84.226`;
- 30 GiB gp3 root volume `vol-06293308fca6bd74f`, delete on termination;
- 30 GiB gp3 data volume `vol-036ca29c5f9966414`, retained on termination;
- legacy security group, instance profile and runtime role;
- active Anchor backend and web ECR repositories;
- active Anchor SQS queues and shared DLQ.

As with Fragments, deleting the stack would remove active application
resources in addition to the stopped host.

## Recurring cost estimate

Stopped instances have no instance-compute charge, but their EBS volumes and
public IPv4 addresses remain billable.

The AWS Price List API returned, effective 2026-09-01 for Paris:

- gp3 storage: USD 0.0928 per GB-month;
- public IPv4 estimate: USD 0.005/hour.

Each legacy host retains 60 GiB of gp3 storage, approximately USD 5.57/month,
plus approximately USD 3.65/month for its public IPv4 address. The two hosts
therefore represent approximately USD 18.44/month before snapshots, ECR
storage, SQS requests and taxes.

This is an engineering estimate, not an AWS invoice forecast.

## Safe cleanup sequence

1. Prove current DNS and deployment workflows reference only the shared platform.
2. Produce and restore-test logical PostgreSQL backups from the active runtime.
3. Snapshot both legacy retained data volumes and record snapshot ids.
4. Extract active ECR, SQS and deployment IAM resources into application-owned
   stacks, using CloudFormation resource import where appropriate.
5. Generate change sets removing only legacy compute resources.
6. Review replacement and deletion actions before execution.
7. Release legacy Elastic IPs only after checking DNS, OAuth callbacks,
   webhooks and allow-lists.
8. Retain snapshots for an agreed rollback period before deleting them.

## Explicit blockers

- the Fragments per-queue DLQ migration has not yet been deployed;
- the 22 historical Fragments DLQ messages are not yet triaged;
- the PostgreSQL restore drill has not yet run on staging;
- active application resources remain mixed with legacy host resources;
- Anchor deployment ownership and IAM usage need confirmation from its repository.
