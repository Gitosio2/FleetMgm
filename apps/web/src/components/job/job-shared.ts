import { isAxiosError } from 'axios'
import type { ApiError, JobStatus } from '@fleetmgm/api'

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

// Extracted out of JobFormModal (now also consumed by JobUsageValueModal) so both surfaces
// resolve backend error codes to the same Spanish text.
export const JOB_ERROR_MESSAGES: Record<string, string> = {
  JOB_ACTUAL_END_WITHOUT_START: 'No puedes indicar un fin real sin haber indicado también un inicio real.',
  JOB_ACTUAL_END_BEFORE_START: 'El fin real no puede ser anterior al inicio real.',
  JOB_ACTUAL_DATE_IN_FUTURE: 'El inicio o el fin real no pueden ser una fecha futura.',
  JOB_USAGE_VALUE_BELOW_CURRENT: 'El valor ingresado es menor al que ya tiene registrado el vehículo.',
  JOB_INVALID_STATE_TRANSITION:
    'El trabajo ya no está en un estado válido para esta acción. Recargá la página e intentá de nuevo.',
}

export const DEFAULT_JOB_ERROR_MESSAGE = 'No se pudo completar la acción.'

export function resolveJobErrorMessage(error: unknown): string {
  if (isAxiosError<ApiError>(error) && error.response?.data.code) {
    return JOB_ERROR_MESSAGES[error.response.data.code] ?? DEFAULT_JOB_ERROR_MESSAGE
  }
  return DEFAULT_JOB_ERROR_MESSAGE
}
