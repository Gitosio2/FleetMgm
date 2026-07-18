import { useState } from 'react'
import type { Supplier } from '@fleetmgm/api'
import { useSuppliers } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { SupplierTable } from '@/components/supplier/SupplierTable'
import { SupplierFormModal } from '@/components/supplier/SupplierFormModal'
import { SupplierFilters } from '@/components/supplier/SupplierFilters'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Suppliers() {
  const [page, setPage] = useState(0)
  const [taxId, setTaxId] = useState('')
  const [name, setName] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingSupplier, setEditingSupplier] = useState<Supplier | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useSuppliers(
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
    setEditingSupplier(undefined)
    setFormOpen(true)
  }

  function openEditForm(supplier: Supplier) {
    setEditingSupplier(supplier)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <SupplierFilters
        taxId={taxId}
        onTaxIdChange={resetPageAnd(setTaxId)}
        name={name}
        onNameChange={resetPageAnd(setName)}
        onCreate={openCreateForm}
        canCreate={canManage}
      />

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando proveedores…</p>
      ) : (
        <SupplierTable suppliers={data?.content ?? []} canManage={canManage} onEdit={openEditForm} />
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
        <SupplierFormModal open={formOpen} onOpenChange={setFormOpen} supplier={editingSupplier} />
      )}
    </div>
  )
}
