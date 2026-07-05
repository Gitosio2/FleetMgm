import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Vehicle } from '@fleetmgm/api'
import { useVehicles } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { VehicleTable } from '@/components/vehicle/VehicleTable'
import { VehicleFormModal } from '@/components/vehicle/VehicleFormModal'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Vehicles() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingVehicle, setEditingVehicle] = useState<Vehicle | undefined>(undefined)

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

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Vehicles</h1>
          <p className="text-on-surface-variant">
            Manage the fleet's vehicles and heavy machinery.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            New vehicle
          </Button>
        )}
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Loading vehicles…</p>
      ) : (
        <VehicleTable vehicles={data?.content ?? []} canManage={canManage} onEdit={openEditForm} />
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Previous
          </Button>
          <span className="text-sm text-on-surface-variant">
            Page {page + 1} of {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </Button>
        </div>
      )}

      {canManage && (
        <VehicleFormModal open={formOpen} onOpenChange={setFormOpen} vehicle={editingVehicle} />
      )}
    </div>
  )
}
