import { isAxiosError } from 'axios'
import type { ApiError, Invoice } from '@fleetmgm/api'
import { useIssueInvoice, usePayInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { PdfDownloadButton } from './PdfDownloadButton'
import { InvoiceDeleteButton } from './InvoiceDeleteButton'

const INVOICE_ERROR_MESSAGES: Record<string, string> = {
  INVOICE_NO_LINE_ITEMS: 'No se puede emitir una factura sin líneas de factura.',
  INVOICE_INVALID_STATE_TRANSITION: 'La factura ya no admite esta acción.',
}

const DEFAULT_INVOICE_ERROR_MESSAGE = 'No se pudo completar la acción.'

function resolveInvoiceErrorMessage(error: unknown): string {
  if (isAxiosError<ApiError>(error) && error.response?.data.code) {
    return INVOICE_ERROR_MESSAGES[error.response.data.code] ?? DEFAULT_INVOICE_ERROR_MESSAGE
  }
  return DEFAULT_INVOICE_ERROR_MESSAGE
}

type InvoiceActionButtonsProps = {
  invoice: Invoice
  onEdit: (invoice: Invoice) => void
}

export function InvoiceActionButtons({ invoice, onEdit }: InvoiceActionButtonsProps) {
  const issueInvoice = useIssueInvoice()
  const payInvoice = usePayInvoice()

  const isPending = issueInvoice.isPending || payInvoice.isPending
  const error = issueInvoice.isError ? issueInvoice.error : payInvoice.isError ? payInvoice.error : null

  return (
    <div className="flex flex-col items-start gap-1">
      <div className="flex items-center gap-1">
        <Button variant="ghost" size="sm" onClick={() => onEdit(invoice)}>
          Editar
        </Button>
        {invoice.status === 'DRAFT' && (
          <Button
            variant="default"
            size="sm"
            disabled={isPending}
            onClick={() => issueInvoice.mutate(invoice.id)}
          >
            Emitir
          </Button>
        )}
        {invoice.status === 'ISSUED' && (
          <Button
            variant="default"
            size="sm"
            disabled={isPending}
            onClick={() => payInvoice.mutate({ id: invoice.id })}
          >
            Marcar pagada
          </Button>
        )}
        <PdfDownloadButton invoiceId={invoice.id} invoiceNumber={invoice.invoiceNumber} />
        {invoice.status !== 'PAID' && invoice.status !== 'CANCELLED' && (
          <InvoiceDeleteButton
            invoiceId={invoice.id}
            invoiceNumber={invoice.invoiceNumber}
            status={invoice.status}
          />
        )}
      </div>
      {error && (
        <p role="alert" className="text-sm text-error">
          {resolveInvoiceErrorMessage(error)}
        </p>
      )}
    </div>
  )
}
