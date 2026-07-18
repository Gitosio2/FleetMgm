import type { WorkerRole } from '@fleetmgm/api'

// Centralized here (rather than duplicated) since it's now consumed in two places —
// WorkerFormModal and the new role filter dropdown (WorkerFilters) — same rationale as
// EXPENSE_CATEGORY_LABEL in supplier-invoice-shared.ts.
export const WORKER_ROLES: WorkerRole[] = ['DRIVER', 'TECHNICIAN', 'BOTH']

export const WORKER_ROLE_LABEL: Record<WorkerRole, string> = {
  DRIVER: 'Conductor',
  TECHNICIAN: 'Técnico',
  BOTH: 'Conductor y técnico',
}
