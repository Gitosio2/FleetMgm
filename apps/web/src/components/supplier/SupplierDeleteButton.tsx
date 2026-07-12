import { Trash2 } from 'lucide-react'
import { useDeleteSupplier } from '@fleetmgm/hooks'
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

type SupplierDeleteButtonProps = {
  supplierId: string
  supplierName: string
}

export function SupplierDeleteButton({ supplierId, supplierName }: SupplierDeleteButtonProps) {
  const deleteSupplier = useDeleteSupplier()

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="ghost" size="sm" aria-label="Eliminar proveedor" disabled={deleteSupplier.isPending}>
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Eliminar a {supplierName}?</AlertDialogTitle>
          <AlertDialogDescription>
            Esto elimina al proveedor de las listas activas. Esta acción no se puede deshacer desde aquí.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={() => deleteSupplier.mutate(supplierId)}>
            Eliminar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
