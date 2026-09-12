-- Transaction boundary is owned by app-store-2026-09.psql.
-- For standalone use, invoke psql with --single-transaction and ON_ERROR_STOP=1.

CREATE TABLE IF NOT EXISTS ticket_verification_jobs (
    job_id uuid PRIMARY KEY,
    command_id uuid NOT NULL,
    ticket_id uuid NOT NULL,
    user_id uuid NOT NULL,
    ocr_text text NULL,
    image_ref text NULL,
    client_at timestamptz NULL,
    state varchar(32) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    lease_owner varchar(160) NULL,
    lease_until timestamptz NULL,
    next_attempt_at timestamptz NOT NULL,
    last_failure text NULL,
    version bigint NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_verification_job_command
    ON ticket_verification_jobs(command_id);
CREATE INDEX IF NOT EXISTS idx_ticket_verification_job_due
    ON ticket_verification_jobs(state,next_attempt_at,lease_until);
CREATE INDEX IF NOT EXISTS idx_ticket_verification_job_user
    ON ticket_verification_jobs(user_id);

ALTER TABLE inbox_messages ADD COLUMN IF NOT EXISTS lease_until timestamptz NULL;
CREATE INDEX IF NOT EXISTS idx_inbox_messages_claim_lease
    ON inbox_messages(destination,status,lease_until);
