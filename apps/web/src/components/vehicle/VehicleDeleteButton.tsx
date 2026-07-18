import { Trash2 } from 'lucide-react'
import { useDeleteVehicle } from '@fleetmgm/hooks'
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

type VehicleDeleteButtonProps = {
  vehicleId: string
  vehicleLabel: string
}

export function VehicleDeleteButton({ vehicleId, vehicleLabel }: VehicleDeleteButtonProps) {
  const deleteVehicle = useDeleteVehicle()

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="destructive" size="sm" aria-label="Eliminar vehículo" disabled={deleteVehicle.isPending}>
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Eliminar {vehicleLabel}?</AlertDialogTitle>
          <AlertDialogDescription>
            Esto elimina el vehículo de las listas activas. Esta acción no se puede deshacer desde aquí.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={() => deleteVehicle.mutate(vehicleId)}>
            Eliminar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
