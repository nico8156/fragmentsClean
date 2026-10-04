ALTER TABLE coffee_photo_retirements ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'RETIRED' CHECK(lifecycle_status IN ('RETIRED','DELETION_PENDING','DELETED'));
ALTER TABLE coffee_photo_retirements ADD COLUMN IF NOT EXISTS purge_requested_at TIMESTAMPTZ;
ALTER TABLE coffee_photo_retirements ADD COLUMN IF NOT EXISTS purged_at TIMESTAMPTZ;
ALTER TABLE coffee_photo_retirements ADD COLUMN IF NOT EXISTS purge_command_id UUID;
CREATE INDEX IF NOT EXISTS ix_coffee_media_purge_pending ON coffee_photo_retirements(purge_requested_at,photo_id) WHERE lifecycle_status='DELETION_PENDING';
-- mediaCatalogContext-owned fact: older inventories may never revive a physically deleted resource.
ALTER TABLE media_catalog_entries ADD COLUMN IF NOT EXISTS physically_deleted BOOLEAN NOT NULL DEFAULT FALSE;
