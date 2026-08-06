# Auditoría RGPD / ePrivacy — FleetMgm

**Fecha:** 2026-07-15
**Alcance:** cumplimiento del Reglamento (UE) 2016/679 (RGPD) y de la Directiva ePrivacy (cookies) en `apps/web/`, `packages/`, `backend/` y la documentación del proyecto (`planning.md`, `CLAUDE.md`).
**Metodología:** revisión estática de código y documentación, sin ejecución de la app ni auditoría legal formal. Este informe es un análisis técnico, no un dictamen jurídico.
**Nota:** informe de solo diagnóstico. No se aplicó ninguna corrección de código en esta pasada.

---

## Resumen ejecutivo

FleetMgm es un proyecto de TFM sin ninguna consideración RGPD documentada: cero menciones de "RGPD", "LOPD", "privacidad", "consentimiento" o "DPO" en `planning.md`/`CLAUDE.md`, pese a que el modelo de datos trata información claramente personal (DNI/NIE de trabajadores, datos de clientes, posición GPS de conductores, registros de auditoría).

**Sobre cookies (ePrivacy):** la aplicación **no usa ninguna cookie**, ni propia ni de terceros — la autenticación es JWT vía header `Authorization: Bearer`, sin `Set-Cookie` en ningún endpoint y con CORS `allowCredentials(false)`. El requisito legal de banner de consentimiento de cookies es, en la práctica, **inaplicable** tal como está construida la app hoy. Sin embargo, se detectaron dos problemas relacionados que sí son relevantes en RGPD aunque no sean "cookies": carga de Google Fonts directamente desde el CDN de Google (transferencia de IP a un tercero sin consentimiento) y tokens/email guardados en `localStorage` sin cifrar ni expirar.

**Sobre protección de datos en general:** aquí es donde están los gaps reales. No existe ningún derecho ARCO/RGPD implementado (acceso, rectificación autoservicio, supresión real, portabilidad), no hay ninguna política de retención ni purga de datos (todo se conserva indefinidamente vía soft-delete), no hay página de política de privacidad ni aviso legal, y no hay documentación de base legal para el tratamiento de datos como el DNI/NIE o la geolocalización de los conductores.

**Veredicto global: cumplimiento de ePrivacy/cookies alto por diseño (no hay cookies que gestionar). Cumplimiento RGPD sustantivo bajo — es un proyecto académico que no ha abordado la protección de datos como requisito, más allá de las medidas de seguridad técnica ya cubiertas en `SECURITY_AUDIT.md`.**

---

## 1. Cookies y ePrivacy

| Aspecto | Estado | Evidencia |
|---|---|---|
| Cookies propias | ✅ Ninguna — sin `document.cookie`, sin librería de cookies | Búsqueda exhaustiva en `apps/web/src`, `packages/*/src`: cero resultados |
| Cookies del backend | ✅ Ninguna — sesión `STATELESS`, sin `Set-Cookie` en `/login`/`/refresh`/`/logout` | `SecurityConfig.java` (`SessionCreationPolicy.STATELESS`); `AuthController.java` devuelve solo JSON |
| Cookies cross-origin | ✅ Explícitamente deshabilitadas | CORS: `config.setAllowCredentials(false)` en `SecurityConfig.corsConfigurationSource()` |
| Banner de consentimiento de cookies | N/A — no aplica al no haber cookies | — |
| **Terceros — Google Fonts** | ⚠️ Carga directa desde `fonts.googleapis.com`/`fonts.gstatic.com`, sin autohospedar | `apps/web/index.html` (`<link rel="preconnect">`/`<link rel="stylesheet">`) — transfiere la IP del visitante a Google sin base legal/consentimiento documentado (mismo problema que motivó la sentencia del tribunal regional de Múnich, 2022, sobre Google Fonts) |
| **Almacenamiento local — tokens + email** | ⚠️ JWT access/refresh + email del usuario en `localStorage`, en claro, indefinidamente | `packages/store/src/authStore.ts:43` — `persist(..., { name: 'fleetmgm-auth' })` sin `partialize` ni storage personalizado; usa el motor por defecto de Zustand (`localStorage`) |
| Otros datos personales en el navegador | ✅ Ninguno — la caché de TanStack Query es solo en memoria, se pierde al recargar | `apps/web/src/main.tsx`: `new QueryClient()` sin persister configurado |
| Rastreadores de terceros (analytics/ads) | ✅ Ninguno | Sin dependencias ni scripts de Google Analytics, Sentry, Hotjar, Meta Pixel, etc. |

**Conclusión de esta sección:** el "banner de cookies" no es el problema aquí — no hay cookies. Los hallazgos reales y accionables son (a) el uso de Google Fonts sin autohospedar y (b) el almacenamiento sin cifrar/expiración de tokens y email en `localStorage`, expuesto además a robo vía XSS si alguna vez se introdujera una vulnerabilidad de ese tipo.

---

## 2. Matriz de cumplimiento RGPD (por principio/artículo)

| Principio / Artículo | Estado | Evidencia |
|---|---|---|
| Art. 5(1)(a) Licitud, lealtad, transparencia | ❌ Sin base legal documentada para tratar DNI/NIE, GPS o datos de clientes | Única justificación: `planning.md` línea 1997, una frase sin desarrollo ("Requisitos legales y operativos confirmados") |
| Art. 5(1)(b) Limitación de la finalidad | ⚠️ El GPS de un conductor se recoge sin declarar el propósito ni informarle | `GpsMockScheduler` genera posición cada 30s para todo vehículo `ACTIVE`, sin mecanismo de aviso |
| Art. 5(1)(c) Minimización de datos | ⚠️ Se exige DNI/NIE completo del trabajador sin análisis de necesidad frente a alternativas (p.ej. solo nº de licencia) | `Worker.nationalId` (`nullable = false`), única justificación una línea en `planning.md:1997` |
| Art. 5(1)(e) Limitación del plazo de conservación | ❌ Sin ninguna política de retención ni purga — todo se conserva para siempre | Único `@Scheduled` en todo el backend genera datos GPS (`GpsMockScheduler`); ninguno los elimina. Soft-delete (`deletedAt`) nunca deriva en borrado físico |
| Art. 6 Base legal del tratamiento | ❌ No documentada en ningún sitio | Cero menciones de base legal (consentimiento, contrato, interés legítimo, obligación legal) en `planning.md`/`CLAUDE.md` |
| Art. 13-14 Transparencia / derecho a ser informado | ❌ Sin política de privacidad ni aviso al conductor sobre el tracking GPS | Sin ruta `/privacy`/`/legal` en `App.tsx`; el propio `DRIVER` no puede ver su posición GPS (matriz de permisos, `planning.md`) |
| Art. 15 Derecho de acceso | ❌ No implementado — ni el propio interesado puede exportar sus datos | Sin endpoint `/export`/`/gdpr` en todo el backend |
| Art. 16 Derecho de rectificación | ⚠️ Parcial — solo vía PUT/PATCH por ADMIN/MANAGER/ADMINISTRATIVE, sin autoservicio del interesado | `DRIVER` no puede editar su propio perfil de `Worker` (matriz de permisos) |
| Art. 17 Derecho de supresión ("al olvido") | ❌ No implementado realmente — solo soft-delete, el registro persiste para siempre | Sin `hardDelete`/`purge` en ningún repositorio del backend |
| Art. 20 Derecho a la portabilidad | ❌ No implementado | Sin exportación estructurada de datos por interesado |
| Art. 25 Protección de datos desde el diseño (privacy by design) | ⚠️ Parcial — hay buenas prácticas de seguridad técnica (RBAC, IDOR checks, cifrado de contraseñas) pero ninguna decisión de diseño motivada explícitamente por RGPD | Ver `SECURITY_AUDIT.md` para el detalle de controles técnicos ya implementados |
| Art. 30 Registro de actividades de tratamiento | ❌ No existe ningún registro de tratamiento (distinto del `AuditLog` técnico) | Sin documentación al respecto |
| Art. 32 Seguridad del tratamiento | ✅ Cubierto en gran medida por los controles técnicos ya auditados en `SECURITY_AUDIT.md` (JWT, BCrypt, RBAC, TLS en prod) | Ver informe de seguridad previo |
| Art. 44-49 Transferencias internacionales | ❌ Sin documentar — no se especifica región de alojamiento de Railway/Vercel | `planning.md`/`README.md`: Vercel + Railway mencionados sin región ni análisis de transferencia |
| Registro de auditoría como dato personal | ⚠️ `AuditLog.oldValues`/`newValues`/`ipAddress` existen en el esquema pero nunca se rellenan en ningún punto del código — la auditoría real es más limitada de lo documentado | `AuthService`, `InvoiceService`, `MaintenanceService`, etc. — ningún caller invoca `setOldValues()`/`setNewValues()`/`setIpAddress()` |

---

## 3. Hallazgos y recomendaciones (por severidad)

### Media

1. **Sin ningún derecho RGPD del interesado implementado** (acceso, supresión real, portabilidad). El soft-delete conserva el DNI/NIE, teléfono, historial de trabajos y posiciones GPS de un trabajador para siempre, incluso después de darlo de baja.
   *Recomendación:* definir un plazo de retención tras el soft-delete (p.ej. purga a los N meses) y, como mínimo para el TFM, documentar la decisión aunque no se implemente el job de purga.

2. **Sin política de privacidad ni aviso legal en el frontend.** No hay ruta ni página que informe a los usuarios (especialmente a los `DRIVER`, cuya ubicación se registra) sobre qué datos se tratan y con qué finalidad.
   *Recomendación:* añadir una página estática `/privacy` con la información mínima exigida por el Art. 13 RGPD.

3. **Geolocalización de conductores sin transparencia**: el propio `DRIVER` no puede ver su posición GPS (se lo prohíbe la matriz de permisos), y no hay ningún mecanismo de aviso/consentimiento sobre el tracking.
   *Recomendación:* al menos documentar la base legal (probablemente interés legítimo/ejecución de contrato laboral) y considerar dar acceso de solo lectura al propio conductor sobre su historial.

4. **Google Fonts cargado desde el CDN de Google** sin autohospedar, transfiriendo la IP del visitante a un tercero.
   *Recomendación:* autohospedar las fuentes (`@fontsource` o descarga local) en vez de enlazar a `fonts.googleapis.com`.

5. **Tokens JWT y email en `localStorage` sin cifrar ni expiración de sesión.** No es estrictamente un incumplimiento de "cookies", pero es un dato personal (email) y credenciales de sesión persistidos indefinidamente y expuestos a robo vía XSS.
   *Recomendación:* evaluar mover el almacenamiento a un mecanismo con expiración más corta o al menos documentar el riesgo residual; revisar si aplica junto con la mitigación CSP ya señalada en `SECURITY_AUDIT.md`.

### Baja

6. **Sin base legal documentada para el DNI/NIE del trabajador.** Existe una única línea en `planning.md` sin desarrollo jurídico.
   *Recomendación:* documentar la base legal (obligación legal/laboral) en `planning.md` o en una futura política de privacidad interna.

7. **`AuditLog` no captura realmente `oldValues`/`newValues`/`ipAddress`** pese a estar en el esquema y documentado en `CLAUDE.md`. Esto no es una fuga de datos (no hay nada que redactar porque no se rellena), pero limita la trazabilidad real de quién accedió/modificó datos personales.
   *Recomendación:* si se decide implementar la captura completa del audit log, aplicar entonces la redacción de contraseñas/tokens ya documentada en `CLAUDE.md:486`.

8. **Sin región de alojamiento especificada** para Railway/Vercel — relevante solo si se despliega con datos reales de personas de la UE.
   *Recomendación:* para un despliegue real (no solo demo de TFM), verificar que la región de Railway/Vercel esté en la UE o documentar la base para la transferencia internacional si no lo está.

### Sin hallazgos (aplicable, comprobado explícitamente)

- **Cookies (propias o de terceros):** ninguna en toda la aplicación — ePrivacy no exige banner de consentimiento porque no hay cookies que consentir.
- **Rastreadores de terceros (analytics/publicidad):** ninguno.
- **Fuga de datos personales vía caché del cliente:** TanStack Query no persiste nada fuera de memoria; no hay datos de otros usuarios cacheados en `localStorage`/`sessionStorage`.

---

## 4. Contexto: naturaleza académica del proyecto

`CLAUDE.md` describe FleetMgm explícitamente como una aplicación de TFM (Trabajo Fin de Máster). Esto no exime legalmente de RGPD si el proyecto llega a desplegarse con datos reales de personas identificables (p.ej. una demo pública con trabajadores/clientes reales), pero sí explica por qué la protección de datos no se abordó como requisito junto al resto de controles OWASP — el foco documentado fue exclusivamente seguridad técnica, no cumplimiento normativo de protección de datos. Antes de cualquier uso más allá de una demo académica controlada, los hallazgos "Media" de la sección 3 deberían resolverse.

---

## 5. Próximos pasos sugeridos (no aplicados en esta pasada)

Por prioridad:
1. Documentar la base legal del tratamiento de DNI/NIE y datos GPS (aunque sea en `planning.md`).
2. Añadir una página mínima de política de privacidad/aviso legal.
3. Definir y, si el alcance del TFM lo permite, implementar una política de retención/purga para GPS, `AuditLog` y soft-deletes.
4. Autohospedar Google Fonts.
5. Evaluar el riesgo de tokens/email en `localStorage` sin cifrar (relacionado con el hallazgo de CSP de `SECURITY_AUDIT.md`).
6. Si se decide dar acceso al propio conductor sobre su GPS, ajustar la matriz de permisos y el `@PreAuthorize` de `GpsService`.
