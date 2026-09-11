BEGIN;

CREATE TABLE IF NOT EXISTS experience_media (
    media_id UUID PRIMARY KEY, experience_id UUID NOT NULL, coffee_id UUID NOT NULL, user_id UUID,
    declared_content_type VARCHAR(64) NOT NULL, declared_size BIGINT NOT NULL,
    pending_object_key VARCHAR(1024) NOT NULL, status VARCHAR(32) NOT NULL,
    object_key VARCHAR(1024), content_type VARCHAR(64), size_bytes BIGINT NOT NULL DEFAULT 0,
    width INTEGER, height INTEGER, sha256 VARCHAR(64), created_at TIMESTAMPTZ NOT NULL,
	updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL,
	CONSTRAINT ck_experience_media_declared_size CHECK (declared_size BETWEEN 1 AND 8000000),
	CONSTRAINT ck_experience_media_status CHECK (status IN ('PENDING','AVAILABLE','DELETION_PENDING','DELETED')),
	CONSTRAINT ck_experience_media_available CHECK (status <> 'AVAILABLE' OR (object_key IS NOT NULL AND content_type='image/jpeg' AND size_bytes > 0 AND width > 0 AND height > 0 AND sha256 IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_experience_media_experience_status ON experience_media(experience_id,status);
CREATE INDEX IF NOT EXISTS ix_experience_media_cleanup ON experience_media(status,updated_at);

CREATE TABLE IF NOT EXISTS experience_media_views (
    media_id UUID PRIMARY KEY, experience_id UUID NOT NULL, user_id UUID,
    status VARCHAR(32) NOT NULL, object_key VARCHAR(1024), content_type VARCHAR(64),
    size_bytes BIGINT NOT NULL DEFAULT 0, width INTEGER, height INTEGER,
	position INTEGER NOT NULL DEFAULT 0, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL,
	CONSTRAINT ck_experience_media_view_status CHECK (status IN ('PENDING','AVAILABLE','DELETION_PENDING','DELETED')),
	CONSTRAINT ck_experience_media_view_position CHECK (position >= 0)
);
CREATE INDEX IF NOT EXISTS ix_experience_media_views_experience ON experience_media_views(experience_id,status,position);

CREATE TABLE IF NOT EXISTS user_avatar_media (
    media_id UUID PRIMARY KEY, user_id UUID, declared_content_type VARCHAR(64) NOT NULL,
    declared_size BIGINT NOT NULL, pending_object_key VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL, object_key VARCHAR(1024), content_type VARCHAR(64),
    size_bytes BIGINT NOT NULL DEFAULT 0, width INTEGER, height INTEGER, sha256 VARCHAR(64),
	created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL,
	CONSTRAINT ck_user_avatar_media_declared_size CHECK (declared_size BETWEEN 1 AND 8000000),
	CONSTRAINT ck_user_avatar_media_status CHECK (status IN ('PENDING','AVAILABLE','DELETION_PENDING','DELETED')),
	CONSTRAINT ck_user_avatar_media_available CHECK (status <> 'AVAILABLE' OR (object_key IS NOT NULL AND content_type='image/jpeg' AND size_bytes > 0 AND width > 0 AND height > 0 AND width=height AND sha256 IS NOT NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_avatar_media_available ON user_avatar_media(user_id) WHERE status='AVAILABLE';
CREATE INDEX IF NOT EXISTS ix_user_avatar_media_cleanup ON user_avatar_media(status,updated_at);

COMMIT;
