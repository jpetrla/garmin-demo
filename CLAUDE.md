# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4 / Java 25 REST API that imports Garmin `.fit` activity files into PostgreSQL. Maven wrapper build; Lombok and MapStruct run as annotation processors (configured in `pom.xml`).

## Commands

```bash
./mvnw verify                                   # full build + all tests + JaCoCo report (what CI runs)
./mvnw test -Dtest=ActivityServiceTest          # one test class
./mvnw test -Dtest=ActivityServiceTest#processActivity_rejectsNullFile   # one test method
docker compose up -d postgres                   # local DB (garmin_demo, user/password: garmin) for ./mvnw spring-boot:run
docker compose up --build                       # DB + the app in a container (app on :8080)
```

- The integration tests (`*IntegrationTest`) start PostgreSQL via Testcontainers, so **Docker must be running**. If it isn't, they all fail with "Could not find a valid Docker environment" while the unit/WebMvc tests still pass. The Dockerfile skips tests (`-DskipTests`) for the same reason.
- There is no separate lint/format step.

## Architecture

Package-by-feature under `cz.jpetrla.garmin.demo`: `activity` (the feature, with `api`, `fit`, `persistence` subpackages) and `web` (cross-cutting MVC config and exception handling).

Upload flow (`POST /api/activities/upload`, multipart part `file`):
`ActivityController` → `ActivityService.processActivity` → `FitActivityParser` → `ActivityData` → `ActivityMapper` (MapStruct) → `ActivityEntity` → `ActivityResponse`.

Things that span several files:

- **FIT parsing is isolated behind `FitActivityParser`** (the only implementation is `GarminFitActivityParser`, using the Garmin FIT SDK). The parser owns all FIT validation: corrupt file, multiple sessions, and missing/non-absolute start time all throw `InvalidActivityFileException`. It returns an immutable `ActivityData`, which always has `startedAt`.
- **Duplicate detection** uses a SHA-256 of the raw file bytes stored in the unique `file_hash` column. `ActivityService` does a fast `existsByFileHash` pre-check, and the unique constraint is the real guard against concurrent uploads (`DataIntegrityViolationException` → `DuplicateActivityException`). `processActivity` is deliberately not `@Transactional`. `ConcurrentActivityUploadIntegrationTest` covers the race.
- **Three parallel field lists**: `ActivityData` (parser output), `ActivityEntity` (DB; adds `id` and `fileHash`), `ActivityResponse` (API; adds `id`). `ActivityMapper` uses `unmappedTargetPolicy = ERROR`, which only catches target properties without a source (e.g. a new `ActivityEntity`/`ActivityResponse` field with no counterpart). It does **not** catch the reverse: a field added only to `ActivityData` (or to the entity for `toResponse`) is silently dropped, since `unmappedSourcePolicy` is left at its default `IGNORE`. So a new field must be added to all three by hand, plus the parser. A new field also needs a Flyway migration; `ddl-auto=validate` fails startup on any schema mismatch.
- **Error mapping** is centralised in `web/GlobalExceptionHandler` (extends `ResponseEntityExceptionHandler`, returns RFC 7807 `ProblemDetail`): invalid file → 400, unknown sort property → 400, not found → 404, duplicate → 409, anything else → generic 500 with the detail logged but not returned.
- **Listing** (`GET /api/activities`) returns `PagedModel`; the controller defaults to sort `startedAt,id` desc and the service appends an `id desc` tiebreaker to client-supplied sorts so paging stays stable.
- **Schema** is managed only by Flyway (`src/main/resources/db/migration`, currently V1–V4). Never edit an applied migration; add a new one.
- CORS allowed origins come from the required property `cors.allowed-origins` (`WebCorsConfig`, `/api/**` only).

## Tests

`src/test/.../testsupport` holds `FitFileFixtures` (builds synthetic FIT files with the SDK, including multi-session, chained and no-session variants) and `PostgresTestConfiguration` (Testcontainers service connection). The test `application.properties` overrides the main one (e.g. `show-sql=false`).
