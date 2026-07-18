import type { VehicleStatus } from '@fleetmgm/api'
import { cn } from '@/lib/utils'
import { VEHICLE_STATUS_LABEL } from '@/lib/vehicle-status-label'

const STATUS_CLASSNAME: Record<VehicleStatus, string> = {
  ACTIVE: 'bg-secondary-container/20 text-secondary',
  MAINTENANCE: 'bg-tertiary-container/40 text-tertiary',
  INACTIVE: 'bg-surface-container-high text-on-surface-variant',
  DECOMMISSIONED: 'bg-error-container/40 text-error',
}

type VehicleStatusBadgeProps = {
  status: VehicleStatus
}

export function VehicleStatusBadge({ status }: VehicleStatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        STATUS_CLASSNAME[status],
      )}
    >
      {VEHICLE_STATUS_LABEL[status]}
    </span>
  )
}
