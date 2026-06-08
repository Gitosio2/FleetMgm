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

## Plan de Desarrollo por Semanas

### Semana 1 (4–10 Jun): Fundación — Auth + Datos Maestros ← EN CURSO

#### Auth — Base ya implementada
- [x] `pom.xml` — Java 21, Spring Boot 3.3.5, dependencias completas
- [x] `application.yml` — datasource, JWT config, actuator, springdoc
- [x] Estructura de paquetes (package-by-feature)
- [x] Entidades `User`, `RefreshToken`, `AppRole` (enum)
- [x] `JwtService` — generación y validación de tokens HS512
- [x] `JwtAuthenticationFilter` — extrae y valida Bearer token en cada request
- [x] `GlobalExceptionHandler`, `PageResponse<T>`, `AuditLog`

#### Auth — SecurityConfig
- [ ] `SecurityConfig` — `HttpSecurity`: sesión stateless, deshabilitar CSRF, registrar `JwtAuthenticationFilter`
- [ ] `SecurityConfig` — endpoints públicos (`/api/v1/auth/**`, `/actuator/health`) vs. autenticados
- [ ] `SecurityConfig` — CORS: origen permitido desde env var `FRONTEND_URL`, métodos y headers permitidos

#### Auth — Lógica de login
- [ ] `Flyway V1` — tabla `users` + tabla `refresh_tokens`
- [ ] `LoginRequest` / `AuthResponse` (records) — DTOs con `@Valid`
- [ ] `AuthService.login()` — verificar credenciales con BCrypt, comprobar cuenta habilitada/no expirada, generar access token (15 min) y refresh token

#### Auth — Lockout y seguridad
- [ ] `AuthService` — lockout: incrementar `failedLoginAttempts` en cada intento fallido
- [ ] `AuthService` — bloquear cuenta 15 min (`lockedUntil`) tras 5 intentos; resetear contador en login exitoso
- [ ] `AuthService` — registrar `AuditLog` en cada login exitoso y en cada bloqueo de cuenta

#### Auth — Refresh y logout
- [ ] `AuthService.refresh()` — buscar hash SHA-256 del refresh token en BD, validar expiración, emitir nuevo access token
- [ ] `AuthService.logout()` — eliminar hash del refresh token de BD (revocación real, no solo expirar el JWT)

#### Auth — Controlador y tests
- [ ] `AuthController` — `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- [ ] Tests `AuthServiceTest` — login OK, contraseña incorrecta, cuenta bloqueada, refresh válido, refresh expirado, logout
- [ ] Tests `AuthControllerTest` (`@WebMvcTest`) — 200, 400, 401 status codes, cuerpo de error estructurado

#### Datos maestros — Clientes
- [ ] `Flyway V2` — tabla `clients`
- [ ] `Client` entity — campos, `@SQLRestriction("deleted_at IS NULL")`
- [ ] `ClientRepository` — findAll paginado, findByTaxId
- [ ] `CreateClientRequest` / `ClientResponse` (records) + `ClientMapper` (MapStruct)
- [ ] `ClientService` — `create`, `findAll` (paginado), `findById`, `update`, `delete` (soft)
- [ ] `ClientController` — CRUD completo, `Location` header en POST, `204` en DELETE
- [ ] `@PreAuthorize` en `ClientService` — solo ADMIN/MANAGER/ADMINISTRATIVE
- [ ] Tests `ClientServiceTest` (Mockito), `ClientControllerTest` (`@WebMvcTest`)

#### Datos maestros — Vehículos
- [ ] `Flyway V3` — tabla `vehicles`
- [ ] `Vehicle` entity — campos, enums `VehicleCategory` / `VehicleStatus` / `UsageMeasure`, `@SQLRestriction`
- [ ] `VehicleRepository` — findAll paginado, query `findAllActiveWithAssignment` (JOIN FETCH)
- [ ] `CreateVehicleRequest` / `VehicleResponse` (records) + `VehicleMapper`
- [ ] `VehicleService` — `create`, `findAll`, `findById`, `update`, `delete` (soft)
- [ ] `VehicleController` — CRUD completo
- [ ] `@PreAuthorize` en `VehicleService` — ADMIN/MANAGER/ADMINISTRATIVE crean/editan; DRIVER solo ve el suyo
- [ ] Tests `VehicleServiceTest`, `VehicleRepositoryTest` (`@DataJpaTest` + Testcontainers PostgreSQL 16), `VehicleControllerTest`

#### Datos maestros — Trabajadores
- [ ] `Flyway V4` — tabla `workers`
- [ ] `Worker` entity — campos, enums `WorkerRole` (DRIVER/TECHNICIAN/BOTH), `LicenseType`, `@SQLRestriction`
- [ ] `WorkerRepository`
- [ ] `CreateWorkerRequest` / `WorkerResponse` (records) + `WorkerMapper`
- [ ] `WorkerService` — `create`, `findAll`, `findById`, `update`, `delete` (soft)
- [ ] `WorkerController`
- [ ] `@PreAuthorize` en `WorkerService` — ADMIN/MANAGER/ADMINISTRATIVE gestionan; DRIVER solo ve su perfil
- [ ] Tests `WorkerServiceTest`, `WorkerControllerTest`

#### Inicialización
- [ ] `DataInitializer` — seed usuarios demo en perfil `dev` (1 ADMIN, 1 MANAGER, 1 DRIVER) con BCrypt cost 12

**Entregable:** Admin puede hacer login, recibir JWT, crear vehículos/trabajadores/clientes. JWT expira en 15 min y se puede renovar. Logout invalida el refresh token en BD.

---

### Semana 2 (11–17 Jun): Asignaciones + Ciclo de vida de Trabajos

#### Asignaciones conductor↔vehículo
- [ ] `Flyway V5a` — tabla `driver_vehicle_assignments` con unique partial index (`WHERE end_date IS NULL`)
- [ ] `DriverVehicleAssignment` entity
- [ ] `AssignmentRepository` — `findActiveByDriverId`, `findActiveByVehicleId`, historial paginado
- [ ] `CreateAssignmentRequest` / `AssignmentResponse` (records) + `AssignmentMapper`
- [ ] `AssignmentService.assign()` — validar que el conductor no tiene ya una asignación activa, crear asignación
- [ ] `AssignmentService.endAssignment()` — poner `endDate = now()` en la asignación activa
- [ ] `AssignmentService` — historial por conductor y por vehículo (paginado)
- [ ] `AssignmentController` — `POST` (asignar), `PATCH /{id}/end` (finalizar), `GET` (historial)
- [ ] `@PreAuthorize` en `AssignmentService` — solo ADMIN/MANAGER/ADMINISTRATIVE pueden asignar
- [ ] Tests `AssignmentServiceTest`, `AssignmentRepositoryTest` (`@DataJpaTest`), `AssignmentControllerTest`

#### Jobs — Entidad y consultas
- [ ] `Flyway V5b` — tabla `jobs` + tabla `usage_logs`
- [ ] `Job` entity — campos, enum `JobStatus` (PENDING/IN_PROGRESS/COMPLETED/CANCELLED), `@SQLRestriction`
- [ ] `JobRepository` — findAll paginado, `findByAssignedDriverIdAndStatusIn` (para DRIVER), JOIN FETCH vehículo y conductor
- [ ] `UsageLog` entity + `UsageLogRepository`

#### Jobs — Lógica de negocio
- [ ] `CreateJobRequest` / `JobResponse` (records) + `JobMapper`
- [ ] `JobService.create()` — crear trabajo PENDING, asignar vehículo y conductor
- [ ] `JobService.start()` — PENDING → IN_PROGRESS, registrar `actualStart`
- [ ] `JobService.complete()` — IN_PROGRESS → COMPLETED, registrar `actualEnd`, `endUsageValue`
- [ ] `JobService.cancel()` — PENDING/IN_PROGRESS → CANCELLED con motivo
- [ ] `JobService` — validación: no retroceder estado (COMPLETED no puede volver a IN_PROGRESS)

#### Jobs — Eventos y UsageLog
- [ ] `JobCompletedEvent` — record con `jobId`, `vehicleId`, `endUsageValue`, `measureType`
- [ ] `JobService` — publicar `JobCompletedEvent` via `ApplicationEventPublisher` al completar
- [ ] `JobEventListener` — `@TransactionalEventListener(AFTER_COMMIT)`: crear `UsageLog` y actualizar `currentKm`/`currentHours` en `Vehicle`

#### Jobs — Controlador, RBAC y tests
- [ ] `JobController` — CRUD + `PATCH /{id}/start`, `PATCH /{id}/complete`, `PATCH /{id}/cancel`
- [ ] `@PreAuthorize` en `JobService` — DRIVER solo ve y actualiza sus trabajos activos; ADMIN/MANAGER/ADMINISTRATIVE gestionan todos
- [ ] Tests `JobServiceTest` — create, transiciones de estado, publicación de evento
- [ ] Tests `JobEventListenerTest` — verifica `UsageLog` creado y `currentKm` actualizado al recibir el evento
- [ ] Tests `JobRepositoryTest` (`@DataJpaTest`), `JobControllerTest`

---

### Semana 3 (18–24 Jun): Taller + Mantenimiento

#### Registros de mantenimiento
- [ ] `Flyway V6a` — tabla `maintenance_records`
- [ ] `MaintenanceRecord` entity — campos, enum `MaintenanceStatus` (SCHEDULED/IN_PROGRESS/COMPLETED)
- [ ] `MaintenanceRepository`
- [ ] `CreateMaintenanceRequest` / `MaintenanceResponse` (records) + `MaintenanceMapper`
- [ ] `MaintenanceService.create()` — crear registro SCHEDULED, publicar `VehicleEntersWorkshopEvent`
- [ ] `MaintenanceService.start()` — SCHEDULED → IN_PROGRESS
- [ ] `MaintenanceService.complete()` — IN_PROGRESS → COMPLETED, registrar `workshopExitDate`, publicar `MaintenanceCompletedEvent`
- [ ] `VehicleEntersWorkshopEvent` + `MaintenanceCompletedEvent` (records)
- [ ] `VehicleService` — `@EventListener`: `VehicleEntersWorkshopEvent` → status `MAINTENANCE`
- [ ] `VehicleService` — `@EventListener`: `MaintenanceCompletedEvent` → status `ACTIVE`
- [ ] `MaintenanceController`
- [ ] `@PreAuthorize` — WORKSHOP_STAFF puede crear/editar; ADMIN/MANAGER/ADMINISTRATIVE también
- [ ] Tests `MaintenanceServiceTest`, `VehicleStatusEventTest` (verifica cambios de estado en Vehicle)

#### Agenda del taller
- [ ] `Flyway V6b` — tabla `workshop_schedules`
- [ ] `WorkshopSchedule` entity — campos, prioridad, estado
- [ ] `WorkshopScheduleRepository` — queries por rango de fecha: hoy, semana actual, mes actual
- [ ] `CreateScheduleRequest` / `ScheduleResponse` (records) + `ScheduleMapper`
- [ ] `WorkshopScheduleService` — crear, editar, cancelar, listar por hoy/semana/mes
- [ ] `WorkshopController` — endpoints + query params de rango temporal
- [ ] `@PreAuthorize` — WORKSHOP_STAFF y superiores
- [ ] Tests `WorkshopScheduleServiceTest`, `WorkshopScheduleRepositoryTest`, `WorkshopControllerTest`

---

### Semana 4 (25 Jun – 1 Jul): Facturación

#### Facturas — Entidad y repositorio
- [ ] `Flyway V7` — tablas `invoices` + `invoice_line_items`
- [ ] `Invoice` entity — campos, enum `InvoiceStatus` (DRAFT/ISSUED/PAID/OVERDUE), `@SQLRestriction`
- [ ] `InvoiceLineItem` entity
- [ ] `InvoiceRepository`, `LineItemRepository`
- [ ] `InvoiceNumberGenerator` — secuencia PostgreSQL para formato `INV-2026-00001`

#### Facturas — Lógica de negocio
- [ ] `CreateInvoiceRequest` / `InvoiceResponse` / `LineItemRequest` (records) + `InvoiceMapper`
- [ ] `BillingService.create()` — crear factura DRAFT vinculada a un cliente
- [ ] `BillingService.addLineItem()` — añadir línea a factura en estado DRAFT
- [ ] `BillingService.issue()` — DRAFT → ISSUED; valida que tiene al menos una línea; calcula subtotal, IVA (21%), total
- [ ] `BillingService.markPaid()` — ISSUED → PAID, registrar `paymentDate`
- [ ] `BillingService` — `JobCompletedEvent` consumer: crear línea de factura automáticamente en la factura DRAFT del cliente
- [ ] `InvoiceController` — CRUD + `PATCH /{id}/issue`, `PATCH /{id}/pay`
- [ ] `@PreAuthorize` — solo ADMIN/MANAGER/ADMINISTRATIVE
- [ ] Tests `BillingServiceTest` — flujo completo DRAFT→ISSUED→PAID, cálculo IVA, consumer de evento

#### PDF y rentabilidad
- [ ] `PdfExportService` — generar PDF de factura con OpenPDF (cabecera, líneas, totales, IVA)
- [ ] `GET /api/v1/invoices/{id}/pdf` — descarga del PDF, `Content-Disposition: attachment`
- [ ] `ProfitabilityRepository` — `@Query` projection: ingresos (`SUM` line items), costes (`SUM` maintenance), margen por vehículo
- [ ] `GET /api/v1/reports/profitability` — paginado, solo ADMIN/MANAGER
- [ ] Tests `PdfExportServiceTest`, `ProfitabilityRepositoryTest` (`@DataJpaTest`)

---

### Semana 5 (2–8 Jul): GPS + Frontend completo

#### Backend GPS
- [ ] `Flyway V8a` — tabla `gps_positions` con índices en `vehicle_id` y `recorded_at`
- [ ] `GpsPosition` entity — campos: lat, lng, heading, speed, `source` (MOCK/DEVICE)
- [ ] `GpsRepository` — `findLatestByVehicleId`, `findLatestForAllActiveVehicles` (proyección)
- [ ] `GpsMockScheduler` — `@Scheduled(fixedDelay = 30_000)`: genera posiciones aleatorias con deriva para vehículos ACTIVE
- [ ] `GpsController` — `GET /api/v1/gps/latest` — lista última posición por vehículo activo
- [ ] `@PreAuthorize` — ADMIN/MANAGER/ADMINISTRATIVE ven todos; DRIVER solo su posición
- [ ] Tests `GpsRepositoryTest`

#### AuditLog viewer
- [ ] `Flyway V8b` — tabla `audit_logs`
- [ ] `AuditLogRepository` — findAll paginado con filtros (entityType, action, fecha)
- [ ] `GET /api/v1/audit` — paginado, solo ADMIN/MANAGER
- [ ] Tests `AuditControllerTest`

#### Frontend — Setup y auth
- [ ] Setup Axios client con interceptor JWT + lógica de auto-refresh al recibir 401 (`api/client.ts`)
- [ ] Zustand `authStore` — sesión de usuario (email, rol, tokens), acciones login/logout
- [ ] `useAuth` hook — wraps login/logout mutations
- [ ] Página `Login` — formulario, manejo de error 401 y cuenta bloqueada

#### Frontend — Layout y navegación
- [ ] Layout principal — sidebar con navegación filtrada por rol del usuario autenticado
- [ ] Rutas protegidas — redirige a Login si no hay sesión; redirige a 403 si rol insuficiente

#### Frontend — Datos maestros
- [ ] `useVehicles` hook + página `Vehicles` — lista paginada, modal crear, modal editar, soft delete
- [ ] `useWorkers` hook + página `Workers` — lista paginada, CRUD
- [ ] `useClients` hook + página `Clients` — lista paginada, CRUD
- [ ] `useAssignments` hook — asignar conductor a vehículo desde detalle de vehículo

#### Frontend — Operaciones
- [ ] `useJobs` hook + página `Jobs` — lista paginada, crear trabajo, cambiar estado (DRIVER solo sus trabajos activos)
- [ ] `useWorkshop` hook + página `Workshop` — vista semana con agenda, crear/cancelar órdenes
- [ ] `useBilling` hook + página `Billing` — lista facturas, crear, emitir, marcar pagada, descargar PDF

#### Frontend — GPS y Dashboard
- [ ] `useGps` hook — polling cada 10s a `/api/v1/gps/latest`
- [ ] Página `Map` — mapa Leaflet con marcadores de vehículos activos, popover con datos del vehículo
- [ ] `useProfitability` hook + sección `Dashboard` — gráfico Recharts ingresos vs costes por vehículo
- [ ] Página `AuditLog` — tabla paginada con filtros (solo ADMIN/MANAGER)

---

### Semana 6 (9–15 Jul): Hardening + Tests de integración + Demo

#### Tests de integración (`@SpringBootTest` + Testcontainers)
- [ ] `AuthFlowIT` — login correcto → JWT → endpoint protegido; login incorrecto 5 veces → cuenta bloqueada
- [ ] `JobLifecycleIT` — crear job → iniciar → completar → verificar `UsageLog` creado y `currentKm` actualizado
- [ ] `InvoiceFlowIT` — crear factura DRAFT → añadir línea → emitir → pagar → descargar PDF

#### CI/CD
- [ ] `.github/workflows/ci.yml` — build + tests + OWASP Dependency-Check + Semgrep en cada PR; falla si CVSS ≥ 7
- [ ] `.github/workflows/security.yml` — OWASP scan semanal programado
- [ ] Anclar GitHub Actions a SHAs concretos (no tags mutables — suply chain)

#### Demo y hardening final
- [ ] `docker-compose.yml` — postgres:16 + backend + frontend (nginx), health checks, `depends_on`
- [ ] `Flyway V9` — seed datos demo realistas (5 vehículos, 3 conductores, 10 trabajos completados, 3 facturas)
- [ ] Revisar headers HTTP en `SecurityConfig`: `X-Content-Type-Options`, `X-Frame-Options`, `HSTS` (prod)
- [ ] OWASP Dependency-Check — corregir cualquier CVE CVSS ≥ 7 que quede
- [ ] `README.md` — diagrama de arquitectura, capturas de pantalla, credenciales demo, instrucciones de arranque local y Railway

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
