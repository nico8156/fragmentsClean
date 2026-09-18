-- Existing opaque refresh tokens cannot be transformed safely without either
-- retaining their bearer value or introducing a database crypto dependency.
-- This one-time cutover deliberately forces reauthentication.
DELETE FROM refresh_tokens;

DROP INDEX IF EXISTS ux_refresh_tokens_token;
ALTER TABLE refresh_tokens
    DROP COLUMN IF EXISTS token,
    ADD COLUMN IF NOT EXISTS token_hash CHAR(64);
ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);
