import { useState } from 'react'
import type { Job, JobStatus } from '@fleetmgm/api'
import { useAllVehicles, useAllWorkers, useJobs } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { JobTable } from '@/components/job/JobTable'
import { JobFormModal } from '@/components/job/JobFormModal'
import { JobFilters } from '@/components/job/JobFilters'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Jobs() {
  const [page, setPage] = useState(0)
  const [title, setTitle] = useState('')
  const [originLocation, setOriginLocation] = useState('')
  const [destinationLocation, setDestinationLocation] = useState('')
  const [vehicleId, setVehicleId] = useState('')
  const [assignedDriverId, setAssignedDriverId] = useState('')
  const [status, setStatus] = useState<JobStatus | ''>('')
  const [actualStartFrom, setActualStartFrom] = useState('')
  const [actualStartTo, setActualStartTo] = useState('')
  const [actualEndFrom, setActualEndFrom] = useState('')
  const [actualEndTo, setActualEndTo] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingJob, setEditingJob] = useState<Job | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  // Same shared "fetch every page" hooks JobFormModal already uses for its own vehicle/driver
  // selects — keeps both dropdowns (form + filter) in sync, and every vehicle/driver selectable
  // regardless of fleet size (see useAllVehicles/useAllWorkers in packages/hooks).
  const { data: vehicles = [] } = useAllVehicles()

  const { data: workers = [] } = useAllWorkers()
  const drivers = workers.filter((worker) => worker.workerRole === 'DRIVER' || worker.workerRole === 'BOTH')

  const { data, isLoading, isError } = useJobs(
    {
      title: title === '' ? undefined : title,
      originLocation: originLocation === '' ? undefined : originLocation,
      destinationLocation: destinationLocation === '' ? undefined : destinationLocation,
      vehicleId: vehicleId === '' ? undefined : vehicleId,
      assignedDriverId: assignedDriverId === '' ? undefined : assignedDriverId,
      status: status === '' ? undefined : status,
      actualStartFrom: actualStartFrom === '' ? undefined : `${actualStartFrom}T00:00:00Z`,
      actualStartTo: actualStartTo === '' ? undefined : `${actualStartTo}T23:59:59Z`,
      actualEndFrom: actualEndFrom === '' ? undefined : `${actualEndFrom}T00:00:00Z`,
      actualEndTo: actualEndTo === '' ? undefined : `${actualEndTo}T23:59:59Z`,
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
    setEditingJob(undefined)
    setFormOpen(true)
  }

  function openEditForm(job: Job) {
    setEditingJob(job)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <JobFilters
        title={title}
        onTitleChange={resetPageAnd(setTitle)}
        originLocation={originLocation}
        onOriginLocationChange={resetPageAnd(setOriginLocation)}
        destinationLocation={destinationLocation}
        onDestinationLocationChange={resetPageAnd(setDestinationLocation)}
        vehicleId={vehicleId}
        onVehicleIdChange={resetPageAnd(setVehicleId)}
        vehicles={vehicles}
        assignedDriverId={assignedDriverId}
        onAssignedDriverIdChange={resetPageAnd(setAssignedDriverId)}
        drivers={drivers}
        status={status}
        onStatusChange={resetPageAnd(setStatus)}
        actualStartFrom={actualStartFrom}
        onActualStartFromChange={resetPageAnd(setActualStartFrom)}
        actualStartTo={actualStartTo}
        onActualStartToChange={resetPageAnd(setActualStartTo)}
        actualEndFrom={actualEndFrom}
        onActualEndFromChange={resetPageAnd(setActualEndFrom)}
        actualEndTo={actualEndTo}
        onActualEndToChange={resetPageAnd(setActualEndTo)}
        onCreate={openCreateForm}
        canCreate={canManage}
      />

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando trabajos…</p>
      ) : isError ? (
        <p role="alert" className="text-sm text-error">
          No se pudieron cargar los datos.
        </p>
      ) : (
        <JobTable jobs={data?.content ?? []} canManage={canManage} onEdit={openEditForm} />
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

      {canManage && <JobFormModal open={formOpen} onOpenChange={setFormOpen} job={editingJob} />}
    </div>
  )
}
