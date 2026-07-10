import type { MaintenanceCategory, MaintenanceRecord, MaintenanceStatus } from '@fleetmgm/api'
import { useCompleteMaintenance, useStartMaintenance } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { cn } from '@/lib/utils'
import { formatVehicleLabel } from '@/lib/vehicle-label'

const STATUS_LABEL: Record<MaintenanceStatus, string> = {
  SCHEDULED: 'Programado',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado',
}

const STATUS_CLASSNAME: Record<MaintenanceStatus, string> = {
  SCHEDULED: 'bg-surface-container-high text-on-surface-variant',
  IN_PROGRESS: 'bg-tertiary-container/40 text-tertiary',
  COMPLETED: 'bg-secondary-container/20 text-secondary',
}

const CATEGORY_LABEL: Record<MaintenanceCategory, string> = {
  PREVENTIVE: 'Preventivo',
  CORRECTIVE: 'Correctivo',
}

type MaintenanceTableProps = {
  records: MaintenanceRecord[]
}

export function MaintenanceTable({ records }: MaintenanceTableProps) {
  const startMaintenance = useStartMaintenance()
  const completeMaintenance = useCompleteMaintenance()

  const isPending = startMaintenance.isPending || completeMaintenance.isPending

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
              <span
                className={cn(
                  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
                  STATUS_CLASSNAME[record.status],
                )}
              >
                {STATUS_LABEL[record.status]}
              </span>
            </TableCell>
            <TableCell>
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
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
