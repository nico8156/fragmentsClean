-- articleContext owns uploads, inventory order and replay checkpoint.
CREATE TABLE IF NOT EXISTS article_media_uploads (
    media_id UUID PRIMARY KEY,article_id UUID NOT NULL,storage_reference TEXT NOT NULL,
    original_name TEXT,content_type VARCHAR(128),size_bytes BIGINT NOT NULL CHECK(size_bytes>0),
    width INTEGER,height INTEGER,purpose VARCHAR(32) NOT NULL CHECK(purpose IN ('STUDIO','GENERATION')),
    uploaded_by UUID,uploaded_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,
    CHECK((width IS NULL AND height IS NULL) OR (width>0 AND height>0))
);
CREATE INDEX IF NOT EXISTS ix_article_media_uploads_article ON article_media_uploads(article_id,media_id);
CREATE TABLE IF NOT EXISTS article_media_catalog_versions(article_id UUID PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE IF NOT EXISTS article_media_catalog_scan(id INTEGER PRIMARY KEY CHECK(id=1),cursor_id UUID,next_scan_at TIMESTAMPTZ NOT NULL DEFAULT now(),completed_at TIMESTAMPTZ);
INSERT INTO article_media_catalog_scan(id) VALUES(1) ON CONFLICT DO NOTHING;
-- mediaCatalogContext owns these local projections and incomplete message parts.
CREATE TABLE IF NOT EXISTS media_catalog_article_versions(article_id UUID PRIMARY KEY,snapshot_version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE IF NOT EXISTS media_catalog_article_parts(article_id UUID NOT NULL,version BIGINT NOT NULL,part INTEGER NOT NULL,parts INTEGER NOT NULL,payload_json TEXT NOT NULL,PRIMARY KEY(article_id,version,part));
CREATE TABLE IF NOT EXISTS media_catalog_article_references(article_id UUID NOT NULL,role VARCHAR(16) NOT NULL,usage_id UUID NOT NULL,media_id UUID NOT NULL,storage_reference TEXT,payload_json JSONB NOT NULL,PRIMARY KEY(article_id,role,usage_id));
CREATE INDEX IF NOT EXISTS ix_media_catalog_article_reference_media ON media_catalog_article_references(media_id,article_id,role,usage_id);
ALTER TABLE media_catalog_entries ADD COLUMN IF NOT EXISTS original_name TEXT;
ALTER TABLE media_catalog_entries ADD COLUMN IF NOT EXISTS uploaded_by UUID;
ALTER TABLE media_catalog_entries ADD COLUMN IF NOT EXISTS purpose VARCHAR(32);
