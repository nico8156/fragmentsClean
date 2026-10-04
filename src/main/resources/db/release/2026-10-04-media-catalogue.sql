-- Catalogue owns only its local projection. No cross-context data copy.
CREATE TABLE IF NOT EXISTS media_catalog_entries (
    origin varchar(24) NOT NULL,
    media_id uuid NOT NULL,
    resource_id uuid,
    owner_id uuid,
    status varchar(24) NOT NULL,
    object_key text,
    content_type varchar(128),
    size_bytes bigint NOT NULL DEFAULT 0,
    width integer,
    height integer,
    created_at timestamptz,
    updated_at timestamptz NOT NULL,
    source_version bigint NOT NULL,
    PRIMARY KEY(origin,media_id),
    CHECK (origin IN ('EXPERIENCE','COFFEE','AVATAR','ARTICLE')),
    CHECK (status IN ('PENDING','AVAILABLE','DELETION_PENDING','DELETED'))
);
CREATE INDEX IF NOT EXISTS ix_media_catalog_owner ON media_catalog_entries(owner_id,origin,media_id);
CREATE INDEX IF NOT EXISTS ix_media_catalog_status ON media_catalog_entries(status,origin,media_id);

ALTER TABLE account_erasure_barriers DROP CONSTRAINT IF EXISTS ck_account_erasure_barriers_context;
ALTER TABLE account_erasure_barriers ADD CONSTRAINT ck_account_erasure_barriers_context CHECK (context_name IN ('AUTHENTICATION','USER_APPLICATION','SOCIAL','TICKET','EXPERIENCE','MEDIA_CATALOG'));

-- Cursor belongs to the producer, alongside experience_media.
CREATE TABLE IF NOT EXISTS experience_media_catalog_scan (
    id integer PRIMARY KEY CHECK(id=1),cursor_id uuid,
    next_scan_at timestamptz NOT NULL DEFAULT now(),completed_at timestamptz
);
INSERT INTO experience_media_catalog_scan(id) VALUES(1) ON CONFLICT DO NOTHING;

-- Preserve pre-existing erasure barriers before any old media events can be replayed.
INSERT INTO account_erasure_barriers(context_name,user_id,status,request_id,erased_at,created_at,updated_at)
SELECT 'MEDIA_CATALOG',user_id,'ERASED',request_id,erased_at,created_at,updated_at
FROM account_erasure_barriers WHERE context_name='EXPERIENCE' AND status='ERASED'
ON CONFLICT(context_name,user_id) DO UPDATE SET status='ERASED',request_id=excluded.request_id,erased_at=excluded.erased_at,updated_at=excluded.updated_at;
