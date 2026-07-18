import { useState } from 'react'
import type { Vehicle, VehicleCategory, VehicleStatus } from '@fleetmgm/api'
import { useVehicles } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { VehicleTable } from '@/components/vehicle/VehicleTable'
import { VehicleFormModal } from '@/components/vehicle/VehicleFormModal'
import { VehicleFilters } from '@/components/vehicle/VehicleFilters'
import { VehicleAssignmentPanel } from '@/components/assignment/VehicleAssignmentPanel'
import { VehicleProfitabilityPanel } from '@/components/vehicle/VehicleProfitabilityPanel'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Vehicles() {
  const [page, setPage] = useState(0)
  const [category, setCategory] = useState<VehicleCategory | ''>('')
  const [status, setStatus] = useState<VehicleStatus | ''>('')
  const [licensePlate, setLicensePlate] = useState('')
  const [vehicleSearch, setVehicleSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingVehicle, setEditingVehicle] = useState<Vehicle | undefined>(undefined)
  const [assignmentVehicle, setAssignmentVehicle] = useState<Vehicle | undefined>(undefined)
  const [profitabilityVehicle, setProfitabilityVehicle] = useState<Vehicle | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useVehicles(
    {
      category: category === '' ? undefined : category,
      status: status === '' ? undefined : status,
      licensePlate: licensePlate === '' ? undefined : licensePlate,
      vehicle: vehicleSearch === '' ? undefined : vehicleSearch,
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

  function openCreateForm() {
    setEditingVehicle(undefined)
    setFormOpen(true)
  }

  function openEditForm(vehicle: Vehicle) {
    setEditingVehicle(vehicle)
    setFormOpen(true)
  }

  function openAssignmentPanel(vehicle: Vehicle) {
    setAssignmentVehicle(vehicle)
  }

  function openProfitabilityPanel(vehicle: Vehicle) {
    setProfitabilityVehicle(vehicle)
  }

  return (
    <div className="flex flex-col gap-6">
      <VehicleFilters
        category={category}
        onCategoryChange={resetPageAnd(setCategory)}
        status={status}
        onStatusChange={resetPageAnd(setStatus)}
        licensePlate={licensePlate}
        onLicensePlateChange={resetPageAnd(setLicensePlate)}
        vehicle={vehicleSearch}
        onVehicleChange={resetPageAnd(setVehicleSearch)}
        onCreate={openCreateForm}
        canCreate={canManage}
      />

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando vehículos…</p>
      ) : (
        <VehicleTable
          vehicles={data?.content ?? []}
          canManage={canManage}
          onEdit={openEditForm}
          onViewAssignment={openAssignmentPanel}
          onViewProfitability={openProfitabilityPanel}
        />
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Anterior
          </Button>
          <span className="text-sm text-on-surface-variant">
            Página {page + 1} de {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Siguiente
          </Button>
        </div>
      )}

      {canManage && (
        <VehicleFormModal open={formOpen} onOpenChange={setFormOpen} vehicle={editingVehicle} />
      )}

      <Dialog
        open={assignmentVehicle != null}
        onOpenChange={(open) => !open && setAssignmentVehicle(undefined)}
      >
        <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              Asignación — {assignmentVehicle && `${assignmentVehicle.make} ${assignmentVehicle.model}`}
            </DialogTitle>
          </DialogHeader>
          {assignmentVehicle && (
            <VehicleAssignmentPanel
              vehicleId={assignmentVehicle.id}
              vehicleLabel={`${assignmentVehicle.make} ${assignmentVehicle.model}`}
              canManage={canManage}
            />
          )}
        </DialogContent>
      </Dialog>

      <Dialog
        open={profitabilityVehicle != null}
        onOpenChange={(open) => !open && setProfitabilityVehicle(undefined)}
      >
        <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              Rentabilidad — {profitabilityVehicle && `${profitabilityVehicle.make} ${profitabilityVehicle.model}`}
            </DialogTitle>
          </DialogHeader>
          {profitabilityVehicle && (
            <VehicleProfitabilityPanel
              vehicleId={profitabilityVehicle.id}
              vehicleLabel={`${profitabilityVehicle.make} ${profitabilityVehicle.model}`}
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
