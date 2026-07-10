import type { WorkshopSchedule } from '@fleetmgm/api'
import { useCancelWorkshopSchedule } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { PRIORITY_LABEL } from './form-shared'
import { ScheduleStatusBadge } from './ScheduleStatusBadge'

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
              <ScheduleStatusBadge status={schedule.status} />
            </TableCell>
            <TableCell>
              <div className="flex flex-col items-start gap-1">
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
                {cancelSchedule.isError && cancelSchedule.variables === schedule.id && (
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
