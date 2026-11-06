# Fitness Buddy: backend

Spring Boot 4 / Java 21 / PostgreSQL REST API for a fitness social network with
gym-partner matching.

## Prerequisites

- Java 21
- PostgreSQL 16 running locally with a database named `fitness_buddy`
  (connection settings in `src/main/resources/application.yml`)
- Docker, for the test suite (Testcontainers starts a throwaway PostgreSQL)

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

| Method | Path | Auth | Success |
|---|---|---|---|
| POST | `/auth/register` | public | 201 + user |
| POST | `/auth/login` | public | 200 + user, sets `accessToken` HttpOnly cookie |
| POST | `/post/newPost` | cookie | 201 + post |
| GET | `/post/posts?cursor=` | cookie | 200 + page of posts + `nextCursor` |

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
