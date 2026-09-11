-- Lot 03: product profile ownership and durable account-deletion lifecycle.
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMPTZ;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS account_deletion_processes (
    request_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    requested_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    acknowledgements TEXT NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS auth_provider_credentials (
    user_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(user_id, provider)
);
