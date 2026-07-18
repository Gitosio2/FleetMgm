import { isAxiosError } from 'axios'
import { Banknote, Eye, Pencil } from 'lucide-react'
import type { ApiError, SupplierInvoice } from '@fleetmgm/api'
import { usePaySupplierInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { SupplierInvoiceDeleteButton } from './SupplierInvoiceDeleteButton'

const SUPPLIER_INVOICE_ERROR_MESSAGES: Record<string, string> = {
  SUPPLIER_INVOICE_INVALID_STATE_TRANSITION: 'Esta factura ya no admite esta acción.',
  SUPPLIER_INVOICE_ALLOCATION_INCOMPLETE:
    'Las líneas de esta factura no suman el subtotal — completa la asignación por vehículo antes de marcarla como pagada.',
}

const DEFAULT_SUPPLIER_INVOICE_ERROR_MESSAGE = 'No se pudo completar la acción.'

function resolveSupplierInvoiceErrorMessage(error: unknown): string {
  if (isAxiosError<ApiError>(error) && error.response?.data.code) {
    return SUPPLIER_INVOICE_ERROR_MESSAGES[error.response.data.code] ?? DEFAULT_SUPPLIER_INVOICE_ERROR_MESSAGE
  }
  return DEFAULT_SUPPLIER_INVOICE_ERROR_MESSAGE
}

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
