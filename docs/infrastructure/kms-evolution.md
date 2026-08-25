# KMS evolution

## Current decision

Staging reuses the existing AWS-managed SSM key:

```text
alias/aws/ssm
arn:aws:kms:eu-west-3:851725375299:key/6e2d9298-8432-48e2-a566-bc10cbc78f2a
```

The `platform-staging` stack does not create a key. The key ARN is supplied as
the `SecretsKmsKeyArn` parameter so the infrastructure remains explicit and
the choice can evolve without changing the secret namespace.

## Recommended production evolution

When production needs stronger separation, create customer-managed keys with
separate ownership and policies:

```text
alias/fragments/production-secrets
alias/anchor/production-secrets
```

The migration sequence should be:

1. create the new key and alias;
2. grant only the corresponding application/runtime role `kms:Decrypt`;
3. copy parameters within the same application namespace using the new key;
4. verify a fresh runtime can load the new parameters;
5. rotate the affected application secrets where appropriate;
6. remove access to the previous key only after rollback retention expires;
7. schedule key rotation and review the key policy.

Changing the KMS key does not by itself rotate the secret value. Secret
rotation and encryption-key migration remain separate, auditable operations.
