# FleetMgm — Plan Arquitectónico Completo

## Context

Aplicación de gestión de flotas con backend Spring Boot + frontend React.
Repositorio greenfield. Plazo: ~6 semanas (hasta mediados de julio 2026).
El objetivo es una app funcional y bien estructurada arquitectónica y securamente.

---

## Stack Tecnológico

### Stack elegido

| Capa | Tecnología | Justificación |
|------|------------|---------------|
| Backend | Java 21 + Spring Boot 3.5 | Ecosistema maduro; Spring Security, Data JPA, Actuator y Validation out-of-the-box. Stack estándar y ampliamente adoptada en producción. Subido desde 3.3 en el Hito 11 — la línea 3.3.x llegó a su último patch (3.3.13) con CVEs CVSS ≥ 7 sin resolver en Spring Core/Security/Tomcat. |
| ORM | Spring Data JPA + Hibernate | El dominio es relacional (vehículos, trabajos, facturas con integridad referencial fuerte); JPA encaja de forma natural. |
| Seguridad | Spring Security + JJWT (HS512 → RS256 en prod) | RBAC battle-tested. JWT con access token 15 min + refresh token 7 días almacenado hasheado en BD. |
| BD | PostgreSQL 16 | JSONB para AuditLog, integridad referencial, transacciones ACID. |
| Migraciones | Flyway | Migraciones versionadas, reproducibles y auditables desde el primer commit. |
| Frontend | React + Vite + TypeScript | SPA con tipado estático; Vite para HMR rápido en desarrollo. |
| UI | shadcn/ui + Tailwind CSS | Componentes accesibles sin opinión sobre diseño; Tailwind portable a React Native (NativeWind) si se añade móvil. |
| Estado servidor | TanStack Query | Cache, invalidación y refetch declarativos sin Redux. |
| Estado cliente | Zustand | Store minimalista para sesión de usuario y preferencias. |
| Mapa | Leaflet + react-leaflet + OpenStreetMap | Sin API key, open source, suficiente para un mapa de flota. |
| Tests backend | JUnit 5 + Mockito + Testcontainers | Tests de integración con PostgreSQL real en contenedor. |
| Tests frontend | Vitest + React Testing Library + MSW | MSW intercepta llamadas API para tests sin servidor real. |
| Monorepo | Turborepo + npm workspaces | Pipeline de tasks cacheado; lógica compartida entre web y mobile sin duplicación. |
| CI/CD | GitHub Actions | Pipeline declarativo en YAML, sin infraestructura adicional. |
| SAST | Semgrep | Reglas específicas para Spring Security y React. |
| CVE scan | OWASP Dependency-Check | Escanea dependencias transitivas contra NVD; falla el build si CVSS ≥ 7. |
| Docs API | SpringDoc OpenAPI (Swagger UI) | Generación automática desde anotaciones; útil para explorar y probar la API interactivamente. |
| Contenedores | Docker Compose | Un solo comando levanta la app completa para la presentación. |

### Seguridad por capas

**SQL Injection:** JPA/Hibernate genera `PreparedStatement` con bind parameters. El SQL y los datos nunca se concatenan. Regla: prohibir `nativeQuery = true` con strings dinámicos; si ORDER BY es dinámico, usar allowlist explícita.

**CSRF:** Deshabilitado intencionadamente. La API es stateless con JWT en el header `Authorization: Bearer`. Sin cookie automática no hay vector CSRF. Contrapartida obligatoria: CORS estricto (solo el origen del frontend, nunca `*`).

**XSS:** Defensa en profundidad — Spring Security añade `X-Content-Type-Options: nosniff` automáticamente; se configura CSP header (`default-src 'self'; script-src 'self'`); React JSX escapa valores dinámicos por defecto. Prohibido `dangerouslySetInnerHTML` con datos de usuario.

**Auth:** JWT HS512 en desarrollo (secret ≥ 64 chars), access token 15 min, refresh token 7 días almacenado hasheado en BD (revocación real en logout). Lockout tras 5 intentos fallidos (15 min, campo `lockedUntil` en User). BCrypt cost 12.

**CVEs:** OWASP Dependency-Check en cada PR + Dependabot para actualizaciones automáticas.

### Mantenibilidad

- Nunca exponer entidades JPA en la API → siempre DTOs (MapStruct para mapeo). Los cambios de esquema no rompen el contrato API.
- Lógica de negocio únicamente en la capa `application/` (`@Service`). Controllers: routing + validación de entrada. Repositories: acceso a datos.
- Constructor injection en lugar de `@Autowired` en campo → tests unitarios sin contexto Spring.
- Spring Boot no enforce la separación de capas: es por convención. La disciplina es del equipo.

### Observabilidad y auditoría

- **Spring Boot Actuator:** `/actuator/health`, `/actuator/metrics`, `/actuator/loggers` (cambio de log level en caliente).
- **Micrometer:** contadores para intentos fallidos de login, timers para latencia de endpoints, gauges para trabajos activos.
- **Structured logging JSON** (logstash-logback-encoder) con correlation ID en MDC para trazar un request de extremo a extremo.
- **AuditLog entity:** registra CREATE/UPDATE/DELETE/LOGIN/ACCESS_DENIED con userId, IP, timestamp, valores antes/después (JSONB). Spring Data JPA Auditing (`@CreatedBy`, `@LastModifiedBy`, `@CreatedDate`) automático.

---

## Arquitectura

### Patrón: Monolito en capas, package-by-feature

No hexagonal (over-engineering para 6 semanas). Monolito modular con límites entre features:

```
com.fleetmgm
├── vehicle/
│   ├── api/          ← @RestController
│   ├── application/  ← @Service (lógica de negocio, @Transactional)
│   ├── domain/       ← @Entity, value objects (sin dependencias Spring)
│   ├── infrastructure/  ← JpaRepository, llamadas externas
│   └── dto/          ← Request/Response DTOs (MapStruct para mapeo)
├── worker/
├── client/
├── job/
├── billing/
├── workshop/
├── gps/
├── auth/
└── shared/           ← Excepciones globales, AuditLog, paginación
```

---

## Modelo de Dominio

### Entidades y campos

**User** — id(UUID), email(unique), passwordHash(BCrypt cost 12), appRole(ADMIN/MANAGER/ADMINISTRATIVE/WORKSHOP_STAFF/DRIVER), enabled, failedLoginAttempts, lockedUntil, soft-delete

**RefreshToken** — id, userId(FK), tokenHash(SHA-256 del token real), expiresAt

**Client** — id(UUID), name, taxId(CIF/NIF, unique), email, phone, address, soft-delete

**Vehicle** — id(UUID), vehicleCategory(LIGHT/HEAVY_VEHICLE/HEAVY_MACHINERY), usageMeasure(KILOMETERS/HOURS), heavySubtype(nullable), licensePlate(nullable — maquinaria), make, model, year, vin, color, status(ACTIVE/MAINTENANCE/INACTIVE/DECOMMISSIONED), currentKm(nullable), currentHours(nullable), soft-delete

**Worker** — id, userId(FK→User, nullable), firstName, lastName, role(DRIVER/TECHNICIAN/BOTH), phone, nationalId(DNI/NIE), licenseType(B/C/D/CE…), licenseExpiry, soft-delete

**DriverVehicleAssignment** — id, driverId(FK), vehicleId(FK), startDate, endDate(nullable=activa), assignedByUserId, notes
> Asignación activa: `WHERE end_date IS NULL`. Unique index parcial garantiza un solo vehículo activo por conductor.

**Job** — id, title, description, vehicleId(FK), assignedDriverId(FK), clientId(FK→Client, nullable), status(PENDING/IN_PROGRESS/COMPLETED/CANCELLED), originLocation, destinationLocation, notes, scheduledStart/End, actualStart/End, startUsageValue, endUsageValue, soft-delete

**UsageLog** — id, vehicleId, value(Long), measureType(KILOMETERS/HOURS), recordedAt, source(MANUAL/GPS/JOB_COMPLETION), jobId(nullable)

**MaintenanceRecord** — id, vehicleId, type, description, usageAtService, cost(BigDecimal), workshopEntryDate, workshopExitDate, technicianId, invoiceId(nullable), status(SCHEDULED/IN_PROGRESS/COMPLETED)

**WorkshopSchedule** — id, vehicleId, technicianId, maintenanceRecordId, scheduledDate, type, priority, status, notes

**Invoice** — id, invoiceNumber(INV-2026-00001), clientId(FK→Client), status(DRAFT/ISSUED/PAID/OVERDUE), issueDate, dueDate, paymentDate, taxRate(21%), subtotal, taxAmount, total, notes, soft-delete

**InvoiceLineItem** — id, invoiceId, description, quantity, unitPrice, subtotal, linkedJobId(nullable), linkedMaintenanceId(nullable)

**SupplierInvoice** — id(UUID), supplierName, supplierInvoiceNumber(nullable), category(ExpenseCategory: MAINTENANCE/FUEL/INSURANCE/LEASING_RENTING/TOLL/OTHER), invoiceDate, dueDate(nullable), paymentDate(nullable), status(SupplierInvoiceStatus: PENDING/PAID), subtotal, taxAmount, total, vehicleId(FK→Vehicle, nullable), notes, documentPath(nullable — ruta a PDF/imagen subido, punto de anclaje para OCR futuro), soft-delete

**SupplierInvoiceLineItem** — id, invoiceId(FK), description, quantity, unitPrice, subtotal, vehicleId(nullable), maintenanceRecordId(nullable)

**GpsPosition** — id, vehicleId(indexed), latitude, longitude, heading, speed, recordedAt(indexed), source(MOCK/DEVICE)

**AuditLog** — id, entityType, entityId, action, performedByUserId, performedByEmail, performedAt, ipAddress, oldValues(JSONB), newValues(JSONB), details

### Rentabilidad (vista calculada, no entidad)
- Ingresos por vehículo = SUM(InvoiceLineItems vinculados a Jobs de ese vehículo)
- Costes por vehículo = SUM(MaintenanceRecord.cost) + SUM(SupplierInvoice.total WHERE vehicleId = ese vehículo)
- Margen = Ingresos − Costes
- Implementado como `@Query` projection de JPA o PostgreSQL View

---

## Matriz de Permisos

| Recurso | ADMIN | MANAGER | ADMINISTRATIVE | WORKSHOP_STAFF | DRIVER |
|---------|-------|---------|----------------|----------------|--------|
| Vehículos — ver lista/detalle | ✅ | ✅ | ✅ | ✅ estado + ficha | Solo el suyo |
| Vehículos — crear/editar/eliminar | ✅ | ✅ | ✅ | ❌ | ❌ |
| Asignación conductor↔vehículo | ✅ | ✅ | ✅ | ❌ | ❌ |
| Mantenimiento — ver | ✅ | ✅ | ✅ | ✅ vehículos del día | ❌ |
| Mantenimiento — crear/editar | ✅ | ✅ | ✅ | ✅ | ❌ |
| Órdenes taller — ver hoy/semana/mes | ✅ | ✅ | ✅ | ✅ | ❌ |
| Órdenes taller — crear/editar | ✅ | ✅ | ✅ | ✅ | ❌ |
| Trabajos — ver todos | ✅ | ✅ | ✅ | ❌ | Solo activos propios |
| Trabajos — crear/editar/asignar | ✅ | ✅ | ✅ | ❌ | ❌ |
| Trabajos — cambiar estado | ✅ | ✅ | ✅ | ❌ | Solo propios activos |
| Historial de trabajos pasados | ✅ | ✅ | ✅ | ❌ | ❌ |
| Clientes — ver/crear/editar | ✅ | ✅ | ✅ | ❌ | ❌ |
| Facturas — ver/crear/editar/pagar | ✅ | ✅ | ✅ | ❌ | ❌ |
| Facturas de proveedor (gastos) — ver/crear/editar | ✅ | ✅ | ✅ | ❌ | ❌ |
| Trabajadores — ver lista | ✅ | ✅ | ✅ | ❌ | Solo su perfil |
| Trabajadores — crear/editar | ✅ | ✅ | ✅ | ❌ | ❌ |
| GPS — mapa de flota completo | ✅ | ✅ | ✅ | ❌ | ❌ |
| GPS — posición propia | ✅ | ✅ | ✅ | ❌ | ❌ |
| Informes de rentabilidad | ✅ | ✅ | ✅ | ❌ | ❌ |
| Registro de auditoría | ✅ | ✅ | ❌ | ❌ | ❌ |
| Gestión de usuarios y roles | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## Eventos entre módulos (Spring Application Events)

`@TransactionalEventListener(phase = AFTER_COMMIT)` para desacoplar módulos sin broker externo.

| Evento | Publicado por | Consumidores |
|--------|--------------|--------------|
| `JobCompletedEvent` | JobService | VehicleService (actualiza km/h), BillingService (línea de factura) |
| `VehicleEntersWorkshopEvent` | MaintenanceService | VehicleService (status → MAINTENANCE) |
| `MaintenanceCompletedEvent` | MaintenanceService | VehicleService (status → ACTIVE) |

---

## GPS Mock

Backend: `@Scheduled(fixedDelay = 30_000)` genera posiciones para todos los vehículos ACTIVE.
Frontend: polling a `/api/v1/gps/latest` cada 10 segundos con Leaflet + react-leaflet.

---

## Estructura del Repositorio

Monorepo gestionado con **Turborepo** + npm workspaces. La lógica de datos (hooks, store, cliente API)
vive en `packages/` y es consumida tanto por `apps/web` como por `apps/mobile`, evitando duplicación.
Los componentes visuales son siempre específicos de cada app — no se comparten.

```
FleetMgm/
├── planning.md                                 ← este documento
├── package.json                                ← workspace root (Turborepo)
├── turbo.json                                  ← pipeline de tasks: dev, build, lint, test
├── tsconfig.base.json                          ← config TypeScript base compartida
│
├── backend/
│   ├── pom.xml                                 ← Java 21, Spring Boot 3.5.16
│   └── src/main/java/com/fleetmgm/
│       ├── FleetMgmApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java             ← JWT filter chain, CORS
│       │   └── AuditorAwareImpl.java
│       ├── auth/
│       │   ├── api/AuthController.java
│       │   ├── application/AuthService.java
│       │   ├── domain/{User, RefreshToken, AppRole}.java
│       │   └── infrastructure/{JwtService, JwtAuthFilter, UserRepository, RefreshTokenRepository}.java
│       ├── vehicle/
│       │   ├── api/VehicleController.java
│       │   ├── application/VehicleService.java
│       │   ├── domain/{Vehicle, DriverVehicleAssignment, UsageLog, enums}.java
│       │   └── infrastructure/{VehicleRepository, AssignmentRepository, UsageLogRepository}.java
│       ├── worker/   ← Worker CRUD + cuenta de usuario
│       ├── client/   ← Client CRUD
│       ├── job/      ← Job lifecycle + eventos
│       ├── billing/  ← Invoice + PDF export
│       ├── workshop/ ← MaintenanceRecord + WorkshopSchedule
│       ├── gps/      ← GpsPosition + @Scheduled mock
│       └── shared/   ← GlobalExceptionHandler, AuditLog, PageResponse
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│           ├── V1__create_users.sql
│           ├── V2__create_clients.sql
│           ├── V3__create_vehicles.sql
│           ├── V4__create_workers.sql                          ← incluye driver_vehicle_assignments
│           ├── V5__create_jobs.sql                              ← incluye usage_logs
│           ├── V6__create_maintenance_workshop.sql
│           ├── V7__create_invoices.sql                          ← incluye supplier_invoices
│           ├── V8__create_gps_audit.sql
│           ├── V9__add_vehicle_license_plate_unique_index.sql   ← aplicada, Hito 8
│           ├── V10__add_worker_national_id_unique_index.sql     ← aplicada, Hito 10
│           └── V11__seed_demo_data.sql                          ← pendiente, Hito 41 (única migración que falta)
│
├── packages/                                   ← lógica compartida entre web y mobile
│   ├── api/                                    ← @fleetmgm/api
│   │   └── src/
│   │       ├── client.ts                       ← instancia Axios con baseURL /api/v1
│   │       ├── types.ts                        ← PageResponse<T>, ApiError y tipos de dominio
│   │       └── index.ts
│   ├── hooks/                                  ← @fleetmgm/hooks
│   │   └── src/
│   │       ├── useAuth.ts                      ← login/logout (TanStack Query)
│   │       ├── useVehicles.ts
│   │       ├── useWorkers.ts
│   │       ├── useClients.ts
│   │       ├── useJobs.ts
│   │       ├── useAssignments.ts
│   │       ├── useWorkshop.ts
│   │       ├── useBilling.ts
│   │       ├── useGps.ts
│   │       ├── useProfitability.ts
│   │       ├── useAuditLog.ts
│   │       └── index.ts
│   └── store/                                  ← @fleetmgm/store
│       └── src/
│           ├── authStore.ts                    ← Zustand: sesión, tokens, rol
│           └── index.ts
│
├── apps/
│   ├── web/                                    ← React + Vite + TypeScript (Hito 12+)
│   │   └── src/
│   │       ├── mocks/                          ← MSW handlers (tests + desarrollo)
│   │       ├── pages/                          ← Login, Dashboard, Vehicles, Workers…
│   │       └── components/                     ← shadcn/ui + componentes por feature
│   └── mobile/                                 ← React Native + Expo (post-web)
│       └── src/
│           ├── screens/                        ← equivalente a pages/
│           └── components/                     ← NativeWind + componentes por feature
│
├── .github/workflows/
│   ├── ci.yml                                  ← Tests + OWASP + SAST en PRs
│   └── security.yml                            ← OWASP semanal programado
└── docker-compose.yml                          ← postgres:16 + backend + apps/web (nginx)
```

### Qué se comparte y qué no

| Capa | `apps/web` | `apps/mobile` | `packages/` |
|------|-----------|--------------|-------------|
| Componentes UI | shadcn/ui + Tailwind | NativeWind + RN primitives | ❌ nunca |
| Hooks de datos | `@fleetmgm/hooks` | `@fleetmgm/hooks` | ✅ |
| Estado cliente | `@fleetmgm/store` | `@fleetmgm/store` | ✅ |
| Cliente API + tipos | `@fleetmgm/api` | `@fleetmgm/api` | ✅ |

---

## Plan de Desarrollo por Hitos

> **Nota sobre orden de tareas (Strict TDD):** A partir del Hito 8, los hitos de lógica siguen
> el ciclo Red → Green → Refactor: los tests se escriben **primero** y los items de implementación
> van después. Los hitos de contrato API (entidades, DTOs, mappers, controller skeleton) no tienen
> tests propios — sus tests de controller viven en el hito de lógica correspondiente.

### Base ya implementada
- [x] `pom.xml` — Java 21, Spring Boot 3.3.5 al inicio, subido a **3.5.16** en el Hito 11 (ver Decisiones de Implementación)
- [x] `application.yml` — datasource, JWT config, actuator, springdoc
- [x] Estructura de paquetes (package-by-feature)
- [x] Entidades `User`, `RefreshToken`, `AppRole` (enum)
- [x] `JwtService` — generación y validación de tokens HS512
- [x] `JwtAuthenticationFilter` — extrae y valida Bearer token en cada request
- [x] `GlobalExceptionHandler`, `PageResponse<T>`, `AuditLog`
- [x] **Flyway V1–V8** — esquema completo de toda la base de datos, aplicado desde el commit de scaffold inicial (`aa3e61e`): `users`, `clients`, `vehicles`, `workers` + `driver_vehicle_assignments` (V4), `jobs` + `usage_logs` (V5), `maintenance_records` + `workshop_schedules` (V6), `invoices` + `invoice_line_items` + `supplier_invoices` + `supplier_invoice_line_items` (V7), `gps_positions` + `audit_logs` (V8)
- [x] **Entidades de dominio** (capa `domain/` únicamente — sin `api/`, `application/`, `dto/`) para todas las features: `Job`/`JobStatus`, `UsageLog`, `MaintenanceRecord`/`MaintenanceStatus`, `WorkshopSchedule`/`SchedulePriority`/`WorkshopStatus`, `Invoice`/`InvoiceLineItem`/`InvoiceStatus`, `SupplierInvoice`/`SupplierInvoiceLineItem`/`SupplierInvoiceStatus`/`ExpenseCategory`, `GpsPosition`/`GpsSource`, `DriverVehicleAssignment`
- [x] `AssignmentRepository` (parcial) — `findActiveByDriverEmail`, usado por `VehicleService` para el filtro "DRIVER solo ve su vehículo"
- [x] `AuditLogRepository` (parcial) — `JpaRepository` base, sin queries de filtro todavía

> **Nota:** `SupplierInvoice` (facturas de proveedor / gastos operativos: mantenimiento, combustible, seguro, leasing, peajes) se scaffoldeó junto con el resto del dominio en el commit inicial, pero no estaba documentado en este plan hasta esta revisión — ver Hitos 30–31. Afecta directamente el cálculo de rentabilidad (ver Modelo de Dominio).

---

### Hito 1 — Auth: SecurityConfig *(implementado — pre Gentle-AI)*
- [x] `SecurityConfig` — `HttpSecurity`: sesión stateless, deshabilitar CSRF, registrar `JwtAuthenticationFilter`
- [x] `SecurityConfig` — endpoints públicos (`/api/v1/auth/**`, `/actuator/health`) vs. autenticados (`anyRequest().authenticated()`)
- [x] `SecurityConfig` — CORS: origen permitido desde env var `FRONTEND_URL`, métodos y headers permitidos, nunca `*`

### Hito 2 — Auth: Contrato API *(implementado — pre Gentle-AI)*
- [x] `Flyway V1` — tabla `users` + tabla `refresh_tokens`
- [x] `LoginRequest` / `AuthResponse` / `RefreshRequest` (records) con `@Valid`
- [x] `AuthController` — `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`

### Hito 3 — Auth: Lógica de login y lockout *(implementado — pre Gentle-AI)*
- [x] `AuthService.login()` — verificar credenciales BCrypt, comprobar cuenta habilitada, generar access token (15 min) + refresh token
- [x] `AuthService` — lockout: incrementar `failedLoginAttempts`; bloquear `lockedUntil = now + 15 min` al 5.º fallo; resetear en login exitoso
- [x] `AuthService` — registrar `AuditLog` en login exitoso y en bloqueo de cuenta
- [x] Tests `AuthServiceTest` — login OK, contraseña incorrecta, cuenta deshabilitada, lockout tras 5 intentos

### Hito 4 — Auth: Refresh, logout y tests de controller *(implementado — pre Gentle-AI)*
- [x] `AuthService.refresh()` — validar hash SHA-256 del refresh token en BD, emitir nuevo access token
- [x] `AuthService.logout()` — eliminar hash del refresh token de BD (revocación real)
- [x] Tests `AuthServiceTest` — refresh válido, refresh expirado, logout
- [x] Tests `AuthControllerTest` (`@WebMvcTest`) — 200, 400, 401, cuerpo de error estructurado
- [x] `DataInitializer` — seed usuarios demo en perfil `dev` (1 ADMIN, 1 MANAGER, 1 DRIVER) con BCrypt cost 12

> **Entregable hitos 1–4:** Login funcional con JWT, lockout, refresh y logout. SpringDoc expone el spec `/api-docs`.

---

### Hito 5 — Clientes: Contrato API *(implementado — pre Gentle-AI)*
- [x] `Flyway V2` — tabla `clients`
- [x] `Client` entity — campos, `@SQLRestriction("deleted_at IS NULL")`
- [x] `CreateClientRequest` / `UpdateClientRequest` / `ClientResponse` (records)
- [x] `ClientMapper` (MapStruct) — `toResponse`, `toEntity`, `updateEntity`
- [x] `ClientController` — `GET /api/v1/clients`, `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`; `Location` header en POST, `204` en DELETE

### Hito 6 — Clientes: Lógica e implementación *(implementado — pre Gentle-AI)*
- [x] `ClientRepository` — `findAll` paginado, `existsByTaxId`
- [x] `ClientService` — `create`, `findAll`, `findById`, `update`, `delete` (soft), `@Transactional`
- [x] `@PreAuthorize` en `ClientService` — solo ADMIN/MANAGER/ADMINISTRATIVE
- [x] Tests `ClientServiceTest` — crear OK, taxId duplicado → excepción, findById no encontrado → 404, soft delete
- [x] Tests `ClientRepositoryTest` (`@DataJpaTest` + Testcontainers PostgreSQL 16) — soft delete excluye registros con `deleted_at`
- [x] Tests `ClientControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403

---

### Hito 7 — Vehículos: Contrato API *(implementado — pre Gentle-AI)*
- [x] `Flyway V3` — tabla `vehicles`
- [x] `Vehicle` entity — campos, enums `VehicleCategory` / `VehicleStatus` / `UsageMeasure`, `@SQLRestriction`
- [x] `CreateVehicleRequest` / `UpdateVehicleRequest` / `VehicleResponse` (records)
- [x] `VehicleMapper` (MapStruct)
- [x] `VehicleController` — CRUD completo

### Hito 8 — Vehículos: Lógica e implementación *(implementado)*
- [x] **[RED]** Tests `VehicleServiceTest` — crear OK, licensePlate duplicada → excepción, findById no encontrado, soft delete, @PreAuthorize DRIVER solo ve el suyo
- [x] **[RED]** Tests `VehicleRepositoryTest` (`@DataJpaTest` + Testcontainers) — findAllActiveWithAssignment excluye soft-deleted y trae JOIN FETCH
- [x] **[RED]** Tests `VehicleControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403 por rol
- [x] **[GREEN]** `VehicleRepository` — `findAll` paginado, `findAllActiveWithAssignment` (JOIN FETCH)
- [x] **[GREEN]** `VehicleService` — `create`, `findAll`, `findById`, `update`, `delete` (soft), `@Transactional`
- [x] **[GREEN]** `@PreAuthorize` en `VehicleService` — ADMIN/MANAGER/ADMINISTRATIVE crean/editan; DRIVER solo ve el suyo

---

### Hito 9 — Trabajadores: Contrato API *(implementado)*
- [x] `Flyway V4` — tabla `workers`
- [x] `Worker` entity — campos, enums `WorkerRole` (DRIVER/TECHNICIAN/BOTH), `LicenseType`, `@SQLRestriction`
- [x] `CreateWorkerRequest` / `UpdateWorkerRequest` / `WorkerResponse` (records)
- [x] `WorkerMapper` (MapStruct)
- [x] `WorkerController` — CRUD completo

### Hito 10 — Trabajadores: Lógica e implementación *(implementado)*
- [x] **[RED]** Tests `WorkerServiceTest` — crear OK, nationalId duplicado → excepción, findById no encontrado, soft delete, @PreAuthorize DRIVER solo ve su perfil
- [x] **[RED]** Tests `WorkerRepositoryTest` (`@DataJpaTest` + Testcontainers) — soft delete excluye registros con `deleted_at`, existsByNationalId
- [x] **[RED]** Tests `WorkerControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403 por rol
- [x] **[GREEN]** `WorkerRepository` — `findAll` paginado, `existsByNationalId`
- [x] **[GREEN]** `WorkerService` — `create`, `findAll`, `findById`, `update`, `delete` (soft), `@Transactional`
- [x] **[GREEN]** `@PreAuthorize` en `WorkerService` — ADMIN/MANAGER/ADMINISTRATIVE gestionan; DRIVER solo ve su perfil

---

> **Nota sobre esta reorganización (revisión post-Hito 10):** el orden original tenía tres problemas
> detectados en auditoría: (1) el CI/CD vivía en el último hito, así que el control de seguridad
> "OWASP Dependency-Check en cada PR" declarado en el Stack no estaba activo durante los primeros ~30
> PRs; (2) los 12 hitos de frontend estaban agrupados al final, arriesgando llegar a la demo con
> backend completo pero UI inexistente si el tiempo se acorta; (3) `SupplierInvoice` (facturas de
> proveedor) estaba scaffoldeado en el commit inicial sin hito asignado. Los tres se corrigen aquí:
> CI adelantado al Hito 11, frontend intercalado inmediatamente después de cada bloque de backend del
> que depende, y un hito propio para facturas de proveedor. También se eliminaron las migraciones
> Flyway ya cubiertas por el scaffold inicial (V1–V8) de los checklists de "Contrato API" — solo
> falta añadir DTOs, mapper y controller sobre el esquema y las entidades que ya existen.

### Hito 11 — CI/CD: Pipeline mínimo *(adelantado desde el final del plan — implementado)*
- [x] Plugin `dependency-check-maven` 12.2.2 en `pom.xml` con `failBuildOnCVSS = 7`
- [x] `.github/workflows/ci.yml` — en cada PR y push a `main`: `./mvnw test` (backend) + `./mvnw dependency-check:check` (OWASP)
- [x] `.github/workflows/security.yml` — OWASP scan semanal programado (`schedule: cron`, lunes 06:00 UTC)
- [x] Maven Wrapper (`mvnw`/`mvnw.cmd` + `.mvn/wrapper/`) añadido — no existía pese a estar documentado en `CLAUDE.md`
- [x] Verificado localmente: `./mvnw test` (60/60) y `./mvnw dependency-check:check` (`BUILD SUCCESS`) tras subir `spring-boot-starter-parent` 3.3.5 → **3.5.16** (la línea 3.3.x llegó a su último patch con CVEs CVSS ≥ 7 sin resolver en Spring Core/Security/Tomcat) + overrides de `postgresql`, `log4j2`, `jackson-bom`, `tomcat.version`, y bump de `springdoc-openapi-starter-webmvc-ui` a 2.8.17
- [ ] Anclar `actions/checkout` / `actions/setup-java` a SHA concreto — diferido al Hito 41 (hardening final); por ahora usan tag `@v4`

---

### Hito 12 — Monorepo: Scaffold Turborepo
- [ ] `package.json` raíz — `workspaces: ["apps/*", "packages/*"]`, scripts `turbo dev/build/lint/test`
- [ ] `turbo.json` — pipeline de tasks con caché: `build` (dependsOn `^build`), `dev` (persistent), `lint`, `test`
- [ ] `tsconfig.base.json` — config TypeScript base compartida por todos los paquetes
- [ ] `packages/api/` — `package.json` (`@fleetmgm/api`), `tsconfig.json`, `src/client.ts` (instancia Axios vacía), `src/types.ts` (`PageResponse<T>`, `ApiError`), `src/index.ts`
- [ ] `packages/hooks/` — `package.json` (`@fleetmgm/hooks`, peerDep React), `tsconfig.json`, `src/index.ts` vacío
- [ ] `packages/store/` — `package.json` (`@fleetmgm/store`), `tsconfig.json`, `src/index.ts` vacío
- [ ] `apps/web/` — `package.json` con deps `@fleetmgm/api`, `@fleetmgm/hooks`, `@fleetmgm/store`; scaffold Vite (`npm create vite@latest`)
- [ ] `apps/mobile/` — `package.json` placeholder con deps `@fleetmgm/api`, `@fleetmgm/hooks`, `@fleetmgm/store`
- [ ] `npm install` en raíz — instala workspaces y verifica que los paquetes se resuelven entre sí
- [ ] Ampliar `.github/workflows/ci.yml` con job `turbo test` (packages + apps/web) ahora que el monorepo existe

### Hito 13 — Frontend: Infraestructura base y auth
> Requiere: Hito 12 (monorepo scaffold) + Hitos 1–4 (backend auth, ya implementados)
- [x] **[RED]** Handlers MSW en `apps/web/src/mocks/handlers.ts` — `POST /api/v1/auth/login` (OK, 401 genérico), `POST /api/v1/auth/refresh`
- [x] **[RED]** Tests `Login.test.tsx` — render formulario, submit OK redirige al dashboard, error 401 muestra mensaje genérico "Invalid credentials"
  > **Nota (revisión Hito 13):** el backend devuelve el mismo `401 INVALID_CREDENTIALS` tanto para contraseña incorrecta como para cuenta bloqueada (`AuthService.login()`, rama `user.isLocked()`) — no expone un código distinguible a propósito, para que un atacante no pueda confirmar por enumeración si una cuenta existe o está bloqueada (OWASP G — Identification & Authentication Failures). El frontend no muestra un aviso específico de "cuenta bloqueada"; el mensaje genérico ante 401 es la decisión de seguridad correcta, no una limitación pendiente de resolver.
- [x] **[RED]** Tests `authStore.test.ts` — login persiste sesión, logout limpia el store, tokens se refrescan ante 401
- [x] **[RED]** Tests `ProtectedRoute.test.tsx` — sin sesión redirige a Login; rol insuficiente muestra 403
- [x] **[GREEN]** `packages/api/src/client.ts` — interceptor JWT + lógica de auto-refresh al recibir 401
- [x] **[GREEN]** Setup MSW — `apps/web/src/mocks/browser.ts` (desarrollo), `apps/web/src/mocks/server.ts` (tests Vitest)
- [x] **[GREEN]** `packages/store/src/authStore.ts` — sesión de usuario (email, rol, tokens), acciones login/logout
- [x] **[GREEN]** `packages/hooks/src/useAuth.ts` — wraps login/logout mutations de TanStack Query
- [x] **[GREEN]** Página `Login` — formulario, manejo de error 401 con mensaje genérico
- [x] **[GREEN]** Layout principal — sidebar con navegación filtrada por rol del usuario autenticado
- [x] **[GREEN]** Rutas protegidas — redirige a Login si no hay sesión; 403 si rol insuficiente

### Hito 14 — Frontend: Clients
> Requiere: Hitos 5–6 (backend clients, ya implementados)
- [x] **[RED]** Handlers MSW — `GET /api/v1/clients`, `POST`, `PUT /{id}`, `DELETE /{id}`
- [x] **[RED]** Tests `Clients.test.tsx` — lista paginada renderiza, modal crear llama a POST, modal editar llama a PUT, soft delete pide confirmación y llama a DELETE, acciones ocultas si rol DRIVER
  > **Nota (revisión Hito 14):** el test se escribió después de `useClients`/los componentes, no antes — se rompió el orden RED→GREEN por avanzar paso a paso sin pausar en el ítem de test. Decisión tomada con el usuario: no revertir el código ya validado (typecheck+lint+build limpios), sino escribir el test contra la implementación existente. No fue un RED real (no pudo fallar primero), pero cierra la cobertura del checklist. Próximos hitos deben pausar explícitamente en cada ítem `[RED]` antes de tocar código de implementación.
- [x] **[GREEN]** `packages/hooks/src/useClients.ts` — lista paginada, create, update, delete con invalidación de caché
- [x] **[GREEN]** `apps/web/src/components/client/` — `ClientTable`, `ClientFormModal`, `ClientDeleteButton` (con confirmación vía `AlertDialog` antes de borrar)
- [x] **[GREEN]** Página `Clients` — composición de componentes, paginación

### Hito 15 — Frontend: Vehicles *(implementado)*
> Requiere: Hitos 7–8 (backend vehicles, ya implementados)
- [x] **[RED]** Handlers MSW — `GET /api/v1/vehicles`, `POST`, `PUT /{id}`, `DELETE /{id}`
- [x] **[RED]** Tests `Vehicles.test.tsx` — lista paginada renderiza, badge de estado correcto por `VehicleStatus`, DRIVER solo ve su vehículo, CRUD oculto para DRIVER
- [x] **[GREEN]** `packages/hooks/src/useVehicles.ts` — lista paginada, create, update, delete con invalidación de caché
- [x] **[GREEN]** `apps/web/src/components/vehicle/` — `VehicleTable`, `VehicleStatusBadge`, `VehicleFormModal`, `VehicleDeleteButton`
- [x] **[GREEN]** Página `Vehicles` — composición de componentes, paginación
  > **Nota (revisión Hito 15):** esta vez se respetó el orden RED→GREEN (test fallando confirmado por falta de `./Vehicles` antes de escribir cualquier componente). Además, al notar que `useClients`/`useVehicles` eran mecánicamente idénticos (mismo query/mutation/invalidate, solo cambia el tipo y el path), se extrajo `packages/hooks/src/createCrudHooks.ts` como factory genérica y se refactorizó `useClients` para usarla — decisión tomada con el usuario tras discutir cuándo generalizar (hook: seguro con 2 casos idénticos comprobados; tabla/formulario: se deja específico por feature, a la espera de un tercer caso real en Workers). Verificado end-to-end con Playwright headless contra el dev server con MSW: ADMIN ve la lista con badges de estado y puede crear/editar/borrar; DRIVER ve solo su vehículo asignado (mock fijo, sin tabla de asignaciones) y no ve acciones de gestión.

### Hito 16 — Frontend: Workers
> Requiere: Hitos 9–10 (backend workers, ya implementados)
- [x] **[RED]** Handlers MSW — `GET /api/v1/workers`, `POST`, `PUT /{id}`, `DELETE /{id}`
- [x] **[RED]** Tests `Workers.test.tsx` — lista paginada renderiza, DRIVER solo ve su perfil, CRUD oculto para DRIVER
- [x] **[GREEN]** `packages/hooks/src/useWorkers.ts` — lista paginada, create, update, delete con invalidación de caché (vía `createCrudHooks`)
- [x] **[GREEN]** `apps/web/src/components/worker/` — `WorkerTable`, `WorkerRoleBadge`, `WorkerDeleteButton`, `WorkerFormModal`
- [x] **[GREEN]** Página `Workers` — composición de componentes, paginación, ruta `/workers` sin restricción de rol (igual que Vehicles)
  > **Nota (revisión Hito 16):** tercer caso real de CRUD paginado. `useWorkers` confirma que `createCrudHooks` generaliza sin fricción a un tercer dominio. La capa de tabla/formulario se mantuvo específica por feature (como se decidió en Hito 15) — `WorkerTable`/`WorkerFormModal` no comparten código estructural real con `VehicleTable`/`VehicleFormModal` más allá del layout visual (columnas y campos son de dominio distinto), así que no se generalizó esa capa.

---

### Hito 17 — Asignaciones conductor↔vehículo: Contrato API
- [x] `Flyway V4` — tabla `driver_vehicle_assignments` con unique partial index (`WHERE end_date IS NULL`) *(ya aplicada, incluida en la migración de workers)*
- [x] `DriverVehicleAssignment` entity *(ya scaffoldeada)*
- [x] `CreateAssignmentRequest` / `AssignmentResponse` (records)
- [x] `AssignmentMapper` (MapStruct)
- [x] `AssignmentController` — `POST /api/v1/assignments` (asignar), `PATCH /{id}/end` (finalizar), `GET /api/v1/workers/{id}/assignments` (historial)
  > **Nota (revisión Hito 17):** `AssignmentController` no usa `@RequestMapping` de clase porque el contrato mezcla dos bases de recursos (`/api/v1/assignments` y `/api/v1/workers/{id}/assignments`); cada método declara su ruta completa. `AssignmentService` se creó como stub (`UnsupportedOperationException`, no listado en este checklist) solo para que el controller compile — decisión explícita para que el Hito 18 arranque en RED de verdad, sin lógica adelantada.

### Hito 18 — Asignaciones: Lógica e implementación
- [x] **[RED]** Tests `AssignmentServiceTest` — asignar OK, conductor ya tiene asignación activa → excepción, finalizar asignación, @PreAuthorize solo ADMIN/MANAGER/ADMINISTRATIVE
- [x] **[RED]** Tests `AssignmentRepositoryTest` (`@DataJpaTest` + Testcontainers) — findActiveByDriverId, unique partial index garantiza una sola activa
- [x] **[RED]** Tests `AssignmentControllerTest` (`@WebMvcTest`) — 201, 400, 404, 409 (sin 403: `@PreAuthorize` vive en el service y un `@MockBean` en `@WebMvcTest` salta el proxy AOP — mismo gap heredado de Worker/Vehicle, no cerrado acá)
- [x] **[GREEN]** `AssignmentRepository` — ampliado con `findActiveByDriverId`, `findActiveByVehicleId` (solo query, sin regla de negocio adicional — el índice único solo protege un vehículo activo por conductor, no al revés), historial paginado (`findByDriverId`)
- [x] **[GREEN]** `AssignmentService.assign()` — 404 si falta driver/vehicle, 409 `ASSIGNMENT_DRIVER_ALREADY_ACTIVE` si el conductor ya tiene asignación activa, resuelve `assignedByUser` desde `SecurityContextHolder` (mismo patrón que `VehicleService`)
- [x] **[GREEN]** `AssignmentService.endAssignment()` — `endDate = now()` en la asignación
- [x] **[GREEN]** `@PreAuthorize` — solo ADMIN/MANAGER/ADMINISTRATIVE pueden asignar/finalizar/ver historial
  > **Nota (revisión Hito 18):** al correr `AssignmentRepositoryTest` con Docker se detectó que ningún `*RepositoryTest` del proyecto corría nunca (ni local ni en CI) — `pom.xml` tenía `excludedGroups=integration` hardcodeado como texto literal, no overrideable por el profile `failsafe`. Arreglado en PR aparte (`fix-backend-integration-test-execution`, base de esta rama) junto con un segundo bug que ese fix destapó: `@DataJpaTest` no escaneaba `JpaAuditingConfig`, así que `createdAt` nunca se poblaba vía `TestEntityManager`. Esta rama depende de esa PR — mergear esa primero.

### Hito 19 — Frontend: Assignments
> Requiere: Hitos 17–18 (backend assignments)
- [ ] **[RED]** Handlers MSW — `POST /api/v1/assignments`, `PATCH /{id}/end`, `GET /api/v1/workers/{id}/assignments`
- [ ] **[RED]** Tests `Assignments.test.tsx` — modal asignación crea correctamente, finalizar asignación actualiza la lista, historial paginado renderiza, 403 oculta acciones a DRIVER
- [ ] **[GREEN]** `packages/hooks/src/useAssignments.ts` — assign, endAssignment, historial paginado
- [ ] **[GREEN]** `apps/web/src/components/assignment/` — `AssignmentModal`, `AssignmentHistory`
- [ ] **[GREEN]** Panel de asignación integrado en página `Vehicles` (detalle de vehículo)

---

### Hito 20 — Trabajos: Contrato API
- [x] `Flyway V5` — tablas `jobs` + `usage_logs` *(ya aplicada)*
- [x] `Job` entity — enum `JobStatus` (PENDING/IN_PROGRESS/COMPLETED/CANCELLED), `@SQLRestriction`; `UsageLog` entity *(ya scaffoldeadas)*
- [ ] `CreateJobRequest` / `UpdateJobRequest` / `JobResponse` (records)
- [ ] `JobMapper` (MapStruct)
- [ ] `JobController` — CRUD + `PATCH /{id}/start`, `PATCH /{id}/complete`, `PATCH /{id}/cancel`

### Hito 21 — Trabajos: Lógica y eventos
- [ ] **[RED]** Tests `JobServiceTest` — crear OK, transición PENDING→IN_PROGRESS, retroceder estado → excepción, completar publica `JobCompletedEvent`, cancelar desde COMPLETED → excepción
- [ ] **[RED]** Tests `JobEventListenerTest` — `JobCompletedEvent` crea `UsageLog` y actualiza `currentKm`/`currentHours` en Vehicle
- [ ] **[RED]** Tests `JobRepositoryTest` (`@DataJpaTest` + Testcontainers) — findByAssignedDriverIdAndStatusIn, JOIN FETCH no produce N+1
- [ ] **[RED]** Tests `JobControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403; DRIVER solo ve y cambia estado de sus trabajos
- [ ] **[GREEN]** `JobRepository` — `findAll` paginado, `findByAssignedDriverIdAndStatusIn`, JOIN FETCH
- [ ] **[GREEN]** `UsageLogRepository`
- [ ] **[GREEN]** `JobService.create()` — crear PENDING
- [ ] **[GREEN]** `JobService.start()` — PENDING → IN_PROGRESS, `actualStart = now()`
- [ ] **[GREEN]** `JobService.complete()` — IN_PROGRESS → COMPLETED, `actualEnd = now()`, publicar `JobCompletedEvent`
- [ ] **[GREEN]** `JobService.cancel()` — PENDING/IN_PROGRESS → CANCELLED
- [ ] **[GREEN]** `JobCompletedEvent` (record) + `JobEventListener` — `@TransactionalEventListener(AFTER_COMMIT)`: crear `UsageLog`, actualizar `currentKm`/`currentHours` en `Vehicle`
- [ ] **[GREEN]** `@PreAuthorize` — DRIVER solo ve y cambia estado de sus trabajos activos

### Hito 22 — Frontend: Jobs
> Requiere: Hitos 20–21 (backend jobs)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/jobs`, `POST`, `PATCH /{id}/start`, `PATCH /{id}/complete`, `PATCH /{id}/cancel`
- [ ] **[RED]** Tests `Jobs.test.tsx` — lista renderiza; DRIVER solo ve sus trabajos activos; botones start/complete/cancel aparecen según rol y estado; transición de estado actualiza el badge
- [ ] **[GREEN]** `packages/hooks/src/useJobs.ts` — lista paginada, create, start, complete, cancel con invalidación de caché
- [ ] **[GREEN]** `apps/web/src/components/job/` — `JobTable`, `JobStatusBadge`, `JobFormModal`, `JobActionButtons`
- [ ] **[GREEN]** Página `Jobs` — composición de componentes, filtro por estado

---

### Hito 23 — Mantenimiento: Contrato API
- [x] `Flyway V6` — tabla `maintenance_records` *(ya aplicada)*
- [x] `MaintenanceRecord` entity — enum `MaintenanceStatus` (SCHEDULED/IN_PROGRESS/COMPLETED) *(ya scaffoldeada)*
- [ ] `CreateMaintenanceRequest` / `MaintenanceResponse` (records)
- [ ] `MaintenanceMapper` (MapStruct)
- [ ] `MaintenanceController` — CRUD + `PATCH /{id}/start`, `PATCH /{id}/complete`

### Hito 24 — Mantenimiento: Lógica y eventos
- [ ] **[RED]** Tests `MaintenanceServiceTest` — crear publica `VehicleEntersWorkshopEvent`, completar publica `MaintenanceCompletedEvent`, transiciones de estado inválidas → excepción
- [ ] **[RED]** Tests `VehicleStatusEventTest` — `VehicleEntersWorkshopEvent` cambia Vehicle a MAINTENANCE; `MaintenanceCompletedEvent` lo devuelve a ACTIVE
- [ ] **[RED]** Tests `MaintenanceControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403; WORKSHOP_STAFF puede crear/editar
- [ ] **[GREEN]** `MaintenanceRepository`
- [ ] **[GREEN]** `VehicleEntersWorkshopEvent` + `MaintenanceCompletedEvent` (records)
- [ ] **[GREEN]** `MaintenanceService.create()` — crear SCHEDULED, publicar `VehicleEntersWorkshopEvent`
- [ ] **[GREEN]** `MaintenanceService.start()` — SCHEDULED → IN_PROGRESS
- [ ] **[GREEN]** `MaintenanceService.complete()` — IN_PROGRESS → COMPLETED, `workshopExitDate = now()`, publicar `MaintenanceCompletedEvent`
- [ ] **[GREEN]** `VehicleService` — `@TransactionalEventListener`: `VehicleEntersWorkshopEvent` → status `MAINTENANCE`; `MaintenanceCompletedEvent` → status `ACTIVE`
- [ ] **[GREEN]** `@PreAuthorize` — WORKSHOP_STAFF puede crear/editar; ADMIN/MANAGER/ADMINISTRATIVE también

### Hito 25 — Agenda del taller: Contrato API
- [x] `Flyway V6` — tabla `workshop_schedules` *(ya aplicada, misma migración que maintenance_records)*
- [x] `WorkshopSchedule` entity — prioridad, estado *(ya scaffoldeada)*
- [ ] `CreateScheduleRequest` / `ScheduleResponse` (records)
- [ ] `ScheduleMapper` (MapStruct)
- [ ] `WorkshopController` — CRUD + `GET /api/v1/workshop/schedules?range=today|week|month`

### Hito 26 — Agenda del taller: Lógica e implementación
- [ ] **[RED]** Tests `WorkshopScheduleServiceTest` — listar hoy, listar semana, listar mes, cancelar, @PreAuthorize WORKSHOP_STAFF y superiores
- [ ] **[RED]** Tests `WorkshopScheduleRepositoryTest` (`@DataJpaTest` + Testcontainers) — queries por rango de fecha devuelven solo registros del periodo correcto
- [ ] **[RED]** Tests `WorkshopControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403 por rol; parámetro `range` inválido → 400
- [ ] **[GREEN]** `WorkshopScheduleRepository` — queries por rango de fecha: hoy, semana actual, mes actual
- [ ] **[GREEN]** `WorkshopScheduleService` — crear, editar, cancelar, listar por rango
- [ ] **[GREEN]** `@PreAuthorize` — WORKSHOP_STAFF y superiores

### Hito 27 — Frontend: Workshop
> Requiere: Hitos 23–26 (backend maintenance + workshop schedules)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/workshop/schedules?range=today|week|month`, `POST`, `PATCH /{id}/cancel`; `GET /api/v1/maintenance`, `POST`, `PATCH /{id}/start`, `PATCH /{id}/complete`
- [ ] **[RED]** Tests `Workshop.test.tsx` — selector de rango filtra la lista correctamente; WORKSHOP_STAFF ve y crea órdenes; cambio de estado actualiza badge
- [ ] **[GREEN]** `packages/hooks/src/useWorkshop.ts` — lista por rango, create, cancel
- [ ] **[GREEN]** `packages/hooks/src/useMaintenance.ts` — lista, create, start, complete
- [ ] **[GREEN]** `apps/web/src/components/workshop/` — `ScheduleTable`, `ScheduleRangeSelector`, `MaintenanceTable`, `MaintenanceFormModal`
- [ ] **[GREEN]** Página `Workshop` — vista unificada de agenda y mantenimientos

---

### Hito 28 — Facturación (clientes): Contrato API
- [x] `Flyway V7` — tablas `invoices` + `invoice_line_items` *(ya aplicada)*
- [x] `Invoice` entity — enum `InvoiceStatus` (DRAFT/ISSUED/PAID/OVERDUE), `@SQLRestriction`; `InvoiceLineItem` entity *(ya scaffoldeadas)*
- [ ] `CreateInvoiceRequest` / `InvoiceResponse` / `LineItemRequest` (records)
- [ ] `InvoiceMapper` (MapStruct)
- [ ] `InvoiceController` — CRUD + `PATCH /{id}/issue`, `PATCH /{id}/pay`, `POST /{id}/line-items`

### Hito 29 — Facturación (clientes): Lógica e implementación
- [ ] **[RED]** Tests `BillingServiceTest` — crear DRAFT, emitir sin líneas → excepción, flujo completo DRAFT→ISSUED→PAID, cálculo IVA 21%, `JobCompletedEvent` crea línea en DRAFT del cliente
- [ ] **[RED]** Tests `BillingControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403; emitir factura sin líneas → 422
- [ ] **[GREEN]** `InvoiceRepository`, `LineItemRepository`
- [ ] **[GREEN]** `InvoiceNumberGenerator` — secuencia PostgreSQL `INV-2026-00001`
- [ ] **[GREEN]** `BillingService.create()` — crear DRAFT
- [ ] **[GREEN]** `BillingService.addLineItem()` — añadir línea a factura DRAFT
- [ ] **[GREEN]** `BillingService.issue()` — DRAFT → ISSUED; valida ≥1 línea; calcula subtotal, IVA, total
- [ ] **[GREEN]** `BillingService.markPaid()` — ISSUED → PAID, `paymentDate = now()`
- [ ] **[GREEN]** `BillingService` — `JobCompletedEvent` consumer: crear línea de factura en la DRAFT del cliente
- [ ] **[GREEN]** `@PreAuthorize` — solo ADMIN/MANAGER/ADMINISTRATIVE

### Hito 30 — Facturas de proveedor: Contrato API *(nuevo — sin hito asignado en el plan original)*
- [x] `Flyway V7` — tablas `supplier_invoices` + `supplier_invoice_line_items` *(ya aplicada, misma migración que invoices)*
- [x] `SupplierInvoice`, `SupplierInvoiceLineItem`, `SupplierInvoiceStatus` (PENDING/PAID), `ExpenseCategory` *(ya scaffoldeadas)*
- [ ] `CreateSupplierInvoiceRequest` / `SupplierInvoiceResponse` / `SupplierLineItemRequest` (records)
- [ ] `SupplierInvoiceMapper` (MapStruct)
- [ ] `SupplierInvoiceController` — CRUD + `PATCH /{id}/pay`, `POST /{id}/line-items`

### Hito 31 — Facturas de proveedor: Lógica e implementación *(nuevo)*
- [ ] **[RED]** Tests `SupplierInvoiceServiceTest` — crear PENDING, marcar PAID, listar por vehicleId, listar por categoría, @PreAuthorize solo ADMIN/MANAGER/ADMINISTRATIVE
- [ ] **[RED]** Tests `SupplierInvoiceControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403
- [ ] **[GREEN]** `SupplierInvoiceRepository`, `SupplierInvoiceLineItemRepository`
- [ ] **[GREEN]** `SupplierInvoiceService.create()` — crear PENDING
- [ ] **[GREEN]** `SupplierInvoiceService.markPaid()` — PENDING → PAID, `paymentDate = now()`
- [ ] **[GREEN]** `@PreAuthorize` — solo ADMIN/MANAGER/ADMINISTRATIVE

### Hito 32 — PDF y rentabilidad
- [ ] **[RED]** Tests `PdfExportServiceTest` — PDF generado contiene cabecera, líneas y totales correctos; IVA calculado al 21%
- [ ] **[RED]** Tests `ProfitabilityRepositoryTest` (`@DataJpaTest` + Testcontainers) — proyección devuelve ingresos, costes (mantenimiento + facturas de proveedor) y margen correctos por vehículo
- [ ] **[GREEN]** `PdfExportService` — generar PDF con OpenPDF (cabecera, líneas, totales, IVA)
- [ ] **[GREEN]** `GET /api/v1/invoices/{id}/pdf` — `Content-Disposition: attachment; filename="INV-...pdf"`
- [ ] **[GREEN]** `ProfitabilityRepository` — `@Query` projection: ingresos (`SUM` line items), costes (`SUM MaintenanceRecord.cost` + `SUM SupplierInvoice.total` por vehículo), margen por vehículo
- [ ] **[GREEN]** `GET /api/v1/reports/profitability` — paginado, solo ADMIN/MANAGER

### Hito 33 — Frontend: Billing
> Requiere: Hitos 28–31 (backend facturación a clientes + facturas de proveedor)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/invoices`, `POST`, `PATCH /{id}/issue`, `PATCH /{id}/pay`, `GET /{id}/pdf`, `POST /{id}/line-items`; `GET /api/v1/supplier-invoices`, `POST`, `PATCH /{id}/pay`
- [ ] **[RED]** Tests `Billing.test.tsx` — lista de facturas de cliente renderiza con badge de estado; flujo DRAFT→ISSUED→PAID actualiza UI; botón PDF dispara descarga (`Content-Disposition: attachment`); lista de facturas de proveedor renderiza filtrable por categoría; 403 oculta acciones a DRIVER
- [ ] **[GREEN]** `packages/hooks/src/useBilling.ts` — lista paginada, create, addLineItem, issue, markPaid
- [ ] **[GREEN]** `packages/hooks/src/useSupplierInvoices.ts` — lista paginada, create, markPaid
- [ ] **[GREEN]** `apps/web/src/components/billing/` — `InvoiceTable`, `InvoiceStatusBadge`, `InvoiceFormModal`, `LineItemList`, `PdfDownloadButton`, `SupplierInvoiceTable`, `SupplierInvoiceFormModal`
- [ ] **[GREEN]** Página `Billing` — secciones separadas: facturación a clientes / gastos de proveedor

---

### Hito 34 — GPS: Contrato API
- [x] `Flyway V8` — tabla `gps_positions` con índices en `vehicle_id` y `recorded_at` *(ya aplicada)*
- [x] `GpsPosition` entity — lat, lng, heading, speed, `source` (MOCK/DEVICE) *(ya scaffoldeada)*
- [ ] `GpsPositionResponse` (record)
- [ ] `GpsController` — `GET /api/v1/gps/latest`

### Hito 35 — GPS: Lógica e implementación
- [ ] **[RED]** Tests `GpsRepositoryTest` (`@DataJpaTest` + Testcontainers) — findLatestByVehicleId devuelve la posición más reciente; vehículos INACTIVE no aparecen en findLatestForAllActiveVehicles
- [ ] **[RED]** Tests `GpsMockSchedulerTest` — scheduler genera exactamente una posición por vehículo ACTIVE con coordenadas dentro del rango esperado
- [ ] **[RED]** Tests `GpsControllerTest` (`@WebMvcTest`) — 200, 403 DRIVER sin acceso global; DRIVER solo ve su posición
- [ ] **[GREEN]** `GpsRepository` — `findLatestByVehicleId`, `findLatestForAllActiveVehicles` (proyección)
- [ ] **[GREEN]** `GpsMockScheduler` — `@Scheduled(fixedDelay = 30_000)`, genera posiciones con deriva aleatoria para vehículos ACTIVE
- [ ] **[GREEN]** `@PreAuthorize` — ADMIN/MANAGER/ADMINISTRATIVE ven todos; DRIVER solo su posición

### Hito 36 — Frontend: GPS Map
> Requiere: Hitos 34–35 (backend GPS)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/gps/latest`
- [ ] **[RED]** Tests `Map.test.tsx` — marcador renderizado por cada vehículo retornado por MSW; popover muestra licensePlate y speed; polling cada 10 s dispara segunda llamada
- [ ] **[GREEN]** `packages/hooks/src/useGps.ts` — polling cada 10 s, invalida caché automáticamente
- [ ] **[GREEN]** `apps/web/src/components/map/` — `FleetMap` (Leaflet + react-leaflet), `VehicleMarker`, `VehiclePopover`
- [ ] **[GREEN]** Página `Map` — mapa Leaflet con marcadores de vehículos activos

---

### Hito 37 — AuditLog viewer
- [x] `Flyway V8` — tabla `audit_logs` *(ya aplicada, misma migración que gps_positions)*
- [x] `AuditLog` entity, `AuditLogRepository` *(stub base ya creado — `JpaRepository` sin queries de filtro)*
- [ ] **[RED]** Tests `AuditControllerTest` (`@WebMvcTest`) — 200 con filtros entityType/action/rango de fechas; 403 ADMINISTRATIVE no tiene acceso
- [ ] **[GREEN]** `AuditLogRepository` — ampliar con `findAll` paginado y filtros (entityType, action, rango de fechas)
- [ ] **[GREEN]** `AuditLogResponse` (record) + `AuditLogController` — `GET /api/v1/audit`, solo ADMIN/MANAGER

### Hito 38 — Frontend: AuditLog
> Requiere: Hito 37 (backend audit viewer)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/audit` con filtros entityType, action, rango de fechas
- [ ] **[RED]** Tests `AuditLog.test.tsx` — tabla paginada renderiza; filtros por entityType y action reducen la lista; 403 si rol ADMINISTRATIVE o inferior
- [ ] **[GREEN]** `packages/hooks/src/useAuditLog.ts` — lista paginada con filtros
- [ ] **[GREEN]** `apps/web/src/components/audit/` — `AuditLogTable`, `AuditLogFilters`
- [ ] **[GREEN]** Página `AuditLog` — tabla paginada con filtros (solo ADMIN/MANAGER)

### Hito 39 — Frontend: Dashboard y rentabilidad
> Requiere: Hito 32 (backend profitability endpoint — incluye costes de mantenimiento y de proveedores)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/reports/profitability`
- [ ] **[RED]** Tests `Dashboard.test.tsx` — gráfico Recharts renderiza barras por vehículo; totales de ingresos/costes/margen son correctos; solo ADMIN/MANAGER ven la sección
- [ ] **[GREEN]** `packages/hooks/src/useProfitability.ts` — lista paginada de rentabilidad por vehículo
- [ ] **[GREEN]** `apps/web/src/components/` — `ProfitabilityChart` (Recharts), `ProfitabilitySummary`
- [ ] **[GREEN]** Página `Dashboard` — KPIs de flota + gráfico de rentabilidad

---

### Hito 40 — Tests de integración (`@SpringBootTest` + Testcontainers)
- [ ] `AuthFlowIT` — login correcto → JWT → endpoint protegido; 5 intentos fallidos → cuenta bloqueada → 401
- [ ] `JobLifecycleIT` — crear job → iniciar → completar → verificar `UsageLog` creado y `currentKm` actualizado
- [ ] `InvoiceFlowIT` — crear DRAFT → añadir línea → emitir → pagar → descargar PDF

### Hito 41 — Demo y hardening final
- [ ] `docker-compose.yml` — postgres:16 + backend + apps/web (nginx), health checks, `depends_on`
- [ ] `Flyway V11` — seed datos demo realistas (5 vehículos, 3 conductores, 10 trabajos completados, 3 facturas de cliente, facturas de proveedor de ejemplo)
- [ ] Revisar headers HTTP en `SecurityConfig`: `X-Content-Type-Options`, `X-Frame-Options`, `HSTS` (prod)
- [ ] Rate limiting en `/api/v1/auth/login` y `/api/v1/auth/refresh` (Bucket4j o filtro Spring Security) — control declarado en Security Model desde el inicio pero sin hito propio hasta esta revisión
- [ ] Structured JSON logging (`logstash-logback-encoder`, ya en `pom.xml`) con correlation ID en MDC en cada request
- [ ] Métrica Micrometer — contador de intentos de login fallidos, expuesto en `/actuator/metrics`
- [ ] Anclar `actions/checkout` / `actions/setup-java` en `ci.yml` y `security.yml` a SHA concreto (no tags mutables — supply chain)
- [ ] OWASP Dependency-Check — corregir cualquier CVE CVSS ≥ 7 pendiente
- [ ] `README.md` — diagrama de arquitectura, capturas de pantalla, credenciales demo, instrucciones Railway y local

---

## Decisiones de Implementación

| Decisión | Elección | Razón |
|----------|----------|-------|
| Herencia vehículos | Entidad única con campos nullable | Sin JOINs para consultas comunes; `vehicleCategory` como discriminador lógico |
| PDF facturas | OpenPDF | Open source, licencia LGPL |
| Eventos entre módulos | Spring `@TransactionalEventListener(AFTER_COMMIT)` | Desacoplamiento sin broker externo |
| Asignación conductor↔vehículo | Tabla con `endDate nullable` + unique index parcial | Historial completo; garantía BD de una sola asignación activa por conductor |
| GPS | Mock `@Scheduled` + polling frontend cada 10s | Sin dispositivos reales; demostrable visualmente |
| Matrícula nullable | Campo `licensePlate nullable` | Maquinaria pesada legalmente no requiere matrícula |
| Unidad de uso | Enum `KILOMETERS / HOURS` por vehículo | Transporte usa km; maquinaria usa horas de motor |
| Cliente | Entidad separada con FK en Job e Invoice | Permite historial de facturas por cliente y reutilización |
| Worker campos | phone, nationalId (DNI/NIE), licenseType (B/C/D…) | Requisitos legales y operativos confirmados |
| Orden de hitos: CI/CD | Movido al Hito 11 (antes era el penúltimo) | Un control de seguridad declarado en el plan pero inactivo durante ~30 PRs no es un control real |
| Orden de hitos: Frontend | Intercalado justo después de cada bloque de backend del que depende, en vez de en bloque al final | Evita llegar a la demo con backend completo pero UI inexistente si el tiempo se acorta |
| Facturas de proveedor | Hito propio (30–31), separado de Facturación a clientes | Ya estaba scaffoldeada (entidad + Flyway) en el commit inicial sin documentar; afecta el cálculo de rentabilidad (costes) |
| Spring Boot 3.3 → 3.5.16 | Subido en el Hito 11, al activar el gate de OWASP | 3.3.13 (último patch de la línea) seguía con CVEs CVSS hasta 9.8 en Spring Core/Security/Tomcat sin backport; 3.5.16 las resuelve. Validado con los 60 tests existentes antes y después del salto |

---

## Despliegue (coste cero)

**Recomendado:**
- Frontend → **Vercel** (gratis, deploy automático desde GitHub)
- Backend + BD → **Railway** (crédito $5/mes cubre el proyecto; sin cold start)

**Alternativa backup:**
- BD → Neon.tech (PostgreSQL gratuito 0.5GB)
- Backend → Render (gratis, pero cold start ~30s)

**Demo presencial:**
```bash
docker compose up        # levanta todo localmente
ngrok http 8080          # URL pública temporal
```

**Variables de entorno en producción (Railway secrets):**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://...
JWT_SECRET=<min 64 chars random>
SPRING_PROFILES_ACTIVE=prod
FRONTEND_URL=https://fleetmgm.vercel.app
```
