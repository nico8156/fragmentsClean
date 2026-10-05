-- RETIRED is reversible admin conservation, not a physical deletion request.
ALTER TABLE user_avatar_media DROP CONSTRAINT IF EXISTS ck_user_avatar_media_status;
ALTER TABLE user_avatar_media ADD CONSTRAINT ck_user_avatar_media_status
  CHECK(status IN ('PENDING','AVAILABLE','RETIRED','DELETION_PENDING','DELETED'));
ALTER TABLE user_avatar_media DROP CONSTRAINT IF EXISTS ck_user_avatar_media_available;
ALTER TABLE user_avatar_media ADD CONSTRAINT ck_user_avatar_media_available
  CHECK(status NOT IN ('AVAILABLE','RETIRED') OR (object_key IS NOT NULL AND content_type='image/jpeg'
    AND size_bytes>0 AND width>0 AND height>0 AND width=height AND sha256 IS NOT NULL));
