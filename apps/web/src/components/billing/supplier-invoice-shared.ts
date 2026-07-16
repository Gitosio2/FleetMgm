import type { ExpenseCategory, SupplierInvoiceStatus } from '@fleetmgm/api'

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
