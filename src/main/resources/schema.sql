-- =========================================================
--  SCHEMA DE BASE
-- =========================================================

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


-- Outbox (event streaming)
CREATE TABLE IF NOT EXISTS outbox_events (
                                             id             BIGSERIAL PRIMARY KEY,
                                             event_id       VARCHAR(100) NOT NULL UNIQUE,
                                             event_type     VARCHAR(100) NOT NULL,
                                             aggregate_type VARCHAR(100) NOT NULL,
                                             aggregate_id   VARCHAR(100) NOT NULL,
                                             stream_key     VARCHAR(200) NOT NULL,
                                             payload_json   TEXT NOT NULL,
                                             occurred_at    TIMESTAMPTZ NOT NULL,
                                             created_at     TIMESTAMPTZ NOT NULL,
                                             status         VARCHAR(50) NOT NULL,
                                             retry_count    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created_at
    ON outbox_events (status, created_at);


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
