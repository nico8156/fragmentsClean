-- Apply before deploying the backend. Existing generations preserve their visual direction.
alter table article_authoring_sagas add column if not exists art_direction varchar(32) not null default 'ORIGINAL';
