import { isAxiosError } from 'axios'
import type { ApiError, ExpenseCategory, SupplierInvoiceStatus } from '@fleetmgm/api'

// Centralized here (rather than duplicated per-component, unlike the smaller 2-value
// MaintenanceCategory map) because this 6-value enum is consumed in three places
// (SupplierInvoiceTable, SupplierInvoiceFormModal, and the Billing page's category filter) —
// duplicating it three times would risk the labels drifting out of sync.
export const EXPENSE_CATEGORY_LABEL: Record<ExpenseCategory, string> = {
  MAINTENANCE: 'Mantenimiento',
  FUEL: 'Combustible',
  INSURANCE: 'Seguro',
  LEASING_RENTING: 'Leasing y renting',
  TOLL: 'Peaje',
  OTHER: 'Otro',
}

// Moved here from SupplierInvoiceStatusBadge for the same reason as EXPENSE_CATEGORY_LABEL above —
// now also consumed by the status filter dropdown (SupplierInvoiceFilters).
export const STATUS_LABEL: Record<SupplierInvoiceStatus, string> = {
  PENDING: 'Pendiente',
  PAID: 'Pagada',
}

// Moved here from SupplierInvoiceActionButtons so PaySupplierInvoiceButton (dashboard card) can
// reuse the same pay-error copy instead of duplicating the map.
const SUPPLIER_INVOICE_ERROR_MESSAGES: Record<string, string> = {
  SUPPLIER_INVOICE_INVALID_STATE_TRANSITION: 'Esta factura ya no admite esta acción.',
  SUPPLIER_INVOICE_ALLOCATION_INCOMPLETE:
    'Las líneas de esta factura no suman el subtotal — completa la asignación por vehículo antes de marcarla como pagada.',
}

const DEFAULT_SUPPLIER_INVOICE_ERROR_MESSAGE = 'No se pudo completar la acción.'

export function resolveSupplierInvoiceErrorMessage(error: unknown): string {
  if (isAxiosError<ApiError>(error) && error.response?.data.code) {
    return SUPPLIER_INVOICE_ERROR_MESSAGES[error.response.data.code] ?? DEFAULT_SUPPLIER_INVOICE_ERROR_MESSAGE
  }
  return DEFAULT_SUPPLIER_INVOICE_ERROR_MESSAGE
}
