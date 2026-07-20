import { useEffect, useState, type FormEvent } from 'react'
import { Info } from 'lucide-react'
import type { CreateInvoiceRequest, Invoice } from '@fleetmgm/api'
import { useAllClients, useCreateInvoice, useUpdateInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { formatCurrency } from '@/lib/currency'
import { LineItemList } from './LineItemList'
import { computeTaxAndTotal, sumLineItemSubtotals } from './invoice-shared'

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

function toNullableString(value: string): string | null {
  return value === '' ? null : value
}

// invoice.taxRate is a fraction (0.2100) — the user enters/sees a whole percentage (21).
// Rounding through an intermediate integer avoids floating-point drift (0.21 * 100 !== 21 exactly).
function fractionToPercentageDisplay(fraction: number | null | undefined): string {
  if (fraction == null) {
    return ''
  }
  return String(Math.round(fraction * 10000) / 100)
}

function percentageInputToFraction(value: string): number | null {
  if (value === '') {
    return null
  }
  return Math.round(Number(value) * 100) / 10000
}

type InvoiceFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  invoice?: Invoice
  onCreated?: (invoice: Invoice) => void
  readOnly?: boolean
}

export function InvoiceFormModal({
  open,
  onOpenChange,
  invoice,
  onCreated,
  readOnly = false,
}: InvoiceFormModalProps) {
  const isEditing = invoice != null
  const createInvoice = useCreateInvoice()
  const updateInvoice = useUpdateInvoice()

  const { data: clients = [] } = useAllClients()

  const [clientId, setClientId] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [notes, setNotes] = useState('')
  const [taxRate, setTaxRate] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setClientId(invoice?.clientId ?? '')
    setDueDate(invoice?.dueDate ?? '')
    setNotes(invoice?.notes ?? '')
    setTaxRate(fractionToPercentageDisplay(invoice?.taxRate))
  }, [open, invoice])

  const isPending = createInvoice.isPending || updateInvoice.isPending

  // DRAFT invoices have subtotal/taxAmount/total stuck at 0 until issue() computes them server-side
  // (see InvoiceService.issue()) — showing invoice.total directly would misleadingly read "0,00€"
  // even with line items worth thousands. This preview mirrors that same computation client-side
  // from the current line items and the tax rate being edited, so the user sees what issuing would
  // produce. ISSUED/PAID invoices show the real persisted values instead, since those are final.
  const isDraft = invoice?.status === 'DRAFT'
  const previewSubtotal = sumLineItemSubtotals(invoice?.lineItems ?? [])
  const previewTaxRate = percentageInputToFraction(taxRate) ?? invoice?.taxRate ?? 0
  const { taxAmount: previewTaxAmount, total: previewTotal } = computeTaxAndTotal(previewSubtotal, previewTaxRate)

  // InvoiceService.update() rejects any edit with 409 INVOICE_INVALID_STATE_TRANSITION unless the
  // invoice is still DRAFT — this surfaces that constraint as a read-only view instead of letting
  // the user fill out the form only to have the save fail (mirrors SupplierInvoiceFormModal's
  // isReadOnly, whose equivalent condition is status === 'PAID' since that domain has no ISSUED).
  // The explicit `readOnly` prop (mirrors ClientFormModal/SupplierFormModal's own readOnly) forces
  // the same locked-down view regardless of status — used by InvoiceInfoLink to open a DRAFT
  // invoice from the dashboard's "Facturas por cobrar" card in consult-only mode.
  const isReadOnly = readOnly || (isEditing && !isDraft)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isReadOnly) {
      return
    }

    const request: CreateInvoiceRequest = {
      clientId,
      dueDate: toNullableString(dueDate),
      notes: toNullableString(notes),
      taxRate: percentageInputToFraction(taxRate),
    }

    if (invoice) {
      updateInvoice.mutate({ id: invoice.id, request }, { onSuccess: () => onOpenChange(false) })
    } else {
      // Stay open and switch into edit mode for the newly created invoice instead of closing —
      // a client invoice can't be created with an amount in one step (CreateInvoiceRequest has no
      // line items), so this lets the user add line items immediately instead of having to close
      // the modal, find the invoice in the list, and reopen it via "Editar".
      createInvoice.mutate(request, { onSuccess: (createdInvoice) => onCreated?.(createdInvoice) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isReadOnly ? 'Factura' : isEditing ? 'Editar factura' : 'Nueva factura'}</DialogTitle>
        </DialogHeader>

        <form id="invoice-form" className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="invoice-client">Cliente</Label>
            <select
              id="invoice-client"
              className={selectClassName}
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
              disabled={isReadOnly}
              required
            >
              <option value="" disabled>
                Seleccioná un cliente
              </option>
              {clients.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </select>
          </div>

          {invoice?.issueDate != null && (
            <div className="flex flex-col gap-1.5">
              <Label>Fecha de emisión</Label>
              <p className="text-sm text-on-surface-variant">{invoice.issueDate}</p>
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="invoice-due-date">Fecha de vencimiento</Label>
              <Input
                id="invoice-due-date"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                disabled={isReadOnly}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center gap-1">
                <Label htmlFor="invoice-tax-rate">IVA (opcional)</Label>
                <span title="Si se deja vacío, se usa el valor por defecto configurado.">
                  <Info className="size-3.5 text-on-surface-variant" />
                </span>
              </div>
              <div className="flex items-center gap-2">
                <Input
                  id="invoice-tax-rate"
                  type="number"
                  min="0"
                  step="any"
                  value={taxRate}
                  onChange={(e) => setTaxRate(e.target.value)}
                  disabled={isReadOnly}
                />
                <span className="text-sm text-on-surface-variant">%</span>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="invoice-notes">Notas</Label>
            <Input
              id="invoice-notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              disabled={isReadOnly}
            />
          </div>

          {(createInvoice.isError || updateInvoice.isError) && (
            <p role="alert" className="text-sm text-error">
              No se pudo completar la acción.
            </p>
          )}
        </form>

        {isEditing && (
          <div className="mt-6 flex flex-col gap-2 border-t border-outline-variant/40 pt-4">
            <h3 className="font-display text-sm font-semibold">Líneas de factura</h3>
            {/* LineItemList itself only shows the "add line" form when status === 'DRAFT' and
                !readOnly — the table of existing lines is always rendered, so ISSUED/PAID
                invoices (or a forced-readOnly DRAFT one) show them read-only instead of not
                being shown at all. */}
            <LineItemList invoice={invoice} readOnly={isReadOnly} />
          </div>
        )}

        {isEditing && (
          <div className="mt-4 grid grid-cols-3 gap-4 rounded-lg border border-outline-variant/40 p-4 text-sm">
            <div>
              <p className="text-on-surface-variant">Subtotal</p>
              <p className="font-semibold">{formatCurrency(isDraft ? previewSubtotal : invoice.subtotal)}</p>
            </div>
            <div>
              <p className="text-on-surface-variant">IVA</p>
              <p className="font-semibold">{formatCurrency(isDraft ? previewTaxAmount : invoice.taxAmount)}</p>
            </div>
            <div>
              <p className="text-on-surface-variant">Total</p>
              <p className="font-semibold">{formatCurrency(isDraft ? previewTotal : invoice.total)}</p>
            </div>
            {isDraft && (
              <p className="col-span-3 text-xs text-on-surface-variant">
                Estimado a partir de las líneas actuales y el IVA configurado — se recalcula y queda fijo al
                emitir la factura.
              </p>
            )}
          </div>
        )}

        <DialogFooter>
          {isReadOnly ? (
            <Button type="button" onClick={() => onOpenChange(false)}>
              Cerrar
            </Button>
          ) : (
            <Button type="submit" form="invoice-form" disabled={isPending || clientId === ''}>
              {isEditing ? 'Guardar cambios' : 'Crear factura'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
