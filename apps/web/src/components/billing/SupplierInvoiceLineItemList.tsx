import { useState, type FormEvent } from 'react'
import type { SupplierInvoice } from '@fleetmgm/api'
import { useAddSupplierLineItem, useVehicles } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

type SupplierInvoiceLineItemListProps = {
  supplierInvoice: SupplierInvoice
}

export function SupplierInvoiceLineItemList({ supplierInvoice }: SupplierInvoiceLineItemListProps) {
  const addLineItem = useAddSupplierLineItem()
  const { data: vehiclesPage } = useVehicles(0, 100)

  const [vehicleId, setVehicleId] = useState('')
  const [description, setDescription] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [unitPrice, setUnitPrice] = useState('')

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
          unitPrice: Number(unitPrice),
          vehicleId,
        },
      },
      {
        onSuccess: () => {
          setVehicleId('')
          setDescription('')
          setQuantity('1')
          setUnitPrice('')
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
            <TableHead>Precio unitario</TableHead>
            <TableHead>Subtotal</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {supplierInvoice.lineItems.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-on-surface-variant">
                Sin líneas todavía.
              </TableCell>
            </TableRow>
          ) : (
            supplierInvoice.lineItems.map((lineItem) => (
              <TableRow key={lineItem.id}>
                <TableCell>{vehicleLabel(lineItem.vehicleId)}</TableCell>
                <TableCell>{lineItem.description}</TableCell>
                <TableCell>{lineItem.quantity}</TableCell>
                <TableCell>{lineItem.unitPrice.toFixed(2)}</TableCell>
                <TableCell>{lineItem.subtotal.toFixed(2)}</TableCell>
              </TableRow>
            ))
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
              <Label htmlFor="supplier-line-item-unit-price">Precio unitario</Label>
              <Input
                id="supplier-line-item-unit-price"
                type="number"
                min="0"
                step="any"
                value={unitPrice}
                onChange={(e) => setUnitPrice(e.target.value)}
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
