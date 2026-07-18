import { Trash2 } from 'lucide-react'
import { useDeleteWorker } from '@fleetmgm/hooks'
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

type WorkerDeleteButtonProps = {
  workerId: string
  workerLabel: string
}

export function WorkerDeleteButton({ workerId, workerLabel }: WorkerDeleteButtonProps) {
  const deleteWorker = useDeleteWorker()

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="destructive" size="sm" aria-label="Eliminar trabajador" disabled={deleteWorker.isPending}>
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Eliminar a {workerLabel}?</AlertDialogTitle>
          <AlertDialogDescription>
            Esto elimina al trabajador de las listas activas. Esta acción no se puede deshacer desde aquí.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={() => deleteWorker.mutate(workerId)}>
            Eliminar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
