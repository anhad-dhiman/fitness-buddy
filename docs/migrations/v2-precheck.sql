-- Read-only. Run against your existing local database BEFORE starting the app
-- with Flyway for the first time. Every query should return zero rows;
-- any row returned would make V2 fail.
--
--   psql -U postgres -d fitness_buddy -f docs/migrations/v2-precheck.sql

\echo '--- users with a missing required value'
SELECT id, user_name, email, (password IS NULL) AS password_missing, role, created_at
FROM users
WHERE user_name IS NULL OR email IS NULL OR password IS NULL
   OR role IS NULL OR created_at IS NULL;

\echo '--- emails used by more than one account (case-insensitive)'
SELECT lower(email) AS email, array_agg(id ORDER BY id) AS user_ids
FROM users GROUP BY lower(email) HAVING count(*) > 1;

\echo '--- usernames used by more than one account (case-insensitive)'
SELECT lower(user_name) AS user_name, array_agg(id ORDER BY id) AS user_ids
FROM users GROUP BY lower(user_name) HAVING count(*) > 1;

\echo '--- posts with a missing/empty body, body > 5000 chars, or no created_at'
SELECT id, user_id, char_length(body) AS body_length, created_at
FROM posts
WHERE body IS NULL OR char_length(body) NOT BETWEEN 1 AND 5000 OR created_at IS NULL;
