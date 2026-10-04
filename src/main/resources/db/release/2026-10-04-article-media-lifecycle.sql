
ALTER TABLE article_media_uploads ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK(lifecycle_status IN ('ACTIVE','RETIRED'));
