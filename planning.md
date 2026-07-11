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
│           ├── V11__add_maintenance_deleted_at.sql              ← aplicada, Hito 24
│           ├── V12__add_maintenance_category.sql                ← aplicada, adenda preventivo/correctivo
│           ├── V13__add_workshop_schedule_deleted_at.sql        ← aplicada, Hito 26
│           └── V14__seed_demo_data.sql                          ← pendiente, Hito 41 (única migración que falta)
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
- [x] **[RED]** Handlers MSW — `POST /api/v1/assignments`, `PATCH /{id}/end`, `GET /api/v1/workers/{id}/assignments`
- [x] **[RED]** Tests `Assignments.test.tsx` — modal asignación crea correctamente, finalizar asignación actualiza la lista, historial paginado renderiza, 403 oculta acciones a DRIVER
- [x] **[GREEN]** `packages/hooks/src/useAssignments.ts` — assign, endAssignment, historial paginado
- [x] **[GREEN]** `apps/web/src/components/assignment/` — `AssignmentModal`, `AssignmentHistory`
- [x] **[GREEN]** Panel de asignación integrado en página `Vehicles` (detalle de vehículo)
  > **Nota (revisión Hito 19, resuelta en PR #25):** se agregaron `GET /vehicles/{id}/assignment` (asignación activa de un vehículo) y `GET /assignments/active?driverIds=...` (batch, evita N+1) para que el conductor asignado se lea del backend en vez de depender de estado local de sesión. También se agregó fallback marca/modelo (`formatVehicleLabel`) para vehículos sin matrícula (maquinaria pesada).

---

### Hito 20 — Trabajos: Contrato API
- [x] `Flyway V5` — tablas `jobs` + `usage_logs` *(ya aplicada)*
- [x] `Job` entity — enum `JobStatus` (PENDING/IN_PROGRESS/COMPLETED/CANCELLED), `@SQLRestriction`; `UsageLog` entity *(ya scaffoldeadas)*
- [x] `CreateJobRequest` / `UpdateJobRequest` / `JobResponse` (records)
- [x] `JobMapper` (MapStruct)
- [x] `JobController` — CRUD + `PATCH /{id}/start`, `PATCH /{id}/complete`, `PATCH /{id}/cancel`
  > `JobService` es un stub (`UnsupportedOperationException("Pending Hito 21")` en cada método), mismo patrón que Hito 17→18 con `AssignmentService`. Sin tests todavía — lógica y tests llegan en Hito 21.

### Hito 21 — Trabajos: Lógica y eventos
- [x] **[RED]** Tests `JobServiceTest` — crear OK, transición PENDING→IN_PROGRESS, retroceder estado → excepción, completar publica `JobCompletedEvent`, cancelar desde COMPLETED → excepción
- [x] **[RED]** Tests `JobEventListenerTest` — `JobCompletedEvent` crea `UsageLog` y actualiza `currentKm`/`currentHours` en Vehicle
- [x] **[RED]** Tests `JobRepositoryTest` (`@DataJpaTest` + Testcontainers) — findByAssignedDriverIdAndStatusIn, JOIN FETCH no produce N+1
- [x] **[RED]** Tests `JobControllerTest` (`@WebMvcTest`) — 201, 400, 404, 409 (transición inválida); DRIVER solo ve y cambia estado de sus trabajos (cubierto en `JobServiceTest`, no en `@WebMvcTest` — mismo gap de `@PreAuthorize`/AOP heredado de Worker/Vehicle/Assignment)
- [x] **[GREEN]** `JobRepository` — `findAllJoinFetch` paginado, `findByAssignedDriverIdAndStatusIn`, ambos con `JOIN FETCH vehicle` + `LEFT JOIN FETCH assignedDriver/client`
- [x] **[GREEN]** `UsageLogRepository` — `JpaRepository` plano, sin métodos custom (sin test dedicado, mismo criterio que otros repos triviales del proyecto)
- [x] **[GREEN]** `JobService.create()` — crear PENDING
- [x] **[GREEN]** `JobService.start()` — PENDING → IN_PROGRESS, `actualStart = now()`, acepta `startUsageValue` opcional (body opcional en el PATCH, decisión tomada con el usuario: el conductor lee el odómetro al arrancar/terminar, no antes)
- [x] **[GREEN]** `JobService.complete()` — IN_PROGRESS → COMPLETED, `actualEnd = now()`, `endUsageValue` opcional, publica `JobCompletedEvent`
- [x] **[GREEN]** `JobService.cancel()` — PENDING/IN_PROGRESS → CANCELLED
- [x] **[GREEN]** `JobCompletedEvent` (record, `job.domain`) + `JobEventListener` (`vehicle.application` — vive ahí porque actualiza `Vehicle`/crea `UsageLog`, no `Job`; confirma desacoplamiento cross-feature vía evento) — `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional` propia: crea `UsageLog` (`source = JOB_COMPLETION`) y actualiza `currentKm`/`currentHours` en `Vehicle` según `usageMeasure`; no-op si `endUsageValue` es null
- [x] **[GREEN]** `@PreAuthorize` — DRIVER solo ve y cambia estado de sus trabajos (ownership check contra `assignedDriver`, mismo patrón `email → User → Worker` que `WorkerService`)
  > **Nota (revisión Hito 21):** primer evento de dominio del proyecto — no había precedente de `ApplicationEvent`/`@TransactionalEventListener` en el código, se diseñó siguiendo la sección "Inter-module Events" de `CLAUDE.md`. Nuevo código de error `JOB_INVALID_STATE_TRANSITION` (409, sin precedente previo de guarda de transición de estado en el proyecto) reutilizado en `start()`/`complete()`/`cancel()`; `VEHICLE_NOT_FOUND`/`WORKER_NOT_FOUND`/`CLIENT_NOT_FOUND` reutilizados tal cual. `PATCH /{id}/start` y `/complete` ganaron un body opcional (`StartJobRequest`/`CompleteJobRequest`) no contemplado en el contrato original del Hito 20. `JobRepositoryTest` (Testcontainers) no se pudo ejecutar en el entorno de desarrollo (sin Docker) — compila limpio, pendiente correr `./mvnw verify -Pfailsafe` localmente antes de mergear.

### Hito 22 — Frontend: Jobs
> Requiere: Hitos 20–21 (backend jobs)
- [x] **[RED]** Handlers MSW — `GET /api/v1/jobs`, `POST`, `PATCH /{id}/start`, `PATCH /{id}/complete`, `PATCH /{id}/cancel` (incluye los 409 `JOB_INVALID_STATE_TRANSITION`/`JOB_USAGE_VALUE_BELOW_CURRENT` para poder testear esos caminos de error)
- [x] **[RED]** Tests `Jobs.test.tsx` — lista renderiza; DRIVER solo ve sus trabajos activos; botones start/complete/cancel aparecen según rol y estado; transición de estado actualiza el badge
- [x] **[GREEN]** `packages/hooks/src/useJobs.ts` — lista paginada, create, update, start, complete, cancel con invalidación de caché
- [x] **[GREEN]** `apps/web/src/components/job/` — `JobTable`, `JobStatusBadge`, `JobFormModal`, `JobActionButtons`
- [x] **[GREEN]** Página `Jobs` — composición de componentes, ruta `/jobs` protegida (management + DRIVER)
  > **Nota (revisión Hito 22):** los botones de acción (Iniciar/Completar/Cancelar) no están condicionados por rol, solo por el estado del trabajo — cualquiera que pueda ver un trabajo en esta página ya tiene permiso para actuar sobre él (management sin restricción; DRIVER porque `list()` ya le filtra el backend a sus propios trabajos activos, blindado en el Hito 21). Solo "Nuevo trabajo" es management-only. Cancelar está disponible tanto en `PENDING` como en `IN_PROGRESS` (coincide con lo que ya permite `JobService.cancel()`). `JobResponse` no desnormaliza `vehicleMake`/`vehicleModel` (a diferencia de `AssignmentResponse`), así que un trabajo con vehículo sin matrícula muestra "—" en la columna Vehículo — pendiente si se decide extenderlo. Verificado además a mano en navegador real (Playwright, no incluido en el repo) contra el dev server con MSW activo, sin errores de consola.
  > **Nota (fix de consistencia posterior, tras Hito 27):** el gap anterior se cerró — `JobResponse`/`JobMapper`, `MaintenanceResponse`/`MaintenanceMapper` y `ScheduleResponse`/`ScheduleMapper` ahora desnormalizan `vehicleMake`/`vehicleModel` exactamente igual que `AssignmentResponse`/`AssignmentMapper` (mismo par de `@Mapping`, ningún cambio de query — el `vehicle` ya venía cargado vía `JOIN FETCH` en los tres repositorios). En frontend, `formatVehicleLabel()` (`apps/web/src/lib/vehicle-label.ts`) se generalizó de `Pick<Assignment, ...>` a un tipo estructural local para poder reutilizarse en `JobTable`, `MaintenanceTable` y `ScheduleTable` además de `AssignmentHistory`/`Workers`; los tres ahora muestran "`<make> <model>`" en vez de "—" cuando el vehículo no tiene matrícula (probado con el vehículo pesado sin matrícula ya presente en los seeds — `job-4`/`maintenance-3`/`schedule-3`). `VehicleTable.tsx` no se tocó (ya tiene columnas `make`/`model` propias). Backend: 188 tests (`./mvnw test`, sin Docker en este entorno — Testcontainers no verificado en este pase). Frontend: 43 tests (`vitest run`), `turbo test lint` verde en las 6 tareas, único warning preexistente de `oxlint` en `AssignmentModal.tsx` sin relación con este cambio.
  > **Nota (revisión 4R pre-commit):** el primer pase de este fix mockeaba `JobMapper`/`MaintenanceMapper`/`ScheduleMapper` en los tests de servicio y no aserciones nuevas en los de controller — un typo en el `@Mapping` de `vehicleMake`/`vehicleModel` habría compilado y pasado toda la suite sin que nada lo detectara. Cerrado añadiendo `jsonPath("$.vehicleMake")`/`jsonPath("$.vehicleModel")` a `getById_returns200_whenFound` en `JobControllerTest`, `MaintenanceControllerTest` y `WorkshopControllerTest`, mismo patrón que ya usaba `AssignmentControllerTest`. El resto de hallazgos de esa revisión (código muerto en `MaintenanceFormModal`, badge de estado duplicado en vez de extraído, gate `canManage` tautológico en `Workshop.tsx`, mutaciones sin manejo de `isError`, cobertura de caminos tristes) quedan documentados como deuda para una rama de fixing posterior — no bloquean este commit.

---

### Hito 23 — Mantenimiento: Contrato API
- [x] `Flyway V6` — tabla `maintenance_records` *(ya aplicada)*
- [x] `MaintenanceRecord` entity — enum `MaintenanceStatus` (SCHEDULED/IN_PROGRESS/COMPLETED) *(ya scaffoldeada)*
- [x] `CreateMaintenanceRequest` / `UpdateMaintenanceRequest` / `MaintenanceResponse` / `StartMaintenanceRequest` / `CompleteMaintenanceRequest` (records)
- [x] `MaintenanceMapper` (MapStruct)
- [x] `MaintenanceController` — CRUD (incluye `PUT`/`DELETE`) + `PATCH /{id}/start`, `PATCH /{id}/complete`
  > **Nota (revisión Hito 23):** `maintenance_records` se scaffoldeó sin columna `deleted_at` (el modelo de datos tampoco la lista como `soft-delete`), pero **se decidió con el usuario mantener soft-delete por consistencia** con todo el resto del proyecto (`@SQLRestriction("deleted_at IS NULL")`) y para conservar visibilidad/recuperabilidad de los borrados. El *quién* borró va por `AuditLog` (`shared/domain/AuditLog.java` ya tiene `performedByUserId`/`performedByEmail`; hoy solo lo usa `AuthService`), **no** por la columna `deleted_at` (que solo da el *cuándo*). La plomería del soft-delete (migración `V11__add_maintenance_deleted_at.sql`; campo `deletedAt` + `@SQLRestriction` en la entidad; `@Mapping(target = "deletedAt", ignore = true)` en `toEntity`/`updateEntity` del mapper) aterriza en Hito 24, no acá — este contrato es solo stub (`MaintenanceService` con `UnsupportedOperationException("Pending Hito 24")` en cada método, mismo patrón que Hito 17→18 y 20→21). Borrado permitido **solo en `SCHEDULED`** (ver Hito 24): no se construye ningún listener `onDeleteMaintenance` — con el vehículo entrando a `MAINTENANCE` en `start()` (no en `create()`), un `SCHEDULED` nunca tocó el estado del vehículo, así que no hay efecto que compensar. Borrar un `IN_PROGRESS` sería un *abort* (transición de estado, futuro `cancel()` + enum `CANCELLED` espejo de `Job`), no un `DELETE`.

### Hito 24 — Mantenimiento: Lógica y eventos
- [x] **[RED]** Tests `MaintenanceServiceTest` — iniciar (`start`) publica `VehicleEntersWorkshopEvent`, completar publica `MaintenanceCompletedEvent`, transiciones de estado inválidas → excepción, borrar un registro no-`SCHEDULED` (IN_PROGRESS/COMPLETED) → 409
- [x] **[RED]** Tests `MaintenanceEventListenerTest` — `VehicleEntersWorkshopEvent` cambia Vehicle a MAINTENANCE; `MaintenanceCompletedEvent` lo devuelve a ACTIVE
- [x] **[RED]** Tests `MaintenanceControllerTest` (`@WebMvcTest`) — 200/201/400/404/409 (el 403 por rol no se cubre acá — mismo gap AOP/`@PreAuthorize` heredado de Job/Vehicle: `@MockBean` en `@WebMvcTest` salta el proxy)
- [x] **[GREEN]** `MaintenanceRepository` — `findAllJoinFetch` (JOIN FETCH vehicle/technician/invoice, sin N+1) + `existsByVehicleIdAndStatus` (para el edge case del listener)
- [x] **[GREEN]** `VehicleEntersWorkshopEvent` + `MaintenanceCompletedEvent` (records, `workshop.domain`)
- [x] **[GREEN]** Migración `V11__add_maintenance_deleted_at.sql` (`ALTER TABLE maintenance_records ADD COLUMN deleted_at TIMESTAMPTZ`) + campo `deletedAt` y `@SQLRestriction("deleted_at IS NULL")` en la entidad + `@Mapping(target = "deletedAt", ignore = true)` en `toEntity`/`updateEntity` del mapper *(la seed de datos, originalmente V11, pasó a V12, luego a V13 y ahora a V14/Hito 41 — ver adenda de categoría de mantenimiento y Hito 26)*
- [x] **[GREEN]** `MaintenanceService.create()` — crear SCHEDULED (sin efecto sobre el vehículo todavía); `list()`/`getById()`/`update()` también cerrados (los stubs restantes del CRUD)
- [x] **[GREEN]** `MaintenanceService.start()` — SCHEDULED → IN_PROGRESS, publicar `VehicleEntersWorkshopEvent` (el vehículo entra a `MAINTENANCE` acá, no en `create()`)
- [x] **[GREEN]** `MaintenanceService.complete()` — IN_PROGRESS → COMPLETED, `workshopExitDate = now()`, publicar `MaintenanceCompletedEvent`
- [x] **[GREEN]** `MaintenanceService.delete()` — soft delete (`deletedAt = now()` + `save()`); rechaza con 409 `MAINTENANCE_DELETE_NOT_ALLOWED` si `status != SCHEDULED` (IN_PROGRESS toca el estado del vehículo, COMPLETED es historial de costes); escribe fila `AuditLog` (acción DELETE, `performedByUserId`/`performedByEmail`) para registrar el *quién*
- [x] **[GREEN]** Listener de eventos → `@TransactionalEventListener`: `VehicleEntersWorkshopEvent` → status `MAINTENANCE`; `MaintenanceCompletedEvent` → status `ACTIVE`
- [x] **[GREEN]** `@PreAuthorize` — WORKSHOP_STAFF puede crear/editar; ADMIN/MANAGER/ADMINISTRATIVE también
  > **Nota (revisión Hito 24):** (1) El listener se implementó como `@Component` dedicado **`MaintenanceEventListener`** en `vehicle.application` (no como métodos en `VehicleService` como decía el checklist literal) — misma convención que `JobEventListener`, respeta SRP y el desacoplamiento cross-feature vía evento. (2) Edge case resuelto: completar un mantenimiento solo devuelve el vehículo a `ACTIVE` si no le queda **otro `IN_PROGRESS`** (`existsByVehicleIdAndStatus`); un `SCHEDULED` no lo retiene porque el vehículo entra a taller en `start()`, no en `create()`. (3) `MaintenanceRepositoryTest` (`@DataJpaTest` + Testcontainers) escrito y **compila**, pero no se ejecutó localmente por falta de Docker — corre en CI / `mvn verify -Pfailsafe`. (4) Suite unitaria completa en verde: 145 tests (35 de mantenimiento: 15 service + 13 controller + 3 listener + 4 repo).

### Adenda — Categoría de mantenimiento (preventivo/correctivo) *(nuevo, sin hito fijo — post-Hito 24, previo al Hito 25)*
> Surgió al diseñar el contrato de agenda (Hito 25): `WorkshopSchedule.maintenanceRecordId` es nullable porque
> hay dos flujos legítimos de creación — (1) mantenimiento programado (cambio de aceite) que genera su entrada
> de agenda, y (2) avería/entrada no planificada que genera primero la agenda y de ahí nace el mantenimiento.
> Ambos son `MaintenanceRecord` válidos; la distinción es de **naturaleza del trabajo**, no de tipo de entidad.
> Se decidió un campo explícito y guardado (no inferido por orden de creación — evita comparar timestamps
> entre tablas, un criterio de filtrado frágil e implícito) para que sea filtrable server-side y alimente
> reportes de coste (Hito 32: preventivo vs. correctivo por vehículo, indicador real de gestión de flotas).
> Rama propia `maintenance-preventive-corrective-category`, ramificada desde `hito24-workshop-maintenance-logic`
> (no desde `main`) para que la migración nueva encadene como `V12` sin chocar con la `V11` del soft-delete.
- [x] **[RED]** Tests `MaintenanceServiceTest` — `create()` sin `category` en el request → default `PREVENTIVE`; `create()` con `category=CORRECTIVE` → se persiste tal cual; `update()` puede recategorizar
- [x] **[GREEN]** Migración `V12__add_maintenance_category.sql` — `ALTER TABLE maintenance_records ADD COLUMN category VARCHAR(10) NOT NULL DEFAULT 'PREVENTIVE'`
- [x] **[GREEN]** Enum `MaintenanceCategory` (`PREVENTIVE`/`CORRECTIVE`) en `workshop.domain`
- [x] **[GREEN]** `MaintenanceRecord` — campo `category` (`@Enumerated(EnumType.STRING)`, default `PREVENTIVE`)
- [x] **[GREEN]** `CreateMaintenanceRequest`/`UpdateMaintenanceRequest`/`MaintenanceResponse` — agregar `category` (opcional en create; si es null, el service aplica `PREVENTIVE`; obligatorio en update — PUT es reemplazo completo)
- [x] **[GREEN]** `MaintenanceMapper` — `toEntity` ignora `category` (el service resuelve el default, evita que un `null` sobreescriba el default de la entidad); `updateEntity`/`toResponse` mapean directo por nombre
- [x] **[GREEN]** `MaintenanceService.create()`/`update()` — cablear `category` (default `PREVENTIVE` si viene null en create)
  > **Nota (revisión adenda):** al escribir esta adenda, `V12` estaba reservado para la seed de datos del Hito 41 (documentado tras el Hito 24). Se corrigieron las 4 referencias desactualizadas (`planning.md` líneas ~232, 535, 543, 705, y la nota de migraciones en `CLAUDE.md`): la seed de datos pasa de V12 a **V13**. Secuencia final: V11 (soft-delete, Hito 24) → V12 (categoría preventivo/correctivo, esta adenda) → V13 (seed data, Hito 41). Suite completa verde con integración: 166 tests (`./mvnw test -Pfailsafe`, Docker).

### Hito 25 — Agenda del taller: Contrato API
- [x] `Flyway V6` — tabla `workshop_schedules` *(ya aplicada, misma migración que maintenance_records)*
- [x] `WorkshopSchedule` entity — prioridad, estado *(ya scaffoldeada)*
- [x] `CreateScheduleRequest` / `UpdateScheduleRequest` / `ScheduleResponse` (records) — `ScheduleResponse` denormaliza `maintenanceCategory` (`PREVENTIVE`/`CORRECTIVE`) del `MaintenanceRecord` enlazado, null si no hay link
- [x] `ScheduleMapper` (MapStruct) — `toEntity` ignora `priority` (mismo motivo que `category` en `MaintenanceMapper`: el default `MEDIUM` de la entidad no debe ser sobreescrito por un `null` del request; el service resuelve el default en Hito 26)
- [x] `WorkshopController` — CRUD (incluye `PUT`/`DELETE`) + `GET /api/v1/workshop/schedules?range=today|week|month` + `PATCH /{id}/start` + `PATCH /{id}/cancel`
  > **Nota (revisión Hito 25):** (1) **Infra nueva compartida:** `BadRequestException` (`shared/exception/`, simétrica a `NotFoundException`/`ConflictException`) + handler 400 en `GlobalExceptionHandler` — no existía ningún mecanismo para devolver 400 en un query param inválido fuera de `@Valid` en un body; sin esto, un `range` malformado caía al handler genérico → 500 (violaba la regla N de `CLAUDE.md`). (2) Enum `ScheduleRange` (`TODAY`/`WEEK`/`MONTH`) con `fromValue()` case-insensitive — el contrato pide minúsculas (`range=today`) pero `Enum.valueOf()` de Spring es case-sensitive por defecto. (3) `/start` y `/cancel` sin body — la entidad no tiene `actualStart`/`actualEnd` (a diferencia de `Job`/`MaintenanceRecord`), son simples cambios de estado. (4) **Decisión de diseño clave:** NO hay `/complete` manual — `WorkshopSchedule.status` pasa a `COMPLETED` solo vía evento cuando el `MaintenanceCompletedEvent` del mantenimiento enlazado se dispare (Hito 26), evitando el mismo antipatrón de "dos caminos al mismo estado" que se descartó para el listener de borrado en Hito 24. (5) `WorkshopScheduleService` es un stub (`UnsupportedOperationException("Pending Hito 26")` en los 7 métodos) — sin tests todavía, mismo patrón que Hito 23→24.

### Hito 26 — Agenda del taller: Lógica e implementación
- [x] **[RED]** Tests `WorkshopScheduleServiceTest` — crear (PENDING), listar hoy/semana/mes, `start()` PENDING→IN_PROGRESS, `cancel()` PENDING/IN_PROGRESS→CANCELLED, transición inválida → 409, borrar restringido a PENDING → 409 en otro estado, @PreAuthorize WORKSHOP_STAFF y superiores
- [x] **[RED]** Tests `ScheduleCompletionListenerTest` (o equivalente) — `MaintenanceCompletedEvent` con `maintenanceRecordId` enlazado a un schedule → `WorkshopSchedule.status` pasa a `COMPLETED`; sin schedule enlazado → no-op
- [x] **[RED]** Tests `WorkshopScheduleRepositoryTest` (`@DataJpaTest` + Testcontainers) — queries por rango de fecha devuelven solo registros del periodo correcto; excluye soft-deleted
- [x] **[RED]** Tests `WorkshopControllerTest` (`@WebMvcTest`) — 201, 400, 404, 409; parámetro `range` inválido → 400 (ya cubierto en Hito 25 vía `ScheduleRange.fromValue`, pero el `@WebMvcTest` completo llega acá)
- [x] **[GREEN]** Migración `V13__add_workshop_schedule_deleted_at.sql` (`ALTER TABLE workshop_schedules ADD COLUMN deleted_at TIMESTAMPTZ`) + `deletedAt` + `@SQLRestriction` en la entidad + ignore en el mapper *(la seed de datos, hasta ahora V13, pasa a **V14**/Hito 41 — misma corrección de numeración que ya se hizo dos veces; actualizado `planning.md` línea del árbol de arquitectura, Hito 41, y `CLAUDE.md`)*
- [x] **[GREEN]** `WorkshopScheduleRepository` — queries por rango de fecha: hoy, semana actual, mes actual
- [x] **[GREEN]** `WorkshopScheduleService.create()` — crear `PENDING`; default `priority = MEDIUM` si viene null (mismo patrón que `category` en `MaintenanceService`)
- [x] **[GREEN]** `WorkshopScheduleService.start()` — `PENDING` → `IN_PROGRESS`
- [x] **[GREEN]** `WorkshopScheduleService.cancel()` — `PENDING`/`IN_PROGRESS` → `CANCELLED`
- [x] **[GREEN]** `WorkshopScheduleService.delete()` — soft delete restringido a `PENDING` (409 en otro estado, mismo criterio que mantenimiento con `SCHEDULED`), audita vía `AuditLog`
- [x] **[GREEN]** Listener — `MaintenanceCompletedEvent` → si existe un `WorkshopSchedule` con ese `maintenanceRecordId`, pasa a `COMPLETED` (sin endpoint manual `/complete`, decisión del Hito 25)
- [x] **[GREEN]** `@PreAuthorize` — WORKSHOP_STAFF y superiores
  > **Nota (revisión Hito 26):** (1) **`getById()`/`update()` completados junto al resto:** aunque el checklist no los listaba explícitamente como ítem GREEN propio, seguían siendo stubs (`UnsupportedOperationException`) heredados del Hito 25 y el `WorkshopController` ya enrutaba `GET /{id}` y `PUT /{id}` hacia ellos; dejarlos a medio implementar habría dejado dos rutas rotas en un milestone que se declara "lógica e implementación" completa. Se implementaron con el mismo patrón que `MaintenanceService` (`NotFoundException` con código `SCHEDULE_NOT_FOUND`, reemplazo completo de relaciones en `update()`). (2) **Bug real descubierto por TDD:** `WorkshopController.list()` declaraba `@RequestParam String range` (obligatorio) — un `GET` sin el parámetro disparaba `MissingServletRequestParameterException`, que el `GlobalExceptionHandler` no maneja explícitamente y cae al catch-all → **500**, no 400. El contrato ya tenía `ScheduleRange.fromValue(null)` preparado para lanzar `BadRequestException("INVALID_RANGE", ...)`, pero nunca se ejecutaba porque Spring cortaba antes. Fix: `@RequestParam(required = false) String range`, dejando que `fromValue()` maneje el `null`. Sin el `WorkshopControllerTest` completo de este hito (`list_returns400_whenRangeMissing`), este 500 habría llegado a producción. (3) **Códigos de error nuevos:** `SCHEDULE_NOT_FOUND`, `SCHEDULE_INVALID_STATE_TRANSITION`, `SCHEDULE_DELETE_NOT_ALLOWED` — mismo criterio de nombres que `MAINTENANCE_*`/`JOB_*`. (4) **Ubicación del listener:** `ScheduleCompletionListener` vive en `workshop.application` (no en `vehicle.application` como `MaintenanceEventListener`) porque muta `WorkshopSchedule`, entidad de este mismo feature — regla "el listener vive en el paquete de la entidad que muta" establecida en Hitos 21/24. Usa `WorkshopScheduleRepository.findByMaintenanceRecordId()` (nuevo método derivado) para localizar el schedule enlazado; sin match, no hace nada (no-op), tal como pedía el checklist. (5) **Rango de fechas:** `listByRange()` calcula los límites en el `service` (no en el repositorio) con `java.time.temporal.TemporalAdjusters` — semana = lunes a domingo ISO (`previousOrSame`/`nextOrSame`), mes = primer al último día calendario (`withDayOfMonth(1)`/`withDayOfMonth(lengthOfMonth())`); el repositorio solo recibe `from`/`to` ya resueltos vía un único método `findAllByScheduledDateBetween` con `JOIN FETCH` (mismo patrón anti-N+1 que `MaintenanceRepository.findAllJoinFetch`). (6) **@PreAuthorize sin test de enforcement dedicado:** igual que en `MaintenanceServiceTest`/`JobServiceTest`, no existe una prueba `@WithMockUser` con contexto Spring real que verifique el rechazo por rol — `@InjectMocks` de Mockito no pasa por el proxy AOP de Spring Security, así que una prueba así sería un placebo sin `@SpringBootTest`. Se siguió la convención ya establecida en el proyecto: la anotación está presente y es revisada por code review, no por test unitario. (7) **Testcontainers/Docker:** disponible en este entorno (a diferencia de hitos previos) — `WorkshopScheduleRepositoryTest` corrió contra PostgreSQL 16 real vía `./mvnw test -Pfailsafe` sin problemas. (8) Suite completa verde: **212 tests** (`./mvnw test -Pfailsafe`, Docker) — 46 nuevos respecto a la base de 166 (22 `WorkshopScheduleServiceTest` + 2 `ScheduleCompletionListenerTest` + 17 `WorkshopControllerTest` + 5 `WorkshopScheduleRepositoryTest`).

### Hito 27 — Frontend: Workshop
> Requiere: Hitos 23–26 (backend maintenance + workshop schedules)
- [x] **[RED]** Handlers MSW — `GET /api/v1/workshop/schedules?range=today|week|month`, `POST`, `PATCH /{id}/cancel`; `GET /api/v1/maintenance`, `POST`, `PATCH /{id}/start`, `PATCH /{id}/complete`
- [x] **[RED]** Tests `Workshop.test.tsx` — selector de rango filtra la lista correctamente; WORKSHOP_STAFF ve y crea órdenes; cambio de estado actualiza badge
- [x] **[GREEN]** `packages/hooks/src/useWorkshop.ts` — lista por rango, create, cancel
- [x] **[GREEN]** `packages/hooks/src/useMaintenance.ts` — lista, create, start, complete
- [x] **[GREEN]** `apps/web/src/components/workshop/` — `ScheduleTable`, `ScheduleRangeSelector`, `MaintenanceTable`, `MaintenanceFormModal`
- [x] **[GREEN]** Página `Workshop` — vista unificada de agenda y mantenimientos
  > **Nota (revisión Hito 27):** (1) **`useWorkshop.ts` deliberadamente sin `start`:** aunque `WorkshopController` expone `PATCH /{id}/start`, el checklist de este hito solo pedía `list/create/cancel` y así se implementó — el frontend no ofrece un botón "Iniciar" para la agenda. La transición real de estado la conduce `MaintenanceTable` (`start`/`complete` sobre la orden de mantenimiento, que es donde ocurre el trabajo físico); el `WorkshopSchedule` pasa a `IN_PROGRESS` únicamente si alguien invoca ese endpoint directamente (fuera de esta UI) y a `COMPLETED` solo vía el listener del backend (`MaintenanceCompletedEvent` → `ScheduleCompletionListener`, Hito 26) — coherente con la decisión de no exponer un `/complete` manual para la agenda. (2) **Sin botón "Iniciar" ni "Completar" propios para `ScheduleTable`:** solo "Cancelar" (`PENDING`/`IN_PROGRESS` → `CANCELLED`), simétrico con el punto anterior. (3) ~~Sin componentes de badge/acciones separados~~ — **corregido en `fixing-hito27-review-findings`:** ver nota de fixing más abajo, ahora sí existen `ScheduleStatusBadge`/`MaintenanceStatusBadge`. (4) **`useCompleteMaintenance` invalida ambas claves de caché** (`['maintenance']` y `['workshop']`), a diferencia de `useCompleteJob` en Hito 22 (que solo invalida `['jobs']` pese a que `JobCompletedEvent` también actualiza `Vehicle`) — decisión consciente: `Workshop` es una vista unificada de agenda + órdenes en la misma página, así que sin la invalidación cruzada la agenda quedaría visualmente desactualizada tras completar la orden enlazada. (5) ~~`MaintenanceFormModal` solo se usa para crear en esta página, pero soporta `record` opcional para reutilizarse como editor~~ — **corregido en `fixing-hito27-review-findings`:** la rama de edición nunca llegó a invocarse desde ningún lado (`MaintenanceTable` no tiene `onEdit`); se eliminó, el modal es create-only. (6) **Mocks MSW — filtrado por rango:** en vez de reproducir la aritmética real de fechas del backend (semana ISO lunes-domingo, mes calendario — ya cubierta por `WorkshopScheduleRepositoryTest`), el mock etiqueta cada `WorkshopSchedule` seed con `rangeTags: ('today'|'week'|'month')[]` explícitos y filtra por pertenencia; evita que el test dependa de en qué día del mes/semana se ejecute la suite. (7) **`range` es obligatorio en el hook** (`useWorkshopSchedules(range, page?, size?)`, sin default) — refleja que `ScheduleRange.fromValue(null)` lanza `BadRequestException` en el backend (Hito 25/26): no existe un "rango por defecto" server-side, así que el frontend siempre debe enviar uno explícito (la página inicializa `range` en `'today'`). (8) Verificado: `apps/web` 40/40 tests (`vitest run`), suite completa del monorepo 43/43 (`turbo test`), `tsc -b` limpio, `oxlint` sin errores nuevos (1 warning preexistente en `AssignmentModal.tsx`, no tocado en este hito).
  > **Nota (rama `fixing-hito27-review-findings`, limpieza post-revisión 4R):** además de implementar el `cancel()` de mantenimiento (adenda siguiente), esta rama corrigió hallazgos de una revisión de código de 4 lentes (risk/resilience/readability/reliability) sobre el diff ya mergeado de este Hito 27: (1) eliminado el código muerto de `MaintenanceFormModal` (ver punto 5 arriba). (2) Extraídos `ScheduleStatusBadge`/`MaintenanceStatusBadge` a sus propios archivos en `apps/web/src/components/workshop/`, mismo patrón que `JobStatusBadge`, reemplazando los mapas de etiqueta/clase que vivían inline duplicados en `ScheduleTable`/`MaintenanceTable`. (3) Eliminado el gate `canManage`/`WORKSHOP_MANAGE_ROLES` de `Workshop.tsx` — era tautológico (la ruta `/workshop` en `App.tsx` ya restringe a ese mismo conjunto de roles; a diferencia de `Jobs`, aquí no existe ningún rol "solo visor"). (4) Añadido manejo visible de errores (`role="alert"`, mensajes "No se pudo completar la acción."/"No se pudieron cargar los datos.") en `MaintenanceTable`, `ScheduleTable`, `MaintenanceFormModal`, `JobActionButtons`, `JobFormModal`, y estados de error a nivel de lista (distintos de "cargando"/"vacío") en `Workshop.tsx`/`Jobs.tsx` — antes un 409/404 o un fallo de red fallaban en silencio. (5) Añadido `apps/web/src/lib/vehicle-label.test.ts` (las 3 ramas de `formatVehicleLabel`) y tests de camino triste (409 por doble clic) en `Jobs.test.tsx`/`Workshop.test.tsx`. Verificado: backend 208 tests, frontend 52 tests (tras sumar también los del `cancel()` de mantenimiento — ver adenda), `turbo build lint test` 7/7. Quedan como deuda documentada (no bloqueante, revisados y aceptados conscientemente): hooks de mutación compartidos por tabla en vez de por fila en `MaintenanceTable`/`ScheduleTable` (un error en una fila puede desaparecer visualmente si otra fila tiene éxito después — a diferencia de `JobActionButtons`, que sí es por fila), sin `AuditLog` en ningún `cancel()`/`start()`/`complete()` de `workshop` (gap preexistente, no introducido aquí), el test de doble-clic depende de una carrera de timing real en vez de un mock determinista, y el mensaje de error repetido inline en 6 archivos en vez de extraído a un componente compartido.

### Adenda — Cancelación de mantenimiento y su relación con la agenda *(implementado en `fixing-hito27-review-findings`)*
> Surgió al probar el flujo real en el frontend del Hito 27: cancelar un `WorkshopSchedule` no tenía ningún
> efecto sobre su `MaintenanceRecord` enlazado. Investigado y confirmado que **no era un artefacto del mock
> MSW** — era el comportamiento real del backend: `WorkshopScheduleService.cancel()` solo mutaba el propio
> `WorkshopSchedule` (no publicaba evento), y `MaintenanceRecord` directamente **no tenía forma de cancelarse**
> (`MaintenanceStatus` solo tenía `SCHEDULED`/`IN_PROGRESS`/`COMPLETED` — sin `CANCELLED`, sin endpoint
> `/cancel`). Ya estaba parcialmente anotado en la revisión del Hito 23 ("borrar un `IN_PROGRESS` sería un
> abort... futuro `cancel()` + enum `CANCELLED` espejo de `Job`, no un `DELETE`") pero nunca se había implementado.
> Diseño acordado con el usuario, implementado en la rama `fixing-hito27-review-findings` (backend):
> 1. **`MaintenanceService.cancel(UUID id)`** — nuevo estado `CANCELLED` en `MaintenanceStatus`, alcanzable
>    desde `SCHEDULED`/`IN_PROGRESS` (no desde `COMPLETED`/`CANCELLED`, estados terminales — lanza
>    `ConflictException("MAINTENANCE_INVALID_STATE_TRANSITION", ...)`) — mismo patrón que `Job`/`WorkshopSchedule`.
>    Nuevo endpoint `PATCH /api/v1/maintenance/{id}/cancel`, sin body, mismo `@PreAuthorize` que `start`/`complete`.
> 2. **Efecto sobre el vehículo** — `MaintenanceEventListener` gana `onMaintenanceCancelled`, con el mismo
>    guard `existsByVehicleIdAndStatus(vehicleId, IN_PROGRESS)` que `onMaintenanceCompleted`: si otro
>    mantenimiento sigue `IN_PROGRESS`, no toca el vehículo; si no, lo pone `ACTIVE`. Cancelar desde
>    `SCHEDULED` es un no-op real sobre el vehículo porque este nunca entró a `MAINTENANCE` por su causa.
> 3. **Cascada bidireccional con `WorkshopSchedule`** — implementada con dos eventos nuevos:
>    `MaintenanceCancelledEvent` (`workshop.domain`, publicado siempre por `MaintenanceService.cancel()`,
>    independientemente del estado previo — necesario para que la cascada hacia `WorkshopSchedule` funcione
>    también al cancelar un mantenimiento `SCHEDULED`) y `ScheduleCancelledEvent` (`workshop.domain`, publicado
>    siempre por `WorkshopScheduleService.cancel()`, con `maintenanceRecordId` nulleable). Un nuevo
>    `ScheduleCancellationListener` (`workshop.application`, sibling de `ScheduleCompletionListener`) consume
>    ambos eventos: `onScheduleCancelled` cancela el `MaintenanceRecord` enlazado si existe y no es terminal;
>    `onMaintenanceCancelled` cancela el `WorkshopSchedule` enlazado (vía `findByMaintenanceRecordId`, ya
>    existente) si existe y no es terminal. El ciclo A-cancela-B → evento → B-intenta-cancelar-A se rompe
>    porque cada listener comprueba el estado *actual* de la entidad relacionada **antes** de llamar a su
>    `cancel()` y hace no-op si ya es terminal (opción preferida sobre capturar `ConflictException`) — para
>    cuando el evento recíproco llega, la entidad ya está en estado terminal y el listener no hace nada.
>    Un `WorkshopSchedule` sin mantenimiento enlazado (`maintenanceRecordId == null`) resuelve el evento con
>    valor `null` y el listener no-opea.
> 4. **Frontend** — fuera del alcance de este trabajo (agente en paralelo); el contrato quedó fijado arriba
>    para que `useMaintenance.ts` añada una mutación `cancel` y `MaintenanceTable` un botón "Cancelar".
>
> **Desviación de diseño respecto al texto original:** el punto 2 original decía "cancelar desde `SCHEDULED`
> no toca el vehículo", lo que podía leerse como "el evento no debe publicarse" en ese caso. Se implementó
> en cambio publicando `MaintenanceCancelledEvent` siempre (symmetric con `ScheduleCancelledEvent`, y
> necesario para que la cascada del punto 3 funcione también al cancelar un `SCHEDULED`), y delegando la
> garantía de "no tocar el vehículo" al guard ya existente en el listener (`existsByVehicleIdAndStatus`):
> como un mantenimiento `SCHEDULED` nunca puso el vehículo en `MAINTENANCE`, el guard reactivador es un
> no-op semántico en ese caso (mismo comportamiento observable, un único punto de publicación del evento).
>
> **Tests (TDD, rama `fixing-hito27-review-findings`):** 20 tests nuevos — `MaintenanceServiceTest` (+4:
> cancel desde `SCHEDULED`/`IN_PROGRESS`, 409 desde `COMPLETED`, 404 si no existe, evento publicado con
> `maintenanceId`/`vehicleId` correctos), `MaintenanceControllerTest` (+3: 200/404/409 en
> `PATCH /{id}/cancel`), `WorkshopScheduleServiceTest` (+2 modificados para verificar `ScheduleCancelledEvent`
> +2 nuevos para el caso `maintenanceRecordId` nulo/no nulo), `MaintenanceEventListenerTest` (+2:
> `onMaintenanceCancelled` reactiva el vehículo / no-op si otro mantenimiento sigue `IN_PROGRESS`),
> `ScheduleCancellationListenerTest` (+9, archivo nuevo: cascada en ambas direcciones, no-op en terminal,
> no-op en `maintenanceRecordId == null`, no-op si no se encuentra la entidad enlazada). Suite completa del
> backend: 188 → 208 tests, 0 fallos (`./mvnw test`; `-Pfailsafe`/Testcontainers no ejecutado, sin Docker
> disponible en el entorno — igual que en hitos anteriores).
>
> **2ª revisión 4R y bugs reales encontrados en esta misma rama:** la implementación de arriba, al pasar por
> una segunda ronda de revisión de 4 lentes antes de comitear, resultó tener 2 bugs de comportamiento real
> (no solo hallazgos de estilo):
> 1. **BLOCKER — reactivación indebida del vehículo:** `onMaintenanceCompleted`/`onMaintenanceCancelled` en
>    `MaintenanceEventListener` solo comprobaban que ningún *otro* mantenimiento siguiera `IN_PROGRESS` antes
>    de poner el vehículo en `ACTIVE` — nunca comprobaban el estado *actual* del vehículo. Cancelar un
>    mantenimiento `SCHEDULED` sobre un vehículo que hubiera pasado a `INACTIVE`/`DECOMMISSIONED` por una vía
>    no relacionada lo reactivaba a `ACTIVE` silenciosamente. Fix: ambos métodos ahora comparten un helper
>    `reactivateVehicleIfNoOtherActiveMaintenance()` que además exige `vehicle.getStatus() == MAINTENANCE`
>    antes de tocarlo — si el vehículo no estaba en `MAINTENANCE`, no hay nada que deshacer. Confirmado con
>    TDD: 2 tests nuevos fallaban contra el código viejo (vehículo `DECOMMISSIONED`/`INACTIVE` que debía
>    permanecer así) y pasan con el fix.
> 2. **CRITICAL — fallos de cascada silenciosos:** los listeners `AFTER_COMMIT` (`ScheduleCancellationListener`,
>    `MaintenanceEventListener`) no tenían manejo de errores — al haber ya hecho commit la transacción
>    original, una excepción ahí no se puede revertir y Spring la registra solo a su propio nivel interno,
>    sin visibilidad de aplicación, mientras el cliente ya recibió `200 OK`. Fix: cada método de listener
>    envuelve su lógica en try/catch y registra a nivel ERROR (SLF4J) con los IDs relevantes (vehicleId/
>    maintenanceId/scheduleId) sin relanzar — nunca queda en silencio, aunque tampoco hay nada que
>    reintentar automáticamente (deuda: no hay mecanismo de reintento/alerta, solo logging).
> 3. **BLOCKER (frontend) — mock de cascada asimétrico:** el handler MSW de cancelar *schedule* no cascadeaba
>    a *maintenance* (al revés que el de cancelar *maintenance*, que sí lo hacía) — corregido para espejar
>    el comportamiento real del backend. `useCancelWorkshopSchedule` tampoco invalidaba la caché de
>    `['maintenance']` — corregido, mismo patrón que `useCancelMaintenance`/`useCompleteMaintenance`.
> 4. **CRITICAL (cobertura) — la cascada bidireccional no tenía ningún test que la verificara en ninguna
>    dirección**, pese a ser la funcionalidad central de esta adenda; corregido extendiendo los tests de
>    cancelar en `Workshop.test.tsx` para verificar que la entidad enlazada también cambia de estado, en
>    ambas direcciones. También se añadió cobertura para los nuevos estados de error de carga en
>    `Jobs.tsx`/`Workshop.tsx` (antes sin ningún test).
> Suite final tras ambos rounds de fix: backend 208 → 215 tests (0 fallos), frontend 50 → 52 tests (0
> fallos), `turbo build lint test` 7/7. Quedan como deuda documentada (no bloqueante, aceptada
> conscientemente sin arreglar en esta rama): hooks de mutación por tabla en vez de por fila en
> `MaintenanceTable`/`ScheduleTable`, sin `AuditLog` en `cancel()` (gap preexistente en `start()`/`complete()`
> también), test de doble-clic dependiente de timing real, sin test de integración `@SpringBootTest` del
> cascade de dos saltos end-to-end (solo unit tests mockeados), y el mensaje de error repetido inline en
> vez de extraído a un componente compartido.

### Adenda — Agenda de taller: creación manual + auto-creación al programar mantenimiento *(implementado en `fixing-hito27-review-findings`)*
> Surgió al validar con el usuario si `npm run dev` (mock MSW) ya reflejaba el ciclo de vida completo de
> taller. Confirmado que **no** — ni el mock ni el backend real creaban una entrada de agenda al crear/
> iniciar/completar una orden de mantenimiento; `WorkshopSchedule` y `MaintenanceRecord` eran completamente
> independientes salvo por la cascada de completar/cancelar ya implementada (adenda anterior). Dos huecos
> reales, resueltos en dos pasos:
> 1. **Creación manual de agenda** — no existía ningún control en la UI para `POST /api/v1/workshop/schedules`
>    (el endpoint y el hook `useCreateWorkshopSchedule` ya existían desde Hito 25/26/27, pero sin cablear).
>    Nuevo componente `ScheduleFormModal` (mismo patrón que `MaintenanceFormModal`: vehículo, técnico
>    opcional, tipo, fecha, prioridad, notas — sin campo `maintenanceRecordId`, fuera de alcance de este
>    paso) y botón "Nueva entrada" junto al selector de rango en `Workshop.tsx`. Sin cambios de backend.
> 2. **Auto-creación al programar mantenimiento** — confirma el flujo (1) ya documentado en la adenda de
>    categoría preventivo/correctivo ("mantenimiento programado que genera su entrada de agenda"), nunca
>    implementado hasta ahora. `CreateMaintenanceRequest` gana `scheduledDate` (nullable a nivel de DTO —
>    Bean Validation no expresa "requerido condicional" limpio; el service valida y lanza
>    `BadRequestException("MAINTENANCE_SCHEDULED_DATE_REQUIRED", ...)` si falta y el flag de abajo está
>    activo). Nuevo toggle de **configuración de despliegue** (decisión explícita del usuario — no expuesto
>    al frontend, no es una preferencia por request/rol): `workshop.auto-create-schedule-on-maintenance-create`
>    en `application.yml`, resuelto vía `${WORKSHOP_AUTO_CREATE_SCHEDULE:true}`, inyectado en
>    `MaintenanceService` por constructor (`@Value`, mismo patrón que `jwt.secret` en `JwtService`).
>
> **Primera implementación (llamada directa) y su corrección vía code-review:** la primera versión de
> `MaintenanceService.create()` inyectaba `WorkshopScheduleService` y lo llamaba sincrónicamente en la misma
> transacción. Una revisión de código (8 ángulos + verificación 1-voto) encontró que esto violaba la regla
> explícita de `CLAUDE.md` ("Use Spring's event system... to add side-effects to a completed job or
> maintenance record without touching JobService or MaintenanceService") e era inconsistente con el propio
> `cancel()` del mismo archivo, que ya resuelve la relación simétrica vía evento. Corregido a:
> `MaintenanceService.create()` publica `MaintenanceScheduledEvent` (nuevo record en `workshop.domain`);
> nuevo `ScheduleCreationListener` (`workshop.application`, sibling de `ScheduleCompletionListener`/
> `ScheduleCancellationListener` — mutan `WorkshopSchedule`, regla de Hito 21/24) lo consume vía
> `@TransactionalEventListener(phase = AFTER_COMMIT)` y llama a `WorkshopScheduleService.create()`, con el
> mismo try/catch + `log.error(...)` sin relanzar que ya usa `ScheduleCancellationListener`. Este cambio
> también eliminó, como efecto colateral, una segunda evaluación redundante de `@PreAuthorize` y guardas
> `NotFoundException` muertas que la llamada directa producía (el vehículo/técnico ya estaban resueltos por
> el caller un momento antes).
>
> **Limitación aceptada, no mitigada por ahora:** al ser `AFTER_COMMIT`, la creación de la orden de
> mantenimiento ya hizo commit (y el HTTP ya devolvió `201`) antes de que `ScheduleCreationListener` intente
> crear la agenda. Si esa segunda transacción falla, no hay rollback posible ni reintento — solo queda un
> `log.error` en el servidor, y la orden de mantenimiento queda persistida **sin** su entrada de agenda
> vinculada, sin que el cliente se entere. Mismo perfil de riesgo que ya acepta el proyecto para
> `ScheduleCompletionListener`/`ScheduleCancellationListener` (adenda anterior, punto "fallos de cascada
> silenciosos"). Decisión explícita del usuario: aceptar por ahora: **si se aborda, será al final** (ej. un
> job de reconciliación que detecte `MaintenanceRecord`s sin `WorkshopSchedule` vinculado cuando el flag está
> activo) — no bloqueante para seguir avanzando.
>
> **Frontend/mock:** `MaintenanceFormModal` gana el campo "Fecha" (prellenado con la fecha **local** del
> usuario — no UTC, ver bug corregido abajo — editable a futuro). `apps/web/src/mocks/handlers.ts` espeja el
> efecto secundario del backend en `POST /api/v1/maintenance` (el mock no modela el toggle: siempre se
> comporta como si estuviera activo). `useCreateMaintenance` pasa a invalidar también la cache de `workshop`,
> mismo patrón que `useCompleteMaintenance`/`useCancelMaintenance`.
>
> **Code-review (8 ángulos + verificación) antes de comitear — 9 hallazgos, 2 descartados:** descartados por
> ser diseño ya documentado/decidido (coexistencia intencional de los dos flujos de creación, `planning.md`
> línea ~558; y el hardcoding de `rangeTags` en el mock, ya preexistente desde Hito 27 con comentario propio).
> De los 9 confirmados, arreglados todos: (1) **bug real de zona horaria** — el default de fecha usaba
> `toISOString()` (UTC) en vez de la fecha local, fecha equivocada para usuarios en UTC-3 después de ~21:00;
> (2) llamada directa en vez de evento (ver arriba); (3) `MaintenanceControllerTest.create_returns201_...
> _whenValid` quedó con un payload que ya no era válido bajo la nueva regla de negocio (`@MockBean` lo
> ocultaba); (4) lookups/guards/`resolveTechnician` duplicados, resuelto como efecto colateral de (2); (5)
> construcción de `WorkshopScheduleMock` duplicada entre dos handlers del mock, extraída a
> `buildWorkshopScheduleMock`; (6) `MaintenanceServiceTest` no podía usar `@InjectMocks` por el `boolean`
> primitivo del constructor (Mockito 5 lanza error en vez de defaultear) — reducido a una sola construcción
> manual + `ReflectionTestUtils` para el test con el flag apagado; (7) comentario de test que mencionaba
> `ReflectionTestUtils` sin usarlo, ahora preciso; (8) `useCreateMaintenance`/`useCompleteMaintenance`/
> `useCancelMaintenance`/`useCancelWorkshopSchedule` duplicaban el mismo `Promise.all` de doble invalidación
> — extraído a `invalidateQueryKeys` (`packages/hooks/src/invalidateQueryKeys.ts`); (9) `ScheduleFormModal`
> duplicaba `selectClassName`/`toNullableString`/`PRIORITY_LABEL` de sus archivos hermanos — extraídos a
> `apps/web/src/components/workshop/form-shared.ts`.
>
> **Tests:** backend 215 → 220 (`./mvnw test`, 0 fallos — nuevo `ScheduleCreationListenerTest`, tests de
> `MaintenanceServiceTest`/`MaintenanceControllerTest` actualizados/renombrados); frontend 52 → 54 (`vitest
> run`, 0 fallos — `Workshop.test.tsx` cubre el botón "Nueva entrada" y la creación de agenda vinculada al
> crear una orden). `oxlint`/`tsc -b` limpios en ambos paquetes tocados.

### Adenda — Edición de órdenes de mantenimiento y entradas de agenda + "Iniciar" en la agenda *(implementado en `fixing-hito27-review-findings`)*
> Mismo patrón raíz que las dos adendas anteriores: el backend ya exponía los tres endpoints desde
> Hito 25/26/27 (`PUT /api/v1/maintenance/{id}` → `MaintenanceController.update()` →
> `MaintenanceService.update()`; `PUT /api/v1/workshop/schedules/{id}` → `WorkshopController.update()` →
> `WorkshopScheduleService.update()`; `PATCH /api/v1/workshop/schedules/{id}/start` →
> `WorkshopController.start()` → `WorkshopScheduleService.start()`, guardado con 409
> `SCHEDULE_INVALID_STATE_TRANSITION` si el estado no es `PENDING`), pero el frontend nunca los cableó —
> ni un botón "Editar", ni un botón "Iniciar" para la agenda, ni los hooks correspondientes. Se decidió con
> el usuario cerrar este hueco en esta misma rama por ser trabajo de cableado puro (sin endpoints ni schema
> nuevos), dejando fuera de alcance (deferido a un hito futuro) los campos de hora de inicio/fin de la
> entrada de agenda — eso sí requiere diseño de contrato nuevo.
>
> **Implementado:**
> 1. `packages/api/src/types.ts` — nuevo tipo `UpdateScheduleRequest`, espejo del `UpdateScheduleRequest.java`
>    del backend. `UpdateMaintenanceRequest` no cambió — no tiene `scheduledDate` porque la fecha programada
>    solo es asignable al crear.
> 2. `packages/hooks/src/useMaintenance.ts` — reexportado `useUpdateMaintenance` desde el `useUpdate()` que
>    `createCrudHooks` ya generaba (nunca se había reexportado, mismo patrón que `useUpdateVehicle`).
> 3. `packages/hooks/src/useWorkshop.ts` — nuevos `useUpdateWorkshopSchedule`/`useStartWorkshopSchedule`
>    (archivo custom, no usa `createCrudHooks`). A diferencia de `useCancelWorkshopSchedule`/
>    `useCompleteMaintenance`, ninguno de los dos invalida la clave `['maintenance']` — ni un `update()` ni
>    un `start()` publican evento hacia `MaintenanceRecord` en el backend, así que no hay cascada que
>    reflejar; invalidan solo `['workshop']`.
> 4. `MaintenanceFormModal`/`ScheduleFormModal` ganan un prop opcional (`record`/`schedule`) y modo edición,
>    mismo patrón que `VehicleFormModal` (`isEditing`, prellenado en el `useEffect` de reset, título y label
>    del botón condicionales). En `MaintenanceFormModal`, el campo "Fecha" solo se renderiza en modo creación
>    (`UpdateMaintenanceRequest` no tiene ese campo); en `ScheduleFormModal` todos los campos de
>    `CreateScheduleRequest` tienen equivalente en `UpdateScheduleRequest`, sin ocultamiento condicional.
> 5. `MaintenanceTable`/`ScheduleTable` ganan un botón "Editar" (ícono `Pencil`, mismo patrón que
>    `VehicleTable`) sin restricción por estado — el backend no la tiene, así que el frontend no inventa una.
>    `ScheduleTable` gana además un botón "Iniciar" (solo visible en `PENDING`, mismo patrón condicional que
>    el "Iniciar" ya existente en `MaintenanceTable`), con su propio manejo de error `role="alert"`.
> 6. `Workshop.tsx` — nuevo estado `editingMaintenance`/`editingSchedule`, limpiado tanto al cerrar el modal
>    como al abrir el formulario de creación (para que "Nueva orden"/"Nueva entrada" no reabran el último
>    registro editado).
> 7. `apps/web/src/mocks/handlers.ts` — tres handlers nuevos (`PUT /maintenance/:id`, `PUT
>    /workshop/schedules/:id`, `PATCH /workshop/schedules/:id/start`), mismo estilo que sus hermanos
>    existentes (404 con el mismo shape de error, 409 `SCHEDULE_INVALID_STATE_TRANSITION` en `start` si el
>    estado no es `PENDING`).
>
> **Deuda aceptada, no corregida aquí (decisión explícita, fuera de alcance):** `MaintenanceService.update()`
> y `WorkshopScheduleService.update()` no tienen guarda de estado en el backend — permiten editar un registro
> en cualquier estado, incluidos `COMPLETED`/`CANCELLED`. El frontend ahora expone un botón "Editar" siempre
> visible que hereda ese comportamiento tal cual: es posible editar una orden de mantenimiento o una entrada
> de agenda ya completada o cancelada desde la UI. Se documenta como gap conocido, no bloqueante — corregirlo
> requeriría una decisión de producto (¿qué campos son editables post-cierre, si alguno?) que no corresponde
> tomar unilateralmente en un hito de cableado de frontend.
>
> **Tests:** frontend 54 → 58 (`vitest run` en `apps/web`, 0 fallos — 4 tests nuevos en `Workshop.test.tsx`:
> edición de una orden de mantenimiento con prellenado y ocultamiento del campo Fecha, edición de una entrada
> de agenda con prellenado completo, "Iniciar" transiciona el badge de una entrada `PENDING` a `En curso`, y
> el camino triste de un doble clic en "Iniciar" que dispara el 409). `packages/hooks` sigue sin archivos de
> test (`createCrudHooks`/las mutaciones nuevas se ejercitan indirectamente vía los tests de componente de
> `apps/web`, mismo criterio ya aplicado a `useUpdateVehicle`). `tsc -b`/`tsc --noEmit` limpios en
> `apps/web`, `packages/api` y `packages/hooks`; `oxlint` sin errores nuevos (mismo warning preexistente en
> `AssignmentModal.tsx`, no tocado en esta rama). Backend no tocado — su suite no se ejecutó en este pase.

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
- [ ] `Flyway V14` — seed datos demo realistas (5 vehículos, 3 conductores, 10 trabajos completados, 3 facturas de cliente, facturas de proveedor de ejemplo)
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
