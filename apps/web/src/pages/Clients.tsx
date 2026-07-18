import { useState } from 'react'
import type { Client } from '@fleetmgm/api'
import { useClients } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { ClientTable } from '@/components/client/ClientTable'
import { ClientFormModal } from '@/components/client/ClientFormModal'
import { ClientFilters } from '@/components/client/ClientFilters'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Clients() {
  const [page, setPage] = useState(0)
  const [taxId, setTaxId] = useState('')
  const [name, setName] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingClient, setEditingClient] = useState<Client | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useClients(
    {
      taxId: taxId === '' ? undefined : taxId,
      name: name === '' ? undefined : name,
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
    setEditingClient(undefined)
    setFormOpen(true)
  }

  function openEditForm(client: Client) {
    setEditingClient(client)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <ClientFilters
        taxId={taxId}
        onTaxIdChange={resetPageAnd(setTaxId)}
        name={name}
        onNameChange={resetPageAnd(setName)}
        onCreate={openCreateForm}
        canCreate={canManage}
      />

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando clientes…</p>
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
        <ClientFormModal open={formOpen} onOpenChange={setFormOpen} client={editingClient} />
      )}
    </div>
  )
}
