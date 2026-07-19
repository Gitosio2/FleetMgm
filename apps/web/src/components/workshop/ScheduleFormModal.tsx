import { useEffect, useState, type FormEvent } from 'react'
import type {
  CreateScheduleRequest,
  MaintenanceCategory,
  SchedulePriority,
  UpdateScheduleRequest,
  WorkshopSchedule,
} from '@fleetmgm/api'
import { useAllVehicles, useAllWorkers, useCreateWorkshopSchedule, useUpdateWorkshopSchedule } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { CATEGORY_LABEL, PRIORITY_LABEL, selectClassName, toNullableString, toNullableTime, toTimeInputValue } from './form-shared'

type ScheduleFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  schedule?: WorkshopSchedule
}

export function ScheduleFormModal({ open, onOpenChange, schedule }: ScheduleFormModalProps) {
  const isEditing = schedule != null
  const createSchedule = useCreateWorkshopSchedule()
  const updateSchedule = useUpdateWorkshopSchedule()

  const { data: vehicles = [] } = useAllVehicles()
  const { data: workers = [] } = useAllWorkers()

  const technicians = workers.filter(
    (worker) => worker.workerRole === 'TECHNICIAN' || worker.workerRole === 'BOTH',
  )

  const [vehicleId, setVehicleId] = useState('')
  const [type, setType] = useState('')
  const [scheduledDate, setScheduledDate] = useState('')
  const [scheduledStartTime, setScheduledStartTime] = useState('')
  const [scheduledEndTime, setScheduledEndTime] = useState('')
  const [priority, setPriority] = useState<SchedulePriority>('MEDIUM')
  const [technicianId, setTechnicianId] = useState('')
  const [notes, setNotes] = useState('')
  const [category, setCategory] = useState<MaintenanceCategory>('PREVENTIVE')
  const [timeRangeError, setTimeRangeError] = useState(false)

  useEffect(() => {
    if (!open) {
      return
    }
    setVehicleId(schedule?.vehicleId ?? '')
    setType(schedule?.type ?? '')
    setScheduledDate(schedule?.scheduledDate ?? '')
    setScheduledStartTime(toTimeInputValue(schedule?.scheduledStartTime ?? null))
    setScheduledEndTime(toTimeInputValue(schedule?.scheduledEndTime ?? null))
    setPriority(schedule?.priority ?? 'MEDIUM')
    setTechnicianId(schedule?.technicianId ?? '')
    setNotes(schedule?.notes ?? '')
    setCategory('PREVENTIVE')
    setTimeRangeError(false)
  }, [open, schedule])

  const isPending = createSchedule.isPending || updateSchedule.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTimeRangeError(false)

    // Only meaningful when both are provided — a single-sided range is not validated, mirroring
    // the backend's SCHEDULE_INVALID_TIME_RANGE rule (Hito 28).
    if (scheduledStartTime && scheduledEndTime && scheduledEndTime <= scheduledStartTime) {
      setTimeRangeError(true)
      return
    }

    if (schedule) {
      const request: UpdateScheduleRequest = {
        vehicleId,
        type,
        scheduledDate,
        scheduledStartTime: toNullableTime(scheduledStartTime),
        scheduledEndTime: toNullableTime(scheduledEndTime),
        priority,
        technicianId: toNullableString(technicianId),
        maintenanceRecordId: schedule.maintenanceRecordId,
        notes: toNullableString(notes),
      }
      updateSchedule.mutate({ id: schedule.id, request }, { onSuccess: () => onOpenChange(false) })
      return
    }

    const request: CreateScheduleRequest = {
      vehicleId,
      type,
      scheduledDate,
      scheduledStartTime: toNullableTime(scheduledStartTime),
      scheduledEndTime: toNullableTime(scheduledEndTime),
      priority,
      technicianId: toNullableString(technicianId),
      notes: toNullableString(notes),
      category,
    }

    createSchedule.mutate(request, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Editar entrada' : 'Nueva entrada'}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="schedule-vehicle">Vehículo</Label>
              <select
                id="schedule-vehicle"
                className={selectClassName}
                value={vehicleId}
                onChange={(e) => setVehicleId(e.target.value)}
                required
              >
                <option value="" disabled>
                  Seleccioná un vehículo
                </option>
                {vehicles.map((vehicle) => (
                  <option key={vehicle.id} value={vehicle.id}>
                    {vehicle.make} {vehicle.model}
                    {vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="schedule-technician">Técnico</Label>
              <select
                id="schedule-technician"
                className={selectClassName}
                value={technicianId}
                onChange={(e) => setTechnicianId(e.target.value)}
              >
                <option value="">Sin asignar</option>
                {technicians.map((technician) => (
                  <option key={technician.id} value={technician.id}>
                    {technician.fullName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="schedule-type">Tipo</Label>
              <Input id="schedule-type" value={type} onChange={(e) => setType(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="schedule-priority">Prioridad</Label>
              <select
                id="schedule-priority"
                className={selectClassName}
                value={priority}
                onChange={(e) => setPriority(e.target.value as SchedulePriority)}
                required
              >
                {(Object.keys(PRIORITY_LABEL) as SchedulePriority[]).map((value) => (
                  <option key={value} value={value}>
                    {PRIORITY_LABEL[value]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {!isEditing && (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="schedule-category">Categoría</Label>
              <select
                id="schedule-category"
                className={selectClassName}
                value={category}
                onChange={(e) => setCategory(e.target.value as MaintenanceCategory)}
                required
              >
                {(Object.keys(CATEGORY_LABEL) as MaintenanceCategory[]).map((value) => (
                  <option key={value} value={value}>
                    {CATEGORY_LABEL[value]}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="schedule-date">Fecha</Label>
            <Input
              id="schedule-date"
              type="date"
              value={scheduledDate}
              onChange={(e) => setScheduledDate(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="schedule-start-time">Hora de inicio</Label>
              <Input
                id="schedule-start-time"
                type="time"
                value={scheduledStartTime}
                onChange={(e) => setScheduledStartTime(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="schedule-end-time">Hora de fin</Label>
              <Input
                id="schedule-end-time"
                type="time"
                value={scheduledEndTime}
                onChange={(e) => setScheduledEndTime(e.target.value)}
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="schedule-notes">Notas</Label>
            <Input id="schedule-notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>

          {timeRangeError && (
            <p role="alert" className="text-sm text-error">
              La hora de fin debe ser posterior a la hora de inicio.
            </p>
          )}

          {(createSchedule.isError || updateSchedule.isError) && (
            <p role="alert" className="text-sm text-error">
              No se pudo completar la acción.
            </p>
          )}

          <DialogFooter>
            <Button type="submit" disabled={isPending}>
              {isEditing ? 'Guardar cambios' : 'Crear entrada'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
