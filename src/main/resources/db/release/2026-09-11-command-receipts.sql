-- Additive release migration for databases created before owner-scoped command receipts.
-- Legacy rows remain ownerless by design and are not exposed through the mobile endpoint.
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS requester_id UUID;
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS command_type VARCHAR(255);
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS fingerprint VARCHAR(64);
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS rejection_code VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_command_status_requester_command
    ON command_status (requester_id, command_id);

-- Backfill only when the retained outbox provides a single unambiguous owner.
-- command_type/fingerprint stay NULL, which marks the receipt as legacy.
WITH candidates AS (
    SELECT (payload_json::jsonb ->> 'commandId')::uuid AS command_id,
           COALESCE(payload_json::jsonb ->> 'userId', payload_json::jsonb ->> 'authorId')::uuid AS requester_id
    FROM outbox_events
    WHERE (payload_json::jsonb ->> 'commandId') ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
      AND COALESCE(payload_json::jsonb ->> 'userId', payload_json::jsonb ->> 'authorId')
          ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
), evidence AS (
    SELECT command_id, MIN(requester_id::text)::uuid AS requester_id
    FROM candidates
    GROUP BY command_id
    HAVING COUNT(DISTINCT requester_id) = 1
)
UPDATE command_status receipt
SET requester_id = evidence.requester_id
FROM evidence
WHERE receipt.command_id = evidence.command_id
  AND receipt.requester_id IS NULL;
