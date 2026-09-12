-- Schema-only snapshot of Fragments staging on 2026-09-12, PostgreSQL 15.19.
-- No application rows, sequence values, credentials or owners are included.
-- Legacy tables are retained solely to prove non-destructive upgrades.
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

CREATE TABLE public.admin_audit_log (
    id uuid NOT NULL,
    actor_user_id uuid NOT NULL,
    action character varying(64) NOT NULL,
    target_type character varying(64) DEFAULT 'USER'::character varying NOT NULL,
    target_id uuid,
    target_user_id uuid,
    command_id uuid,
    outcome character varying(32) NOT NULL,
    reason character varying(240),
    occurred_at timestamp with time zone NOT NULL
);

CREATE TABLE public.admin_studio_articles (
    article_id uuid NOT NULL,
    status character varying(32) NOT NULL,
    payload_json text NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    published_at timestamp with time zone,
    deleted_at timestamp with time zone,
    last_command_id uuid
);

CREATE TABLE public.admin_user_access (
    user_id uuid NOT NULL,
    granted_at timestamp with time zone NOT NULL,
    granted_by uuid
);

CREATE TABLE public.app_users (
    id uuid NOT NULL,
    auth_user_id uuid NOT NULL,
    display_name character varying(255) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    avatar_url character varying(512),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);

CREATE TABLE public.article_authoring_sagas (
    saga_id uuid NOT NULL,
    article_id uuid NOT NULL,
    revision_id uuid NOT NULL,
    theme text NOT NULL,
    trigger character varying(32) NOT NULL,
    state character varying(40) NOT NULL,
    version bigint NOT NULL,
    generation_attempts integer DEFAULT 0 NOT NULL,
    lease_owner character varying(128),
    lease_until timestamp with time zone,
    failure_category character varying(32),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT article_authoring_saga_attempts_ck CHECK ((generation_attempts >= 0)),
    CONSTRAINT article_authoring_saga_version_ck CHECK ((version >= 0))
);

CREATE TABLE public.article_generation_artifacts (
    run_id uuid NOT NULL,
    saga_id uuid NOT NULL,
    article_id uuid NOT NULL,
    revision_id uuid NOT NULL,
    schema_version character varying(64) NOT NULL,
    draft_json text NOT NULL,
    created_at timestamp with time zone NOT NULL
);

CREATE TABLE public.article_generation_runs (
    run_id uuid NOT NULL,
    saga_id uuid NOT NULL,
    attempt integer NOT NULL,
    worker_id character varying(128) NOT NULL,
    status character varying(16) NOT NULL,
    provider_response_id character varying(256),
    provider character varying(64),
    model character varying(128),
    schema_version character varying(64),
    failure_category character varying(32),
    started_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone,
    CONSTRAINT article_generation_run_attempt_ck CHECK ((attempt > 0)),
    CONSTRAINT article_generation_run_status_ck CHECK (((status)::text = ANY ((ARRAY['STARTED'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE public.article_review_approvals (
    approval_id uuid NOT NULL,
    saga_id uuid NOT NULL,
    article_id uuid NOT NULL,
    revision_id uuid NOT NULL,
    token_hash character varying(128) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT article_review_approval_expiry_ck CHECK ((expires_at > created_at))
);

CREATE TABLE public.article_revision_images (
    image_id uuid NOT NULL,
    revision_id uuid NOT NULL,
    section_id uuid,
    "position" integer NOT NULL,
    storage_reference text NOT NULL,
    width integer NOT NULL,
    height integer NOT NULL,
    alt text NOT NULL,
    source character varying(64),
    attribution text,
    CONSTRAINT ck_article_revision_image_dimensions CHECK (((width > 0) AND (height > 0))),
    CONSTRAINT ck_article_revision_image_position CHECK (("position" >= 0))
);

CREATE TABLE public.article_revision_paragraphs (
    paragraph_id uuid NOT NULL,
    section_id uuid NOT NULL,
    "position" integer NOT NULL,
    body text NOT NULL,
    CONSTRAINT ck_article_revision_paragraph_position CHECK (("position" >= 0))
);

CREATE TABLE public.article_revision_sections (
    section_id uuid NOT NULL,
    revision_id uuid NOT NULL,
    "position" integer NOT NULL,
    heading character varying(140) NOT NULL,
    CONSTRAINT ck_article_revision_section_position CHECK (("position" >= 0))
);

CREATE TABLE public.article_revision_tags (
    revision_id uuid NOT NULL,
    "position" integer NOT NULL,
    tag character varying(80) NOT NULL,
    CONSTRAINT ck_article_revision_tag_position CHECK (("position" >= 0))
);

CREATE TABLE public.article_revisions (
    revision_id uuid NOT NULL,
    article_id uuid NOT NULL,
    revision_number integer NOT NULL,
    title text NOT NULL,
    introduction text NOT NULL,
    conclusion text NOT NULL,
    cover_reference text,
    cover_width integer,
    cover_height integer,
    cover_alt text,
    reading_time_min integer NOT NULL,
    status character varying(32) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    published_at timestamp with time zone,
    version bigint NOT NULL,
    CONSTRAINT ck_article_revision_cover_dimensions CHECK ((((cover_reference IS NULL) AND (cover_width IS NULL) AND (cover_height IS NULL) AND (cover_alt IS NULL)) OR ((cover_reference IS NOT NULL) AND (cover_width > 0) AND (cover_height > 0) AND (cover_alt IS NOT NULL))))
);

CREATE TABLE public.articles (
    article_id uuid NOT NULL,
    slug character varying(255) NOT NULL,
    locale character varying(20) NOT NULL,
    author_id uuid NOT NULL,
    author_name character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    intro text NOT NULL,
    blocks_json text NOT NULL,
    conclusion text NOT NULL,
    cover_url text,
    cover_width integer,
    cover_height integer,
    cover_alt text,
    tags_json text NOT NULL,
    reading_time_min integer NOT NULL,
    coffee_ids_json text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    published_at timestamp with time zone,
    status character varying(32) NOT NULL,
    version bigint NOT NULL,
    working_revision_id uuid,
    published_revision_id uuid
);

CREATE TABLE public.articles_projection (
    id uuid NOT NULL,
    slug character varying(255) NOT NULL,
    locale character varying(20) NOT NULL,
    title text NOT NULL,
    intro text NOT NULL,
    blocks_json text NOT NULL,
    conclusion text NOT NULL,
    cover_json text,
    tags_json text NOT NULL,
    author_id uuid NOT NULL,
    author_name character varying(255) NOT NULL,
    reading_time_min integer NOT NULL,
    published_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    version bigint NOT NULL,
    status character varying(32) NOT NULL,
    coffee_ids_json text NOT NULL
);

CREATE TABLE public.auth_users (
    id uuid NOT NULL,
    provider character varying(32) NOT NULL,
    provider_user_id character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    email_verified boolean NOT NULL,
    display_name character varying(255),
    avatar_url character varying(512),
    last_login_at timestamp with time zone NOT NULL
);

CREATE TABLE public.coffee_opening_hours (
    coffee_id uuid NOT NULL,
    day_code integer NOT NULL,
    start_minute integer NOT NULL,
    end_minute integer NOT NULL,
    CONSTRAINT coffee_opening_hours_check CHECK ((start_minute < end_minute)),
    CONSTRAINT coffee_opening_hours_day_code_check CHECK (((day_code >= 0) AND (day_code <= 6))),
    CONSTRAINT coffee_opening_hours_end_minute_check CHECK (((end_minute >= 1) AND (end_minute <= 1440))),
    CONSTRAINT coffee_opening_hours_start_minute_check CHECK (((start_minute >= 0) AND (start_minute <= 1439)))
);

CREATE TABLE public.coffee_openinghours_projection (
    id uuid NOT NULL,
    coffee_id uuid NOT NULL,
    weekday_description character varying(255) NOT NULL
);

CREATE TABLE public.coffee_photos (
    coffee_id uuid NOT NULL,
    photo_id uuid NOT NULL,
    photo_uri character varying(2000) NOT NULL,
    is_cover boolean DEFAULT false NOT NULL,
    sort_order integer NOT NULL
);

CREATE TABLE public.coffee_photos_projection (
    id uuid NOT NULL,
    coffee_id uuid NOT NULL,
    photo_uri character varying(2000) NOT NULL,
    is_cover boolean DEFAULT false NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);

CREATE TABLE public.coffee_projection_checkpoints (
    coffee_id uuid NOT NULL,
    latest_version bigint NOT NULL,
    publication_status character varying(32) NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    changed_at timestamp with time zone NOT NULL
);

CREATE TABLE public.coffee_summaries_projection (
    id uuid NOT NULL,
    google_place_id character varying(255),
    name character varying(255) NOT NULL,
    address_line1 character varying(255),
    city character varying(255),
    postal_code character varying(32),
    country character varying(8),
    lat double precision NOT NULL,
    lon double precision NOT NULL,
    phone_number character varying(64),
    website character varying(512),
    tags_json jsonb,
    rating numeric(3,1),
    version integer NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    publication_status character varying(32) DEFAULT 'PUBLISHED'::character varying NOT NULL
);

CREATE TABLE public.coffees (
    id uuid NOT NULL,
    google_place_id character varying(255),
    name character varying(255) NOT NULL,
    address_line1 character varying(255),
    city character varying(255),
    postal_code character varying(32),
    country character varying(8),
    lat double precision NOT NULL,
    lon double precision NOT NULL,
    phone_number character varying(64),
    website character varying(512),
    tags_csv text,
    version integer NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    archived_at timestamp with time zone,
    publication_status character varying(32) DEFAULT 'PUBLISHED'::character varying NOT NULL
);

CREATE TABLE public.command_status (
    command_id uuid NOT NULL,
    status character varying(32) NOT NULL,
    aggregate_type character varying(100),
    aggregate_id character varying(100),
    event_id character varying(50),
    event_type character varying(255),
    applied_at timestamp with time zone,
    rejected_at timestamp with time zone,
    reason text,
    updated_at timestamp with time zone NOT NULL
);

CREATE TABLE public.comments (
    comment_id uuid NOT NULL,
    target_id uuid NOT NULL,
    author_id uuid NOT NULL,
    parent_id uuid,
    body character varying(4000) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    edited_at timestamp without time zone,
    deleted_at timestamp without time zone,
    moderation character varying(32) NOT NULL,
    version bigint NOT NULL
);

CREATE TABLE public.editorial_generation_executions (
    execution_id uuid NOT NULL,
    operation character varying(64) NOT NULL,
    model character varying(128) NOT NULL,
    input_tokens integer NOT NULL,
    output_tokens integer NOT NULL,
    estimated_cost numeric(12,6) NOT NULL,
    duration_millis bigint NOT NULL,
    outcome character varying(32) NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    CONSTRAINT editorial_generation_executions_duration_millis_check CHECK ((duration_millis >= 0)),
    CONSTRAINT editorial_generation_executions_estimated_cost_check CHECK ((estimated_cost >= (0)::numeric)),
    CONSTRAINT editorial_generation_executions_input_tokens_check CHECK ((input_tokens >= 0)),
    CONSTRAINT editorial_generation_executions_output_tokens_check CHECK ((output_tokens >= 0))
);

CREATE TABLE public.editorial_publication_schedule (
    schedule_id uuid NOT NULL,
    article_id uuid NOT NULL,
    revision_id uuid,
    operation character varying(32) NOT NULL,
    due_at timestamp with time zone NOT NULL,
    status character varying(32) NOT NULL,
    lease_owner character varying(128),
    lease_until timestamp with time zone,
    rejection_reason text,
    created_at timestamp with time zone NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT editorial_publication_schedule_operation_check CHECK (((operation)::text = ANY ((ARRAY['PUBLISH'::character varying, 'ARCHIVE'::character varying])::text[]))),
    CONSTRAINT editorial_publication_schedule_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'CLAIMED'::character varying, 'DISPATCHED'::character varying, 'COMPLETED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE public.editorial_source_signals (
    signal_id uuid NOT NULL,
    source_id uuid NOT NULL,
    external_id character varying(512) NOT NULL,
    title text NOT NULL,
    summary text,
    url text NOT NULL,
    author character varying(512),
    published_at timestamp with time zone,
    discovered_at timestamp with time zone NOT NULL,
    fingerprint character varying(128) NOT NULL,
    status character varying(32) DEFAULT 'NEW'::character varying NOT NULL
);

CREATE TABLE public.editorial_sources (
    source_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    access_mode character varying(32) NOT NULL,
    authority_level character varying(32) NOT NULL,
    endpoint text NOT NULL,
    polling_frequency_seconds bigint NOT NULL,
    enabled boolean NOT NULL,
    status character varying(32) NOT NULL,
    last_checked_at timestamp with time zone,
    last_successful_check_at timestamp with time zone,
    next_check_at timestamp with time zone NOT NULL,
    failure_count integer DEFAULT 0 NOT NULL,
    lease_owner character varying(128),
    lease_until timestamp with time zone,
    checkpoint_etag character varying(512),
    checkpoint_last_modified character varying(512),
    checkpoint_external_id character varying(512),
    checkpoint_published_at timestamp with time zone,
    version bigint NOT NULL,
    CONSTRAINT editorial_sources_failure_count_check CHECK ((failure_count >= 0)),
    CONSTRAINT editorial_sources_polling_frequency_seconds_check CHECK ((polling_frequency_seconds > 0)),
    CONSTRAINT editorial_sources_version_check CHECK ((version >= 0))
);

CREATE TABLE public.editorial_topic_candidates (
    topic_candidate_id uuid NOT NULL,
    subject text NOT NULL,
    suggested_angle text NOT NULL,
    signal_ids_json text NOT NULL,
    detected_at timestamp with time zone NOT NULL,
    status character varying(32) NOT NULL
);

CREATE TABLE public.identities (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    provider character varying(50) NOT NULL,
    provider_user_id character varying(255) NOT NULL,
    email character varying(255),
    created_at timestamp with time zone NOT NULL,
    last_auth_at timestamp with time zone
);

CREATE TABLE public.inbox_messages (
    id bigint NOT NULL,
    destination character varying(255) NOT NULL,
    event_id character varying(50) NOT NULL,
    event_type character varying(255) NOT NULL,
    event_version integer NOT NULL,
    received_at timestamp with time zone NOT NULL,
    processed_at timestamp with time zone,
    status character varying(32) NOT NULL,
    error_message text
);

CREATE SEQUENCE public.inbox_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.inbox_messages_id_seq OWNED BY public.inbox_messages.id;

CREATE TABLE public.likes (
    like_id uuid NOT NULL,
    user_id uuid NOT NULL,
    target_id uuid NOT NULL,
    active boolean NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    version bigint NOT NULL
);

CREATE TABLE public.outbox_events (
    id bigint NOT NULL,
    event_id character varying(50) NOT NULL,
    event_type character varying(255) NOT NULL,
    aggregate_type character varying(100) NOT NULL,
    aggregate_id character varying(100) NOT NULL,
    stream_key character varying(255) NOT NULL,
    payload_json text NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    status character varying(32) NOT NULL,
    retry_count integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.outbox_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.outbox_events_id_seq OWNED BY public.outbox_events.id;

CREATE TABLE public.projection_sync_events (
    id bigint NOT NULL,
    event_name character varying(100) NOT NULL,
    projection character varying(100),
    scope character varying(50),
    entity_id character varying(100),
    version bigint,
    changed_at timestamp with time zone NOT NULL,
    payload_json jsonb NOT NULL
);

CREATE SEQUENCE public.projection_sync_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.projection_sync_events_id_seq OWNED BY public.projection_sync_events.id;

CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token character varying(512) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked boolean NOT NULL
);

CREATE TABLE public.saved_coffees (
    saved_coffee_id uuid NOT NULL,
    user_id uuid NOT NULL,
    coffee_id uuid NOT NULL,
    active boolean NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    version bigint NOT NULL
);

CREATE TABLE public.social_comments_projection (
    id uuid NOT NULL,
    target_id uuid NOT NULL,
    author_id uuid NOT NULL,
    parent_id uuid,
    body character varying(4000) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    edited_at timestamp without time zone,
    deleted_at timestamp without time zone,
    moderation character varying(32) NOT NULL,
    like_count bigint DEFAULT 0 NOT NULL,
    reply_count bigint DEFAULT 0 NOT NULL,
    version bigint NOT NULL
);

CREATE TABLE public.social_likes_projection (
    like_id uuid NOT NULL,
    user_id uuid NOT NULL,
    target_id uuid NOT NULL,
    active boolean NOT NULL,
    version bigint NOT NULL,
    updated_at timestamp with time zone NOT NULL
);

CREATE TABLE public.ticket_status_projection (
    ticket_id uuid NOT NULL,
    user_id uuid NOT NULL,
    status character varying(32) NOT NULL,
    outcome character varying(32),
    image_ref text,
    ocr_text text,
    amount_cents integer,
    currency character varying(8),
    ticket_date timestamp with time zone,
    merchant_name text,
    merchant_address text,
    payment_method text,
    rejection_reason text,
    version bigint NOT NULL,
    occurred_at timestamp with time zone NOT NULL
);

CREATE TABLE public.tickets (
    ticket_id uuid NOT NULL,
    user_id uuid NOT NULL,
    status character varying(32) NOT NULL,
    ocr_text text,
    image_ref text,
    amount_cents integer,
    currency character varying(8) NOT NULL,
    ticket_date timestamp with time zone,
    merchant_name text,
    merchant_address text,
    payment_method text,
    line_items_json text,
    rejection_reason text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    version bigint NOT NULL
);

CREATE TABLE public.user_entitlements_projection (
    user_id uuid NOT NULL,
    confirmed_tickets integer NOT NULL,
    version bigint NOT NULL,
    updated_at timestamp with time zone NOT NULL
);

CREATE TABLE public.user_saved_coffee_cafes_projection (
    coffee_id uuid NOT NULL,
    name text NOT NULL,
    address_line1 text,
    city text,
    postal_code text,
    country text,
    archived boolean DEFAULT false NOT NULL,
    version bigint NOT NULL,
    updated_at timestamp with time zone NOT NULL
);

CREATE TABLE public.user_saved_coffees_projection (
    saved_coffee_id uuid NOT NULL,
    user_id uuid NOT NULL,
    coffee_id uuid NOT NULL,
    active boolean NOT NULL,
    version bigint NOT NULL,
    updated_at timestamp with time zone NOT NULL
);

CREATE TABLE public.user_social_projection (
    user_id uuid NOT NULL,
    display_name character varying(255) NOT NULL,
    avatar_url character varying(512),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    version bigint NOT NULL
);

CREATE TABLE public.users (
    user_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    display_name character varying(255) NOT NULL,
    avatar_url character varying(512),
    bio text,
    locale character varying(20) NOT NULL,
    version bigint NOT NULL
);

ALTER TABLE ONLY public.inbox_messages ALTER COLUMN id SET DEFAULT nextval('public.inbox_messages_id_seq'::regclass);

ALTER TABLE ONLY public.outbox_events ALTER COLUMN id SET DEFAULT nextval('public.outbox_events_id_seq'::regclass);

ALTER TABLE ONLY public.projection_sync_events ALTER COLUMN id SET DEFAULT nextval('public.projection_sync_events_id_seq'::regclass);

ALTER TABLE ONLY public.admin_audit_log
    ADD CONSTRAINT admin_audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.admin_studio_articles
    ADD CONSTRAINT admin_studio_articles_pkey PRIMARY KEY (article_id);

ALTER TABLE ONLY public.admin_user_access
    ADD CONSTRAINT admin_user_access_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.article_authoring_sagas
    ADD CONSTRAINT article_authoring_sagas_pkey PRIMARY KEY (saga_id);

ALTER TABLE ONLY public.article_generation_artifacts
    ADD CONSTRAINT article_generation_artifacts_pkey PRIMARY KEY (run_id);

ALTER TABLE ONLY public.article_generation_runs
    ADD CONSTRAINT article_generation_runs_pkey PRIMARY KEY (run_id);

ALTER TABLE ONLY public.article_generation_runs
    ADD CONSTRAINT article_generation_runs_saga_id_attempt_key UNIQUE (saga_id, attempt);

ALTER TABLE ONLY public.article_review_approvals
    ADD CONSTRAINT article_review_approvals_pkey PRIMARY KEY (approval_id);

ALTER TABLE ONLY public.article_review_approvals
    ADD CONSTRAINT article_review_approvals_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.article_revision_images
    ADD CONSTRAINT article_revision_images_pkey PRIMARY KEY (image_id);

ALTER TABLE ONLY public.article_revision_paragraphs
    ADD CONSTRAINT article_revision_paragraphs_pkey PRIMARY KEY (paragraph_id);

ALTER TABLE ONLY public.article_revision_sections
    ADD CONSTRAINT article_revision_sections_pkey PRIMARY KEY (section_id);

ALTER TABLE ONLY public.article_revision_tags
    ADD CONSTRAINT article_revision_tags_pkey PRIMARY KEY (revision_id, "position");

ALTER TABLE ONLY public.article_revisions
    ADD CONSTRAINT article_revisions_pkey PRIMARY KEY (revision_id);

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_pkey PRIMARY KEY (article_id);

ALTER TABLE ONLY public.articles_projection
    ADD CONSTRAINT articles_projection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.auth_users
    ADD CONSTRAINT auth_users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.coffee_opening_hours
    ADD CONSTRAINT coffee_opening_hours_pkey PRIMARY KEY (coffee_id, day_code, start_minute);

ALTER TABLE ONLY public.coffee_openinghours_projection
    ADD CONSTRAINT coffee_openinghours_projection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.coffee_photos
    ADD CONSTRAINT coffee_photos_coffee_id_sort_order_key UNIQUE (coffee_id, sort_order);

ALTER TABLE ONLY public.coffee_photos
    ADD CONSTRAINT coffee_photos_pkey PRIMARY KEY (coffee_id, photo_id);

ALTER TABLE ONLY public.coffee_photos_projection
    ADD CONSTRAINT coffee_photos_projection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.coffee_projection_checkpoints
    ADD CONSTRAINT coffee_projection_checkpoints_pkey PRIMARY KEY (coffee_id);

ALTER TABLE ONLY public.coffee_summaries_projection
    ADD CONSTRAINT coffee_summaries_projection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.coffees
    ADD CONSTRAINT coffees_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.command_status
    ADD CONSTRAINT command_status_pkey PRIMARY KEY (command_id);

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (comment_id);

ALTER TABLE ONLY public.editorial_generation_executions
    ADD CONSTRAINT editorial_generation_executions_pkey PRIMARY KEY (execution_id);

ALTER TABLE ONLY public.editorial_publication_schedule
    ADD CONSTRAINT editorial_publication_schedule_pkey PRIMARY KEY (schedule_id);

ALTER TABLE ONLY public.editorial_source_signals
    ADD CONSTRAINT editorial_source_signals_pkey PRIMARY KEY (signal_id);

ALTER TABLE ONLY public.editorial_source_signals
    ADD CONSTRAINT editorial_source_signals_source_id_external_id_key UNIQUE (source_id, external_id);

ALTER TABLE ONLY public.editorial_sources
    ADD CONSTRAINT editorial_sources_pkey PRIMARY KEY (source_id);

ALTER TABLE ONLY public.editorial_topic_candidates
    ADD CONSTRAINT editorial_topic_candidates_pkey PRIMARY KEY (topic_candidate_id);

ALTER TABLE ONLY public.identities
    ADD CONSTRAINT identities_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inbox_messages
    ADD CONSTRAINT inbox_messages_destination_event_id_key UNIQUE (destination, event_id);

ALTER TABLE ONLY public.inbox_messages
    ADD CONSTRAINT inbox_messages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.likes
    ADD CONSTRAINT likes_pkey PRIMARY KEY (like_id);

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_event_id_key UNIQUE (event_id);

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.projection_sync_events
    ADD CONSTRAINT projection_sync_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.saved_coffees
    ADD CONSTRAINT saved_coffees_pkey PRIMARY KEY (saved_coffee_id);

ALTER TABLE ONLY public.social_comments_projection
    ADD CONSTRAINT social_comments_projection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.social_likes_projection
    ADD CONSTRAINT social_likes_projection_pkey PRIMARY KEY (like_id);

ALTER TABLE ONLY public.ticket_status_projection
    ADD CONSTRAINT ticket_status_projection_pkey PRIMARY KEY (ticket_id);

ALTER TABLE ONLY public.tickets
    ADD CONSTRAINT tickets_pkey PRIMARY KEY (ticket_id);

ALTER TABLE ONLY public.article_revision_images
    ADD CONSTRAINT uq_article_revision_image_position UNIQUE (revision_id, section_id, "position");

ALTER TABLE ONLY public.article_revisions
    ADD CONSTRAINT uq_article_revision_number UNIQUE (article_id, revision_number);

ALTER TABLE ONLY public.article_revision_sections
    ADD CONSTRAINT uq_article_revision_section_position UNIQUE (revision_id, "position");

ALTER TABLE ONLY public.article_revision_tags
    ADD CONSTRAINT uq_article_revision_tag_value UNIQUE (revision_id, tag);

ALTER TABLE ONLY public.article_revision_paragraphs
    ADD CONSTRAINT uq_article_section_paragraph_position UNIQUE (section_id, "position");

ALTER TABLE ONLY public.user_entitlements_projection
    ADD CONSTRAINT user_entitlements_projection_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.user_saved_coffee_cafes_projection
    ADD CONSTRAINT user_saved_coffee_cafes_projection_pkey PRIMARY KEY (coffee_id);

ALTER TABLE ONLY public.user_saved_coffees_projection
    ADD CONSTRAINT user_saved_coffees_projection_pkey PRIMARY KEY (saved_coffee_id);

ALTER TABLE ONLY public.user_social_projection
    ADD CONSTRAINT user_social_projection_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);

CREATE INDEX idx_admin_studio_articles_status_updated_at ON public.admin_studio_articles USING btree (status, updated_at DESC);

CREATE INDEX idx_article_authoring_saga_lease ON public.article_authoring_sagas USING btree (lease_until);

CREATE INDEX idx_article_authoring_saga_state ON public.article_authoring_sagas USING btree (state, updated_at);

CREATE INDEX idx_article_generation_run_saga ON public.article_generation_runs USING btree (saga_id, attempt);

CREATE INDEX idx_article_review_approval_expiry ON public.article_review_approvals USING btree (expires_at, consumed_at);

CREATE INDEX idx_article_revision_images_revision ON public.article_revision_images USING btree (revision_id, section_id, "position");

CREATE INDEX idx_article_revisions_article_updated ON public.article_revisions USING btree (article_id, updated_at DESC);

CREATE INDEX idx_articles_projection_public_page ON public.articles_projection USING btree (locale, status, published_at DESC, id DESC);

CREATE INDEX idx_coffee_openinghours_coffee_id ON public.coffee_openinghours_projection USING btree (coffee_id);

CREATE INDEX idx_coffee_photos_coffee_id ON public.coffee_photos_projection USING btree (coffee_id);

CREATE INDEX idx_comments_target_created_at ON public.comments USING btree (target_id, created_at);

CREATE INDEX idx_editorial_generation_executions_occurred ON public.editorial_generation_executions USING btree (occurred_at DESC);

CREATE INDEX idx_editorial_publication_schedule_due ON public.editorial_publication_schedule USING btree (status, due_at);

CREATE INDEX idx_editorial_source_signals_new ON public.editorial_source_signals USING btree (status, discovered_at);

CREATE INDEX idx_editorial_sources_due ON public.editorial_sources USING btree (enabled, next_check_at);

CREATE INDEX idx_editorial_topic_candidates_status ON public.editorial_topic_candidates USING btree (status, detected_at DESC);

CREATE INDEX idx_inbox_messages_destination_status ON public.inbox_messages USING btree (destination, status);

CREATE INDEX idx_likes_target_active ON public.likes USING btree (target_id, active);

CREATE INDEX idx_likes_user_target ON public.likes USING btree (user_id, target_id);

CREATE INDEX idx_projection_sync_events_projection_id ON public.projection_sync_events USING btree (projection, id);

CREATE INDEX idx_social_comments_projection_author_visible ON public.social_comments_projection USING btree (author_id, deleted_at, moderation);

CREATE INDEX idx_social_comments_projection_target_created_at ON public.social_comments_projection USING btree (target_id, created_at);

CREATE INDEX idx_social_likes_projection_target_active ON public.social_likes_projection USING btree (target_id, active);

CREATE INDEX idx_social_likes_projection_user_active ON public.social_likes_projection USING btree (user_id, active);

CREATE INDEX idx_social_likes_projection_user_target ON public.social_likes_projection USING btree (user_id, target_id);

CREATE INDEX idx_ticket_status_status ON public.ticket_status_projection USING btree (status);

CREATE INDEX idx_ticket_status_user ON public.ticket_status_projection USING btree (user_id);

CREATE INDEX idx_user_social_projection_updated_at ON public.user_social_projection USING btree (updated_at);

CREATE INDEX ix_admin_audit_log_occurred_at ON public.admin_audit_log USING btree (occurred_at DESC);

CREATE INDEX ix_admin_audit_log_target_occurred_at ON public.admin_audit_log USING btree (target_type, target_id, occurred_at DESC);

CREATE INDEX ix_app_users_auth_user_id ON public.app_users USING btree (auth_user_id);

CREATE INDEX ix_saved_coffees_user_active ON public.saved_coffees USING btree (user_id, active);

CREATE INDEX ix_user_saved_coffees_projection_coffee_active ON public.user_saved_coffees_projection USING btree (coffee_id, active);

CREATE INDEX ix_user_saved_coffees_projection_user_active ON public.user_saved_coffees_projection USING btree (user_id, active, updated_at DESC);

CREATE UNIQUE INDEX uq_article_authoring_saga_revision ON public.article_authoring_sagas USING btree (revision_id);

CREATE UNIQUE INDEX uq_article_generation_artifact_revision ON public.article_generation_artifacts USING btree (revision_id);

CREATE UNIQUE INDEX uq_article_review_approval_revision ON public.article_review_approvals USING btree (saga_id, revision_id);

CREATE UNIQUE INDEX uq_editorial_publication_schedule_active_operation ON public.editorial_publication_schedule USING btree (article_id, operation) WHERE ((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'CLAIMED'::character varying, 'DISPATCHED'::character varying])::text[]));

CREATE UNIQUE INDEX ux_auth_users_provider_user ON public.auth_users USING btree (provider, provider_user_id);

CREATE UNIQUE INDEX ux_coffees_google_place_id ON public.coffees USING btree (google_place_id) WHERE (google_place_id IS NOT NULL);

CREATE UNIQUE INDEX ux_refresh_tokens_token ON public.refresh_tokens USING btree (token);

CREATE UNIQUE INDEX ux_saved_coffees_user_coffee ON public.saved_coffees USING btree (user_id, coffee_id);

CREATE UNIQUE INDEX ux_user_saved_coffees_projection_user_coffee ON public.user_saved_coffees_projection USING btree (user_id, coffee_id);

ALTER TABLE ONLY public.admin_audit_log
    ADD CONSTRAINT admin_audit_log_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.auth_users(id);

ALTER TABLE ONLY public.admin_user_access
    ADD CONSTRAINT admin_user_access_granted_by_fkey FOREIGN KEY (granted_by) REFERENCES public.auth_users(id);

ALTER TABLE ONLY public.admin_user_access
    ADD CONSTRAINT admin_user_access_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.auth_users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_auth_user_id_fkey FOREIGN KEY (auth_user_id) REFERENCES public.auth_users(id);

ALTER TABLE ONLY public.article_generation_artifacts
    ADD CONSTRAINT article_generation_artifacts_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.article_generation_runs(run_id);

ALTER TABLE ONLY public.article_generation_artifacts
    ADD CONSTRAINT article_generation_artifacts_saga_id_fkey FOREIGN KEY (saga_id) REFERENCES public.article_authoring_sagas(saga_id);

ALTER TABLE ONLY public.article_generation_runs
    ADD CONSTRAINT article_generation_runs_saga_id_fkey FOREIGN KEY (saga_id) REFERENCES public.article_authoring_sagas(saga_id);

ALTER TABLE ONLY public.article_review_approvals
    ADD CONSTRAINT article_review_approvals_saga_id_fkey FOREIGN KEY (saga_id) REFERENCES public.article_authoring_sagas(saga_id);

ALTER TABLE ONLY public.article_revision_images
    ADD CONSTRAINT article_revision_images_revision_id_fkey FOREIGN KEY (revision_id) REFERENCES public.article_revisions(revision_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.article_revision_images
    ADD CONSTRAINT article_revision_images_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.article_revision_sections(section_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.article_revision_paragraphs
    ADD CONSTRAINT article_revision_paragraphs_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.article_revision_sections(section_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.article_revision_sections
    ADD CONSTRAINT article_revision_sections_revision_id_fkey FOREIGN KEY (revision_id) REFERENCES public.article_revisions(revision_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.article_revision_tags
    ADD CONSTRAINT article_revision_tags_revision_id_fkey FOREIGN KEY (revision_id) REFERENCES public.article_revisions(revision_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.article_revisions
    ADD CONSTRAINT article_revisions_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(article_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.coffee_opening_hours
    ADD CONSTRAINT coffee_opening_hours_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.coffee_photos
    ADD CONSTRAINT coffee_photos_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.editorial_source_signals
    ADD CONSTRAINT editorial_source_signals_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.editorial_sources(source_id);

ALTER TABLE ONLY public.saved_coffees
    ADD CONSTRAINT saved_coffees_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_users(id);
