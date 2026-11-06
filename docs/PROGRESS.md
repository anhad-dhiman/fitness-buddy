# Progress tracker

## Done

| Milestone | What |
|---|---|
| 0 | Codebase audit |
| 1.1 | Package-by-feature restructure (`com.fitnessbuddy.{auth,user,post,security,common,config}`) |
| 1.2 | Flyway migrations (V1 baseline, V2 constraints + feed index), `ddl-auto: validate`, Testcontainers tests |
| 1.3 | Problem Details error contract, Bean Validation, duplicate-registration fix (incl. race), services free of HTTP types, 201s |
| 1.4 | Secrets from env/.env; JWT parsed once, typed `AuthenticatedUser` principal, filter registered once; JSON 401/403; `STATELESS`; CORS; `Secure` cookie by default; logout; login timing equalisation; CSRF tokens (`csrf.spa()`) rotated at login/logout |

## Next

| Milestone | Scope |
|---|---|
| **1.5 Mapping & entities** | ModelMapper → MapStruct; drop `@Data` on entities; `Role` enum; author DTO gets `id`, loses `role`; `@CreationTimestamp`, microsecond-truncated timestamps |
| 1.6 Posts CRUD | `/api/...` REST paths; get/update/delete with ownership (403); posts by user (+ `posts(user_id)` index); LAZY + fetch join (N+1); configurable page size; `updated_at`; `@Version` |
| 1.7 Profile | `GET/PUT /users/me`, `GET /users/{id}`, change password |
| 1.8 Refresh tokens & account status | rotating refresh tokens stored hashed; deactivation |

## Known issues (not yet fixed)

| ID | Issue | Planned |
|---|---|---|
| B5 | 15-minute session, no refresh token; a JWT copied before logout stays valid until it expires | 1.8 |
| — | Git history (baseline commit) still contains the old JWT key and DB password. Harmless once rotated, but keep the repo private or change both. | now (you) |
| S4/A4 | `@Data` on entities (`toString` includes password hash; unsafe equals/hashCode) | 1.5 |
| S5 | Feed exposes every author's `role`; no author `id` | 1.5 |
| A3 | ModelMapper (silent runtime mapping failures) | 1.5 |
| — | Create-post response has nanosecond timestamp, feed has microsecond | 1.5 |
| B3 | Feed page size hard-coded to 2 | 1.6 |
| — | Feed N+1: EAGER `@ManyToOne` user, one query per distinct author | 1.6 |
| A8 | Verb URLs (`/post/newPost`), no `/api` prefix | 1.6 |
| — | The SPA reads the API's XSRF-TOKEN cookie. That works locally because cookies are scoped by host, not port (localhost:5173 can read localhost:8080's). Deployed frontend and API must share a site (e.g. app./api. subdomains with a cookie Domain), or serve the frontend from the API's origin. | 8 |
| — | Registration 409 reveals that an email is registered (accepted UX trade-off; rate limiting in Phase 7) | 7 |
