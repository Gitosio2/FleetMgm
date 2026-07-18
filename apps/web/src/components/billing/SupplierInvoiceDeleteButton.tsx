import { Trash2 } from 'lucide-react'
import { useDeleteSupplierInvoice } from '@fleetmgm/hooks'
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

type SupplierInvoiceDeleteButtonProps = {
  invoiceId: string
}

export function SupplierInvoiceDeleteButton({ invoiceId }: SupplierInvoiceDeleteButtonProps) {
  const deleteInvoice = useDeleteSupplierInvoice()

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          aria-label="Eliminar factura de proveedor"
          disabled={deleteInvoice.isPending}
        >
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Eliminar factura de proveedor?</AlertDialogTitle>
          <AlertDialogDescription>
            Esto elimina la factura de proveedor. Esta acción no se puede deshacer.
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
