ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS next_attempt_at timestamptz NULL,
    ADD COLUMN IF NOT EXISTS lease_until timestamptz NULL,
    ADD COLUMN IF NOT EXISTS lease_owner varchar(128) NULL,
    ADD COLUMN IF NOT EXISTS last_error varchar(1000) NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_events_due
    ON outbox_events (status, COALESCE(next_attempt_at, created_at), id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_stream_order
    ON outbox_events (stream_key, id, status);
