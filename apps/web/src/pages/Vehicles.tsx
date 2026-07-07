import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Vehicle } from '@fleetmgm/api'
import { useVehicles } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { VehicleTable } from '@/components/vehicle/VehicleTable'
import { VehicleFormModal } from '@/components/vehicle/VehicleFormModal'
import { VehicleAssignmentPanel } from '@/components/assignment/VehicleAssignmentPanel'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Vehicles() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingVehicle, setEditingVehicle] = useState<Vehicle | undefined>(undefined)
  const [assignmentVehicle, setAssignmentVehicle] = useState<Vehicle | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useVehicles(page, PAGE_SIZE)

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

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Vehículos</h1>
          <p className="text-on-surface-variant">
            Gestiona los vehículos y maquinaria pesada de la flota.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            Nuevo vehículo
          </Button>
        )}
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando vehículos…</p>
      ) : (
        <VehicleTable
          vehicles={data?.content ?? []}
          canManage={canManage}
          onEdit={openEditForm}
          onViewAssignment={openAssignmentPanel}
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
    </div>
  )
}
