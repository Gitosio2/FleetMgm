import { Pencil } from 'lucide-react'
import type { MaintenanceCategory, MaintenanceRecord } from '@fleetmgm/api'
import { useCancelMaintenance, useCompleteMaintenance, useStartMaintenance } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { MaintenanceStatusBadge } from './MaintenanceStatusBadge'

const CATEGORY_LABEL: Record<MaintenanceCategory, string> = {
  PREVENTIVE: 'Preventivo',
  CORRECTIVE: 'Correctivo',
}

type MaintenanceTableProps = {
  records: MaintenanceRecord[]
  onEdit: (record: MaintenanceRecord) => void
}

export function MaintenanceTable({ records, onEdit }: MaintenanceTableProps) {
  const startMaintenance = useStartMaintenance()
  const completeMaintenance = useCompleteMaintenance()
  const cancelMaintenance = useCancelMaintenance()

  const isPending = startMaintenance.isPending || completeMaintenance.isPending || cancelMaintenance.isPending

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vehículo</TableHead>
          <TableHead>Tipo</TableHead>
          <TableHead>Categoría</TableHead>
          <TableHead>Técnico</TableHead>
          <TableHead>Estado</TableHead>
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
            <TableCell>
              <div className="flex flex-col items-start gap-1">
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Editar orden"
                    onClick={() => onEdit(record)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  {record.status === 'SCHEDULED' && (
                    <Button
                      variant="default"
                      size="sm"
                      disabled={isPending}
                      onClick={() => startMaintenance.mutate({ id: record.id, usageAtService: null })}
                    >
                      Iniciar
                    </Button>
                  )}
                  {record.status === 'IN_PROGRESS' && (
                    <Button
                      variant="default"
                      size="sm"
                      disabled={isPending}
                      onClick={() => completeMaintenance.mutate({ id: record.id, cost: null })}
                    >
                      Completar
                    </Button>
                  )}
                  {(record.status === 'SCHEDULED' || record.status === 'IN_PROGRESS') && (
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={isPending}
                      onClick={() => cancelMaintenance.mutate(record.id)}
                    >
                      Cancelar
                    </Button>
                  )}
                </div>
                {startMaintenance.isError && startMaintenance.variables?.id === record.id && (
                  <p role="alert" className="text-sm text-error">
                    No se pudo completar la acción.
                  </p>
                )}
                {completeMaintenance.isError && completeMaintenance.variables?.id === record.id && (
                  <p role="alert" className="text-sm text-error">
                    No se pudo completar la acción.
                  </p>
                )}
                {cancelMaintenance.isError && cancelMaintenance.variables === record.id && (
                  <p role="alert" className="text-sm text-error">
                    No se pudo completar la acción.
                  </p>
                )}
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
