# FleetMgm — Plan Arquitectónico Completo

## Context

Proyecto de fin de máster: aplicación de gestión de flotas con backend Spring Boot + frontend React.
Repositorio greenfield. Plazo: ~6 semanas (hasta mediados de julio 2026).
El objetivo es una app funcional y bien estructurada arquitectónica y securamente para presentación académica.

---

## Stack Tecnológico

### Stack elegido

| Capa | Tecnología | Justificación |
|------|------------|---------------|
| Backend | Java 21 + Spring Boot 3.3 | Ecosistema maduro; Spring Security, Data JPA, Actuator y Validation out-of-the-box. Reconocible como elección de producción por un tribunal de máster. |
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
| CI/CD | GitHub Actions | Pipeline declarativo en YAML, sin infraestructura adicional. |
| SAST | Semgrep | Reglas específicas para Spring Security y React. |
| CVE scan | OWASP Dependency-Check | Escanea dependencias transitivas contra NVD; falla el build si CVSS ≥ 7. |
| Docs API | SpringDoc OpenAPI (Swagger UI) | Generación automática desde anotaciones; útil para demo ante tribunal. |
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

**GpsPosition** — id, vehicleId(indexed), latitude, longitude, heading, speed, recordedAt(indexed), source(MOCK/DEVICE)

**AuditLog** — id, entityType, entityId, action, performedByUserId, performedByEmail, performedAt, ipAddress, oldValues(JSONB), newValues(JSONB), details

### Rentabilidad (vista calculada, no entidad)
- Ingresos por vehículo = SUM(InvoiceLineItems vinculados a Jobs de ese vehículo)
- Costes por vehículo = SUM(MaintenanceRecord.cost)
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

```
FleetMgm/
├── planning.md                                 ← este documento
├── backend/
│   ├── pom.xml                                 ← Java 21, Spring Boot 3.3.5
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
│           ├── V4__create_workers.sql
│           ├── V5__create_jobs.sql
│           ├── V6__create_maintenance_workshop.sql
│           ├── V7__create_invoices.sql
│           ├── V8__create_gps_audit.sql
│           └── V9__seed_demo_data.sql
├── frontend/
│   ├── package.json                            ← Vite + React + TS + Tailwind
│   └── src/
│       ├── api/client.ts                       ← Axios + JWT interceptor + refresh automático
│       ├── store/authStore.ts                  ← Zustand
│       ├── pages/{Login, Dashboard, Vehicles, Workers, Jobs, Workshop, Billing, Map, Clients}.tsx
│       ├── components/{ui/, vehicle/, worker/, job/, map/, billing/, workshop/}
│       └── hooks/{useAuth, useVehicles, useWorkers, useJobs}
├── .github/workflows/
│   ├── ci.yml                                  ← Tests + OWASP + SAST en PRs
│   └── security.yml                            ← OWASP semanal programado
└── docker-compose.yml                          ← postgres:16 + backend + frontend (nginx)
```

---

## Plan de Desarrollo por Hitos

### Base ya implementada
- [x] `pom.xml` — Java 21, Spring Boot 3.3.5, dependencias completas
- [x] `application.yml` — datasource, JWT config, actuator, springdoc
- [x] Estructura de paquetes (package-by-feature)
- [x] Entidades `User`, `RefreshToken`, `AppRole` (enum)
- [x] `JwtService` — generación y validación de tokens HS512
- [x] `JwtAuthenticationFilter` — extrae y valida Bearer token en cada request
- [x] `GlobalExceptionHandler`, `PageResponse<T>`, `AuditLog`

---

### Hito 1 — Auth: SecurityConfig
- [ ] `SecurityConfig` — `HttpSecurity`: sesión stateless, deshabilitar CSRF, registrar `JwtAuthenticationFilter`
- [ ] `SecurityConfig` — endpoints públicos (`/api/v1/auth/**`, `/actuator/health`) vs. autenticados (`anyRequest().authenticated()`)
- [ ] `SecurityConfig` — CORS: origen permitido desde env var `FRONTEND_URL`, métodos y headers permitidos, nunca `*`

### Hito 2 — Auth: Contrato API
- [ ] `Flyway V1` — tabla `users` + tabla `refresh_tokens`
- [ ] `LoginRequest` / `AuthResponse` / `RefreshRequest` (records) con `@Valid`
- [ ] `AuthController` — `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`

### Hito 3 — Auth: Lógica de login y lockout
- [ ] `AuthService.login()` — verificar credenciales BCrypt, comprobar cuenta habilitada, generar access token (15 min) + refresh token
- [ ] `AuthService` — lockout: incrementar `failedLoginAttempts`; bloquear `lockedUntil = now + 15 min` al 5.º fallo; resetear en login exitoso
- [ ] `AuthService` — registrar `AuditLog` en login exitoso y en bloqueo de cuenta
- [ ] Tests `AuthServiceTest` — login OK, contraseña incorrecta, cuenta deshabilitada, lockout tras 5 intentos

### Hito 4 — Auth: Refresh, logout y tests de controller
- [ ] `AuthService.refresh()` — validar hash SHA-256 del refresh token en BD, emitir nuevo access token
- [ ] `AuthService.logout()` — eliminar hash del refresh token de BD (revocación real)
- [ ] Tests `AuthServiceTest` — refresh válido, refresh expirado, logout
- [ ] Tests `AuthControllerTest` (`@WebMvcTest`) — 200, 400, 401, cuerpo de error estructurado
- [ ] `DataInitializer` — seed usuarios demo en perfil `dev` (1 ADMIN, 1 MANAGER, 1 DRIVER) con BCrypt cost 12

> **Entregable hitos 1–4:** Login funcional con JWT, lockout, refresh y logout. SpringDoc expone el spec `/api-docs`.

---

### Hito 5 — Clientes: Contrato API
- [ ] `Flyway V2` — tabla `clients`
- [ ] `Client` entity — campos, `@SQLRestriction("deleted_at IS NULL")`
- [ ] `CreateClientRequest` / `UpdateClientRequest` / `ClientResponse` (records)
- [ ] `ClientMapper` (MapStruct) — `toResponse`, `toEntity`, `updateEntity`
- [ ] `ClientController` — `GET /api/v1/clients`, `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`; `Location` header en POST, `204` en DELETE

### Hito 6 — Clientes: Lógica e implementación
- [ ] `ClientRepository` — `findAll` paginado, `existsByTaxId`
- [ ] `ClientService` — `create`, `findAll`, `findById`, `update`, `delete` (soft), `@Transactional`
- [ ] `@PreAuthorize` en `ClientService` — solo ADMIN/MANAGER/ADMINISTRATIVE
- [ ] Tests `ClientServiceTest` — crear OK, taxId duplicado → excepción, findById no encontrado → 404, soft delete
- [ ] Tests `ClientRepositoryTest` (`@DataJpaTest` + Testcontainers PostgreSQL 16) — soft delete excluye registros con `deleted_at`
- [ ] Tests `ClientControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403

---

### Hito 7 — Vehículos: Contrato API
- [ ] `Flyway V3` — tabla `vehicles`
- [ ] `Vehicle` entity — campos, enums `VehicleCategory` / `VehicleStatus` / `UsageMeasure`, `@SQLRestriction`
- [ ] `CreateVehicleRequest` / `UpdateVehicleRequest` / `VehicleResponse` (records)
- [ ] `VehicleMapper` (MapStruct)
- [ ] `VehicleController` — CRUD completo

### Hito 8 — Vehículos: Lógica e implementación
- [ ] `VehicleRepository` — `findAll` paginado, `findAllActiveWithAssignment` (JOIN FETCH)
- [ ] `VehicleService` — `create`, `findAll`, `findById`, `update`, `delete` (soft), `@Transactional`
- [ ] `@PreAuthorize` en `VehicleService` — ADMIN/MANAGER/ADMINISTRATIVE crean/editan; DRIVER solo ve el suyo
- [ ] Tests `VehicleServiceTest` — crear OK, licensePlate duplicada → excepción, findById no encontrado, soft delete
- [ ] Tests `VehicleRepositoryTest` (`@DataJpaTest` + Testcontainers)
- [ ] Tests `VehicleControllerTest` (`@WebMvcTest`)

---

### Hito 9 — Trabajadores: Contrato API
- [ ] `Flyway V4` — tabla `workers`
- [ ] `Worker` entity — campos, enums `WorkerRole` (DRIVER/TECHNICIAN/BOTH), `LicenseType`, `@SQLRestriction`
- [ ] `CreateWorkerRequest` / `UpdateWorkerRequest` / `WorkerResponse` (records)
- [ ] `WorkerMapper` (MapStruct)
- [ ] `WorkerController` — CRUD completo

### Hito 10 — Trabajadores: Lógica e implementación
- [ ] `WorkerRepository` — `findAll` paginado, `existsByNationalId`
- [ ] `WorkerService` — `create`, `findAll`, `findById`, `update`, `delete` (soft), `@Transactional`
- [ ] `@PreAuthorize` en `WorkerService` — ADMIN/MANAGER/ADMINISTRATIVE gestionan; DRIVER solo ve su perfil
- [ ] Tests `WorkerServiceTest` — crear OK, nationalId duplicado → excepción, findById no encontrado, soft delete
- [ ] Tests `WorkerRepositoryTest`, `WorkerControllerTest`

---

### Hito 11 — Asignaciones conductor↔vehículo: Contrato API
- [ ] `Flyway V5a` — tabla `driver_vehicle_assignments` con unique partial index (`WHERE end_date IS NULL`)
- [ ] `DriverVehicleAssignment` entity
- [ ] `CreateAssignmentRequest` / `AssignmentResponse` (records)
- [ ] `AssignmentMapper` (MapStruct)
- [ ] `AssignmentController` — `POST /api/v1/assignments` (asignar), `PATCH /{id}/end` (finalizar), `GET /api/v1/workers/{id}/assignments` (historial)

### Hito 12 — Asignaciones: Lógica e implementación
- [ ] `AssignmentRepository` — `findActiveByDriverId`, `findActiveByVehicleId`, historial paginado
- [ ] `AssignmentService.assign()` — validar una sola asignación activa por conductor, crear asignación
- [ ] `AssignmentService.endAssignment()` — `endDate = now()` en la asignación activa
- [ ] `@PreAuthorize` — solo ADMIN/MANAGER/ADMINISTRATIVE pueden asignar
- [ ] Tests `AssignmentServiceTest` — asignar OK, conductor ya tiene asignación activa → excepción, finalizar asignación
- [ ] Tests `AssignmentRepositoryTest` (`@DataJpaTest`), `AssignmentControllerTest`

---

### Hito 13 — Trabajos: Contrato API
- [ ] `Flyway V5b` — tablas `jobs` + `usage_logs`
- [ ] `Job` entity — campos, enum `JobStatus` (PENDING/IN_PROGRESS/COMPLETED/CANCELLED), `@SQLRestriction`
- [ ] `UsageLog` entity
- [ ] `CreateJobRequest` / `UpdateJobRequest` / `JobResponse` (records)
- [ ] `JobMapper` (MapStruct)
- [ ] `JobController` — CRUD + `PATCH /{id}/start`, `PATCH /{id}/complete`, `PATCH /{id}/cancel`

### Hito 14 — Trabajos: Lógica y eventos
- [ ] `JobRepository` — `findAll` paginado, `findByAssignedDriverIdAndStatusIn` (para DRIVER), JOIN FETCH
- [ ] `UsageLogRepository`
- [ ] `JobService.create()` — crear PENDING
- [ ] `JobService.start()` — PENDING → IN_PROGRESS, `actualStart = now()`
- [ ] `JobService.complete()` — IN_PROGRESS → COMPLETED, `actualEnd = now()`, publicar `JobCompletedEvent`
- [ ] `JobService.cancel()` — PENDING/IN_PROGRESS → CANCELLED
- [ ] `JobCompletedEvent` (record) + `JobEventListener` — `@TransactionalEventListener(AFTER_COMMIT)`: crear `UsageLog`, actualizar `currentKm`/`currentHours` en `Vehicle`
- [ ] `@PreAuthorize` — DRIVER solo ve y cambia estado de sus trabajos activos
- [ ] Tests `JobServiceTest` — crear OK, transición PENDING→IN_PROGRESS, retroceder estado → excepción, completar publica evento
- [ ] Tests `JobEventListenerTest`, `JobRepositoryTest` (`@DataJpaTest`), `JobControllerTest`

---

### Hito 15 — Mantenimiento: Contrato API
- [ ] `Flyway V6a` — tabla `maintenance_records`
- [ ] `MaintenanceRecord` entity — campos, enum `MaintenanceStatus` (SCHEDULED/IN_PROGRESS/COMPLETED)
- [ ] `CreateMaintenanceRequest` / `MaintenanceResponse` (records)
- [ ] `MaintenanceMapper` (MapStruct)
- [ ] `MaintenanceController` — CRUD + `PATCH /{id}/start`, `PATCH /{id}/complete`

### Hito 16 — Mantenimiento: Lógica y eventos
- [ ] `MaintenanceRepository`
- [ ] `VehicleEntersWorkshopEvent` + `MaintenanceCompletedEvent` (records)
- [ ] `MaintenanceService.create()` — crear SCHEDULED, publicar `VehicleEntersWorkshopEvent`
- [ ] `MaintenanceService.start()` — SCHEDULED → IN_PROGRESS
- [ ] `MaintenanceService.complete()` — IN_PROGRESS → COMPLETED, `workshopExitDate = now()`, publicar `MaintenanceCompletedEvent`
- [ ] `VehicleService` — `@TransactionalEventListener`: `VehicleEntersWorkshopEvent` → status `MAINTENANCE`; `MaintenanceCompletedEvent` → status `ACTIVE`
- [ ] `@PreAuthorize` — WORKSHOP_STAFF puede crear/editar; ADMIN/MANAGER/ADMINISTRATIVE también
- [ ] Tests `MaintenanceServiceTest` — crear publica evento, completar publica evento, Vehicle cambia de estado
- [ ] Tests `VehicleStatusEventTest`, `MaintenanceControllerTest`

---

### Hito 17 — Agenda del taller: Contrato API
- [ ] `Flyway V6b` — tabla `workshop_schedules`
- [ ] `WorkshopSchedule` entity — campos, prioridad, estado
- [ ] `CreateScheduleRequest` / `ScheduleResponse` (records)
- [ ] `ScheduleMapper` (MapStruct)
- [ ] `WorkshopController` — CRUD + `GET /api/v1/workshop/schedules?range=today|week|month`

### Hito 18 — Agenda del taller: Lógica e implementación
- [ ] `WorkshopScheduleRepository` — queries por rango de fecha: hoy, semana actual, mes actual
- [ ] `WorkshopScheduleService` — crear, editar, cancelar, listar por rango
- [ ] `@PreAuthorize` — WORKSHOP_STAFF y superiores
- [ ] Tests `WorkshopScheduleServiceTest` — listar hoy, listar semana, cancelar
- [ ] Tests `WorkshopScheduleRepositoryTest` (`@DataJpaTest`), `WorkshopControllerTest`

---

### Hito 19 — Facturación: Contrato API
- [ ] `Flyway V7` — tablas `invoices` + `invoice_line_items`
- [ ] `Invoice` entity — campos, enum `InvoiceStatus` (DRAFT/ISSUED/PAID/OVERDUE), `@SQLRestriction`
- [ ] `InvoiceLineItem` entity
- [ ] `CreateInvoiceRequest` / `InvoiceResponse` / `LineItemRequest` (records)
- [ ] `InvoiceMapper` (MapStruct)
- [ ] `InvoiceController` — CRUD + `PATCH /{id}/issue`, `PATCH /{id}/pay`, `POST /{id}/line-items`

### Hito 20 — Facturación: Lógica e implementación
- [ ] `InvoiceRepository`, `LineItemRepository`
- [ ] `InvoiceNumberGenerator` — secuencia PostgreSQL `INV-2026-00001`
- [ ] `BillingService.create()` — crear DRAFT
- [ ] `BillingService.addLineItem()` — añadir línea a factura DRAFT
- [ ] `BillingService.issue()` — DRAFT → ISSUED; valida ≥1 línea; calcula subtotal, IVA, total
- [ ] `BillingService.markPaid()` — ISSUED → PAID, `paymentDate = now()`
- [ ] `BillingService` — `JobCompletedEvent` consumer: crear línea de factura en la DRAFT del cliente
- [ ] `@PreAuthorize` — solo ADMIN/MANAGER/ADMINISTRATIVE
- [ ] Tests `BillingServiceTest` — crear DRAFT, emitir sin líneas → excepción, flujo completo DRAFT→ISSUED→PAID, cálculo IVA 21%
- [ ] Tests `BillingControllerTest`

### Hito 21 — PDF y rentabilidad
- [ ] `PdfExportService` — generar PDF con OpenPDF (cabecera, líneas, totales, IVA)
- [ ] `GET /api/v1/invoices/{id}/pdf` — `Content-Disposition: attachment; filename="INV-...pdf"`
- [ ] `ProfitabilityRepository` — `@Query` projection: ingresos (`SUM` line items), costes (`SUM` maintenance.cost), margen por vehículo
- [ ] `GET /api/v1/reports/profitability` — paginado, solo ADMIN/MANAGER
- [ ] Tests `PdfExportServiceTest`, `ProfitabilityRepositoryTest` (`@DataJpaTest`)

---

### Hito 22 — GPS: Contrato API
- [ ] `Flyway V8a` — tabla `gps_positions` con índices en `vehicle_id` y `recorded_at`
- [ ] `GpsPosition` entity — lat, lng, heading, speed, `source` (MOCK/DEVICE)
- [ ] `GpsPositionResponse` (record)
- [ ] `GpsController` — `GET /api/v1/gps/latest`

### Hito 23 — GPS: Lógica e implementación
- [ ] `GpsRepository` — `findLatestByVehicleId`, `findLatestForAllActiveVehicles` (proyección)
- [ ] `GpsMockScheduler` — `@Scheduled(fixedDelay = 30_000)`, genera posiciones con deriva aleatoria para vehículos ACTIVE
- [ ] `@PreAuthorize` — ADMIN/MANAGER/ADMINISTRATIVE ven todos; DRIVER solo su posición
- [ ] Tests `GpsRepositoryTest` (`@DataJpaTest`)

---

### Hito 24 — AuditLog viewer
- [ ] `Flyway V8b` — tabla `audit_logs`
- [ ] `AuditLogRepository` — `findAll` paginado con filtros (entityType, action, rango de fechas)
- [ ] `AuditLogResponse` (record) + `AuditLogController` — `GET /api/v1/audit`, solo ADMIN/MANAGER
- [ ] Tests `AuditControllerTest`

---

### Hito 25 — Frontend: Setup base y auth
- [ ] Axios client con interceptor JWT + lógica de auto-refresh al recibir 401 (`api/client.ts`)
- [ ] Zustand `authStore` — sesión de usuario (email, rol, tokens), acciones login/logout
- [ ] `useAuth` hook — wraps login/logout mutations de TanStack Query
- [ ] Página `Login` — formulario, manejo de error 401 y mensaje de cuenta bloqueada
- [ ] Layout principal — sidebar con navegación filtrada por rol del usuario autenticado
- [ ] Rutas protegidas — redirige a Login si no hay sesión; 403 si rol insuficiente

### Hito 26 — Frontend: Datos maestros
- [ ] `useClients` hook + página `Clients` — lista paginada, modal crear, modal editar, soft delete
- [ ] `useVehicles` hook + página `Vehicles` — lista paginada, CRUD, badge de estado
- [ ] `useWorkers` hook + página `Workers` — lista paginada, CRUD
- [ ] `useAssignments` hook — asignar conductor a vehículo desde detalle de vehículo, historial

### Hito 27 — Frontend: Operaciones
- [ ] `useJobs` hook + página `Jobs` — lista paginada, crear trabajo, botones de cambio de estado (DRIVER solo sus trabajos activos)
- [ ] `useWorkshop` hook + página `Workshop` — vista por rango (hoy/semana/mes), crear/cancelar órdenes
- [ ] `useBilling` hook + página `Billing` — lista facturas, crear, emitir, marcar pagada, botón descarga PDF

### Hito 28 — Frontend: GPS y Dashboard
- [ ] `useGps` hook — polling cada 10 s a `/api/v1/gps/latest`
- [ ] Página `Map` — mapa Leaflet con marcadores de vehículos activos, popover con datos del vehículo
- [ ] `useProfitability` hook + sección `Dashboard` — gráfico Recharts ingresos vs costes por vehículo
- [ ] Página `AuditLog` — tabla paginada con filtros por entidad y acción (solo ADMIN/MANAGER)

---

### Hito 29 — Tests de integración (`@SpringBootTest` + Testcontainers)
- [ ] `AuthFlowIT` — login correcto → JWT → endpoint protegido; 5 intentos fallidos → cuenta bloqueada → 401
- [ ] `JobLifecycleIT` — crear job → iniciar → completar → verificar `UsageLog` creado y `currentKm` actualizado
- [ ] `InvoiceFlowIT` — crear DRAFT → añadir línea → emitir → pagar → descargar PDF

### Hito 30 — CI/CD
- [ ] `.github/workflows/ci.yml` — build + tests + OWASP Dependency-Check + Semgrep en cada PR; falla si CVSS ≥ 7
- [ ] `.github/workflows/security.yml` — OWASP scan semanal programado
- [ ] Anclar GitHub Actions a SHAs concretos (no tags mutables — supply chain)

### Hito 31 — Demo y hardening final
- [ ] `docker-compose.yml` — postgres:16 + backend + frontend (nginx), health checks, `depends_on`
- [ ] `Flyway V9` — seed datos demo realistas (5 vehículos, 3 conductores, 10 trabajos completados, 3 facturas)
- [ ] Revisar headers HTTP en `SecurityConfig`: `X-Content-Type-Options`, `X-Frame-Options`, `HSTS` (prod)
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

---

## Despliegue (coste cero)

**Recomendado:**
- Frontend → **Vercel** (gratis, deploy automático desde GitHub)
- Backend + BD → **Railway** (crédito $5/mes cubre el proyecto; sin cold start)

**Alternativa backup:**
- BD → Neon.tech (PostgreSQL gratuito 0.5GB)
- Backend → Render (gratis, pero cold start ~30s — avisar al tribunal)

**Demo presencial:**
```bash
docker compose up        # levanta todo localmente
ngrok http 8080          # URL pública temporal para el tribunal
```

**Variables de entorno en producción (Railway secrets):**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://...
JWT_SECRET=<min 64 chars random>
SPRING_PROFILES_ACTIVE=prod
FRONTEND_URL=https://fleetmgm.vercel.app
```
