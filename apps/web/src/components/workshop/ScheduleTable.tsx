import { Check, Pencil, Play, X } from 'lucide-react'
import type { WorkshopSchedule } from '@fleetmgm/api'
import { useCancelWorkshopSchedule, useCompleteWorkshopSchedule, useStartWorkshopSchedule } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { CATEGORY_LABEL, PRIORITY_LABEL } from './form-shared'
import { ScheduleStatusBadge } from './ScheduleStatusBadge'

type ScheduleTableProps = {
  schedules: WorkshopSchedule[]
  onEdit: (schedule: WorkshopSchedule) => void
}

export function ScheduleTable({ schedules, onEdit }: ScheduleTableProps) {
  const startSchedule = useStartWorkshopSchedule()
  const completeSchedule = useCompleteWorkshopSchedule()
  const cancelSchedule = useCancelWorkshopSchedule()

  const isPending = startSchedule.isPending || completeSchedule.isPending || cancelSchedule.isPending

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vehículo</TableHead>
          <TableHead>Tipo</TableHead>
          <TableHead>Categoría</TableHead>
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
            <TableCell>
              {schedule.maintenanceCategory ? CATEGORY_LABEL[schedule.maintenanceCategory] : '—'}
            </TableCell>
            <TableCell>{schedule.scheduledDate}</TableCell>
            <TableCell>{PRIORITY_LABEL[schedule.priority]}</TableCell>
            <TableCell>{schedule.technicianName ?? '—'}</TableCell>
            <TableCell>
              <ScheduleStatusBadge status={schedule.status} />
            </TableCell>
            <TableCell>
              <div className="flex flex-col items-start gap-1">
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Editar entrada"
                    title="Editar entrada"
                    onClick={() => onEdit(schedule)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  {schedule.status === 'PENDING' && (
                    <Button
                      variant="success"
                      size="sm"
                      aria-label="Iniciar entrada"
                      title="Iniciar entrada"
                      disabled={isPending}
                      onClick={() => startSchedule.mutate(schedule.id)}
                    >
                      <Play className="size-4" />
                    </Button>
                  )}
                  {schedule.status === 'IN_PROGRESS' && (
                    <Button
                      variant="secondary"
                      size="sm"
                      aria-label="Completar entrada"
                      title="Completar entrada"
                      disabled={isPending}
                      onClick={() => completeSchedule.mutate(schedule.id)}
                    >
                      <Check className="size-4" />
                    </Button>
                  )}
                  {(schedule.status === 'PENDING' || schedule.status === 'IN_PROGRESS') && (
                    <Button
                      variant="destructive"
                      size="sm"
                      aria-label="Cancelar entrada"
                      title="Cancelar entrada"
                      disabled={isPending}
                      onClick={() => cancelSchedule.mutate(schedule.id)}
                    >
                      <X className="size-4" />
                    </Button>
                  )}
                </div>
                {startSchedule.isError && startSchedule.variables === schedule.id && (
                  <p role="alert" className="text-sm text-error">
                    No se pudo completar la acción.
                  </p>
                )}
                {completeSchedule.isError && completeSchedule.variables === schedule.id && (
                  <p role="alert" className="text-sm text-error">
                    No se pudo completar la acción.
                  </p>
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
