import { useState, type FormEvent } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import type { SupplierInvoice, SupplierLineItemRequest, SupplierLineItemResponse } from '@fleetmgm/api'
import {
  useAddSupplierLineItem,
  useAllVehicles,
  useDeleteSupplierLineItem,
  useUpdateSupplierLineItem,
} from '@fleetmgm/hooks'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatCurrency } from '@/lib/currency'

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

type SupplierInvoiceLineItemListProps = {
  supplierInvoice: SupplierInvoice
  readOnly?: boolean
}

type VehicleOption = { id: string; make: string; model: string; licensePlate: string | null }

export function SupplierInvoiceLineItemList({
  supplierInvoice,
  readOnly = false,
}: SupplierInvoiceLineItemListProps) {
  const deleteLineItem = useDeleteSupplierLineItem()
  const { data: vehicles = [] } = useAllVehicles()

  const [editingId, setEditingId] = useState<string | null>(null)
  const [isAdding, setIsAdding] = useState(false)

  const canEditLineItems = !readOnly && supplierInvoice.status === 'PENDING'
  const allocatedTotal = supplierInvoice.lineItems.reduce((sum, lineItem) => sum + lineItem.subtotal, 0)
  const remaining = supplierInvoice.subtotal - allocatedTotal

  function vehicleLabel(id: string | null): string {
    if (id == null) {
      return '—'
    }
    const vehicle = vehicles.find((v) => v.id === id)
    if (!vehicle) {
      return '—'
    }
    return `${vehicle.make} ${vehicle.model}${vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}`
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-on-surface-variant" data-testid="line-item-allocation-summary">
        {`Asignado: ${formatCurrency(allocatedTotal)} / ${formatCurrency(supplierInvoice.subtotal)}`}
        {remaining !== 0 && ` (quedan ${formatCurrency(remaining)} por asignar)`}
      </p>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Vehículo</TableHead>
            <TableHead>Descripción</TableHead>
            <TableHead>Cantidad</TableHead>
            <TableHead>Subtotal</TableHead>
            <TableHead>Acciones</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {supplierInvoice.lineItems.length === 0 && !isAdding && (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-on-surface-variant">
                Sin líneas todavía.
              </TableCell>
            </TableRow>
          )}

          {supplierInvoice.lineItems.map((lineItem) =>
            editingId === lineItem.id ? (
              <SupplierLineItemFormRow
                key={lineItem.id}
                invoiceId={supplierInvoice.id}
                lineItem={lineItem}
                vehicles={vehicles}
                onCancel={() => setEditingId(null)}
                onSaved={() => setEditingId(null)}
              />
            ) : (
              <TableRow key={lineItem.id}>
                <TableCell>{vehicleLabel(lineItem.vehicleId)}</TableCell>
                <TableCell>{lineItem.description}</TableCell>
                <TableCell>{lineItem.quantity}</TableCell>
                <TableCell>{formatCurrency(lineItem.subtotal)}</TableCell>
                <TableCell>
                  {canEditLineItems && (
                    <div className="flex gap-1">
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        aria-label="Editar línea"
                        title="Editar línea"
                        onClick={() => setEditingId(lineItem.id)}
                      >
                        <Pencil className="size-4" />
                      </Button>
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            aria-label="Eliminar línea"
                            title="Eliminar línea"
                            disabled={deleteLineItem.isPending}
                          >
                            <Trash2 className="size-4" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>¿Eliminar esta línea?</AlertDialogTitle>
                            <AlertDialogDescription>
                              Esto elimina la línea "{lineItem.description}" de la factura. Esta acción no se
                              puede deshacer desde aquí.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancelar</AlertDialogCancel>
                            <AlertDialogAction
                              onClick={() =>
                                deleteLineItem.mutate({
                                  id: supplierInvoice.id,
                                  lineItemId: lineItem.id,
                                })
                              }
                            >
                              Eliminar
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
                  )}
                </TableCell>
              </TableRow>
            ),
          )}

          {canEditLineItems &&
            (isAdding ? (
              <SupplierLineItemFormRow
                invoiceId={supplierInvoice.id}
                vehicles={vehicles}
                onCancel={() => setIsAdding(false)}
                onSaved={() => setIsAdding(false)}
              />
            ) : (
              <TableRow>
                <TableCell colSpan={5}>
                  <Button type="button" variant="ghost" size="sm" onClick={() => setIsAdding(true)}>
                    <Plus className="size-4" />
                    Añadir línea
                  </Button>
                </TableCell>
              </TableRow>
            ))}
        </TableBody>
      </Table>
    </div>
  )
}

type SupplierLineItemFormRowProps = {
  invoiceId: string
  lineItem?: SupplierLineItemResponse
  vehicles: VehicleOption[]
  onCancel: () => void
  onSaved: () => void
}

function SupplierLineItemFormRow({
  invoiceId,
  lineItem,
  vehicles,
  onCancel,
  onSaved,
}: SupplierLineItemFormRowProps) {
  const isEditing = lineItem != null
  const addLineItem = useAddSupplierLineItem()
  const updateLineItem = useUpdateSupplierLineItem()
  const mutation = isEditing ? updateLineItem : addLineItem

  const [vehicleId, setVehicleId] = useState(lineItem?.vehicleId ?? '')
  const [description, setDescription] = useState(lineItem?.description ?? '')
  const [quantity, setQuantity] = useState(String(lineItem?.quantity ?? '1'))
  const [subtotal, setSubtotal] = useState(lineItem != null ? String(lineItem.subtotal) : '')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: SupplierLineItemRequest = {
      description,
      quantity: Number(quantity),
      subtotal: Number(subtotal),
      vehicleId,
    }

    if (isEditing) {
      updateLineItem.mutate({ id: invoiceId, lineItemId: lineItem.id, request }, { onSuccess: onSaved })
    } else {
      addLineItem.mutate({ id: invoiceId, request }, { onSuccess: onSaved })
    }
  }

  const idPrefix = isEditing ? `edit-supplier-line-item-${lineItem.id}` : 'new-supplier-line-item'

  return (
    <TableRow>
      <TableCell colSpan={5}>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${idPrefix}-vehicle`}>Vehículo de la línea</Label>
              <select
                id={`${idPrefix}-vehicle`}
                className={selectClassName}
                value={vehicleId}
                onChange={(e) => setVehicleId(e.target.value)}
                required
              >
                <option value="" disabled>
                  Selecciona un vehículo
                </option>
                {vehicles.map((vehicle) => (
                  <option key={vehicle.id} value={vehicle.id}>
                    {vehicle.make} {vehicle.model}
                    {vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${idPrefix}-description`}>Descripción</Label>
              <Input
                id={`${idPrefix}-description`}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${idPrefix}-quantity`}>Cantidad</Label>
              <Input
                id={`${idPrefix}-quantity`}
                type="number"
                min="0"
                step="any"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${idPrefix}-subtotal`}>Coste total</Label>
              <Input
                id={`${idPrefix}-subtotal`}
                type="number"
                min="0"
                step="any"
                value={subtotal}
                onChange={(e) => setSubtotal(e.target.value)}
                required
              />
            </div>
          </div>

          {mutation.isError && (
            <p role="alert" className="text-sm text-error">
              {isEditing ? 'No se pudo actualizar la línea.' : 'No se pudo agregar la línea.'}
            </p>
          )}

          <div className="flex gap-2">
            <Button type="submit" size="sm" variant="outline" disabled={mutation.isPending}>
              {isEditing ? 'Guardar' : 'Agregar línea'}
            </Button>
            <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
              Cancelar
            </Button>
          </div>
        </form>
      </TableCell>
    </TableRow>
  )
}
