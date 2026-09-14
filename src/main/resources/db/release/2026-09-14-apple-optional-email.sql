-- Apple identity is keyed by (provider, provider_user_id), not email.
-- Preserve all existing emails; do not manufacture email addresses.
ALTER TABLE auth_users ALTER COLUMN email DROP NOT NULL;
