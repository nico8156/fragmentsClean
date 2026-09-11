-- =========================================================
--  SCHEMA DE BASE
-- =========================================================
CREATE TABLE IF NOT EXISTS identities (
                                          id               UUID PRIMARY KEY,
                                          user_id          UUID         NOT NULL,
                                          provider         VARCHAR(50)  NOT NULL,
                                          provider_user_id VARCHAR(255) NOT NULL,
                                          email            VARCHAR(255),
                                          created_at       TIMESTAMPTZ  NOT NULL,
                                          last_auth_at     TIMESTAMPTZ
);
-- Coffee shops

CREATE TABLE IF NOT EXISTS coffees (
                         id              UUID PRIMARY KEY,
                         google_place_id VARCHAR(255),
                         name            VARCHAR(255) NOT NULL,
                         address_line1   VARCHAR(255),
                         city            VARCHAR(255),
                         postal_code     VARCHAR(32),
                         country         VARCHAR(8),
                         lat             DOUBLE PRECISION NOT NULL,
                         lon             DOUBLE PRECISION NOT NULL,
                         phone_number    VARCHAR(64),
                         website         VARCHAR(512),
                         tags_csv        TEXT,
                         version         INTEGER NOT NULL,
                         updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                         archived_at     TIMESTAMP WITH TIME ZONE
);

ALTER TABLE IF EXISTS coffees
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE IF EXISTS coffees
    ADD COLUMN IF NOT EXISTS publication_status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED';

CREATE UNIQUE INDEX IF NOT EXISTS ux_coffees_google_place_id
    ON coffees (google_place_id)
    WHERE google_place_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS coffee_photos (
    coffee_id UUID NOT NULL REFERENCES coffees(id) ON DELETE CASCADE,
    photo_id UUID NOT NULL,
    photo_uri VARCHAR(2000) NOT NULL,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (coffee_id, photo_id),
    UNIQUE (coffee_id, sort_order)
);

CREATE TABLE IF NOT EXISTS coffee_opening_hours (
    coffee_id UUID NOT NULL REFERENCES coffees(id) ON DELETE CASCADE,
    day_code INTEGER NOT NULL CHECK (day_code BETWEEN 0 AND 6),
    start_minute INTEGER NOT NULL CHECK (start_minute BETWEEN 0 AND 1439),
    end_minute INTEGER NOT NULL CHECK (end_minute BETWEEN 1 AND 1440),
    PRIMARY KEY (coffee_id, day_code, start_minute),
    CHECK (start_minute < end_minute)
);

-- CREATE INDEX ix_coffees_city ON coffees (city);
-- CREATE INDEX ix_coffees_lat_lon ON coffees (lat, lon);
--
--
-- CREATE INDEX IF NOT EXISTS idx_coffees_google_id
--     ON coffees (google_id);


-- COFFEE_Projection

CREATE TABLE IF NOT EXISTS coffee_summaries_projection (
                                             id              UUID PRIMARY KEY,
                                             google_place_id VARCHAR(255),
                                             name            VARCHAR(255) NOT NULL,
                                             address_line1   VARCHAR(255),
                                             city            VARCHAR(255),
                                             postal_code     VARCHAR(32),
                                             country         VARCHAR(8),
                                             lat             DOUBLE PRECISION NOT NULL,
                                             lon             DOUBLE PRECISION NOT NULL,
                                             phone_number    VARCHAR(64),
                                             website         VARCHAR(512),
                                             tags_json       JSONB,
                                             rating          NUMERIC(3,1),          -- optionnel pour plus tard
                                             version         INTEGER NOT NULL,
                                             updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
ALTER TABLE IF EXISTS coffee_summaries_projection
    ADD COLUMN IF NOT EXISTS publication_status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED';

CREATE TABLE IF NOT EXISTS coffee_projection_checkpoints (
    coffee_id           UUID PRIMARY KEY,
    latest_version      BIGINT NOT NULL,
    publication_status  VARCHAR(32) NOT NULL,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    changed_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS coffee_photos_projection (
  id         UUID PRIMARY KEY,
  coffee_id  UUID NOT NULL,
  photo_uri  VARCHAR(2000) NOT NULL,
  is_cover   BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INTEGER NOT NULL DEFAULT 0
);
ALTER TABLE coffee_photos_projection ADD COLUMN IF NOT EXISTS is_cover BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE coffee_photos_projection ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_coffee_photos_coffee_id
  ON coffee_photos_projection(coffee_id);

INSERT INTO coffee_photos (coffee_id, photo_id, photo_uri, is_cover, sort_order)
SELECT projection.coffee_id, projection.id, projection.photo_uri,
       ROW_NUMBER() OVER (PARTITION BY projection.coffee_id ORDER BY projection.id) = 1,
       ROW_NUMBER() OVER (PARTITION BY projection.coffee_id ORDER BY projection.id) - 1
FROM coffee_photos_projection projection
JOIN coffees coffee ON coffee.id = projection.coffee_id
ON CONFLICT (coffee_id, photo_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS coffee_openinghours_projection (
  id                  UUID PRIMARY KEY,
  coffee_id           UUID NOT NULL,
  weekday_description VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_coffee_openinghours_coffee_id
  ON coffee_openinghours_projection(coffee_id);



-- Likes (write model)
CREATE TABLE IF NOT EXISTS likes (
                                     like_id     UUID PRIMARY KEY,
                                     user_id     UUID         NOT NULL,
                                     target_id   UUID         NOT NULL,
                                     active      BOOLEAN      NOT NULL,
                                     updated_at  TIMESTAMPTZ  NOT NULL,
                                     version     BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_likes_target_active
    ON likes (target_id, active);

CREATE INDEX IF NOT EXISTS idx_likes_user_target
    ON likes (user_id, target_id);

-- Likes projection (read model)
CREATE TABLE IF NOT EXISTS social_likes_projection (
    like_id    UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    target_id  UUID        NOT NULL,
    active     BOOLEAN     NOT NULL,
    version    BIGINT      NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_social_likes_projection_user_target
    ON social_likes_projection (user_id, target_id);

CREATE INDEX IF NOT EXISTS idx_social_likes_projection_target_active
    ON social_likes_projection (target_id, active);

CREATE INDEX IF NOT EXISTS idx_social_likes_projection_user_active
    ON social_likes_projection (user_id, active);

-- Comments (write model)
CREATE TABLE IF NOT EXISTS comments (
                                        comment_id   UUID          NOT NULL PRIMARY KEY,
                                        target_id    UUID          NOT NULL,
                                        author_id    UUID          NOT NULL,
                                        parent_id    UUID          NULL,

                                        body         VARCHAR(4000) NOT NULL,

                                        created_at   TIMESTAMP     NOT NULL,
                                        edited_at    TIMESTAMP     NULL,
                                        deleted_at   TIMESTAMP     NULL,

                                        moderation   VARCHAR(32)   NOT NULL,
                                        version      BIGINT        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_target_created_at
    ON comments (target_id, created_at);


-- Comments projection (read model)
CREATE TABLE IF NOT EXISTS social_comments_projection (
                                                          id           UUID          NOT NULL PRIMARY KEY,
                                                          target_id    UUID          NOT NULL,
                                                          author_id    UUID          NOT NULL,
                                                          parent_id    UUID          NULL,

                                                          body         VARCHAR(4000) NOT NULL,

                                                          created_at   TIMESTAMP     NOT NULL,
                                                          edited_at    TIMESTAMP     NULL,
                                                          deleted_at   TIMESTAMP     NULL,

                                                          moderation   VARCHAR(32)   NOT NULL,
                                                          like_count   BIGINT        NOT NULL DEFAULT 0,
                                                          reply_count  BIGINT        NOT NULL DEFAULT 0,
                                                          version      BIGINT        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_social_comments_projection_target_created_at
    ON social_comments_projection (target_id, created_at);

CREATE INDEX IF NOT EXISTS idx_social_comments_projection_author_visible
    ON social_comments_projection (author_id, deleted_at, moderation);

-- Social moderation write models
CREATE TABLE IF NOT EXISTS content_reports (
    report_id UUID PRIMARY KEY,
    comment_id UUID NOT NULL,
    target_id UUID NOT NULL,
    author_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(40) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    CONSTRAINT uq_content_reports_reporter_comment UNIQUE (reporter_id, comment_id)
);

CREATE TABLE IF NOT EXISTS user_blocks (
    block_id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL,
    blocked_user_id UUID NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_user_blocks_users UNIQUE (blocker_id, blocked_user_id),
    CONSTRAINT ck_user_blocks_not_self CHECK (blocker_id <> blocked_user_id)
);

-- Local read models fed by social integration events.
CREATE TABLE IF NOT EXISTS social_content_reports_projection (
    report_id UUID PRIMARY KEY,
    comment_id UUID NOT NULL,
    target_id UUID NOT NULL,
    author_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(40) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_social_reports_status_created
    ON social_content_reports_projection (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_social_reports_comment
    ON social_content_reports_projection (comment_id);

CREATE TABLE IF NOT EXISTS social_user_blocks_projection (
    block_id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL,
    blocked_user_id UUID NOT NULL,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_social_user_blocks_users UNIQUE (blocker_id, blocked_user_id)
);

CREATE INDEX IF NOT EXISTS idx_social_user_blocks_active
    ON social_user_blocks_projection (blocker_id, active);

CREATE TABLE IF NOT EXISTS social_moderation_actions_projection (
    action_id UUID PRIMARY KEY,
    report_id UUID NOT NULL,
    comment_id UUID NOT NULL,
    operator_id UUID NOT NULL,
    decision VARCHAR(20) NOT NULL,
    reason VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_social_moderation_actions_report
    ON social_moderation_actions_projection (report_id, occurred_at DESC);


create table if not exists users (
                                     user_id    uuid primary key,
                                     created_at timestamp with time zone not null,
                                     updated_at timestamp with time zone not null,
                                     display_name varchar(255) not null,
                                     avatar_url  varchar(512),
                                     bio         text,
                                     locale      varchar(20) not null,
                                     version     bigint not null
);
CREATE TABLE IF NOT EXISTS user_social_projection (
                                                      user_id      UUID PRIMARY KEY,
                                                      display_name VARCHAR(255) NOT NULL,
                                                      avatar_url   VARCHAR(512),
                                                      created_at   TIMESTAMP    NOT NULL,
                                                      updated_at   TIMESTAMP    NOT NULL,
                                                      version      BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_social_projection_updated_at
    ON user_social_projection (updated_at);


CREATE TABLE IF NOT EXISTS outbox_events (
                               id              BIGSERIAL PRIMARY KEY,          -- clé interne, cursor

                               event_id        VARCHAR(50)  NOT NULL UNIQUE,   -- UUID métier (string)
                               event_type      VARCHAR(255) NOT NULL,          -- FQCN de l'event
                               aggregate_type  VARCHAR(100) NOT NULL,          -- ex: "User"
                               aggregate_id    VARCHAR(100) NOT NULL,          -- ex: userId.toString()
                               stream_key      VARCHAR(255) NOT NULL,          -- ex: "user:{userId}"

                               payload_json    TEXT        NOT NULL,           -- @Lob String

                               occurred_at     TIMESTAMPTZ NOT NULL,           -- Instant
                               created_at      TIMESTAMPTZ NOT NULL,           -- Instant

                               status          VARCHAR(32) NOT NULL,           -- OutboxStatus (enum string)
                               retry_count     INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS inbox_messages (
    id              BIGSERIAL PRIMARY KEY,
    destination     VARCHAR(255) NOT NULL,
    event_id        VARCHAR(50)  NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    event_version   INTEGER      NOT NULL,
    received_at     TIMESTAMPTZ  NOT NULL,
    processed_at    TIMESTAMPTZ,
    status          VARCHAR(32)  NOT NULL,
    error_message   TEXT,
    UNIQUE(destination, event_id)
);

CREATE INDEX IF NOT EXISTS idx_inbox_messages_destination_status
    ON inbox_messages (destination, status);

CREATE TABLE IF NOT EXISTS projection_sync_events (
    id            BIGSERIAL PRIMARY KEY,
    event_name    VARCHAR(100) NOT NULL,
    projection    VARCHAR(100),
    scope         VARCHAR(50),
    entity_id     VARCHAR(100),
    version       BIGINT,
    changed_at    TIMESTAMPTZ NOT NULL,
    payload_json  JSONB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_projection_sync_events_projection_id
    ON projection_sync_events (projection, id);

CREATE TABLE IF NOT EXISTS command_status (
    command_id      UUID PRIMARY KEY,
    requester_id    UUID,
    command_type    VARCHAR(255),
    fingerprint     VARCHAR(64),
    status          VARCHAR(32) NOT NULL,
    aggregate_type  VARCHAR(100),
    aggregate_id    VARCHAR(100),
    event_id        VARCHAR(50),
    event_type      VARCHAR(255),
    applied_at      TIMESTAMPTZ,
    rejected_at     TIMESTAMPTZ,
    rejection_code  VARCHAR(100),
    reason          TEXT,
    updated_at      TIMESTAMPTZ NOT NULL
);

-- Additive compatibility for environments created before durable owner-scoped receipts.
-- Legacy rows deliberately remain ownerless and are never exposed by the mobile endpoint.
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS requester_id UUID;
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS command_type VARCHAR(255);
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS fingerprint VARCHAR(64);
ALTER TABLE command_status ADD COLUMN IF NOT EXISTS rejection_code VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_command_status_requester_command
    ON command_status (requester_id, command_id);

WITH candidates AS (
    SELECT (payload_json::jsonb ->> 'commandId')::uuid AS command_id,
           COALESCE(payload_json::jsonb ->> 'userId', payload_json::jsonb ->> 'authorId')::uuid AS requester_id
    FROM outbox_events
    WHERE (payload_json::jsonb ->> 'commandId') ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
      AND COALESCE(payload_json::jsonb ->> 'userId', payload_json::jsonb ->> 'authorId')
          ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
), evidence AS (
    SELECT command_id, MIN(requester_id::text)::uuid AS requester_id
    FROM candidates
    GROUP BY command_id
    HAVING COUNT(DISTINCT requester_id) = 1
)
UPDATE command_status receipt
SET requester_id = evidence.requester_id
FROM evidence
WHERE receipt.command_id = evidence.command_id
  AND receipt.requester_id IS NULL;

CREATE TABLE IF NOT EXISTS articles (
                          article_id        UUID PRIMARY KEY,
                          slug              VARCHAR(255)   NOT NULL,
                          locale            VARCHAR(20)    NOT NULL,

                          author_id         UUID           NOT NULL,
                          author_name       VARCHAR(255)   NOT NULL,

                          title             VARCHAR(255)   NOT NULL,
                          intro             TEXT           NOT NULL,
                          blocks_json       TEXT           NOT NULL,
                          conclusion        TEXT           NOT NULL,

                          cover_url         TEXT,
                          cover_width       INTEGER,
                          cover_height      INTEGER,
                          cover_alt         TEXT,

                          tags_json         TEXT           NOT NULL,
                          reading_time_min  INTEGER        NOT NULL,

                          coffee_ids_json   TEXT,

                          created_at        TIMESTAMPTZ    NOT NULL,
                          updated_at        TIMESTAMPTZ    NOT NULL,
                          published_at      TIMESTAMPTZ,

                          status            VARCHAR(32)    NOT NULL,
                          version           BIGINT         NOT NULL,

                          -- Compatibility pointers used during revision migration.
                          working_revision_id   UUID,
                          published_revision_id UUID
);

ALTER TABLE articles
    ADD COLUMN IF NOT EXISTS working_revision_id UUID;

ALTER TABLE articles
    ADD COLUMN IF NOT EXISTS published_revision_id UUID;

CREATE TABLE IF NOT EXISTS article_revisions (
    revision_id       UUID PRIMARY KEY,
    article_id        UUID         NOT NULL REFERENCES articles(article_id) ON DELETE CASCADE,
    revision_number   INTEGER      NOT NULL,
    title             TEXT         NOT NULL,
    introduction      TEXT         NOT NULL,
    conclusion        TEXT         NOT NULL,
    cover_reference   TEXT,
    cover_width       INTEGER,
    cover_height      INTEGER,
    cover_alt         TEXT,
    reading_time_min  INTEGER      NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    published_at      TIMESTAMPTZ,
    version           BIGINT       NOT NULL,

    CONSTRAINT uq_article_revision_number UNIQUE (article_id, revision_number),
    CONSTRAINT ck_article_revision_cover_dimensions CHECK (
        (cover_reference IS NULL AND cover_width IS NULL AND cover_height IS NULL AND cover_alt IS NULL)
        OR (cover_reference IS NOT NULL AND cover_width > 0 AND cover_height > 0 AND cover_alt IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_article_revisions_article_updated
    ON article_revisions (article_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS article_revision_sections (
    section_id   UUID PRIMARY KEY,
    revision_id  UUID         NOT NULL REFERENCES article_revisions(revision_id) ON DELETE CASCADE,
    position     INTEGER      NOT NULL,
    heading      VARCHAR(140) NOT NULL,

    CONSTRAINT uq_article_revision_section_position UNIQUE (revision_id, position),
    CONSTRAINT ck_article_revision_section_position CHECK (position >= 0)
);

CREATE TABLE IF NOT EXISTS article_revision_paragraphs (
    paragraph_id UUID PRIMARY KEY,
    section_id   UUID         NOT NULL REFERENCES article_revision_sections(section_id) ON DELETE CASCADE,
    position     INTEGER      NOT NULL,
    body         TEXT         NOT NULL,

    CONSTRAINT uq_article_section_paragraph_position UNIQUE (section_id, position),
    CONSTRAINT ck_article_revision_paragraph_position CHECK (position >= 0)
);

CREATE TABLE IF NOT EXISTS article_revision_images (
    image_id           UUID PRIMARY KEY,
    revision_id        UUID         NOT NULL REFERENCES article_revisions(revision_id) ON DELETE CASCADE,
    section_id         UUID         REFERENCES article_revision_sections(section_id) ON DELETE CASCADE,
    position           INTEGER      NOT NULL,
    storage_reference  TEXT         NOT NULL,
    width              INTEGER      NOT NULL,
    height             INTEGER      NOT NULL,
    alt                TEXT         NOT NULL,
    source             VARCHAR(64),
    attribution        TEXT,

    CONSTRAINT uq_article_revision_image_position UNIQUE (revision_id, section_id, position),
    CONSTRAINT ck_article_revision_image_position CHECK (position >= 0),
    CONSTRAINT ck_article_revision_image_dimensions CHECK (width > 0 AND height > 0)
);

CREATE INDEX IF NOT EXISTS idx_article_revision_images_revision
    ON article_revision_images (revision_id, section_id, position);

CREATE TABLE IF NOT EXISTS article_revision_tags (
    revision_id UUID         NOT NULL REFERENCES article_revisions(revision_id) ON DELETE CASCADE,
    position    INTEGER      NOT NULL,
    tag         VARCHAR(80)  NOT NULL,

    PRIMARY KEY (revision_id, position),
    CONSTRAINT uq_article_revision_tag_value UNIQUE (revision_id, tag),
    CONSTRAINT ck_article_revision_tag_position CHECK (position >= 0)
);

CREATE TABLE IF NOT EXISTS articles_projection (
                                     id               UUID PRIMARY KEY,
                                     slug             VARCHAR(255) NOT NULL,
                                     locale           VARCHAR(20)  NOT NULL,

                                     title            TEXT         NOT NULL,
                                     intro            TEXT         NOT NULL,
                                     blocks_json      TEXT         NOT NULL, -- Array<ArticleBlockView> en JSON
                                     conclusion       TEXT         NOT NULL,

                                     cover_json       TEXT,                 -- ImageRefView en JSON
                                     tags_json        TEXT         NOT NULL, -- string[]

                                     author_id        UUID         NOT NULL,
                                     author_name      VARCHAR(255) NOT NULL,

                                     reading_time_min INT          NOT NULL,

                                     published_at     TIMESTAMPTZ  NOT NULL,
                                     updated_at       TIMESTAMPTZ  NOT NULL,

                                     version          BIGINT       NOT NULL,
                                     status           VARCHAR(32)  NOT NULL, -- "published", "draft", "archived"

                                     coffee_ids_json  TEXT         NOT NULL  -- UUID[] sérialisés en JSON
);

--
-- CREATE INDEX idx_articles_projection_slug_locale
--     ON articles_projection (slug, locale);
--
CREATE INDEX IF NOT EXISTS idx_articles_projection_public_page
    ON articles_projection (locale, status, published_at DESC, id DESC);



-- -- Index utiles pour le dispatcher (batch sur PENDING, dans l'ordre d'id)
-- CREATE INDEX idx_outbox_events_status_id
--     ON outbox_events (status, id);
--
-- -- Optionnel : routing / replays par stream
-- CREATE INDEX idx_outbox_events_stream_key_id
--     ON outbox_events (stream_key, id);

CREATE TABLE IF NOT EXISTS auth_users (
                                          id               UUID PRIMARY KEY,
                                          provider         VARCHAR(32)      NOT NULL, -- "GOOGLE"
                                          provider_user_id VARCHAR(255)     NOT NULL, -- Google sub
                                          email            VARCHAR(255)     NOT NULL,
                                          email_verified   BOOLEAN          NOT NULL,
                                          display_name     VARCHAR(255),
                                          avatar_url       VARCHAR(512),
                                          last_login_at    TIMESTAMPTZ      NOT NULL
);
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_users_provider_user
    ON auth_users (provider, provider_user_id);

CREATE TABLE IF NOT EXISTS admin_user_access (
    user_id UUID PRIMARY KEY REFERENCES auth_users(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL,
    granted_by UUID REFERENCES auth_users(id)
);

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES auth_users(id),
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL DEFAULT 'USER',
    target_id UUID,
    target_user_id UUID,
    command_id UUID,
    outcome VARCHAR(32) NOT NULL,
    reason VARCHAR(240),
    occurred_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE admin_audit_log ADD COLUMN IF NOT EXISTS target_type VARCHAR(64) NOT NULL DEFAULT 'USER';
ALTER TABLE admin_audit_log ADD COLUMN IF NOT EXISTS target_id UUID;
ALTER TABLE admin_audit_log ADD COLUMN IF NOT EXISTS command_id UUID;
ALTER TABLE admin_audit_log ADD COLUMN IF NOT EXISTS reason VARCHAR(240);

CREATE INDEX IF NOT EXISTS ix_admin_audit_log_occurred_at ON admin_audit_log (occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_admin_audit_log_target_occurred_at ON admin_audit_log (target_type, target_id, occurred_at DESC);


CREATE TABLE IF NOT EXISTS app_users (
                                         id            UUID PRIMARY KEY,
                                         auth_user_id  UUID           NOT NULL REFERENCES auth_users(id),
                                         display_name  VARCHAR(255)   NOT NULL,
                                         created_at    TIMESTAMPTZ    NOT NULL
);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_url varchar(512);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS lifecycle_status varchar(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS deletion_requested_at timestamptz;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS deleted_at timestamptz;

CREATE TABLE IF NOT EXISTS account_deletion_processes (
    request_id uuid primary key,
    user_id uuid not null unique,
    requested_at timestamptz not null,
    status varchar(32) not null,
    acknowledgements text not null,
    completed_at timestamptz,
    version bigint not null
);

CREATE INDEX IF NOT EXISTS ix_app_users_auth_user_id
    ON app_users (auth_user_id);

CREATE TABLE IF NOT EXISTS saved_coffees (
                                             saved_coffee_id UUID PRIMARY KEY,
                                             user_id UUID NOT NULL REFERENCES app_users(id),
                                             coffee_id UUID NOT NULL,
                                             active BOOLEAN NOT NULL,
                                             updated_at TIMESTAMPTZ NOT NULL,
                                             version BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_saved_coffees_user_coffee
    ON saved_coffees (user_id, coffee_id);

CREATE INDEX IF NOT EXISTS ix_saved_coffees_user_active
    ON saved_coffees (user_id, active);

CREATE TABLE IF NOT EXISTS user_saved_coffees_projection (
                                                             saved_coffee_id UUID PRIMARY KEY,
                                                             user_id UUID NOT NULL,
                                                             coffee_id UUID NOT NULL,
                                                             active BOOLEAN NOT NULL,
                                                             version BIGINT NOT NULL,
                                                             updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_saved_coffees_projection_user_coffee
    ON user_saved_coffees_projection (user_id, coffee_id);

CREATE INDEX IF NOT EXISTS ix_user_saved_coffees_projection_user_active
    ON user_saved_coffees_projection (user_id, active, updated_at DESC);

CREATE INDEX IF NOT EXISTS ix_user_saved_coffees_projection_coffee_active
    ON user_saved_coffees_projection (coffee_id, active);

CREATE TABLE IF NOT EXISTS user_saved_coffee_cafes_projection (
                                                                  coffee_id UUID PRIMARY KEY,
                                                                  name TEXT NOT NULL,
                                                                  address_line1 TEXT,
                                                                  city TEXT,
                                                                  postal_code TEXT,
                                                                  country TEXT,
                                                                  archived BOOLEAN NOT NULL DEFAULT false,
                                                                  version BIGINT NOT NULL,
                                                                  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id         UUID PRIMARY KEY,
                                              user_id    UUID        NOT NULL,
                                              token      VARCHAR(512) NOT NULL,
                                              expires_at TIMESTAMPTZ NOT NULL,
                                              revoked    BOOLEAN      NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_token
    ON refresh_tokens (token);

CREATE TABLE IF NOT EXISTS auth_provider_credentials (
    user_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(user_id, provider)
);

create table if not exists tickets (
                                       ticket_id uuid primary key,
                                       user_id uuid not null,

                                       status varchar(32) not null,

                                       ocr_text text null,
                                       image_ref text null,

                                       amount_cents integer null,
                                       currency varchar(8) not null,

                                       ticket_date timestamptz null,

                                       merchant_name text null,
                                       merchant_address text null,
                                       payment_method text null,

                                       line_items_json text null,

                                       rejection_reason text null,

                                       created_at timestamptz not null,
                                       updated_at timestamptz not null,

                                       version bigint not null
);

create table if not exists ticket_submission_fingerprints (
    fingerprint varchar(64) primary key,
    ticket_id uuid not null unique,
    user_id uuid not null,
    created_at timestamptz not null
);
insert into ticket_submission_fingerprints(fingerprint,ticket_id,user_id,created_at)
select fingerprint,ticket_id,user_id,created_at from (
    select 'v1:' || md5(lower(regexp_replace(btrim(ocr_text), '\s+', ' ', 'g'))) fingerprint,
           ticket_id,user_id,created_at,
           row_number() over (
             partition by md5(lower(regexp_replace(btrim(ocr_text), '\s+', ' ', 'g')))
             order by created_at,ticket_id) ordinal
    from tickets where ocr_text is not null and btrim(ocr_text) <> ''
) canonical where ordinal=1
on conflict do nothing;

create table if not exists ticket_status_projection (
                                                        ticket_id uuid primary key,
                                                        history_position bigint generated by default as identity unique,
                                                        user_id uuid not null,

                                                        status varchar(32) not null,
                                                        outcome varchar(32) null,

                                                        image_ref text null,
                                                        ocr_text text null,

                                                        amount_cents integer null,
                                                        currency varchar(8) null,
                                                        ticket_date timestamptz null,

                                                        merchant_name text null,
                                                        merchant_address text null,
                                                        payment_method text null,

                                                        rejection_reason text null,

                                                        version bigint not null,
                                                        occurred_at timestamptz not null
);

alter table ticket_status_projection
    add column if not exists history_position bigint generated by default as identity;

create index if not exists idx_ticket_status_user on ticket_status_projection(user_id);
create index if not exists idx_ticket_status_status on ticket_status_projection(status);
create unique index if not exists idx_ticket_status_history_position
    on ticket_status_projection(history_position);
create index if not exists idx_ticket_status_user_history
    on ticket_status_projection(user_id, history_position desc);

create table if not exists user_entitlements_projection (
                                                            user_id uuid primary key,
                                                            confirmed_tickets integer not null,
                                                            version bigint not null,
                                                            updated_at timestamptz not null
);

-- Pass belongs to userApplicationContext. These tables are local contributions
-- fed by integration events; normal reads never join another bounded context.
create table if not exists pass_ticket_contributions (
    ticket_id uuid primary key,
    user_id uuid not null,
    active boolean not null,
    source_version bigint not null,
    updated_at timestamptz not null
);
create index if not exists idx_pass_ticket_contributions_user_active
    on pass_ticket_contributions(user_id, active);

create table if not exists pass_experience_contributions (
    experience_id uuid primary key,
    user_id uuid not null,
    coffee_id uuid not null,
    active boolean not null,
    source_version bigint not null,
    updated_at timestamptz not null
);
create index if not exists idx_pass_experience_contributions_user_active
    on pass_experience_contributions(user_id, active);

create table if not exists user_pass_projection (
    user_id uuid primary key,
    policy_version integer not null,
    published_experiences integer not null,
    distinct_experienced_coffees integer not null,
    validated_tickets integer not null,
    acquired_levels text not null,
    version bigint not null,
    updated_at timestamptz not null
);

insert into pass_ticket_contributions(ticket_id,user_id,active,source_version,updated_at)
select ticket_id,user_id,(status='CONFIRMED'),version,updated_at
from tickets
where ocr_text is null or btrim(ocr_text) = ''
   or exists (select 1 from ticket_submission_fingerprints fingerprint
              where fingerprint.ticket_id=tickets.ticket_id)
on conflict(ticket_id) do nothing;

with legacy as (
    select contribution.user_id,
           count(*) filter (where contribution.active) :: integer as tickets,
           (select count(*) from social_comments_projection comment
             where comment.author_id=contribution.user_id and comment.deleted_at is null
               and comment.moderation='PUBLISHED') :: integer as comments,
           (select count(*) from social_likes_projection liked
             where liked.user_id=contribution.user_id and liked.active=true) :: integer as likes,
           max(contribution.updated_at) as updated_at
    from pass_ticket_contributions contribution
    group by contribution.user_id
)
insert into user_pass_projection(user_id,policy_version,published_experiences,
    distinct_experienced_coffees,validated_tickets,acquired_levels,version,updated_at)
select user_id,2,0,0,tickets,
       case
         when tickets>=10 and comments>=5 and likes>=5 then 'COFFEE_TASTER,URBAN_EXPLORER,SOCIAL_BEAN,FRAGMENTS_MASTER'
         when tickets>=5 and comments>=3 then 'COFFEE_TASTER,URBAN_EXPLORER'
         when tickets>=3 then 'COFFEE_TASTER'
         else ''
       end,
       1,updated_at
from legacy
on conflict(user_id) do nothing;

-- Durable process-manager state for long-running Studio article authoring.
-- The saga is coordination state; article content remains owned by articleContext.
create table if not exists article_authoring_sagas (
    saga_id uuid primary key,
    article_id uuid not null,
    revision_id uuid not null,
    theme text not null,
    trigger varchar(32) not null,
    state varchar(40) not null,
    version bigint not null,
    generation_attempts integer not null default 0,
    lease_owner varchar(128),
    lease_until timestamptz,
    failure_category varchar(32),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint article_authoring_saga_version_ck check (version >= 0),
    constraint article_authoring_saga_attempts_ck check (generation_attempts >= 0)
);
create unique index if not exists uq_article_authoring_saga_revision on article_authoring_sagas(revision_id);
create index if not exists idx_article_authoring_saga_state on article_authoring_sagas(state, updated_at);
create index if not exists idx_article_authoring_saga_lease on article_authoring_sagas(lease_until);

-- One immutable attempt record per saga attempt; provider payload is not stored here.
create table if not exists article_generation_runs (
    run_id uuid primary key,
    saga_id uuid not null references article_authoring_sagas(saga_id),
    attempt integer not null,
    worker_id varchar(128) not null,
    status varchar(16) not null,
    provider_response_id varchar(256),
    provider varchar(64),
    model varchar(128),
    schema_version varchar(64),
    failure_category varchar(32),
    started_at timestamptz not null,
    completed_at timestamptz,
    constraint article_generation_run_attempt_ck check (attempt > 0),
    constraint article_generation_run_status_ck check (status in ('STARTED','SUCCEEDED','FAILED')),
    unique (saga_id, attempt)
);
create index if not exists idx_article_generation_run_saga on article_generation_runs(saga_id, attempt);

-- Normalized provider output awaiting relational revision materialisation.
create table if not exists article_generation_artifacts (
    run_id uuid primary key references article_generation_runs(run_id),
    saga_id uuid not null references article_authoring_sagas(saga_id),
    article_id uuid not null,
    revision_id uuid not null,
    schema_version varchar(64) not null,
    draft_json text not null,
    created_at timestamptz not null
);
create unique index if not exists uq_article_generation_artifact_revision on article_generation_artifacts(revision_id);

-- Single-use, revision-bound publication approvals. Only the token hash is stored.
create table if not exists article_review_approvals (
    approval_id uuid primary key,
    saga_id uuid not null references article_authoring_sagas(saga_id),
    article_id uuid not null,
    revision_id uuid not null,
    token_hash varchar(128) not null unique,
    expires_at timestamptz not null,
    consumed_at timestamptz,
    created_at timestamptz not null,
    constraint article_review_approval_expiry_ck check (expires_at > created_at)
);
create unique index if not exists uq_article_review_approval_revision
    on article_review_approvals(saga_id, revision_id);
create index if not exists idx_article_review_approval_expiry
    on article_review_approvals(expires_at, consumed_at);

-- Editorial intelligence owns source cadence and provider-neutral checkpoints.
create table if not exists editorial_sources (
    source_id uuid primary key,
    name varchar(255) not null,
    access_mode varchar(32) not null,
    authority_level varchar(32) not null,
    endpoint text not null,
    polling_frequency_seconds bigint not null check (polling_frequency_seconds > 0),
    enabled boolean not null,
    status varchar(32) not null,
    last_checked_at timestamptz,
    last_successful_check_at timestamptz,
    next_check_at timestamptz not null,
    failure_count integer not null default 0 check (failure_count >= 0),
    lease_owner varchar(128),
    lease_until timestamptz,
    checkpoint_etag varchar(512),
    checkpoint_last_modified varchar(512),
    checkpoint_external_id varchar(512),
    checkpoint_published_at timestamptz,
    version bigint not null check (version >= 0)
);
alter table editorial_sources add column if not exists checkpoint_last_modified varchar(512);
create index if not exists idx_editorial_sources_due on editorial_sources(enabled, next_check_at);

create table if not exists editorial_source_signals (
    signal_id uuid primary key,
    source_id uuid not null references editorial_sources(source_id),
    external_id varchar(512) not null,
    title text not null,
    summary text,
    url text not null,
    author varchar(512),
    published_at timestamptz,
    discovered_at timestamptz not null,
    fingerprint varchar(128) not null,
    status varchar(32) not null default 'NEW',
    unique (source_id, external_id)
);
create index if not exists idx_editorial_source_signals_new on editorial_source_signals(status, discovered_at);

-- Analysis output remains separate from article authoring until a human retains it.
create table if not exists editorial_topic_candidates (
    topic_candidate_id uuid primary key,
    subject text not null,
    suggested_angle text not null,
    signal_ids_json text not null,
    detected_at timestamptz not null,
    status varchar(32) not null
);
create index if not exists idx_editorial_topic_candidates_status on editorial_topic_candidates(status, detected_at desc);

create table if not exists editorial_generation_executions (
    execution_id uuid primary key,
    operation varchar(64) not null,
    model varchar(128) not null,
    input_tokens integer not null check (input_tokens >= 0),
    output_tokens integer not null check (output_tokens >= 0),
    estimated_cost numeric(12,6) not null check (estimated_cost >= 0),
    duration_millis bigint not null check (duration_millis >= 0),
    outcome varchar(32) not null,
    occurred_at timestamptz not null
);
create index if not exists idx_editorial_generation_executions_occurred on editorial_generation_executions(occurred_at desc);

-- Editorial planning is a durable intent. Scheduler only advances due intents.
create table if not exists editorial_publication_schedule (
    schedule_id uuid primary key,
    article_id uuid not null,
    revision_id uuid,
    operation varchar(32) not null check (operation in ('PUBLISH','ARCHIVE')),
    due_at timestamptz not null,
    status varchar(32) not null check (status in ('SCHEDULED','CLAIMED','DISPATCHED','COMPLETED','REJECTED','CANCELLED')),
    lease_owner varchar(128), lease_until timestamptz,
    rejection_reason text,
    created_at timestamptz not null,
    version bigint not null default 0
);
alter table editorial_publication_schedule add column if not exists revision_id uuid;
alter table editorial_publication_schedule add column if not exists rejection_reason text;
alter table editorial_publication_schedule drop constraint if exists editorial_publication_schedule_status_check;
alter table editorial_publication_schedule add constraint editorial_publication_schedule_status_check
    check (status in ('SCHEDULED','CLAIMED','DISPATCHED','COMPLETED','REJECTED','CANCELLED'));
create index if not exists idx_editorial_publication_schedule_due on editorial_publication_schedule(status,due_at);
create unique index if not exists uq_editorial_publication_schedule_active_operation
    on editorial_publication_schedule(article_id, operation)
    where status in ('SCHEDULED','CLAIMED','DISPATCHED');
