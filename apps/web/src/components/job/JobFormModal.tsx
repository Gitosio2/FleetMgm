import { useEffect, useState, type FormEvent } from 'react'
import type { CreateJobRequest, Job } from '@fleetmgm/api'
import { useClients, useCreateJob, useUpdateJob, useVehicles, useWorkers } from '@fleetmgm/hooks'
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

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

type JobFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  job?: Job
}

function toNullableString(value: string): string | null {
  return value === '' ? null : value
}

function toIsoOrNull(value: string): string | null {
  return value === '' ? null : new Date(value).toISOString()
}

export function JobFormModal({ open, onOpenChange, job }: JobFormModalProps) {
  const isEditing = job != null
  const createJob = useCreateJob()
  const updateJob = useUpdateJob()

  const { data: vehiclesPage } = useVehicles(0, 100)
  const { data: workersPage } = useWorkers(0, 100)
  const { data: clientsPage } = useClients(0, 100)

  const drivers = (workersPage?.content ?? []).filter(
    (worker) => worker.workerRole === 'DRIVER' || worker.workerRole === 'BOTH',
  )

  const [vehicleId, setVehicleId] = useState('')
  const [assignedDriverId, setAssignedDriverId] = useState('')
  const [clientId, setClientId] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [originLocation, setOriginLocation] = useState('')
  const [destinationLocation, setDestinationLocation] = useState('')
  const [notes, setNotes] = useState('')
  const [scheduledStart, setScheduledStart] = useState('')
  const [scheduledEnd, setScheduledEnd] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setVehicleId(job?.vehicleId ?? '')
    setAssignedDriverId(job?.assignedDriverId ?? '')
    setClientId(job?.clientId ?? '')
    setTitle(job?.title ?? '')
    setDescription(job?.description ?? '')
    setOriginLocation(job?.originLocation ?? '')
    setDestinationLocation(job?.destinationLocation ?? '')
    setNotes(job?.notes ?? '')
    setScheduledStart(job?.scheduledStart?.slice(0, 16) ?? '')
    setScheduledEnd(job?.scheduledEnd?.slice(0, 16) ?? '')
  }, [open, job])

  const isPending = createJob.isPending || updateJob.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: CreateJobRequest = {
      vehicleId,
      assignedDriverId: toNullableString(assignedDriverId),
      clientId: toNullableString(clientId),
      title,
      description: toNullableString(description),
      originLocation,
      destinationLocation,
      notes: toNullableString(notes),
      scheduledStart: toIsoOrNull(scheduledStart),
      scheduledEnd: toIsoOrNull(scheduledEnd),
    }

    if (job) {
      updateJob.mutate({ id: job.id, request }, { onSuccess: () => onOpenChange(false) })
      return
    }

    createJob.mutate(request, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Editar trabajo' : 'Nuevo trabajo'}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="job-title">Título</Label>
            <Input id="job-title" value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="job-vehicle">Vehículo</Label>
              <select
                id="job-vehicle"
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
              <Label htmlFor="job-driver">Conductor</Label>
              <select
                id="job-driver"
                className={selectClassName}
                value={assignedDriverId}
                onChange={(e) => setAssignedDriverId(e.target.value)}
              >
                <option value="">Sin asignar</option>
                {drivers.map((driver) => (
                  <option key={driver.id} value={driver.id}>
                    {driver.fullName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="job-client">Cliente</Label>
            <select
              id="job-client"
              className={selectClassName}
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
            >
              <option value="">Sin cliente</option>
              {(clientsPage?.content ?? []).map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="job-origin">Origen</Label>
              <Input
                id="job-origin"
                value={originLocation}
                onChange={(e) => setOriginLocation(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="job-destination">Destino</Label>
              <Input
                id="job-destination"
                value={destinationLocation}
                onChange={(e) => setDestinationLocation(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="job-scheduled-start">Inicio previsto</Label>
              <Input
                id="job-scheduled-start"
                type="datetime-local"
                value={scheduledStart}
                onChange={(e) => setScheduledStart(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="job-scheduled-end">Fin previsto</Label>
              <Input
                id="job-scheduled-end"
                type="datetime-local"
                value={scheduledEnd}
                onChange={(e) => setScheduledEnd(e.target.value)}
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="job-description">Descripción</Label>
            <Input id="job-description" value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="job-notes">Notas</Label>
            <Input id="job-notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={isPending}>
              {isEditing ? 'Guardar cambios' : 'Crear trabajo'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
