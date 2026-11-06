-- V2: Integrity constraints for users and posts, plus the feed index.
--
-- Postgres runs each Flyway migration inside a transaction, so if any
-- statement fails (e.g. existing duplicate emails), the whole file is rolled
-- back and the application refuses to start. See docs/migrations/README.md.

-- ---------------------------------------------------------------- users ----

-- Every account must have these. A NULL password is what the original
-- register endpoint stored when the request omitted "password".
ALTER TABLE users
    ALTER COLUMN user_name  SET NOT NULL,
    ALTER COLUMN email      SET NOT NULL,
    ALTER COLUMN password   SET NOT NULL,
    ALTER COLUMN role       SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

-- Case-insensitive uniqueness: "A@x.com" and "a@x.com" are the same account.
-- These are expression (functional) indexes, so a query only uses them when
-- it filters on the same expression: WHERE lower(email) = lower(?).
CREATE UNIQUE INDEX ux_users_email     ON users (lower(email));
CREATE UNIQUE INDEX ux_users_user_name ON users (lower(user_name));

-- ---------------------------------------------------------------- posts ----

-- VARCHAR(255) was Hibernate's default for String and made any post longer
-- than 255 characters fail. TEXT has no practical limit; the CHECK enforces
-- the real business rule (the API validates the same limit first).
ALTER TABLE posts
    ALTER COLUMN body       TYPE TEXT,
    ALTER COLUMN body       SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE posts
    ADD CONSTRAINT ck_posts_body_length CHECK (char_length(body) BETWEEN 1 AND 5000);

-- Serves the keyset-paginated feed:
--   ORDER BY created_at DESC, id DESC  [WHERE (created_at, id) < (?, ?)]
-- With this index Postgres reads the newest N rows straight from the index
-- instead of sorting the entire table on every feed request.
CREATE INDEX ix_posts_created_at_id ON posts (created_at DESC, id DESC);
