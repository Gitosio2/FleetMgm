import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Invoice } from '@fleetmgm/api'
import { useInvoices } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { InvoiceTable } from '@/components/billing/InvoiceTable'
import { InvoiceFormModal } from '@/components/billing/InvoiceFormModal'

const PAGE_SIZE = 20

export function Billing() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingInvoiceId, setEditingInvoiceId] = useState<string | undefined>(undefined)

  const { data, isLoading, isError } = useInvoices(page, PAGE_SIZE)

  // Derived from the live query data (not a captured snapshot) so that mutations made while the
  // modal is open — e.g. adding a line item, which invalidates and refetches the invoice list —
  // are reflected immediately in the modal's LineItemList instead of showing stale line items.
  const editingInvoice = data?.content.find((invoice) => invoice.id === editingInvoiceId)

  function openCreateForm() {
    setEditingInvoiceId(undefined)
    setFormOpen(true)
  }

  function openEditForm(invoice: Invoice) {
    setEditingInvoiceId(invoice.id)
    setFormOpen(true)
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
    </div>
  )
}
