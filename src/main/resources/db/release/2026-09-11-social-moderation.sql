CREATE TABLE IF NOT EXISTS content_reports (
    report_id UUID PRIMARY KEY, comment_id UUID NOT NULL, target_id UUID NOT NULL,
    author_id UUID NOT NULL, reporter_id UUID NOT NULL, reason VARCHAR(40) NOT NULL,
    details VARCHAR(1000), status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ, version BIGINT NOT NULL,
    CONSTRAINT uq_content_reports_reporter_comment UNIQUE (reporter_id, comment_id)
);
CREATE TABLE IF NOT EXISTS user_blocks (
    block_id UUID PRIMARY KEY, blocker_id UUID NOT NULL, blocked_user_id UUID NOT NULL,
    active BOOLEAN NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL, CONSTRAINT uq_user_blocks_users UNIQUE (blocker_id, blocked_user_id),
    CONSTRAINT ck_user_blocks_not_self CHECK (blocker_id <> blocked_user_id)
);
CREATE TABLE IF NOT EXISTS social_content_reports_projection (
    report_id UUID PRIMARY KEY, comment_id UUID NOT NULL, target_id UUID NOT NULL,
    author_id UUID NOT NULL, reporter_id UUID NOT NULL, reason VARCHAR(40) NOT NULL,
    details VARCHAR(1000), status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ, version BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_social_reports_status_created ON social_content_reports_projection (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_social_reports_comment ON social_content_reports_projection (comment_id);
CREATE TABLE IF NOT EXISTS social_user_blocks_projection (
    block_id UUID PRIMARY KEY, blocker_id UUID NOT NULL, blocked_user_id UUID NOT NULL,
    active BOOLEAN NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL,
    CONSTRAINT uq_social_user_blocks_users UNIQUE (blocker_id, blocked_user_id)
);
CREATE INDEX IF NOT EXISTS idx_social_user_blocks_active ON social_user_blocks_projection (blocker_id, active);
CREATE TABLE IF NOT EXISTS social_moderation_actions_projection (
    action_id UUID PRIMARY KEY, report_id UUID NOT NULL, comment_id UUID NOT NULL,
    operator_id UUID NOT NULL, decision VARCHAR(20) NOT NULL, reason VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_social_moderation_actions_report ON social_moderation_actions_projection (report_id, occurred_at DESC);
