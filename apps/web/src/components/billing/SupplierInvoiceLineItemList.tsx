import { useState, type FormEvent } from 'react'
import { Pencil, Trash2 } from 'lucide-react'
import type { SupplierInvoice, SupplierLineItemResponse } from '@fleetmgm/api'
import { useAddSupplierLineItem, useDeleteSupplierLineItem, useUpdateSupplierLineItem, useVehicles } from '@fleetmgm/hooks'
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

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

const inlineSelectClassName =
  'flex h-9 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-2 py-1 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

type SupplierInvoiceLineItemListProps = {
  supplierInvoice: SupplierInvoice
}

type EditLineItemFormProps = {
  invoiceId: string
  lineItem: SupplierLineItemResponse
  vehiclesPage: { content: { id: string; make: string; model: string; licensePlate: string | null }[] } | undefined
  onDone: () => void
}

function EditLineItemRow({ invoiceId, lineItem, vehiclesPage, onDone }: EditLineItemFormProps) {
  const updateLineItem = useUpdateSupplierLineItem()

  const [vehicleId, setVehicleId] = useState(lineItem.vehicleId ?? '')
  const [description, setDescription] = useState(lineItem.description)
  const [quantity, setQuantity] = useState(String(lineItem.quantity))
  const [subtotal, setSubtotal] = useState(String(lineItem.subtotal))

  function handleSave() {
    updateLineItem.mutate(
      {
        id: invoiceId,
        lineItemId: lineItem.id,
        request: {
          description,
          quantity: Number(quantity),
          subtotal: Number(subtotal),
          vehicleId,
        },
      },
      { onSuccess: onDone },
    )
  }

  // Preview of the derived average unit price while editing — subtotal is now the direct user
  // input, so there's no subtotal to preview; the previously-computed value (unit price) is
  // shown here instead. Guarded against quantity being 0/empty to avoid NaN/Infinity while typing.
  const previewUnitPrice = Number(quantity) > 0 ? (Number(subtotal) / Number(quantity)).toFixed(2) : '—'

  return (
    <TableRow>
      <TableCell>
        <select
          aria-label="Vehículo a editar"
          className={inlineSelectClassName}
          value={vehicleId}
          onChange={(e) => setVehicleId(e.target.value)}
        >
          <option value="" disabled>
            Selecciona un vehículo
          </option>
          {(vehiclesPage?.content ?? []).map((vehicle) => (
            <option key={vehicle.id} value={vehicle.id}>
              {vehicle.make} {vehicle.model}
              {vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}
            </option>
          ))}
        </select>
      </TableCell>
      <TableCell>
        <Input
          aria-label="Descripción a editar"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </TableCell>
      <TableCell>
        <Input
          aria-label="Cantidad a editar"
          type="number"
          min="0"
          step="any"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
      </TableCell>
      <TableCell>{previewUnitPrice}</TableCell>
      <TableCell>
        <Input
          aria-label="Coste total a editar"
          type="number"
          min="0"
          step="any"
          value={subtotal}
          onChange={(e) => setSubtotal(e.target.value)}
        />
      </TableCell>
      <TableCell>
        <div className="flex gap-1">
          <Button variant="outline" size="sm" onClick={handleSave} disabled={updateLineItem.isPending}>
            Guardar
          </Button>
          <Button variant="ghost" size="sm" onClick={onDone} disabled={updateLineItem.isPending}>
            Cancelar
          </Button>
        </div>
      </TableCell>
    </TableRow>
  )
}

export function SupplierInvoiceLineItemList({ supplierInvoice }: SupplierInvoiceLineItemListProps) {
  const addLineItem = useAddSupplierLineItem()
  const deleteLineItem = useDeleteSupplierLineItem()
  const { data: vehiclesPage } = useVehicles(0, 100)

  const [vehicleId, setVehicleId] = useState('')
  const [description, setDescription] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [subtotal, setSubtotal] = useState('')
  const [editingLineItemId, setEditingLineItemId] = useState<string | null>(null)

  const canAddLineItem = supplierInvoice.status === 'PENDING'

  const allocatedTotal = supplierInvoice.lineItems.reduce((sum, lineItem) => sum + lineItem.subtotal, 0)
  const remaining = supplierInvoice.subtotal - allocatedTotal

  function vehicleLabel(id: string | null): string {
    if (id == null) {
      return '—'
    }
    const vehicle = (vehiclesPage?.content ?? []).find((v) => v.id === id)
    if (!vehicle) {
      return '—'
    }
    return `${vehicle.make} ${vehicle.model}${vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}`
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    addLineItem.mutate(
      {
        id: supplierInvoice.id,
        request: {
          description,
          quantity: Number(quantity),
          subtotal: Number(subtotal),
          vehicleId,
        },
      },
      {
        onSuccess: () => {
          setVehicleId('')
          setDescription('')
          setQuantity('1')
          setSubtotal('')
        },
      },
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-on-surface-variant" data-testid="line-item-allocation-summary">
        {`Asignado: ${allocatedTotal.toFixed(2)} € / ${supplierInvoice.subtotal.toFixed(2)} €`}
        {remaining !== 0 && ` (quedan ${remaining.toFixed(2)} € por asignar)`}
      </p>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Vehículo</TableHead>
            <TableHead>Descripción</TableHead>
            <TableHead>Cantidad</TableHead>
            <TableHead>Precio medio</TableHead>
            <TableHead>Subtotal</TableHead>
            {canAddLineItem && <TableHead>Acciones</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {supplierInvoice.lineItems.length === 0 ? (
            <TableRow>
              <TableCell colSpan={canAddLineItem ? 6 : 5} className="text-center text-on-surface-variant">
                Sin líneas todavía.
              </TableCell>
            </TableRow>
          ) : (
            supplierInvoice.lineItems.map((lineItem) =>
              editingLineItemId === lineItem.id ? (
                <EditLineItemRow
                  key={lineItem.id}
                  invoiceId={supplierInvoice.id}
                  lineItem={lineItem}
                  vehiclesPage={vehiclesPage}
                  onDone={() => setEditingLineItemId(null)}
                />
              ) : (
                <TableRow key={lineItem.id}>
                  <TableCell>{vehicleLabel(lineItem.vehicleId)}</TableCell>
                  <TableCell>{lineItem.description}</TableCell>
                  <TableCell>{lineItem.quantity}</TableCell>
                  <TableCell>{lineItem.unitPrice.toFixed(2)}</TableCell>
                  <TableCell>{lineItem.subtotal.toFixed(2)}</TableCell>
                  {canAddLineItem && (
                    <TableCell>
                      <div className="flex gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label="Editar línea"
                          onClick={() => setEditingLineItemId(lineItem.id)}
                        >
                          <Pencil className="size-4" />
                        </Button>
                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              aria-label="Eliminar línea"
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
                                  deleteLineItem.mutate({ id: supplierInvoice.id, lineItemId: lineItem.id })
                                }
                              >
                                Eliminar
                              </AlertDialogAction>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      </div>
                    </TableCell>
                  )}
                </TableRow>
              ),
            )
          )}
        </TableBody>
      </Table>

      {canAddLineItem && (
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="supplier-line-item-vehicle">Vehículo de la línea</Label>
            <select
              id="supplier-line-item-vehicle"
              className={selectClassName}
              value={vehicleId}
              onChange={(e) => setVehicleId(e.target.value)}
              required
            >
              <option value="" disabled>
                Selecciona un vehículo
              </option>
              {(vehiclesPage?.content ?? []).map((vehicle) => (
                <option key={vehicle.id} value={vehicle.id}>
                  {vehicle.make} {vehicle.model}
                  {vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-line-item-description">Descripción</Label>
              <Input
                id="supplier-line-item-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-line-item-quantity">Cantidad</Label>
              <Input
                id="supplier-line-item-quantity"
                type="number"
                min="0"
                step="any"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="supplier-line-item-subtotal">Coste total</Label>
              <Input
                id="supplier-line-item-subtotal"
                type="number"
                min="0"
                step="any"
                value={subtotal}
                onChange={(e) => setSubtotal(e.target.value)}
                required
              />
            </div>
          </div>

          {addLineItem.isError && (
            <p role="alert" className="text-sm text-error">
              No se pudo agregar la línea.
            </p>
          )}

          <Button type="submit" size="sm" variant="outline" disabled={addLineItem.isPending}>
            Agregar línea
          </Button>
        </form>
      )}
    </div>
  )
}
