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
│           ├── V14__add_workshop_time_range.sql                 ← aplicada, Hito 28
│           ├── V15__add_job_price.sql                           ← aplicada, Hito 31 (precio del job, para la línea de factura automática)
│           ├── V16__create_invoice_number_seq.sql               ← aplicada, Hito 31 (secuencia PostgreSQL para INV-2026-00001)
│           ├── V17__drop_dead_invoice_maintenance_links.sql     ← aplicada (limpieza: MaintenanceRecord.invoice e InvoiceLineItem.linkedMaintenance, nunca usados)
│           ├── V18__seed_demo_data.sql                          ← pendiente, Hito 45 (única migración de datos que falta)
│           └── V19__create_suppliers.sql                        ← aplicada, Hito 36 (entidad maestra Supplier + FK supplier_invoices.supplier_id)
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

> **Nota:** `SupplierInvoice` (facturas de proveedor / gastos operativos: mantenimiento, combustible, seguro, leasing, peajes) se scaffoldeó junto con el resto del dominio en el commit inicial, pero no estaba documentado en este plan hasta esta revisión — ver Hitos 32–33. Afecta directamente el cálculo de rentabilidad (ver Modelo de Dominio).

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
- [ ] Anclar `actions/checkout` / `actions/setup-java` a SHA concreto — diferido al Hito 45 (hardening final); por ahora usan tag `@v4`

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
- [x] **[GREEN]** Migración `V11__add_maintenance_deleted_at.sql` (`ALTER TABLE maintenance_records ADD COLUMN deleted_at TIMESTAMPTZ`) + campo `deletedAt` y `@SQLRestriction("deleted_at IS NULL")` en la entidad + `@Mapping(target = "deletedAt", ignore = true)` en `toEntity`/`updateEntity` del mapper *(la seed de datos, originalmente V11, pasó a V12, luego a V13, luego a V14/Hito 43, luego a V15/Hito 45, luego a V17/Hito 45 y ahora a **V18**/Hito 45 — Hito 28 insertó V14 para el rango horario, Hito 31 insertó V15 (precio del job) y V16 (secuencia de numeración de factura), el Hito 36 insertó V19 (entidad Supplier) sin afectar al número de la seed, y una limpieza posterior inserta V17 (borra `MaintenanceRecord.invoice`/`InvoiceLineItem.linkedMaintenance`, nunca usados); ver adenda de categoría de mantenimiento, Hito 26, Hito 28, Hito 31 y la limpieza de campos muertos)*
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
> reportes de coste (Hito 34: preventivo vs. correctivo por vehículo, indicador real de gestión de flotas).
> Rama propia `maintenance-preventive-corrective-category`, ramificada desde `hito24-workshop-maintenance-logic`
> (no desde `main`) para que la migración nueva encadene como `V12` sin chocar con la `V11` del soft-delete.
- [x] **[RED]** Tests `MaintenanceServiceTest` — `create()` sin `category` en el request → default `PREVENTIVE`; `create()` con `category=CORRECTIVE` → se persiste tal cual; `update()` puede recategorizar
- [x] **[GREEN]** Migración `V12__add_maintenance_category.sql` — `ALTER TABLE maintenance_records ADD COLUMN category VARCHAR(10) NOT NULL DEFAULT 'PREVENTIVE'`
- [x] **[GREEN]** Enum `MaintenanceCategory` (`PREVENTIVE`/`CORRECTIVE`) en `workshop.domain`
- [x] **[GREEN]** `MaintenanceRecord` — campo `category` (`@Enumerated(EnumType.STRING)`, default `PREVENTIVE`)
- [x] **[GREEN]** `CreateMaintenanceRequest`/`UpdateMaintenanceRequest`/`MaintenanceResponse` — agregar `category` (opcional en create; si es null, el service aplica `PREVENTIVE`; obligatorio en update — PUT es reemplazo completo)
- [x] **[GREEN]** `MaintenanceMapper` — `toEntity` ignora `category` (el service resuelve el default, evita que un `null` sobreescriba el default de la entidad); `updateEntity`/`toResponse` mapean directo por nombre
- [x] **[GREEN]** `MaintenanceService.create()`/`update()` — cablear `category` (default `PREVENTIVE` si viene null en create)
  > **Nota (revisión adenda):** al escribir esta adenda, `V12` estaba reservado para la seed de datos del Hito 43 (documentado tras el Hito 24). Se corrigieron las 4 referencias desactualizadas (`planning.md` líneas ~232, 535, 543, 705, y la nota de migraciones en `CLAUDE.md`): la seed de datos pasa de V12 a **V13**. Secuencia final: V11 (soft-delete, Hito 24) → V12 (categoría preventivo/correctivo, esta adenda) → V13 (seed data, Hito 43). Suite completa verde con integración: 166 tests (`./mvnw test -Pfailsafe`, Docker).

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
- [x] **[GREEN]** Migración `V13__add_workshop_schedule_deleted_at.sql` (`ALTER TABLE workshop_schedules ADD COLUMN deleted_at TIMESTAMPTZ`) + `deletedAt` + `@SQLRestriction` en la entidad + ignore en el mapper *(la seed de datos, hasta ahora V13, pasa a **V14**/Hito 43 en este hito — misma corrección de numeración que ya se hizo dos veces; actualizado `planning.md` línea del árbol de arquitectura, Hito 43, y `CLAUDE.md`; pasa a **V15**/Hito 45 al insertarse Hito 28, a **V17**/Hito 45 al insertarse Hito 31, y a **V18**/Hito 45 al insertarse la limpieza de campos muertos, ver notas de esas secciones)*
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

### Hito 28 — Agenda del taller: rango horario *(nuevo — sin hito asignado en el plan original)*
> Surgió al usuario plantearse la utilidad de una vista de "horario del día" para el taller: sin hora de
> inicio/fin, la agenda solo ordena por fecha, no por franja horaria, y no se puede construir una tabla que
> muestre qué trabajos ocupan qué momento del día. Decisión acordada con el usuario: campos de hora **libres**
> (`LocalTime` de inicio y fin), no slots horarios fijos — con un solo taller y pocos técnicos, un sistema de
> slots agrega complejidad de modelado (¿un trabajo ocupa uno o varios slots?) sin aportar nada que ordenar
> por hora no resuelva ya. Aplica a las dos entidades, con semántica distinta en cada una: `WorkshopSchedule`
> gana la franja **planificada** (para el lado "tiene que hacer" de la vista de horario); `MaintenanceRecord`
> gana la hora real de entrada/salida, complemento de `workshopEntryDate`/`workshopExitDate` ya existentes
> (para el lado "ha hecho" de la vista). **Decisión explícita del usuario sobre solapamientos:** por ahora se
> ignoran del todo — el service puede crear o actualizar dos franjas solapadas para el mismo técnico sin
> ningún aviso, ni bloqueo ni warning. Cuando exista una ventana visual de horario (Hito 29 u otra futura) se
> añadirá una advertencia de solapamiento en esa vista; no antes, y no como validación de backend.
- [x] **[RED]** Tests `WorkshopScheduleServiceTest` (+8) — `create()`/`update()` con solo `scheduledStartTime`, solo `scheduledEndTime`, ambos en orden válido → persisten; ambos con `end` anterior o igual a `start` → `BadRequestException("SCHEDULE_INVALID_TIME_RANGE")`; dos schedules del mismo técnico con franjas solapadas → ambas persisten sin ningún aviso (fija la decisión explícita de no validar solapamientos)
- [x] **[GREEN]** `Flyway V14__add_workshop_time_range.sql` — `ALTER TABLE workshop_schedules ADD COLUMN scheduled_start_time TIME, ADD COLUMN scheduled_end_time TIME`; `ALTER TABLE maintenance_records ADD COLUMN workshop_entry_time TIME, ADD COLUMN workshop_exit_time TIME` (todas nullable)
- [x] **[GREEN]** `WorkshopSchedule` entity — campos `scheduledStartTime`/`scheduledEndTime` (`LocalTime`, nullable)
- [x] **[GREEN]** `MaintenanceRecord` entity — campos `workshopEntryTime`/`workshopExitTime` (`LocalTime`, nullable)
- [x] **[GREEN]** `CreateScheduleRequest`/`UpdateScheduleRequest`/`ScheduleResponse` — añaden `scheduledStartTime`/`scheduledEndTime` (opcionales); validación de que `endTime` sea posterior a `startTime` cuando ambos vienen informados
- [x] **[GREEN]** `MaintenanceResponse` — expone `workshopEntryTime`/`workshopExitTime` (solo lectura; se fijan en `start()`/`complete()`, no en `create()`/`update()`, igual que sus contrapartes de fecha)
- [x] **[GREEN]** `MaintenanceService.start()`/`complete()` — aceptan hora opcional en el request (`StartMaintenanceRequest`/`CompleteMaintenanceRequest`); si no viene, usan `LocalTime.now()` igual que hoy hacen con la fecha
- [x] **[GREEN]** `ScheduleMapper`/`MaintenanceMapper` — los nuevos campos se mapean por coincidencia de nombre sin necesidad de `@Mapping` explícito (a diferencia de `priority`/`category`, no tienen default de entidad que proteger)
  > **Nota (revisión Hito 28):** (1) **Validación de rango horario, capa elegida:** el proyecto no tenía ninguna infraestructura de `@Constraint` de nivel de clase (cross-field) para Bean Validation, así que se descartó introducirla para una única comprobación de dos campos — habría sido sobre-ingeniería para el caso. Se implementó en su lugar un método privado `WorkshopScheduleService.validateTimeRange(LocalTime, LocalTime)`, invocado al inicio de `create()` y `update()` (antes de resolver `vehicleId`/`technicianId`/`maintenanceRecordId`, para fallar rápido sin tocar repositorios si el rango ya es inválido), que lanza `BadRequestException("SCHEDULE_INVALID_TIME_RANGE", ...)` cuando ambos campos vienen informados y `end` no es estrictamente posterior a `start` (`!endTime.isAfter(startTime)`, cubre tanto "antes" como "igual"). Mismo patrón ya usado por `MaintenanceService.create()` con `MAINTENANCE_SCHEDULED_DATE_REQUIRED`. (2) **Sin validación de solapamiento, confirmado:** `validateTimeRange()` solo compara el par `start`/`end` de la propia request consigo mismo — no consulta otros `WorkshopSchedule` del mismo técnico ni de otro. Test `create_allowsOverlappingTimeRanges_forSameTechnician_noValidationPerformed` crea dos schedules para el mismo técnico con franjas horarias que se solapan (09:00–11:00 y 10:00–12:00) y verifica que ambas llamadas a `create()` tienen éxito — fija la decisión explícita del usuario documentada en la introducción de este hito. (3) **Comportamiento real de `start()`/`complete()`:** se confirmó leyendo el código existente que `workshopEntryDate`/`workshopExitDate` **siempre** se fijan incondicionalmente a `LocalDate.now()` en `start()`/`complete()` — no hay override por request para la fecha. Para mantener la misma semántica "siempre se fija algo" en el par de hora, `workshopEntryTime`/`workshopExitTime` se fijan siempre también, pero con una fuente dual: usan el valor de `request.entryTime()`/`request.exitTime()` si el caller lo provee explícitamente, y si no, caen a `LocalTime.now()` — a diferencia de la fecha (que no tiene ningún campo de override en el request). Esto es exactamente lo que planning.md ya anticipaba en la línea original de este ítem ("si no viene, usan `LocalTime.now()` igual que hoy hacen con la fecha"). (4) **Mappers sin cambios de código:** `ScheduleMapper`/`MaintenanceMapper` no requirieron ninguna línea nueva — MapStruct mapea `scheduledStartTime`/`scheduledEndTime`/`workshopEntryTime`/`workshopExitTime` automáticamente por coincidencia de nombre entre DTO y entidad, igual que ya ocurre con el resto de los campos sin `@Mapping` explícito; confirmado compilando el proyecto tras el cambio de las entidades/DTOs. (5) **DTOs con record posicional — impacto en tests existentes:** al ser records de Java, añadir campos nuevos obliga a actualizar *todas* las invocaciones posicionales existentes (`CreateScheduleRequest`, `UpdateScheduleRequest`, `ScheduleResponse`, `MaintenanceResponse`, `StartMaintenanceRequest`, `CompleteMaintenanceRequest`), incluida la única invocación de producción en `ScheduleCreationListener`. Se añadieron los dos campos nuevos al final de cada record (después de `notes`/`createdAt` según el caso) para minimizar el churn — cada call site existente solo necesitó `, null, null` adicional, sin reordenar argumentos ya posicionados. (6) **TDD real, no solo plumbing:** el plumbing (campos nuevos nullable en DTOs/entidades) se implementó junto con la lógica porque los records no permiten una migración incremental compilable ítem por ítem; sí se verificó RED real para la lógica de negocio (la única con comportamiento a probar): se deshabilitó temporalmente la llamada a `validateTimeRange()` en `create()`/`update()`, se confirmaron 3 fallos (`create_throwsBadRequest_whenEndTimeNotAfterStartTime`, `create_throwsBadRequest_whenEndTimeEqualsStartTime`, `update_throwsBadRequest_whenEndTimeNotAfterStartTime`), y se restauró la línea original verificando vuelta a verde — descartando además, vía `git diff`, que la restauración no dejara residuos. (7) **Sin cambios en `WorkshopScheduleRepositoryTest`/`MaintenanceRepositoryTest`:** los campos nuevos son columnas planas sin ninguna query nueva que las use — no se inventó cobertura de repositorio para comportamiento inexistente, según el propio criterio del hito. (8) **Tests finales:** `./mvnw test` 220 → 228 (8 nuevos, todos en `WorkshopScheduleServiceTest`); `./mvnw test -Pfailsafe` 244 → 252 (mismos 8, Docker disponible en este entorno). Ambas suites en 0 fallos antes y después.

### Hito 29 — Frontend: Vista de horario del día *(nuevo)*
> Requiere: Hito 28 (backend rango horario)
- [x] **[RED]** Tests `DaySchedule.test.tsx` — combina `WorkshopSchedule` (planificado) y `MaintenanceRecord`
      (real) del día seleccionado, ordenados por hora; una entrada sin hora aparece al final, no rompe el
      orden de las que sí la tienen
- [x] **[RED]** Tests de los inputs de hora nuevos en `ScheduleFormModal` — validación de cliente "hora de fin
      posterior a hora de inicio" antes de enviar. **Corrección de alcance:** este ítem mencionaba también
      `MaintenanceFormModal` en el borrador original de este hito — es un error, corregido aquí sin
      implementarlo tal cual estaba escrito. `workshopEntryTime`/`workshopExitTime` de `MaintenanceRecord` son
      de solo lectura (los fija `start()`/`complete()` en el backend, Hito 28) y no forman parte de
      `CreateMaintenanceRequest`/`UpdateMaintenanceRequest` — no hay nada que un input en ese formulario
      pudiera enviar. Ver nota de revisión más abajo.
- [x] **[GREEN]** `ScheduleFormModal` — inputs `type="time"` para `scheduledStartTime`/`scheduledEndTime`
      (`MaintenanceFormModal` deliberadamente no tocado, ver nota de revisión)
- [x] **[GREEN]** Nuevo componente `DaySchedule.tsx` (tabla cronológica) montado en `Workshop.tsx`
- [x] **[GREEN]** MSW mocks — reflejan los nuevos campos en los handlers existentes de maintenance/schedules
  > **Nota (revisión Hito 29):** (1) **Corrección de alcance sobre `MaintenanceFormModal`:** el borrador
  > original de este hito proponía inputs de hora también en `MaintenanceFormModal`, pero
  > `workshopEntryTime`/`workshopExitTime` no son campos editables por el usuario — se fijan siempre
  > server-side en `start()`/`complete()` (con fallback a `LocalTime.now()` si no vienen en el request, ver
  > Hito 28), igual que ya ocurre con `workshopEntryDate`/`workshopExitDate`. `MaintenanceTable.tsx` ya llama
  > a `useStartMaintenance`/`useCompleteMaintenance` con valores hardcodeados (`usageAtService: null`,
  > `cost: null`) sin ninguna UI de input para esos campos opcionales existentes; añadir inputs de hora ahí
  > habría sido alcance nuevo no pedido. El frontend solo necesitaba **mostrar** ambos campos, lo cual hace
  > el nuevo `DaySchedule`. (2) **Conversión `HH:mm` ↔ `HH:mm:ss`:** `<input type="time">` emite/acepta
  > `HH:mm` (sin segundos) vía `onChange`; el backend espera `HH:mm:ss`. Se añadieron dos helpers a
  > `form-shared.ts`: `toNullableTime(value)` (`'' → null`, si no `` `${value}:00` ``) para el envío, y
  > `toTimeInputValue(value)` (recorta a los primeros 5 caracteres, o `''` si es `null`) para el prellenado.
  > La idea original del enunciado era prellenar directamente con el string `HH:mm:ss` del backend confiando
  > en que el navegador oculta los segundos visualmente — cierto en navegadores reales, pero **no** en jsdom
  > (entorno de test), que conserva el string literal sin sanitizar. Se optó por normalizar explícitamente a
  > `HH:mm` en el prellenado en lugar de depender de un comportamiento de sanitización específico del
  > navegador que los tests no podían verificar — mantiene el estado del componente en un único formato
  > consistente (`HH:mm`) en todo momento, evitando además una comparación de rango mixta `HH:mm` vs
  > `HH:mm:ss` en la validación de cliente. (3) **Diseño de `DaySchedule`:** fetching autocontenido —
  > `useWorkshopSchedules('today', 0, 100)` (server-side ya filtra por rango) + `useMaintenanceRecords(0,
  > 100)` filtrado client-side por `workshopEntryDate === hoy || workshopExitDate === hoy` (el endpoint no
  > tiene filtro de fecha). Comparador de orden estable: ambas entradas sin hora → `0`; una sin hora → va al
  > final; si no, comparación lexicográfica de los strings `HH:mm:ss` (`Array.prototype.sort` es estable en
  > todo motor JS que soporta este proyecto). Se montó como tercera sección en `Workshop.tsx`, **encima** de
  > "Agenda" — es el resumen "qué pasa hoy" más inmediatamente útil, y tiene sentido verlo antes que las
  > tablas completas de agenda/mantenimiento. (4) **Mocks con hora determinista:** los handlers `/start` y
  > `/complete` de `/api/v1/maintenance/:id` fijan `workshopEntryTime`/`workshopExitTime` a strings fijos
  > (`'08:00:00'`/`'16:00:00'`) en lugar de derivarlos de `new Date()`, mismo criterio ya documentado en el
  > comentario de `rangeTags` de este archivo — evita que las aserciones de orden por hora dependan de a qué
  > hora real se ejecuta la suite. (5) **Colisión de texto con `Workshop.test.tsx`:** montar `DaySchedule` en
  > `Workshop.tsx` hizo que `schedule-1` ("Cambio de aceite", el único seed con rango `today`) y cualquier
  > entrada de agenda recién creada (siempre etiquetadas para los tres rangos) aparecieran duplicados en la
  > página (tabla de Agenda + widget nuevo) — 11 tests existentes/nuevos de `Workshop.test.tsx` empezaron a
  > fallar por queries ambiguas (`getByText`/`findByText` sin acotar). Se corrigieron acotando esas
  > aserciones a la sección correspondiente vía dos helpers nuevos (`agendaSection()`/
  > `maintenanceSection()`, que ubican la `<section>` por su encabezado `<h2>`), en vez de evitar el
  > solapamiento — es un comportamiento real e intencional de la UI, no un bug a esconder. (6) **Tests
  > finales:** `vitest run` en `apps/web` 58 → 68 (10 nuevos: 4 en `Workshop.test.tsx` para los inputs de
  > hora de `ScheduleFormModal`, 6 en `DaySchedule.test.tsx`), 0 fallos antes y después. `tsc -b` limpio en
  > `apps/web`, `packages/api`, `packages/hooks`. `oxlint` sin advertencias nuevas (mismo warning preexistente
  > en `AssignmentModal.tsx`). Backend no tocado.

---

### Hito 30 — Facturación (clientes): Contrato API
- [x] `Flyway V7` — tablas `invoices` + `invoice_line_items` *(ya aplicada)*
- [x] `Invoice` entity — enum `InvoiceStatus` (DRAFT/ISSUED/PAID/OVERDUE), `@SQLRestriction`; `InvoiceLineItem` entity *(ya scaffoldeadas)*
- [x] `CreateInvoiceRequest` / `UpdateInvoiceRequest` / `InvoiceResponse` / `LineItemRequest` / `LineItemResponse` (records)
- [x] `InvoiceMapper` (MapStruct)
- [x] `InvoiceController` — CRUD (incl. `PUT` full replace) + `PATCH /{id}/issue`, `PATCH /{id}/pay`, `POST /{id}/line-items`
- [x] `InvoiceService` — stub, every method `throw new UnsupportedOperationException("Pending Hito 31")`, mirrors the `WorkshopScheduleService`/`MaintenanceService` contract-hito precedent

> **Nota (revisión Hito 30):**
> 1. `clientName` en `InvoiceResponse` mapea a `Client.getName()` — es el único campo de nombre que expone la entidad (no hay `companyName`/`fullName`).
> 2. `InvoiceResponse` deliberadamente no incluye una lista anidada de line items — `Invoice` no tiene colección `@OneToMany` hacia `InvoiceLineItem` en este hito; la consulta join-fetch queda para el Hito 31.
> 3. Se añadió `UpdateInvoiceRequest` y el endpoint `PUT /{id}` (pregunta abierta original), resuelto a favor por decisión del usuario, siguiendo el mismo precedente de `Maintenance`/`WorkshopSchedule` (contrato completo con `PUT` de reemplazo total).
> 4. `Invoice`/`InvoiceLineItem` no fueron modificadas — se usaron tal cual estaban scaffoldeadas.
> 5. `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")` se agregó ya desde el stub (no se esperó al Hito 31): es una anotación de una línea que no depende de que exista lógica de negocio, y refleja la restricción de rol de facturación documentada en `CLAUDE.md`/planning (nunca `WORKSHOP_STAFF`/`DRIVER`).

### Hito 31 — Facturación (clientes): Lógica e implementación
> **Nota de alcance:** el checklist original de este hito nombra `BillingService`/`BillingController`, pero
> Hito 30 ya creó `InvoiceService`/`InvoiceController` (nombrados por la entidad, no por el paquete) — la
> lógica de este hito va dentro de esos archivos ya existentes, no en clases nuevas.
>
> **Prerequisito descubierto al planificar este hito:** `Job` no tiene ningún campo de precio/tarifa, y
> `JobCompletedEvent` tampoco lo lleva — sin eso, el consumer no puede calcular `quantity`/`unitPrice` para la
> línea de factura automática. Decisión con el usuario: agregar `price` (`BigDecimal`, nullable — un `Job`
> sin `clientId` no factura nada, y uno con `clientId` pero sin `price` cargado es un gap de datos que el
> consumer trata como no-op, no como error bloqueante) a `Job`, seteable en `CreateJobRequest`/
> `UpdateJobRequest`. Migración nueva `V15__add_job_price.sql`, más `V16__create_invoice_number_seq.sql`
> (secuencia PostgreSQL para `InvoiceNumberGenerator`, ver más abajo) — la seed de datos pasa de **V15** a
> **V17** (misma corrección de numeración ya documentada dos veces en Hito 24/26, ahora una tercera vez).
- [x] `Flyway V15__add_job_price.sql` — `ALTER TABLE jobs ADD COLUMN price NUMERIC(12,2)` (nullable)
- [x] `Job` entity — campo `price` (`BigDecimal`, nullable)
- [x] `CreateJobRequest`/`UpdateJobRequest`/`JobResponse` — añaden `price` (opcional en create/update, siempre presente en response)
- [x] `JobCompletedEvent` — gana `clientId`/`price`/`title` (denormalizados en el evento, mismo criterio ya usado para `vehicleId`/`endUsageValue` — el consumer no necesita volver a resolver el `Job`)
- [x] `Flyway V16__create_invoice_number_seq.sql` — `CREATE SEQUENCE invoice_number_seq;`
- [x] **[RED]** Tests `InvoiceServiceTest` — crear DRAFT, emitir sin líneas → `ConflictException` (409, mismo patrón que las demás transiciones de estado inválidas del proyecto — no se introduce un status 422 nuevo), flujo completo DRAFT→ISSUED→PAID, cálculo IVA 21%, `JobCompletedEvent` crea línea en DRAFT del cliente (con `price`), `JobCompletedEvent` sin `clientId` o sin `price` → no-op
- [x] **[RED]** Tests `InvoiceControllerTest` (`@WebMvcTest`) — 201, 400, 404, 403 (el 403 por rol no se cubre acá — mismo gap AOP/`@PreAuthorize` heredado de Job/Vehicle/Maintenance: `@MockBean` en `@WebMvcTest` salta el proxy); emitir factura sin líneas → 409
- [x] **[GREEN]** `InvoiceRepository`, `LineItemRepository`
- [x] **[GREEN]** `InvoiceNumberGenerator` — secuencia PostgreSQL `INV-2026-00001` (año actual + secuencia de 5 dígitos con cero a la izquierda)
- [x] **[GREEN]** `InvoiceService.create()` — crear DRAFT
- [x] **[GREEN]** `InvoiceService.addLineItem()` — añadir línea a factura DRAFT
- [x] **[GREEN]** `InvoiceService.issue()` — DRAFT → ISSUED; valida ≥1 línea; calcula subtotal, IVA, total
- [x] **[GREEN]** `InvoiceService.pay()` — ISSUED → PAID, `paymentDate = now()` (el checklist original decía `markPaid()`; el controller ya expuesto en Hito 30 llama `pay()`, se implementó con ese nombre para matchear el contrato existente)
- [x] **[GREEN]** `InvoiceJobCompletionListener` — `JobCompletedEvent` consumer: crea línea de factura en la DRAFT del cliente (o crea una DRAFT nueva si no hay ninguna abierta)
- [x] **[GREEN]** `@PreAuthorize` — ya presente desde el stub de Hito 30, confirmado correcto (sin cambios)

> **Nota (revisión Hito 31):**
> 1. **Redondeo de `BigDecimal`:** `RoundingMode.HALF_UP` a 2 decimales (`MONEY_SCALE`) en `InvoiceService.issue()`
>    para `subtotal`/`taxAmount`/`total`. No había precedente de redondeo de dinero en el resto del código
>    (`MaintenanceRecord.cost` se persiste tal cual, nunca se calcula), así que se adoptó el estándar
>    convencional para aritmética de moneda.
> 2. **Guards DRAFT-only:** `update()`, `delete()`, `addLineItem()` e `issue()` en `InvoiceService` exigen
>    `status == DRAFT` (409 `INVOICE_INVALID_STATE_TRANSITION`/`INVOICE_DELETE_NOT_ALLOWED` si no) — decisión
>    deliberada, no un descuido: mejora consciente sobre el gap ya documentado y aceptado en
>    `MaintenanceService.update()`/`WorkshopScheduleService.update()` (sin guard de estado en `update()`).
>    Al construir `InvoiceService` desde cero para este hito, se cerró el guard correctamente en vez de
>    reproducir la misma deuda técnica.
> 3. **Secuencia de numeración:** `InvoiceRepository.nextInvoiceNumberSequenceValue()` — un método
>    `@Query(nativeQuery = true)` que envuelve `SELECT nextval('invoice_number_seq')`, expuesto en el mismo
>    `JpaRepository` de la entidad. No había precedente de `EntityManager`/`JdbcTemplate` en el resto del
>    código — cada repositorio en este proyecto es un `JpaRepository` plano — así que mantener el pull de la
>    secuencia dentro del propio repositorio de `Invoice` es la opción más simple y consistente, sin
>    introducir un mecanismo de acceso nuevo. `InvoiceNumberGenerator` (`billing.application`) formatea el
>    resultado como `INV-<Year.now()>-<%05d>`.
> 4. **`InvoiceJobCompletionListener` (find-or-create DRAFT):** vive en `billing.application` (mutó
>    `Invoice`/`InvoiceLineItem`, no `Job`) y sigue el mismo patrón `@TransactionalEventListener(phase =
>    AFTER_COMMIT)` con try/catch + log de error sin relanzar que ya usan los listeners de `workshop`.
>    No-op si `clientId` o `price` son `null`. Si hay una `DRAFT` abierta del cliente, la reutiliza
>    (`findFirstByClientIdAndStatusOrderByCreatedAtAsc`, empate resuelto por la más antigua); si no,
>    crea una nueva. La descripción de la línea auto-generada es simplemente `event.title()` (el título
>    del `Job`, sin prefijo) — se mantuvo simple según lo indicado, con fallback a `"Job " + jobId` si el
>    título viniera nulo (no debería ocurrir, `Job.title` es `@NotBlank`, pero se resuelve igual de forma
>    defensiva, como el resto de las relaciones opcionales en este código).
> 5. **Tests finales:** `./mvnw test` 230 → 271 (unit, +41: 40 tests nuevos en `billing.api`/`billing.application`
>    + 1 test nuevo en `JobServiceTest` para el evento sin cliente/precio). `./mvnw test -Pfailsafe`
>    254 → 300 (+46: los mismos 41 más las 5 pruebas nuevas de `InvoiceRepositoryTest`). Ambas suites en
>    verde, 0 failures/errors.
> 6. **Bug real encontrado en revisión independiente antes de comitear:** `addLineItem()` calculaba
>    `subtotal = quantity.multiply(unitPrice)` sin redondear a `MONEY_SCALE`, a diferencia de `issue()`, que sí
>    redondea todo. `LineItemRequest` no restringe la escala de `quantity`/`unitPrice`, así que un `quantity`
>    o `unitPrice` con más de 2 decimales (ej. `3 × 10.005 = 30.015`) devolvía un `subtotal` de escala 3 en el
>    `LineItemResponse` de `POST /{id}/line-items` — inconsistente con el contrato de moneda a 2 decimales que
>    el resto del feature respeta. No corrompía los totales agregados de `issue()` (la columna `NUMERIC(12,2)`
>    ya redondea al persistir), pero sí era una respuesta de API visible con más precisión de la prometida.
>    Corregido aplicando el mismo `.setScale(MONEY_SCALE, RoundingMode.HALF_UP)` que ya usa `issue()`. Test de
>    regresión añadido (`addLineItem_roundsSubtotalToTwoDecimals_whenMultiplicationProducesMore`, verifica
>    valor y escala explícitamente, no solo `isEqualByComparingTo` que ignora la escala). Suite final: 271 → 272.
> 7. **`taxRate` hardcodeado al 21%, detectado por el usuario tras abrir el PR:** `Invoice.taxRate` tenía un
>    valor por defecto en la entidad (`new BigDecimal("0.2100")`), pero ni `CreateInvoiceRequest` ni
>    `UpdateInvoiceRequest` exponían el campo, y `InvoiceMapper` lo ignoraba explícitamente en ambos
>    (`@Mapping(target = "taxRate", ignore = true)`) — no existía ninguna forma de cambiarlo sin tocar código.
>    El usuario señaló que el IVA cambia con el tiempo y que existen tarifas reducidas/bonificadas para
>    algunos escenarios, así que el 21% fijo no era aceptable. Solución en dos partes, sin tocar el `ignore
>    = true` del mapper (mismo criterio ya usado para `priority` en `ScheduleMapper`/`category` en
>    `MaintenanceMapper`: el default de la entidad no debe ser pisado silenciosamente por un `null` del
>    request; el service resuelve el default de forma explícita):
>    - **Default configurable:** `billing.default-tax-rate: ${BILLING_DEFAULT_TAX_RATE:0.2100}` en
>      `application.yml` (mismo patrón que `workshop.auto-create-schedule-on-maintenance-create`).
>      `InvoiceService` gana un parámetro de constructor `@Value("${billing.default-tax-rate}") BigDecimal
>      defaultTaxRate`, mismo estilo de inyección que `autoCreateScheduleOnCreate` en `MaintenanceService`.
>    - **Override opcional por factura:** `CreateInvoiceRequest`/`UpdateInvoiceRequest` ganan un campo
>      `taxRate` (`BigDecimal`, nullable, sin `@Positive` — un `@DecimalMin(value = "0")` permite una tarifa
>      reducida del 0%, legalmente real). Semántica de `null` **distinta** entre los dos métodos: en
>      `create()`, `null` → usa `defaultTaxRate`; en `update()`, `null` → **no toca** la tarifa ya existente
>      de la factura (no la resetea al default configurado). Esta asimetría es intencional: `update()` solo
>      debe aplicar lo que el caller provee explícitamente, nunca reemplazar un valor ya asignado por el
>      valor por defecto global.
>    - Tests nuevos en `InvoiceServiceTest`: default aplicado en `create()` sin `taxRate` en el request,
>      tarifa explícita respetada en `create()`, override aplicado en `update()`, tarifa existente preservada
>      en `update()` con `taxRate = null`, y un caso de `issue()` con tarifa no-21% (`0.10`) para probar que el
>      cálculo de impuesto lee el campo real y no tiene el 21% hardcodeado en ninguna parte. Suite final:
>      272 → 277 (5 tests nuevos). `BILLING_DEFAULT_TAX_RATE` no se agregó a la lista de variables de entorno
>      de producción en la sección "Despliegue" — es opcional con un default sano, mismo criterio ya aplicado
>      a `WORKSHOP_AUTO_CREATE_SCHEDULE` (tampoco listada ahí).
> 8. **`pay()` no aceptaba fecha de pago, detectado por el usuario tras el punto anterior:** igual que
>    `taxRate`, `PATCH /{id}/pay` no recibía body — siempre `LocalDate.now()`, sin forma de registrar un pago
>    retroactivo (ej. se cobró la semana pasada pero se carga hoy). Corregido con el mismo patrón ya usado en
>    `StartMaintenanceRequest`/`CompleteMaintenanceRequest`: nuevo `PayInvoiceRequest(LocalDate paymentDate)`
>    opcional (`@RequestBody(required = false)` en el controller); si `request`/`request.paymentDate()` es
>    `null`, usa `LocalDate.now()`, igual que antes. Tests nuevos en `InvoiceServiceTest` (fecha explícita
>    respetada, `null` cae a `now()`, request completo `null` cae a `now()`) y `InvoiceControllerTest`
>    (200 sin body, 200 con body). Suite final: 277 → 280.

### Hito 32 — Facturas de proveedor: Contrato API *(nuevo — sin hito asignado en el plan original)*
- [x] `Flyway V7` — tablas `supplier_invoices` + `supplier_invoice_line_items` *(ya aplicada, misma migración que invoices)*
- [x] `SupplierInvoice`, `SupplierInvoiceLineItem`, `SupplierInvoiceStatus` (PENDING/PAID), `ExpenseCategory` *(ya scaffoldeadas)*
- [x] `CreateSupplierInvoiceRequest` / `UpdateSupplierInvoiceRequest` / `SupplierInvoiceResponse` / `SupplierLineItemRequest` / `SupplierLineItemResponse` (records)
- [x] `SupplierInvoiceMapper` (MapStruct)
- [x] `SupplierInvoiceController` — CRUD (incl. `PUT` full replace) + `PATCH /{id}/pay`, `POST /{id}/line-items`
- [x] `SupplierInvoiceService` — stub, every method `throw new UnsupportedOperationException("Pending Hito 33")`, mirrors the `InvoiceService` Hito-30 contract-hito precedent

> **Nota (revisión Hito 32):**
> 1. **`subtotal`/`taxAmount`/`total` provistos directamente, no calculados:** a diferencia de `Invoice`
>    (facturación a clientes), donde la app calcula estos valores sumando line items al momento de `issue()`,
>    una `SupplierInvoice` representa una factura YA RECIBIDA de un proveedor externo — sus montos vienen tal
>    cual figuran en el documento físico/recibido. No existe un paso equivalente a `issue()` en el ciclo de
>    vida de este feature (`SupplierInvoiceStatus` es solo PENDING/PAID, sin DRAFT/ISSUED).
>    `SupplierInvoiceLineItem` existe puramente como desglose informativo de costos (ej. repartir el costo de
>    una factura de combustible entre varios vehículos, o vincular parte del gasto a un `MaintenanceRecord`
>    puntual) — sus líneas NO retroalimentan ni recalculan los totales de la factura. Por eso
>    `CreateSupplierInvoiceRequest`/`UpdateSupplierInvoiceRequest` exponen `subtotal`/`taxAmount`/`total` como
>    campos `@NotNull` directamente provistos, y `SupplierInvoiceMapper` los mapea por nombre en vez de
>    ignorarlos (a diferencia de `InvoiceMapper.toEntity()`, que sí los ignora porque en `Invoice` se calculan
>    después).
> 2. **`PayInvoiceRequest` reutilizado, no duplicado:** `SupplierInvoiceController.pay()` reutiliza el mismo
>    `PayInvoiceRequest(LocalDate paymentDate)` de `billing.dto` agregado en el Hito 31 para `Invoice.pay()` —
>    misma forma exacta (un único campo `paymentDate` opcional), no se justifica un
>    `PaySupplierInvoiceRequest` separado. Esto también aplica la lección ya aprendida en el Hito 31 (punto 8
>    de su nota de revisión) desde el arranque: el stub de `SupplierInvoiceService.pay()` acepta el
>    `paymentDate` opcional desde el día uno, sin necesitar un fix de seguimiento como le pasó a `Invoice`.
> 3. **Firma de `list()` con filtros por adelantado:** `SupplierInvoiceService.list(UUID vehicleId,
>    ExpenseCategory category, Pageable pageable)` define ya los dos parámetros de filtro opcionales que el
>    Hito 33 va a implementar ("listar por vehicleId, listar por categoría" en su checklist) — el stub
>    simplemente lanza `UnsupportedOperationException` sin importar los argumentos. Mismo criterio ya usado en
>    Hito 25 (definió el query param `range` antes de que Hito 26 implementara su lógica real).
> 4. `SupplierInvoice`/`SupplierInvoiceLineItem` no fueron modificadas — se usaron tal cual estaban
>    scaffoldeadas.
> 5. `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")` agregado ya desde el stub, mismo
>    criterio que Hito 30 — refleja la restricción de rol de facturación, nunca `WORKSHOP_STAFF`/`DRIVER`.
> 6. **Tests finales:** sin tests nuevos en este hito (contrato-only, `SupplierInvoiceServiceTest`/
>    `SupplierInvoiceControllerTest` quedan para el Hito 33). `./mvnw test` se mantiene en 280, 0
>    failures/errors — sin regresión.

### Hito 33 — Facturas de proveedor: Lógica e implementación *(nuevo)*
- [x] **[RED→GREEN]** Tests `SupplierInvoiceServiceTest` — crear PENDING (vehicle resuelto opcionalmente, 404 si vehicleId inválido), `update()`/`delete()`/`pay()`/`addLineItem()` solo desde PENDING (409 en cualquier otro caso), `pay()` usa `paymentDate` provisto o cae a `now()`, `addLineItem()` redondea subtotal a 2 decimales y NO toca los totales de la factura, `list()` delega al repositorio con filtros opcionales
- [x] **[RED→GREEN]** Tests `SupplierInvoiceControllerTest` (`@WebMvcTest`) — 201, 400, 404, 409, `pay` con/sin body (403 no se prueba — gap conocido y aceptado de `@WebMvcTest`/`@MockBean`, no atraviesa el proxy AOP de Security, igual que `InvoiceControllerTest`)
- [x] **[RED→GREEN]** Tests `SupplierInvoiceRepositoryTest` (`@DataJpaTest` + Testcontainers) — sin filtros devuelve todo, filtro por `vehicleId`, filtro por `category`, ambos combinados, `LEFT JOIN FETCH vehicle` inicializado sin queries extra, soft-deleted excluidos
- [x] **[GREEN]** `SupplierInvoiceRepository.findAllJoinFetch(vehicleId, category, pageable)` — JPQL parametrizado con el idiom `(:param IS NULL OR ...)` para ambos filtros opcionales (sin concatenación de strings, cumple la regla de SQL injection de `CLAUDE.md`); `SupplierInvoiceLineItemRepository` sin métodos extra (no hay paso de suma de líneas que lo necesite)
- [x] **[GREEN]** `SupplierInvoiceService.create()` — crea en PENDING, resuelve `vehicleId` opcionalmente (404 `VEHICLE_NOT_FOUND` si no existe)
- [x] **[GREEN]** `SupplierInvoiceService.pay()` — PENDING → PAID, `paymentDate` provisto o `now()` (mismo patrón ya corregido en `Invoice.pay()` en el Hito 31, aplicado aquí desde el día uno)
- [x] **[GREEN]** `SupplierInvoiceService.update()`/`delete()`/`addLineItem()` — mismo guard PENDING-only que `pay()`, no listados explícitamente en el checklist original pero parte del contrato ya expuesto en el Hito 32 (ver nota de revisión abajo)
- [x] **[GREEN]** `@PreAuthorize` — solo ADMIN/MANAGER/ADMINISTRATIVE (ya presente desde el stub del Hito 32, sin cambios)

> **Nota (revisión Hito 33):**
> 1. **Guard PENDING-only extendido a `update()`/`delete()`/`addLineItem()`:** el checklist original de este hito
>    solo mencionaba explícitamente `create()`/`markPaid()`, pero el contrato ya expuesto desde el Hito 32
>    incluye `update()`, `delete()` y `addLineItem()` en el mismo controller. Confirmado con el usuario antes de
>    implementar: se aplica la misma disciplina de guard de estado ya establecida para `Invoice` en el Hito 31
>    (donde `update()`/`delete()` sí tenían guard desde el principio, a diferencia de
>    `MaintenanceService`/`WorkshopScheduleService`, que carecían de él) — `PENDING` es el único estado no
>    terminal aquí, así que `update()`, `delete()` y `addLineItem()` lanzan `ConflictException` (409) fuera de
>    ese estado, con códigos propios de este feature (`SUPPLIER_INVOICE_INVALID_STATE_TRANSITION`,
>    `SUPPLIER_INVOICE_DELETE_NOT_ALLOWED`) en vez de reutilizar los de `Invoice`.
> 2. **Query de filtro doble opcional, parametrizada:** `findAllJoinFetch(vehicleId, category, pageable)` usa el
>    idiom estándar de Spring Data JPA `(:param IS NULL OR campo = :param)` para ambos filtros — verificado
>    contra PostgreSQL real (Testcontainers), no solo compilación: el SQL generado usa `? is null or v.id=?` y
>    `? is null or si.category=?`, totalmente parametrizado, sin concatenación de strings (regla de SQL
>    injection de `CLAUDE.md`). `LEFT JOIN FETCH` (no `JOIN FETCH`) porque `vehicle` es nullable en
>    `SupplierInvoice`, a diferencia del `client` obligatorio de `Invoice`.
> 3. **Redondeo de dinero aplicado desde el día uno:** `addLineItem()` usa
>    `quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)` desde la primera implementación — la
>    lección del Hito 31 (bug de redondeo detectado y corregido en `Invoice.addLineItem()`) se aplicó aquí sin
>    necesidad de un fix de seguimiento. Test de regresión con los mismos valores no triviales
>    (`3 × 10.005 = 30.015 → 30.02`, no el valor crudo de escala 3).
> 4. **Line items no retroalimentan los totales:** confirmado el diseño ya fijado en el Hito 32 —
>    `addLineItem()` nunca recalcula `subtotal`/`taxAmount`/`total` de la factura (no existe un paso equivalente
>    a `issue()` en este feature). Test explícito verifica que los totales de la factura quedan sin cambios
>    tras agregar una línea.
> 5. **Tests finales:** `./mvnw test` 280 → 317 (+21 `SupplierInvoiceServiceTest`, +16
>    `SupplierInvoiceControllerTest`), 0 failures/errors. `./mvnw test -Pfailsafe` 352 (+6
>    `SupplierInvoiceRepositoryTest`), 0 failures/errors.

### Hito 34 — PDF y rentabilidad
- [x] **[RED]** Tests `PdfExportServiceTest` — PDF generado contiene cabecera, líneas y totales correctos; IVA calculado dinámicamente desde `invoice.getTaxRate()`
- [x] **[RED]** Tests `ProfitabilityRepositoryTest` (`@DataJpaTest` + Testcontainers) — proyección devuelve ingresos, costes (mantenimiento + facturas de proveedor) y margen correctos por vehículo
- [x] **[GREEN]** `PdfExportService` — generar PDF con OpenPDF (cabecera, líneas, totales, IVA)
- [x] **[GREEN]** `GET /api/v1/invoices/{id}/pdf` — `Content-Disposition: attachment; filename="INV-...pdf"`
- [x] **[GREEN]** `ProfitabilityRepository` — `@Query` projection: ingresos (`SUM` line items), costes (`SUM MaintenanceRecord.cost` + `SUM SupplierInvoice.total` por vehículo), margen por vehículo
- [x] **[GREEN]** `GET /api/v1/reports/profitability` — paginado, ADMIN/MANAGER/ADMINISTRATIVE

> **Nota (revisión Hito 34 — PDF):** Slice parcial — solo la parte de exportación PDF; `ProfitabilityRepository`/`GET /api/v1/reports/profitability` quedan pendientes como trabajo separado.
> 1. **API de OpenPDF (primer uso en el codebase):** `com.lowagie.text.Document` + `PdfWriter.getInstance(document, outputStream)` para el documento; `Paragraph`/`Chunk.NEWLINE` para cabecera y totales; `PdfPTable`/`PdfPCell`/`Phrase` para la tabla de líneas. Lectura de vuelta para tests con `PdfReader` + `com.lowagie.text.pdf.parser.PdfTextExtractor.getTextFromPage(int)`.
> 2. **IVA nunca hardcodeado:** `PdfExportService.formatTaxRate()` lee siempre `invoice.getTaxRate()` (fracción, ej. `0.2100`) y la formatea como porcentaje (`"21.00%"`). Test `generateInvoicePdf_formatsNonDefaultTaxRate_insteadOfHardcoding21Percent` construye una factura con `taxRate = 0.10` y verifica que el PDF muestre `"10.00%"` (y explícitamente que NO contenga `"21.00%"`) — este es el test que detectaría una regresión a IVA fijo.
> 3. **Sin test de controller nuevo:** no se agregó un test `@WebMvcTest` dedicado al endpoint `GET /{id}/pdf` — el método del controller es una delegación fina (dos llamadas: `invoiceService.getById` para el número de factura, `pdfExportService.generateInvoicePdf` para los bytes) ya cubierta por `PdfExportServiceTest`. Sí fue necesario añadir `@MockBean PdfExportService` al `InvoiceControllerTest` existente para que el contexto `@WebMvcTest` siga arrancando con el nuevo parámetro de constructor.
> 4. **Tests:** 317 → 321 (4 nuevos), todos en verde.
> 5. **Copy en español, detectado por el usuario tras la primera implementación:** las etiquetas del PDF
>    (`Invoice`, `Client:`, `Issue date:`, `Description`, `Tax rate:`, etc.) habían salido en inglés porque
>    `PdfExportService` vive en el backend, fuera del alcance literal de la regla "UI copy en español" de
>    `CLAUDE.md` (que menciona explícitamente `apps/web/`). Pero este PDF es el documento más de cara al
>    cliente de toda la app — la factura real que recibe —, así que se corrigió a español: "Factura",
>    "Cliente:", "Fecha de emisión:", "Fecha de vencimiento:", "Descripción"/"Cantidad"/"Precio unitario"
>    (tabla de líneas), "IVA:" (antes "Tax rate:"), "Importe IVA:" (antes "Tax amount:"), "Total:" sin cambio.
>    Nuevo test `generateInvoicePdf_usesSpanishLabels` (TDD: RED confirmado con las etiquetas viejas en inglés,
>    luego GREEN) verifica las etiquetas nuevas vía el mismo `PdfTextExtractor` ya usado por los demás tests —
>    los acentos (á/é/í/ó/ú/ñ) se conservan correctamente en la extracción, sin necesidad de configurar una
>    fuente/encoding distinta a la Helvetica por defecto de OpenPDF. Suite final: 321 → 322.
>
> **Nota (revisión Hito 34 — rentabilidad, segunda mitad):**
> 1. **Corrección de permisos:** el checklist de este hito dice "solo ADMIN/MANAGER" para
>    `GET /api/v1/reports/profitability`, pero la Matriz de Permisos (fila "Informes de rentabilidad")
>    marca ✅ para ADMIN, MANAGER **y ADMINISTRATIVE** — el mismo trío usado en
>    `InvoiceService.ROLES`/`SupplierInvoiceService.ROLES`/`PdfExportService.ROLES` en todo el resto de
>    `billing`. Se usó el trío (`hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')`), no la frase del
>    checklist — la Matriz es la fuente de verdad de permisos de este proyecto, el checklist es texto
>    heredado del plan pre-Gentle-AI y ya se sabía impreciso (mismo tipo de inconsistencia corregida antes
>    para el naming `InvoiceService`/`BillingService`).
> 2. **Por qué native query y no JPQL — verificado, no asumido:** `MaintenanceRecord.vehicle` y
>    `SupplierInvoice.vehicle` SÍ son relaciones `@ManyToOne` directas a `Vehicle`, y `InvoiceLineItem`
>    llega a `Vehicle` en dos saltos vía `linkedJob.vehicle` (`Job.vehicle` también es `@ManyToOne`
>    directa). Es decir, cada suma individual (ingresos, mantenimiento, facturas de proveedor) sí es
>    expresable en JPQL por separado. Lo que JPQL/HQL no permite es combinarlas en una sola fila por
>    vehículo sin duplicación: la especificación JPA no permite subconsultas correlacionadas en la
>    cláusula `SELECT`, así que un único `SELECT` con tres `JOIN` (jobs, maintenance_records,
>    supplier_invoices) hacia el mismo vehículo produce el clásico problema de "fan-out" — cada
>    combinación cruzada de filas de las tres colecciones multiplica el `SUM`, dando totales incorrectos.
>    La alternativa sin fan-out en JPQL puro exigiría tres queries por separado más un merge en memoria
>    por vehículo, lo cual complica la paginación sobre `vehicles`. Por eso `ProfitabilityRepository` usa
>    una query nativa está con tres subconsultas correlacionadas (`COALESCE((SELECT SUM(...) ... WHERE
>    x.vehicle_id = v.id), 0)`), sin concatenación de ningún valor provisto por el llamador — la única
>    parte dinámica es el `Pageable` estándar de Spring Data (LIMIT/OFFSET con bind parameters), por lo
>    que no viola la regla de "no native queries dinámicas" de `CLAUDE.md` (esa regla apunta a
>    concatenación de strings, no a native queries per se).
> 3. **Regla "solo Jobs" para ingresos:** el modelo de dominio dice explícitamente "vinculados a Jobs de
>    ese vehículo" — `InvoiceLineItem.linkedMaintenance` existe pero NO cuenta para ingresos, aunque
>    ambos campos (`linkedJob`, `linkedMaintenance`) conviven en la misma entidad. Test dedicado
>    `findProfitabilityByVehicle_excludesLineItemsLinkedOnlyViaMaintenance_notJob` prueba esto
>    explícitamente: un line item de 999.00 vinculado solo por `linkedMaintenance` no aparece en
>    `revenue` (queda en 0), mientras que el coste de mantenimiento del mismo vehículo sí se contabiliza
>    en `costs` — evita que la regla dependa solo de que nunca se ejercite el otro camino.
> 4. **Sin test de controller nuevo:** igual que con `PdfExportService`, `ProfitabilityController` es una
>    delegación fina de una sola línea (`profitabilityService.list(pageable)`), ya cubierta indirectamente
>    por `ProfitabilityServiceTest`. No se agregó un `@WebMvcTest` dedicado.
> 5. **Tests:** unitarios 322 → 325 (+3 `ProfitabilityServiceTest`); suite completa con Testcontainers
>    (`-Pfailsafe`) 363 en verde, incluyendo 3 nuevos `ProfitabilityRepositoryTest`.
> 6. **Bug post-merge (PR #45, no detectado por code review): facturas de proveedor multi-vehículo
>    invisibles para el reporte.** `ProfitabilityRepository.findProfitabilityByVehicle` solo leía
>    `SupplierInvoice.vehicle_id` (columna propia de la cabecera de la factura) para sumar costes.
>    Esto ignoraba por completo `SupplierInvoiceLineItem.vehicle_id` — una asociación por-línea,
>    nullable, separada de la de la cabecera. Detectado no por revisión de código sino razonando en
>    conversación con el usuario sobre el propósito real de esa feature: una factura de proveedor
>    puede legítimamente cubrir VARIOS vehículos a la vez (ej. una factura mensual de combustible para
>    toda la flota) — en ese caso la factura no tiene un único `vehicle_id` (queda `NULL`, porque no
>    pertenece a un solo vehículo) y el desglose de costes por vehículo vive en cambio en sus líneas
>    (`SupplierInvoiceLineItem.vehicle_id` por línea, ej. "100L → vehículo 1", "80L → vehículo 2").
>    Antes de esta corrección, una factura compartida contribuía `0` al coste de cada vehículo aunque
>    el dato para atribuirlo correctamente ya existía en `supplier_invoice_line_items`.
>    **Corrección:** se agregó una tercera subconsulta correlacionada a `costs`, sumando
>    `SupplierInvoiceLineItem.subtotal` por vehículo, mediante `JOIN` a `supplier_invoices` y filtrando
>    `si2.vehicle_id IS NULL` — es decir, solo se leen líneas de facturas SIN vehículo propio en la
>    cabecera. Esto garantiza que cada factura se cuente por exactamente un camino: si tiene
>    `vehicle_id` propio, se usa `SupplierInvoice.total` (como antes); si no lo tiene, se usa la suma de
>    sus líneas. Nunca ambos a la vez para la misma factura (evita doble conteo).
>    Test dedicado `findProfitabilityByVehicle_doesNotDoubleCount_singleVehicleInvoiceWithLineItems`
>    construye una factura de un solo vehículo (`vehicle_id` seteado, total 200.00) que ADEMÁS tiene una
>    línea etiquetada al mismo vehículo (subtotal 200.00, igual al total) — si la corrección estuviera
>    mal (sumando ambos caminos), este vehículo mostraría 400.00 en lugar de 200.00; el test confirma
>    200.00. Segundo test nuevo, `findProfitabilityByVehicle_attributesSharedSupplierInvoiceCosts_viaLineItems`,
>    confirmó el bug en rojo contra la query original (esperaba 60.00, la query vieja devolvía 0) antes
>    del fix, y en verde después.
>    **Tests:** unitarios sin cambio (325, no se tocó `ProfitabilityServiceTest`); suite completa con
>    Testcontainers (`-Pfailsafe`) 363 → 365 (+2 nuevos en `ProfitabilityRepositoryTest`), todos en verde.

### Adenda — Limpieza de campos muertos: `MaintenanceRecord.invoice` e `InvoiceLineItem.linkedMaintenance` *(nuevo, sin hito fijo — post-Hito 34, previo al Hito 35)*
> Surgió durante una explicación técnica del reporte de rentabilidad (Hito 34) al usuario: al recorrer
> `ProfitabilityRepository`, el usuario preguntó si un `MaintenanceRecord` (mantenimiento de taller) podía
> realmente enlazarse a una factura de **cliente**. No fue detectado por una revisión de código — fue una
> pregunta de negocio que llevó a rastrear el código y confirmar dos campos sin caso de uso real:
> 1. **`MaintenanceRecord.invoice`** (FK a `Invoice` de cliente, scaffoldeada en V6/V7) — **campo
>    totalmente muerto**: `setInvoice(...)` nunca se invoca en ningún camino de código de producción.
>    Solo era legible (siempre `null`) vía `MaintenanceResponse.invoiceId`.
> 2. **`InvoiceLineItem.linkedMaintenance`** (FK a `MaintenanceRecord`, alcanzable vía el parámetro opcional
>    `linkedMaintenanceId` del endpoint manual `POST /api/v1/invoices/{id}/line-items`) — código alcanzable,
>    pero **sin escenario de negocio real** para esta aplicación: una factura de cliente nunca factura
>    directamente un trabajo de mantenimiento de taller. Confirmado con el usuario que esto no representa
>    un caso de uso de la app.
>
> **Distinción clave con `SupplierInvoiceLineItem.maintenanceRecord`:** esa relación (factura de
> **proveedor** → mantenimiento) es la contraparte legítima y **se dejó intacta** — ahí sí hay un caso de
> negocio real y siempre válido: el taller/proveedor factura a la empresa por un trabajo de mantenimiento
> (lado del coste). La limpieza de esta adenda es exclusivamente sobre el lado de facturación a clientes.
>
> **Migración `V17__drop_dead_invoice_maintenance_links.sql`** (ver árbol de arquitectura y nota de
> numeración en Hito 24): elimina el constraint `fk_maintenance_invoice` y la columna `invoice_id` de
> `maintenance_records`, y la columna `linked_maintenance_id` de `invoice_line_items` (su FK era un
> `REFERENCES` inline sin constraint nombrado propio, así que el `DROP COLUMN` la remueve sin paso adicional).
> La seed de datos pasa de V17 a **V18**.
>
> **Cambios de código:** `MaintenanceRecord` (campo/getter/setter `invoice` fuera), `MaintenanceMapper`
> (mapping `invoiceId` fuera), `MaintenanceResponse` (campo `invoiceId` fuera), `InvoiceLineItem` (campo/
> getter/setter `linkedMaintenance` fuera), `InvoiceMapper` (mappings `linkedMaintenanceId`/`linkedMaintenance`
> fuera), `LineItemRequest`/`LineItemResponse` (campo `linkedMaintenanceId` fuera), `InvoiceService.addLineItem()`
> (helper `resolveLinkedMaintenance` fuera). **`MaintenanceRepository` se eliminó del constructor de
> `InvoiceService`** — tras quitar `resolveLinkedMaintenance`, era su único punto de uso en esa clase
> (verificado: ningún otro método de `InvoiceService` lo usaba), y el proyecto evita dependencias inyectadas
> sin uso. También se descubrió y corrigió un `@Query` de `MaintenanceRepository.findAllJoinFetch` con
> `LEFT JOIN FETCH m.invoice` que los tests unitarios (Mockito, sin contexto Spring) no podían detectar —
> solo lo reveló la suite de integración (`-Pfailsafe`), confirmando por qué ese comando es parte obligatoria
> de la verificación de este tipo de limpieza.
>
> **`ProfitabilityRepositoryTest` — simplificación:** el test `findProfitabilityByVehicle_excludesLineItemsLinkedOnlyViaMaintenance_notJob`
> (que probaba "una línea enlazada solo vía `linkedMaintenance`, no vía `linkedJob`, no cuenta como ingreso")
> ya no tiene sentido con el campo eliminado — la regla de negocio que sigue vigente y debe seguir probada es
> más simple: **una línea de factura con `linkedJob == null` no cuenta como ingreso, punto**. Renombrado a
> `findProfitabilityByVehicle_excludesLineItem_whenLinkedJobIsNull`, y el helper `persistLineItem(...)` perdió
> su parámetro `linkedMaintenance` (ya no hay un segundo camino de enlace que construir).
>
> **Frontend:** `packages/api/src/types.ts` (`MaintenanceRecord.invoiceId` fuera) y `apps/web/src/mocks/handlers.ts`
> (tipo y seeds del mock de mantenimiento, mismo campo fuera) — no existe todavía ningún tipo `LineItemRequest`/
> `LineItemResponse` en el frontend (Hito 35 aún no se implementó), así que no hay nada más que tocar ahí.
>
> **Tests:** backend unitario 325 → **324** (-1, se eliminó `addLineItem_throwsNotFound_whenLinkedMaintenanceMissing`,
> probaba el campo removido); suite completa con Testcontainers (`-Pfailsafe`) 365 → **364**; frontend
> (`vitest run`) sin cambio, **68/68**; `tsc -b` limpio en `packages/api` y `apps/web`. Todo verde.

### Adenda — `InvoiceResponse.lineItems` *(nuevo, sin hito fijo — prerequisito de Hito 35)*
> Al planificar Hito 35 (frontend Billing) surgió que no existía ninguna forma de leer las líneas ya
> creadas de una factura: `InvoiceResponse` deliberadamente no incluía una lista anidada de line items
> (decisión documentada en la nota de revisión de Hito 30, punto 2) y el único endpoint de line items es
> `POST /{id}/line-items` (agregar una), no una lectura. El frontend necesita renderizar un `LineItemList`
> para una factura existente, así que se confirmó con el usuario agregar `lineItems` a `InvoiceResponse`.
>
> **Riesgo N+1 (`InvoiceResponse` se usa tanto en `list()` paginado como en `getById()`):** `getById()`
> resuelve las líneas con el `lineItemRepository.findAllByInvoiceId(id)` ya existente (una sola factura, una
> consulta extra, sin problema). `list()` es el caso sensible: se agregó `LineItemRepository.findAllByInvoiceIdIn(List<UUID>)`
> (derived query, sin `@Query`) para traer las líneas de **toda la página en una sola consulta**, agrupadas en
> memoria por `invoice.getId()` (`Collectors.groupingBy`) — 2 consultas totales para la página completa, no
> 1+N. Un invoice sin líneas recibe lista vacía (`getOrDefault(id, List.of())`), nunca `null`.
>
> **Ensamblado fuera de MapStruct:** `InvoiceMapper.toResponse(Invoice)` no puede mapear `lineItems` (no es
> una propiedad de `Invoice`) — se declaró `@Mapping(target = "lineItems", ignore = true)` explícito (regla
> de `CLAUDE.md`: nunca dejar un campo sin mapear silenciosamente) y un helper privado en `InvoiceService`
> (`toResponseWithLineItems`) reconstruye el record final copiando los campos del mapeo base más la lista de
> `LineItemResponse` ya resuelta por el caller.
>
> **Tests:** se agregó un regression test que verifica con Mockito que `findAllByInvoiceIdIn` se invoca
> **exactamente una vez** por página (no una vez por factura), fijando la ausencia de N+1 como contrato, no
> solo como detalle de implementación. Backend unitario 324 → **329** (+5); suite completa con Testcontainers
> (`-Pfailsafe`) 364 → **369** (+5). Todo verde.
>
> **Bug real encontrado en revisión antes de comitear:** la primera versión solo enrutó `list()`/`getById()`
> a través de `toResponseWithLineItems` — `create()`, `update()`, `issue()` y `pay()` seguían llamando a
> `invoiceMapper.toResponse(...)` directo, así que devolvían `lineItems: null` incluso cuando la factura
> **ya tenía líneas** (`issue()`/`pay()` solo pueden ejecutarse sobre una factura con al menos una línea, por
> la propia guarda `INVOICE_NO_LINE_ITEMS`). Corregido: los cuatro métodos ahora pasan por
> `toResponseWithLineItems` — `create()` con `List.of()` directo (una factura recién creada no puede tener
> líneas todavía, se evita la consulta), `update()`/`pay()` con `lineItemRepository.findAllByInvoiceId(id)`,
> `issue()` reutilizando las líneas que ya había cargado para el cálculo del IVA (sin consulta extra). Ajustado
> también el test `issue_computesSubtotalTaxAndTotal_whenAtLeastOneLineItem`, que no mockeaba
> `invoiceMapper.toResponse(lineItem)` y por eso el resultado real traía `lineItems=[null, null]` en vez de
> fallar en silencio con una lista vacía — el mock de MapStruct para tipos no stubbeados no lanza excepción,
> solo devuelve `null` por entrada. Suite final sin cambio de conteo (329/369), mismos tests corregidos, no
> agregados.

### Hito 35 — Frontend: Billing
> Requiere: Hitos 30–33 (backend facturación a clientes + facturas de proveedor)
- [x] **[RED]** Handlers MSW — `GET /api/v1/invoices`, `POST`, `PATCH /{id}/issue`, `PATCH /{id}/pay`, `GET /{id}/pdf`, `POST /{id}/line-items` *(cliente)*
- [x] **[RED]** Handlers MSW — `GET /api/v1/supplier-invoices` (filtrable por `vehicleId`/`category`), `POST`, `GET /{id}`, `PUT /{id}`, `PATCH /{id}/pay`
- [x] **[RED]** Tests `Billing.test.tsx` — lista de facturas de cliente renderiza con badge de estado; flujo DRAFT→ISSUED→PAID actualiza UI; botón PDF dispara descarga; 409 sin líneas de factura muestra error
- [x] **[RED]** Tests `Billing.test.tsx` — lista de facturas de proveedor renderiza filtrable por categoría; crea/edita/marca pagada; "Marcar pagada" ausente en `PAID`
- [x] **[GREEN]** `packages/hooks/src/useBilling.ts` — lista paginada, detalle por id, create, update, delete, addLineItem, issue, pay, downloadPdf
- [x] **[GREEN]** `packages/hooks/src/useSupplierInvoices.ts` — lista paginada filtrable, create, update, pay
- [x] **[GREEN]** `apps/web/src/components/billing/` — `InvoiceTable`, `InvoiceStatusBadge`, `InvoiceFormModal`, `LineItemList`, `PdfDownloadButton`, `InvoiceActionButtons`
- [x] **[GREEN]** `apps/web/src/components/billing/` — `SupplierInvoiceTable`, `SupplierInvoiceStatusBadge`, `SupplierInvoiceFormModal`, `SupplierInvoiceActionButtons`
- [x] **[GREEN]** Página `Billing` — sección de facturación a clientes + sección de gastos de proveedor (`/billing`, `MANAGEMENT_ROLES`)

> **Nota (revisión Hito 35 — clientes):** Esta revisión implementó únicamente la porción de
> facturación a **clientes** de Hito 35; `SupplierInvoiceTable`/`SupplierInvoiceFormModal`/
> `useSupplierInvoices.ts` quedan pendientes como trabajo separado y posterior.
> 1. **Descarga de PDF:** primera feature de descarga de archivos en el frontend — no había
>    precedente en el código. Implementado en `useDownloadInvoicePdf` (`packages/hooks/src/useBilling.ts`)
>    como `useMutation` sin invalidación de caché (es un efecto lateral de un solo disparo, no
>    estado de aplicación): `apiClient.get(..., { responseType: 'blob' })` → `URL.createObjectURL`
>    → click sintético en un `<a download>` → `URL.revokeObjectURL`. En jsdom, `URL.createObjectURL`
>    no está implementado — `Billing.test.tsx` lo stubea localmente y espía
>    `HTMLAnchorElement.prototype.click` en vez de intentar validar contenido binario real.
> 2. **Edición permitida sin importar el estado (guard solo en backend):** `InvoiceActionButtons`
>    muestra "Editar" siempre, igual que `MaintenanceTable`/`ScheduleTable` muestran "Editar orden"/
>    "Editar entrada" sin condicionar por estado — el backend devuelve 409 si `update()` se llama
>    fuera de `DRAFT`. Se mantiene el mismo precedente por consistencia; no se inventó un guard de
>    UI que el resto del código no usa.
> 3. **Sin test específico de DRIVER/403:** `ProtectedRoute.test.tsx` ya cubre genéricamente el
>    caso "rol no permitido → 403" (incluyendo un caso concreto con rol `DRIVER`), y `/billing` usa
>    el mismo `ProtectedRoute allowedRoles={MANAGEMENT_ROLES}` que `/clients`. Al ser un guardado a
>    nivel de ruta (DRIVER no puede ni siquiera llegar a `/billing`, no hay acciones parcialmente
>    visibles como en Jobs/Workshop), no se agregó un test redundante en `Billing.test.tsx`.
> 4. **Layout de `InvoiceTable`/`LineItemList`:** sin expandir/colapsar filas — mismo patrón simple
>    que `JobTable`/`MaintenanceTable` (que tampoco lo tienen). Las líneas de factura solo se
>    muestran dentro de `InvoiceFormModal` en modo edición y cuando `status === 'DRAFT'`, ya que el
>    modal ya tiene el objeto `Invoice` completo — evita una vista de detalle separada.
> 5. **Tests:** 68 → 75 (7 tests nuevos en `Billing.test.tsx`). `tsc -b` limpio en
>    `packages/api`/`packages/hooks`/`apps/web`. `oxlint` sin warnings nuevos (solo el warning
>    preexistente documentado de `AssignmentModal.tsx`).
> 6. **Bug de UX detectado por el usuario tras la primera implementación:** el campo de IVA mostraba
>    y enviaba la fracción cruda (`0.21`) en vez del porcentaje entero (`21`) que un usuario esperaría
>    escribir/leer. Corregido en `InvoiceFormModal.tsx` con dos helpers de conversión
>    (`fractionToPercentageDisplay`/`percentageInputToFraction`), redondeando por un paso entero
>    intermedio (`Math.round(x * 10000) / 100`, no una división/multiplicación directa) para evitar
>    el drift de punto flotante de JS (`0.21 * 100 !== 21` exactamente). Se agregó un `%` visible junto
>    al input. Test de regresión (`shows the tax rate as a whole percentage and submits it as a
>    fraction`): crea una factura con `IVA=10`, reabre el formulario y confirma que el campo muestra
>    `10` (no `0.1`), agrega una línea de 100.00 y emite — si el `10` se hubiera enviado como fracción
>    cruda al backend, el total resultante sería `1100.00` (100 + 1000%) en vez de `110.00` (100 + 10%).
>    Suite final: 75 → 76.
> 7. **Fallo real de CI, mal diagnosticado en el primer intento:** el test de descarga de PDF fallaba en CI
>    (`click` llamado 0 veces) pero pasaba siempre en local. Primer diagnóstico (incorrecto, sin evidencia):
>    "CI es más lento, el `waitFor` necesita más timeout" — se subió a 5000ms sin reproducir el problema
>    real. El log de CI mostró el verdadero error: `TypeError: object.stream is not a function` dentro de
>    `extractBody`/`Response` de undici, al construir MSW un `Response` interno desde un `Blob` de jsdom
>    (que carece de `.stream()`) para servir una petición XHR con `responseType: 'blob'`. Reproducido de
>    forma determinística corriendo la suite dentro de un contenedor Docker con **Node 22** real (la versión
>    exacta de CI — local usa Node 24, que tolera esta combinación jsdom/undici sin fallar, de ahí que nunca
>    se viera en desarrollo). Cambiar el `Blob` del handler mock por un `string` (primer intento de fix) NO
>    resolvió nada — se confirmó reproduciendo el mismo error con ese cambio ya aplicado dentro del mismo
>    contenedor, porque el `Blob` problemático no lo construye el handler: lo construye jsdom internamente al
>    resolver `xhr.response` para `responseType: 'blob'`, antes de que MSW lo pase a undici. **Fix real:** en
>    `apps/web/src/test/setup.ts`, forzar `globalThis.Blob` al `Blob` nativo de `node:buffer` (el mismo que
>    usa undici internamente) antes de que arranque el servidor MSW, sustituyendo el polyfill incompleto de
>    jsdom. Revertidos ambos intentos anteriores (el `Blob`→`string` del mock y el timeout de 5000ms) al
>    quedar sin justificación una vez corregida la causa real. Verificado corriendo la suite completa dentro
>    del mismo contenedor Node 22 (76/76, sin "unhandled rejection") y en local Node 24 (76/76). El import de
>    `node:buffer` necesitó `@ts-expect-error` — este proyecto de frontend no tiene `@types/node` y no se
>    justifica agregarlo solo por este import de test.
>    **Incidente colateral:** un primer intento de reproducir esto en Docker con bind-mount directo al
>    repositorio (`-v` al path real de Windows) dejó `npm ci` a medio correr dentro del contenedor, que
>    alcanzó a borrar paquetes del `node_modules` local antes de fallar con un error de E/S — reparado con
>    `taskkill` de procesos `node.exe` colgados que tenían binarios nativos bloqueados, seguido de `npm ci`
>    limpio. Las corridas de reproducción posteriores se hicieron copiando el repo al propio filesystem del
>    contenedor (`tar` + `docker cp`), no vía bind-mount, para no repetir el riesgo.
> 8. **Bug real reportado por el usuario: los PDF descargados no abrían.** Investigado metódicamente antes
>    de asumir la causa: (a) se generó un PDF real vía `PdfExportService` (test temporal, luego eliminado) y
>    se validó con `file`/`pdftotext` (poppler, implementación de PDF completamente independiente de
>    OpenPDF) — el archivo es un PDF 1.5 válido y legible; (b) se hizo pasar esos mismos bytes por un test
>    `@WebMvcTest` real de `InvoiceController` (no un mock del servicio, la serialización HTTP real de
>    Spring) y se confirmó igualdad byte a byte antes/después — descarta corrupción en el backend o en el
>    transporte HTTP. La causa real estaba en el frontend: `useDownloadInvoicePdf` llamaba a
>    `window.URL.revokeObjectURL(url)` en el mismo tick que `link.click()` — una condición de carrera
>    documentada (Firefox bug 1282407, Chromium issue 41380177): el navegador puede no haber empezado a leer
>    el blob para la descarga real antes de que la URL se invalide, produciendo un archivo vacío o
>    corrupto. Corregido diferiendo la revocación con `setTimeout(() => window.URL.revokeObjectURL(url), 0)`.
>    Test ajustado para esperar (`waitFor`) la revocación en vez de asumirla síncrona. Suite sin cambio de
>    conteo (76/76), mismo test corregido, no agregado. Efecto colateral menor detectado en el camino (no
>    corregido, no era la causa raíz): el texto con acentos del PDF (`Descripción`, `emisión`) se extrae mal
>    con `pdftotext` (probablemente falta de mapeo `ToUnicode` en la fuente Helvetica/WinAnsi por defecto de
>    OpenPDF) — no impide abrir el archivo, pero sí afecta copiar/pegar o buscar texto; queda como deuda
>    documentada para revisar si se retoma el feature de PDF.
> 9. **El fix del punto 8 no resolvió el reporte del usuario — causa real distinta.** El usuario seguía
>    viendo "Error al cargar el documento PDF" en Chrome y Acrobat Reader después del fix de
>    `revokeObjectURL`. Se descartó el backend por segunda vez, ahora con más rigor: un test
>    `@DataJpaTest` + Testcontainers nuevo (`PdfExportServiceRealDataDebugTest`, temporal, eliminado tras
>    confirmar) persistió un `Client`/`Invoice`/`InvoiceLineItem` reales en Postgres (con `client` lazy,
>    exactamente como en producción) y llamó a `PdfExportService` real — sigue produciendo un PDF válido
>    (`file`/`pdftotext`), descartando también un problema de lazy-loading que el test original con
>    Mockito no podía haber detectado. La causa real: `apps/web/.env.local` tiene
>    `VITE_ENABLE_MSW=true` — el usuario probaba con `npm run dev`, que activa los mocks de MSW (no habla
>    con el backend real). El handler mock de `GET /invoices/:id/pdf` devolvía el string literal
>    `` `%PDF-1.4 mock content for ${invoiceNumber}` `` — **empieza** con la firma de PDF (por eso el test
>    unitario `startsWith("%PDF")` pasaba) pero no tiene objetos, tabla `xref` ni `%%EOF`: no es un PDF real,
>    y cualquier visor real lo rechaza correctamente. Corregido con `buildMinimalPdf()`
>    (`apps/web/src/mocks/handlers.ts`) — construye un PDF mínimo pero estructuralmente válido (objetos,
>    tabla xref con offsets calculados dinámicamente en bytes, trailer, `%%EOF`), verificado también con
>    `file`/`pdftotext` fuera de la suite de tests antes de commitear. Contenido 100% ASCII, por lo que
>    `string.length` de JS es un offset de bytes válido sin necesitar `TextEncoder`. Suite sin cambio de
>    conteo (76/76) — el fix es en el mock, no agrega cobertura nueva.
> 10. **Ajuste de UX pedido por el usuario: el botón de guardado debe quedar al final del modal.** El botón
>     "Guardar cambios"/"Crear factura" estaba dentro del `<form>` principal, antes de la sección "Líneas de
>     factura" (que se renderiza como hermano después de `</form>`) — visualmente el botón quedaba antes de
>     las líneas, no al final. Mover el bloque de líneas dentro del `<form>` principal no era viable:
>     `LineItemList.tsx` renderiza su propio `<form>` para "Agregar línea", y anidar un `<form>` dentro de
>     otro es HTML5 inválido (comportamiento indefinido entre navegadores). Corregido asociando el botón al
>     formulario por `id` en vez de por anidación DOM: `<form id="invoice-form">` y
>     `<Button type="submit" form="invoice-form">` fuera del `<form>`, renderizado después del bloque de
>     líneas — atributo HTML5 estándar para botones de submit ubicados fuera de su `<form>`. Suite sin cambio
>     de conteo (76/76 — ningún test dependía de la posición DOM del botón).
> 11. **Pregunta del usuario: ¿agregar fecha de emisión al formulario?** `issueDate` no es un campo
>     libre: `InvoiceService.issue()` lo fija automáticamente a `LocalDate.now()` al pasar `DRAFT` →
>     `ISSUED` (línea 183), y `InvoiceMapper` lo ignora explícitamente en create/update — permitir
>     editarlo en el modal contradiría esa regla de auditoría (la fecha real de emisión). Se
>     consultó al usuario, que confirmó mostrarlo solo como dato de solo lectura. Agregado en
>     `InvoiceFormModal.tsx`: fila condicional (`invoice?.issueDate != null`) con la fecha en texto
>     plano, sin `<Input>`; no aparece en facturas `DRAFT` (issueDate es `null`) ni al crear una
>     factura nueva. Test nuevo: `shows the issue date as read-only for an ISSUED invoice, and hides
>     it for a DRAFT invoice`. Suite: 76 → 77.
> 12. **Pedido de seguimiento: mostrar también la fecha de emisión en la tabla.** Agregada columna
>     "Emisión" en `InvoiceTable.tsx`, mismo patrón que "Vencimiento" (`invoice.issueDate ?? '—'`,
>     texto plano, sin formateo). Test nuevo (`shows the issue date column in the invoice table...`)
>     verifica el placeholder `—` en la factura `DRAFT` y la fecha real en las facturas `ISSUED`/
>     `PAID`. Este cambio hizo colisionar una aserción del test anterior (nota 11): buscaba
>     `screen.getByText(issued.issueDate!)` de forma global, y ahora esa fecha aparece tanto en la
>     tabla como en el modal — corregido acotando la búsqueda con `within(dialog)`. Suite: 77 → 78.
> 13. **Frontend de facturas de proveedor — CRUD básico (rama `hito35-frontend-supplier-billing`).**
>     Implementado como segunda sección en la misma página `Billing` (no hay ruta ni ítem de nav
>     propio), siguiendo el patrón de dos secciones de `Workshop.tsx`. Antes de implementar se
>     confirmó con el usuario el alcance: `SupplierInvoice` es un modelo distinto al de `Invoice`
>     (clientes) — `subtotal`/`taxAmount`/`total` se ingresan **directamente** (como figuran en la
>     factura física del proveedor), no se calculan a partir de líneas; las líneas (`addLineItem`)
>     son un mecanismo de reparto de costo entre vehículos usado solo por `ProfitabilityRepository`
>     cuando la factura no tiene `vehicleId` propio, y `SupplierInvoiceResponse` ni siquiera expone
>     `lineItems` en la respuesta. Se decidió implementar solo el CRUD básico (listar/filtrar por
>     vehículo y categoría, crear, editar, marcar pagada) en esta pasada, dejando el reparto de
>     costos por línea —y el fix de backend que expondría `lineItems`, análogo al de
>     `InvoiceResponse` en Hito 35— como seguimiento aparte.
>     - `packages/api/src/types.ts`: `ExpenseCategory`, `SupplierInvoiceStatus`, `SupplierInvoice`,
>       `CreateSupplierInvoiceRequest`/`UpdateSupplierInvoiceRequest` (reutiliza `PayInvoiceRequest`
>       ya existente).
>     - `packages/hooks/src/useSupplierInvoices.ts`: lista paginada filtrable, create, update, pay
>       (sin `delete`/`getById` — no hay botón de eliminar ni vista de detalle en este alcance, para
>       no scaffoldear hooks sin uso).
>     - `apps/web/src/components/billing/`: `SupplierInvoiceTable`, `SupplierInvoiceStatusBadge`
>       (`PENDING`→"Pendiente", `PAID`→"Pagada"), `SupplierInvoiceFormModal` (proveedor, nº factura,
>       categoría, vehículo opcional, fecha de factura, vencimiento, subtotal/IVA/total como montos
>       directos — no reutiliza el patrón de IVA-porcentaje del formulario de clientes),
>       `SupplierInvoiceActionButtons` ("Editar" siempre visible + "Marcar pagada" solo en
>       `PENDING`, sin botón de descarga de PDF — no existe ese endpoint para proveedores). Mapa
>       `EXPENSE_CATEGORY_LABEL` centralizado en `supplier-invoice-shared.ts` (se usa en 3 lugares —
>       tabla, formulario, filtro — a diferencia de otros enums de 2 valores que sí se duplican por
>       archivo en este código).
>     - Filtro de categoría: `<select>` nativo simple en `Billing.tsx`, no un componente dedicado
>       (una sola lista desplegable no justifica el patrón de `ScheduleRangeSelector`).
>     - Implementado por un sub-agente con un brief detallado (contrato de backend, límites de
>       alcance, convenciones exactas a espejar) y verificado con un segundo sub-agente de revisión
>       en contexto fresco antes de commitear — sin hallazgos, incluyendo verificación independiente
>       de `tsc -b`/`vitest run`/`oxlint` y del contrato de backend campo por campo.
>     - Suite: 78 → 85 (+7 tests de proveedor). 3 tests de clientes preexistentes necesitaron
>       acotar su alcance (`within(row)`/`within(dialog)`/regex anclada) porque la segunda sección
>       en la misma página introdujo colisiones de texto ambiguo (mismo patrón de bug ya visto en
>       la nota 12) — no se debilitó ninguna aserción, solo se acotó su búsqueda.

### Hito 36 — Proveedores: Entidad maestra (Supplier) *(nuevo — sin hito asignado en el plan original)*
> Requiere: Hito 35 (frontend de facturación)
- [x] **[GREEN]** Nueva entidad `Supplier` (paquete propio `com.fleetmgm.supplier`, dato maestro cross-cutting como `client` — no anidado bajo `billing`): `name` (obligatorio), `taxId`/NIF (opcional, único cuando está presente vía índice parcial — mismo patrón que `Vehicle.licensePlate`), `email`, `phone`, `address`, soft-delete estándar
- [x] **[GREEN]** Migración `V19__create_suppliers.sql` — crea `suppliers`; backfill de un `Supplier` por cada `supplier_name` distinto ya usado en `supplier_invoices`; añade `supplier_invoices.supplier_id` (FK NOT NULL) y elimina la columna de texto libre `supplier_name`
- [x] **[GREEN]** `SupplierInvoice.supplierName: String` reemplazado por `supplier: Supplier` (`@ManyToOne` obligatorio) — cambio de contrato en `SupplierInvoiceResponse`/`Create`/`UpdateSupplierInvoiceRequest` (ahora `supplierId`)
- [x] **[GREEN]** CRUD completo backend+frontend (`/api/v1/suppliers`, página `/suppliers`, `SupplierTable`/`SupplierFormModal`/`SupplierDeleteButton`)
- [x] **[GREEN]** Navegación: el ítem "Proveedores" existente (→ `/supplier-invoices`) se renombra a "Gastos de proveedor"; nuevo ítem "Proveedores" (→ `/suppliers`) — mismo patrón que "Clientes" vs "Facturación"
  > **Nota:** motivado por poder filtrar/reportar gasto por proveedor más adelante (NIF, teléfono, dirección) y evitar
  > duplicados de nombre por error tipográfico en las facturas de proveedor. Se valoró y descartó con el usuario una
  > alternativa más ligera (autocompletado de `supplierName` sin entidad nueva) por no cubrir NIF/teléfono ni permitir
  > reporting real por proveedor. Rama `hito36-supplier-master-entity`, PR #49, mergeada. Suite: backend y frontend
  > en verde (ver PR para el conteo exacto de tests).

### Hito 37 — Facturas de proveedor: Líneas por vehículo *(nuevo — sin hito asignado en el plan original)*
> Requiere: Hito 33 (backend facturas de proveedor), Hito 36 (entidad Supplier)
- [x] **[GREEN]** `SupplierInvoiceLineItem` (ya existía en el esquema desde V7 sin superficie API/UI) expuesto vía `SupplierInvoiceResponse.lineItems`, mismo patrón sin N+1 que ya usa `InvoiceService` (batch fetch en `list()`, fetch único en `getById()`)
- [x] **[GREEN]** Caso de uso: una factura compartida (p. ej. factura mensual de combustible de varios vehículos) se reparte en una línea por matrícula — cantidad (consumo del mes) + coste total — en vez de fijar la factura entera a un solo vehículo por cabecera
- [x] **[GREEN]** Vehículo de cabecera y líneas por vehículo mutuamente excluyentes (409 `SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT`) — coincide con una invariante que `ProfitabilityRepository` ya asumía (`si2.vehicle_id IS NULL`) pero que nada forzaba hasta ahora
- [x] **[GREEN]** Pagar (`PENDING → PAID`) una factura con reparto por líneas exige `Σ líneas.subtotal == cabecera.subtotal` (409 `SUPPLIER_INVOICE_ALLOCATION_INCOMPLETE` si no cuadra)
- [x] **[GREEN]** CRUD completo de línea (crear/editar/borrar) restringido a facturas `PENDING`; 404 `SUPPLIER_LINE_ITEM_NOT_FOUND` si la línea no pertenece a la factura del path (defensa IDOR)
- [x] **[GREEN]** Facturas `PAID` pasan a ser de solo consulta en el frontend (campos deshabilitados, botón "Ver" en vez de "Editar")
- [x] **[GREEN]** Subtotal/IVA/Total del formulario se autocalculan entre sí (editar Subtotal o IVA recalcula Total; editar Total recalcula Subtotal)
  > **Nota:** el campo de línea pide **cantidad + coste total**, no cantidad + precio unitario — decisión tomada con
  > el usuario: en la práctica (anexos de tarjetas de combustible tipo Solred/CEPSA/DKV) se conoce el consumo y el
  > importe total del mes, no un precio unitario único (varía a diario); el "precio medio" se deriva en servidor
  > (coste ÷ cantidad) solo a título informativo. Rama `hito37-supplier-invoice-line-items`, PR #50. Suite: backend
  > 375 tests, frontend 97 tests, ambos en verde; CI en success sobre el commit de merge con `main`.

> **Nota:** los Hitos 36–37 (entidad `Supplier` + líneas de factura de proveedor) no estaban en el plan original;
> se añadieron tras revisión con el usuario, igual que ocurrió con las facturas de proveedor (Hitos 32–33). GPS y
> los hitos siguientes se renumeraron +2 (antiguo Hito 36 → nuevo Hito 38, etc.) para hacer sitio.

---

### Hito 38 — GPS: Contrato API
- [x] `Flyway V8` — tabla `gps_positions` con índices en `vehicle_id` y `recorded_at` *(ya aplicada)*
- [x] `GpsPosition` entity — lat, lng, heading, speed, `source` (MOCK/DEVICE) *(ya scaffoldeada)*
- [x] `GpsPositionResponse` (record) — denormaliza `vehicleId`/`licensePlate` del vehículo (mismo patrón que `ScheduleResponse`), evita que el frontend tenga que resolverlos aparte
- [x] `GpsController` — `GET /api/v1/gps/latest`
- [x] `GpsMapper` (MapStruct) — `toResponse`
- [x] `GpsService` — stub, `findLatest()` lanza `UnsupportedOperationException("Pending Hito 39")`, mismo precedente contrato-hito que `InvoiceService`/`SupplierInvoiceService` (no listado en el checklist original)
  > **Nota (revisión Hito 38):** sin `@PreAuthorize` en el stub — a diferencia de Billing (bloqueo total de rol),
  > aquí todos los roles autenticados llaman al mismo endpoint y es DRIVER quien recibe una respuesta filtrada
  > a su propio vehículo; ese filtrado es lógica de negocio real (depende de datos, no solo del rol) y queda
  > para el Hito 39, igual que el resto del contrato. Contrato-only: sin tests nuevos (mismo criterio que
  > Hito 32); `./mvnw test` sigue en verde, sin regresión.

### Hito 39 — GPS: Lógica e implementación
- [x] **[RED→GREEN]** Tests `GpsRepositoryTest` (`@DataJpaTest` + Testcontainers) — `findFirstByVehicleIdOrderByRecordedAtDesc` devuelve la posición más reciente (y vacío si no hay ninguna); `findLatestForAllActiveVehicles` excluye vehículos INACTIVE y devuelve solo la última posición por vehículo (no el historial completo)
- [x] **[RED→GREEN]** Tests `GpsMockSchedulerTest` (JUnit 5 + Mockito, sin contexto Spring) — genera exactamente una posición por vehículo ACTIVE devuelto por el repositorio; sin vehículos ACTIVE no llama a `save()`; coordenadas dentro del spread inicial alrededor de la base (Madrid) cuando el vehículo no tiene posición previa; dentro del rango de deriva respecto a la posición previa cuando sí la tiene
- [x] **[RED→GREEN]** Tests `GpsServiceTest` (JUnit 5 + Mockito, sin contexto Spring) — `findLatest()` devuelve `findLatestForAllActiveVehicles()` mapeado (vacío si ningún vehículo activo registró posición)
- [x] **[GREEN]** Tests `GpsControllerTest` (`@WebMvcTest`) — 200 con posiciones, 200 con lista vacía; el 403 por rol **no** se prueba aquí — mismo gap ya documentado en `SupplierInvoiceControllerTest` (Hito 33): `@AutoConfigureMockMvc(addFilters = false)` + `GpsService` mockeado no atraviesa el proxy AOP de `@PreAuthorize`. Esa lógica de filtrado por rol está cubierta por `GpsServiceTest`.
- [x] **[GREEN]** `GpsRepository` — `findFirstByVehicleIdOrderByRecordedAtDesc` (`@EntityGraph` sobre `vehicle`, evita N+1); `findLatestForAllActiveVehicles` (subquery correlacionada `MAX(recordedAt)` por vehículo — JPQL no soporta `ROW_NUMBER()/PARTITION BY`, y con el volumen que genera un scheduler mock esto es suficientemente eficiente sin recurrir a `nativeQuery`)
- [x] **[GREEN]** `VehicleRepository.findAllByStatus(VehicleStatus)` — no existía ningún filtro por estado; añadido para que el scheduler encuentre los vehículos ACTIVE (no confundir con `findAllActiveWithAssignment`, que pese al nombre no filtra por `status` — ver comentario ya existente en el propio repositorio)
- [x] **[GREEN]** `GpsMockScheduler` — `@Scheduled(fixedDelay = 30_000)` (ya habilitado vía `@EnableScheduling` en `FleetMgmApplication`), genera una posición por vehículo ACTIVE con deriva aleatoria respecto a la última conocida (o un punto base aleatorio si no hay historial) — la deriva evita que el vehículo "teletransporte" en el mapa
- [x] **[GREEN]** `@PreAuthorize` en `GpsService.findLatest()` — `hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')` (WORKSHOP_STAFF y DRIVER fuera de la lista → 403)
  > **Nota (revisión Hito 39):** la primera versión de este hito incluía a DRIVER en el `@PreAuthorize` y una
  > rama en `GpsService` que le devolvía solo la posición de su propio vehículo (reutilizando
  > `AssignmentRepository.findActiveByDriverEmail`, mismo mecanismo de `VehicleService`). Se revirtió por decisión
  > del usuario al planificar el Hito 40: un DRIVER ya sabe físicamente dónde está su vehículo — mostrárselo en un
  > mapa no aporta nada, a diferencia de ADMIN/MANAGER/ADMINISTRATIVE que sí necesitan ver toda la flota. `GpsService`
  > vuelve a depender solo de `GpsRepository`/`GpsMapper` (se retira la dependencia de `AssignmentRepository`).
  > Suite completa (`./mvnw test -Pfailsafe`): 431 tests, 0 failures/errors — sin regresión.

> **Addendum (planificando Hito 40):** `GpsPositionResponse` ganó el campo `vehicleCategory` (denormalizado desde
> `vehicle.vehicleCategory`, mismo patrón que `licensePlate`) — decisión con el usuario para poder pintar un icono
> de marcador distinto por categoría (`LIGHT_VEHICLE`/`HEAVY_VEHICLE`/`HEAVY_MACHINERY`) en el mapa del Hito 40 sin
> que el frontend tenga que cruzar `vehicleId` contra `useVehicles()` por su cuenta. Cambio de contrato menor sobre
> el Hito 38 ya mergeado — `GpsMapper`/tests actualizados, `./mvnw test -Pfailsafe`: 433 tests, 0 failures/errors.

### Hito 40 — Frontend: GPS Map
> Requiere: Hitos 38–39 (backend GPS)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/gps/latest`
- [ ] **[RED]** Tests `Map.test.tsx` — marcador renderizado por cada vehículo retornado por MSW; popover muestra licensePlate y speed; polling cada 10 s dispara segunda llamada
- [ ] **[GREEN]** `packages/hooks/src/useGps.ts` — polling cada 10 s, invalida caché automáticamente
- [ ] **[GREEN]** `apps/web/src/components/map/` — `FleetMap` (Leaflet + react-leaflet), `VehicleMarker`, `VehiclePopover`
- [ ] **[GREEN]** Página `Map` — mapa Leaflet con marcadores de vehículos activos

---

### Hito 41 — AuditLog viewer
- [x] `Flyway V8` — tabla `audit_logs` *(ya aplicada, misma migración que gps_positions)*
- [x] `AuditLog` entity, `AuditLogRepository` *(stub base ya creado — `JpaRepository` sin queries de filtro)*
- [x] **[RED]** Tests `AuditLogControllerTest` (`@WebMvcTest`) — 200 sin filtros, 200 filtrado por entityType/action/rango de fechas, 400 si `action` es inválido
  > **Nota:** el 403 de ADMINISTRATIVE no se testea en el controller — `@AutoConfigureMockMvc(addFilters = false)` +
  > servicio mockeado no ejecuta el proxy AOP de `@PreAuthorize` (mismo gap documentado en `GpsControllerTest`/
  > `SupplierInvoiceControllerTest`; ningún test del repo verifica hoy un `@PreAuthorize` por esa vía). Se mantiene
  > la anotación en `AuditLogService.list()` sin test dedicado, consistente con el resto de features.
- [x] **[GREEN]** `AuditLogRepository` — `findAllFiltered` con filtros opcionales (entityType, action, rango de fechas) vía idiom `(:param IS NULL OR ...)`, mismo patrón que `SupplierInvoiceRepository`
- [x] **[GREEN]** `AuditLogResponse` (record) + `AuditLogMapper` + `AuditLogController` — `GET /api/v1/audit`, `AuditLogService` con `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`
- [x] **[GREEN]** `GlobalExceptionHandler` — nuevo handler para `MethodArgumentTypeMismatchException` (400), gap descubierto al testear `action` inválido

### Hito 42 — Frontend: AuditLog
> Requiere: Hito 41 (backend audit viewer)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/audit` con filtros entityType, action, rango de fechas
- [ ] **[RED]** Tests `AuditLog.test.tsx` — tabla paginada renderiza; filtros por entityType y action reducen la lista; 403 si rol ADMINISTRATIVE o inferior
- [ ] **[GREEN]** `packages/hooks/src/useAuditLog.ts` — lista paginada con filtros
- [ ] **[GREEN]** `apps/web/src/components/audit/` — `AuditLogTable`, `AuditLogFilters`
- [ ] **[GREEN]** Página `AuditLog` — tabla paginada con filtros (solo ADMIN/MANAGER)

### Hito 43 — Frontend: Dashboard y rentabilidad
> Requiere: Hito 34 (backend profitability endpoint — incluye costes de mantenimiento y de proveedores)
- [ ] **[RED]** Handlers MSW — `GET /api/v1/reports/profitability`
- [ ] **[RED]** Tests `Dashboard.test.tsx` — gráfico Recharts renderiza barras por vehículo; totales de ingresos/costes/margen son correctos; solo ADMIN/MANAGER ven la sección
- [ ] **[GREEN]** `packages/hooks/src/useProfitability.ts` — lista paginada de rentabilidad por vehículo
- [ ] **[GREEN]** `apps/web/src/components/` — `ProfitabilityChart` (Recharts), `ProfitabilitySummary`
- [ ] **[GREEN]** Página `Dashboard` — KPIs de flota + gráfico de rentabilidad

---

### Hito 44 — Tests de integración (`@SpringBootTest` + Testcontainers)
- [ ] `AuthFlowIT` — login correcto → JWT → endpoint protegido; 5 intentos fallidos → cuenta bloqueada → 401
- [ ] `JobLifecycleIT` — crear job → iniciar → completar → verificar `UsageLog` creado y `currentKm` actualizado
- [ ] `InvoiceFlowIT` — crear DRAFT → añadir línea → emitir → pagar → descargar PDF

### Hito 45 — Demo y hardening final
- [ ] `docker-compose.yml` — postgres:16 + backend + apps/web (nginx), health checks, `depends_on`
- [ ] `Flyway V18` — seed datos demo realistas (5 vehículos, 3 conductores, 10 trabajos completados, 3 facturas de cliente, facturas de proveedor de ejemplo)
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
| Entidad `Supplier` | Hito propio (36), separada del dato libre `supplierName` | Evita duplicados por typo y habilita filtrar/reportar gasto por proveedor (NIF, teléfono); se descartó un autocompletado sin entidad nueva por no cubrir esos casos |
| Líneas de factura de proveedor | Hito propio (37); campo cantidad + coste total, no cantidad + precio unitario | Los anexos reales de tarjetas de combustible dan consumo e importe total del mes, no un precio unitario único (varía a diario); el precio medio se deriva en servidor solo a título informativo |
| Orden de hitos: GPS y siguientes | Renumerados +2 (antiguo 36→38, ..., antiguo 43→45) | Hicieron sitio a los Hitos 36–37 (Supplier + líneas de factura de proveedor), añadidos tras el Hito 35 sin estar en el plan original |

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
