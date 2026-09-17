\set ON_ERROR_STOP on

BEGIN;

-- Recovery-only cross-context purge. This script runs against an isolated restored database before
-- it is allowed to serve traffic; normal application behavior remains bounded-context owned.
DELETE FROM social_moderation_actions_projection action
WHERE action.operator_id = :'user_id'::uuid OR action.report_id IN (
  SELECT report_id FROM social_content_reports_projection
  WHERE reporter_id = :'user_id'::uuid OR author_id = :'user_id'::uuid
);
DELETE FROM social_content_reports_projection WHERE reporter_id = :'user_id'::uuid OR author_id = :'user_id'::uuid;
DELETE FROM social_user_blocks_projection WHERE blocker_id = :'user_id'::uuid OR blocked_user_id = :'user_id'::uuid;
DELETE FROM content_reports WHERE reporter_id = :'user_id'::uuid OR author_id = :'user_id'::uuid;
DELETE FROM user_blocks WHERE blocker_id = :'user_id'::uuid OR blocked_user_id = :'user_id'::uuid;
DELETE FROM social_comments_projection WHERE author_id = :'user_id'::uuid;
DELETE FROM social_likes_projection WHERE user_id = :'user_id'::uuid;
DELETE FROM comments WHERE author_id = :'user_id'::uuid;
DELETE FROM likes WHERE user_id = :'user_id'::uuid;
DELETE FROM user_social_projection WHERE user_id = :'user_id'::uuid;
DELETE FROM users WHERE user_id = :'user_id'::uuid;

DELETE FROM ticket_verification_jobs WHERE user_id = :'user_id'::uuid;
DELETE FROM ticket_submission_fingerprints WHERE user_id = :'user_id'::uuid;
DELETE FROM ticket_status_projection WHERE user_id = :'user_id'::uuid;
DELETE FROM user_entitlements_projection WHERE user_id = :'user_id'::uuid;
DELETE FROM tickets WHERE user_id = :'user_id'::uuid;

UPDATE experience_media
SET status = 'DELETED', user_id = NULL, updated_at = now(), version = version + 1
WHERE user_id = :'user_id'::uuid AND status <> 'DELETED';
DELETE FROM experience_media_views WHERE user_id = :'user_id'::uuid;
DELETE FROM experience_moderation_actions_projection
WHERE operator_id = :'user_id'::uuid OR report_id IN (
  SELECT report_id FROM experience_reports_projection
  WHERE author_id = :'user_id'::uuid OR reporter_id = :'user_id'::uuid
);
DELETE FROM experience_reports_projection WHERE author_id = :'user_id'::uuid OR reporter_id = :'user_id'::uuid;
DELETE FROM experience_reports WHERE author_id = :'user_id'::uuid OR reporter_id = :'user_id'::uuid;
DELETE FROM experience_views WHERE user_id = :'user_id'::uuid;
DELETE FROM experiences WHERE user_id = :'user_id'::uuid;
DELETE FROM experience_user_profiles WHERE user_id = :'user_id'::uuid;
DELETE FROM experience_user_blocks WHERE blocker_id = :'user_id'::uuid OR blocked_user_id = :'user_id'::uuid;

UPDATE user_avatar_media
SET status = 'DELETED', user_id = NULL, updated_at = now(), version = version + 1
WHERE user_id = :'user_id'::uuid AND status <> 'DELETED';
DELETE FROM user_saved_coffees_projection WHERE user_id = :'user_id'::uuid;
DELETE FROM saved_coffees WHERE user_id = :'user_id'::uuid;
DELETE FROM pass_ticket_contributions WHERE user_id = :'user_id'::uuid;
DELETE FROM pass_experience_contributions WHERE user_id = :'user_id'::uuid;
DELETE FROM user_pass_projection WHERE user_id = :'user_id'::uuid;

DELETE FROM admin_user_access WHERE user_id = :'auth_user_id'::uuid;
DELETE FROM auth_provider_credentials WHERE user_id = :'auth_user_id'::uuid;
UPDATE refresh_tokens SET revoked = true WHERE user_id = :'auth_user_id'::uuid;
UPDATE auth_users
SET provider_user_id = 'deleted:' || id,
    email = 'deleted+' || id || '@invalid.local',
    email_verified = false,
    display_name = NULL,
    avatar_url = NULL,
    lifecycle_status = 'DELETED',
    deleted_at = GREATEST(COALESCE(deleted_at, :'requested_at'::timestamptz), :'requested_at'::timestamptz)
WHERE id = :'auth_user_id'::uuid;

UPDATE app_users
SET display_name = 'Compte supprimé',
    avatar_url = NULL,
    lifecycle_status = 'DELETED',
    deletion_requested_at = COALESCE(deletion_requested_at, :'requested_at'::timestamptz),
    deleted_at = COALESCE(deleted_at, :'requested_at'::timestamptz),
    updated_at = GREATEST(updated_at, :'requested_at'::timestamptz),
    version = version + 1
WHERE id = :'user_id'::uuid AND lifecycle_status <> 'DELETED';

DELETE FROM account_deletion_processes WHERE user_id = :'user_id'::uuid;
DELETE FROM command_status WHERE requester_id = :'user_id'::uuid;
DELETE FROM projection_sync_events
WHERE payload_json::text LIKE '%' || :'user_id' || '%'
   OR payload_json::text LIKE '%' || :'auth_user_id' || '%';
DELETE FROM outbox_events
WHERE payload_json LIKE '%' || :'user_id' || '%'
   OR payload_json LIKE '%' || :'auth_user_id' || '%';

INSERT INTO account_erasure_barriers(
  context_name, user_id, status, request_id, erased_at, created_at, updated_at
)
SELECT scope, :'user_id'::uuid, 'ERASED', :'request_id'::uuid,
       :'requested_at'::timestamptz, now(), now()
FROM unnest(ARRAY['AUTHENTICATION','USER_APPLICATION','SOCIAL','TICKET','EXPERIENCE']) AS scope
ON CONFLICT(context_name, user_id) DO UPDATE
SET status = 'ERASED', request_id = EXCLUDED.request_id,
    erased_at = LEAST(account_erasure_barriers.erased_at, EXCLUDED.erased_at), updated_at = now();

COMMIT;
