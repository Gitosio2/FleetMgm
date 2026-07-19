import { Banknote, Eye, Pencil } from 'lucide-react'
import type { SupplierInvoice } from '@fleetmgm/api'
import { usePaySupplierInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { SupplierInvoiceDeleteButton } from './SupplierInvoiceDeleteButton'
import { resolveSupplierInvoiceErrorMessage } from './supplier-invoice-shared'

type SupplierInvoiceActionButtonsProps = {
  invoice: SupplierInvoice
  onEdit: (invoice: SupplierInvoice) => void
}

export function SupplierInvoiceActionButtons({ invoice, onEdit }: SupplierInvoiceActionButtonsProps) {
  const payInvoice = usePaySupplierInvoice()

  const isPending = payInvoice.isPending
  const editLabel = invoice.status === 'PAID' ? 'Ver factura de proveedor' : 'Editar factura de proveedor'

  return (
    <div className="flex flex-col items-start gap-1">
      <div className="flex items-center gap-1">
        <Button
          variant="ghost"
          size="sm"
          aria-label={editLabel}
          title={editLabel}
          onClick={() => onEdit(invoice)}
        >
          {invoice.status === 'PAID' ? <Eye className="size-4" /> : <Pencil className="size-4" />}
        </Button>
        {invoice.status === 'PENDING' && (
          <Button
            variant="ghost"
            size="sm"
            aria-label="Marcar factura como pagada"
            title="Marcar factura como pagada"
            disabled={isPending}
            onClick={() => payInvoice.mutate({ id: invoice.id })}
          >
            <Banknote className="size-4" />
          </Button>
        )}
        {invoice.status === 'PENDING' && <SupplierInvoiceDeleteButton invoiceId={invoice.id} />}
      </div>
      {payInvoice.isError && (
        <p role="alert" className="text-sm text-error">
          {resolveSupplierInvoiceErrorMessage(payInvoice.error)}
        </p>
      )}
    </div>
  )
}
