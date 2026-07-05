import { Trash2 } from 'lucide-react'
import { useDeleteClient } from '@fleetmgm/hooks'
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

type ClientDeleteButtonProps = {
  clientId: string
  clientName: string
}

export function ClientDeleteButton({ clientId, clientName }: ClientDeleteButtonProps) {
  const deleteClient = useDeleteClient()

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="ghost" size="sm" aria-label="Eliminar cliente" disabled={deleteClient.isPending}>
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Eliminar a {clientName}?</AlertDialogTitle>
          <AlertDialogDescription>
            Esto elimina al cliente de las listas activas. Esta acción no se puede deshacer desde aquí.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={() => deleteClient.mutate(clientId)}>
            Eliminar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
