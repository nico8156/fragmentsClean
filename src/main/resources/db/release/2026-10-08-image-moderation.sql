-- Additive states owned by image producers; historical images are not reclassified here.
ALTER TABLE experience_media DROP CONSTRAINT ck_experience_media_status;
ALTER TABLE experience_media ADD CONSTRAINT ck_experience_media_status CHECK(status IN ('PENDING','REVIEW_REQUIRED','REJECTED','AVAILABLE','DELETION_PENDING','DELETED'));
ALTER TABLE experience_media_views DROP CONSTRAINT ck_experience_media_view_status;
ALTER TABLE experience_media_views ADD CONSTRAINT ck_experience_media_view_status CHECK(status IN ('PENDING','REVIEW_REQUIRED','REJECTED','AVAILABLE','DELETION_PENDING','DELETED'));
ALTER TABLE user_avatar_media DROP CONSTRAINT ck_user_avatar_media_status;
ALTER TABLE user_avatar_media ADD CONSTRAINT ck_user_avatar_media_status CHECK(status IN ('PENDING','REVIEW_REQUIRED','REJECTED','AVAILABLE','RETIRED','DELETION_PENDING','DELETED'));
ALTER TABLE media_catalog_entries DROP CONSTRAINT media_catalog_entries_status_check;
ALTER TABLE media_catalog_entries ADD CONSTRAINT media_catalog_entries_status_check CHECK(status IN ('PENDING','REVIEW_REQUIRED','REJECTED','AVAILABLE','DELETION_PENDING','DELETED'));
