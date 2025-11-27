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


-- =========================
-- LIKES
-- =========================

-- "me" a liké le TARGET_ID (actif)
INSERT INTO likes (
    like_id, user_id, target_id, active, updated_at, version
) VALUES (
             '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
             '11111111-1111-1111-1111-111111111111',
             'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
             TRUE,
             '2024-01-01T10:00:00Z',
             3
         );

-- Un autre user a liké le même café (actif)
INSERT INTO likes (
    like_id, user_id, target_id, active, updated_at, version
) VALUES (
             '22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
             '22222222-2222-2222-2222-222222222222',
             'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
             TRUE,
             '2024-01-01T10:00:00Z',
             3
         );

-- Like inactif (ne doit pas compter dans le summary)
INSERT INTO likes (
    like_id, user_id, target_id, active, updated_at, version
) VALUES (
             '33333333-cccc-cccc-cccc-cccccccccccc',
             '33333333-3333-3333-3333-333333333333',
             'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
             FALSE,
             '2024-01-01T09:00:00Z',
             3
         );


-- =========================
-- COMMENTS (write model)
-- =========================

INSERT INTO comments (
    comment_id, target_id, author_id, parent_id,
    body, created_at, edited_at, deleted_at,
    moderation, version
) VALUES
      (
          'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
          'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
          '11111111-1111-1111-1111-111111111111',
          NULL,
          'older comment from author 1',
          '2024-01-01 09:00:00',
          NULL,
          NULL,
          'ACCEPTED',
          1
      ),
      (
          'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
          'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
          '22222222-2222-2222-2222-222222222222',
          NULL,
          'newer comment from author 2',
          '2024-01-01 10:00:00',
          NULL,
          NULL,
          'ACCEPTED',
          2
      ),
      (
          'cccccccc-cccc-cccc-cccc-cccccccccccc',
          'e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3',
          '11111111-1111-1111-1111-111111111111',
          NULL,
          'deleted comment (should not appear in read)',
          '2024-01-01 11:00:00',
          NULL,
          '2024-01-01 11:30:00',
          'ACCEPTED',
          3
      );


-- =========================
-- SOCIAL_COMMENTS_PROJECTION (read model)
-- =========================
-- Projection “cohérente” avec les deux premiers commentaires
-- (tu pourras plus tard la remplir via un projector/event handler)

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
          'ACCEPTED',
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
          'ACCEPTED',
          1,
          0,
          1
      );
