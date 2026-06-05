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

### Semana 1 (4–10 Jun): Fundación ← EN CURSO
- [x] pom.xml — Java 21, Spring Boot 3.3.5, dependencias completas
- [x] application.yml — datasource, JWT config, actuator, springdoc
- [x] Estructura de paquetes (package-by-feature)
- [x] Entidades Auth: User, RefreshToken, AppRole
- [x] JwtService (HS512), JwtAuthenticationFilter
- [x] Shared: GlobalExceptionHandler, PageResponse, AuditLog
- [ ] SecurityConfig — filter chain, CORS, endpoints públicos
- [ ] AuthService + AuthController (login/refresh/logout)
- [ ] Flyway V1 (users) + V2 (clients) + V3 (vehicles) + V4 (workers)
- [ ] Vehicle entity + CRUD completo con protección por rol
- [ ] Worker entity + CRUD
- [ ] Client entity + CRUD
- [ ] DataInitializer — seed demo users (dev profile)

**Entregable:** Admin puede hacer login, crear vehículos/trabajadores/clientes.

### Semana 2 (11–17 Jun): Asignaciones + Trabajos
- Worker ↔ Vehicle assignment (historial)
- Job lifecycle (PENDING → IN_PROGRESS → COMPLETED)
- Rol DRIVER: ve solo sus trabajos activos
- UsageLog al completar trabajo (JobCompletedEvent)
- Flyway V5 (jobs)

### Semana 3 (18–24 Jun): Taller + Mantenimiento
- MaintenanceRecord + WorkshopSchedule
- Panel WORKSHOP_STAFF: vista hoy/semana/mes
- VehicleEntersWorkshopEvent, MaintenanceCompletedEvent
- Flyway V6

### Semana 4 (25 Jun – 1 Jul): Facturación
- Invoice + InvoiceLineItem
- Flujo DRAFT → ISSUED → PAID
- Export PDF (OpenPDF)
- Dashboard rentabilidad: ingresos vs costes
- Flyway V7

### Semana 5 (2–8 Jul): Mapa GPS + Informes
- GpsPosition + @Scheduled mock
- Mapa Leaflet polling 10s
- Informes visuales con Recharts
- AuditLog viewer (solo ADMIN)
- Flyway V8

### Semana 6 (9–15 Jul): Hardening + Demo
- OWASP Dependency Check → corregir CVEs High+
- docker-compose.yml — un comando levanta todo
- Tests de integración (Testcontainers)
- Flyway V9 seed datos demo
- README con diagrama, credenciales, instrucciones
- GitHub Actions CI/CD

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
