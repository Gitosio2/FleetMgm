import { useMemo, useState } from 'react'
import { Plus } from 'lucide-react'
import type { Worker } from '@fleetmgm/api'
import { useActiveAssignmentsByDrivers, useWorkers } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { WorkerTable } from '@/components/worker/WorkerTable'
import { WorkerFormModal } from '@/components/worker/WorkerFormModal'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'
import { formatVehicleLabel } from '@/lib/vehicle-label'

const PAGE_SIZE = 20

export function Workers() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingWorker, setEditingWorker] = useState<Worker | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useWorkers(page, PAGE_SIZE)

  const driverIds = useMemo(() => data?.content.map((worker) => worker.id) ?? [], [data])
  const { data: activeAssignments } = useActiveAssignmentsByDrivers(driverIds)
  const assignedVehicleByDriverId = useMemo(() => {
    const map = new Map<string, string>()
    for (const assignment of activeAssignments ?? []) {
      map.set(assignment.driverId, formatVehicleLabel(assignment))
    }
    return map
  }, [activeAssignments])

  function openCreateForm() {
    setEditingWorker(undefined)
    setFormOpen(true)
  }

  function openEditForm(worker: Worker) {
    setEditingWorker(worker)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Trabajadores</h1>
          <p className="text-on-surface-variant">
            Gestiona los conductores y técnicos de la flota.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            Nuevo trabajador
          </Button>
        )}
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando trabajadores…</p>
      ) : (
        <WorkerTable
          workers={data?.content ?? []}
          canManage={canManage}
          onEdit={openEditForm}
          assignedVehicleByDriverId={assignedVehicleByDriverId}
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
        <WorkerFormModal open={formOpen} onOpenChange={setFormOpen} worker={editingWorker} />
      )}
    </div>
  )
}
