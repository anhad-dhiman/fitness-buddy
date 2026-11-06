# Progress tracker

## Done

| Milestone | What |
|---|---|
| 0 | Codebase audit |
| 1.1 | Package-by-feature restructure (`com.fitnessbuddy.{auth,user,post,security,common,config}`) |
| 1.2 | Flyway migrations (V1 baseline, V2 constraints + feed index), `ddl-auto: validate`, Testcontainers tests |
| 1.3 | Problem Details error contract, Bean Validation, duplicate-registration fix (incl. race), services free of HTTP types, 201s |

## Next

| Milestone | Scope |
|---|---|
| **1.4 Security hardening** | JWT secret + DB password from env vars (**rotate both**: they were in source); filter not a `@Component`, parse JWT once, typed principal; 401 entry point (JSON); `STATELESS` sessions; CORS for React; `Secure` cookie per profile; logout; login timing equalisation; CSRF decision |
| 1.5 Mapping & entities | ModelMapper → MapStruct; drop `@Data` on entities; `Role` enum; author DTO gets `id`, loses `role`; `@CreationTimestamp`, microsecond-truncated timestamps |
| 1.6 Posts CRUD | `/api/...` REST paths; get/update/delete with ownership (403); posts by user (+ `posts(user_id)` index); LAZY + fetch join (N+1); configurable page size; `updated_at`; `@Version` |
| 1.7 Profile | `GET/PUT /users/me`, `GET /users/{id}`, change password |
| 1.8 Refresh tokens & account status | rotating refresh tokens stored hashed; deactivation |

## Known issues (not yet fixed)

| ID | Issue | Planned |
|---|---|---|
| S1 | JWT signing key and DB password hard-coded in source | 1.4 |
| S2 | Cookie auth with CSRF disabled (SameSite=Lax mitigates) | 1.4 |
| S3 | Cookie `Secure=false` | 1.4 |
| B4 | Unauthenticated → 403 (should be 401), non-Problem-Details body | 1.4 |
| B5 | 15-minute session, no refresh, no logout | 1.4 / 1.8 |
| S6 | Login timing reveals whether an email is registered | 1.4 |
| A5/A6 | JWT parsed 3× per request; filter registered twice | 1.4 |
| S4/A4 | `@Data` on entities (`toString` includes password hash; unsafe equals/hashCode) | 1.5 |
| S5 | Feed exposes every author's `role`; no author `id` | 1.5 |
| A3 | ModelMapper (silent runtime mapping failures) | 1.5 |
| — | Create-post response has nanosecond timestamp, feed has microsecond | 1.5 |
| B3 | Feed page size hard-coded to 2 | 1.6 |
| — | Feed N+1: EAGER `@ManyToOne` user, one query per distinct author | 1.6 |
| A8 | Verb URLs (`/post/newPost`), no `/api` prefix | 1.6 |
| — | Registration 409 reveals that an email is registered (accepted UX trade-off; rate limiting in Phase 7) | 7 |
