-- Owner-local author lookup for received reports and their decision history.
CREATE INDEX IF NOT EXISTS ix_social_reports_author_created
    ON social_content_reports_projection (author_id, created_at DESC, report_id DESC);
