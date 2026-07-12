import type { AuditAction } from '@fleetmgm/api'

export const AUDIT_ACTION_LABEL: Record<AuditAction, string> = {
  CREATE: 'Creación',
  UPDATE: 'Actualización',
  DELETE: 'Eliminación',
  LOGIN: 'Inicio de sesión',
  LOGOUT: 'Cierre de sesión',
  ACCESS_DENIED: 'Acceso denegado',
  ACCOUNT_LOCKED: 'Cuenta bloqueada',
}

// entityType is a free-text string set by whichever service writes the AuditLog row (see
// AuditLog.java), not a backend enum — this map only covers the values currently produced.
// Unknown values fall back to the raw string (see AuditLogTable) so a future producer doesn't
// silently disappear from the UI.
export const AUDIT_ENTITY_TYPE_LABEL: Record<string, string> = {
  Invoice: 'Factura',
  SupplierInvoice: 'Factura de proveedor',
  MaintenanceRecord: 'Mantenimiento',
  WorkshopSchedule: 'Cita de taller',
  User: 'Usuario',
}
