import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { ExpenseCategory, SupplierInvoice, SupplierInvoiceStatus } from '@fleetmgm/api'
import { useSupplierInvoices, useSuppliers, useVehicles } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { SupplierInvoiceTable } from '@/components/billing/SupplierInvoiceTable'
import { SupplierInvoiceFormModal } from '@/components/billing/SupplierInvoiceFormModal'
import { SupplierInvoiceFilters } from '@/components/billing/SupplierInvoiceFilters'

const PAGE_SIZE = 20

export function SupplierInvoices() {
  const [page, setPage] = useState(0)
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

  const { data: suppliersPage } = useSuppliers(0, 100)
  const { data: vehiclesPage } = useVehicles(0, 100)

  const { data, isLoading, isError } = useSupplierInvoices(
    {
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
        <Button onClick={openCreateForm}>
          <Plus className="size-4" />
          Nueva factura de proveedor
        </Button>
      </div>

      <SupplierInvoiceFilters
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
        suppliers={suppliersPage?.content ?? []}
        vehicles={vehiclesPage?.content ?? []}
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
      />
    </div>
  )
}
