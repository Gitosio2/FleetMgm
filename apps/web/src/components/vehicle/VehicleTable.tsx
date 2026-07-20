import { Pencil, TrendingUp, Users } from 'lucide-react'
import type { Vehicle } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { VehicleStatusBadge } from './VehicleStatusBadge'
import { VehicleDeleteButton } from './VehicleDeleteButton'

type VehicleTableProps = {
  vehicles: Vehicle[]
  canManage: boolean
  onEdit: (vehicle: Vehicle) => void
  onViewAssignment: (vehicle: Vehicle) => void
  onViewProfitability: (vehicle: Vehicle) => void
}

function usage(vehicle: Vehicle): string {
  return vehicle.usageMeasure === 'KILOMETERS'
    ? `${vehicle.currentKm ?? 0} km`
    : `${vehicle.currentHours ?? 0} h`
}

export function VehicleTable({
  vehicles,
  canManage,
  onEdit,
  onViewAssignment,
  onViewProfitability,
}: VehicleTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Matrícula</TableHead>
          <TableHead>Vehículo</TableHead>
          <TableHead>Uso</TableHead>
          <TableHead>Estado</TableHead>
          {canManage && <TableHead>Acciones</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {vehicles.map((vehicle) => (
          <TableRow key={vehicle.id}>
            <TableCell>{vehicle.licensePlate ?? '—'}</TableCell>
            <TableCell>{vehicle.make} {vehicle.model}</TableCell>
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
                    aria-label="Editar vehículo"
                    title="Editar vehículo"
                    onClick={() => onEdit(vehicle)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Ver asignación"
                    title="Ver asignación"
                    onClick={() => onViewAssignment(vehicle)}
                  >
                    <Users className="size-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Ver rentabilidad"
                    title="Ver rentabilidad"
                    onClick={() => onViewProfitability(vehicle)}
                  >
                    <TrendingUp className="size-4" />
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
