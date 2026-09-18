ALTER TABLE projection_sync_events
    ADD COLUMN IF NOT EXISTS audience VARCHAR(16),
    ADD COLUMN IF NOT EXISTS recipient_id VARCHAR(100);

-- Existing rows fail closed. Known public/user-owned shapes are then promoted
-- explicitly; an unrecognised historical shape remains visible to admins only.
UPDATE projection_sync_events
SET audience = 'ADMIN', recipient_id = NULL
WHERE audience IS NULL;

UPDATE projection_sync_events
SET audience = 'PUBLIC', recipient_id = NULL
WHERE projection IN ('articles', 'coffees')
   OR (projection IN ('comments', 'likes') AND scope = 'target')
   OR (projection = 'experiences' AND scope = 'coffee');

UPDATE projection_sync_events
SET audience = 'USER', recipient_id = entity_id
WHERE scope = 'user'
  AND projection IN ('entitlements', 'savedCoffees', 'blocked-users', 'experiences')
  AND entity_id IS NOT NULL;

UPDATE projection_sync_events event
SET audience = 'USER', recipient_id = ticket.user_id::text
FROM ticket_status_projection ticket
WHERE event.projection = 'tickets'
  AND event.scope = 'entity'
  AND event.entity_id = ticket.ticket_id::text;

ALTER TABLE projection_sync_events
    -- A producer that bypasses the typed repository must fail closed.
    ALTER COLUMN audience SET DEFAULT 'ADMIN',
    ALTER COLUMN audience SET NOT NULL;

ALTER TABLE projection_sync_events
    ADD CONSTRAINT projection_sync_events_audience_check
        CHECK (audience IN ('PUBLIC', 'USER', 'ADMIN')) NOT VALID,
    ADD CONSTRAINT projection_sync_events_recipient_check
        CHECK ((audience = 'USER' AND recipient_id IS NOT NULL AND recipient_id <> '')
            OR (audience <> 'USER' AND recipient_id IS NULL)) NOT VALID;

ALTER TABLE projection_sync_events
    VALIDATE CONSTRAINT projection_sync_events_audience_check;
ALTER TABLE projection_sync_events
    VALIDATE CONSTRAINT projection_sync_events_recipient_check;
