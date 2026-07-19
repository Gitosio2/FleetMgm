import type { JobStatus } from '@fleetmgm/api'

// Centralized here (rather than duplicated) since it's now consumed in two places —
// JobStatusBadge and the status filter dropdown (JobFilters) — same rationale as
// WORKER_ROLE_LABEL in worker-shared.ts.
export const JOB_STATUSES: JobStatus[] = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']

export const JOB_STATUS_LABEL: Record<JobStatus, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
}
