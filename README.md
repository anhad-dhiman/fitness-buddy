# Fitness Buddy: backend

Spring Boot 4 / Java 21 / PostgreSQL REST API for a fitness social network with
gym-partner matching.

## Prerequisites

- Java 21
- PostgreSQL 16 running locally with a database named `fitness_buddy`
  (connection settings in `src/main/resources/application.yml`)
- Docker, for the test suite (Testcontainers starts a throwaway PostgreSQL)

## Configure (one-time)

Secrets are never stored in the repository. Copy the template and fill it in:

```bash
cp .env.example .env
```

- `DB_PASSWORD`: your local PostgreSQL password
- `JWT_SECRET`: generate a fresh key (`openssl rand -base64 32`, or the
  PowerShell command in `.env.example`)
- `COOKIE_SECURE=false`: only for local plain-http development

`.env` is gitignored. Spring loads it from the working directory (the project
root in IntelliJ or with `./mvnw`). Real environment variables override it,
which is how a deployed server provides them.

## Run

```bash
./mvnw spring-boot:run
```

Flyway applies the migrations in `src/main/resources/db/migration` on startup.
**Upgrading a database created before Flyway was introduced?** Read
[`docs/migrations/README.md`](docs/migrations/README.md) first.

## Test

```bash
./mvnw test
```

| Kind | Annotation | Needs Docker | Example |
|---|---|---|---|
| Unit | `@ExtendWith(MockitoExtension.class)` | no | `AuthServiceImplTests` |
| Web slice | `@WebMvcTest` | no | `AuthControllerTests` |
| Persistence | `@DataJpaTest` + Testcontainers | yes | `PostRepoTests`, `SchemaConstraintsTests` |
| Full integration | `@SpringBootTest` + Testcontainers | yes | `AuthFlowIntegrationTests` |

## API (current)

| Method | Path | Auth | CSRF header | Success |
|---|---|---|---|---|
| GET | `/auth/csrf` | public | n/a | 204, sets `XSRF-TOKEN` cookie |
| POST | `/auth/register` | public | required | 201 + user |
| POST | `/auth/login` | public | required | 200 + user, sets `accessToken` (HttpOnly) and a new `XSRF-TOKEN` |
| POST | `/auth/logout` | public | required | 204, deletes `accessToken`, new `XSRF-TOKEN` |
| POST | `/post/newPost` | cookie | required | 201 + post |
| GET | `/post/posts?cursor=` | cookie | n/a | 200 + page of posts + `nextCursor` |

Not logged in → `401`. Missing or wrong CSRF header → `403`.

### Calling the API from the React app

Authentication uses two cookies:

| Cookie | Readable by JS | Purpose |
|---|---|---|
| `accessToken` | no (HttpOnly) | the JWT; the browser sends it automatically |
| `XSRF-TOKEN` | yes | CSRF token; JS copies it into the `X-XSRF-TOKEN` header |

1. When the app starts, call `GET /auth/csrf` once.
2. Send every request with credentials (`withCredentials: true` in axios,
   `credentials: "include"` with fetch).
3. On every POST/PUT/PATCH/DELETE, send header `X-XSRF-TOKEN` with the
   **current** value of the `XSRF-TOKEN` cookie. It changes at login and
   logout, so read it per request. axios does all of this automatically:

```js
const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,
  withXSRFToken: true, // send X-XSRF-TOKEN cross-origin too (axios >= 1.6)
});
await api.get("/auth/csrf");
```

The frontend origin must be listed in `CORS_ALLOWED_ORIGINS` (default
`http://localhost:5173`, Vite's dev server). Reading the `XSRF-TOKEN` cookie
works locally because cookies are scoped by host, not port. Deployment needs
the frontend and API on the same site (see `docs/PROGRESS.md`).

Errors use RFC 9457 Problem Details (`application/problem+json`):

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400,
  "detail": "Request validation failed", "instance": "/auth/register",
  "errors": { "email": ["Email must be a valid email address"] } }
```

## Package layout

```
com.fitnessbuddy
├── auth/       register & login (controller, service, dto)
├── user/       user entity, repository, dto, mapper
├── post/       posts & feed (controller, service, repository, entity, dto, mapper)
├── security/   JWT creation/validation, auth filter, auth cookie
├── common/     cross-cutting: exceptions + handler, validation, mapper interface
└── config/     Spring configuration (security, mapping)
```

Progress and known issues: [`docs/PROGRESS.md`](docs/PROGRESS.md).
