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
                         updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

-- -- Optionnel, mais bien pratique :
-- CREATE UNIQUE INDEX ux_coffees_google_place_id
--     ON coffees (google_place_id)
--     WHERE google_place_id IS NOT NULL;
--
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
                          version           BIGINT         NOT NULL
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
-- CREATE INDEX idx_articles_projection_published_at_desc
--     ON articles_projection (published_at DESC, id DESC);



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
                                          last_login_at    TIMESTAMPTZ      NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_users_provider_user
    ON auth_users (provider, provider_user_id);


CREATE TABLE IF NOT EXISTS app_users (
                                         id            UUID PRIMARY KEY,
                                         auth_user_id  UUID           NOT NULL REFERENCES auth_users(id),
                                         display_name  VARCHAR(255)   NOT NULL,
                                         created_at    TIMESTAMPTZ    NOT NULL
);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_url varchar(512);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS ix_app_users_auth_user_id
    ON app_users (auth_user_id);

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id         UUID PRIMARY KEY,
                                              user_id    UUID        NOT NULL,
                                              token      VARCHAR(512) NOT NULL,
                                              expires_at TIMESTAMPTZ NOT NULL,
                                              revoked    BOOLEAN      NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_token
    ON refresh_tokens (token);

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

create index if not exists idx_tickets_user_id on tickets(user_id);
create index if not exists idx_tickets_status on tickets(status);
