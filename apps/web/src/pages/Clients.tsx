import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Client } from '@fleetmgm/api'
import { useClients } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { ClientTable } from '@/components/client/ClientTable'
import { ClientFormModal } from '@/components/client/ClientFormModal'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Clients() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingClient, setEditingClient] = useState<Client | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useClients(page, PAGE_SIZE)

  function openCreateForm() {
    setEditingClient(undefined)
    setFormOpen(true)
  }

  function openEditForm(client: Client) {
    setEditingClient(client)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Clients</h1>
          <p className="text-on-surface-variant">
            Manage the clients billed through completed jobs.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            New client
          </Button>
        )}
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Loading clients…</p>
      ) : (
        <ClientTable clients={data?.content ?? []} canManage={canManage} onEdit={openEditForm} />
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
        <ClientFormModal open={formOpen} onOpenChange={setFormOpen} client={editingClient} />
      )}
    </div>
  )
}
