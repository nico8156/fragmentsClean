-- Direct moderation has no synthetic user report. Deploy before enabling the admin route.
ALTER TABLE experience_moderation_actions_projection ALTER COLUMN report_id DROP NOT NULL;
CREATE INDEX IF NOT EXISTS ix_experience_moderation_actions_experience ON experience_moderation_actions_projection(experience_id,occurred_at DESC,action_id DESC);
CREATE INDEX IF NOT EXISTS ix_experience_views_admin_created ON experience_views(created_at DESC,experience_id DESC);
CREATE INDEX IF NOT EXISTS ix_experience_views_admin_author ON experience_views(user_id,created_at DESC,experience_id DESC);
