CREATE TABLE IF NOT EXISTS account_erasure_barriers (
    context_name varchar(32) NOT NULL,
    user_id uuid NOT NULL,
    status varchar(16) NOT NULL,
    request_id uuid,
    erased_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_account_erasure_barriers PRIMARY KEY(context_name,user_id),
    CONSTRAINT ck_account_erasure_barriers_context CHECK (
        context_name IN ('AUTHENTICATION','USER_APPLICATION','SOCIAL','TICKET','EXPERIENCE')),
    CONSTRAINT ck_account_erasure_barriers_status CHECK (status IN ('ACTIVE','ERASED')),
    CONSTRAINT ck_account_erasure_barriers_erased CHECK (
        (status='ACTIVE' AND request_id IS NULL AND erased_at IS NULL)
        OR (status='ERASED' AND request_id IS NOT NULL AND erased_at IS NOT NULL))
);
