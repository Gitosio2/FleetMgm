import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Job } from '@fleetmgm/api'
import { useJobs } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { JobTable } from '@/components/job/JobTable'
import { JobFormModal } from '@/components/job/JobFormModal'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Jobs() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingJob, setEditingJob] = useState<Job | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading, isError } = useJobs(page, PAGE_SIZE)

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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Trabajos</h1>
          <p className="text-on-surface-variant">Gestiona los trabajos y su ciclo de vida.</p>
        </div>
        {canManage && (
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            Nuevo trabajo
          </Button>
        )}
      </div>

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
