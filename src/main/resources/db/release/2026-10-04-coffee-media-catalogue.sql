CREATE TABLE IF NOT EXISTS media_catalog_coffee_versions (
    coffee_id UUID PRIMARY KEY,
    snapshot_version BIGINT NOT NULL DEFAULT -1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS coffee_media_catalog_scan (
    id INTEGER PRIMARY KEY CHECK(id=1),cursor_id UUID,next_scan_at TIMESTAMPTZ NOT NULL DEFAULT now(),completed_at TIMESTAMPTZ
);
INSERT INTO coffee_media_catalog_scan(id) VALUES(1) ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS ix_media_catalog_resource ON media_catalog_entries(origin,resource_id,source_version);
