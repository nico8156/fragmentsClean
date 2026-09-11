-- Lot 05: experienceContext write models, read projections and local references.
-- The INSERT statements are a one-time migration backfill. Runtime behavior is
-- fed only by stable integration events on experiences-events.
CREATE TABLE IF NOT EXISTS experiences (
    experience_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    coffee_id UUID NOT NULL,
    message VARCHAR(4000),
    publication_status VARCHAR(32) NOT NULL,
    moderation_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_experiences_user_updated ON experiences(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS ix_experiences_coffee_updated ON experiences(coffee_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS experience_reports (
    report_id UUID PRIMARY KEY,
    experience_id UUID NOT NULL,
    coffee_id UUID NOT NULL,
    author_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(64) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    CONSTRAINT uq_experience_reporter UNIQUE(reporter_id, experience_id)
);
CREATE INDEX IF NOT EXISTS ix_experience_reports_experience_status ON experience_reports(experience_id,status);

CREATE TABLE IF NOT EXISTS experience_views (
    experience_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    coffee_id UUID NOT NULL,
    message VARCHAR(4000),
    publication_status VARCHAR(32) NOT NULL,
    moderation_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_experience_views_coffee_created ON experience_views(coffee_id,created_at DESC,experience_id DESC);
CREATE INDEX IF NOT EXISTS ix_experience_views_user_created ON experience_views(user_id,created_at DESC,experience_id DESC);

CREATE TABLE IF NOT EXISTS experience_reports_projection (
    report_id UUID PRIMARY KEY,
    experience_id UUID NOT NULL,
    coffee_id UUID NOT NULL,
    author_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(64) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    moderation_version BIGINT NOT NULL DEFAULT -1
);
ALTER TABLE experience_reports_projection ADD COLUMN IF NOT EXISTS moderation_version BIGINT NOT NULL DEFAULT -1;
CREATE INDEX IF NOT EXISTS ix_experience_reports_projection_queue ON experience_reports_projection(status,created_at);
CREATE INDEX IF NOT EXISTS ix_experience_reports_projection_reporter ON experience_reports_projection(reporter_id,experience_id);

CREATE TABLE IF NOT EXISTS experience_moderation_actions_projection (
    action_id UUID PRIMARY KEY,
    report_id UUID NOT NULL,
    experience_id UUID NOT NULL,
    operator_id UUID NOT NULL,
    decision VARCHAR(32) NOT NULL,
    reason VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_experience_moderation_actions_report ON experience_moderation_actions_projection(report_id,occurred_at DESC);

CREATE TABLE IF NOT EXISTS experience_user_profiles (
    user_id UUID PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512),
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS experience_user_blocks (
    block_id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL,
    blocked_user_id UUID NOT NULL,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_experience_user_blocks UNIQUE(blocker_id,blocked_user_id)
);
CREATE INDEX IF NOT EXISTS ix_experience_user_blocks_active ON experience_user_blocks(blocker_id,active);
CREATE TABLE IF NOT EXISTS experience_coffee_references (
    coffee_id UUID PRIMARY KEY,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

INSERT INTO experience_coffee_references(coffee_id,active,updated_at,version)
SELECT id, archived_at IS NULL AND publication_status='PUBLISHED', updated_at, version FROM coffees
ON CONFLICT(coffee_id) DO NOTHING;
INSERT INTO experience_user_profiles(user_id,display_name,avatar_url,updated_at,version)
SELECT id,display_name,avatar_url,updated_at,version FROM app_users
ON CONFLICT(user_id) DO NOTHING;

-- Processes started before Experience existed have no Experience-owned data.
-- Mark that participant acknowledged so legacy in-flight deletion is not blocked.
UPDATE account_deletion_processes
SET acknowledgements = CASE WHEN acknowledgements = '' THEN 'EXPERIENCE' ELSE acknowledgements || ',EXPERIENCE' END
WHERE status = 'IN_PROGRESS' AND position('EXPERIENCE' in acknowledgements) = 0;
