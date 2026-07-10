import type { SchedulePriority, WorkshopSchedule, WorkshopStatus } from '@fleetmgm/api'
import { useCancelWorkshopSchedule } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { cn } from '@/lib/utils'
import { formatVehicleLabel } from '@/lib/vehicle-label'

const STATUS_LABEL: Record<WorkshopStatus, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
}

const STATUS_CLASSNAME: Record<WorkshopStatus, string> = {
  PENDING: 'bg-surface-container-high text-on-surface-variant',
  IN_PROGRESS: 'bg-tertiary-container/40 text-tertiary',
  COMPLETED: 'bg-secondary-container/20 text-secondary',
  CANCELLED: 'bg-error-container/40 text-error',
}

const PRIORITY_LABEL: Record<SchedulePriority, string> = {
  LOW: 'Baja',
  MEDIUM: 'Media',
  HIGH: 'Alta',
  URGENT: 'Urgente',
}

type ScheduleTableProps = {
  schedules: WorkshopSchedule[]
}

export function ScheduleTable({ schedules }: ScheduleTableProps) {
  const cancelSchedule = useCancelWorkshopSchedule()

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vehículo</TableHead>
          <TableHead>Tipo</TableHead>
          <TableHead>Fecha</TableHead>
          <TableHead>Prioridad</TableHead>
          <TableHead>Técnico</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead>Acciones</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {schedules.map((schedule) => (
          <TableRow key={schedule.id}>
            <TableCell>{formatVehicleLabel(schedule)}</TableCell>
            <TableCell>{schedule.type}</TableCell>
            <TableCell>{schedule.scheduledDate}</TableCell>
            <TableCell>{PRIORITY_LABEL[schedule.priority]}</TableCell>
            <TableCell>{schedule.technicianName ?? '—'}</TableCell>
            <TableCell>
              <span
                className={cn(
                  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
                  STATUS_CLASSNAME[schedule.status],
                )}
              >
                {STATUS_LABEL[schedule.status]}
              </span>
            </TableCell>
            <TableCell>
              {(schedule.status === 'PENDING' || schedule.status === 'IN_PROGRESS') && (
                <Button
                  variant="outline"
                  size="sm"
                  disabled={cancelSchedule.isPending}
                  onClick={() => cancelSchedule.mutate(schedule.id)}
                >
                  Cancelar
                </Button>
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
