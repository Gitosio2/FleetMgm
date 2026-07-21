import { useState } from 'react'
import type { ExpenseCategory, SupplierInvoice, SupplierInvoiceStatus } from '@fleetmgm/api'
import { useAllSuppliers, useAllVehicles, useSupplierInvoice, useSupplierInvoices } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { SupplierInvoiceTable } from '@/components/billing/SupplierInvoiceTable'
import { SupplierInvoiceFormModal } from '@/components/billing/SupplierInvoiceFormModal'
import { SupplierInvoiceFilters } from '@/components/billing/SupplierInvoiceFilters'

const PAGE_SIZE = 20

export function SupplierInvoices() {
  const [page, setPage] = useState(0)
  const [supplierInvoiceNumber, setSupplierInvoiceNumber] = useState('')
  const [supplierId, setSupplierId] = useState('')
  const [categoryFilter, setCategoryFilter] = useState<ExpenseCategory | ''>('')
  const [vehicleId, setVehicleId] = useState('')
  const [status, setStatus] = useState<SupplierInvoiceStatus | ''>('')
  const [invoiceDateFrom, setInvoiceDateFrom] = useState('')
  const [invoiceDateTo, setInvoiceDateTo] = useState('')
  const [dueDateFrom, setDueDateFrom] = useState('')
  const [dueDateTo, setDueDateTo] = useState('')
  const [totalMin, setTotalMin] = useState('')
  const [totalMax, setTotalMax] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingInvoiceId, setEditingInvoiceId] = useState<string | undefined>(undefined)

  const { data: suppliers = [] } = useAllSuppliers()
  const { data: vehicles = [] } = useAllVehicles()

  const { data, isLoading, isError } = useSupplierInvoices(
    {
      supplierInvoiceNumber: supplierInvoiceNumber === '' ? undefined : supplierInvoiceNumber,
      supplierId: supplierId === '' ? undefined : supplierId,
      category: categoryFilter === '' ? undefined : categoryFilter,
      vehicleId: vehicleId === '' ? undefined : vehicleId,
      status: status === '' ? undefined : status,
      invoiceDateFrom: invoiceDateFrom === '' ? undefined : invoiceDateFrom,
      invoiceDateTo: invoiceDateTo === '' ? undefined : invoiceDateTo,
      dueDateFrom: dueDateFrom === '' ? undefined : dueDateFrom,
      dueDateTo: dueDateTo === '' ? undefined : dueDateTo,
      totalMin: totalMin === '' ? undefined : Number(totalMin),
      totalMax: totalMax === '' ? undefined : Number(totalMax),
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

  // Fetched by id directly (not derived from the filtered/paginated list query) so the modal keeps
  // working regardless of active filters, page, or refetch timing. Deriving this from data.content
  // (as an earlier version did) silently failed whenever the invoice being edited — most critically
  // one *just created* via onCreated below — didn't happen to be present in the current filtered
  // page: the modal would revert to "Nueva factura de proveedor" and a second submit created a
  // duplicate invoice. useSupplierInvoice still reacts live to line-item mutations, since those
  // invalidate the whole [SUPPLIER_INVOICE_KEY] prefix, which covers this per-id query too.
  const { data: editingInvoice } = useSupplierInvoice(editingInvoiceId ?? '')

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
      <SupplierInvoiceFilters
        onCreate={openCreateForm}
        supplierInvoiceNumber={supplierInvoiceNumber}
        onSupplierInvoiceNumberChange={resetPageAnd(setSupplierInvoiceNumber)}
        supplierId={supplierId}
        onSupplierIdChange={resetPageAnd(setSupplierId)}
        category={categoryFilter}
        onCategoryChange={resetPageAnd(setCategoryFilter)}
        vehicleId={vehicleId}
        onVehicleIdChange={resetPageAnd(setVehicleId)}
        status={status}
        onStatusChange={resetPageAnd(setStatus)}
        invoiceDateFrom={invoiceDateFrom}
        onInvoiceDateFromChange={resetPageAnd(setInvoiceDateFrom)}
        invoiceDateTo={invoiceDateTo}
        onInvoiceDateToChange={resetPageAnd(setInvoiceDateTo)}
        dueDateFrom={dueDateFrom}
        onDueDateFromChange={resetPageAnd(setDueDateFrom)}
        dueDateTo={dueDateTo}
        onDueDateToChange={resetPageAnd(setDueDateTo)}
        totalMin={totalMin}
        onTotalMinChange={resetPageAnd(setTotalMin)}
        totalMax={totalMax}
        onTotalMaxChange={resetPageAnd(setTotalMax)}
        suppliers={suppliers}
        vehicles={vehicles}
      />

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
        onCreated={(created) => setEditingInvoiceId(created.id)}
      />
    </div>
  )
}
