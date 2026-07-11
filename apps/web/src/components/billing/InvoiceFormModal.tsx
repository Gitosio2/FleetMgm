import { useEffect, useState, type FormEvent } from 'react'
import type { CreateInvoiceRequest, Invoice } from '@fleetmgm/api'
import { useClients, useCreateInvoice, useUpdateInvoice } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { LineItemList } from './LineItemList'

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
}

export function InvoiceFormModal({ open, onOpenChange, invoice }: InvoiceFormModalProps) {
  const isEditing = invoice != null
  const createInvoice = useCreateInvoice()
  const updateInvoice = useUpdateInvoice()

  const { data: clientsPage } = useClients(0, 100)

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

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: CreateInvoiceRequest = {
      clientId,
      dueDate: toNullableString(dueDate),
      notes: toNullableString(notes),
      taxRate: percentageInputToFraction(taxRate),
    }

    if (invoice) {
      updateInvoice.mutate({ id: invoice.id, request }, { onSuccess: () => onOpenChange(false) })
    } else {
      createInvoice.mutate(request, { onSuccess: () => onOpenChange(false) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Editar factura' : 'Nueva factura'}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="invoice-client">Cliente</Label>
            <select
              id="invoice-client"
              className={selectClassName}
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
              required
            >
              <option value="" disabled>
                Seleccioná un cliente
              </option>
              {(clientsPage?.content ?? []).map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="invoice-due-date">Fecha de vencimiento</Label>
              <Input
                id="invoice-due-date"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="invoice-tax-rate">IVA (opcional, usa el valor por defecto si se deja vacío)</Label>
              <div className="flex items-center gap-2">
                <Input
                  id="invoice-tax-rate"
                  type="number"
                  min="0"
                  step="any"
                  value={taxRate}
                  onChange={(e) => setTaxRate(e.target.value)}
                />
                <span className="text-sm text-on-surface-variant">%</span>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="invoice-notes">Notas</Label>
            <Input id="invoice-notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>

          {(createInvoice.isError || updateInvoice.isError) && (
            <p role="alert" className="text-sm text-error">
              No se pudo completar la acción.
            </p>
          )}

          <DialogFooter>
            <Button type="submit" disabled={isPending || clientId === ''}>
              {isEditing ? 'Guardar cambios' : 'Crear factura'}
            </Button>
          </DialogFooter>
        </form>

        {isEditing && invoice.status === 'DRAFT' && (
          <div className="mt-6 flex flex-col gap-2 border-t border-outline-variant/40 pt-4">
            <h3 className="font-display text-sm font-semibold">Líneas de factura</h3>
            <LineItemList invoice={invoice} />
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
