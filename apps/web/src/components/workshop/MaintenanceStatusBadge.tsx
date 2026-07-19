import type { MaintenanceStatus } from '@fleetmgm/api'
import { cn } from '@/lib/utils'
import { STATUS_LABEL } from './form-shared'

const STATUS_CLASSNAME: Record<MaintenanceStatus, string> = {
  SCHEDULED: 'bg-surface-container-high text-on-surface-variant',
  IN_PROGRESS: 'bg-tertiary-container/40 text-on-tertiary-container',
  COMPLETED: 'bg-secondary-container/20 text-secondary',
  CANCELLED: 'bg-error-container/40 text-error',
}

type MaintenanceStatusBadgeProps = {
  status: MaintenanceStatus
}

export function MaintenanceStatusBadge({ status }: MaintenanceStatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium',
        STATUS_CLASSNAME[status],
      )}
    >
      {STATUS_LABEL[status]}
    </span>
  )
}
