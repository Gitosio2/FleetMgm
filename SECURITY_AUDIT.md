# Auditoría de seguridad — FleetMgm

**Fecha:** 2026-07-15
**Alcance:** cumplimiento de los requisitos de seguridad declarados en `planning.md` / `CLAUDE.md` frente al estado real del código (`backend/`, `apps/web/`, `.github/workflows/`), más una búsqueda dirigida de vulnerabilidades (OWASP Top 10, ediciones 2010–2025).
**Metodología:** revisión estática de código (sin ejecución de la app), contraste línea a línea contra el documento de planificación. No se ejecutó ningún escáner dinámico (DAST) ni pentest activo.
**Nota:** este es un informe de solo lectura. Ninguna corrección se aplicó en esta pasada; los hallazgos quedan listados por severidad para una futura sesión de remediación.

---

## Resumen ejecutivo

La aplicación cumple la **gran mayoría** de los controles de seguridad documentados: autenticación JWT con bloqueo de cuenta, RBAC por capa de servicio con comprobaciones de propiedad (defensa IDOR), contraseñas con BCrypt coste 12, cabeceras de seguridad básicas, rate limiting en endpoints de auth, actuator restringido y un pipeline de CI con OWASP Dependency-Check realmente forzando el umbral CVSS ≥ 7. No se encontraron vulnerabilidades de inyección SQL/JPQL, IDOR, XSS vía `dangerouslySetInnerHTML`, deserialización insegura, SSRF ni redirects sin validar.

Sin embargo, existen **varias discrepancias entre lo documentado y lo implementado**: la Content-Security-Policy y el cambio a RS256 en producción están declarados en `CLAUDE.md`/`planning.md` pero no implementados; Semgrep SAST se declara como parte del pipeline de CI pero no está conectado en ningún workflow. Ninguno de estos gaps es una vulnerabilidad explotada activamente, pero representan defensas en profundidad ausentes respecto a lo que el propio proyecto exige.

**Veredicto global: cumplimiento alto, con gaps documentales/de implementación de severidad media que deben cerrarse antes de considerar el hardening "completo".**

---

## 1. Matriz de cumplimiento

| Control | Requisito (CLAUDE.md / planning.md) | Estado real | Evidencia |
|---|---|---|---|
| Algoritmo JWT | HS512 en dev, **RS256 en prod** | ⚠️ Solo HS512, en todos los perfiles. No existe código de manejo de claves RSA en el repo | `backend/.../auth/infrastructure/JwtService.java` (uso fijo de `Jwts.SIG.HS512`); planning.md línea 19 |
| Expiración de tokens | Access 15 min / Refresh 7 días | ✅ Cumple | `application.yml`: `jwt.access-token-expiration-ms=900000`, `jwt.refresh-token-expiration-ms=604800000` |
| Refresh token en BD | Almacenado como hash SHA-256, nunca en claro | ✅ Cumple | `AuthService.sha256(...)`, `RefreshToken.tokenHash` |
| Revocación en logout | Logout borra el registro de `RefreshToken` en BD | ✅ Cumple | `AuthService.logout()` |
| Rotación de refresh token | No exigido explícitamente, pero es buena práctica estándar | ⚠️ No implementada — `/auth/refresh` reemite el mismo token | `AuthService.refresh()` — devuelve `request.refreshToken()` sin reemplazo |
| Bloqueo de cuenta | 5 intentos fallidos → bloqueo 15 min | ✅ Cumple (con corrección histórica) | `AuthService`: `MAX_FAILED_ATTEMPTS=5`, `LOCK_DURATION_SECONDS=900`; nota: un bug real de persistencia (rollback deshacía el contador) fue detectado y corregido en Hito 45 según el propio planning.md — el bloqueo llegó a ser "funcionalmente inexistente" antes del fix |
| Hash de contraseñas | BCrypt coste 12 | ✅ Cumple | `SecurityConfig.java`: `new BCryptPasswordEncoder(12)` |
| RBAC — 5 roles | `@PreAuthorize` en capa `application/`, nunca en controllers | ✅ Cumple, sin excepciones detectadas en 14 servicios revisados | `VehicleService`, `JobService`, `ClientService`, `InvoiceService`, etc. |
| IDOR / ownership | Cada acceso de `DRIVER` a su propio recurso debe verificarse, no solo el rol | ✅ Cumple — comprobación explícita de propiedad además del rol | `JobService.assertDriverOwnsJob()`, `WorkerService.assertDriverOwnsProfile()`, `VehicleService.assertDriverOwnsVehicle()` |
| IDs no enumerables | UUID en todas las entidades, nunca enteros secuenciales | ✅ Cumple | `GenerationType.UUID` en las 16 entidades revisadas |
| CORS | Solo el origen del frontend, nunca `*` | ✅ Cumple | `SecurityConfig.corsConfigurationSource()`: origen único `${FRONTEND_URL}` |
| CSRF | Deshabilitado (API stateless) | ✅ Cumple (coherente con JWT sin cookies) | `SecurityConfig.java` línea 52 |
| Cabeceras HTTP | `X-Content-Type-Options`, `X-Frame-Options`, HSTS (prod) | ✅ Cumple | `SecurityConfig.java` líneas 93-104 |
| **CSP** | `Content-Security-Policy: default-src 'self'; script-src 'self'` | ❌ **No implementado** | Ausente en `SecurityConfig.java` y en `apps/web/index.html`; declarado en CLAUDE.md líneas 42, 461, 473, 557 |
| Actuator | Solo `/health` y `/info` públicos, resto `ADMIN` | ✅ Cumple | `SecurityConfig.java` línea 70; `application.yml` exposure |
| Rate limiting auth | Limitar `/auth/login` y `/auth/refresh` | ✅ Cumple | `RateLimitFilter` (Bucket4j), 10 req/min por IP+endpoint, HTTP 429 |
| Swagger/H2 en prod | Deshabilitados en `prod` | ✅ Cumple (H2 ni siquiera existe como dependencia) | `application.yml` perfil `prod`: `springdoc.swagger-ui.enabled=false` |
| OWASP Dependency-Check | Falla el build si CVSS ≥ 7 | ✅ Cumple, realmente forzado | `backend/pom.xml`: `<failBuildOnCVSS>7</failBuildOnCVSS>` |
| **Semgrep SAST** | Declarado como parte de `ci.yml` | ❌ **No implementado** | Ausente en `.github/workflows/ci.yml` y `security.yml`; no existe config de Semgrep en el repo; declarado en CLAUDE.md línea 618 y planning.md líneas 31, 279 |
| Pinning de GitHub Actions | SHA fijo, no tags mutables | ✅ Cumple | Todos los `uses:` en `ci.yml`/`security.yml` usan SHA completo |
| Manejo de errores | Nunca exponer stack traces / nombres de excepción | ✅ Cumple | `GlobalExceptionHandler.handleGeneral()` devuelve mensaje genérico + `correlationId` |
| Escaneo de dependencias JS | No especificado explícitamente, pero implícito en "F — Vulnerable Components" | ⚠️ Ausente — solo el backend Java tiene control CVE (`dependency-check-maven`) | `ci.yml` job frontend solo corre `npm ci` + build/lint/test, sin `npm audit` |

---

## 2. Hallazgos de vulnerabilidad / gaps (por severidad)

### Media

1. **CSP no implementada** — Ausencia de la cabecera `Content-Security-Policy` en las respuestas del backend y sin `<meta>` equivalente en `apps/web/index.html`. Es una capa de defensa en profundidad contra XSS explícitamente exigida por el propio `CLAUDE.md`. React ya escapa JSX por defecto y no se encontró ningún `dangerouslySetInnerHTML`, por lo que el riesgo residual es bajo, pero la capa declarada falta.
   *Recomendación:* añadir `.headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'")))` en `SecurityConfig.java`.

2. **RS256 nunca implementado en producción** — El stack declara "HS512 en dev, RS256 en prod", pero `JwtService` usa HS512 de forma incondicional en todos los perfiles, sin ninguna ruta de código para pares de claves RSA. Actualmente la seguridad de los tokens en producción depende íntegramente de la confidencialidad de un secreto simétrico compartido (`JWT_SECRET`), en vez de una clave privada nunca expuesta.
   *Recomendación:* decidir si el requisito sigue vigente; si es así, implementar `RSASSA` con `KeyPair` gestionado vía variables de entorno/secret manager en el perfil `prod`. Si se decide mantener HS512 en prod, actualizar la documentación para que no prometa algo que no se entrega.

3. **Semgrep SAST declarado pero ausente del CI** — `CLAUDE.md` y `planning.md` afirman que `ci.yml` ejecuta "tests + OWASP Dependency-Check + Semgrep SAST" en cada PR, pero no existe ningún step de Semgrep ni archivo de configuración (`.semgrep.yml`) en el repositorio. Esto significa que no hay SAST activo — solo el escaneo de dependencias (CVEs conocidas), no de patrones de código inseguro propios.
   *Recomendación:* añadir un job de Semgrep a `ci.yml` (p. ej. `semgrep/semgrep-action` con reglas para Spring Security y React) o corregir la documentación si se decide no adoptarlo.

4. **Sin validación explícita de longitud mínima de `JWT_SECRET` en arranque** — `application.yml` define un fallback hardcodeado (`changeme-changeme-...`, 71 caracteres) si la variable de entorno `JWT_SECRET` no está definida. La única protección contra un secreto débil es la `WeakKeyException` implícita de la librería jjwt (que solo dispara si la clave es criptográficamente insuficiente para HS512, no si es la constante conocida "changeme..."). Si por error de despliegue se omite `JWT_SECRET` en un entorno real, la aplicación arrancará y firmará tokens válidos con un secreto público en el historial de git, permitiendo forjar JWTs arbitrarios.
   *Recomendación:* añadir una comprobación explícita en el arranque (p. ej. `@PostConstruct` o `ApplicationRunner`) que falle si `JWT_SECRET` no fue sobreescrito (comparar contra el valor por defecto) cuando `spring.profiles.active` incluye `prod`.

### Baja

5. **Credenciales hardcodeadas de bajo riesgo en artefactos de desarrollo/demo:**
   - `docker-compose.yml`: contraseña de Postgres en texto plano (`fleetmgm`) — aceptable para un compose local, pero conviene documentar que no debe reutilizarse en ningún entorno accesible.
   - `V20__seed_demo_data.sql`: los 10 usuarios semilla (incluido `admin@fleetmgm.demo`) comparten la contraseña `Demo1234!` (mismo hash BCrypt). Está correctamente aislado tras el perfil Spring `demo` (no se activa por defecto ni en CI), pero el hash y la contraseña son públicos en el historial de git — si el perfil `demo` se activase alguna vez contra una base de datos expuesta a internet, sería una cuenta de administrador con contraseña conocida.
   *Recomendación:* mantener el aislamiento por perfil; considerar generar una contraseña aleatoria por entorno de demo si alguna vez se expone públicamente más allá de una presentación controlada.

6. **Sin `npm audit` (o equivalente) en CI** — El job de frontend en `ci.yml` no incluye ningún escaneo de vulnerabilidades de dependencias JavaScript; solo el backend Java tiene `dependency-check-maven`. Esto deja un punto ciego frente a CVEs en el árbol de dependencias npm.
   *Recomendación:* añadir `npm audit --audit-level=high` (o Dependabot alerts, ya mencionado como intención en `planning.md`) al job de frontend en `ci.yml`.

### Sin hallazgos (categorías revisadas explícitamente, sin problemas)

- **Inyección SQL/JPQL:** todas las `nativeQuery`/JPQL usan parámetros ligados; no hay `ORDER BY` construido por concatenación de strings de usuario.
- **IDOR / Broken Access Control:** comprobaciones de propiedad de recurso presentes en todos los flujos de `DRIVER`; resto de roles con `@PreAuthorize` consistente.
- **XSS:** sin `dangerouslySetInnerHTML` en todo `apps/web/src`.
- **Deserialización insegura:** sin `ObjectInputStream` ni tipado polimórfico de Jackson sobre entrada no confiable.
- **SSRF:** no existe ningún endpoint que reciba una URL de usuario y la resuelva server-side.
- **Redirects no validados:** no hay `redirect_uri`/`next`/`returnUrl` en frontend ni backend.
- **Manejo de errores:** `GlobalExceptionHandler` nunca filtra stack traces ni nombres de excepción; siempre responde con el shape estándar + `correlationId`.
- **Supply chain (Actions):** todos los `uses:` de GitHub Actions están pineados a SHA, no a tags mutables.
- **TODO/FIXME de seguridad:** no se encontraron marcadores de deuda técnica relacionados con auth/validación en el código fuente.

---

## 3. Notas del propio `planning.md` relevantes para esta auditoría

- El documento registra como **hito cerrado** (Hito 45) un bug de producción real: el contador de intentos fallidos de login nunca se persistía por culpa del rollback transaccional por defecto ante `BadCredentialsException`, dejando el bloqueo de cuenta "funcionalmente inexistente" hasta que se detectó vía tests de integración con `@SpringBootTest` (los tests unitarios con Mockito no lo detectaban, al no correr en una transacción real). Ya está corregido (`@Transactional(noRollbackFor = ...)`), pero es un buen recordatorio de que los mocks de servicio no sustituyen pruebas contra una transacción real para lógica que depende de side effects transaccionales.
- El propio plan reconoce como **deuda aceptada** (no un bug) que ningún test de controller (`@WebMvcTest`) ejercita realmente el proxy AOP de Spring Security, por lo que no hay ningún test HTTP end-to-end que verifique un 403 por `@PreAuthorize` — la cobertura de RBAC es solo a nivel de servicio (Mockito) o revisión de código.
- El plan declara explícitamente 3 CVEs de severidad media (CVSS 5.3) aceptados como riesgo residual (por debajo del umbral CVSS ≥ 7 que hace fallar el build): `commons-lang3` 3.17.0, `jackson-databind` 2.22.0, y un `DOMPurify` empaquetado dentro de `swagger-ui` vía springdoc.

---

## 4. Próximos pasos sugeridos (no aplicados en esta pasada)

Por prioridad:
1. Implementar la cabecera CSP en `SecurityConfig.java`.
2. Conectar Semgrep al workflow de CI (o corregir la documentación si se descarta).
3. Añadir una comprobación de arranque contra el `JWT_SECRET` por defecto en perfiles `prod`.
4. Decidir y documentar el estado real de RS256 en producción (implementarlo o retirar la promesa).
5. Añadir rotación de refresh token en `/auth/refresh`.
6. Añadir `npm audit` al pipeline de CI del frontend.
