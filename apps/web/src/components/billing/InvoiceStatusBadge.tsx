import type { InvoiceStatus } from '@fleetmgm/api'
import { cn } from '@/lib/utils'
import { STATUS_LABEL } from './invoice-shared'

const STATUS_CLASSNAME: Record<InvoiceStatus, string> = {
  DRAFT: 'bg-surface-container-high text-on-surface-variant',
  ISSUED: 'bg-tertiary-container/40 text-on-tertiary-container',
  PAID: 'bg-secondary-container/20 text-secondary',
  OVERDUE: 'bg-error-container/40 text-error',
  CANCELLED: 'bg-surface-container-highest text-on-surface-variant line-through',
}

type InvoiceStatusBadgeProps = {
  status: InvoiceStatus
}

export function InvoiceStatusBadge({ status }: InvoiceStatusBadgeProps) {
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
