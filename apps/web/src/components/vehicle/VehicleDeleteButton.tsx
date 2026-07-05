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
        <Button variant="ghost" size="sm" aria-label="Delete vehicle" disabled={deleteVehicle.isPending}>
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete {vehicleLabel}?</AlertDialogTitle>
          <AlertDialogDescription>
            This removes the vehicle from active lists. This action cannot be undone from here.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction onClick={() => deleteVehicle.mutate(vehicleId)}>
            Delete
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
