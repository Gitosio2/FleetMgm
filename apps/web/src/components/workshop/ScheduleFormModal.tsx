import { useEffect, useState, type FormEvent } from 'react'
import type { CreateScheduleRequest, SchedulePriority } from '@fleetmgm/api'
import { useCreateWorkshopSchedule, useVehicles, useWorkers } from '@fleetmgm/hooks'
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
import { PRIORITY_LABEL, selectClassName, toNullableString } from './form-shared'

type ScheduleFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ScheduleFormModal({ open, onOpenChange }: ScheduleFormModalProps) {
  const createSchedule = useCreateWorkshopSchedule()

  const { data: vehiclesPage } = useVehicles(0, 100)
  const { data: workersPage } = useWorkers(0, 100)

  const technicians = (workersPage?.content ?? []).filter(
    (worker) => worker.workerRole === 'TECHNICIAN' || worker.workerRole === 'BOTH',
  )

  const [vehicleId, setVehicleId] = useState('')
  const [type, setType] = useState('')
  const [scheduledDate, setScheduledDate] = useState('')
  const [priority, setPriority] = useState<SchedulePriority>('MEDIUM')
  const [technicianId, setTechnicianId] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setVehicleId('')
    setType('')
    setScheduledDate('')
    setPriority('MEDIUM')
    setTechnicianId('')
    setNotes('')
  }, [open])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: CreateScheduleRequest = {
      vehicleId,
      type,
      scheduledDate,
      priority,
      technicianId: toNullableString(technicianId),
      notes: toNullableString(notes),
    }

    createSchedule.mutate(request, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Nueva entrada</DialogTitle>
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
                {(vehiclesPage?.content ?? []).map((vehicle) => (
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

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="schedule-notes">Notas</Label>
            <Input id="schedule-notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>

          {createSchedule.isError && (
            <p role="alert" className="text-sm text-error">
              No se pudo completar la acción.
            </p>
          )}

          <DialogFooter>
            <Button type="submit" disabled={createSchedule.isPending}>
              Crear entrada
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
