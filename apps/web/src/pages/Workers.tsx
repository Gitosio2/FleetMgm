import { useMemo, useState } from 'react'
import type { Worker, WorkerRole } from '@fleetmgm/api'
import { useActiveAssignmentsByDrivers, useWorkers } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { WorkerTable } from '@/components/worker/WorkerTable'
import { WorkerFormModal } from '@/components/worker/WorkerFormModal'
import { WorkerFilters } from '@/components/worker/WorkerFilters'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'
import { formatVehicleLabel } from '@/lib/vehicle-label'

const PAGE_SIZE = 20

export function Workers() {
  const [page, setPage] = useState(0)
  const [name, setName] = useState('')
  const [nationalId, setNationalId] = useState('')
  const [workerRole, setWorkerRole] = useState<WorkerRole | ''>('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingWorker, setEditingWorker] = useState<Worker | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useWorkers(
    {
      name: name === '' ? undefined : name,
      nationalId: nationalId === '' ? undefined : nationalId,
      workerRole: workerRole === '' ? undefined : workerRole,
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
      <WorkerFilters
        name={name}
        onNameChange={resetPageAnd(setName)}
        nationalId={nationalId}
        onNationalIdChange={resetPageAnd(setNationalId)}
        workerRole={workerRole}
        onWorkerRoleChange={resetPageAnd(setWorkerRole)}
        onCreate={openCreateForm}
        canCreate={canManage}
      />

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
