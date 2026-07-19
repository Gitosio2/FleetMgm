import type { WorkshopStatus } from '@fleetmgm/api'
import { cn } from '@/lib/utils'

const STATUS_LABEL: Record<WorkshopStatus, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
}

const STATUS_CLASSNAME: Record<WorkshopStatus, string> = {
  PENDING: 'bg-surface-container-high text-on-surface-variant',
  IN_PROGRESS: 'bg-tertiary-container/40 text-on-tertiary-container',
  COMPLETED: 'bg-secondary-container/20 text-secondary',
  CANCELLED: 'bg-error-container/40 text-error',
}

type ScheduleStatusBadgeProps = {
  status: WorkshopStatus
}

export function ScheduleStatusBadge({ status }: ScheduleStatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        STATUS_CLASSNAME[status],
      )}
    >
      {STATUS_LABEL[status]}
    </span>
  )
}
