import { Pencil } from 'lucide-react'
import type { Vehicle } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { VehicleStatusBadge } from './VehicleStatusBadge'
import { VehicleDeleteButton } from './VehicleDeleteButton'

type VehicleTableProps = {
  vehicles: Vehicle[]
  canManage: boolean
  onEdit: (vehicle: Vehicle) => void
}

function usage(vehicle: Vehicle): string {
  return vehicle.usageMeasure === 'KILOMETERS'
    ? `${vehicle.currentKm ?? 0} km`
    : `${vehicle.currentHours ?? 0} h`
}

export function VehicleTable({ vehicles, canManage, onEdit }: VehicleTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vehicle</TableHead>
          <TableHead>License plate</TableHead>
          <TableHead>Usage</TableHead>
          <TableHead>Status</TableHead>
          {canManage && <TableHead>Actions</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {vehicles.map((vehicle) => (
          <TableRow key={vehicle.id}>
            <TableCell>{vehicle.make} {vehicle.model}</TableCell>
            <TableCell>{vehicle.licensePlate ?? '—'}</TableCell>
            <TableCell>{usage(vehicle)}</TableCell>
            <TableCell>
              <VehicleStatusBadge status={vehicle.status} />
            </TableCell>
            {canManage && (
              <TableCell>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Edit vehicle"
                    onClick={() => onEdit(vehicle)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  <VehicleDeleteButton
                    vehicleId={vehicle.id}
                    vehicleLabel={`${vehicle.make} ${vehicle.model}`}
                  />
                </div>
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
