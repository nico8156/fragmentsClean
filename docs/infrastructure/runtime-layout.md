# Shared staging runtime layout

The shared EC2 host must keep platform, Anchor, and Fragments ownership
separate. The layout is prepared by
`scripts/bootstrap-shared-staging-layout.sh`.

## Host directories

```text
/srv/platform/
  caddy/                         # platform-owned reverse proxy files
  scripts/                       # platform operational scripts

/srv/anchor/staging/
  db/                            # Anchor schema/data deployment files
  apple-wallet-certs/            # mounted runtime certificates, not Git data

/srv/fragments/staging/
  db/                            # Fragments schema/data deployment files
  coffee-photos/                 # Fragments working/object staging path

/srv/anchor-postgres/postgres-data/
/srv/fragments-postgres/postgres-data/
```

The two PostgreSQL directories are never shared. Their credentials, Compose
projects, Docker networks, backups, and restore procedures remain independent.

## Docker boundary

Anchor and Fragments keep separate private application networks. Each project
may expose one explicitly named edge network for the platform Caddy instance:

```text
anchor-staging-edge
fragments-staging-edge
```

Backends bind only inside their project network or to loopback during the
transition. Caddy is the only service that binds host ports 80 and 443.

## Bootstrap safety

The bootstrap script is intentionally limited to idempotent directory creation.
It does not:

- format or mount an EBS volume;
- delete or move existing data;
- change ownership of existing files;
- create Docker networks;
- start or stop containers;
- write secrets.

Volume mounting and permissions require a separate, reviewed operational step
after the platform instance and EBS volume have been identified.

## Migration rule

The existing Anchor and Fragments Compose files remain deployable on their
current hosts until the shared-host Compose adaptations and Caddy routing have
passed validation. This layout is not a cutover by itself.
