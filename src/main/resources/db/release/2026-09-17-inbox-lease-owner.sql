ALTER TABLE inbox_messages
    ADD COLUMN IF NOT EXISTS lease_owner varchar(36) NULL;
