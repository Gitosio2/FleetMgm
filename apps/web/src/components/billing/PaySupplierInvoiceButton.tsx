import { Banknote } from 'lucide-react'
import { usePaySupplierInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { resolveSupplierInvoiceErrorMessage } from './supplier-invoice-shared'

type PaySupplierInvoiceButtonProps = {
  supplierInvoiceId: string
}

export function PaySupplierInvoiceButton({ supplierInvoiceId }: PaySupplierInvoiceButtonProps) {
  const payInvoice = usePaySupplierInvoice()

  return (
    <div className="flex flex-col items-end gap-1">
      <Button
        variant="ghost"
        size="sm"
        aria-label="Marcar factura como pagada"
        title="Marcar factura como pagada"
        disabled={payInvoice.isPending}
        onClick={() => payInvoice.mutate({ id: supplierInvoiceId })}
      >
        <Banknote className="size-4" />
      </Button>
      {payInvoice.isError && (
        <p role="alert" className="text-sm text-error">
          {resolveSupplierInvoiceErrorMessage(payInvoice.error)}
        </p>
      )}
    </div>
  )
}
