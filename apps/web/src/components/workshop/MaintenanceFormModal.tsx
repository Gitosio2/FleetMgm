import { useEffect, useState, type FormEvent } from 'react'
import type { MaintenanceCategory, MaintenanceRecord, UpdateMaintenanceRequest } from '@fleetmgm/api'
import { useAllVehicles, useAllWorkers, useUpdateMaintenance } from '@fleetmgm/hooks'
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
import { CATEGORY_LABEL, selectClassName, toNullableNumber, toNullableString } from './form-shared'

type MaintenanceFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  record: MaintenanceRecord
}

// Edit-only: creation now happens exclusively from Agenda (ScheduleFormModal), which always
// creates its linked MaintenanceRecord at the same time — see WorkshopScheduleService.create().
export function MaintenanceFormModal({ open, onOpenChange, record }: MaintenanceFormModalProps) {
  const updateMaintenance = useUpdateMaintenance()

  const { data: vehicles = [] } = useAllVehicles()
  const { data: workers = [] } = useAllWorkers()

  const technicians = workers.filter(
    (worker) => worker.workerRole === 'TECHNICIAN' || worker.workerRole === 'BOTH',
  )

  const [vehicleId, setVehicleId] = useState('')
  const [type, setType] = useState('')
  const [description, setDescription] = useState('')
  const [technicianId, setTechnicianId] = useState('')
  const [category, setCategory] = useState<MaintenanceCategory>('PREVENTIVE')
  const [cost, setCost] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setVehicleId(record.vehicleId)
    setType(record.type)
    setDescription(record.description ?? '')
    setTechnicianId(record.technicianId ?? '')
    setCategory(record.category)
    setCost(record.cost != null ? record.cost.toFixed(2) : '')
  }, [open, record])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: UpdateMaintenanceRequest = {
      vehicleId,
      type,
      description: toNullableString(description),
      technicianId: toNullableString(technicianId),
      category,
      cost: toNullableNumber(cost),
    }
    updateMaintenance.mutate({ id: record.id, request }, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Editar orden</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="maintenance-vehicle">Vehículo</Label>
              <select
                id="maintenance-vehicle"
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
              <Label htmlFor="maintenance-technician">Técnico</Label>
              <select
                id="maintenance-technician"
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
              <Label htmlFor="maintenance-type">Tipo</Label>
              <Input id="maintenance-type" value={type} onChange={(e) => setType(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="maintenance-category">Categoría</Label>
              <select
                id="maintenance-category"
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
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="maintenance-description">Descripción</Label>
              <Input
                id="maintenance-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="maintenance-cost">Coste</Label>
              <div className="relative">
                <Input
                  id="maintenance-cost"
                  type="number"
                  min="0"
                  step="0.01"
                  className="pr-8"
                  value={cost}
                  onChange={(e) => setCost(e.target.value)}
                  onBlur={() => setCost((current) => (current === '' ? current : Number(current).toFixed(2)))}
                />
                <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-sm text-on-surface-variant">
                  €
                </span>
              </div>
            </div>
          </div>

          {updateMaintenance.isError && (
            <p role="alert" className="text-sm text-error">
              No se pudo completar la acción.
            </p>
          )}

          <DialogFooter>
            <Button type="submit" disabled={updateMaintenance.isPending}>
              Guardar cambios
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
