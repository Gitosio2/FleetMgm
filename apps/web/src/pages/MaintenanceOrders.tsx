import { useState } from 'react'
import type { MaintenanceCategory, MaintenanceRecord, MaintenanceStatus } from '@fleetmgm/api'
import { useMaintenanceRecords, useVehicles, useWorkers } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { MaintenanceTable } from '@/components/workshop/MaintenanceTable'
import { MaintenanceFormModal } from '@/components/workshop/MaintenanceFormModal'
import { MaintenanceFilters } from '@/components/workshop/MaintenanceFilters'

const PAGE_SIZE = 20

// Read-only registry — creation and the full lifecycle (iniciar/completar/cancelar) live in
// Agenda now. "Editar" is the only action here, for correcting categoría/costo/descripción.
export function MaintenanceOrders() {
  const [page, setPage] = useState(0)
  const [vehicleId, setVehicleId] = useState('')
  const [type, setType] = useState('')
  const [category, setCategory] = useState<MaintenanceCategory | ''>('')
  const [status, setStatus] = useState<MaintenanceStatus | ''>('')
  const [technicianId, setTechnicianId] = useState('')
  const [costFrom, setCostFrom] = useState('')
  const [costTo, setCostTo] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingMaintenance, setEditingMaintenance] = useState<MaintenanceRecord | undefined>(undefined)

  // Same "fetch a large unpaginated page, filter client-side" approach MaintenanceFormModal
  // already uses for its own vehicle/technician selects — keeps both dropdowns (form + filter) in
  // sync without a dedicated unpaginated endpoint.
  const { data: vehiclesPage } = useVehicles({}, 0, 100)
  const vehicles = vehiclesPage?.content ?? []

  const { data: workersPage } = useWorkers({}, 0, 100)
  const technicians = (workersPage?.content ?? []).filter(
    (worker) => worker.workerRole === 'TECHNICIAN' || worker.workerRole === 'BOTH',
  )

  const { data: maintenancePage, isLoading, isError } = useMaintenanceRecords(
    {
      vehicleId: vehicleId === '' ? undefined : vehicleId,
      type: type === '' ? undefined : type,
      category: category === '' ? undefined : category,
      status: status === '' ? undefined : status,
      technicianId: technicianId === '' ? undefined : technicianId,
      costFrom: costFrom === '' ? undefined : Number(costFrom),
      costTo: costTo === '' ? undefined : Number(costTo),
    },
    page,
    PAGE_SIZE,
  )

  function resetPageAnd<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value)
      setPage(0)
    }
  }

  function openEditForm(record: MaintenanceRecord) {
    setEditingMaintenance(record)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <MaintenanceFilters
        vehicleId={vehicleId}
        onVehicleIdChange={resetPageAnd(setVehicleId)}
        vehicles={vehicles}
        type={type}
        onTypeChange={resetPageAnd(setType)}
        category={category}
        onCategoryChange={resetPageAnd(setCategory)}
        status={status}
        onStatusChange={resetPageAnd(setStatus)}
        technicianId={technicianId}
        onTechnicianIdChange={resetPageAnd(setTechnicianId)}
        technicians={technicians}
        costFrom={costFrom}
        onCostFromChange={resetPageAnd(setCostFrom)}
        costTo={costTo}
        onCostToChange={resetPageAnd(setCostTo)}
      />

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando mantenimientos…</p>
      ) : isError ? (
        <p role="alert" className="text-sm text-error">
          No se pudieron cargar los datos.
        </p>
      ) : (
        <MaintenanceTable records={maintenancePage?.content ?? []} onEdit={openEditForm} />
      )}
      {maintenancePage && maintenancePage.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
            Anterior
          </Button>
          <span className="text-sm text-on-surface-variant">
            Página {page + 1} de {maintenancePage.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= maintenancePage.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Siguiente
          </Button>
        </div>
      )}

      {editingMaintenance && (
        <MaintenanceFormModal
          open={formOpen}
          onOpenChange={(open) => {
            setFormOpen(open)
            if (!open) {
              setEditingMaintenance(undefined)
            }
          }}
          record={editingMaintenance}
        />
      )}
    </div>
  )
}
