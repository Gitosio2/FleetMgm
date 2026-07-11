import type { SupplierInvoiceStatus } from '@fleetmgm/api'
import { cn } from '@/lib/utils'

const STATUS_LABEL: Record<SupplierInvoiceStatus, string> = {
  PENDING: 'Pendiente',
  PAID: 'Pagada',
}

const STATUS_CLASSNAME: Record<SupplierInvoiceStatus, string> = {
  PENDING: 'bg-surface-container-high text-on-surface-variant',
  PAID: 'bg-secondary-container/20 text-secondary',
}

type SupplierInvoiceStatusBadgeProps = {
  status: SupplierInvoiceStatus
}

export function SupplierInvoiceStatusBadge({ status }: SupplierInvoiceStatusBadgeProps) {
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
