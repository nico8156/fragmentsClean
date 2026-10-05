-- Checkpoint belongs to userApplicationContext; tracked avatars only.
CREATE TABLE IF NOT EXISTS avatar_media_catalog_scan (
    id INTEGER PRIMARY KEY CHECK(id=1),cursor_id UUID,next_scan_at TIMESTAMPTZ NOT NULL DEFAULT now(),completed_at TIMESTAMPTZ
);
INSERT INTO avatar_media_catalog_scan(id) VALUES(1) ON CONFLICT DO NOTHING;
-- Historical user erasure must survive the first avatar replay.
INSERT INTO account_erasure_barriers(context_name,user_id,status,request_id,erased_at,created_at,updated_at)
SELECT 'MEDIA_CATALOG',user_id,'ERASED',request_id,erased_at,created_at,updated_at
FROM account_erasure_barriers WHERE context_name='USER_APPLICATION' AND status='ERASED'
ON CONFLICT(context_name,user_id) DO UPDATE SET status='ERASED',request_id=excluded.request_id,erased_at=excluded.erased_at,updated_at=excluded.updated_at;
