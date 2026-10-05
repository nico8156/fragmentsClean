ALTER TABLE article_media_uploads DROP CONSTRAINT IF EXISTS article_media_uploads_lifecycle_status_check;
ALTER TABLE article_media_uploads ADD CONSTRAINT article_media_uploads_lifecycle_status_check CHECK(lifecycle_status IN ('ACTIVE','RETIRED','DELETION_PENDING','DELETED'));
CREATE INDEX IF NOT EXISTS ix_article_media_purge_pending ON article_media_uploads(updated_at,media_id) WHERE lifecycle_status='DELETION_PENDING';
