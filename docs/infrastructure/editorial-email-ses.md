# Fragments editorial email with SES

The `fragments-editorial-email-staging` stack is deliberately separate from
`platform-staging`. Updating the platform stack can re-evaluate its dynamic AMI
reference, so email resources must not put the shared EC2 or its volumes at
risk.

The email stack owns only:

- the `anchor-event.fr` SES sending-domain identity;
- the `nmaldiney@gmail.com` recipient identity required by the SES sandbox;
- a scoped inline policy on `fragments-platform-staging-runtime` allowing SES
  sends from that domain identity only.

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
