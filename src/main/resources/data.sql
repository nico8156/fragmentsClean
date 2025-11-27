-- =========================================================
--  DONNEES DE DEMO
-- =========================================================

-- Clean des tables (ordre pour respecter les FK éventuelles si tu en ajoutes plus tard)
DELETE FROM social_comments_projection;
DELETE FROM comments;
DELETE FROM likes;
DELETE FROM outbox_events;
DELETE FROM coffees;

-- =========================
-- COFFEES
-- =========================

INSERT INTO coffees (
    id, google_id, display_name, formatted_address,
    national_phone_number, website_uri, latitude, longitude
) VALUES
      (
          'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
          'google-coffee-1',
          'Fragments Coffee Roasters',
          '123 Rue du Café, 35000 Rennes, France',
          '+33 2 99 00 00 01',
          'https://fragments.coffee',
          48.111,
          -1.678
      ),
      (
          'aaaaaaaa-0000-0000-0000-000000000000',
          'google-coffee-2',
          'Alternate Brew Lab',
          '42 Rue des Grains, 35000 Rennes, France',
          '+33 2 99 00 00 02',
          'https://alternate-brew.example.com',
          48.112,
          -1.679
      );



INSERT INTO social_comments_projection (
    id, target_id, author_id, parent_id,
    body, created_at, edited_at, deleted_at,
    moderation, like_count, reply_count, version
) VALUES
      (
          'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
          'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
          '22222222-2222-2222-2222-222222222222',
          NULL,
          'newer comment from author 2',
          '2024-01-01 10:00:00',
          NULL,
          NULL,
          'PUBLISHED',
          2,
          1,
          2
      ),
      (
          'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
          'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
          '11111111-1111-1111-1111-111111111111',
          NULL,
          'older comment from author 1',
          '2024-01-01 09:00:00',
          NULL,
          NULL,
          'PUBLISHED',
          1,
          0,
          1
      );
