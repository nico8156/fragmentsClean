CREATE INDEX IF NOT EXISTS ix_experience_reports_author_created ON experience_reports_projection(author_id,created_at DESC,report_id DESC);
