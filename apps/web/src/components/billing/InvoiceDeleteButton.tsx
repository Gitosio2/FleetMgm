import { Trash2 } from 'lucide-react'
import type { InvoiceStatus } from '@fleetmgm/api'
import { useDeleteInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'

type InvoiceDeleteButtonProps = {
  invoiceId: string
  invoiceNumber: string
  status: InvoiceStatus
}

// ISSUED/OVERDUE invoices already consumed a sequential invoice_number_seq value — "deleting" one
// doesn't soft-delete it (InvoiceService.delete), it cancels it, so the confirmation copy has to
// say so explicitly instead of promising a removal that won't happen.
export function InvoiceDeleteButton({ invoiceId, invoiceNumber, status }: InvoiceDeleteButtonProps) {
  const deleteInvoice = useDeleteInvoice()
  const isDraft = status === 'DRAFT'

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button
          variant="destructive"
          size="sm"
          aria-label="Eliminar factura"
          title="Eliminar factura"
          disabled={deleteInvoice.isPending}
        >
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Eliminar factura {invoiceNumber}?</AlertDialogTitle>
          <AlertDialogDescription>
            {isDraft
              ? 'Esto elimina el borrador. Esta acción no se puede deshacer.'
              : 'La factura ya está emitida: se marcará como anulada en vez de eliminarse, y no se reutilizará su número. Crea una factura nueva si es necesario. Esta acción no se puede deshacer.'}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={() => deleteInvoice.mutate(invoiceId)}>
            Eliminar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
