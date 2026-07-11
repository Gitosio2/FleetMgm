import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { ExpenseCategory, Invoice, SupplierInvoice } from '@fleetmgm/api'
import { useInvoices, useSupplierInvoices } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { InvoiceTable } from '@/components/billing/InvoiceTable'
import { InvoiceFormModal } from '@/components/billing/InvoiceFormModal'
import { SupplierInvoiceTable } from '@/components/billing/SupplierInvoiceTable'
import { SupplierInvoiceFormModal } from '@/components/billing/SupplierInvoiceFormModal'
import { EXPENSE_CATEGORY_LABEL } from '@/components/billing/supplier-invoice-shared'

const PAGE_SIZE = 20

const selectClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

export function Billing() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingInvoiceId, setEditingInvoiceId] = useState<string | undefined>(undefined)

  const [supplierPage, setSupplierPage] = useState(0)
  const [categoryFilter, setCategoryFilter] = useState<ExpenseCategory | ''>('')
  const [supplierFormOpen, setSupplierFormOpen] = useState(false)
  const [editingSupplierInvoiceId, setEditingSupplierInvoiceId] = useState<string | undefined>(undefined)

  const { data, isLoading, isError } = useInvoices(page, PAGE_SIZE)
  const {
    data: supplierData,
    isLoading: supplierLoading,
    isError: supplierError,
  } = useSupplierInvoices(undefined, categoryFilter === '' ? undefined : categoryFilter, supplierPage, PAGE_SIZE)

  // Derived from the live query data (not a captured snapshot) so that mutations made while the
  // modal is open — e.g. adding a line item, which invalidates and refetches the invoice list —
  // are reflected immediately in the modal's LineItemList instead of showing stale line items.
  const editingInvoice = data?.content.find((invoice) => invoice.id === editingInvoiceId)
  const editingSupplierInvoice = supplierData?.content.find(
    (invoice) => invoice.id === editingSupplierInvoiceId,
  )

  function openCreateForm() {
    setEditingInvoiceId(undefined)
    setFormOpen(true)
  }

  function openEditForm(invoice: Invoice) {
    setEditingInvoiceId(invoice.id)
    setFormOpen(true)
  }

  function openCreateSupplierForm() {
    setEditingSupplierInvoiceId(undefined)
    setSupplierFormOpen(true)
  }

  function openEditSupplierForm(invoice: SupplierInvoice) {
    setEditingSupplierInvoiceId(invoice.id)
    setSupplierFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Facturación a clientes</h1>
          <p className="text-on-surface-variant">Gestiona las facturas de clientes y su ciclo de vida.</p>
        </div>
        <Button onClick={openCreateForm}>
          <Plus className="size-4" />
          Nueva factura
        </Button>
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando facturas…</p>
      ) : isError ? (
        <p role="alert" className="text-sm text-error">
          No se pudieron cargar los datos.
        </p>
      ) : (
        <InvoiceTable invoices={data?.content ?? []} onEdit={openEditForm} />
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

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold">Gastos de proveedores</h2>
          <div className="flex items-center gap-3">
            <select
              aria-label="Filtrar por categoría"
              className={selectClassName}
              value={categoryFilter}
              onChange={(e) => {
                setCategoryFilter(e.target.value as ExpenseCategory | '')
                setSupplierPage(0)
              }}
            >
              <option value="">Todas las categorías</option>
              {(Object.keys(EXPENSE_CATEGORY_LABEL) as ExpenseCategory[]).map((value) => (
                <option key={value} value={value}>
                  {EXPENSE_CATEGORY_LABEL[value]}
                </option>
              ))}
            </select>
            <Button size="sm" onClick={openCreateSupplierForm}>
              <Plus className="size-4" />
              Nueva factura de proveedor
            </Button>
          </div>
        </div>

        {supplierLoading ? (
          <p className="text-on-surface-variant">Cargando facturas de proveedores…</p>
        ) : supplierError ? (
          <p role="alert" className="text-sm text-error">
            No se pudieron cargar los datos.
          </p>
        ) : (
          <SupplierInvoiceTable invoices={supplierData?.content ?? []} onEdit={openEditSupplierForm} />
        )}

        {supplierData && supplierData.totalPages > 1 && (
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={supplierPage === 0}
              onClick={() => setSupplierPage((current) => current - 1)}
            >
              Anterior
            </Button>
            <span className="text-sm text-on-surface-variant">
              Página {supplierPage + 1} de {supplierData.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={supplierPage + 1 >= supplierData.totalPages}
              onClick={() => setSupplierPage((current) => current + 1)}
            >
              Siguiente
            </Button>
          </div>
        )}
      </section>

      <InvoiceFormModal
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open)
          if (!open) {
            setEditingInvoiceId(undefined)
          }
        }}
        invoice={editingInvoice}
      />
      <SupplierInvoiceFormModal
        open={supplierFormOpen}
        onOpenChange={(open) => {
          setSupplierFormOpen(open)
          if (!open) {
            setEditingSupplierInvoiceId(undefined)
          }
        }}
        supplierInvoice={editingSupplierInvoice}
      />
    </div>
  )
}
