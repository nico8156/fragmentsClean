# Fragments editorial email with SES

The `fragments-editorial-email-staging` stack is deliberately separate from
`platform-staging`. Updating the platform stack can re-evaluate its dynamic AMI
reference, so email resources must not put the shared EC2 or its volumes at
risk.

The email stack owns only:

- the `anchor-event.fr` SES sending-domain identity;
- the `nmaldiney@gmail.com` recipient identity required by the SES sandbox;
- a scoped inline policy on `fragments-platform-staging-runtime` allowing SES
  sends for the domain identity and, while SES is sandboxed, the verified review
  recipient identity.

Easy DKIM exposes three CNAME records as CloudFormation outputs. Add them to the
OVH DNS zone without changing the existing root MX records. OVH may separately
redirect `studio@anchor-event.fr` to `nmaldiney@gmail.com` for replies.

Recommended headers:

```text
From: Fragments Studio <studio@anchor-event.fr>
Reply-To: nmaldiney@gmail.com
```

While SES remains in sandbox, the recipient must confirm the AWS verification
email. Request production access before sending to unverified recipients.
After production access is granted, remove the recipient identity resource from
the runtime IAM policy; keep only the sending-domain identity.

Runtime configuration is supplied through the deployment secret/configuration
channel, never through the Studio bundle:

```text
EDITORIAL_EMAIL_ENABLED=true
EDITORIAL_EMAIL_FROM=studio@anchor-event.fr
EDITORIAL_EMAIL_RECIPIENT=nmaldiney@gmail.com
EDITORIAL_EMAIL_STUDIO_BASE_URL=https://studio-staging.anchor-event.fr
EDITORIAL_EMAIL_REGION=eu-west-3
```

The application consumes the existing platform runtime role permission. The
sender must remain `studio@anchor-event.fr` (or another verified SES identity)
and the sandbox recipient must remain verified until SES production access is
granted.

The approval token signing secret is a separate runtime secret:

```text
EDITORIAL_APPROVAL_SECRET=<generated-high-entropy-secret>
EDITORIAL_APPROVAL_TTL=PT24H
```

It must not be placed in GitHub variables, the Studio bundle, an email body, or
the repository. The email only carries the signed token in the Studio link.

The staging bootstrap loads this secret directly from the encrypted SSM
parameter `/fragments/staging/EDITORIAL_APPROVAL_SECRET`. It also enables the
SES adapter and injects the non-secret sender, recipient, Studio URL, region and
TTL settings into the runtime `.env`. Consequently, every successful OpenAI
generation emits the same `article.generation.completed` integration event and
produces one review email, whether the generation was requested manually from
Studio or started by the weekly scheduler. The generation origin does not
control notification delivery.
