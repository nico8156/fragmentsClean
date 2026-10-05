CREATE INDEX IF NOT EXISTS ix_social_comments_author_created ON social_comments_projection(author_id,created_at DESC,id DESC);
