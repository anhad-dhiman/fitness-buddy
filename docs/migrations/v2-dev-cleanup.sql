-- DEVELOPMENT DATABASES ONLY. THIS DELETES DATA.
--
-- Removes the rows that v2-precheck.sql reports, so V2 can apply to a local
-- database that collected bad data from the old, unvalidated endpoints.
-- Never run this against a database with real users. There, each bad row
-- needs a deliberate decision instead of deletion.
--
--   psql -U postgres -d fitness_buddy -f docs/migrations/v2-dev-cleanup.sql

BEGIN;

-- Bad posts first.
DELETE FROM posts
WHERE body IS NULL OR char_length(body) NOT BETWEEN 1 AND 5000;

UPDATE posts SET created_at = now() WHERE created_at IS NULL;

-- Users who can never log in or are incomplete (and their posts, because of
-- the foreign key).
DELETE FROM posts WHERE user_id IN (
    SELECT id FROM users
    WHERE user_name IS NULL OR email IS NULL OR password IS NULL OR role IS NULL);
DELETE FROM users
WHERE user_name IS NULL OR email IS NULL OR password IS NULL OR role IS NULL;

UPDATE users SET created_at = now() WHERE created_at IS NULL;

-- Duplicates: keep the oldest account (lowest id) per email / username.
DELETE FROM posts WHERE user_id IN (
    SELECT id FROM (
        SELECT id,
               row_number() OVER (PARTITION BY lower(email) ORDER BY id)     AS email_rank,
               row_number() OVER (PARTITION BY lower(user_name) ORDER BY id) AS name_rank
        FROM users) ranked
    WHERE email_rank > 1 OR name_rank > 1);
DELETE FROM users WHERE id IN (
    SELECT id FROM (
        SELECT id,
               row_number() OVER (PARTITION BY lower(email) ORDER BY id)     AS email_rank,
               row_number() OVER (PARTITION BY lower(user_name) ORDER BY id) AS name_rank
        FROM users) ranked
    WHERE email_rank > 1 OR name_rank > 1);

-- Normalise stored emails to the form the application now uses.
UPDATE users SET email = lower(trim(email));

COMMIT;
