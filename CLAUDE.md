# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FleetMgm is a Master's thesis fleet management application. Backend: Java 21 + Spring Boot 3.5 (started on 3.3, upgraded in Hito 11 once the OWASP Dependency-Check gate exposed unpatched CVSS >= 7 CVEs at the end of the 3.3.x line). Frontend: React + Vite + TypeScript. The project is greenfield — source code is scaffolded incrementally per `planning.md`, which is the source of truth for all architectural decisions.

**Timeline:** ~6 weeks (Jun–mid Jul 2026). See `planning.md` for week-by-week checklist.

---

## Commands

### Backend (`backend/`)

```bash
./mvnw spring-boot:run                        # Start dev server (port 8080)
./mvnw test                                   # Run all tests
./mvnw test -Dtest=VehicleServiceTest         # Run a single test class
./mvnw verify -Pfailsafe                      # Integration tests (Testcontainers — requires Docker)
./mvnw spotbugs:check                         # Static analysis
./mvnw dependency-check:check                 # OWASP CVE scan (falla si CVSS ≥ 7)
```

### Monorepo (root)

```bash
turbo dev            # Starts all apps in dev mode (web on port 5173)
turbo build          # Builds all packages and apps
turbo test           # Runs Vitest across packages/ and apps/web
turbo lint           # Lints all workspaces
```

### Web app (`apps/web/`)

```bash
npm run dev          # Start Vite dev server (port 5173)
npm run build        # Production build
npm run lint         # oxlint
npm run test         # Vitest (unit + component tests)
npm run test:ui      # Vitest UI mode
```

### Full stack

```bash
docker compose up    # Starts postgres:16 + backend + apps/web (nginx)
```

---

## JPA — Avoiding N+1 Queries

Never rely on Hibernate lazy-loading associations inside a loop. If a service method needs an association, declare it explicitly in the repository — not after the fact.

**Use `@EntityGraph` for known fetch requirements:**
```java
@EntityGraph(attributePaths = {"assignedDriver", "client"})
Optional<Job> findById(UUID id);
```

**Use `JOIN FETCH` in JPQL for list queries:**
```java
@Query("SELECT v FROM Vehicle v LEFT JOIN FETCH v.activeAssignment WHERE v.status = :status")
List<Vehicle> findAllActiveWithAssignment(@Param("status") VehicleStatus status);
```

**Use JPQL projections when only a subset of fields is needed** — no entity loaded, no associations, no risk.

Enable SQL logging in the `dev` profile (`spring.jpa.show-sql=true`, `spring.jpa.properties.hibernate.format_sql=true`) to verify queries during development. The same query repeated with different IDs in the log means an N+1 has slipped through.

---

## Soft Delete Pattern

All entities with logical deletion share the same implementation — never use `deleteById()` on them.

**Entity:** annotate with `@SQLRestriction("deleted_at IS NULL")` (Hibernate 6 / Spring Boot 3+). Hibernate appends this filter to every query automatically — no manual `WHERE` clause needed in repositories.

```java
@Entity
@SQLRestriction("deleted_at IS NULL")
public class Vehicle {
    // ...
    private Instant deletedAt;  // null = active, non-null = deleted
}
```

**Service:** set `deletedAt = Instant.now()` and call `save()`. Returns `204 No Content`.

```java
public void delete(UUID id) {
    Vehicle vehicle = repo.findById(id).orElseThrow(...);
    vehicle.setDeletedAt(Instant.now());
    repo.save(vehicle);
}
```

**Audit / recovery queries** that need to see deleted records must use a `nativeQuery` or temporarily disable the filter — never remove `@SQLRestriction` from the entity.

---

## Transaction Rules

`@Transactional` belongs exclusively in `application/` (`@Service`). Never on controllers or repositories.

- Read-only methods use `@Transactional(readOnly = true)` — disables dirty checking and prevents accidental flushes.
- Methods that publish `@TransactionalEventListener(AFTER_COMMIT)` events must run inside a transaction or the event is silently dropped.
- `spring.jpa.open-in-view=false` is mandatory. With OSIV disabled, any lazy association accessed outside an active transaction throws `LazyInitializationException` immediately — a visible bug rather than a silent N+1 in production.
- Resolve lazy associations inside the `@Transactional` service method via one of: MapStruct mapping before the transaction closes, `@EntityGraph` on the repository method, or a JPQL projection. Never fix it by switching to `FetchType.EAGER` globally.

---

## Java 21 Conventions

### Use records for all DTOs
```java
// request
public record CreateVehicleRequest(
    @NotBlank String licensePlate,
    @NotNull VehicleCategory category
) {}

// response
public record VehicleResponse(UUID id, String licensePlate, VehicleStatus status) {}
```
No Lombok, no POJO boilerplate. Records are immutable by default — ideal for request/response objects that must never be mutated after construction.

### Optional — return values only
- **Yes:** `Optional<Vehicle> findById(UUID id)` in repositories.
- **No:** never as a method parameter, never as a `@Entity` field (Hibernate does not support it), never wrapped in another container (`Optional<List<...>>`).
- Unwrap immediately at the service call site with `.orElseThrow(() -> new NotFoundException(...))`.

### Switch expressions on enums
```java
// always exhaustive; unhandled values fail fast
String label = switch (vehicle.getCategory()) {
    case LIGHT_VEHICLE    -> "Light";
    case HEAVY_VEHICLE    -> "Heavy";
    case HEAVY_MACHINERY  -> "Machinery";
    // no default — compiler enforces exhaustiveness on sealed enums
};
```
If a `default` branch is needed (e.g. the enum is not sealed), it must throw `IllegalStateException`, never return silently.

### Pattern matching
```java
// instanceof pattern matching — no explicit cast
if (cause instanceof ConstraintViolationException cve) {
    // use cve directly
}
```

### Other Java 21 features
- **Text blocks** for multi-line JPQL queries or SQL in `@Query`.
- **Sealed interfaces** for closed domain hierarchies where exhaustiveness matters (use sparingly — only when the variant set is truly fixed).
- Avoid **virtual threads** (`Thread.ofVirtual()`) until Spring Boot's Tomcat integration is explicitly configured for them; the default thread pool model is sufficient for this project's scale.

---

## Testing Strategy

### Backend — which tool per layer

| Layer | Tool | What it proves |
|-------|------|----------------|
| `application/` (services) | JUnit 5 + Mockito, no Spring context | Business logic in isolation |
| `infrastructure/` (repositories) | `@DataJpaTest` + Testcontainers (PostgreSQL 16) | JPA queries work against the real schema + Flyway migrations |
| `api/` (controllers) | `@WebMvcTest` + Mockito of the service | Input validation, HTTP status codes, JSON serialisation |
| Full flows | `@SpringBootTest` + Testcontainers | Critical paths only (e.g. login → job → invoice) |

**Rule:** never use H2 for repository tests — queries must run against PostgreSQL 16 with the actual Flyway migration sequence applied. Testcontainers handles this automatically when `@DataJpaTest` detects a `pg` datasource.

### Service test example
```java
class VehicleServiceTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks VehicleService vehicleService;  // no Spring context

    @Test
    void createVehicle_persistsAndReturnsDto() {
        // arrange — stub only what this test needs
        // act
        // assert on the returned DTO, verify repository.save() called once
    }
}
```

### Repository test example
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)   // use real PostgreSQL
@Testcontainers
class VehicleRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void findActiveVehicles_excludesSoftDeleted() { ... }
}
```

### Controller test example
```java
@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @MockBean VehicleService vehicleService;  // only the service is mocked

    @Test
    void createVehicle_returns201_withLocation() { ... }

    @Test
    void createVehicle_returns400_whenLicensePlateMissing() { ... }
}
```

### Frontend
- **Component tests (Vitest + React Testing Library):** render the component, interact via user-event, assert on the DOM. Never assert on implementation details (state, internal methods).
- **API mocking (MSW):** define handlers in `src/mocks/handlers.ts`. Tests import the mock server — no real backend needed.
- **Do not test** TanStack Query internals or Zustand store shape directly; test the rendered output that depends on them.

---

## Commit Conventions

Follow [Conventional Commits](https://www.conventionalcommits.org/). Format: `type(scope): description`.

```
feat(vehicle): add soft delete endpoint
fix(auth): prevent login bypass on locked accounts
test(job): add repository tests for active job queries
refactor(billing): extract invoice number generation to domain method
chore(deps): bump spring-boot to 3.3.6
```

**Types:** `feat` · `fix` · `test` · `refactor` · `docs` · `chore`

**Scope:** matches the feature package name — `auth`, `vehicle`, `worker`, `client`, `job`, `billing`, `workshop`, `gps`, `shared`. Use `deps` for dependency updates and `ci` for pipeline changes.

---

## Branch Naming Conventions

Each milestone in `planning.md` gets its own branch. Format:

```
hito<NN>-<scope>-<short-description>
```

- `NN` is zero-padded to two digits so branches sort correctly (`01`, `02`, …, `31`).
- `<scope>` matches the commit scope for that milestone: `auth`, `vehicle`, `client`, `job`, `billing`, `workshop`, `gps`, `ci`, etc.
- `<short-description>` is 1–3 kebab-case words.

**Examples**

| Hito | Branch |
|------|--------|
| Hito 1 — SecurityConfig | `hito01-auth-security-config` |
| Hito 2 — Auth API contract | `hito02-auth-api-contract` |
| Hito 5 — Clients CRUD | `hito05-client-crud` |
| Hito 7 — Vehicles CRUD | `hito07-vehicle-crud` |
| Hito 13 — Jobs lifecycle | `hito13-job-lifecycle` |
| Hito 19 — Invoicing | `hito19-billing-invoices` |
| Hito 25 — Frontend login | `hito25-frontend-login` |
| Hito 30 — CI/CD pipeline | `hito30-ci-owasp-semgrep` |

Merge each hito branch into `main` via PR when the milestone is complete.
Never push directly to `main`.

---

## MapStruct Conventions

Mappers live in the `dto/` sub-package of each feature, alongside the records they map. One mapper interface per feature.

```java
@Mapper(componentModel = "spring")  // required — makes it a Spring bean
public interface VehicleMapper {

    VehicleResponse toResponse(Vehicle vehicle);

    Vehicle toEntity(CreateVehicleRequest request);

    @Mapping(target = "id", ignore = true)        // always explicit, never silent
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateVehicleRequest request, @MappingTarget Vehicle vehicle);
}
```

**Rules:**
- `componentModel = "spring"` is mandatory on every mapper — otherwise MapStruct generates a static instance that cannot be injected in tests.
- Mappers contain field mapping only. Any derived value or business rule goes in the `application/` service before calling the mapper.
- Every ignored field must be declared with `@Mapping(target = "...", ignore = true)` — never rely on MapStruct's silent unmapped-field behaviour.

---

## TanStack Query Conventions

Query keys follow the pattern `[feature, scope?, id?]`. Define them as constants inside the feature hook — never inline in components.

```ts
// hooks/useVehicles.ts
const VEHICLES_KEY = 'vehicles'

export function useVehicles() {
  return useQuery({ queryKey: [VEHICLES_KEY], queryFn: fetchVehicles })
}

export function useVehicle(id: string) {
  return useQuery({ queryKey: [VEHICLES_KEY, id], queryFn: () => fetchVehicle(id) })
}

export function useCreateVehicle() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createVehicle,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [VEHICLES_KEY] }),
  })
}
```

**Rules:**
- After any mutation (create, update, delete) invalidate the top-level feature key (`[VEHICLES_KEY]`) — this refreshes both list and detail queries in one call.
- Scoped sublists use a second element: `['jobs', 'active']`, `['jobs', 'driver', driverId]`.
- Components never call `queryClient` directly — only hooks do.

---

## UI Copy Language

Project-specific override of the global default: **user-facing UI copy in `apps/web/` is written in Spanish** — page titles, labels, button text, form field labels, empty/loading states, confirmation dialogs, toasts, and validation messages shown to the end user.

This does **not** change anything else — everything else stays in English per the global convention:
- Code, identifiers, variable/function/component names, file names
- Comments
- Commit messages, PR descriptions, branch names
- Enum values and API contracts (`VehicleStatus.ACTIVE`, `/api/v1/vehicles`, etc.) — only their *displayed label* is translated (e.g. a `VEHICLE_STATUS_LABEL` map), never the wire value
- Backend code and any DTO/entity/exception content

Centralize translated strings the same way `VehicleStatusBadge`/`VehicleFormModal` already do it — a `Record<Enum, string>` (or equivalent) mapping the raw value to its Spanish label — so a future i18n pass (if one is ever undertaken) only has to swap the map's construction, not hunt down inline strings.

---

## API Contract

### URL patterns
```
GET    /api/v1/{feature}          list (paginated)
POST   /api/v1/{feature}          create
GET    /api/v1/{feature}/{uuid}   get by id
PUT    /api/v1/{feature}/{uuid}   full replace
PATCH  /api/v1/{feature}/{uuid}   partial update
DELETE /api/v1/{feature}/{uuid}   soft delete
```
All IDs in URLs are **UUID**. Never expose sequential integers — enumeration is not practical with UUIDs, and the service layer enforces ownership anyway (IDOR defence, OWASP B).

### HTTP verbs
- **POST** — create a new resource; returns `201 Created` + `Location` header.
- **PUT** — replace the full resource; idempotent.
- **PATCH** — update one or more fields; idempotent.
- **DELETE** — soft delete (sets `deletedAt`); returns `204 No Content`.
- **GET** — never mutates state; safe to retry.

### Error response body
Every error from `GlobalExceptionHandler` returns the same shape:
```json
{
  "status": 404,
  "code": "VEHICLE_NOT_FOUND",
  "message": "Vehicle f3a1c2d4-... not found",
  "correlationId": "a1b2c3d4"
}
```
`code` is a SCREAMING_SNAKE_CASE application-level constant — never expose exception class names or stack traces.

### Pagination
Paginated endpoints accept `?page=0&size=20&sort=createdAt,desc` and return:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```
This is `PageResponse<T>` from `shared/`. Always use this wrapper — never return a raw `List` from a paginated endpoint.

---

## Architecture

### Backend: Package-by-feature monolith

Each domain feature lives under `com.fleetmgm/<feature>/` with four fixed sub-packages:

```
<feature>/
  api/            @RestController — routing + input validation only
  application/    @Service — all business logic, @Transactional
  domain/         @Entity, value objects — zero Spring dependencies
  infrastructure/ JpaRepository + any external calls
  dto/            Request/Response records (MapStruct for mapping)
```

Features: `auth`, `vehicle`, `worker`, `client`, `job`, `billing`, `workshop`, `gps`.  
Cross-cutting: `shared/` — `GlobalExceptionHandler`, `AuditLog`, `PageResponse<T>`.  
Config: `config/SecurityConfig.java`, `config/AuditorAwareImpl.java`.

**Rule:** Never expose JPA entities in the API layer. Always map to DTOs. Business logic lives exclusively in `application/`; controllers do routing + validation, repositories do data access.

### Frontend: Monorepo structure

Turborepo + npm workspaces. Shared logic lives in `packages/`; visual components are app-specific.

```
packages/
  api/src/
    client.ts            Axios instance with JWT Bearer interceptor + auto-refresh
    types.ts             PageResponse<T>, ApiError, shared domain types
  hooks/src/
    useAuth.ts           login/logout mutations (TanStack Query)
    useVehicles.ts       }
    useWorkers.ts        } TanStack Query wrappers — consumed by web and mobile
    useJobs.ts           }
    …
  store/src/
    authStore.ts         Zustand — user session, tokens, role

apps/web/src/
  mocks/                 MSW handlers (browser.ts for dev, server.ts for Vitest)
  pages/                 One file per route (Login, Dashboard, Vehicles, …)
  components/            Sub-divided by feature (vehicle/, worker/, job/, map/, billing/, workshop/)

apps/mobile/src/         React Native + Expo — post-web; consumes same packages/
  screens/               Equivalent to pages/
  components/            NativeWind + RN primitives
```

Server state via **TanStack Query**; client state via **Zustand**. UI components from **shadcn/ui + Tailwind CSS**.

---

## Security Model

**JWT:** HS512 in dev (secret ≥ 64 chars), RS256 in prod. Access token: 15 min. Refresh token: 7 days, stored as SHA-256 hash in `RefreshToken` table (allows real revocation on logout).

**RBAC — 5 roles:** `ADMIN > MANAGER > ADMINISTRATIVE > WORKSHOP_STAFF > DRIVER`. Permissions are enforced at the service layer via `@PreAuthorize`. See `planning.md` §Matriz de Permisos for the full permission table.

**Account lockout:** 5 failed login attempts → locked 15 min (`lockedUntil` field on `User`).

**CSRF:** Disabled (stateless JWT API). Requires strict CORS — only the frontend origin, never `*`.

**SQL injection:** Strictly JPA parameterised queries. No `nativeQuery = true` with dynamic strings. Dynamic ORDER BY must use an explicit allowlist.

**XSS:** CSP header (`default-src 'self'; script-src 'self'`). `dangerouslySetInnerHTML` with user data is forbidden.

---

## OWASP Top 10 — All Versions (2010 · 2013 · 2017 · 2021 · 2025)

The rules below cover the **union** of every OWASP Top 10 edition published to date (including the 2025 RC1). Entries marked with the existing Security Model (JWT, CSRF, XSS, SQL injection) are cross-referenced rather than repeated in full.

### A — Injection (all editions; XSS merged here in 2021)
- SQL: JPA `PreparedStatement` bind parameters only. `nativeQuery = true` with dynamic strings is forbidden. Dynamic `ORDER BY` requires an explicit allowlist enum.
- OS command: never pass user input to `ProcessBuilder` / `Runtime.exec()`.
- LDAP/JPQL/SSTI: treat all external input as untrusted; use typed parameters, never string concatenation.
- XSS: React JSX escapes by default. `dangerouslySetInnerHTML` with user data is forbidden. CSP header: `default-src 'self'; script-src 'self'`.

### B — Broken Access Control (all editions; covers IDOR from 2013)
- Every service method that returns or mutates a resource must verify the caller owns or has permission over that resource — not just that the caller is authenticated.
- `@PreAuthorize` annotations live on `@Service` methods, not controllers. Controllers are not a security boundary.
- Never expose sequential integer IDs in URLs; use UUIDs so that enumeration is not practical.
- Soft-deleted records must be excluded from all queries (`WHERE deleted_at IS NULL`).

### C — Cryptographic Failures (2021) / Sensitive Data Exposure (2013 · 2017)
- Passwords: BCrypt cost 12. Never log, return, or store plaintext passwords.
- Refresh tokens: stored as SHA-256 hash only. The raw token is never persisted.
- JWT secret: HS512 in dev (≥ 64 chars), RS256 in prod. Never commit secrets; load from env vars.
- TLS in production is mandatory (Railway / Vercel enforce it). No HTTP-only endpoints in prod.
- `AuditLog.oldValues` / `newValues` must strip password hashes and tokens before writing JSONB.

### D — Insecure Design (2021)
- Threat-model each new endpoint: who can call it, what data does it expose, what state does it mutate.
- Rate-limit auth endpoints (`/api/v1/auth/login`, `/api/v1/auth/refresh`) at the Spring Security or reverse-proxy layer.
- Account lockout after 5 failed login attempts (`lockedUntil` on `User`, 15 min lock). See Security Model section.

### E — Security Misconfiguration (all editions)
- Spring Boot Actuator: expose only `/health` and `/info` publicly; all other actuator endpoints require `ADMIN` role.
- CORS: allow only the explicit frontend origin (`FRONTEND_URL` env var). Never `*`.
- `SPRING_PROFILES_ACTIVE=prod` must disable H2 console, Swagger UI, and debug-level logging.
- No default credentials in `V9__seed_demo_data.sql` that are reused in production.
- HTTP response headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security` (prod only).

### F — Vulnerable & Outdated Components / Software Supply Chain Failures (2013 · 2017 · 2021 · **A03:2025**)
- OWASP Dependency-Check runs on every PR (`ci.yml`); build fails on CVSS ≥ 7.
- Dependabot is configured for automatic dependency updates.
- Do not add new dependencies without checking their CVE history.
- **2025 addition — supply chain scope:** verify the integrity of the build pipeline itself. Pin GitHub Actions to a specific commit SHA (`uses: actions/checkout@<sha>`), not a mutable tag. Use Maven Central or a mirrored repository — do not resolve artefacts from arbitrary third-party registries. Lock the Node lockfile (`package-lock.json`) and never ignore lockfile divergences.

### G — Identification & Authentication Failures (2021) / Broken Authentication (2017)
- Session management is stateless JWT. Logout invalidates the refresh token hash in the DB.
- Access token TTL: 15 min. Refresh token TTL: 7 days.
- JWT signature algorithm must be explicitly validated on the server — reject `"alg": "none"`.
- Password reset flows (future) must use time-limited single-use tokens, not security questions.

### H — Software & Data Integrity Failures (2021) / Insecure Deserialization (2017)
- Never deserialise untrusted Java object streams (`ObjectInputStream`). Use JSON (Jackson) with a strict `@JsonTypeInfo` policy — disable polymorphic type handling on untrusted input.
- CI pipeline (`ci.yml`) verifies build artefacts; do not pull dependencies from untrusted registries.
- Frontend build artifacts are served from Vercel's CDN with sub-resource integrity (SRI) where applicable.

### I — Security Logging & Monitoring Failures (2017 · 2021)
- Every `ACCESS_DENIED`, failed login, and privilege escalation attempt must write an `AuditLog` row.
- Structured JSON logging (logstash-logback-encoder) with a correlation ID in MDC on every request.
- Micrometer counter incremented on each failed login attempt — alert threshold configurable via Actuator.
- Do not log full request bodies that may contain passwords or tokens. Log sanitised summaries.

### J — SSRF — Server-Side Request Forgery (2021)
- No endpoint should accept a user-supplied URL and fetch it server-side (e.g. webhook callbacks, avatar URLs). If such a feature is added, enforce an explicit allowlist of domains.
- Internal services (Actuator, DB) must not be reachable from the public network.

### K — XML External Entities — XXE (2017)
- If XML parsing is ever added, disable external entity resolution: `factory.setFeature("http://xml.org/sax/features/external-general-entities", false)`.
- Prefer JSON over XML for all API contracts.

### L — Unvalidated Redirects & Forwards (2010 · 2013)
- Never use a user-supplied `redirect_uri` or `next` parameter without validating it against an explicit allowlist of allowed origins.
- Frontend router redirects after login must only point to internal routes.

### M — CSRF (2010 · 2013)
- CSRF is intentionally disabled because the API is stateless (JWT in `Authorization` header, no cookies). This is safe only if CORS is strict — see E above. If cookies are ever introduced, re-enable CSRF protection immediately.

### N — Mishandling of Exceptional Conditions (**A10:2025 — new**)
- Never fail open: if an exception occurs during an authorisation check, deny access, do not grant it. Spring Security's `AccessDeniedException` must propagate; catch-all handlers must not silently swallow it and continue.
- Do not expose stack traces or internal error details to the client. `GlobalExceptionHandler` must map all unhandled exceptions to a generic `500` body with only a correlation ID.
- Business-logic errors (e.g. a vehicle already assigned, a job already completed) must return a structured error response — not an unhandled `RuntimeException` with a 500 status.
- Validate assumptions at domain boundaries: if a value that should never be null is null, throw an explicit `IllegalStateException` with a clear message rather than letting a `NullPointerException` surface unpredictably.
- Exhaustive `switch` / `when` expressions on enums: add a `default` branch that throws `IllegalStateException` so unhandled new enum values fail fast instead of silently doing nothing.

---

## Security by Design

These patterns apply at design time — before a line of code is written — and complement the OWASP rules above.

**Secure defaults.** Every new endpoint, feature flag, and configuration value must default to the most restrictive safe option. An endpoint with no explicit `@PreAuthorize` must be blocked, not permitted. Spring Security's `http.authorizeHttpRequests().anyRequest().authenticated()` enforces this globally.

**Least privilege.** The database user used by the application must only have `SELECT / INSERT / UPDATE / DELETE` on application tables — no `DROP`, `CREATE`, or superuser rights. The JWT filter runs before controllers; the filter chain grants no authority beyond what the token's role claim explicitly states.

**Minimise attack surface.** Remove or disable anything not actively used: H2 console, Swagger UI, and Spring Boot DevTools must be off in the `prod` profile. Every additional endpoint, library, and feature is additional surface; justify additions against their security cost.

**Defence in depth.** No single control is the last line of defence. Input validation → service-layer authorisation → parameterised queries → CSP headers → OWASP Dependency-Check → Semgrep SAST are independent layers; a bypass of one must not compromise the system.

**Fail securely.** On any unexpected error, deny access and log. Never fall back to a permissive state. See N (Mishandling of Exceptional Conditions) above.

**Separation of duties.** A `DRIVER` cannot create or assign their own jobs. A `WORKSHOP_STAFF` member cannot modify invoices. Encode these constraints both in `@PreAuthorize` and in domain-layer invariants so they cannot be bypassed by calling services out of sequence.

**Zero trust on internal calls.** Even though modules communicate via Spring Application Events (no network hop), listener methods must still verify that the event payload is internally consistent — not assume the publisher pre-validated everything.

**Immutability in the domain layer.** Domain value objects (`AppRole`, `VehicleCategory`, `UsageMeasure`, status enums) must be immutable. Changing an entity's status (e.g. `ACTIVE → MAINTENANCE`) must go through a named method with explicit precondition checks, not a raw setter.

**Audit by default.** Every state-changing operation must produce an `AuditLog` row. This is not optional telemetry — it is a security control. New service methods that mutate data must include audit logging as part of the method, not as an afterthought.

**Explicit trust boundaries.** User-supplied data is untrusted until validated (`@Valid` on DTOs at the controller boundary). Internal events and service return values are trusted. Never mix the two: do not pass a raw HTTP request body deeper than the controller, and do not re-validate internally produced values as if they were external.

---

## SOLID Principles

These apply to both backend (Java) and frontend (TypeScript) code throughout the project.

**Single Responsibility:** Each class/component does one thing. `@RestController` handles HTTP only; `@Service` holds business logic only; `@Repository` handles data access only. A React component either fetches data (via a custom hook) or renders UI — not both.

**Open/Closed:** Extend behaviour without modifying existing code. Use Spring's event system (`ApplicationEventPublisher`) to add side-effects to a completed job or maintenance record without touching `JobService` or `MaintenanceService`. On the frontend, extend feature hooks rather than editing shared utilities.

**Liskov Substitution:** Subtypes must be substitutable. Service interfaces (e.g. `VehicleService`) should honour their contract fully in every implementation — relevant when adding a mock/stub for tests.

**Interface Segregation:** Keep interfaces narrow. Don't add a method to `VehicleRepository` that only one caller needs and that breaks the abstraction for every other caller. Prefer separate Spring Data repository interfaces per aggregate over a single god-repository.

**Dependency Inversion:** Depend on abstractions, not concretions. Backend services must be injected via constructor (never `@Autowired` on a field) so they can be unit-tested without a Spring context. Frontend hooks expose a typed API contract; components never import Axios directly.

---

## Inter-module Events

Modules communicate via **Spring Application Events** — no external broker. All listeners use `@TransactionalEventListener(phase = AFTER_COMMIT)`.

| Event | Publisher | Consumers |
|-------|-----------|-----------|
| `JobCompletedEvent` | `JobService` | `VehicleService` (updates km/h), `BillingService` (creates invoice line) |
| `VehicleEntersWorkshopEvent` | `MaintenanceService` | `VehicleService` (sets status → MAINTENANCE) |
| `MaintenanceCompletedEvent` | `MaintenanceService` | `VehicleService` (sets status → ACTIVE) |

---

## Database

**PostgreSQL 16.** Migrations managed by **Flyway** under `src/main/resources/db/migration/`.

Migration sequence: V1 users → V2 clients → V3 vehicles → V4 workers (+ driver_vehicle_assignments) → V5 jobs (+ usage_logs) → V6 maintenance/workshop → V7 invoices (+ supplier_invoices) → V8 gps/audit → V9 vehicle license plate unique index → V10 worker national ID unique index → V11 maintenance_records `deleted_at` soft-delete column (Hito 24) → V12 maintenance_records `category` (preventive/corrective, standalone addendum) → V13 workshop_schedules `deleted_at` soft-delete column (Hito 26) → V14 workshop_schedules/maintenance_records start/end time-of-day columns (pending, Hito 28) → V15 seed demo data (pending, Hito 43). V1–V8 shipped the full schema (including tables for features not yet implemented at the application layer) in the initial scaffold commit; V14 and V15 remain. Note: `maintenance_records` shipped in V6 without `deleted_at`; V11 adds it so the entity can adopt the standard `@SQLRestriction("deleted_at IS NULL")` soft-delete pattern like every other entity. Same rationale applies to `workshop_schedules` and V13 (Hito 26).

**Key schema notes:**
- `DriverVehicleAssignment.end_date IS NULL` = active assignment. A partial unique index enforces one active vehicle per driver.
- `Vehicle` is a single table with nullable fields discriminated by `vehicleCategory` (LIGHT/HEAVY_VEHICLE/HEAVY_MACHINERY). `licensePlate` is nullable (heavy machinery has none). Usage tracked by `usageMeasure` (KILOMETERS or HOURS).
- `AuditLog.oldValues` / `newValues` are JSONB.
- `Invoice` numbers follow the format `INV-2026-00001`.
- Passwords: BCrypt cost 12.

---

## CI/CD (`.github/workflows/`)

`ci.yml` — triggered on PRs: tests + OWASP Dependency-Check + Semgrep SAST.  
`security.yml` — weekly scheduled OWASP scan.

OWASP check fails the build if any dependency has CVSS ≥ 7.

---

## GPS Mock

Backend: `@Scheduled(fixedDelay = 30_000)` generates positions for all `ACTIVE` vehicles.  
Frontend: polls `/api/v1/gps/latest` every 10 seconds via Leaflet + react-leaflet + OpenStreetMap (no API key required).

---

## Deployment

**Zero-cost recommended:** Frontend → Vercel; Backend + DB → Railway.

**Local demo:**
```bash
docker compose up
ngrok http 8080    # temporary public URL
```

**Required production env vars:**
```
SPRING_DATASOURCE_URL
JWT_SECRET          # min 64 chars
SPRING_PROFILES_ACTIVE=prod
FRONTEND_URL        # e.g. https://fleetmgm.vercel.app
```
