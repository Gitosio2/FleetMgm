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
