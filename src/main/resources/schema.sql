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
                                       id                  UUID PRIMARY KEY,
                                       google_id           VARCHAR(255) NOT NULL,
                                       display_name        VARCHAR(255) NOT NULL,
                                       formatted_address   VARCHAR(255) NOT NULL,
                                       national_phone_number VARCHAR(255),
                                       website_uri         VARCHAR(255),
                                       latitude            DOUBLE PRECISION NOT NULL,
                                       longitude           DOUBLE PRECISION NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_coffees_google_id
    ON coffees (google_id);


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

-- -- Index utiles pour le dispatcher (batch sur PENDING, dans l'ordre d'id)
-- CREATE INDEX idx_outbox_events_status_id
--     ON outbox_events (status, id);
--
-- -- Optionnel : routing / replays par stream
-- CREATE INDEX idx_outbox_events_stream_key_id
--     ON outbox_events (stream_key, id);

