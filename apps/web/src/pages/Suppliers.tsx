import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Supplier } from '@fleetmgm/api'
import { useSuppliers } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { SupplierTable } from '@/components/supplier/SupplierTable'
import { SupplierFormModal } from '@/components/supplier/SupplierFormModal'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20

export function Suppliers() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingSupplier, setEditingSupplier] = useState<Supplier | undefined>(undefined)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && MANAGEMENT_ROLES.includes(role)

  const { data, isLoading } = useSuppliers(page, PAGE_SIZE)

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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Proveedores</h1>
          <p className="text-on-surface-variant">
            Gestiona los proveedores a los que se les asocian gastos y facturas.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            Nuevo proveedor
          </Button>
        )}
      </div>

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
