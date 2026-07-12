import { useEffect, useState, type FormEvent } from 'react'
import type { CreateSupplierInvoiceRequest, ExpenseCategory, SupplierInvoice } from '@fleetmgm/api'
import { useCreateSupplierInvoice, useUpdateSupplierInvoice, useVehicles } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { EXPENSE_CATEGORY_LABEL } from './supplier-invoice-shared'
import { SupplierInvoiceLineItemList } from './SupplierInvoiceLineItemList'

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

function toNullableString(value: string): string | null {
  return value === '' ? null : value
}

type SupplierInvoiceFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  supplierInvoice?: SupplierInvoice
}

export function SupplierInvoiceFormModal({ open, onOpenChange, supplierInvoice }: SupplierInvoiceFormModalProps) {
  const isEditing = supplierInvoice != null
  const createSupplierInvoice = useCreateSupplierInvoice()
  const updateSupplierInvoice = useUpdateSupplierInvoice()

  const { data: vehiclesPage } = useVehicles(0, 100)

  const [supplierName, setSupplierName] = useState('')
  const [supplierInvoiceNumber, setSupplierInvoiceNumber] = useState('')
  const [category, setCategory] = useState<ExpenseCategory>('OTHER')
  const [vehicleId, setVehicleId] = useState('')
  const [invoiceDate, setInvoiceDate] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [subtotal, setSubtotal] = useState('')
  const [taxAmount, setTaxAmount] = useState('')
  const [total, setTotal] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setSupplierName(supplierInvoice?.supplierName ?? '')
    setSupplierInvoiceNumber(supplierInvoice?.supplierInvoiceNumber ?? '')
    setCategory(supplierInvoice?.category ?? 'OTHER')
    setVehicleId(supplierInvoice?.vehicleId ?? '')
    setInvoiceDate(supplierInvoice?.invoiceDate ?? '')
    setDueDate(supplierInvoice?.dueDate ?? '')
    setSubtotal(supplierInvoice ? String(supplierInvoice.subtotal) : '')
    setTaxAmount(supplierInvoice ? String(supplierInvoice.taxAmount) : '')
    setTotal(supplierInvoice ? String(supplierInvoice.total) : '')
    setNotes(supplierInvoice?.notes ?? '')
  }, [open, supplierInvoice])

  const isPending = createSupplierInvoice.isPending || updateSupplierInvoice.isPending
  // A PAID invoice is immutable — the backend already rejects update()/addLineItem() with 409
  // SUPPLIER_INVOICE_INVALID_STATE_TRANSITION for it; this surfaces that constraint as a read-only
  // view instead of letting the user fill out the form only to have the save fail.
  const isReadOnly = supplierInvoice?.status === 'PAID'

  // Subtotal is the "source" amount (what the supplier states pre-tax) — editing it or the tax
  // recomputes Total. Editing Total directly does the inverse: it recomputes Subtotal, holding the
  // tax amount fixed, so Subtotal + IVA === Total always holds after either edit path.
  function recomputeTotal(nextSubtotal: string, nextTaxAmount: string) {
    if (nextSubtotal === '' || nextTaxAmount === '') {
      return
    }
    const sub = Number(nextSubtotal)
    const tax = Number(nextTaxAmount)
    if (Number.isNaN(sub) || Number.isNaN(tax)) {
      return
    }
    setTotal((sub + tax).toFixed(2))
  }

  function recomputeSubtotal(nextTotal: string, currentTaxAmount: string) {
    if (nextTotal === '' || currentTaxAmount === '') {
      return
    }
    const tot = Number(nextTotal)
    const tax = Number(currentTaxAmount)
    if (Number.isNaN(tot) || Number.isNaN(tax)) {
      return
    }
    setSubtotal((tot - tax).toFixed(2))
  }
  // Header vehicle and per-vehicle line items are mutually exclusive (see ProfitabilityRepository's
  // si2.vehicle_id IS NULL condition, which already assumes this invariant). Once the invoice has
  // line items, the header vehicle select is locked — the backend would reject it anyway (409
  // SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT), this just surfaces the constraint earlier.
  const hasLineItems = (supplierInvoice?.lineItems.length ?? 0) > 0

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isReadOnly) {
      return
    }

    const request: CreateSupplierInvoiceRequest = {
      supplierName,
      supplierInvoiceNumber: toNullableString(supplierInvoiceNumber),
      category,
      invoiceDate,
      dueDate: toNullableString(dueDate),
      vehicleId: toNullableString(vehicleId),
      subtotal: Number(subtotal),
      taxAmount: Number(taxAmount),
      total: Number(total),
      notes: toNullableString(notes),
    }

    if (supplierInvoice) {
      updateSupplierInvoice.mutate(
        { id: supplierInvoice.id, request },
        { onSuccess: () => onOpenChange(false) },
      )
    } else {
      createSupplierInvoice.mutate(request, { onSuccess: () => onOpenChange(false) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {isReadOnly
              ? 'Factura de proveedor'
              : isEditing
                ? 'Editar factura de proveedor'
                : 'Nueva factura de proveedor'}
          </DialogTitle>
        </DialogHeader>

        <form id="supplier-invoice-form" className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-supplier-name">Proveedor</Label>
              <Input
                id="supplier-invoice-supplier-name"
                value={supplierName}
                onChange={(e) => setSupplierName(e.target.value)}
                disabled={isReadOnly}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-number">Nº factura proveedor</Label>
              <Input
                id="supplier-invoice-number"
                value={supplierInvoiceNumber}
                onChange={(e) => setSupplierInvoiceNumber(e.target.value)}
                disabled={isReadOnly}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-category">Categoría</Label>
              <select
                id="supplier-invoice-category"
                className={selectClassName}
                value={category}
                onChange={(e) => setCategory(e.target.value as ExpenseCategory)}
                disabled={isReadOnly}
                required
              >
                {(Object.keys(EXPENSE_CATEGORY_LABEL) as ExpenseCategory[]).map((value) => (
                  <option key={value} value={value}>
                    {EXPENSE_CATEGORY_LABEL[value]}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-vehicle">Vehículo</Label>
              <select
                id="supplier-invoice-vehicle"
                className={selectClassName}
                value={vehicleId}
                onChange={(e) => setVehicleId(e.target.value)}
                disabled={isReadOnly || hasLineItems}
              >
                <option value="">Sin vehículo asociado</option>
                {(vehiclesPage?.content ?? []).map((vehicle) => (
                  <option key={vehicle.id} value={vehicle.id}>
                    {vehicle.make} {vehicle.model}
                    {vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}
                  </option>
                ))}
              </select>
              {!isReadOnly && hasLineItems && (
                <p className="text-xs text-on-surface-variant">
                  Esta factura ya tiene vehículos asignados por línea.
                </p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-date">Fecha de factura</Label>
              <Input
                id="supplier-invoice-date"
                type="date"
                value={invoiceDate}
                onChange={(e) => setInvoiceDate(e.target.value)}
                disabled={isReadOnly}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-due-date">Fecha de vencimiento</Label>
              <Input
                id="supplier-invoice-due-date"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                disabled={isReadOnly}
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-subtotal">Subtotal</Label>
              <Input
                id="supplier-invoice-subtotal"
                type="number"
                min="0"
                step="0.01"
                value={subtotal}
                onChange={(e) => {
                  setSubtotal(e.target.value)
                  recomputeTotal(e.target.value, taxAmount)
                }}
                disabled={isReadOnly}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-tax-amount">IVA</Label>
              <Input
                id="supplier-invoice-tax-amount"
                type="number"
                min="0"
                step="0.01"
                value={taxAmount}
                onChange={(e) => {
                  setTaxAmount(e.target.value)
                  recomputeTotal(subtotal, e.target.value)
                }}
                disabled={isReadOnly}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-invoice-total">Total</Label>
              <Input
                id="supplier-invoice-total"
                type="number"
                min="0"
                step="0.01"
                value={total}
                onChange={(e) => {
                  setTotal(e.target.value)
                  recomputeSubtotal(e.target.value, taxAmount)
                }}
                disabled={isReadOnly}
                required
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="supplier-invoice-notes">Notas</Label>
            <Input
              id="supplier-invoice-notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              disabled={isReadOnly}
            />
          </div>

          {(createSupplierInvoice.isError || updateSupplierInvoice.isError) && (
            <p role="alert" className="text-sm text-error">
              No se pudo completar la acción.
            </p>
          )}
        </form>

        {isEditing && !supplierInvoice.vehicleId && (
          <div className="mt-6 flex flex-col gap-2 border-t border-outline-variant/40 pt-4">
            <h3 className="font-display text-sm font-semibold">Líneas por vehículo</h3>
            <SupplierInvoiceLineItemList supplierInvoice={supplierInvoice} />
          </div>
        )}

        <DialogFooter className="mt-6">
          {isReadOnly ? (
            <Button type="button" onClick={() => onOpenChange(false)}>
              Cerrar
            </Button>
          ) : (
            <Button type="submit" form="supplier-invoice-form" disabled={isPending}>
              {isEditing ? 'Guardar cambios' : 'Crear factura'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
