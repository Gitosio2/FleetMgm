import { Pencil } from 'lucide-react'
import type { Worker } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { WorkerRoleBadge } from './WorkerRoleBadge'
import { WorkerDeleteButton } from './WorkerDeleteButton'

type WorkerTableProps = {
  workers: Worker[]
  canManage: boolean
  onEdit: (worker: Worker) => void
  assignedVehicleByDriverId?: Map<string, string>
}

export function WorkerTable({ workers, canManage, onEdit, assignedVehicleByDriverId }: WorkerTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Trabajador</TableHead>
          <TableHead>Rol</TableHead>
          <TableHead>Documento</TableHead>
          <TableHead>Teléfono</TableHead>
          <TableHead>Vehículo asignado</TableHead>
          {canManage && <TableHead>Acciones</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {workers.map((worker) => (
          <TableRow key={worker.id}>
            <TableCell>{worker.fullName}</TableCell>
            <TableCell>
              <WorkerRoleBadge role={worker.workerRole} />
            </TableCell>
            <TableCell>{worker.nationalId}</TableCell>
            <TableCell>{worker.phone ?? '—'}</TableCell>
            <TableCell>{assignedVehicleByDriverId?.get(worker.id) ?? '—'}</TableCell>
            {canManage && (
              <TableCell>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Editar trabajador"
                    title="Editar trabajador"
                    onClick={() => onEdit(worker)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  <WorkerDeleteButton workerId={worker.id} workerLabel={worker.fullName} />
                </div>
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
