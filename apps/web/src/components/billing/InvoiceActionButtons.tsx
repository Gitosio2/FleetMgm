import { Banknote, Eye, Pencil, Send } from 'lucide-react'
import type { Invoice } from '@fleetmgm/api'
import { useIssueInvoice, usePayInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { PdfDownloadButton } from './PdfDownloadButton'
import { InvoiceDeleteButton } from './InvoiceDeleteButton'
import { resolveInvoiceErrorMessage } from './invoice-shared'

type InvoiceActionButtonsProps = {
  invoice: Invoice
  onEdit: (invoice: Invoice) => void
}

export function InvoiceActionButtons({ invoice, onEdit }: InvoiceActionButtonsProps) {
  const issueInvoice = useIssueInvoice()
  const payInvoice = usePayInvoice()

  const isPending = issueInvoice.isPending || payInvoice.isPending
  const error = issueInvoice.isError ? issueInvoice.error : payInvoice.isError ? payInvoice.error : null
  const editLabel = invoice.status === 'PAID' ? 'Ver factura' : 'Editar factura'

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
        {invoice.status === 'DRAFT' && (
          <Button
            variant="ghost"
            size="sm"
            aria-label="Emitir factura"
            title="Emitir factura"
            disabled={isPending}
            onClick={() => issueInvoice.mutate(invoice.id)}
          >
            <Send className="size-4" />
          </Button>
        )}
        {invoice.status === 'ISSUED' && (
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
