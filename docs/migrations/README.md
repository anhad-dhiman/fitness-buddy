# Database migrations (Flyway)

The schema is defined by the SQL files in `src/main/resources/db/migration/`,
applied in version order by Flyway at application startup. Hibernate only
*validates* the schema (`ddl-auto: validate`); it never changes it.

## Rules

1. **Never edit a migration that has been applied anywhere.** Flyway stores a
   checksum per migration and refuses to start if an applied file changed.
2. Every schema change is a new file: `V<next number>__<what_it_does>.sql`
   (two underscores).
3. Write the constraint in SQL *and* validate in the API. The API check gives
   users a helpful 400/409; the database constraint guarantees correctness
   when the API check is bypassed or races (two requests at the same instant).

## Upgrading an existing local database (one-time)

Your local `fitness_buddy` database was created by Hibernate's old
`ddl-auto: update`. On first startup Flyway *baselines* it (records it as
version 1 without running V1) and then applies V2.

V2 adds NOT NULL and UNIQUE constraints, so it fails if the database already
contains rows the old unvalidated endpoints allowed (duplicate emails, NULL
passwords, empty posts). The failed migration is rolled back completely, and
the app refuses to start with `Migration ... V2 ... failed! Changes
successfully rolled back.`

Choose one:

**Option A: start fresh (simplest, loses local test data)**

```bash
psql -U postgres -c "DROP DATABASE fitness_buddy" -c "CREATE DATABASE fitness_buddy"
```

**Option B: keep your data**

```bash
psql -U postgres -d fitness_buddy -f docs/migrations/v2-precheck.sql     # read-only report
psql -U postgres -d fitness_buddy -f docs/migrations/v2-dev-cleanup.sql  # DEV ONLY: deletes bad rows
psql -U postgres -d fitness_buddy -f docs/migrations/v2-precheck.sql     # every section should show (0 rows)
```

Then start the application normally.
