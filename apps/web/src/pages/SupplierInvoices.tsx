import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { ExpenseCategory, SupplierInvoice } from '@fleetmgm/api'
import { useSupplierInvoices } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { SupplierInvoiceTable } from '@/components/billing/SupplierInvoiceTable'
import { SupplierInvoiceFormModal } from '@/components/billing/SupplierInvoiceFormModal'
import { EXPENSE_CATEGORY_LABEL } from '@/components/billing/supplier-invoice-shared'

const PAGE_SIZE = 20

const selectClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

export function SupplierInvoices() {
  const [page, setPage] = useState(0)
  const [categoryFilter, setCategoryFilter] = useState<ExpenseCategory | ''>('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingInvoiceId, setEditingInvoiceId] = useState<string | undefined>(undefined)

  const { data, isLoading, isError } = useSupplierInvoices(
    undefined,
    categoryFilter === '' ? undefined : categoryFilter,
    page,
    PAGE_SIZE,
  )

  // Derived from the live query data (not a captured snapshot) so that mutations made while the
  // modal is open are reflected immediately instead of showing stale data.
  const editingInvoice = data?.content.find((invoice) => invoice.id === editingInvoiceId)

  function openCreateForm() {
    setEditingInvoiceId(undefined)
    setFormOpen(true)
  }

  function openEditForm(invoice: SupplierInvoice) {
    setEditingInvoiceId(invoice.id)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Facturas de proveedor</h1>
          <p className="text-on-surface-variant">Gestiona los gastos operativos y sus proveedores.</p>
        </div>
        <div className="flex items-center gap-3">
          <select
            aria-label="Filtrar por categoría"
            className={selectClassName}
            value={categoryFilter}
            onChange={(e) => {
              setCategoryFilter(e.target.value as ExpenseCategory | '')
              setPage(0)
            }}
          >
            <option value="">Todas las categorías</option>
            {(Object.keys(EXPENSE_CATEGORY_LABEL) as ExpenseCategory[]).map((value) => (
              <option key={value} value={value}>
                {EXPENSE_CATEGORY_LABEL[value]}
              </option>
            ))}
          </select>
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            Nueva factura de proveedor
          </Button>
        </div>
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando facturas de proveedores…</p>
      ) : isError ? (
        <p role="alert" className="text-sm text-error">
          No se pudieron cargar los datos.
        </p>
      ) : (
        <SupplierInvoiceTable invoices={data?.content ?? []} onEdit={openEditForm} />
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

      <SupplierInvoiceFormModal
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open)
          if (!open) {
            setEditingInvoiceId(undefined)
          }
        }}
        supplierInvoice={editingInvoice}
      />
    </div>
  )
}
