import { Pencil } from 'lucide-react'
import type { MaintenanceRecord } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatCurrency } from '@/lib/currency'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { CATEGORY_LABEL } from './form-shared'
import { MaintenanceStatusBadge } from './MaintenanceStatusBadge'

type MaintenanceTableProps = {
  records: MaintenanceRecord[]
  onEdit: (record: MaintenanceRecord) => void
}

// Read-only registry: the full lifecycle (crear/iniciar/completar/cancelar) lives in Agenda
// (ScheduleTable) now — every schedule entry always has a linked order. "Editar" stays here since
// it corrects fields Agenda's form doesn't carry (categoría, costo, descripción).
export function MaintenanceTable({ records, onEdit }: MaintenanceTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vehículo</TableHead>
          <TableHead>Tipo</TableHead>
          <TableHead>Categoría</TableHead>
          <TableHead>Técnico</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead>Coste</TableHead>
          <TableHead>Acciones</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {records.map((record) => (
          <TableRow key={record.id}>
            <TableCell>{formatVehicleLabel(record)}</TableCell>
            <TableCell>{record.type}</TableCell>
            <TableCell>{CATEGORY_LABEL[record.category]}</TableCell>
            <TableCell>{record.technicianName ?? '—'}</TableCell>
            <TableCell>
              <MaintenanceStatusBadge status={record.status} />
            </TableCell>
            <TableCell>{record.cost != null ? formatCurrency(record.cost) : '—'}</TableCell>
            <TableCell>
              <Button
                variant="ghost"
                size="sm"
                aria-label="Editar orden"
                title="Editar orden"
                onClick={() => onEdit(record)}
              >
                <Pencil className="size-4" />
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
