import type { WorkerRole } from '@fleetmgm/api'
import { cn } from '@/lib/utils'

const ROLE_LABEL: Record<WorkerRole, string> = {
  DRIVER: 'Conductor',
  TECHNICIAN: 'Técnico',
  BOTH: 'Conductor y técnico',
}

const ROLE_CLASSNAME: Record<WorkerRole, string> = {
  DRIVER: 'bg-secondary-container/20 text-secondary',
  TECHNICIAN: 'bg-tertiary-container/40 text-on-tertiary-container',
  BOTH: 'bg-primary-container/40 text-primary',
}

type WorkerRoleBadgeProps = {
  role: WorkerRole
}

export function WorkerRoleBadge({ role }: WorkerRoleBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        ROLE_CLASSNAME[role],
      )}
    >
      {ROLE_LABEL[role]}
    </span>
  )
}
