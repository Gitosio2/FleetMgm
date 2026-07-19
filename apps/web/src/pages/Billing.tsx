import { useState } from 'react'
import type { Invoice, InvoiceStatus } from '@fleetmgm/api'
import { useAllClients, useInvoice, useInvoices } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { InvoiceTable } from '@/components/billing/InvoiceTable'
import { InvoiceFormModal } from '@/components/billing/InvoiceFormModal'
import { InvoiceFilters } from '@/components/billing/InvoiceFilters'

const PAGE_SIZE = 20

export function Billing() {
  const [page, setPage] = useState(0)
  const [invoiceNumber, setInvoiceNumber] = useState('')
  const [clientId, setClientId] = useState('')
  const [status, setStatus] = useState<InvoiceStatus | ''>('')
  const [issueDateFrom, setIssueDateFrom] = useState('')
  const [issueDateTo, setIssueDateTo] = useState('')
  const [dueDateFrom, setDueDateFrom] = useState('')
  const [dueDateTo, setDueDateTo] = useState('')
  const [totalMin, setTotalMin] = useState('')
  const [totalMax, setTotalMax] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingInvoiceId, setEditingInvoiceId] = useState<string | undefined>(undefined)

  const { data: clients = [] } = useAllClients()

  const { data, isLoading, isError } = useInvoices(
    {
      invoiceNumber: invoiceNumber === '' ? undefined : invoiceNumber,
      clientId: clientId === '' ? undefined : clientId,
      status: status === '' ? undefined : status,
      issueDateFrom: issueDateFrom === '' ? undefined : issueDateFrom,
      issueDateTo: issueDateTo === '' ? undefined : issueDateTo,
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
  // page: the modal would revert to "Nueva factura" and a second submit created a duplicate invoice.
  // useInvoice still reacts live to line-item mutations, since those invalidate the whole
  // [INVOICE_KEY] prefix, which covers this per-id query too.
  const { data: editingInvoice } = useInvoice(editingInvoiceId ?? '')

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
      <InvoiceFilters
        onCreate={openCreateForm}
        invoiceNumber={invoiceNumber}
        onInvoiceNumberChange={resetPageAnd(setInvoiceNumber)}
        clientId={clientId}
        onClientIdChange={resetPageAnd(setClientId)}
        status={status}
        onStatusChange={resetPageAnd(setStatus)}
        issueDateFrom={issueDateFrom}
        onIssueDateFromChange={resetPageAnd(setIssueDateFrom)}
        issueDateTo={issueDateTo}
        onIssueDateToChange={resetPageAnd(setIssueDateTo)}
        dueDateFrom={dueDateFrom}
        onDueDateFromChange={resetPageAnd(setDueDateFrom)}
        dueDateTo={dueDateTo}
        onDueDateToChange={resetPageAnd(setDueDateTo)}
        totalMin={totalMin}
        onTotalMinChange={resetPageAnd(setTotalMin)}
        totalMax={totalMax}
        onTotalMaxChange={resetPageAnd(setTotalMax)}
        clients={clients}
      />

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
        onCreated={(created) => setEditingInvoiceId(created.id)}
      />
    </div>
  )
}
