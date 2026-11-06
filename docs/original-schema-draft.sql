CREATE TABLE "users" (
  "id" integer PRIMARY KEY,
  "username" varchar,
  "password" varchar,
  "role" varchar,
  "email" varchar,
  "created_at" timestamp
);

CREATE TABLE "posts" (
  "id" integer PRIMARY KEY,
  "body" varchar,
  "user_id" integer NOT NULL,
  "created_at" timestamp
);

CREATE TABLE "follows" (
  "following_user_id" integer NOT NULL,
  "followed_user_id" integer NOT NULL,
  "created_at" timestamp,
  PRIMARY KEY ("following_user_id", "followed_user_id")
);

CREATE TABLE "messages" (
  "id" interger PRIMARY KEY,
  "user_id" integer NOT NULL,
  "body" varchar NOT NULL,
  "sent_at" timestamp
);

ALTER TABLE "posts" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "follows" ADD FOREIGN KEY ("following_user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "follows" ADD FOREIGN KEY ("followed_user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "messages" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;
