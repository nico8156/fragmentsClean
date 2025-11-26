create table coffees (
    id UUID primary key,
    google_id varchar(255) not null,
    display_name varchar(255) not null,
    formatted_address varchar(255) not null,
    national_phone_number varchar(255),
    website_uri varchar(255),
    latitude double precision not null,
    longitude double precision not null
);

CREATE TABLE likes (
                       like_id   UUID PRIMARY KEY,
                       user_id   UUID        NOT NULL,
                       target_id UUID        NOT NULL,
                       active    BOOLEAN     NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL,
                       version   BIGINT      NOT NULL
);

CREATE TABLE outbox_events (
                               id           BIGSERIAL PRIMARY KEY,
                               event_id     VARCHAR(100) NOT NULL UNIQUE,
                               event_type   VARCHAR(100) NOT NULL,
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id VARCHAR(100) NOT NULL,
                               stream_key   VARCHAR(200) NOT NULL,
                               payload_json TEXT NOT NULL,
                               occurred_at  TIMESTAMPTZ NOT NULL,
                               created_at   TIMESTAMPTZ NOT NULL,
                               status       VARCHAR(50) NOT NULL,
                               retry_count  INTEGER NOT NULL
);
