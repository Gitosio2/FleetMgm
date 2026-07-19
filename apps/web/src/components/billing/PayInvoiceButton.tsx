import { Banknote } from 'lucide-react'
import { usePayInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { resolveInvoiceErrorMessage } from './invoice-shared'

type PayInvoiceButtonProps = {
  invoiceId: string
}

export function PayInvoiceButton({ invoiceId }: PayInvoiceButtonProps) {
  const payInvoice = usePayInvoice()

  return (
    <div className="flex flex-col items-end gap-1">
      <Button
        variant="ghost"
        size="sm"
        aria-label="Marcar factura como pagada"
        title="Marcar factura como pagada"
        disabled={payInvoice.isPending}
        onClick={() => payInvoice.mutate({ id: invoiceId })}
      >
        <Banknote className="size-4" />
      </Button>
      {payInvoice.isError && (
        <p role="alert" className="text-sm text-error">
          {resolveInvoiceErrorMessage(payInvoice.error)}
        </p>
      )}
    </div>
  )
}
