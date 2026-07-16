import { useState, type FormEvent } from 'react'
import type { Invoice } from '@fleetmgm/api'
import { useAddLineItem } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatCurrency } from '@/lib/currency'

type LineItemListProps = {
  invoice: Invoice
}

export function LineItemList({ invoice }: LineItemListProps) {
  const addLineItem = useAddLineItem()

  const [description, setDescription] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [unitPrice, setUnitPrice] = useState('')

  const canAddLineItem = invoice.status === 'DRAFT'

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    addLineItem.mutate(
      {
        id: invoice.id,
        request: {
          description,
          quantity: Number(quantity),
          unitPrice: Number(unitPrice),
        },
      },
      {
        onSuccess: () => {
          setDescription('')
          setQuantity('1')
          setUnitPrice('')
        },
      },
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Descripción</TableHead>
            <TableHead>Cantidad</TableHead>
            <TableHead>Precio unitario</TableHead>
            <TableHead>Subtotal</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {invoice.lineItems.length === 0 ? (
            <TableRow>
              <TableCell colSpan={4} className="text-center text-on-surface-variant">
                Sin líneas de factura todavía.
              </TableCell>
            </TableRow>
          ) : (
            invoice.lineItems.map((lineItem) => (
              <TableRow key={lineItem.id}>
                <TableCell>{lineItem.description}</TableCell>
                <TableCell>{lineItem.quantity}</TableCell>
                <TableCell>{formatCurrency(lineItem.unitPrice)}</TableCell>
                <TableCell>{formatCurrency(lineItem.subtotal)}</TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>

      {canAddLineItem && (
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <div className="grid grid-cols-3 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="line-item-description">Descripción</Label>
              <Input
                id="line-item-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="line-item-quantity">Cantidad</Label>
              <Input
                id="line-item-quantity"
                type="number"
                min="0"
                step="any"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="line-item-unit-price">Precio unitario</Label>
              <Input
                id="line-item-unit-price"
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
