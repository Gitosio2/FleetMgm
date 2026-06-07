# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FleetMgm is a Master's thesis fleet management application. Backend: Java 21 + Spring Boot 3.3. Frontend: React + Vite + TypeScript. The project is greenfield — source code is scaffolded incrementally per `planning.md`, which is the source of truth for all architectural decisions.

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

### Frontend (`frontend/`)

```bash
npm run dev          # Start Vite dev server (port 5173)
npm run build        # Production build
npm run lint         # ESLint
npm run test         # Vitest (unit + component tests)
npm run test:ui      # Vitest UI mode
```

### Full stack

```bash
docker compose up    # Starts postgres:16 + backend + frontend (nginx)
```

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

### Frontend: Feature-scoped components

```
src/
  api/client.ts          Axios instance with JWT Bearer interceptor + auto-refresh
  store/authStore.ts     Zustand — user session and preferences
  pages/                 One file per route (Login, Dashboard, Vehicles, …)
  components/            Sub-divided by feature (vehicle/, worker/, job/, map/, billing/, workshop/)
  hooks/                 TanStack Query wrappers (useVehicles, useJobs, …)
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

## OWASP Top 10 — All Versions (2010 · 2013 · 2017 · 2021)

The rules below cover the **union** of every OWASP Top 10 edition published to date. Entries marked with the existing Security Model (JWT, CSRF, XSS, SQL injection) are cross-referenced rather than repeated in full.

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

### F — Vulnerable & Outdated Components (2013 · 2017 · 2021)
- OWASP Dependency-Check runs on every PR (`ci.yml`); build fails on CVSS ≥ 7.
- Dependabot is configured for automatic dependency updates.
- Do not add new dependencies without checking their CVE history.

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

Migration sequence: V1 users → V2 clients → V3 vehicles → V4 workers → V5 jobs → V6 maintenance/workshop → V7 invoices → V8 gps/audit → V9 seed demo data.

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
