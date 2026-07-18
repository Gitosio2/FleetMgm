import { useState, type FormEvent } from 'react'
import { Pencil } from 'lucide-react'
import type { Invoice, LineItemResponse } from '@fleetmgm/api'
import { useAddLineItem, useUpdateLineItem } from '@fleetmgm/hooks'
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
  const [editingId, setEditingId] = useState<string | null>(null)

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
            <TableHead>Acciones</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {invoice.lineItems.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-on-surface-variant">
                Sin líneas de factura todavía.
              </TableCell>
            </TableRow>
          ) : (
            invoice.lineItems.map((lineItem) =>
              editingId === lineItem.id ? (
                <EditableLineItemRow
                  key={lineItem.id}
                  invoiceId={invoice.id}
                  lineItem={lineItem}
                  onCancel={() => setEditingId(null)}
                  onSaved={() => setEditingId(null)}
                />
              ) : (
                <TableRow key={lineItem.id}>
                  <TableCell>{lineItem.description}</TableCell>
                  <TableCell>{lineItem.quantity}</TableCell>
                  <TableCell>{formatCurrency(lineItem.unitPrice)}</TableCell>
                  <TableCell>{formatCurrency(lineItem.subtotal)}</TableCell>
                  <TableCell>
                    {canAddLineItem && (
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
                    )}
                  </TableCell>
                </TableRow>
              ),
            )
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

type EditableLineItemRowProps = {
  invoiceId: string
  lineItem: LineItemResponse
  onCancel: () => void
  onSaved: () => void
}

function EditableLineItemRow({ invoiceId, lineItem, onCancel, onSaved }: EditableLineItemRowProps) {
  const updateLineItem = useUpdateLineItem()

  const [description, setDescription] = useState(lineItem.description)
  const [quantity, setQuantity] = useState(String(lineItem.quantity))
  const [unitPrice, setUnitPrice] = useState(String(lineItem.unitPrice))

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    updateLineItem.mutate(
      {
        invoiceId,
        lineItemId: lineItem.id,
        request: {
          description,
          quantity: Number(quantity),
          unitPrice: Number(unitPrice),
        },
      },
      { onSuccess: onSaved },
    )
  }

  return (
    <TableRow>
      <TableCell colSpan={5}>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <div className="grid grid-cols-3 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`edit-line-item-description-${lineItem.id}`}>Descripción</Label>
              <Input
                id={`edit-line-item-description-${lineItem.id}`}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`edit-line-item-quantity-${lineItem.id}`}>Cantidad</Label>
              <Input
                id={`edit-line-item-quantity-${lineItem.id}`}
                type="number"
                min="0"
                step="any"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`edit-line-item-unit-price-${lineItem.id}`}>Precio unitario</Label>
              <Input
                id={`edit-line-item-unit-price-${lineItem.id}`}
                type="number"
                min="0"
                step="any"
                value={unitPrice}
                onChange={(e) => setUnitPrice(e.target.value)}
                required
              />
            </div>
          </div>

          {updateLineItem.isError && (
            <p role="alert" className="text-sm text-error">
              No se pudo actualizar la línea.
            </p>
          )}

          <div className="flex gap-2">
            <Button type="submit" size="sm" variant="outline" disabled={updateLineItem.isPending}>
              Guardar
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
